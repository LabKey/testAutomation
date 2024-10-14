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
import org.labkey.remoteapi.query.SaveRowsResponse;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
        SampleTypeLimitsTest init = (SampleTypeLimitsTest) getCurrentTest();
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
            FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", SAMPLE_TYPE_NAME);
            TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                    .withColumns(List.of(
                            TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                            TestDataGenerator.simpleFieldDef("label", FieldDefinition.ColumnType.String)));
            dgen.addDataSupplier("label", () -> TestDataGenerator.randomString(10))
                    .withGeneratedRows(10000);
            dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
            SaveRowsResponse saveRowsResponse = dgen.insertRows(createDefaultConnection(), dgen.getRows());
            log("Successfully  inserted " + saveRowsResponse.getRowsAffected());

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
        verifyValidLookupSample("quf_lookUpField", response.getRows().get(0).get("RowId").toString(), "Sample1", "query", false);
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
        goToProjectHome();

        log("Create sample type with lookup field to " + SAMPLE_TYPE_NAME);
        String sampleTypeName = "SampleTypeWithLookup";
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        SampleTypeDefinition definition = new SampleTypeDefinition(sampleTypeName);
        definition.addField(new FieldDefinition("label", FieldDefinition.ColumnType.String));
        definition.addField(new FieldDefinition("lookUpField",
                new FieldDefinition.LookupInfo(null, "exp.materials", "10000Samples")
                    .setTableType(FieldDefinition.ColumnType.Integer))
                    .setDescription("LookUp in same container with 10000 samples"));
        sampleHelper.createSampleType(definition);
        sampleHelper.goToSampleType(sampleTypeName);

        log("Insert one sample that we can use to derive from");
        insertSampleTypeRow("Material", "Test1");

        log("Attempt Derive Samples with invalid lookup value");
        initDeriveSamplesForm(sampleTypeName, "Derivative1");
        verifyInvalidLookupSample("outputSample1_lookUpField", "Sample3", "Could not convert value 'Sample3' (String) for Integer field 'lookUpField'.");

        log("Insert Derive Samples with valid lookup display value");
        verifyValidLookupSample("outputSample1_lookUpField", "Sample2", "Sample2", "Material", true);

        log("Insert Derive Samples with valid lookup to sample RowId");
        initDeriveSamplesForm(sampleTypeName, "Derivative2");
        SelectRowsCommand command = new SelectRowsCommand("samples", SAMPLE_TYPE_NAME);
        command.setFilters(Arrays.asList(new Filter("Name", "Sample1")));
        SelectRowsResponse response = command.execute(createDefaultConnection(), getProjectName());
        verifyValidLookupSample("outputSample1_lookUpField", response.getRows().get(0).get("RowId").toString(), "Sample1", "Material", true);
    }

    private void initDeriveSamplesForm(String sampleTypeName, String sampleName)
    {
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        samplesTable.uncheckAllOnPage();
        samplesTable.checkCheckbox(0);
        samplesTable.clickHeaderButtonAndWait("Derive Samples");
        selectOptionByText(Locator.name("targetSampleTypeId"), sampleTypeName + " in /" + getProjectName());
        clickButton("Next");
        setFormElement(Locator.name("outputSample1_Name"), sampleName);
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

        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        Map indexRow = Map.of("name", "seed", "data", TestDataGenerator.randomInt(3, 2000), "testIndex", 0); // create the first seed in the lineage
        SaveRowsResponse seedInsert = dgen.insertRows(createDefaultConnection(), List.of(indexRow));
        SelectRowsResponse seedSelect = dgen.getRowsFromServer(createDefaultConnection(),
                List.of("lsid", "name", "parent", "data", "testIndex"));

        // create a serial table of records; each derived from the former via parent:name column reference
        // insert them all at once
        String previousName = "seed";
        int testIndex = 1;
        int intendedGenerationDepth = 99;
        for (int i = 0; i < intendedGenerationDepth; i++)
        {
            String name = TestDataGenerator.randomString(30);
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
        while(node.getChildren().size()>0)  // walk the node depth until the end
        {
            node = node.getChildren().get(0).getNode();
            generationDepth++;
        }
        assertEquals("Expect lineage depth to be" +intendedGenerationDepth, intendedGenerationDepth, generationDepth);
    }

}
