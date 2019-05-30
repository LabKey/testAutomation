package org.labkey.test.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.AbstractDataRegionExportOrSignHelper;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.TestDataValidator;
import org.labkey.test.util.TextSearcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@Category({DailyA.class})
@BaseWebDriverTest.ClassTimeout(minutes = 10)
public class SampleSetRemoteAPITest extends BaseWebDriverTest
{
    public final String FOLDER_NAME = "Samples";
    public final String LINEAGE_FOLDER = "LineageSamples";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        SampleSetRemoteAPITest init = (SampleSetRemoteAPITest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME, new String[]{"Experiment"});
        _containerHelper.createSubfolder(getProjectName(), LINEAGE_FOLDER, new String[]{"Experiment"});

        projectMenu().navigateToProject(getProjectName());
        portalHelper.addWebPart("Sample Sets");

        projectMenu().navigateToFolder(getProjectName(), FOLDER_NAME);
        portalHelper.addWebPart("Sample Sets");

        projectMenu().navigateToFolder(getProjectName(), LINEAGE_FOLDER);
        portalHelper.addWebPart("Sample Sets");
    }

    @Before
    public void preTest() throws Exception
    {
        goToProjectHome();
    }

    /**
     * regression coverage for Issue 37514 - sampleset lookup to exp.Files crashes when viewing the sampleset
     * @throws IOException
     * @throws CommandException
     */
    @Test
    public void samplesWithLookupsToExpFilesTest() throws IOException, CommandException
    {
        // create a basic sampleset with a lookup reference to exp.Files
        String lookupContainer = getProjectName() + "/" + LINEAGE_FOLDER;
        navigateToFolder(getProjectName(), FOLDER_NAME);
        // create another with a lookup to it
        TestDataGenerator lookupDgen = new TestDataGenerator("exp.materials", "expFileSampleLookups", getCurrentContainerPath())
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("strLookup", FieldDefinition.ColumnType.Lookup)
                                .setLookup(new FieldDefinition.LookupInfo(lookupContainer, "exp", "Files")
                                        .setTableType("int"))
                ));
        lookupDgen.createDomain(createDefaultConnection(true), "SampleSet");
        lookupDgen.addCustomRow(Map.of("name", "B"));
        lookupDgen.insertRows(createDefaultConnection(true), lookupDgen.getRows());

        refresh();
        DataRegionTable sampleSetList =  DataRegionTable.DataRegion(getDriver()).withName("SampleSet").waitFor();

        // now attempt to view the table- should trip Issue 37514 and crash
        waitAndClick(Locator.linkWithText("expFileSampleLookups"));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        // clean up on success
        lookupDgen.deleteDomain(createDefaultConnection(true));
    }


    /**
     * generates a small sampleset, pastes data into it via the UI, including "Q" and "N" missing value indicators
     * @throws IOException
     * @throws CommandException
     */
    @Test
    public void importMissingValueSampleSet() throws IOException, CommandException
    {
        String missingValueTable = "mvSamplesForImport";
        String lookupContainer = getProjectName() + "/" + LINEAGE_FOLDER;
        navigateToFolder(getProjectName(), FOLDER_NAME);

        navigateToFolder(getProjectName(), FOLDER_NAME);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", missingValueTable, getCurrentContainerPath())
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("mvStringData", FieldDefinition.ColumnType.String)
                                .setMvEnabled(true).setLabel("MV Field"),
                        TestDataGenerator.simpleFieldDef("volume", FieldDefinition.ColumnType.Double)
                ));
        dgen.createDomain(createDefaultConnection(true), "SampleSet");
        dgen.addCustomRow(Map.of("name", "First", "mvStringData", "Q", "volume", 13.5));
        dgen.addCustomRow(Map.of("name", "Second", "mvStringData", "Q", "volume", 15.5));
        dgen.addCustomRow(Map.of("name", "Third","mvStringData", "N", "volume", 16.5));
        dgen.addCustomRow(Map.of("name", "Fourth","mvStringData", "N", "volume", 17.5));

        // write the domain data into TSV format, for import via the UI
        String importTsv = dgen.writeTsvContents();

        refresh();
        DataRegionTable sampleSetList =  DataRegionTable.DataRegion(getDriver()).withName("SampleSet").waitFor();
        waitAndClick(Locator.linkWithText(missingValueTable));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        // paste the TSV data into the form
        materialsList.clickImportBulkData();
        setFormElement(Locator.textarea("text"), importTsv);
        clickButton("Submit");

        // re-find materialsList after importing MV data
        materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        List<Map<String, String>> insertedRows = new ArrayList<>();
        for (int i=0; i<4; i++)
        {
            insertedRows.add(materialsList.getRowDataAsMap(i));
        }

        // confirm that every one of the initially-created rows were present with all values in the materialsList dataregion
        TestDataValidator validator = dgen.getValidator();      // validator has a copy of
        String error = validator.enumerateMissingRows(insertedRows, Arrays.asList("Flag"));  // ensure all 4 rows made it with expected values
        assertEquals("", error);    // if any expected rows are absent, error will describe what it expected but did not find
    }

    @Test
    @Ignore
    public void exportMissingValueSampleSetToTSV() throws CommandException, IOException
    {
        String missingValueTable = "mvSamplesForExport";
        navigateToFolder(getProjectName(), FOLDER_NAME);

        navigateToFolder(getProjectName(), FOLDER_NAME);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", missingValueTable, getCurrentContainerPath())
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("mvStringData", FieldDefinition.ColumnType.String)
                                .setMvEnabled(true).setLabel("MV Field"),
                        TestDataGenerator.simpleFieldDef("vol", FieldDefinition.ColumnType.Double)
                ));
        dgen.createDomain(createDefaultConnection(true), "SampleSet");
        dgen.addCustomRow(Map.of("name", "1st", "mvStringData", "Q", "vol", 17.5));
        dgen.addCustomRow(Map.of("name", "2nd", "mvStringData", "Q", "vol", 19.5));
        dgen.addCustomRow(Map.of("name", "3rd","mvStringData", "N", "vol", 22.25));
        dgen.addCustomRow(Map.of("name", "4th","mvStringData", "N", "vol", 38.75));
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());     // insert data via API rather than UI

        // prepare expected values-
        String expectedTSVData = dgen.writeTsvContents();
        String[] tsvRows = expectedTSVData.split("\n");
        List<String> dataRows = new ArrayList();
        for (int i=1; i < tsvRows.length; i++) // don't validate columns; we expect labels instead of column names
        {
            dataRows.add(tsvRows[i]);
        }

        refresh();
        DataRegionTable sampleSetList =  DataRegionTable.DataRegion(getDriver()).withName("SampleSet").waitFor();
        waitAndClick(Locator.linkWithText(missingValueTable));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        materialsList.checkAllOnPage();
        DataRegionExportHelper exportHelper = new DataRegionExportHelper(materialsList);
        File file = exportHelper.exportText(AbstractDataRegionExportOrSignHelper.TextSeparator.TAB);

        TextSearcher exportFileSearcher = new TextSearcher(file);
        String fileContents = Files.readString(Paths.get(file.getCanonicalPath()));
        log("parsing file contents, expecting [" + expectedTSVData + "]");
        log("actual: [" + fileContents + "]");
        for (String expectedRow : dataRows)
        {
            assertTextPresent(exportFileSearcher, expectedRow);
        }

        log("foo");
    }


    @Test
    public void sampleSetWithMissingValueField() throws IOException, CommandException
    {
        String missingValueTable = "mvSamples";

        navigateToFolder(getProjectName(), FOLDER_NAME);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", missingValueTable, getCurrentContainerPath())
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("mvStringData", FieldDefinition.ColumnType.String)
                                .setMvEnabled(true).setLabel("MV Field"),
                        TestDataGenerator.simpleFieldDef("volume", FieldDefinition.ColumnType.Double)
                ));
        dgen.createDomain(createDefaultConnection(true), "SampleSet");
        dgen.addCustomRow(Map.of("name", "First", "volume", 13.5));
        dgen.addCustomRow(Map.of("name", "Second", "volume", 15.5));
        dgen.addCustomRow(Map.of("name", "Third", "volume", 16.5));
        dgen.addCustomRow(Map.of("name", "Fourth", "volume", 17.5));
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        refresh();
        DataRegionTable sampleSetList =  DataRegionTable.DataRegion(getDriver()).withName("SampleSet").waitFor();
        waitAndClick(Locator.linkWithText(missingValueTable));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        materialsList.setSort("Volume", SortDirection.ASC);    // fix the order here so later when we edit a row by index it stays there on page refresh

        materialsList.clickEditRow(0);
        setFormElement(Locator.input("quf_mvStringData"), "testValue");

        /* SQL and PG handle casing differently- and labkey passes the differently-cased field names straight through.
        * Until we figure out a way to do case-insensitive matching over xpath, fork in the test code based on which DB we're running */
        if(WebTestHelper.getDatabaseType().equals(WebTestHelper.DatabaseType.MicrosoftSQLServer))
            selectOptionByText(Locator.tagWithAttribute("select", "name", "quf_mvStringData_MVIndicator"), "Q");
        else
            selectOptionByText(Locator.tagWithAttribute("select", "name", "quf_mvstringdata_mvindicator"), "Q");
        clickButton("Submit");

        materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        materialsList.clickEditRow(1);
        setFormElement(Locator.input("quf_mvStringData"), "otherValue");
        if(WebTestHelper.getDatabaseType().equals(WebTestHelper.DatabaseType.MicrosoftSQLServer))
            selectOptionByText(Locator.tagWithAttribute("select", "name", "quf_mvStringData_MVIndicator"), "N");
        else
            selectOptionByText(Locator.tagWithAttribute("select", "name", "quf_mvstringdata_mvindicator"), "N");
        clickButton("Submit");
        materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        Map<String, String> goodQARowValues = materialsList.getRowDataAsMap(0);
        assertEquals("First", goodQARowValues.get("Name"));
        assertEquals("Q", goodQARowValues.get("mvStringData"));
        assertEquals("13.5", goodQARowValues.get("volume"));
        Map<String, String> badQARowValues = materialsList.getRowDataAsMap(1);
        assertEquals("Second", badQARowValues.get("Name"));
        assertEquals("N", badQARowValues.get("mvStringData"));
        assertEquals("15.5", badQARowValues.get("volume"));
        Map<String, String> uneditedRowValues = materialsList.getRowDataAsMap(2);
        assertEquals("Third", uneditedRowValues.get("Name"));
        assertEquals(" ", uneditedRowValues.get("mvStringData"));
        assertEquals("16.5", uneditedRowValues.get("volume"));

        dgen.deleteDomain(createDefaultConnection(true));
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "SampleSetRemoteAPITest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }
}
