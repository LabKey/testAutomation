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
package org.labkey.test.tests.study;

import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.tests.StudyBaseTest;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ListHelper;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: Feb 24, 2012
 */
public class StudyScheduleTester
{
    // dataset names
    private static final String IMPORT_DATASET = "ImportDataset";
    private static final String MANUAL_DATASET = "ManualDataset";
    private static final String GHOST_DATASET_1 = "GhostDataset1";
    private static final String GHOST_DATASET_2 = "GhostDataset2";
    private static final String GHOST_DATASET_3 = "GhostDataset3";
    private static final String GHOST_DATASET_4 = "GhostDataset4";
    private static final String GHOST_DATASET_5 = "GhostDataset5";
    private static final String GHOST_DATASET_6 = "GhostDataset6";

    // categories
    private static final String GHOST_CATEGORY = "GhostCategory";
    private static final String ASSAY_CATEGORY = "Assay Data";

    private enum DatasetType {
        importFromFile,
        defineManually,
        placeholder,
        linkeToExisting,
    }

    private StudyBaseTest _test;
    private String _folderName;
    private String _sampleDataPath;

    public StudyScheduleTester(StudyBaseTest test, String folderName, String sampleDataPath)
    {
        _test = test;
        _folderName = folderName;
        _sampleDataPath = sampleDataPath;
    }

