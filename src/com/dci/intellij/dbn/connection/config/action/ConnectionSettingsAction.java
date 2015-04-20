package com.dci.intellij.dbn.connection.config.action;

import javax.swing.Icon;
import org.jetbrains.annotations.Nullable;

import com.dci.intellij.dbn.common.action.DBNDataKeys;
import com.dci.intellij.dbn.connection.config.ui.ConnectionBundleSettingsForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;

public abstract class ConnectionSettingsAction extends DumbAwareAction{
    public ConnectionSettingsAction(String text, Icon icon) {
        super(text, null, icon);
    }

    @Nullable
    ConnectionBundleSettingsForm getSettingsForm(AnActionEvent e) {
        return e.getData((DBNDataKeys.CONNECTION_BUNDLE_SETTINGS));
    }
}