/*
 * Copyright (c) 2011-2012 LabKey Corporation
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

import org.labkey.remoteapi.assay.ExpObject;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.PostgresOnlyTest;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DecimalFormat;

/**
 * User: elvan
 * Date: 10/22/11
 * Time: 7:14 PM
 */
public class GenotypingTest extends BaseSeleniumWebTest implements PostgresOnlyTest
{
    public static final String first454importNum = "207";
    public static final String second454importNum = "208";
    public static final String illuminaImportNum = "206";
    protected int pipelineJobCount=1;

    String pipelineLoc =  getLabKeyRoot() + "/sampledata/genotyping";
    protected int runNum = 0; //this is globally unique, so we need to retrieve it every time.
    protected String checkboxId = ".select";
//    private String expectedAnalysisCount = "1 - 61 of 61";

    DataRegionTable drt = null;
    private String samples = "samples";

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

    public void setUp2()
    {
        createProject(getProjectName(), "Genotyping");
        setUpLists();
        configureAdmin();
        clickLinkContainingText(getProjectName());
        setPipelineRoot(pipelineLoc);
    }

    //pre-
    private void setUpLists()
    {
        log("Import genotyping list");
        clickLinkContainingText(getProjectName());
        clickLinkContainingText("manage lists");

        clickButton("Import List Archive");
//        sleep(500);
        setFormElement("listZip", new File(pipelineLoc, "sequencing.lists.zip"));
        clickButton("Import List Archive");

        assertTextPresent(
                samples,
                "mids",
                "sequences",
                "runs"
            );

    }

