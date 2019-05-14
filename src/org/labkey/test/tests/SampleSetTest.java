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

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.experiment.LineageCommand;
import org.labkey.remoteapi.experiment.LineageNode;
import org.labkey.remoteapi.experiment.LineageResponse;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleSetHelper;
import org.labkey.test.util.TestDataGenerator;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.util.DataRegionTable.DataRegion;

@Category({DailyA.class})
@BaseWebDriverTest.ClassTimeout(minutes = 20)
public class SampleSetTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleSetTestProject";
    private static final String FOLDER_NAME = "SampleSetTestFolder";
    private static final String LINEAGE_FOLDER = "LineageSampleSetFolder";
    private static final String LINEAGE_SAMPLE_SET_NAME = "LineageSampleSet";
    private static final String PROJECT_SAMPLE_SET_NAME = "ProjectSampleSet";
    private static final String PROJECT_PARENT_SAMPLE_SET_NAME = "ProjectParentSampleSet";
    private static final String FOLDER_SAMPLE_SET_NAME = "FolderSampleSet";
    private static final String PARENT_SAMPLE_SET_NAME = "ParentSampleSet";
    private static final String FOLDER_CHILDREN_SAMPLE_SET_NAME = "FolderChildrenSampleSet";
    private static final String FOLDER_GRANDCHILDREN_SAMPLE_SET_NAME = "FolderGrandchildrenSampleSet";
    private static final String CASE_INSENSITIVE_SAMPLE_SET = "CaseInsensitiveSampleSet";
    private static final String LOWER_CASE_SAMPLE_SET = "caseinsensitivesampleset";

    protected static final String PIPELINE_PATH = "/sampledata/xarfiles/expVerify";

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

        // Comment out this line (after you run once) it will make iterating on  tests much easier.
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

    // Uncomment this function (after you run once) it will make iterating on tests much easier.
