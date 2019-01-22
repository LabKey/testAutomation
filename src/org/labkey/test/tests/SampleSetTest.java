/*
 * Copyright (c) 2007-2018 LabKey Corporation
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

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyA;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.DataRegionTable.DataRegion;

@Category({DailyA.class})
@BaseWebDriverTest.ClassTimeout(minutes = 9)
public class SampleSetTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleSetTestProject";
    private static final String FOLDER_NAME = "SampleSetTestFolder";
    private static final String LINEAGE_FOLDER = "LineageSampleSetFolder";
    private static final String LINEAGE_SAMPLE_SET_NAME = "LineageSampleSet";
    private static final String PROJECT_SAMPLE_SET_NAME = "ProjectSampleSet";
    private static final String FOLDER_SAMPLE_SET_NAME = "FolderSampleSet";
    private static final String FOLDER_CHILDREN_SAMPLE_SET_NAME = "FolderChildrenSampleSet";
    private static final String FOLDER_GRANDCHILDREN_SAMPLE_SET_NAME = "FolderGrandchildrenSampleSet";
    private static final String CASE_INSENSITIVE_SAMPLESET = "CaseInsensitiveSampleSet";
    private static final String LOWER_CASE_SAMPLESET = "caseinsensitivesampleset";

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

    private static final String PROJECT_INVALID_SUBFOLDER_REFERENCE_SAMPLE_SET_TSV = "Key Col\tParent\n" +
            "ProjectS1\tSampleSetBVTChildA\n";

    private static final String PROJECT_VALID_SUBFOLDER_REFERENCE_SAMPLE_SET_TSV = "Key Col\tParent\n" +
            "ProjectS1\t/SampleSetTestProject/SampleSetTestFolder.FolderChildrenSampleSet.SampleSetBVTChildA\n";

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @BeforeClass
    public static void setupProject()
    {
        SampleSetTest init = (SampleSetTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(PROJECT_NAME, null);
        _containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[]{"Experiment"});
        _containerHelper.createSubfolder(PROJECT_NAME, LINEAGE_FOLDER, new String[]{"Experiment"});

        projectMenu().navigateToProject(PROJECT_NAME);
        portalHelper.addWebPart("Sample Sets");

        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        portalHelper.addWebPart("Sample Sets");

        projectMenu().navigateToFolder(PROJECT_NAME, LINEAGE_FOLDER);
        portalHelper.addWebPart("Sample Sets");
    }

    @Test
    public void doLineageDerivationTest()
    {
        String sampleText = "KeyCol\tIntCol\tStringCol\n" +
                "Sample12ab\t1012\talpha\n" +
                "Sample13c4\t1023\tbeta\n" +
                "Sample14d5\t1024\tgamma\n" +
                "Sampleabcd\t1035\tepsilon\n" +
                "Sampledefg\t1046\tzeta";
        importSampleSet(PROJECT_NAME, LINEAGE_FOLDER, LINEAGE_SAMPLE_SET_NAME, sampleText);

        // at this point, we're in the LINEAGE_FOLDER, on the Experiment tab, looking at the sample sets properties and Sample Set contents webparts.
        // now we add more samples,
        String deriveSamples = "Name\tMaterialInputs/LineageSampleSet\n" +
                "A\t\n" +
                "B\tA\n" +      // B and C both derive from A, so should get the same Run
                "C\tA\n" +      // D derives from B, so should get its own run
                "D\tB\n";
        clickButton("Import More Samples");
        setFormElement(Locator.name("data"), deriveSamples);
        checkRadioButton(Locator.radioButtonById("insertOnlyChoice"));
        clickButton("Submit");

        log("foo");
        SelectRowsResponse samples = executeSelectRowCommand("samples", LINEAGE_SAMPLE_SET_NAME, ContainerFilter.Current, getProjectName()+"/"+LINEAGE_FOLDER, null);
        Map<String, Object> rowA =  samples.getRows().stream().filter((a)-> a.get("Name").equals("A")).collect(Collectors.toList()).get(0);
        Map<String, Object> rowB =  samples.getRows().stream().filter((a)-> a.get("Name").equals("B")).collect(Collectors.toList()).get(0);
        Map<String, Object> rowC =  samples.getRows().stream().filter((a)-> a.get("Name").equals("C")).collect(Collectors.toList()).get(0);
        Map<String, Object> rowD =  samples.getRows().stream().filter((a)-> a.get("Name").equals("D")).collect(Collectors.toList()).get(0);

        assertNull("Row A shouldn't have a parent", rowA.get("Run"));
        assertEquals("Rows B and C should both derive from A and get the same run", rowB.get("Run"), rowC.get("Run"));
        assertNotNull("RowD should have a parent", rowD.get("Run"));
        assertNotEquals("RowD should not equal B", rowD.get("Run"), rowB.get("Run"));
        assertNotEquals("RowD should not equal C", rowD.get("Run"), rowC.get("Run"));
    }

    private void importSampleSet(String project, String folder, String samplesetName, String sampleText)
    {
        projectMenu().navigateToFolder(project, folder);

        clickButton("Import Sample Set");
        setFormElement(Locator.id("name"), samplesetName);
        setFormElement(Locator.name("data"), sampleText);
        clickButton("Submit");
    }

    @Test
    public void testCreateSampleSetNoExpression()
    {
        String sampleSetName = "SimpleCreateNoExp";
        List<String> fieldNames = Arrays.asList("StringValue", "IntValue");
        log("Create a new sample set with a name and no name expression");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        click(Locator.tagWithClassContaining("i", "fa-plus"));
        log("Verify the name field is required");
        clickButton("Create");
        assertTextPresent("You must supply a name for the sample set.");

        setFormElement(Locator.id("name"), sampleSetName);
        clickButton("Create");

        log("Add fields to the sample set");
        PropertiesEditor fieldProperties = new PropertiesEditor.PropertiesEditorFinder(getDriver()).withTitle("Field Properties").find();
        fieldProperties.addField(new FieldDefinition(fieldNames.get(0)).setType(FieldDefinition.ColumnType.String));
        fieldProperties.addField(new FieldDefinition(fieldNames.get(1)).setType(FieldDefinition.ColumnType.Integer));
        clickButton("Save");

        log("Go to the sample set and verify its fields");
        click(Locator.linkWithText(sampleSetName));
        List<String> names = getSampleSetFields();
        for (String name : fieldNames)
            assertTrue("'" + name + "' should be one of the fields", names.contains(name));

        log("Add a single row to the sample set");
//        Map<String, String> fieldMap =
        DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents")
                .clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), "S-1");
        setFormElement(Locator.name("quf_StringValue"), "Ess");
        setFormElement(Locator.name("quf_IntValue"), "1");
        clickButton("Submit");

        log("Verify values were saved");
        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents");
        Map<String, String> rowData = drt.getRowDataAsMap(0);
        assertEquals("Name not as expected", "S-1", rowData.get("Name"));
        assertEquals(fieldNames.get(0) + " not as expected", "Ess", rowData.get(fieldNames.get(0)));
        assertEquals(fieldNames.get(1) + "not as expected", "1", rowData.get(fieldNames.get(1)));

        log ("Add multiple rows via simple (default) import mechanism");
        drt.clickHeaderMenu("Insert data", "Simple bulk data import");
        String text = "Name\t" + fieldNames.get(0) + "\t" + fieldNames.get(1) + "\n" +
                "S-2\tTee\t2\n" +
                "S-3\tEwe\t3";
        setFormElement(Locator.name("text"), text);
        clickButton("Submit");
        drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents");
        assertEquals("Number of samples not as expected", 3, drt.getDataRowCount());

        int index = drt.getRowIndex("Name", "S-2");
        assertTrue("Should have row with second sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for second sample not as expected", "Tee", rowData.get(fieldNames.get(0)));
        assertEquals(fieldNames.get(1) + " for second sample not as expected", "2", rowData.get(fieldNames.get(1)));

        log("Try to create a sample set with the same name.");
        click(Locator.linkWithText("Sample Sets"));
        click(Locator.tagWithClassContaining("i", "fa-plus"));
        setFormElement(Locator.id("name"), sampleSetName);
        clickButton("Create");
        assertTextPresent("A sample set with that name already exists.");

        clickButton("Cancel");
        drt = DataRegion(this.getDriver()).find();
        assertEquals("Data region should be sample sets listing", "SampleSet", drt.getDataRegionName());
    }

    @Test
    public void testCreateSampleSetWithExpression()
    {
        String sampleSetName = "SimpleCreateWithExp";
        List<String> fieldNames = Arrays.asList("StringValue", "FloatValue");

        log("Create a new sample set with a name and name expression");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        click(Locator.tagWithClassContaining("i", "fa-plus"));
        setFormElement(Locator.id("name"), sampleSetName);
        setFormElement(Locator.id("nameExpression"), "${" + fieldNames.get(0) + "}-${batchRandomId}-${randomId}");
        clickButton("Create");

        log("Add fields to the sample set");
        PropertiesEditor fieldProperties = new PropertiesEditor.PropertiesEditorFinder(getDriver()).withTitle("Field Properties").find();
        fieldProperties.addField(new FieldDefinition(fieldNames.get(0)).setType(FieldDefinition.ColumnType.String));
        fieldProperties.addField(new FieldDefinition(fieldNames.get(1)).setType(FieldDefinition.ColumnType.Double));
        clickButton("Save");

        log("Go to the sample set and verify its fields");
        click(Locator.linkWithText(sampleSetName));
        List<String> names = getSampleSetFields();
        for (String name : fieldNames)
            assertTrue("'" + name + "' should be one of the fields", names.contains(name));

        log("Add data without supplying the name");
        DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents")
                .clickInsertNewRow();
        setFormElement(Locator.name("quf_" + fieldNames.get(0)), "Vee");
        setFormElement(Locator.name("quf_" + fieldNames.get(1)), "1.6");
        clickButton("Submit");

        log("Verify values are as expected with name expression saved");
        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents");
        Map<String, String> rowData = drt.getRowDataAsMap(0);
        assertTrue("Name not as expected", rowData.get("Name").startsWith("Vee-"));
        assertEquals(fieldNames.get(0) + " not as expected", "Vee", rowData.get(fieldNames.get(0)));
        assertEquals(fieldNames.get(1) + "not as expected", "1.6", rowData.get(fieldNames.get(1)));

        log("Add data with name provided");
        DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents")
                .clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), "NoExpression");
        clickButton("Submit");
        log("Verify values are as expected with name expression saved");
        drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents");
        rowData = drt.getRowDataAsMap(0);
        assertEquals("Name not as expected", "NoExpression", rowData.get("Name"));

        log ("Add multiple rows via simple (default) import mechanism");
        drt.clickHeaderMenu("Insert data", "Simple bulk data import");
        String text = fieldNames.get(0) + "\t" + fieldNames.get(1) + "\n" +
                "Dubya\t2.1\n" +
                "Ex\t44.2";
        setFormElement(Locator.name("text"), text);
        clickButton("Submit");
        drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents");
        assertEquals("Number of samples not as expected", 4, drt.getDataRowCount());

        assertTrue("Should have row with first imported value", drt.getRowIndex(fieldNames.get(0), "Dubya") >= 0);
        assertTrue("Should have row with second imported value", drt.getRowIndex(fieldNames.get(0), "Ex") >= 0);
    }

    @Test
    public void testSteps()
    {
        clickProject(PROJECT_NAME);
        clickButton("Import Sample Set");
        setFormElement(Locator.id("name"), PROJECT_SAMPLE_SET_NAME);
        checkRadioButton(Locator.radioButtonByNameAndValue("uploadType", "file"));
        setFormElement(Locator.tagWithName("input", "file"), TestFileUtils.getSampleData("sampleSet.xlsx").getAbsolutePath());
        selectParentColumn("Parent");
        clickButton("Submit");

        clickFolder(FOLDER_NAME);
        clickButton("Import Sample Set");
        setFormElement(Locator.id("name"), FOLDER_SAMPLE_SET_NAME);
        setFormElement(Locator.name("data"), "KeyCol-Folder\tIntCol-Folder\tStringCol-Folder\n" +
                "SampleSetBVT11\t101\taa\n" +
                "SampleSetBVT4\t102\tbb\n" +
                "SampleSetBVT12\t102\tbb\n" +
                "SampleSetBVT13\t103\tcc\n" +
                "SampleSetBVT14\t104\tdd");
        clickButton("Submit");

        // Do some manual derivation
        clickAndWait(Locator.linkWithText("Sample Sets"));
        assertTextPresent(PROJECT_SAMPLE_SET_NAME, FOLDER_NAME);

        clickButton("Show All Materials");
        assertTextPresent(FOLDER_SAMPLE_SET_NAME);
        assertTextNotPresent(PROJECT_SAMPLE_SET_NAME);

        checkCheckbox(Locator.name(".toggle"));
        clickButton("Derive Samples");

        if (isElementPresent(Locator.linkWithText("configure a valid pipeline root for this folder")))
        {
            setPipelineRoot(TestFileUtils.getLabKeyRoot() + PIPELINE_PATH);
        }

        clickFolder(FOLDER_NAME);
        assertTextPresent(FOLDER_SAMPLE_SET_NAME, PROJECT_SAMPLE_SET_NAME);
        clickAndWait(Locator.linkWithText(FOLDER_SAMPLE_SET_NAME));
        checkCheckbox(Locator.name(".toggle"));
        clickButton("Derive Samples");

        selectOptionByText(Locator.name("inputRole0"), "Add a new role...");
        setFormElement(Locator.id("customRole0"), "FirstRole");
        selectOptionByText(Locator.name("inputRole1"), "Add a new role...");
        setFormElement(Locator.id("customRole1"), "SecondRole");
        selectOptionByText(Locator.name("inputRole2"), "Add a new role...");
        setFormElement(Locator.id("customRole2"), "ThirdRole");
        selectOptionByText(Locator.name("inputRole3"), "Add a new role...");
        setFormElement(Locator.id("customRole3"), "FourthRole");
        selectOptionByText(Locator.name("outputCount"), "2");
        selectOptionByText(Locator.name("targetSampleSetId"), "FolderSampleSet in /SampleSetTestProject/SampleSetTestFolder");
        clickButton("Next");

        setFormElement(Locator.name("outputSample1_KeyColFolder"), "SampleSetBVT15");
        setFormElement(Locator.name("outputSample2_KeyColFolder"), "SampleSetBVT16");
        checkCheckbox(Locator.name("outputSample1_IntColFolderCheckBox"));
        setFormElement(Locator.name("outputSample1_IntColFolder"), "500a");
        setFormElement(Locator.name("outputSample1_StringColFolder"), "firstOutput");
        setFormElement(Locator.name("outputSample2_StringColFolder"), "secondOutput");
        clickButton("Submit");

        assertTextPresent("must be of type Integer");
        checkCheckbox(Locator.name("outputSample1_IntColFolderCheckBox"));
        setFormElement(Locator.name("outputSample1_IntColFolder"), "500");
        clickButton("Submit");

        clickAndWait(Locator.linkContainingText("Derive 2 samples"));
        clickAndWait(Locator.linkContainingText("Text View"));
        assertTextPresent("FirstRole", "SecondRole", "ThirdRole", "FourthRole");

        clickAndWait(Locator.linkContainingText("16"));
        clickAndWait(Locator.linkContainingText("derive samples from this sample"));

        selectOptionByText(Locator.name("inputRole0"), "FirstRole");
        selectOptionByText(Locator.name("targetSampleSetId"), "ProjectSampleSet in /SampleSetTestProject");
        clickButton("Next");

        setFormElement(Locator.name("outputSample1_KeyCol"), "200");
        setFormElement(Locator.name("outputSample1_IntCol"), "600");
        setFormElement(Locator.name("outputSample1_StringCol"), "String");
        setFormElement(Locator.name("outputSample1_DateCol"), "BadDate");
        uncheckCheckbox(Locator.name("outputSample1_BoolCol"));
        clickButton("Submit");

        assertTextPresent("must be of type Date and Time");
        setFormElement(Locator.name("outputSample1_DateCol"), "1/1/2007");
        clickButton("Submit");

        assertElementPresent(Locator.linkWithText("Derive sample from SampleSetBVT16"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT11"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT12"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT13"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT14"));

        clickAndWait(Locator.linkWithText("SampleSetBVT11"));

        assertElementPresent(Locator.linkWithText("Derive sample from SampleSetBVT16"));
        assertElementPresent(Locator.linkWithText("Derive 2 samples from SampleSetBVT11, SampleSetBVT12, SampleSetBVT13, SampleSetBVT14, SampleSetBVT4"));

        clickFolder(FOLDER_NAME);
        clickAndWait(Locator.linkWithText(FOLDER_SAMPLE_SET_NAME));

        assertTextPresent("aa", "bb", "cc", "dd", "firstOutput", "secondOutput");

        clickAndWait(Locator.linkWithText("Sample Sets"));
        clickButton("Show All Materials");
        assertTextPresent("ProjectSampleSet", "200");

        // Try to derive samples using the parent column
        clickTab("Experiment");
        clickAndWait(Locator.linkWithText("Sample Sets"));
        clickButton("Import Sample Set");
        setFormElement(Locator.name("name"), FOLDER_CHILDREN_SAMPLE_SET_NAME);
        setFormElement(Locator.name("data"), AMBIGUOUS_CHILD_SAMPLE_SET_TSV);
        selectParentColumn("Parent");
        clickButton("Submit");
        assertTextPresent("Failed to find sample parent: Found 2 values matching: SampleSetBVT4");

        // Try again with a qualified sample name
        setFormElement(Locator.name("data"), CHILD_SAMPLE_SET_TSV);
        selectParentColumn("Parent");
        clickButton("Submit");
        assertTextPresent("SampleSetBVTChildA");

        fileAttachmentTest();
        clickAndWait(Locator.linkWithText("SampleSetBVTChildB"));

        // Make sure that the parent got wired up
        clickAndWait(Locator.linkWithText("SampleSetBVT4"));
        // Check out the run
        clickAndWait(Locator.linkWithText("Derive sample from SampleSetBVT4"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT4"));
        assertElementPresent(Locator.linkWithText("SampleSetBVTChildB"));

        // Make a grandchild set, but first try to insert as a duplicate set name
        clickTab("Experiment");
        clickAndWait(Locator.linkWithText("Sample Sets"));
        clickButton("Import Sample Set");
        setFormElement(Locator.name("name"), FOLDER_CHILDREN_SAMPLE_SET_NAME);
        setFormElement(Locator.name("data"), GRANDCHILD_SAMPLE_SET_TSV);
        selectParentColumn("Parent");
        clickButton("Submit");

        assertTextPresent("A sample set with that name already exists");
        setFormElement(Locator.name("name"), FOLDER_GRANDCHILDREN_SAMPLE_SET_NAME);
        selectParentColumn("Parent");
        clickButton("Submit");

        waitAndClickAndWait(Locator.linkWithText("SampleSetBVTGrandchildA"));

        // These two regions are used throughout the remaining jumps comparing parent/child sets
        DataRegionTable childMaterialsRegion = new DataRegionTable("childMaterials", this.getDriver());
        DataRegionTable parentMaterialsRegion = new DataRegionTable("parentMaterials", this.getDriver());

        // Filter out any child materials, though there shouldn't be any
        childMaterialsRegion.setFilter("Name", "Is Blank");
        // Check for parents and grandparents
        assertTextPresent("SampleSetBVTChildB", "SampleSetBVT4", "SampleSetBVT11");

        // Verify that we've chained things together properly
        clickAndWait(Locator.linkWithText("SampleSetBVTChildA"));
        // Filter out any child materials so we can just check for parents
        childMaterialsRegion.setFilter("Name", "Is Blank");
        assertTextPresent("SampleSetBVT11");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVTGrandchildA"));
        // Switch to filter out any parent materials so we can just check for children
        parentMaterialsRegion.setFilter("Name", "Is Blank");
        childMaterialsRegion.clearFilter("Name");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVT11"));
        assertTextPresent("SampleSetBVTGrandchildA");

        // Go up the chain one more hop
        parentMaterialsRegion.clearAllFilters("Name");
        clickAndWait(Locator.linkWithText("SampleSetBVT11"));
        // Filter out any child materials so we can just check for parents
        childMaterialsRegion.setFilter("Name", "Is Blank");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVTChildA"));
        assertElementNotPresent(Locator.linkWithText("SampleSetBVTGrandchildA"));
        // Switch to filter out any parent materials so we can just check for children
        parentMaterialsRegion.setFilter("Name", "Is Blank");
        childMaterialsRegion.clearFilter("Name");
        assertTextPresent("SampleSetBVTChildA", "SampleSetBVTGrandchildA");

        clickAndWait(Locator.linkWithText(FOLDER_CHILDREN_SAMPLE_SET_NAME));
        clickButton("Import More Samples");
        checkRadioButton(Locator.radioButtonById("insertOrUpdateChoice"));
        setFormElement(Locator.name("data"), REPARENTED_CHILD_SAMPLE_SET_TSV);
        clickButton("Submit");

        clickAndWait(Locator.linkWithText("SampleSetBVTChildB"));
        assertTextPresent("2.222");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVT4"));
        // Filter out any child materials so we can just check for parents
        childMaterialsRegion.setFilter("Name", "Is Blank");
        assertElementPresent(Locator.linkWithText("SampleSetBVT14"));
        assertElementNotPresent(Locator.linkWithText("SampleSetBVTGrandchildA"));
        // Switch to filter out any parent materials so we can just check for children
        parentMaterialsRegion.setFilter("Name", "Is Blank");
        childMaterialsRegion.clearFilter("Name");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVT14"));
        assertElementPresent(Locator.linkWithText("SampleSetBVTGrandchildA"));

        // Verify that the event was audited
        goToModule("Query");
        viewQueryData("auditLog", "SampleSetAuditEvent");
        assertTextPresent(
                "Samples inserted or updated in: " + FOLDER_SAMPLE_SET_NAME,
                "Samples inserted or updated in: " + FOLDER_CHILDREN_SAMPLE_SET_NAME,
                "Samples inserted or updated in: " + FOLDER_GRANDCHILDREN_SAMPLE_SET_NAME);

        // Verify that we can reference samples in other containers by including a folder path
        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText(PROJECT_SAMPLE_SET_NAME));
        clickButton("Import More Samples");
        checkRadioButton(Locator.radioButtonById("insertOrUpdateChoice"));
        setFormElement(Locator.name("data"), PROJECT_INVALID_SUBFOLDER_REFERENCE_SAMPLE_SET_TSV);
        clickButton("Submit");
        assertTextPresent("Could not find parent material with name 'SampleSetBVTChildA'.");
        setFormElement(Locator.name("data"), PROJECT_VALID_SUBFOLDER_REFERENCE_SAMPLE_SET_TSV);
        clickButton("Submit");
        // Verify it got linked up correctly
        clickAndWait(Locator.linkWithText("ProjectS1"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT13"));
        assertElementPresent(Locator.linkWithText("SampleSetBVTChildA"));

        // make sure we are case-sensitive when creating samplesets -- regression coverage for issue 33743
        clickProject(PROJECT_NAME);
        clickButton("Import Sample Set");
        setFormElement(Locator.id("name"), CASE_INSENSITIVE_SAMPLESET);
        checkRadioButton(Locator.radioButtonByNameAndValue("uploadType", "file"));
        setFormElement(Locator.tagWithName("input", "file"), TestFileUtils.getSampleData("sampleSet.xlsx").getAbsolutePath());
        selectParentColumn("Parent");
        clickButton("Submit");

        clickProject(PROJECT_NAME);
        clickButton("Import Sample Set");
        setFormElement(Locator.id("name"), LOWER_CASE_SAMPLESET);
        checkRadioButton(Locator.radioButtonByNameAndValue("uploadType", "file"));
        setFormElement(Locator.tagWithName("input", "file"), TestFileUtils.getSampleData("sampleSet.xlsx").getAbsolutePath());
        selectParentColumn("Parent");
        clickButton("Submit");
        waitForElement(Locator.tagWithClass("div", "labkey-error").containing("A sample set with that name already exists."));
        clickProject(PROJECT_NAME);
        assertElementPresent(Locator.linkWithText(CASE_INSENSITIVE_SAMPLESET));
        assertElementNotPresent(Locator.linkWithText(LOWER_CASE_SAMPLESET));
    }

    final File experimentFilePath = new File(TestFileUtils.getLabKeyRoot() + PIPELINE_PATH, "experiment.xar.xml");

    private void fileAttachmentTest()
    {
        enableFileInput();

        setFileAttachment(0, experimentFilePath);
        setFileAttachment(1, new File(TestFileUtils.getLabKeyRoot() +  "/sampledata/sampleset/RawAndSummary~!@#$%^&()_+-[]{};',..xlsx"));
        insertNewWithFileAttachmentTest();

        // Added these last two test to check for regressions with exporting a grid with a file attachment column and deleting a file attachment column.
        exportGridWithAttachment(3, 4, "experiment-1.xar.xml", "experiment.xar.xml", "rawandsummary~!@#$%^&()_+-[]{};',..xlsx");
        deleteAttachmentColumn();
        exportGridWithAttachment(3, 4, "", "", "");
    }

    private List<String> getSampleSetFields()
    {
        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents");
        return drt.getColumnNames();
    }

    private void insertNewWithFileAttachmentTest()
    {
        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents");
        drt.clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), "SampleSetInsertedManually");
        setFormElement(Locator.name("quf_FileAttachment"), experimentFilePath);
        clickButton("Submit");
        //a double upload causes the file to be appended with a count
        assertTextPresent("experiment-1.xar.xml");
    }

    private void enableFileInput()
    {
        String fileField = "FileAttachment";
        waitAndClickAndWait(Locator.lkButton("Edit Fields"));
        PropertiesEditor fieldProperties = new PropertiesEditor.PropertiesEditorFinder(getDriver()).withTitle("Field Properties").waitFor();
        fieldProperties.addField(new FieldDefinition(fileField).setType(FieldDefinition.ColumnType.File).setLabel(fileField).setDescription(fileField));
        clickButton("Save");
    }

    private void setFileAttachment(int index, File attachment)
    {
        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents");
        drt.clickEditRow(index);
        setFormElement(Locator.name("quf_FileAttachment"),  attachment);
        clickButton("Submit");

        String path = drt.getDataAsText(index, "File Attachment");
        assertNotNull("Path shouldn't be null", path);
        assertTrue("Path didn't contain " + attachment.getName() + ", but was: " + path, path.contains(attachment.getName()));
    }

    private void selectParentColumn(String parentCol)
    {
        fireEvent(Locator.name("data"), SeleniumEvent.blur);
        waitForFormElementToEqual(Locator.id("idCol1"), "0"); // "KeyCol"
        waitForElement(Locator.css("select#parentCol > option").withText(parentCol));
        selectOptionByText(Locator.id("parentCol"), parentCol);
    }

    private void exportGridWithAttachment(int numOfRows, int exportColumn, String... expectedFilePaths)
    {
        DataRegionTable list;
        DataRegionExportHelper exportHelper;
        File exportedFile;
        Workbook workbook;
        Sheet sheet;
        List<String> exportedColumn;
        int row;

        log("Export the grid to excel.");
        list = new DataRegionTable("Material", this.getDriver());
        exportHelper = new DataRegionExportHelper(list);
        exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLS);

        try
        {
            workbook = ExcelHelper.create(exportedFile);
            sheet = workbook.getSheetAt(0);

            assertEquals("Wrong number of rows exported to " + exportedFile.getName(), numOfRows, sheet.getLastRowNum());

            log("Validate that the value for the attachment columns is as expected.");
            exportedColumn = ExcelHelper.getColumnData(sheet, exportColumn);
            row = 1;
            for (String filePath : expectedFilePaths)
            {
                if (filePath.length() == 0)
                {
                    assertEquals("Value of attachment column for row " + row + " not exported as expected.", "", exportedColumn.get(row).trim());
                }
                else
                {
                    assertThat("Value of attachment column for row " + row + " not exported as expected.", exportedColumn.get(row).trim().toLowerCase(), CoreMatchers.containsString(filePath));
                }
                row++;
            }
        }
        catch (IOException | InvalidFormatException e)
        {
            throw new RuntimeException(e);
        }

    }

    private void deleteAttachmentColumn()
    {
        log("Remove the attachment columns and validate that everything still works.");
        waitAndClickAndWait(Locator.lkButton("Edit Fields"));
        PropertiesEditor fieldProperties = new PropertiesEditor.PropertiesEditorFinder(getDriver()).withTitle("Field Properties").waitFor();
        fieldProperties.selectField(2).markForDeletion();

        // Can't use _listHelper.clickSave, it waits for a "Edit Desing" button and a "Done" button.
        waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Save"), 0);
        waitForElement(Locator.lkButton("Edit Fields"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
