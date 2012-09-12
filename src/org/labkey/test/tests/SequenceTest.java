package org.labkey.test.tests;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 5/28/12
 * Time: 7:12 PM
 */
public class SequenceTest extends LabModulesTest
{
    private String sequencepipelineLoc =  getLabKeyRoot() + "/sampledata/sequence";
    private String illuminaPipelineLoc =  getLabKeyRoot() + "/sampledata/genotyping";
    private final String TEMPLATE_NAME = "SequenceTest Saved Template";
    private Integer _readsetCt = 14;
    private final String ILLUMINA_CSV = "SequenceImport.csv";

    @Override
    protected String getProjectName()
    {
        return "SequenceVerifyProject";// + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setUpTest();

        importReadsetMetadata();
        createIlluminaSampleSheet();
        importIlluminaTest();
        readsetFeaturesTest();
        analysisPanelTest();

        //TODO: verify pipelines:
        //setPipelineRoot(sequencePipelineLoc);
        //importBasicReadsetTest();
        //importBarcodedReadsetTest();
        //importMergedReadsetTest();

        //TODO: once we get analyses imported
        // analysis details page
        // SNP viewers / other reports
        // search page
        // reference sequence management
    }

    protected void setUpTest() throws Exception
    {
        _containerHelper.createProject(getProjectName(), "Sequence Analysis");
        deleteTemplateRow();
        goToProjectHome();
    }


    /**
     * This method is designed to import an initial set of readset records, which will be used for
     * illumina import
     */
    protected void importReadsetMetadata()
    {
        //create readset records for illumina run
        goToProjectHome();
        waitForText("Create Readsets");
        _helper.clickSpanContaining("Create Readsets");
        waitForPageToLoad();
        waitForText("Run Id");
        Ext4Helper.clickTabContainingText(this, "Import Spreadsheet");
        waitForText("Copy/Paste Data");

        setText("text", getIlluminaNames());

        click(Locator.ext4Button("Upload"));
        Ext4Helper.waitForMaskToDisappear(this);
        waitForElement(Ext4Helper.ext4Window("Success"));
        assertTextPresent("Success!");
        clickButton("OK");
        waitForPageToLoad();

        log("verifying readset count correct");
        waitForText("Total Readsets Imported");
        Assert.assertTrue("Wrong number of readsets present", isElementPresent(_helper.getNavPanelItem("Total Readsets Imported:", _readsetCt.toString())));
    }

