package com.dci.intellij.dbn.execution.method.ui;

import com.dci.intellij.dbn.common.dispose.Disposer;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.data.editor.text.TextContentType;
import com.dci.intellij.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dci.intellij.dbn.data.editor.ui.TextFieldWithPopup;
import com.dci.intellij.dbn.data.editor.ui.TextFieldWithTextEditor;
import com.dci.intellij.dbn.data.editor.ui.UserValueHolderImpl;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.data.type.DBNativeDataType;
import com.dci.intellij.dbn.data.type.DataTypeDefinition;
import com.dci.intellij.dbn.data.type.GenericDataType;
import com.dci.intellij.dbn.execution.method.MethodExecutionArgumentValue;
import com.dci.intellij.dbn.execution.method.MethodExecutionInput;
import com.dci.intellij.dbn.execution.method.MethodExecutionManager;
import com.dci.intellij.dbn.object.DBArgument;
import com.dci.intellij.dbn.object.DBType;
import com.dci.intellij.dbn.object.DBTypeAttribute;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodExecutionInputArgumentForm extends DBNFormImpl<MethodExecutionInputForm> {
    private JPanel mainPanel;
    private JLabel argumentLabel;
    private JLabel argumentTypeLabel;
    private JPanel typeAttributesPanel;
    private JPanel inputFieldPanel;

    private JTextField inputTextField;
    private UserValueHolderImpl<String> userValueHolder;

    private DBObjectRef<DBArgument> argumentRef;
    private List<MethodExecutionInputTypeAttributeForm> typeAttributeForms = new ArrayList<>();

    MethodExecutionInputArgumentForm(MethodExecutionInputForm parentForm, final DBArgument argument) {
        super(parentForm);
        this.argumentRef = DBObjectRef.from(argument);
        String argumentName = argument.getName();
        argumentLabel.setText(argumentName);
        argumentLabel.setIcon(argument.getIcon());

        DBDataType dataType = argument.getDataType();

        argumentTypeLabel.setForeground(UIUtil.getInactiveTextColor());

        DBType declaredType = dataType.getDeclaredType();

        if (dataType.isNative()) {
            argumentTypeLabel.setText(dataType.getQualifiedName());
            typeAttributesPanel.setVisible(false);
        } else if (declaredType != null) {
            typeAttributesPanel.setLayout(new BoxLayout(typeAttributesPanel, BoxLayout.Y_AXIS));
            List<DBTypeAttribute> attributes = declaredType.getAttributes();
            for (DBTypeAttribute attribute : attributes) {
                addAttributePanel(attribute);
            }
        }

        if (declaredType != null) {
            argumentTypeLabel.setIcon(declaredType.getIcon());
            argumentTypeLabel.setText(declaredType.getName());
        }

        if (argument.isInput() && dataType.isNative()) {
            DBNativeDataType nativeDataType = dataType.getNativeDataType();
            DataTypeDefinition dataTypeDefinition = nativeDataType.getDataTypeDefinition();
            GenericDataType genericDataType = dataTypeDefinition.getGenericDataType();

            Project project = argument.getProject();
            MethodExecutionInput executionInput = parentForm.getExecutionInput();
            String value = executionInput.getInputValue(argument);

            if (genericDataType.is(GenericDataType.XMLTYPE, GenericDataType.CLOB)) {
                TextFieldWithTextEditor inputField = new TextFieldWithTextEditor(project, "[" + genericDataType.name() + "]");

                TextContentType contentType =
                        genericDataType == GenericDataType.XMLTYPE ?
                                TextContentType.get(project, "XML") :
                                TextContentType.getPlainText(project);
                if (contentType == null) {
                    contentType = TextContentType.getPlainText(project);
                }

                userValueHolder = new UserValueHolderImpl<>(argumentName, DBObjectType.ARGUMENT, dataType, project);
                userValueHolder.setUserValue(value);
                userValueHolder.setContentType(contentType);
                inputField.setUserValueHolder(userValueHolder);

                inputField.setPreferredSize(new Dimension(240, -1));
                inputTextField = inputField.getTextField();
                inputFieldPanel.add(inputField, BorderLayout.CENTER);
            } else {
                TextFieldWithPopup inputField = new TextFieldWithPopup(project);
                inputField.setPreferredSize(new Dimension(240, -1));
                if (genericDataType == GenericDataType.DATE_TIME) {
                    inputField.createCalendarPopup(false);
                }

                inputField.createValuesListPopup(createValuesProvider(), true);
                inputTextField = inputField.getTextField();
                inputTextField.setText(value);
                inputFieldPanel.add(inputField, BorderLayout.CENTER);
            }

            inputTextField.setDisabledTextColor(inputTextField.getForeground());
        } else {
            inputFieldPanel.setVisible(false);
        }
    }

    @NotNull
    private ListPopupValuesProvider createValuesProvider() {
        return new ListPopupValuesProvider() {
            @Override
            public String getDescription() {
                return "History Values List";
            }

            @Override
            public List<String> getValues() {
                DBArgument argument = getArgument();
                if (argument != null) {
                    MethodExecutionInput executionInput = ensureParentComponent().getExecutionInput();
                    return executionInput.getInputValueHistory(argument, null);
                }

                return Collections.emptyList();
            }

            @Override
            public List<String> getSecondaryValues() {
                DBArgument argument = getArgument();
                if (argument != null) {
                    ConnectionHandler connectionHandler = argument.getConnectionHandler();
                    MethodExecutionManager executionManager = MethodExecutionManager.getInstance(argument.getProject());
                    MethodExecutionArgumentValue argumentValue = executionManager.getArgumentValuesCache().getArgumentValue(connectionHandler.getConnectionId(), argument.getName(), false);
                    if (argumentValue != null) {
                        List<String> cachedValues = new ArrayList<>(argumentValue.getValueHistory());
                        cachedValues.removeAll(getValues());
                        return cachedValues;
                    }
                }
                return Collections.emptyList();
            }

            @Override
            public boolean isLongLoading() {
                return false;
            }
        };
    }

    private void addAttributePanel(DBTypeAttribute attribute) {
        MethodExecutionInputTypeAttributeForm argumentComponent = new MethodExecutionInputTypeAttributeForm(this, getArgument(), attribute);
        typeAttributesPanel.add(argumentComponent.getComponent());
        typeAttributeForms.add(argumentComponent);
    }

    public DBArgument getArgument() {
        return DBObjectRef.get(argumentRef);
    }

    @NotNull
    @Override
    public JPanel ensureComponent() {
        return mainPanel;
    }

    public void updateExecutionInput() {
        DBArgument argument = getArgument();
        if (argument != null) {
            MethodExecutionInput executionInput = ensureParentComponent().getExecutionInput();
            if (typeAttributeForms.size() >0 ) {
                for (MethodExecutionInputTypeAttributeForm typeAttributeComponent : typeAttributeForms) {
                    typeAttributeComponent.updateExecutionInput();
                }
            } else if (userValueHolder != null ) {
                String value = userValueHolder.getUserValue();
                executionInput.setInputValue(argument, value);
            } else {
                String value = CommonUtil.nullIfEmpty(inputTextField == null ? null : inputTextField.getText());
                executionInput.setInputValue(argument, value);
            }
        }
    }

    protected int[] getMetrics(int[] metrics) {
        if (typeAttributeForms.size() > 0) {
            for (MethodExecutionInputTypeAttributeForm typeAttributeComponent : typeAttributeForms) {
                metrics = typeAttributeComponent.getMetrics(metrics);
            }
        }

        return new int[] {
            (int) Math.max(metrics[0], argumentLabel.getPreferredSize().getWidth()),
            (int) Math.max(metrics[1], inputFieldPanel.getPreferredSize().getWidth()),
            (int) Math.max(metrics[2], argumentTypeLabel.getPreferredSize().getWidth())};
    }

    protected void adjustMetrics(int[] metrics) {
        if (typeAttributeForms.size() > 0) {
            for (MethodExecutionInputTypeAttributeForm typeAttributeComponent : typeAttributeForms) {
                typeAttributeComponent.adjustMetrics(metrics);
            }
        }
        argumentLabel.setPreferredSize(new Dimension(metrics[0], argumentLabel.getHeight()));
        inputFieldPanel.setPreferredSize(new Dimension(metrics[1], inputFieldPanel.getHeight()));
        argumentTypeLabel.setPreferredSize(new Dimension(metrics[2], argumentTypeLabel.getHeight()));
    }

    public void addDocumentListener(DocumentListener documentListener){
        if (inputTextField != null) {
            inputTextField.getDocument().addDocumentListener(documentListener);
        }

        for (MethodExecutionInputTypeAttributeForm typeAttributeComponent : typeAttributeForms){
            typeAttributeComponent.addDocumentListener(documentListener);
        }
    }

    @Override
    public void disposeInner() {
        Disposer.dispose(typeAttributeForms);
        super.disposeInner();
    }

    public int getScrollUnitIncrement() {
        return (int) (typeAttributeForms.size() > 0 ?
                typeAttributeForms.get(0).getComponent().getPreferredSize().getHeight() :
                mainPanel.getPreferredSize().getHeight());
    }
}
