/*
 * Copyright (c) 2011-2013 LabKey Corporation
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

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.ExecuteSqlCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * User: elvan
 * Date: 10/22/11
 * Time: 7:14 PM
 */
public class GenotypingTest extends BaseSeleniumWebTest
{
    public static final String first454importNum = "207";
    public static final String second454importNum = "208";
    public static final String illuminaImportNum = "206";
    protected int pipelineJobCount = 0;

    String pipelineLoc =  getLabKeyRoot() + "/sampledata/genotyping";
    protected int runNum = 0; //this is globally unique, so we need to retrieve it every time.
    protected String checkboxId = ".select";
//    private String expectedAnalysisCount = "1 - 61 of 61";

    DataRegionTable drt = null;
    private String samples = "samples";
    private String TEMPLATE_NAME = "GenotypingTest Saved Template";

    @Override
    protected String getProjectName()
    {
        return "GenotypingVerifyProject";
    }

    protected int analysisCount = 1410;
    protected String getExpectedAnalysisCount()
    {
        return new DecimalFormat().format(analysisCount);
    }

    public boolean isFileUploadTest()
    {
        return true;
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    public void setUp2()
    {
        _containerHelper.createProject(getProjectName(), "Genotyping");
        setUpLists();
        configureAdmin();
        clickProject(getProjectName());
        setPipelineRoot(pipelineLoc);
    }

    //pre-
    private void setUpLists()
    {
        log("Import genotyping list");
        clickProject(getProjectName());
        _listHelper.importListArchive(getProjectName(), new File(pipelineLoc, "sequencing.lists.zip"));
        assertTextPresent(
                samples,
                "mids",
                "sequences",
                "runs"
        );

    }

    private void configureAdmin()
    {
        clickProject(getProjectName());
        clickLink("adminSettings");

        String[] listVals = {"sequences", "runs", samples};
        for(int i=0; i<3; i++)
        {
            click(Locator.linkContainingText("configure",i));
            _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);
            _extHelper.selectComboBoxItem("Schema:", "lists");
            _extHelper.selectComboBoxItem("Query:", listVals[i]);
            _extHelper.selectComboBoxItem("View:", "[default view]");
            _extHelper.clickExtButton("Submit", 0);
            _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        }
        setFormElement(Locator.name("galaxyURL"), "http://galaxy.labkey.org:8080");
        clickButton("Submit");
        clickButton("Load Sequences");

        log("Configure Galaxy Server Key");
        clickAndWait(Locator.linkWithText("My Settings"));
        setFormElement(Locator.name("galaxyKey"), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        clickButton("Submit");
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setUp2();
        goToProjectHome();

        //TODO: need to fix 454/genotyping tests
        importRunTest();
        importRunAgainTest(); //Issue 13695
        runAnalysisTest();
        importSecondRunTest();
        //TODO:  split illumina and 454 into separate tests, since they have so little overlap.
        verifyIlluminaSampleSheet();
        goToProjectHome();
        importIlluminaRunTest();
        verifyIlluminaExport();
        verifyAnalysis();
        verifyCleanIlluminaSampleSheets();

    }

    //verify that with good data, there is no QC warning when creating an illumina sample sheet
    //https://docs.google.com/a/labkey.com/file/d/0B45Fm0-0-NLtdmpDR1hKaW5jSWc/edit
    private void verifyCleanIlluminaSampleSheets()
    {
        importFolderFromZip(new File(pipelineLoc, "/genoCleanSamples.folder.zip"), 6);
        goToProjectHome();
        click(Locator.linkWithText("Samples"));
        waitForText("SIVkcol2");
        DataRegionTable d = new DataRegionTable("query", this);
        d.checkAllOnPage();
        clickButton("Create Illumina Sample Sheet");
        waitForText("You have chosen to export 6 samples");
        assertTextNotPresent("Warning");
    }

    private void importSecondRunTest()
    {
        if(!isGroupConcatSupported())
            return;
        goToProjectHome();
        startImportRun("secondRead/reads.txt", "Import 454 Reads", second454importNum);
        waitForPipelineJobsToComplete(++pipelineJobCount, "Import reads for 454 run", true);
        clickAndWait(Locator.linkWithText("COMPLETE"));
        clickButton("Data");
        assertTextPresent("G3BTA6P01BEVU9", "G3BTA6P01BD5P9");
    }

    //importing the same thing again should fail
    //Issue 13695
    private void importRunAgainTest()
    {
//        log("verify we can't import the same run twice");
//        goToProjectHome();
//        startImportRun("/reads.txt", "Import Reads");
//        waitForText("ERROR");

    }

    private void runAnalysisTest()
    {

        if(!isGroupConcatSupported())
            return;
//        getToRunScreen();
        sendDataToGalaxyServer();
        receiveDataFromGalaxyServer();
    }

    private void getToRunScreen()
    {
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("View Runs"));
        clickRunLink(first454importNum);
    }

