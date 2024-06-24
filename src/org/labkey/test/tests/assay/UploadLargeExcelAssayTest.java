package org.labkey.test.tests.assay;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.categories.Assays;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.assay.AssayImportPage;
import org.labkey.test.pages.assay.AssayRunsPage;
import org.labkey.test.pages.assay.AssayUploadJobsPage;
import org.labkey.test.pages.query.SourceQueryPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.assay.GeneralAssayDesign;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.AbstractDataRegionExportOrSignHelper;
import org.labkey.test.util.TestDataGenerator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({Assays.class})
public class UploadLargeExcelAssayTest extends BaseWebDriverTest
{
    public static String LARGE_ASSAY = "chaos_assay";
    public static List<PropertyDescriptor> ASSAY_FIELDS = new ArrayList<PropertyDescriptor>();

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        UploadLargeExcelAssayTest init = (UploadLargeExcelAssayTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup() throws Exception
    {
        _containerHelper.createProject(getProjectName(), "Assay");

        for (int i=0; i<25; i++)
        {
            ASSAY_FIELDS.add(new FieldDefinition(String.format("field-%d", i), FieldDefinition.ColumnType.String));
        }
        var assayDesign = new GeneralAssayDesign(LARGE_ASSAY)
                .setDataFields(ASSAY_FIELDS, false);
        var protocol = assayDesign.createAssay(getProjectName(), createDefaultConnection());

        // update the assay to support background import
        var updateAssayPage = ReactAssayDesignerPage.beginAt(this, getProjectName(), protocol.getProtocolId(),
                "General", getDriver().getCurrentUrl());
        updateAssayPage.setBackgroundImport(true)
                .clickSave();
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testUpload200kRows() throws Exception
    {
        // generate a large .tsv file
        String fileName = "200kXlsxFile.xlsx";
        var dgen = new TestDataGenerator("samples", "chaos_sample", getProjectName());
        dgen.withColumns(ASSAY_FIELDS)
                .generateRows(200000);
        var largeTsvFile = dgen.writeData("largeTsvImportFile");
        // convert to .xlsx
        log("converting large tsv file to .xlsx");
        var largeXlsXFile = TestFileUtils.convertTabularToXlsx(largeTsvFile, "\t", "import", fileName);
        log("finished converting file to xlsx");

        clickAndWait(Locator.linkWithText(LARGE_ASSAY));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.input("name"), "200k");
        checkRadioButton(Locator.inputById("Fileupload"));
        setFormElement(Locator.input("__primaryFile__"), largeXlsXFile);
        clickButton("Save and Finish");

        var assayJobsPage = new AssayUploadJobsPage(getDriver());
        var pipelineDetailsPage = assayJobsPage.clickJobStatus("200k");
        pipelineDetailsPage.waitForComplete(10 * WebDriverWrapper.WAIT_FOR_PAGE);

        var qPage = SourceQueryPage.beginAt(this, getProjectName(), "assay.General.chaos_assay", "Data");
        var dataregion  = qPage.viewData(Duration.ofSeconds(60));
        var exportFile = dataregion.expandExportPanel()
                .exportExcel(AbstractDataRegionExportOrSignHelper.ExcelFileType.XLSX);
    }

    @Override
    protected String getProjectName()
    {
        return "UploadLargeExcelAssayTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
