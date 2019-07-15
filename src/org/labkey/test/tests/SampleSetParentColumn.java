package org.labkey.test.tests;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyC;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleSetHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category({DailyC.class})
public class SampleSetParentColumn extends BaseWebDriverTest
{

    private static final String PROJECT_NAME = "SampleSetTestProject";
    private static final String SUB_FOLDER_NAME = "SampleSetTestFolder";

    protected static final String PARENT_CONTAINER_SAMPLE_SET_NAME = "PrePopulatedSampleSet";
    protected static final String PARENT_CONTAINER_SAMPLE_SET_CAPTION = "Pre Populated Sample Set";

    protected static final String SIBLING_SAMPLE_SET_NAME = "SiblingSampleSet";
    protected static final String SIBLING_SAMPLE_SET_CAPTION = "Sibling Sample Set";

    protected static final String COL_DESCRIPTION_CAPTION = "Description";
    protected static final String COL_DESCRIPTION_NAME = "Description";
    protected static final String COL_NAME_CAPTION = "Sample ID";
    protected static final String COL_NAME_NAME = "name";

    protected static Map<String, String> _mapCaptionToName = Map.of(
            COL_DESCRIPTION_CAPTION, COL_DESCRIPTION_NAME,
            COL_NAME_CAPTION, COL_NAME_NAME);

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @BeforeClass
    public static void setupProject()
    {
        SampleSetParentColumn init = (SampleSetParentColumn) getCurrentTest();

        // Comment out this line (after you run once) it will make iterating on  tests much easier.
        init.doSetup();
    }

    private void doSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(PROJECT_NAME, "Study");
        _containerHelper.createSubfolder(PROJECT_NAME, SUB_FOLDER_NAME, "Study");

        projectMenu().navigateToProject(PROJECT_NAME);
        portalHelper.addWebPart("Sample Sets");

