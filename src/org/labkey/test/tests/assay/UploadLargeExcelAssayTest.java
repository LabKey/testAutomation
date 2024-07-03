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
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.assay.AssayImportPage;
import org.labkey.test.pages.assay.AssayRunsPage;
import org.labkey.test.pages.assay.AssayUploadJobsPage;
import org.labkey.test.pages.query.SourceQueryPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.assay.GeneralAssayDesign;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.AbstractDataRegionExportOrSignHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.exp.SampleTypeAPIHelper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({Assays.class, Daily.class})
public class UploadLargeExcelAssayTest extends BaseWebDriverTest
{
    public static String LARGE_ASSAY = "chaos_assay";
    public static String LARGE_ASSAY_2 = "large_assay_2";
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

        var assayDesign1 = new GeneralAssayDesign(LARGE_ASSAY_2)
                .setDataFields(ASSAY_FIELDS, false);
        var protocol1 = assayDesign1.createAssay(getProjectName(), createDefaultConnection());

        // update the assays to support background import
        var updateAssayPage = ReactAssayDesignerPage.beginAt(this, getProjectName(), protocol.getProtocolId(),
                "General", getDriver().getCurrentUrl());
        updateAssayPage.setBackgroundImport(true)
                .clickSave();

        var updateAssayPage1 = ReactAssayDesignerPage.beginAt(this, getProjectName(), protocol1.getProtocolId(),
                "General", getDriver().getCurrentUrl());
        updateAssayPage1.setBackgroundImport(true)
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
        var dgen = new TestDataGenerator("samples", "chaos_sample", getProjectName())
            .withColumns(ASSAY_FIELDS);
        log("writing large tsv file");
        var largeTsvFile = dgen.writeGeneratedDataToFile(200000, "largeTsvFile.tsv");
        log("finished writing large tsv file");


        // import tsv to assay1
        goToProjectHome();
        log("importing large excel file to assay");
        clickAndWait(Locator.linkWithText(LARGE_ASSAY));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.input("name"), "200k");
        checkRadioButton(Locator.inputById("Fileupload"));
        setFormElement(Locator.input("__primaryFile__"), largeTsvFile);
        clickButton("Save and Finish");

        // wait for import complete
        var assayJobsPage1 = new AssayUploadJobsPage(getDriver());
        var pipelineDetailsPage1 = assayJobsPage1.clickJobStatus("200k");
        pipelineDetailsPage1.waitForComplete(10 * WebDriverWrapper.WAIT_FOR_PAGE);

        // export to excel
        log("exporting samples fields to excel");
        var firstQPage = SourceQueryPage.beginAt(this, getProjectName(), "assay.General.chaos_assay", "Data");
        var firstDataregion  = firstQPage.viewData(Duration.ofSeconds(60));
        waitForText("1 - 100 of 200,000");
        var largeXlsxFile = firstDataregion.expandExportPanel()
                .exportExcel(AbstractDataRegionExportOrSignHelper.ExcelFileType.XLSX);

        log("importing large excel file to assay");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(LARGE_ASSAY_2));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.input("name"), "200k take 2");
        checkRadioButton(Locator.inputById("Fileupload"));
        setFormElement(Locator.input("__primaryFile__"), largeXlsxFile);
        clickButton("Save and Finish");

        var assayJobsPage2 = new AssayUploadJobsPage(getDriver());
        var pipelineDetailsPage2 = assayJobsPage2.clickJobStatus("200k take 2");
        pipelineDetailsPage2.waitForComplete(10 * WebDriverWrapper.WAIT_FOR_PAGE);

        var qPage = SourceQueryPage.beginAt(this, getProjectName(), "assay.General.large_assay_2", "Data");
        var dataregion  = qPage.viewData(Duration.ofSeconds(60));
        waitForText("1 - 100 of 200,000");
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
