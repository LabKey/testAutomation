/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.DRT;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.UIContainerHelper;

@Category({DRT.class, BVT.class, DailyA.class})
public class BasicTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "BasicVerifyProject";
    private static final String FOLDER_NAME = "childfolder";
    private static final String FOLDER_RENAME = "renamedfolder";

    public BasicTest()
    {
        super();
        _containerHelper = new UIContainerHelper(this);
    }

    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Test
    public void testSteps()
    {
        // Disable scheduled system maintenance
        setSystemMaintenance(false);

        goToSiteSettings();
        checkRadioButton(Locator.radioButtonByNameAndValue("usageReportingLevel", "MEDIUM"));     // Force devs to report full usage info
        checkRadioButton(Locator.radioButtonByNameAndValue("exceptionReportingLevel", "HIGH"));   // Force devs to report full exception info
        clickButton("Save");

        _containerHelper.createProject(PROJECT_NAME, null);
        createPermissionsGroup("testers");
        _ext4Helper.clickTabContainingText("Permissions");
        assertPermissionSetting("testers", "No Permissions");
        setPermissions("testers", "Editor");
        clickButton("Save and Finish");
        createSubfolder(getProjectName(), FOLDER_NAME, new String[] {"Messages", "Wiki", "MS2"});

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Search");
        setFormElement(Locator.id("query"), "labkey");
        clickButton("Search");
        assertTextPresent("Found", "results");  // just make sure we get the results page

        goToAdminConsole();

        if (TestProperties.isDevModeEnabled())
            assertTextNotPresent("Production"); // Verify that we're running in dev mode
        else
            assertTextNotPresent("Development"); // Unless we're not supposed to be.

        // Navigate to the credits page and verify that all external components are documented
        waitAndClick(Locator.linkWithText("credits"));
        assertTextNotPresent("WARNING:");

        // Check for unrecognized scripts on the orphaned scripts page (only available in dev mode)
        if (TestProperties.isDevModeEnabled())
        {
            goToAdminConsole();
            clickAndWait(Locator.linkWithText("sql scripts"));
            clickAndWait(Locator.linkWithText("orphaned scripts"));
            assertTextNotPresent("WARNING:");
        }

        ensureAdminMode();
        clickProject(PROJECT_NAME);
        clickFolder(FOLDER_NAME);

        log("Test folder aliasing");
        pushLocation();
        renameFolder(PROJECT_NAME, FOLDER_NAME, FOLDER_RENAME, true);
        popLocation();
        assertTextPresent(FOLDER_RENAME);

        // Verify scheduled system maintenance is disabled (see above). Can disable this only in dev mode.
        if (TestProperties.isDevModeEnabled())
        {
            goToAdminConsole();
            waitAndClick(Locator.linkWithText("running threads"));
            assertTextNotPresent("SystemMaintenance");
        }
    }

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/core";
    }
}
