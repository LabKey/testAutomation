package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public interface FileBrowserHelperParams
{
    @LogMethod(quiet = true)
    public void selectFileBrowserItem(@LoggedParam String path);

    @LogMethod(quiet = true)
    public void clickFileBrowserFileCheckbox(@LoggedParam String fileName);

    public void selectFileBrowserRoot();

    @LogMethod
    public void selectAllFileBrowserFiles();

    @LogMethod(quiet = true)
    public void expandFileBrowserRootNode();

    public void renameFile(String currentName, String newName);

    public void createFolder(String folderName);

    public void goToConfigureButtonsTab();

    public void goToAdminMenu();

    public void uploadFile(File file);

    public void uploadFile(File file, @Nullable String description, @Nullable List<FileBrowserExtendedProperty> fileProperties);

    public void importFile(String fileName, String importAction);

    public void selectImportDataAction(@LoggedParam String actionName);

    public void clickFileBrowserButton(@LoggedParam String actionName);

    public void waitForFileGridReady();

    public void waitForImportDataEnabled();

}
