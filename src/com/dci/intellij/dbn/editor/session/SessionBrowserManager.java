package com.dci.intellij.dbn.editor.session;

import java.sql.SQLException;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.dci.intellij.dbn.common.AbstractProjectComponent;
import com.dci.intellij.dbn.common.util.MessageUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.vfs.DBSessionBrowserVirtualFile;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.components.StorageScheme;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;

@State(
    name = "DBNavigator.Project.SessionEditorManager",
    storages = {
        @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/dbnavigator.xml", scheme = StorageScheme.DIRECTORY_BASED),
        @Storage(file = StoragePathMacros.PROJECT_FILE)}
)
public class SessionBrowserManager extends AbstractProjectComponent implements PersistentStateComponent<Element> {

    private SessionBrowserManager(Project project) {
        super(project);
    }

    public static SessionBrowserManager getInstance(Project project) {
        return project.getComponent(SessionBrowserManager.class);
    }

    public void openSessionBrowser(ConnectionHandler connectionHandler) {
        Project project = connectionHandler.getProject();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        DBSessionBrowserVirtualFile sessionBrowserFile = connectionHandler.getSessionBrowserFile();
        try {
            sessionBrowserFile.read();
            fileEditorManager.openFile(sessionBrowserFile, true);
        } catch (SQLException e) {
            MessageUtil.showErrorDialog(project, "Session Browser could not open for connection \"" + connectionHandler.getName() + "\".", e);
        }
    }


    /****************************************
    *             ProjectComponent          *
    *****************************************/
    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.Project.SessionEditorManager";
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }


    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getState() {
        Element element = new Element("state");
        return element;
    }

    @Override
    public void loadState(Element element) {
    }

}