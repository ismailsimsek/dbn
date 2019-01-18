package com.dci.intellij.dbn.object.common.list.loader;

import com.dci.intellij.dbn.common.content.DynamicContentElement;
import com.dci.intellij.dbn.common.content.DynamicContentType;
import com.dci.intellij.dbn.common.content.loader.DynamicContentLoader;
import com.dci.intellij.dbn.connection.GenericDatabaseElement;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.common.DBObjectType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DBObjectListLoaderRegistry {
    private static final Map<DynamicContentType, DynamicContentLoader> ROOT_LOADERS = new HashMap<>();
    private static final Map<DynamicContentType, Map<DynamicContentType, DynamicContentLoader>> CHILD_LOADERS = new HashMap<>();

    public static void register(@NotNull GenericDatabaseElement parent, @NotNull DynamicContentType contentType, @NotNull DynamicContentLoader loader) {
        if (parent instanceof DBObject) {
            DBObject parentObject = (DBObject) parent;
            DBObjectType parentObjectType = parentObject.getObjectType();
            Map<DynamicContentType, DynamicContentLoader> childLoaders = CHILD_LOADERS.computeIfAbsent(parentObjectType, k -> new HashMap<>());
            DynamicContentLoader contentLoader = childLoaders.get(contentType);
            if (contentLoader == null) {
                childLoaders.put(contentType, loader);
            } else if (contentLoader != loader){
                System.out.println("Duplicate loader");
            }
        } else {
            DynamicContentLoader contentLoader = ROOT_LOADERS.get(contentType);
            if (contentLoader == null) {
                ROOT_LOADERS.put(contentType, loader);
            } else if (contentLoader != loader){
                System.out.println("Duplicate loader");
            }
        }
    }

    @NotNull
    public static <T extends DynamicContentElement> DynamicContentLoader<T> get(@NotNull GenericDatabaseElement parent, @NotNull DynamicContentType contentType) {
        if (parent instanceof DBObject) {
            DBObject parentObject = (DBObject) parent;
            DBObjectType parentObjectType = parentObject.getObjectType();
            Map<DynamicContentType, DynamicContentLoader> childLoaders = CHILD_LOADERS.get(parentObjectType);
            return childLoaders.get(contentType);
        } else {
            return ROOT_LOADERS.get(contentType);
        }
    }
}
