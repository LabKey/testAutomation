/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.ListHelper;

import java.io.File;

/**
 * User: Mark Igra
 * Date: Jun 7, 2007
 * Time: 5:40:36 PM
 */
public class StudyExtraTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "ProtocolVerifyProject";
    protected static final String FOLDER_NAME = "My Folder";
    protected static final String TEST_RUN1 = "FirstRun";
    private static final String STUDY_FOLDER = "VaccineStudy";

    protected void doTestSteps()
    {
        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "None", null);

        addWebPart("Vaccine Study Protocols");
        clickNavButton("New Protocol");

        while(isTextPresent("Loading"))
            sleep(500);

        waitForElement(Locator.inputByLabel("Protocol Name", 1), defaultWaitForPage);
        setFormElement(Locator.inputByLabel("Protocol Name", 1), STUDY_FOLDER);
        setFormElement(Locator.inputByLabel("Investigator", 3), "My Investigator");
        setFormElement(Locator.inputByLabel("Grant", 1), "My Grant");
        setFormElement(Locator.inputByLabel("Species", 3), "Rabbit");
        selenium.fireEvent(Locator.raw("//div[contains(text(), 'Click to edit description')]/..").toString(), "focus");
        setFormElement("protocolDescription", "This is a very important protocol");

        clickNavButton("Save", 0);
        //This is done async so need to sleep a bit...
        int n = 1;
        while (!isTextPresent("Revision 1 saved successfully") && n++ < 20)
            sleep(500);
        assertTextPresent("Revision 1 saved successfully");

        setText("//table[@id='ImmunogenGrid']/tbody/tr[2]/td[2]/input", "Immunogen1");
        //Make sure that Immunization schedule updated
        assertTextPresent("Immunogen1|Adjuvant1");
        setText("//table[@id='ImmunogenGrid']/tbody/tr[4]/td[2]/input", "Immunogen3");
        selenium.select("//table[@id='ImmunogenGrid']/tbody/tr[4]/td[3]/select", "label=Fowlpox");
        setText("//table[@id='ImmunogenGrid']/tbody/tr[4]/td[4]/input", "1.9e8 Ad vg");
        selenium.select("//table[@id='ImmunogenGrid']/tbody/tr[4]/td[5]/select", "label=Intramuscular (IM)");

        selenium.answerOnNextPrompt("New Gene");
        selenium.select("//table[@id='AntigenGrid3']/tbody/tr[2]/td[2]/select", "label=<Add New>");
        assertEquals("Enter new value.", selenium.getPrompt());
        selenium.select("//table[@id='AntigenGrid3']/tbody/tr[2]/td[3]/select", "label=Clade C");
        selenium.type("//table[@id='ImmunizationGrid']/tbody/tr[3]/td[3]/input", "1");
        selenium.type("//table[@id='ImmunizationGrid']/tbody/tr[4]/td[3]/input", "2");
        selenium.type("//table[@id='ImmunizationGrid']/tbody/tr[5]/td[2]/input", "Vaccine2");
        selenium.type("//table[@id='ImmunizationGrid']/tbody/tr[5]/td[3]/input", "3");
        selenium.click("//div[contains(text(), '(none)')]");
        selenium.click("//label[text()='Immunogen3']/../input");
        selenium.click("//label[text()='Adjuvant1']/../input");
        click(Locator.tagWithText("button", "Done"));
        selenium.click("//div[contains(text(), '(none)')]");
        selenium.click("//label[text()='Immunogen3']/../input");
        click(Locator.tagWithText("button", "Done"));
        selenium.click("//table[@id='AssayGrid']//div[contains(text(), 'Add Timepoint')]");
        selenium.type("timepointName", "Pre-immunization");
        click(Locator.tagWithText("button", "OK"));
        selenium.click("//td/div[text()='Neutralizing Antibodies Panel 1']/../..//input");
        clickNavButton("Finished");

        //Can't simply wait for page load here cause also need to wait for
        //GWT to do its thing.
        n = 1;
        while (!isTextPresent("This is a very important protocol") && n++ < 10)
            sleep(1000);

        assertTextPresent("Immunogen3");
        assertTextPresent("Fowlpox");
        assertTextPresent("Immunogen3|Adjuvant1");
        assertTextPresent("Pre-immunization");

        clickNavButton("Edit");
        n = 1;
        while (!isTextPresent("This is a very important protocol") && n++ < 10)
            sleep(1000);

		selenium.click("//table[@id='AssayGrid']//div[contains(text(), 'Add Timepoint')]");
		selenium.type("timepointCount", "8");
        click(Locator.tagWithText("button", "OK"));
        selenium.click("//td/div[text()='Neutralizing Antibodies Panel 1']/ancestor::tr/td[5]//input");
        clickNavButton("Finished");

        //Can't simply wait for page load here cause also need to wait for
        //GWT to do its thing.
        n = 1;
        while (!isTextPresent("This is a very important protocol") && n++ < 10)
            sleep(1000);

        clickNavButton("Create Study Folder");
        setFormElement("beginDate", "2007-01-01");
        clickNavButton("Next");
        String cohorts = "SubjectId\tCohort\tStartDate\n" +
                "V1\tVaccine\t2007-01-01\n" +
                "P1\tPlacebo\t2007-06-01\n" +
                "P2\tPlacebo\t2007-06-01\n" +
                "V2\tVaccine2\t2007-11-01\n" +
                "V3\tVaccine2\t2007-11-01\n" +
                "V4\tVaccine2\t2007-11-01";

        setFormElement("participantTSV", cohorts);
        clickNavButton("Next");
        String specimens = "Vial Id\tSample Id\tDate\tTimepoint Number\tVolume\tUnits\tSpecimen Type\tderivative_type\tAdditive Type\tSubject Id\n" +
 "V1-0\tV1-0\t2007-01-01\t1.0000\t\t\tBlood\tSerum\t\tV1\n" +
 "P1-0\tP1-0\t2007-06-01\t1.0000\t\t\tBlood\tSerum\t\tP1\n" +
 "P2-0\tP2-0\t2007-06-01\t1.0000\t\t\tBlood\tSerum\t\tP2\n" +
 "V2-0\tV2-0\t2007-11-01\t1.0000\t\t\tBlood\tSerum\t\tV2\n" +
 "V3-0\tV3-0\t2007-11-01\t1.0000\t\t\tBlood\tSerum\t\tV3\n" +
 "V4-0\tV4-0\t2007-11-01\t1.0000\t\t\tBlood\tSerum\t\tV4\n" +
 "V1-8\tV1-8\t2007-01-09\t2.0000\t\t\tBlood\tSerum\t\tV1\n" +
 "P1-8\tP1-8\t2007-06-09\t2.0000\t\t\tBlood\tSerum\t\tP1\n" +
 "P2-8\tP2-8\t2007-11-09\t2.0000\t\t\tBlood\tSerum\t\tP2\n" +
 "V2-8\tV2-8\t2007-11-09\t2.0000\t\t\tBlood\tSerum\t\tV2\n" +
 "V3-8\tV3-8\t2007-11-09\t2.0000\t\t\tBlood\tSerum\t\tV3\n" +
 "V4-8\tV4-8\t2007-11-09\t2.0000\t\t\tBlood\tSerum\t\tV4";
        setFormElement("specimenTSV", specimens);
		clickNavButton("Next");
        clickNavButton("Finish");
        clickAdminMenuItem("Folder", "Management");
        clickLinkContainingText("Folder Settings");
        checkCheckbox(Locator.checkboxByTitle("Experiment"));
        checkCheckbox(Locator.checkboxByTitle("Query"));
        clickNavButton("Update Folder");

        addWebPart("Lists");
        clickLinkWithText("manage lists");

        clickNavButton("Create New List");
        waitForElement(Locator.id("ff_name"), defaultWaitForPage);
		setFormElement("ff_name", "List1");
		clickNavButton("Create List", 0);
        waitForElement(Locator.navButton("Add Field"),30000);
        clickNavButton("Add Field", 0);
        ListHelper.setColumnName(this, 1, "Value");
        clickNavButton("Save", 0);
        waitForElement(Locator.navButton("Done"), 30000);
        clickNavButton("Done");
        clickLinkWithText("view data");
        clickNavButton("Insert New");
        //
		selenium.type("firstInputField", "1");
		selenium.type("quf_Value", "One");
        submit();

