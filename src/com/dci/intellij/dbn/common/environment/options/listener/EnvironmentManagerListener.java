package com.dci.intellij.dbn.common.environment.options.listener;

import java.util.EventListener;

import com.dci.intellij.dbn.vfs.DBContentVirtualFile;
import com.intellij.util.messages.Topic;

public interface EnvironmentManagerListener extends EventListener {
    Topic<EnvironmentManagerListener> TOPIC = Topic.create("Environment changed", EnvironmentManagerListener.class);
    void configurationChanged();
    void editModeChanged(DBContentVirtualFile databaseContentFile);
}