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

import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;

import java.io.File;

public class CAVDStudyTest extends StudyBaseTest
{
    private static final String PROJECT_NAME = "CAVDStudyTest";
    private static final String STUDY_NAME = "CAVDStudyTest Study";

    @Override
    protected void doCreateSteps()
    {
        createProject("CAVDStudyTest", "CAVD Study");
    }

    @Override
    protected void  doVerifySteps()
    {
        doVerifyEmptyStudy();
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    private void doVerifyEmptyStudy()
    {
        log("Verifying that the study is empty.");
        // Make sure a study was made.
        assertTextNotPresent("No study is active in the current container.");
        assertTextPresent(STUDY_NAME + " tracks data in");

        // Make sure Data tab is hidden
        assertTabNotPresent("Data");

        // Change timepoint type.
        clickLinkWithText("Edit");
        waitForText("Timepoint Type");
        checkRadioButton("TimepointType", "VISIT");
        clickButton("Submit");
        waitForPageToLoad();

        //Check to see if date is checked.
        clickLinkWithText("Edit");
        waitForText("Timepoint Type");
        assertRadioButtonSelected("TimepointType", "VISIT");

        addDataset();

        clickLinkWithText("Overview");

        assertTabPresent("Data");

        clickLinkWithText("Edit");

        waitForText("Timepoint Type");
        assertEquals(2, getXpathCount(Locator.xpath("//input[@type='radio'][@name='TimepointType'][@disabled='']")));
    }

    private void addDataset()
    {
        clickLinkWithText("Manage");
        clickLinkWithText("Study Schedule");

        log("adding dataset: " + "ImportedDataset");

        clickButton("Add Dataset", 0);
        waitForElement(Locator.xpath("//span[text() = 'New Dataset']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//label[text() = 'Name:']/..//input"), "ImportedDataset");


        click(Locator.ext4Radio("Import data from file"));
        clickNavButton("Next");

        String datasetFileName = getSampleDataPath() + "/datasets/plate001.tsv";
        File file = new File(WebTestHelper.getLabKeyRoot(), datasetFileName);

        if (file.exists())
        {
            Locator fileUpload = Locator.xpath("//input[@name = 'uploadFormElement']");
            waitForElement(fileUpload, WAIT_FOR_JAVASCRIPT);
            setFormElement(fileUpload, file.getAbsolutePath());

            waitForElement(Locator.xpath("//div[@class = 'gwt-HTML' and contains(text(), 'Showing first 5 rows')]"), WAIT_FOR_JAVASCRIPT);
            clickNavButton("Import");
        }
        else
            fail("The dataset import .tsv file (plate001.tsv) does not exist");
    }
}
