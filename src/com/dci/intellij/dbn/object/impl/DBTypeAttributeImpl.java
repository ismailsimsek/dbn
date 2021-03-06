package com.dci.intellij.dbn.object.impl;

import com.dci.intellij.dbn.browser.ui.HtmlToolTipBuilder;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.database.common.metadata.def.DBTypeAttributeMetadata;
import com.dci.intellij.dbn.object.DBType;
import com.dci.intellij.dbn.object.DBTypeAttribute;
import com.dci.intellij.dbn.object.common.DBObjectImpl;
import com.dci.intellij.dbn.object.common.list.DBObjectNavigationList;
import com.dci.intellij.dbn.object.common.list.DBObjectNavigationListImpl;
import com.dci.intellij.dbn.object.properties.DBDataTypePresentableProperty;
import com.dci.intellij.dbn.object.properties.PresentableProperty;
import com.dci.intellij.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DBTypeAttributeImpl extends DBObjectImpl<DBTypeAttributeMetadata> implements DBTypeAttribute {
    private DBDataType dataType;
    private short position;

    DBTypeAttributeImpl(DBType parent, DBTypeAttributeMetadata metadata) throws SQLException {
        super(parent, metadata);
    }

    @Override
    protected String initObject(DBTypeAttributeMetadata metadata) throws SQLException {
        String name = metadata.getAttributeName();
        position = metadata.getPosition();
        dataType = DBDataType.get(getConnectionHandler(), metadata.getDataType());
        return name;
    }


    @Override
    public short getPosition() {
        return position;
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.TYPE_ATTRIBUTE;
    }

    @Override
    public DBType getType() {
        return (DBType) getParentObject();        
    }

    @Override
    public DBDataType getDataType() {
        return dataType;
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ttb.append(true, "type attribute", true);
        ttb.append(false, " - ", true);
        ttb.append(false, dataType.getQualifiedName(), true);

        ttb.createEmptyRow();
        super.buildToolTip(ttb);            
    }

    @Override
    public List<PresentableProperty> getPresentableProperties() {
        List<PresentableProperty> properties = super.getPresentableProperties();
        properties.add(0, new DBDataTypePresentableProperty(dataType));
        return properties;
    }

    @Override
    public String getPresentableTextConditionalDetails() {
        return dataType.getQualifiedName();
    }

    @Override
    protected List<DBObjectNavigationList> createNavigationLists() {
        List<DBObjectNavigationList> objectNavigationLists = new ArrayList<>();

        if (dataType.isDeclared()) {
            objectNavigationLists.add(new DBObjectNavigationListImpl("Type", dataType.getDeclaredType()));
        }

        return objectNavigationLists;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof DBTypeAttribute) {
            DBTypeAttribute typeAttribute = (DBTypeAttribute) o;
            if (getType().equals(typeAttribute.getType())) {
                return position - typeAttribute.getPosition();
            }
        }
        return super.compareTo(o);
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/

    @Override
    public boolean isLeaf() {
        return true;
    }

}
