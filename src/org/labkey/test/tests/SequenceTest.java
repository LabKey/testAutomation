/*
 * Copyright (c) 2012 LabKey Corporation
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
package org.labkey.test.tests;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.junit.Assert;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LabModuleHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.ext4cmp.Ext4CmpRefWD;
import org.labkey.test.util.ext4cmp.Ext4FieldRefWD;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 5/28/12
 * Time: 7:12 PM
 */
public class SequenceTest extends BaseWebDriverTest
{
    protected LabModuleHelper _helper = new LabModuleHelper(this);
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
        _helper.clickNavPanelItem("Create Readsets");
        waitForPageToLoad();
        waitForText("Run Id");
        _ext4Helper.clickTabContainingText("Import Spreadsheet");
        waitForText("Copy/Paste Data");

        setFormElementJS(Locator.name("text"), getIlluminaNames());

        waitAndClick(Locator.ext4Button("Upload"));
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
        _helper.waitForField("Investigator Name");

        Ext4FieldRefWD.getForLabel(this, "Reagent Cassette Id").setValue("FlowCell");

        String[][] fieldPairs = {
                {"Investigator Name", "Investigator"},
                {"Experiment Name", "Experiment"},
                {"Project Name", "Project"},
                {"Description", "Description"}
        };

        for (String[] a : fieldPairs)
        {
            Ext4FieldRefWD.getForLabel(this, a[0]).setValue(a[1]);
        }

        //save combo record count for later use
        Ext4FieldRefWD templateCombo = Ext4FieldRefWD.getForLabel(this, "Template");
        Long originalCount = (Long)templateCombo.getEval("store.getCount()");

