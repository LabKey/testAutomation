package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.admin.ExportFolderPage;
import org.labkey.test.pages.admin.ImportFolderPage;
import org.labkey.test.pages.query.ExecuteQueryPage;
import org.labkey.test.pages.study.ManageStudyPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.StudyHelper;
import org.labkey.test.util.TestDataGenerator;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 10)
public class SampleTypeLinkToStudyTest extends BaseWebDriverTest
{
    final static String SAMPLE_TYPE_PROJECT = "Sample Type Test Project";
    final static String VISIT_BASED_STUDY = "Visit Based Study Test Project";
    final static String DATE_BASED_STUDY = "Date Based Study Test Project";
    final static String ASSAY_NAME = "Test assay";
    final static String SAMPLE_TYPE1 = "Sample type 1";
    final static String SAMPLE_TYPE2 = "Sample type 2";

    private static int cnt = 0; // to keep count of rows which are already linked.

    protected DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    protected String now = LocalDateTime.now().format(_dateTimeFormatter);

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        SampleTypeLinkToStudyTest init = (SampleTypeLinkToStudyTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createProject(VISIT_BASED_STUDY, "Study");
        _studyHelper.startCreateStudy()
                .setTimepointType(StudyHelper.TimepointType.VISIT)
                .createStudy();

        _containerHelper.createProject(DATE_BASED_STUDY, "Study");
        _studyHelper.startCreateStudy()
                .setTimepointType(StudyHelper.TimepointType.DATE)
                .createStudy();

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        new PortalHelper(getDriver()).addBodyWebPart("Sample Types");

        goToProjectHome(VISIT_BASED_STUDY);
        new PortalHelper(getDriver()).addBodyWebPart("Datasets");

        goToProjectHome(DATE_BASED_STUDY);
        new PortalHelper(getDriver()).addBodyWebPart("Datasets");

        createSampleTypes();
    }

    private void createSampleTypes()
    {
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        log("Creating sample types");
        String data1 = "Name\tVisitId\tVisitDate\tParticipantId\n" +
                "blood\t1\t" + now + "\tP1\n" +
                "urine\t2\t" + now + "\tP2\n" +
                "plasma\t3\t" + now + "\tP3\n" +
                "stool\t4\t" + now + "\tP4\n";
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition(SAMPLE_TYPE1)
                .setFields(List.of(
                        new FieldDefinition("VisitId", FieldDefinition.ColumnType.VisitId),
                        new FieldDefinition("VisitDate", FieldDefinition.ColumnType.VisitDate),
                        new FieldDefinition("ParticipantId", FieldDefinition.ColumnType.Subject))), data1);

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        String data2 = "Name\tVisitDate\tParticipantId\n" +
                "First\t" + now + "\tP1\n" +
                "Second\t" + now + "\tP2\n" +
                "Third\t" + now + "\tP3\n" +
                "Fourth\t" + now + "\tP4\n";

        sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition(SAMPLE_TYPE2)
                .setFields(List.of(
                        new FieldDefinition("VisitDate", FieldDefinition.ColumnType.VisitDate),
                        new FieldDefinition("ParticipantId", FieldDefinition.ColumnType.Subject))), data2);
    }

    @Test
    public void testLinkToStudy()
    {
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        int numOfRowsLinked = 2;
        log("Linking sample types two rows to study");
        linkToStudy(VISIT_BASED_STUDY, SAMPLE_TYPE1, numOfRowsLinked, null);

        log("Verifying the linked sample type in study");
        goToProjectHome(VISIT_BASED_STUDY);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE1));
        DataRegionTable table = new DataRegionTable("Dataset", getDriver());

        checker().verifyEquals("Incorrect number of rows linked", 2, table.getDataRowCount());
        checker().verifyEquals("Incorrect Participant ID's", Arrays.asList("P3", "P4"), table.getColumnDataAsText("ParticipantId"));
        checker().verifyEquals("Incorrect category for the dataset(Uncategorized case)", " ", getCategory(VISIT_BASED_STUDY, SAMPLE_TYPE1));

        log("Verifying log entries");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE1));
        String expectedComment = "2 row(s) were linked to a study from the sample type: " + SAMPLE_TYPE1;
        verifyLinkToHistory(expectedComment);
        verifyAuditLogEvents(expectedComment, numOfRowsLinked, Arrays.asList("stool", "plasma"));
    }

    @Test
    public void testDatasetRecall()
    {
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(DATE_BASED_STUDY, SAMPLE_TYPE2, 1, null);

        recallDataset(DATE_BASED_STUDY, SAMPLE_TYPE2);

        log("Verifying log entries");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE2));
        String expectedComment = "1 row(s) were recalled from a study to the sample type: " + SAMPLE_TYPE2;
        verifyLinkToHistory(expectedComment);
        verifyAuditLogEvents(expectedComment, null, null);
    }

    @Test
    public void testSampleTypeLinkedToMultipleStudy()
    {
        log("Linking the Sample type to " + VISIT_BASED_STUDY);
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(VISIT_BASED_STUDY, SAMPLE_TYPE1, 1, null);

        log("Linking the Sample type to " + DATE_BASED_STUDY);
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(DATE_BASED_STUDY, SAMPLE_TYPE1, 1, null);

        log("Verifying link to " + VISIT_BASED_STUDY);
        goToProjectHome(VISIT_BASED_STUDY);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE1));
        DataRegionTable table = new DataRegionTable("Dataset", getDriver());
        checker().verifyEquals("Incorrect of " + SAMPLE_TYPE1 + "to study" + VISIT_BASED_STUDY, 1, table.getDataRowCount());

        log("Verifying link to " + DATE_BASED_STUDY);
        goToProjectHome(DATE_BASED_STUDY);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE1));
        table = new DataRegionTable("Dataset", getDriver());
        checker().verifyEquals("Incorrect of " + SAMPLE_TYPE1 + "to study" + DATE_BASED_STUDY, 1, table.getDataRowCount());

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE1));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        checker().verifyTrue("Missing linked column for Visit based study",
                samplesTable.getColumnNames().contains("linked_to_Visit_Based_Study_Test_Project_Study"));
        checker().verifyTrue("Missing linked column for Date based study",
                samplesTable.getColumnNames().contains("linked_to_Date_Based_Study_Test_Project_Study"));
    }

    @Test
    public void testAutoLinkToStudy()
    {
        String sampleName = "SampleTypeWithAutoLinkToStudy";
        String categoryName = "CAT1";

        log("Creating sample type with auto link enabled");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition(sampleName)
                .setAutoLinkDataToStudy("/" + VISIT_BASED_STUDY)
                .setLinkedDatasetCategory(categoryName)
                .setFields(List.of(
                        new FieldDefinition("VisitID", FieldDefinition.ColumnType.VisitId),
                        new FieldDefinition("Date", FieldDefinition.ColumnType.VisitDate),
                        new FieldDefinition("ParticipantID", FieldDefinition.ColumnType.Subject))));

        log("Inserting row into the sample type");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        clickAndWait(Locator.linkWithText(sampleName));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        samplesTable.clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), "one");
        setFormElement(Locator.name("quf_VisitID"), "1");
        setFormElement(Locator.name("quf_Date"), "12/12/2020");
        setFormElement(Locator.name("quf_ParticipantID"), "P1");
        clickButton("Submit");

        samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        checker().verifyTrue("Missing linked column",
                samplesTable.getColumnNames().contains("linked_to_Visit_Based_Study_Test_Project_Study"));
        checker().verifyEquals("Missing auto link for the inserted row", "linked",
                samplesTable.getDataAsText(0, "linked_to_Visit_Based_Study_Test_Project_Study"));

        checker().verifyEquals("Incorrect category for the dataset when auto linked.", categoryName,
                getCategory(VISIT_BASED_STUDY, sampleName));
    }

    @Test
    public void testDeleteLinkedSampleType()
    {
        String sampleName = "SampleTypeDelete";
        log("Creating sample types for deletion");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        String data = "Name\tVisitId\tVisitDate\tParticipantId\n" +
                "blood\t1\t" + now + "\tP1\n" +
                "urine\t2\t" + now + "\tP2\n";
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition(sampleName)
                .setFields(List.of(
                        new FieldDefinition("VisitId", FieldDefinition.ColumnType.VisitId),
                        new FieldDefinition("VisitDate", FieldDefinition.ColumnType.VisitDate),
                        new FieldDefinition("ParticipantId", FieldDefinition.ColumnType.Subject))), data);

        log("Linking sample types to studies");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(VISIT_BASED_STUDY, sampleName, 1, null);

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(DATE_BASED_STUDY, sampleName, 2, null);

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        clickAndWait(Locator.linkWithText(sampleName));

        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        samplesTable.checkCheckbox(0);
        samplesTable.clickHeaderButton("Delete");

        Window error = Window.Window(getDriver()).withTitle("Permanently delete 1 sample").waitFor();
        String expectedErrorMsg = "The selected sample will be permanently deleted.\n" +
                "\n" +
                "The selected row(s) will also be deleted from the linked dataset(s) in the following studies:\n" +
                DATE_BASED_STUDY + " Study\n" + VISIT_BASED_STUDY + " Study\n" + "Deletion cannot be undone. Do you want to proceed?";
        checker().verifyEquals("Incorrect delete message", expectedErrorMsg, error.getBody());
        error.clickButton("Yes, Delete", true);
    }

    @Test
    public void testLineageSupport()
    {
        String derivedSampleName = "Derived Plasma";
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        String data = "Name\tVisitId\tVisitDate\tParticipantId\n" +
                "BL-1\t1\t" + now + "\tP1\n" +
                "BL-2\t2\t" + now + "\tP2\n";
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition("Blood")
                .setFields(List.of(
                        new FieldDefinition("VisitId", FieldDefinition.ColumnType.VisitId),
                        new FieldDefinition("VisitDate", FieldDefinition.ColumnType.VisitDate),
                        new FieldDefinition("ParticipantId", FieldDefinition.ColumnType.Subject))), data);

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        data = "Name\tVolume\n" +
                "PL-1\t10\n";
        sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition("Plasma")
                .setFields(List.of(
                        new FieldDefinition("Volume", FieldDefinition.ColumnType.Integer))), data);

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        clickAndWait(Locator.linkWithText("Blood"));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        samplesTable.setSort("Name", SortDirection.ASC);
        samplesTable.checkCheckbox(0);
        samplesTable.clickHeaderButton("Derive Samples");
        selectOptionByText(Locator.name("targetSampleTypeId"), "Plasma in /" + SAMPLE_TYPE_PROJECT);
        clickButton("Next");
        setFormElement(Locator.name("outputSample1_Name"), derivedSampleName);
        setFormElement(Locator.name("outputSample1_Volume"), "1");
        clickButton("Submit");

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        clickAndWait(Locator.linkWithText("Plasma"));

        samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        CustomizeView customizeView = samplesTable.getCustomizeView();
        customizeView.openCustomizeViewPanel();
        customizeView.showHiddenItems();
        customizeView.addColumn("INPUTS/MATERIALS/BLOOD/VisitDate");
        customizeView.addColumn("INPUTS/MATERIALS/BLOOD/ParticipantId");
        customizeView.saveCustomView();
        samplesTable.checkCheckbox(samplesTable.getRowIndex("Name", derivedSampleName));
        samplesTable.clickHeaderButtonAndWait("Link to Study");

        log("Link to study: Choose target");
        selectOptionByText(Locator.id("targetStudy"), "/" + DATE_BASED_STUDY + " (" + DATE_BASED_STUDY + " Study)");
        clickButton("Next");
        new DataRegionTable("query", getDriver()).clickHeaderButtonAndWait("Link to Study");

        DataRegionTable table = new DataRegionTable("Dataset", getDriver());
        Map<String, String> rowData = table.getRowDataAsMap(0);
        checker().verifyEquals("Incorrect Name linked", derivedSampleName, rowData.get("Name"));
        checker().verifyEquals("Incorrect Participant ID linked", "P1", rowData.get("ParticipantId"));
        checker().verifyEquals("Incorrect Date linked", now, rowData.get("date"));
    }

    @Test
    public void testAssaySupport()
    {
        String importData = "Samples\nstool";
        String runName = "First Run";
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        goToManageAssays();
        ReactAssayDesignerPage assayDesignerPage = _assayHelper.createAssayDesign("General", ASSAY_NAME);
        assayDesignerPage.goToResultsFields()
                .removeField("ParticipantID")
                .removeField("Date")
                .addField("Samples")
                .setType(FieldDefinition.ColumnType.Sample)
                .setSampleType(SAMPLE_TYPE1);
        assayDesignerPage.clickFinish();

        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        DataRegionTable table = new DataRegionTable("Runs", getDriver());
        table.clickHeaderButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("name"), runName);
        setFormElement(Locator.name("TextAreaDataCollector.textArea"), importData);
        clickButton("Save and Finish");

        clickAndWait(Locator.linkWithText(runName));
        table = new DataRegionTable("Data", getDriver());
        CustomizeView customizeView = table.getCustomizeView();
        customizeView.openCustomizeViewPanel();
        customizeView.showHiddenItems();
        customizeView.addColumn("Samples/ParticipantId");
        customizeView.addColumn("Samples/VisitDate");
        customizeView.saveCustomView();

        table.checkCheckbox(0);
        table.clickHeaderButton("Link to Study");
        selectOptionByText(Locator.id("targetStudy"), "/" + DATE_BASED_STUDY + " (" + DATE_BASED_STUDY + " Study)");
        clickButton("Next");

        checker().verifyEquals("Incorrect Participant ID deduced", "P4", Locator.name("participantId").findElement(getDriver()).getAttribute("value"));
        checker().verifyEquals("Incorrect Visit ID deduced", now, Locator.name("date").findElement(getDriver()).getAttribute("value"));

        new DataRegionTable("Data", getDriver()).clickHeaderButtonAndWait("Link to Study");
    }

    @Test
    public void testQCState()
    {
        goToProjectHome(DATE_BASED_STUDY);
        clickAndWait(Locator.linkWithText("Manage Study"));
        ManageStudyPage studyPage = new ManageStudyPage(getDriver());
        studyPage.manageDatasetQCStates()
                .addStateRow("Approved", "We all like approval", true)
                .addStateRow("Pending Review", "Still deciding", true)
                .addStateRow("Rejected", "No one likes to be reviewed.", true)
                .clickSave()                    // have to save the form here; default entry qc state needs a page cycle to be selectable below
                .manageDatasetQCStates()
                .setDefaultPublishDataQCState("Pending Review")
                .clickSave();

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(DATE_BASED_STUDY, SAMPLE_TYPE1, 2, null);

        goToProjectHome(DATE_BASED_STUDY);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE1));
        DataRegionTable table = new DataRegionTable("Dataset", getDriver());
        CustomizeView customizeView = table.openCustomizeGrid();
        customizeView.addColumn("QCState");
        customizeView.saveCustomView();
        checker().verifyEquals("Incorrect value for QC state", Arrays.asList("Pending Review?", "Pending Review?"), table.getColumnDataAsText("QCState"));

        log("Changing the QC state of one row");
        table.checkCheckbox(0);
        new BootstrapMenu.BootstrapMenuFinder(getDriver()).withButtonTextContaining("QC State").find().clickSubMenu(true, "Update state of selected rows");
        selectOptionByText(Locator.name("newState"), "Approved");
        setFormElement(Locator.name("comments"), "Approved");
        clickButton("Update Status");

        new BootstrapMenu.BootstrapMenuFinder(getDriver()).withButtonTextContaining("QC State").find().clickSubMenu(true, "All data");
        checker().verifyEquals("Incorrect value for QC state after update", Arrays.asList("Approved?", "Pending Review?"), table.getColumnDataAsText("QCState"));
    }

    @Test
    public void testFolderImportExport()
    {
        log("Creating the subfolder to import the exported studies for verification");
        String IMPORT_FOLDER_1 = "Imported folder 1";
        String IMPORT_FOLDER_2 = "Imported folder 2";
        goToProjectHome(VISIT_BASED_STUDY);
        _containerHelper.createSubfolder(VISIT_BASED_STUDY, IMPORT_FOLDER_1);
        _containerHelper.createSubfolder(VISIT_BASED_STUDY, IMPORT_FOLDER_2);

        log("Linking sample types two rows to study");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        int numOfRowsLinked = 2;
        linkToStudy(VISIT_BASED_STUDY, SAMPLE_TYPE1, numOfRowsLinked, null);

        log("Exporting both Dataset Data and Dataset Definitions");
        goToProjectHome(VISIT_BASED_STUDY);
        goToFolderManagement().goToExportTab();
        File exportArchive = new ExportFolderPage(getDriver())
                .exportToBrowserAsZipFile();

        ImportFolderPage.beginAt(this, VISIT_BASED_STUDY + "/" + IMPORT_FOLDER_1)
                .selectLocalZipArchive()
                .chooseFile(exportArchive)
                .clickImportFolder();
        waitForPipelineJobsToFinish(1);

        navigateToFolder(VISIT_BASED_STUDY, IMPORT_FOLDER_1);
        clickAndWait(Locator.linkContainingText("dataset"));
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE1));
        DataRegionTable table = new DataRegionTable("Dataset", getDriver());
        checker().verifyEquals("Incorrect number of rows imported", 2, table.getDataRowCount());

        log("Exporting only Dataset Definitions");
        goToProjectHome(VISIT_BASED_STUDY);
        goToFolderManagement().goToExportTab();
        exportArchive = new ExportFolderPage(getDriver())
                .includeSampleDatasetData(false)
                .exportToBrowserAsZipFile();

        ImportFolderPage.beginAt(this, VISIT_BASED_STUDY + "/" + IMPORT_FOLDER_2)
                .selectLocalZipArchive()
                .chooseFile(exportArchive)
                .clickImportFolder();
        waitForPipelineJobsToFinish(1);

        navigateToFolder(VISIT_BASED_STUDY, IMPORT_FOLDER_2);
        clickAndWait(Locator.linkContainingText("dataset"));
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE1));
        table = new DataRegionTable("Dataset", getDriver());
        checker().verifyEquals("No data should have been imported", 0, table.getDataRowCount());
    }

    /*
        Test coverage for : Issue 42937: Assay results grid loading performance can degrade with a large number of "copied to study" columns
     */
    @Test
    public void testLinkedColumnNotDisplayedCase()
    {
        log("Creating 2 more studies");
        _containerHelper.createProject(SAMPLE_TYPE_PROJECT + " Study 1", "Study");
        _studyHelper.startCreateStudy().createStudy();

        _containerHelper.createProject(SAMPLE_TYPE_PROJECT + " Study 2", "Study");
        _studyHelper.startCreateStudy().createStudy();

        log("Linking one row from sample type to all the studies");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(DATE_BASED_STUDY, SAMPLE_TYPE1, 1, null);

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(VISIT_BASED_STUDY, SAMPLE_TYPE1, 1, null);

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(SAMPLE_TYPE_PROJECT + " Study 1", SAMPLE_TYPE1, 1, null);

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(SAMPLE_TYPE_PROJECT + " Study 2", SAMPLE_TYPE1, 1, null);

        log("Verifying linked column does not exists because more then 3 studies are linked");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE1));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        checker().verifyFalse("Linked column for Visit based study should not be present",
                samplesTable.getColumnNames().contains("linked_to_Visit_Based_Study_Test_Project_Study"));
        checker().verifyFalse("Linked column for Date based study should not be present",
                samplesTable.getColumnNames().contains("linked_to_Date_Based_Study_Test_Project_Study"));
        checker().verifyFalse("Linked column for Study 1 should not be present",
                samplesTable.getColumnNames().contains("linked_to_Sample_Type_Test_Project_Study_1_Study"));
        checker().verifyFalse("Linked column for Study 2 should not be present",
                samplesTable.getColumnNames().contains("linked_to_Sample_Type_Test_Project_Study_2_Study"));

        log("Verifying if columns can be added from customize grid");
        CustomizeView customizeView = samplesTable.openCustomizeGrid();
        customizeView.addColumn("linked_to_Sample_Type_Test_Project_Study_1_Study");
        customizeView.addColumn("linked_to_Sample_Type_Test_Project_Study_2_Study");
        customizeView.addColumn("linked_to_Visit_Based_Study_Test_Project_Study");
        customizeView.addColumn("linked_to_Date_Based_Study_Test_Project_Study");
        customizeView.saveCustomView();
    }

    @Test
    public void testManualDatasetCategoryLink()
    {
        String categoryName1 = "CAT1";
        String categoryName2 = "CAT2";
        createDatasetCategory(VISIT_BASED_STUDY, categoryName1);
        goToProjectHome();

        log("Linking the sample type to preexisting dataset category");
        linkToStudy(VISIT_BASED_STUDY, SAMPLE_TYPE1, 1, categoryName1);

        log("Linking the sample type to new dataset category");
        goToProjectHome();
        linkToStudy(DATE_BASED_STUDY, SAMPLE_TYPE2, 1, categoryName2);

        checker().verifyEquals("Incorrect category for the dataset(Category exists case)", categoryName1,
                getCategory(VISIT_BASED_STUDY, SAMPLE_TYPE1));
        checker().verifyEquals("Incorrect category for the dataset(New category case)", categoryName2,
                getCategory(DATE_BASED_STUDY, SAMPLE_TYPE2));
    }

    @Test
    public void testOverWritingDatasetCategory()
    {
        String categoryName = "CAT1";
        createDatasetCategory(DATE_BASED_STUDY, categoryName);
        goToProjectHome();

        log("Linking the sample type to preexisting dataset category");
        linkToStudy(DATE_BASED_STUDY, SAMPLE_TYPE2, 1, categoryName);

        log("Linking more rows same dataset and trying to over write the dataset category");
        goToProjectHome();
        linkToStudy(DATE_BASED_STUDY, SAMPLE_TYPE2, 2, "CAT2");

        checker().verifyEquals("Category should not have overridden", categoryName, getCategory(DATE_BASED_STUDY, SAMPLE_TYPE2));
    }

    @Before
    public void preTest() throws Exception
    {
        //deleting the datasets from study folders.
        if(TestDataGenerator.doesDomainExists(DATE_BASED_STUDY, "study", "Sample type 1"))
            TestDataGenerator.deleteDomain(DATE_BASED_STUDY, "study", "Sample type 1");
        if(TestDataGenerator.doesDomainExists(DATE_BASED_STUDY, "study", "Sample type 2"))
            TestDataGenerator.deleteDomain(DATE_BASED_STUDY, "study", "Sample type 2");
        if(TestDataGenerator.doesDomainExists(VISIT_BASED_STUDY, "study", "Sample type 1"))
            TestDataGenerator.deleteDomain(VISIT_BASED_STUDY, "study", "Sample type 1");
        if(TestDataGenerator.doesDomainExists(VISIT_BASED_STUDY, "study", "Sample type 2"))
            TestDataGenerator.deleteDomain(VISIT_BASED_STUDY, "study", "Sample type 2");
        cnt = 0; //Resetting the counter between the tests.
    }

    private void linkToStudy(String targetStudy, String sampleName, int numOfRowsToBeLinked, @Nullable String categoryName)
    {
        clickAndWait(Locator.linkWithText(sampleName));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        for (int i = 0; i < numOfRowsToBeLinked; i++)
            samplesTable.checkCheckbox(i);

        samplesTable.clickHeaderButtonAndWait("Link to Study");

        log("Link to study: Choose target");
        selectOptionByText(Locator.id("targetStudy"), "/" + targetStudy + " (" + targetStudy + " Study)");
        if (categoryName != null)
            setFormElement(Locator.name("autoLinkCategory"), categoryName);
        clickButton("Next");

        new DataRegionTable("query", getDriver()).clickHeaderButtonAndWait("Link to Study");
    }

    private void createDatasetCategory(String projectName, String name)
    {
        goToProjectHome(projectName);
        goToManageViews();
        Locator.linkWithText("Manage Categories").findElement(getDriver()).click();
        _extHelper.waitForExtDialog("Manage Categories");
        Window categoryWindow = new Window.WindowFinder(getDriver()).withTitle("Manage Categories").waitFor();
        categoryWindow.clickButton("New Category", 0);
        WebElement newCategoryField = Locator.input("label").withAttributeContaining("id", "textfield").notHidden().waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        setFormElementJS(newCategoryField, name);
        fireEvent(newCategoryField, SeleniumEvent.blur);
        waitForElement(Ext4Helper.Locators.window("Manage Categories").append("//div").withText(name));
        clickButton("Done", 0);
        _extHelper.waitForExtDialogToDisappear("Manage Categories");
    }

    private String getCategory(String projectName, String datasetName)
    {
        goToProjectHome(projectName);
        goToSchemaBrowser();
        ExecuteQueryPage executeQueryPage = ExecuteQueryPage.beginAt(this, "study", "DataSets");
        DataRegionTable table = executeQueryPage.getDataRegion();
        table.setFilter("Label", "Equals", datasetName);
        return table.getDataAsText(0, "categoryid");
    }

    private void verifyLinkToHistory(String expectedComments)
    {
        clickButton("Link to Study History");
        DataRegionTable table = DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();
        checker().verifyEquals("Mismatch in the comment", expectedComments, table.getDataAsText(0, "Comment"));
    }

    private void verifyAuditLogEvents(String Comment, @Nullable Integer numOfRowsLinked, @Nullable List<String> linkedSamples)
    {
        goToAdminConsole().clickAuditLog();
        doAndWaitForPageToLoad(() -> selectOptionByText(Locator.name("view"), "Link to Study events"));

        DataRegionTable auditTable = DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();
        checker().verifyEquals("Incorrect audit log entry for Link to Study events", Comment,
                auditTable.getDataAsText(0, "Comment"));
        if (linkedSamples != null)
        {
            doAndWaitForPageToLoad(() -> selectOptionByText(Locator.name("view"), "Sample timeline events"));
            auditTable = DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();
            List<String> samples = new ArrayList<>();
            for (int i = 0; i < numOfRowsLinked; i++)
                samples.add(auditTable.getDataAsText(i, "SampleName"));
            checker().verifyEquals("Incorrect sample names in the audit log", linkedSamples, samples);
        }
    }

    private void recallDataset(String study, String sampleType)
    {
        goToProjectHome(study);
        if (isElementPresent(Locator.linkWithText(sampleType)))
        {
            clickAndWait(Locator.linkWithText(sampleType));
            DataRegionTable table = new DataRegionTable("Dataset", getDriver());
            if (table.getDataRowCount() > 0)
            {
                table.checkAllOnPage();
                table.clickHeaderButton("Recall");
                acceptAlert();
            }
        }
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return SAMPLE_TYPE_PROJECT;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(SAMPLE_TYPE_PROJECT, afterTest);
        _containerHelper.deleteProject(VISIT_BASED_STUDY, afterTest);
        _containerHelper.deleteProject(DATE_BASED_STUDY, afterTest);
        _containerHelper.deleteProject(SAMPLE_TYPE_PROJECT + " Study 1", afterTest);
        _containerHelper.deleteProject(SAMPLE_TYPE_PROJECT + " Study 2", afterTest);

    }
}
