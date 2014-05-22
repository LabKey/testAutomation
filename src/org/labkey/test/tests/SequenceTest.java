/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.External;
import org.labkey.test.categories.LabModule;
import org.labkey.test.categories.ONPRC;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LabModuleHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.labkey.test.util.ext4cmp.Ext4GridRef;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({External.class, ONPRC.class, LabModule.class})
public class SequenceTest extends BaseWebDriverTest
{
    protected LabModuleHelper _helper = new LabModuleHelper(this);
    protected String _pipelineRoot = null;
    protected final String _sequencePipelineLoc =  getLabKeyRoot() + "/externalModules/labModules/SequenceAnalysis/resources/sampleData";
    protected final String _illuminaPipelineLoc =  getLabKeyRoot() + "/sampledata/genotyping";
    protected final String _readsetPipelineName = "Import sequence data";

    private final String TEMPLATE_NAME = "SequenceTest Saved Template";
    private Integer _readsetCt = 0;
    private int _startedPipelineJobs = 0;
    private final String ILLUMINA_CSV = "SequenceImport.csv";

    @Override
    protected String getProjectName()
    {
        return "SequenceVerifyProject";// + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Test
    public void testSteps() throws Exception
    {
        setUpTest();

        importReadsetMetadata();
        createIlluminaSampleSheet();
        importIlluminaTest();
        readsetFeaturesTest();
        analysisPanelTest();
        readsetPanelTest();
        readsetImportTest();
    }

    protected void setUpTest()
    {
        _containerHelper.createProject(getProjectName(), "Sequence Analysis");
        goToProjectHome();
    }


    /**
     * This method is designed to import an initial set of readset records, which will be used for
     * illumina import
     */
    private void importReadsetMetadata()
    {
        //create readset records for illumina run
        goToProjectHome();
        waitForText("Create Readsets");
        waitAndClickAndWait(Locator.linkWithText("Create Readsets"));

        _helper.waitForField("Sample Id", WAIT_FOR_PAGE);
        _ext4Helper.clickTabContainingText("Import Spreadsheet");
        waitForText("Copy/Paste Data");

        setFormElementJS(Locator.name("text"), getIlluminaNames());

        waitAndClick(Ext4Helper.Locators.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Success"));
        _readsetCt += 14;
        assertTextPresent("Success!");
        clickButton("OK");

        log("verifying readset count correct");
        waitForText("Total Readsets Imported");
        waitForElement(LabModuleHelper.getNavPanelItem("Total Readsets Imported:", _readsetCt.toString()));
    }

    /**
     * This method is designed to exercise the illumina template UI and create a CSV import template.  This template
     * is later used by importIlluminaTest()
     */
    private void createIlluminaSampleSheet()
    {
        goToProjectHome();
        _helper.clickNavPanelItemAndWait("Total Readsets Imported:", 1);
        _helper.waitForDataRegion("query");

        //verify CSV file creation
        DataRegionTable dr = new DataRegionTable("query", this);
        dr.checkAllOnPage();
        _extHelper.clickMenuButton("More Actions", "Create Illumina Sample Sheet");

        waitForText("You have chosen to export " + _readsetCt + " samples");
        _helper.waitForField("Investigator Name");

        //this is used later to view the download
        String url = getCurrentRelativeURL();
        url += "&exportAsWebPage=1";
        beginAt(url);
        waitForText("You have chosen to export " + _readsetCt + " samples");
        _helper.waitForField("Investigator Name");

        Ext4FieldRef.getForLabel(this, "Reagent Cassette Id").setValue("FlowCell");

        String[][] fieldPairs = {
                {"Investigator Name", "Investigator"},
                {"Experiment Name", "Experiment"},
                {"Project Name", "Project"},
                {"Description", "Description"},
                {"Application", "FASTQ Only"},
                {"Sample Kit", "Nextera XT", "Assay"},
        };

        for (String[] a : fieldPairs)
        {
            Ext4FieldRef.getForLabel(this, a[0]).setValue(a[1]);
        }

        _ext4Helper.clickTabContainingText("Preview Header");
        waitForText("Edit Sheet");
        for (String[] a : fieldPairs)
        {
            String propName = a.length == 3 ? a[2] : a[0];
            assertEquals(a[1], Ext4FieldRef.getForLabel(this, propName).getValue());
        }

        clickButton("Edit Sheet", 0);
        waitForText("Done Editing");
        for (String[] a : fieldPairs)
        {
            String propName = a.length == 3 ? a[2] : a[0];
            assertTextPresent(propName + "," + a[1]);
        }

        //add new values
        String prop_name = "NewProperty";
        String prop_value = "NewValue";
        Ext4FieldRef textarea = _ext4Helper.queryOne("textarea[itemId='sourceField']", Ext4FieldRef.class);
        String newValue = prop_name + "," + prop_value;
        String val = (String)textarea.getValue();
        val += "\n" + newValue;
        textarea.setValue(val);
        clickButton("Done Editing", 0);

        //verify template has changed
        _ext4Helper.clickTabContainingText("General Info");
        assertEquals("Custom", Ext4FieldRef.getForLabel(this, "Application").getValue());

        //verify samples present
        _ext4Helper.clickTabContainingText("Preview Samples");
        waitForText("Sample_ID");

        int expectRows = (11 * (14 +  1));  //11 cols, 14 rows, plus header
        assertEquals(expectRows, getElementCount(Locator.xpath("//td[contains(@class, 'x4-table-layout-cell')]")));

        //NOTE: hitting download will display the text in the browser; however, this replaces newlines w/ spaces.  therefore we use selenium
        //to directly get the output
        Ext4CmpRef panel = _ext4Helper.queryOne("#illuminaPanel", Ext4CmpRef.class);
        String outputTable = panel.getEval("getTableOutput().join(\"<>\")").toString();
        outputTable = outputTable.replaceAll("<>", System.getProperty("line.separator"));

        //then we download anyway
        clickButton("Download For Instrument");

        //the browser converts line breaks to spaces.  this is a hack to get them back
        String text = _helper.getPageText();
        for (String[] a : fieldPairs)
        {
            String propName = a.length == 3 ? a[2] : a[0];
            String line = propName + "," + a[1];
            assertTextPresent(line);

            text.replaceAll(line, line + System.getProperty("line.separator"));
        }

        assertTextPresent(prop_name + "," + prop_value);

        File importTemplate = new File(_illuminaPipelineLoc, ILLUMINA_CSV);
        if (importTemplate.exists())
            importTemplate.delete();


        //NOTE: use the text generated directly using JS
        saveFile(importTemplate.getParentFile(), importTemplate.getName(), outputTable);
        beginAt("/project/" + getProjectName() + "/begin.view");
    }

    private String getIlluminaNames()
    {
        String[] barcodes5 = {"N701", "N702", "N703", "N704", "N705", "N706", "N701", "N702", "N701", "N702", "N703", "N704", "N705", "N706"};
        String[] barcodes3 = {"N502", "N502", "N502", "N502", "N502", "N502", "N503", "N503", "N501", "N501", "N501", "N501", "N501", "N501"};

        StringBuilder sb = new StringBuilder("Name\tPlatform\tBarcode5\tBarcode3\n");
        int i = 0;
        while (i < barcodes5.length)
        {
            sb.append("Illumina" + (i+1) + "\tILLUMINA\t" + barcodes5[i] + "\t" + barcodes3[i] + "\n");
            i++;
        }
        return sb.toString();
    }

    /**
     * This test will kick off a pipeline import using the illumina pipeline.  Verification of the result
     * is performed by readsetFeaturesTest()
     */
    private void importIlluminaTest()
    {
        setPipelineRoot(_illuminaPipelineLoc);
        initiatePipelineJob("Import Illumina data", ILLUMINA_CSV);

        setFormElement(Locator.name("protocolName"), "TestIlluminaRun" + _helper.getRandomInt());
        setFormElement(Locator.name("runDate"), "08/25/2011");
        setFormElement(Locator.name("fastqPrefix"), "Il");

        click(Ext4Helper.Locators.ext4Button("Import Data"));
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("OK"));

        waitAndClickAndWait(Locator.linkContainingText("All"));
        _startedPipelineJobs++;
        waitForElement(Locator.tagContainingText("span", "Data Pipeline"));
        waitForPipelineJobsToComplete(_startedPipelineJobs, "Import Illumina", false);
        assertTextPresent("COMPLETE");
    }

    /**
     * This method has several puposes.  It will verify that the records from illuminaImportTest() were
     * created properly.  It also exercises various features associated with the readset grid, including
     * the FASTQC report and downloading of results
     * @throws Exception
     */
    private void readsetFeaturesTest() throws IOException
    {
        //verify import and instrument run creation
        goToProjectHome();
        _helper.clickNavPanelItemAndWait("Total Readsets Imported:", 1);
        _helper.waitForDataRegion("query");

        DataRegionTable dr = new DataRegionTable("query", this);
        for (int i = 0; i < dr.getDataRowCount(); i++)
        {
            String rowId = dr.getDataAsText(i, "Readset Id");
            String file1 = dr.getDataAsText(i, "Input File");
            assertEquals("Incorrect or no filename associated with readset", "Illumina-R1-" + rowId + ".fastq.gz", file1);

            String file2 = dr.getDataAsText(i, "Input File2");
            assertEquals("Incorrect or no filename associated with readset", "Illumina-R2-" + rowId + ".fastq.gz", file2);

            String instrumentRun = dr.getDataAsText(i, "Instrument Run");
            assertTrue("Incorrect or no instrument run associated with readset", instrumentRun.startsWith("TestIlluminaRun"));
        }

        log("Verifying instrument run and details page");
        dr.clickLink(2, "Instrument Run");

        waitForText("Instrument Run Details");
        waitForText("Run Id"); //crude proxy for loading of the details panel
        waitForText("Readsets");
        DataRegionTable rs = _helper.getDrForQueryWebpart("Readsets");
        assertEquals("Incorrect readset count found", 14, rs.getDataRowCount());

        waitForText("Quality Metrics");
        DataRegionTable qm = _helper.getDrForQueryWebpart("Quality Metrics");
        assertEquals("Incorrect quality metric count found", 30, qm.getDataRowCount());
        String totalSequences = qm.getDataAsText(qm.getRow("File Id", "Illumina-R1-Control.fastq.gz"), "Metric Value");
        assertEquals("Incorrect value for total sequences", "9.0", totalSequences);

        log("Verifying readset details page");
        goToProjectHome();
        _helper.clickNavPanelItemAndWait("Total Readsets Imported:", 1);
        _helper.waitForDataRegion("query");
        dr = new DataRegionTable("query", this);
        dr.clickLink(1, 1);

        waitForText("Readset Details");
        waitForText("Readset Id:"); //crude proxy for details panel

        waitForText("Analyses Using This Readset");
        DataRegionTable dr1 = _helper.getDrForQueryWebpart("Analyses Using This Readset");
        assertEquals("Incorrect analysis count", 0, dr1.getDataRowCount());

        waitForText("Quality Metrics");
        DataRegionTable dr2 = _helper.getDrForQueryWebpart("Quality Metrics");
        assertEquals("Incorrect analysis count", 2, dr2.getDataRowCount());

        //verify export
        log("Verifying FASTQ Export");
        goToProjectHome();
        _helper.clickNavPanelItemAndWait("Total Readsets Imported:", 1);
        _helper.waitForDataRegion("query");
        dr = new DataRegionTable("query", this);
        dr.checkAllOnPage();
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_query']" + Locator.navButton("More Actions").getPath()), "Download Sequence Files");
        waitForElement(Ext4Helper.ext4Window("Export Files"));
        waitForText("Export Files As");
        Ext4CmpRef window = _ext4Helper.queryOne("#exportFilesWin", Ext4CmpRef.class);
        String fileName = "MyFile";
        Ext4FieldRef.getForLabel(this, "File Prefix").setValue(fileName);
        String url = window.getEval("getURL()").toString();
        assertTrue("Improper URL to download sequences", url.contains("zipFileName=" + fileName));
        assertTrue("Improper URL to download sequences", url.contains("exportFiles.view?"));
        assertEquals("Wrong number of files selected", 28, StringUtils.countMatches(url, "dataIds="));

        _ext4Helper.queryOne("field[boxLabel='Forward Reads']", Ext4FieldRef.class).setValue("false");
        _ext4Helper.queryOne("field[boxLabel='Merge into Single FASTQ File']", Ext4FieldRef.class).setChecked(true);

        url = window.getEval("getURL()").toString();
        assertEquals("Wrong number of files selected", 14, StringUtils.countMatches(url, "dataIds="));
        assertTrue("Improper URL to download sequences", url.contains("mergeFastqFiles.view?"));

        waitAndClick(Ext4Helper.Locators.ext4Button("Submit"));
        validateFastqDownload(fileName + ".fastq.gz");

        log("Verifying FASTQC Report");
        dr.uncheckAllOnPage();
        dr.checkCheckbox(2);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_query']" + Locator.navButton("More Actions").getPath()), "View FASTQC Report");

        waitForText("File Summary");
        assertTextPresent("Per base sequence quality");

        log("Verifying View Analyses");
        goToProjectHome();
        _helper.clickNavPanelItemAndWait("Total Readsets Imported:", 1);

        waitForText("Instrument Run"); //proxy for dataRegion loading
        dr = new DataRegionTable("query", this);
        dr.uncheckAllOnPage();

        //NOTE: this is going to be sensitive to the ordering of params by the DataRegion.  May need a more robust
        //approach if that is variable.
        dr.checkCheckbox(1);
        String id1 = dr.getDataAsText(1, "Readset Id");
        dr.checkCheckbox(2);
        String id2 = dr.getDataAsText(2, "Readset Id");

        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_query']" + Locator.navButton("More Actions").getPath()), "View Analyses");

        waitForText("Analysis Type"); //proxy for dataRegion loading

        assertTextPresent("readset IS ONE OF (");
        assertTrue("Filter not applied correct", (isTextPresent(id1 + ", " + id2) || isTextPresent(id2 + ", " + id1)));

        log("Verifying Readset Edit");
        goToProjectHome();
        _helper.clickNavPanelItemAndWait("Total Readsets Imported:", 1);

        waitForText("Instrument Run"); //proxy for dataRegion loading
        dr.clickLink(1, 0);

        waitForText("3-Barcode:");
        waitForText("Run:");
        String newName = "ChangedSample";
        Ext4FieldRef.getForLabel(this, "Name").setValue(newName);
        sleep(250); //wait for value to save
        clickButton("Submit", 0);
        waitForElement(Ext4Helper.ext4Window("Success"));
        assertTextPresent("Your upload was successful!");
        clickButton("OK");

        _helper.waitForDataRegion("query");
        assertEquals("Changed sample name not applied", newName, dr.getDataAsText(1, "Name"));

        //note: 'Analyze Selected' option is verified separately
    }

    /**
     * The intent of this method is to test the Sequence Analysis Panel,
     * with the goal of exercising all UI options.  It directly calls getJsonParams() on the panel,
     * in order to inspect the JSON it would send to the server, but does not initiate a pipeline job.
     */
    private void analysisPanelTest()
    {
        log("Verifying Analysis Panel UI");

        goToProjectHome();
        _helper.clickNavPanelItemAndWait("Total Readsets Imported:", 1);
        _helper.waitForDataRegion("query");
        DataRegionTable dr = new DataRegionTable("query", this);
        dr.uncheckAllOnPage();
        dr.checkCheckbox(2);
        List<String> rowIds = new ArrayList<>();
        rowIds.add(dr.getDataAsText(2, "Readset Id"));
        dr.checkCheckbox(6);
        rowIds.add(dr.getDataAsText(6, "Readset Id"));

        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_query']" + Locator.navButton("More Actions").getPath()), "Analyze Selected");
        waitForElement(Ext4Helper.ext4Window("Import Data"));
        waitForText("Description");
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Submit"));

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
        String analysisDescription = "This is the description for my analysis";
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
        Ext4FieldRef.getForLabel(this, "Description").setValue(analysisDescription);

        log("Verifying Pre-processing section");
        WebElement el = getDriver().findElement(By.id(Ext4FieldRef.getForLabel(this, "Total Reads").getId()));
        assertFalse("Reads field should be hidden", el.isDisplayed());
        Ext4FieldRef.getForLabel(this, "Downsample Reads").setChecked(true);
        el = getDriver().findElement(By.id(Ext4FieldRef.getForLabel(this, "Total Reads").getId()));
        assertTrue("Reads field should be visible", el.isDisplayed());

        Ext4FieldRef.getForLabel(this, "Total Reads").setValue(totalReads);

        Ext4FieldRef.getForLabel(this, "Minimum Read Length").setValue(minReadLength);
        Ext4FieldRef.getForLabel(this, "Adapter Trimming").setChecked(true);
        waitForText("Adapters");
        clickButton("Common Adapters", 0);
        waitForElement(Ext4Helper.ext4Window("Choose Adapters"));
        waitForText("Choose Adapter Group");
        Ext4FieldRef.getForLabel(this, "Choose Adapter Group").setValue("Roche-454 FLX Amplicon");
        waitAndClick(Ext4Helper.Locators.ext4Button("Submit"));

        waitForText(rocheAdapters[0][0]);
        waitForText(rocheAdapters[1][0]);
        assertTextBefore(rocheAdapters[0][0], rocheAdapters[1][0]);
        assertTextPresent(rocheAdapters[0][1]);
        assertTextPresent(rocheAdapters[1][1]);

        _ext4Helper.queryOne("#adapterGrid", Ext4CmpRef.class).eval("getSelectionModel().select(0)");
        clickButton("Move Down", 0);
        sleep(500);
        assertTextBefore(rocheAdapters[1][0], rocheAdapters[0][0]);

        _ext4Helper.queryOne("#adapterGrid", Ext4CmpRef.class).eval("getSelectionModel().select(1)");
        clickButton("Move Up", 0);
        sleep(500);
        assertTextBefore(rocheAdapters[0][0], rocheAdapters[1][0]);

        clickButton("Remove", 0);
        sleep(500);
        assertTextNotPresent(rocheAdapters[0][0]);

        Ext4FieldRef.getForLabel(this, "Seed Mismatches").setValue(seedMismatches);
        Ext4FieldRef.getForLabel(this, "Simple Clip Threshold").setValue(simpleClipThreshold);

        el = getDriver().findElement(By.id(Ext4FieldRef.getForLabel(this, "Window Size").getId()));
        assertFalse("Window Size field should be hidden", el.isDisplayed());
        Ext4FieldRef.getForLabel(this, "Quality Trimming (by sliding window)").setChecked(true);

        el = getDriver().findElement(By.id(Ext4FieldRef.getForLabel(this, "Window Size").getId()));
        assertTrue("Window Size field should be visible", el.isDisplayed());

        Ext4FieldRef.getForLabel(this, "Window Size").setValue(qualWindowSize);
        Ext4FieldRef.getForLabel(this, "Avg Qual").setValue(qualAvgQual);

        el = getDriver().findElement(By.id(Ext4FieldRef.getForLabel(this, "Min Qual").getId()));
        assertFalse("Min Qual field should be hidden", el.isDisplayed());

        log("Testing Alignment Section");

        log("Testing whether sections are disabled when alignment unchecked");
        Ext4FieldRef.getForLabel(this, "Perform Alignment").setChecked(false);
        assertEquals("Field should be hidden", false, Ext4FieldRef.isFieldPresent(this, "Reference Library Type"));
        assertEquals("Field should be hidden", false, Ext4FieldRef.isFieldPresent(this, "Aligner"));

        assertEquals("Field should be disabled", true, Ext4FieldRef.getForLabel(this, "Sequence Based Genotyping").getEval("isDisabled()"));
        assertEquals("Field should be disabled", true, Ext4FieldRef.getForLabel(this, "Min SNP Quality").getEval("isDisabled()"));

        Ext4FieldRef.getForLabel(this, "Perform Alignment").setChecked(true);
        waitForText("Reference Library Type");
        sleep(500);

        log("Testing aligner field and descriptions");
        Ext4FieldRef alignerField = Ext4FieldRef.getForLabel(this, "Aligner");

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

        String aligner = "bwasw";
        alignerField.setValue(aligner);
        alignerField.eval("fireEvent(\"select\", Ext4.getCmp('" + alignerField.getId() + "'), '" + aligner + "')");
        waitForText("BWA-SW uses a different algorithm than BWA");

        final BaseWebDriverTest test = this;
        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                return !(Boolean) Ext4FieldRef.getForLabel(test, "Virus Strain").getEval("store.isLoading()");
            }
        }, "Combo store did not load", WAIT_FOR_JAVASCRIPT);

        sleep(100);
        Ext4FieldRef.getForLabel(this, "Virus Strain").setValue(strain);

        Ext4FieldRef.getForLabel(this, "Min SNP Quality").setValue(minSnpQual);
        Ext4FieldRef.getForLabel(this, "Min Avg SNP Quality").setValue(minAvgSnpQual);
        Ext4FieldRef.getForLabel(this, "Min DIP Quality").setValue(minDipQual);
        Ext4FieldRef.getForLabel(this, "Min Avg DIP Quality").setValue(minAvgDipQual);

        Ext4FieldRef.getForLabel(this, "Sequence Based Genotyping").setChecked(true);