        _ext4Helper.clickTabContainingText("Preview Header");
        waitForText("Edit Sheet");
        for (String[] a : fieldPairs)
        {
            Assert.assertEquals(a[1], Ext4FieldRefWD.getForLabel(this, a[0]).getValue());
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
        Ext4FieldRefWD textarea = _ext4Helper.queryOne("textarea[itemId='sourceField']", Ext4FieldRefWD.class);
        String newValue = prop_name + "," + prop_value;
        String val = textarea.getValue();
        val += "\n" + newValue;
        textarea.setValue(val);
        clickButton("Done Editing", 0);

        //verify template has changed
        _ext4Helper.clickTabContainingText("General Info");
        Assert.assertEquals("Custom", Ext4FieldRefWD.getForLabel(this, "Template").getValue());

        //verify values persisted
        _ext4Helper.clickTabContainingText("Preview Header");
        waitForText("Edit Sheet");
        Assert.assertEquals(prop_value, Ext4FieldRefWD.getForLabel(this, prop_name).getValue());

        //save template
        clickButton("Save As Template", 0);
        waitForElement(Ext4Helper.ext4Window("Choose Name"));
        Ext4FieldRefWD textfield = _ext4Helper.queryOne("textfield", Ext4FieldRefWD.class);
        textfield.setValue(TEMPLATE_NAME);
        clickButton("OK", 0);
        _ext4Helper.clickTabContainingText("General Info");
        Assert.assertEquals(TEMPLATE_NAME, Ext4FieldRefWD.getForLabel(this, "Template").getValue());

        //if we navigate too quickly, before the insertRows has returned, the test can get a JS error
        //therefore we sleep
        sleep(500);

        //verify samples present
        _ext4Helper.clickTabContainingText("Preview Samples");
        waitForText("Sample_ID");

        int expectRows = (11 * (14 +  1));  //11 cols, 14 rows, plus header
        Assert.assertEquals(expectRows, getXpathCount(Locator.xpath("//td[contains(@class, 'x4-table-layout-cell')]")));

        //make sure values persisted
        refresh();
        String url = getCurrentRelativeURL();
        url += "&exportAsWebPage=1";
        beginAt(url);

        waitForText("Template");

        _helper.waitForField("Investigator Name");
        for (String[] a : fieldPairs)
        {
            Ext4FieldRefWD.getForLabel(this, a[0]).setValue(a[1]);
        }

        templateCombo.setValue(TEMPLATE_NAME);

        Long count = (Long)templateCombo.getEval("store.getCount()");
        Long expected = originalCount + 1;
        Assert.assertEquals("Combo store does not have correct record number", expected, count);
        sleep(50);
        Assert.assertEquals("Field value not set correctly", TEMPLATE_NAME, Ext4FieldRefWD.getForLabel(this, "Template").getValue());
        _ext4Helper.clickTabContainingText("Preview Header");
        waitForText("Edit Sheet");
        Assert.assertEquals(prop_value, Ext4FieldRefWD.getForLabel(this, prop_name).getValue());

        //NOTE: hitting download will display the text in the browser; however, this replaces newlines w/ spaces.  therefore we use selenium
        //to directly get the output
        Ext4CmpRefWD panel = _ext4Helper.queryOne("#illuminaPanel", Ext4CmpRefWD.class);
        String outputTable = panel.getEval("getTableOutput().join(\"<>\")").toString();
        outputTable = outputTable.replaceAll("<>", System.getProperty("line.separator"));

        //then we download anyway
        clickButton("Download");

        //the browser converts line breaks to spaces.  this is a hack to get them back
        String text = _driver.getPageSource().replaceAll("<[^>]+>|&[^;]+;", "");
        text = text.replaceAll(" {2,}", " ");
        text = text.replaceAll(", ", ",\n").replaceAll("] ", "]\n");
        for (String[] a : fieldPairs)
        {
            String line = a[0] + "," + a[1];
            assertTextPresent(line);

            text.replaceAll(line, line + System.getProperty("line.separator"));
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

        setFormElement(Locator.name("protocolName"), "TestIlluminaRun" + _helper.getRandomInt());
        setFormElement(Locator.name("runDate"), "08/25/2011");
        setFormElement(Locator.name("fastqPrefix"), "Il");

        click(Locator.ext4Button("Import Data"));
        waitAndClick(Locator.ext4Button("OK"));
        waitForPageToLoad();
        clickLinkWithText("All");
        waitForPipelineJobsToComplete(1, "Import Illumina", false);
        assertTextPresent("COMPLETE");
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
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_query']" +Locator.navButton("More Actions").getPath()), "Download Sequence Files");
        waitForElement(Ext4Helper.ext4Window("Export Files"));
        waitForText("Export Files As");
        Ext4CmpRefWD window = _ext4Helper.queryOne("#exportFilesWin", Ext4CmpRefWD.class);
        String fileName = "MyFile";
        Ext4FieldRefWD.getForLabel(this, "File Prefix").setValue(fileName);
        String url = window.getEval("getURL()").toString();
        Assert.assertTrue("Improper URL to download sequences", url.contains("zipFileName=" + fileName));
        Assert.assertTrue("Improper URL to download sequences", url.contains("exportFiles.view?"));
        Assert.assertEquals("Wrong number of files selected", 28, StringUtils.countMatches(url, "dataIds="));

        _ext4Helper.queryOne("field[boxLabel='Forward Reads']", Ext4FieldRefWD.class).setValue("false");
        _ext4Helper.queryOne("field[boxLabel='Merge into Single FASTQ File']", Ext4FieldRefWD.class).setChecked(true);

        url = window.getEval("getURL()").toString();
        Assert.assertEquals("Wrong number of files selected", 14, StringUtils.countMatches(url, "dataIds="));
        Assert.assertTrue("Improper URL to download sequences", url.contains("mergeFastqFiles.view?"));

        waitAndClick(Locator.ext4Button("Cancel"));

        validateFastqDownload(url);

        log("Verifying FASTQC Report");
        dr.uncheckAllOnPage();
        dr.checkCheckbox(2);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_query']" + Locator.navButton("More Actions").getPath()), "View FASTQC Report");
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
        dr.checkCheckbox(1);
        String id1 = dr.getDataAsText(1, "Readset Id");
        dr.checkCheckbox(2);
        String id2 = dr.getDataAsText(2, "Readset Id");

        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_query']" +Locator.navButton("More Actions").getPath()), "View Analyses");
        waitForPageToLoad();
        waitForText("Analysis Type"); //proxy for dataRegion loading

        assertTextPresent("readset IS ONE OF (");
        Assert.assertTrue("Filter not applied correct", (isTextPresent(id1 + ", " + id2) || isTextPresent(id2 + ", " + id1)));

        log("Verifying Readset Edit");
        goToProjectHome();
        _helper.clickNavPanelItem("Total Readsets Imported:", _readsetCt.toString());
        waitForPageToLoad();
        waitForText("Instrument Run"); //proxy for dataRegion loading
        dr.clickLink(1,0);
        waitForPageToLoad();
        waitForText("Run Id:");
        String newName = "ChangedSample";
        Ext4FieldRefWD.getForLabel(this, "Name").setValue(newName);
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

        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_query']" +Locator.navButton("More Actions").getPath()), "Analyze Selected");
        waitForElement(Ext4Helper.ext4Window("Import Data"));
        waitForText("Description");
        waitAndClick(Locator.ext4Button("Submit"));
        waitForPageToLoad();

        log("Verifying analysis UI");

        //setup local variables
        String totalReads = "450";
        String minReadLength = "68";
        String seedMismatches = "3";
        String simpleClipThreshold = "0";
        String qualWindowSize = "8";
        String qualAvgQual = "19";
        //String maskMinQual = "21";
        //String customRefName = "CustomRef1";
        //String customRefSeq = "ATGATGATG";
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

        Ext4FieldRefWD.getForLabel(this, "Job Name").setValue(jobName);
        Ext4FieldRefWD.getForLabel(this, "Protocol Description").setValue(protocolDescription);

        log("Verifying Pre-processing section");
        WebElement el = _driver.findElement(By.id(Ext4FieldRefWD.getForLabel(this, "Total Reads").getId()));
        Assert.assertFalse("Reads field should be hidden", el.isDisplayed());
        Ext4FieldRefWD.getForLabel(this, "Downsample Reads").setChecked(true);
        el = _driver.findElement(By.id(Ext4FieldRefWD.getForLabel(this, "Total Reads").getId()));
        Assert.assertTrue("Reads field should be visible", el.isDisplayed());

        Ext4FieldRefWD.getForLabel(this, "Total Reads").setValue(totalReads);

        Ext4FieldRefWD.getForLabel(this, "Minimum Read Length").setValue(minReadLength);
        Ext4FieldRefWD.getForLabel(this, "Adapter Trimming").setChecked(true);
        waitForText("Adapters");
        clickButton("Common Adapters", 0);
        waitForElement(Ext4Helper.ext4Window("Choose Adapters"));
        waitForText("Choose Adapter Group");
        Ext4FieldRefWD.getForLabel(this, "Choose Adapter Group").setValue("Roche-454 FLX Amplicon");
        waitAndClick(Locator.ext4Button("Submit"));

        waitForText(rocheAdapters[0][0]);
        waitForText(rocheAdapters[1][0]);
        assertTextBefore(rocheAdapters[0][0], rocheAdapters[1][0]);
        assertTextPresent(rocheAdapters[0][1]);
        assertTextPresent(rocheAdapters[1][1]);

        _ext4Helper.queryOne("#adapterGrid", Ext4CmpRefWD.class).eval("getSelectionModel().select(0)");
        clickButton("Move Down", 0);
        sleep(500);
        assertTextBefore(rocheAdapters[1][0], rocheAdapters[0][0]);

        _ext4Helper.queryOne("#adapterGrid", Ext4CmpRefWD.class).eval("getSelectionModel().select(1)");
        clickButton("Move Up", 0);
        sleep(500);
        assertTextBefore(rocheAdapters[0][0], rocheAdapters[1][0]);

        clickButton("Remove", 0);
        sleep(500);
        assertTextNotPresent(rocheAdapters[0][0]);

        Ext4FieldRefWD.getForLabel(this, "Seed Mismatches").setValue(seedMismatches);
        Ext4FieldRefWD.getForLabel(this, "Simple Clip Threshold").setValue(simpleClipThreshold);

        el = _driver.findElement(By.id(Ext4FieldRefWD.getForLabel(this, "Window Size").getId()));
        Assert.assertFalse("Window Size field should be hidden", el.isDisplayed());
        Ext4FieldRefWD.getForLabel(this, "Quality Trimming (by sliding window)").setChecked(true);

        el = _driver.findElement(By.id(Ext4FieldRefWD.getForLabel(this, "Window Size").getId()));
        Assert.assertTrue("Window Size field should be visible", el.isDisplayed());

        Ext4FieldRefWD.getForLabel(this, "Window Size").setValue(qualWindowSize);
        Ext4FieldRefWD.getForLabel(this, "Avg Qual").setValue(qualAvgQual);

        el = _driver.findElement(By.id(Ext4FieldRefWD.getForLabel(this, "Min Qual").getId()));
        Assert.assertFalse("Min Qual field should be hidden", el.isDisplayed());

        log("Testing Alignment Section");

        log("Testing whether sections are disabled when alignment unchecked");
        Ext4FieldRefWD.getForLabel(this, "Perform Alignment").setChecked(false);
        Assert.assertEquals("Field should be hidden", false, Ext4FieldRefWD.isFieldPresent(this, "Reference Library Type"));
        Assert.assertEquals("Field should be hidden", false, Ext4FieldRefWD.isFieldPresent(this, "Aligner"));

        Assert.assertEquals("Field should be disabled", true, (Boolean)Ext4FieldRefWD.getForLabel(this, "Sequence Based Genotyping").getEval("isDisabled()"));
        Assert.assertEquals("Field should be disabled", true, (Boolean)Ext4FieldRefWD.getForLabel(this, "Min SNP Quality").getEval("isDisabled()"));

        Ext4FieldRefWD.getForLabel(this, "Perform Alignment").setChecked(true);
        waitForText("Reference Library Type");
        sleep(500);

        log("Testing aligner field and descriptions");
        Ext4FieldRefWD alignerField = Ext4FieldRefWD.getForLabel(this, "Aligner");

        alignerField.setValue("bwa");
        alignerField.eval("fireEvent(\"select\", Ext4.getCmp('" + alignerField.getId() + "'), 'bwa')");
        waitForText("BWA is a commonly used aligner");

        alignerField.setValue("bowtie");
        alignerField.eval("fireEvent(\"select\", Ext4.getCmp('" + alignerField.getId() + "'), 'bowtie')");
        waitForText("Bowtie is a fast aligner often used for short reads");

        alignerField.setValue("lastz");
        alignerField.eval("fireEvent(\"select\", Ext4.getCmp('" + alignerField.getId() + "'), 'lastz')");
        waitForText("Lastz has performed well for both sequence-based");

        alignerField.setValue("mosaik");
        alignerField.eval("fireEvent(\"select\", Ext4.getCmp('" + alignerField.getId() + "'), 'mosaik')");
        waitForText("Mosaik is suitable for longer reads");

        String aligner = "bwa-sw";
        alignerField.setValue(aligner);
        alignerField.eval("fireEvent(\"select\", Ext4.getCmp('" + alignerField.getId() + "'), '" + aligner + "')");
        waitForText("BWA-SW uses a different algorithm than BWA");

        final BaseWebDriverTest test = this;
        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                return !(Boolean)Ext4FieldRefWD.getForLabel(test, "Virus Strain").getEval("store.isLoading()");
            }
        }, "Combo store did not load", WAIT_FOR_JAVASCRIPT);

        sleep(100);
        Ext4FieldRefWD.getForLabel(this, "Virus Strain").setValue(strain);

        Ext4FieldRefWD.getForLabel(this, "Min SNP Quality").setValue(minSnpQual);
        Ext4FieldRefWD.getForLabel(this, "Min Avg SNP Quality").setValue(minAvgSnpQual);
        Ext4FieldRefWD.getForLabel(this, "Min DIP Quality").setValue(minDipQual);
        Ext4FieldRefWD.getForLabel(this, "Min Avg DIP Quality").setValue(minAvgDipQual);

        Ext4FieldRefWD.getForLabel(this, "Sequence Based Genotyping").setChecked(true);

        //TODO
