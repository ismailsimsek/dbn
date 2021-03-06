package com.dci.intellij.dbn.editor.data.state.column;

import com.dci.intellij.dbn.common.options.setting.SettingsSupport;
import com.dci.intellij.dbn.common.state.PersistentStateElement;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.object.DBColumn;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class DatasetColumnState implements Comparable<DatasetColumnState>, PersistentStateElement {
    private String name;
    private short position = -1;
    private boolean visible = true;

    private DatasetColumnState(DatasetColumnState columnState) {
        name = columnState.name;
        position = columnState.position;
        visible = columnState.visible;
    }
    public DatasetColumnState(DBColumn column) {
        init(column);
    }

    public void init(DBColumn column) {
        if (StringUtil.isEmpty(name)) {
            // not initialized yet
            name = column.getName();
            position = (short) (column.getPosition() -1);
            visible = true;
        }
    }

    public DatasetColumnState(Element element) {
        readState(element);
    }

    @Override
    public void readState(Element element) {
        name = element.getAttributeValue("name");
        position = SettingsSupport.getShortAttribute(element, "position", (short) -1);
        visible = SettingsSupport.getBooleanAttribute(element, "visible", true);
    }

    @Override
    public void writeState(Element element) {
        element.setAttribute("name", name);
        SettingsSupport.setIntegerAttribute(element, "position", position);
        SettingsSupport.setBooleanAttribute(element, "visible", visible);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(short position) {
        this.position = position;
    }

    public String getName() {
        return name;
    }


    @Override
    public int compareTo(@NotNull DatasetColumnState remote) {
        return position-remote.position;
    }

    /*****************************************************************
     *                     equals / hashCode                         *
     *****************************************************************/
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DatasetColumnState that = (DatasetColumnState) o;

        if (position != that.position) return false;
        if (visible != that.visible) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + position;
        result = 31 * result + (visible ? 1 : 0);
        return result;
    }

    @Override
    protected DatasetColumnState clone() {
        return new DatasetColumnState(this);
    }

    @Override
    public String toString() {
        return name + ' ' + position + (visible ? " visible" : " hidden");
    }
}
