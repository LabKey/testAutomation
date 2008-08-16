/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
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
        while (!isTextPresent("Revision 1 saved successfully") && n++ < 5)
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
        selenium.click("check3");
        selenium.click("check4");
        click(Locator.tagWithText("button", "Done"));
        selenium.click("//div[contains(text(), '(none)')]");
        selenium.click("check8");
        click(Locator.tagWithText("button", "Done"));
        selenium.click("//table[@id='AssayGrid']//div[contains(text(), 'Click to Add Timepoint')]");
        selenium.type("timepointName", "Pre-immunization");
        click(Locator.tagWithText("button", "OK"));
        selenium.click("check12");
        clickNavButton("Finished", 0);

        //Can't simply wait for page load here cause also need to wait for
        //GWT to do its thing.
        waitForPageToLoad(30000);
        n = 1;
        while (!isTextPresent("This is a very important protocol") && n++ < 10)
            sleep(1000);

        assertTextPresent("Immunogen3");
        assertTextPresent("Fowlpox");
        assertTextPresent("Immunogen3|Adjuvant1");
        assertTextPresent("Pre-immunization");

        clickNavButton("Edit", 0);
        waitForPageToLoad(30000);
        n = 1;
        while (!isTextPresent("This is a very important protocol") && n++ < 10)
            sleep(1000);

		selenium.click("//table[@id='AssayGrid']//div[contains(text(), 'Click to Add Timepoint')]");
		selenium.type("timepointCount", "8");
        click(Locator.tagWithText("button", "OK"));
		selenium.click("check8");
        clickNavButton("Finished", 0);

        //Can't simply wait for page load here cause also need to wait for
        //GWT to do its thing.
        waitForPageToLoad(30000);
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
        clickLinkWithText("Customize Folder");
        checkCheckbox(Locator.checkboxByTitle("Experiment", false));
        checkCheckbox(Locator.checkboxByTitle("Query", false));
        clickNavButton("Update Folder");
        addWebPart("Lists");
        clickLinkWithText("manage lists");
        clickNavButton("Create New List");
		setFormElement("ff_name", "List1");
		clickNavButton("Create List");
        clickLinkWithText("edit fields");
        waitForElement(Locator.imageWithSrc("Field.button", true),30000);
        clickNavButton("Add Field", 0);
		selenium.type("ff_name0", "Value");
        clickNavButton("Save");
        waitForElement(Locator.linkWithText("view data"), 30000);
        clickLinkWithText("view data");
        clickNavButton("Insert New");
        //
		selenium.type("firstInputField", "1");
		selenium.type("quf_Value", "One");
        submit();
        clickLinkWithText(STUDY_FOLDER + " Study");
        clickLinkWithText("Manage Study");
        clickNavButton("Snapshot Study Data");
        setFormElement("schemaName", "VerifySnapshot");
        clickNavButton("Create Snapshot");
        assertTextPresent("Snapshot completed successfully");
        clickLinkWithText(STUDY_FOLDER + " Study");
		clickNavButton("Admin", 0);
        selenium.mouseOver("//a[contains(text(),'Go To Module')]");
        waitForElement(Locator.xpath("//a[contains(text(),'Query')]"), 2000);
        selenium.click("//a[contains(text(),'Query')]");
		selenium.waitForPageToLoad("30000");
        clickLinkWithText("Schema Administration");
		clickLinkWithText("Define New Schema");
		setFormElement("userSchemaName", "VerifySnapshot");
		setFormElement("dbSchemaName", "verifysnapshot");
        clickNavButton("Create");
		assertTextPresent("VerifySnapshot");
        clickLinkWithText("Query start page");
        clickLinkWithText("VerifySnapshot");
		assertTrue(isTextPresent("List1") || isTextPresent("list1"));
        if (isLinkPresentWithText("Subjects"))
            clickLinkWithText("Subjects");
        else if (isLinkPresentWithText("subjects"))
            clickLinkWithText("subjects");
        else
            fail("Missing subjects table");
        assertTextPresent("Vaccine2");
		clickLinkWithText(STUDY_FOLDER + " Study");
        setupPipeline(PROJECT_NAME);
        defineAssay(PROJECT_NAME);
        uploadRun();

        clickLinkContainingText(TEST_RUN1);
        selenium.click(".toggle");
        clickNavButton("Copy Selected to Study");
        clickNavButton("Next");
        clickNavButton("Copy to Study");
        clickLinkContainingText(STUDY_FOLDER + " Study");
        clickLinkContainingText("Manage Study");

        //Resnapshot the data & pick up the new table
        clickNavButton("Snapshot Study Data");
        assertTextPresent("TestAssay1");
        clickNavButton("Create Snapshot");
        clickLinkWithText(STUDY_FOLDER +" Study");

        //Now refresh the schema metadata from the server & make sure we pick up new table
        clickNavButton("Admin", 0);
        selenium.mouseOver("//a[contains(text(),'Go To Module')]");
        waitForElement(Locator.xpath("//a[contains(text(),'Query')]"), 2000);
        selenium.click("//a[contains(text(),'Query')]");
        selenium.waitForPageToLoad("30000");

        clickLinkWithText("Schema Administration");
        clickLinkWithText("Reload");
        assertTextPresent("Schema VerifySnapshot was reloaded successfully.");
        clickLinkWithText("Query start page");
        clickLinkWithText("VerifySnapshot");
        if (isTextPresent("TestAssay1"))
            clickLinkWithText("TestAssay1");
        else if (isTextPresent("testassay1"))
            clickLinkWithText("testassay1");
        else
            fail("TestAssay1 table not present");

        assertTextPresent("P1");
        assertTextPresent("V3");


        clickLinkWithText("VaccineStudy Study");
        clickLinkWithText("Study Navigator");
        assertTextPresent("Day 12");
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Create New Dataset");
        setFormElement("typeName", "Simple");
        clickNavButton("Next");
        waitForElement(Locator.raw("ff_name0"), WAIT_FOR_GWT);
        selenium.type("ff_name0", "Value");
        clickNavButton("Save", 0);
        selenium.waitForPageToLoad("30000");
        clickNavButton("Import Data");
        selenium.type("tsv", "participantid\tDate\tValue\treplace\nP1\t2/1/2007\tHello\nPnew\t11/17/2007\tGoodbye");
        submit();
        assertTextPresent("-120");
        assertTextPresent("320");
        clickLinkWithText("VaccineStudy Study");
        clickLinkWithText("Study Navigator");
        assertTextPresent("Day 320");
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Timepoints");
        setFormElement("startDate", "2007-11-01");
        submit();
        clickLinkWithText("VaccineStudy Study");
        clickLinkWithText("Study Navigator");
        //Make sure our guy picked up the new study start date
        assertTextPresent("Day 16");
        clickLinkWithText("VaccineStudy Study");
        clickLinkWithText("Subjects");
        clickNavButton("Import Data");
        setFormElement("tsv", "participantid\tDate\tCohort\tStartDate\nPnew\t11/7/2007\tPlacebo\t11/7/2007");
        submit();
        clickLinkWithText("VaccineStudy Study");
        clickLinkWithText("Study Navigator");
        //Make sure our guy picked up the his personal start date
        assertTextPresent("Day 10");