//        Ext4FieldRefWD.getForLabel(this, "Max Alignment Mismatches").setValue(maxAlignMismatch);
//        Ext4FieldRefWD.getForLabel(this, "Assemble Unaligned Reads").setChecked(true);
//        Ext4FieldRefWD.getForLabel(this, "Assembly Percent Identity").setValue(assembleUnalignedPct);
//        Ext4FieldRefWD.getForLabel(this, "Min Sequences Per Contig").setValue(minContigsForNovel);

        Ext4CmpRefWD panel = _ext4Helper.queryOne("#sequenceAnalysisPanel", Ext4CmpRefWD.class);
        Map<String, Object> params = (Map)panel.getEval("getJsonParams()");

        Assert.assertEquals("Incorect param in form JSON", getContainerId(getURL().toString()), params.get("containerId"));
        Assert.assertEquals("Incorect param in form JSON", getBaseURL() + "/", params.get("baseUrl"));

        Long id1 = (Long)executeScript("return LABKEY.Security.currentUser.id");
        Long id2 = (Long)params.get("userId");
        Assert.assertEquals("Incorect param in form JSON", id1, id2);
        String containerPath = getURL().getPath().replaceAll("/sequenceAnalysis.view", "").replaceAll("(.)*/sequenceanalysis", "");
        Assert.assertEquals("Incorect param in form JSON", containerPath, params.get("containerPath"));

        Assert.assertEquals("Incorect param in form JSON", true, params.get("deleteIntermediateFiles"));
        Assert.assertEquals("Incorect param in form JSON", protocolDescription, params.get("protocolDescription"));
        Assert.assertEquals("Incorect param in form JSON", jobName, params.get("protocolName"));

        Assert.assertEquals("Incorect param in form JSON", false, params.get("saveProtocol"));
        Assert.assertEquals("Incorect param in form JSON", false, params.get("recalibrateBam"));

        Assert.assertEquals("Incorect param in form JSON", true, params.get("preprocessing.downsample"));
        Assert.assertEquals("Incorect param in form JSON", totalReads, params.get("preprocessing.downsampleReadNumber").toString());

        Assert.assertEquals("Incorect param in form JSON", minReadLength, params.get("preprocessing.minLength").toString());

        Assert.assertEquals("Incorect param in form JSON", true, params.get("preprocessing.trimAdapters"));

        Assert.assertEquals("Incorect param in form JSON", seedMismatches, params.get("preprocessing.seedMismatches").toString());
        Assert.assertEquals("Incorect param in form JSON", simpleClipThreshold, params.get("preprocessing.simpleClipThreshold").toString());

        List<Object> adapter = (List)params.get("adapter_0");
        Assert.assertEquals("Incorect param in form JSON", rocheAdapters[1][0], adapter.get(0).toString());
        Assert.assertEquals("Incorect param in form JSON", rocheAdapters[1][1], adapter.get(1).toString());
        Assert.assertEquals("Incorect param in form JSON", true, adapter.get(2));
        Assert.assertEquals("Incorect param in form JSON", true, adapter.get(3));

        Assert.assertEquals("Incorect param in form JSON", true, params.get("preprocessing.qual2"));
        Assert.assertEquals("Incorect param in form JSON", qualAvgQual, params.get("preprocessing.qual2_avgQual").toString());
        Assert.assertEquals("Incorect param in form JSON", qualWindowSize, params.get("preprocessing.qual2_windowSize").toString());

