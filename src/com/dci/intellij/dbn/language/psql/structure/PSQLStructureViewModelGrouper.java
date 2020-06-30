package com.dci.intellij.dbn.language.psql.structure;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.psi.BasePsiElement;
import com.dci.intellij.dbn.language.common.psi.IdentifierPsiElement;
import com.dci.intellij.dbn.language.psql.PSQLFile;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.smartTree.ActionPresentation;
import com.intellij.ide.util.treeView.smartTree.ActionPresentationData;
import com.intellij.ide.util.treeView.smartTree.Group;
import com.intellij.ide.util.treeView.smartTree.Grouper;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class PSQLStructureViewModelGrouper implements Grouper {
    private final ActionPresentation actionPresentation = new ActionPresentationData("Group by Object Type", "", Icons.ACTION_GROUP);

    private static final Collection<Group> EMPTY_GROUPS = new ArrayList<>(0);

    @Override
    @NotNull
    public Collection<Group> group(@NotNull AbstractTreeNode<?> abstractTreeNode, @NotNull Collection<TreeElement> collection) {
        Map<DBObjectType, Group> groups = null;
        if (abstractTreeNode.getValue() instanceof PSQLStructureViewElement) {
            PSQLStructureViewElement structureViewElement = (PSQLStructureViewElement) abstractTreeNode.getValue();
            Object value = structureViewElement.getValue();
            if (value instanceof BasePsiElement || value instanceof PSQLFile) {

                for (TreeElement treeElement : collection) {
                    if (treeElement instanceof PSQLStructureViewElement) {
                        PSQLStructureViewElement element = (PSQLStructureViewElement) treeElement;
                        if (element.getValue() instanceof BasePsiElement) {
                            BasePsiElement basePsiElement = (BasePsiElement) element.getValue();
                            if (!basePsiElement.elementType.is(ElementTypeAttribute.ROOT)) {
                                BasePsiElement subjectPsiElement = basePsiElement.findFirstPsiElement(ElementTypeAttribute.SUBJECT);
                                if (subjectPsiElement instanceof IdentifierPsiElement) {
                                    IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) subjectPsiElement;
                                    DBObjectType objectType = identifierPsiElement.getObjectType();
                                    switch (objectType) {
                                        case PACKAGE_PROCEDURE: objectType = DBObjectType.PROCEDURE; break;
                                        case PACKAGE_FUNCTION: objectType = DBObjectType.FUNCTION; break;
                                        case TYPE_PROCEDURE: objectType = DBObjectType.PROCEDURE; break;
                                        case TYPE_FUNCTION: objectType = DBObjectType.FUNCTION; break;
                                    }

                                    if (groups == null) groups = new EnumMap<>(DBObjectType.class);
                                    PSQLStructureViewModelGroup group = (PSQLStructureViewModelGroup) groups.get(objectType);
                                    if (group == null) {
                                        group = new PSQLStructureViewModelGroup(objectType);
                                        groups.put(objectType, group);
                                    }
                                    group.addChild(treeElement);
                                }
                            }
                        }
                    }
                }
            }
        }

        return groups == null ? EMPTY_GROUPS : groups.values();
    }

    @Override
    @NotNull
    public ActionPresentation getPresentation() {
        return actionPresentation;
    }

    @Override
    @NotNull
    public String getName() {
        return "Object Type";
    }
}
