package com.dci.intellij.dbn.navigation.options.ui;

import com.dci.intellij.dbn.common.options.ui.CompositeConfigurationEditorForm;
import com.dci.intellij.dbn.navigation.options.NavigationSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class NavigationSettingsForm extends CompositeConfigurationEditorForm<NavigationSettings> {
    private JPanel mainPanel;
    private JPanel objectsLookupSettingsPanel;

    public NavigationSettingsForm(NavigationSettings settings) {
        super(settings);
        objectsLookupSettingsPanel.add(settings.getObjectsLookupSettings().createComponent(), BorderLayout.CENTER);
    }

    @NotNull
    @Override
    public JPanel ensureComponent() {
        return mainPanel;
    }
}
