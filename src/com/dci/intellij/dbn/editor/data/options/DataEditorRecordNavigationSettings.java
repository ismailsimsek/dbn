package com.dci.intellij.dbn.editor.data.options;

import com.dci.intellij.dbn.common.options.BasicConfiguration;
import com.dci.intellij.dbn.common.options.setting.SettingsSupport;
import com.dci.intellij.dbn.data.record.navigation.RecordNavigationTarget;
import com.dci.intellij.dbn.editor.data.options.ui.DataEditorRecordNavigationSettingsForm;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class DataEditorRecordNavigationSettings extends BasicConfiguration<DataEditorSettings, DataEditorRecordNavigationSettingsForm> {
    private RecordNavigationTarget navigationTarget = RecordNavigationTarget.VIEWER;

    public DataEditorRecordNavigationSettings(DataEditorSettings parent) {
        super(parent);
    }

    @NotNull
    @Override
    public DataEditorRecordNavigationSettingsForm createConfigurationEditor() {
        return new DataEditorRecordNavigationSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "record-navigation";
    }

    public RecordNavigationTarget getNavigationTarget() {
        return navigationTarget;
    }

    public void setNavigationTarget(RecordNavigationTarget navigationTarget) {
        this.navigationTarget = navigationTarget;
    }

    @Override
    public void readConfiguration(Element element) {
        navigationTarget = SettingsSupport.getEnum(element, "navigation-target", RecordNavigationTarget.VIEWER);
        if (navigationTarget == RecordNavigationTarget.PROMPT) {
            navigationTarget = RecordNavigationTarget.ASK;
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        SettingsSupport.setEnum(element, "navigation-target", navigationTarget);
    }
}
