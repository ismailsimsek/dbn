package com.dci.intellij.dbn.connection.transaction.ui;

import com.dci.intellij.dbn.common.dispose.Disposer;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.ui.Borders;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.ui.GUIUtil;
import com.dci.intellij.dbn.common.util.EventUtil;
import com.dci.intellij.dbn.connection.ConnectionBundle;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionManager;
import com.dci.intellij.dbn.connection.ConnectionType;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.transaction.PendingTransactionBundle;
import com.dci.intellij.dbn.connection.transaction.TransactionAction;
import com.dci.intellij.dbn.connection.transaction.TransactionListener;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingTransactionsForm extends DBNFormImpl<PendingTransactionsDialog> {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel detailsPanel;
    private JList connectionsList;
    private List<ConnectionHandler> connectionHandlers = new ArrayList<ConnectionHandler>();

    private Map<ConnectionHandler, PendingTransactionsDetailForm> uncommittedChangeForms = new HashMap<ConnectionHandler, PendingTransactionsDetailForm>();

    PendingTransactionsForm(PendingTransactionsDialog parentComponent) {
        super(parentComponent);
        GuiUtils.replaceJSplitPaneWithIDEASplitter(mainPanel);
        mainPanel.setBorder(Borders.BOTTOM_LINE_BORDER);

        connectionsList.addListSelectionListener(e -> {
            ConnectionHandler connectionHandler = (ConnectionHandler) connectionsList.getSelectedValue();
            showChangesForm(connectionHandler);
        });

        connectionsList.setCellRenderer(new ListCellRenderer());
        connectionsList.setSelectedIndex(0);
        updateListModel();

        Project project = getProject();
        EventUtil.subscribe(project, this, TransactionListener.TOPIC, transactionListener);
    }

    private void updateListModel() {
        checkDisposed();
        DefaultListModel model = new DefaultListModel();
        ConnectionManager connectionManager = ConnectionManager.getInstance(getProject());
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        for (ConnectionHandler connectionHandler : connectionBundle.getConnectionHandlers()) {
            if (connectionHandler.hasUncommittedChanges()) {
                connectionHandlers.add(connectionHandler);
                model.addElement(connectionHandler);
            }
        }
        connectionsList.setModel(model);
        if (model.size() > 0) {
            connectionsList.setSelectedIndex(0);
        }
    }

    boolean hasUncommittedChanges() {
        for (ConnectionHandler connectionHandler : connectionHandlers) {
            if (connectionHandler.hasUncommittedChanges()) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public JPanel ensureComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        Disposer.dispose(uncommittedChangeForms);
        super.disposeInner();
    }

    public List<ConnectionHandler> getConnectionHandlers (){
        return connectionHandlers;
    }

    public void showChangesForm(ConnectionHandler connectionHandler) {
        detailsPanel.removeAll();
        if (connectionHandler != null) {
            PendingTransactionsDetailForm pendingTransactionsForm = uncommittedChangeForms.get(connectionHandler);
            if (pendingTransactionsForm == null) {
                pendingTransactionsForm = new PendingTransactionsDetailForm(connectionHandler, null, true);
                uncommittedChangeForms.put(connectionHandler, pendingTransactionsForm);
            }
            detailsPanel.add(pendingTransactionsForm.getComponent(), BorderLayout.CENTER);
        }

        GUIUtil.repaint(detailsPanel);
    }

    private static class ListCellRenderer extends ColoredListCellRenderer {

        @Override
        protected void customizeCellRenderer(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
            ConnectionHandler connectionHandler = (ConnectionHandler) value;
            setIcon(connectionHandler.getIcon());
            append(connectionHandler.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            List<DBNConnection> connections = connectionHandler.getConnections(ConnectionType.MAIN, ConnectionType.SESSION);
            int changes = 0;
            for (DBNConnection connection : connections) {
                PendingTransactionBundle dataChanges = connection.getDataChanges();
                changes += dataChanges == null ? 0 : dataChanges.size();
            }

            append(" (" + changes + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
    }

    /********************************************************
     *                Transaction Listener                  *
     ********************************************************/
    private TransactionListener transactionListener = new TransactionListener() {
        @Override
        public void afterAction(@NotNull ConnectionHandler connectionHandler, DBNConnection connection, TransactionAction action, boolean succeeded) {
            refreshForm();
        }
    };

    private void refreshForm() {
        Dispatch.run(() -> updateListModel());
    }
}
