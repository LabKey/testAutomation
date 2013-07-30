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

import org.junit.experimental.categories.Category;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Study;
import org.labkey.test.util.ListHelper;

import java.io.File;

/**
 * User: Mark Igra
 * Date: Jun 7, 2007
 * Time: 5:40:36 PM
 */
@Category({DailyA.class, Study.class})
public class VaccineProtocolTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "ProtocolVerifyProject";
    protected static final String FOLDER_NAME = "My Folder";
    protected static final String TEST_RUN1 = "FirstRun";
    private static final String STUDY_FOLDER = "VaccineStudy";
    private static final String LIST_NAME = "List1";

    protected void doTestSteps()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "None", null);

        populateStudyDesignLookups();

        clickFolder(FOLDER_NAME);
        addWebPart("Vaccine Study Protocols");
        clickButton("New Protocol");

        waitForTextToDisappear("Loading", WAIT_FOR_JAVASCRIPT);        

        waitForElement(Locator.inputByLabel("Protocol Name", 1), defaultWaitForPage);
        setFormElement(Locator.inputByLabel("Protocol Name", 1), STUDY_FOLDER);
        setFormElement(Locator.inputByLabel("Investigator", 3), "My Investigator");
        setFormElement(Locator.inputByLabel("Grant", 1), "My Grant");
        setFormElement(Locator.inputByLabel("Species", 3), "Rabbit");
        selenium.fireEvent(Locator.xpath("//div[contains(text(), 'Click to edit description')]/..").toString(), "focus");
        setFormElement("protocolDescription", "This is a very important protocol");

        clickButton("Finished");
        waitForText("No assays have been scheduled.");
        clickButton("Edit");
        waitForText("Enter vaccine information in the grids below.");

        // set the initial study design information to match the previous defaults, prior to 13.3 changes to remove all default values
        setText("//table[@id='ImmunogenGrid']/tbody/tr[2]/td[2]/input", "Cp1");
        selenium.select("//table[@id='ImmunogenGrid']/tbody/tr[2]/td[3]/select", "label=Canarypox");
        setText("//table[@id='ImmunogenGrid']/tbody/tr[2]/td[4]/input", "1.5e10 Ad vg");
        selenium.select("//table[@id='ImmunogenGrid']/tbody/tr[2]/td[5]/select", "label=Intramuscular (IM)");
        setText("//table[@id='ImmunogenGrid']/tbody/tr[3]/td[2]/input", "gp120");
        selenium.select("//table[@id='ImmunogenGrid']/tbody/tr[3]/td[3]/select", "label=Subunit Protein");
        setText("//table[@id='ImmunogenGrid']/tbody/tr[3]/td[4]/input", "1.6e8 Ad vg");
        selenium.select("//table[@id='ImmunogenGrid']/tbody/tr[3]/td[5]/select", "label=Intramuscular (IM)");
        setText("//table[@id='AdjuvantGrid']/tbody/tr[2]/td[2]/input", "Adjuvant1");
        setText("//table[@id='AdjuvantGrid']/tbody/tr[3]/td[2]/input", "Adjuvant2");
        setText("//table[@id='ImmunizationGrid']/tbody/tr[3]/td[2]/input", "Vaccine");
        setText("//table[@id='ImmunizationGrid']/tbody/tr[3]/td[3]/input", "30");
        setText("//table[@id='ImmunizationGrid']/tbody/tr[4]/td[2]/input", "Placebo");
        setText("//table[@id='ImmunizationGrid']/tbody/tr[4]/td[3]/input", "30");
        selenium.click("//table[@id='ImmunizationGrid']//div[contains(text(), 'Add Timepoint')]");
        selenium.type("timepointCount", "0");
        click(Locator.tagWithText("button", "OK"));
        selenium.click("//table[@id='ImmunizationGrid']//div[contains(text(), 'Add Timepoint')]");
        selenium.type("timepointCount", "28");
        click(Locator.tagWithText("button", "OK"));
        selenium.click("//div[contains(text(), '(none)')]");
        selenium.click("//label[text()='Cp1']/../input");
        selenium.click("//label[text()='Adjuvant1']/../input");
        click(Locator.tagWithText("button", "Done"));
        selenium.click("//div[contains(text(), '(none)')]");
        selenium.click("//label[text()='gp120']/../input");
        selenium.click("//label[text()='Adjuvant1']/../input");
        click(Locator.tagWithText("button", "Done"));
        selenium.click("//div[contains(text(), '(none)')]");
        selenium.click("//label[text()='Adjuvant1']/../input");
        click(Locator.tagWithText("button", "Done"));
        selenium.click("//div[contains(text(), '(none)')]");
        selenium.click("//label[text()='Adjuvant1']/../input");
        click(Locator.tagWithText("button", "Done"));
        selenium.select("//table[@id='AssayGrid']/tbody/tr[3]/td[2]/select", "label=ELISPOT");
        selenium.select("//table[@id='AssayGrid']/tbody/tr[3]/td[3]/select", "label=Schmitz");
        selenium.select("//table[@id='AssayGrid']/tbody/tr[4]/td[2]/select", "label=Neutralizing Antibodies Panel 1");
        selenium.select("//table[@id='AssayGrid']/tbody/tr[4]/td[3]/select", "label=Montefiori");
        selenium.select("//table[@id='AssayGrid']/tbody/tr[5]/td[2]/select", "label=ICS");
        selenium.select("//table[@id='AssayGrid']/tbody/tr[5]/td[3]/select", "label=McElrath");
        selenium.select("//table[@id='AssayGrid']/tbody/tr[6]/td[2]/select", "label=ELISA");
        selenium.select("//table[@id='AssayGrid']/tbody/tr[6]/td[3]/select", "label=Lab 1");

        // change study design information to test GWT UI components
        setText("//table[@id='ImmunogenGrid']/tbody/tr[2]/td[2]/input", "Immunogen1");
        assertTextPresent("Immunogen1|Adjuvant1"); //Make sure that Immunization schedule updated
        setText("//table[@id='ImmunogenGrid']/tbody/tr[4]/td[2]/input", "Immunogen3");
        selenium.select("//table[@id='ImmunogenGrid']/tbody/tr[4]/td[3]/select", "label=Fowlpox");
        setText("//table[@id='ImmunogenGrid']/tbody/tr[4]/td[4]/input", "1.9e8 Ad vg");
        selenium.select("//table[@id='ImmunogenGrid']/tbody/tr[4]/td[5]/select", "label=Intramuscular (IM)");

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
        clickButton("Finished");

        waitForText("This is a very important protocol",WAIT_FOR_JAVASCRIPT);

        assertTextPresent("Immunogen3");
        assertTextPresent("Fowlpox");
        assertTextPresent("Immunogen3|Adjuvant1");
        assertTextPresent("Pre-immunization");

        clickButton("Edit");
        waitForText("This is a very important protocol",WAIT_FOR_JAVASCRIPT);

		selenium.click("//table[@id='AssayGrid']//div[contains(text(), 'Add Timepoint')]");
		selenium.type("timepointCount", "8");
        click(Locator.tagWithText("button", "OK"));
        selenium.click("//td/div[text()='Neutralizing Antibodies Panel 1']/ancestor::tr/td[5]//input");
        clickButton("Finished");

        waitForText("This is a very important protocol",WAIT_FOR_JAVASCRIPT);

        clickButton("Create Study Folder");
        setFormElement("beginDate", "2007-01-01");
        clickButton("Next");
        String cohorts = "SubjectId\tCohort\tStartDate\n" +
                "V1\tVaccine\t2007-01-01\n" +
                "P1\tPlacebo\t2007-06-01\n" +
                "P2\tPlacebo\t2007-06-01\n" +
                "V2\tVaccine2\t2007-11-01\n" +
                "V3\tVaccine2\t2007-11-01\n" +
                "V4\tVaccine2\t2007-11-01";

        setFormElement("participantTSV", cohorts);
        clickButton("Next");
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
		clickButton("Next");
        clickButton("Finish");
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkCheckbox(Locator.checkboxByTitle("Experiment"));
        checkCheckbox(Locator.checkboxByTitle("Query"));
        clickButton("Update Folder");

        addWebPart("Lists");
        clickAndWait(Locator.linkWithText("manage lists"));

        ListHelper.ListColumn valueColumn = new ListHelper.ListColumn("Value", "Value", ListHelper.ListColumnType.String, "Vaccine Value");
        _listHelper.createList(STUDY_FOLDER, LIST_NAME, ListHelper.ListColumnType.Integer, "Key", valueColumn);
        clickButton("Done");

        clickAndWait(Locator.linkWithText(LIST_NAME));
        clickButton("Insert New");
		selenium.type("quf_Key", "1");
		selenium.type("quf_Value", "One");
        submit();

		clickAndWait(Locator.linkWithText(STUDY_FOLDER + " Study"));
        setupPipeline(PROJECT_NAME);
        defineAssay(PROJECT_NAME);
        uploadRun();

        clickAndWait(Locator.linkContainingText(TEST_RUN1));
        selenium.click(".toggle");
        clickButton("Copy to Study");
        clickButton("Next");
        clickButton("Copy to Study");
        clickAndWait(Locator.linkContainingText(STUDY_FOLDER + " Study"));

        addWebPart("Datasets");
        clickAndWait(Locator.linkWithText("TestAssay1"));
        assertTextPresent("P1");
        assertTextPresent("V3");
        assertTextPresent("V4-8");

