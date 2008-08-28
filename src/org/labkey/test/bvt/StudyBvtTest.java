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

import com.thoughtworks.selenium.SeleniumException;
import org.labkey.test.Locator;
import org.labkey.test.drt.StudyTest;

import java.io.File;
import java.io.FilenameFilter;

/**
 * User: brittp
 * Date: Dec 8, 2006
 * Time: 4:30:24 PM
 */
public class StudyBvtTest extends StudyTest
{
    private static final String SPECIMEN_ARCHIVE_B = "/sampledata/study/sample_b.specimens";
    protected static final String TEST_GROUP = "firstGroup";
    protected static final String TEST_USER = "user1@test.com";

    private final static String[] R_SCRIPTS = { "rScript1", "rScript2", "rScript3" };
    private final static String DATA_SET = "DEM-1: Demographics";
    private final static String DATA_BASE_PREFIX = "DEM";
    private final static String R_SCRIPT1_METHOD = "func1";
    private final static String R_VIEW = "rView";
    private final static String R_SCRIPT1_ORIG_FUNC = "length(x)";
    private final static String R_SCRIPT1_EDIT_FUNC = "length(x) * 2";

    private static final String CREATE_CHART_MENU = "Views:Create:Chart View";
    private static final String CREATE_R_MENU = "Views:Create:R View";
    private static final String TEST_GRID_VIEW = "Test Grid View";

    // mssql and postgres
    private String R_SCRIPT1(String function, String database)
    {
        return "data <- labkey.data\n" +
            "dbirth <- data$" + database + "bdt\n" +
            "dbirth\n" +
            "sex <- data$" + database + "sex\n" +
            "sexor <- data$" + database + "sexor\n" +
            R_SCRIPT1_METHOD + " <- function(x)\n" +
            "{\n" + function + "\n}\n" +
            "png(filename=\"${imgout:img1}\")\n" +
            "plot(sex, sexor)\n" +
            "dev.off()\n" +
            "pdf(file=\"${pdfout:study}\")\n" +
            "plot(sex, sexor)\n" +
            "dev.off()";
    }
    private final static String TEST_ADD_ENTRY = "999000000";
    private final static String R_SCRIPT1_TEXT1 = "1965-03-06";
    private final static String R_SCRIPT1_TEXT2 = "1980-08-01";
    private final static String R_SCRIPT1_IMG = "resultImage";
    private final static String R_SCRIPT1_PDF = "PDF output file (click to download)";
    private final static String R_FILTER = "DEMhisp";
    private final static String R_FILTERED = "999320565";
    private final static String R_SORT = "DEMsex";
    private final static String R_SORT1 = "Male";
    private final static String R_SORT2 = "Female";
    private final static String R_REMCOL = "5. Sexual orientation";
    private final static String R_SCRIPT2_METHOD = "func2";
    private String R_SCRIPT2(String database, String colName)
    {
        return "source(\"" + R_SCRIPTS[0] + ".R\")\n" +
            R_SCRIPT2_METHOD +
            " <- function(x, y)\n" +
            "{\nn1 <- " + R_SCRIPT1_METHOD +
            "(y)\n" +
            "n2 <- mean(x)\n" +
            "n3 <- n1 + n2\n" +
            "n3\n}\n" +
            "func2(labkey.data$" + colName + ", labkey.data$" + database + "natam)";
    }
    private final static String R_SCRIPT2_TEXT1 = "999320648";
    private final static String USER1 = "ruser1@rscripts.com";
    private String R_SCRIPT3(String database, String colName)
    {
        return "source(\"" + R_SCRIPTS[1] + ".R\")\n" +
            "x <- func2(labkey.data$" + colName + ", labkey.data$" + database + "sex)\n" +
            "x\n";
    }
    private final static String R_SCRIPT2_TEXT2 = "999320672";

    protected void doCleanup() throws Exception
    {
        deleteUser(TEST_USER);
        deleteUser(USER1);
        super.doCleanup();
    }