//        Ext4FieldRef.getForLabel(this, "Max Alignment Mismatches").setValue(maxAlignMismatch);
//        Ext4FieldRef.getForLabel(this, "Assemble Unaligned Reads").setChecked(true);
//        Ext4FieldRef.getForLabel(this, "Assembly Percent Identity").setValue(assembleUnalignedPct);
//        Ext4FieldRef.getForLabel(this, "Min Sequences Per Contig").setValue(minContigsForNovel);

        Ext4CmpRef panel = _ext4Helper.queryOne("#sequenceAnalysisPanel", Ext4CmpRef.class);
        Map<String, Object> params = (Map)panel.getEval("getJsonParams()");

        String container = (String)executeScript("return LABKEY.Security.currentContainer.id");
        assertEquals("Incorect param in form JSON", container, params.get("containerId"));
        assertEquals("Incorect param in form JSON", getBaseURL() + "/", params.get("baseUrl"));

        Long id1 = (Long)executeScript("return LABKEY.Security.currentUser.id");
        Long id2 = (Long)params.get("userId");
        assertEquals("Incorect param in form JSON", id1, id2);
        String containerPath = getURL().getPath().replaceAll("/sequenceAnalysis.view", "").replaceAll("(.)*/sequenceanalysis", "");
        assertEquals("Incorect param in form JSON", containerPath, params.get("containerPath"));

        assertEquals("Incorect param in form JSON", true, params.get("deleteIntermediateFiles"));
        assertEquals("Incorect param in form JSON", analysisDescription, params.get("analysisDescription"));
        assertEquals("Incorect param in form JSON", jobName, params.get("protocolName"));

        assertEquals("Incorect param in form JSON", false, params.get("saveProtocol"));

        assertEquals("Incorect param in form JSON", true, params.get("preprocessing.downsample"));
        assertEquals("Incorect param in form JSON", totalReads, params.get("preprocessing.downsampleReadNumber").toString());

        assertEquals("Incorect param in form JSON", minReadLength, params.get("preprocessing.minLength").toString());

        assertEquals("Incorect param in form JSON", true, params.get("preprocessing.trimAdapters"));

        assertEquals("Incorect param in form JSON", seedMismatches, params.get("preprocessing.seedMismatches").toString());
        assertEquals("Incorect param in form JSON", simpleClipThreshold, params.get("preprocessing.simpleClipThreshold").toString());

        List<Object> adapter = (List)params.get("adapter_0");
        assertEquals("Incorect param in form JSON", rocheAdapters[1][0], adapter.get(0).toString());
        assertEquals("Incorect param in form JSON", rocheAdapters[1][1], adapter.get(1).toString());
        assertEquals("Incorect param in form JSON", true, adapter.get(2));
        assertEquals("Incorect param in form JSON", true, adapter.get(3));

        assertEquals("Incorect param in form JSON", true, params.get("preprocessing.qual2"));
        assertEquals("Incorect param in form JSON", qualAvgQual, params.get("preprocessing.qual2_avgQual").toString());
        assertEquals("Incorect param in form JSON", qualWindowSize, params.get("preprocessing.qual2_windowSize").toString());