    private void clickRunLink(String runId)
    {
        DataRegionTable dr = new DataRegionTable("Runs", this);
        int rowNum = dr.getRow("runs", runId);
        String rowId = dr.getColumnDataAsText("Run").get(rowNum);
        clickAndWait(Locator.linkWithText(rowId));
    }

    private void verifyAnalysis()
    {
        if(!isGroupConcatSupported())
            return;
        goToProjectHome();

        clickAndWait(Locator.linkWithText("View Analyses"));
        clickAndWait(Locator.linkWithText("" + getRunNumber()));  // TODO: This is probably still too permissive... need a more specific way to get the run link

        assertTextPresent("Reads", "Sample Id", "Percent");

        assertTextPresent("TEST09");
//        assertTextPresent("TEST14", 2);
        assertTextPresent("1 - 100 of 1,410");
        startAlterMatches();
        deleteMatchesTest();
        alterMatchesTest();

    }

    private void deleteMatchesTest()
    {

        String[] alleleContentsBeforeDeletion = (String[]) drt.getColumnDataAsText("Allele Name").toArray(new String[] {"a"});


        //attempt to delete a row and cancel
        click(Locator.name(checkboxId, 2));
//        click(Locator.name(checkboxId, 15));

        selenium.chooseCancelOnNextConfirmation();
        clickButton("Delete", 0);
        selenium.getConfirmation();
        assertTextPresent(getExpectedAnalysisCount());


        //delete some rows
        selenium.chooseOkOnNextConfirmation();
        clickButton("Delete", 0);
        selenium.getConfirmation();

        waitForPageToLoad();
        waitForText("1 match was deleted.");

    }