/*
        clickAndWait(Locator.linkContainingText("Manage Study"));

        //Resnapshot the data & pick up the new table
        clickButton("Snapshot Study Data");
        clickButton("Create Snapshot");
        clickAndWait(Locator.linkWithText(STUDY_FOLDER +" Study"));

        //Now refresh the schema metadata from the server & make sure we pick up new table
        goToModule("Query");
        _extHelper.clickExtButton(this, "Schema Administration");
        clickAndWait(Locator.linkWithText("reload"));
        assertTextPresent("Schema VerifySnapshot was reloaded successfully.");
        clickAndWait(Locator.linkWithText("Query Schema Browser"));
        selectSchema("VerifySnapshot");
        if (isQueryPresent("VerifySnapshot", "TestAssay1"))
            viewQueryData("VerifySnapshot", "TestAssay1");
        else if (isQueryPresent("VerifySnapshot", "testassay1"))
            viewQueryData("VerifySnapshot", "testassay1");
        else
            Assert.fail("TestAssay1 table not present");
*/

        clickAndWait(Locator.linkWithText(STUDY_FOLDER + " Study"));
        clickAndWait(Locator.linkWithText("Study Navigator"));
        assertTextPresent("Day 12");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText("Create New Dataset"));
        setFormElement("typeName", "Simple");
        clickButton("Next");
        waitForElement(Locator.name("ff_name0"), WAIT_FOR_JAVASCRIPT);
        _listHelper.setColumnName(0, "Value");
        clickButton("Save");
        waitForElement(Locator.navButton("View Data"), WAIT_FOR_JAVASCRIPT);
        clickButton("View Data");
        clickButton("Import Data");
        _listHelper.submitTsvData("participantid\tDate\tValue\treplace\nP1\t2/1/2007\tHello\nPnew\t11/17/2007\tGoodbye");

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("Day");
        _customizeViewsHelper.applyCustomView();
        assertTextPresent("-120");
        assertTextPresent("320");
        clickAndWait(Locator.linkWithText(STUDY_FOLDER + " Study"));
        clickAndWait(Locator.linkWithText("Study Navigator"));
        assertTextPresent("Day 320");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Timepoints"));
        setFormElement("startDate", "2007-11-01");
        submit();
        clickAndWait(Locator.linkWithText(STUDY_FOLDER + " Study"));
        clickAndWait(Locator.linkWithText("Study Navigator"));
        //Make sure our guy picked up the new study start date
        assertTextPresent("Day 16");
        clickAndWait(Locator.linkWithText(STUDY_FOLDER + " Study"));
        clickAndWait(Locator.linkWithText("Subjects"));
        clickButton("Import Data");
        _listHelper.submitTsvData("participantid\tDate\tCohort\tStartDate\nPnew\t11/7/2007\tPlacebo\t11/7/2007");
        clickAndWait(Locator.linkWithText(STUDY_FOLDER + " Study"));
        clickAndWait(Locator.linkWithText("Study Navigator"));
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
        clickProject(project);
        addWebPart("Data Pipeline");
        clickButton("Setup");
        File dir = getTestTempDir();
        dir.mkdirs();

        setPipelineRoot(dir.getAbsolutePath());

        //make sure it was set
        assertTextPresent("The pipeline root was set to '" + dir.getAbsolutePath() + "'");
    } //setupPipeline


    public boolean isFileUploadTest()
    {
        return true;
    }
    /**
     * Defines an test assay at the project level for the security-related tests
     */
    protected void defineAssay(String projectName)
    {
        log("Defining a test assay at the project level");
        //define a new assay at the project level
        //the pipeline must already be setup
        clickProject(projectName);
        addWebPart("Assay List");

        _assayHelper.uploadXarFileAsAssayDesign(getSampledataPath() + "/studyextra/TestAssay1.xar", 1, "TestAssay1.xar");
        goToProjectHome();

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
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY));

        clickButton("Import Data");
        selenium.select("//select[@name='targetStudy']", getTargetStudyOptionText(PROJECT_NAME, FOLDER_NAME, STUDY_FOLDER));
        click(Locator.radioButtonByNameAndValue("participantVisitResolver", "SampleInfo"));
        clickButton("Next");


        log("Run properties and data");
        selenium.type("name", TEST_RUN1);
		selenium.type("comments", TEST_RUN1_COMMENTS);
        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA1);
        clickButton("Save and Finish");
        // reenable the following lines when we've moved to strict type checking of the incoming file.  For now, we're
        // flexible and only error if required columns are missing.
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
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

    private void populateStudyDesignLookups()
    {
        goToProjectHome();

        goToQuery("StudyDesignAssays");
        for (String assay : new String[]{"ELISPOT", "Neutralizing Antibodies Panel 1", "ICS", "ELISA"})
            insertLookupRecord(assay, assay + " Label");

        goToQuery("StudyDesignLabs");
        for (String lab : new String[]{"Schmitz", "Montefiori", "McElrath", "Lab 1"})
            insertLookupRecord(lab, lab + " Label");

        goToQuery("StudyDesignRoutes");
        for (String route : new String[]{"Intramuscular (IM)"})
            insertLookupRecord(route, route + " Label");

        goToQuery("StudyDesignImmunogenTypes");
        for (String immunogenType : new String[]{"Canarypox", "Fowlpox", "Subunit Protein"})
            insertLookupRecord(immunogenType, immunogenType + " Label");

        goToQuery("StudyDesignGenes");
        for (String gene : new String[]{"Gag", "Env"})
            insertLookupRecord(gene, gene + " Label");

        goToQuery("StudyDesignSubTypes");
        for (String subType : new String[]{"Clade B", "Clade C"})
            insertLookupRecord(subType, subType + " Label");
    }

    private void insertLookupRecord(String name, String label)
    {
        clickButton("Insert New");
        if (name != null) setFormElement(Locator.name("quf_Name"), name);
        if (label != null) setFormElement(Locator.name("quf_Label"), label);
        clickButton("Submit");
    }

    private void goToQuery(String queryName)
    {
        goToSchemaBrowser();
        selectQuery("study", queryName);
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
    }
}
