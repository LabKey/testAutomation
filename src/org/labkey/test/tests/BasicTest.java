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

package org.labkey.test.tests;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.Base;
import org.labkey.test.categories.DRT;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Git;
import org.labkey.test.categories.Hosting;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.WebPart;
import org.labkey.test.util.UIContainerHelper;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

@Category({Base.class, DRT.class, BVT.class, DailyA.class, Git.class, Hosting.class})
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
    public void testSystemSettings()
    {
        // Disable scheduled system maintenance
        setSystemMaintenance(false);

        goToAdminConsole().goToServerInformationSection();
        WebElement modeElement = Locator.tagWithText("td", "Mode").append("/../td[2]").findElement(getDriver());
        String mode = modeElement.getText();
        if (TestProperties.isDevModeEnabled())
            Assert.assertEquals("Development", mode); // Verify that we're running in dev mode
        else
            Assert.assertEquals("Production", mode); // Unless we're not supposed to be.

        goToAdminConsole().clickSiteSettings();
        checkRadioButton(Locator.radioButtonByNameAndValue("usageReportingLevel", "NONE"));     // Don't report usage to labkey.org
        checkRadioButton(Locator.radioButtonByNameAndValue("exceptionReportingLevel", "NONE"));   // Don't report exceptions to labkey.org - we leave the self-report setting unchanged
        clickButton("Save");

        // Verify scheduled system maintenance is disabled (see above). Can disable this only in dev mode.
        if (TestProperties.isDevModeEnabled())
        {
            goToAdminConsole().clickRunningThreads();
            assertTextNotPresent("SystemMaintenance");
        }
    }

    @Test
    public void testFolderAndRole()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME, new String[] {"Messages", "Wiki", "FileContent"});
        _permissionsHelper.createPermissionsGroup("testers");
        _ext4Helper.clickTabContainingText("Permissions");
        _permissionsHelper.assertPermissionSetting("testers", "No Permissions");
        _permissionsHelper.setPermissions("testers", "Editor");

        clickButton("Save and Finish");
        log("Test folder aliasing");
        pushLocation();
        _containerHelper.renameFolder(PROJECT_NAME, FOLDER_NAME, FOLDER_RENAME, true);
        popLocation();
        assertTextPresent(FOLDER_RENAME);

        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.assertPermissionSetting("testers", "Editor");
    }

    @Test
    public void testCredits()
    {
        // Navigate to the credits page and verify that all external components are documented
        beginAt(WebTestHelper.buildURL("admin", "credits"));
        Locator.XPathLocator warningLoc = Locator.tagWithClass("div", "labkey-wiki").containing("WARNING:");
        List<WebElement> warningWebparts = Locator.tagWithClass("table", "labkey-wp").withDescendant(warningLoc).findElements(getDriver());
        if (warningWebparts.size() > 0)
        {
            List<String> badModules = new ArrayList<>();
            for (WebElement wpEl : warningWebparts)
            {
                WebPart webPart = new BodyWebPart(getDriver(), wpEl);
                String title = webPart.getTitle();
                Pattern moduleNamePattern = Pattern.compile(".* ([^\\s]+) Module");
                Matcher matcher = moduleNamePattern.matcher(title);
                String module;
                if(matcher.find())
                    module = matcher.group(1);
                else
                    module = "<unknown>";
                log("Warning for " + module + " Module: " + warningLoc.findElement(wpEl).getText());
                if (!ignoreCreditsWarnings(module))
                    badModules.add(module);
            }
            assertTrue("Credits page is not up-to-date. See log for more details", badModules.isEmpty());
        }
        else
        {
            warningLoc.findElements(getDriver()).forEach((e)->log(e.getText()));
            assertTextNotPresent("WARNING:"); // In case the page format changes. Update test if this fails
        }
    }

    private boolean ignoreCreditsWarnings(String moduleName)
    {
        Set<String> ignoredModules = Collections.newSetFromMap(new CaseInsensitiveHashMap<>());
        // This set isn't expected to change much, so just hard code it for now
        ignoredModules.addAll(Arrays.asList("elispot", "pepdb", "peptide", "specimen_tracking"));
        return ignoredModules.contains(moduleName);
    }

    @Test
    public void testScripts()
    {
        Assume.assumeTrue(TestProperties.isDevModeEnabled());
        // Check for unrecognized scripts on the orphaned scripts page (only available in dev mode)
        beginAt(WebTestHelper.buildURL("admin-sql", "orphanedScripts"));
        assertTextNotPresent("WARNING:");
    }

    @Test @Ignore
    public void testRedirects()
    {
        goToHome();
        final String expectedTitle = getDriver().getTitle();

        beginAt("/login/initialUser.view");
        Assert.assertEquals("Initial user action did not redirect properly when logged in", expectedTitle, getDriver().getTitle());
    }

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("core", "login", "admin");
    }
}
