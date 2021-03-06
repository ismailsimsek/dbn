package com.dci.intellij.dbn.editor.code.options.ui;

import com.dci.intellij.dbn.common.options.SettingsChangeNotifier;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.dci.intellij.dbn.common.util.EventUtil;
import com.dci.intellij.dbn.editor.code.options.CodeEditorGeneralSettings;
import com.dci.intellij.dbn.language.common.SpellcheckingSettingsListener;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.dci.intellij.dbn.common.ui.GUIUtil.updateBorderTitleForeground;

public class CodeEditorGeneralSettingsForm extends ConfigurationEditorForm<CodeEditorGeneralSettings> {
    private JCheckBox showObjectNavigationGutterCheckBox;
    private JCheckBox specDeclarationGutterCheckBox;
    private JPanel mainPanel;
    private JCheckBox enableSpellchecking;
    private JCheckBox enableReferenceSpellchecking;

    public CodeEditorGeneralSettingsForm(CodeEditorGeneralSettings settings) {
        super(settings);
        updateBorderTitleForeground(mainPanel);
        resetFormChanges();
        registerComponent(mainPanel);
    }

    @NotNull
    @Override
    public JPanel ensureComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        CodeEditorGeneralSettings configuration = getConfiguration();
        configuration.setShowObjectsNavigationGutter(showObjectNavigationGutterCheckBox.isSelected());
        configuration.setShowSpecDeclarationNavigationGutter(specDeclarationGutterCheckBox.isSelected());

        boolean enableSpellchecking = this.enableSpellchecking.isSelected();
        boolean enableReferenceSpellchecking = this.enableReferenceSpellchecking.isSelected();
        boolean spellcheckingSettingsChanged =
                configuration.isEnableSpellchecking() != enableSpellchecking ||
                configuration.isEnableReferenceSpellchecking() != enableReferenceSpellchecking;

        configuration.setEnableSpellchecking(enableSpellchecking);
        configuration.setEnableReferenceSpellchecking(enableReferenceSpellchecking);

        Project project = configuration.getProject();
        if (spellcheckingSettingsChanged) {
            SettingsChangeNotifier.register(
                    () -> EventUtil.notify(project,
                            SpellcheckingSettingsListener.TOPIC,
                            (listener) -> listener.settingsChanged()));
        }
    }

    @Override
    public void resetFormChanges() {
        CodeEditorGeneralSettings settings = getConfiguration();
        showObjectNavigationGutterCheckBox.setSelected(settings.isShowObjectsNavigationGutter());
        specDeclarationGutterCheckBox.setSelected(settings.isShowSpecDeclarationNavigationGutter());
        enableSpellchecking.setSelected(settings.isEnableSpellchecking());
        enableReferenceSpellchecking.setSelected(settings.isEnableReferenceSpellchecking());
    }
}