    /**
     * This method is designed to exercise the illumina template UI and create a CSV import template.  This template
     * is later used by importIlluminaTest()
     */
    private void createIlluminaSampleSheet()
    {
        goToProjectHome();
        _helper.clickNavPanelItem("Total Readsets Imported:", _readsetCt.toString());
        waitForPageToLoad();

        //verify CSV file creation
        DataRegionTable dr = new DataRegionTable("query", this);
        dr.checkAllOnPage();
        clickMenuButton("More Actions", "Create Illumina Sample Sheet");
        waitForPageToLoad();
        waitForText("You have chosen to export " + _readsetCt + " samples");
        waitForText("Flow Cell Id");

        Ext4FieldRef.getForLabel(this, "Flow Cell Id").setValue("FlowCell");

        String[][] fieldPairs = {
            {"Investigator Name", "Investigator"},
            {"Experiment Name", "Experiment"},
            {"Project Name", "Project"},
            {"Description", "Description"}
        };

        for (String[] a : fieldPairs)
        {
            Ext4FieldRef.getForLabel(this, a[0]).setValue(a[1]);
        }

        //save combo record count for later use
        Ext4FieldRef templateCombo = Ext4FieldRef.getForLabel(this, "Template");
        int originalCount = Integer.parseInt(templateCombo.eval("this.store.getCount()"));

        Ext4Helper.clickTabContainingText(this, "Preview Header");
        waitForText("Edit Sheet");
        for (String[] a : fieldPairs)
        {
            Assert.assertEquals(a[1], Ext4FieldRef.getForLabel(this, a[0]).getValue());
        }

        clickButton("Edit Sheet", 0);
        waitForText("Done Editing");
        for (String[] a : fieldPairs)
        {
            assertTextPresent(a[0] + "," + a[1]);
        }

        //add new values
        String prop_name = "NewProperty";
        String prop_value = "NewValue";
        Ext4FieldRef textarea = Ext4Helper.queryOne(this, "textarea[itemId='sourceField']", Ext4FieldRef.class);
        String newValue = prop_name + "," + prop_value;
        textarea.eval("this.setValue(this.getValue() + \"\\\\n" + newValue + "\")");
        clickButton("Done Editing", 0);

        //verify template has changed
        Ext4Helper.clickTabContainingText(this, "General Info");
        Assert.assertEquals("Custom", Ext4FieldRef.getForLabel(this, "Template").getValue());

        //verify values persisted
        Ext4Helper.clickTabContainingText(this, "Preview Header");
        waitForText("Edit Sheet");
        Assert.assertEquals(prop_value, Ext4FieldRef.getForLabel(this, prop_name).getValue());

        //save template
        clickButton("Save As Template", 0);
        waitForElement(Ext4Helper.ext4Window("Choose Name"));
        Ext4FieldRef textfield = Ext4Helper.queryOne(this, "textfield", Ext4FieldRef.class);
        textfield.setValue(TEMPLATE_NAME);
        clickButton("OK", 0);
        Ext4Helper.clickTabContainingText(this, "General Info");
        Assert.assertEquals(TEMPLATE_NAME, Ext4FieldRef.getForLabel(this, "Template").getValue());

        //if we navigate too quickly, before the insertRows has returned, the test can get a JS error
        //therefore we sleep
        sleep(500);

        //verify samples present
        Ext4Helper.clickTabContainingText(this, "Preview Samples");
        waitForText("Sample_ID");

        int expectRows = (11 * (14 +  1));  //11 cols, 14 rows, plus header
        Assert.assertEquals(expectRows, selenium.getXpathCount("//td[contains(@class, 'x4-table-layout-cell')]"));

        //make sure values persisted
        refresh();
        String url = getCurrentRelativeURL();
        url += "&exportAsWebPage=1";
        beginAt(url);

        waitForText("Template");
        for (String[] a : fieldPairs)
        {
            Ext4FieldRef.getForLabel(this, a[0]).setValue(a[1]);
        }

        templateCombo.setValue(TEMPLATE_NAME);

        int count = Integer.parseInt(templateCombo.eval("this.store.getCount()"));
        Assert.assertEquals("Combo store does not have correct record number", (originalCount + 1), count);
        sleep(50);
        Assert.assertEquals("Field value not set correctly", TEMPLATE_NAME, Ext4FieldRef.getForLabel(this, "Template").getValue());
        Ext4Helper.clickTabContainingText(this, "Preview Header");
        waitForText("Edit Sheet");
        Assert.assertEquals(prop_value, Ext4FieldRef.getForLabel(this, prop_name).getValue());

        //NOTE: hitting download will display the text in the browser; however, this replaces newlines w/ spaces.  therefore we use selenium
        //to directly get the output
        Ext4CmpRef panel = Ext4Helper.queryOne(this, "#illuminaPanel", Ext4CmpRef.class);
        String outputTable = panel.eval("this.getTableOutput().join(\"<>\")");
        outputTable = outputTable.replaceAll("<>", "\n");

        //then we download anyway
        clickButton("Download");

        //the browser converts line breaks to spaces.  this is a hack to get them back
        String text = getWrapper().getBodyText().replaceAll(", ", ",\n").replaceAll("] ", "]\n");
        for (String[] a : fieldPairs)
        {
            String line = a[0] + "," + a[1];
            assertTextPresent(line);

            text.replaceAll(line, line + "\n");
        }

        assertTextPresent(prop_name + "," + prop_value);

        File importTemplate = new File(illuminaPipelineLoc, ILLUMINA_CSV);
        if (importTemplate.exists())
            importTemplate.delete();


        //NOTE: use the text generated directly using JS
        saveFile(importTemplate.getParentFile(), importTemplate.getName(), outputTable);
        beginAt("/project/" + getProjectName() + "/begin.view");
    }

    private String getIlluminaNames()
    {
        _readsetCt = 14;
        String[] barcodes5 = {"N701", "N702", "N703", "N704", "N705", "N706", "N701", "N702", "N701", "N702", "N703", "N704", "N705", "N706"};
        String[] barcodes3 = {"N502", "N502", "N502", "N502", "N502", "N502", "N503", "N503", "N501", "N501", "N501", "N501", "N501", "N501"};

        StringBuilder sb = new StringBuilder("Name\tPlatform\tBarcode5\tBarcode3\n");
        int i = 0;
        while (i < _readsetCt)
        {
            sb.append("Illumina" + (i+1) + "\tILLUMINA\t" + barcodes5[i] + "\t" + barcodes3[i] + "\n");
            i++;
        }
        return sb.toString();
    }

    private void deleteTemplateRow() throws Exception
    {
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        DeleteRowsCommand cmd = new DeleteRowsCommand("sequenceanalysis", "illumina_templates");
        cmd.addRow(Collections.singletonMap("Name", (Object) TEMPLATE_NAME));
        SaveRowsResponse resp = cmd.execute(cn, getProjectName());
        log("Template rows deleted: " + resp.getRowsAffected());
    }

    protected void importBasicReadsetTest()
    {
        selectPipelineJob("Import sequence data", "sample454_SIV.sff");

        readsetPanelTest();

        //TODO: set import options

        //TODO: verify import
    }

    protected void importBarcodedReadsetTest()
    {
        selectPipelineJob("Import sequence data", "dualBarcodes_SIV.fastq");

        //TODO: set import options

        //TODO: verify import
    }

    protected void importMergedReadsetTest()
    {
        selectPipelineJob("Import sequence data", "sample454_SIV.sff", "dualBarcodes_SIV.fastq");

        //TODO: set import options

        //TODO: verify import
    }

    /**
     * This test will kick off a pipeline import using the illumina pipeline.  Verification of the result
     * is performed by readsetFeaturesTest()
     */
    protected void importIlluminaTest()
    {
        setPipelineRoot(illuminaPipelineLoc);
        selectPipelineJob("Import Illumina data", ILLUMINA_CSV);

        setText("protocolName", "TestIlluminaRun" +  _helper.getRandomInt());
        setText("runDate", "08/25/2011");
        setText("fastqPrefix", "Il");

        click(Locator.ext4Button("Import Data"));
        waitForAlert("Analysis Started!", WAIT_FOR_JAVASCRIPT);
        waitForPageToLoad();
        clickLinkWithText("All");
        waitForPipelineJobsToComplete(1, "Import Illumina", false);
        assertTextNotPresent("ERROR");
    }

