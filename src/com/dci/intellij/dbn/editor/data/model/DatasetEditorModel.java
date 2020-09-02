package com.dci.intellij.dbn.editor.data.model;

import com.dci.intellij.dbn.common.LoggerFactory;
import com.dci.intellij.dbn.common.dispose.AlreadyDisposedException;
import com.dci.intellij.dbn.common.dispose.Disposer;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.environment.EnvironmentManager;
import com.dci.intellij.dbn.common.thread.CancellableDatabaseCall;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.common.util.MessageUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionProperties;
import com.dci.intellij.dbn.connection.ResourceUtil;
import com.dci.intellij.dbn.connection.SessionId;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.jdbc.DBNResultSet;
import com.dci.intellij.dbn.connection.jdbc.DBNStatement;
import com.dci.intellij.dbn.data.model.resultSet.ResultSetDataModel;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.editor.data.DatasetEditor;
import com.dci.intellij.dbn.editor.data.DatasetEditorError;
import com.dci.intellij.dbn.editor.data.filter.DatasetFilter;
import com.dci.intellij.dbn.editor.data.filter.DatasetFilterInput;
import com.dci.intellij.dbn.editor.data.filter.DatasetFilterManager;
import com.dci.intellij.dbn.editor.data.options.DataEditorSettings;
import com.dci.intellij.dbn.editor.data.state.DatasetEditorState;
import com.dci.intellij.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dci.intellij.dbn.object.DBColumn;
import com.dci.intellij.dbn.object.DBConstraint;
import com.dci.intellij.dbn.object.DBDataset;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.dci.intellij.dbn.connection.ConnectionProperty.RS_TYPE_FORWARD_ONLY;
import static com.dci.intellij.dbn.connection.ConnectionProperty.RS_TYPE_SCROLL_INSENSITIVE;
import static com.dci.intellij.dbn.editor.data.model.RecordStatus.*;

