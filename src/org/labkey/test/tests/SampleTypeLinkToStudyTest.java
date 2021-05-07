package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyC;
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

@Category({DailyC.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class SampleTypeLinkToStudyTest extends BaseWebDriverTest
{
    final static String SAMPLE_TYPE_PROJECT = "Sample Type Test Project";
    final static String VISIT_BASED_STUDY = "Visit Based Study Test Project";
    final static String DATE_BASED_STUDY = "Date Based Study Test Project";
    final static String SAMPLE_TYPE1 = "Sample type 1";
    final static String SAMPLE_TYPE2 = "Sample type 2";

    protected DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
        String now = LocalDateTime.now().format(_dateTimeFormatter);
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

        recallDataset(DATE_BASED_STUDY,SAMPLE_TYPE2,1);

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

        recallDataset(DATE_BASED_STUDY, SAMPLE_TYPE1,1);
        recallDataset(VISIT_BASED_STUDY,SAMPLE_TYPE1,1);
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
        DataRegionTable table = new DataRegionTable("query", getDriver());
        checker().verifyEquals("Mismatch in the comment", expectedComments, table.getDataAsText(0, "comment"));
    }

    private void verifyAuditLogEvents(String Comment)
    {
        goToAdminConsole().clickAuditLog();
        doAndWaitForPageToLoad(() -> selectOptionByText(Locator.name("view"), "Link to Study events"));

        DataRegionTable auditTable = new DataRegionTable("query", getDriver());
        checker().verifyEquals("Incorrect audit log entry for Link to Study events", Comment,
                auditTable.getDataAsText(0, "comment"));
    }

    private void recallDataset(String study, String sampleType, int numOfRows)
    {
        goToProjectHome(study);
        clickAndWait(Locator.linkWithText(sampleType));
        DataRegionTable table = new DataRegionTable("Dataset", getDriver());
        for(int i=0; i < numOfRows ; i++)
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