//    @Override
//    protected void doCleanup(boolean afterTest)
//    {
//        log("Do nothing.");
//    }

    @Test
    public void doLineageDerivationTest()
    {
        String sampleText = "Name\tIntCol\tStringCol\n" +
                "Sample12ab\t1012\talpha\n" +
                "Sample13c4\t1023\tbeta\n" +
                "Sample14d5\t1024\tgamma\n" +
                "Sampleabcd\t1035\tepsilon\n" +
                "Sampledefg\t1046\tzeta";
        projectMenu().navigateToFolder(PROJECT_NAME, LINEAGE_FOLDER);
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(LINEAGE_SAMPLE_SET_NAME, null,
                Map.of("IntCol", FieldDefinition.ColumnType.Integer,
                        "StringCol", FieldDefinition.ColumnType.String),
                sampleText);

        // at this point, we're in the LINEAGE_FOLDER, on the Experiment tab, looking at the sample sets properties and Sample Set contents webparts.
        // now we add more samples,
        String deriveSamples = "Name\tMaterialInputs/LineageSampleSet\n" +
                "A\t\n" +
                "B\tA\n" +      // B and C both derive from A, so should get the same Run
                "C\tA\n" +      // D derives from B, so should get its own run
                "D\tB\n";
        sampleHelper.bulkImport(deriveSamples);

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

    @Test
    public void testCreateSampleSetNoExpression()
    {
        String sampleSetName = "SimpleCreateNoExp";
        Map<String, FieldDefinition.ColumnType> fields = Map.of("StringValue", FieldDefinition.ColumnType.String, "IntValue", FieldDefinition.ColumnType.Integer);
        log("Create a new sample set with a name and no name expression");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);

        SampleSetHelper sampleSetHelper = new SampleSetHelper(this);
        sampleSetHelper.goToCreateNewSampleSet();
        log("Verify the name field is required");
        clickButton("Create");
        assertTextPresent("You must supply a name for the sample set.");
        clickButton("Cancel");

        sampleSetHelper.createSampleSet(sampleSetName)
                .addFields(fields)
                .goToSampleSet(sampleSetName)
                .verifyFields();

        log("Add a single row to the sample set");
        Map<String, String> fieldMap = Map.of("Name", "S-1", "StringValue", "Ess", "IntValue", "1");
        sampleSetHelper.insertRow(fieldMap);

        log("Verify values were saved");
        sampleSetHelper.verifyDataValues(Collections.singletonList(fieldMap));

        List<Map<String, String>> data = new ArrayList<>();
        data.add(Map.of("Name", "S-2", "StringValue", "Tee", "IntValue", "2"));
        data.add(Map.of("Name", "S-3", "StringValue", "Ewe", "IntValue", "3"));
        sampleSetHelper.bulkImport(data);

        assertEquals("Number of samples not as expected", 3, sampleSetHelper.getSampleCount());

        sampleSetHelper.verifyDataValues(data);

        log("Try to create a sample set with the same name.");
        clickAndWait(Locator.linkWithText("Sample Sets"));
        sampleSetHelper = new SampleSetHelper(this);
        sampleSetHelper.createSampleSet(sampleSetName, null);
        assertTextPresent("A sample set with that name already exists.");

        clickButton("Cancel");
        DataRegionTable drt = DataRegion(this.getDriver()).find();
        assertEquals("Data region should be sample sets listing", "SampleSet", drt.getDataRegionName());
    }

    @Test
    public void testCreateSampleSetWithExpression()
    {
        String sampleSetName = "SimpleCreateWithExp";
        List<String> fieldNames = Arrays.asList("StringValue", "FloatValue");
        Map<String, FieldDefinition.ColumnType> fields = Map.of(fieldNames.get(0), FieldDefinition.ColumnType.String, fieldNames.get(1), FieldDefinition.ColumnType.Double);
        SampleSetHelper sampleSetHelper = new SampleSetHelper(this);
        log("Create a new sample set with a name and name expression");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        sampleSetHelper.createSampleSet(sampleSetName, "${" + fieldNames.get(0) + "}-${batchRandomId}-${randomId}")
                .addFields(fields)
                .goToSampleSet(sampleSetName)
                .verifyFields();

        log("Add data without supplying the name");
        Map<String, String> fieldMap = Map.of(fieldNames.get(0), "Vee", fieldNames.get(1), "1.6");
        sampleSetHelper.insertRow(fieldMap);

        log("Verify values are as expected with name expression saved");
        DataRegionTable drt = sampleSetHelper.getSamplesDataRegionTable();
        int index = drt.getRowIndex(fieldNames.get(0), "Vee");
        assertTrue("Did not find row containing data", index >= 0);
        Map<String, String> rowData = drt.getRowDataAsMap(index);
        assertTrue("Name not as expected", rowData.get("Name").startsWith("Vee-"));
        assertEquals(fieldNames.get(0) + " not as expected", "Vee", rowData.get(fieldNames.get(0)));
        assertEquals(fieldNames.get(1) + "not as expected", "1.6", rowData.get(fieldNames.get(1)));

        log("Add data with name provided");
        sampleSetHelper.insertRow(Map.of("Name", "NoExpression"));

        log("Verify values are as expected with name value saved");
        drt = sampleSetHelper.getSamplesDataRegionTable();
        index = drt.getRowIndex("Name", "NoExpression");
        assertTrue("Did not find row with inserted name", index >= 0);

        log ("Add multiple rows via simple (default) import mechanism");
        List<Map<String, String>> data = new ArrayList<>();
        data.add(Map.of(fieldNames.get(0), "Dubya", fieldNames.get(1), "2.1"));
        data.add(Map.of(fieldNames.get(0), "Ex", fieldNames.get(1), "4.2"));
        sampleSetHelper.bulkImport(data);

        assertEquals("Number of samples not as expected", 4, sampleSetHelper.getSampleCount());

        assertTrue("Should have row with first imported value", drt.getRowIndex(fieldNames.get(0), "Dubya") >= 0);
        assertTrue("Should have row with second imported value", drt.getRowIndex(fieldNames.get(0), "Ex") >= 0);
    }

    private void selectInsertOption(String value, int index)
    {
        List<WebElement> buttons = Locator.radioButtonByNameAndValue("insertOption", value).findElements(this.getDriver());
        buttons.get(index).click();
    }

    @Test
    public void testImportTypeOptions()
    {
        String sampleSetName = "ImportErrors";
        List<String> fieldNames = Arrays.asList("StringValue");

        log("Create a new sample set with a name");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(sampleSetName, null, Map.of("StringValue", FieldDefinition.ColumnType.String));

        log("Go to the sample set and add some data");
        clickAndWait(Locator.linkWithText(sampleSetName));
        DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents")
                .clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), "Name1");
        setFormElement(Locator.name("quf_" + fieldNames.get(0)), "Bee");
        clickButton("Submit");

        log("Try to import overlapping data with TSV");

        DataRegionTable drt = sampleHelper.getSamplesDataRegionTable();
        drt.clickImportBulkData();
        String header = "Name\t" + fieldNames.get(0) + "\n";
        String overlap =  "Name1\tToBee\n";
        String newData = "Name2\tSee\n";
        setFormElement(Locator.name("text"), header + overlap + newData);
        clickButton("Submit", "duplicate key");

        log("Switch to 'Insert and Update'");
        sampleHelper.selectImportOption(SampleSetHelper.MERGE_DATA_OPTION, 1);
        clickButton("Submit");

        log("Validate data was updated and new data added");
        drt = sampleHelper.getSamplesDataRegionTable();
        assertEquals("Number of samples not as expected", 2, drt.getDataRowCount());

        int index = drt.getRowIndex("Name", "Name1");
        assertTrue("Should have row with first sample name", index >= 0);
        Map<String, String> rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for sample 'Name1' not as expected", "ToBee", rowData.get(fieldNames.get(0)));

        index = drt.getRowIndex("Name", "Name2");
        assertTrue("Should have a row with the second sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for sample 'Name2' not as expected", "See", rowData.get(fieldNames.get(0)));

        log("Try to import overlapping data from file");
        drt.clickImportBulkData();
        click(Locator.tagWithText("h3", "Upload file (.xlsx, .xls, .csv, .txt)"));
        setFormElement(Locator.tagWithName("input", "file"), TestFileUtils.getSampleData("simpleSampleSet.xls").getAbsolutePath());
        clickButton("Submit", "duplicate key");

        log ("Switch to 'Insert and Update'");
        selectInsertOption("MERGE", 0);
        clickButton("Submit");
        log ("Validate data was updated and new data added");
        assertEquals("Number of samples not as expected", 3, drt.getDataRowCount());

        index = drt.getRowIndex("Name", "Name1");
        assertTrue("Should have row with first sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for sample 'Name1' not as expected", "NotTwoBee", rowData.get(fieldNames.get(0)));

        index = drt.getRowIndex("Name", "Name2");
        assertTrue("Should have a row with the second sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for sample 'Name2' not as expected", "Sea", rowData.get(fieldNames.get(0)));

        index = drt.getRowIndex("Name", "Name3");
        assertTrue("Should have a row with the thrid sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for sample 'Name' not as expected", "Dee", rowData.get(fieldNames.get(0)));

    }

    @Test
    public void testReservedFieldNames()
    {

        log("Validate that reservered values cannot be used as field names.");

        List<String> reserveredNames = Arrays.asList("Name", "Description", "Flag", "RowId", "SampleSet", "Folder", "Run", "Inputs", "Outputs");

        clickProject(PROJECT_NAME);

        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet("InvalidFieldNames");

        PropertiesEditor fieldProperties = new PropertiesEditor.PropertiesEditorFinder(getWrappedDriver()).withTitle("Field Properties").waitFor();

        fieldProperties.addField();

        StringBuilder errorMsg = new StringBuilder();

        reserveredNames.forEach(value -> {
            setFormElement(Locator.tagWithName("input", "ff_name0"), value);
            try
            {
                waitForElementToBeVisible(Locator.xpath("//input[@title=\"'" + value + "' is reserved\"]"));
            }
            catch (NoSuchElementException nse)
            {
                errorMsg.append(value);
                errorMsg.append(" is not marked as a reserved field name.");
                errorMsg.append("\n");
            }
        });

        if(errorMsg.length() > 0)
            Assert.fail(errorMsg.toString());

        clickButton("Cancel");

        log("Looks like all reserved filed names were caught.");
    }

    @Test
    public void testLineageWithImplicitParentColumn() throws IOException, CommandException
    {
        // create a sampleset with the following explicit domain columns
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "implicitParentage", getCurrentContainerPath())
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", "String"),
                        TestDataGenerator.simpleFieldDef("data", "int"),
                        TestDataGenerator.simpleFieldDef("stringData", "String")
                ));
        dgen.createDomain(createDefaultConnection(true), "SampleSet");
        dgen.addRow(List.of("A", 12, dgen.randomString(15)));
        dgen.addRow(List.of("B", 13, dgen.randomString(15)));
        dgen.addRow(List.of("C", 15, dgen.randomString(15)));
        dgen.addCustomRow(Map.of("name", "D", "data", 12, "stringData", dgen.randomString(15), "parent", "B"));
        dgen.addCustomRow(Map.of("name", "E", "data", 14, "stringData", dgen.randomString(15), "parent", "B"));
        dgen.addCustomRow(Map.of("name", "F", "data", 12, "stringData", dgen.randomString(15), "parent", "A,B"));
        dgen.addCustomRow(Map.of("name", "G", "data", 12, "stringData", dgen.randomString(15), "parent", "C"));
        dgen.addCustomRow(Map.of("name", "H", "data", 14, "stringData", dgen.randomString(15), "parent", "A,B,C"));
        dgen.addCustomRow(Map.of("name", "I", "data", 12, "stringData", dgen.randomString(15), "parent", "B,G"));

        SaveRowsResponse saveRowsResponse = dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        // get row 'B' after insert
        Map<String, Object> rowB = saveRowsResponse.getRows().stream().filter((a)-> a.get("name").equals("B"))
            .findFirst().orElse(null);
        Map<String, Object> rowH = saveRowsResponse.getRows().stream().filter((a)-> a.get("name").equals("H"))
                .findFirst().orElse(null);
        Map<String, Object> rowI = saveRowsResponse.getRows().stream().filter((a)-> a.get("name").equals("I"))
                .findFirst().orElse(null);

        refresh();
        DataRegionTable sampleSetList =  DataRegionTable.DataRegion(getDriver()).withName("SampleSet").waitFor();
        waitAndClick(Locator.linkWithText("implicitParentage"));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        // get the lineage graph
        LineageCommand lineageCommand = new LineageCommand.Builder(rowB.get("lsid").toString())
                .setChildren(true)
                .setParents(false)
                .setDepth(3).build();
        LineageResponse lineageResponse = lineageCommand.execute(createDefaultConnection(true), getCurrentContainerPath());
        List<LineageNode> nodeChildren = new ArrayList<>();
        for (LineageNode.Edge run : lineageResponse.getSeed().getChildren())
        {
            for (LineageNode.Edge child : run.getNode().getChildren()) // children in this context are runs
            {
                nodeChildren.add(child.getNode());      // in this case, D and E were processed as a single run
            }
        }
        assertEquals(5, nodeChildren.size());

        // now delete row B
        dgen.deleteRows(createDefaultConnection(true), List.of(rowB));

        // get the lineage graph
        LineageCommand parents = new LineageCommand.Builder(rowH.get("lsid").toString())
                .setChildren(false)
                .setParents(true)
                .setDepth(3).build();
        LineageResponse parentResponse = parents.execute(createDefaultConnection(true), getCurrentContainerPath());
        List<LineageNode> nodeParents = new ArrayList<>();
        for (LineageNode.Edge run : parentResponse.getSeed().getParents())
        {
            for (LineageNode.Edge child : run.getNode().getParents()) // children in this context are runs
            {
                nodeParents.add(child.getNode());      // in this case, D and E were processed as a single run
            }
        }
        assertEquals(2, nodeParents.size());
    }

    /**
     * regression coverage for https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=37465
     * @throws IOException
     * @throws CommandException
     */
    @Test
    public void testLookupWithInvalidLookupValue() throws IOException, CommandException
    {
        // create a sampleset
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "badLookupTest", getCurrentContainerPath())
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", "String"),
                        TestDataGenerator.simpleFieldDef("data", "int")
                ));
        dgen.createDomain(createDefaultConnection(true), "SampleSet");
        dgen.addCustomRow(Map.of("name", "A", "data", 12));     // no parent
        dgen.addCustomRow(Map.of("name", "B", "data", 12,  "MaterialInputs/badLookupTest", "A"));   // derives from A
        dgen.addCustomRow(Map.of("name", "C", "data", 12,  "MaterialInputs/badLookupTest", "A"));
        dgen.addCustomRow(Map.of("name", "D", "data", 12,  "MaterialInputs/badLookupTest", "BOGUS")); //<--bad lookup here
        try
        {
            dgen.insertRows(createDefaultConnection(true), dgen.getRows());
            fail("Expect CommandException when inserting bogus lookup");
        }catch (CommandException successMaybe)
        {
            assertTrue("expect bad lookup to produce error containing [Sample input 'BOGUS' in SampleSet 'badLookupTest' not found];\n" +
                            "instead got: [" + successMaybe.getMessage() + "]",
                    successMaybe.getMessage().contains("Sample input 'BOGUS' in SampleSet 'badLookupTest' not found"));
        }
    }

    /**
     *  regression coverage for https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=37465
     * @throws IOException
     * @throws CommandException
     */
    @Test
    public void testLookupWithInvalidParentColumnValue() throws IOException, CommandException
    {
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "badParentLookup", getCurrentContainerPath())
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", "String"),
                        TestDataGenerator.simpleFieldDef("data", "int")
                ));
        dgen.createDomain(createDefaultConnection(true), "SampleSet");
        dgen.addCustomRow(Map.of("name", "A", "data", 12));     // no parent
        dgen.addCustomRow(Map.of("name", "B", "data", 13,  "parent", "A"));   // derives from A
        dgen.addCustomRow(Map.of("name", "C", "data", 14,  "parent", "A"));
        dgen.addCustomRow(Map.of("name", "D", "data", 15,  "parent", "BOGUS")); //<--bad lookup here
        try
        {
            dgen.insertRows(createDefaultConnection(true), dgen.getRows());
            fail("Expect CommandException when inserting bogus lookup");
        }catch (CommandException successMaybe)  // success looks like a CommandException with the expected message
        {
            assertTrue("expect bad lookup to produce error containing [Sample input 'BOGUS' in SampleSet 'badLookupTest' not found];\n" +
                    "instead got: [" + successMaybe.getMessage() + "]",
                    successMaybe.getMessage().contains("Sample input 'BOGUS' in SampleSet 'badLookupTest' not found"));
        }

        log("foo");
    }

    /**
     * This test creates a domain with an explicit 'parent' column and supplies a set of lineage nodes that use the explicit
     * column and an ad-hoc column (MaterialInput/TableName) for lineage
     * The test then deletes some rows and confirms that values in the 'parent' columns persist when their parent row
     * is deleted, but lineage values in MaterialInputs/TableName do not persist after their parent is deleted,
     * @throws IOException
     * @throws CommandException
     */
    @Test
    public void deleteLineageParent() throws IOException, CommandException
    {
        // create a sampleset with the following explicit domain columns
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "Family", getCurrentContainerPath())
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", "String"),
                        TestDataGenerator.simpleFieldDef("parent", "String"),   // parent is a 'magic' field, looks up to name column
                        TestDataGenerator.simpleFieldDef("age", "int"),
                        TestDataGenerator.simpleFieldDef("height", "int")
                ))
                .addDataSupplier("parent", () -> null)  // don't put generated data into 'parent'- insert will fail if the value doesn't reference a name
                .withGeneratedRows(25);
        dgen.createDomain(createDefaultConnection(true), "SampleSet");
        dgen.addRow(List.of("A","null", 56, 60));
        dgen.addRow(List.of("B", "null", 48, 50));
        dgen.addRow(List.of("C", "A,B", 12, 50));
        dgen.addRow(List.of("D", "B", 15, 60));
        dgen.addCustomRow(Map.of("name", "E", "age", 12, "height", 44, "MaterialInputs/Family", "B"));
        dgen.addCustomRow(Map.of("name", "F", "age", 12, "height", 44, "MaterialInputs/Family", "A,B"));
        dgen.addRow(List.of("G", "C", 15, 60));
        dgen.addRow(List.of("H", "A,B,C", 15, 60));
        dgen.addRow(List.of("I", "G", 15, 60));
        SaveRowsResponse saveRowsResponse = dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        navigateToFolder(getProjectName(), LINEAGE_FOLDER);
        DataRegionTable sampleSetList =  DataRegionTable.DataRegion(getDriver()).withName("SampleSet").waitFor();
        waitAndClick(Locator.linkWithText("Family"));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        assertEquals(34, materialsList.getDataRowCount());

        // peel saved rows A,B,C from the insert response
        List<Map<String, Object>> rowsToDelete = saveRowsResponse.getRows().stream()
                .filter((a)-> a.get("name").equals("A") ||
                        a.get("name").equals("B") ||
                        a.get("name").equals("C")).collect(Collectors.toList());

        Map<String, Object> E = saveRowsResponse.getRows().stream()
                .filter((a)-> a.get("name").equals("E")).findFirst().orElse(null);
        LineageCommand lineageCommand = new LineageCommand.Builder(E.get("lsid").toString())
                .setChildren(false)
                .setParents(true)
                .setDepth(1).build();
        LineageResponse lineageResponse = lineageCommand.execute(createDefaultConnection(true), getCurrentContainerPath());
        assertEquals("don't expect MaterialInput/tablename lookup to persist records that have been deleted",
                1, lineageResponse.getSeed().getParents().size());

        // delete rows A, B, C
        dgen.deleteRows(createDefaultConnection(true), rowsToDelete);
        SelectRowsResponse selectResponse = dgen.getRowsFromServer(createDefaultConnection(true),
                List.of("rowId", "lsid", "name", "parent", "age", "height", "MaterialInputs/Family", "Inputs/First"));
        List<Map<String, Object>> remainingRows = selectResponse.getRows();

        // get rows with parents A, B, C
        Map<String, Object> rowD = remainingRows.stream()
                .filter((a)-> a.get("name").equals("D")).findFirst().orElse(null);
        Map<String, Object> rowG = remainingRows.stream()
                .filter((a)-> a.get("name").equals("G")).findFirst().orElse(null);
        Map<String, Object> rowH = remainingRows.stream()
                .filter((a)-> a.get("name").equals("H")).findFirst().orElse(null);

        // ensure that after deleting parents, these rows' derivation is still preserved in explicit column 'parent'
        assertEquals("Expect parent value to remain even if parent row is removed","B", rowD.get("parent"));
        assertEquals("Expect parent value to remain even if parent row is removed","C", rowG.get("parent"));
        assertEquals("Expect parent value to remain even if parent row is removed","A,B,C", rowH.get("parent"));

        // now make sure materialInputs derivations don't persist references to deleted records
        Map<String, Object> rowE = remainingRows.stream()
                .filter((a)-> a.get("name").equals("E")).findFirst().orElse(null);
        LineageCommand linCmd = new LineageCommand.Builder(rowE.get("lsid").toString())
                .setChildren(false)
                .setParents(true)
                .setDepth(1).build();
        LineageResponse linResponse = linCmd.execute(createDefaultConnection(true), getCurrentContainerPath());
        assertEquals("don't expect MaterialInput/tablename lookup to persist records that have been deleted",
                0, linResponse.getSeed().getParents().size());
    }

    @Test
    public void testInsertLargeLineageGraph() throws IOException, CommandException
    {
        // create a sampleset with the following explicit domain columns
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "bigLineage", getCurrentContainerPath())
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", "String"),
                        TestDataGenerator.simpleFieldDef("parent", "String"),   // parent is a 'magic' field, looks up to name column
                        TestDataGenerator.simpleFieldDef("data", "int"),
                        TestDataGenerator.simpleFieldDef("testIndex", "int")
                ));

        dgen.createDomain(createDefaultConnection(true), "SampleSet");
        Map indexRow = Map.of("name", "seed", "data", dgen.randomInt(3, 2000), "testIndex", 0); // create the first seed in the lineage
        SaveRowsResponse seedInsert = dgen.insertRows(createDefaultConnection(true), List.of(indexRow));
        SelectRowsResponse seedSelect = dgen.getRowsFromServer(createDefaultConnection(true),
                List.of("lsid", "name", "parent", "data", "testIndex"));

        // create a serial table of records; each derived from the former via parent:name column reference
        // insert them all at once
        String previousName = "seed";
        int testIndex = 1;
        int intendedGenerationDepth = 4999;
        for (int i = 0; i < intendedGenerationDepth; i++)
        {
            String name = dgen.randomString(30);
            Map row = Map.of("name", name, "data", dgen.randomInt(3, 1395), "testIndex", testIndex , "parent", previousName);
            dgen.addCustomRow(row);
            previousName = name;
            testIndex++;
        }
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());
        SelectRowsResponse insertedSelect = dgen.getRowsFromServer(createDefaultConnection(true),
                List.of("name", "parent", "data", "testIndex"));

        navigateToFolder(getProjectName(), LINEAGE_FOLDER);      // the dataregion is helpful when debugging, not needed for testing
        DataRegionTable sampleSetList =  DataRegionTable.DataRegion(getDriver()).withName("SampleSet").waitFor();
        waitAndClick(Locator.linkWithText("bigLineage"));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        Map<String, Object> seed = seedSelect.getRows().stream()
                .filter((a)-> a.get("testIndex").equals(0)).findFirst().orElse(null);
        LineageCommand linCmd = new LineageCommand.Builder(seed.get("lsid").toString())
                .setChildren(true)
                .setParents(false)
                .setDepth(intendedGenerationDepth).build();
        LineageResponse linResponse = linCmd.execute(createDefaultConnection(true), getCurrentContainerPath());
        LineageNode node = linResponse.getSeed();
        int generationDepth = 0;
        while(node.getChildren().size()>0)  // walk the node depth until the end
        {
            node = node.getChildren().get(0).getNode();
            generationDepth++;
        }
        assertEquals("Expect lineage depth to be" +intendedGenerationDepth, intendedGenerationDepth, generationDepth);
    }

    @Test
    public void testUpdateAndDeleteWithCommentsAndFlags()
    {
        final String SAMPLE_SET_NAME = "UpdateAndDeleteFields";
        final String SAMPLE_NAME_TO_DELETE = "ud01";
        final String SAMPLE_FLAG_UPDATE = "ud02";
        final String FLAG_UPDATE = "Updated Flag Value";
        final String SAMPLE_DESC_UPDATE = "ud03";
        final String DESC_UPDATE = "This is the updated description";
        final String SAMPLE_UPDATE_BOTH = "ud04";
        final String FLAG_UPDATE_1 = "New Flag Value";
        final String DESC_UPDATE_1 = "New description when one did not exist before.";
        final String FLAG_UPDATE_2 = "Flag Value Updated After Add";
        final String DESC_UPDATE_2 = "Updated description after adding a description.";

        StringBuilder errorLog = new StringBuilder();

        log("Validate that update and delete works correctly with the Comment and Flag fields.");

        clickProject(PROJECT_NAME);

        // Map.of creates an immutable collection I want to be able to update these data/collection items.
        Map<String, String> descriptionUpdate = new HashMap<>();
        descriptionUpdate.put("Name", SAMPLE_DESC_UPDATE);
        descriptionUpdate.put("Field01", "cc");
        descriptionUpdate.put("Description", "Here is the second description.");
        descriptionUpdate.put("Flag", "");

        Map<String, String> flagUpdate = new HashMap<>();
        flagUpdate.put("Name", SAMPLE_FLAG_UPDATE);
        flagUpdate.put("Field01", "bb");
        flagUpdate.put("Description", "");
        flagUpdate.put("Flag", "Flag Value 2");

        Map<String, String> updateBoth = new HashMap<>();
        updateBoth.put("Name", SAMPLE_UPDATE_BOTH);
        updateBoth.put("Field01", "dd");
        updateBoth.put("Description", "");
        updateBoth.put("Flag", "");

        List<Map<String, String>> sampleData = new ArrayList<>();

        sampleData.add(Map.of("Name", SAMPLE_NAME_TO_DELETE, "Field01", "aa", "Description", "This is description number 1.", "Flag", "Flag Value 1"));
        sampleData.add(flagUpdate);
        sampleData.add(descriptionUpdate);
        sampleData.add(updateBoth);

        // Some extra samples not really sure I will need them.
        sampleData.add(Map.of("Name", "ud05", "Field01", "ee", "Description", "This is description for sample 5.", "Flag", "Flag Value 5"));
        sampleData.add(Map.of("Name", "ud06", "Field01", "ff", "Description", "This is description for sample 6.", "Flag", "Flag Value 6"));

        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(SAMPLE_SET_NAME, null,
                Map.of("Field01",  FieldDefinition.ColumnType.String),
                sampleData);

        List<Map<String, String>> resultsFromDB = getSampleDataFromDB("/SampleSetTestProject","UpdateAndDeleteFields", Arrays.asList("Name", "Flag/Comment", "Field01", "Description"));

        Assert.assertTrue("Newly inserted Sample Set data not as expected. Stopping the test here.", areDataListEqual(resultsFromDB, sampleData));

        // Change the view so screen shot on failure is helpful.
        sampleHelper = new SampleSetHelper(this);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        CustomizeView cv = drtSamples.openCustomizeGrid();
        cv.addColumn("Description");
        cv.saveCustomView();

        log("Delete a record that has a description and an flag/comment");
        int rowIndex = drtSamples.getIndexWhereDataAppears(SAMPLE_NAME_TO_DELETE, "Name");
        drtSamples.checkCheckbox(rowIndex);
        drtSamples.clickHeaderButton("Delete");
        waitForElementToBeVisible(Locator.lkButton("Confirm Delete"));
        clickAndWait(Locator.lkButton("Confirm Delete"));

        // Remove the same row from the Sample Set input data.
        int testDataIndex = getSampleIndexFromTestInput(SAMPLE_NAME_TO_DELETE, sampleData);
        sampleData.remove(testDataIndex);

        log("Check that the Sample has been removed.");

        // Not going to use asserts (and possibly fail on first test), will try all the scenarios and then check at the end.
        String errorMsg = "Sample Set data is not as expected after a delete.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'delete sample' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Now update a sample's description.");

        testDataIndex = getSampleIndexFromTestInput(SAMPLE_DESC_UPDATE, sampleData);
        sampleData.get(testDataIndex).replace("Description", DESC_UPDATE);

        updateSampleSet(sampleData.get(testDataIndex));

        errorMsg = "Sample Set data is not as expected after a update of Description.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'update description' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Now delete the sample's description.");
        sampleData.get(testDataIndex).replace("Description", "");

        updateSampleSet(sampleData.get(testDataIndex));

        errorMsg = "Sample Set data is not as expected after deleting the Description.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'delete description' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Let's repeat it all again for a sample's flag/comment.");
        testDataIndex = getSampleIndexFromTestInput(SAMPLE_FLAG_UPDATE, sampleData);
        sampleData.get(testDataIndex).replace("Flag", FLAG_UPDATE);

        updateSampleSet(sampleData.get(testDataIndex));

        errorMsg = "Sample Set data is not as expected after a update of Flag/Comment.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'update flag/comment' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Now delete the sample's Flag/Comment.");
        sampleData.get(testDataIndex).replace("Flag", "");

        updateSampleSet(sampleData.get(testDataIndex));

        errorMsg = "Sample Set data is not as expected after deleting the Flag/Comment.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'delete flag/comment' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Finally update and delete both flag and description for a sample.");
        testDataIndex = getSampleIndexFromTestInput(SAMPLE_UPDATE_BOTH, sampleData);
        sampleData.get(testDataIndex).replace("Flag", FLAG_UPDATE_1);
        sampleData.get(testDataIndex).replace("Description", DESC_UPDATE_1);

        updateSampleSet(sampleData.get(testDataIndex));

        errorMsg = "Sample Set data is not as expected after a adding a Description and a Flag/Comment to an existing sample.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'adding a description and flag/comment' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Now update both values.");

        sampleData.get(testDataIndex).replace("Flag", FLAG_UPDATE_2);
        sampleData.get(testDataIndex).replace("Description", DESC_UPDATE_2);

        updateSampleSet(sampleData.get(testDataIndex));

        errorMsg = "Sample Set data is not as expected after a updating both a Description and a Flag/Comment.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'updating both a description and flag/comment' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Now delete both the Description and Flag/Comment from the sample.");
        sampleData.get(testDataIndex).replace("Flag", "");
        sampleData.get(testDataIndex).replace("Description", "");

        updateSampleSet(sampleData.get(testDataIndex));

        errorMsg = "Sample Set data is not as expected after deleting the Description and Flag/Comment.";
        if(!checkExpectedAgainstDB(sampleData, errorMsg))
        {
            errorLog.append("Failure with 'deleting both a description and flag/comment' test.\n");
            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        if(errorLog.length() > 0)
            Assert.fail(errorLog.toString());

        log("All done.");
    }

    private void updateSampleSet(Map<String, String> updatedFields)
    {
        List<Map<String, String>> updateSampleData = new ArrayList<>();
        updateSampleData.add(updatedFields);

        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.bulkImport(updateSampleData, SampleSetHelper.MERGE_DATA_OPTION);

    }

    private boolean checkExpectedAgainstDB(List<Map<String, String>> expectedData, String errorMsg)
    {
        boolean returnValue;
        List<Map<String, String>> resultsFromDB;

        resultsFromDB = getSampleDataFromDB("/SampleSetTestProject","UpdateAndDeleteFields", Arrays.asList("Name", "Flag/Comment", "Field01", "Description"));

        if(!areDataListEqual(resultsFromDB, expectedData))
        {
            log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

            returnValue = false;
        }
        else
        {
            returnValue = true;
        }
        return returnValue;

    }

    protected boolean areDataListEqual(List<Map<String, String>> list01, List<Map<String, String>> list02)
    {
        return areDataListEqual(list01, list02, true);
    }
    protected boolean areDataListEqual(List<Map<String, String>> list01, List<Map<String, String>> list02, boolean logMismatch)
    {
        if( list01.size() != list02.size())
            return false;

        // Order the two lists so compare can be done by index and not by searching the two lists.
        Collections.sort(list01, (Map<String, String> o1, Map<String, String> o2)->
                {
                    return o1.get("Name").compareTo(o2.get("Name"));
                }
        );

        Collections.sort(list02, (Map<String, String> o1, Map<String, String> o2)->
                {
                    return o1.get("Name").compareTo(o2.get("Name"));
                }
        );

        for(int i = 0; i < list01.size(); i++)
        {
            if(!list01.get(i).equals(list02.get(i)))
            {
                if(logMismatch)
                {
                    log("Found a mismatch in the lists.");
                    log("list01(" + i + "): " + list01.get(i));
                    log("list02(" + i + "): " + list02.get(i));
                }
                return false;
            }
        }

        return true;
    }

    protected int getSampleIndexFromTestInput(String sampleName, List<Map<String, String>> testData)
    {
        int index;
        for(index = 0; index < testData.size(); index++)
        {
            if(testData.get(index).get("Name").toString().equalsIgnoreCase(sampleName))
                break;
        }

        if(index < testData.size())
            return index;

        Assert.fail("Ummm... I couldn't find a sample with the name '" + sampleName + "' in the test data, are you sure it should be there?");

        // Need this otherwise I get a red squiggly.
        return -1;

    }

    protected List<Map<String, String>> getSampleDataFromDB(String folderPath, String sampleSetName, List<String> fields)
    {
        List<Map<String, String>> results = new ArrayList<>(6);
        Map<String, String> tempRow;

        Connection cn = new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        SelectRowsCommand cmd = new SelectRowsCommand("samples", sampleSetName);
        cmd.setColumns(fields);

        try
        {
            SelectRowsResponse response = cmd.execute(cn, folderPath);

            for (Map<String, Object> row : response.getRows())
            {

                tempRow = new HashMap<>();

                for(String key : row.keySet())
                {

                    if (fields.contains(key))
                    {

                        String tmpFlag = key;

                        if(key.equalsIgnoreCase("Flag/Comment"))
                            tmpFlag = "Flag";

                        if (null == row.get(key))
                        {
                            tempRow.put(tmpFlag, "");
                        }
                        else
                        {
                            tempRow.put(tmpFlag, row.get(key).toString());
                        }

                    }

                }

                results.add(tempRow);

            }

        }
        catch(CommandException | IOException excp)
        {
            Assert.fail(excp.getMessage());
        }

        return results;
    }

    @Test
    public void testMissingFieldIndicatorAndRequiredFields()
    {
        final String SAMPLE_SET_NAME = "MissingValues";
        final String INDICATOR_ONLY_SAMPLE_NAME = "mv02";
        final String VALUE_ONLY_SAMPLE_NAME = "mv04";
        final String BOTH_FIELDS_SAMPLE_NAME = "mv06";
        final String INCONSISTENT_SAMPLE_NAME = "mv07";
        final String UPDATE_SAMPLE_NAME = "mv08";

        final String REQUIRED_FIELD_NAME = "field01";
        final String MISSING_FIELD_NAME = "field02";
        String INDICATOR_FIELD_NAME;

        // Unfortunately Postgres and MSSQL case the missing indicator field differently. This causes issues when
        // getting the data by a db query and validating it against expected values.
        if(WebTestHelper.getDatabaseType().equals(WebTestHelper.DatabaseType.MicrosoftSQLServer))
            INDICATOR_FIELD_NAME = MISSING_FIELD_NAME + "_MVIndicator";
        else
            INDICATOR_FIELD_NAME = MISSING_FIELD_NAME + "_mvindicator";

        StringBuilder errorLog = new StringBuilder();

        log("Validate missing values and required fields in a Sample Set.");

        log("Create expected missing value indicators.");
        clickProject(PROJECT_NAME);

        final String MV_INDICATOR_01 = "Q";
        final String MV_DESCRIPTION_01 = "Data currently under quality control review.";
        final String MV_INDICATOR_02 = "N";
        final String MV_DESCRIPTION_02 = "Required field marked by site as 'data not available'.";
        final String MV_INDICATOR_03 = "X";
        final String MV_DESCRIPTION_03 = "Here is a non system one.";

        List<Map<String, String>> missingValueIndicators = new ArrayList<>();
        missingValueIndicators.add(Map.of("indicator", MV_INDICATOR_01, "description", MV_DESCRIPTION_01));
        missingValueIndicators.add(Map.of("indicator", MV_INDICATOR_02, "description", MV_DESCRIPTION_02));
        missingValueIndicators.add(Map.of("indicator", MV_INDICATOR_03, "description", MV_DESCRIPTION_03));

        setupMVIndicators(missingValueIndicators);

        clickProject(PROJECT_NAME);

        int expectedMissingCount = 0;
        List<Map<String, String>> sampleData = new ArrayList<>();

        Map<String, String> indicatorOnlySample = new HashMap<>();
        indicatorOnlySample.put("Name", INDICATOR_ONLY_SAMPLE_NAME);
        indicatorOnlySample.put(REQUIRED_FIELD_NAME, "bb_mv01");
        indicatorOnlySample.put(MISSING_FIELD_NAME, "");
        indicatorOnlySample.put(INDICATOR_FIELD_NAME, "Q");
        expectedMissingCount++;

        Map<String, String> valueOnlySample = new HashMap<>();
        valueOnlySample.put("Name", VALUE_ONLY_SAMPLE_NAME);
        valueOnlySample.put(REQUIRED_FIELD_NAME, "dd_mv01");
        valueOnlySample.put(MISSING_FIELD_NAME, "X");
        valueOnlySample.put(INDICATOR_FIELD_NAME, "");
        expectedMissingCount++;

        Map<String, String> bothFieldsSample = new HashMap<>();
        bothFieldsSample.put("Name", BOTH_FIELDS_SAMPLE_NAME);
        bothFieldsSample.put(REQUIRED_FIELD_NAME, "ff_mv01");
        bothFieldsSample.put(MISSING_FIELD_NAME, "N");
        bothFieldsSample.put(INDICATOR_FIELD_NAME, "N");
        expectedMissingCount++;

        // This may actually be a redundant test case. It is basically the same as the "both" test case.
        Map<String, String> inconsistentSample = new HashMap<>();
        inconsistentSample.put("Name", INCONSISTENT_SAMPLE_NAME);
        inconsistentSample.put(REQUIRED_FIELD_NAME, "gg_mv01");
        inconsistentSample.put(MISSING_FIELD_NAME, "Here is a valid string value.");
        inconsistentSample.put(INDICATOR_FIELD_NAME, "Q");
        expectedMissingCount++;

        Map<String, String> updateSample = new HashMap<>();
        updateSample.put("Name", UPDATE_SAMPLE_NAME);
        updateSample.put(REQUIRED_FIELD_NAME, "hh_mv01");
        updateSample.put(MISSING_FIELD_NAME, "X");
        updateSample.put(INDICATOR_FIELD_NAME, "X");
        expectedMissingCount++;

        sampleData.add(Map.of("Name", "mv01", REQUIRED_FIELD_NAME, "aa_mv01", MISSING_FIELD_NAME, "This value is here.", INDICATOR_FIELD_NAME, ""));
        sampleData.add(indicatorOnlySample);
        sampleData.add(Map.of("Name", "mv03", REQUIRED_FIELD_NAME, "cc_mv01", MISSING_FIELD_NAME, "Just to break things up.", INDICATOR_FIELD_NAME, ""));
        sampleData.add(valueOnlySample);
        sampleData.add(Map.of("Name", "mv05", REQUIRED_FIELD_NAME, "ee_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, ""));
        sampleData.add(bothFieldsSample);
        sampleData.add(inconsistentSample);
        sampleData.add(updateSample);

        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(SAMPLE_SET_NAME, null);
        List<FieldDefinition> fields = new ArrayList<>();
        fields.add(new FieldDefinition(REQUIRED_FIELD_NAME)
                .setType(FieldDefinition.ColumnType.String)
                .setMvEnabled(false)
                .setRequired(true));
        fields.add(new FieldDefinition(MISSING_FIELD_NAME)
                .setType(FieldDefinition.ColumnType.String)
                .setMvEnabled(true)
                .setRequired(false));

        sampleHelper.addFields(fields);

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));
        sampleHelper = new SampleSetHelper(this);

        sampleHelper.bulkImport(sampleData);

        // Change the view so the missing value indicator is there and for the screen shot is useful on failure.
        sampleHelper = new SampleSetHelper(this);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        CustomizeView cv = drtSamples.openCustomizeGrid();
        cv.showHiddenItems();
        cv.addColumn(INDICATOR_FIELD_NAME);
        cv.saveCustomView();

        List<Map<String, String>> resultsFromDB = getSampleDataFromDB("/" + PROJECT_NAME, SAMPLE_SET_NAME, Arrays.asList("Name", REQUIRED_FIELD_NAME, MISSING_FIELD_NAME, INDICATOR_FIELD_NAME));

        // After doing a bulk upload it looks like the value field is stored as an empty field in the DB.
        // Need to update the sample data to reflect what is expected from the DB.
        int testDataIndex = getSampleIndexFromTestInput(VALUE_ONLY_SAMPLE_NAME, sampleData);
        sampleData.get(testDataIndex).replace(INDICATOR_FIELD_NAME, sampleData.get(testDataIndex).get(MISSING_FIELD_NAME));
        sampleData.get(testDataIndex).replace(MISSING_FIELD_NAME, "");

        testDataIndex = getSampleIndexFromTestInput(BOTH_FIELDS_SAMPLE_NAME, sampleData);
        sampleData.get(testDataIndex).replace(MISSING_FIELD_NAME, "");

        testDataIndex = getSampleIndexFromTestInput(INCONSISTENT_SAMPLE_NAME, sampleData);
        sampleData.get(testDataIndex).replace(MISSING_FIELD_NAME, "");

        testDataIndex = getSampleIndexFromTestInput(UPDATE_SAMPLE_NAME, sampleData);
        sampleData.get(testDataIndex).replace(MISSING_FIELD_NAME, "");

        Assert.assertTrue("Newly inserted Sample Set data not as expected. Stopping the test here.", areDataListEqual(resultsFromDB, sampleData));

        // Not going to use asserts (and possibly fail on first test), will try all the scenarios and then check at the end.
        String errorMsg;
        if(getElementCount(Locator.xpath("//td[contains(@class, 'labkey-mv-indicator')]")) != expectedMissingCount)
        {
            errorMsg = "Number of missing value UI indicators is not as expected.\nExpected " + expectedMissingCount + " found " + getElementCount(Locator.xpath("//td[contains(@class, 'labkey-mv-indicator')]"));
            log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Now update a sample (give a value in the missing value field) and validate.");
        final String UPDATED_VALUE = "This should remove the unknown value indicator.";
        testDataIndex = getSampleIndexFromTestInput(UPDATE_SAMPLE_NAME, sampleData);
        sampleData.get(testDataIndex).replace(MISSING_FIELD_NAME, UPDATED_VALUE);
        sampleData.get(testDataIndex).replace(INDICATOR_FIELD_NAME, "");

        // TODO: Need to pass in all of the columns so as not to lose any data. See TODO comment below.
        List<Map<String, String>> updateSampleData = new ArrayList<>();
        updateSampleData.add(sampleData.get(testDataIndex));
        sampleHelper.bulkImport(updateSampleData, SampleSetHelper.MERGE_DATA_OPTION);
        expectedMissingCount--;

        // TODO: Need to revisit. When doing a bulk update if a field is missing the update views it as a request to
        //  set the value to empty. Why not view this as make no changes to the field value? And if we want to set
        //  the field to empty add the column to the update but give no value.
        // The commented out code below does this (set only the column I want to update.
//        Map<String, String> tempSample = new HashMap<>();
//        tempSample.put("Name", UPDATE_SAMPLE_NAME);
//        tempSample.put(MISSING_FIELD_NAME, UPDATED_VALUE);
//
//        List<Map<String, String>> updateSampleData = new ArrayList<>();
//        updateSampleData.add(tempSample);
//
//        sampleHelper.bulkImport(updateSampleData, SampleSetHelper.MERGE_DATA_OPTION);

        if(getElementCount(Locator.xpath("//td[contains(@class, 'labkey-mv-indicator')]")) != expectedMissingCount)
        {
            errorMsg = "After updating a value the number of missing UI indicators is not as expected.\nExpected " + expectedMissingCount + " found " + getElementCount(Locator.xpath("//td[contains(@class, 'labkey-mv-indicator')]"));
            log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        resultsFromDB = getSampleDataFromDB("/" + PROJECT_NAME, SAMPLE_SET_NAME, Arrays.asList("Name", REQUIRED_FIELD_NAME, MISSING_FIELD_NAME, INDICATOR_FIELD_NAME));

        if(!areDataListEqual(resultsFromDB, sampleData))
        {
            errorMsg = "After updating a value the data in the DB is not as expected.";
            log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        // Not really sure this is useful, we can remove in the future.
        log("Validate that the help div is shown when mouse over a missing value.");
        mouseOver(Locator.linkWithText(MV_INDICATOR_03));
        sleep(500);
        if(!isElementVisible(Locator.xpath("//span[@id='helpDivBody'][text()='" + MV_DESCRIPTION_03 + "']")))
        {
            errorMsg = "The missing value pop-up helper was shown as expected.\nExpected a div control with the text '" + MV_DESCRIPTION_03 + "'.";
            log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Now add a single sample via the UI");
        final String UI_INSERT_SAMPLE_NAME = "mv09";
        final String UI_STATIC_FIELD_TEXT = "This sample was added from the UI.";

        DataRegionTable drt = sampleHelper.getSamplesDataRegionTable();
        drt.clickInsertNewRow();

        Locator sampleNameElement = Locator.name("quf_Name");
        Locator sampleStaticFieldElement = Locator.name("quf_" + REQUIRED_FIELD_NAME);
        Locator sampleMissingFieldElement = Locator.name("quf_" + MISSING_FIELD_NAME);
        Locator sampleMissingFieldIndElement = Locator.name("quf_" + INDICATOR_FIELD_NAME);
        waitForElementToBeVisible(sampleNameElement);

        setFormElement(sampleNameElement, UI_INSERT_SAMPLE_NAME);
        setFormElement(sampleStaticFieldElement, UI_STATIC_FIELD_TEXT);
        selectOptionByValue(sampleMissingFieldIndElement, MV_INDICATOR_03);
        clickButton("Submit");
        expectedMissingCount++;

        // Add this element to expected sample data.
        sampleData.add(Map.of("Name", UI_INSERT_SAMPLE_NAME, REQUIRED_FIELD_NAME, UI_STATIC_FIELD_TEXT, MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, MV_INDICATOR_03));

        if(getElementCount(Locator.xpath("//td[contains(@class, 'labkey-mv-indicator')]")) != expectedMissingCount)
        {
            errorMsg = "After adding a sample with a missing value through the UI the number of missing UI indicators is not as expected.\nExpected " + expectedMissingCount + " found " + getElementCount(Locator.xpath("//td[contains(@class, 'labkey-mv-indicator')]"));
            log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        resultsFromDB = getSampleDataFromDB("/" + PROJECT_NAME, SAMPLE_SET_NAME, Arrays.asList("Name", REQUIRED_FIELD_NAME, MISSING_FIELD_NAME, INDICATOR_FIELD_NAME));

        if(!areDataListEqual(resultsFromDB, sampleData))
        {
            errorMsg = "After adding a sample with a missing value through the UI the data in the DB is not as expected.";
            log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Validate that the required field check works as expected.");
        updateSampleData = new ArrayList<>();
        updateSampleData.add(Map.of("Name", "mv10", REQUIRED_FIELD_NAME, "", MISSING_FIELD_NAME, "There should be no value in the required field.", INDICATOR_FIELD_NAME, ""));
        sampleHelper.bulkImport(updateSampleData, SampleSetHelper.IMPORT_DATA_OPTION, 0);

        boolean errorMsgShown;
        try
        {
            waitForElementToBeVisible(Locator.xpath("//div[contains(@class, 'labkey-error')][contains(text(),'Missing value for required property')]"));
            errorMsgShown = true;
            clickButton("Cancel");
        }
        catch(NoSuchElementException nse)
        {
            errorMsgShown = false;
        }

        if(!errorMsgShown)
        {
            errorMsg = "No error message was shown when a required field is missing.";
            log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        log("Now validate that adding a single row from the UI has the same behavior.");
        final String UI_MISSING_REQ_SAMPLE_NAME = "mv10";
        final String UI_MISSING_FIELD_TEXT = "This should generate an error.";
        drt = sampleHelper.getSamplesDataRegionTable();
        drt.clickInsertNewRow();
        waitForElementToBeVisible(sampleNameElement);

        setFormElement(sampleNameElement, UI_MISSING_REQ_SAMPLE_NAME);
        setFormElement(sampleMissingFieldElement, UI_MISSING_FIELD_TEXT);
        clickButton("Submit", 0);

        try
        {
            waitForElementToBeVisible(Locator.xpath("//span[contains(@class, 'help-block')]/font[@class='labkey-error'][text()='This field is required']"));
            errorMsgShown = true;
            clickButton("Cancel");
        }
        catch(NoSuchElementException nse)
        {
            errorMsgShown = false;
        }

        if(!errorMsgShown)
        {
            errorMsg = "No error message was shown when a required field is missing in the UI.";
            log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        // How about automation that updates an existing field?

        if(errorLog.length() > 0)
            Assert.fail(errorLog.toString());

        log("All done.");
    }

    @LogMethod
    private void setupMVIndicators(List<Map<String, String>> missingValueIndicators)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Missing Values"));
        uncheckCheckbox(Locator.checkboxById("inherit"));

        // Delete all site-level settings
        for (WebElement deleteButton : Locator.tagWithAttribute("img", "alt", "delete").findElements(getDriver()))
        {
            deleteButton.click();
            shortWait().until(ExpectedConditions.stalenessOf(deleteButton));
        }

        for(int index = 0; index < missingValueIndicators.size(); index++)
        {
            clickButton("Add", 0);
            WebElement mvInd = Locator.css("#mvIndicatorsDiv input[name=mvIndicators]").index(index).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
            setFormElement(mvInd, missingValueIndicators.get(index).get("indicator"));
            WebElement mvLabel = Locator.css("#mvIndicatorsDiv input[name=mvLabels]").index(index).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
            setFormElement(mvLabel, missingValueIndicators.get(index).get("description"));
        }
        clickButton("Save");
    }

    @Test
    public void testCreateAndDeriveSamples()
    {
        Map<String, FieldDefinition.ColumnType> sampleSetFields = Map.of("IntCol", FieldDefinition.ColumnType.Integer,
                "StringCol", FieldDefinition.ColumnType.String,
                "DateCol", FieldDefinition.ColumnType.DateTime,
                "BoolCol", FieldDefinition.ColumnType.Boolean);
        File sampleSetFile = TestFileUtils.getSampleData("sampleSet.xlsx");

        clickProject(PROJECT_NAME);
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(PROJECT_SAMPLE_SET_NAME, null, sampleSetFields, sampleSetFile);

        clickFolder(FOLDER_NAME);
        sampleHelper.createSampleSet(FOLDER_SAMPLE_SET_NAME, null,
                Map.of("IntCol-Folder",  FieldDefinition.ColumnType.Integer,
                       "StringCol-Folder", FieldDefinition.ColumnType.String),
                "Name\tIntCol-Folder\tStringCol-Folder\n" +
                        "SampleSetBVT11\t101\taa\n" +
                        "SampleSetBVT4\t102\tbb\n" +
                        "SampleSetBVT12\t102\tbb\n" +
                        "SampleSetBVT13\t103\tcc\n" +
                        "SampleSetBVT14\t104\tdd");

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

        setFormElement(Locator.name("outputSample1_Name"), "SampleSetBVT15");
        setFormElement(Locator.name("outputSample2_Name"), "SampleSetBVT16");
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

        setFormElement(Locator.name("outputSample1_Name"), "200");
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
    }

    @Test
    public void testAuditLog()
    {
        String sampleSetName = "TestAuditLogSampleSet";
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        SampleSetHelper helper = new SampleSetHelper(this);
        helper.createSampleSet(sampleSetName, null, Map.of(
                "First", FieldDefinition.ColumnType.String,
                "Second", FieldDefinition.ColumnType.Integer),
                "Name\tFirst\tSecond\n" +
                        "Audit-1\tsome\t100");

        goToModule("Query");
        viewQueryData("auditLog", "SampleSetAuditEvent");
        assertTextPresent(
                "Samples inserted or updated in: " + sampleSetName);

    }


    @Test
    public void testParentChild()
    {
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);

        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        log("Create parent sample set");
        sampleHelper.createSampleSet(PARENT_SAMPLE_SET_NAME, null,
                Map.of("IntCol",  FieldDefinition.ColumnType.Integer),
                "Name\tIntCol\n" +
                        "SampleSetBVT11\t101\n" +
                        "SampleSetBVT4\t102\n" +
                        "SampleSetBVT12\t102\n" +
                        "SampleSetBVT13\t103\n" +
                        "SampleSetBVT14\t104");

        log("Create child sample set");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        sampleHelper.createSampleSet(FOLDER_CHILDREN_SAMPLE_SET_NAME, null,
                Map.of("OtherProp", FieldDefinition.ColumnType.Double),
                "Name\tMaterialInputs/" + PARENT_SAMPLE_SET_NAME + "\tOtherProp\n" +
                        "SampleSetBVTChildA\tSampleSetBVT11\t1.1\n" +
                        "SampleSetBVTChildB\tSampleSetBVT4\t2.2\n"
                );


        // Make sure that the parent got wired up
        log("Verify parent references");
        DataRegionTable table = sampleHelper.getSamplesDataRegionTable();
        table.openCustomizeGrid();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addColumn(new String[]{"Inputs", "Materials", PARENT_SAMPLE_SET_NAME});
        _customizeViewsHelper.clickViewGrid();
        waitAndClickAndWait(Locator.linkWithText("SampleSetBVT4"));

        // Check out the run
        clickAndWait(Locator.linkWithText("Derive sample from SampleSetBVT4"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT4"));
        assertElementPresent(Locator.linkWithText("SampleSetBVTChildB"));

        // Make a grandchild set
        log("Create a grandparent sample set");
        clickTab("Experiment");
        clickAndWait(Locator.linkWithText("Sample Sets"));

        sampleHelper.createSampleSet(FOLDER_GRANDCHILDREN_SAMPLE_SET_NAME, null,
                Map.of("OtherProp", FieldDefinition.ColumnType.Double),
                "Name\tMaterialInputs/" + FOLDER_CHILDREN_SAMPLE_SET_NAME + "\tOtherProp\n" +
                        "SampleSetBVTGrandchildA\tSampleSetBVTChildA,SampleSetBVTChildB\t11.11\n");

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

        log("Change parents for the child samples");
        clickAndWait(Locator.linkWithText(FOLDER_CHILDREN_SAMPLE_SET_NAME));
        String REPARENTED_CHILD_SAMPLE_SET_TSV = "Name\tMaterialInputs/" + PARENT_SAMPLE_SET_NAME + "\tOtherProp\n" +
                "SampleSetBVTChildA\tSampleSetBVT13\t1.111\n" +
                "SampleSetBVTChildB\tSampleSetBVT14\t2.222\n";

        sampleHelper.bulkImport(REPARENTED_CHILD_SAMPLE_SET_TSV, SampleSetHelper.MERGE_DATA_OPTION);

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
    }

    @Test
    public void testParentInProjectSampleSet()
    {
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        clickProject(PROJECT_NAME);
        sampleHelper.createSampleSet(PROJECT_PARENT_SAMPLE_SET_NAME, null,
                Map.of("Field1", FieldDefinition.ColumnType.String),
                "Name\tField1\n" +
                        "ProjectS1\tsome value\n");

        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        sampleHelper.createSampleSet("ChildOfProject", null,
                Map.of("IntCol",  FieldDefinition.ColumnType.Integer),
                "Name\tMaterialInputs/" + PROJECT_PARENT_SAMPLE_SET_NAME + "\n" +
                        "COP1\tProjectS1\n");

        // Verify it got linked up correctly
        clickAndWait(Locator.linkWithText("COP1"));
        assertElementPresent(Locator.linkWithText("ProjectS1"));
    }

    @Test
    public void testCaseSensitivity()
    {
        SampleSetHelper sampleHelper = new SampleSetHelper(this);

        // make sure we are case-sensitive when creating samplesets -- regression coverage for issue 33743
        clickProject(PROJECT_NAME);
        sampleHelper.createSampleSet(CASE_INSENSITIVE_SAMPLE_SET);

        clickProject(PROJECT_NAME);
        sampleHelper.createSampleSet(LOWER_CASE_SAMPLE_SET);
        waitForElement(Locator.tagWithClass("div", "labkey-error").containing("A sample set with that name already exists."));
        clickProject(PROJECT_NAME);
        assertElementPresent(Locator.linkWithText(CASE_INSENSITIVE_SAMPLE_SET));
        assertElementNotPresent(Locator.linkWithText(LOWER_CASE_SAMPLE_SET));
    }

    @Test
    public void testLookUpValidatorForSampleSets()
    {
        final String SAMPLE_SET= "Sample with lookup validator";
        final String listName = "Fruits from Excel";

        log("Infer from excel file, then import data");
        _listHelper.createListFromFile(getProjectName(), listName, TestFileUtils.getSampleData("dataLoading/excel/fruits.xls"));
        waitForElement(Locator.linkWithText("pomegranate"));
        assertNoLabKeyErrors();
        int listRowCount = new DataRegionTable.DataRegionFinder(getDriver()).withName("query")
                .find()
                .getDataRowCount();

        goToProjectHome();
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(SAMPLE_SET);
        List<FieldDefinition> fields = new ArrayList<>();
        final String lookupColumnLabel = "Label for lookup column";
        fields.add(new FieldDefinition("Key")
                .setLabel(lookupColumnLabel)
                .setLookup(new FieldDefinition.LookupInfo(null, "lists", listName))
                .setValidator(new ListHelper.LookUpValidator()));
        sampleHelper.addFields(fields);

        goToProjectHome();
        clickAndWait(Locator.linkWithText(SAMPLE_SET));
        DataRegionTable table = sampleHelper.getSamplesDataRegionTable();
        table.clickInsertNewRow();

        setFormElement(Locator.name("quf_Name"),"1");
        selectOptionByText(Locator.name("quf_Key"),"apple");
        clickButton("Submit");

        assertEquals("Single row inserted",1, table.getDataRowCount());
        assertElementPresent(Locator.linkWithText("apple"));

        String missingPk = String.valueOf(listRowCount + 1);
        String tsvString =
                "Name\tKey\n" +
                "2\t" + missingPk;
        table.clickImportBulkData();
        setFormElement(Locator.id("tsv3"), tsvString);
        _listHelper.submitImportTsv_error("Value '" + missingPk + "' was not present in lookup target 'lists." + listName + "' for field '" + lookupColumnLabel + "'");
    }

    @Test
    public void fileAttachmentTest()
    {
        File experimentFilePath = new File(TestFileUtils.getLabKeyRoot() + PIPELINE_PATH, "experiment.xar.xml");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);

        String sampleSetName = "FileAttachmentSampleSet";
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(sampleSetName, null,
                Map.of("OtherProp", FieldDefinition.ColumnType.String,
                        "FileAttachment", FieldDefinition.ColumnType.File),
                "Name\tOtherProp\n" +
                        "FA-1\tOne\n" +
                        "FA-2\tTwo\n");

        Set<String> expectedHeaders = new HashSet<>();
        expectedHeaders.add("Name");
        expectedHeaders.add("Flag");
        expectedHeaders.add("Other Prop");
        expectedHeaders.add("File Attachment");

        setFileAttachment(0, experimentFilePath);
        setFileAttachment(1, new File(TestFileUtils.getLabKeyRoot() +  "/sampledata/sampleset/RawAndSummary~!@#$%^&()_+-[]{};',..xlsx"));

        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents");
        drt.clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), "SampleSetInsertedManually");
        setFormElement(Locator.name("quf_FileAttachment"), experimentFilePath);
        clickButton("Submit");
        //a double upload causes the file to be appended with a count
        assertTextPresent("experiment-1.xar.xml");
        int attachIndex = drt.getColumnIndex("File Attachment");

        // Added these last two test to check for regressions with exporting a grid with a file attachment column and deleting a file attachment column.
        exportGridWithAttachment(3, expectedHeaders, attachIndex, "experiment-1.xar.xml", "experiment.xar.xml", "rawandsummary~!@#$%^&()_+-[]{};',..xlsx");

        log("Remove the attachment columns and validate that everything still works.");
        waitAndClickAndWait(Locator.lkButton("Edit Fields"));
        PropertiesEditor fieldProperties = new PropertiesEditor.PropertiesEditorFinder(getDriver()).withTitle("Field Properties").waitFor();
        fieldProperties.selectField("FileAttachment").markForDeletion();

        // Can't use _listHelper.clickSave, it waits for a "Edit Design" button and a "Done" button.
        waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Save"), 0);
        waitForElement(Locator.lkButton("Edit Fields"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        expectedHeaders.remove("File Attachment");

        exportGridVerifyRowCountAndHeader(3, expectedHeaders);
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

    private Sheet exportGridVerifyRowCountAndHeader(int numRows, Set<String> expectedHeaders)
    {
        DataRegionTable list;
        DataRegionExportHelper exportHelper;
        File exportedFile;
        Workbook workbook;
        Sheet sheet;

        log("Export the grid to excel.");
        list = new DataRegionTable("Material", this.getDriver());
        exportHelper = new DataRegionExportHelper(list);
        exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLS);

        try
        {
            workbook = ExcelHelper.create(exportedFile);
            sheet = workbook.getSheetAt(0);

            assertEquals("Wrong number of rows exported to " + exportedFile.getName(), numRows, sheet.getLastRowNum());
            if (expectedHeaders != null)
            {
                Set<String> actualHeaders = new HashSet<>(ExcelHelper.getRowData(sheet, 0));
                assertEquals("Column headers not as expected", expectedHeaders, actualHeaders);
            }

            return sheet;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

    }

    private void exportGridWithAttachment(int numOfRows, Set<String> expectedHeaders, int exportColumn, String... expectedFilePaths)
    {
        Sheet sheet = exportGridVerifyRowCountAndHeader(numOfRows, expectedHeaders);
        List<String> exportedColumn;
        int row;

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


    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