    /**
     * This method has several puposes.  It will verify that the records from illuminaImportTest() were
     * created properly.  It also exercises various features associated with the readset grid, including
     * the FASTQC report and downloading of results
     * @throws Exception
     */
    private void readsetFeaturesTest() throws Exception
    {
        //verify import and instrument run creation
        goToProjectHome();
        _helper.clickNavPanelItem("Total Readsets Imported:", _readsetCt.toString());
        waitForPageToLoad();

        DataRegionTable dr = new DataRegionTable("query", this);
        for (int i = 0; i < dr.getDataRowCount(); i++)
        {
            String rowId = dr.getDataAsText(i, "Readset Id");
            String file1 = dr.getDataAsText(i, "Input File");
            Assert.assertEquals("Incorrect or no filename associated with readset", "Illumina-R1-" + rowId + ".fastq.gz", file1);

            String file2 = dr.getDataAsText(i, "Input File2");
            Assert.assertEquals("Incorrect or no filename associated with readset", "Illumina-R2-" + rowId + ".fastq.gz", file2);

            String instrumentRun = dr.getDataAsText(i, "Instrument Run");
            Assert.assertTrue("Incorrect or no instrument run associated with readset", instrumentRun.startsWith("TestIlluminaRun"));
        }

        log("Verifying instrument run and details page");
        dr.clickLink(2, "Instrument Run");
        waitForPageToLoad();
        waitForText("Instrument Run Details");
        waitForText("Run Id"); //crude proxy for loading of the details panel
        waitForText("Readsets");
        DataRegionTable rs = _helper.getDrForQueryWebpart("Readsets");
        Assert.assertEquals("Incorrect readset count found", 14, rs.getDataRowCount());

        waitForText("Quality Metrics");
        DataRegionTable qm = _helper.getDrForQueryWebpart("Quality Metrics");
        Assert.assertEquals("Incorrect quality metric count found", 30, qm.getDataRowCount());
        String totalSequences = qm.getDataAsText(qm.getRow("File Id", "Illumina-R1-Control.fastq.gz"), "Metric Value");
        Assert.assertEquals("Incorrect value for total sequences", "9.0", totalSequences);

        log("Verifying readset details page");
        goToProjectHome();
        _helper.clickNavPanelItem("Total Readsets Imported:", _readsetCt.toString());
        waitForPageToLoad();
        dr = new DataRegionTable("query", this);
        dr.clickLink(1,1);
        waitForPageToLoad();

        waitForText("Readset Details");
        waitForText("Readset Id:"); //crude proxy for details panel

        waitForText("Analyses Using This Readset");
        DataRegionTable dr1 = _helper.getDrForQueryWebpart("Analyses Using This Readset");
        Assert.assertEquals("Incorrect analysis count", 0, dr1.getDataRowCount());

        waitForText("Quality Metrics");
        DataRegionTable dr2 = _helper.getDrForQueryWebpart("Quality Metrics");
        Assert.assertEquals("Incorrect analysis count", 2, dr2.getDataRowCount());

        //verify export
        log("Verifying FASTQ Export");
        goToProjectHome();
        _helper.clickNavPanelItem("Total Readsets Imported:", _readsetCt.toString());
        waitForPageToLoad();
        dr = new DataRegionTable("query", this);
        dr.checkAllOnPage();
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_query']" +Locator.navButton("More Actions").getPath()), "Download Sequence Files");
        waitForElement(Ext4Helper.ext4Window("Export Files"));
        waitForText("Export Files As");
        Ext4CmpRef window = Ext4Helper.queryOne(this, "#exportFilesWin", Ext4CmpRef.class);
        String fileName = "MyFile";
        Ext4FieldRef.getForLabel(this, "File Prefix").setValue(fileName);
        String url = window.eval("this.getURL()");
        Assert.assertTrue("Improper URL to download sequences", url.contains("zipFileName=" + fileName));
        Assert.assertTrue("Improper URL to download sequences", url.contains("exportFiles.view?"));
        Assert.assertEquals("Wrong number of files selected", 28, StringUtils.countMatches(url, "dataIds="));

        Ext4Helper.queryOne(this, "field[boxLabel='Forward Reads']", Ext4FieldRef.class).setValue("false");
        Ext4Helper.queryOne(this,"field[boxLabel='Merge into Single FASTQ File']", Ext4FieldRef.class).setChecked(true);

        url = window.eval("this.getURL()");
        Assert.assertEquals("Wrong number of files selected", 14, StringUtils.countMatches(url, "dataIds="));
        Assert.assertTrue("Improper URL to download sequences", url.contains("mergeFastqFiles.view?"));

        clickButton("Cancel", 0);

        validateFastqDownload(url);

        log("Verifying FASTQC Report");
        dr.uncheckAllOnPage();
        dr.checkCheckbox(2);
        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_query']" + Locator.navButton("More Actions").getPath()), "View FASTQC Report");
        waitForPageToLoad();
        waitForText("File Summary");
        assertTextPresent("Per base sequence quality");

        log("Verifying View Analyses");
        goToProjectHome();
        _helper.clickNavPanelItem("Total Readsets Imported:", _readsetCt.toString());
        waitForPageToLoad();
        waitForText("Instrument Run"); //proxy for dataRegion loading
        dr = new DataRegionTable("query", this);
        dr.uncheckAllOnPage();

        //NOTE: this is going to be sensitive to the ordering of params by the DataRegion.  May need a more robust
        //approach if that is variable.
        StringBuilder filterText = new StringBuilder("readset IS ONE OF (");
        dr.checkCheckbox(1);
        filterText.append(dr.getDataAsText(1, "Readset Id") + ", ");
        dr.checkCheckbox(2);
        filterText.append(dr.getDataAsText(2, "Readset Id") + ")");

        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_query']" +Locator.navButton("More Actions").getPath()), "View Analyses");
        waitForPageToLoad();
        waitForText("Analysis Type"); //proxy for dataRegion loading
        assertTextPresent(filterText.toString());

        log("Verifying Readset Edit");
        goToProjectHome();
        _helper.clickNavPanelItem("Total Readsets Imported:", _readsetCt.toString());
        waitForPageToLoad();
        waitForText("Instrument Run"); //proxy for dataRegion loading
        dr.clickLink(1,0);
        waitForPageToLoad();
        waitForText("Run Id:");
        String newName = "ChangedSample";
        Ext4FieldRef.getForLabel(this, "Name").setValue(newName);
        sleep(250); //wait for value to save
        clickButton("Submit", 0);
        waitForElement(Ext4Helper.ext4Window("Success"));
        assertTextPresent("Your upload was successful!");
        clickButton("OK");
        waitForPageToLoad();
        Assert.assertEquals("Changed sample name not applied", newName, dr.getDataAsText(1, "Name"));

        //note: 'Analyze Selected' option is verified separately
    }

