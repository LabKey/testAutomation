package org.labkey.test.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.remoteapi.domain.GetDomainCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
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


    @Test
    public void sampleSetWithMissingValueField() throws IOException, CommandException
    {
        goToProjectHome("Home");
        String missingValueTable = "mvSamples";
        DomainResponse getMVSamplesDomain = new GetDomainCommand("exp.materials", missingValueTable)
                .execute(createDefaultConnection(true), getCurrentContainerPath());

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
        selectOptionByText(Locator.tagWithAttribute("select", "name", "quf_mvStringData_MVIndicator"),
                "Q");
        clickButton("Submit");
        materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        materialsList.clickEditRow(1);
        setFormElement(Locator.input("quf_mvStringData"), "otherValue");
        selectOptionByText(Locator.tagWithAttribute("select", "name", "quf_mvStringData_MVIndicator"),
                "N");
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