//        clickLinkWithText("Manage Assays");
//        clickNavButton("Create Assay");

    }

    protected static final String TEST_ASSAY = "TestAssay1";
    protected static final String TEST_ASSAY_DESC = "Description for assay 1";
    protected static final String TEST_ASSAY_SET_PROP_EDIT = "NewTargetStudy";
    protected static final String TEST_ASSAY_SET_PROP_NAME = "testAssaySetProp";
    protected static final int TEST_ASSAY_SET_PREDEFINED_PROP_COUNT = 2;
    protected static final int TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT = 4;
    protected static final String[] TEST_ASSAY_DATA_PROP_NAMES = {"Value" };
    protected static final String[] TEST_ASSAY_DATA_PROP_TYPES = {"Integer" };
    protected final static int WAIT_FOR_GWT = 5000;

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

        setFormElement("path", dir.getAbsolutePath());
        clickNavButton("Set");

        //make sure it was set
        assertTextPresent("The pipeline root was set to '" + dir.getAbsolutePath() + "'.");
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
        clickLinkWithText("Manage Assays");
        clickNavButton("New Assay Design");
        selectOptionByText("providerName", "General");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_GWT);

        selenium.type("//input[@id='AssayDesignerName']", TEST_ASSAY);
        selenium.type("//textarea[@id='AssayDesignerDescription']", TEST_ASSAY_DESC);

        for (int i = TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT; i < TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + TEST_ASSAY_DATA_PROP_TYPES.length; i++)
        {
            selenium.mouseOver(getPropertyXPath("Data Fields") + "//img[contains(@src, 'Add+Field.button')]");
            selenium.mouseDown(getPropertyXPath("Data Fields") + "//img[contains(@src, 'Add+Field.button')]");
            selenium.mouseUp(getPropertyXPath("Data Fields") + "//img[contains(@src, 'Add+Field.button')]");
            selenium.type(getPropertyXPath("Data Fields") + "//input[@id='ff_name" + i + "']", TEST_ASSAY_DATA_PROP_NAMES[i - TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT]);
            selenium.type(getPropertyXPath("Data Fields") + "//input[@id='ff_label" + i + "']", TEST_ASSAY_DATA_PROP_NAMES[i - TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT]);
            selenium.select(getPropertyXPath("Data Fields") + "//select[@id='ff_type" + i + "']", TEST_ASSAY_DATA_PROP_TYPES[i - TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT]);
        }

        sleep(1000);
        clickNavButton("    Save    ", 0);
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

        clickNavButton("Upload Runs");
        selenium.select("//select[@name='targetStudy']", getTargetStudyOptionText(PROJECT_NAME, FOLDER_NAME, STUDY_FOLDER));
        click(Locator.checkboxByNameAndValue("participantVisitResolver", "SampleInfo", true));
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
        deleteProject(PROJECT_NAME);
    }

    public String getAssociatedModuleDirectory()
    {
        return "study";
    }
}
