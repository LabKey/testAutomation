package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
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
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.StudyHelper;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class SampleTypeLinkedStudyExportTest extends BaseWebDriverTest
{
    private final static String SAMPLE_TYPE_PROJECT = "Sample Types Project";
    private final static String SAMPLE_TYPE = "Samples";
    private final static String STUDY_EXPORT = "Export Study Test Project";
    private final static String STUDY_IMPORT = "Import Study Test Project";

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

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);

        _containerHelper.createProject(STUDY_EXPORT, "Study");
        _studyHelper.startCreateStudy()
                .setTimepointType(StudyHelper.TimepointType.VISIT)
                .createStudy();

        _containerHelper.createProject(STUDY_IMPORT, "Study");
        _studyHelper.startCreateStudy()
                .setTimepointType(StudyHelper.TimepointType.VISIT)
                .createStudy();

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        new PortalHelper(getDriver()).addBodyWebPart("Sample Types");

        goToProjectHome(SAMPLE_TYPE_PROJECT);
        log("Creating sample types");
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition(SAMPLE_TYPE)
                .setAutoLinkDataToStudy("/" + STUDY_EXPORT)
                .setFields(List.of(
                        new FieldDefinition("VisitID", FieldDefinition.ColumnType.VisitId),
                        new FieldDefinition("Date", FieldDefinition.ColumnType.VisitDate),
                        new FieldDefinition("ParticipantID", FieldDefinition.ColumnType.Subject))));

        goToProjectHome(STUDY_IMPORT);
        new PortalHelper(getDriver()).addBodyWebPart("Datasets");

        goToProjectHome(STUDY_EXPORT);
        new PortalHelper(getDriver()).addBodyWebPart("Datasets");
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
        samplesTable.clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), "Blood");
        setFormElement(Locator.name("quf_VisitID"), "1");
        setFormElement(Locator.name("quf_Date"), now);
        setFormElement(Locator.name("quf_ParticipantID"), "P1");
        clickButton("Submit");

        log("Export the Sample type folder");
        goToProjectHome(SAMPLE_TYPE_PROJECT);
        goToFolderManagement()
                .goToExportTab();
        File exportArchive = new ExportFolderPage(getDriver())
                .includeSampleTypeAndDataClasses(true)
                .exportToBrowserAsZipFile();

        log("Navigate into the destination folder and import there");
        goToProjectHome(STUDY_IMPORT);
        ImportFolderPage.beginAt(this, STUDY_IMPORT)
                .selectLocalZipArchive()
                .chooseFile(exportArchive)
                .clickImportFolder();
        waitForPipelineJobsToFinish(1);

        goToProjectHome(STUDY_IMPORT);
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE));
        DataRegionTable table = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        checker().verifyEquals("Incorrect Columns in imported sample type", Arrays.asList("Name", "Flag", "Visit ID", "Date", "Participant ID", "Linked to Export Study Test Project Study"), table.getColumnLabels());

        clickAndWait(Locator.linkWithText("linked"));
        checker().verifyEquals("Linked to wrong study", STUDY_EXPORT, getCurrentProject());
    }

    /*
        TODO : Test coverage for Issue 45711: Imported study has additional columns in dataset compare to exported study
     */
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
        _containerHelper.deleteProject(STUDY_EXPORT, afterTest);
        _containerHelper.deleteProject(STUDY_IMPORT, afterTest);

    }
}
