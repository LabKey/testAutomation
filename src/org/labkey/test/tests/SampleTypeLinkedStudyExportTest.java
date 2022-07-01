package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.admin.ExportFolderPage;
import org.labkey.test.pages.admin.ImportFolderPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.StudyHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class SampleTypeLinkedStudyExportTest extends BaseWebDriverTest
{
    private final static String SAMPLE_TYPE_PROJECT = "Sample Type Project";
    private final static String SAMPLE_TYPE = "Samples";
    private final static String LINKED_STUDY = "Target Study Project";
    private final static String IMPORT_PROJECT = "Import Test Project";
    private static TestDataGenerator SAMPLE_GENERATOR;

    protected DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    protected String now = LocalDateTime.now().format(_dateTimeFormatter);

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        SampleTypeLinkedStudyExportTest init = (SampleTypeLinkedStudyExportTest) getCurrentTest();
        init.doSetup();
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return SAMPLE_TYPE_PROJECT;
    }

    private void doSetup() throws IOException, CommandException
    {
        _containerHelper.createProject(getProjectName(), null);

        _containerHelper.createProject(LINKED_STUDY, "Study");
        _studyHelper.startCreateStudy()
                .setTimepointType(StudyHelper.TimepointType.VISIT)
                .createStudy();
        String containerId = ((APIContainerHelper) _containerHelper).getContainerId(LINKED_STUDY);

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        log("Creating sample types");
        SAMPLE_GENERATOR = new SampleTypeDefinition(SAMPLE_TYPE)
                .setAutoLinkDataToStudy(containerId)
                .setFields(List.of(
                        new FieldDefinition("VisitID", FieldDefinition.ColumnType.VisitId),
                        new FieldDefinition("Date", FieldDefinition.ColumnType.VisitDate),
                        new FieldDefinition("ParticipantID", FieldDefinition.ColumnType.Subject)))
                .create(createDefaultConnection(), SAMPLE_TYPE_PROJECT);

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        new PortalHelper(getDriver()).addBodyWebPart("Sample Types");

        _containerHelper.createProject(IMPORT_PROJECT, "Study");
    }

    /*
        Test coverage for : Issue 45238: NPE when importing sample type with auto-link study via pipeline job
     */
    @Test
    public void testImportSampleTypeFolder()
    {
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        samplesTable.clickInsertNewRow().update(Map.of(
                "Name", "Blood",
                "VisitID", "1",
                "Date", now,
                "ParticipantID", "P1"));

        log("Export the Sample type folder");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        goToFolderManagement()
                .goToExportTab();
        File exportArchive = new ExportFolderPage(getDriver())
                .includeSampleTypeAndDataClasses(true)
                .exportToBrowserAsZipFile();

        log("Navigate into the destination folder and import there");
        goToProjectHome(IMPORT_PROJECT);
        ImportFolderPage.beginAt(this, IMPORT_PROJECT)
                .selectLocalZipArchive()
                .chooseFile(exportArchive)
                .clickImportFolder();
        waitForPipelineJobsToFinish(1);

        goToProjectHome(IMPORT_PROJECT);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE));
        DataRegionTable table = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        checker().verifyEquals("Incorrect Columns in imported sample type", Arrays.asList("Name", "Flag", "Visit ID", "Date", "Participant ID",
                "Linked to " + LINKED_STUDY + " Study"), table.getColumnLabels());

        clickAndWait(Locator.linkWithText("linked"));
        checker().verifyEquals("Linked to wrong study", LINKED_STUDY, getCurrentProject());
    }

    /*
        TODO : Test coverage for Issue 45711: Imported study has additional columns in dataset compare to exported study
     */
    @Ignore
    @Test
    public void testImportLinkedStudyFolder()
    {

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
        _containerHelper.deleteProject(LINKED_STUDY, afterTest);
        _containerHelper.deleteProject(IMPORT_PROJECT, afterTest);

    }
}
