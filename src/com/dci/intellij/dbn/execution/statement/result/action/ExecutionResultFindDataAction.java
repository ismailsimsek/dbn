package com.dci.intellij.dbn.execution.statement.result.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.dci.intellij.dbn.execution.statement.result.ui.StatementExecutionResultForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ExecutionResultFindDataAction extends AbstractExecutionResultAction {
    public ExecutionResultFindDataAction() {
        super("Find data", Icons.ACTION_FIND);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        StatementExecutionCursorResult executionResult = getExecutionResult(e);
        if (Failsafe.check(executionResult)) {
            StatementExecutionResultForm resultForm = executionResult.getForm();
            if (Failsafe.check(resultForm)) {
                resultForm.showSearchHeader();
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        getTemplatePresentation().setText("Find Data");
    }
}
