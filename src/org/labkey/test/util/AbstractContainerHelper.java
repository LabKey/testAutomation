/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public abstract class AbstractContainerHelper extends AbstractHelper
{
    private List<String> _createdProjects = new ArrayList<>();

    public AbstractContainerHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    /** @param folderType the name of the type of container to create.
     * May be null, in which case you get the server's default folder type */
    @LogMethod(quiet = true)
    public final void createProject(@LoggedParam String projectName, @Nullable String folderType)
    {
        doCreateProject(projectName, folderType);
        _createdProjects.add(projectName);
    }

    public abstract void createSubfolder(String parentPath, String folderName, @Nullable String folderType);


    protected abstract void doCreateProject(String projectName, String folderType);
    protected abstract void doCreateFolder(String projectName, String folderType, String path);

    public List<String> getCreatedProjects()
    {
        return _createdProjects;
    }

    // Projects might be created by other means
    public void addCreatedProject(String projectName)
    {
        _createdProjects.add(projectName);
    }

    public final void deleteProject(String projectName) throws TestTimeoutException
    {
        deleteProject(projectName, true, 90000);
    }

    @LogMethod
    public final void deleteProject(@LoggedParam String projectName, boolean failIfNotFound, int wait) throws TestTimeoutException
    {
        doDeleteProject(projectName, failIfNotFound, wait);
        _createdProjects.remove(projectName);
    }

    public abstract void doDeleteProject(String projectName, boolean failIfNotFound, int wait) throws TestTimeoutException;

    @LogMethod(quiet = true)
    public void setFolderType(@LoggedParam String folderType)
    {
        _test.goToFolderManagement();
        _test.clickAndWait(Locator.linkWithText("Folder Type"));
        _test.click(Locator.radioButtonByNameAndValue("folderType", folderType));
        _test.clickButton("Update Folder");
    }

    public void enableModule(String projectName, String moduleName)
    {
        _test.ensureAdminMode();
        _test.clickProject(projectName);
        enableModule(moduleName);
    }

    public void enableModule(String moduleName)
    {
        enableModules(Collections.singletonList(moduleName));
    }

    public void enableModules(List<String> moduleNames)
    {
        enableModules(moduleNames, false);
    }

    // NOTE: consider using this pathway all the time (e.g. drop the the checkFirst flag).
    public void enableModules(List<String> moduleNames, boolean checkFirst)
    {
        _test.goToFolderManagement();
        _test.clickAndWait(Locator.linkWithText("Folder Type"));
        for (String moduleName : moduleNames)
        {
            Locator loc = Locator.checkboxByTitle(moduleName);
            if(checkFirst)
                assertTrue(moduleName + " module was not detected. Check that the module is installed and you are on the supported backend.", _test.isElementPresent(loc));
            _test.checkCheckbox(loc);
        }
        _test.clickButton("Update Folder");
    }

    public void disableModules(String... moduleNames)
    {
        _test.goToFolderManagement();
        _test.clickAndWait(Locator.linkWithText("Folder Type"));
        for (String moduleName : moduleNames)
        {
            _test.uncheckCheckbox(Locator.checkboxByTitle(moduleName));
        }
        _test.clickButton("Update Folder");
    }
}