    @Override
    protected void doTestSteps()
    {
        super.doTestSteps();

        // verify that we correctly warn when specimen tracking hasn't been configured
        clickLinkWithText("Study 001");
        clickLinkWithText("Create New Request");
        assertTextPresent("Specimen management is not configured for this study");

        // configure specimen tracking
        clickLinkWithText("Study 001");
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Request Statuses");
        setFormElement("newLabel", "New Request");
        clickNavButton("Save");
        setFormElement("newLabel", "Pending Approval");
        clickNavButton("Save");
        setFormElement("newLabel", "Complete");
        clickNavButton("Done");
        clickLinkWithText("Manage Actors and Groups");
        setFormElement("newLabel", "Institutional Review Board");
        selectOptionByText("newPerSite", "Multiple Per Study (Location Affiliated)");
        clickNavButton("Save");
        setFormElement("newLabel", "Scientific Leadership Group");
        selectOptionByText("newPerSite", "One Per Study");
        clickNavButton("Save");
        clickLinkWithText("Update Members");
        clickLinkWithText("FHCRC - Seattle");
        assertTextPresent("Institutional Review Board, FHCRC - Seattle");
        assertTextPresent("This group currently has no members.");
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Default Requirements");
        selectOptionByText("providerActor", "Institutional Review Board");
        setFormElement("providerDescription", "To be deleted");
        clickNavButtonByIndex("Add Requirement", 1);
        assertTextPresent("To be deleted");
        clickLinkWithText("Delete");
        assertTextNotPresent("To be deleted");
        selectOptionByText("providerActor", "Institutional Review Board");
        setFormElement("providerDescription", "Providing lab approval");
        clickNavButtonByIndex("Add Requirement", 1);
        selectOptionByText("receiverActor", "Institutional Review Board");
        setFormElement("receiverDescription", "Receiving lab approval");
        clickNavButtonByIndex("Add Requirement", 2);
        selectOptionByText("generalActor", "Scientific Leadership Group");
        setFormElement("generalDescription", "SLG Request Approval");
        clickNavButtonByIndex("Add Requirement", 3);
        clickLinkWithText("manage study");

        // create specimen request
        clickLinkWithText("Study 001");
        clickLinkWithText("Study Navigator");

        assertLinkNotPresentWithText("24");
        selectOptionByText("QCState", "All data");
        waitForPageToLoad();

        clickLinkWithText("24");
        //getDialog().setWorkingForm("Dataset");
        checkCheckbox(Locator.checkboxByName(".toggle", false));
        clickNavButton("View Specimens");
        assertTextPresent("364V05000031");
        clickLinkWithText("Show Vial and Request Options");
        checkCheckbox(Locator.checkboxByName(".toggle", false));
        clickNavButton("Request Options", 0);
        clickLinkWithText("Create New Request");
        assertTextPresent("HAQ0003Y-09");
        assertTextPresent("BAQ00051-09");
        assertTextNotPresent("KAQ0003Q-01");
        selectOptionByText("destinationSite", "Duke University");
        setFormElement("inputs", new String[] { "An Assay Plan", "Duke University, NC", "My comments" });
        clickNavButton("Create Request");

        assertTextPresent("This request has not been submitted");
        assertNavButtonPresent("Cancel Request");
        assertNavButtonPresent("Submit Request");
        clickLinkWithText("Specimen Requests");

        assertNavButtonPresent("Submit");
        assertNavButtonPresent("Cancel");
        assertNavButtonPresent("Details");
        assertTextPresent("Not Yet Submitted");
        clickNavButton("Submit");
        selenium.getConfirmation();
        clickLinkWithText("Specimen Requests");
        assertNavButtonNotPresent("Submit");
        assertNavButtonPresent("Details");
        assertTextPresent("New Request");

        // test auto-fill:
        clickNavButton("Create New Request");
        String inputs = selenium.getValue("inputs");
        System.out.println(inputs);
        assertFormElementNotEquals(Locator.dom("document.forms[0].inputs[1]"), "Duke University, NC");
        selectOptionByText("destinationSite", "Duke University");
        assertFormElementEquals(Locator.dom("document.forms[0].inputs[1]"), "Duke University, NC");
        clickNavButton("Cancel");

        // manage new request
        clickNavButton("Details");
        assertTextNotPresent("Complete");
        assertTextNotPresent("WARNING: Missing Specimens");
        assertTextPresent("New Request");
        assertTextNotPresent("Pending Approval");
        clickLinkWithText("Update Status");
        selectOptionByText("status", "Pending Approval");
        setFormElement("comments", "Request is now pending.");
        clickNavButton("Save Changes and Send Notifications");
        assertTextNotPresent("New Request");
        assertTextPresent("Pending Approval");
        clickLinkWithText("Details", 0);
        assertTextPresent("Duke University");
        assertTextPresent("Providing lab approval");
        checkCheckbox("complete");
        setFormElement("comment", "Approval granted.");
        if (isFileUploadAvailable())
            setFormElement("formFiles[0]", new File(getLabKeyRoot() + VISIT_MAP).getPath());
        else
            log("File upload skipped.");
        clickNavButton("Save Changes and Send Notifications");
        assertTextPresent("Complete");

        clickLinkWithText("Details", 1);
        clickNavButton("Delete Requirement");
        assertTextNotPresent("Receiving lab approval");

        clickLinkWithText("Originating Location Specimen Lists");
        assertTextPresent("WARNING: The requirements for this request are incomplete");
        assertTextPresent("KCMC, Moshi, Tanzania");
        clickNavButton("Cancel");

        clickLinkWithText("View History");
        assertTextPresent("Request is now pending.");
        assertTextPresent("Approval granted.");
        assertTextPresent("Institutional Review Board (Duke University), Receiving lab approval");
        if (isFileUploadAvailable())
            assertTextPresent(VISIT_MAP.substring(VISIT_MAP.lastIndexOf("/") + 1));

        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("Permissions");
        clickNavButton("Study Security");

        // enable advanced study security
        selectOptionByValue("securityString", "ADVANCED_READ");
        selenium.waitForPageToLoad("30000");

        click(Locator.xpath("//td[.='Users']/..//input[@value='READ']"));
        clickAndWait(Locator.id("groupUpdateButton"));

        // set the QC state 
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText(DATA_SET);
        clickMenuButton("QC State", "QCState:All data");
        checkAllOnPage("Dataset");
        clickMenuButton("QC State", "QCState:updateSelected");
        selectOptionByText("newState", "Approved");
        setFormElement("comments", "This data is approved.");
        clickNavButton("Update Status");
        clickMenuButton("QC State", "QCState:Approved");

        pushLocation();

        if (checkRSetup())
            RReportTest();

        popLocation();
        
        // test specimen comments
        clickLinkWithText("Study 001");
        clickLinkWithText("Plasma, Unknown Processing");
        clickNavButton("Enable Comments");
        checkAllOnPage("SpecimenDetail");
        clickMenuButton("Comments", "Comments:Set");
        setFormElement("comments", "These vials are very important.");
        clickNavButton("Save Changes");
        assertTextPresent("These vials are very important.", 4);
        setFilter("SpecimenDetail", "SpecimenNumber", "Equals", "350V06002983");
        checkAllOnPage("SpecimenDetail");
        clickMenuButton("Comments", "Comments:Clear");
        selenium.getConfirmation();
        assertTextNotPresent("These vials are very important.");
        clearFilter("SpecimenDetail", "SpecimenNumber");
        assertTextPresent("These vials are very important.", 2);
        clickMenuButton("Comments", "Comments:Exit");

        // import second archive, verify that that data is merged:
        click(Locator.linkWithText("Projects"));
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        importSpecimenArchive(SPECIMEN_ARCHIVE_B);


        // verify that comments remain after second specimen load
        clickLinkWithText("Study 001");
        clickLinkWithText("Plasma, Unknown Processing");
        assertTextPresent("These vials are very important.", 2);

        // check to see that data in the specimen archive was merged correctly:
        clickLinkWithText("Study 001");
        clickLinkWithText("By Vial");
        assertTextPresent("DRT000XX-01");
        clickLinkWithText("Search");
        clickLinkWithText("Search by specimen");

        /*

        WARNING: Using getFormElementNameByTableCaption() is dangerous... if muliple values are returned their
        order is unpredictable, since they come back in keyset order.  The code below breaks under Java 6.

        String[] globalUniqueIDCompareElems = getFormElementNameByTableCaption("Specimen Number", 0, 1);
        String[] globalUniqueIDValueElems = getFormElementNameByTableCaption("Specimen Number", 0, 2);
        String[] participantIDFormElems = getFormElementNameByTableCaption("Participant Id", 0, 1);
        setFormElement(globalUniqueIDCompareElems[1], "CONTAINS");
        setFormElement(globalUniqueIDValueElems[0], "1416");
        setFormElement(participantIDFormElems[2], "999320528");

        */

        // Hard-code the element names, since code above is unpredictable
        selectOptionByValue("searchParams[0].compareType", "CONTAINS");
        setFormElement("searchParams[0].value", "1416");
        selectOptionByValue("searchParams[1].value", "999320528");

        clickNavButton("Search");
        assertTextPresent("350V06001416");
        clickLinkWithText("Show Vial and Request Options");
        // if our search worked, we'll only have six vials:
        assertLinkPresentWithTextCount("999320528", 6);
        assertTextNotPresent("DRT000XX-01");
        clickLinkWithText("[history]");
        assertTextPresent("GAA082NH-01");
        assertTextPresent("BAD");
        assertTextPresent("1.0&nbsp;ML");
        assertTextPresent("Added Comments");
        assertTextPresent("Johannesburg, South Africa");

        clickLinkWithText("Study 001");
        clickLinkWithText("View Existing Requests");
        clickNavButton("Details");
        assertTextPresent("WARNING: Missing Specimens");
        clickNavButton("Delete missing specimens");
        selenium.getConfirmation();
        assertTextNotPresent("WARNING: Missing Specimens");
        assertTextPresent("Duke University");
        assertTextPresent("An Assay Plan");
        assertTextPresent("Providing lab approval");
        assertTextPresent("HAQ0003Y-09");
        assertTextPresent("BAQ00051-09");
        assertTextNotPresent("BAQ00051-10");
        assertTextPresent("BAQ00051-11");

        log("Test editing rows in a dataset");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);

