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

import org.labkey.test.Locator;

/**
 * User: jeckels
 * Date: Aug 10, 2007
 *
 * Modified by DaveS on 13 Sept 13 2007
 *  Added security-related tests, and refactored the code to run Luminex by itself in a separate project
 */
public class AssayTest extends AbstractAssayTest
{
    protected static final String TEST_ASSAY = "TestAssay1";
    protected static final String TEST_ASSAY_DESC = "Description for assay 1";

    protected static final String TEST_ASSAY_SET_PROP_EDIT = "NewTargetStudy";
    protected static final String TEST_ASSAY_SET_PROP_NAME = "testAssaySetProp";
    protected static final int TEST_ASSAY_SET_PREDEFINED_PROP_COUNT = 2;
    protected static final String[] TEST_ASSAY_SET_PROP_TYPES = { "Boolean", "Number (Double)", "Integer", "DateTime" };
    protected static final String[] TEST_ASSAY_SET_PROPERTIES = { "false", "100.0", "200", "2001-10-10" };
    protected static final String TEST_ASSAY_RUN_PROP_NAME = "testAssayRunProp";
    protected static final int TEST_ASSAY_RUN_PREDEFINED_PROP_COUNT = 0;
    protected static final String[] TEST_ASSAY_RUN_PROP_TYPES = { "Text (String)", "Boolean", "Number (Double)", "Integer", "DateTime" };
    protected static final String TEST_ASSAY_RUN_PROP1 = "TestRunProp";
    protected static final String TEST_ASSAY_DATA_PROP_NAME = "testAssayDataProp";
    protected static final int TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT = 4;
    protected static final String[] TEST_ASSAY_DATA_PROP_TYPES = { "Boolean", "Integer", "DateTime" };
    protected static final String TEST_RUN1 = "FirstRun";
    protected static final String TEST_RUN1_COMMENTS = "First comments";
    protected static final String TEST_RUN1_DATA1 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "20\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\n" +
            "s1\ta\t1\ttrue\t20\t2000-01-01\n" +
            "s2\tb\t2\ttrue\t19\t2000-02-02\n" +
            "s3\tc\t3\ttrue\t18\t2000-03-03\n" +
            "s4\td\t4\tfalse\t17\t2000-04-04\n" +
            "s5\te\t5\tfalse\t16\t2000-05-05\n" +
            "s6\tf\tg\tfalse\t15\t2000-06-06";
    protected static final String TEST_RUN1_DATA2 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "4\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\n" +
            "s1\ta\t1\ttrue\t20\t2000-01-01\n" +
            "s2\tb\t2\ttrue\t19\t2000-02-02\n" +
            "s3\tc\t3\ttrue\t18\t2000-03-03\n" +
            "s4\td\t4\tfalse\t17\t2000-04-04\n" +
            "s5\te\t5\tfalse\t16\t2000-05-05\n" +
            "s6\tf\tg\tfalse\t15\t2000-06-06";
    protected static final String TEST_RUN1_DATA3 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "4\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\n" +
            "s1\ta\t1\ttrue\t20\t\n" +
            "s2\tb\t2\ttrue\t19\t\n" +
            "s3\tc\t3\ttrue\t18\t";
    protected static final String TEST_RUN1_DATA4 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "4\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\n" +
            "s1\ta\t1\ttrue\t\t2000-01-01\n" +
            "s2\tb\t2\ttrue\t\t2000-02-02\n" +
            "s3\tc\t3\ttrue\t\t2000-03-03\n" +
            "s4\td\t4\tfalse\t\t2000-04-04\n" +
            "s5\te\t5\tfalse\t\t2000-05-05\n" +
            "s6\tf\t6\tfalse\t\t2000-06-06";
    protected static final String TEST_RUN2 = "SecondRun";
    protected static final String TEST_RUN2_COMMENTS = "Second comments";
    protected static final String TEST_RUN2_DATA1 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "4\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\n" +
            "s7\tg\t7\ttrue\t20\t2000-01-01\n" +
            "s8\th\t8\ttrue\t19\t2000-02-02\n" +
            "s9\ti\t9\ttrue\t18\t2000-03-03\n";
    private final static String TEST_ASSAY_PERMS_STUDY_READSOME = "READOWN";
    private final static String TEST_ASSAY_PERMS_STUDY_READNONE = "NONE";

    private String getPropertyXPath(String propertyHeading)
    {
        return "//td[contains(text(), '" + propertyHeading + "')]/../..";
    }

    public String getAssociatedModuleDirectory()
    {
        return "study";
    }

