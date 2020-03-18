package org.labkey.test.tests;

import org.hamcrest.CoreMatchers;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyC;
import org.labkey.test.components.labkey.ui.samples.SampleTypeDesigner;
import org.labkey.test.pages.experiment.UpdateSampleSetPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleSetDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleSetHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleSetAPIHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Category({DailyC.class})
public class SampleSetParentColumnTest extends BaseWebDriverTest
{

    private static final String PROJECT_NAME = "SampleSetParentAliasProject";
    private static final String SUB_FOLDER_NAME = "ParentAliasSubFolder";

    protected static final String PARENT_CONTAINER_SAMPLE_SET_NAME = "PrePopulatedSampleSet";
    protected static final String PARENT_CONTAINER_SAMPLE_SET_CAPTION = "Pre Populated Sample Set";

    protected static final String PARENT_CONTAINER_DATA_CLASS_NAME = "PrePopulatedDataClass";
    protected static final String PARENT_CONTAINER_DATA_CLASS_CAPTION = "Pre Populated Data Class";

    protected static final String SIBLING_SAMPLE_SET_NAME = "SiblingSampleSet";
    protected static final String SIBLING_SAMPLE_SET_CAPTION = "Sibling Sample Set";

    protected static final String SIBLING_DATA_CLASS_NAME = "SiblingDataClass";
    protected static final String SIBLING_DATA_CLASS_CAPTION = "Sibling Data Class";

    protected static final String COL_DESCRIPTION_CAPTION = "Description";
    protected static final String COL_DESCRIPTION_NAME = "Description";
    protected static final String COL_NAME_CAPTION = "Sample ID";
    protected static final String COL_NAME_NAME = "name";

    protected static Map<String, String> _mapCaptionToName = Map.of(
            COL_DESCRIPTION_CAPTION, COL_DESCRIPTION_NAME,
            COL_NAME_CAPTION, COL_NAME_NAME);

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

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @BeforeClass
    public static void setupProject()
    {
        SampleSetParentColumnTest init = (SampleSetParentColumnTest) getCurrentTest();

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
        portalHelper.addWebPart("Data Classes");

        createAndPopulateSampleSet(PARENT_CONTAINER_SAMPLE_SET_NAME, PROJECT_NAME, "S_", "Use me as a parent. ");
        createAndPopulateDataClass(PARENT_CONTAINER_DATA_CLASS_NAME, PROJECT_NAME, "DC_", "DataClass Object parent. ");

        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);
        portalHelper.addWebPart("Sample Sets");
        portalHelper.addWebPart("Data Classes");