public class DatasetEditorModel
        extends ResultSetDataModel<DatasetEditorModelRow, DatasetEditorModelCell>
        implements ListSelectionListener {

    private static final Logger LOGGER = LoggerFactory.createLogger();

    private boolean isResultSetUpdatable;
    private DatasetEditor datasetEditor;
    private DataEditorSettings settings;
    private DBObjectRef<DBDataset> datasetRef;

    private CancellableDatabaseCall loaderCall;
    private ResultSetAdapter resultSetAdapter;

    private List<DatasetEditorModelRow> changedRows = new ArrayList<>();

    public DatasetEditorModel(DatasetEditor datasetEditor) throws SQLException {
        super(datasetEditor.getConnectionHandler());
        Project project = getProject();
        this.datasetEditor = datasetEditor;
        DBDataset dataset = datasetEditor.getDataset();
        this.datasetRef = DBObjectRef.from(dataset);
        this.settings =  DataEditorSettings.getInstance(project);
        setHeader(new DatasetEditorModelHeader(datasetEditor, null));
        this.isResultSetUpdatable = DatabaseFeature.UPDATABLE_RESULT_SETS.isSupported(getConnectionHandler());

        EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
        boolean readonly = environmentManager.isReadonly(dataset, DBContentType.DATA);
        setEnvironmentReadonly(readonly);
    }

    public void load(final boolean useCurrentFilter, final boolean keepChanges) throws SQLException {
        set(DIRTY, false);
        checkDisposed();
        closeResultSet();
        int timeout = getSettings().getGeneralSettings().getFetchTimeout().value();
        AtomicReference<DBNStatement> statementRef = new AtomicReference<>();
        ConnectionHandler connectionHandler = getConnectionHandler();
        DBNConnection connection = connectionHandler.getMainConnection();

        loaderCall = new CancellableDatabaseCall(connectionHandler, connection, timeout, TimeUnit.SECONDS) {
            @Override
            public Object execute() throws Exception {
                DBNResultSet newResultSet = loadResultSet(useCurrentFilter, statementRef);

                if (newResultSet != null) {
                    checkDisposed();
                    setHeader(new DatasetEditorModelHeader(getDatasetEditor(), newResultSet));

                    setResultSet(newResultSet);
                    setResultSetExhausted(false);
                    if (keepChanges) snapshotChanges(); else clearChanges();

                    int rowCount = computeRowCount();

                    fetchNextRecords(rowCount, true);
                    restoreChanges();
                }
                loaderCall = null;
                return null;
            }

            @Override
            public void cancel() {
                DBNStatement statement = statementRef.get();
                ResourceUtil.cancel(statement);
                loaderCall = null;
                set(DIRTY, true);
            }
        };
        loaderCall.start();
    }

    @Override
    public void setResultSet(DBNResultSet resultSet) throws SQLException {
        super.setResultSet(resultSet);

        // instructions the adapter
        Disposer.dispose(resultSetAdapter);
        ConnectionHandler connectionHandler = getConnectionHandler();
        resultSetAdapter = DatabaseFeature.UPDATABLE_RESULT_SETS.isSupported(connectionHandler) ?
                new EditableResultSetAdapter(this, resultSet) :
                new ReadonlyResultSetAdapter(this, resultSet);
        Disposer.register(this, resultSetAdapter);
    }

    @NotNull
    ResultSetAdapter getResultSetAdapter() {
        return Failsafe.nn(resultSetAdapter);
    }

    private int computeRowCount() {
        int originalRowCount = getRowCount();
        int stateRowCount = getState().getRowCount();
        int fetchRowCount = Math.max(stateRowCount, originalRowCount);

        int fetchBlockSize = getSettings().getGeneralSettings().getFetchBlockSize().value();
        fetchRowCount = (fetchRowCount/fetchBlockSize + 1) * fetchBlockSize;

        return Math.max(fetchRowCount, fetchBlockSize);
    }

    public DataEditorSettings getSettings() {
        return Failsafe.nn(settings);
    }

    private DBNResultSet loadResultSet(boolean useCurrentFilter, AtomicReference<DBNStatement> statementRef) throws SQLException {
        int timeout = getSettings().getGeneralSettings().getFetchTimeout().value();
        ConnectionHandler connectionHandler = getConnectionHandler();
        DBNConnection connection = connectionHandler.getMainConnection();
        DBDataset dataset = getDataset();
        Project project = dataset.getProject();
        DatasetFilter filter = DatasetFilterManager.EMPTY_FILTER;
        if (useCurrentFilter) {
            DatasetFilterManager filterManager = DatasetFilterManager.getInstance(project);
            filter = filterManager.getActiveFilter(dataset);
            if (filter == null) filter = DatasetFilterManager.EMPTY_FILTER;
        }

        String selectStatement = filter.createSelectStatement(dataset, getState().getSortingState());
        DBNStatement statement = null;
        if (isReadonly()) {
            statement = connection.createStatement();
        } else {
            // ensure we always get a statement,
            ConnectionProperties properties = connection.getProperties();
            if (properties.is(RS_TYPE_SCROLL_INSENSITIVE)) {
                try {
                    statement = connection.createStatement(
                            ResultSet.TYPE_SCROLL_INSENSITIVE,
                            ResultSet.CONCUR_UPDATABLE);

                } catch (Throwable e) {
                    LOGGER.warn("Failed to create SCROLL_INSENSITIVE statement: " + e.getMessage());
                }
            }

            if (statement == null && properties.is(RS_TYPE_FORWARD_ONLY)) {
                try {
                    statement = connection.createStatement(
                            ResultSet.TYPE_FORWARD_ONLY,
                            ResultSet.CONCUR_READ_ONLY);
                } catch (Throwable e) {
                    LOGGER.warn("Failed to create FORWARD_ONLY statement: " + e.getMessage());
                }
            }

            if (statement == null) {
                // default statement creation
                statement = connection.createStatement();
            }
        }
        statementRef.set(statement);
        checkDisposed();
        if (timeout != -1) {
            statement.setQueryTimeout(timeout);
        }

        return statement.executeQuery(selectStatement);
    }

    public boolean isDirty() {
        return is(DIRTY);
    }

    public void cancelDataLoad() {
        if (loaderCall != null) {
            loaderCall.requestCancellation();
        }
    }

    public boolean isLoadCancelled() {
        return loaderCall != null && loaderCall.isCancelRequested();
    }

    private void snapshotChanges() {
        for (DatasetEditorModelRow row : getRows()) {
            if (row.is(DELETED) || row.is(MODIFIED) || row.is(INSERTED)) {
                changedRows.add(row);
            }
        }
    }

    private void restoreChanges() {
        if (hasChanges()) {
            for (DatasetEditorModelRow row : getRows()) {
                checkDisposed();

                DatasetEditorModelRow changedRow = lookupChangedRow(row, true);
                if (changedRow != null) {
                    row.updateStatusFromRow(changedRow);
                }
            }
            set(MODIFIED, true);
        }
    }

    private DatasetEditorModelRow lookupChangedRow(DatasetEditorModelRow row, boolean remove) {
        for (DatasetEditorModelRow changedRow : changedRows) {
            if (changedRow.isNot(DELETED) && changedRow.matches(row, false)) {
                if (remove) changedRows.remove(changedRow);
                return changedRow;
            }
        }
        return null;
    }

    @Override
    protected void disposeRow(DatasetEditorModelRow row) {
        if (!changedRows.contains(row)) {
            super.disposeRow(row);
        }
    }

    @NotNull
    @Override
    public DatasetEditorState getState() {
        return datasetEditor == null ? DatasetEditorState.VOID : datasetEditor.getEditorState();
    }

    private boolean hasChanges() {
        return changedRows.size() > 0;
    }

    private void clearChanges() {
        changedRows.clear();
    }

    @Override
    public boolean isReadonly() {
        return !isEditable();
    }

    public boolean isEditable() {
        return getDataset().isEditable(DBContentType.DATA);
    }

    @NotNull
    @Override
    public DatasetEditorModelHeader getHeader() {
        return (DatasetEditorModelHeader) super.getHeader();
    }

    @Override
    protected DatasetEditorModelRow createRow(int resultSetRowIndex) throws SQLException {
        return new DatasetEditorModelRow(this, getResultSet(), resultSetRowIndex);
    }

    @NotNull
    public DBDataset getDataset() {
        return Failsafe.nn(DBObjectRef.get(datasetRef));
    }

    @NotNull
    public DatasetEditor getDatasetEditor() {
        return Failsafe.nn(datasetEditor);
    }

    @NotNull
    public DatasetEditorTable getEditorTable() {
        return getDatasetEditor().getEditorTable();
    }

    @Nullable
    public DatasetFilterInput resolveForeignKeyRecord(DatasetEditorModelCell cell) {
        DBColumn column = cell.getColumnInfo().getColumn();
        if (column.isForeignKey()) {
            for (DBConstraint constraint : column.getConstraints()) {
                constraint = (DBConstraint) constraint.getUndisposedElement();
                if (constraint != null && constraint.isForeignKey()) {
                    DBConstraint foreignKeyConstraint = constraint.getForeignKeyConstraint();
                    if (foreignKeyConstraint != null) {
                        DBDataset foreignKeyDataset = foreignKeyConstraint.getDataset();
                        DatasetFilterInput filterInput = new DatasetFilterInput(foreignKeyDataset);

                        for (DBColumn constraintColumn : constraint.getColumns()) {
                            constraintColumn = (DBColumn) constraintColumn.getUndisposedElement();
                            if (constraintColumn != null) {
                                DBColumn foreignKeyColumn = constraintColumn.getForeignKeyColumn();
                                if (foreignKeyColumn != null) {
                                    DatasetEditorModelCell constraintCell = cell.getRow().getCellForColumn(constraintColumn);
                                    if (constraintCell != null) {
                                        Object value = constraintCell.getUserValue();
                                        filterInput.setColumnValue(foreignKeyColumn, value);
                                    }
                                }
                            }
                        }
                        return filterInput;

                    }

                }
            }
        }
        return null;
    }

    /****************************************************************
     *                        Editor actions                        *
     ****************************************************************/
    public void deleteRecords(int[] rowIndexes) {
        DatasetEditorTable editorTable = getEditorTable();
        editorTable.fireEditingCancel();
        Progress.prompt(getProject(), "Deleting records", true,
                (progress) -> {
                    progress.setIndeterminate(false);
                    for (int index : rowIndexes) {
                        progress.setFraction(CommonUtil.getProgressPercentage(index, rowIndexes.length));
                        DatasetEditorModelRow row = getRowAtIndex(index);
                        if (progress.isCanceled()) break;

                        if (row != null && row.isNot(DELETED)) {
                            int rsRowIndex = row.getResultSetRowIndex();
                            row.delete();
                            if (row.is(DELETED)) {
                                shiftResultSetRowIndex(rsRowIndex, -1);
                                notifyRowUpdated(index);
                            }
                        }
                        set(MODIFIED, true);
                    }
                    DBDataset dataset = getDataset();
                    DBNConnection connection = getConnection();
                    connection.notifyDataChanges(dataset.getVirtualFile());
                });
    }

    public void insertRecord(int rowIndex) {
        ResultSetAdapter resultSetAdapter = getResultSetAdapter();
        DatasetEditorTable editorTable = getEditorTable();
        DBDataset dataset = getDataset();
        try {
            set(INSERTING, true);
            editorTable.stopCellEditing();
            resultSetAdapter.startInsertRow();
            DatasetEditorModelRow newRow = createRow(getRowCount()+1);

            newRow.reset();
            newRow.set(INSERTING, true);
            addRowAtIndex(rowIndex, newRow);
            notifyRowsInserted(rowIndex, rowIndex);

            editorTable.selectCell(rowIndex, editorTable.getSelectedColumn() == -1 ? 0 : editorTable.getSelectedColumn());

            DBNConnection connection = getConnection();
            connection.notifyDataChanges(dataset.getVirtualFile());
        } catch (SQLException e) {
            set(INSERTING, false);
            MessageUtil.showErrorDialog(getProject(), "Could not insert record for " + dataset.getQualifiedNameWithType() + ".", e);
        }
    }

    public void duplicateRecord(int rowIndex) {
        ResultSetAdapter resultSetAdapter = getResultSetAdapter();
        DatasetEditorTable editorTable = getEditorTable();
        DBDataset dataset = getDataset();
        try {
            set(INSERTING, true);
            editorTable.stopCellEditing();
            int insertIndex = rowIndex + 1;
            resultSetAdapter.startInsertRow();
            DatasetEditorModelRow oldRow = getRowAtIndex(rowIndex);
            DatasetEditorModelRow newRow = createRow(getRowCount() + 1);

            newRow.reset();
            newRow.set(INSERTING, true);
            newRow.updateDataFromRow(oldRow);
            addRowAtIndex(insertIndex, newRow);
            notifyRowsInserted(insertIndex, insertIndex);

            editorTable.selectCell(insertIndex, editorTable.getSelectedColumn());
            DBNConnection connection = getConnection();
            connection.notifyDataChanges(dataset.getVirtualFile());
        } catch (SQLException e) {
            set(INSERTING, false);
            MessageUtil.showErrorDialog(getProject(), "Could not duplicate record in " + dataset.getQualifiedNameWithType() + ".", e);
        }
    }

    public void postInsertRecord(boolean propagateError, boolean rebuild, boolean reset) throws SQLException {
        ResultSetAdapter resultSetAdapter = getResultSetAdapter();
        DatasetEditorTable editorTable = getEditorTable();
        DatasetEditorModelRow row = getInsertRow();
        if (row != null) {
            if (row.isEmptyData()) {
                throw AlreadyDisposedException.INSTANCE;
            }
            try {
                editorTable.stopCellEditing();
                resultSetAdapter.insertRow();

                row.reset();
                row.set(INSERTED, true);
                set(MODIFIED, true);
                set(INSERTING, false);
                if (rebuild) load(true, true);
            } catch (SQLException e) {
                DatasetEditorError error = new DatasetEditorError(getConnectionHandler(), e);
                if (reset) {
                    set(INSERTING, false);
                } else {
                    row.notifyError(error, true, true);
                }
                if (!error.isNotified() || propagateError) throw e;
            }
        }
    }

    public void cancelInsert(boolean notifyListeners) {
        ResultSetAdapter resultSetAdapter = getResultSetAdapter();
        DatasetEditorTable editorTable = getEditorTable();
        try {
            editorTable.fireEditingCancel();
            DatasetEditorModelRow insertRow = getInsertRow();
            if (insertRow != null) {
                int rowIndex = insertRow.getIndex();
                removeRowAtIndex(rowIndex);
                if (notifyListeners) notifyRowsDeleted(rowIndex, rowIndex);
            }
            resultSetAdapter.cancelInsertRow();
            set(INSERTING, false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * after delete or insert performed on a result set, the row indexes have to be shifted accordingly
     */
    private void shiftResultSetRowIndex(int fromIndex, int shifting) {
        for (DatasetEditorModelRow row : getRows()) {
            if (row.getResultSetRowIndex() > fromIndex) {
                row.shiftResultSetRowIndex(shifting);
            }
        }
    }

    @Nullable
    public DatasetEditorModelRow getInsertRow() {
        for (DatasetEditorModelRow row : getRows()) {
            if (row.is(INSERTING)) {
                return row;
            }
        }
        return null;
    }

    public int getInsertRowIndex() {
        DatasetEditorModelRow insertRow = getInsertRow();
        return insertRow == null ? -1 : insertRow.getIndex();
    }

    public void revertChanges() {
        for (DatasetEditorModelRow row : getRows()) {
            row.revertChanges();
        }
    }

    public boolean isResultSetUpdatable() {
        return isResultSetUpdatable;
    }

    /*********************************************************
     *                      DataModel                       *
     *********************************************************/
    @Override
    public DatasetEditorModelCell getCellAt(int rowIndex, int columnIndex) {
        return (DatasetEditorModelCell) super.getCellAt(rowIndex, columnIndex);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        DatasetEditorModelCell cell = getCellAt(rowIndex, columnIndex);
        if (cell != null) {
            cell.updateUserValue(value, false);
        }
    }

    public void setValueAt(Object value, String errorMessage,  int rowIndex, int columnIndex) {
        DatasetEditorModelCell cell = getCellAt(rowIndex, columnIndex);
        if (cell != null) {
            cell.updateUserValue(value, errorMessage);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        DatasetEditorTable editorTable = getEditorTable();
        DatasetEditorState editorState = getState();
        if (isReadonly() || isEnvironmentReadonly() || isDirty()) {
            return false;

        } else if (editorState.isReadonly()) {
            return false;

        } else if (editorTable.isLoading()) {
            return false;

        } else if (!editorTable.isEditingEnabled()) {
            return false;

        } else if (editorTable.getSelectedColumnCount() > 1 || editorTable.getSelectedRowCount() > 1) {
            return false;

        } else if (!getConnectionHandler().isConnected(SessionId.MAIN)) {
            return false;
        }

        DatasetEditorModelRow row = getRowAtIndex(rowIndex);
        if (row == null) {
            return false;

        } else if (row.is(DELETED)) {
            return false;

        } else if (row.is(UPDATING)) {
            return false;

        } else if (is(INSERTING)) {
            return row.is(INSERTING);
        }

        return true;
    }

    /*********************************************************
     *                ListSelectionListener                  *
     *********************************************************/
    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (is(INSERTING) && !event.getValueIsAdjusting()) {
            DatasetEditorModelRow insertRow = getInsertRow();
            if (insertRow != null) {
                int index = insertRow.getIndex();

                ListSelectionModel listSelectionModel = (ListSelectionModel) event.getSource();
                int selectionIndex = listSelectionModel.getLeadSelectionIndex();

                if (index != selectionIndex) {
                    //postInsertRecord();
                }
            }
        }
    }
}
