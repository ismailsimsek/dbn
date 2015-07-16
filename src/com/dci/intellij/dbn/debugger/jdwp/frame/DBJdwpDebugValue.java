package com.dci.intellij.dbn.debugger.jdwp.frame;

import javax.swing.Icon;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.thread.SimpleBackgroundTask;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.debugger.jdwp.DBJdwpDebugProcess;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XNamedValue;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueModifier;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;

public class DBJdwpDebugValue extends XNamedValue implements Comparable<DBJdwpDebugValue>{
    private DBJdwpDebugValueModifier modifier;
    private DBJdwpDebugProcess debugProcess;
    private String value;
    private String errorMessage;
    private Icon icon;
    private int frameIndex;
    private DBJdwpDebugValue parentValue;
    private Set<String> childVariableNames;

    public DBJdwpDebugValue(DBJdwpDebugProcess debugProcess, DBJdwpDebugValue parentValue, String variableName, @Nullable Set<String> childVariableNames, Icon icon, int frameIndex) {
        super(variableName);
        this.debugProcess = debugProcess;
        if (icon == null) {
            icon = parentValue == null ?
                    Icons.DBO_VARIABLE :
                    Icons.DBO_ATTRIBUTE;
        }
        this.icon = icon;
        this.parentValue = parentValue;

        this.frameIndex = frameIndex;
        this.childVariableNames = childVariableNames;
    }

    public DBJdwpDebugProcess getDebugProcess() {
        return debugProcess;
    }

    public String getVariableName() {
        return getName();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void computePresentation(@NotNull final XValueNode node, @NotNull XValuePlace place) {
        //node.setPresentation(icon, null, "", childVariableNames != null);
        new SimpleBackgroundTask("load variable value") {
            @Override
            protected void execute() {
                try {
                    String variableName = getVariableName();
                    String databaseVariableName = parentValue == null ? variableName : parentValue.getVariableName() + "." + variableName;
/*
                    VariableInfo variableInfo = debugProcess.getDebuggerInterface().getVariableInfo(
                            databaseVariableName.toUpperCase(), frameIndex,
                            debugProcess.getDebugConnection());
                    value = variableInfo.getValue();
                    errorMessage = variableInfo.getError();
*/
                    if (childVariableNames != null) {
                        errorMessage = null;
                    }

                    if (value == null) {
                        value = childVariableNames != null ? "" : "null";
                    } else {
                        if (!StringUtil.isNumber(value)) {
                            value = '\'' + value + '\'';
                        }
                    }

                    if (errorMessage != null) {
                        errorMessage = errorMessage.toLowerCase();
                        value = "";
                    }
                    if (childVariableNames != null) {
                        errorMessage = "record";
                    }
                } catch (Exception e) {
                    value = "";
                    errorMessage = e.getMessage();
                } finally {
                    node.setPresentation(icon, errorMessage, CommonUtil.nvl(value, "null"), childVariableNames != null);
                }

            }
        }.start();
    }

    @Override
    public XValueModifier getModifier() {
        if (modifier == null) modifier = new DBJdwpDebugValueModifier(this);
        return modifier;
    }

    public int compareTo(@NotNull DBJdwpDebugValue remote) {
        return getName().compareTo(remote.getName());
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        if (childVariableNames != null) {
            for (String childVariableName : childVariableNames) {
                childVariableName = childVariableName.substring(getVariableName().length() + 1);
                XValueChildrenList debugValueChildren = new XValueChildrenList();
                DBJdwpDebugValue value = new DBJdwpDebugValue(debugProcess, this, childVariableName, null, null, frameIndex);
                debugValueChildren.add(value);
                node.addChildren(debugValueChildren, true);
            }
        } else {
            super.computeChildren(node);
        }

    }

/*    private List<DBObject> getChildObjects() {
        DBObject object = DBObjectRef.get(objectRef);
        if (object instanceof DBVirtualObject) {
            DBObjectListContainer childObjectsContainer = object.getChildObjects();
            if (childObjectsContainer != null) {
                List<DBObjectList<DBObject>> objectLists = childObjectsContainer.getAllObjectLists();
                if (objectLists.size() > 0) {
                    return objectLists.get(0).getObjects();
                }
            }
        }
        return null;
    }*/
}