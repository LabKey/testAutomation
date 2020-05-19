package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.experiment.LineageCommand;
import org.labkey.remoteapi.experiment.LineageNode;
import org.labkey.remoteapi.experiment.LineageResponse;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyC;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.params.experiment.SampleSetDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExperimentalFeaturesHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleSetHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({DailyC.class})
@BaseWebDriverTest.ClassTimeout(minutes = 20)
public class SampleTypeLineageTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleType_Lineage_Test_Project";
    private static final String FOLDER_NAME = "SampleType_Lineage_Test_Folder";

    private static final String LINEAGE_FOLDER = "LineageSampleTyepFolder";
    private static final String LINEAGE_SAMPLE_SET_NAME = "LineageSampleType";

    private static final String PROJECT_SAMPLE_SET_NAME = "ProjectSampleType";
    private static final String PROJECT_PARENT_SAMPLE_SET_NAME = "ProjectParentSampleType";
    private static final String FOLDER_SAMPLE_SET_NAME = "FolderSampleType";
    private static final String PARENT_SAMPLE_SET_NAME = "ParentSampleType";
    private static final String FOLDER_CHILDREN_SAMPLE_SET_NAME = "FolderChildrenSampleSet";
    private static final String FOLDER_GRANDCHILDREN_SAMPLE_SET_NAME = "FolderGrandchildrenSampleSet";

    protected static final File PIPELINE_PATH = TestFileUtils.getSampleData("xarfiles/expVerify");

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
        SampleTypeLineageTest init = (SampleTypeLineageTest) getCurrentTest();

        // Comment out this line (after you run once) it will make iterating on  tests much easier.
        init.doSetup();
    }

    private void doSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(PROJECT_NAME, null);
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Sample Sets");

        _containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME, "Collaboration");
        portalHelper.addWebPart("Sample Sets");

        _containerHelper.createSubfolder(PROJECT_NAME, LINEAGE_FOLDER, "Collaboration");
        portalHelper.addWebPart("Sample Sets");
        portalHelper.exitAdminMode();

        Connection cn = createDefaultConnection(false);
        ExperimentalFeaturesHelper.setExperimentalFeature(cn, "resolve-lookups-by-value", true);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        Connection cn = createDefaultConnection(false);
        ExperimentalFeaturesHelper.setExperimentalFeature(cn, "resolve-lookups-by-value", false);
    }

    // Uncomment this function (after you run once) it will make iterating on tests much easier.