    /**
     * Cleanup entry point.
     */
    protected void doCleanup()
    {
        revertToAdmin();
        try
        {
            //delete all folders and then finally the project
            //the UI currently does not allow deleting a folder that
            //contains other folders, so we need to delete them from
            //the bottom up
            deleteFolder(TEST_ASSAY_PRJ_SECURITY, TEST_ASSAY_FLDR_LAB1);
            deleteFolder(TEST_ASSAY_PRJ_SECURITY, TEST_ASSAY_FLDR_STUDY1);
            deleteFolder(TEST_ASSAY_PRJ_SECURITY, TEST_ASSAY_FLDR_STUDY2);
            deleteFolder(TEST_ASSAY_PRJ_SECURITY, TEST_ASSAY_FLDR_STUDY3);
            deleteFolder(TEST_ASSAY_PRJ_SECURITY, TEST_ASSAY_FLDR_STUDIES);
            deleteFolder(TEST_ASSAY_PRJ_SECURITY, TEST_ASSAY_FLDR_LABS);
            deleteProject(TEST_ASSAY_PRJ_SECURITY); //should also delete the groups

            //delete user accounts
            deleteUser(TEST_ASSAY_USR_PI1);
            deleteUser(TEST_ASSAY_USR_TECH1);
            deleteFile(getTestTempDir());
        }
        catch(Throwable T) {/* ignore */}
    } //doCleanup()

    /**
     *  Performs the Assay security test
     *  This test creates a project with a folder hierarchy with multiple groups and users;
     *  defines an Assay at the project level; uploads run data as a labtech; publishes
     *  as a PI, and tests to make sure that security is properly enforced
     */
    protected void doTestSteps()
    {
        log("Starting Assay security scenario tests");
        setupEnvironment();
        setupPipeline(TEST_ASSAY_PRJ_SECURITY);
        defineAssay();
        uploadRuns(TEST_ASSAY_FLDR_LAB1, TEST_ASSAY_USR_TECH1);
        publishData();
        editAssay();
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
        clickLinkWithText("Manage Assays");
        clickNavButton("New Assay Design");
        setFormElement("providerName", "General");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@type='text']"), WAIT_FOR_GWT);

        selenium.type("//input[@type='text']", TEST_ASSAY);
        selenium.type("//textarea", TEST_ASSAY_DESC);

        for (int i = TEST_ASSAY_SET_PREDEFINED_PROP_COUNT; i < TEST_ASSAY_SET_PREDEFINED_PROP_COUNT + TEST_ASSAY_SET_PROP_TYPES.length; i++)
        {
            selenium.click(getPropertyXPath("Upload Set Fields") + "//img[@id='button_Add Field']");
            selenium.type(getPropertyXPath("Upload Set Fields") + "//input[@id='ff_name" + i + "']", TEST_ASSAY_SET_PROP_NAME + i);
            selenium.type(getPropertyXPath("Upload Set Fields") + "//input[@id='ff_label" + i + "']", TEST_ASSAY_SET_PROP_NAME + i);
            selenium.select(getPropertyXPath("Upload Set Fields") + "//select[@id='ff_type" + i + "']", TEST_ASSAY_SET_PROP_TYPES[i - TEST_ASSAY_SET_PREDEFINED_PROP_COUNT]);
        }

        for (int i = TEST_ASSAY_RUN_PREDEFINED_PROP_COUNT; i < TEST_ASSAY_RUN_PREDEFINED_PROP_COUNT + TEST_ASSAY_RUN_PROP_TYPES.length; i++)
        {
            selenium.click(getPropertyXPath("Run Fields") + "//img[@id='button_Add Field']");
            selenium.type(getPropertyXPath("Run Fields") + "//input[@id='ff_name" + i + "']", TEST_ASSAY_RUN_PROP_NAME + i);
            selenium.type(getPropertyXPath("Run Fields") + "//input[@id='ff_label" + i + "']", TEST_ASSAY_RUN_PROP_NAME + i);
            selenium.select(getPropertyXPath("Run Fields") + "//select[@id='ff_type" + i + "']", TEST_ASSAY_RUN_PROP_TYPES[i - TEST_ASSAY_RUN_PREDEFINED_PROP_COUNT]);
        }

        for (int i = TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT; i < TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + TEST_ASSAY_DATA_PROP_TYPES.length; i++)
        {
            selenium.click(getPropertyXPath("Data Fields") + "//img[@id='button_Add Field']");
            selenium.type(getPropertyXPath("Data Fields") + "//input[@id='ff_name" + i + "']", TEST_ASSAY_DATA_PROP_NAME + i);
            selenium.type(getPropertyXPath("Data Fields") + "//input[@id='ff_label" + i + "']", TEST_ASSAY_DATA_PROP_NAME + i);
            selenium.select(getPropertyXPath("Data Fields") + "//select[@id='ff_type" + i + "']", TEST_ASSAY_DATA_PROP_TYPES[i - TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT]);
        }

