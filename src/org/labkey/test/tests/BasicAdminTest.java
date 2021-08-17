/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;
import org.labkey.test.categories.Base;
import org.labkey.test.categories.DRT;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Git;
import org.labkey.test.categories.Hosting;
import org.labkey.test.util.UIContainerHelper;
import org.labkey.test.util.UIPermissionsHelper;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

@Category({Base.class, DRT.class, Daily.class, Git.class, Hosting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class BasicAdminTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "BasicVerifyProject";
    private static final String FOLDER_NAME = "childfolder";
    private static final String FOLDER_RENAME = "renamedfolder";

    @Override
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
    public void testSystemSettings()
    {
        Assume.assumeFalse("Testing system settings require site admin.", TestProperties.isPrimaryUserAppAdmin());

        // Disable scheduled system maintenance
        setSystemMaintenance(false);

        goToAdminConsole().goToServerInformationSection();
        WebElement modeElement = Locator.tagWithText("td", "Mode").append("/../td[2]").findElement(getDriver());
        String mode = modeElement.getText();
        if (TestProperties.isDevModeEnabled())
            checker().verifyEquals("Wrong server mode",
                    TestProperties.isDevModeEnabled() ? "Development" : "Production", mode); // Verify whether we're running in dev mode

        // Verify scheduled system maintenance is disabled (see above). Can disable this only in dev mode.
        if (TestProperties.isDevModeEnabled() && !TestProperties.isPrimaryUserAppAdmin())
        {
            goToAdminConsole().clickRunningThreads();
            assertTextNotPresent("SystemMaintenance");
        }
    }

    @Test
    public void testFolderAndRole()
    {
        UIPermissionsHelper permissionsHelper = new UIPermissionsHelper(this);
        UIContainerHelper containerHelper = new UIContainerHelper(this);

        containerHelper.createProject(PROJECT_NAME, null);
        containerHelper.createSubfolder(getProjectName(), FOLDER_NAME, new String[] {"FileContent"});
        permissionsHelper.createPermissionsGroup("testers");
        _ext4Helper.clickTabContainingText("Permissions");
        permissionsHelper.assertPermissionSetting("testers", "No Permissions");
        permissionsHelper.setPermissions("testers", "Editor");

        clickButton("Save and Finish");
        log("Test folder aliasing");
        pushLocation();
        containerHelper.renameFolder(PROJECT_NAME, FOLDER_NAME, FOLDER_RENAME, true);
        popLocation();
        assertTextPresent(FOLDER_RENAME);

        permissionsHelper.assertPermissionSetting("testers", "Editor");
    }

    @Test @Ignore
    public void testRedirects()
    {
        goToHome();
        final String expectedTitle = getDriver().getTitle();

        beginAt("/login/initialUser.view");
        Assert.assertEquals("Initial user action did not redirect properly when logged in", expectedTitle, getDriver().getTitle());
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("core");
    }
}
