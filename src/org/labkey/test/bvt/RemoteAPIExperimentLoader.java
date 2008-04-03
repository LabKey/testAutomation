package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.Crawler;

/**
 * User: brittp
 * Date: Nov 22, 2005
 * Time: 1:31:42 PM
 */
public class RemoteAPIExperimentLoader extends BaseSeleniumWebTest
{
    private static final int MAX_WAIT_SECONDS = 60*5;

    public String getAssociatedModuleDirectory()
    {
        return "experiment";
    }

    protected void doCleanup()
    {
        try {deleteFolder(RemoteAPITest.PROJECT_NAME, RemoteAPITest.PRIVATE_SUBFOLDER_NAME); } catch (Throwable t) {}
        try {deleteFolder(RemoteAPITest.PROJECT_NAME, RemoteAPITest.PUBLIC_SUBFOLDER_NAME); } catch (Throwable t) {}
        try {deleteProject(RemoteAPITest.PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        throw new UnsupportedOperationException();
    }

    public void initialize(boolean keepExisting) throws Exception
    {
        try
        {
            setUp();
            log("\n\n=============== Starting " + getClass().getSimpleName() + " =================");

            beginAt("");
            signIn();
            if (keepExisting && isLinkPresentWithText(RemoteAPITest.PROJECT_NAME))
            {
                return;
            }

            cleanup();

            createProject(RemoteAPITest.PROJECT_NAME);
            setPermissions("Guests", "Reader");

            createSubfolder(RemoteAPITest.PROJECT_NAME, RemoteAPITest.PUBLIC_SUBFOLDER_NAME, new String[0]);
            clickLinkWithText("Permissions");
            setPermissions("Guests", "Reader");
            createSubfolder(RemoteAPITest.PROJECT_NAME, RemoteAPITest.PRIVATE_SUBFOLDER_NAME, new String[0]);
            clickLinkWithText("Permissions");
            setPermissions("Guests", "No Permissions");

            clickLinkWithText(RemoteAPITest.PRIVATE_SUBFOLDER_NAME);
            addWebPart("Data Pipeline");
            clickNavButton("Setup");
            setFormElement("path", getLabKeyRoot() + "/sampledata/xarfiles/RemoteAPITest/private");
            submit();

            clickNavButton("View Status");
            clickNavButton("Process and Import Data");
            clickNavButton("Import Experiment");
            clickLinkWithText("Data Pipeline");
            // Unfortunately assertNotLinkWithText also picks up the "Errors" link in the header.
            assertTextNotPresent(">ERROR<");  // Must be surrounded by an anchor tag.
            int seconds = 0;
            while (!isLinkPresentWithText("COMPLETE") && seconds++ < MAX_WAIT_SECONDS)
            {
                sleep(1000);
                refresh();
            }
            if (!isLinkPresentWithText("COMPLETE"))
                fail("Import did not complete.");

            clickLinkWithText(RemoteAPITest.PROJECT_NAME);
            addWebPart("Data Pipeline");
            clickNavButton("Setup");
            setFormElement("path", getLabKeyRoot() + "/sampledata/xarfiles/RemoteAPITest");
            submit();
            clickNavButton("View Status");
            clickNavButton("Process and Import Data");
            clickNavButton("Import Experiment");
            clickLinkWithText("Data Pipeline");
            // Unfortunately assertNotLinkWithText also picks up the "Errors" link in the header.
            assertTextNotPresent(">ERROR<");  // Must be surrounded by an anchor tag.
            seconds = 0;
            while (!isLinkPresentWithText("COMPLETE") && seconds++ < MAX_WAIT_SECONDS)
            {
                sleep(1000);
                refresh();
            }
            if (!isLinkPresentWithText("COMPLETE"))
                fail("Import did not complete.");

            checkCheckbox(".toggle");
            clickNavButton("Delete");

            clickNavButton("Process and Import Data");
            clickLinkWithText("fractionation");
            clickNavButton("Import Experiment");
            clickLinkWithText("Data Pipeline");
            // Unfortunately assertNotLinkWithText also picks up the "Errors" link in the header.
            assertTextNotPresent(">ERROR<");  // Must be surrounded by an anchor tag.
            seconds = 0;
            while (!isLinkPresentWithText("COMPLETE") && seconds++ < MAX_WAIT_SECONDS)
            {
                sleep(1000);
                refresh();
            }
            if (!isLinkPresentWithText("COMPLETE"))
                fail("Import did not complete.");

            if (enableLinkCheck())
            {
                Crawler crawler = new Crawler(this);
                crawler.crawlAllLinks();
            }
        }
        finally
        {
            try
            {
                populateLastPageInfo();
            }
            catch (Throwable t)
            {
                System.out.println("Unable to determine information about the last page: server not started or -Dlabkey.port incorrect?");
            }
            log("=============== Completed " + getClass().getSimpleName() + " =================");
        }
    }
}
