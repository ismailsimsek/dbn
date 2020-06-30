package com.dci.intellij.dbn.vfs;

import com.dci.intellij.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class DatabaseOpenFileDescriptor extends OpenFileDescriptor {
    public DatabaseOpenFileDescriptor(Project project, @NotNull VirtualFile file, int offset) {
        super(project, file, offset);
    }

    public DatabaseOpenFileDescriptor(Project project, @NotNull VirtualFile file, int line, int col) {
        super(project, file, line, col);
    }

    public DatabaseOpenFileDescriptor(Project project, @NotNull VirtualFile file) {
        super(project, file);
    }

    @Override
    public void navigate(boolean requestFocus) {
        super.navigate(requestFocus);
    }

    @Override
    public void navigateIn(@NotNull Editor e) {
        super.navigateIn(e);
    }

    @Override
    public boolean canNavigate() {
        return super.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return super.canNavigateToSource();
    }

    @Override
    public boolean navigateInEditor(@NotNull Project project, boolean requestFocus) {
        return super.navigateInEditor(project, requestFocus);
    }

    @NotNull
    @Override
    public VirtualFile getFile() {
        VirtualFile file = super.getFile();
        if (file instanceof DBSourceCodeVirtualFile) {
            DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) file;
            return sourceCodeFile.getMainDatabaseFile();
        }

        return file;
    }
}
