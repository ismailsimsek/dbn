package com.dci.intellij.dbn.execution.statement.options;

import com.dci.intellij.dbn.common.options.Configuration;
import com.dci.intellij.dbn.common.options.setting.SettingsUtil;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.dci.intellij.dbn.common.util.EventUtil;
import com.dci.intellij.dbn.common.util.ProjectSupplier;
import com.dci.intellij.dbn.execution.ExecutionTarget;
import com.dci.intellij.dbn.execution.common.options.ExecutionEngineSettings;
import com.dci.intellij.dbn.execution.common.options.ExecutionTimeoutSettings;
import com.dci.intellij.dbn.execution.common.options.TimeoutSettingsListener;
import com.dci.intellij.dbn.execution.statement.options.ui.StatementExecutionSettingsForm;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class StatementExecutionSettings extends Configuration implements ExecutionTimeoutSettings, ProjectSupplier {
    private static final String REMEMBER_OPTION_HINT = ""/*"\n\n(you can remember your option and change it at any time in Settings > Execution Engine > Statement Execution)"*/;

    private ExecutionEngineSettings parent;
    private int resultSetFetchBlockSize = 100;
    private int executionTimeout = 20;
    private int debugExecutionTimeout = 600;
    private boolean focusResult = false;
    private boolean promptExecution = false;

    public StatementExecutionSettings(ExecutionEngineSettings parent) {
        this.parent = parent;
    }

    @Override
    public String getDisplayName() {
        return "Statement execution settings";
    }

    @Override
    public String getHelpTopic() {
        return "executionEngine";
    }

    @NotNull
    @Override
    public Project getProject() {
        return parent.getProject();
    }

    /*********************************************************
    *                       Settings                        *
    *********************************************************/

    public int getResultSetFetchBlockSize() {
        return resultSetFetchBlockSize;
    }

    public void setResultSetFetchBlockSize(int resultSetFetchBlockSize) {
        this.resultSetFetchBlockSize = resultSetFetchBlockSize;
    }

    @Override
    public int getExecutionTimeout() {
        return executionTimeout;
    }

    @Override
    public int getDebugExecutionTimeout() {
        return debugExecutionTimeout;
    }

    @Override
    public boolean setExecutionTimeout(int executionTimeout) {
        if (this.executionTimeout != executionTimeout) {
            this.executionTimeout = executionTimeout;
            return true;
        }
        return false;
    }

    @Override
    public boolean setDebugExecutionTimeout(int debugExecutionTimeout) {
        if (this.debugExecutionTimeout != debugExecutionTimeout) {
            this.debugExecutionTimeout = debugExecutionTimeout;
            return true;
        }
        return false;
    }

    void notifyTimeoutChanges() {
        EventUtil.notify(parent.getProject(), TimeoutSettingsListener.TOPIC).settingsChanged(ExecutionTarget.STATEMENT);
    }

    public void setFocusResult(boolean focusResult) {
        this.focusResult = focusResult;
    }

    public boolean isFocusResult() {
        return focusResult;
    }

    public boolean isPromptExecution() {
        return promptExecution;
    }

    public void setPromptExecution(boolean promptExecution) {
        this.promptExecution = promptExecution;
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
    @Override
    @NotNull
    public ConfigurationEditorForm createConfigurationEditor() {
        return new StatementExecutionSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "statement-execution";
    }

    @Override
    public void readConfiguration(Element element) {
        resultSetFetchBlockSize = SettingsUtil.getInteger(element, "fetch-block-size", resultSetFetchBlockSize);
        executionTimeout = SettingsUtil.getInteger(element, "execution-timeout", executionTimeout);
        debugExecutionTimeout = SettingsUtil.getInteger(element, "debug-execution-timeout", debugExecutionTimeout);
        focusResult = SettingsUtil.getBoolean(element, "focus-result", focusResult);
        promptExecution = SettingsUtil.getBoolean(element, "prompt-execution", promptExecution);
    }

    @Override
    public void writeConfiguration(Element element) {
        SettingsUtil.setInteger(element, "fetch-block-size", resultSetFetchBlockSize);
        SettingsUtil.setInteger(element, "execution-timeout", executionTimeout);
        SettingsUtil.setInteger(element, "debug-execution-timeout", debugExecutionTimeout);
        SettingsUtil.setBoolean(element, "focus-result", focusResult);
        SettingsUtil.setBoolean(element, "prompt-execution", promptExecution);
    }
}
