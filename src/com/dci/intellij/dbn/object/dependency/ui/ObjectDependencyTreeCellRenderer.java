package com.dci.intellij.dbn.object.dependency.ui;

import javax.swing.Icon;
import javax.swing.JTree;
import org.jetbrains.annotations.NotNull;

import com.dci.intellij.dbn.common.load.LoadIcon;
import com.dci.intellij.dbn.common.ui.MergedIcon;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.object.common.DBObject;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.ui.UIUtil;

public class ObjectDependencyTreeCellRenderer extends ColoredTreeCellRenderer {

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        ObjectDependencyTreeNode node = (ObjectDependencyTreeNode) value;
        DBObject object = node.getObject();

        if (object != null) {
            ObjectDependencyTreeNode selectedNode = (ObjectDependencyTreeNode) tree.getLastSelectedPathComponent();
            boolean isLoading = node.isLoading();
            boolean highlight = !isLoading && selectedNode != null && selectedNode != node && CommonUtil.safeEqual(object, selectedNode.getObject());

            SimpleTextAttributes regularAttributes = highlight ?
                    SimpleTextAttributes.REGULAR_ATTRIBUTES.derive(SimpleTextAttributes.STYLE_PLAIN, null, new JBColor(0xCCCCFF, 0x155221), null) :
                    isLoading ? SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES;
            SimpleTextAttributes grayAttributes = highlight ?
                    SimpleTextAttributes.GRAY_ATTRIBUTES.derive(SimpleTextAttributes.STYLE_PLAIN, null, new JBColor(0xCCCCFF, 0x155221), null) :
                    isLoading ? SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES;

            Icon objectIcon = object.getIcon();
            ObjectDependencyTreeModel model = node.getModel();
            Icon dependencyTypeIcon = model.getDependencyType().getSoftIcon();
            Icon icon = node.getParent() == null ? objectIcon :
                    objectIcon == null ? dependencyTypeIcon : new MergedIcon(dependencyTypeIcon, 1, objectIcon);
            setIcon(icon);

            setBackground(selected ? UIUtil.getTreeSelectionBackground() : regularAttributes.getBgColor());
            //if (highlight) setBorder(new LineBorder(JBColor.red)); else setBorder(null);
            boolean appendSchema = true;

            ObjectDependencyTreeNode rootNode = model.getRoot();
            if (rootNode == null) {
                appendSchema = false;
            } else {
                DBObject rootObject = rootNode.getObject();
                if (rootObject == null || CommonUtil.safeEqual(rootObject.getSchema(), object.getSchema())) {
                    appendSchema = false;
                }
            }

            append(object.getName(), regularAttributes);
            if (appendSchema) {
                append(" (" + object.getSchema().getName() + ")", grayAttributes);
            }

            SpeedSearchUtil.applySpeedSearchHighlighting(tree, this, true, selected);
        } else {
            setIcon(LoadIcon.INSTANCE);
            append("Loading...", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
        }
    }

    @Override
    protected boolean shouldDrawBackground() {
        return isFocused();
    }
}