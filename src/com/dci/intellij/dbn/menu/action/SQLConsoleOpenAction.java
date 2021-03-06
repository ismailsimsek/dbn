package com.dci.intellij.dbn.menu.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.DumbAwareProjectAction;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.util.MessageUtil;
import com.dci.intellij.dbn.connection.ConnectionBundle;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionHandlerRef;
import com.dci.intellij.dbn.connection.ConnectionManager;
import com.dci.intellij.dbn.connection.console.DatabaseConsoleManager;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.object.DBConsole;
import com.dci.intellij.dbn.options.ConfigId;
import com.dci.intellij.dbn.options.ProjectSettingsManager;
import com.dci.intellij.dbn.vfs.DBConsoleType;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.dci.intellij.dbn.common.message.MessageCallback.conditional;

public class SQLConsoleOpenAction extends DumbAwareProjectAction {
    public SQLConsoleOpenAction() {
        super("Open SQL console...", null, Icons.FILE_SQL_CONSOLE);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        //FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.file");
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();


        ConnectionHandler singleConnectionHandler = null;
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        if (connectionBundle.getConnectionHandlers().size() > 0) {
            actionGroup.addSeparator();
            for (ConnectionHandler connectionHandler : connectionBundle.getConnectionHandlers()) {
                SelectConnectionAction connectionAction = new SelectConnectionAction(connectionHandler);
                actionGroup.add(connectionAction);
                singleConnectionHandler = connectionHandler;
            }
        }

        if (actionGroup.getChildrenCount() > 1) {
            ListPopup popupBuilder = JBPopupFactory.getInstance().createActionGroupPopup(
                    "Select Console Connection",
                    actionGroup,
                    e.getDataContext(),
                    //JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                    true,
                    true,
                    true,
                    null,
                    actionGroup.getChildrenCount(),
                    preselect -> {
/*
                        SelectConsoleAction selectConnectionAction = (SelectConsoleAction) action;
                        return latestSelection == selectConnectionAction.connectionHandler;
*/
                        return true;
                    });

            popupBuilder.showCenteredInCurrentWindow(project);
        } else {
            if (singleConnectionHandler != null) {
                openSQLConsole(singleConnectionHandler);
            } else {
                MessageUtil.showInfoDialog(
                        project, "No connections available.", "No database connections found. Please setup a connection first",
                        new String[]{"Setup Connection", "Cancel"}, 0,
                        (option) -> conditional(option == 0,
                                () -> {
                                    ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(project);
                                    settingsManager.openProjectSettings(ConfigId.CONNECTIONS);
                                }));
            }

        }

    }

    private class SelectConnectionAction extends ActionGroup {
        private ConnectionHandlerRef connectionHandlerRef;

        private SelectConnectionAction(ConnectionHandler connectionHandler) {
            super(connectionHandler.getName(), null, connectionHandler.getIcon());
            this.connectionHandlerRef = ConnectionHandlerRef.from(connectionHandler);
            setPopup(true);
        }
/*
        @Override
        public void actionPerformed(AnActionEvent e) {
            openSQLConsole(connectionHandler);
            latestSelection = connectionHandler;
        }*/

        @NotNull
        @Override
        public AnAction[] getChildren(AnActionEvent e) {
            ConnectionHandler connectionHandler = connectionHandlerRef.ensure();
            List<AnAction> actions = new ArrayList<>();
            Collection<DBConsole> consoles = connectionHandler.getConsoleBundle().getConsoles();
            for (DBConsole console : consoles) {
                actions.add(new SelectConsoleAction(console));
            }
            actions.add(Separator.getInstance());
            actions.add(new SelectConsoleAction(connectionHandler, DBConsoleType.STANDARD));
            if (DatabaseFeature.DEBUGGING.isSupported(connectionHandler)) {
                actions.add(new SelectConsoleAction(connectionHandler, DBConsoleType.DEBUG));
            }

            return actions.toArray(new AnAction[0]);
        }
    }

    private static class SelectConsoleAction extends AnAction{
        private ConnectionHandlerRef connectionHandlerRef;
        private DBConsole console;
        private DBConsoleType consoleType;


        SelectConsoleAction(ConnectionHandler connectionHandler, DBConsoleType consoleType) {
            super("New " + consoleType.getName() + "...");
            this.connectionHandlerRef = ConnectionHandlerRef.from(connectionHandler);
            this.consoleType = consoleType;
        }

        SelectConsoleAction(DBConsole console) {
            super(console.getName().replaceAll("_", "__"), null, console.getIcon());
            this.console = console;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (console == null) {
                ConnectionHandler connectionHandler = connectionHandlerRef.ensure();
                DatabaseConsoleManager databaseConsoleManager = DatabaseConsoleManager.getInstance(connectionHandler.getProject());
                databaseConsoleManager.showCreateConsoleDialog(connectionHandler, consoleType);
            } else {
                ConnectionHandler connectionHandler = Failsafe.nn(console.getConnectionHandler());
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(connectionHandler.getProject());
                fileEditorManager.openFile(console.getVirtualFile(), true);
            }
        }
    }

    private static void openSQLConsole(ConnectionHandler connectionHandler) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(connectionHandler.getProject());
        DBConsole defaultConsole = connectionHandler.getConsoleBundle().getDefaultConsole();
        fileEditorManager.openFile(defaultConsole.getVirtualFile(), true);
    }
}
