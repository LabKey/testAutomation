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

package org.labkey.test.tests;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.UIContainerHelper;

/**
 * User: marki
 * Date: March 23, 2007
 * Time: 1:57:05 PM
 */
public class BasicTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "BasicVerifyProject";
    private static final String FOLDER_NAME = "childfolder";
    private static final String FOLDER_RENAME = "renamedfolder";
    private static final String WIKI_WEBPART_TEXT = "The Wiki web part displays a single wiki page.";
    private static final String MESSAGES_WEBPART_TEXT = "all messages";

    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        _containerHelper = new UIContainerHelper(this);
    }

    protected void doTestSteps()
    {
        log("Test browser version");
        String source = getHtmlSource();
        assertTrue("Unsupported browser", isBrowser(source, "Firefox/", 2.0, 14.0) || isBrowser(source, "MSIE ", 7.0, 8.0));

        // Disable scheduled system maintenance
        setSystemMaintenance(false);
        // Manually start system maintenance... we'll check for completion at the end of the test (before mem check)
        startSystemMaintenance();

        checkRadioButton("usageReportingLevel", "MEDIUM");     // Force devs to report full usage info
        checkRadioButton("exceptionReportingLevel", "HIGH");   // Force devs to report full exception info
        clickNavButton("Save");

        _containerHelper.createProject(PROJECT_NAME, null);
        createPermissionsGroup("testers");
        assertPermissionSetting("testers", "No Permissions");
        setPermissions("testers", "Editor");
        clickNavButton("Save and Finish");
        enableModule(PROJECT_NAME, "MS2");
        createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[] {"Messages", "Wiki", "MS2"});
        addWebPart("Messages");
        assertLinkPresentWithText("Messages");
        addWebPart("Wiki");
        assertTextPresent("Wiki");
        assertLinkPresentWithText("Create a new wiki page");
        addWebPart("Wiki Table of Contents");
        // move messages below wiki:
        clickLinkWithImage("/_images/partdown.png", 0);
        waitForExtMaskToDisappear(30000);
        assertTextBefore(WIKI_WEBPART_TEXT, MESSAGES_WEBPART_TEXT);

        refresh();
        // Verify that the asynchronous save worked by refreshing:
        assertTextBefore(WIKI_WEBPART_TEXT, MESSAGES_WEBPART_TEXT);

        // remove wiki by clicking the first delete link:
        clickLinkWithImage("/_images/partdelete.png", 0);
        waitForExtMaskToDisappear(30000);
        assertTextNotPresent(WIKI_WEBPART_TEXT);

        refresh();
        // verify that the web part removal was correctly saved:
        assertTextNotPresent(WIKI_WEBPART_TEXT);

        // verify that messages is still present:
        assertLinkPresentWithText("Messages");
        addWebPart("MS2 Runs");
        assertLinkPresentWithText("MS2 Runs");

        addWebPart("Search");
        setFormElement("query", "labkey");
        clickNavButton("Search");
        assertTextPresent("Found", "results");  // just make sure we get the results page

        goToAdminConsole();

        if (enableDevMode())
            assertTextNotPresent("Production"); // Verify that we're running in dev mode
        else
            assertTextNotPresent("Development"); // Unless we're not supposed to be.

        // Navigate to the credits page and verify that all external components are documented
        clickLinkWithText("credits");
        assertTextNotPresent("WARNING:");

        ensureAdminMode();
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);

        log("Test folder aliasing");
        pushLocation();
        renameFolder(PROJECT_NAME, FOLDER_NAME, FOLDER_RENAME, true);
        popLocation();
        assertTextPresent(FOLDER_RENAME);

        log("Test webpart buttons");
        clickWebpartMenuItem("Messages", "Customize");      
        assertTextPresent("Customize");
        clickNavButton("Cancel");
        clickLinkWithImage(getContextPath() + "/_images/partdown.png", 0);
        waitForExtMaskToDisappear();
        assertTextBefore("No data to show", "No messages");
        
        refresh();
        assertTextBefore("No data to show", "No messages");

        final Locator searchLocator = Locator.raw("//tr[th[@title='Search']]//a/img[@title='Remove From Page']");
        clickAndWait(searchLocator, 0);
        waitForExtMaskToDisappear();
        assertElementNotPresent(searchLocator);
        refresh();
        // verify that web part is gone, even after a refresh:
        assertElementNotPresent(searchLocator);

        // Now that the test is done, ensure that system maintenance is complete...
        waitForSystemMaintenanceCompletion();

        // Verify scheduled system maintenance is disabled.
        goToAdminConsole();
        clickLinkWithText("running threads");
        assertTextNotPresent("SystemMaintenance");
    }

    private boolean isBrowser(String source, String browserNameAndSeparator, double startVersion, double endVersion)
    {
        if (source.indexOf(browserNameAndSeparator) != -1)
        {
            int start = source.indexOf(browserNameAndSeparator);
            int end = source.indexOf("-->", start);
            String version = source.substring(start, end);

            // Handle "Firefox/3.6.25", etc.
            if (StringUtils.countMatches(version, ".") > 1)
            {
                int secondDot = version.indexOf(".", version.indexOf(".") + 1);
                version = version.substring(0, secondDot);
            }

            double versionNumber = Double.parseDouble(version.substring(browserNameAndSeparator.length()));
            assertTrue("The LabKey test suite requires " + browserNameAndSeparator.substring(0, browserNameAndSeparator.length() - 1) + " " + startVersion + " - " + endVersion, versionNumber >= startVersion && versionNumber <= endVersion);
            log("Browser = " + version);
            return true;
        }

        return false;
    }

    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}
