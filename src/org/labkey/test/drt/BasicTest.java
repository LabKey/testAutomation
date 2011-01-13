/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

package org.labkey.test.drt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

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
        try {deleteFolder(PROJECT_NAME, FOLDER_NAME); } catch (Throwable t) {}
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }


    protected void doTestSteps()
    {
        // Manually start system maintenance... we'll check for completion at the end of the test (before mem check)
        startSystemMaintenance();

        checkRadioButton("usageReportingLevel", "MEDIUM");     // Force devs to report full usage info
        checkRadioButton("exceptionReportingLevel", "HIGH");   // Force devs to report full exception info
        clickNavButton("Save");

        createProject(PROJECT_NAME);
        createPermissionsGroup("testers");
        assertPermissionSetting("testers", "No Permissions");
        setPermissions("testers", "Editor");
        clickNavButton("Save and Finish");
        createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[] {"Messages", "Wiki"});
        addWebPart("Messages");
        assertLinkPresentWithText("Messages");
        addWebPart("Wiki");
        assertTextPresent("Wiki");
        assertLinkPresentWithText("Create a new wiki page");
        addWebPart("Wiki TOC");
        // move messages below wiki:
        clickLinkWithImage("/_images/partdown.gif", 0);
        waitFor(new Checker()
        {
            public boolean check()
            {
                return isTextBefore(WIKI_WEBPART_TEXT, MESSAGES_WEBPART_TEXT);
            }
        }, "Web parts failed to reorder", 30000);

        refresh();
        // Verify that the asynchronous save worked by refreshing:
        assertTextBefore(WIKI_WEBPART_TEXT, MESSAGES_WEBPART_TEXT);

        // remove wiki by clicking the first delete link:
        clickLinkWithImage("/_images/partdelete.gif", 0);
        waitFor(new Checker()
        {
            public boolean check()
            {
                return !isTextPresent(WIKI_WEBPART_TEXT);
            }
        }, "Web part was not removed correctly.", 30000);
        refresh();
        // verify that the web part removal was correctly saved:
        assertTextNotPresent(WIKI_WEBPART_TEXT);

        // verify that messages is still present:
        assertLinkPresentWithText("Messages");
        addWebPart("MS2 Experiment Runs");
        assertLinkPresentWithText("MS2 Experiment Runs");

        addWebPart("Search");
        setFormElement("query", "labkey");
        clickNavButton("Search");
        assertTextPresent("Found", "results");  // just make sure we get the results page

        clickLinkWithText("Admin Console");

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

        log("Test firefox version");
        String source = getHtmlSource();
        assertTrue("The LabKey test suite requires Firefox 2.0, 3.0, 3.5, or 3.6", source.contains("Firefox/3.6") || source.contains("Firefox/3.5") || source.contains("Firefox/3.0") || source.contains("Firefox/2.0"));

        log("Test webpart buttons");
        clickAndWait(Locator.raw("//th[contains(text(), 'Search')]/..//a/img[@title='Customize Web Part']"));
        assertTextPresent("Customize");
        clickNavButton("Cancel");
        clickAndWait(Locator.raw("//a[contains(text(), 'Messages')]/../..//a/img[@title='Move Down']"), 0);
        waitFor(new Checker() {
            public boolean check()
            {
                return isTextBefore("No data to show", "No messages");
            }
        }, "Failed to move web part.", 30000);
        refresh();
        assertTextBefore("No data to show", "No messages");

        final Locator searchLocator = Locator.raw("//th[contains(text(), 'Search')]/..//a/img[@title='Remove From Page']");
        clickAndWait(searchLocator, 0);
        waitFor(new Checker() {
            public boolean check()
            {
                return !isElementPresent(searchLocator);
            }
        }, "Failed to remove web part.", 30000);
        refresh();
        // verify that web part is gone, even after a refresh:
        assertElementNotPresent(searchLocator);

        // Now that the test is done, ensure that system maintenance is complete...
        waitForSystemMaintenanceCompletion();
    }

    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}
