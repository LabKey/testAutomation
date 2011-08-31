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

import org.labkey.test.Locator;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 8/16/11
 * Time: 3:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class StudyRedesignTest extends StudyBaseTest
{

    protected String[] someDataSets = {"Dataset Browse","AE-1:(VTN) AE Log", "MV-1: Missed Visit", "RCF-1: Reactogenicity-Day 2"};
    private static final String DATA_BROWSE_TABLE_NAME = "";

    @Override
    protected void doCreateSteps()
    {

        importStudy();
        startSpecimenImport(2);

        // wait for study and specimens to finish loading
        waitForPipelineJobsToComplete(1, "study import", false);
        waitForSpecimenImport();
        setStudyRedesign();
    }

    @Override
    protected void doVerifySteps()
    {
        datasetBrowserWebPartTest();
    }

    private static final String datasetBrowse = "Dataset Browse (Experimental)";

    private void datasetBrowserWebPartTest()
    {
        log("Dataset Browser test");
        clickLinkContainingText("Clinical and Assay Data");
        addWebPart(datasetBrowse);
        waitForText(someDataSets[3]);
        assertTextPresent("Dataset Browse", "Name", "Type", "Access");

        assertDataDisplayedAlphabetically();

        //TODO:  waiting on hypermove fix
//        datasetBrowseClickDataTest();


    }


    private void datasetBrowseClickDataTest()
    {

        log("Test click behavior for datasets");

        Object[][] vals = getDPDataOnClick();

        for(Object[] val : vals)
        {
            clickSingleDataSet((String) val[0], (String) val[1], (String) val[2], true);

        }
    }

    private Object[][] getDPDataOnClick()
    {
        //TODO when hypermove fixed
        Object[][] ret = {
                {"AE-1:(VTN) AE Log", "", ""},
        };
        return ret;  //To change body of created methods use File | Settings | File Templates.
    }

    private void clickSingleDataSet(String title, String source, String type, boolean testDelete)
    {
        Locator l = Locator.tagWithText("div", title);
        click(l);
        assertTextPresentInThisOrder(title, "Source: " + source, "Type: " + type);
        clickButtonContainingText("View");
        assertTextPresent(title);
        selenium.goBack();
        if(testDelete)
        {
            //this feature is scheduled for removal
        }
    }

     private void clickSingleDataSet(String title, String source, String type)
    {
       clickSingleDataSet(title, source, type, false);
    }

    //Issue 12914: dataset browse web part not displaying data sets alphabetically
    private void assertDataDisplayedAlphabetically()
    {
      //ideally we'd grab the test names from the normal dataset part, but that would take a lot of time to write
        assertTextPresentInThisOrder(someDataSets);
    }
}