        createAndPopulateSampleSet(SIBLING_SAMPLE_SET_NAME, PROJECT_NAME + "/" + SUB_FOLDER_NAME, "SIB_", "Use me as a parent (sub folder). ");
        createAndPopulateDataClass(SIBLING_DATA_CLASS_NAME, PROJECT_NAME + "/" + SUB_FOLDER_NAME, "DCSIB_", "DataClass Object parent (sub folder). ");

    }

    private void createAndPopulateSampleSet(String sampleSetName, String path, String namePrefix, String descriptionPrefix)
    {
        TestDataGenerator dataGen = createEmptySampleSet(sampleSetName, path);
        populateDomainWithSimpleData(dataGen, namePrefix, descriptionPrefix);
    }

    protected TestDataGenerator createEmptySampleSet(String sampleSetName, String path)
    {
        return createEmptySampleSet(sampleSetName, path, null);
    }

    protected TestDataGenerator createEmptySampleSet(String sampleSetName, String path, @Nullable List<FieldDefinition> customFields)
    {
        List<FieldDefinition> fields = new ArrayList<>();

        // From what I saw I had problems if I did not create the name field for a sample set.
        for(String key : _mapCaptionToName.keySet())
        {

            switch(key)
            {
                case COL_NAME_CAPTION:
                    fields.add(new FieldDefinition(_mapCaptionToName.get(key))
                            .setType(FieldDefinition.ColumnType.String));
                    break;
                default:
                    // These fields are automatically created when the domain is created. Do nothing for them.
                    break;
            }

        }

        if(null != customFields)
            fields.addAll(customFields);

        return createEmptyDomain("exp.materials", "SampleSet", sampleSetName, path, fields);
    }

    private void createAndPopulateDataClass(String dataClassName, String path, String namePrefix, String descriptionPrefix)
    {
        TestDataGenerator dataGen = createEmptyDataClass(dataClassName, path);
        populateDomainWithSimpleData(dataGen, namePrefix, descriptionPrefix);
    }

    protected TestDataGenerator createEmptyDataClass(String dataClassName, String path)
    {
        return createEmptyDataClass(dataClassName, path, null);
    }

    protected TestDataGenerator createEmptyDataClass(String dataClassName, String path, @Nullable List<FieldDefinition> fields)
    {
        // Unlike Sample Sets Data Classes complained if I explicitly created the name field.
        return createEmptyDomain("exp.data", "DataClass", dataClassName, path, fields);
    }

    protected TestDataGenerator createEmptyDomain(String schema, String domainKind, String domainName, String path, @Nullable List<FieldDefinition> fields)
    {
        TestDataGenerator dgen;

        if(null != fields)
        {
            dgen = new TestDataGenerator(schema, domainName, path).withColumns(fields);
        }
        else
        {
            dgen = new TestDataGenerator(schema, domainName, path);
        }

        try
        {
            dgen.createDomain(createDefaultConnection(true), domainKind);
        }
        catch (IOException | CommandException rethrow)
        {
            throw new RuntimeException(rethrow);
        }

        return dgen;
    }

    private void populateDomainWithSimpleData(TestDataGenerator dataGen, String namePrefix, String descriptionPrefix)
    {
        List<Map<String, Object>> preDefinedSamples = new ArrayList<>();

        for(int i = 0; i < 30; i++)
        {
            preDefinedSamples.add(Map.of(COL_NAME_NAME, namePrefix + i,
                    COL_DESCRIPTION_CAPTION, descriptionPrefix + namePrefix + i));
        }

        try
        {
            dataGen.insertRows(createDefaultConnection(true), preDefinedSamples);
        }
        catch (IOException | CommandException rethrow)
        {
            throw new RuntimeException(rethrow);
        }

    }

    private void checkAllRowsInDataRegion(String dataRegionName, String columnName, List<String> expectedValues)
    {
        DataRegionTable dataRegionTable = new DataRegionTable(dataRegionName, this);

        List<String> dataInTable = dataRegionTable.getColumnDataAsText(columnName);

        Assert.assertEquals("Number of entries in the column '" + columnName + "' is not as expected.", expectedValues.size(), dataInTable.size());

        for(String expectedValue : expectedValues)
        {
            Assert.assertTrue("Value '" + expectedValue + "' was not shown in column '" + columnName + "' in data region '" + dataRegionName +"'.", dataInTable.contains(expectedValue));
        }

    }

    private void regexCheckRowInDataRegion(String dataRegionName, int rowIndex, String columnName, String regexExpected)
    {
        DataRegionTable dataRegionTable = new DataRegionTable(dataRegionName, this);

        List<String> dataInTable = dataRegionTable.getColumnDataAsText(columnName);

        Assert.assertTrue("There were no rows in the data region '" + dataRegionName + "'.", dataInTable.size() > 0);
        Assert.assertTrue("The index given for the row (" + rowIndex + ") is beyond the number of rows in the data region '" + dataRegionName + "' (" + dataInTable.size() + ").", rowIndex < dataInTable.size());

        String cellValue = dataInTable.get(rowIndex);

        Pattern p = Pattern.compile(regexExpected);
        Matcher m = p.matcher(cellValue);

        Assert.assertTrue("For data-region '" + dataRegionName + "', column '" + columnName + "' the regular express '" + regexExpected + "' did not match the value '" + cellValue + "'.", m.find());

    }

    @Test
    public void testParentInSameSampleSet()
    {

        final String PARENT_COLUMN = "P1";
        final String SAMPLE_SET_NAME = "SimpleSampleSet01";

        String sampleText = "Name\t" + PARENT_COLUMN + "\n" +
                "SA_01\t\n" +
                "SA_02\tSA_03\n" +
                "SA_03\t\n" +
                "SA_04\tSA_01\n" +
                "SA_05\tSA_02\n";

        SampleSetDefinition definition = new SampleSetDefinition(SAMPLE_SET_NAME);
        definition.addParentAlias(PARENT_COLUMN);
        SampleSetAPIHelper.createEmptySampleSet(PROJECT_NAME + "/" + SUB_FOLDER_NAME, definition);
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        log("Import samples that have a parent alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.goToSampleSet(SAMPLE_SET_NAME).bulkImport(sampleText);

        log("Go to the detail page for various samples and make sure the precursor and child sample values are correct.");

        log("Check sample 'SA_05' and make sure the parent materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SA_05"));

        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("SA_03", "SA_02"));

        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" ", "Derive sample from SA_03"));

        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from SA_02"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SA_03' and make sure the child materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SA_03"));

        checkAllRowsInDataRegion("childMaterials", "Name", List.of("SA_05", "SA_02"));

        checkAllRowsInDataRegion("childMaterials", "Run", List.of("Derive sample from SA_02", "Derive sample from SA_03"));

        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from SA_02", "Derive sample from SA_03"));

    }

    @Test
    public void testValidAliasNames()
    {

        final String PARENT_COLUMN_1 = "P2 Column";
        final String SAMPLE_SET_NAME = "SimpleSampleSet02";

        String sampleText = "Name\t" + PARENT_COLUMN_1 + "\n" +
                "SB_01\n" +
                "SB_02\tSB_03\n" +
                "SB_03\n" +
                "SB_04\tSB_01\n" +
                "SB_05\tSB_02\n";

        SampleSetDefinition definition = new SampleSetDefinition(SAMPLE_SET_NAME);
        definition.addParentAlias(PARENT_COLUMN_1);
        SampleSetAPIHelper.createEmptySampleSet(PROJECT_NAME + "/" + SUB_FOLDER_NAME, definition);
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        log("Import samples that have a parent alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.goToSampleSet(SAMPLE_SET_NAME).bulkImport(sampleText);

        log("Go to the detail page for various samples and make sure the precursor and child sample values are correct.");

        log("Check sample 'SB_05' and make sure the parent materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SB_05"));

        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("SB_03", "SB_02"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" ", "Derive sample from SB_03"));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from SB_02"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SB_03' and make sure the child materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SB_03"));

        checkAllRowsInDataRegion("childMaterials", "Name", List.of("SB_05", "SB_02"));
        checkAllRowsInDataRegion("childMaterials", "Run", List.of("Derive sample from SB_02", "Derive sample from SB_03"));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from SB_02", "Derive sample from SB_03"));

        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);
        sampleHelper = new SampleSetHelper(this);

        log("Insert samples where parent column is a different case.");
        sampleText = "Name\t" + PARENT_COLUMN_1.toLowerCase() + "\n" +
                "SB_06\n" +
                "SB_07\tSB_08\n" +
                "SB_08\n" +
                "SB_09\tSB_06\n" +
                "SB_10\tSB_07\n";

        sampleHelper.goToSampleSet(SAMPLE_SET_NAME);
        sampleHelper.bulkImport(sampleText);

        log("Go to the detail page for the second set of samples imported and make sure the precursor and child sample values are correct.");

        log("Check sample 'SB_10' and make sure the parent materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SB_10"));

        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("SB_08", "SB_07"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" ", "Derive sample from SB_08"));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from SB_07"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SB_08' and make sure the child materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SB_08"));

        checkAllRowsInDataRegion("childMaterials", "Name", List.of("SB_10", "SB_07"));
        checkAllRowsInDataRegion("childMaterials", "Run", List.of("Derive sample from SB_07", "Derive sample from SB_08"));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from SB_07", "Derive sample from SB_08"));

    }

    @Test
    public void testParentInParentContainer()
    {

        final String PARENT_COLUMN = "P3";
        final String SAMPLE_SET_NAME = "SimpleSampleSet03";

        String sampleText = "Name\t" + PARENT_COLUMN + "\n" +
                "SC_01\n" +
                "SC_02\tS_0\n" +
                "SC_03\n" +
                "SC_04\tS_1\n" +
                "SC_05\tS_0\n";

        SampleSetDefinition definition = new SampleSetDefinition(SAMPLE_SET_NAME);
        definition.addParentAlias(PARENT_COLUMN, PARENT_CONTAINER_SAMPLE_SET_NAME);
        SampleSetAPIHelper.createEmptySampleSet(PROJECT_NAME + "/" + SUB_FOLDER_NAME, definition);
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        log("Import samples that have a parent alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.goToSampleSet(SAMPLE_SET_NAME).bulkImport(sampleText);

        log("Check sample 'SC_05' and make sure the parent materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SC_05"));
        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("S_0"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" "));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive 2 samples from S_0"));

    }

    @Test
    public void testParentInSiblingContainer()
    {

        final String PARENT_COLUMN = "P4";
        final String SAMPLE_SET_NAME = "SimpleSampleSet04";

        String sampleText = "Name\t" + PARENT_COLUMN + "\n" +
                "SD_01\n" +
                "SD_02\tSIB_0\n" +
                "SD_03\n" +
                "SD_04\tSIB_1\n" +
                "SD_05\tSIB_0\n";

        SampleSetDefinition definition = new SampleSetDefinition(SAMPLE_SET_NAME);
        definition.addParentAlias(PARENT_COLUMN, SIBLING_SAMPLE_SET_NAME);
        SampleSetAPIHelper.createEmptySampleSet(PROJECT_NAME + "/" + SUB_FOLDER_NAME, definition);
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        log("Import samples that have a parent alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.goToSampleSet(SAMPLE_SET_NAME).bulkImport(sampleText);

        log("Check sample 'SD_05' and make sure the parent materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SD_05"));
        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("SIB_0"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" "));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive 2 samples from SIB_0"));

    }

    @Test
    public void testMultipleParentColumns()
    {

        final String PARENT_COLUMN_SUB = "Parent-Sub-Folder";
        final String PARENT_COLUMN_CONTAINER = "Parent Parent Folder";
        final String SAMPLE_SET_NAME = "SimpleSampleSet05";

        String sampleText = "Name\t" + PARENT_COLUMN_SUB + "\t" + PARENT_COLUMN_CONTAINER + "\n" +
                "SE_01\n" +
                "SE_02\t\tS_10\n" +
                "SE_03\tSE_01\n" +
                "SE_04\n" +
                "SE_05\tSE_04\tS_11\n";

        SampleSetDefinition definition = new SampleSetDefinition(SAMPLE_SET_NAME);
        definition.addParentAlias(PARENT_COLUMN_CONTAINER, PARENT_CONTAINER_SAMPLE_SET_NAME);
        definition.addParentAlias(PARENT_COLUMN_SUB);
        SampleSetAPIHelper.createEmptySampleSet(PROJECT_NAME + "/" + SUB_FOLDER_NAME, definition);
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        log("Import samples that have a parent alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.goToSampleSet(SAMPLE_SET_NAME).bulkImport(sampleText);

        log("Check sample 'SE_02' and make sure the parent materials are correct. It's parent should be in the parent container.");
        waitAndClickAndWait(Locator.linkWithText("SE_02"));
        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("S_10"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" "));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from S_10"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SE_03' and make sure the parent materials are correct. It's parent should be in the same sample set.");
        waitAndClickAndWait(Locator.linkWithText("SE_03"));
        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("SE_01"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" "));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from SE_01"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SE_05' which should have two parents, one in this sample set and another in the parent container sample set .");
        waitAndClickAndWait(Locator.linkWithText("SE_05"));
        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("SE_04", "S_11"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" ", " "));
        checkAllRowsInDataRegion("parentMaterials", "Sample Set", List.of(SAMPLE_SET_NAME, PARENT_CONTAINER_SAMPLE_SET_NAME));

        regexCheckRowInDataRegion("Runs", 0, "Name", "Derive sample from (?:S_11, SE_04|SE_04, S_11)");

    }

    /**
     Check that the alias column does not conflict with parent mappings (Issue 37946)
     **/
    @Test
    public void testMaterialInputsWorkWithAliases()
    {

        final String PARENT_COLUMN_SUB = "Parent-Sub-Folder";
        final String PARENT_COLUMN_CONTAINER = "Parent Parent Folder";
        final String SAMPLE_SET_NAME = "SimpleSampleSet06";

        String sampleText = "Name\tmaterialInputs/" + SAMPLE_SET_NAME + "\tmaterialInputs/" + PARENT_CONTAINER_SAMPLE_SET_NAME + "\n" +
                "SF_01\t\n" +
                "SF_02\t\tS_20\n" +
                "SF_03\tSF_01\n" +
                "SF_04\n" +
                "SF_05\tSF_04\tS_21\n";

        log("Add the parent columns just like before.");
        SampleSetDefinition definition = new SampleSetDefinition(SAMPLE_SET_NAME);
        definition.addParentAlias(PARENT_COLUMN_CONTAINER, PARENT_CONTAINER_SAMPLE_SET_NAME);
        definition.addParentAlias(PARENT_COLUMN_SUB);
        SampleSetAPIHelper.createEmptySampleSet(PROJECT_NAME + "/" + SUB_FOLDER_NAME, definition);
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        log("Import samples that use the 'materialInputs column header.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.goToSampleSet(SAMPLE_SET_NAME).bulkImport(sampleText);

        log("Check sample 'SF_02' and make sure the parent materials are correct. It's parent should be in the parent container.");
        waitAndClickAndWait(Locator.linkWithText("SF_02"));
        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("S_20"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" "));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from S_20"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SF_03' and make sure the parent materials are correct. It's parent should be in the same sample set.");
        waitAndClickAndWait(Locator.linkWithText("SF_03"));
        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("SF_01"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" "));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from SF_01"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SF_05' which should have two parents, one in this sample set and another in the parent container sample set .");
        waitAndClickAndWait(Locator.linkWithText("SF_05"));
        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("SF_04", "S_21"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" ", " "));

        checkAllRowsInDataRegion("parentMaterials", "Sample Set", List.of(SAMPLE_SET_NAME, PARENT_CONTAINER_SAMPLE_SET_NAME));

        regexCheckRowInDataRegion("Runs", 0, "Name", "Derive sample from (?:S_21, SF_04|SF_04, S_21)");

        // Not really sure how valuable the following checks are.
        log("Now import some more samples using the alias.");
        // Add the Alias column as a regression test.
        sampleText = "Name\t" + PARENT_COLUMN_SUB + "\t" + PARENT_COLUMN_CONTAINER + "\n" +
                "SF_06\n" +
                "SF_07\t\tS_22\n" +
                "SF_08\tSF_01\n" +
                "SF_09\n" +
                "SF_10\tSF_04\tS_23\n";

        sampleHelper.goToSampleSet(SAMPLE_SET_NAME);
        sampleHelper.bulkImport(sampleText);

        log("Check sample 'SF_07' and make sure the parent materials are correct. It's parent should be in the parent container.");
        waitAndClickAndWait(Locator.linkWithText("SF_07"));
        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("S_22"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" "));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from S_22"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SF_08' and make sure the parent materials are correct. It's parent should be in the same sample set.");
        waitAndClickAndWait(Locator.linkWithText("SF_08"));
        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("SF_01"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" "));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from SF_01"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SF_10' which should have two parents, one in this sample set and another in the parent container sample set .");
        waitAndClickAndWait(Locator.linkWithText("SF_10"));
        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("SF_04", "S_23"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" ", " "));
        checkAllRowsInDataRegion("parentMaterials", "Sample Set", List.of(SAMPLE_SET_NAME, PARENT_CONTAINER_SAMPLE_SET_NAME));

    }

    @Test
    public void testUseThenRemoveAnAliasParentColumn()
    {

        final String PARENT_COLUMN = "P7";
        final String SAMPLE_SET_NAME = "SimpleSampleSet07";

        String sampleText = "Name\t" + PARENT_COLUMN + "\n" +
                "SG_01\n" +
                "SG_02\tSG_01\n";

        SampleSetDefinition definition = new SampleSetDefinition(SAMPLE_SET_NAME);
        definition.addParentAlias(PARENT_COLUMN);
        SampleSetAPIHelper.createEmptySampleSet(PROJECT_NAME + "/" + SUB_FOLDER_NAME, definition);
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        log("Import samples that have a parent alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.goToSampleSet(SAMPLE_SET_NAME).bulkImport(sampleText);

        log("Skip validation of this basic case (it is checked in another test).");

        log("Remove the parent alias column");
        clickButton("Edit Set");

        UpdateSampleSetPage updatePage = new UpdateSampleSetPage(getDriver());

        updatePage.removeParentAlias(PARENT_COLUMN);
        updatePage.clickSave();

        log("Import some more samples using the alias column and make sure it doesn't work.");
        sampleText = "Name\t" + PARENT_COLUMN + "\n" +
                "SG_03\tSG_01\n";

        sampleHelper.goToSampleSet(SAMPLE_SET_NAME).bulkImport(sampleText);

        log("Check sample 'SG_03' and make sure there are no parents.");
        waitAndClickAndWait(Locator.linkWithText("SG_03"));

        DataRegionTable dataRegionTable = new DataRegionTable("parentMaterials", this);

        Assert.assertEquals("There should be no entries in the 'Precursor Samples' table.", 0, dataRegionTable.getDataRowCount());

        log("Make sure that the original element still has it's parent.");

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SG_02' and make sure that the parent values are unchanged.");
        waitAndClickAndWait(Locator.linkWithText("SG_02"));
        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("SG_01"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" "));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from SG_01"));

        log("Go to SG_01 and make sure it still only has one child.");
        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        waitAndClickAndWait(Locator.linkWithText("SG_01"));

        checkAllRowsInDataRegion("childMaterials", "Name", List.of("SG_02"));

        checkAllRowsInDataRegion("childMaterials", "Run", List.of("Derive sample from SG_01"));

        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from SG_01"));

    }

    @Test
    public void testAliasNameConflictsWithFieldName()
    {
        final String ALIAS_NAME_CONFLICT = "ConflictName";
        final String GOOD_PARENT_NAME = "P8";
        final String SAMPLE_SET_NAME = "SimpleSampleSet08";

        goToProjectHome();
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        String path = PROJECT_NAME + "/" + SUB_FOLDER_NAME;

        List<FieldDefinition> fields = new ArrayList<>();

        fields.add(new FieldDefinition(ALIAS_NAME_CONFLICT)
                .setType(FieldDefinition.ColumnType.String));

        log("Create a sample set named '" + SAMPLE_SET_NAME + "' in '" + path + "'.");
        createEmptySampleSet(SAMPLE_SET_NAME, path, fields);

        // Because the sample set was create with the API the test will need to refresh the page
        // in order to see the sample set in the dataregion
        refresh();

        SampleSetHelper sampleHelper = new SampleSetHelper(this);

        log("Add a parent alias column to the sample set that conflicts with a given column name.");
        UpdateSampleSetPage updatePage = sampleHelper.goToEditSampleSet(SAMPLE_SET_NAME);
        updatePage.addParentAlias(ALIAS_NAME_CONFLICT);
        List<String> errors = getTexts(updatePage.clickSaveExpectingError());

        Assert.assertEquals("Error message not as expected.",
                "A sample set property already exists with parent alias header: " + ALIAS_NAME_CONFLICT,
                String.join("\n", errors));

        log("Now add a valid parent column and check that you cannot now add a field in the sample set with the same name.");
        updatePage.setParentAlias(1, GOOD_PARENT_NAME, SampleTypeDesigner.CURRENT_SAMPLE_TYPE);
        updatePage.clickSave();

        clickFolder(SUB_FOLDER_NAME);
        updatePage = sampleHelper.goToEditSampleSet(SAMPLE_SET_NAME);
        updatePage.getDomainEditor().addField(GOOD_PARENT_NAME);
        errors = getTexts(updatePage.clickSaveExpectingError());

        String errorMsgExpectedTxt = "'" + GOOD_PARENT_NAME + "' is a reserved field name in '" + SAMPLE_SET_NAME + "'.";
        Assert.assertThat("Error message", String.join("\n", errors), CoreMatchers.containsString(errorMsgExpectedTxt));

        updatePage.clickCancel();

        log("Validated name conflicts.");
    }

    @Test
    public void testDataClassAsParent()
    {
        final String PARENT_COLUMN_SUBFOLDER = "DC_Parent-Sub-Folder";
        final String PARENT_COLUMN_CONTAINER = "DC Parent Parent Folder";
        final String SAMPLE_SET_NAME = "SimpleSampleSet09";

        String sampleText = "Name\t" + PARENT_COLUMN_SUBFOLDER + "\t" + PARENT_COLUMN_CONTAINER + "\n" +
                "SI_01\n" +
                "SI_02\t\tDC_1\n" +
                "SI_03\tDCSIB_1\n" +
                "SI_04\n" +
                "SI_05\tDCSIB_2\tDC_2\n";

        SampleSetDefinition definition = new SampleSetDefinition(SAMPLE_SET_NAME);
        definition.addDataParentAlias(PARENT_COLUMN_SUBFOLDER, SIBLING_DATA_CLASS_NAME);
        definition.addDataParentAlias(PARENT_COLUMN_CONTAINER, PARENT_CONTAINER_DATA_CLASS_NAME);
        SampleSetAPIHelper.createEmptySampleSet(PROJECT_NAME + "/" + SUB_FOLDER_NAME, definition);
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        log("Import samples that have a parent alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.goToSampleSet(SAMPLE_SET_NAME).bulkImport(sampleText);

        log("Check sample 'SI_02' and make sure the parent materials are correct. It's parent should be in from the Data Class in the parent container.");
        waitAndClickAndWait(Locator.linkWithText("SI_02"));
        checkAllRowsInDataRegion("parentData", "Name", List.of("DC_1"));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from DC_1"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SI_03' and make sure the parent materials are correct. It's parent should be from the Data Class in the same folder.");
        waitAndClickAndWait(Locator.linkWithText("SI_03"));
        checkAllRowsInDataRegion("parentData", "Name", List.of("DCSIB_1"));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from DCSIB_1"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SI_05' which should have two parents, one from the Data Class in this folder and another in the Data Class in the parent container.");
        waitAndClickAndWait(Locator.linkWithText("SI_05"));

        checkAllRowsInDataRegion("parentData", "Name", List.of("DC_2", "DCSIB_2"));

        regexCheckRowInDataRegion("Runs", 0, "Name", "Derive sample from (?:DC_2, DCSIB_2|DCSIB_2, DC_2)");

    }

    @Test
    public void testMaterialInputsWithColumnNamedAlias()
    {

        final String PARENT_COLUMN = "P10";
        final String SAMPLE_SET_NAME = "SimpleSampleSet10";

        // Add the Alias column as a regression test.
        String sampleText = "Name\tAlias\t" + PARENT_COLUMN + "\n" +
                "SJ_01\n" +
                "SJ_02\t\tS_0\n" +
                "SJ_03\n" +
                "SJ_04\t\tS_1\n" +
                "SJ_05\t\tS_0\n";

        SampleSetDefinition definition = new SampleSetDefinition(SAMPLE_SET_NAME);
        definition.addParentAlias(PARENT_COLUMN, PARENT_CONTAINER_SAMPLE_SET_NAME);
        SampleSetAPIHelper.createEmptySampleSet(PROJECT_NAME + "/" + SUB_FOLDER_NAME, definition);
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        log("Import samples that have a parent alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.goToSampleSet(SAMPLE_SET_NAME).bulkImport(sampleText);

        log("Check sample 'SJ_05' and make sure the parent materials are correct.");
        waitAndClickAndWait(Locator.linkWithText("SJ_05"));
        checkAllRowsInDataRegion("parentMaterials", "Name", List.of("S_0"));
        checkAllRowsInDataRegion("parentMaterials", "Run", List.of(" "));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive 2 samples from S_0"));

    }

    @Test
    public void testDataInputsWithColumnNamedAlias()
    {
        final String PARENT_COLUMN_SUBFOLDER = "DC_Parent-Sub-Folder";
        final String PARENT_COLUMN_CONTAINER = "DC Parent Parent Folder";
        final String SAMPLE_SET_NAME = "SimpleSampleSet11";

        // Add the Alias column as a regression test.
        String sampleText = "Name\tAlias\t" + PARENT_COLUMN_SUBFOLDER + "\t" + PARENT_COLUMN_CONTAINER + "\n" +
                "SK_01\t\n" +
                "SK_02\t\t\tDC_1\n" +
                "SK_03\t\tDCSIB_1\n" +
                "SK_04\n" +
                "SK_05\t\tDCSIB_2\tDC_2\n";

        SampleSetDefinition definition = new SampleSetDefinition(SAMPLE_SET_NAME);
        definition.addDataParentAlias(PARENT_COLUMN_CONTAINER, PARENT_CONTAINER_DATA_CLASS_NAME);
        definition.addDataParentAlias(PARENT_COLUMN_SUBFOLDER, SIBLING_DATA_CLASS_NAME);
        SampleSetAPIHelper.createEmptySampleSet(PROJECT_NAME + "/" + SUB_FOLDER_NAME, definition);
        projectMenu().navigateToFolder(PROJECT_NAME, SUB_FOLDER_NAME);

        log("Import samples that have a parent alias column.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.goToSampleSet(SAMPLE_SET_NAME).bulkImport(sampleText);

        log("Check sample 'SK_02' and make sure the parent materials are correct. It's parent should be in from the Data Class in the parent container.");
        waitAndClickAndWait(Locator.linkWithText("SK_02"));
        checkAllRowsInDataRegion("parentData", "Name", List.of("DC_1"));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from DC_1"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SK_03' and make sure the parent materials are correct. It's parent should be from the Data Class in the same folder.");
        waitAndClickAndWait(Locator.linkWithText("SK_03"));
        checkAllRowsInDataRegion("parentData", "Name", List.of("DCSIB_1"));
        checkAllRowsInDataRegion("Runs", "Name", List.of("Derive sample from DCSIB_1"));

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        log("Check sample 'SK_05' which should have two parents, one from the Data Class in this folder and another in the Data Class in the parent container.");
        waitAndClickAndWait(Locator.linkWithText("SK_05"));

        checkAllRowsInDataRegion("parentData", "Name", List.of("DC_2", "DCSIB_2"));

        regexCheckRowInDataRegion("Runs", 0, "Name", "Derive sample from (?:DC_2, DCSIB_2|DCSIB_2, DC_2)");

    }

}