        // Set some to required
        selenium.click(getPropertyXPath("Upload Set Fields") + "//input[@id='ff_name" + (TEST_ASSAY_SET_PREDEFINED_PROP_COUNT) + "']");
        selenium.click(getPropertyXPath("Upload Set Fields") + "//input[@type='checkbox']");

        selenium.click(getPropertyXPath("Upload Set Fields") + "//input[@id='ff_name" + (TEST_ASSAY_SET_PREDEFINED_PROP_COUNT + 1) + "']");
        selenium.click(getPropertyXPath("Upload Set Fields") + "//input[@type='checkbox']");

        selenium.click(getPropertyXPath("Run Fields") + "//input[@id='ff_name0']");
        selenium.click(getPropertyXPath("Run Fields") + "//input[@type='checkbox']");

        selenium.click(getPropertyXPath("Data Fields") + "//input[@id='ff_name0']");
        selenium.click(getPropertyXPath("Data Fields") + "//input[@type='checkbox']");

        selenium.click(getPropertyXPath("Data Fields") + "//input[@id='ff_name" + (TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + 2) + "']");
        selenium.click(getPropertyXPath("Data Fields") + "//input[@type='checkbox']");

        sleep(1000);
        click(Locator.id("button_Save Changes"));
        waitForText("Save successful.", 20000);

    } //defineAssay()

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
        impersonateUser(asUser);
        clickLinkWithText(TEST_ASSAY_PRJ_SECURITY);
        clickLinkWithText(folder);

        clickLinkWithText("Assay List");
        clickLinkWithText(TEST_ASSAY);

        clickNavButton("Upload Runs");
        assertTextPresent(TEST_ASSAY_SET_PROP_NAME + "3");

        log("Upload set properties");
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
        clickNavButton("Save and Upload Another Run");
        assertTextPresent("There are errors in the uploaded data: " + TEST_ASSAY_DATA_PROP_NAME + "6 is required. ");

        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA4);
        clickNavButton("Save and Upload Another Run");

        assertEquals("", selenium.getValue("name"));
        assertEquals("", selenium.getValue("comments"));
        selenium.type("name", TEST_RUN2);
		selenium.type("comments", TEST_RUN2_COMMENTS);
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN2_DATA1);
        clickNavButton("Save and Finish");

        log("Check out the data for one of the runs");
        assertTextPresent(TEST_ASSAY + " Runs");
        assertTextPresent(TEST_ASSAY_RUN_PROP1);
        assertTextPresent(TEST_ASSAY_SET_PROPERTIES[0]);
        assertTextPresent(TEST_ASSAY_SET_PROPERTIES[3]);
        clickLinkWithText(TEST_RUN1);
        waitForPageToLoad();
        isTextPresent("2.0");
        assertTextNotPresent("7.0");
        assertTextPresent(TEST_ASSAY_DATA_PROP_NAME + "4");
        assertTextPresent(TEST_ASSAY_DATA_PROP_NAME + "5");
        assertTextPresent(TEST_ASSAY_DATA_PROP_NAME + "6");
        assertTextPresent("2000-06-06");
        assertTextPresent("0.0");
        assertTextPresent("f");

        log("Check out the data for all of the runs");
        clickLinkWithText("view all data");
        waitForPageToLoad();
        isTextPresent("2.0");
        assertTextPresent("7.0");
        assertTextPresent("18");

        revertToAdmin(TEST_ASSAY_PRJ_SECURITY);
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
        impersonateUser(TEST_ASSAY_USR_PI1, TEST_ASSAY_PRJ_SECURITY);

        //select the Lab1 folder and view all the data for the test assay
        clickLinkWithText(TEST_ASSAY_FLDR_LAB1);
        clickLinkWithText(TEST_ASSAY);
        clickLinkWithText("view all data");

        //select all the data rows and click publish
        selenium.click(".toggle");
        clickNavButton("Copy Selected to Study");

        //the target study selected before was Study2, but the PI is not an editor there
        //so ensure that system has correctly caught this fact and now asks the PI to
        //select a different study, and lists only those studies in which the PI is
        //an editor

        //ensure warning
        assertTextPresent("WARNING: You do not have permissions to publish to one or more of the selected run's associated studies.");

        //ensure that Study2 and Study 3 are not available in the target study drop down
        assertElementNotPresent(Locator.xpath("//select[@name='targetStudy']/option[.='" +
                getTargetStudyOptionText(TEST_ASSAY_FLDR_STUDY2) + "']"));
        assertElementNotPresent(Locator.xpath("//select[@name='targetStudy']/option[.='" +
                getTargetStudyOptionText(TEST_ASSAY_FLDR_STUDY3) + "']"));

        //Study1 is the only one left, so it should be there and already be selected
        assertElementPresent(Locator.xpath("//select[@name='targetStudy']/option[.='" +
                getTargetStudyOptionText(TEST_ASSAY_FLDR_STUDY1) + "']"));

        clickNavButton("Next");
        clickNavButton("Copy to Study");

        log("Verifying that the data was published");
        assertTextPresent("a");
        assertTextPresent(TEST_RUN1_COMMENTS);
        assertTextPresent("2000-01-01");
        clickLinkWithText("Study Overview");
        clickLinkWithText("9");

        assertTextPresent("2.0");
        assertTextPresent(TEST_RUN1_COMMENTS);
        assertTextPresent(TEST_RUN2_COMMENTS);
        assertTextPresent(TEST_RUN1);
        assertTextPresent(TEST_RUN2);
        assertTextPresent("2000-06-06");
        assertTextPresent(TEST_ASSAY_RUN_PROP1);
        assertTextPresent("18");

    } //publishData()

    /**
     * Tests editing of an existing assay definition
     */
    private void editAssay()
    {
        log("Testing edit and delete and assay definition");
        revertToAdmin(); //need to be admin to edit assay at project root

        clickLinkWithText(TEST_ASSAY_PRJ_SECURITY);

        clickLinkWithText(TEST_ASSAY);
        selenium.mouseDown(Locator.linkWithText("manage assay design >>").toString());
        clickLinkWithText("edit assay design");
        waitForElement(Locator.raw(getPropertyXPath("Data Fields") + "//input[@id='ff_name5']"), WAIT_FOR_GWT);
        selenium.type(getPropertyXPath("Data Fields") + "//input[@id='ff_name5']", TEST_ASSAY_DATA_PROP_NAME + "edit");
        selenium.type(getPropertyXPath("Data Fields") + "//input[@id='ff_label5']", TEST_ASSAY_DATA_PROP_NAME + "edit");
        click(Locator.raw(getPropertyXPath("Data Fields") + "//img[@id='partdelete_4']"));
        waitForElement(Locator.raw("//img[@id='partdeleted_4']"), WAIT_FOR_GWT);
        click(Locator.id("button_Save Changes"));
        waitForText("Save successful.", WAIT_FOR_GWT);

        //ensure that label has changed in run data in Lab 1 folder
        clickLinkWithText(TEST_ASSAY_FLDR_LAB1);
        clickLinkWithText(TEST_ASSAY);
        clickLinkWithText(TEST_RUN1);
        assertTextPresent(TEST_ASSAY_DATA_PROP_NAME + "edit");
        assertTextNotPresent(TEST_ASSAY_DATA_PROP_NAME + 4);

        AuditLogTest.verifyAuditEvent(this, AuditLogTest.ASSAY_AUDIT_EVENT, AuditLogTest.COMMENT_COLUMN, "were copied to a study from the assay: " + TEST_ASSAY, 5);
    } //editAssay()

    /**
     * Reverts to the admin account, and then selects a particular project
     *
     * @param project project to select.
     */
    private void revertToAdmin(String project)
    {
        revertToAdmin();
        clickLinkWithText(project);
    }

    /**
     * Copied from SeleniumUserPermissionsTest - Consider moving this to BaseWebTest?
     *
     * Once you have impersonatned a user you can't go back and impersonate another until you sign in.
     * So to be safe, always sign out as the Admin User and Sign back in
     *
     * @param userEmailAddress user email address to impersonate
     */
    private void impersonateUser(String userEmailAddress)
    {
        log("impersonating user : " + userEmailAddress);
        signOut();
        signIn();
        ensureAdminMode();
        clickLinkWithText("Admin Console");
        setFormElement("email", userEmailAddress);
        clickNavButton("Impersonate");
    } //impersonateUser(email)

    /**
     * Impersonates another user, and selects a particular project
     *
     * @param userEmailAddress user to impersonate
     * @param project project to select after impersonation
     */
    private void impersonateUser(String userEmailAddress, String project)
    {
        impersonateUser(userEmailAddress);
        clickLinkWithText(project);
    } //impersonateUser(email, project)

    protected boolean isFileUploadTest()
    {
        return true;
    }
}