        clickLinkWithText("Permissions");
        clickNavButton("Study Security");

        selectOptionByValue("securityString", "BASIC_WRITE");
        selenium.waitForPageToLoad("30000");
        
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("DEM-1: Demographics");

        clickLinkWithText("edit");
        setFormElement("quf_DEMbdt", "2001-11-11");
        clickNavButton("Submit");
        clickMenuButton("QC State", "QCState:Pending Review");
        assertTextPresent("2001-11-11");

        log("Test adding a row to a dataset");
        clickNavButton("Insert New");
        clickNavButton("Submit");
        assertTextPresent("This field is required");
        setFormElement("quf_participantid", TEST_ADD_ENTRY);
        setFormElement("quf_SequenceNum", "123");
        clickNavButton("Submit");
        clickMenuButton("QC State", "QCState:All data");
        assertTextPresent(TEST_ADD_ENTRY);

        log("Test deleting rows in a dataset");
        checkCheckbox(Locator.raw("//input[contains(@value, '999320529')]"));
        clickNavButton("Delete Selected");
        selenium.getConfirmation();
        assertTextNotPresent("999320529");

        // configure QC state management to show all data by default so the next steps don't have to keep changing the state:
        clickLinkWithText("Study 001");
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage QC States");
        selectOptionByText("showPrivateDataByDefault", "All data");
        clickNavButton("Save");

