package org.labkey.test.tests;

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;

/**
 * User: tgaluhn
 * Date: 4/25/2014
 */
@Category(DailyB.class)
public class StudyDatasetImportFieldsTest extends StudyBaseTest
{
    private static final String PROJECT_NAME =  "Dataset Import Fields Test Project";
    private static final String STUDY_NAME = "Dataset Import Fields Test Study";
    private static final String FOLDER_NAME =  "Dataset Import Fields Test Folder";
    private static final String INITIAL_COL = "Test Field";
    private static final String INITIAL_COL_VAL = "Some data";
    private static final String REPLACEMENT_COL = "Nukeum";

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected String getStudyLabel()
    {
        return STUDY_NAME;
    }

    @Override
    protected String getFolderName()
    {
        return FOLDER_NAME;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected void doCreateSteps()
    {
        initializeFolder();
        clickFolder(getFolderName());
    }

    @Override
    protected void doVerifySteps() throws Exception
    {
        clickButton("Create Study", 0);
        waitForText(FOLDER_NAME + " Study");
        clickButton("Create Study", 0);
        waitForText("Manage Datasets");
        goToManageDatasets();
        click(Locator.linkWithText("Create New Dataset"));
        waitForElement(Locator.name("typeName"));
        setFormElement(Locator.name("typeName"), "Test Dataset");
        clickButton("Next", 0);
        waitForElement(Locator.name("ff_name0"));
        setFormElement(Locator.name("ff_name0"), INITIAL_COL);
        clickButton("Save", 0);
        waitForElement(Locator.linkWithText("View Data"));
        click(Locator.linkWithText("View Data"));
        waitForText("Insert New");
        clickAndWait(Locator.linkWithText("Insert New"));
        waitForElement(Locator.name("quf_ParticipantId"));
        setFormElement(Locator.name("quf_ParticipantId"), "47");
        setFormElement(Locator.name("quf_SequenceNum"), "47");
        setFormElement(Locator.name("quf_date"), "4/25/2014");
        setFormElement(Locator.name("quf_Test Field"), INITIAL_COL_VAL);
        clickButton("Submit", 0);
        waitForText("Manage Dataset");
        assertTextPresent(INITIAL_COL_VAL);
        click(Locator.linkWithText("Manage Dataset"));
        waitForText("Edit Definition");
        click(Locator.linkWithText("Edit Definition"));
        waitForText("Import Fields");
        clickButton("Import Fields", "WARNING");
        setFormElement(Locator.id("schemaImportBox"), "Property\n" + REPLACEMENT_COL);
        clickButton("Import", 0);
        waitForText(REPLACEMENT_COL);
        clickButton("Save", 0);
        waitForText("View Data");
        click(Locator.linkWithText("View Data"));
        //waitForText("47");
        waitForText("No data to show.");
        assertTextPresent(REPLACEMENT_COL);
        checkExpectedErrors(1);
    }
}
