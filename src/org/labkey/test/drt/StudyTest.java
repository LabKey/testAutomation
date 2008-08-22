/*
 * Copyright (c) 2008 LabKey Corporation
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

import org.apache.commons.lang.time.FastDateFormat;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.*;
import java.util.Date;
import java.net.URL;

/**
 * User: brittp
 * Date: Mar 9, 2006
 * Time: 1:54:57 PM
 */
public class StudyTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "StudyVerifyProject";
    protected static final String FOLDER_NAME = "My Study";
    protected static final String VISIT_MAP = "/sampledata/study/v068_visit_map.txt";
    private static final String CRF_SCHEMAS = "/sampledata/study/schema.tsv";
    private static final String SPECIMEN_ARCHIVE_A = "/sampledata/study/sample_a.specimens";
    private static final String ARCHIVE_TEMP_DIR = "/sampledata/study/drt_temp";
    private static final int MAX_WAIT_SECONDS = 4*60;
    private String _studyDataRoot = null;
    private int _completedSpecimenImports = 0;


    public String getAssociatedModuleDirectory()
    {
        return "study";
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void doCleanup() throws Exception
    {
        try { deleteProject(PROJECT_NAME); } catch (Throwable e) {}
    }

    protected void doTestSteps()
    {
        createStudy();

        // verify reports
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("Manage Reports and Views");
        clickLinkWithText("new enrollment view");
        selectOptionByText("datasetId", "1: DEM-1: Demographics");
        waitForPageToLoad();
        URL lastPageURL = getLastPageURL();
        //selenium.open(lastPageURL + "?datasetId=1");
        selectOptionByText("sequenceNum", "Screening");
//        selectOption("propertyId", "Initial Specimen Coll Dt");
        clickNavButton("Submit");
        assertImagePresentWithSrc("timePlot.view?reportId=", true);

        clickLinkWithText("Study 001");
        clickLinkWithText("Study Navigator");
        clickLinkWithText("24");

        // verify that cohorts are working
        assertTextPresent("999320016");
        assertTextPresent("999320518");

        clickMenuButton("Cohorts", "Cohorts:Group 1");
        waitForPageToLoad();
        assertTextPresent("999320016");
        assertTextNotPresent("999320518");

        clickMenuButton("Cohorts", "Cohorts:Group 2");
        waitForPageToLoad();
        assertTextNotPresent("999320016");
        assertTextPresent("999320518");

        // verify that the participant view repsects the cohort filter:
        clickLinkWithText("999320518");
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Participant >");
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Participant >");
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Participant >");

        clickLinkWithText("Dataset: DEM-1: Demographics, All Visits");

        clickMenuButton("Views", "Views:Create", "Views:Create:Crosstab View");
        selectOptionByValue("rowField",  "DEMsex");
        selectOptionByValue("colField", "DEMsexor");
        selectOptionByValue("statField", "SequenceNum");
        clickNavButton("Submit");

        String[] row3 = new String[] {"Male", "2", "9", "3", "14"};
        assertTableRowsEqual("report", 3, new String[][] {row3});

        setFormElement("label", "TestReport");
        selectOptionByText("showWithDataset", "DEM-1: Demographics");
        clickNavButton("Save");

        clickLinkWithText("Study 001");
        assertTextPresent("TestReport");
        clickLinkWithText("TestReport");

        assertTableCellTextEquals("report", 2, 0, "Female");

        //Delete the report
        clickLinkWithText("Study 001");
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Reports and Views");
        click(Locator.linkWithText("delete"));
        selenium.getConfirmation();
        waitForPageToLoad();

        click(Locator.linkWithText("delete"));
        selenium.getConfirmation();
        waitForPageToLoad();
        assertLinkNotPresentWithText("TestReport");

        // create new grid view report:
        String viewName = "DRT Eligibility Query";
        clickLinkWithText("new grid view");
        setFormElement("label", viewName);
        selectOptionByText("params", "ECI-1: Eligibility Criteria");
        clickNavButton("Create View");
        assertLinkPresentWithText("999320016");
        //Not sure what we are lookgin for here
        //assertTextPresent("urn:lsid");
        assertNavButtonNotPresent("go");
        clickLinkWithText("Study 001");
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Reports and Views");

        click(Locator.linkWithText("delete"));
        selenium.getConfirmation();
        waitForPageToLoad(longWaitForPage);

        assertTextNotPresent(viewName);

        // create new external report
        clickLinkWithText("Study 001");
        clickLinkWithText("DEM-1: Demographics");
        clickMenuButton("Views", "Views:Create", "Views:Create:Advanced View");
        selectOptionByText("queryName", "DEM-1: Demographics");
        String java = System.getProperty("java.home") + "/bin/java";
        setFormElement("commandLine", java + " -cp " + getLabKeyRoot() + "/server/test/build/classes org.labkey.test.util.Echo ${DATA_FILE} ${REPORT_FILE}");
        clickNavButton("Submit");
        assertTextPresent("Female");
        setFormElement("commandLine", java + " -cp " + getLabKeyRoot() + "/server/test/build/classes org.labkey.test.util.Echo ${DATA_FILE}");
        selectOptionByValue("fileExtension", "tsv");
        clickNavButton("Submit");
        assertTextPresent("Female");
        setFormElement("label", "tsv");
        selectOptionByText("showWithDataset", "DEM-1: Demographics");
        clickNavButton("Save");
        clickLinkWithText("Study 001");
        clickLinkWithText("tsv");
        assertTextPresent("Female");

        // test custom assays
        clickLinkWithText("Study 001");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Create New Dataset");
        setFormElement("typeName", "verifyAssay");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='DatasetDesignerName']"), WAIT_FOR_GWT);

        checkCheckbox("additionalKey", 1, true);

        clickNavButton("Import Schema", 0);
        waitForElement(Locator.xpath("//textarea[@id='schemaImportBox']"), WAIT_FOR_GWT);

        setFormElement("schemaImportBox", "Property\tLabel\tRangeURI\tNotNull\tDescription\n" +
                "SampleId\tSample Id\txsd:string\ttrue\tstring\n" +
                "DateField\tDateField\txsd:dateTime\tfalse\tThis is a date field\n" +
                "NumberField\tNumberField\txsd:double\ttrue\tThis is a number\n" +
                "TextField\tTextField\txsd:string\tfalse\tThis is a text field");

        clickNavButton("Import", 0);
        waitForElement(Locator.xpath("//input[@id='ff_label3']"), WAIT_FOR_GWT);

        click(Locator.xpath("//span[@id='button_dataField']"));
        selenium.select("list_dataField", "label=Sample Id");

        clickNavButton("Save");
        waitForElement(Locator.navButton("Import Data"), WAIT_FOR_GWT);
        clickNavButton("Import Data");

        String tsv = "participantid\tsequencenum\tvisitdate\tSampleId\tDateField\tNumberField\tTextField\treplace\n" +
                "1234\t1\t1/1/2006\t1234_A\t2/1/2006\t1.2\ttext\t\n" +
                "1234\t1\t1/1/2006\t1234_B\t2/1/2006\t1.2\ttext\t\n";
        String errorRow = "\tbadvisitd\t1/1/2006\t\ttext\t";
        setFormElement("tsv", tsv + "\n" + errorRow);
        clickNavButton("Import Data");
        assertTextPresent("Row 3 does not contain required field participantid.");
        assertTextPresent("Row 3 data type error for field SequenceNum.");
        assertTextPresent("Row 3 does not contain required field SampleId.");
        assertTextPresent("Row 3 data type error for field DateField.");
        assertTextPresent("Row 3 does not contain required field NumberField.");

        setFormElement("tsv", tsv);
        clickNavButton("Import Data", longWaitForPage);
        assertTextPresent("1234");
        assertTextPresent("2006-02-01");
        assertTextPresent("1.2");
        assertTextNotPresent("QC State");

        // configure QC state management before our second upload
        clickLinkWithText("Study 001");
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage QC States");
        setFormElement("newLabel", "Approved");
        setFormElement("newDescription", "We all like approval.");
        clickNavButton("Save");
        setFormElement("newLabel", "Pending Review");
        setFormElement("newDescription", "No one likes to be reviewed.");
        clickCheckbox("newPublicData", false);
        clickNavButton("Save");
        selectOptionByText("defaultDirectEntryQCState", "Pending Review");
        clickNavButton("Save");

        // return to dataset import page
        clickLinkWithText("Study 001");
        clickLinkWithText("verifyAssay");
        assertTextPresent("QC State");
        assertTextNotPresent("1234");
        clickMenuButton("QC State", "QCState:All data");
        assertTextPresent("Pending Review");
        assertTextPresent("1234");

        //Import same data again
        clickNavButton("Import Data");
        setFormElement("tsv", tsv);
        clickNavButton("Import Data");
        assertTextPresent("Duplicates were found");
        //Now explicitly replace
        tsv = "participantid\tsequencenum\tvisitdate\tSampleId\tDateField\tNumberField\tTextField\treplace\n" +
                "1234\t1\t1/1/2006\t1234_A\t2/1/2006\t5000\tnew text\tTRUE\n" +
                "1234\t1\t1/1/2006\t1234_B\t2/1/2006\t5000\tnew text\tTRUE\n";
        setFormElement("tsv", tsv);
        clickNavButton("Import Data");
        assertTextPresent("5000.0");
        assertTextPresent("new text");
        assertTextPresent("QC State");
        assertTextPresent("Pending Review");

        // upload specimen data and verify import
        clickLinkWithText("Study 001");
        importSpecimenArchive(SPECIMEN_ARCHIVE_A);
        clickLinkWithText("Study 001");
        clickLinkWithText("Blood (Whole)");
        assertTextPresent("350V06001416");
        assertTextNotPresent("DRT000XX-01");
        assertTextPresent("GAA082NH-01");
        clickLinkWithText("Hide Vial and Request Options");
        assertTextPresent("Total:");
        assertTextPresent("444");

        assertTextNotPresent("BAD");

        clickLinkWithText("Show Vial and Request Options");
        clickLinkContainingText("history");
        assertTextPresent("1.0&nbsp;ML");
        assertTextNotPresent("Added Comments");
        assertTextPresent("Johannesburg, South Africa");
    }

    protected void createStudy()
    {
        _studyDataRoot = getLabKeyRoot() + "/sampledata/study";

        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "Study", null, true);
        clickNavButton("Create Study");
        click(Locator.checkboxByNameAndValue("simpleRepository", "false", true));
        clickNavButton("Create Study");

        // change study label
        clickLinkWithText("Change Label");
        setFormElement("label", "Study 001");
        clickNavButton("Update");
        assertTextPresent("Study 001");

        // import visit map
        clickLinkWithText("Manage Visits");
        clickLinkWithText("Import Visit Map");
        String visitMapData = getFileContents(VISIT_MAP);
        setLongTextField("content", visitMapData);
        clickNavButton("Import");

        // define forms
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Define Dataset Schemas");
        clickLinkWithText("Bulk Import Schemas");
        setFormElement("typeNameColumn", "platename");
        setFormElement("labelColumn", "platelabel");
        setFormElement("typeIdColumn", "plateno");
        setLongTextField("tsv", getFileContents(CRF_SCHEMAS));
        clickNavButton("Submit", 180000);
        assertTextPresent("DEM-1: Demographics");
        clickLinkWithText("489");
        assertTextPresent("ESIdt");
        assertTextPresent("Form Completion Date");

        // setup cohorts:
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Cohorts");
        selectOptionByText("participantCohortDataSetId", "EVC-1: Enrollment Vaccination");
        waitForPageToLoad();
        selectOptionByText("participantCohortProperty", "2. Enrollment group");
        clickNavButton("Update Assignments");

        // hide visits:
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Visits");
        for (int i = 0; i < 2; i++)
        {
            clickLinkWithText("edit", i);
            uncheckCheckbox("showByDefault");
            clickNavButton("Save");
        }

        // test optional/required/not associated
        clickLinkWithText("edit", 0);
        selectOption("dataSetStatus", 0, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 1, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 2, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 3, "OPTIONAL");
        selectOption("dataSetStatus", 4, "OPTIONAL");
        selectOption("dataSetStatus", 5, "OPTIONAL");
        selectOption("dataSetStatus", 6, "REQUIRED");
        selectOption("dataSetStatus", 7, "REQUIRED");
        selectOption("dataSetStatus", 8, "REQUIRED");
        clickNavButton("Save");
        clickLinkWithText("edit", 0);
        selectOption("dataSetStatus", 0, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 1, "OPTIONAL");
        selectOption("dataSetStatus", 2, "REQUIRED");
        selectOption("dataSetStatus", 3, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 4, "OPTIONAL");
        selectOption("dataSetStatus", 5, "REQUIRED");
        selectOption("dataSetStatus", 6, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 7, "OPTIONAL");
        selectOption("dataSetStatus", 8, "REQUIRED");
        clickNavButton("Save");
        clickLinkWithText("edit", 0);
        assertSelectOption("dataSetStatus", 0, "NOT_ASSOCIATED");
        assertSelectOption("dataSetStatus", 1, "OPTIONAL");
        assertSelectOption("dataSetStatus", 2, "REQUIRED");
        assertSelectOption("dataSetStatus", 3, "NOT_ASSOCIATED");
        assertSelectOption("dataSetStatus", 4, "OPTIONAL");
        assertSelectOption("dataSetStatus", 5, "REQUIRED");
        assertSelectOption("dataSetStatus", 6, "NOT_ASSOCIATED");
        assertSelectOption("dataSetStatus", 7, "OPTIONAL");
        assertSelectOption("dataSetStatus", 8, "REQUIRED");

        clickLinkWithText("Study 001");
        clickLinkWithText("Study Navigator");
        assertTextNotPresent("Screening Cycle");
        assertTextNotPresent("Cycle 1");
        assertTextPresent("Pre-exist Cond");
        clickLinkWithText("Show Hidden Data");
        assertTextPresent("Screening Cycle");
        assertTextPresent("Cycle 1");
        assertTextPresent("Pre-exist Cond");

        // upload data:
        clickLinkWithText("Study 001");
        clickLinkWithText("Data Pipeline");
        clickNavButton("Setup");
        setFormElement("path", _studyDataRoot);
        submit();
        clickLinkWithText("Pipeline");
        clickNavButton("Process and Import Data");
        if (isNavButtonPresent("Delete log"))
            clickNavButton("Delete log");
        generateFiles();
        if (!isNavButtonPresent("Import datasets"))
            fail("Datasets must be generated: run 'makePlates.bat' from within sampledata/study.");
        clickNavButton("Import datasets");
        clickNavButton("Submit");

        // Unfortunately isLinkWithTextPresent also picks up the "Errors" link in the header,
        // and it picks up the upload of 'FPX-1: Final Complete Physical Exam' as containing complete.
        // we exclude the word 'REPLACE' to catch this case:
        startTimer();
        while ((!isLinkPresentWithText("COMPLETE") || isLinkPresentWithText("REPLACE")) &&
                !isLinkPresentWithText("ERROR") &&
                elapsedSeconds() < MAX_WAIT_SECONDS)
        {
            log("Waiting for data import");
            sleep(1000);
            refresh();
        }
        assertLinkNotPresentWithText("ERROR");  // Must be surrounded by an anchor tag.
        assertLinkPresentWithTextCount("COMPLETE", 1);

        clickLinkWithText("Study 001");
        clickLinkWithText("Study Navigator");
        clickLinkWithText("24");
        assertTextPresent("Male");
        assertTextPresent("African American or Black");
        clickLinkWithText("999320016");
        assertTextPresent("right deltoid");
    }

    protected void importSpecimenArchive(String archivePath)
    {
        log("Starting import of specimen archive " + archivePath);
        File copiedArchive = new File(new File(getLabKeyRoot() + ARCHIVE_TEMP_DIR), FastDateFormat.getInstance("MMddHHmmss").format(new Date()) + ".specimens");
        File specimenArchive = new File(getLabKeyRoot() + archivePath);
        // copy the file into its own directory
        copyFile(specimenArchive, copiedArchive);
        clickLinkWithText("Data Pipeline");
        assertLinkPresentWithTextCount("COMPLETE", _completedSpecimenImports + 1);
        clickNavButton("Process and Import Data");
        String tempDirShortName = ARCHIVE_TEMP_DIR.substring(ARCHIVE_TEMP_DIR.lastIndexOf('/') + 1);
        clickLinkWithText(tempDirShortName);
        clickNavButton("Import specimen data");
        clickNavButton("Start Import");

        // Unfortunately isLinkWithTextPresent also picks up the "Errors" link in the header.
        startTimer();
        while (countLinksWithText("COMPLETE") == _completedSpecimenImports + 1 && !isLinkPresentWithText("ERROR") && elapsedSeconds() < MAX_WAIT_SECONDS)
        {
            log("Waiting for specimen import...");
            sleep(1000);
            refresh();
        }
        // Unfortunately assertNotLinkWithText also picks up the "Errors" link in the header.
        assertLinkNotPresentWithText("ERROR");  // Must be surrounded by an anchor tag.
        assertLinkPresentWithTextCount("COMPLETE", _completedSpecimenImports + 2);
        _completedSpecimenImports++;
        copiedArchive.delete();
    }


    private void generateFiles()
    {
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(_studyDataRoot + "/v068_dump.sql"));
            String s;
            while (null != (s = in.readLine()))
            {
                if (!s.startsWith("COPY"))
                    continue;

                String copy = s;
                String head = copy.substring(s.indexOf('(') + 1);
                head = head.substring(0, head.indexOf(')'));
                String plate = s.substring(5, s.indexOf('(')).trim();

                String[] cols = head.split(",");
                String header = "";
                for (String c : cols)
                    header += "\t" + c.trim();
                header = header.trim();

                File f = new File(_studyDataRoot + "/" + plate + ".tsv");
                PrintWriter out = new PrintWriter(new FileWriter(f));
                log("Generated plate " + f.getPath());
                out.println("# " + plate);
                out.println(header);
                while (null != (s = in.readLine()) && !s.startsWith("\\."))
                    out.println(s);
                out.flush();
                out.close();
            }
        }
        catch (IOException e)
        {
            fail("Unable to generate data files: " + e.getMessage());
        }
    }

    private void deleteGeneratedFiles()
    {
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(_studyDataRoot + "/v068_dump.sql"));
            String s;
            while (null != (s = in.readLine()))
            {
                if (!s.startsWith("COPY"))
                    continue;

                String plate = s.substring(5, s.indexOf('(')).trim();
                File f = new File(_studyDataRoot + "/" + plate + ".tsv");
                f.delete();
            }
        }
        catch (IOException e)
        {
            fail("Unable to generate data files: " + e.getMessage());
        }
    }

    private void deleteLogFiles()
    {
        File dataRoot = new File(_studyDataRoot);
        File[] logFiles = dataRoot.listFiles(new FilenameFilter(){
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".log");
            }
        });
        for (File f : logFiles)
            f.delete();
    }

    private void deleteAssayUploadFiles()
    {
        deleteDir(new File(_studyDataRoot, "assaydata"));
    }

    private void deleteReportFiles()
    {
        deleteDir(new File(_studyDataRoot, "Reports"));
    }

    private void deleteDir(File dir)
    {
        if (!dir.exists())
            return;

        File[] assayDataFiles = dir.listFiles();
        for (File f : assayDataFiles)
            f.delete();

        dir.delete();
    }

    long start = 0;
    private void startTimer()
    {
        start = System.currentTimeMillis();
    }


    private int elapsedSeconds()
    {
        return (int)((System.currentTimeMillis() - start) / 1000);
    }

    protected void selectOption(String name, int i, String value)
    {
        selectOptionByValue(Locator.tagWithName("select", name).index(i), value);
    }

    protected void assertSelectOption(String name, int i, String expected)
    {
        assertEquals(selenium.getSelectedValue(Locator.tagWithName("select", name).index(i).toString()), expected);
    }
}
