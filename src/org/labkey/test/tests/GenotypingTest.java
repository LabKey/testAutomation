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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 10/22/11
 * Time: 7:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class GenotypingTest extends BaseSeleniumWebTest
{
    String pipelineLoc =  getLabKeyRoot() + "/sampledata/NOCHECKIN";
    protected int runNum = 0; //this is globally unique, so we need to retrieve it every time.
    protected String checkboxId = ".select";
//    private String expectedAnalysisCount = "1 - 61 of 61";

        DataRegionTable drt = null;

    @Override
    protected String getProjectName()
    {
        return "Genotyping Verify Project";
    }

    protected int analysisCount = 61;
    protected String getExpectedAnalysisCount()
    {
        return "1 - " + analysisCount + " of " + analysisCount;
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
        setFormElement("listZip", new File(pipelineLoc, "sequences.lists.zip"));
        clickButton("Import List Archive");

        //get the second list set
        //TODO:  integrate these, or at least label the second one better
        clickButton("Import List Archive");
        setFormElement("listZip", new File(pipelineLoc, "genotyping_2011-10-21_11-45-58.lists.zip"));
        clickButton("Import List Archive");

        assertTextPresent("cohortList","emPCR",
                            "jrApplications",
                            "jrControlMetrics",
                            "jrLibraries",
                            "jrLibraryDesign",
                            "jrMetrics",
                            "jrReadLength",
                            "jrRuns",
                            "jrRunsOld",
                            "jrUsers",
                            "mids",
                            "origin",
                            "sequences",
                            "species");
    }

    private void configureAdmin()
    {
        clickLinkContainingText(getProjectName());
        clickLinkContainingText("Admin", 2);

        String[] listVals = {"sequences", "jrRuns", "jrLibraryDesign"};
        for(int i=0; i<3; i++)
        {
            clickLinkContainingText("configure",i, false);
            waitForExtMask();
            ExtHelper.clickExtDropDownMenu(this, "userQuery_schema", "lists");
            ExtHelper.clickExtDropDownMenu(this,"userQuery_query", listVals[i]);
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
        clickLinkContainingText(getProjectName());

        importRunTest();
//        importRunAgainTest(); //bug Issue 13695
        runAnalysisTest();
        //To change body of implemented methods use File | Settings | File Templates.

    }

    //importing the same thing again should fail
    private void importRunAgainTest()
    {
        goToProjectHome();
        startImportRun();
//        assertTextNotPresent("ERROR");
        waitForText("ERROR");

    }

    private void runAnalysisTest()
    {
        getToRunScreen();
        sendDataToGalaxyServer();
        receiveDataFromGalaxyServer();
        verifyAnalysis();


    }

    private void getToRunScreen()
    {
        clickLinkWithText(getProjectName());
        clickLinkWithText("View Runs");
        clickLinkWithText("115");

    }

    private void verifyAnalysis()
    {
        goToProjectHome();
        sleep(1000);//todo
        clickLinkContainingText("View Analyses");
        clickLinkContainingText("" + getRunNumber());
        //TODO:  how do I verify that an anlysis looks correct?

        assertTextPresent("Reads", "Sample Id", "Percent");

        waitForTextWithRefresh(getExpectedAnalysisCount(), 20000);
        startAlterMatches();
        deleteMatchesTest();
        alterMatchesTest();
        sleep(500);//TODO

    }

    private void deleteMatchesTest()
    {

        String[] alleleContentsBeforeDeletion = (String[]) drt.getColumnDataAsText("Allele Name").toArray(new String[] {"a"});


        //attempt to delete ar ow and cancel
        click(Locator.name(checkboxId, 10));
        click(Locator.name(checkboxId, 15));

        selenium.chooseCancelOnNextConfirmation();
        clickButton("Delete", 0);
        selenium.getConfirmation();
        assertTextPresent(getExpectedAnalysisCount());


        //delete some rows
        selenium.chooseOkOnNextConfirmation();
        clickButton("Delete", 0);
        selenium.getConfirmation();

        sleep(500);
        waitForText("2 matches were deleted.");
        String[] alleleContentsAfterDeletion = (String[]) drt.getColumnDataAsText("Allele Name").toArray(new String[] {"a"});

        assertEquals("Allele not successfully deleted", alleleContentsBeforeDeletion.length-2, alleleContentsAfterDeletion.length);

        int[][] alleleMatchUps = {{0,0}, {11,10},{17,15}}; //[i][0] is the index of a specific allele before deletion, [i][]1] is location after
        for(int i=0; i<alleleMatchUps.length; i++)
        {
            assertEquals("allele list after deletion not as expected at index: " + alleleMatchUps[i][1], alleleContentsBeforeDeletion[alleleMatchUps[i][0]], alleleContentsAfterDeletion[alleleMatchUps[i][1]]);
        }

        //click some things

    }

    private void alterMatchesTest()
    {
        sleep(5000);
        String expectedNewAlleles = "Mafa-A1*063:03:01, Mafa-A1*063:01";
//        System.out.println(countText(expectedNewAlleles));
//        assertTextNotPresent(expectedNewAlleles);

        //combine two samples
        click(Locator.name(checkboxId, 2));
        click(Locator.name(checkboxId, 3));
        clickButton("Combine", 0);
        waitForExtMask();

        /*verify the list is what we expct.  Because the two samples had the following lists
        * TODO
        * TODO
        * WE expect them to combine to the following:
         */
        String[] alleles = {"Mafa-A1*063:01", "Mafa-A1*063:02", "Mafa-A1*063:03:01", "Mafa-A1*063:03:02"};
        for(String allele: alleles)
        {
            Locator l =  Locator.tagWithText("div", allele);
            isElementPresent(l);
            assertEquals(1, getXpathCount(Locator.xpath(l.toXpath())));
        }

        //click some checkboxes
        mouseDown(Locator.tagContainingText("div", alleles[1]));
        mouseDown(Locator.tagContainingText("div", alleles[2]));

        //combine some but not all of the matches
        ExtHelper.clickXGridPanelCheckbox(this, 0, true);
        ExtHelper.clickXGridPanelCheckbox(this, 2, true);
        clickButtonContainingText("Combine", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        refresh();

        int newIdIndex = getCombinedSampleRowIndex();
        assertEquals("193", drt.getDataAsText(newIdIndex, "Reads") );
        assertEquals("3.4%", drt.getDataAsText(newIdIndex, "Percent") );
        assertEquals("334.0", drt.getDataAsText(newIdIndex,"Average Length") );
        assertEquals("102", drt.getDataAsText(newIdIndex, "Pos Reads") );
        assertEquals("90", drt.getDataAsText(newIdIndex, "Neg Reads") );
        assertEquals("0", drt.getDataAsText(newIdIndex, "Pos Ext Reads") );
        assertEquals("1", drt.getDataAsText(newIdIndex, "Neg Ext Reads") );
        String[] allelesAfterMerge = drt.getDataAsText(newIdIndex, "Allele Name").replace(" ", "").split(",") ;
        assertEquals(2,allelesAfterMerge.length);
        if(allelesAfterMerge[0].equals(alleles[0]))
        {
            assertEquals(alleles[2], allelesAfterMerge[1]);
        }
        else if(allelesAfterMerge[0].equals(alleles[2]))
        {
            assertEquals(alleles[0], allelesAfterMerge[1]);
        }
        else
        {
            fail("unexpected allele names for combined sample id");
        }
//        TODO: verify allelese
//        assertEquals("), expectedNewAlleles);
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
        String analysisFolder = "analysis_" + getRunNumber();          //TODO:  how to get this
        for(String file: filesToCopy)
        {
            copyFile(pipelineLoc + "/" + file, pipelineLoc + "/" + analysisFolder + "/" + file);
        }
        refresh();
        waitForText("genotyping analysis");
        sleep(500); //TODO:  pipeline job
        clickLinkContainingText("COMPLETE");
        assertTextPresent("genotyping analysis");
        waitForText("Submitting genotyping analysis job complete");
        //TODO:  more checks?
        //TODO
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

    private void importRunTest()
    {
        log("import genotyping run");
        startImportRun();
        //TODO:  isn't there a helper for this?
       waitForPipelineJobsToComplete(1, "Import Run", false);
        assertTextNotPresent("IMPORT");
        assertTextNotPresent("ERROR");

        goToProjectHome();
        clickLinkWithText("View Runs");
        clickLinkWithText("115");

        verifySamples();


    }

    private void verifySamples()
    {
        String indexSampleSequence[][] = {{"3", "LK200", "CTACGACTGCGTGGGCTACGTGGACGACACGCAGTTCGTGCGGTTCGACAGCGACGCCGAGAGCCAGAGGATGGAGCCGCGGGCGCCGTGGGTGGAGCAGGAGGGTCCGGAGTATTGGGACCGGAGCACACGGTACATGAAGACCGAGACACAGAATGCCCCAGTGAACCTGCGGAACCTGCGCGGCTACTACAACCAGAGCGAGGCCGGGTCTCACACCATCCAGAAGATGTACGGCTGCGACCTGGGGCCCGACGGGCGCCTCCTCCGCGGGTATGACCAGCACGCCTACGACGGCAAGGATTACATCGCCCTGAACCAGGACCTGCGCTCCTGGACCGCCGCGGACATGGCGGCTCAGAACACCCAGCGGAAGTGGGAGGCGGCGGATGTGGCGGAGAGGATGAGAGCCTACCTGGAGGGGACGTGCCTGGAGTGGCTCCGGAGACACCTGGAGAACGGGAAGGAGACACTGCAGCGCTTGGACCCCCCAAGACACATGTGACCCACCACCCCGT"}};
        DataRegionTable drt = new DataRegionTable("Reads", this);

        for(int i=0; i<indexSampleSequence.length; i++)
        {
            int index = Integer.parseInt(indexSampleSequence[i][0]);
            assertEquals(indexSampleSequence[i][1], drt.getDataAsText(index, "Sample Id"));
            assertEquals(indexSampleSequence[i][2], drt.getDataAsText(index, "Sequence"));
        }

    }

    private void startImportRun()
    {
        clickLinkContainingText("Import Run");
        ExtHelper.clickFileBrowserFileCheckbox(this, "reads.txt");

        selectImportDataAction("Import Reads");
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
            deleteProject(getProjectName());
        deleteDir(new File("C:\\labkey_base\\sampledata\\NOCHECKIN\\analysis_" + getRunNumber()));
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
