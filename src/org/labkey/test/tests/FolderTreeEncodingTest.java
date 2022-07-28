/*
 * Copyright (c) 2017-2018 LabKey Corporation
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
package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.admin.PermissionsPage;
import org.labkey.test.pages.core.admin.ConfigureFileSystemAccessPage;

import java.util.List;

@Category(Daily.class)
public class FolderTreeEncodingTest extends BaseWebDriverTest
{
    private static final String CHILD_CONTAINER = "ChildContainer &nbsp";

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return "FolderTreeEncodingTest &nbsp";
    }

    @BeforeClass
    public static void initTest()
    {
        FolderTreeEncodingTest init = (FolderTreeEncodingTest)getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), CHILD_CONTAINER);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testPermissionsPageFolderTree()
    {
        // Issue 45079: Double encoding at the root of the permissions page folder tree
        PermissionsPage page = goToFolderPermissions();
        isElementPresent(Locator.linkWithText(CHILD_CONTAINER).withClass("x4-tree-node-text"));
        isElementPresent(Locator.linkWithText(getProjectName()).withClass("x4-tree-node-text"));
    }

    @Test
    public void testAdminFileDirectoriesFolderTree()
    {
        // Issue 45802: Double encoded folder names in Files -> File Directories list box
        ConfigureFileSystemAccessPage page = goToAdminConsole().clickFiles();
        page.selectFolder(getProjectName());
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