    /**
     * The intent of this method is to test the Sequence Analysis Panel,
     * with the goal of exercising all UI options.  It directly calls getJsonParams() on the panel,
     * in order to inspect the JSON it would send to the server, but does not initiate a pipeline job.
     */
    private void analysisPanelTest() throws JSONException
    {
        log("Verifying Analysis Panel UI");

        goToProjectHome();
        _helper.clickNavPanelItem("Total Readsets Imported:", _readsetCt.toString());
        waitForPageToLoad();
        DataRegionTable dr = new DataRegionTable("query", this);
        dr.uncheckAllOnPage();
        dr.checkCheckbox(2);
        List<String> rowIds = new ArrayList<String>();
        rowIds.add(dr.getDataAsText(2, "Readset Id"));
        dr.checkCheckbox(6);
        rowIds.add(dr.getDataAsText(6, "Readset Id"));

        ExtHelper.clickExtMenuButton(this, false, Locator.xpath("//table[@id='dataregion_query']" +Locator.navButton("More Actions").getPath()), "Analyze Selected");
        waitForElement(Ext4Helper.ext4Window("Import Data"));
        waitForText("Description");
        waitAndClick(Locator.ext4Button("Submit"));
        waitForPageToLoad();

        log("Verifying analysis UI");

        //setup local variables
        String totalReads = "450";
        String minReadLength = "68";
        String adapterMismatchTolerated = "3";
        String adapterDeletionsTolerated = "0";
        String qualWindowSize = "8";
        String qualAvgQual = "19";
        String maskMinQual = "21";
        String customRefName = "CustomRef1";
        String customRefSeq = "ATGATGATG";
        String jobName = "TestAnalysisJob";
        String protocolDescription = "This is the description for my analysis";
        String qualThreshold = "0.11";
        String minSnpQual = "23";
        String minAvgSnpQual = "24";
        String minDipQual = "25";
        String minAvgDipQual = "26";
        String maxAlignMismatch = "5";
        String strain = "HXB2";
        String[][] rocheAdapters = {{"Roche-454 FLX Amplicon A", "GCCTCCCTCGCGCCATCAG"}, {"Roche-454 FLX Amplicon B", "GCCTTGCCAGCCCGCTCAG"}};
        String assembleUnalignedPct = "90.3";
        String minContigsForNovel = "7";

        waitForText("Readset Name");
        for (String rowId : rowIds)
        {
            assertTextPresent("Illumina-R1-" + rowId + ".fastq.gz");
            assertTextPresent("Illumina-R2-" + rowId + ".fastq.gz");
        }

        Ext4FieldRef.getForLabel(this, "Job Name").setValue(jobName);
        Ext4FieldRef.getForLabel(this, "Protocol Description").setValue(protocolDescription);

        log("Verifying Pre-processing section");
        Assert.assertFalse("Reads field should be hidden", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Total Reads").getId()).toXpath()));
        Ext4FieldRef.getForLabel(this, "Downsample Reads").setChecked(true);
        Assert.assertTrue("Reads field should be visible", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Total Reads").getId()).toXpath()));

        Ext4FieldRef.getForLabel(this, "Total Reads").setValue(totalReads);

        Ext4FieldRef.getForLabel(this, "Minimum Read Length").setValue(minReadLength);
        Ext4FieldRef.getForLabel(this, "Adapter Trimming").setChecked(true);
        waitForText("Adapters");
        clickButton("Common Adapters", 0);
        waitForElement(Ext4Helper.ext4Window("Choose Adapters"));
        waitForText("Choose Adapter Group");
        Ext4FieldRef.getForLabel(this, "Choose Adapter Group").setValue("Roche-454 FLX Amplicon");
        waitAndClick(Locator.ext4Button("Submit"));

        waitForText(rocheAdapters[0][0]);
        waitForText(rocheAdapters[1][0]);
        assertTextBefore(rocheAdapters[0][0], rocheAdapters[1][0]);
        assertTextPresent(rocheAdapters[0][1]);
        assertTextPresent(rocheAdapters[1][1]);

        Ext4Helper.queryOne(this, "#adapterGrid", Ext4CmpRef.class).eval("this.getSelectionModel().select(0)");
        clickButton("Move Down", 0);
        sleep(500);
        assertTextBefore(rocheAdapters[1][0], rocheAdapters[0][0]);

        Ext4Helper.queryOne(this, "#adapterGrid", Ext4CmpRef.class).eval("this.getSelectionModel().select(1)");
        clickButton("Move Up", 0);
        sleep(500);
        assertTextBefore(rocheAdapters[0][0], rocheAdapters[1][0]);

        clickButton("Remove", 0);
        sleep(500);
        assertTextNotPresent(rocheAdapters[0][0]);

        Ext4FieldRef.getForLabel(this, "Mismatches Tolerated").setValue(adapterMismatchTolerated);
        Ext4FieldRef.getForLabel(this, "Deletions Tolerated").setValue(adapterDeletionsTolerated);

        Assert.assertFalse("Trim threshold field should be hidden", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Trim Threshold").getId()).toXpath()));
        Ext4FieldRef.getForLabel(this, "Quality Trimming (running score)").setChecked(true);
        Assert.assertTrue("Trim threshold field should be visible", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Trim Threshold").getId()).toXpath()));
        Ext4FieldRef.getForLabel(this, "Trim Threshold").setValue(qualThreshold);

        Assert.assertFalse("Window Size field should be hidden", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Window Size").getId()).toXpath()));
        Ext4FieldRef.getForLabel(this, "Quality Trimming (by window)").setChecked(true);
        Assert.assertTrue("Window Size field should be visible", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Window Size").getId()).toXpath()));

        Ext4FieldRef.getForLabel(this, "Window Size").setValue(qualWindowSize);
        Ext4FieldRef.getForLabel(this, "Avg Qual").setValue(qualAvgQual);

        Assert.assertFalse("Min Qual field should be hidden", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Min Qual").getId()).toXpath()));
        Ext4FieldRef.getForLabel(this, "Mask Low Quality Bases").setChecked(true);
        Assert.assertTrue("Min Qual field should be visible", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Min Qual").getId()).toXpath()));

        Ext4FieldRef.getForLabel(this, "Min Qual").setValue(maskMinQual);

        log("Testing Alignment Section");

        log("Testing whether sections are disabled when alignment unchecked");
        Ext4FieldRef.getForLabel(this, "Perform Alignment").setChecked(false);
        Assert.assertEquals("Field should be hidden", null, Ext4FieldRef.getForLabel(this, "Reference Library Type"));
        Assert.assertEquals("Field should be hidden", null, Ext4FieldRef.getForLabel(this, "Aligner"));
        Assert.assertEquals("Field should be hidden", null, Ext4FieldRef.getForLabel(this, "Min SNP Quality"));

        Assert.assertEquals("Field should be disabled", "true", Ext4FieldRef.getForLabel(this, "Call SNPs").eval("this.isDisabled()"));
        Assert.assertEquals("Field should be disabled", "true", Ext4FieldRef.getForLabel(this, "Sequence Based Genotyping").eval("this.isDisabled()"));

        Ext4FieldRef.getForLabel(this, "Perform Alignment").setChecked(true);
        waitForText("Reference Library Type");
        sleep(200);

        Ext4FieldRef.getForLabel(this, "Virus Strain").setValue(strain);

        Ext4FieldRef.getForLabel(this, "Use Custom Reference").setChecked(true);
        Assert.assertEquals("Field should be disabled", "true", Ext4FieldRef.getForLabel(this, "Virus Strain").eval("this.isDisabled()"));
        Assert.assertTrue("Field should be visible", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Reference Name").getId()).toXpath()));
        Assert.assertTrue("Field should be visible", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Sequence").getId()).toXpath()));

        Ext4FieldRef.getForLabel(this, "Reference Name").setValue(customRefName);
        Ext4FieldRef.getForLabel(this, "Sequence").setValue(customRefSeq);

        log("Testing aligner field and descriptions");
        Ext4FieldRef alignerField = Ext4FieldRef.getForLabel(this, "Aligner");

        alignerField.setValue("bwa");
        alignerField.eval("this.fireEvent(\"select\", this, this.getValue())");
        waitForText("BWA is a commonly used aligner");

        alignerField.setValue("bowtie");
        alignerField.eval("this.fireEvent(\"select\", this, this.getValue())");
        waitForText("Bowtie is a fast aligner often used for short reads");

        alignerField.setValue("lastz");
        alignerField.eval("this.fireEvent(\"select\", this, this.getValue())");
        waitForText("Lastz has performed well for both sequence-based");

        alignerField.setValue("mosaik");
        alignerField.eval("this.fireEvent(\"select\", this, this.getValue())");
        waitForText("Mosaik is suitable for longer reads");

        String aligner = "bwa-sw";
        alignerField.setValue(aligner);
        alignerField.eval("this.fireEvent(\"select\", this, this.getValue())");
        waitForText("BWA-SW uses a different algorithm than BWA");

        log("Testing SNP settings panel");
        Ext4FieldRef.getForLabel(this, "Call SNPs").setChecked(false);
        Assert.assertEquals("Field should be disabled", "true", Ext4FieldRef.getForLabel(this, "Sequence Based Genotyping").eval("this.isDisabled()"));
        Assert.assertEquals("Field should be hidden", null, Ext4FieldRef.getForLabel(this, "Min SNP Quality"));
        Assert.assertEquals("Field should be hidden", null, Ext4FieldRef.getForLabel(this, "Min Avg SNP Quality"));
        Assert.assertEquals("Field should be hidden", null, Ext4FieldRef.getForLabel(this, "Min DIP Quality"));
        Assert.assertEquals("Field should be hidden", null, Ext4FieldRef.getForLabel(this, "Min Avg DIP Quality"));

        Ext4FieldRef.getForLabel(this, "Call SNPs").setChecked(true);
        Assert.assertEquals("Field should be enabled", "false", Ext4FieldRef.getForLabel(this, "Sequence Based Genotyping").eval("this.isDisabled()"));
        Assert.assertTrue("Field should be visible", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Min SNP Quality").getId()).toXpath()));
        Assert.assertTrue("Field should be visible", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Min Avg SNP Quality").getId()).toXpath()));
        Assert.assertTrue("Field should be visible", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Min DIP Quality").getId()).toXpath()));
        Assert.assertTrue("Field should be visible", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Min Avg DIP Quality").getId()).toXpath()));

        Ext4FieldRef.getForLabel(this, "Min SNP Quality").setValue(minSnpQual);
        Ext4FieldRef.getForLabel(this, "Min Avg SNP Quality").setValue(minAvgSnpQual);
        Ext4FieldRef.getForLabel(this, "Min DIP Quality").setValue(minDipQual);
        Ext4FieldRef.getForLabel(this, "Min Avg DIP Quality").setValue(minAvgDipQual);

        Ext4FieldRef.getForLabel(this, "Sequence Based Genotyping").setChecked(true);

        Ext4FieldRef.getForLabel(this, "Max Alignment Mismatches").setValue(maxAlignMismatch);
        Ext4FieldRef.getForLabel(this, "Assemble Unaligned Reads").setChecked(true);

        Ext4FieldRef.getForLabel(this, "Assembly Percent Identity").setValue(assembleUnalignedPct);
        Ext4FieldRef.getForLabel(this, "Min Sequences Per Contig").setValue(minContigsForNovel);

        Ext4FieldRef.getForLabel(this, "Import Results into LabKey").setChecked(false);
        Assert.assertFalse("Field should be hidden", getWrapper().isVisible(Locator.id(Ext4FieldRef.getForLabel(this, "Do Not Import SNPs").getId()).toXpath()));
        Ext4FieldRef.getForLabel(this, "Import Results into LabKey").setChecked(true);
        Ext4FieldRef.getForLabel(this, "Do Not Import SNPs").setChecked(true);

        Ext4CmpRef panel = Ext4Helper.queryOne(this, "#sequenceAnalysisPanel", Ext4CmpRef.class);
        String jsonString = panel.eval("selenium.browserbot.getCurrentWindow().Ext4.encode(this.getJsonParams())");
        JSONObject json = new JSONObject(jsonString);

        Assert.assertEquals("Incorect param in form JSON", getContainerId(getURL().toString()), json.getString("containerId"));
        Assert.assertEquals("Incorect param in form JSON", getBaseURL() + "/", json.getString("baseUrl"));
        Assert.assertEquals("Incorect param in form JSON", selenium.getEval("window.LABKEY.Security.currentUser.id"), json.getString("userId"));
        String containerPath = getURL().getPath().replaceAll("/sequenceAnalysis.view", "").replaceAll("(.)*/sequenceanalysis", "");
        Assert.assertEquals("Incorect param in form JSON", containerPath, json.getString("containerPath"));

        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("deleteIntermediateFiles"));
        Assert.assertEquals("Incorect param in form JSON", protocolDescription, json.getString("protocolDescription"));
        Assert.assertEquals("Incorect param in form JSON", jobName, json.getString("protocolName"));

        Assert.assertEquals("Incorect param in form JSON", "false", json.getString("saveProtocol"));
        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("lkDbImport"));
        Assert.assertEquals("Incorect param in form JSON", "false", json.getString("recalibrateBam"));

        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("preprocessing.downsample"));
        Assert.assertEquals("Incorect param in form JSON", totalReads, json.getString("preprocessing.downsampleReadNumber"));

        Assert.assertEquals("Incorect param in form JSON", minReadLength, json.getString("preprocessing.minLength"));

        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("preprocessing.trimAdapters"));
        Assert.assertEquals("Incorect param in form JSON", adapterDeletionsTolerated, json.getString("preprocessing.adapterDeletionsAllowed"));
        Assert.assertEquals("Incorect param in form JSON", adapterMismatchTolerated, json.getString("preprocessing.adapterEditDistance"));

        JSONArray adapter = json.getJSONArray("adapter_0");
        Assert.assertEquals("Incorect param in form JSON", rocheAdapters[1][0], adapter.get(0));
        Assert.assertEquals("Incorect param in form JSON", rocheAdapters[1][1], adapter.get(1));
        Assert.assertEquals("Incorect param in form JSON", true, adapter.get(2));
        Assert.assertEquals("Incorect param in form JSON", true, adapter.get(3));

        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("preprocessing.qual2"));
        Assert.assertEquals("Incorect param in form JSON", qualAvgQual, json.getString("preprocessing.qual2_avgQual"));
        Assert.assertEquals("Incorect param in form JSON", qualWindowSize, json.getString("preprocessing.qual2_windowSize"));

        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("preprocessing.qual"));
        Assert.assertEquals("Incorect param in form JSON", qualThreshold, json.getString("preprocessing.qual_threshold"));

        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("preprocessing.mask"));
        Assert.assertEquals("Incorect param in form JSON", maskMinQual, json.getString("preprocessing.mask_minQual"));

        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("useCustomReference"));
        Assert.assertEquals("Incorect param in form JSON", customRefSeq, json.getString("virus.refSequence"));
        Assert.assertEquals("Incorect param in form JSON", customRefName, json.getString("virus.custom_strain_name"));

        Assert.assertEquals("Incorect param in form JSON", "", json.getString("dbprefix"));
        Assert.assertEquals("Incorect param in form JSON", "Virus", json.getString("dna.category"));

        Assert.assertEquals("Incorect param in form JSON", "Virus", json.getString("analysisType"));
        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("doTranslation"));

        Assert.assertEquals("Incorect param in form JSON", "null", json.getString("dna.subset"));
        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("qualityMetrics"));

        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("doAlignment"));
        Assert.assertEquals("Incorect param in form JSON", aligner, json.getString("aligner"));

        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("doSnpCalling"));
        Assert.assertEquals("Incorect param in form JSON", "false", json.getString("snp.neighborhoodQual"));
        Assert.assertEquals("Incorect param in form JSON", minSnpQual, json.getString("snp.minQual"));
        Assert.assertEquals("Incorect param in form JSON", minAvgSnpQual, json.getString("snp.minAvgSnpQual"));
        Assert.assertEquals("Incorect param in form JSON", minDipQual, json.getString("snp.minIndelQual"));
        Assert.assertEquals("Incorect param in form JSON", minAvgDipQual, json.getString("snp.minAvgDipQual"));

        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("assembleUnaligned"));
        Assert.assertEquals("Incorect param in form JSON", maxAlignMismatch, json.getString("maxAlignMismatch"));
        Assert.assertEquals("Incorect param in form JSON", assembleUnalignedPct, json.getString("assembleUnalignedPct"));
        Assert.assertEquals("Incorect param in form JSON", minContigsForNovel, json.getString("minContigsForNovel"));

        Assert.assertEquals("Incorect param in form JSON", "false", json.getString("debugMode"));

        JSONObject sample = json.getJSONObject("sample_0");
        String readset = sample.getString("readset");
        Assert.assertEquals("Incorect param in form JSON", "Illumina-R1-" + readset + ".fastq.gz", sample.getString("fileName"));
        Assert.assertEquals("Incorect param in form JSON", "Illumina-R2-" + readset + ".fastq.gz", sample.getString("fileName2"));
        Assert.assertEquals("Incorect param in form JSON", "ILLUMINA", sample.getString("platform"));
        Assert.assertFalse("Incorect param in form JSON", "".equals(sample.getString("instrument_run_id")));
        Assert.assertFalse("Incorect param in form JSON", "".equals(sample.getString("readsetname")));
        Assert.assertFalse("Incorect param in form JSON", "".equals(sample.getString("fileId")));
        Assert.assertFalse("Incorect param in form JSON", "".equals(sample.getString("fileId2")));

        sample = json.getJSONObject("sample_1");
        readset = sample.getString("readset");
        Assert.assertEquals("Incorect param in form JSON", "Illumina-R1-" + readset + ".fastq.gz", sample.getString("fileName"));
        Assert.assertEquals("Incorect param in form JSON", "Illumina-R2-" + readset + ".fastq.gz", sample.getString("fileName2"));
        Assert.assertEquals("Incorect param in form JSON", "ILLUMINA", sample.getString("platform"));
        Assert.assertFalse("Incorect param in form JSON", "".equals(sample.getString("instrument_run_id")));
        Assert.assertFalse("Incorect param in form JSON", "".equals(sample.getString("readsetname")));
        Assert.assertFalse("Incorect param in form JSON", "".equals(sample.getString("fileId")));
        Assert.assertFalse("Incorect param in form JSON", "".equals(sample.getString("fileId2")));

        log("Testing UI changes when selecting different analysis settings");

        String url = getURL().toString();
        url += "&debugMode=1";
        beginAt(url);
        waitForPageToLoad();

        Ext4FieldRef analysisField = Ext4FieldRef.getForLabel(this, "Specialized Analysis");
        analysisField.setValue("SBT");

        Assert.assertEquals("Incorrect field value", "DNA", Ext4FieldRef.getForLabel(this, "Reference Library Type").getValue());
        String species = "Human";
        String molType = "gDNA";

        String jobName2 = "Job2";
        Ext4FieldRef.getForLabel(this, "Job Name").setValue(jobName2);
        waitForElementToDisappear(Ext4Helper.invalidField(), 500);

        Ext4FieldRef.getForLabel(this, "Species").setValue(species);
        Ext4FieldRef.getForLabel(this, "Subset").eval("this.setValue([\"KIR\",\"MHC\"])");
        Ext4FieldRef.getForLabel(this, "Molecule Type").setValue(molType);
        Ext4FieldRef.getForLabel(this, "Loci").eval("this.setValue([\"KIR1D\",\"HLA-A\"])");

        jsonString = panel.eval("selenium.browserbot.getCurrentWindow().Ext4.encode(this.getJsonParams())");
        json = new JSONObject(jsonString);

        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("debugMode"));
        Assert.assertEquals("Incorect param in form JSON", species, json.getString("dna.species"));
        Assert.assertEquals("Incorect param in form JSON", "KIR;MHC", json.getString("dna.subset"));
        Assert.assertEquals("Incorect param in form JSON", molType, json.getString("dna.mol_type"));
        Assert.assertEquals("Incorect param in form JSON", "KIR1D;HLA-A", json.getString("dna.locus"));

        //also test lastz params
        Assert.assertEquals("Incorect param in form JSON", "lastz", json.getString("aligner"));
        Assert.assertEquals("Incorect param in form JSON", "90", json.getString("lastz.continuity"));
        Assert.assertEquals("Incorect param in form JSON", "98", json.getString("lastz.identity"));

        Assert.assertEquals("Incorect param in form JSON", "true", json.getString("noSnpImport"));
        Assert.assertEquals("Incorect param in form JSON", 4, StringUtils.split(json.getString("fileNames"), ";").length);
        Assert.assertEquals("Incorect param in form JSON", "0", json.getString("maxAlignMismatch"));
    }

