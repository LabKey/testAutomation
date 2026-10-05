/*
 * Copyright (c) 2020-2026 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.experiment.LineageCommand;
import org.labkey.remoteapi.experiment.LineageNode;
import org.labkey.remoteapi.experiment.LineageResponse;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.RowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.categories.Daily;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.params.list.VarListDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.query.QueryApiHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.labkey.test.util.TestDataGenerator.ALPHANUMERIC_STRING;
import static org.labkey.test.util.exp.SampleTypeAPIHelper.SAMPLE_TYPE_DATA_REGION_NAME;
import static org.labkey.test.util.exp.SampleTypeAPIHelper.SAMPLE_TYPE_DOMAIN_KIND;

/**
 * Test cases that use large amounts of data or in other ways stress the system. If they fail they can interfere with
 * other tests, and can be very troublesome when running locally.
 */
@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 10)
public class SampleTypeLimitsTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleTypeLimitsTest";
    private static final String SAMPLE_TYPE_NAME = "10000Samples"; // Testing with 10,000 samples because as per the product the lookup is converted into text field only when the samples exceed 10,000 samples

    @Override
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
        SampleTypeLimitsTest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(PROJECT_NAME, null);
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Sample Types");
        portalHelper.addWebPart("Lists");

        log("Creating the sample type of 10000 samples");
        try
        {
            TestDataGenerator dgen = new TestDataGenerator("exp.materials", SAMPLE_TYPE_NAME, getProjectName())
                    .withColumns(List.of(
                            TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                            TestDataGenerator.simpleFieldDef("label", FieldDefinition.ColumnType.String)));
            dgen.setAlphaNumericStr(true);
            dgen.addDataSupplier("label", () -> TestDataGenerator.randomString(10, null, ALPHANUMERIC_STRING))
                    .withGeneratedRows(10000);
            dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
            RowsResponse rowsResponse = dgen.insertRows(createDefaultConnection(), dgen.getRows());
            log("Successfully  inserted " + rowsResponse.getRowsAffected());

            log("Waiting for the sample data to get generated");
            goToProjectHome();
            waitAndClickAndWait(Locator.linkWithText(SAMPLE_TYPE_NAME));

            log("Inserting rows to make sample type >10,000 rows");
            insertSampleTypeRow("Material", "Sample1");
            insertSampleTypeRow("Material", "Sample2");
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
    }

    @Test
    public void testStringLookupFields() throws IOException, CommandException
    {
        goToProjectHome();

        log("Creating the list via API");
        String listName = "MainList";
        ListDefinition listDef = new VarListDefinition(listName);
        listDef.setKeyName("id");
        listDef.addField(new FieldDefinition("name", FieldDefinition.ColumnType.String));
        listDef.addField(new FieldDefinition("lookUpField",
                new FieldDefinition.LookupInfo(null, "exp.materials", "10000Samples")
                        .setTableType(FieldDefinition.ColumnType.Integer))
                .setDescription("LookUp in same container with 10000 samples"));
        listDef.getCreateCommand().execute(createDefaultConnection(), getProjectName());

        log("Inserting the new row in the list with the newly created sample display name");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));
        DataRegionTable table = DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();
        table.clickInsertNewRow();
        setFormElement(Locator.name("quf_id"), "1");
        setFormElement(Locator.name("quf_name"), "1");
        verifyInvalidLookupSample("quf_lookUpField", "Sample3", null);
        verifyValidLookupSample("quf_lookUpField", "Sample1");

        log("Verifying editing list row with the sample display name");
        table.clickEditRow("1");
        verifyInvalidLookupSample("quf_lookUpField", "Sample3", null);
        verifyValidLookupSample("quf_lookUpField", "Sample2");

        log("Verifying editing list row with the sample RowId");
        table.clickEditRow("1");
        SelectRowsCommand command = new SelectRowsCommand("samples", SAMPLE_TYPE_NAME);
        command.setFilters(Arrays.asList(new Filter("Name", "Sample1")));
        SelectRowsResponse response = command.execute(createDefaultConnection(), getProjectName());
        verifyValidLookupSample("quf_lookUpField", response.getRows().getFirst().get("RowId").toString(), "Sample1", "query", false);
    }

    private void verifyInvalidLookupSample(String fieldName, String sampleValue, @Nullable String expectedErrorMsg)
    {
        setFormElement(Locator.name(fieldName), sampleValue);
        clickButton("Submit");

        String errMsg = Locators.labkeyError.findElement(getDriver()).getText();
        assertEquals("Expected error is different", expectedErrorMsg == null ? "Could not convert value: " + sampleValue : expectedErrorMsg, errMsg);
    }

    private void verifyValidLookupSample(String fieldName, String sampleValue)
    {
        verifyValidLookupSample(fieldName, sampleValue, sampleValue, "query", false);
    }

    private void verifyValidLookupSample(String fieldName, String sampleValue, String sampleDisplay, String dataRegionName, boolean navigateViaBreadcrumb)
    {
        setFormElement(Locator.name(fieldName), sampleValue);
        clickButton("Submit");

        if (navigateViaBreadcrumb)
            clickAndWait(Locator.tagWithClass("ol", "breadcrumb").childTag("li").index(1).childTag("a"));

        log("Verifying row is inserted correctly");
        DataRegionTable table = DataRegionTable.DataRegion(getDriver()).withName(dataRegionName).waitFor();
        assertEquals("Lookup field value is incorrect", sampleDisplay, table.getDataAsText(0, "lookUpField"));
    }

    private void insertSampleTypeRow(String regionName, String rowValue)
    {
        DataRegionTable table = DataRegionTable.DataRegion(getDriver()).withName(regionName).waitFor();
        table.clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), rowValue);
        setFormElement(Locator.name("quf_label"), rowValue);
        clickButton("Submit");
    }

    @Test
    public void testDeriveSamplesLookupFields() throws IOException, CommandException
    {
        log("Create sample type with lookup field to " + SAMPLE_TYPE_NAME);
        String sampleTypeName = "SampleTypeWithLookup";
        new SampleTypeDefinition(sampleTypeName)
                .addField(new FieldDefinition("label", FieldDefinition.ColumnType.String))
                .addField(new FieldDefinition("lookUpField",
                        new FieldDefinition.LookupInfo(null, "exp.materials", SAMPLE_TYPE_NAME)
                                .setTableType(FieldDefinition.ColumnType.Integer))
                        .setDescription("LookUp in same container with 10000 samples"))
                .create(createDefaultConnection(), getProjectName());
        QueryApiHelper queryHelper = new QueryApiHelper(createDefaultConnection(), getProjectName(), "samples", sampleTypeName);

        log("Insert one sample that we can use to derive from");
        queryHelper.insertRows(List.of(Map.of("Name", "Test1", "label", "Test1")));
        String parentColumn = "MaterialInputs/" + sampleTypeName;

        log("Attempt to derive a sample with invalid lookup value");
        try
        {
            queryHelper.importData(deriveSampleTsv(parentColumn, "Derivative1", "Sample3"), true);
            fail("Deriving a sample with an invalid lookup value should fail");
        }
        catch (CommandException e)
        {
            checker().verifyTrue("Unexpected error for invalid lookup value: " + e.getMessage(), e.getMessage().contains("Sample3"));
        }

        log("Derive a sample with valid lookup display value");
        queryHelper.importData(deriveSampleTsv(parentColumn, "Derivative1", "Sample2"), true);

        log("Derive a sample with valid lookup to sample RowId");
        SelectRowsCommand command = new SelectRowsCommand("samples", SAMPLE_TYPE_NAME);
        command.setFilters(Arrays.asList(new Filter("Name", "Sample1")));
        SelectRowsResponse response = command.execute(createDefaultConnection(), getProjectName());
        queryHelper.importData(deriveSampleTsv(parentColumn, "Derivative2", response.getRows().getFirst().get("RowId").toString()), true);

        log("Verify derived samples have the expected lookup values");
        goToProjectHome();
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.goToSampleType(sampleTypeName);
        DataRegionTable table = sampleHelper.getSamplesDataRegionTable();
        checker().verifyEquals("Lookup field value is incorrect", "Sample2",
                table.getDataAsText(table.getRowIndex("Name", "Derivative1"), "lookUpField"));
        checker().verifyEquals("Lookup field value is incorrect", "Sample1",
                table.getDataAsText(table.getRowIndex("Name", "Derivative2"), "lookUpField"));
    }

    private String deriveSampleTsv(String parentColumn, String sampleName, String lookupValue)
    {
        return "Name\t" + parentColumn + "\tlookUpField\n" + sampleName + "\tTest1\t" + lookupValue + "\n";
    }

    @Test
    public void testInsertLargeLineageGraph() throws IOException, CommandException
    {
        goToProjectHome();
        // create a sampleset with the following explicit domain columns
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "bigLineage", getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("data", FieldDefinition.ColumnType.Integer),
                        TestDataGenerator.simpleFieldDef("testIndex", FieldDefinition.ColumnType.Integer)
                ));
        dgen.setAlphaNumericStr(true);
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        Map indexRow = Map.of("name", "seed", "data", TestDataGenerator.randomInt(3, 2000), "testIndex", 0); // create the first seed in the lineage
        RowsResponse seedInsert = dgen.insertRows(createDefaultConnection(), List.of(indexRow));
        SelectRowsResponse seedSelect = dgen.getRowsFromServer(createDefaultConnection(),
                List.of("lsid", "name", "parent", "data", "testIndex"));

        // create a serial table of records; each derived from the former via parent:name column reference
        // insert them all at once
        String previousName = "seed";
        int testIndex = 1;
        int intendedGenerationDepth = 99;
        for (int i = 0; i < intendedGenerationDepth; i++)
        {
            String name = TestDataGenerator.randomString(30, null, ALPHANUMERIC_STRING);
            Map row = Map.of("name", name, "data", TestDataGenerator.randomInt(3, 1395), "testIndex", testIndex , "MaterialInputs/bigLineage", previousName);
            dgen.addCustomRow(row);
            previousName = name;
            testIndex++;
        }
        dgen.insertRows(createDefaultConnection(), dgen.getRows());
        dgen.getRowsFromServer(createDefaultConnection(), List.of("name", "data", "testIndex"));

        goToProjectHome();      // the dataregion is helpful when debugging, not needed for testing
        DataRegionTable.DataRegion(getDriver()).withName(SAMPLE_TYPE_DATA_REGION_NAME).waitFor();
        waitAndClick(Locator.linkWithText("bigLineage"));
        DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        Map<String, Object> seed = seedSelect.getRows().stream()
                .filter((a)-> a.get("testIndex").equals(0)).findFirst().orElse(null);
        LineageCommand linCmd = new LineageCommand.Builder(seed.get("lsid").toString())
                .setChildren(true)
                .setParents(false)
                .setDepth(intendedGenerationDepth).build();
        LineageResponse linResponse = linCmd.execute(createDefaultConnection(), getCurrentContainerPath());
        LineageNode node = linResponse.getSeed();
        int generationDepth = 0;
        while(!node.getChildren().isEmpty())  // walk the node depth until the end
        {
            node = node.getChildren().getFirst().getNode();
            generationDepth++;
        }
        assertEquals("Expect lineage depth to be" +intendedGenerationDepth, intendedGenerationDepth, generationDepth);
    }

}
