/*
 * Copyright (c) 2007-2010 LabKey Corporation
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
    private static final String FOLDER_CHILDREN_SAMPLE_SET_NAME = "FolderChildrenSampleSet";
    private static final String FOLDER_GRANDCHILDREN_SAMPLE_SET_NAME = "FolderGrandchildrenSampleSet";

    protected static final String PIPELINE_PATH = "/sampledata/xarfiles/expVerify";
    private static final String AMBIGUOUS_CHILD_SAMPLE_SET_TSV = "Name\tParent\tOtherProp\n" +
            "SampleSetBVTChildA\tSampleSetBVT11\t1.1\n" +
            "SampleSetBVTChildB\tSampleSetBVT4\t2.2\n";

    private static final String CHILD_SAMPLE_SET_TSV = "Name\tParent\tOtherProp\n" +
            "SampleSetBVTChildA\tSampleSetBVT11\t1.1\n" +
            "SampleSetBVTChildB\tFolderSampleSet.SampleSetBVT4\t2.2\n";

    private static final String REPARENTED_CHILD_SAMPLE_SET_TSV = "Name\tParent\tOtherProp\n" +
            "SampleSetBVTChildA\tSampleSetBVT13\t1.111\n" +
            "SampleSetBVTChildB\tFolderSampleSet.SampleSetBVT14\t2.222\n";

    private static final String GRANDCHILD_SAMPLE_SET_TSV = "Name\tParent\tOtherProp\n" +
            "SampleSetBVTGrandchildA\tSampleSetBVTChildA,SampleSetBVTChildB\t11.11\n";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/experiment";
    }

    protected void doCleanup()
    {
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
                "SampleSetBVT1\t100\ta\t1/1/2000\tTRUE\n" +
                "SampleSetBVT2\t200\tb\t2/2/2000\tFALSE\n" +
                "SampleSetBVT3\t300\tc\t3/3/2000\tTRUE\n" +
                "SampleSetBVT4\t400\td\t4/4/2000\tFALSE");
        clickNavButton("Submit");

        clickLinkWithText(FOLDER_NAME);
        addWebPart("Sample Sets");
        clickNavButton("Import Sample Set");
        setFormElement("name", FOLDER_SAMPLE_SET_NAME);
        setFormElement("data", "KeyCol-Folder\tIntCol-Folder\tStringCol-Folder\n" +
                "SampleSetBVT11\t101\taa\n" +
                "SampleSetBVT4\t102\tbb\n" +
                "SampleSetBVT12\t102\tbb\n" +
                "SampleSetBVT13\t103\tcc\n" +
                "SampleSetBVT14\t104\tdd");
        clickNavButton("Submit");

        // Do some manual derivation
        clickLinkWithText("Sample Sets");
        assertTextPresent(PROJECT_SAMPLE_SET_NAME);
        assertTextPresent(FOLDER_NAME);

        clickNavButton("Show All Materials");
        assertTextPresent(FOLDER_SAMPLE_SET_NAME);
        assertTextNotPresent(PROJECT_SAMPLE_SET_NAME);

        clickCheckbox(".toggle");
        clickNavButton("Derive Samples");

        if (isLinkPresentWithText("configure a valid pipeline root for this folder"))
        {
            setPipelineRoot(getLabKeyRoot() + PIPELINE_PATH);
        }

        clickLinkWithText(FOLDER_NAME);
        assertTextPresent(FOLDER_SAMPLE_SET_NAME);
        assertTextPresent(PROJECT_SAMPLE_SET_NAME);
        clickLinkWithText(FOLDER_SAMPLE_SET_NAME);
        clickCheckbox(".toggle");
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

        setFormElement("outputSample1_KeyColFolder", "SampleSetBVT15");
		setFormElement("outputSample2_KeyColFolder", "SampleSetBVT16");
		selenium.click("outputSample1_IntColFolderCheckBox");
		setFormElement("outputSample1_IntColFolder", "500a");
		setFormElement("outputSample1_StringColFolder", "firstOutput");
		setFormElement("outputSample2_StringColFolder", "secondOutput");
        clickNavButton("Submit");

        assertTextPresent("must be of type Integer");
        clickCheckbox("outputSample1_IntColFolderCheckBox");
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

        assertLinkPresentWithText("Derive sample from SampleSetBVT16");
        assertLinkPresentWithText("SampleSetBVT11");
        assertLinkPresentWithText("SampleSetBVT12");
        assertLinkPresentWithText("SampleSetBVT13");
        assertLinkPresentWithText("SampleSetBVT14");

        clickLinkWithText("SampleSetBVT11");

        assertLinkPresentWithText("Derive sample from SampleSetBVT16");
        assertLinkPresentWithText("Derive 2 samples from SampleSetBVT11, SampleSetBVT12, SampleSetBVT13, SampleSetBVT14, SampleSetBVT4");

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

        // Try to derive samples using the parent column
        clickTab("Experiment");
        clickLinkWithText("Sample Sets");
        clickNavButton("Import Sample Set");
        setFormElement("name", FOLDER_CHILDREN_SAMPLE_SET_NAME);
        setFormElement("data", AMBIGUOUS_CHILD_SAMPLE_SET_TSV);
        selectOptionByText("parentCol", "Parent");
        clickNavButton("Submit");
        assertTextPresent("More than one match for parent material");

        // Try again with a qualified sample name
        setFormElement("data", CHILD_SAMPLE_SET_TSV);
        selectOptionByText("parentCol", "Parent");
        clickNavButton("Submit");
        assertTextPresent("SampleSetBVTChildA");
        clickLinkWithText("SampleSetBVTChildB");

        // Make sure that the parent got wired up
        clickLinkWithText("SampleSetBVT4");
        // Check out the run
        clickLinkWithText("Derive sample from SampleSetBVT4");
        assertLinkPresentWithText("SampleSetBVT4");
        assertLinkPresentWithText("SampleSetBVTChildB");

        // Make a grandchild set, but first try to insert as a duplicate set name
        clickTab("Experiment");
        clickLinkWithText("Sample Sets");
        clickNavButton("Import Sample Set");
        setFormElement("name", FOLDER_CHILDREN_SAMPLE_SET_NAME);
        setFormElement("data", GRANDCHILD_SAMPLE_SET_TSV);
        selectOptionByText("parentCol", "Parent");
        clickNavButton("Submit");

        assertTextPresent("A sample set with that name already exists");
        setFormElement("name", FOLDER_GRANDCHILDREN_SAMPLE_SET_NAME);
        clickNavButton("Submit");

        clickLinkWithText("SampleSetBVTGrandchildA");
        // Filter out any child materials, though there shouldn't be any
        setFilter("childMaterials", "Name", "Is Blank");
        // Check for parents and grandparents
        assertTextPresent("SampleSetBVTChildB");
        assertTextPresent("SampleSetBVT4");
        assertTextPresent("SampleSetBVT11");

        // Verify that we've chained things together properly
        clickLinkWithText("SampleSetBVTChildA");
        // Filter out any child materials so we can just check for parents
        setFilter("childMaterials", "Name", "Is Blank");
        assertTextPresent("SampleSetBVT11");
        assertLinkNotPresentWithText("SampleSetBVTGrandchildA");
        // Switch to filter out any parent materials so we can just check for children
        setFilter("parentMaterials", "Name", "Is Blank");
        clearFilter("childMaterials", "Name");
        assertLinkNotPresentWithText("SampleSetBVT11");
        assertTextPresent("SampleSetBVTGrandchildA");

        // Go up the chain one more hop
        clearAllFilters("parentMaterials", "Name");
        clickLinkWithText("SampleSetBVT11");
        // Filter out any child materials so we can just check for parents
        setFilter("childMaterials", "Name", "Is Blank");
        assertLinkNotPresentWithText("SampleSetBVTChildA");
        assertLinkNotPresentWithText("SampleSetBVTGrandchildA");
        // Switch to filter out any parent materials so we can just check for children
        setFilter("parentMaterials", "Name", "Is Blank");
        clearFilter("childMaterials", "Name");
        assertTextPresent("SampleSetBVTChildA");
        assertTextPresent("SampleSetBVTGrandchildA");

        clickLinkWithText(FOLDER_CHILDREN_SAMPLE_SET_NAME);
        clickNavButton("Import More Samples");
        clickRadioButtonById("insertOrUpdateChoice");
        setFormElement("data", REPARENTED_CHILD_SAMPLE_SET_TSV);
        clickNavButton("Submit");

        clickLinkWithText("SampleSetBVTChildB");
        assertTextPresent("2.222");
        assertLinkNotPresentWithText("SampleSetBVT4");
        // Filter out any child materials so we can just check for parents
        setFilter("childMaterials", "Name", "Is Blank");
        assertLinkPresentWithText("SampleSetBVT14");
        assertLinkNotPresentWithText("SampleSetBVTGrandchildA");
        // Switch to filter out any parent materials so we can just check for children
        setFilter("parentMaterials", "Name", "Is Blank");
        clearFilter("childMaterials", "Name");
        assertLinkNotPresentWithText("SampleSetBVT14");
        assertLinkPresentWithText("SampleSetBVTGrandchildA");
    }
}