    /**
     * This method will make a request to download merged FASTQ files created during the illumina test
     * @param url
     * @throws Exception
     */
    private void validateFastqDownload(String url) throws Exception
    {
        log("Verifying merged FASTQ export");

        HttpClient httpClient = WebTestHelper.getHttpClient(url);
        GetMethod method = null;

        try
        {
            //first try FASTQ merge
            url = getBaseURL().replaceAll(getContextPath(), "") + url;
            method = new GetMethod(url);
            int status = httpClient.executeMethod(method);
            Assert.assertTrue("FASTQ was not Downloaded", status == HttpStatus.SC_OK);
            Assert.assertTrue("Response header incorrect", method.getResponseHeader("Content-Disposition").getValue().startsWith("attachment;"));
            Assert.assertTrue("Response header incorrect", method.getResponseHeader("Content-Type").getValue().startsWith("application/x-gzip"));

            InputStream is = null;
            GZIPInputStream gz = null;
            BufferedReader br = null;

            try
            {
                is = method.getResponseBodyAsStream();
                gz = new GZIPInputStream(is);
                br = new BufferedReader(new InputStreamReader(gz));
                int count = 0;
                String thisLine;
                while ((thisLine = br.readLine()) != null)
                {
                    count++;
                }

                int expectedLength = 504;
                log("Response length was " + count + ", expected " + expectedLength);
                //TODO: reenable this check once it can be made to work reliably on team city
                //Assert.assertTrue("Length of file doesnt match expected value of "+expectedLength+", was: " + count, count == expectedLength);

                method.releaseConnection();
            }
            finally
            {
                if(is != null)
                    is.close();
                if(gz != null)
                    gz.close();
                if(br != null)
                    br.close();
            }

        }
        finally
        {
            if (null != method)
                method.releaseConnection();
        }
    }

    private void selectPipelineJob(String importAction, String... files)
    {
        goToProjectHome();
        waitForText("Upload Files");
        _helper.clickSpanContaining("Upload Files / Start Analysis");
        waitForPageToLoad();
        waitForText("fileset");
        ExtHelper.selectFileBrowserRoot(this);
        for (String f : files)
        {
            ExtHelper.selectFileBrowserItem(this, f);
        }

        selectImportDataAction(importAction);
        waitForPageToLoad();
    }

    /**
     * The intent of this method is to perform additional tests of this Readset Import Panel,
     * with the goal of exercising all UI options
     */
    private void readsetPanelTest()
    {
        //TODO: perform extra tests on UI of this panel
    }

    @Override
    protected void doCleanup() throws Exception
    {
        File dir = new File(illuminaPipelineLoc);
        File[] files = dir.listFiles();
        for(File file: files)
        {
            if(file.isDirectory() &&
                (file.getName().startsWith("sequenceAnalysis_") || file.getName().equals("illuminaImport") || file.getName().equals(".labkey")))
                deleteDir(file);
            if(file.getName().startsWith("SequenceImport"))
                file.delete();
        }

        deleteProject(getProjectName());
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}