        // additional report and security tests
        setupDatasetSecurity();
        createCharts();
        doTestSecurity();
    }

    protected void setupDatasetSecurity()
    {
        click(Locator.linkWithText("Projects"));
        sleep(3000);
        clickLinkWithText("StudyVerifyProject");
        clickLinkWithText("My Study");

        // create a test group and give it container read perms
        clickLinkWithText("Permissions");
        clickAndWait(Locator.xpath("//a[contains(@href, '/labkey/security/StudyVerifyProject/container.view?')]"));
        setFormElement("name", TEST_GROUP);
        clickAndWait(Locator.xpath("//input[@value='Create']"));

        // add user to the first test group
        selenium.click("managegroup/StudyVerifyProject/" + TEST_GROUP);
        selenium.waitForPageToLoad("30000");
        setFormElement("names", TEST_USER);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");
        clickAndWait(Locator.xpath("//a[contains(@href, '/labkey/security/StudyVerifyProject/container.view?')]"));

        selectOptionByText("//td[contains(text(), '" + TEST_GROUP + "')]/..//td/select", "Reader");
        clickNavButton("Update");

        // give the test group read access to only the DEM-1 dataset
        selenium.click("link=exact:*My Study");
        selenium.waitForPageToLoad("30000");
        clickNavButton("Study Security");

        // enable advanced study security
        selectOptionByValue("securityString", "ADVANCED_READ");
        selenium.waitForPageToLoad("30000");

        click(Locator.xpath("//td[.='" + TEST_GROUP + "']/..//th/input[@value='READOWN']"));
        clickAndWait(Locator.id("groupUpdateButton"));

        selectOptionByText("dataset.1", "Read");
        clickAndWait(Locator.xpath("//form[@id='datasetSecurityForm']//input[contains(@value, 'Save')]"));
    }

    protected void cleanPipelineItem(String item)
    {
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("Data Pipeline");
        if (isTextPresent(item))
        {
            checkCheckbox(Locator.raw("//td[contains(text(), '" + item + "')]/../td/input"));
            clickNavButton("Delete");
            assertTextNotPresent(item);
        }
    }

    protected void deleteRReports()
    {
        log("Clean up R Reports");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("Manage Reports and Views");
        for (String script : R_SCRIPTS)
        {
            while (isTextPresent(script))
            {
                click(Locator.raw("//a[contains(text(),'" + script + "')]/../../td[3]/a"));
                assertTrue(selenium.getConfirmation().matches("^Permanently delete the selected view[\\s\\S]$"));
                waitForPageToLoad();
            }
            assertTextNotPresent(script);
        }
    }

    protected boolean checkRSetup()
    {
        ensureAdminMode();
        clickLinkWithText("Admin Console");
        clickLinkWithText("R view configuration");
        log("Check if it already is configured");
        
        try
        {
            if (getAttribute(Locator.name("programPath"), "value") != null &&
                    getAttribute(Locator.name("programPath"), "value").compareTo("") != 0)
                  return true;
        }
        catch (SeleniumException e)
        {
            log("Ignoring Selenium Error");
            log(e.getMessage());
        }

        log("Try configuring R");
        String rHome = System.getenv("R_HOME");
        if (rHome != null)
        {
            log("R_HOME is set to: " + rHome + " searching for the R application");
            File rHomeDir = new File(rHome);
            File[] files = rHomeDir.listFiles(new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    if ("r.exe".equalsIgnoreCase(name) || "r".equalsIgnoreCase(name))
                        return true;
                    return false;
                }
            });

            for (File file : files)
            {
                setFormElement("programPath", file.getAbsolutePath());
                clickNavButton("Submit");
                if (isTextPresent("The R View configuration has been updated."))
                {
                    log("R has been successfully configured");
                    return true;
                }
            }
        }
        log("Failed R configuration, skipping R tests");
        log("Environment info: " + System.getenv());
        return false;
    }

    protected boolean comparePaths(String path1, String path2)
    {
        String[] parseWith = { "/", "\\\\" };
        for (String parser1 : parseWith)
        {
            String[] path1Split = path1.split(parser1);
            for  (String parser2 : parseWith)
            {
                String[] path2Split = path2.split(parser2);
                if (path1Split.length == path2Split.length)
                {
                    int index = 0;
                    while (path1Split[index].compareTo(path2Split[index]) == 0)
                    {
                        index++;
                        if (index > path2Split.length - 1)
                            return true;
                    }
                }
            }
        }
        return false;
    }

    protected boolean tryScript(String script, String verify)
    {
        log("Try script");

        if (!isLinkPresentWithText("Download input data") && isLinkPresentWithText("Source"))
        {
            clickLinkWithText("Source");
        }
        setFormElement(Locator.id("script"), script);
        clickNavButton("Execute Script");
        waitForPageToLoad();
        return (isTextPresent(verify));
    }

    protected void RReportTest()
    {
        log("Create an R Report");
        click(Locator.linkWithText("Projects"));
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText(DATA_SET);
        clickMenuButton("Views", "Views:Create", CREATE_R_MENU);

        log("Execute bad scripts");
        clickNavButton("Execute Script");
        assertTextPresent("An R script must be provided.");
        if (!tryScript(R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX) + "\nbadString", R_SCRIPT1_TEXT1))
            if (!tryScript(R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX.toLowerCase()) + "\nbadString", R_SCRIPT1_TEXT1))
                fail("Their was an error running the script");
        assertTextPresent("Error 1 executing command");
        assertTextPresent("Error: object \"badString\" not found");
        assertTextPresent(R_SCRIPT1_TEXT1);
        assertTextPresent(R_SCRIPT1_TEXT2);
        assertElementPresent(Locator.id(R_SCRIPT1_IMG));
        assertTextPresent(R_SCRIPT1_PDF);

        log("Execute and save a script");
        if (!tryScript(R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX), R_SCRIPT1_TEXT1))
            if (!tryScript(R_SCRIPT1(R_SCRIPT1_ORIG_FUNC, DATA_BASE_PREFIX.toLowerCase()), R_SCRIPT1_TEXT1))
                fail("Their was an error running the script");
        log("Check that the script executed properly");
        assertTextPresent(R_SCRIPT1_TEXT1);
        assertTextPresent(R_SCRIPT1_TEXT2);
        assertElementPresent(Locator.id(R_SCRIPT1_IMG));
        assertTextPresent(R_SCRIPT1_PDF);
        clickLinkWithText("Source");
        clickNavButton("Save View", 0);
        setFormElement("reportName", R_SCRIPTS[0]);
        clickNavButton("Save");

        log("Create view");
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        removeCustomizeViewColumn(R_REMCOL);
        addCustomizeViewFilter(R_FILTER, "3.Latino/a or Hispanic?", "Does Not Equal", "Yes");
        addCustomizeViewSort(R_SORT, "2.What is your sex?", "DESC");
        setFormElement("ff_columnListName", R_VIEW);
        clickNavButton("Save");

        log("Check that customize view worked");
        assertTextNotPresent(R_REMCOL);
        assertTextNotPresent(R_FILTERED);
        assertTextBefore(R_SORT1, R_SORT2);

        log("Check that R respects column changes, filters and sorts of data");
        pushLocation();
        clickMenuButton("Views", "Views:Create", CREATE_R_MENU);
        setFormElement(Locator.id("script"), "labkey.data");
        clickNavButton("Execute Script");
        assertTextNotPresent(R_REMCOL);
        assertTextNotPresent(R_FILTERED);
        assertTextBefore(R_SORT1, R_SORT2);
        popLocation();

        // no longer relevant, all views for the dataset are in the dropdown now
