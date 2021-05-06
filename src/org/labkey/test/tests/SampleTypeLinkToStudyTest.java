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
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.labkey.test.util.exp.SampleTypeAPIHelper.SAMPLE_TYPE_DOMAIN_KIND;

@Category({DailyC.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class SampleTypeLinkToStudyTest extends BaseWebDriverTest
{
    final static String SAMPLE_TYPE_PROJECT = "Sample Type Test Project";
    final static String LINKED_STUDY_PROJECT = "Linked Study Test Project";

    final static String SAMPLE_TYPE1 = "Sample type 1";
    final static String SAMPLE_TYPE2 = "Sample type 2";

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        SampleTypeLinkToStudyTest init = (SampleTypeLinkToStudyTest) getCurrentTest();
        init.doSetup();
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
        _containerHelper.deleteProject(LINKED_STUDY_PROJECT, afterTest);
    }

    private void doSetup() throws IOException, CommandException
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createProject(LINKED_STUDY_PROJECT, "Study");
        clickButton("Create Study");
        clickButton("Create Study");

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        new PortalHelper(getDriver()).addBodyWebPart("Sample Types");

        goToProjectHome(LINKED_STUDY_PROJECT);
        new PortalHelper(getDriver()).addBodyWebPart("Datasets");

        createSampleTypes();
    }

    private void createSampleTypes() throws IOException, CommandException
    {
        goToProjectHome(SAMPLE_TYPE_PROJECT);

        log("Creating sample types");
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", SAMPLE_TYPE1);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String)));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        dgen.addCustomRow(Map.of("name", "blood"));
        dgen.addCustomRow(Map.of("name", "urine"));
        dgen.addCustomRow(Map.of("name", "stool"));
        dgen.addCustomRow(Map.of("name", "sweat"));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

        lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", SAMPLE_TYPE2);
        dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("int", FieldDefinition.ColumnType.Integer)));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        dgen.addCustomRow(Map.of("name", "first"));
        dgen.addCustomRow(Map.of("name", "second"));
        dgen.addCustomRow(Map.of("name", "third"));
        dgen.addCustomRow(Map.of("name", "fourth"));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

    }

    @Test
    public void testLinkToStudy()
    {
        goToProjectHome(SAMPLE_TYPE_PROJECT);

        log("Linking sample types one row to study");
        linkToStudy(SAMPLE_TYPE1, 2, Arrays.asList("P1", "P2"), Arrays.asList("1", "2"));

        log("Verifying the linked sample type in study");
        goToProjectHome(LINKED_STUDY_PROJECT);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE1));
        DataRegionTable table = new DataRegionTable("Dataset", getDriver());

        checker().verifyEquals("Incorrect number of rows linked", 2, table.getDataRowCount());
        checker().verifyEquals("Incorrect Participant ID's", Arrays.asList("P1", "P2"), table.getColumnDataAsText("ParticipantId"));

        log("Verifying log entries");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE1));
        String expectedComment = "2 row(s) were linked to a study from the sample type: " + SAMPLE_TYPE1;
        verifyLinkToHistory(expectedComment);
        verifyAuditLogEvents(expectedComment);
    }

    @Test
    public void testDatasetRecall()
    {
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        linkToStudy(SAMPLE_TYPE2, 1, Arrays.asList("P3"), Arrays.asList("3"));

        goToProjectHome(LINKED_STUDY_PROJECT);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE2));
        DataRegionTable table = new DataRegionTable("Dataset", getDriver());
        table.checkCheckbox(0);
        table.clickHeaderButton("Recall");
        acceptAlert();
        checker().verifyEquals("Dataset row not recalled", 0, table.getDataRowCount());

        log("Verifying log entries");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE2));
        String expectedComment = "1 row(s) were recalled from a study to the sample type: " + SAMPLE_TYPE2;
        verifyLinkToHistory(expectedComment);
        verifyAuditLogEvents(expectedComment);
    }

    private void linkToStudy(String sampleName, int numOfRowsToBeLinked, List<String> participantID, List<String> visitId)
    {
        clickAndWait(Locator.linkWithText(sampleName));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        for (int i = 0; i < numOfRowsToBeLinked; i++)
        {
            samplesTable.checkCheckbox(i);
        }

        samplesTable.clickHeaderButtonAndWait("Link to Study");

        log("Link to study: Choose target");
        selectOptionByText(Locator.id("targetStudy"), "/" + LINKED_STUDY_PROJECT + " (" + LINKED_STUDY_PROJECT + " Study)");
        clickButton("Next");

        log("Lick to Study: Verify Results");
        for (int i = 0; i < numOfRowsToBeLinked; i++)
        {
            setFormElement(Locator.name("participantId").index(i), participantID.get(i));
            setFormElement(Locator.name("visitId").index(i), visitId.get(i));
        }
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

}