        setUpPopulatedSampleSet(PARENT_CONTAINER_SAMPLE_SET_NAME, PROJECT_NAME, "S_");

        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);
        portalHelper.addWebPart("Sample Sets");

        setUpPopulatedSampleSet(SIBLING_SAMPLE_SET_NAME, PROJECT_NAME + "/" + SUB_FOLDER_NAME, "SIB_");
    }

    private void setUpPopulatedSampleSet(String sampleSetName, String path, String namePrefix)
    {
        List<Map<String, String>> preDefinedSamples = new ArrayList<>();

        // Create a sample set.
        TestDataGenerator dgen = createEmptySampleSet(sampleSetName, path);

        // Create the data structure that will have the expected UI data (_CAPTION mapped with a value).
        for(int i = 0; i < 30; i++)
        {
            preDefinedSamples.add(Map.of(COL_NAME_CAPTION, namePrefix + i,
                    COL_DESCRIPTION_CAPTION, "Use me as a parent. " + namePrefix + i));
        }

        // Create some samples so we have known data to work with. Must add a 'Name' column, and need to used the fields _NAME value.
        for (Map<String, String> sampleUIData : preDefinedSamples)
        {
            Map<String, Object> sampleInsertData = new HashMap<>();

            for (String key : sampleUIData.keySet())
            {
                sampleInsertData.put(_mapCaptionToName.get(key), sampleUIData.get(key));
            }

            dgen.addCustomRow(sampleInsertData);

        }

        try
        {
            dgen.insertRows(createDefaultConnection(true), dgen.getRows());
        }
        catch (IOException | CommandException rethrow)
        {
            throw new RuntimeException(rethrow);
        }

    }

    protected TestDataGenerator createEmptySampleSet(String sampleSetName, String path)
    {
        List<FieldDefinition> fields = new ArrayList<>();

        for(String key : _mapCaptionToName.keySet())
        {

            switch(key)
            {
                case COL_NAME_CAPTION:
                    fields.add(new FieldDefinition(_mapCaptionToName.get(key))
                            .setType(FieldDefinition.ColumnType.String));
                    break;
                case COL_DESCRIPTION_CAPTION:
                default:
                    // These fields are automatically created when the sample set is created. Do nothing for them.
                    break;
            }

        }

        TestDataGenerator dgen = new TestDataGenerator("exp.materials", sampleSetName, path)
                .withColumnSet(fields);

        try
        {
            dgen.createDomain(createDefaultConnection(true), "SampleSet");
        }
        catch (IOException | CommandException rethrow)
        {
            throw new RuntimeException(rethrow);
        }

        return dgen;
    }

    private void createSampleSetAndSetParentColumn(String sampleSetName, String path, Map<String, String> parentColumnAliases)
    {

        log("Create a sample set named '" + sampleSetName + "' in '" + path + "'.");
        createEmptySampleSet(sampleSetName, path);
        // Because the sample set was create with the API the test will need to refresh the page
        // in order to see the sample set in the dataregion
        refresh();

        SampleSetHelper sampleHelper = new SampleSetHelper(this);

        log("Add a parent alias column to the sample set.");
        sampleHelper.goToEditSampleSet(sampleSetName);

        sampleHelper.addParentColumnAlias(parentColumnAliases);

        clickButton("Update");

    }

    private void checkDataRegionOnSampleDetailPage(String dataRegionName, String columnName, List<String> expectedValues)
    {
        DataRegionTable dataRegionTable = new DataRegionTable(dataRegionName, this);

        List<String> dataInTable = dataRegionTable.getColumnDataAsText(columnName);

        Assert.assertEquals("Number of entries in the column '" + columnName + "' is not as expected.", expectedValues.size(), dataInTable.size());

        for(String expectedValue : expectedValues)
        {
            Assert.assertTrue("Value '" + expectedValue + "' was not shown in column '" + columnName + "'.", dataInTable.contains(expectedValue));
        }

    }

    @Test
    public void testParentInSameSampleSet()
    {

        final String PARENT_COLUMN = "P1";
        final String SAMPLE_SET_NAME = "SimpleSampleSet01";

        // Add the Alias column as a regression test.
        String sampleText = "Name\tAlias\t" + PARENT_COLUMN + "\n" +
                "SA_01\t\n" +
                "SA_02\t\tSA_03\n" +
                "SA_03\t\n" +
                "SA_04\t\tSA_01\n" +
                "SA_05\t\tSA_02\n";

        goToProjectHome();
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        createSampleSetAndSetParentColumn(SAMPLE_SET_NAME, PROJECT_NAME + "/" + SUB_FOLDER_NAME, Map.of(PARENT_COLUMN, SAMPLE_SET_NAME));

        log("Import samples that have an alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.bulkImport(sampleText);

        log("Go to the detail page for various samples and make sure the precursor and child sample values are correct.");

        log("Check sample 'SA_05' and make sure the parent materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SA_05"));

        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("SA_03", "SA_02"));

        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" ", "Derive sample from SA_03"));

        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from SA_02"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SA_03' and make sure the child materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SA_03"));

        checkDataRegionOnSampleDetailPage("childMaterials", "Name", List.of("SA_05", "SA_02"));

        checkDataRegionOnSampleDetailPage("childMaterials", "Run", List.of("Derive sample from SA_02", "Derive sample from SA_03"));

        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from SA_02", "Derive sample from SA_03"));

    }

    @Test
    public void testValidAliasNames()
    {

        final String PARENT_COLUMN_1 = "P2 Column";
        final String SAMPLE_SET_NAME = "SimpleSampleSet02";

        String sampleText = "Name\tAlias\t" + PARENT_COLUMN_1 + "\n" +
                "SB_01\n" +
                "SB_02\t\tSB_03\n" +
                "SB_03\n" +
                "SB_04\t\tSB_01\n" +
                "SB_05\t\tSB_02\n";

        goToProjectHome();
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        createSampleSetAndSetParentColumn(SAMPLE_SET_NAME, PROJECT_NAME + "/" + SUB_FOLDER_NAME, Map.of(PARENT_COLUMN_1, SAMPLE_SET_NAME));

        log("Import samples that have an alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.bulkImport(sampleText);

        log("Go to the detail page for various samples and make sure the precursor and child sample values are correct.");

        log("Check sample 'SB_05' and make sure the parent materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SB_05"));

        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("SB_03", "SB_02"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" ", "Derive sample from SB_03"));
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from SB_02"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SB_03' and make sure the child materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SB_03"));

        checkDataRegionOnSampleDetailPage("childMaterials", "Name", List.of("SB_05", "SB_02"));
        checkDataRegionOnSampleDetailPage("childMaterials", "Run", List.of("Derive sample from SB_02", "Derive sample from SB_03"));
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from SB_02", "Derive sample from SB_03"));

        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);
        sampleHelper = new SampleSetHelper(this);

        log("Insert samples where parent column is a different case.");
        sampleText = "Name\tAlias\t" + PARENT_COLUMN_1.toLowerCase() + "\n" +
                "SB_06\n" +
                "SB_07\t\tSB_08\n" +
                "SB_08\n" +
                "SB_09\t\tSB_06\n" +
                "SB_10\t\tSB_07\n";

        sampleHelper.goToSampleSet(SAMPLE_SET_NAME);
        sampleHelper.bulkImport(sampleText);

        log("Go to the detail page for the second set of samples imported and make sure the precursor and child sample values are correct.");

        log("Check sample 'SB_10' and make sure the parent materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SB_10"));

        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("SB_08", "SB_07"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" ", "Derive sample from SB_08"));
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from SB_07"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SB_08' and make sure the child materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SB_08"));

        checkDataRegionOnSampleDetailPage("childMaterials", "Name", List.of("SB_10", "SB_07"));
        checkDataRegionOnSampleDetailPage("childMaterials", "Run", List.of("Derive sample from SB_07", "Derive sample from SB_08"));
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from SB_07", "Derive sample from SB_08"));

    }

    @Test
    public void testParentInParentContainer()
    {

        final String PARENT_COLUMN = "P3";
        final String SAMPLE_SET_NAME = "SimpleSampleSet03";

        // Add the Alias column as a regression test.
        String sampleText = "Name\tAlias\t" + PARENT_COLUMN + "\n" +
                "SC_01\t\n" +
                "SC_02\t\tS_0\n" +
                "SC_03\t\n" +
                "SC_04\t\tS_1\n" +
                "SC_05\t\tS_0\n";

        goToProjectHome();
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        createSampleSetAndSetParentColumn(SAMPLE_SET_NAME, PROJECT_NAME + "/" + SUB_FOLDER_NAME, Map.of(PARENT_COLUMN, PARENT_CONTAINER_SAMPLE_SET_NAME));

        log("Import samples that have an alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.bulkImport(sampleText);

        log("Check sample 'SC_05' and make sure the parent materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SC_05"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("S_0"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" "));
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive 2 samples from S_0"));

    }

    @Test
    public void testParentInSiblingContainer()
    {

        final String PARENT_COLUMN = "P4";
        final String SAMPLE_SET_NAME = "SimpleSampleSet04";

        // Add the Alias column as a regression test.
        String sampleText = "Name\tAlias\t" + PARENT_COLUMN + "\n" +
                "SD_01\t\n" +
                "SD_02\t\tSIB_0\n" +
                "SD_03\t\n" +
                "SD_04\t\tSIB_1\n" +
                "SD_05\t\tSIB_0\n";

        goToProjectHome();
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        createSampleSetAndSetParentColumn(SAMPLE_SET_NAME, PROJECT_NAME + "/" + SUB_FOLDER_NAME, Map.of(PARENT_COLUMN, SIBLING_SAMPLE_SET_NAME));

        log("Import samples that have an alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.bulkImport(sampleText);

        log("Check sample 'SD_05' and make sure the parent materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SD_05"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("SIB_0"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" "));
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive 2 samples from SIB_0"));

    }

    @Test
    public void testMultipleParentColumns()
    {

        final String PARENT_COLUMN_SUB = "Parent-Sub-Folder";
        final String PARENT_COLUMN_CONTAINER = "Parent Parent Folder";
        final String SAMPLE_SET_NAME = "SimpleSampleSet05";

        // Add the Alias column as a regression test.
        String sampleText = "Name\tAlias\t" + PARENT_COLUMN_SUB + "\t" + PARENT_COLUMN_CONTAINER + "\n" +
                "SE_01\t\n" +
                "SE_02\t\t\tS_10\n" +
                "SE_03\t\tSE_01\n" +
                "SE_04\n" +
                "SE_05\t\tSE_04\tS_11\n";

        goToProjectHome();
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        createSampleSetAndSetParentColumn(SAMPLE_SET_NAME,
                PROJECT_NAME + "/" + SUB_FOLDER_NAME,
                Map.of(PARENT_COLUMN_CONTAINER, PARENT_CONTAINER_SAMPLE_SET_NAME,
                        PARENT_COLUMN_SUB, SAMPLE_SET_NAME));

        log("Import samples that have an alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.bulkImport(sampleText);

        log("Check sample 'SE_02' and make sure the parent materials are correct. It's parent should be in the parent container.");
        waitAndClickAndWait(Locator.linkWithText("SE_02"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("S_10"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" "));
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from S_10"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SE_03' and make sure the parent materials are correct. It's parent should be in the same sample set.");
        waitAndClickAndWait(Locator.linkWithText("SE_03"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("SE_01"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" "));
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from SE_01"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SE_05' which should have two parents, one in this sample set and another in the parent container sample set .");
        waitAndClickAndWait(Locator.linkWithText("SE_05"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("SE_04", "S_11"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" ", " "));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Sample Set", List.of(SAMPLE_SET_NAME, PARENT_CONTAINER_SAMPLE_SET_NAME));

        // Not sure how reliable this test will be, will the order always be "S_11, SE_04"?
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from S_11, SE_04"));

    }

    @Test
    public void testMaterialInputsWorkWithAliases()
    {

        final String PARENT_COLUMN_SUB = "Parent-Sub-Folder";
        final String PARENT_COLUMN_CONTAINER = "Parent Parent Folder";
        final String SAMPLE_SET_NAME = "SimpleSampleSet06";

        // Add the Alias column as a regression test.
        String sampleText = "Name\tAlias\tmaterialInputs/" + SAMPLE_SET_NAME + "\tmaterialInputs/" + PARENT_CONTAINER_SAMPLE_SET_NAME + "\n" +
                "SF_01\t\n" +
                "SF_02\t\t\tS_20\n" +
                "SF_03\t\tSF_01\n" +
                "SF_04\n" +
                "SF_05\t\tSF_04\tS_21\n";

        goToProjectHome();
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        log("Add the parent columns just like before.");
        createSampleSetAndSetParentColumn(SAMPLE_SET_NAME,
                PROJECT_NAME + "/" + SUB_FOLDER_NAME,
                Map.of(PARENT_COLUMN_CONTAINER, PARENT_CONTAINER_SAMPLE_SET_NAME,
                        PARENT_COLUMN_SUB, SAMPLE_SET_NAME));

        log("Import samples that use the 'materialInputs column header.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.bulkImport(sampleText);

        log("Check sample 'SF_02' and make sure the parent materials are correct. It's parent should be in the parent container.");
        waitAndClickAndWait(Locator.linkWithText("SF_02"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("S_20"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" "));
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from S_20"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SF_03' and make sure the parent materials are correct. It's parent should be in the same sample set.");
        waitAndClickAndWait(Locator.linkWithText("SF_03"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("SF_01"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" "));
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from SF_01"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SF_05' which should have two parents, one in this sample set and another in the parent container sample set .");
        waitAndClickAndWait(Locator.linkWithText("SF_05"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("SF_04", "S_21"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" ", " "));

        // TODO uncomment this check when issue 37982 is resolved.
//        checkDataRegionOnSampleDetailPage("parentMaterials", "Sample Set", List.of(SAMPLE_SET_NAME, PARENT_CONTAINER_SAMPLE_SET_NAME));

        // Not sure how reliable this test will be, will the order always be "SE_11, SE_04"?
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from S_21, SF_04"));

        // Not really sure how valuable the following checks are.
        log("Now import some more samples using the alias.");
        // Add the Alias column as a regression test.
        sampleText = "Name\tAlias\t" + PARENT_COLUMN_SUB + "\t" + PARENT_COLUMN_CONTAINER + "\n" +
                "SF_06\t\n" +
                "SF_07\t\t\tS_22\n" +
                "SF_08\t\tSF_01\n" +
                "SF_09\n" +
                "SF_10\t\tSF_04\tS_23\n";

        sampleHelper.goToSampleSet(SAMPLE_SET_NAME);
        sampleHelper.bulkImport(sampleText);

        log("Check sample 'SF_07' and make sure the parent materials are correct. It's parent should be in the parent container.");
        waitAndClickAndWait(Locator.linkWithText("SF_07"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("S_22"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" "));
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from S_22"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SF_08' and make sure the parent materials are correct. It's parent should be in the same sample set.");
        waitAndClickAndWait(Locator.linkWithText("SF_08"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("SF_01"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" "));
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from SF_01"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SF_10' which should have two parents, one in this sample set and another in the parent container sample set .");
        waitAndClickAndWait(Locator.linkWithText("SF_10"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("SF_04", "S_23"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" ", " "));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Sample Set", List.of(SAMPLE_SET_NAME, PARENT_CONTAINER_SAMPLE_SET_NAME));

    }

    @Test
    public void testUseThenRemoveAndAliasParentColumn()
    {

        final String PARENT_COLUMN = "P7";
        final String SAMPLE_SET_NAME = "SimpleSampleSet07";

        // Add the Alias column as a regression test.
        String sampleText = "Name\tAlias\t" + PARENT_COLUMN + "\n" +
                "SG_01\t\n" +
                "SG_02\t\tSG_01\n";

        goToProjectHome();
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        createSampleSetAndSetParentColumn(SAMPLE_SET_NAME, PROJECT_NAME + "/" + SUB_FOLDER_NAME, Map.of(PARENT_COLUMN, SAMPLE_SET_NAME));

        log("Import samples that have an alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.bulkImport(sampleText);

        log("Skip validation of this basic case (it is checked in another test).");

        log("Remove the parent alias column");
        clickButton("Edit Set");

        sampleHelper = new SampleSetHelper(this);

        sampleHelper.removeParentColumnAlias(PARENT_COLUMN);

        log("Import some more samples using the alias column and make sure it doesn't work.");
        sampleText = "Name\tAlias\t" + PARENT_COLUMN + "\n" +
                "SG_03\t\tSG_01\n";

        sampleHelper.bulkImport(sampleText);

        log("Check sample 'SG_03' and make sure there are no parents.");
        waitAndClickAndWait(Locator.linkWithText("SG_03"));

        DataRegionTable dataRegionTable = new DataRegionTable("parentMaterials", this);

        Assert.assertEquals("There should be no entries in the 'Precursor Samples' table.", 0, dataRegionTable.getDataRowCount());

        log("Make sure that the original element still has it's parent.");

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SG_02' and make sure that the parent values are unchanged.");
        waitAndClickAndWait(Locator.linkWithText("SG_02"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Name", List.of("SG_01"));
        checkDataRegionOnSampleDetailPage("parentMaterials", "Run", List.of(" "));
        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from SG_01"));

        log("Go to SG_01 and make sure it still only has one child.");
        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        waitAndClickAndWait(Locator.linkWithText("SG_01"));

        checkDataRegionOnSampleDetailPage("childMaterials", "Name", List.of("SG_02"));

        checkDataRegionOnSampleDetailPage("childMaterials", "Run", List.of("Derive sample from SG_01"));

        checkDataRegionOnSampleDetailPage("Runs", "Name", List.of("Derive sample from SG_01"));

    }

}
