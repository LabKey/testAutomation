/*
 * Copyright (c) 2012-2015 LabKey Corporation
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
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.reader.Readers;
import org.labkey.api.util.FileUtil;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.External;
import org.labkey.test.categories.LabModule;
import org.labkey.test.categories.ONPRC;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LabModuleHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.labkey.test.util.ext4cmp.Ext4ComboRef;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.labkey.test.util.ext4cmp.Ext4FileFieldRef;
import org.labkey.test.util.ext4cmp.Ext4GridRef;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({External.class, ONPRC.class, LabModule.class})
public class SequenceTest extends BaseWebDriverTest
{
    protected LabModuleHelper _helper = new LabModuleHelper(this);
    protected String _pipelineRoot = null;
    protected final String _sequencePipelineLoc = TestFileUtils.getLabKeyRoot() + "/externalModules/labModules/SequenceAnalysis/resources/sampleData";
    protected final String _illuminaPipelineLoc = TestFileUtils.getLabKeyRoot() + "/sampledata/sequenceAnalysis";
    protected final String _readsetPipelineName = "Import sequence data";
    protected final String _alignmentImportPipelineName = "Import Alignment(s)";

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

        alignmentImportPanelTest();
        readsetImportTest();

        //TODO
        //analyzeAlignmentPanelTest();

        //will also verify UI + pipeline jobs
        createReferenceGenome(this, _startedPipelineJobs);
        _startedPipelineJobs++;
        addReferenceGenomeTracks(this, TEST_GENOME_NAME);

        File testBam = new File(TestFileUtils.getLabKeyRoot(), "/externalModules/labModules/SequenceAnalysis/resources/sampleData/test.bam");
        SequenceTest.addOutputFile(this, testBam, SequenceTest.TEST_GENOME_NAME, "TestBAM", "This is an output file", false);
    }

    protected void setUpTest()
    {
        _containerHelper.createProject(getProjectName(), "Sequence Analysis");

        //populate initial data.  these rows should already be present.
        _containerHelper.enableModule("Shared", "SequenceAnalysis");
        beginAt(getBaseURL() + "/sequenceanalysis/Shared/populateSequences.view");

        waitAndClick(Ext4Helper.Locators.ext4Button("Populate Viral Sequences"));
        waitForElement(Locator.tagContainingText("div", "Populate Complete"), 200000);

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
        waitAndClick(Locator.linkWithText("Plan Sequence Run (Create Readsets)"));
        waitForElement(Ext4Helper.Locators.window("Create Readsets"));
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Submit"));

        _helper.waitForField("Sample Id", WAIT_FOR_PAGE);
        _ext4Helper.clickTabContainingText("Import Spreadsheet");
        waitForText("Copy/Paste Data");

        setFormElementJS(Locator.name("text"), getIlluminaNames());

        waitAndClick(Ext4Helper.Locators.ext4Button("Upload"));
        waitForElement(Ext4Helper.Locators.window("Success"));
        _readsetCt += 14;
        assertTextPresent("Success!");
        clickButton("OK");

        log("verifying readset count correct");
        waitForText("Sequence Readsets");
        waitForElement(LabModuleHelper.getNavPanelItem("Sequence Readsets:", _readsetCt.toString()));
    }

    /**
     * This method is designed to exercise the illumina template UI and create a CSV import template.  This template
     * is later used by importIlluminaTest()
     */
    private void createIlluminaSampleSheet()
    {
        goToProjectHome();
        _helper.clickNavPanelItemAndWait("Readsets:", 1);
        _helper.waitForDataRegion("query");

        //verify CSV file creation
        _extHelper.clickMenuButton(true, "Views", "All");
        _helper.waitForDataRegion("query");
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
        String val = (String) textarea.getValue();
        val += "\n" + newValue;
        textarea.setValue(val);
        clickButton("Done Editing", 0);

        //verify template has changed
        _ext4Helper.clickTabContainingText("General Info");
        assertEquals("Custom", Ext4FieldRef.getForLabel(this, "Application").getValue());

        //verify samples present
        _ext4Helper.clickTabContainingText("Preview Samples");
        waitForText("Sample_ID");

        int expectRows = (11 * (14 + 1));  //11 cols, 14 rows, plus header
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
            sb.append("Illumina" + (i + 1) + "\tILLUMINA\t" + barcodes5[i] + "\t" + barcodes3[i] + "\n");
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
        initiatePipelineJob("Import Illumina data", Arrays.asList(ILLUMINA_CSV));

        setFormElement(Locator.name("protocolName"), "TestIlluminaRun" + _helper.getRandomInt());
        setFormElement(Locator.name("runDate"), "08/25/2011");
        setFormElement(Locator.name("fastqPrefix"), "Il");

        waitAndClick(Ext4Helper.Locators.ext4ButtonEnabled("Import Data"));
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
     *
     * @throws Exception
     */
    private void readsetFeaturesTest() throws IOException
    {
        //verify import and instrument run creation
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=sequenceanalysis&query.queryName=readdata");

        DataRegionTable dr = new DataRegionTable("query", this);
        _helper.waitForDataRegion("query");

        for (int i = 0; i < dr.getDataRowCount(); i++)
        {
            String rowId = dr.getDataAsText(i, "Readset Id");
            String file1 = dr.getDataAsText(i, "Input File");
            assertEquals("Incorrect or no filename associated with readset", "Illumina-R1-" + rowId + ".fastq.gz", file1);

            String file2 = dr.getDataAsText(i, "Input File2");
            assertEquals("Incorrect or no filename associated with readset", "Illumina-R2-" + rowId + ".fastq.gz", file2);
        }

        goToProjectHome();
        _helper.clickNavPanelItemAndWait("Readsets:", 1);
        _helper.waitForDataRegion("query");

        dr = new DataRegionTable("query", this);

        log("Verifying instrument run and details page");
        clickAndWait(dr.link(2, "Instrument Run"));

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
        _helper.clickNavPanelItemAndWait("Readsets:", 1);
        _helper.waitForDataRegion("query");
        dr = new DataRegionTable("query", this);
        clickAndWait(dr.link(1, 1));

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
        _helper.clickNavPanelItemAndWait("Readsets:", 1);
        _helper.waitForDataRegion("query");
        dr = new DataRegionTable("query", this);
        dr.checkAllOnPage();
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_query']" + Locator.lkButton("More Actions").getPath()), "Download Sequence Files");
        waitForElement(Ext4Helper.Locators.window("Export Files"));
        waitForText("Export Files As");
        Ext4CmpRef window = _ext4Helper.queryOne("#exportFilesWin", Ext4CmpRef.class);
        String fileName = "MyFile";
        Ext4FieldRef.getForLabel(this, "File Prefix").setValue(fileName);
        String url = window.getEval("getURL()").toString();
        assertTrue("Improper URL to download sequences", url.contains("zipFileName=" + fileName));
        assertTrue("Improper URL to download sequences", url.contains("exportSequenceFiles.view?"));

        _ext4Helper.queryOne("field[boxLabel='Forward Reads']", Ext4FieldRef.class).setValue("false");
        _ext4Helper.queryOne("field[boxLabel='Merge into Single FASTQ File']", Ext4FieldRef.class).setChecked(true);

        url = window.getEval("getURL()").toString();
        assertTrue("Improper URL to download sequences", url.contains("mergeFastqFiles.view?"));

        File exportFile = clickAndWaitForDownload(Ext4Helper.Locators.ext4Button("Submit"));
        validateFastqDownload(exportFile);

        log("Verifying FASTQC Report");
        dr.uncheckAllOnPage();
        dr.checkCheckbox(2);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_query']" + Locator.lkButton("More Actions").getPath()), "View FASTQC Report");
        waitForElement(Ext4Helper.Locators.window("FastQC"));
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("OK"));

        waitForText("File Summary");
        assertTextPresent("Per base sequence quality");

        log("Verifying Readset Edit");
        goToProjectHome();
        _helper.clickNavPanelItemAndWait("Readsets:", 1);

        waitForText("Instrument Run"); //proxy for dataRegion loading
        clickAndWait(dr.link(1, 0));

        waitForText("3-Barcode:");
        waitForText("Run:");
        String newName = "ChangedSample";
        Ext4FieldRef.getForLabel(this, "Name").setValue(newName);
        sleep(250); //wait for value to save
        clickButton("Submit", 0);
        waitForElement(Ext4Helper.Locators.window("Success"));
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
        _helper.clickNavPanelItemAndWait("Readsets:", 1);
        _helper.waitForDataRegion("query");
        DataRegionTable dr = new DataRegionTable("query", this);
        dr.uncheckAllOnPage();
        dr.checkCheckbox(2);
        List<String> rowIds = new ArrayList<>();
        rowIds.add(dr.getDataAsText(2, "Readset Id"));
        dr.checkCheckbox(6);
        rowIds.add(dr.getDataAsText(6, "Readset Id"));

        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_query']" + Locator.lkButton("More Actions").getPath()), "Align/Analyze Selected");
        waitForElement(Ext4Helper.Locators.window("Import Data"));
        waitForText("Description");
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Submit"));

        log("Verifying analysis UI");

        //setup local variables
        String totalReads = "450";
        String minReadLength = "68";
        String seedMismatches = "3";
        String simpleClipThreshould = "0";
        String qualWindowSize = "8";
        String qualAvgQual = "19";
        //String maskMinQual = "21";
        //String customRefName = "CustomRef1";
        //String customRefSeq = "ATGATGATG";
        String jobName = "TestAnalysisJob";
        String analysisDescription = "This is the description for my analysis";
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
        waitForElement(Ext4Helper.Locators.window("Copy Previous Run?"));
        waitAndClick(Ext4Helper.Locators.window("Copy Previous Run?").append(Ext4Helper.Locators.ext4Button("Cancel")));
        waitForElementToDisappear(Ext4Helper.Locators.window("Copy Previous Run?"));

        for (String rowId : rowIds)
        {
            assertTextPresent("Illumina-R1-" + rowId + ".fastq.gz", "Illumina-R2-" + rowId + ".fastq.gz");
        }

        Ext4FieldRef.getForLabel(this, "Job Name").setValue(jobName);
        Ext4FieldRef.getForLabel(this, "Description").setValue(analysisDescription);

        log("Verifying Pre-processing section");
        waitAndClick(Ext4Helper.Locators.ext4Button("Add Step").index(0));
        Locator.XPathLocator win = Ext4Helper.Locators.window("Add Steps");
        waitForElement(win);

        waitAndClick(Locator.id(_ext4Helper.queryOne("window ldk-linkbutton[text='View Description']", Ext4CmpRef.class).getId()).append(Locator.tag("a")));
        waitForElement(Ext4Helper.Locators.window("Tool Details"));
        waitAndClick(Ext4Helper.Locators.window("Tool Details").append(Ext4Helper.Locators.ext4Button("Done")));

        List<Ext4CmpRef> btns = _ext4Helper.componentQuery("window ldk-linkbutton[text='Add']", Ext4CmpRef.class);
        for (Ext4CmpRef btn : btns)
        {
            waitAndClick(Locator.id(btn.getId()).append(Locator.tag("a")));
        }

        waitAndClick(Ext4Helper.Locators.ext4Button("Done"));
        waitForElementToDisappear(Ext4Helper.Locators.window("Add Steps"));

        Map<String, Ext4CmpRef> fieldsetMap = new HashMap<>();
        String[] setNames = {"Adapter Trimming", "Crop Reads", "Downsample Reads", "Head Crop", "Quality Trimming (Adaptive)", "Quality Trimming (Sliding Window)", "Read Length Filter"};
        isPresentInThisOrder(setNames);

        for (String name : setNames)
        {
            fieldsetMap.put(name, _ext4Helper.queryOne("fieldset[title='" + name + "']", Ext4CmpRef.class));
        }

        waitAndClick(Locator.id(fieldsetMap.get("Downsample Reads").down("ldk-linkbutton[text='Move Down']", Ext4CmpRef.class).getId()).append(Locator.tag("a")));
        isTextBefore("Head Crop", "Downsample Reads");

        waitAndClick(Locator.id(fieldsetMap.get("Head Crop").down("ldk-linkbutton[text='Remove']", Ext4CmpRef.class).getId()).append(Locator.tag("a")));
        waitForElementToDisappear(Locator.id(fieldsetMap.get("Head Crop").getId()));

        Integer overlapLength = 6;
        Double errorRate = 0.2;
        Integer cropLength = 500;
        Integer totalReadsVal = 100;

        //now set params
        Ext4FieldRef.getForLabel(this, "Overlap Length").setValue(overlapLength);
        Ext4FieldRef.getForLabel(this, "Error Rate").setValue(errorRate);
        Ext4FieldRef.getForLabel(this, "Min Length").setValue(minReadLength);
        Ext4FieldRef.getForLabel(this, "Crop Length").setValue(cropLength);
        Ext4FieldRef.getForLabel(this, "Total Reads").setValue(totalReadsVal);
        Ext4FieldRef.getForLabel(this, "Window Size").setValue(qualWindowSize);
        Ext4FieldRef.getForLabel(this, "Avg Qual").setValue(qualAvgQual);
        Ext4FieldRef.getForLabel(this, "Minimum Read Length").setValue(minReadLength);

        waitForText("Adapters");
        waitAndClick(Ext4Helper.Locators.ext4Button("Common Adapters").index(1));
        waitForElement(Ext4Helper.Locators.window("Choose Adapters"));
        waitForText("Choose Adapter Group");
        Ext4FieldRef.getForLabel(this, "Choose Adapter Group").setValue("Roche-454 FLX Amplicon");
        waitAndClick(Ext4Helper.Locators.ext4Button("Submit"));
        Ext4GridRef grid = _ext4Helper.componentQuery("sequenceanalysis-adapterpanel grid", Ext4GridRef.class).get(1);
        grid.waitForRowCount(2);
        Assert.assertEquals("Incorrect cell value", rocheAdapters[0][0], grid.getFieldValue(1, "adapterName"));
        Assert.assertEquals("Incorrect cell value", rocheAdapters[1][0], grid.getFieldValue(2, "adapterName"));
        Assert.assertEquals("Incorrect cell value", rocheAdapters[0][1], grid.getFieldValue(1, "adapterSequence"));
        Assert.assertEquals("Incorrect cell value", rocheAdapters[1][1], grid.getFieldValue(2, "adapterSequence"));

        grid.eval("getSelectionModel().select(0)");
        grid.clickTbarButton("Move Down");
        sleep(500);
        assertTextBefore(rocheAdapters[1][0], rocheAdapters[0][0]);

        grid.eval("getSelectionModel().select(1)");
        grid.clickTbarButton("Move Up");
        sleep(500);
        assertTextBefore(rocheAdapters[0][0], rocheAdapters[1][0]);

        grid.clickTbarButton("Remove");
        sleep(500);
        assertTextNotPresent(rocheAdapters[0][0]);

        log("Testing Alignment Section");

        log("Testing whether sections are disabled when alignment unchecked");
        Ext4FieldRef.getForLabel(this, "Perform Alignment").setChecked(false);
        assertEquals("Field should be hidden", false, Ext4FieldRef.isFieldPresent(this, "Reference Genome Type"));
        assertEquals("Field should be hidden", false, Ext4FieldRef.isFieldPresent(this, "Choose Aligner"));

        Ext4FieldRef.getForLabel(this, "Perform Alignment").setChecked(true);
        waitForText("Reference Genome Type");
        sleep(500);

        log("Testing aligner field and descriptions");
        Ext4ComboRef refLibraryField = Ext4ComboRef.getForLabel(this, "Reference Genome Type");
        Ext4ComboRef alignerField = Ext4ComboRef.getForLabel(this, "Choose Aligner");

        refLibraryField.setComboByDisplayValue("Custom Library");
        Ext4FieldRef.isFieldPresent(this, "Reference Name");
        Ext4FieldRef.isFieldPresent(this, "Sequence");

        refLibraryField.setComboByDisplayValue("DNA Sequences");
        Ext4FieldRef.isFieldPresent(this, "Species");
        Ext4FieldRef.isFieldPresent(this, "Loci");

        refLibraryField.setComboByDisplayValue("Viral Sequences");
        Ext4ComboRef strainField = Ext4ComboRef.getForLabel(this, "Virus Strain");
        strainField.waitForStoreLoad();
        strainField.setValue(strain);

        alignerField.setComboByDisplayValue("BWA-SW");
        waitForText("BWA-SW uses a different algorithm than BWA that is better suited for longer reads.");

        alignerField.setComboByDisplayValue("BWA");
        Ext4FieldRef.isFieldPresent(this, "Max Mismatches");

        alignerField.setComboByDisplayValue("Mosaik");
        Ext4FieldRef.getForLabel(this, "Alignment Threshold").setValue(60);

//        Ext4FieldRef.getForLabel(this, "Min SNP Quality").setValue(minSnpQual);
//        Ext4FieldRef.getForLabel(this, "Min Avg SNP Quality").setValue(minAvgSnpQual);
//        Ext4FieldRef.getForLabel(this, "Min DIP Quality").setValue(minDipQual);
//        Ext4FieldRef.getForLabel(this, "Min Avg DIP Quality").setValue(minAvgDipQual);
//
//        Ext4FieldRef.getForLabel(this, "Sequence Based Genotyping").setChecked(true);

        Ext4CmpRef panel = _ext4Helper.queryOne("#sequenceAnalysisPanel", Ext4CmpRef.class);
        Map<String, Object> params = (Map) panel.getEval("getJsonParams()");

        assertEquals("Incorect param in form JSON", true, params.get("deleteIntermediateFiles"));
        assertEquals("Incorect param in form JSON", analysisDescription, params.get("protocolDescription"));
        assertEquals("Incorect param in form JSON", jobName, params.get("protocolName"));

        assertEquals("Incorect param in form JSON", "IlluminaAdapterTrimming;AdapterTrimming;CropReads;DownsampleReads;MaxInfoTrim;SlidingWindowTrim;ReadLengthFilter", params.get("fastqProcessing"));
        assertEquals("Incorect param in form JSON", overlapLength.toString(), params.get("fastqProcessing.AdapterTrimming.overlapLength").toString());
        assertEquals("Incorect param in form JSON", errorRate.toString(), params.get("fastqProcessing.AdapterTrimming.errorRate").toString());
        assertEquals("Incorect param in form JSON", minReadLength, params.get("fastqProcessing.AdapterTrimming.minLength").toString());

        assertEquals("Incorect param in form JSON", cropLength.toString(), params.get("fastqProcessing.CropReads.cropLength").toString());
        assertEquals("Incorect param in form JSON", totalReadsVal.toString(), params.get("fastqProcessing.DownsampleReads.downsampleReadNumber").toString());
        assertEquals("Incorect param in form JSON", minReadLength, params.get("fastqProcessing.ReadLengthFilter.minLength").toString());

        assertEquals("Incorect param in form JSON", qualAvgQual, params.get("fastqProcessing.SlidingWindowTrim.avgQual").toString());
        assertEquals("Incorect param in form JSON", qualWindowSize, params.get("fastqProcessing.SlidingWindowTrim.windowSize").toString());

        assertEquals("Incorect param in form JSON", "Virus", params.get("referenceLibraryCreation"));
        assertEquals("Incorect param in form JSON", "Virus", params.get("referenceLibraryCreation.Virus.category"));
        assertEquals("Incorect param in form JSON", strain, params.get("referenceLibraryCreation.Virus.subset"));

        List<Object> adapters = (List) params.get("fastqProcessing.AdapterTrimming.adapters");
        List<Object> adapter1 = (List) adapters.get(0);
        assertEquals("Incorect param in form JSON", rocheAdapters[1][0], adapter1.get(0).toString());
        assertEquals("Incorect param in form JSON", rocheAdapters[1][1], adapter1.get(1).toString());
        assertEquals("Incorect param in form JSON", true, adapter1.get(2));
        assertEquals("Incorect param in form JSON", true, adapter1.get(3));

        assertEquals("Incorect param in form JSON", alignerField.getValue(), params.get("alignment"));
        assertEquals("Incorect param in form JSON", 60L, params.get("alignment.Mosaik.align_threshold"));
        assertEquals("Incorect param in form JSON", 32L, params.get("alignment.Mosaik.hash_size"));
        assertEquals("Incorect param in form JSON", 0.02, params.get("alignment.Mosaik.max_mismatch_pct"));
        assertEquals("Incorect param in form JSON", true, params.get("alignment.Mosaik.output_multiple"));

        Map sample = (Map) params.get("readset_0");
        assertFalse("Incorect param in form JSON", null == sample.get("readset"));
        assertFalse("Incorect param in form JSON", null == sample.get("readsetname"));

        sample = (Map) params.get("readset_1");
        assertFalse("Incorect param in form JSON", null == sample.get("readset"));
        assertFalse("Incorect param in form JSON", null == sample.get("readsetname"));
    }

    /**
     * This method will make a request to download merged FASTQ files created during the illumina test
     */
    private void validateFastqDownload(File output) throws IOException
    {
        log("Verifying merged FASTQ export");

        assertTrue("Unable to find file: " + output.getPath(), output.exists());
        log("File size: " + FileUtils.sizeOf(output));

        try (InputStream fileStream = new FileInputStream(output); BufferedReader br = Readers.getReader(new GZIPInputStream(fileStream)))
        {
            int count = 0;
            int totalChars = 0;
            String thisLine;
            List<String> lines = new ArrayList<>();
            while ((thisLine = br.readLine()) != null)
            {
                count++;
                totalChars += thisLine.length();
                lines.add(thisLine);
            }

            int expectedLength = 504;
            assertEquals("Length of file doesnt match expected value.  Total characters: " + totalChars, expectedLength, count);

            output.delete();
        }
    }

    protected void initiatePipelineJob(String importAction, List<String> files)
    {
        goToProjectHome();
        waitForText("Upload Files");

        //avoid entering a workbook
        beginAt(getBaseURL() + "/pipeline/" + getProjectName() + "/browse.view");
        waitForText("fileset");
        _fileBrowserHelper.waitForFileGridReady();
        for (String f : files)
        {
            _fileBrowserHelper.checkFileBrowserFileCheckbox(f);
        }

        _fileBrowserHelper.selectImportDataAction(importAction);

    }

    /**
     * The intent of this method is to perform additional tests of this AlignmentImportPanel,
     * with the goal of exercising all UI options, but not actually running the pipeline job since this runs remotely.
     */
    private void alignmentImportPanelTest() throws IOException
    {
        log("Verifying AlignmentImportPanel UI");

        goToProjectHome();
        setPipelineRoot(_sequencePipelineLoc);

        File inputBam = new File(_sequencePipelineLoc, "test.bam");
        if (!inputBam.exists())
        {
            FileUtils.copyFile(new File(TestFileUtils.getLabKeyRoot(), "/externalModules/labModules/SequenceAnalysis/resources/sampleData/test.bam"), inputBam);
        }

        initiatePipelineJob(_alignmentImportPipelineName, Arrays.asList(inputBam.getName()));
        waitForText("Job Name");

        Ext4FieldRef.getForLabel(this, "Job Name").setValue("AlignmentTest_" + System.currentTimeMillis());

        waitForElement(Locator.linkContainingText(inputBam.getName()));

        Ext4FieldRef treatmentField = Ext4FieldRef.getForLabel(this, "Treatment of Input Files");
        Assert.assertEquals("Incorrect starting value for input file-handling field", "delete", treatmentField.getValue());

        waitAndClick(Ext4Helper.Locators.ext4Button("Import Data"));
        waitForElement(Ext4Helper.Locators.window("Error"));
        waitForElement(Locator.tagContainingText("div", "There are 4 errors.  Please review the cells highlighted in red."));
        click(Ext4Helper.Locators.ext4Button("OK"));

        Ext4GridRef readsetGrid = _ext4Helper.queryOne("#sampleGrid", Ext4GridRef.class);
        readsetGrid.setGridCell(1, "readsetname", "Readset1");
        readsetGrid.setGridCell(1, "platform", "ILLUMINA");
        readsetGrid.setGridCell(1, "application", "DNA Sequencing (Genome)");

        waitAndClick(Ext4Helper.Locators.ext4Button("Add Step"));
        Locator.XPathLocator win = Ext4Helper.Locators.window("Add Steps");
        waitForElement(win);

        List<Ext4CmpRef> btns = _ext4Helper.componentQuery("window ldk-linkbutton[text='Add']", Ext4CmpRef.class);
        for (Ext4CmpRef btn : btns)
        {
            waitAndClick(Locator.id(btn.getId()).append(Locator.tag("a")));
        }

        waitAndClick(Ext4Helper.Locators.ext4Button("Done"));
        waitForElementToDisappear(Ext4Helper.Locators.window("Add Steps"));

        //TODO: check JSON

        goToProjectHome();
    }

    /**
     * The intent of this method is to perform additional tests of this Readset Import Panel,
     * with the goal of exercising all UI options
     */
    private void readsetPanelTest() throws IOException
    {
        log("Verifying Readset Import Panel UI");

        goToProjectHome();
        setPipelineRoot(_sequencePipelineLoc);

        List<String> files = new ArrayList<>();

        String filename1 = "dualBarcodes_SIV.fastq.gz";
        files.add(filename1);

        String filename2 = "sample454_SIV.sff";
        files.add(filename2);

        //multiples files from a single group
        List<Pair<String, String>> readGroup = new ArrayList<>();
        readGroup.add(Pair.of("s_G1_L001_R1_001.fastq.gz", "s_G1_L001_R2_001.fastq.gz"));
        readGroup.add(Pair.of("s_G1_L001_R1_002.fastq.gz", "s_G1_L001_R2_002.fastq.gz"));
        readGroup.add(Pair.of("s_G1_L002_R1_001.fastq.gz", "s_G1_L002_R2_001.fastq.gz"));
        for (Pair<String, String> pair : readGroup)
        {
            FileUtils.copyFile(new File(_sequencePipelineLoc, "paired1.fastq.gz"), new File(_sequencePipelineLoc, pair.getLeft()));
            files.add(pair.getLeft());

            FileUtils.copyFile(new File(_sequencePipelineLoc, "paired2.fastq.gz"), new File(_sequencePipelineLoc, pair.getRight()));
            files.add(pair.getRight());
        }

        initiatePipelineJob(_readsetPipelineName, files);
        waitForText("Job Name");

        Ext4FieldRef.getForLabel(this, "Job Name").setValue("SequenceTest_" + System.currentTimeMillis());

        waitForElement(Locator.linkContainingText(filename1));
        waitForElement(Locator.linkContainingText(filename2));

        Ext4FieldRef barcodeField = Ext4FieldRef.getForLabel(this, "Use Barcodes");
        Ext4FieldRef treatmentField = Ext4FieldRef.getForLabel(this, "Treatment of Input Files");

        assertEquals("Incorrect starting value for input file-handling field", "delete", treatmentField.getValue());

        Ext4GridRef readDataGrid = _ext4Helper.queryOne("#readDataGrid", Ext4GridRef.class);
        Ext4GridRef readsetGrid = _ext4Helper.queryOne("#readsetGrid", Ext4GridRef.class);

        //verify file groups worked right
        Assert.assertEquals("incorrect row count", 5, readDataGrid.getRowCount());
        Assert.assertEquals("incorrect row count", 3, readsetGrid.getRowCount());

        Assert.assertNotNull(StringUtils.trimToNull(Objects.toString(readDataGrid.getFieldValue(1, "fileRecord1"))));
        Assert.assertNull(StringUtils.trimToNull(Objects.toString(readDataGrid.getFieldValue(1, "fileRecord2"))));

        Assert.assertNotNull(StringUtils.trimToNull(Objects.toString(readDataGrid.getFieldValue(2, "fileRecord1"))));
        Assert.assertNotNull(StringUtils.trimToNull(Objects.toString(readDataGrid.getFieldValue(2, "fileRecord2"))));
        Assert.assertNotNull(StringUtils.trimToNull(Objects.toString(readDataGrid.getFieldValue(3, "fileRecord1"))));
        Assert.assertNotNull(StringUtils.trimToNull(Objects.toString(readDataGrid.getFieldValue(3, "fileRecord2"))));
        Assert.assertNotNull(StringUtils.trimToNull(Objects.toString(readDataGrid.getFieldValue(4, "fileRecord1"))));
        Assert.assertNotNull(StringUtils.trimToNull(Objects.toString(readDataGrid.getFieldValue(4, "fileRecord2"))));

        Assert.assertNotNull(StringUtils.trimToNull(Objects.toString(readDataGrid.getFieldValue(5, "fileRecord1"))));
        Assert.assertNull(StringUtils.trimToNull(Objects.toString(readDataGrid.getFieldValue(5, "fileRecord2"))));

        Assert.assertEquals("incorrect group count", 3L, readDataGrid.getEval("store.getGroups().length"));

        //then split groups
        waitAndClick(readDataGrid.getRow(2));
        readDataGrid.clickTbarButton("Split/Regroup Selected");
        waitForElement(Ext4Helper.Locators.window("Split/Regroup Files"));
        String groupName = "NewGroup";
        _ext4Helper.queryOne("window textfield", Ext4FieldRef.class).setValue(groupName);
        waitAndClick(Ext4Helper.Locators.ext4ButtonEnabled("OK"));
        sleep(200);
        Assert.assertEquals("incorrect group count", 4L, readDataGrid.getEval("store.getGroups().length"));
        Assert.assertEquals("incorrect readsetname", "dualBarcodes_SIV", readsetGrid.getFieldValue(1, "fileGroupId"));
        readDataGrid.setGridCell(2, "fileGroupId", groupName);
        sleep(200);
        Assert.assertEquals("incorrect group count", 3L, readDataGrid.getEval("store.getGroups().length"));
        Assert.assertEquals("incorrect readsetname", groupName, readsetGrid.getFieldValue(1, "fileGroupId"));

        readDataGrid.setGridCell(1, "fileGroupId", groupName);
        Assert.assertEquals("incorrect readsetname", "sample454_SIV", readsetGrid.getFieldValue(3, "fileGroupId"));
        readDataGrid.setGridCell(5, "fileGroupId", "sample454_SIVb");
        Assert.assertEquals("incorrect readsetname", "sample454_SIVb", readsetGrid.getFieldValue(3, "fileGroupId"));

        //now we check readset info
        readsetGrid.clickTbarButton("Bulk Edit");
        waitForElement(Ext4Helper.Locators.window("Bulk Edit").notHidden());
        Ext4FieldRef.waitForField(this, "Select Field");
        sleep(500);

        _ext4Helper.selectComboBoxItem("Select Field:", Ext4Helper.TextMatchTechnique.CONTAINS, "Application");
        Ext4ComboRef.waitForField(this, "Application");
        Ext4ComboRef.getForLabel(this, "Application").setValue("DNA Sequencing (Genome)");
        waitAndClick(Ext4Helper.Locators.ext4ButtonEnabled("Submit"));
        waitForElementToDisappear(Ext4Helper.Locators.window("Bulk Edit"));
        sleep(200);
        Assert.assertEquals("field not set", "DNA Sequencing (Genome)", readsetGrid.getFieldValue(1, "application"));
        Assert.assertEquals("field not set", "DNA Sequencing (Genome)", readsetGrid.getFieldValue(2, "application"));
        Assert.assertEquals("field not set", "DNA Sequencing (Genome)", readsetGrid.getFieldValue(3, "application"));

        barcodeField.setChecked(true);
        sleep(200);
        Assert.assertEquals("Incorrect value for input file-handling field after barcode toggle", "compress", treatmentField.getValue());
        Assert.assertFalse("MID5 column should not be hidden", (Boolean) readsetGrid.getEval("columns[1].hidden"));
        Assert.assertFalse("MID3 column should not be hidden", (Boolean) readsetGrid.getEval("columns[2].hidden"));

        barcodeField.setChecked(false);
        sleep(200);
        assertEquals("Incorrect value for input file-handling field after barcode toggle", "delete", treatmentField.getValue());
        assertTrue("MID5 column should be hidden", (Boolean) readsetGrid.getEval("columns[1].hidden"));
        assertTrue("MID3 column should be hidden", (Boolean) readsetGrid.getEval("columns[2].hidden"));

        waitAndClick(Ext4Helper.Locators.ext4Button("Import Data"));
        waitForElement(Ext4Helper.Locators.window("Error"));
        waitForElement(Locator.tagContainingText("div", "There are 12 errors.  Please review the cells highlighted in red."));
        click(Ext4Helper.Locators.ext4Button("OK"));

        setBasicSampleDetails(readsetGrid);
        readsetGrid.setGridCell(2, "readsetname", "Readset1"); //duplicate name

        waitAndClick(Ext4Helper.Locators.ext4Button("Import Data"));
        waitForElement(Ext4Helper.Locators.window("Error"));
        waitForElement(Locator.tagContainingText("div", "There are 2 errors.  Please review the cells highlighted in red.  Note: you can hover over the cell for more information on the issue."));
        waitAndClick(Ext4Helper.Locators.ext4Button("OK"));

        //try duplicate barcodes
        barcodeField.setChecked(true);

        String barcode = "FLD0376";
        readsetGrid.setGridCellJS(1, "barcode5", barcode);
        readsetGrid.setGridCellJS(2, "barcode3", barcode);
        waitAndClick(Ext4Helper.Locators.ext4Button("Import Data"));
        waitForElement(Ext4Helper.Locators.window("Error"));

        waitForElement(Locator.tagContainingText("div", "There are 2 errors.  Please review the cells highlighted in red."));
        click(Ext4Helper.Locators.ext4Button("OK"));
        barcodeField.setChecked(false);
        readsetGrid.setGridCell(2, "readsetname", "Readset2");
        Ext4CmpRef panel = _ext4Helper.queryOne("#sequenceAnalysisPanel", Ext4CmpRef.class);
        Map<String, Object> params = (Map) panel.getEval("getJsonParams()");
        Map<String, Object> fieldsJson = (Map) params.get("json");
        //List<Long> fileIds = (List)params.get("distinctIds");
        //List<String> fieldsNames = (List)params.get("distinctNames");

        String container = (String) executeScript("return LABKEY.Security.currentContainer.id");
        assertEquals("Incorect param for containerId", container, fieldsJson.get("containerId"));
        assertEquals("Incorect param for baseURL", getBaseURL() + "/", fieldsJson.get("baseUrl"));

        Long id1 = (Long) executeScript("return LABKEY.Security.currentUser.id");
        Long id2 = (Long) fieldsJson.get("userId");
        assertEquals("Incorect param for userId", id1, id2);
        String containerPath = getURL().getPath().replaceAll("/importReadset.view(.)*", "").replaceAll("(.)*/sequenceanalysis", "");
        assertEquals("Incorect param for containerPath", containerPath, fieldsJson.get("containerPath"));

        Collections.sort(files);
        assertEquals("Unexpected value for param", StringUtils.join(files, ";"), fieldsJson.get("fileNames"));

        assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) fieldsJson.get("inputfile.barcodeGroups")));
        assertEquals("Unexpected value for param", new Long(0), fieldsJson.get("inputfile.barcodeEditDistance"));
        assertEquals("Unexpected value for param", new Long(0), fieldsJson.get("inputfile.barcodeOffset"));
        assertEquals("Unexpected value for param", new Long(0), fieldsJson.get("inputfile.barcodeDeletions"));
        assertEquals("Unexpected value for param", false, fieldsJson.get("inputfile.barcode"));
        assertEquals("Unexpected value for param", "delete", fieldsJson.get("inputfile.inputTreatment"));
        assertEquals("Unexpected value for param", true, fieldsJson.get("deleteIntermediateFiles"));

        Map<String, Object> fileGroup0 = (Map) fieldsJson.get("fileGroup_1");
        Assert.assertEquals("Unexpected value for param", groupName, fileGroup0.get("name"));
        List<Map> arr0 = (List)fileGroup0.get("files");
        Assert.assertEquals(2, arr0.size());
        Assert.assertEquals(groupName, arr0.get(0).get("fileGroupId"));
        Assert.assertEquals("s-G1_L001", arr0.get(0).get("platformUnit"));

        Assert.assertEquals(groupName, arr0.get(1).get("fileGroupId"));
        Assert.assertEquals("", arr0.get(1).get("platformUnit"));

        Map<String, Object> fileGroup1 = (Map) fieldsJson.get("fileGroup_2");
        Assert.assertEquals(2, ((List)fileGroup1.get("files")).size());
        Map<String, Object> fileGroup2 = (Map) fieldsJson.get("fileGroup_3");
        Assert.assertEquals(1, ((List)fileGroup2.get("files")).size());

        Map<String, Object> sample0 = (Map) fieldsJson.get("readset_0");
        Map<String, Object> sample1 = (Map) fieldsJson.get("readset_1");
        Map<String, Object> sample2 = (Map) fieldsJson.get("readset_2");

        Assert.assertEquals("Unexpected value for param", "Readset1", sample0.get("readsetname"));
        Assert.assertEquals("Unexpected value for param", "Subject1", sample0.get("subjectid"));
        Assert.assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) sample0.get("readset")));
        Assert.assertEquals("Unexpected value for param", "ILLUMINA", sample0.get("platform"));
        Assert.assertEquals("Unexpected value for param", groupName, StringUtils.trimToNull((String) sample0.get("fileGroupId")));
        Assert.assertNull("param should not be present", StringUtils.trimToNull((String) sample0.get("barcode5")));
        Assert.assertNull("param should not be present", StringUtils.trimToNull((String) sample0.get("barcode3")));

        Assert.assertEquals("Unexpected value for param", "Readset2", sample1.get("readsetname"));
        Assert.assertEquals("Unexpected value for param", "Subject2", sample1.get("subjectid"));
        Assert.assertEquals("Unexpected value for param", "2010-10-20T07:00:00.000Z", sample1.get("sampledate"));
        Assert.assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) sample1.get("readset")));
        Assert.assertEquals("Unexpected value for param", "LS454", sample1.get("platform"));
        Assert.assertEquals("Unexpected value for param", "s-G1", StringUtils.trimToNull((String) sample1.get("fileGroupId")));
        Assert.assertEquals("Unexpected value for param", "gDNA", sample1.get("sampletype"));
        Assert.assertNull("param should not be present", StringUtils.trimToNull((String)sample1.get("barcode5")));
        Assert.assertNull("param should not be present", StringUtils.trimToNull((String)sample1.get("barcode3")));

        Assert.assertEquals("Unexpected value for param", "Readset3", sample2.get("readsetname"));
        Assert.assertEquals("Unexpected value for param", "Subject3", sample2.get("subjectid"));
        Assert.assertEquals("Unexpected value for param", "2010-10-20T07:00:00.000Z", sample2.get("sampledate"));
        Assert.assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) sample2.get("readset")));
        Assert.assertEquals("Unexpected value for param", "LS454", sample2.get("platform"));
        Assert.assertEquals("Unexpected value for param", "sample454_SIVb", StringUtils.trimToNull((String) sample2.get("fileGroupId")));
        Assert.assertEquals("Unexpected value for param", "gDNA", sample2.get("sampletype"));
        Assert.assertNull("param should not be present", StringUtils.trimToNull((String)sample2.get("barcode5")));
        Assert.assertNull("param should not be present", StringUtils.trimToNull((String)sample2.get("barcode3")));

        barcodeField.setValue(true);
        Ext4FieldRef.getForLabel(this, "Additional Barcodes To Test").setValue("GSMIDs");
        Ext4FieldRef.getForLabel(this, "Mismatches Tolerated").setValue(9);
        Ext4FieldRef.getForLabel(this, "Deletions Tolerated").setValue(9);
        Ext4FieldRef.getForLabel(this, "Allowed Distance From Read End").setValue(9);

        Ext4FieldRef.getForLabel(this, "Delete Intermediate Files").setValue(false);
        treatmentField.setValue("compress");

        readsetGrid.setGridCell(1, "barcode5", "FLD0001");
        readsetGrid.setGridCell(2, "barcode5", "FLD0002");
        readsetGrid.setGridCell(3, "barcode5", "FLD0003");

        params = (Map) panel.getEval("getJsonParams()");
        fieldsJson = (Map) params.get("json");

        Assert.assertEquals("Unexpected value for param", true, fieldsJson.get("inputfile.barcode"));
        Assert.assertEquals("Unexpected value for param", Collections.singletonList("GSMIDs"), fieldsJson.get("inputfile.barcodeGroups"));
        Assert.assertEquals("Unexpected value for param", new Long(9), fieldsJson.get("inputfile.barcodeEditDistance"));
        Assert.assertEquals("Unexpected value for param", new Long(9), fieldsJson.get("inputfile.barcodeOffset"));
        Assert.assertEquals("Unexpected value for param", new Long(9), fieldsJson.get("inputfile.barcodeDeletions"));

        Assert.assertEquals("Unexpected value for param", "compress", fieldsJson.get("inputfile.inputTreatment"));
        Assert.assertEquals("Unexpected value for param", false, fieldsJson.get("deleteIntermediateFiles"));

        sample0 = (Map) fieldsJson.get("readset_0");
        Assert.assertEquals("Unexpected value for param", "Readset1", sample0.get("readsetname"));
        Assert.assertEquals("Unexpected value for param", "Subject1", sample0.get("subjectid"));
        Assert.assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) sample0.get("readset")));
        Assert.assertEquals("Unexpected value for param", "ILLUMINA", sample0.get("platform"));
        Assert.assertEquals("Unexpected value for param", null, StringUtils.trimToNull((String) sample0.get("fileId")));
        Assert.assertEquals("Unexpected value for param", "gDNA", sample0.get("sampletype"));
        Assert.assertEquals("Unexpected value for param", "FLD0001", sample0.get("barcode5"));
        Assert.assertNull("Unexpected value for param", StringUtils.trimToNull((String)sample0.get("barcode3")));
    }

    private void setBasicSampleDetails(Ext4GridRef grid)
    {
        grid.setGridCell(1, "readsetname", "Readset1");
        grid.setGridCellJS(1, "platform", "ILLUMINA");
        grid.setGridCellJS(1, "sampletype", "gDNA");
        grid.setGridCell(1, "subjectid", "Subject1");

        grid.setGridCell(2, "readsetname", "Readset2");
        grid.setGridCellJS(2, "platform", "LS454");
        grid.setGridCellJS(2, "sampletype", "gDNA");
        grid.setGridCell(2, "subjectid", "Subject2");
        grid.setGridCell(2, "sampledate", "2010-10-20");

        if (grid.getRowCount() > 2)
        {
            grid.setGridCell(3, "readsetname", "Readset3");
            grid.setGridCellJS(3, "platform", "LS454");
            grid.setGridCellJS(3, "sampletype", "gDNA");
            grid.setGridCell(3, "subjectid", "Subject3");
            grid.setGridCell(3, "sampledate", "2010-10-20");
        }
    }

    private void readsetImportTest() throws IOException, CommandException
    {
        log("Verifying Readset Import");

        goToProjectHome();
        setPipelineRoot(_sequencePipelineLoc);

        List<String> files = new ArrayList<>();

        String filename1 = "dualBarcodes_SIV.fastq.gz";
        files.add(filename1);

        String filename2 = "paired1.fastq.gz";
        files.add(filename2);

        //multiples files from a single group
        List<Pair<String, String>> readGroup = new ArrayList<>();
        readGroup.add(Pair.of("s_G1_L001_R1_001.fastq.gz", "s_G1_L001_R2_001.fastq.gz"));

        readGroup.add(Pair.of("s_G1_L002_R1_001.fastq.gz", "s_G1_L002_R2_001.fastq.gz"));
        for (Pair<String, String> pair : readGroup)
        {
            FileUtils.copyFile(new File(_sequencePipelineLoc, "paired1.fastq.gz"), new File(_sequencePipelineLoc, pair.getLeft()));
            files.add(pair.getLeft());

            FileUtils.copyFile(new File(_sequencePipelineLoc, "paired2.fastq.gz"), new File(_sequencePipelineLoc, pair.getRight()));
            files.add(pair.getRight());
        }

        initiatePipelineJob(_readsetPipelineName, files);
        waitForText("Job Name");

        Ext4FieldRef.getForLabel(this, "Job Name").setValue("SequenceTest_" + System.currentTimeMillis());

        waitForElement(Locator.linkContainingText(filename1));
        waitForElement(Locator.linkContainingText(filename2));

        Ext4FieldRef.getForLabel(this, "Treatment of Input Files").setValue("none");
        Ext4GridRef readsetGrid = _ext4Helper.queryOne("#readsetGrid", Ext4GridRef.class);

        String readset1 = "ImportReadsetTest1";
        String readset2 = "ImportReadsetTest2";
        String readset3 = "ImportReadsetTest3";

        readsetGrid.setGridCell(1, "readsetname", readset1);
        readsetGrid.setGridCellJS(1, "platform", "ILLUMINA");
        readsetGrid.setGridCellJS(1, "application", "DNA Sequencing (Genome)");
        readsetGrid.setGridCellJS(1, "sampletype", "gDNA");
        readsetGrid.setGridCell(1, "subjectid", "Subject1");
        readsetGrid.setGridCell(1, "sampledate", "2011-02-03");

        readsetGrid.setGridCell(2, "readsetname", readset2);
        readsetGrid.setGridCellJS(2, "platform", "LS454");
        readsetGrid.setGridCellJS(2, "application", "DNA Sequencing (Genome)");
        readsetGrid.setGridCellJS(2, "sampletype", "gDNA");
        readsetGrid.setGridCell(2, "subjectid", "Subject2");

        readsetGrid.setGridCell(3, "readsetname", readset3);
        readsetGrid.setGridCellJS(3, "platform", "LS454");
        readsetGrid.setGridCellJS(3, "application", "DNA Sequencing (Genome)");
        readsetGrid.setGridCellJS(3, "sampletype", "gDNA");
        readsetGrid.setGridCell(3, "subjectid", "Subject3");

        waitAndClick(Ext4Helper.Locators.ext4Button("Import Data"));
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("OK"));

        waitAndClickAndWait(Locator.linkWithText("All"));
        _startedPipelineJobs++;
        waitForElement(Locator.tagContainingText("span", "Data Pipeline"));
        waitForPipelineJobsToComplete(_startedPipelineJobs, "Import Readsets", false);
        assertTextPresent("COMPLETE");

        log("verify readsets created");
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        SelectRowsCommand sr = new SelectRowsCommand("sequenceanalysis", "sequence_readsets");
        sr.addFilter(new Filter("name", readset1 + ";" + readset2 + ";" + readset3, Filter.Operator.IN));
        SelectRowsResponse resp = sr.execute(cn, getProjectName());
        Assert.assertEquals("Incorrect readset number", 3, resp.getRowCount().intValue());


        SelectRowsCommand sr2 = new SelectRowsCommand("sequenceanalysis", "readdata");
        sr2.addFilter(new Filter("readset/name", readset1, Filter.Operator.EQUAL));
        Assert.assertEquals("Incorrect readdata number", 1, sr2.execute(cn, getProjectName()).getRowCount().intValue());

        SelectRowsCommand sr3 = new SelectRowsCommand("sequenceanalysis", "readdata");
        sr3.addFilter(new Filter("readset/name", readset2, Filter.Operator.EQUAL));
        Assert.assertEquals("Incorrect readdata number", 1, sr3.execute(cn, getProjectName()).getRowCount().intValue());

        SelectRowsCommand sr4 = new SelectRowsCommand("sequenceanalysis", "readdata");
        sr4.addFilter(new Filter("readset/name", readset3, Filter.Operator.EQUAL));
        Assert.assertEquals("Incorrect readdata number", 2, sr4.execute(cn, getProjectName()).getRowCount().intValue());

        log("attempting to re-import same files");
        goToProjectHome();
        initiatePipelineJob(_readsetPipelineName, Arrays.asList(filename1, filename2));
        waitForText("Job Name");
        waitForElement(Ext4Helper.Locators.window("Error"));
        waitForElement(Locator.tagContainingText("div", "There are errors with the input files"));
        isTextPresent("File is already used in existing readsets')]");
        waitAndClick(Ext4Helper.Locators.ext4Button("OK"));
        goToProjectHome();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        cleanupDirectory(_illuminaPipelineLoc);
        super.doCleanup(afterTest);
    }

    protected void cleanupDirectory(String path)
    {
        File dir = new File(path);
        File[] files = dir.listFiles();
        for (File file : files)
        {
            if (file.isDirectory() &&
                    (file.getName().startsWith("sequenceAnalysis_") || file.getName().equals("illuminaImport") || file.getName().equals(".labkey")))
                TestFileUtils.deleteDir(file);
            if (file.getName().startsWith("SequenceImport"))
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
    public List<String> getAssociatedModules()
    {
        return null;
    }

    public static String TEST_GENOME_NAME = "TestGenome1";

    public static void createReferenceGenome(BaseWebDriverTest test, int previouslyStartedPipelineJobs) throws Exception
    {
        test.log("creating SIVmac239 reference genome");
        test.beginAt("/sequenceanalysis/" + test.getContainerId() + "/begin.view");

        test.waitAndClickAndWait(Locator.linkContainingText("Reference Sequences"));
        DataRegionTable dr = new DataRegionTable("query", test);
        dr.setFilter("name", "Equals", "SIVmac239");
        dr.checkCheckbox(0);
        test._extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_query']" + Locator.lkButton("More Actions").getPath()), "Create Reference Genome");
        test.waitForElement(Ext4Helper.Locators.window("Create Reference Genome"));
        Ext4FieldRef.getForLabel(test, "Name").setValue(TEST_GENOME_NAME);
        Ext4FieldRef.getForLabel(test, "Description").setValue("This is a reference genome description");
        test.waitAndClick(Ext4Helper.Locators.ext4ButtonEnabled("Submit"));
        test.waitForElement(Ext4Helper.Locators.window("Success"));
        test.waitAndClickAndWait(Ext4Helper.Locators.ext4ButtonEnabled("OK"));
        test.waitForPipelineJobsToComplete(previouslyStartedPipelineJobs + 1, "Create Reference Genome", false);
        test.goToProjectHome();
    }

    public static void addReferenceGenomeTracks(BaseWebDriverTest test, String genomeName) throws Exception
    {
        test.log("adding resources to genome: " + genomeName);
        test.beginAt("/sequenceanalysis/" + test.getContainerId() + "/begin.view");

        test.waitAndClickAndWait(Locator.linkContainingText("Reference Genomes"));
        DataRegionTable dr = new DataRegionTable("query", test);
        dr.setFilter("name", "Equals", genomeName);

        //page has loaded
        dr = new DataRegionTable("query", test);
        test.clickAndWait(dr.link(0, "Genome Name"));
        test.waitForElement(Locator.tagContainingText("span", "Aliases for External Browsers or Databases"));

        File dataDir = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/sequenceAnalysis/genomeAnnotations");
        for (File f : dataDir.listFiles())
        {
            test.log("adding track: " + f.getName());
            test.clickButton("Add Annotation", 0);
            test.waitForElement(Ext4Helper.Locators.window("Import Annotations/Tracks"));
            Ext4FieldRef.getForLabel(test, "Track Name").setValue(LabModuleHelper.getBaseName(f.getName()));
            Ext4FieldRef.getForLabel(test, "Description").setValue("This is the track description");
            Ext4FileFieldRef fileField = test._ext4Helper.queryOne("field[fieldLabel=File]", Ext4FileFieldRef.class);
            fileField.setToFile(f);

            test.waitAndClick(Ext4Helper.Locators.ext4ButtonEnabled("Submit"));
            test.waitForElement(Ext4Helper.Locators.window("Success"));
            test.waitAndClick(Ext4Helper.Locators.ext4ButtonEnabled("OK"));
            test.waitForElement(Locator.tagContainingText("a", FileUtil.getBaseName(f.getName()))); //proxy for DR reload
        }
    }

    public static void addOutputFile(BaseWebDriverTest test, File toAdd, String genomeName, String name, String description, boolean isWorkbook) throws Exception
    {
        test.log("adding output file: " + toAdd.getName());
        test.beginAt("/query/" + test.getContainerId() + "/executeQuery.view?query.queryName=outputfiles&schemaName=sequenceanalysis");

        test.clickButton("Import Files", 0);
        if (!isWorkbook)
        {
            test.waitForElement(Ext4Helper.Locators.window("Import Sequence Output File"));
            test.waitAndClick(Ext4Helper.Locators.ext4Button("Submit"));
        }

        test.waitForElement(Ext4Helper.Locators.window("Import Output File"));
        Ext4FieldRef.getForLabel(test, "Name").setValue(name);
        Ext4FieldRef.getForLabel(test, "Description").setValue(description);
        Ext4ComboRef.getForLabel(test, "Reference Genome").setComboByDisplayValue(genomeName);

        Ext4FileFieldRef fileField = test._ext4Helper.queryOne("field[fieldLabel=File]", Ext4FileFieldRef.class);
        fileField.setToFile(toAdd);

        test.waitAndClick(Ext4Helper.Locators.ext4ButtonEnabled("Submit"));
        test.waitForElement(Ext4Helper.Locators.window("Success"));
        test.waitAndClickAndWait(Ext4Helper.Locators.ext4ButtonEnabled("OK"));
    }
}