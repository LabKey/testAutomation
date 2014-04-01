/*
 * Copyright (c) 2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    @LogMethod(quiet = true)
    public void expandFileBrowserRootNode();

    public void openFolderTree();

    public void renameFile(String currentName, String newName);

    public void moveFile(String fileName, String destinationPath);

    public void createFolder(String folderName);

    public void addToolbarButton(String buttonId);

    public void removeToolbarButton(String buttonId);

    public void goToConfigureButtonsTab();

    public void goToAdminMenu();

    public void uploadFile(File file);

    public void uploadFile(File file, @Nullable String description, @Nullable List<FileBrowserExtendedProperty> fileProperties, boolean replace);

    public void importFile(String filePath, String importAction);

    public void selectImportDataAction(@LoggedParam String actionName);

    public void clickFileBrowserButton(@LoggedParam String actionName);

    public void waitForFileGridReady();

    public void waitForImportDataEnabled();

}
