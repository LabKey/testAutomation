/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.NoSuite;

import java.util.List;

@Category({NoSuite.class})
public class ModuleTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ModuleVerifyProject";
    private static final String TEST_MODULE_TEMPLATE_FOLDER_NAME = "testmodule";

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Test
    public void testSteps()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        log("Test module created from moduleTemplate");
        goToFolderManagement().goToFolderTypeTab()
                .enableModule(TEST_MODULE_TEMPLATE_FOLDER_NAME)
                .save();
        clickTab(TEST_MODULE_TEMPLATE_FOLDER_NAME);
        assertTextPresent("Hello, and welcome to the " + TEST_MODULE_TEMPLATE_FOLDER_NAME + " module.");
    }
    
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }
}
