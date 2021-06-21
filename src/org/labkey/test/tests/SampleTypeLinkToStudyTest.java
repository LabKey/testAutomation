package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyC;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({DailyC.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class SampleTypeLinkToStudyTest extends BaseWebDriverTest
{
    final static String SAMPLE_TYPE_PROJECT = "Sample Type Test Project";
    final static String VISIT_BASED_STUDY = "Visit Based Study Test Project";
    final static String DATE_BASED_STUDY = "Date Based Study Test Project";
    final static String ASSAY_NAME = "Test assay";
    final static String SAMPLE_TYPE1 = "Sample type 1";
    final static String SAMPLE_TYPE2 = "Sample type 2";

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
        clickButton("Create Study");
        clickButton("Create Study");

        _containerHelper.createProject(DATE_BASED_STUDY, "Study");
        clickButton("Create Study");
        checkRadioButton(Locator.radioButtonById("dateTimepointType"));
        clickButton("Create Study");

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

        log("Linking sample types one row to study");
        linkToStudy(VISIT_BASED_STUDY, SAMPLE_TYPE1, 2);

        log("Verifying the linked sample type in study");
        goToProjectHome(VISIT_BASED_STUDY);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE1));
        DataRegionTable table = new DataRegionTable("Dataset", getDriver());

        checker().verifyEquals("Incorrect number of rows linked", 2, table.getDataRowCount());
        checker().verifyEquals("Incorrect Participant ID's", Arrays.asList("P3", "P4"), table.getColumnDataAsText("ParticipantId"));

        log("Verifying log entries");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE1));
        String expectedComment = "2 row(s) were linked to a study from the sample type: " + SAMPLE_TYPE1;
        verifyLinkToHistory(expectedComment);
        verifyAuditLogEvents(expectedComment);

        recallDataset(VISIT_BASED_STUDY, SAMPLE_TYPE1, 2);
    }

    @Test
    public void testDatasetRecall()
    {
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(DATE_BASED_STUDY, SAMPLE_TYPE2, 1);

        recallDataset(DATE_BASED_STUDY, SAMPLE_TYPE2, 1);

        log("Verifying log entries");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE2));
        String expectedComment = "1 row(s) were recalled from a study to the sample type: " + SAMPLE_TYPE2;
        verifyLinkToHistory(expectedComment);
        verifyAuditLogEvents(expectedComment);
    }

    @Test
    public void testSampleTypeLinkedToMultipleStudy()
    {
        log("Linking the Sample type to " + VISIT_BASED_STUDY);
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(VISIT_BASED_STUDY, SAMPLE_TYPE1, 1);

        log("Linking the Sample type to " + DATE_BASED_STUDY);
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(DATE_BASED_STUDY, SAMPLE_TYPE1, 1);

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

        recallDataset(DATE_BASED_STUDY, SAMPLE_TYPE1, 1);
        recallDataset(VISIT_BASED_STUDY, SAMPLE_TYPE1, 1);
    }

    @Test
    public void testAutoLinkToStudy()
    {
        String sampleName = "SampleTypeWithAutoLinkToStudy";

        log("Creating sample type with auto link enabled");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition(sampleName)
                .setAutoLinkDataToStudy("/" + VISIT_BASED_STUDY)
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
        linkToStudy(VISIT_BASED_STUDY, sampleName, 1);

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(DATE_BASED_STUDY, sampleName, 2);

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

    private void linkToStudy(String targetStudy, String sampleName, int numOfRowsToBeLinked)
    {
        clickAndWait(Locator.linkWithText(sampleName));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        for (int i = 0; i < numOfRowsToBeLinked; i++)
            samplesTable.checkCheckbox(i);

        samplesTable.clickHeaderButtonAndWait("Link to Study");

        log("Link to study: Choose target");
        selectOptionByText(Locator.id("targetStudy"), "/" + targetStudy + " (" + targetStudy + " Study)");
        clickButton("Next");

        new DataRegionTable("query", getDriver()).clickHeaderButtonAndWait("Link to Study");
    }

    private void verifyLinkToHistory(String expectedComments)
    {
        clickButton("Link to Study History");
        DataRegionTable table = DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();
        checker().verifyEquals("Mismatch in the comment", expectedComments, table.getDataAsText(0, "Comment"));
    }

    private void verifyAuditLogEvents(String Comment)
    {
        goToAdminConsole().clickAuditLog();
        doAndWaitForPageToLoad(() -> selectOptionByText(Locator.name("view"), "Link to Study events"));

        DataRegionTable auditTable = DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();
        checker().verifyEquals("Incorrect audit log entry for Link to Study events", Comment,
                auditTable.getDataAsText(0, "Comment"));
    }

    private void recallDataset(String study, String sampleType, int numOfRows)
    {
        goToProjectHome(study);
        clickAndWait(Locator.linkWithText(sampleType));
        DataRegionTable table = new DataRegionTable("Dataset", getDriver());
        for (int i = 0; i < numOfRows; i++)
            table.checkCheckbox(i);
        table.clickHeaderButton("Recall");
        acceptAlert();
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
    }
}
