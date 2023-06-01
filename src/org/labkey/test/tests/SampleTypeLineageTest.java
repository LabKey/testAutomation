package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
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
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import static org.labkey.test.util.exp.SampleTypeAPIHelper.SAMPLE_TYPE_DOMAIN_KIND;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 10)
public class SampleTypeLineageTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleType_Lineage_Test_Project";
    private static final String SUB_FOLDER_NAME = "SubFolder_A";

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
        portalHelper.addWebPart("Sample Types");

        _containerHelper.createSubfolder(PROJECT_NAME, SUB_FOLDER_NAME, "Collaboration");
        portalHelper.addWebPart("Sample Types");

        portalHelper.exitAdminMode();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        // If you are debugging tests change this function to do nothing.
        // It can make re-running faster but you need to valid the integrity of the test data on your own.
//        log("Do nothing.");
    }

    /**
     *  coverage for https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=37466
     */
    @Test
    @Ignore
    public void testLineageWithImplicitParentColumn() throws IOException, CommandException
    {
        goToProjectHome();

        // create a sampleset with the following explicit domain columns
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "implicitParentage", getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("data", FieldDefinition.ColumnType.Integer),
                        TestDataGenerator.simpleFieldDef("stringData", FieldDefinition.ColumnType.String)
                ));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        dgen.addRow(List.of("A", 12, dgen.randomString(15)));
        dgen.addRow(List.of("B", 13, dgen.randomString(15)));
        dgen.addRow(List.of("C", 15, dgen.randomString(15)));
        dgen.addCustomRow(Map.of("name", "D", "data", 12, "stringData", dgen.randomString(15), "MaterialInputs/implicitParentage", "B"));
        dgen.addCustomRow(Map.of("name", "E", "data", 14, "stringData", dgen.randomString(15), "MaterialInputs/implicitParentage", "B"));
        dgen.addCustomRow(Map.of("name", "F", "data", 12, "stringData", dgen.randomString(15), "MaterialInputs/implicitParentage", "A,B"));
        dgen.addCustomRow(Map.of("name", "G", "data", 12, "stringData", dgen.randomString(15), "MaterialInputs/implicitParentage", "C"));
        dgen.addCustomRow(Map.of("name", "H", "data", 14, "stringData", dgen.randomString(15), "MaterialInputs/implicitParentage", "A,B,C"));
        dgen.addCustomRow(Map.of("name", "I", "data", 12, "stringData", dgen.randomString(15), "MaterialInputs/implicitParentage", "B,G"));

        SaveRowsResponse saveRowsResponse = dgen.insertRows(createDefaultConnection(), dgen.getRows());

        // get row 'B' after insert
        Map<String, Object> rowB = saveRowsResponse.getRows().stream().filter((a)-> a.get("name").equals("B"))
                .findFirst().orElse(null);
        Map<String, Object> rowH = saveRowsResponse.getRows().stream().filter((a)-> a.get("name").equals("H"))
                .findFirst().orElse(null);

        refresh();
        DataRegionTable.DataRegion(getDriver()).withName(SAMPLE_TYPE_DOMAIN_KIND).waitFor();
        waitAndClickAndWait(Locator.linkWithText("implicitParentage"));
        DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        // get the lineage graph
        LineageCommand lineageCommand = new LineageCommand.Builder(rowB.get("lsid").toString())
                .setChildren(true)
                .setParents(false)
                .setDepth(3).build();
        LineageResponse lineageResponse = lineageCommand.execute(createDefaultConnection(), getCurrentContainerPath());
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
        dgen.deleteRows(createDefaultConnection(), List.of(rowB));

        // get the lineage graph
        LineageCommand parents = new LineageCommand.Builder(rowH.get("lsid").toString())
                .setChildren(false)
                .setParents(true)
                .setDepth(3).build();
        LineageResponse parentResponse = parents.execute(createDefaultConnection(), getCurrentContainerPath());
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
        dgen.deleteDomain(createDefaultConnection());
    }

    @Test
    public void testLineageWithParentInRootFolder()
    {
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        clickProject(PROJECT_NAME);
        final String parentSampleType = "ParentSampleType_InRoot";

        sampleHelper.createSampleType(new SampleTypeDefinition(parentSampleType).setFields(
                List.of(new FieldDefinition("Field1", FieldDefinition.ColumnType.String))),
                "Name\tField1\n" +
                        "ProjectS1\tsome value\n");

        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);
        final String childSampleType = "ChildOfProject";
        sampleHelper.createSampleType(new SampleTypeDefinition(childSampleType).setFields(
                List.of(new FieldDefinition("IntCol",  FieldDefinition.ColumnType.Integer))),
                "Name\tMaterialInputs/" + parentSampleType + "\n" +
                        "COP1\tProjectS1\n");

        // Verify it got linked up correctly
        clickAndWait(Locator.linkWithText("COP1"));
        assertElementPresent(Locator.linkWithText("ProjectS1"));
    }

    @Test
    public void testSampleTypeAndLineageInSubfolder()
    {
        /*
        This test will create a sample type named "ParentFolder_SampleType" in the root folder.
        It will populate this sample type with data from a xls file.
        It then creates in a subfolder a sample type named "SubFolder_SampleType".

        The behavior in LabKey is:
        The subfolder will see the sample type from the parent folder (ParentFolder_SampleType) but none of the samples in it.
        The root folder cannot see the sample type created in the subfolder (SubFolder_SampleType).

        Then in the sub folder it will create a derived sample and put it in ParentFolder_SampleType. When the test looks
        at ParentFolder_SampleType in the sub folder it should see one sample (the derived sample). When it looks at
        ParentFolder_SampleType in the root folder it should only see the samples originally imported from the file and
        not the newly created derived sample.
         */

        log("Create a sample type in the root folder and import the data from a xls file.");
        List<FieldDefinition> sampleTypeFields = List.of(
                new FieldDefinition("IntCol", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("StringCol", FieldDefinition.ColumnType.String),
                new FieldDefinition("DateCol", FieldDefinition.ColumnType.DateAndTime),
                new FieldDefinition("BoolCol", FieldDefinition.ColumnType.Boolean));
        File sampleTypeFile = TestFileUtils.getSampleData("sampleType.xlsx");

        clickProject(PROJECT_NAME);
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        final String parentFolderSampleType = "ParentFolder_SampleType";

        sampleHelper.createSampleType(new SampleTypeDefinition(parentFolderSampleType).setFields(sampleTypeFields), sampleTypeFile);

        log("Create a sample type in a sub folder.");
        clickFolder(SUB_FOLDER_NAME);
        final String subFolderSampleType = "SubFolder_SampleType";

        sampleHelper.createSampleType(new SampleTypeDefinition(subFolderSampleType).setFields(
                List.of(new FieldDefinition("IntCol-Folder",  FieldDefinition.ColumnType.Integer),
                        new FieldDefinition("StringCol-Folder", FieldDefinition.ColumnType.String))),
                "Name\tIntCol-Folder\tStringCol-Folder\n" +
                        "SampleSetBVT11\t101\taa\n" +
                        "SampleSetBVT4\t102\tbb\n" +
                        "SampleSetBVT12\t102\tbb\n" +
                        "SampleSetBVT13\t103\tcc\n" +
                        "SampleSetBVT14\t104\tdd");

        clickAndWait(Locator.linkWithText("Sample Types"));
        checker().wrapAssertion(()->assertTextPresent(parentFolderSampleType, SUB_FOLDER_NAME));

        log("Validate that all materials visible in the subfolder do not include anything from the root folder.");
        clickButton("Show All Materials");
        checker().wrapAssertion(()->assertTextPresent(subFolderSampleType));
        checker().wrapAssertion(()->assertTextNotPresent(parentFolderSampleType));

        log("Derive two samples from the samples already present in this sample type (SubFolder_SampleType).");
        clickFolder(SUB_FOLDER_NAME);
        checker().wrapAssertion(()->assertTextPresent(subFolderSampleType, parentFolderSampleType));

        clickAndWait(Locator.linkWithText(subFolderSampleType));
        checkCheckbox(Locator.name(".toggle"));
        clickButton("Derive Samples");
        waitForElement(Locator.name("inputRole0"));

        selectOptionByText(Locator.name("inputRole0"), "Add a new role...");
        setFormElement(Locator.id("customRole0"), "FirstRole");
        selectOptionByText(Locator.name("inputRole1"), "Add a new role...");
        setFormElement(Locator.id("customRole1"), "SecondRole");
        selectOptionByText(Locator.name("inputRole2"), "Add a new role...");
        setFormElement(Locator.id("customRole2"), "ThirdRole");
        selectOptionByText(Locator.name("inputRole3"), "Add a new role...");
        setFormElement(Locator.id("customRole3"), "FourthRole");
        selectOptionByText(Locator.name("outputCount"), "2");
        selectOptionByText(Locator.name("targetSampleTypeId"), subFolderSampleType + " in /" + getProjectName() + "/" + SUB_FOLDER_NAME);
        clickButton("Next");

        setFormElement(Locator.name("outputSample1_Name"), "SampleSetBVT15");
        setFormElement(Locator.name("outputSample2_Name"), "SampleSetBVT16");
        checkCheckbox(Locator.name("outputSample1_IntColFolderCheckBox"));
        setFormElement(Locator.name("outputSample1_IntColFolder"), "500a");
        setFormElement(Locator.name("outputSample1_StringColFolder"), "firstOutput");
        setFormElement(Locator.name("outputSample2_StringColFolder"), "secondOutput");
        clickButton("Submit");

        log("Do a simple check that data validation works.");
        checker().verifyTrue("Expected error message '(String) for Integer field' is not present.",
                isTextPresent("(String) for Integer field"));
        checkCheckbox(Locator.name("outputSample1_IntColFolderCheckBox"));
        setFormElement(Locator.name("outputSample1_IntColFolder"), "500");
        clickButton("Submit");

        clickAndWait(Locator.linkContainingText("Derive 2 samples"));
        clickAndWait(Locator.linkContainingText("Text View"));
        assertTextPresent("FirstRole", "SecondRole", "ThirdRole", "FourthRole");

        log("Select one of these new derived samples and derive a sample from it");
        log("But put this new derived sample in the sample type created in the parent folder (ParentFolder_SampleType)");
        clickAndWait(Locator.linkContainingText("16"));
        clickAndWait(Locator.linkContainingText("derive samples from this sample"));

        selectOptionByText(Locator.name("inputRole0"), "FirstRole");
        selectOptionByText(Locator.name("targetSampleTypeId"), parentFolderSampleType + " in /" + getProjectName());
        clickButton("Next");

        String derivedSampleName = "Only_In_Sub_Folder";
        setFormElement(Locator.name("outputSample1_Name"), derivedSampleName);
        setFormElement(Locator.name("outputSample1_IntCol"), "600");
        setFormElement(Locator.name("outputSample1_StringCol"), "String");
        setFormElement(Locator.name("outputSample1_DateCol"), "BadDate");
        uncheckCheckbox(Locator.name("outputSample1_BoolCol"));
        clickButton("Submit");

        log("Again check that data validation works as expected.");
        checker().verifyTrue("Expected error message '(String) for Date field' is not present.",
                isTextPresent("(String) for Date field"));
        setFormElement(Locator.name("outputSample1_DateCol"), "1/1/2007");
        clickButton("Submit");

        log("Check that the correct sample id is shown as the parent.");
        checker().verifyTrue("Link to parent sample not present.",
                isElementPresent(Locator.linkWithText("Derive sample from SampleSetBVT16")));

        log("Check that all of the grandparents are also listed.");
        checker().verifyTrue("Link to grandparent 'SampleSetBVT11' is not present",
                isElementPresent(Locator.linkWithText("SampleSetBVT11")));
        checker().verifyTrue("Link to grandparent 'SampleSetBVT12' is not present",
                isElementPresent(Locator.linkWithText("SampleSetBVT12")));
        checker().verifyTrue("Link to grandparent 'SampleSetBVT13' is not present",
                isElementPresent(Locator.linkWithText("SampleSetBVT13")));
        checker().verifyTrue("Link to grandparent 'SampleSetBVT14' is not present",
                isElementPresent(Locator.linkWithText("SampleSetBVT14")));

        log("Go to one of the grandparents and check that the expected text/links are there.");
        clickAndWait(Locator.linkWithText("SampleSetBVT11"));

        checker().verifyTrue("Link to parent sample not present.",
                isElementPresent(Locator.linkWithText("Derive sample from SampleSetBVT16")));

        checker().verifyTrue("Expected run text is not present.",
                isElementPresent(
                        Locator.linkWithText("Derive 2 samples from SampleSetBVT11, SampleSetBVT12, SampleSetBVT13, SampleSetBVT14, SampleSetBVT4")
                )
        );

        log("Go to the 'SubFolder_SampleType' and make sure the expected data is there.");
        clickFolder(SUB_FOLDER_NAME);
        clickAndWait(Locator.linkWithText(subFolderSampleType));

        checker().wrapAssertion(()->assertTextPresent("aa", "bb", "cc", "dd", "firstOutput", "secondOutput"));

        log("Look at all of the materials in the subfolder and validate that the expected derived sample is there.");
        clickAndWait(Locator.linkWithText("Sample Types"));
        clickButton("Show All Materials");
        checker().wrapAssertion(()-> assertTextPresent(parentFolderSampleType, derivedSampleName));

        log("Go back to the root folder and verify that none of the samples in sub folder are visible.");
        goToProjectHome();
        sampleHelper.goToSampleType(parentFolderSampleType);
        checker().wrapAssertion(()->assertElementNotPresent(Locator.linkWithText(derivedSampleName)));
    }

    @Test
    public void testUpdateLineageUsingFileImport()
    {
        // If you change these values you will need to change the tsv data files as well.
        final String sampleTypeName = "UpdateLineage_By_File";
        final String columnId = "IntField";
        final String columnName = "Int Field";
        final String testSample = "SU-6";
        final String testData = "6";
        final String updatedTestData = "16";
        final String parentSample = "SU-1";
        final String updatedParentSample = "SU-2";
        final String updatedChildSample = "SU-10";

        goToProjectHome();

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        log("Create a simple sample type with some samples imported from a file.");
        final File sampleTypeInitFile = TestFileUtils.getSampleData("Update_Lineage_A.tsv");

        sampleHelper.createSampleType(new SampleTypeDefinition(sampleTypeName).setFields(
                List.of(new FieldDefinition(columnId,  FieldDefinition.ColumnType.Integer))),
                sampleTypeInitFile);

        log("Check that the imported data is as expected.");
        DataRegionTable dataRegionTable = new DataRegionTable("Material", this);
        int row = dataRegionTable.getIndexWhereDataAppears(testSample, "Name");
        String data = dataRegionTable.getDataAsText(row, columnName);
        checker().verifyEquals("Something doesn't look right. Value for column not as expected.",
                testData, data);

        clickAndWait(Locator.linkWithText(testSample));

        log(String.format("Check that '%s' is shown as the parent of sample '%s'.", parentSample, testSample));
        checkRowsInDataRegion("parentMaterials", "Name", List.of(parentSample));

        log("Check that there are no child samples.");
        checkRowsInDataRegion("childMaterials", "Name", new ArrayList<>());

        goToProjectHome();

        waitAndClickAndWait(Locator.linkWithText(sampleTypeName));

        final File sampleTypeUpdateFile = TestFileUtils.getSampleData("Update_Lineage_B.tsv");

        sampleHelper.mergeImport(sampleTypeUpdateFile);

        log("Check that the updated data is shown.");
        dataRegionTable = new DataRegionTable("Material", this);
        row = dataRegionTable.getIndexWhereDataAppears(testSample, "Name");
        data = dataRegionTable.getDataAsText(row, columnName);
        checker().verifyEquals("Value for column not updated as expected.",
                updatedTestData, data);

        clickAndWait(Locator.linkWithText(testSample));

        log(String.format("Check that '%s' is shown as the parent of sample '%s'.", updatedParentSample, testSample));
        checkRowsInDataRegion("parentMaterials", "Name", List.of(updatedParentSample));

        log(String.format("Check that '%s' is shown as a child of sample '%s'.", updatedChildSample, testSample));
        checkRowsInDataRegion("childMaterials", "Name", List.of(updatedChildSample));

        log("Test complete.");
    }

    @Test
    public void testLineageWithThreeGenerations()
    {
        goToProjectHome();

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        log("Create parent sample type");
        final String parentSampleType = "ParentSampleType";
        sampleHelper.createSampleType(new SampleTypeDefinition(parentSampleType).setFields(
                List.of(new FieldDefinition("IntCol",  FieldDefinition.ColumnType.Integer))),
                "Name\tIntCol\n" +
                        "SampleSetBVT11\t101\n" +
                        "SampleSetBVT4\t102\n" +
                        "SampleSetBVT12\t102\n" +
                        "SampleSetBVT13\t103\n" +
                        "SampleSetBVT14\t104");

        // Refresh the page so the new sample type shows up in the UI.
        refresh();
        log("Go to the sample type page to create a new sample type.");
        clickAndWait(Locator.linkWithText("Sample Types"));

        log("Create child sample type");
        final String childrenSampleType = "ChildrenSampleType";
        sampleHelper.createSampleType(new SampleTypeDefinition(childrenSampleType).setFields(
                List.of(new FieldDefinition("OtherProp", FieldDefinition.ColumnType.Decimal))),
                "Name\tMaterialInputs/" + parentSampleType + "\tOtherProp\n" +
                        "SampleSetBVTChildA\tSampleSetBVT11\t1.1\n" +
                        "SampleSetBVTChildB\tSampleSetBVT4\t2.2\n"
        );


        // Make sure that the parent got wired up
        log("Verify parent references");
        DataRegionTable table = sampleHelper.getSamplesDataRegionTable();
        table.openCustomizeGrid();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addColumn(new String[]{"Inputs", "Materials", parentSampleType});
        _customizeViewsHelper.clickViewGrid();
        waitAndClickAndWait(Locator.linkWithText("SampleSetBVT4"));

        // Check out the run
        clickAndWait(Locator.linkWithText("Derive sample from SampleSetBVT4"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT4"));
        assertElementPresent(Locator.linkWithText("SampleSetBVTChildB"));

        // Make a grandchild type
        log("Create a grandparent sample type");
        goToModule("Experiment");
        scrollIntoView(Locator.linkWithText("Sample Types"));
        clickAndWait(Locator.linkWithText("Sample Types"));

        final String grandchildrenSampleType = "FolderGrandchildrenSampleSet";
        sampleHelper.createSampleType(new SampleTypeDefinition(grandchildrenSampleType).setFields(
                List.of(new FieldDefinition("OtherProp", FieldDefinition.ColumnType.Decimal))),
                "Name\tMaterialInputs/" + childrenSampleType + "\tOtherProp\n" +
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
        clickAndWait(Locator.linkWithText(childrenSampleType));
        String REPARENTED_CHILD_SAMPLE_TYPE_TSV = "Name\tMaterialInputs/" + parentSampleType + "\tOtherProp\n" +
                "SampleSetBVTChildA\tSampleSetBVT13\t1.111\n" +
                "SampleSetBVTChildB\tSampleSetBVT14\t2.222\n";

        sampleHelper.mergeImport(REPARENTED_CHILD_SAMPLE_TYPE_TSV);

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
    public void testDeriveSampleByImport()
    {
        String sampleText = "Name\tIntCol\tStringCol\n" +
                "Sample12ab\t1012\talpha\n" +
                "Sample13c4\t1023\tbeta\n" +
                "Sample14d5\t1024\tgamma\n" +
                "Sampleabcd\t1035\tepsilon\n" +
                "Sampledefg\t1046\tzeta";
        goToProjectHome();
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        final String sampleTypeName = "LineageSampleType";

        sampleHelper.createSampleType(new SampleTypeDefinition(sampleTypeName).setFields(
                List.of(new FieldDefinition("IntCol", FieldDefinition.ColumnType.Integer),
                        new FieldDefinition("StringCol", FieldDefinition.ColumnType.String))),
                sampleText);

        String deriveSamples = "Name\tMaterialInputs/" + sampleTypeName + "\n" +
                "A\t\n" +
                "B\tA\n" +      // B and C both derive from A, so should get the same Run
                "C\tA\n" +      // D derives from B, so should get its own run
                "D\tB\n";
        sampleHelper.bulkImport(deriveSamples);

        SelectRowsResponse samples = executeSelectRowCommand("samples", sampleTypeName,
                ContainerFilter.Current, getProjectName(), null);
        Map<String, Object> rowA =  samples.getRows().stream().filter(
                (a)-> a.get("Name").equals("A")).collect(Collectors.toList()).get(0);
        Map<String, Object> rowB =  samples.getRows().stream().filter(
                (a)-> a.get("Name").equals("B")).collect(Collectors.toList()).get(0);
        Map<String, Object> rowC =  samples.getRows().stream().filter(
                (a)-> a.get("Name").equals("C")).collect(Collectors.toList()).get(0);
        Map<String, Object> rowD =  samples.getRows().stream().filter(
                (a)-> a.get("Name").equals("D")).collect(Collectors.toList()).get(0);

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
        final TestDataGenerator dgen = new SampleTypeDefinition(sampleTypeName)
                .create(createDefaultConnection(), getProjectName());

        for(int i = 1; i < newSampleIndex; i++)
        {
            Map<String, Object> sampleData = new HashMap<>();
            sampleData.put("name", namePrefix + i);
            dgen.addCustomRow(sampleData);
        }

        dgen.insertRows(createDefaultConnection(), dgen.getRows());
        // Refresh the page so the new sample type shows up in the UI.
        refresh();
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
        goToProjectHome();
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        sampleHelper.goToSampleType(sampleTypeName);
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

    private void checkRowsInDataRegion(String dataRegionName, String columnName, final List<String> expectedValues)
    {
        DataRegionTable dataRegionTable = new DataRegionTable(dataRegionName, this);

        List<String> dataInTable = dataRegionTable.getColumnDataAsText(columnName);
        Collections.sort(dataInTable);

        // Protect when a list created using list.of is passed in. List.of creates an unmodifiable list and results in
        // a UnsupportedOperationException if sorted.
        List<String> localExpectedValues = new ArrayList<>(expectedValues);
        Collections.sort(localExpectedValues);

        checker().verifyEquals("Entries in the column '" + columnName + "' are not as expected.",
                localExpectedValues, dataInTable);

    }

    /**
     * regression coverage for https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=37465
     */
    @Test
    public void testLineageWithInvalidValue() throws IOException, CommandException
    {
        goToProjectHome();
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "badLineageTest", getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("data", FieldDefinition.ColumnType.Integer)
                ));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        dgen.addCustomRow(Map.of("name", "A", "data", 12));     // no parent
        dgen.addCustomRow(Map.of("name", "B", "data", 12,  "MaterialInputs/badLineageTest", "A"));   // derives from A
        dgen.addCustomRow(Map.of("name", "C", "data", 12,  "MaterialInputs/badLineageTest", "A"));
        dgen.addCustomRow(Map.of("name", "D", "data", 12,  "MaterialInputs/badLineageTest", "BOGUS")); //<--bad lineage here
        try
        {
            dgen.insertRows(createDefaultConnection(), dgen.getRows());
            fail("Expect CommandException when inserting bogus lineage");
        }catch (CommandException successMaybe)
        {
            assertTrue("expect bad lineage to produce error containing [Sample 'BOGUS' not found in Sample Type 'badLineageTest'.];\n" +
                            "instead got: [" + successMaybe.getMessage() + "]",
                    successMaybe.getMessage().contains("Sample 'BOGUS' not found in Sample Type 'badLineageTest'."));
        }
    }

    /**
     *  regression coverage for https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=37465
     */
    @Test
    public void testLineageWithInvalidParentColumnValue() throws IOException, CommandException
    {
        goToProjectHome();
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "badParentLineage", getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("data", FieldDefinition.ColumnType.Integer)
                ));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        dgen.addCustomRow(Map.of("name", "A", "data", 12));     // no parent
        dgen.addCustomRow(Map.of("name", "B", "data", 13,  "MaterialInputs/badParentLineage", "A"));   // derives from A
        dgen.addCustomRow(Map.of("name", "C", "data", 14,  "MaterialInputs/badParentLineage", "B"));
        dgen.addCustomRow(Map.of("name", "D", "data", 15,  "MaterialInputs/badParentLineage", "BOGUS")); //<--bad lineage here
        try
        {
            dgen.insertRows(createDefaultConnection(), dgen.getRows());
            fail("Expect CommandException when inserting bogus lineage");
        }catch (CommandException successMaybe)  // success looks like a CommandException with the expected message
        {
            assertTrue("expect bad lineage to produce error containing [Sample 'BOGUS' not found in Sample Type 'badParentLineage'.];\n" +
                            "instead got: [" + successMaybe.getMessage() + "]",
                    successMaybe.getMessage().contains("Sample 'BOGUS' not found in Sample Type 'badParentLineage'."));
        }

        // clean up on success
        dgen.deleteDomain(createDefaultConnection());
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
        goToProjectHome();

        // create a sampleset with the following explicit domain columns
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "Family",
                getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("age", FieldDefinition.ColumnType.Integer),
                        TestDataGenerator.simpleFieldDef("height", FieldDefinition.ColumnType.Integer)
                ));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        dgen.addRow(List.of("A", 56, 60));
        dgen.addRow(List.of("B", 48, 50));
        dgen.addCustomRow(Map.of("name", "C", "age", 12, "height", 44, "MaterialInputs/Family", "A,B"));
        dgen.addCustomRow(Map.of("name", "D", "age", 12, "height", 44, "MaterialInputs/Family", "B"));
        dgen.addCustomRow(Map.of("name", "E", "age", 12, "height", 44, "MaterialInputs/Family", "B"));
        dgen.addCustomRow(Map.of("name", "F", "age", 12, "height", 44, "MaterialInputs/Family", "A,B"));
        dgen.addCustomRow(Map.of("name", "G", "age", 12, "height", 44, "MaterialInputs/Family", "C"));
        dgen.addCustomRow(Map.of("name", "H", "age", 12, "height", 44, "MaterialInputs/Family", "A,B,C"));
        dgen.addCustomRow(Map.of("name", "I", "age", 12, "height", 44, "MaterialInputs/Family", "G"));
        SaveRowsResponse saveRowsResponse = dgen.insertRows(createDefaultConnection(), dgen.getRows());

        // Refresh the page so the new sample type shows up in the UI.
        refresh();

        DataRegionTable.DataRegion(getDriver()).withName(SAMPLE_TYPE_DOMAIN_KIND).waitFor();
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
        LineageResponse lineageResponse = lineageCommand.execute(createDefaultConnection(), getCurrentContainerPath());
        assertEquals("Number of initial parents for samples not as expected.",
                1, lineageResponse.getSeed().getParents().size());

        // delete rows A, B
        dgen.deleteRows(createDefaultConnection(), rowsToDelete);
        SelectRowsResponse selectResponse = dgen.getRowsFromServer(createDefaultConnection(),
                List.of("rowId", "lsid", "name", "parent", "age", "height", "MaterialInputs/Family", "Inputs/First"));
        List<Map<String, Object>> remainingRows = selectResponse.getRows();

        // now make sure the run that created the derived sample is deleted when the parent is deleted
        Map<String, Object> rowE = remainingRows.stream()
                .filter((a)-> a.get("name").equals("E")).findFirst().orElse(null);
        LineageCommand linCmd = new LineageCommand.Builder(rowE.get("lsid").toString())
                .setChildren(false)
                .setParents(true)
                .setDepth(1).build();
        LineageResponse linResponse = linCmd.execute(createDefaultConnection(), getCurrentContainerPath());
        assertEquals("The number of runs for the child sample whose parent was deleted is not as expected.",
                0, linResponse.getSeed().getParents().size());

        dgen.deleteDomain(createDefaultConnection());
    }

    @Test
    public void testDeleteSampleSources() throws CommandException, IOException
    {
        SampleTypeDefinition sampleType = new SampleTypeDefinition("DeleteSourcesSamples")
                .addField(new FieldDefinition("strCol", FieldDefinition.ColumnType.String));
        DataClassDefinition dataClass = new DataClassDefinition("DeleteSourcesData")
                .addField(new FieldDefinition("strCol", FieldDefinition.ColumnType.String));

        TestDataGenerator sampleGenerator = sampleType.create(createDefaultConnection(), getProjectName());
        TestDataGenerator dataGenerator = dataClass.create(createDefaultConnection(), getProjectName());

        final String sampleParentKey = "MaterialInputs/" + sampleType.getName();
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
        sampleGenerator.insertRows(createDefaultConnection(), sampleGenerator.getRows());

        goToProjectHome();

        SampleTypeHelper sampleHelper = new SampleTypeHelper(getDriver());

        goToModule("Experiment");
        sampleHelper.goToSampleType(sampleType.getName());
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
    public void testDeleteSamplesSomeWithDerivedSamples()
    {
        final String SAMPLE_TYPE_NAME = "DeleteSamplesWithParents";
        List<String> parentSampleNames = Arrays.asList("P-1", "P-2", "P-3");
        List<Map<String, String>> sampleData = new ArrayList<>();
        parentSampleNames.forEach(name -> {
            sampleData.add(Map.of("Name", name));
        });

        clickProject(PROJECT_NAME);
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        log("Create a sample type with some potential parents");
        sampleHelper.createSampleType(new SampleTypeDefinition(SAMPLE_TYPE_NAME), sampleData);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        log("Derive one sample from another");
        drtSamples.checkCheckbox(drtSamples.getIndexWhereDataAppears(parentSampleNames.get(0), "Name"));
        clickButton("Derive Samples");
        waitAndClickAndWait(Locator.lkButton("Next"));
        String childName = parentSampleNames.get(0) + ".1";
        setFormElement(Locator.name("outputSample1_Name"), childName);
        clickButton("Submit");

        log("Derive a sample from the one just created");
        clickAndWait(Locator.linkContainingText("derive samples from this sample"));
        clickButton("Next");
        String grandchildName = childName + ".1";
        setFormElement(Locator.name("outputSample1_Name"), grandchildName);
        clickButton("Submit");

        log("Derive a sample with two parents");
        clickAndWait(Locator.linkContainingText(SAMPLE_TYPE_NAME));
        drtSamples.checkCheckbox(drtSamples.getIndexWhereDataAppears(parentSampleNames.get(1), "Name"));
        drtSamples.checkCheckbox(drtSamples.getIndexWhereDataAppears(childName, "Name"));
        clickButton("Derive Samples");
        waitAndClickAndWait(Locator.lkButton("Next"));
        String twoParentChildName = parentSampleNames.get(1) + "+" + childName + ".1";
        setFormElement(Locator.name("outputSample1_Name"), twoParentChildName);
        clickButton("Submit");

        clickAndWait(Locator.linkContainingText(SAMPLE_TYPE_NAME));

        log("Try to delete parent sample");
        drtSamples.checkCheckbox(drtSamples.getIndexWhereDataAppears(parentSampleNames.get(0), "Name"));
        drtSamples.clickHeaderButton("Delete");
        Window.Window(getDriver()).withTitle("No samples can be deleted").waitFor()
                .clickButton("Dismiss", true);

        log("Try to delete multiple parent samples");
        drtSamples.checkCheckbox(drtSamples.getIndexWhereDataAppears(parentSampleNames.get(1), "Name"));
        drtSamples.checkCheckbox(drtSamples.getIndexWhereDataAppears(childName, "Name"));
        drtSamples.clickHeaderButton("Delete");
        Window.Window(getDriver()).withTitle("No samples can be deleted").waitFor()
                .clickButton("Dismiss", true);
        drtSamples.uncheckAllOnPage();
        assertEquals("No selection should remain", 0, drtSamples.getCheckedCount());
        assertEquals("No selection should remain", 0, drtSamples.getSelectedCount());

        log("Try to delete parent and child");
        drtSamples.checkCheckbox(drtSamples.getIndexWhereDataAppears(parentSampleNames.get(1), "Name"));
        drtSamples.checkCheckbox(drtSamples.getIndexWhereDataAppears(twoParentChildName, "Name"));
        assertEquals("Parent and child should be checked", 2, drtSamples.getCheckedCount());
        assertEquals("Parent and child should be checked", 2, drtSamples.getSelectedCount());

        sampleHelper.deleteSamples(drtSamples, "Permanently delete 1 sample");
        assertEquals("Deleted sample " + twoParentChildName + " still appears in grid", -1, drtSamples.getIndexWhereDataAppears(twoParentChildName, "Name"));
        assertTrue("Parent sample " + parentSampleNames.get(1) + " does not appears in grid", drtSamples.getIndexWhereDataAppears(parentSampleNames.get(1), "Name") > -1);
        assertEquals("Only parent sample should be checked", 1, drtSamples.getCheckedCount());
        assertEquals("Only parent sample should be checked", 1, drtSamples.getSelectedCount());

        log("Now that the child is gone, try to delete the parent");
        sampleHelper.deleteSamples(drtSamples, "Permanently delete 1 sample");

        assertEquals("Deleted sample " + parentSampleNames.get(1) + " still appears in grid", -1, drtSamples.getIndexWhereDataAppears(parentSampleNames.get(1), "Name"));
        assertEquals("No selection should remain", 0, drtSamples.getCheckedCount());

        log("Now try to delete what's left, in several hitches");
        drtSamples.checkAllOnPage();
        sampleHelper.deleteSamples(drtSamples, "Permanently delete 2 samples");
        assertEquals("Number of samples after deletion not as expected", 2, drtSamples.getDataRowCount());

        sampleHelper.deleteSamples(drtSamples, "Permanently delete 1 sample");
        assertEquals("Number of samples after deletion not as expected", 1, drtSamples.getDataRowCount());

        sampleHelper.deleteSamples(drtSamples, "Permanently delete 1 sample");
        assertEquals("Number of samples after deletion not as expected", 0, drtSamples.getDataRowCount());

    }

}
