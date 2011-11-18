/*
 * Copyright (c) 2011 LabKey Corporation
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

    @Override
    protected String getProjectName()
    {
        return "Genotyping Verify Project";
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
        setFileValue("listZip", pipelineLoc + "/sequences.lists.zip"  );
        clickButton("Import List Archive");

        //get the second list set
        //TODO:  integrate these, or at least label the second one better
        clickButton("Import List Archive");
        setFileValue("listZip", pipelineLoc + "/genotyping_2011-10-21_11-45-58.lists.zip"  );
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
        runAnalysisTest();
        //To change body of implemented methods use File | Settings | File Templates.
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
        //TODO
    }

    private void receiveDataFromGalaxyServer()
    {
        String[] filesToCopy = {"matches.txt", "analysis_complete.txt"};
        String analysisFolder = "analysis_12";          //TODO:  how to get this
        for(String file: filesToCopy)
        {
            copyFile(pipelineLoc + "/" + file, pipelineLoc + "/" + analysisFolder + "/" + file);
        }
        refresh();
        waitForText("Import genotyping analysis");
        sleep(500); //TODO:  pipeline job
        clickLinkContainingText("COMPLETE");
        assertTextPresent("Import genotyping analysis");
        clickButton("Data");
        //TODO:  more checks?
        //TODO
    }

    private void sendDataToGalaxyServer()
    {
        clickButton("Add Analysis");
        Locator menuLocator = Locator.xpath("//input[@name='sequencesView']/../input[2]");
        ExtHelper.clickExtDropDownMenu(this, menuLocator,  "[default]");                       //TODO:  this should be cyno
        clickButton("Submit");
        waitForText("COMPLETE");
        //T

    }

    private void importRunTest()
    {
        log("import genotyping run");
        clickLinkContainingText("Import Run");
        ExtHelper.clickFileBrowserFileCheckbox(this, "reads.txt");

        selectImportDataAction("Import Reads");
        clickButton("Import Reads");

        //TODO:  isn't there a helper for this?
       waitForPipelineJobsToComplete(1, "Import Run", false);
        assertTextNotPresent("IMPORT");
        assertTextNotPresent("ERROR");
        log("TODO:  remove this");
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
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