/*
        log("Check that R scripts from different view cant be accessed");
        clickNavButton("Reports >>", 0);
        assertTextNotPresent(R_SCRIPTS[0]);
        assertElementNotPresent(Locator.raw("//select[@name='Dataset.viewName']//option[.='" + R_SCRIPTS[0] + "']"));
*/

        log("Check saved R script");
        clickMenuButton("Views", "Views:default");
        pushLocation();
        //clickNavButton("Reports >>", 0);
        //clickLinkWithText(R_SCRIPTS[0]);
        clickMenuButton("Views", "Views:" + R_SCRIPTS[0]);
        assertTextPresent("null device");
        assertTextNotPresent("Error 1 executing command");
        assertTextPresent(R_SCRIPT1_TEXT1);
        assertTextPresent(R_SCRIPT1_TEXT2);
        assertElementPresent(Locator.id(R_SCRIPT1_IMG));
        assertTextPresent(R_SCRIPT1_PDF);
        popLocation();

        log("Create second R script");
        clickMenuButton("Views", "Views:Create", CREATE_R_MENU);
        click(Locator.raw("//td[contains(text(),'" + R_SCRIPTS[0] + "')]/input"));
        if (!tryScript(R_SCRIPT2(DATA_BASE_PREFIX, "participantId"), R_SCRIPT2_TEXT1))
            if (!tryScript(R_SCRIPT2(DATA_BASE_PREFIX.toLowerCase(), "participantid"), R_SCRIPT2_TEXT1))
                fail("Their was an error running the script");
        clickLinkWithText("Source");
        checkCheckbox("shareReport");
        checkCheckbox("runInBackground");
        clickNavButton("Execute Script");

        log("Check that R script worked");
        waitForPageToLoad();
        // Add once issue 3738 is fixed