    public void basicTest()
    {
        _test.log("Study Schedule Test");
        String dataset = "PRE-1: Pre-Existing Conditions";
        String visit = "Screening Cycle";

        // check required timepoints
        goToStudySchedule();
//        getXpathCount(Locator.xpath("//div[./span[@class='x4-column-header-text']]//div[text()='" + visit +"']"));
        _test.assertElementPresent(Locator.xpath("//div[@data-qtip='" + dataset + "']//..//..//..//td[3]//div[@class='checked']"));

        // change a required visit to optional
        _test.clickWebpartMenuItem("Study Schedule", "Manage Visits");
        _test.clickAndWait(Locator.xpath("//table[@id='visits']//tr[./th[text() = '" + visit + "']]/td/a[text() = 'edit']"));
        _test.selectOption("dataSetStatus", 2, "OPTIONAL");
        _test.clickNavButton("Save");

        // verify that change is noted in schedule
        goToStudySchedule();
        _test.assertElementPresent(Locator.xpath("//div[@data-qtip='" + dataset + "']//..//..//..//td[3]//div[@class='unchecked']"));

        // revert change
        _test.clickWebpartMenuItem("Study Schedule", "Manage Visits");
        _test.clickAndWait(Locator.xpath("//table[@id='visits']//tr[./th[text() = '" + visit + "']]/td/a[text() = 'edit']"));
        _test.selectOption("dataSetStatus", 2, "REQUIRED");
        _test.clickNavButton("Save");

        // verify dataset 'data' link
        goToStudySchedule();
        _test.click(Locator.xpath("//div[@data-qtip='" + dataset + "']//..//..//..//td[2]//a")); // go to dataset
        _test.waitForText(dataset);

        // test paging
        goToStudySchedule();
//        click(Locator.xpath("//input[@role='checkbox']"));
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'button-next')][1]"), _test.WAIT_FOR_JAVASCRIPT); //wait for next button to appear
        _test.assertTextNotPresent("Cycle 2");
        _test.click(Locator.xpath("//div[contains(@class, 'button-next')][1]")); //click next button
        _test.waitForText("Cycle 2");
        _test.waitForText(dataset);
    }

    public void linkDatasetTest()
    {
        goToStudySchedule();

        // create a dataset from a file import
        addDataset(IMPORT_DATASET, ASSAY_CATEGORY, DatasetType.importFromFile);

        // verify it shows up in the schedule
        _test.waitForElement(Locator.xpath("//div[text()='" + IMPORT_DATASET + "']"), StudyBaseTest.WAIT_FOR_JAVASCRIPT);
        _test.assertElementPresent(Locator.xpath("//div[text() = '" + IMPORT_DATASET + "']/../../../td//img[@alt='dataset']"));

        // click on the dataset link to verify it takes you to the dataset view
        _test.click(Locator.xpath("//div[text() = '" + IMPORT_DATASET + "']/../../../td//a"));
        _test.waitForPageToLoad();
        _test.assertTextPresent("Dataset:", IMPORT_DATASET);
        goToStudySchedule();

        // create a dataset from a manual definition
        addDataset(MANUAL_DATASET, null, DatasetType.defineManually);
        // verify it shows up in the schedule
        _test.waitForElement(Locator.xpath("//div[text()='" + MANUAL_DATASET + "']"), StudyBaseTest.WAIT_FOR_JAVASCRIPT);
        _test.assertElementPresent(Locator.xpath("//div[text() = '" + MANUAL_DATASET + "']/../../../td//img[@alt='dataset']"));

        // create and verify a placeholder datasets
        createPlaceholderDataset(GHOST_DATASET_1, GHOST_CATEGORY, true);
        createPlaceholderDataset(GHOST_DATASET_2, null, false);
        createPlaceholderDataset(GHOST_DATASET_3, GHOST_CATEGORY, false);

        // link the placeholder datasets
        linkDatasetFromSchedule(GHOST_DATASET_1, DatasetType.linkeToExisting, IMPORT_DATASET);

        // verify the expectation dataset gets converted and the existing dataset is gone
        _test.waitForElement(Locator.xpath("//div[text()='" + GHOST_DATASET_1 + "']"), StudyBaseTest.WAIT_FOR_JAVASCRIPT);
        _test.assertElementPresent(Locator.xpath("//div[text() = '" + GHOST_DATASET_1 + "']/../../../td//img[@alt='dataset']"));
        _test.assertElementNotPresent(Locator.xpath("//div[text() = '" + IMPORT_DATASET + "']"));

        // click on the dataset link to verify it takes you to the dataset view
        _test.click(Locator.xpath("//div[text() = '" + GHOST_DATASET_1 + "']/../../../td//a"));
        _test.waitForPageToLoad();
        _test.assertTextPresent("Dataset:", GHOST_DATASET_1);
        goToStudySchedule();

        // link manually
        linkDatasetFromSchedule(GHOST_DATASET_2, DatasetType.defineManually, null);

        // link by importing file
        linkDatasetFromSchedule(GHOST_DATASET_3, DatasetType.importFromFile, null);

        // verify the expectation dataset gets converted and the existing dataset is gone
        _test.waitForElement(Locator.xpath("//div[text()='" + GHOST_DATASET_3 + "']"), StudyBaseTest.WAIT_FOR_JAVASCRIPT);
        _test.assertElementPresent(Locator.xpath("//div[text() = '" + GHOST_DATASET_3 + "']/../../../td//img[@alt='dataset']"));

        // click on the dataset link to verify it takes you to the dataset view
        _test.click(Locator.xpath("//div[text() = '" + GHOST_DATASET_3 + "']/../../../td//a"));
        _test.waitForPageToLoad();
        _test.assertTextPresent("Dataset:", GHOST_DATASET_3);
    }

    public void linkFromDatasetDetailsTest()
    {
        goToStudySchedule();

        // create and verify a placeholder datasets
        createPlaceholderDataset(GHOST_DATASET_4, GHOST_CATEGORY, true);
        createPlaceholderDataset(GHOST_DATASET_5, null, false);
        createPlaceholderDataset(GHOST_DATASET_6, GHOST_CATEGORY, false);

        _test.clickAdminMenuItem("Manage Study");
        _test.clickLinkWithText("Manage Datasets");

        linkDatasetFromDetails(GHOST_DATASET_4, DatasetType.linkeToExisting, GHOST_DATASET_1);
        _test.assertElementPresent(Locator.xpath("//a[text()='" + GHOST_DATASET_4 + "']"));
        _test.assertElementNotPresent(Locator.xpath("//a[text()='" + GHOST_DATASET_1 + "']"));
        linkDatasetFromDetails(GHOST_DATASET_5, DatasetType.defineManually, null);
        linkDatasetFromDetails(GHOST_DATASET_6, DatasetType.importFromFile, null);
    }

    private void createPlaceholderDataset(String name, String category, boolean verify)
    {
        // create and verify a placeholder dataset
        addDataset(name, null, DatasetType.placeholder);
        _test.waitForElement(Locator.xpath("//div[text()='" + name + "']"), StudyBaseTest.WAIT_FOR_JAVASCRIPT);
        _test.assertElementPresent(Locator.xpath("//div[text() = '" + name + "']/../../../td//img[@alt='link data']"));

        if (verify)
        {
            // verify a placeholder dataset cannot be edited from the manage dataset page
            _test.clickLinkWithText(_folderName);
            _test.clickAdminMenuItem("Manage Study");
            _test.clickLinkWithText("Manage Datasets");
            _test.clickLinkWithText(name);

            _test.assertTextNotPresent("View Data", "Edit Definition");
            goToStudySchedule();
        }
    }

    private void addDataset(String name, String category, DatasetType type)
    {
        _test.log("adding dataset: " + name + " type: " + type);

        _test.clickButton("Add Dataset", 0);
        _test.waitForElement(Locator.xpath("//span[text() = 'New Dataset']"), StudyBaseTest.WAIT_FOR_JAVASCRIPT);
        _test.setFormElement(Locator.xpath("//label[text() = 'Name:']/..//input"), name);

        if (category != null)
        {
            _test.setFormElement(Locator.xpath("//label[text() = 'Category:']/..//input"), category);
        }

        switch (type)
        {
            case defineManually:
                _test.click(Locator.ext4Radio("Define dataset manually"));
                _test.clickNavButton("Next");

                _test.waitForElement(Locator.xpath("//input[@id='DatasetDesignerName']"), StudyBaseTest.WAIT_FOR_JAVASCRIPT);

                // add a single name field
                ListHelper.setColumnName(_test, 0, "antigenName");
                _test.clickNavButton("Save");
                break;
            case importFromFile:
                _test.click(Locator.ext4Radio("Import data from file"));
                _test.clickNavButton("Next");

                String datasetFileName = _sampleDataPath + "/datasets/plate001.tsv";
                File file = new File(WebTestHelper.getLabKeyRoot(), datasetFileName);

                if (file.exists())
                {
                    Locator fileUpload = Locator.xpath("//input[@name = 'uploadFormElement']");
                    _test.waitForElement(fileUpload, StudyBaseTest.WAIT_FOR_JAVASCRIPT);
                    _test.setFormElement(fileUpload, file.getAbsolutePath());

                    _test.waitForElement(Locator.xpath("//div[@class = 'gwt-HTML' and contains(text(), 'Showing first 5 rows')]"), StudyBaseTest.WAIT_FOR_JAVASCRIPT);
                    _test.clickNavButton("Import");
                }
                else
                    StudyBaseTest.fail("The dataset import .tsv file (plate001.tsv) does not exist");
                break;
            case placeholder:
                _test.click(Locator.ext4Radio("do this later"));
                _test.clickButton("Done", 0);

                break;
        }
        goToStudySchedule();
    }

    private void linkDatasetFromSchedule(String name, DatasetType type, String targetDataset)
    {
        _test.log("linking dataset: " + name + " to type: " + type + " from study schedule.");
        _test.waitForElement(Locator.xpath("//div[text()='" + name + "']"), StudyBaseTest.WAIT_FOR_JAVASCRIPT);

        Locator link = Locator.xpath("//div[text() = '" + name + "']/../../../td//img[@alt='link data']/../..//div");
        _test.assertElementPresent(link);


        _test.mouseClick(link.toString());
        _test.click(link);
        _test.log("show define dataset dialog");
        _test.waitForElement(Locator.xpath("//span[text() = 'Define Dataset']"), StudyBaseTest.WAIT_FOR_JAVASCRIPT);

        linkDataset(name, type, targetDataset);
        goToStudySchedule();
    }

    private void linkDatasetFromDetails(String name, DatasetType type, String targetDataset)
    {
        _test.log("linking dataset: " + name + " to type: " + type + "from dataset details.");

        _test.clickLinkContainingText(name);
        _test.click(Locator.xpath("//span[text()='Link or Define Dataset']"));
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'x4-form-display-field')][text()='Define " + name + "']"), StudyBaseTest.WAIT_FOR_JAVASCRIPT);

        linkDataset(name, type, targetDataset);

        _test.clickTab("Manage");
        _test.clickLinkContainingText("Manage Datasets");
    }

    private void linkDataset(String name, DatasetType type, String targetDataset)
    {
        switch (type)
        {
            case defineManually:
                _test.click(Locator.ext4Radio("Define dataset manually"));
                _test.clickNavButton("Next");

                _test.waitForElement(Locator.xpath("//input[@id='DatasetDesignerName']"), StudyBaseTest.WAIT_FOR_JAVASCRIPT);

                // add a single name field
                ListHelper.setColumnName(_test, 0, "antigenName");
                _test.clickNavButton("Save");
                break;
            case importFromFile:
                _test.click(Locator.ext4Radio("Import data from file"));
                _test.clickNavButton("Next");

                String datasetFileName = _sampleDataPath + "/datasets/plate001.tsv";
                File file = new File(WebTestHelper.getLabKeyRoot(), datasetFileName);

                if (file.exists())
                {
                    Locator fileUpload = Locator.xpath("//input[@name = 'uploadFormElement']");
                    _test.waitForElement(fileUpload, StudyBaseTest.WAIT_FOR_JAVASCRIPT);
                    _test.setFormElement(fileUpload, file.getAbsolutePath());

                    _test.waitForElement(Locator.xpath("//div[@class = 'gwt-HTML' and contains(text(), 'Showing first 5 rows')]"), StudyBaseTest.WAIT_FOR_JAVASCRIPT);
                    _test.clickNavButton("Import");
                }
                else
                    StudyBaseTest.fail("The dataset import .tsv file (plate001.tsv) does not exist");
                break;
            case linkeToExisting:
                _test.click(Locator.ext4Radio("Link to existing dataset"));

                Locator.XPathLocator comboParent = Locator.xpath("//div[contains(@class, 'existing-dataset-combo')]");
                Ext4Helper.selectComboBoxItem(_test, comboParent, targetDataset);

                _test.clickButton("Done", 0);
                break;
        }
    }

    private void goToStudySchedule()
    {
        _test.clickLinkWithText(_folderName);
        _test.clickAdminMenuItem("Manage Study");
        _test.clickLinkWithText("Study Schedule");

        // wait for grid to load
        _test.waitForText("verifyAssay"); // verify dataset column
        _test.waitForText("Termination"); // verify timepoint
    }
}
