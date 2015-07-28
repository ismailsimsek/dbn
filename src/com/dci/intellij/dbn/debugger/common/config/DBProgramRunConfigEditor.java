package com.dci.intellij.dbn.debugger.common.config;

import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;

import com.dci.intellij.dbn.common.dispose.DisposerUtil;
import com.dci.intellij.dbn.debugger.jdbc.config.ui.DBProgramRunConfigurationEditorForm;
import com.dci.intellij.dbn.execution.ExecutionInput;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;

public abstract class DBProgramRunConfigEditor<T extends DBProgramRunConfig, F extends DBProgramRunConfigurationEditorForm<T>, I extends ExecutionInput> extends SettingsEditor<T> {
    private T configuration;
    private F configurationEditorForm;

    public DBProgramRunConfigEditor(T configuration) {
        this.configuration = configuration;
    }

    public T getConfiguration() {
        return configuration;
    }

    protected abstract F createConfigurationEditorForm();

    public F getConfigurationEditorForm(boolean create) {
        if (create && (configurationEditorForm == null || configurationEditorForm.isDisposed())) {
            configurationEditorForm = createConfigurationEditorForm();
        }
        return configurationEditorForm;
    }

    @Override
    protected void disposeEditor() {
        DisposerUtil.dispose(configurationEditorForm);
        configurationEditorForm = null;
    }

    @Override
    protected void resetEditorFrom(T configuration) {
        getConfigurationEditorForm(true).readConfiguration(configuration);
    }

    @Override
    protected void applyEditorTo(T configuration) throws ConfigurationException {
        getConfigurationEditorForm(true).writeConfiguration(configuration);
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        configurationEditorForm = getConfigurationEditorForm(true);
        return configurationEditorForm.getComponent();
    }

    public abstract void setExecutionInput(I executionInput);
}