//        assertElementNotPresent(Locator.id(R_SCRIPT1_IMG));
//        assertTextNotPresent(R_SCRIPT1_PDF);
        assertTextPresent(R_SCRIPT2_TEXT1);
        clickLinkWithText("Source");
        //click(Locator.raw("//td[contains(text(),'" + R_SCRIPTS[0] + "')]/input"));
        clickNavButton("Save View", 0);
        setFormElement("reportName", R_SCRIPTS[1]);
        clickNavButton("Save");

        log("Check that background run works");
        //clickNavButton("Reports >>", 0);
        //clickLinkWithText(R_SCRIPTS[1]);
        //selectOptionByText("Dataset.viewName", R_SCRIPTS[1]);
        //goToPipelineItem(R_SCRIPTS[1]);
        //assertTextPresent(R_SCRIPT2_TEXT1);

        log("Test user permissions");
        clickLinkWithText("Permissions");
        clickLink("managegroup/" + PROJECT_NAME + "/Users");
        setFormElement("names", USER1);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");
        clickLinkWithText("Permissions");
        setPermissions("Users", "Editor");
        impersonate(USER1);

        log("Access shared R script");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText(DATA_SET);
        pushLocation();
        //clickNavButton("Reports >>", 0);
        //assertTextNotPresent(R_SCRIPTS[0]);
        assertElementNotPresent(Locator.raw("//select[@name='Dataset.viewName']//option[.='" + R_SCRIPTS[0] + "']"));

        clickMenuButton("Views", "Views:" + R_SCRIPTS[1]);
        //goToPipelineItem(R_SCRIPTS[1]);
        //assertTextPresent(R_SCRIPT2_TEXT1);
        popLocation();
        // Exception is logged with the server and creates an error at the end