/*
        Snapshot Study Data feature has been removed

        clickLinkWithText(STUDY_FOLDER + " Study");
        clickLinkWithText("Manage Study");
        clickNavButton("Snapshot Study Data");
        setFormElement("schemaName", "VerifySnapshot");
        clickNavButton("Create Snapshot");
        assertTextPresent("Snapshot completed successfully");
        clickLinkWithText(STUDY_FOLDER + " Study");
        goToModule("Query");
        ExtHelper.clickExtButton(this, "Schema Administration");
		clickLinkWithText("define new schema");
		setFormElement("userSchemaName", "VerifySnapshot");
		setFormElement("dbSchemaName", "verifysnapshot");
        clickNavButton("Create");
		assertTextPresent("VerifySnapshot");
        clickLinkWithText("Query Schema Browser");
        selectSchema("VerifySnapshot");
		assertTrue(isQueryPresent("VerifySnapshot", "List1", 3000) || isQueryPresent("VerifySnapshot", "list1"));
        if (isQueryPresent("VerifySnapshot", "Subjects"))
            viewQueryData("VerifySnapshot", "Subjects");
        else if (isQueryPresent("VerifySnapshot", "subjects"))
            viewQueryData("VerifySnapshot", "subjects");
        else
            fail("Missing subjects table");
        assertTextPresent("Vaccine2");

       */

		clickLinkWithText(STUDY_FOLDER + " Study");
        setupPipeline(PROJECT_NAME);
        defineAssay(PROJECT_NAME);
        uploadRun();

        clickLinkContainingText(TEST_RUN1);
        selenium.click(".toggle");
        clickNavButton("Copy to Study");
        clickNavButton("Next");
        clickNavButton("Copy to Study");
        clickLinkContainingText(STUDY_FOLDER + " Study");

        addWebPart("Datasets");
        clickLinkWithText("TestAssay1");
        assertTextPresent("P1");
        assertTextPresent("V3");
        assertTextPresent("V4-8");