    private void configureAdmin()
    {
        clickLinkContainingText(getProjectName());
        waitForPageToLoad();
        clickLinkContainingText("Admin");

        String[] listVals = {"sequences", "runs", samples};
        for(int i=0; i<3; i++)
        {
            clickLinkContainingText("configure",i, false);
            waitForExtMask();
            ExtHelper.clickExtDropDownMenu(this, "userQuery_schema", "lists");
            ExtHelper.clickExtDropDownMenu(this, "userQuery_query", listVals[i]);
            ExtHelper.clickExtDropDownMenu(this, "userQuery_view", "[default view]");
            clickButton("Submit", 0);
            waitForExtMaskToDisappear();
        }
        setFormElement(Locator.name("galaxyURL"), "http://galaxy.labkey.org:8080");
        clickButton("Submit");
        clickButton("Load Sequences");

        log("Configure Galaxy Server Key");
        clickLinkWithText("My Settings");
        setFormElement("galaxyKey", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        clickButton("Submit");
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setUp2();
        goToProjectHome();


        //TODO: need to fix 454/genotyping tests
        importRunTest();
////        importRunAgainTest(); //bug Issue 13695
        runAnalysisTest();
        importSecondRunTest();
        goToProjectHome();
        importIlluminaRunTest();

    }

    private void importSecondRunTest()
    {
        goToProjectHome();
        startImportRun("secondRead/reads.txt", "Import 454 Reads", second454importNum);
        waitForPipelineJobsToComplete(pipelineJobCount++, "Import reads for 206", true);
        clickLinkWithText("COMPLETE");
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
//        getToRunScreen();
        sendDataToGalaxyServer();
        receiveDataFromGalaxyServer();
        pipelineJobCount+=2;
        verifyAnalysis();


    }

    private void getToRunScreen()
    {
        clickLinkWithText(getProjectName());
        clickLinkWithText("View Runs");
        clickLinkWithText(first454importNum);

    }

    private void verifyAnalysis()
    {
        goToProjectHome();

        clickLinkWithText("View Analyses");
        clickLinkWithText("" + getRunNumber());  // TODO: This is probably still too permissive... need a more specific way to get the run link

        assertTextPresent("Reads", "Sample Id", "Percent");

        waitForTextWithRefresh("TEST09", 60000);
//        assertTextPresent("TEST14", 2);
        waitForTextWithRefresh("1 - 100 of 1,410", 15000);
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

        sleep(500);
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
            Locator l =  Locator.tagWithText("div", allele);
            isElementPresent(l);
            assertEquals(1, getXpathCount(Locator.xpath(l.toXpath())));
        }

        //click some checkboxes
//        mouseDown(Locator.tagContainingText("div", alleles[1]));
//        mouseDown(Locator.tagContainingText("div", alleles[2]));

        //combine some but not all of the matches
        ExtHelper.clickXGridPanelCheckbox(this, 0, true);
        clickButtonContainingText("Combine", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        refresh();

        int newIdIndex = getCombinedSampleRowIndex();
        assertEquals("19", drt.getDataAsText(newIdIndex, "Reads") );
        assertEquals("7.3%", drt.getDataAsText(newIdIndex, "Percent") );
        assertEquals("300.0", drt.getDataAsText(newIdIndex,"Average Length") );
        assertEquals("14", drt.getDataAsText(newIdIndex, "Pos Reads") );
        assertEquals("5", drt.getDataAsText(newIdIndex, "Neg Reads") );
        assertEquals("0", drt.getDataAsText(newIdIndex, "Pos Ext Reads") );
        assertEquals("0", drt.getDataAsText(newIdIndex, "Neg Ext Reads") );
//        String[] allelesAfterMerge = drt.getDataAsText(newIdIndex, "Allele Name").replace(" ", "").split(",") ;
//        assertEquals(1,allelesAfterMerge.length);
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
        waitForText("Submit genotyping analysis ");
    }

    private int getRunNumber()
    {
        return runNum;
    }

    private void sendDataToGalaxyServer()
    {
        clickButton("Add Analysis");
        Locator menuLocator = Locator.xpath("//input[@name='sequencesView']/../input[2]");
        ExtHelper.clickExtDropDownMenu(this, menuLocator,  "[default]");                       //TODO:  this should be cyno
        clickButton("Submit");
        waitForText("COMPLETE");
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
        waitForPipelineJobsToComplete(pipelineJobCount++, "Import Run", false);
        assertTextNotPresent("ERROR");

        goToProjectHome();
        clickLinkWithText("View Runs");
        clickLinkWithText(illuminaImportNum);

        verifyIlluminaSamples();
    }

    private void assertExportButtonPresent()
    {
        String[] exportTypes = {"Excel 97", "Excel 2007", ".iqy"};
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
        waitForExtMaskToDisappear();

    }

    private void importRunTest()
    {
        log("import genotyping run");
        startImportRun("reads.txt", "Import 454 Reads", first454importNum);
        waitForPipelineJobsToComplete(pipelineJobCount++, "Import Run", false);
//        assertTextNotPresent("IMPORT");
        assertTextNotPresent("ERROR");

        goToProjectHome();
        clickLinkWithText("View Runs");
        clickLinkWithText(first454importNum);

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

        assertEquals(30, files.length);
        DataRegionTable d = new DataRegionTable("Reads", this);
        assertEquals(d.getDataRowCount(), 30);
        assertTextPresent("Read Count");
        assertEquals("9", d.getDataAsText(d.getIndexWhereDataAppears("IlluminaSamples-R1-4947.fastq.gz", "Filename") + 1, "Read Count"));
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
//            assertEquals(indexSampleSequence[i][1], drt.getDataAsText(index, "Sample Id"));
//            assertEquals(indexSampleSequence[i][2], drt.getDataAsText(index, "Sequence"));
//        }

    }

    private void startImportRun(String file, String importAction, String associatedRun)
    {
        clickLinkContainingText("Import Run");
        ExtHelper.selectFileBrowserItem(this, file);

        selectImportDataAction(importAction);
        setFormElement("run", associatedRun);
        clickButton("Import Reads");

    }

    private void startImportIlluminaRun(String file, String importAction)
    {
        clickLinkContainingText("Import Run");
        sleep(1000);
        ExtHelper.selectFileBrowserRoot(this);
        ExtHelper.selectFileBrowserItem(this, file);

        selectImportDataAction(importAction);

        setFormElement("run", illuminaImportNum);
        setFormElement("prefix", "Illumina-");
        clickButton("Import Reads");

    }

    @Override
    protected void doCleanup() throws Exception
    {
        //delete run first, due to issue  ###
//        goToHome();
//        if(isTextPresent(getProjectName()))
//        {
//            clickLinkContainingText(getProjectName());
//
//            if(isTextPresent("View Runs"))
//            {
//                clickLinkContainingText("View Runs");
//                click(Locator.name(".select"));
//                clickButton("Delete");
//                getConfirmationAndWait();
//            }
//        }
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
        deleteProject(getProjectName());

//        deleteDir(new File(pipelineLoc + "\\analysis_" + getRunNumber()));
//        deleteDir(new File(pipelineLoc + "\\analysis_" + (getRunNumber()-1)));
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
