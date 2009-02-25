/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;

/**
 * User: jeckels
 * Date: Nov 19, 2007
 */
public class SampleSetTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "SampleSetTestProject";
    private static final String FOLDER_NAME = "SampleSetTestFolder";
    private static final String PROJECT_SAMPLE_SET_NAME = "ProjectSampleSet";
    private static final String FOLDER_SAMPLE_SET_NAME = "FolderSampleSet";

    protected static final String PIPELINE_PATH = "/sampledata/xarfiles/expVerify";

    public String getAssociatedModuleDirectory()
    {
        return "experiment";
    }

    protected void doCleanup()
    {
        try {deleteFolder(PROJECT_NAME, FOLDER_NAME); } catch (Throwable t) {}
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[] { "Experiment" });

        clickLinkWithText(PROJECT_NAME);
        addWebPart("Sample Sets");

        clickNavButton("Import Sample Set");
        setFormElement("name", PROJECT_SAMPLE_SET_NAME);
        setFormElement("data", "KeyCol\tIntCol\tStringCol\tDateCol\tBoolCol\n" +
                "1\t100\ta\t1/1/2000\tTRUE\n" +
                "2\t200\tb\t2/2/2000\tFALSE\n" +
                "3\t300\tc\t3/3/2000\tTRUE\n" +
                "4\t400\td\t4/4/2000\tFALSE");
        clickNavButton("Submit");

        clickLinkWithText(FOLDER_NAME);
        addWebPart("Sample Sets");
        clickNavButton("Import Sample Set");
        setFormElement("name", FOLDER_SAMPLE_SET_NAME);
        setFormElement("data", "KeyCol-Folder\tIntCol-Folder\tStringCol-Folder\n" +
                "11\t101\taa\n" +
                "12\t102\tbb\n" +
                "13\t103\tcc\n" +
                "14\t104\tdd");
        clickNavButton("Submit");

        clickLinkWithText("Sample Sets");
        assertTextPresent(PROJECT_SAMPLE_SET_NAME);
        assertTextPresent(FOLDER_NAME);

        clickNavButton("Show All Materials");
        assertTextPresent(FOLDER_SAMPLE_SET_NAME);
        assertTextNotPresent(PROJECT_SAMPLE_SET_NAME);

        clickCheckbox(".toggle", false);
        clickNavButton("Derive Samples");

        clickLinkContainingText("valid pipeline root");
        setFormElement("path", getLabKeyRoot() + PIPELINE_PATH);
        clickNavButton("Set");

        clickLinkWithText(FOLDER_NAME);
        assertTextPresent(FOLDER_SAMPLE_SET_NAME);
        assertTextPresent(PROJECT_SAMPLE_SET_NAME);
        clickLinkWithText(FOLDER_SAMPLE_SET_NAME);
        clickCheckbox(".toggle", false);
        clickNavButton("Derive Samples");

        selenium.select("inputRole0", "label=Add a new role...");
        setFormElement("customRole0", "FirstRole");
        selenium.select("inputRole1", "label=Add a new role...");
        setFormElement("customRole1", "SecondRole");
        selenium.select("inputRole2", "label=Add a new role...");
        setFormElement("customRole2", "ThirdRole");
        selenium.select("inputRole3", "label=Add a new role...");
        setFormElement("customRole3", "FourthRole");
        selenium.select("outputCount", "label=2");
        selenium.select("targetSampleSetId", "label=FolderSampleSet in /SampleSetTestProject/SampleSetTestFolder");
        clickNavButton("Next");

        setFormElement("outputSample1_KeyColFolder", "15");
		setFormElement("outputSample2_KeyColFolder", "16");
		selenium.click("outputSample1_IntColFolderCheckBox");
		setFormElement("outputSample1_IntColFolder", "500a");
		setFormElement("outputSample1_StringColFolder", "firstOutput");
		setFormElement("outputSample2_StringColFolder", "secondOutput");
        clickNavButton("Submit");

        assertTextPresent("must be of type Integer");
        clickCheckbox("outputSample1_IntColFolderCheckBox", false);
        setFormElement("outputSample1_IntColFolder", "500");
        clickNavButton("Submit");

        clickLinkContainingText("Derive 2 samples");
        clickLinkContainingText("text view");
        assertTextPresent("FirstRole");
        assertTextPresent("SecondRole");
        assertTextPresent("ThirdRole");
        assertTextPresent("FourthRole");

        clickLinkContainingText("16");
        clickLinkContainingText("derive samples from this sample");

        selectOptionByText("inputRole0", "FirstRole");
        selenium.select("targetSampleSetId", "label=ProjectSampleSet in /SampleSetTestProject");
        clickNavButton("Next");

        setFormElement("outputSample1_KeyCol", "200");
        setFormElement("outputSample1_IntCol", "600");
        setFormElement("outputSample1_StringCol", "String");
        setFormElement("outputSample1_DateCol", "BadDate");
        setFormElement("outputSample1_BoolCol", "FALSE");
        clickNavButton("Submit");

        assertTextPresent("must be of type Date and Time");
        setFormElement("outputSample1_DateCol", "1/1/2007");
        clickNavButton("Submit");

        assertLinkPresentWithText("Derive sample from 16");
        assertLinkPresentWithText("11");
        assertLinkPresentWithText("12");
        assertLinkPresentWithText("13");
        assertLinkPresentWithText("14");

        clickLinkWithText("11");

        assertLinkPresentWithText("Derive sample from 16");
        assertLinkPresentWithText("Derive 2 samples from 11, 12, 13, 14");

        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText(FOLDER_SAMPLE_SET_NAME);

        assertTextPresent("aa");
        assertTextPresent("bb");
        assertTextPresent("cc");
        assertTextPresent("dd");
        assertTextPresent("firstOutput");
        assertTextPresent("secondOutput");

        clickLinkWithText("Sample Sets");
        clickNavButton("Show All Materials");
        assertTextPresent("ProjectSampleSet");
        assertTextPresent("200");
    }
}