//        log("Try to create an R Report");
//        clickNavButton("Reports >>", 0);
//        clickLinkWithText("Create R Report");
//        assertTextPresent("401");
//        clickNavButton("Home");

        log("Change user permission");
        stopImpersonating();
        clickLinkWithText(PROJECT_NAME);
        if (isTextPresent("Enable Admin"))
            clickLinkWithText("Enable Admin");
        clickLinkWithText("Permissions");
        setPermissions("Users", "Admin (all permissions)");

        log("Create a new R script which uses others R scripts");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText(DATA_SET);
        clickMenuButton("Views", "Views:Create", CREATE_R_MENU);
        click(Locator.raw("//td[contains(text(),'" + R_SCRIPTS[0] + "')]/input"));
        click(Locator.raw("//td[contains(text(),'" + R_SCRIPTS[1] + "')]/input"));
        if (!tryScript(R_SCRIPT3(DATA_BASE_PREFIX, "participantId"), R_SCRIPT2_TEXT1))
            if (!tryScript(R_SCRIPT3(DATA_BASE_PREFIX.toLowerCase(), "participantid"), R_SCRIPT2_TEXT1))
                fail("Their was an error running the script");
        assertTextPresent(R_SCRIPT2_TEXT1);
        clickLinkWithText("Source");
        clickNavButton("Save View", 0);

        log("Test editing R scripts");
        signOut();
        signIn();
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("Manage Reports and Views");
        click(Locator.raw("//a[contains(text(),'" + R_SCRIPTS[0] + "')]/../..//a[.='edit']"));
        waitForPageToLoad();
        if (!tryScript(R_SCRIPT1(R_SCRIPT1_EDIT_FUNC, DATA_BASE_PREFIX), R_SCRIPT1_TEXT1))
            if (!tryScript(R_SCRIPT1(R_SCRIPT1_EDIT_FUNC, DATA_BASE_PREFIX.toLowerCase()), R_SCRIPT1_TEXT1))
                fail("Their was an error running the script");
        clickLinkWithText("Source");
        clickNavButton("Save View");

        log("Check that edit worked");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("Manage Reports and Views");
        click(Locator.raw("//a[contains(text(),'" + R_SCRIPTS[1] + "')]/../..//a[.='edit']"));
        waitForPageToLoad();
        checkCheckbox(Locator.name("includedReports"));
        clickNavButton("Execute Script");
        clickNavButton("Start Job");
        waitForPageToLoad();
        waitForElement(Locator.navButton("Start Job"), 30000);
        assertTextPresent(R_SCRIPT2_TEXT1);
        assertTextNotPresent(R_SCRIPT2_TEXT2);

        log("Clean up R pipeline jobs");
        cleanPipelineItem(R_SCRIPTS[1]);
    }

    protected void createCharts()
    {
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);

        clickLinkWithText("APX-1: Abbreviated Physical Exam");
        clickMenuButton("Views", "Views:Create", CREATE_CHART_MENU);
        waitForElement(Locator.xpath("//select[@name='columnsX']"), WAIT_FOR_GWT);
        selectOptionByText("columnsX", "1. Weight");
        selectOptionByText("columnsY", "4. Pulse");
        checkCheckbox("participantChart");
        clickNavButton("Save", 0);
        sleep(2000);

        setFormElement("reportName", "participant chart");
        clickNavButton("OK", 0);

        waitForElement(Locator.navButton("Views"), 5000);

        clickMenuButton("Views", "Views:default");
        clickMenuButton("Views", "Views:Create", CREATE_CHART_MENU);
        waitForElement(Locator.xpath("//select[@name='columnsX']"), WAIT_FOR_GWT);

        // create a non-participant chart
        selectOptionByText("columnsX", "1. Weight");
        selectOptionByText("columnsY", "4. Pulse");
        clickNavButton("Save", 0);
        sleep(2000);

        setFormElement("reportName", "non participant chart");
        setFormElement("description", "a private chart");
        checkCheckbox("shareReport");
        clickNavButton("OK", 0);

        waitForElement(Locator.navButton("Views"), 5000);

        // create grid view
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("Manage Reports and Views");
        clickLinkWithText("new grid view");
        setFormElement("label", TEST_GRID_VIEW);
        selectOptionByText("datasetSelection", "APX-1: Abbreviated Physical Exam");
        clickNavButton("Create View");
    }

    protected void doTestSecurity()
    {
        click(Locator.linkWithText("Projects"));
        sleep(3000);
        clickLinkWithText("StudyVerifyProject");
        clickLinkWithText("My Study");

        clickLinkWithText("Manage Reports and Views");
        clickAndWait(Locator.xpath("//a[.='participant chart']/../..//td/a[.='permissions']"));

        selenium.click("useExplicit");
        checkCheckbox(Locator.xpath("//td[.='" + TEST_GROUP + "']/..//td/input[@type='checkbox']"));
        clickNavButton("save");

        clickLinkWithText("Manage Reports and Views");
        clickAndWait(Locator.xpath("//a[.='" + TEST_GRID_VIEW + "']/../..//td/a[.='permissions']"));

        selenium.click("useExplicit");
        checkCheckbox(Locator.xpath("//td[.='" + TEST_GROUP + "']/..//td/input[@type='checkbox']"));
        clickNavButton("save");

        click(Locator.linkWithText("Manage Site"));
        sleep(3000);
        clickLinkWithText("Admin Console");
        impersonate(TEST_USER);
        clickLinkWithText("StudyVerifyProject");
        clickLinkWithText("My Study");

        assertLinkNotPresentWithText("APX-1: Abbreviated Physical Exam");
        clickLinkWithText("participant chart");

        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText(TEST_GRID_VIEW);
        assertTextPresent("999320016");
        pushLocation();
        clickMenuButton("Views", "Views:default");
        assertTextPresent("User does not have read permission on this dataset.");
/*
        no longer showing the query button by default.
        popLocation();
        clickMenuButton("Query", "Query:APX-1: Abbreviated Physical Exam");
        assertTextPresent("User does not have read permission on this dataset.");
*/
        stopImpersonating();
        deleteCharts();
    }

    protected void deleteCharts()
    {
        click(Locator.linkWithText("Projects"));
        sleep(3000);
        clickLinkWithText("StudyVerifyProject");
        clickLinkWithText("My Study");
        clickLinkWithText("Manage Reports and Views");
        click(Locator.xpath("//a[.='participant chart']/../..//td/a[.='delete']"));
		assertTrue(selenium.getConfirmation().matches("^Permanently delete the selected view[\\s\\S]$"));
        waitForPageToLoad();
        click(Locator.xpath("//a[.='non participant chart']/../..//td/a[.='delete']"));
		assertTrue(selenium.getConfirmation().matches("^Permanently delete the selected view[\\s\\S]$"));
        waitForPageToLoad();
    }
}