    private void alterMatchesTest()
    {
        sleep(5000);
        String expectedNewAlleles = "Mafa-A1*063:03:01, Mafa-A1*063:01";
//        System.out.println(countText(expectedNewAlleles));
//        assertTextNotPresent(expectedNewAlleles);

        //combine two samples
        click(Locator.name(checkboxId, 0));
        click(Locator.name(checkboxId, 1));
        clickButton("Combine", 0);
        waitForExtMask();

        /*verify the list is what we expct.  Because the two samples had the following lists
        * WE expect them to combine to the following:
         */
        String[] alleles = {"Mamu-A1*004:01:01", "Mamu-A1*004:01:02"};
        for(String allele: alleles)
        {
            Locator.XPathLocator l =  Locator.tagWithText("div", allele);
            isElementPresent(l);
            Assert.assertEquals(1, getXpathCount(l));
        }

        //click some checkboxes
//        mouseDown(Locator.tagContainingText("div", alleles[1]));
//        mouseDown(Locator.tagContainingText("div", alleles[2]));

        //combine some but not all of the matches
        _extHelper.clickXGridPanelCheckbox(0, true);
        clickButtonContainingText("Combine", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        refresh();

        int newIdIndex = getCombinedSampleRowIndex();
        Assert.assertEquals("19", drt.getDataAsText(newIdIndex, "Reads") );
        Assert.assertEquals("7.3%", drt.getDataAsText(newIdIndex, "Percent") );
        Assert.assertEquals("300.0", drt.getDataAsText(newIdIndex,"Average Length") );
        Assert.assertEquals("14", drt.getDataAsText(newIdIndex, "Pos Reads") );
        Assert.assertEquals("5", drt.getDataAsText(newIdIndex, "Neg Reads") );
        Assert.assertEquals("0", drt.getDataAsText(newIdIndex, "Pos Ext Reads") );
        Assert.assertEquals("0", drt.getDataAsText(newIdIndex, "Neg Ext Reads") );
//        String[] allelesAfterMerge = drt.getDataAsText(newIdIndex, "Allele Name").replace(" ", "").split(",") ;
//        Assert.assertEquals(1,allelesAfterMerge.length);
        assertTextPresent(alleles[0]);
    }

    protected int getCombinedSampleRowIndex()
    {
        String xpath  = "//table[@id='dataregion_Analysis']/tbody/tr";
        Locator l = null;
        int index = 0;
        String goalClass = "labkey-error-row";
        for(index = 0; index<50; index++)
        {
            l = Locator.xpath(xpath + "[" + (index+5) + "]");    //the first four rows are invisible spacers and never contain data.
            if(getAttribute(l, "class").equals(goalClass))
                break;
        }
        return index;
    }

    /**
     * enable altering of matches and verify expected changes
     * precondition:  already at analysis page
     */
    private void startAlterMatches()
    {
       clickButton("Alter Matches");

        for(String buttonText : new String[] {"Stop Altering Matches", "Combine", "Delete"})
        {
            assertElementPresent(Locator.xpath("//a[contains(@class,'button')]/span[text()='" + buttonText + "']"));
        }

        drt = new DataRegionTable( "Analysis", this);
    }

    private void receiveDataFromGalaxyServer()
    {
        String[] filesToCopy = {"matches.txt", "analysis_complete.txt"};
        String analysisFolder = "analysis_" + getRunNumber();
        for(String file: filesToCopy)
        {
            copyFile(pipelineLoc + "/" + file, pipelineLoc + "/" + analysisFolder + "/" + file);
        }
        refresh();
        waitForPipelineJobsToComplete(++pipelineJobCount, "Import genotyping analysis", false);
    }

    private int getRunNumber()
    {
        return runNum;
    }

    private void sendDataToGalaxyServer()
    {
        clickButton("Add Analysis");
        Locator menuLocator = Locator.xpath("//input[@name='sequencesView']/../input[2]");
        _extHelper.clickExtDropDownMenu(menuLocator, "[default]");                       //TODO:  this should be cyno
        clickButton("Submit");
        waitForPipelineJobsToComplete(++pipelineJobCount, "Submit genotyping analysis", false);
        findAndSetAnalysisNumber();

    }

    private void findAndSetAnalysisNumber()
    {
        Locator l = Locator.tagContainingText("td", "Submit genotyping analysis");
        isElementPresent(l);
        getText(l);
        String[] temp = getText(l).split(" ");
        setAnalysisNumber(Integer.parseInt(temp[temp.length-1]));

    }

    private void setAnalysisNumber(int i)
    {
        runNum = i;
    }

    private void importIlluminaRunTest()
    {
        log("import illumina run");
        startImportIlluminaRun("IlluminaSamples.csv", "Import Illumina Reads");
        waitForPipelineJobsToComplete(++pipelineJobCount, "Import Run", false);
        assertTextNotPresent("ERROR");

        goToProjectHome();
        clickAndWait(Locator.linkWithText("View Runs"));
        clickRunLink(illuminaImportNum);

        verifyIlluminaSamples();
    }

    private void verifyIlluminaExport() throws Exception
    {
        log("Verifying FASTQ and ZIP export");

        String url = WebTestHelper.getBaseURL() + "/genotyping/" + getProjectName() + "/mergeFastqFiles.view";
        HttpClient httpClient = WebTestHelper.getHttpClient();
        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpPost method = null;
        HttpResponse response = null;

        try
        {
            ExecuteSqlCommand cmd = new ExecuteSqlCommand("genotyping", "SELECT s.* from genotyping.SequenceFiles s LEFT JOIN (select max(rowid) as rowid from genotyping.Runs r WHERE platform = 'Illumina' group by rowid) r ON r.rowid = s.run");
            Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

            SelectRowsResponse resp = cmd.execute(cn, getProjectName());
            Assert.assertTrue("Wrong number of files found.  Expected 30, found " + resp.getRows().size(), resp.getRows().size() == 30);

            //first try FASTQ merge
            method = new HttpPost(url);
            List<NameValuePair> args = new ArrayList<>();
            for (Map<String, Object> row : resp.getRows())
            {
                args.add(new BasicNameValuePair("dataIds", row.get("DataId").toString()));
            }

            args.add(new BasicNameValuePair("zipFileName", "genotypingExport"));

            method.setEntity(new UrlEncodedFormEntity(args));
            response = httpClient.execute(method, context);
            int status = response.getStatusLine().getStatusCode();
            Assert.assertTrue("FASTQ was not Downloaded", status == HttpStatus.SC_OK);
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

                int expectedLength = 1088;
                Assert.assertTrue("Length of file doesnt match expected value of "+expectedLength+", was: " + count, count == expectedLength);

                EntityUtils.consume(response.getEntity());
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

            //then ZIP export
            url = WebTestHelper.getBaseURL() + "/experiment/" + getProjectName() + "/exportFiles.view";
            httpClient = WebTestHelper.getHttpClient();

            method = new HttpPost(url);
            args = new ArrayList<>();
            for (Map<String, Object> row : resp.getRows())
            {
                args.add(new BasicNameValuePair("dataIds", row.get("DataId").toString()));
            }

            args.add(new BasicNameValuePair("zipFileName", "genotypingZipExport"));
            method.setEntity(new UrlEncodedFormEntity(args));
            response = httpClient.execute(method, context);
            status = response.getStatusLine().getStatusCode();
            Assert.assertEquals("Status code was incorrect", HttpStatus.SC_OK, status);
            Assert.assertEquals("Response header incorrect", "attachment; filename=\"genotypingZipExport\"", response.getHeaders("Content-Disposition")[0].getValue());
            Assert.assertEquals("Response header incorrect", "application/zip;charset=UTF-8", response.getHeaders("Content-Type")[0].getValue());
        }
        finally
        {
            if (null != response)
                EntityUtils.consume(response.getEntity());
            if (httpClient != null)
                httpClient.getConnectionManager().shutdown();
        }
    }

    private void verifyIlluminaSampleSheet()
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Samples"));
        waitForPageToLoad();
        DataRegionTable d = new DataRegionTable("query", this);
        String viewName = "Yellow Peas";
        createCusomizedView(viewName, new String[]{"Created"}, new String[] {"fivemid"});
        d.checkAllOnPage();
        clickButton("Create Illumina Sample Sheet");
        waitForPageToLoad();
        waitForText("Reagent Cassette Id");
        Ext4FieldRef.getForLabel(this, "Reagent Cassette Id").setValue("FlowCell");

        String[][] fieldPairs = {
            {"Investigator Name", "Investigator"},
            {"Experiment Number", "Experiment"},
            {"Project Name", "Project"},
            {"Description", "Description"}
        };

        for (String[] a : fieldPairs)
        {
            Ext4FieldRef.getForLabel(this, a[0]).setValue(a[1]);
        }

        _ext4Helper.clickTabContainingText("Preview Header");
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
        Ext4FieldRef textarea = _ext4Helper.queryOne("textarea[itemId='sourceField']", Ext4FieldRef.class);
        String newValue = prop_name + "," + prop_value;
        textarea.eval("this.setValue(this.getValue() + \"\\\\n" + newValue + "\")");
        clickButton("Done Editing", 0);

        assertTextPresent("Warning: Sample indexes do not support both color channels at each position. See Preview Samples tab for more information.");

        //verify template has changed
        _ext4Helper.clickTabContainingText("General Info");
        Assert.assertEquals("Custom", Ext4FieldRef.getForLabel(this, "Template").getValue());

        //set custom view
        _ext4Helper.selectComboBoxItem("Custom View:", viewName);

        //verify values persisted
        _ext4Helper.clickTabContainingText("Preview Header");
        waitForText("Edit Sheet");
        Assert.assertEquals(prop_value, Ext4FieldRef.getForLabel(this, prop_name).getValue());

        //save template
        clickButton("Save As Template", 0);
        waitForElement(Ext4Helper.ext4Window("Choose Name"));
        Ext4FieldRef textfield = _ext4Helper.queryOne("textfield", Ext4FieldRef.class);
        textfield.setValue(TEMPLATE_NAME);
        clickButton("OK", 0);
        _ext4Helper.clickTabContainingText("General Info");
        Assert.assertEquals(TEMPLATE_NAME, Ext4FieldRef.getForLabel(this, "Template").getValue());

        //if we navigate too quickly, before the insertRows has returned, the test can get a JS error
        //therefore we sleep
        sleep(200);

        //verify samples present
        _ext4Helper.clickTabContainingText("Preview Samples");
        waitForText("Sample Name");

        int expectRows =  966; //(16 * (49 +  1)) + 16;  //11 cols, 45 rows, plus header and validation row (which is only 8 cols)
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
        Ext4FieldRef combo = Ext4FieldRef.getForLabel(this, "Template");
        combo.setValue(TEMPLATE_NAME);

        Integer count = Integer.parseInt(combo.eval("this.store.getCount()"));
        Assert.assertTrue("Combo store does not have correct record number", 3 == count);
        sleep(50);
        Assert.assertEquals("Field value not set correctly", TEMPLATE_NAME, Ext4FieldRef.getForLabel(this, "Template").getValue());
        _ext4Helper.clickTabContainingText("Preview Header");
        waitForText("Edit Sheet");
        Assert.assertEquals(prop_value, Ext4FieldRef.getForLabel(this, prop_name).getValue());

        clickButton("Download");

        for (String[] a : fieldPairs)
        {
            assertTextPresent(a[0] + "," + a[1]);
        }

        assertTextPresent(prop_name + "," + prop_value);
        goToHome();
        goToProjectHome();
    }