/*
        clickLinkContainingText("Manage Study");

        //Resnapshot the data & pick up the new table
        clickNavButton("Snapshot Study Data");
        clickNavButton("Create Snapshot");
        clickLinkWithText(STUDY_FOLDER +" Study");

        //Now refresh the schema metadata from the server & make sure we pick up new table
        goToModule("Query");
        ExtHelper.clickExtButton(this, "Schema Administration");
        clickLinkWithText("reload");
        assertTextPresent("Schema VerifySnapshot was reloaded successfully.");
        clickLinkWithText("Query Schema Browser");
        selectSchema("VerifySnapshot");
        if (isQueryPresent("VerifySnapshot", "TestAssay1"))
            viewQueryData("VerifySnapshot", "TestAssay1");
        else if (isQueryPresent("VerifySnapshot", "testassay1"))
            viewQueryData("VerifySnapshot", "testassay1");
        else
            fail("TestAssay1 table not present");
*/

        clickLinkWithText(STUDY_FOLDER + " Study");
        clickLinkWithText("Study Navigator");
        assertTextPresent("Day 12");
        clickTab("Manage");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Create New Dataset");
        setFormElement("typeName", "Simple");
        clickNavButton("Next");
        waitForElement(Locator.raw("ff_name0"), WAIT_FOR_JAVASCRIPT);
        ListHelper.setColumnName(this, 0, "Value");
        clickNavButton("Save");
        waitForElement(Locator.navButton("View Data"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("View Data");
        clickNavButton("Import Data");
        ListHelper.submitTsvData(this, "participantid\tDate\tValue\treplace\nP1\t2/1/2007\tHello\nPnew\t11/17/2007\tGoodbye");

        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Day");
        CustomizeViewsHelper.applyCustomView(this);
        assertTextPresent("-120");
        assertTextPresent("320");
        clickLinkWithText(STUDY_FOLDER + " Study");
        clickLinkWithText("Study Navigator");
        assertTextPresent("Day 320");
        clickTab("Manage");
        clickLinkWithText("Study Schedule");
        clickLinkWithText("Manage Timepoints");
        setFormElement("startDate", "2007-11-01");
        submit();
        clickLinkWithText(STUDY_FOLDER + " Study");
        clickLinkWithText("Study Navigator");
        //Make sure our guy picked up the new study start date
        assertTextPresent("Day 16");
        clickLinkWithText(STUDY_FOLDER + " Study");
        clickLinkWithText("Subjects");
        clickNavButton("Import Data");
        ListHelper.submitTsvData(this, "participantid\tDate\tCohort\tStartDate\nPnew\t11/7/2007\tPlacebo\t11/7/2007");
        clickLinkWithText(STUDY_FOLDER + " Study");
        clickLinkWithText("Study Navigator");
        //Make sure our guy picked up the his personal start date
        assertTextPresent("Day 10");
    }

    protected static final String TEST_ASSAY = "TestAssay1";
    protected static final String TEST_ASSAY_DESC = "Description for assay 1";
    protected static final String TEST_ASSAY_SET_PROP_EDIT = "NewTargetStudy";
    protected static final String TEST_ASSAY_SET_PROP_NAME = "testAssaySetProp";
    protected static final int TEST_ASSAY_SET_PREDEFINED_PROP_COUNT = 2;
    protected static final int TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT = 4;
    protected static final String[] TEST_ASSAY_DATA_PROP_NAMES = {"Value" };
    protected static final ListHelper.ListColumnType[] TEST_ASSAY_DATA_PROP_TYPES = { ListHelper.ListColumnType.Integer };
    // protected final static int WAIT_FOR_JAVASCRIPT = 5000;  uncomment to override base class

    /**
     * Sets up the data pipeline for the specified project. This can be called from any page.
     * @param project name of project for which the pipeline should be setup
     */
    protected void setupPipeline(String project)
    {
        log("Setting up data pipeline for project " + project);
        clickLinkWithText(project);
        addWebPart("Data Pipeline");
        clickNavButton("Setup");
        File dir = getTestTempDir();
        dir.mkdirs();

        setPipelineRoot(dir.getAbsolutePath());

        //make sure it was set
        assertTextPresent("The pipeline root was set to '" + dir.getAbsolutePath() + "'");
    } //setupPipeline

    /**
     * Defines an test assay at the project level for the security-related tests
     */
    protected void defineAssay(String projectName)
    {
        log("Defining a test assay at the project level");
        //define a new assay at the project level
        //the pipeline must already be setup
        clickLinkWithText(projectName);
        addWebPart("Assay List");

        //copied from old test
        clickNavButton("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "General");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        selenium.type("//input[@id='AssayDesignerName']", TEST_ASSAY);
        selenium.type("//textarea[@id='AssayDesignerDescription']", TEST_ASSAY_DESC);

        for (int i = TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT; i < TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + TEST_ASSAY_DATA_PROP_TYPES.length; i++)
        {
            selenium.click(getPropertyXPath("Data Fields") + Locator.navButton("Add Field").getPath());
            ListHelper.setColumnName(this, getPropertyXPath("Data Fields"), i, TEST_ASSAY_DATA_PROP_NAMES[i - TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT]);
            ListHelper.setColumnLabel( this, getPropertyXPath("Data Fields"), i, TEST_ASSAY_DATA_PROP_NAMES[i - TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT]);
            ListHelper.setColumnType(this, getPropertyXPath("Data Fields"), i, TEST_ASSAY_DATA_PROP_TYPES[i - TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT]);
        }

        sleep(1000);
        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);

    } //defineAssay()

    /**
     * Generates the text that appears in the target study drop-down for a given study name
     * @param studyName name of the target study
     * @return formatted string of what appears in the target study drop-down
     */
    protected String getTargetStudyOptionText(String projectName, String folderName, String studyName)
    {
        //the format used in the drop down is:
        // /<project>/<studies>/<study1> (<study> Study)
        return "/" + projectName + "/" + folderName + "/" +
                    studyName + " (" + studyName + " Study)";
    } //getTargetStudyOptionText()

    protected static final String TEST_RUN1_COMMENTS = "First comments";
    protected static final String TEST_RUN1_DATA1 = "specimenID\tparticipantID\tvisitID\tDate\tValue\n" +
            "V1-8\tV1\t\t2007-11-13\t1\n" +
            "P1-8\tP1\t\t2007-11-13\t2\n" +
            "P2-8\tP2\t\t2007-11-13\t3\n" +
            "V2-8\tV2\t\t2007-11-13\t4\n" +
            "V3-8\tV3\t\t2007-11-13\t5\n" +
            "V4-8\tV4\t\t2007-11-13\t6";

    protected void uploadRun()
    {
        clickLinkWithText("Assay List");
        clickLinkWithText(TEST_ASSAY);

        clickNavButton("Import Data");
        selenium.select("//select[@name='targetStudy']", getTargetStudyOptionText(PROJECT_NAME, FOLDER_NAME, STUDY_FOLDER));
        click(Locator.radioButtonByNameAndValue("participantVisitResolver", "SampleInfo"));
        clickNavButton("Next");


        log("Run properties and data");
        selenium.type("name", TEST_RUN1);
		selenium.type("comments", TEST_RUN1_COMMENTS);
        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA1);
        clickNavButton("Save and Finish");
        // reenable the following lines when we've moved to strict type checking of the incoming file.  For now, we're
        // flexible and only error if required columns are missing.
    }

    protected void doCleanup() throws Exception
    {
        if (isLinkPresentContainingText(PROJECT_NAME))
        {
            deleteProject(PROJECT_NAME);
        }
    }

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }
}
