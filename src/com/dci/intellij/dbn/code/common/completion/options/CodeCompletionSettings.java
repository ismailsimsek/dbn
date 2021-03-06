package com.dci.intellij.dbn.code.common.completion.options;

import com.dci.intellij.dbn.code.common.completion.options.filter.CodeCompletionFiltersSettings;
import com.dci.intellij.dbn.code.common.completion.options.general.CodeCompletionFormatSettings;
import com.dci.intellij.dbn.code.common.completion.options.sorting.CodeCompletionSortingSettings;
import com.dci.intellij.dbn.code.common.completion.options.ui.CodeCompletionSettingsForm;
import com.dci.intellij.dbn.common.options.CompositeProjectConfiguration;
import com.dci.intellij.dbn.common.options.Configuration;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.options.ConfigId;
import com.dci.intellij.dbn.options.ProjectSettings;
import com.dci.intellij.dbn.options.ProjectSettingsManager;
import com.dci.intellij.dbn.options.TopLevelConfig;
import com.intellij.openapi.project.Project;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class CodeCompletionSettings extends CompositeProjectConfiguration<ProjectSettings, CodeCompletionSettingsForm> implements TopLevelConfig {
    private CodeCompletionFiltersSettings filterSettings  = new CodeCompletionFiltersSettings(this);
    private CodeCompletionSortingSettings sortingSettings = new CodeCompletionSortingSettings(this);
    private CodeCompletionFormatSettings formatSettings   = new CodeCompletionFormatSettings(this);

    public CodeCompletionSettings(ProjectSettings parent) {
        super(parent);
        loadDefaults();
    }

    public static CodeCompletionSettings getInstance(@NotNull Project project) {
        return ProjectSettingsManager.getSettings(project).getCodeCompletionSettings();
    }

    @NotNull
    @Override
    public String getId() {
        return "DBNavigator.Project.CodeCompletionSettings";
    }


    @Override
    public String getDisplayName() {
        return "Code Completion";
    }

    @Override
    public String getHelpTopic() {
        return "codeEditor";
    }

    @Override
    public ConfigId getConfigId() {
        return ConfigId.CODE_COMPLETION;
    }

    private void loadDefaults() {
       try {
           Document document = CommonUtil.loadXmlFile(getClass(), "default-settings.xml");
           Element root = document.getRootElement();
           readConfiguration(root);
       } catch (Exception e) {
           e.printStackTrace();
       }
   }

   /*********************************************************
    *                         Custom                        *
    *********************************************************/
    public CodeCompletionFiltersSettings getFilterSettings() {
        return filterSettings;
    }

    public CodeCompletionSortingSettings getSortingSettings() {
        return sortingSettings;
    }

    public CodeCompletionFormatSettings getFormatSettings() {
        return formatSettings;
    }

    /*********************************************************
    *                     Configuration                      *
    *********************************************************/

    @Override
    @NotNull
    public CodeCompletionSettingsForm createConfigurationEditor() {
        return new CodeCompletionSettingsForm(this);
    }

    @NotNull
    @Override
    public CodeCompletionSettings getOriginalSettings() {
        return CodeCompletionSettings.getInstance(getProject());
    }

    @Override
    public String getConfigElementName() {
        return "code-completion-settings";
    }

    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[] {
                filterSettings,
                sortingSettings,
                formatSettings};
    }
}