    private void createCusomizedView(String viewName, String[] columnsToAdd, String[] columnsToRemove )
    {
        _customizeViewsHelper.openCustomizeViewPanel();

        for(String column : columnsToAdd)
        {
            _customizeViewsHelper.addCustomizeViewColumn(column);
        }

        for(String column : columnsToRemove)
        {
            _customizeViewsHelper.removeCustomizeViewColumn(column);
        }

        _customizeViewsHelper.saveCustomView(viewName);
    }

    private void assertExportButtonPresent()
    {
        String xpath =  "//a[contains(@class, 'disabled-button')]/span[text()='Download Selected']";
        assertElementPresent(Locator.xpath(xpath));

        xpath = xpath.replace("disabled", "labkey");
        click(Locator.name(checkboxId, 2));
        click(Locator.name(checkboxId, 3));
        click(Locator.name(checkboxId, 9));
        Locator exportButton = Locator.xpath(xpath);

        click(exportButton);
        waitForText("Export Files");
        assertTextPresent("ZIP Archive", "Merge");
        clickButtonContainingText("Cancel", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);

    }

    private void importRunTest()
    {
        log("import genotyping run");
        startImportRun("reads.txt", "Import 454 Reads", first454importNum);
        waitForPipelineJobsToComplete(++pipelineJobCount, "Import Run", false);

        goToProjectHome();
        clickAndWait(Locator.linkWithText("View Runs"));
        clickRunLink(first454importNum);

        verifySamples();
    }


