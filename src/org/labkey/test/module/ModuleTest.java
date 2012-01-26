/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

package org.labkey.test.module;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * User: ulberge
 * Date: Aug 7, 2007
 */
public class ModuleTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "ModuleVerifyProject";
    private static final String TEST_MODULE_TEMPLATE_FOLDER_NAME = "testmodule";

    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        createProject(PROJECT_NAME);
        log("Test module created from moduleTemplate");
        clickLinkWithText(PROJECT_NAME);
        clickAdminMenuItem("Project", "Management");
        clickLinkContainingText("Folder Settings");
        checkCheckbox(Locator.raw("//input[@value='" + TEST_MODULE_TEMPLATE_FOLDER_NAME + "']"));
        clickNavButton("Update Folder");
        clickTab(TEST_MODULE_TEMPLATE_FOLDER_NAME);
        assertTextPresent("Hello, and welcome to the " + TEST_MODULE_TEMPLATE_FOLDER_NAME + " module.");
    }
    
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }
}