//        assertEquals("Incorect param in form JSON", "true", params.get("preprocessing.mask"));
//        assertEquals("Incorect param in form JSON", maskMinQual, params.get("preprocessing.mask_minQual"));

//        assertEquals("Incorect param in form JSON", "true", params.get("useCustomReference"));
//        assertEquals("Incorect param in form JSON", customRefSeq, params.get("virus.refSequence"));
//        assertEquals("Incorect param in form JSON", customRefName, params.get("virus.custom_strain_name"));

        assertEquals("Incorect param in form JSON", strain, params.get("dbprefix"));
        assertEquals("Incorect param in form JSON", "Virus", params.get("dna.category"));

        assertEquals("Incorect param in form JSON", "Virus", params.get("analysisType"));
        assertEquals("Incorect param in form JSON", strain, params.get("dna.subset"));

        assertEquals("Incorect param in form JSON", true, params.get("doAlignment"));
        assertEquals("Incorect param in form JSON", aligner, params.get("aligner"));

        assertEquals("Incorect param in form JSON", false, params.get("snp.neighborhoodQual"));
        assertEquals("Incorect param in form JSON", minSnpQual, params.get("snp.minQual").toString());
        assertEquals("Incorect param in form JSON", minAvgSnpQual, params.get("snp.minAvgSnpQual").toString());
        assertEquals("Incorect param in form JSON", minDipQual, params.get("snp.minIndelQual").toString());
        assertEquals("Incorect param in form JSON", minAvgDipQual, params.get("snp.minAvgDipQual").toString());

        //assertEquals("Incorect param in form JSON", "true", params.get("assembleUnaligned"));
        //assertEquals("Incorect param in form JSON", maxAlignMismatch, params.get("maxAlignMismatch"));
        //assertEquals("Incorect param in form JSON", assembleUnalignedPct, params.get("assembleUnalignedPct"));
        //assertEquals("Incorect param in form JSON", minContigsForNovel, params.get("minContigsForNovel"));

        assertEquals("Incorect param in form JSON", false, params.get("debugMode"));

        Map sample = (Map)params.get("sample_0");
        Long readset = (Long)sample.get("readset");
        assertEquals("Incorect param in form JSON", "Illumina-R1-" + readset + ".fastq.gz", sample.get("fileName"));
        assertEquals("Incorect param in form JSON", "Illumina-R2-" + readset + ".fastq.gz", sample.get("fileName2"));
        assertEquals("Incorect param in form JSON", "ILLUMINA", sample.get("platform"));
        assertFalse("Incorect param in form JSON", null == sample.get("instrument_run_id"));
        assertFalse("Incorect param in form JSON", null == sample.get("readsetname"));
        assertFalse("Incorect param in form JSON", null == sample.get("fileId"));
        assertFalse("Incorect param in form JSON", null == sample.get("fileId2"));

        sample = (Map)params.get("sample_1");
        readset = (Long)sample.get("readset");
        assertEquals("Incorect param in form JSON", "Illumina-R1-" + readset + ".fastq.gz", sample.get("fileName"));
        assertEquals("Incorect param in form JSON", "Illumina-R2-" + readset + ".fastq.gz", sample.get("fileName2"));
        assertEquals("Incorect param in form JSON", "ILLUMINA", sample.get("platform"));
        assertFalse("Incorect param in form JSON", null == sample.get("instrument_run_id"));
        assertFalse("Incorect param in form JSON", null == sample.get("readsetname"));
        assertFalse("Incorect param in form JSON", null == sample.get("fileId"));
        assertFalse("Incorect param in form JSON", null == sample.get("fileId2"));

        assertEquals("Incorect param in form JSON", true, params.get("sbtAnalysis"));
        assertEquals("Incorect param in form JSON", true, params.get("aaSnpByCodon"));
        assertEquals("Incorect param in form JSON", true, params.get("ntSnpByPosition"));
        assertEquals("Incorect param in form JSON", true, params.get("ntCoverage"));

        log("Testing UI changes when selecting different analysis settings");

        String url = getURL().toString();
        url += "&debugMode=1";
        beginAt(url);
        waitForText("Specialized Analysis");

        Ext4FieldRef analysisField = Ext4FieldRef.getForLabel(this, "Specialized Analysis");
        analysisField.setValue("SBT");

        assertEquals("Incorrect field value", "DNA", Ext4FieldRef.getForLabel(this, "Reference Library Type").getValue());
        String species = "Human";
        String molType = "gDNA";

        String jobName2 = "Job2";
        Ext4FieldRef.getForLabel(this, "Job Name").setValue(jobName2);
        waitForElementToDisappear(Ext4Helper.invalidField(), WAIT_FOR_JAVASCRIPT);

        Ext4FieldRef.getForLabel(this, "Species").setValue(species);
        Ext4FieldRef.getForLabel(this, "Subset").eval("setValue([\"KIR\",\"MHC\"])");
        Ext4FieldRef.getForLabel(this, "Molecule Type").setValue(molType);
        Ext4FieldRef.getForLabel(this, "Loci").eval("setValue([\"KIR1D\",\"HLA-A\"])");

        Ext4FieldRef.getForLabel(this, "Calculate and Save NT SNPs").setValue("false");
        Ext4FieldRef.getForLabel(this, "Calculate and Save Coverage Depth").setValue("false");
        Ext4FieldRef.getForLabel(this, "Calculate and Save AA SNPs").setValue("false");

        params = (Map)panel.getEval("getJsonParams()");

        assertEquals("Incorect param in form JSON", true, params.get("debugMode"));
        assertEquals("Incorect param in form JSON", species, params.get("dna.species"));
        assertEquals("Incorect param in form JSON", "KIR;MHC", params.get("dna.subset"));
        assertEquals("Incorect param in form JSON", molType, params.get("dna.mol_type"));
        assertEquals("Incorect param in form JSON", "KIR1D;HLA-A", params.get("dna.locus"));

        //also test mosaik params
        assertEquals("Incorect param in form JSON", "mosaik", params.get("aligner"));
        assertEquals("Incorect param in form JSON", 0.02, params.get("mosaik.max_mismatch_pct"));
        assertEquals("Incorect param in form JSON", new Long(200), params.get("mosaik.max_hash_positions"));

        assertEquals("Incorect param in form JSON", true, params.get("sbtAnalysis"));
        assertEquals("Incorect param in form JSON", false, params.get("aaSnpByCodon"));
        assertEquals("Incorect param in form JSON", false, params.get("ntCoverage"));
        assertEquals("Incorect param in form JSON", false, params.get("ntSnpByPosition"));
        assertEquals("Incorect param in form JSON", 4, StringUtils.split((String)params.get("fileNames"), ";").length);
    }

    /**
     * This method will make a request to download merged FASTQ files created during the illumina test
     * @param filename
     * @throws Exception
     */
    private void validateFastqDownload(String filename) throws IOException
    {
        log("Verifying merged FASTQ export");

        File output = new File(getDownloadDir(), filename);
        _helper.waitForFileOfSize(output, 15000);  //size measured at 15924

        assertTrue("Unable to find file: " + output.getPath(), output.exists());
        log("File size: " + FileUtils.sizeOf(output));

        try (
                InputStream fileStream = new FileInputStream(output);
                InputStream gzipStream = new GZIPInputStream(fileStream);
                Reader decoder = new InputStreamReader(gzipStream);
                BufferedReader br = new BufferedReader(decoder);
        )
        {
            int count = 0;
            int totalChars = 0;
            String thisLine;
            List<String> lines = new ArrayList<>();
            while ((thisLine = br.readLine()) != null)
            {
                count++;
                totalChars+= thisLine.length();
                lines.add(thisLine);
            }

            int expectedLength = 504;
            assertEquals("Length of file doesnt match expected value.  Total characters: " + totalChars, expectedLength, count);

            output.delete();
        }
    }

    protected void initiatePipelineJob(String importAction, String... files)
    {
        goToProjectHome();
        waitForText("Upload Files");
        waitAndClickAndWait(Locator.linkContainingText("Upload Files / Start Analysis"));

        waitForText("fileset");
        _fileBrowserHelper.expandFileBrowserRootNode();
        for (String f : files)
        {
            _fileBrowserHelper.clickFileBrowserFileCheckbox(f);
        }

        _fileBrowserHelper.selectImportDataAction(importAction);

    }

    /**
     * The intent of this method is to perform additional tests of this Readset Import Panel,
     * with the goal of exercising all UI options
     */
    private void readsetPanelTest()
    {
        log("Verifying Readset Import Panel UI");

        goToProjectHome();
        setPipelineRoot(_sequencePipelineLoc);

        String filename1 = "sample454_SIV.sff";
        String filename2 = "dualBarcodes_SIV.fastq.gz";
        initiatePipelineJob(_readsetPipelineName, filename1, filename2);
        waitForText("Job Name");

        Ext4FieldRef.getForLabel(this, "Job Name").setValue("SequenceTest_" + System.currentTimeMillis());

        waitForElement(Locator.linkContainingText(filename1));
        waitForElement(Locator.linkContainingText(filename2));

        Ext4FieldRef barcodeField = Ext4FieldRef.getForLabel(this, "Use Barcodes");
        Ext4FieldRef treatmentField = Ext4FieldRef.getForLabel(this, "Treatment of Input Files");
        Ext4FieldRef mergeField = Ext4FieldRef.getForLabel(this, "Merge Files");
        Ext4FieldRef pairedField = Ext4FieldRef.getForLabel(this, "Data Is Paired End");
        Ext4GridRef grid = getSampleGrid();

        assertEquals("Incorrect starting value for input file-handling field", "delete", treatmentField.getValue());

        barcodeField.setChecked(true);
        sleep(100);
        assertEquals("Incorrect value for input file-handling field after barcode toggle", "compress", treatmentField.getValue());
        assertFalse("MID5 column should not be hidden", (Boolean)grid.getEval("columns[2].hidden"));
        assertFalse("MID3 column should not be hidden", (Boolean)grid.getEval("columns[3].hidden"));

        barcodeField.setChecked(false);
        sleep(100);
        assertEquals("Incorrect value for input file-handling field after barcode toggle", "delete", treatmentField.getValue());
        assertTrue("MID5 column should be hidden", (Boolean) grid.getEval("columns[2].hidden"));
        assertTrue("MID3 column should be hidden", (Boolean) grid.getEval("columns[3].hidden"));

        mergeField.setChecked(true);
        sleep(100);
        assertEquals("Incorrect value for input file-handling field after merge toggle", "compress", treatmentField.getValue());
        assertTrue("Paired end field should be disabled when merge is checked", pairedField.isDisabled());

        Ext4FieldRef mergenameField = Ext4FieldRef.getForLabel(this, "Name For Merged File");

        assertTrue("Merge name field should be visible", mergenameField.isVisible());
        assertEquals("Merged file name not set in grid correctly", "MergedFile", grid.getFieldValue(1, "fileName"));
        mergenameField.setValue("MergeFile2");
        sleep(100);
        assertEquals("Merged file name not set in grid correctly", "MergeFile2", grid.getFieldValue(1, "fileName"));

        mergeField.setChecked(false);
        sleep(100);
        assertEquals("Incorrect value for input file-handling field after merge toggle", "delete", treatmentField.getValue());
        assertFalse("Merge name field should be hidden", mergenameField.isVisible());
        assertFalse("Paired end field should be enable when merge is unchecked", pairedField.isDisabled());

        pairedField.setChecked(true);
        assertFalse("Paired file column should not be hidden", (Boolean) grid.getEval("columns[1].hidden"));

        pairedField.setChecked(false);
        assertTrue("Paired file column should be hidden", (Boolean) grid.getEval("columns[1].hidden"));

        //now set real values
        click(Ext4Helper.Locators.ext4Button("Add"));
        waitForElement(Ext4GridRef.locateExt4GridRow(2, grid.getId()));

        //the first field is pre-selected
        grid.cancelEdit();

        grid.setGridCell(1, "fileName", filename1);
        grid.setGridCell(2, "fileName", filename1);
        waitAndClick(Ext4Helper.Locators.ext4Button("Import Data"));
        waitForElement(Ext4Helper.ext4Window("Error"));
        assertTextPresent("For each file, you must provide either the Id of an existing, unused readset or a name/platform to create a new one");
        click(Ext4Helper.Locators.ext4Button("OK"));

        grid.setGridCell(1, "readsetname", "Readset1");
        grid.setGridCellJS(1, "platform", "ILLUMINA");
        grid.setGridCell(1, "inputmaterial", "InputMaterial");
        grid.setGridCell(1, "subjectid", "Subject1");

        grid.setGridCell(2, "readsetname", "Readset2");
        grid.setGridCellJS(2, "platform", "LS454");
        grid.setGridCell(2, "inputmaterial", "InputMaterial2");
        grid.setGridCell(2, "subjectid", "Subject2");
        grid.setGridCell(2, "sampledate", "2010-10-20");

        waitAndClick(Ext4Helper.Locators.ext4Button("Import Data"));
        waitForElement(Ext4Helper.ext4Window("Error"));
        waitForElement(Locator.tagContainingText("div", "Duplicate Sample: " + filename1 + ". Please remove or edit rows"));
        waitAndClick(Ext4Helper.Locators.ext4Button("OK"));

        //verify paired end
        pairedField.setChecked(true);
        waitAndClick(Ext4Helper.Locators.ext4Button("Import Data"));
        waitForElement(Ext4Helper.ext4Window("Error"));
        assertTextPresent("Either choose a file or unchecked paired-end.");
        waitAndClick(Ext4Helper.Locators.ext4Button("OK"));
        pairedField.setChecked(false);

        //try duplicate barcodes
        barcodeField.setChecked(true);
        String barcode = "FLD0376";
        grid.setGridCellJS(1, "mid5", barcode);
        grid.setGridCellJS(2, "mid3", barcode);
        waitAndClick(Ext4Helper.Locators.ext4Button("Import Data"));
        waitForElement(Ext4Helper.ext4Window("Error"));
        assertTextPresent("All samples must either use no barcodes, 5' only, 3' only or both ends");
        click(Ext4Helper.Locators.ext4Button("OK"));
        barcodeField.setChecked(false);
        grid.setGridCell(2, "fileName", filename2);

        Ext4CmpRef panel = _ext4Helper.queryOne("#sequenceAnalysisPanel", Ext4CmpRef.class);
        Map<String, Object> params = (Map)panel.getEval("getJsonParams()");
        Map<String, Object> fieldsJson = (Map)params.get("json");
        //List<Long> fileIds = (List)params.get("distinctIds");
        //List<String> fieldsNames = (List)params.get("distinctNames");

        String container = (String)executeScript("return LABKEY.Security.currentContainer.id");
        assertEquals("Incorect param for containerId", container, fieldsJson.get("containerId"));
        assertEquals("Incorect param for baseURL", getBaseURL() + "/", fieldsJson.get("baseUrl"));

        Long id1 = (Long)executeScript("return LABKEY.Security.currentUser.id");
        Long id2 = (Long)fieldsJson.get("userId");
        assertEquals("Incorect param for userId", id1, id2);
        String containerPath = getURL().getPath().replaceAll("/importReadset.view(.)*", "").replaceAll("(.)*/sequenceanalysis", "");
        assertEquals("Incorect param for containerPath", containerPath, fieldsJson.get("containerPath"));

        assertEquals("Unexpected value for param", false, fieldsJson.get("inputfile.pairedend"));
        assertEquals("Unexpected value for param", filename1 + ";" + filename2, fieldsJson.get("fileNames"));
        assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) fieldsJson.get("inputfile.barcodeGroups")));
        assertEquals("Unexpected value for param", false, fieldsJson.get("inputfile.merge"));
        assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) fieldsJson.get("inputfile.merge.basename")));
        assertEquals("Unexpected value for param", new Long(0), fieldsJson.get("inputfile.barcodeEditDistance"));
        assertEquals("Unexpected value for param", new Long(0), fieldsJson.get("inputfile.barcodeOffset"));
        assertEquals("Unexpected value for param", new Long(0), fieldsJson.get("inputfile.barcodeDeletions"));
        assertEquals("Unexpected value for param", false, fieldsJson.get("inputfile.barcode"));
        assertEquals("Unexpected value for param", "delete", fieldsJson.get("inputfile.inputTreatment"));
        assertEquals("Unexpected value for param", true, fieldsJson.get("deleteIntermediateFiles"));

        Map<String, Object> sample0 = (Map)fieldsJson.get("sample_0");
        Map<String, Object> sample1 = (Map)fieldsJson.get("sample_1");

        assertEquals("Unexpected value for param", filename1, sample0.get("fileName"));
        assertEquals("Unexpected value for param", "Readset1", sample0.get("readsetname"));
        assertEquals("Unexpected value for param", "Subject1", sample0.get("subjectid"));
        assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) sample0.get("readset")));
        assertEquals("Unexpected value for param", "ILLUMINA", sample0.get("platform"));
        assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) sample0.get("fileId")));
        assertEquals("Unexpected value for param", "InputMaterial", sample0.get("inputmaterial"));
        assertFalse("param shold not be present", sample0.containsKey("mid5"));
        assertFalse("param shold not be present", sample0.containsKey("mid3"));

        assertEquals("Unexpected value for param", filename2, sample1.get("fileName"));
        assertEquals("Unexpected value for param", "Readset2", sample1.get("readsetname"));
        assertEquals("Unexpected value for param", "Subject2", sample1.get("subjectid"));
        assertEquals("Unexpected value for param", "2010-10-20T07:00:00.000Z", sample1.get("sampledate"));
        assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) sample1.get("readset")));
        assertEquals("Unexpected value for param", "LS454", sample1.get("platform"));
        assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) sample1.get("fileId")));
        assertEquals("Unexpected value for param", "InputMaterial2", sample1.get("inputmaterial"));
        assertFalse("param shold not be present", sample1.containsKey("mid5"));
        assertFalse("param shold not be present", sample1.containsKey("mid3"));

        barcodeField.setValue(true);
        Ext4FieldRef.getForLabel(this, "Additional Barcodes").setValue("GSMIDs");
        Ext4FieldRef.getForLabel(this, "Mismatches Tolerated").setValue(9);
        Ext4FieldRef.getForLabel(this, "Deletions Tolerated").setValue(9);
        Ext4FieldRef.getForLabel(this, "Allowed Distance From Read End").setValue(9);

        Ext4FieldRef.getForLabel(this, "Delete Intermediate Files").setValue(false);
        treatmentField.setValue("compress");

        mergeField.setValue(true);
        String mergedName = "MergedFile99";
        mergenameField.setValue(mergedName);

        params = (Map)panel.getEval("getJsonParams()");
        fieldsJson = (Map)params.get("json");

        assertEquals("Unexpected value for param", true, fieldsJson.get("inputfile.barcode"));
        assertEquals("Unexpected value for param", Collections.singletonList("GSMIDs"), fieldsJson.get("inputfile.barcodeGroups"));
        assertEquals("Unexpected value for param", new Long(9), fieldsJson.get("inputfile.barcodeEditDistance"));
        assertEquals("Unexpected value for param", new Long(9), fieldsJson.get("inputfile.barcodeOffset"));
        assertEquals("Unexpected value for param", new Long(9), fieldsJson.get("inputfile.barcodeDeletions"));

        assertEquals("Unexpected value for param", true, fieldsJson.get("inputfile.merge"));
        assertEquals("Unexpected value for param", mergedName, fieldsJson.get("inputfile.merge.basename"));

        assertEquals("Unexpected value for param", "compress", fieldsJson.get("inputfile.inputTreatment"));
        assertEquals("Unexpected value for param", false, fieldsJson.get("deleteIntermediateFiles"));

        sample0 = (Map)fieldsJson.get("sample_0");
        assertEquals("Unexpected value for param", mergedName, sample0.get("fileName"));
        assertEquals("Unexpected value for param", "Readset1", sample0.get("readsetname"));
        assertEquals("Unexpected value for param", "Subject1", sample0.get("subjectid"));
        assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) sample0.get("readset")));
        assertEquals("Unexpected value for param", "ILLUMINA", sample0.get("platform"));
        assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) sample0.get("fileId")));
        assertEquals("Unexpected value for param", "InputMaterial", sample0.get("inputmaterial"));
        assertFalse("param shold not be present", sample0.containsKey("mid5"));
        assertFalse("param shold not be present", sample0.containsKey("mid3"));

        mergeField.setValue(false);
        barcodeField.setValue(false);

        pairedField.setValue(true);
        grid.setGridCell(1, "fileName", filename1);
        grid.setGridCellJS(1, "fileName2", filename2);
        params = (Map)panel.getEval("getJsonParams()");
        fieldsJson = (Map)params.get("json");
        sample0 = (Map)fieldsJson.get("sample_0");
        assertEquals("Unexpected value for param", filename1, sample0.get("fileName"));
        assertEquals("Unexpected value for param", filename2, sample0.get("fileName2"));

        pairedField.setValue(false);
        barcodeField.setValue(true);
        waitAndClick(Ext4Helper.Locators.ext4Button("Add"));
        waitForElement(Ext4GridRef.locateExt4GridRow(2, grid.getId()));
        getDriver().switchTo().activeElement().sendKeys(Keys.ESCAPE);
        sleep(100);

        grid.setGridCell(1, "fileName", filename1);
        String readsetNew = "ReadsetNew";
        grid.setGridCell(1, "readsetname", readsetNew);
        grid.setGridCellJS(1, "platform", "SANGER");
        grid.setGridCell(2, "fileName", filename2);
        String barcode2 = "FLD0374";
        grid.setGridCellJS(1, "mid5", barcode);
        grid.setGridCellJS(1, "mid3", barcode2);
        grid.setGridCellJS(2, "mid3", barcode);
        grid.setGridCellJS(2, "mid5", barcode2);
        params = (Map)panel.getEval("getJsonParams()");
        fieldsJson = (Map)params.get("json");
        sample0 = (Map)fieldsJson.get("sample_0");
        assertEquals("Unexpected value for param", filename1, sample0.get("fileName"));
        assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) sample0.get("fileName2")));
        assertEquals("Unexpected value for param", barcode, sample0.get("mid5"));
        assertEquals("Unexpected value for param", barcode2, sample0.get("mid3"));
        assertEquals("Unexpected value for param", "SANGER", sample0.get("platform"));
        assertEquals("Unexpected value for param", readsetNew, sample0.get("readsetname"));

        sample1 = (Map)fieldsJson.get("sample_1");
        assertEquals("Unexpected value for param", filename2, sample1.get("fileName"));
        assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String)sample1.get("fileName2")));
        assertEquals("Unexpected value for param", barcode2, sample1.get("mid5"));
        assertEquals("Unexpected value for param", barcode, sample1.get("mid3"));
    }

    private void readsetImportTest()
    {
        log("Verifying Readset Import");

        goToProjectHome();
        setPipelineRoot(_sequencePipelineLoc);

        String filename1 = "paired1.fastq.gz";
        String filename2 = "dualBarcodes_SIV.fastq.gz";
        initiatePipelineJob(_readsetPipelineName, filename1, filename2);
        waitForText("Job Name");

        Ext4FieldRef.getForLabel(this, "Job Name").setValue("SequenceTest_" + System.currentTimeMillis());

        waitForElement(Locator.linkContainingText(filename1));
        waitForElement(Locator.linkContainingText(filename2));

        Ext4FieldRef.getForLabel(this, "Treatment of Input Files").setValue("none");
        Ext4GridRef grid = getSampleGrid();

        String readset1 = "ReadsetTest1";
        String readset2 = "ReadsetTest2";

        grid.setGridCell(1, "readsetname", readset1);
        grid.setGridCellJS(1, "platform", "ILLUMINA");
        grid.setGridCell(1, "inputmaterial", "InputMaterial");
        grid.setGridCell(1, "subjectid", "Subject1");
        grid.setGridCell(1, "sampledate", "2011-02-03");

        grid.setGridCell(2, "readsetname", readset2);
        grid.setGridCellJS(2, "platform", "LS454");
        grid.setGridCell(2, "inputmaterial", "InputMaterial2");
        grid.setGridCell(2, "subjectid", "Subject2");

        waitAndClick(Ext4Helper.Locators.ext4Button("Import Data"));
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("OK"));

        waitAndClickAndWait(Locator.linkWithText("All"));
        _startedPipelineJobs++;
        waitForElement(Locator.tagContainingText("span", "Data Pipeline"));
        waitForPipelineJobsToComplete(_startedPipelineJobs, "Import Readsets", false);
        assertTextPresent("COMPLETE");

        //verify readsets created
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        SelectRowsCommand sr = new SelectRowsCommand("sequenceanalysis", "sequence_readsets");
        sr.addFilter(new Filter("name", readset1 + ";" + readset2, Filter.Operator.IN));
        SelectRowsResponse resp;
        try
        {
            resp = sr.execute(cn, getProjectName());
        }
        catch (IOException | CommandException fail)
        {
            throw new RuntimeException(fail);
        }
        assertEquals("Incorrect readset number", 2, resp.getRowCount().intValue());

        log("attempting to re-import same files");
        goToProjectHome();
        initiatePipelineJob(_readsetPipelineName, filename1, filename2);
        waitForText("Job Name");
        waitForElement(Ext4Helper.ext4Window("Error"));
        waitForElement(Locator.tagContainingText("div", "There are errors with the input files"));
        isTextPresent("File is already used in existing readsets')]");
        assertElementPresent(Locator.xpath("//td[contains(@style, 'background: red')]"));
        waitAndClick(Ext4Helper.Locators.ext4Button("OK"));
        goToProjectHome();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        cleanupDirectory(_illuminaPipelineLoc);

        deleteProject(getProjectName(), afterTest);
    }

    protected Ext4GridRef getSampleGrid()
    {
        return _ext4Helper.queryOne("#sampleGrid", Ext4GridRef.class);
    }

    protected void cleanupDirectory(String path)
    {
        File dir = new File(path);
        File[] files = dir.listFiles();
        for(File file: files)
        {
            if(file.isDirectory() &&
                    (file.getName().startsWith("sequenceAnalysis_") || file.getName().equals("illuminaImport") || file.getName().equals(".labkey")))
                deleteDir(file);
            if(file.getName().startsWith("SequenceImport"))
                file.delete();
        }
    }

    @Override
    public void setPipelineRoot(String rootPath)
    {
        if (rootPath.equals(_pipelineRoot))
            return;

        _pipelineRoot = rootPath;
        super.setPipelineRoot(rootPath);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}