    private class OutputFilter implements FilenameFilter
    {
        public boolean accept(File dir, String name)
        {
            return name.startsWith("IlluminaSamples-");
        }
    }
    private void verifyIlluminaSamples()
    {
        assertExportButtonPresent();
        File dir = new File(pipelineLoc);
        FilenameFilter filter = new OutputFilter();
        File[] files = dir.listFiles(filter);

        Assert.assertEquals(30, files.length);
        DataRegionTable d = new DataRegionTable("Reads", this);
        Assert.assertEquals(d.getDataRowCount(), 30);
        assertTextPresent("Read Count");
        Assert.assertEquals("9", d.getDataAsText(d.getIndexWhereDataAppears("IlluminaSamples-R1-4947.fastq.gz", "Filename") + 1, "Read Count"));
    }

    private void verifySamples()
    {
        waitForTextWithRefresh("1 - 100 of 9,411", defaultWaitForPage);
        assertTextPresent("Name", "Sample Id", "Sequence", "G3BT");
//        String indexSampleSequence[][] = {{"4", "TEST14", "tcagtgtcacacgaGTGGCTACGTGGACGACCGTATCGCCTCCTGCGGAGATCATCGTGTGACActgagcgggctggcaaggcgcatag"}};
//        DataRegionTable drt = new DataRegionTable("Reads", this);
//
//        for(int i=0; i<indexSampleSequence.length; i++)
//        {
//            int index = Integer.parseInt(indexSampleSequence[i][0]);
//            Assert.assertEquals(indexSampleSequence[i][1], drt.getDataAsText(index, "Sample Id"));
//            Assert.assertEquals(indexSampleSequence[i][2], drt.getDataAsText(index, "Sequence"));
//        }

    }