//        Assert.assertEquals("Incorect param in form JSON", "true", params.get("preprocessing.mask"));
//        Assert.assertEquals("Incorect param in form JSON", maskMinQual, params.get("preprocessing.mask_minQual"));

//        Assert.assertEquals("Incorect param in form JSON", "true", params.get("useCustomReference"));
//        Assert.assertEquals("Incorect param in form JSON", customRefSeq, params.get("virus.refSequence"));
//        Assert.assertEquals("Incorect param in form JSON", customRefName, params.get("virus.custom_strain_name"));

        Assert.assertEquals("Incorect param in form JSON", strain, params.get("dbprefix"));
        Assert.assertEquals("Incorect param in form JSON", "Virus", params.get("dna.category"));

        Assert.assertEquals("Incorect param in form JSON", "Virus", params.get("analysisType"));
        Assert.assertEquals("Incorect param in form JSON", strain, params.get("dna.subset"));
        Assert.assertEquals("Incorect param in form JSON", true, params.get("qualityMetrics"));

        Assert.assertEquals("Incorect param in form JSON", true, params.get("doAlignment"));
        Assert.assertEquals("Incorect param in form JSON", aligner, params.get("aligner"));

        Assert.assertEquals("Incorect param in form JSON", false, params.get("snp.neighborhoodQual"));
        Assert.assertEquals("Incorect param in form JSON", minSnpQual, params.get("snp.minQual").toString());
        Assert.assertEquals("Incorect param in form JSON", minAvgSnpQual, params.get("snp.minAvgSnpQual").toString());
        Assert.assertEquals("Incorect param in form JSON", minDipQual, params.get("snp.minIndelQual").toString());
        Assert.assertEquals("Incorect param in form JSON", minAvgDipQual, params.get("snp.minAvgDipQual").toString());

        //Assert.assertEquals("Incorect param in form JSON", "true", params.get("assembleUnaligned"));
        //Assert.assertEquals("Incorect param in form JSON", maxAlignMismatch, params.get("maxAlignMismatch"));
        //Assert.assertEquals("Incorect param in form JSON", assembleUnalignedPct, params.get("assembleUnalignedPct"));
        //Assert.assertEquals("Incorect param in form JSON", minContigsForNovel, params.get("minContigsForNovel"));

        Assert.assertEquals("Incorect param in form JSON", false, params.get("debugMode"));

        Map sample = (Map)params.get("sample_0");
        Long readset = (Long)sample.get("readset");
        Assert.assertEquals("Incorect param in form JSON", "Illumina-R1-" + readset + ".fastq.gz", sample.get("fileName"));
        Assert.assertEquals("Incorect param in form JSON", "Illumina-R2-" + readset + ".fastq.gz", sample.get("fileName2"));
        Assert.assertEquals("Incorect param in form JSON", "ILLUMINA", sample.get("platform"));
        Assert.assertFalse("Incorect param in form JSON", null == sample.get("instrument_run_id"));
        Assert.assertFalse("Incorect param in form JSON", null == sample.get("readsetname"));
        Assert.assertFalse("Incorect param in form JSON", null == sample.get("fileId"));
        Assert.assertFalse("Incorect param in form JSON", null == sample.get("fileId2"));

        sample = (Map)params.get("sample_1");
        readset = (Long)sample.get("readset");
        Assert.assertEquals("Incorect param in form JSON", "Illumina-R1-" + readset + ".fastq.gz", sample.get("fileName"));
        Assert.assertEquals("Incorect param in form JSON", "Illumina-R2-" + readset + ".fastq.gz", sample.get("fileName2"));
        Assert.assertEquals("Incorect param in form JSON", "ILLUMINA", sample.get("platform"));
        Assert.assertFalse("Incorect param in form JSON", null == sample.get("instrument_run_id"));
        Assert.assertFalse("Incorect param in form JSON", null == sample.get("readsetname"));
        Assert.assertFalse("Incorect param in form JSON", null == sample.get("fileId"));
        Assert.assertFalse("Incorect param in form JSON", null == sample.get("fileId2"));

        Assert.assertEquals("Incorect param in form JSON", true, params.get("sbtAnalysis"));
        Assert.assertEquals("Incorect param in form JSON", true, params.get("aaSnpByCodon"));
        Assert.assertEquals("Incorect param in form JSON", true, params.get("ntSnpByPosition"));
        Assert.assertEquals("Incorect param in form JSON", true, params.get("ntCoverage"));

        log("Testing UI changes when selecting different analysis settings");

        String url = getURL().toString();
        url += "&debugMode=1";
        beginAt(url);
        waitForPageToLoad();

        Ext4FieldRefWD analysisField = Ext4FieldRefWD.getForLabel(this, "Specialized Analysis");
        analysisField.setValue("SBT");

        Assert.assertEquals("Incorrect field value", "DNA", Ext4FieldRefWD.getForLabel(this, "Reference Library Type").getValue());
        String species = "Human";
        String molType = "gDNA";

        String jobName2 = "Job2";
        Ext4FieldRefWD.getForLabel(this, "Job Name").setValue(jobName2);
        waitForElementToDisappear(Ext4Helper.invalidField(), WAIT_FOR_JAVASCRIPT);

        Ext4FieldRefWD.getForLabel(this, "Species").setValue(species);
        Ext4FieldRefWD.getForLabel(this, "Subset").eval("setValue([\"KIR\",\"MHC\"])");
        Ext4FieldRefWD.getForLabel(this, "Molecule Type").setValue(molType);
        Ext4FieldRefWD.getForLabel(this, "Loci").eval("setValue([\"KIR1D\",\"HLA-A\"])");

        Ext4FieldRefWD.getForLabel(this, "Calculate and Save NT SNPs").setValue("false");
        Ext4FieldRefWD.getForLabel(this, "Calculate and Save Coverage Depth").setValue("false");
        Ext4FieldRefWD.getForLabel(this, "Calculate and Save AA SNPs").setValue("false");

        params = (Map)panel.getEval("getJsonParams()");

        Assert.assertEquals("Incorect param in form JSON", true, params.get("debugMode"));
        Assert.assertEquals("Incorect param in form JSON", species, params.get("dna.species"));
        Assert.assertEquals("Incorect param in form JSON", "KIR;MHC", params.get("dna.subset"));
        Assert.assertEquals("Incorect param in form JSON", molType, params.get("dna.mol_type"));
        Assert.assertEquals("Incorect param in form JSON", "KIR1D;HLA-A", params.get("dna.locus"));

        //also test mosaik params
        Assert.assertEquals("Incorect param in form JSON", "mosaik", params.get("aligner"));
        Assert.assertEquals("Incorect param in form JSON", 0.02, params.get("mosaik.max_mismatch_pct"));
        Assert.assertEquals("Incorect param in form JSON", new Long(200), params.get("mosaik.max_hash_positions"));

        Assert.assertEquals("Incorect param in form JSON", true, params.get("sbtAnalysis"));
        Assert.assertEquals("Incorect param in form JSON", false, params.get("aaSnpByCodon"));
        Assert.assertEquals("Incorect param in form JSON", false, params.get("ntCoverage"));
        Assert.assertEquals("Incorect param in form JSON", false, params.get("ntSnpByPosition"));
        Assert.assertEquals("Incorect param in form JSON", 4, StringUtils.split((String)params.get("fileNames"), ";").length);
    }

    /**
     * This method will make a request to download merged FASTQ files created during the illumina test
     * @param url
     * @throws Exception
     */
    private void validateFastqDownload(String url) throws Exception
    {
        log("Verifying merged FASTQ export");

        HttpClient httpClient = WebTestHelper.getHttpClient();
        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpGet method = null;
        HttpResponse response = null;

        try
        {
            //first try FASTQ merge
            url = getBaseURL().replaceAll(getContextPath(), "") + url;
            method = new HttpGet(url);
            response = httpClient.execute(method, context);
            int status = response.getStatusLine().getStatusCode();
            Assert.assertEquals("FASTQ was not Downloaded", HttpStatus.SC_OK, status);
            Assert.assertTrue("Response header incorrect", response.getHeaders("Content-Disposition")[0].getValue().startsWith("attachment;"));
            Assert.assertTrue("Response header incorrect", response.getHeaders("Content-Type")[0].getValue().startsWith("application/x-gzip"));

            InputStream is = null;
            GZIPInputStream gz = null;
            BufferedReader br = null;

            try
            {
                is = response.getEntity().getContent();
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
            if (null != response)
                EntityUtils.consume(response.getEntity());
            if (httpClient != null)
                httpClient.getConnectionManager().shutdown();
        }
    }

    private void selectPipelineJob(String importAction, String... files)
    {
        goToProjectHome();
        waitForText("Upload Files");
        _helper.clickNavPanelItem("Upload Files / Start Analysis");
        waitForPageToLoad();
        waitForText("fileset");
        _extHelper.selectFileBrowserRoot();
        for (String f : files)
        {
            _extHelper.selectFileBrowserItem(f);
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
    protected void doCleanup(boolean afterTest)
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