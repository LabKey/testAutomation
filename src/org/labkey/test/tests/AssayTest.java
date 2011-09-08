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

import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.ListHelper;
import static org.labkey.test.util.ListHelper.ListColumnType;

import java.io.File;

/**
 * User: jeckels
 * Date: Aug 10, 2007
 *
 * Modified by DaveS on 13 Sept 13 2007
 *  Added security-related tests, and refactored the code to run Luminex by itself in a separate project
 */
public class AssayTest extends AbstractAssayTest
{
    protected static final String TEST_ASSAY = "Test" + TRICKY_CHARACTERS + "Assay1";
    protected static final String TEST_ASSAY_DESC = "Description for assay 1";

    protected static final String TEST_ASSAY_SET_PROP_EDIT = "NewTargetStudy";
    protected static final String TEST_ASSAY_SET_PROP_NAME = "testAssaySetProp";
    protected static final int TEST_ASSAY_SET_PREDEFINED_PROP_COUNT = 2;
    protected static final ListColumnType[] TEST_ASSAY_SET_PROP_TYPES = { ListColumnType.Boolean, ListColumnType.Double, ListColumnType.Integer, ListColumnType.DateTime };
    protected static final String[] TEST_ASSAY_SET_PROPERTIES = { "false", "100.0", "200", "2001-10-10" };
    protected static final String TEST_ASSAY_RUN_PROP_NAME = "testAssayRunProp";
    protected static final int TEST_ASSAY_RUN_PREDEFINED_PROP_COUNT = 0;
    protected static final ListColumnType[] TEST_ASSAY_RUN_PROP_TYPES = { ListColumnType.String, ListColumnType.Boolean, ListColumnType.Double, ListColumnType.Integer, ListColumnType.DateTime };
    protected static final String TEST_ASSAY_RUN_PROP1 = "TestRunProp";
    protected static final String TEST_ASSAY_DATA_PROP_NAME = "testAssayDataProp";
    protected static final String TEST_ASSAY_DATA_ALIASED_PROP_NAME = "testAssayAliasedData";
    protected static final String ALIASED_DATA = "aliasedData";
    public static final int TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT = 4;
    protected static final ListColumnType[] TEST_ASSAY_DATA_PROP_TYPES = { ListColumnType.Boolean, ListColumnType.Integer, ListColumnType.DateTime, ListColumnType.String };
    protected static final String TEST_RUN1 = "FirstRun";
    protected static final String TEST_RUN1_COMMENTS = "First comments";
    protected static final String TEST_RUN1_DATA1 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "20\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\n" +
            "AAA07XK5-05\t\t\ttrue\t20\t2000-01-01\n" +
            "AAA07XMC-02\t\t\ttrue\t19\t2000-02-02\n" +
            "AAA07XMC-04\t\t\ttrue\t18\t2000-03-03\n" +
            "AAA07XSF-02\t\t\tfalse\t17\t2000-04-04\n" +
            "AssayTestControl1\te\t5\tfalse\t16\t2000-05-05\n" +
            "AssayTestControl2\tf\tg\tfalse\t15\t2000-06-06";
    protected static final String TEST_RUN1_DATA2 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "4\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\n" +
            "AAA07XK5-05\t\ttrue\t20\t2000-01-01\n" +
            "AAA07XMC-02\t\t\ttrue\t19\t2000-02-02\n" +
            "AAA07XMC-04\t\t\ttrue\t18\t2000-03-03\n" +
            "AAA07XSF-02\t\t\tfalse\t17\t2000-04-04\n" +
            "AssayTestControl1\te\t5\tfalse\t16\t2000-05-05\n" +
            "AssayTestControl2\tf\tg\tfalse\t15\t2000-06-06";
    protected static final String TEST_RUN1_DATA3 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "4\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\n" +
            "AAA07XK5-05\t\t\ttrue\t20\t\n" +
            "AAA07XMC-02\t\t\ttrue\t19\t\n" +
            "AAA07XMC-04\t\t\ttrue\t18\t";
    protected static final String TEST_RUN1_DATA4 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "4\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\t" + TEST_ASSAY_DATA_ALIASED_PROP_NAME + "\n" +
            "AAA07XK5-05\t\t\ttrue\t\t2000-01-01\t"+ALIASED_DATA+"\n" +
            "AAA07XMC-02\t\t\ttrue\t\t2000-02-02\t"+ALIASED_DATA+"\n" +
            "AAA07XMC-04\t\t\ttrue\t\t2000-03-03\t"+ALIASED_DATA+"\n" +
            "AAA07XSF-02\t\t\tfalse\t\t2000-04-04\t"+ALIASED_DATA+"\n" +
            "AssayTestControl1\te\t5\tfalse\t\t2000-05-05\t"+ALIASED_DATA+"\n" +
            "AssayTestControl2\tf\t6\tfalse\t\t2000-06-06\t"+ALIASED_DATA;
    protected static final String TEST_RUN2 = "SecondRun";
    protected static final String TEST_RUN2_COMMENTS = "Second comments";
    protected static final String TEST_RUN2_DATA1 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "4\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\n" +
            "BAQ00051-09\tg\t7\ttrue\t20\t2000-01-01\n" +
            "BAQ00051-08\th\t8\ttrue\t19\t2000-02-02\n" +
            "BAQ00051-11\ti\t9\ttrue\t18\t2000-03-03\n";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PRJ_SECURITY;
    }

    /**
     * Cleanup entry point.
     */
    protected void doCleanup()
    {
        try
        {
            deleteProject(TEST_ASSAY_PRJ_SECURITY); //should also delete the groups

            //delete user accounts
            deleteUser(TEST_ASSAY_USR_PI1);
            deleteUser(TEST_ASSAY_USR_TECH1);
            deleteDir(getTestTempDir());
        }
        catch(Throwable T) {/* ignore */}
    } //doCleanup()

    /**
     *  Performs the Assay security test
     *  This test creates a project with a folder hierarchy with multiple groups and users;
     *  defines an Assay at the project level; uploads run data as a labtech; publishes
     *  as a PI, and tests to make sure that security is properly enforced
     */
    protected void runUITests()
    {
        log("Starting Assay security scenario tests");
        setupEnvironment();
        setupPipeline(TEST_ASSAY_PRJ_SECURITY);
        SpecimenImporter importer = new SpecimenImporter(getTestTempDir(), new File(getLabKeyRoot(), "/sampledata/study/specimens/sample_a.specimens"), new File(getTestTempDir(), "specimensSubDir"), TEST_ASSAY_FLDR_STUDY2, 1);
        importer.importAndWaitForComplete();
        defineAssay();
        uploadRuns(TEST_ASSAY_FLDR_LAB1, TEST_ASSAY_USR_TECH1);
        editResults();
        publishData();
        editAssay();
        viewCrossFolderData();
    }

    private void editResults()
    {
        // Verify that the results aren't editable by default
        clickLinkWithText(TEST_ASSAY_FLDR_LAB1);
        clickLinkWithText(TEST_ASSAY);
        clickLinkWithText("view results");
        assertLinkNotPresentWithText("edit");
        assertNavButtonNotPresent("Delete");

        // Edit the design to make them editable
        click(Locator.linkWithText("manage assay design"));
        selenium.chooseOkOnNextConfirmation();
        clickLinkWithText("edit assay design");
        assertConfirmation("This assay is defined in the /Assay Security Test folder. Would you still like to edit it?");
        waitForElement(Locator.xpath("//span[@id='id_editable_results_properties']"), WAIT_FOR_JAVASCRIPT);
        checkCheckbox(Locator.xpath("//span[@id='id_editable_results_properties']/input"));
        clickNavButton("Save & Close");

        // Try an edit
        clickLinkWithText(TEST_ASSAY_FLDR_LAB1);
        clickLinkWithText(TEST_ASSAY);
        clickLinkWithText("view results");
        clickLinkWithText("edit");
        setText("quf_SpecimenID", "EditedSpecimenID");
        setText("quf_VisitID", "601.5");
        setText("quf_testAssayDataProp5", "a");
        clickNavButton("Submit");
        assertTextPresent("Could not convert value: a");
        setText("quf_testAssayDataProp5", "514801");
        clickNavButton("Submit");
        assertTextPresent("EditedSpecimenID", "601.5", "514801");

        // Try a delete
        checkCheckbox(".select");
        selenium.chooseOkOnNextConfirmation();
        clickNavButton("Delete");
        assertConfirmation("Are you sure you want to delete the selected row?");

        // Verify that the edit was audited
        goToModule("Query");
        selectQuery("auditLog", "ExperimentAuditEvent");
        waitForElement(Locator.linkWithText("view data"), WAIT_FOR_JAVASCRIPT);
        clickLinkWithText("view data");
        assertTextPresent("Data row, id ", ", edited.", 
                "Specimen ID changed from 'AAA07XK5-05' to 'EditedSpecimenID'",
                "Visit ID changed from '601.0' to '601.5",
                "testAssayDataProp5 changed from blank to '514801'");
        assertTextPresent("Deleted data row.");

        clickLinkWithText(TEST_ASSAY_PRJ_SECURITY);
    }

    /**
     * Defines an test assay at the project level for the security-related tests
     */
    private void defineAssay()
    {
        log("Defining a test assay at the project level");
        //define a new assay at the project level
        //the pipeline must already be setup
        clickLinkWithText(TEST_ASSAY_PRJ_SECURITY);
        addWebPart("Assay List");

        //copied from old test
        clickNavButton("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "General");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        selenium.type("//input[@id='AssayDesignerName']", TEST_ASSAY);
        selenium.type("//textarea[@id='AssayDesignerDescription']", TEST_ASSAY_DESC);

        for (int i = TEST_ASSAY_SET_PREDEFINED_PROP_COUNT; i < TEST_ASSAY_SET_PREDEFINED_PROP_COUNT + TEST_ASSAY_SET_PROP_TYPES.length; i++)
        {
            addField("Batch Fields", i, TEST_ASSAY_SET_PROP_NAME + i, TEST_ASSAY_SET_PROP_NAME + i, TEST_ASSAY_SET_PROP_TYPES[i - TEST_ASSAY_SET_PREDEFINED_PROP_COUNT]);
        }

        for (int i = TEST_ASSAY_RUN_PREDEFINED_PROP_COUNT; i < TEST_ASSAY_RUN_PREDEFINED_PROP_COUNT + TEST_ASSAY_RUN_PROP_TYPES.length; i++)
        {
            addField("Run Fields", i, TEST_ASSAY_RUN_PROP_NAME + i, TEST_ASSAY_RUN_PROP_NAME + i, TEST_ASSAY_RUN_PROP_TYPES[i - TEST_ASSAY_RUN_PREDEFINED_PROP_COUNT]);
        }

        for (int i = TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT; i < TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + TEST_ASSAY_DATA_PROP_TYPES.length; i++)
        {
            addField("Data Fields", i, TEST_ASSAY_DATA_PROP_NAME + i, TEST_ASSAY_DATA_PROP_NAME + i, TEST_ASSAY_DATA_PROP_TYPES[i - TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT]);
        }

        // Set some to required
        setRequired("Batch Fields", TEST_ASSAY_SET_PREDEFINED_PROP_COUNT);
        setRequired("Batch Fields", TEST_ASSAY_SET_PREDEFINED_PROP_COUNT+1);
        setRequired("Run Fields", 0);
        setRequired("Data Fields", 0);
        setRequired("Data Fields", TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + 2);

        // import aliases
        ListHelper.clickRow(this, getPropertyXPath("Data Fields"), TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + 3);
        click(Locator.xpath(getPropertyXPath("Data Fields") + "//span[contains(@class,'x-tab-strip-text') and text()='Advanced']"));
        waitForElement(Locator.xpath(getPropertyXPath("Data Fields") + "//td/input[@id='importAliases']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath(getPropertyXPath("Data Fields") + "//td/input[@id='importAliases']"), TEST_ASSAY_DATA_ALIASED_PROP_NAME);

        sleep(1000);
        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);

    } //defineAssay()

    private void setRequired(String where, int index)
    {
        String prefix = getPropertyXPath(where);
        ListHelper.clickRow(this, prefix, index);
        click(Locator.xpath(prefix + "//span[contains(@class,'x-tab-strip-text') and text()='Validators']"));
        waitAndClick(WAIT_FOR_JAVASCRIPT, Locator.xpath(prefix + "//span/input[@name='required']"), 0);
    }


    /**
     * Generates the text that appears in the target study drop-down for a given study name
     * @param studyName name of the target study
     * @return formatted string of what appears in the target study drop-down
     */
    private String getTargetStudyOptionText(String studyName)
    {
        //the format used in the drop down is:
        // /<project>/<studies>/<study1> (<study> Study)
        return "/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" +
                    studyName + " (" + studyName + " Study)";
    } //getTargetStudyOptionText()

    /**
     * Uploads run data for the centrally defined Assay while impersonating a labtech-style user
     * @param folder    name of the folder into which we should upload
     * @param asUser    the user to impersonate before uploading
     */
    private void uploadRuns(String folder, String asUser)
    {
        log("Uploading runs into folder " + folder + " as user " + asUser);
        impersonate(asUser);
        clickLinkWithText(TEST_ASSAY_PRJ_SECURITY);
        clickLinkWithText(folder);

        clickLinkWithText("Assay List");
        clickLinkWithText(TEST_ASSAY);

        clickNavButton("Import Data");
        assertTextPresent(TEST_ASSAY_SET_PROP_NAME + "3");

        log("Batch properties");
        clickNavButton("Next");
        assertTextPresent(TEST_ASSAY_SET_PROP_NAME + (TEST_ASSAY_SET_PREDEFINED_PROP_COUNT + 1) + " is required and must be of type Number (Double).");
        setFormElement(TEST_ASSAY_SET_PROP_NAME + (TEST_ASSAY_SET_PREDEFINED_PROP_COUNT + 1), "Bad Test");
        setFormElement(TEST_ASSAY_SET_PROP_NAME + (TEST_ASSAY_SET_PREDEFINED_PROP_COUNT + 2), "Bad Test");
        setFormElement(TEST_ASSAY_SET_PROP_NAME + (TEST_ASSAY_SET_PREDEFINED_PROP_COUNT + 3), "Bad Test");
        clickNavButton("Next");
        assertTextPresent(TEST_ASSAY_SET_PROP_NAME + (TEST_ASSAY_SET_PREDEFINED_PROP_COUNT + 1) + " must be of type Number (Double).");
        assertTextPresent(TEST_ASSAY_SET_PROP_NAME + (TEST_ASSAY_SET_PREDEFINED_PROP_COUNT + 2) + " must be of type Integer.");
        assertTextPresent(TEST_ASSAY_SET_PROP_NAME + (TEST_ASSAY_SET_PREDEFINED_PROP_COUNT + 3) + " must be of type Date and Time.");
        setFormElement(TEST_ASSAY_SET_PROP_NAME + (TEST_ASSAY_SET_PREDEFINED_PROP_COUNT + 1), TEST_ASSAY_SET_PROPERTIES[1]);
        setFormElement(TEST_ASSAY_SET_PROP_NAME + (TEST_ASSAY_SET_PREDEFINED_PROP_COUNT + 2), TEST_ASSAY_SET_PROPERTIES[2]);
        setFormElement(TEST_ASSAY_SET_PROP_NAME + (TEST_ASSAY_SET_PREDEFINED_PROP_COUNT + 3), TEST_ASSAY_SET_PROPERTIES[3]);

        //ensure that the target study drop down contains Study 1 and Study 2 only and not Study 3
        //(labtech1 does not have read perms to Study 3)
        assertTextPresent(getTargetStudyOptionText(TEST_ASSAY_FLDR_STUDY1));
        assertTextPresent(getTargetStudyOptionText(TEST_ASSAY_FLDR_STUDY2));
        assertTextNotPresent(getTargetStudyOptionText(TEST_ASSAY_FLDR_STUDY3));

        //select Study2 as the target study (note that PI is not an Editor in this study so we can test for override case)
        selenium.select("//select[@name='targetStudy']", getTargetStudyOptionText(TEST_ASSAY_FLDR_STUDY2));

        clickNavButton("Next");

        log("Check properties set.");
        assertTextPresent(TEST_ASSAY_SET_PROPERTIES[1]);
        assertTextPresent(TEST_ASSAY_SET_PROPERTIES[2]);
        assertTextPresent(TEST_ASSAY_SET_PROPERTIES[3]);
        assertTextPresent(TEST_ASSAY_SET_PROPERTIES[0]);

        log("Run properties and data");
        clickNavButton("Save and Finish");
        assertTextPresent(TEST_ASSAY_RUN_PROP_NAME + "0 is required and must be of type Text (String).");
        selenium.type("name", TEST_RUN1);
		selenium.type("comments", TEST_RUN1_COMMENTS);
        setFormElement(TEST_ASSAY_RUN_PROP_NAME + "0", TEST_ASSAY_RUN_PROP1);
        clickNavButton("Save and Finish");
        assertTextPresent("Data file contained zero data rows");
        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA1);
        clickNavButton("Save and Finish");
        // reenable the following lines when we've moved to strict type checking of the incoming file.  For now, we're
        // flexible and only error if required columns are missing.
/*
        assertTextPresent("Expected columns were not found: " + TEST_ASSAY_DATA_PROP_NAME + "2");
        assertTextPresent("Unexpected columns were found: " + TEST_ASSAY_DATA_PROP_NAME + "20");
*/
        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA2);
        clickNavButton("Save and Finish");
        assertTextPresent("There are errors in the uploaded data: VisitID must be of type Number (Double)");
        assertEquals(TEST_RUN1, selenium.getValue("name"));
        assertEquals(TEST_RUN1_COMMENTS, selenium.getValue("comments"));
//        setFormElement("dataCollectorName", "textAreaDataProvider");
        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA3);
        clickNavButton("Save and Import Another Run");
        assertTextPresent("There are errors in the uploaded data: " + TEST_ASSAY_DATA_PROP_NAME + "6 is required. ");

        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA4);
        clickNavButton("Save and Import Another Run");

        assertEquals("", selenium.getValue("name"));
        assertEquals("", selenium.getValue("comments"));
        selenium.type("name", TEST_RUN2);
		selenium.type("comments", TEST_RUN2_COMMENTS);
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN2_DATA1);
        clickNavButton("Save and Finish");

        log("Check out the data for one of the runs");
        assertNoLabkeyErrors();
        assertTextPresent(TEST_ASSAY + " Runs");
        assertTextPresent(TEST_ASSAY_RUN_PROP1);
        assertTextPresent(TEST_ASSAY_SET_PROPERTIES[0]);
        assertTextPresent(TEST_ASSAY_SET_PROPERTIES[3]);
        clickLinkWithText(TEST_RUN1);
        isTextPresent("2.0");
        assertTextNotPresent("7.0");
        // Make sure that our specimen IDs resolved correctly
        assertTextPresent("AAA07XSF-02");
        assertTextPresent("999320885");
        assertTextPresent("301");
        assertTextPresent("AAA07XK5-05");
        assertTextPresent("999320812");
        assertTextPresent("601");
        assertTextPresent(TEST_ASSAY_DATA_PROP_NAME + "4");
        assertTextPresent(TEST_ASSAY_DATA_PROP_NAME + "5");
        assertTextPresent(TEST_ASSAY_DATA_PROP_NAME + "6");
        assertTextPresent("2000-06-06");
        assertTextPresent("0.0");
        assertTextPresent("f");
        assertTextPresent(ALIASED_DATA);

        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "SpecimenID/GlobalUniqueId", "Specimen Global Unique Id");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "SpecimenID/Specimen/PrimaryType", "Specimen Specimen Primary Type");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "SpecimenID/AssayMatch", "Specimen Assay Match");
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Run/testAssayRunProp1");
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Run/Batch/testAssaySetProp2");
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "testAssayDataProp4");
        CustomizeViewsHelper.applyCustomView(this);

        assertTextPresent("Blood (Whole)", 4);

        Locator.XPathLocator trueLocator = Locator.xpath("//table[contains(@class, 'labkey-data-region')]//td[text() = 'true']");
        int totalTrues = getXpathCount(trueLocator);
        assertEquals(4, totalTrues);

        setFilter(TEST_ASSAY + " Data", "SpecimenID", "Starts With", "AssayTestControl");

        // verify that there are no trues showing for the assay match column that were filtered out
        totalTrues = getXpathCount(trueLocator);
        assertEquals(0, totalTrues);

        log("Check out the data for all of the runs");
        clickLinkWithText("view results");
        clearAllFilters(TEST_ASSAY + " Data", "SpecimenID");
        isTextPresent("2.0");
        assertTextPresent("7.0");
        assertTextPresent("18");

        assertTextPresent("Blood (Whole)", 7);

        Locator.XPathLocator falseLocator = Locator.xpath("//table[contains(@class, 'labkey-data-region')]//td[text() = 'false']");
        int totalFalses = getXpathCount(falseLocator);
        assertEquals(3, totalFalses);

        setFilter(TEST_ASSAY + " Data", "SpecimenID", "Does Not Start With", "BAQ");

        // verify the falses have been filtered out
        totalFalses = getXpathCount(falseLocator);
        assertEquals(0, totalFalses);

        //Check to see that the bad specimen report includes the bad assay results and not the good ones
        //The report doesn't have top level UI (use a wiki) so just jump there.
        beginAt("specimencheck/" + TEST_ASSAY_PRJ_SECURITY + "/assayReport.view");
        waitForText("Global Specimen ID", 10000);
        waitForElement(Locator.linkWithText("BAQ00051-09"), 10000);
        assertLinkPresentWithText("BAQ00051-09");
        assertLinkPresentWithText("BAQ00051-08");
        assertLinkPresentWithText("BAQ00051-11");
        assertTextNotPresent("AAA");
        stopImpersonating();
        clickLinkWithText(TEST_ASSAY_PRJ_SECURITY);
    } //uploadRuns()

    /**
     * Impersonates the PI user and publishes the data previous uploaded.
     * This will also verify that the PI cannot publish to studies for which
     * the PI does not have Editor permissions.
     */
    private void publishData()
    {
        log("Publishing the data as the PI");

        //impersonate the PI
        impersonate(TEST_ASSAY_USR_PI1);
        clickLinkWithText(TEST_ASSAY_PRJ_SECURITY);

        //select the Lab1 folder and view all the data for the test assay
        clickLinkWithText(TEST_ASSAY_FLDR_LAB1);
        clickLinkWithText(TEST_ASSAY);
        clickLinkWithText("view results");

        //select all the data rows and click publish
        selenium.click(".toggle");
        clickNavButton("Copy to Study");

        //the target study selected before was Study2, but the PI is not an editor there
        //so ensure that system has correctly caught this fact and now asks the PI to
        //select a different study, and lists only those studies in which the PI is
        //an editor

        //ensure warning
        assertTextPresent("WARNING: You do not have permissions to copy to one or more of the selected run's associated studies.");

        //ensure that Study2 and Study 3 are not available in the target study drop down
        assertElementNotPresent(Locator.xpath("//select[@name='targetStudy']/option[.='" +
                getTargetStudyOptionText(TEST_ASSAY_FLDR_STUDY2) + "']"));
        assertElementNotPresent(Locator.xpath("//select[@name='targetStudy']/option[.='" +
                getTargetStudyOptionText(TEST_ASSAY_FLDR_STUDY3) + "']"));

        //Study1 is the only one left, so it should be there and already be selected
        assertElementPresent(Locator.xpath("//select[@name='targetStudy']/option[.='" +
                getTargetStudyOptionText(TEST_ASSAY_FLDR_STUDY1) + "']"));

        // Make sure the selected study is Study1
        selectOptionByText(Locator.xpath("//select[@name='targetStudy']"), getTargetStudyOptionText(TEST_ASSAY_FLDR_STUDY1));

        clickNavButton("Next");
        assertTextPresent("Copy to " + TEST_ASSAY_FLDR_STUDY1 + " Study: Verify Results");

        clickNavButton("Copy to Study");

        log("Verifying that the data was published");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "QCState", "QC State");
        CustomizeViewsHelper.applyCustomView(this);
        assertTextPresent("Pending Review");
        assertTextPresent("a");
        assertTextPresent(TEST_RUN1_COMMENTS);
        assertTextPresent("2000-01-01");
        clickLinkWithText("Study Overview");

        log("Test participant counts and row counts in study overview");
        String[] row2 = new String[]{TEST_ASSAY, "7", "1", "1", "1", "1", "1", "2"};
        assertTableRowsEqual("studyOverview", 1, new String[][]{row2});
        // Manually click the checkbox -- normal checkCheckbox() method doesn't seem to work for checkbox that reloads using onchange event
        click(Locator.checkboxByNameAndValue("visitStatistic", "RowCount"));
        waitForPageToLoad();
        row2 = new String[]{TEST_ASSAY, "7 / 8", "1 / 1", "1 / 1", "1 / 1", "1 / 1", "1 / 1", "2 / 3"};
        assertTableRowsEqual("studyOverview", 1, new String[][]{row2});
        uncheckCheckbox("visitStatistic", "ParticipantCount");
        waitForPageToLoad();
        row2 = new String[]{TEST_ASSAY, "8", "1", "1", "1", "1", "1", "3"};
        assertTableRowsEqual("studyOverview", 1, new String[][]{row2});

        clickLinkWithText("8");

        assertTextPresent("301.0");
        assertTextPresent("9.0");
        assertTextPresent("8.0");
        assertLinkPresentWithTextCount("999320396", 2);
        assertLinkPresentWithTextCount("999320885", 1);
        assertTextPresent(TEST_RUN1_COMMENTS);
        assertTextPresent(TEST_RUN2_COMMENTS);
        assertTextPresent(TEST_RUN1);
        assertTextPresent(TEST_RUN2);
        assertTextPresent("2000-06-06");
        assertTextPresent(TEST_ASSAY_RUN_PROP1);
        assertTextPresent("18");

        // test recall
        clickLinkWithText(TEST_ASSAY_FLDR_LAB1);
        clickLinkWithText(TEST_ASSAY);
        clickLinkWithText("view copy-to-study history");

        clickLinkWithText("details");
        checkCheckbox(Locator.checkboxByName(".toggle"));
        clickNavButton("Recall Rows", 0);
        getConfirmationAndWait();
        assertTextPresent("row(s) were recalled to the assay: " + TEST_ASSAY);

        // verify audit entry was adjusted
        clickLinkWithText("details");
        assertTextPresent("All rows that were previously copied in this event have been recalled");

        stopImpersonating();
    } //publishData()

    /**
     * Tests editing of an existing assay definition
     */
    private void editAssay()
    {
        log("Testing edit and delete and assay definition");

        clickLinkWithText(TEST_ASSAY_PRJ_SECURITY);

        clickLinkWithText(TEST_ASSAY);
        click(Locator.linkWithText("manage assay design"));
        clickLinkWithText("edit assay design");
        waitForElement(Locator.raw(getPropertyXPath("Data Fields") + "//td//input[@name='ff_name5']"), WAIT_FOR_JAVASCRIPT);
        ListHelper.setColumnName(this, getPropertyXPath("Data Fields"), 5, TEST_ASSAY_DATA_PROP_NAME + "edit");
        ListHelper.setColumnLabel(this, getPropertyXPath("Data Fields"), 5, TEST_ASSAY_DATA_PROP_NAME + "edit");
        deleteField("Data Fields", 4);
        clickNavButton("Save", 0);
        waitForText("Save successful.", WAIT_FOR_JAVASCRIPT);

        //ensure that label has changed in run data in Lab 1 folder
        clickLinkWithText(TEST_ASSAY_FLDR_LAB1);
        clickLinkWithText(TEST_ASSAY);
        clickLinkWithText(TEST_RUN1);
        assertTextPresent(TEST_ASSAY_DATA_PROP_NAME + "edit");
        assertTextNotPresent(TEST_ASSAY_DATA_PROP_NAME + 4);

        AuditLogTest.verifyAuditEvent(this, AuditLogTest.ASSAY_AUDIT_EVENT, AuditLogTest.COMMENT_COLUMN, "were copied to a study from the assay: " + TEST_ASSAY, 5);
    } //editAssay()

    private void viewCrossFolderData()
    {
        log("Testing cross-folder data");

        clickLinkWithText(TEST_ASSAY_PRJ_SECURITY);
        
        addWebPart("Assay Runs");
        selectOptionByText("viewProtocolId", "General: " + TEST_ASSAY);
        // assay runs has a details page that needs to be submitted
        clickButton("Submit", defaultWaitForPage);

        // Set the container filter to include subfolders
        clickMenuButton("Views", "Folder Filter", "Current folder and subfolders");

        assertTextPresent("FirstRun");
        assertTextPresent("SecondRun");

        log("Setting the customized view to include subfolders");
        CustomizeViewsHelper.openCustomizeViewPanel(this);

        CustomizeViewsHelper.clipFolderFilter(this);
        CustomizeViewsHelper.saveCustomView(this, "");

        assertTextPresent("FirstRun");
        assertTextPresent("SecondRun");

        log("Testing select all data and view");
        clickCheckbox(".toggle");
        clickButton("Show Results", defaultWaitForPage);
        verifySpecimensPresent(3, 2, 3);

        log("Testing clicking on a run");
        clickLinkWithText(TEST_ASSAY_PRJ_SECURITY);
        clickLinkWithText("FirstRun");
        verifySpecimensPresent(3, 2, 0);

        clickLinkWithText("view results");
        clearAllFilters(TEST_ASSAY + " Data", "SpecimenID");
        verifySpecimensPresent(3, 2, 3);

        log("Testing assay-study linkage");
        clickLinkWithText(TEST_ASSAY_FLDR_STUDY1);
        clickLinkWithText(TEST_ASSAY);
        clickButton("View Source Assay", defaultWaitForPage);

        assertTextPresent("FirstRun");
        assertTextPresent("SecondRun");

        clickLinkWithText("FirstRun");
        verifySpecimensPresent(3, 2, 0);

        clickLinkWithText("view results");
        clearAllFilters(TEST_ASSAY + " Data", "SpecimenID");
        verifySpecimensPresent(3, 2, 3);

        // Verify that the correct copied to study column is present
        assertTextPresent("Copied to Study 1 Study");

        log("Testing copy to study availability");
        clickLinkWithText(TEST_ASSAY_PRJ_SECURITY);
        clickLinkWithText("SecondRun");

        clickCheckbox(".toggle");
        clickButton("Copy to Study", defaultWaitForPage);
        clickButton("Next", defaultWaitForPage);

        verifySpecimensPresent(0, 0, 3);

        clickButton("Cancel", defaultWaitForPage);

        clickLinkWithText(TEST_ASSAY_PRJ_SECURITY);
    }

    private void verifySpecimensPresent(int aaa07Count, int controlCount, int baq00051Count)
    {
        assertTextPresent("AAA07", aaa07Count);
        assertTextPresent("AssayTestControl", controlCount);
        assertTextPresent("BAQ00051", baq00051Count);
    }

    protected boolean isFileUploadTest()
    {
        return true;
    }
}