    private void startImportRun(String file, String importAction, String associatedRun)
    {
        clickAndWait(Locator.linkContainingText("Import Run"));
        _extHelper.selectFileBrowserItem(file);

        selectImportDataAction(importAction);
        setFormElement(Locator.name("run"), associatedRun);
        clickButton("Import Reads");

    }

    private void startImportIlluminaRun(String file, String importAction)
    {
        clickAndWait(Locator.linkContainingText("Import Run"));
        _extHelper.selectFileBrowserItem(file);

        selectImportDataAction(importAction);

        setFormElement(Locator.name("run"), illuminaImportNum);
        setFormElement(Locator.name("prefix"), "Illumina-");
        clickButton("Import Reads");

    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        File dir = new File(pipelineLoc);
        File[] files = dir.listFiles();
        for(File file: files)
        {
            if(file.isDirectory() && file.getName().startsWith("analysis_"))
                deleteDir(file);
            if(file.getName().startsWith("import_reads_"))
                file.delete();
            if(file.getName().startsWith("IlluminaSamples-"))
                file.delete();
        }

        files = new File(pipelineLoc + "/secondRead").listFiles();

        if(files != null)
        {
            for(File file: files)
            {
                if(!file.getName().equals("reads.txt"))
                    file.delete();
            }
        }

        deleteTemplateRow(afterTest);
        deleteProject(getProjectName(), afterTest);

//        deleteDir(new File(pipelineLoc + "\\analysis_" + getRunNumber()));
//        deleteDir(new File(pipelineLoc + "\\analysis_" + (getRunNumber()-1)));
    }

    private void deleteTemplateRow(boolean failOnError)
    {
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        DeleteRowsCommand cmd = new DeleteRowsCommand("genotyping", "IlluminaTemplates");
        cmd.addRow(Collections.singletonMap("Name", (Object) TEMPLATE_NAME));
        SaveRowsResponse resp;
        try
        {
            resp = cmd.execute(cn, getProjectName());
        }
        catch (Exception ex)
        {
            if (failOnError)
                throw new RuntimeException(ex);
            else
            {
                log("Template rows not deleted. Nothing to be deleted.");
                return;
            }
        }
        log("Template rows deleted: " + resp.getRowsAffected());
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/genotyping";
    }

}
