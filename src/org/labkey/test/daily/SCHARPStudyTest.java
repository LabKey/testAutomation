package org.labkey.test.daily;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ListHelper;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: dave
 * Date: Aug 4, 2009
 * Time: 1:59:39 PM
 */
public class SCHARPStudyTest extends BaseSeleniumWebTest
{
    public static final String PROJECT_NAME="SCHARP Study Test";

    private String _labkeyRoot = getLabKeyRoot();
    private String _pipelinePathMain = new File(_labkeyRoot, "/sampledata/study").getPath();
    private String _zipFilePath = new File(_labkeyRoot, "/sampledata/study/studyshell.zip").getPath();

    protected static class StatusChecker implements Checker
    {
        private BaseSeleniumWebTest _test;
        private String _waitForMessage;
        private Locator _loc = Locator.id("vq-status");

        public StatusChecker(String waitForMessage, BaseSeleniumWebTest test)
        {
            _test = test;
            _waitForMessage = waitForMessage;
        }

        public boolean check()
        {
            String curMessage = _test.getText(_loc);
            if (null == curMessage)
                fail("Can't get message in locator " + _loc.toString());
            return (curMessage.startsWith(_waitForMessage));
        }
    }

    protected void doTestSteps() throws Exception
    {
        log("creating project...");
        createProject(PROJECT_NAME, "Study");

        log("importing study...");
        setupPipeline();
        importStudy();
        createLookupLists();

        log("navigating to validation page...");
        clickAdminMenuItem("Go To Module", "Query");
        clickLinkWithText("Validate Queries");

        log("waiting for queries to load...");
        waitFor(new StatusChecker("All queries loaded.", this), "Queries did not load on validation page!", 10000);

        log("stating query validation...");

        // For now, only include the default visible columns, as Flow has a number of queries that
        // will fail to validate if we include all the columns. There is an open bug on this.
        // see https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=8439
        clickRadioButtonById("rb-include-defvis");

        selenium.click(Locator.id("btn-validate").toString());
        waitFor(new StatusChecker("Validation complete.", this), "Queries took to long to validate!", 30000);
        waitFor(new StatusChecker("Validation complete. All queries were validated successfully.", this), "Validation of queries failed!", 100);
        log("all queries validated successfully.");
    }

    private void createLookupLists()
    {
        //The ZIP file makes use of the following lookup lists
        ListHelper.createList(this, PROJECT_NAME, "WB Scoring (Denny)",
                ListHelper.ListColumnType.AutoInteger, "Key",
                new ListHelper.ListColumn("Value", "Value", ListHelper.ListColumnType.String, "")
        );
    }

    protected void setupPipeline()
    {
        log("Setting pipeline root to " + _pipelinePathMain + "...");
        clickNavButton("Pipeline Setup");
        setFormElement("path", _pipelinePathMain);
        clickNavButton("Set");
        assertTextPresent("The pipeline root was set");
        clickLinkWithText(PROJECT_NAME);
    }

    protected void importStudy()
    {
        log("Importing study from " + _zipFilePath + "...");
        clickNavButton("Import Study");
        setFormElement("studyZip", _zipFilePath);
        clickNavButton("Import Study");
        assertTextNotPresent("This file does not appear to be a valid .zip file");

        if (isTextPresent("You must select a .zip file to import"))
        {
            setFormElement("studyZip", _zipFilePath);
            clickNavButton("Import Study");
        }

        assertTextPresent("Data Pipeline");

        while(countLinksWithText("COMPLETE") < 2)
        {
            if (countLinksWithText("ERROR") > 0)
            {
                fail("Job in ERROR state found in the list");
            }

            log("Wating for study to finish loading...");
            sleep(3000);
            refresh();
        }

        clickLinkWithText(PROJECT_NAME);
    }

    protected void doCleanup() throws Exception
    {
        log("Starting cleanup...");
        try
        {
            deleteProject(PROJECT_NAME);
        }
        catch(Throwable ignore){}
        log("Cleaned up successfully.");
    }

    public String getAssociatedModuleDirectory()
    {
        return "study";
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }
}
