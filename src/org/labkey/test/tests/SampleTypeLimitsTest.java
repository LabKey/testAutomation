package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.experiment.LineageCommand;
import org.labkey.remoteapi.experiment.LineageNode;
import org.labkey.remoteapi.experiment.LineageResponse;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyC;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExperimentalFeaturesHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.tests.SampleSetTest.SAMPLE_TYPE_DATA_REGION_NAME;
import static org.labkey.test.tests.SampleSetTest.SAMPLE_TYPE_DOMAIN_KIND;

/**
 * Test cases that use large amounts of data or in other ways stress the system. If they fail they can interfere with
 * other tests, and can be very troublesome when running locally.
 */
@Category({DailyC.class})
@BaseWebDriverTest.ClassTimeout(minutes = 10)
public class SampleTypeLimitsTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleTypeLimitsTest";

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

        Connection cn = createDefaultConnection();
        ExperimentalFeaturesHelper.setExperimentalFeature(cn, "resolve-lookups-by-value", true);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        Connection cn = createDefaultConnection();
        ExperimentalFeaturesHelper.setExperimentalFeature(cn, "resolve-lookups-by-value", false);
    }

    @Test
    public void testStringLookupFields() throws IOException, CommandException
    {
        String sampleTypeName = "10000Samples"; // Testing with 10,000 samples because as per the product the lookup is converted into text field only when the samples exceed 10,000 samples
        String listName = "MainList";

        goToProjectHome();
        new PortalHelper(this).addWebPart("Lists");

        log("Creating the sample type of 10000 samples");
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleTypeName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("label", FieldDefinition.ColumnType.String)));
        dgen.addDataSupplier("label", () -> dgen.randomString(10))
                .withGeneratedRows(10000);
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        SaveRowsResponse saveRowsResponse = dgen.insertRows(createDefaultConnection(), dgen.getRows());
        log("Successfully  inserted " + saveRowsResponse.getRowsAffected());

        log("Waiting for the sample data to get generated");
        goToProjectHome();
        waitAndClickAndWait(Locator.linkWithText(sampleTypeName));

        log("Inserting 10,001 row in the sampleset");
        DataRegionTable table = new DataRegionTable("Material", getDriver());
        table.clickInsertNewRow();

        setFormElement(Locator.name("quf_Name"), "Sample1");
        setFormElement(Locator.name("quf_label"), "Sample1");
        clickButton("Submit");

        log("Creating the list via API");
        ListDefinition listDef = new ListDefinition(listName);
        listDef.setKeyName("id");
        listDef.addField(new FieldDefinition("name", FieldDefinition.ColumnType.String));
        listDef.addField(new FieldDefinition("lookUpField",
                new FieldDefinition.LookupInfo(null, "exp.materials", "10000Samples")
                        .setTableType(FieldDefinition.ColumnType.Integer))
                .setDescription("LookUp in same container with 10000 samples"));

        listDef.getCreateCommand().execute(createDefaultConnection(), getProjectName());

        log("Inserting the new row in the list with the newly created sample as lookup");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));
        table = new DataRegionTable("query", getDriver());
        table.clickInsertNewRow();
        setFormElement(Locator.name("quf_id"), "1");
        setFormElement(Locator.name("quf_name"), "1");
        setFormElement(Locator.name("quf_lookUpField"), "Sample2");
        clickButton("Submit");

        String errMsg = Locators.labkeyError.findElement(getDriver()).getText();
        assertEquals("Expecpted error is different", "Could not convert value: Sample2", errMsg);

        setFormElement(Locator.name("quf_lookUpField"), "Sample1");
        clickButton("Submit");

        log("Verifying row is inserted correctly");
        table = new DataRegionTable("query", getDriver());
        assertEquals("Lookup field value is incorrect", Arrays.asList("Sample1"), table.getColumnDataAsText("lookUpField"));

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
        Map indexRow = Map.of("name", "seed", "data", dgen.randomInt(3, 2000), "testIndex", 0); // create the first seed in the lineage
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
            String name = dgen.randomString(30);
            Map row = Map.of("name", name, "data", dgen.randomInt(3, 1395), "testIndex", testIndex , "MaterialInputs/bigLineage", previousName);
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

        // clean up the sampleset on success
        dgen.deleteDomain(createDefaultConnection());
    }

}