//    @Override
//    protected void doCleanup(boolean afterTest)
//    {
//        log("Do nothing.");
//    }

    /**
     *  coverage for https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=37466
     */
    @Test
    @Ignore
    public void testLineageWithImplicitParentColumn() throws IOException, CommandException
    {
        navigateToFolder(getProjectName(), LINEAGE_FOLDER);

        // create a sampleset with the following explicit domain columns
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "implicitParentage", getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("data", FieldDefinition.ColumnType.Integer),
                        TestDataGenerator.simpleFieldDef("stringData", FieldDefinition.ColumnType.String)
                ));
        dgen.createDomain(createDefaultConnection(true), "SampleSet");
        dgen.addRow(List.of("A", 12, dgen.randomString(15)));
        dgen.addRow(List.of("B", 13, dgen.randomString(15)));
        dgen.addRow(List.of("C", 15, dgen.randomString(15)));
        dgen.addCustomRow(Map.of("name", "D", "data", 12, "stringData", dgen.randomString(15), "MaterialInputs/implicitParentage", "B"));
        dgen.addCustomRow(Map.of("name", "E", "data", 14, "stringData", dgen.randomString(15), "MaterialInputs/implicitParentage", "B"));
        dgen.addCustomRow(Map.of("name", "F", "data", 12, "stringData", dgen.randomString(15), "MaterialInputs/implicitParentage", "A,B"));
        dgen.addCustomRow(Map.of("name", "G", "data", 12, "stringData", dgen.randomString(15), "MaterialInputs/implicitParentage", "C"));
        dgen.addCustomRow(Map.of("name", "H", "data", 14, "stringData", dgen.randomString(15), "MaterialInputs/implicitParentage", "A,B,C"));
        dgen.addCustomRow(Map.of("name", "I", "data", 12, "stringData", dgen.randomString(15), "MaterialInputs/implicitParentage", "B,G"));

        SaveRowsResponse saveRowsResponse = dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        // get row 'B' after insert
        Map<String, Object> rowB = saveRowsResponse.getRows().stream().filter((a)-> a.get("name").equals("B"))
                .findFirst().orElse(null);
        Map<String, Object> rowH = saveRowsResponse.getRows().stream().filter((a)-> a.get("name").equals("H"))
                .findFirst().orElse(null);
        Map<String, Object> rowI = saveRowsResponse.getRows().stream().filter((a)-> a.get("name").equals("I"))
                .findFirst().orElse(null);

        refresh();
        DataRegionTable.DataRegion(getDriver()).withName("SampleSet").waitFor();
        waitAndClickAndWait(Locator.linkWithText("implicitParentage"));
        DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

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
        assertEquals( "Issue 37466: Expect rowH to still derive from A and C",2, nodeParents.size());

        // only delete on success
        dgen.deleteDomain(createDefaultConnection(true));
    }

    @Test
    public void testParentInProjectSampleType()
    {
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        clickProject(PROJECT_NAME);
        sampleHelper.createSampleSet(new SampleSetDefinition(PROJECT_PARENT_SAMPLE_SET_NAME).setFields(
                List.of(new FieldDefinition("Field1", FieldDefinition.ColumnType.String))),
                "Name\tField1\n" +
                        "ProjectS1\tsome value\n");

        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        sampleHelper.createSampleSet(new SampleSetDefinition("ChildOfProject").setFields(
                List.of(new FieldDefinition("IntCol",  FieldDefinition.ColumnType.Integer))),
                "Name\tMaterialInputs/" + PROJECT_PARENT_SAMPLE_SET_NAME + "\n" +
                        "COP1\tProjectS1\n");

        // Verify it got linked up correctly
        clickAndWait(Locator.linkWithText("COP1"));
        assertElementPresent(Locator.linkWithText("ProjectS1"));
    }

    @Test
    public void testCreateAndDeriveSamples()
    {
        List<FieldDefinition> sampleSetFields = List.of(
                new FieldDefinition("IntCol", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("StringCol", FieldDefinition.ColumnType.String),
                new FieldDefinition("DateCol", FieldDefinition.ColumnType.DateAndTime),
                new FieldDefinition("BoolCol", FieldDefinition.ColumnType.Boolean));
        File sampleSetFile = TestFileUtils.getSampleData("sampleSet.xlsx");

        clickProject(PROJECT_NAME);
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(new SampleSetDefinition(PROJECT_SAMPLE_SET_NAME).setFields(sampleSetFields), sampleSetFile);

        clickFolder(FOLDER_NAME);
        sampleHelper.createSampleSet(new SampleSetDefinition(FOLDER_SAMPLE_SET_NAME).setFields(
                List.of(new FieldDefinition("IntCol-Folder",  FieldDefinition.ColumnType.Integer),
                        new FieldDefinition("StringCol-Folder", FieldDefinition.ColumnType.String))),
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
            setPipelineRoot(PIPELINE_PATH.getAbsolutePath());
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
        selectOptionByText(Locator.name("targetSampleSetId"), "FolderSampleType in /" + getProjectName() + "/SampleType_Lineage_Test_Folder");
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
        selectOptionByText(Locator.name("targetSampleSetId"), "ProjectSampleType in /" + getProjectName());
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
        assertTextPresent("ProjectSampleType", "200");
    }

    /**
     * This test creates a domain with an explicit 'parent' column and supplies a set of lineage nodes that use the explicit
     * column and an ad-hoc column (MaterialInput/TableName) for lineage
     * The test then deletes some rows and confirms that values in the 'parent' columns persist when their parent row
     * is deleted, but lineage values in MaterialInputs/TableName do not persist after their parent is deleted,
     */
    @Test
    public void testDeleteLineageParent() throws IOException, CommandException
    {
        navigateToFolder(getProjectName(), LINEAGE_FOLDER);

        // create a sampleset with the following explicit domain columns
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "Family", getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("age", FieldDefinition.ColumnType.Integer),
                        TestDataGenerator.simpleFieldDef("height", FieldDefinition.ColumnType.Integer)
                ));
        dgen.createDomain(createDefaultConnection(true), "SampleSet");
        dgen.addRow(List.of("A", 56, 60));
        dgen.addRow(List.of("B", 48, 50));
        dgen.addCustomRow(Map.of("name", "C", "age", 12, "height", 44, "MaterialInputs/Family", "A,B"));
        dgen.addCustomRow(Map.of("name", "D", "age", 12, "height", 44, "MaterialInputs/Family", "B"));
        dgen.addCustomRow(Map.of("name", "E", "age", 12, "height", 44, "MaterialInputs/Family", "B"));
        dgen.addCustomRow(Map.of("name", "F", "age", 12, "height", 44, "MaterialInputs/Family", "A,B"));
        dgen.addCustomRow(Map.of("name", "G", "age", 12, "height", 44, "MaterialInputs/Family", "C"));
        dgen.addCustomRow(Map.of("name", "H", "age", 12, "height", 44, "MaterialInputs/Family", "A,B,C"));
        dgen.addCustomRow(Map.of("name", "I", "age", 12, "height", 44, "MaterialInputs/Family", "G"));
        SaveRowsResponse saveRowsResponse = dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        refresh();
        DataRegionTable sampleSetList =  DataRegionTable.DataRegion(getDriver()).withName("SampleSet").waitFor();
        waitAndClick(Locator.linkWithText("Family"));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        assertEquals(9, materialsList.getDataRowCount());

        // peel saved rows A and B from the insert response
        List<Map<String, Object>> rowsToDelete = saveRowsResponse.getRows().stream()
                .filter((a)-> a.get("name").equals("B") ||
                        a.get("name").equals("A")).collect(Collectors.toList());

        Map<String, Object> E = saveRowsResponse.getRows().stream()
                .filter((a)-> a.get("name").equals("E")).findFirst().orElse(null);
        LineageCommand lineageCommand = new LineageCommand.Builder(E.get("lsid").toString())
                .setChildren(false)
                .setParents(true)
                .setDepth(1).build();
        LineageResponse lineageResponse = lineageCommand.execute(createDefaultConnection(true), getCurrentContainerPath());
        assertEquals("don't expect MaterialInput/tablename columns to persist records that have been deleted",
                1, lineageResponse.getSeed().getParents().size());

        // delete rows A, B
        dgen.deleteRows(createDefaultConnection(true), rowsToDelete);
        SelectRowsResponse selectResponse = dgen.getRowsFromServer(createDefaultConnection(true),
                List.of("rowId", "lsid", "name", "parent", "age", "height", "MaterialInputs/Family", "Inputs/First"));
        List<Map<String, Object>> remainingRows = selectResponse.getRows();

        // now make sure materialInputs derivations don't persist references to deleted records
        Map<String, Object> rowE = remainingRows.stream()
                .filter((a)-> a.get("name").equals("E")).findFirst().orElse(null);
        LineageCommand linCmd = new LineageCommand.Builder(rowE.get("lsid").toString())
                .setChildren(false)
                .setParents(true)
                .setDepth(1).build();
        LineageResponse linResponse = linCmd.execute(createDefaultConnection(true), getCurrentContainerPath());
        assertEquals("don't expect MaterialInput/tablename columns to persist records that have been deleted",
                0, linResponse.getSeed().getParents().size());

        dgen.deleteDomain(createDefaultConnection(true));
    }

    @Test
    public void testDeleteSampleSources() throws CommandException, IOException
    {
        SampleSetDefinition sampleSet = new SampleSetDefinition("DeleteSourcesSamples").addField(new FieldDefinition("strCol"));
        DataClassDefinition dataClass = new DataClassDefinition("DeleteSourcesData").addField(new FieldDefinition("strCol"));

        TestDataGenerator sampleGenerator = TestDataGenerator.createDomain(getProjectName(), sampleSet);
        TestDataGenerator dataGenerator = TestDataGenerator.createDomain(getProjectName(), dataClass);

        final String sampleParentKey = "MaterialInputs/" + sampleSet.getName();
        final String dataParentKey = "DataInputs/" + dataClass.getName();

        final String dataParentA = "DPD-A";
        final String dataParentB = "DPD-B";
        final String dataParents = "DPD-A,DPD-B";
        dataGenerator.addCustomRow(Map.of("Name", dataParentA));
        dataGenerator.addCustomRow(Map.of("Name", dataParentB));
        dataGenerator.insertRows();

        final String sampleParentA = "DPS-A";
        final String sampleParentB = "DPS-B";
        final String sampleParents = "DPS-A,DPS-B";
        sampleGenerator.addRow(List.of(sampleParentA, "a-v1"));
        sampleGenerator.addRow(List.of(sampleParentB, "b-v1"));
        final String sampleC = "DPS-C";
        final String sampleD = "DPS-D";
        final String sampleE = "DPS-E";
        final String sampleF = "DPS-F";
        final String sampleG = "DPS-G";
        final String sampleH = "DPS-H";
        final String sampleI = "DPS-I";
        sampleGenerator.addCustomRow(Map.of("name", sampleC, "strCol", "c-v1", sampleParentKey, sampleParents, dataParentKey, dataParents));
        sampleGenerator.addCustomRow(Map.of("name", sampleD, "strCol", "d-v1", sampleParentKey, sampleParents, dataParentKey, dataParents));
        sampleGenerator.addCustomRow(Map.of("name", sampleE, "strCol", "e-v1", sampleParentKey, sampleParents, dataParentKey, dataParents));
        sampleGenerator.addCustomRow(Map.of("name", sampleF, "strCol", "f-v1", sampleParentKey, sampleParents, dataParentKey, dataParents));
        sampleGenerator.addCustomRow(Map.of("name", sampleG, "strCol", "g-v1", sampleParentKey, sampleParents, dataParentKey, dataParents));
        sampleGenerator.addCustomRow(Map.of("name", sampleH, "strCol", "h-v1", sampleParentKey, sampleParents, dataParentKey, dataParents));
        sampleGenerator.addCustomRow(Map.of("name", sampleI, "strCol", "i-v1", sampleParentKey, sampleParents, dataParentKey, dataParents));
        sampleGenerator.insertRows(createDefaultConnection(true), sampleGenerator.getRows());

        goToProjectHome();

        SampleSetHelper sampleHelper = new SampleSetHelper(getDriver());

        goToModule("Experiment");
        sampleHelper.goToSampleSet(sampleSet.getName());
        saveLocation();

        sampleHelper.mergeImport(List.of(
                Map.of("name", sampleC, "strCol", "c-v2") // Just update data
        ));
        clickAndWait(Locator.linkWithText(sampleC));
        assertElementPresent(Locator.linkWithText(sampleParentA));
        assertElementPresent(Locator.linkWithText(sampleParentB));
        assertElementPresent(Locator.linkWithText(dataParentA));
        assertElementPresent(Locator.linkWithText(dataParentB));

        recallLocation();
        sampleHelper.mergeImport(List.of(
                Map.of("name", sampleD, sampleParentKey, sampleParents) // Don't specify an existing parent column
        ));
        clickAndWait(Locator.linkWithText(sampleD));
        assertElementPresent(Locator.linkWithText(sampleParentA));
        assertElementPresent(Locator.linkWithText(sampleParentB));
        assertElementPresent(Locator.linkWithText(dataParentA));
        assertElementPresent(Locator.linkWithText(dataParentB));

        recallLocation();
        sampleHelper.mergeImport(List.of(
                Map.of("name", sampleE, sampleParentKey, sampleParents, dataParentKey, "") // Clear one parent column
        ));
        clickAndWait(Locator.linkWithText(sampleE));
        assertElementPresent(Locator.linkWithText(sampleParentA));
        assertElementPresent(Locator.linkWithText(sampleParentB));
        assertElementNotPresent(Locator.linkWithText(dataParentA));
        assertElementNotPresent(Locator.linkWithText(dataParentB));

        recallLocation();
        sampleHelper.mergeImport(List.of(
                Map.of("name", sampleF, sampleParentKey, "", dataParentKey, "") // Clear all lineage columns
        ));
        clickAndWait(Locator.linkWithText(sampleF));
        assertElementNotPresent(Locator.linkWithText(sampleParentA));
        assertElementNotPresent(Locator.linkWithText(sampleParentB));
        assertElementNotPresent(Locator.linkWithText(dataParentA));
        assertElementNotPresent(Locator.linkWithText(dataParentB));

        recallLocation();
        sampleHelper.mergeImport(List.of(
                Map.of("name", sampleG, sampleParentKey, sampleParents, dataParentKey, dataParents), // Leave unmodified
                Map.of("name", sampleH, sampleParentKey, sampleParentA, dataParentKey, dataParentB), // Modify lineage columns
                Map.of("name", sampleI, sampleParentKey, "", dataParentKey, "") // Modify lineage columns
        ));
        clickAndWait(Locator.linkWithText(sampleG));
        assertElementPresent(Locator.linkWithText(sampleParentA));
        assertElementPresent(Locator.linkWithText(sampleParentB));
        assertElementPresent(Locator.linkWithText(dataParentA));
        assertElementPresent(Locator.linkWithText(dataParentB));
        recallLocation();
        clickAndWait(Locator.linkWithText(sampleH));
        assertElementPresent(Locator.linkWithText(sampleParentA));
        assertElementNotPresent(Locator.linkWithText(sampleParentB));
        assertElementNotPresent(Locator.linkWithText(dataParentA));
        assertElementPresent(Locator.linkWithText(dataParentB));
        recallLocation();
        clickAndWait(Locator.linkWithText(sampleI));
        assertElementNotPresent(Locator.linkWithText(sampleParentA));
        assertElementNotPresent(Locator.linkWithText(sampleParentB));
        assertElementNotPresent(Locator.linkWithText(dataParentA));
        assertElementNotPresent(Locator.linkWithText(dataParentB));

    }

    @Test
    public void testParentChild()
    {
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);

        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        log("Create parent sample set");
        sampleHelper.createSampleSet(new SampleSetDefinition(PARENT_SAMPLE_SET_NAME).setFields(
                List.of(new FieldDefinition("IntCol",  FieldDefinition.ColumnType.Integer))),
                "Name\tIntCol\n" +
                        "SampleSetBVT11\t101\n" +
                        "SampleSetBVT4\t102\n" +
                        "SampleSetBVT12\t102\n" +
                        "SampleSetBVT13\t103\n" +
                        "SampleSetBVT14\t104");

        log("Create child sample set");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        sampleHelper.createSampleSet(new SampleSetDefinition(FOLDER_CHILDREN_SAMPLE_SET_NAME).setFields(
                List.of(new FieldDefinition("OtherProp", FieldDefinition.ColumnType.Decimal))),
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
        goToModule("Experiment");
        scrollIntoView(Locator.linkWithText("Sample Sets"));
        clickAndWait(Locator.linkWithText("Sample Sets"));

        sampleHelper.createSampleSet(new SampleSetDefinition(FOLDER_GRANDCHILDREN_SAMPLE_SET_NAME).setFields(
                List.of(new FieldDefinition("OtherProp", FieldDefinition.ColumnType.Decimal))),
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

        sampleHelper.mergeImport(REPARENTED_CHILD_SAMPLE_SET_TSV);

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
    public void testInsertLargeLineageGraph() throws IOException, CommandException
    {
        navigateToFolder(getProjectName(), LINEAGE_FOLDER);
        // create a sampleset with the following explicit domain columns
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "bigLineage", getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("data", FieldDefinition.ColumnType.Integer),
                        TestDataGenerator.simpleFieldDef("testIndex", FieldDefinition.ColumnType.Integer)
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
        int intendedGenerationDepth = 99;
        for (int i = 0; i < intendedGenerationDepth; i++)
        {
            String name = dgen.randomString(30);
            Map row = Map.of("name", name, "data", dgen.randomInt(3, 1395), "testIndex", testIndex , "MaterialInputs/bigLineage", previousName);
            dgen.addCustomRow(row);
            previousName = name;
            testIndex++;
        }
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());
        SelectRowsResponse insertedSelect = dgen.getRowsFromServer(createDefaultConnection(true),
                List.of("name", "data", "testIndex"));

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

        // clean up the sampleset on success
        dgen.deleteDomain(createDefaultConnection(true));
    }

    @Test
    public void testLineageDerivation()
    {
        String sampleText = "Name\tIntCol\tStringCol\n" +
                "Sample12ab\t1012\talpha\n" +
                "Sample13c4\t1023\tbeta\n" +
                "Sample14d5\t1024\tgamma\n" +
                "Sampleabcd\t1035\tepsilon\n" +
                "Sampledefg\t1046\tzeta";
        projectMenu().navigateToFolder(PROJECT_NAME, LINEAGE_FOLDER);
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(new SampleSetDefinition(LINEAGE_SAMPLE_SET_NAME).setFields(
                List.of(new FieldDefinition("IntCol", FieldDefinition.ColumnType.Integer),
                        new FieldDefinition("StringCol", FieldDefinition.ColumnType.String))),
                sampleText);

        // at this point, we're in the LINEAGE_FOLDER, on the Experiment tab, looking at the sample sets properties and Sample Set contents webparts.
        // now we add more samples,
        String deriveSamples = "Name\tMaterialInputs/" + LINEAGE_SAMPLE_SET_NAME + "\n" +
                "A\t\n" +
                "B\tA\n" +      // B and C both derive from A, so should get the same Run
                "C\tA\n" +      // D derives from B, so should get its own run
                "D\tB\n";
        sampleHelper.bulkImport(deriveSamples);

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
    public void testDeriveSampleByUI() throws CommandException, IOException
    {
        String sampleTypeName= "LineageUI_01";
        String namePrefix = "SampleUI-";
        int newSampleIndex = 6;

        goToProjectHome();

        log("Create a simple sample type with some samples.");
        final TestDataGenerator dgen = TestDataGenerator.createDomain(getProjectName() +"/" + LINEAGE_FOLDER,
                new SampleSetDefinition(sampleTypeName).setFields(new ArrayList<FieldDefinition>()));

        for(int i = 1; i < newSampleIndex; i++)
        {
            Map<String, Object> sampleData = new HashMap<>();
            sampleData.put("name", namePrefix + i);
            dgen.addCustomRow(sampleData);
        }

        dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        projectMenu().navigateToFolder(PROJECT_NAME, LINEAGE_FOLDER);

        waitAndClickAndWait(Locator.linkWithText(sampleTypeName));

        log("Go to the first samples detail page and derive a sample from it.");

        String parentSample = namePrefix + "1";
        log(String.format("Using sample named '%s' as the parent.", parentSample));

        waitAndClickAndWait(Locator.linkWithText(parentSample));

        waitAndClickAndWait(Locator.linkWithText("derive samples from this sample"));

        log("Nothing fancy just going to create a single derived sample in the same sample type.");
        waitAndClickAndWait(Locator.lkButton("Next"));
        Locator nameTxtbox = Locator.inputByNameContaining("_Name");
        waitForElement(nameTxtbox);

        String newSampleName = namePrefix + newSampleIndex;
        log(String.format("The new sample will be named '%s' and it shall go forth upon the land.", newSampleName));

        setFormElement(nameTxtbox, newSampleName);
        clickAndWait(Locator.lkButton("Submit"));

        // Increment the index.
        newSampleIndex++;

        log(String.format("Check that '%s' is shown as the parent.", parentSample));
        checkRowsInDataRegion("parentMaterials", "Name", List.of(parentSample));

        log(String.format("Go to the detail page for '%s' and verify it has a child.", parentSample));
        clickAndWait(Locator.linkWithText(parentSample));
        checkRowsInDataRegion("childMaterials", "Name", List.of(newSampleName));

        log("For the parent also verify that the run column (for the child sample) is populated as expected.");
        String runTextStrFormat = "Derive sample from %s";
        checkRowsInDataRegion("childMaterials", "Run", List.of(String.format(runTextStrFormat, parentSample)));

        log("Go back to the sample type page and select several samples from the grid and derive a sample from them.");
        projectMenu().navigateToFolder(PROJECT_NAME, LINEAGE_FOLDER);
        SampleSetHelper sampleHelper = new SampleSetHelper(this);

        sampleHelper.goToSampleSet(sampleTypeName);
        DataRegionTable drt = sampleHelper.getSamplesDataRegionTable();

        List<String> parents = Arrays.asList(namePrefix + "2", namePrefix + "3", namePrefix + "4");
        log(String.format("Using samples '%s' as the parents.", parents));

        for(String parent : parents)
        {
            int index = drt.getIndexWhereDataAppears(parent, "Name");
            drt.checkCheckbox(index);
        }

        drt.clickHeaderButtonAndWait("Derive Samples");
        log("Again, nothing fancy just going to create a single derived sample.");
        waitAndClickAndWait(Locator.lkButton("Next"));
        nameTxtbox = Locator.inputByNameContaining("_Name");
        waitForElement(nameTxtbox);

        newSampleName = namePrefix + newSampleIndex;
        log(String.format("The new sample will be named '%s' and it will go to sea.", newSampleName));

        setFormElement(nameTxtbox, newSampleName);
        clickAndWait(Locator.lkButton("Submit"));

        // Increment the index.
        newSampleIndex++;

        log("Check that the expected samples are shown as the parents of this new sample.");
        checkRowsInDataRegion("parentMaterials", "Name", parents);

        log("Go look at each one of the parents and make sure they have the new sample as a child and the run column is correct.");

        String parentListUI = parents.get(0) + ", " + parents.get(1) + ", " + parents.get(2);
        String runTextUI = String.format(runTextStrFormat, parentListUI);

        for(String parent : parents)
        {
            // A link to the sample type should be visible on this page, use it to go back to the sample type page.
            clickAndWait(Locator.linkWithText(sampleTypeName));
            clickAndWait(Locator.linkWithText(parent));
            checkRowsInDataRegion("childMaterials", "Name", List.of(newSampleName));
            checkRowsInDataRegion("childMaterials", "Run", List.of(runTextUI));
        }

        log("Finally derive a sample from a sample that was itself also derived.");

        clickAndWait(Locator.linkWithText(sampleTypeName));

        log("Make the parent sample the same sample that was just created.");
        parentSample = newSampleName;
        log(String.format("Using sample named '%s' as the parent.", parentSample));

        waitAndClickAndWait(Locator.linkWithText(parentSample));

        waitAndClickAndWait(Locator.linkWithText("derive samples from this sample"));

        log("Again just create a single derived sample.");
        waitAndClickAndWait(Locator.lkButton("Next"));
        nameTxtbox = Locator.inputByNameContaining("_Name");
        waitForElement(nameTxtbox);

        newSampleName = namePrefix + newSampleIndex;
        log(String.format("The new sample will be named '%s' and this one was a failure to launch and never left home.", newSampleName));

        setFormElement(nameTxtbox, newSampleName);
        clickAndWait(Locator.lkButton("Submit"));

        List<String> newParents = new ArrayList<>();
        newParents.add(parentSample);
        newParents.addAll(parents);

        log(String.format("Check that all samples '%s' are listed as parents.", newParents));
        checkRowsInDataRegion("parentMaterials", "Name", newParents);

        log("Also check that the run column for the parents are as expected.");
        checkRowsInDataRegion("parentMaterials", "Run", List.of(runTextUI, " ", " ", " "));

        log(String.format("Go to the 'derived' parent '%s' and verify it has '%s' as a child.", parentSample, newSampleName));

        clickAndWait(Locator.linkWithText(parentSample));
        checkRowsInDataRegion("childMaterials", "Name", List.of(newSampleName));

        log(String.format("Verify again that '%s' parents are still as expected.", parentSample));
        checkRowsInDataRegion("parentMaterials", "Name", parents);

        String grandParent = parents.get(0);
        log(String.format("Select one of '%s' parents, '%s' and verify that it now has two children.", parentSample, grandParent));

        clickAndWait(Locator.linkWithText(grandParent));
        checkRowsInDataRegion("childMaterials", "Name", Arrays.asList(newSampleName, parentSample));

        log("Test complete.");
    }

    private void checkRowsInDataRegion(String dataRegionName, String columnName, List<String> expectedValues)
    {
        DataRegionTable dataRegionTable = new DataRegionTable(dataRegionName, this);

        List<String> dataInTable = dataRegionTable.getColumnDataAsText(columnName);

        checker().verifyEquals("Number of entries in the column '" + columnName + "' is not as expected.",
                expectedValues.size(), dataInTable.size());

        for(String expectedValue : expectedValues)
        {
            checker().verifyTrue(String.format("Value '%s' was not shown in column '%s' in data region '%s'.",
                    expectedValue, columnName, dataRegionName),
                    dataInTable.contains(expectedValue));

        }

    }

    /**
     * regression coverage for https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=37465
     */
    @Test
    public void testLineageWithInvalidValue() throws IOException, CommandException
    {
        navigateToFolder(getProjectName(), LINEAGE_FOLDER);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "badLineageTest", getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("data", FieldDefinition.ColumnType.Integer)
                ));
        dgen.createDomain(createDefaultConnection(true), "SampleSet");
        dgen.addCustomRow(Map.of("name", "A", "data", 12));     // no parent
        dgen.addCustomRow(Map.of("name", "B", "data", 12,  "MaterialInputs/badLineageTest", "A"));   // derives from A
        dgen.addCustomRow(Map.of("name", "C", "data", 12,  "MaterialInputs/badLineageTest", "A"));
        dgen.addCustomRow(Map.of("name", "D", "data", 12,  "MaterialInputs/badLineageTest", "BOGUS")); //<--bad lineage here
        try
        {
            dgen.insertRows(createDefaultConnection(true), dgen.getRows());
            fail("Expect CommandException when inserting bogus lineage");
        }catch (CommandException successMaybe)
        {
            assertTrue("expect bad lineage to produce error containing [Sample input 'BOGUS' in SampleSet 'badLineageTest' not found];\n" +
                            "instead got: [" + successMaybe.getMessage() + "]",
                    successMaybe.getMessage().contains("Sample input 'BOGUS' in SampleSet 'badLineageTest' not found"));
        }
    }

    /**
     *  regression coverage for https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=37465
     */
    @Test
    public void testLineageWithInvalidParentColumnValue() throws IOException, CommandException
    {
        navigateToFolder(getProjectName(), LINEAGE_FOLDER);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "badParentLineage", getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("data", FieldDefinition.ColumnType.Integer)
                ));
        dgen.createDomain(createDefaultConnection(true), "SampleSet");
        dgen.addCustomRow(Map.of("name", "A", "data", 12));     // no parent
        dgen.addCustomRow(Map.of("name", "B", "data", 13,  "MaterialInputs/badParentLineage", "A"));   // derives from A
        dgen.addCustomRow(Map.of("name", "C", "data", 14,  "MaterialInputs/badParentLineage", "B"));
        dgen.addCustomRow(Map.of("name", "D", "data", 15,  "MaterialInputs/badParentLineage", "BOGUS")); //<--bad lineage here
        try
        {
            dgen.insertRows(createDefaultConnection(true), dgen.getRows());
            fail("Expect CommandException when inserting bogus lineage");
        }catch (CommandException successMaybe)  // success looks like a CommandException with the expected message
        {
            assertTrue("expect bad lineage to produce error containing [Sample input 'BOGUS' in SampleSet 'badParentLineage' not found];\n" +
                            "instead got: [" + successMaybe.getMessage() + "]",
                    successMaybe.getMessage().contains("Sample input 'BOGUS' in SampleSet 'badParentLineage' not found"));
        }

        // clean up on success
        dgen.deleteDomain(createDefaultConnection(true));
    }

}
