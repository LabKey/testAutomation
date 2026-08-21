/*
 * Copyright (c) 2016-2026 LabKey Corporation
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.ui.domainproperties.EntityTypeDesigner;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.experiment.CreateSampleTypePage;
import org.labkey.test.pages.experiment.UpdateSampleTypePage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.params.FieldInfo;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.AuditLogHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.OptionalFeatureHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.TextUtils;
import org.labkey.test.util.data.TestDataUtils;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.labkey.test.params.FieldDefinition.DOMAIN_TRICKY_CHARACTERS;
import static org.labkey.test.util.EscapeUtil.escapeForNameExpression;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class SampleTypeNameExpressionTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleType_Name_Expression_Test";

    // Issue 53548: Naming Pattern with a default value containing a () or {} cannot be saved.
    private static final String DEFAULT_SAMPLE_PARENT_VALUE = "SS" +
            EscapeUtil.escapeForNameExpression(TestDataGenerator.randomString(3, "{}()_"));

    private static final String PARENT_SAMPLE_TYPE = "PS" + DOMAIN_TRICKY_CHARACTERS;
    private static final String PARENT_SAMPLE_TYPE_INPUT = escapeForNameExpression(PARENT_SAMPLE_TYPE);

    // No random names for deriving sample type
    // Issue 53306: LabKey Server: Derive samples form does not distinguish between fields that differ by 'special characters'
    private static final FieldInfo COL_DSTR = new FieldInfo("DStr", ColumnType.String);
    private static final FieldInfo COL_DINT = new FieldInfo("DInt", ColumnType.Integer);

    private static final String PARENT_SAMPLE_01 = "parent01";
    private static final String PARENT_SAMPLE_02 = "parent02";
    private static final String PARENT_SAMPLE_03 = "#parent03";
    private static final String PARENT_SAMPLE_04 = "#parent04";
    private static final String PARENT_SAMPLE_05 = "\"parent05";
    private static final String PARENT_SAMPLE_06 = "parent,06";
    private static final String PARENT_SAMPLE_07 = "\"parent07";
    private static final String PARENT_FIELD_VALUE_PREFIX = "Tricky Parent";

    // Create fields that have each of the tricky characters for name expressions.
    // Having field names that differ only by special characters causes problems (Issue 53315)
    private static final FieldInfo PARENT_FIELD_BACKSLASH = new FieldInfo("Str A \\", ColumnType.String);
    private static final FieldInfo PARENT_FIELD_DOLLAR_DATE = new FieldInfo("Date B $", ColumnType.DateAndTime);
    private static final FieldInfo PARENT_FIELD_FORWARDSLASH = new FieldInfo("Str C /", ColumnType.String);
    private static final FieldInfo PARENT_FIELD_PERIOD = new FieldInfo("Str D .", ColumnType.String);
    private static final FieldInfo PARENT_FIELD_AMPERSAND = new FieldInfo("Str E &", ColumnType.String);
    private static final FieldInfo PARENT_FIELD_CURLY_LEFT = new FieldInfo("Str F {", ColumnType.String);
    private static final FieldInfo PARENT_FIELD_CURLY_RIGHT_INT = new FieldInfo("Int G }", ColumnType.Integer);
    private static final FieldInfo PARENT_FIELD_TILDE = new FieldInfo("Str H ~", ColumnType.String);
    private static final FieldInfo PARENT_FIELD_COMMA = new FieldInfo("Str I ,", ColumnType.String);

    private static final File PARENT_EXCEL = TestFileUtils.getSampleData("samples/ParentSamples.xlsx");

    protected final AuditLogHelper _auditLogHelper = new AuditLogHelper(this);

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        SampleTypeNameExpressionTest test = getCurrentTest();
        test.doSetup();
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        OptionalFeatureHelper.resetOptionalFeature(createDefaultConnection(), "deriveSamplesNotInApp");
    }

    private void addDataRow(TestDataGenerator dataGenerator, String name, int intVal)
    {
        Map<String, Object> sampleData = new HashMap<>();

        // Cannot use Map.of there are too many fields.
        sampleData.put("name", name);
        sampleData.put(PARENT_FIELD_BACKSLASH.getName(),
                String.format("%d %s 01", intVal, PARENT_FIELD_VALUE_PREFIX));
        sampleData.put(PARENT_FIELD_DOLLAR_DATE.getName(), intVal + "/14/2020");
        sampleData.put(PARENT_FIELD_FORWARDSLASH.getName(),
                String.format("%d %s 03", intVal, PARENT_FIELD_VALUE_PREFIX));
        sampleData.put(PARENT_FIELD_PERIOD.getName(),
                String.format("%d %s 04", intVal, PARENT_FIELD_VALUE_PREFIX));
        sampleData.put(PARENT_FIELD_AMPERSAND.getName(),
                String.format("%d %s 05", intVal, PARENT_FIELD_VALUE_PREFIX));
        sampleData.put(PARENT_FIELD_CURLY_LEFT.getName(),
                String.format("%d %s 06", intVal, PARENT_FIELD_VALUE_PREFIX));
        sampleData.put(PARENT_FIELD_CURLY_RIGHT_INT.getName(), intVal);
        sampleData.put(PARENT_FIELD_TILDE.getName(),
                String.format("%d %s 08", intVal, PARENT_FIELD_VALUE_PREFIX));
        sampleData.put(PARENT_FIELD_COMMA.getName(),
                String.format("%d %s 09", intVal, PARENT_FIELD_VALUE_PREFIX));

        dataGenerator.addCustomRow(sampleData);
    }

    private void doSetup() throws IOException, CommandException
    {
        _containerHelper.createProject(getProjectName(), null);
        new PortalHelper(this).addWebPart("Sample Types");

        goToProjectHome();

        log(String.format("Create a 'parent' sample type named '%s'.", PARENT_SAMPLE_TYPE));

        SampleTypeDefinition definition = new SampleTypeDefinition(PARENT_SAMPLE_TYPE);
        definition = definition.setFields(List.of(
                PARENT_FIELD_BACKSLASH.getFieldDefinition(),
                PARENT_FIELD_DOLLAR_DATE.getFieldDefinition(),
                PARENT_FIELD_FORWARDSLASH.getFieldDefinition(),
                PARENT_FIELD_PERIOD.getFieldDefinition(),
                PARENT_FIELD_AMPERSAND.getFieldDefinition(),
                PARENT_FIELD_CURLY_LEFT.getFieldDefinition(),
                PARENT_FIELD_CURLY_RIGHT_INT.getFieldDefinition(),
                PARENT_FIELD_TILDE.getFieldDefinition(),
                PARENT_FIELD_COMMA.getFieldDefinition()));

        TestDataGenerator dataGenerator = SampleTypeAPIHelper.createEmptySampleType(getCurrentContainerPath(), definition);

        log(String.format("Give the parent sample type '%1$s' three samples named '%2$s', '%3$s' and '%4$s'.",
                PARENT_SAMPLE_TYPE, PARENT_SAMPLE_01, PARENT_SAMPLE_02, PARENT_SAMPLE_03));

        addDataRow(dataGenerator, PARENT_SAMPLE_01, 1);
        addDataRow(dataGenerator, PARENT_SAMPLE_02, 2);
        addDataRow(dataGenerator, PARENT_SAMPLE_03, 3);
        addDataRow(dataGenerator, PARENT_SAMPLE_04, 4);
        addDataRow(dataGenerator, PARENT_SAMPLE_05, 5);
        addDataRow(dataGenerator, PARENT_SAMPLE_06, 6);
        addDataRow(dataGenerator, PARENT_SAMPLE_07, 7);

        dataGenerator.insertRows();

        // Just want to get an updated view of the sample types.
        refresh();
        OptionalFeatureHelper.setOptionalFeature(createDefaultConnection(), "deriveSamplesNotInApp", true);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testSimpleNameExpression()
    {
        FieldInfo colA = FieldInfo.random("A");
        FieldInfo colB = FieldInfo.random("B");
        FieldInfo colC = FieldInfo.random("C");
        String nameExpression = "${%s}-${%s}.${genId}.${batchRandomId}.${container}.${randomId}"
            .formatted(escapeForNameExpression(colA.getName()), escapeForNameExpression(colB.getName()));
        String data = TestDataUtils.stringFromRows(List.of(
            List.of(colA.getName(), colB.getName(), colC.getName()),
                List.of("a", "b", "c"),
                List.of("a", "b", "c"),
                List.of("a", "b", "c")));

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition("SimpleNameExprTest")
                        .setNameExpression(nameExpression)
                        .setFields(List.of(colA.getFieldDefinition(),
                            colB.getFieldDefinition(),
                            colC.getFieldDefinition())),
                data
                );

        // Verify SampleType details
        assertTextPresent(nameExpression);

        DataRegionTable materialTable = new DataRegionTable("Material", this);
        List<String> names = materialTable.getColumnDataAsText("Name");

        log("generated sample names:");
        names.forEach(this::log);

        assertEquals(3, names.size());

        String batchRandomId = names.get(0).split("\\.")[2];
        assertThat(names.get(0), startsWith("a-b.3." + batchRandomId + "." + PROJECT_NAME + "."));
        assertThat(names.get(1), startsWith("a-b.2." + batchRandomId + "." + PROJECT_NAME +"."));
        assertThat(names.get(2), startsWith("a-b.1." + batchRandomId + "." + PROJECT_NAME + "."));
    }


    // Issue 50924: LKSM: Importing samples using naming expression referencing parent inputs with # result in error
    @Test
    public void testDeriveFromCommentLikeParents()
    {
        final String sampleTypeName = "ParentNamesExprTest";

        log("Verify import tsv to create derivative would ignore lines starting with #");
        String nameExpression = "${MaterialInputs/" + PARENT_SAMPLE_TYPE_INPUT + "}-child";
        String data = "MaterialInputs/" + PARENT_SAMPLE_TYPE + "\n";
        data += PARENT_SAMPLE_01 + "\n";

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition(sampleTypeName)
                        .setNameExpression(nameExpression), data);

        data = "MaterialInputs/" + PARENT_SAMPLE_TYPE + "\n";
        data += PARENT_SAMPLE_01 + "," + PARENT_SAMPLE_02 + "," + PARENT_SAMPLE_03 + "\n";
        // tsv lines starting with # should be ignored
        data += PARENT_SAMPLE_03 + "\n";
        data += PARENT_SAMPLE_03 + "," + PARENT_SAMPLE_02 + "\n";
        sampleHelper.bulkImport(data);

        DataRegionTable materialTable = new DataRegionTable("Material", this);
        List<String> names = materialTable.getColumnDataAsText("Name");
        Collections.reverse(names);

        log("generated sample names:");
        names.forEach(this::log);

        assertEquals(2, names.size());

        List<String> expectedNames = new ArrayList<>();
        expectedNames.add(PARENT_SAMPLE_01 + "-child");
        expectedNames.add("[" + PARENT_SAMPLE_01 + ", " + PARENT_SAMPLE_02 + ", " + PARENT_SAMPLE_03 + "]-child");
        assertEquals("Sample names are not as expected", expectedNames, names);

        log("Verify import tsv should successfully create derivatives from line starting with #, as long as values are quoted");
        data = "MaterialInputs/" + EscapeUtil.fieldKeyEncodePart(PARENT_SAMPLE_TYPE) + "\n"; // fully encoded
        data += "\"" + PARENT_SAMPLE_03 + "\"\n";

        sampleHelper.bulkImport(data);

        names = materialTable.getColumnDataAsText("Name");
        Collections.reverse(names);

        log("generated sample names:");
        names.forEach(this::log);

        assertEquals(3, names.size());

        expectedNames.add(PARENT_SAMPLE_03 + "-child");
        assertEquals("Sample names are not as expected", expectedNames, names);

        log("Verify import tsv should successfully create derivatives from parent starting with #, as long as this is not the 1st field in the row");
        data = "Description\tMaterialInputs/" + EscapeUtil.fieldKeyEncodePart(PARENT_SAMPLE_TYPE) + "\n"; // fully encoded
        data += "Parents with leading # should work\t" + PARENT_SAMPLE_03 + "," + PARENT_SAMPLE_02 + "\n";

        sampleHelper.bulkImport(data);

        names = materialTable.getColumnDataAsText("Name");
        Collections.reverse(names);

        log("generated sample names:");
        names.forEach(this::log);

        assertEquals(4, names.size());

        expectedNames.add("[" + PARENT_SAMPLE_03 + ", " + PARENT_SAMPLE_02 + "]-child");
        assertEquals("Sample names are not as expected", expectedNames, names);

        log("Check that lineage is created as expected.");
        clickAndWait(Locator.linkWithText(PARENT_SAMPLE_03 + "-child"));
        checker().verifyTrue("Derivation run is not present.",
                isElementPresent(Locator.linkWithText("Derive sample from " + PARENT_SAMPLE_03)));
        clickAndWait(Locator.linkWithText(sampleTypeName));

        log("Verify import EXCEL should not ignore lines starting with #, and should work with sample containing double quotes");
        sampleHelper.bulkImport(PARENT_EXCEL);

        names = materialTable.getColumnDataAsText("Name");
        Collections.reverse(names);
        log("generated sample names:");
        names.forEach(this::log);

        assertEquals(10, names.size());
        expectedNames.add(PARENT_SAMPLE_04 + "-child");
        expectedNames.add("[" + PARENT_SAMPLE_04 + ", " + PARENT_SAMPLE_03 + "]-child");
        expectedNames.add("[" + PARENT_SAMPLE_01 + ", " + PARENT_SAMPLE_04 + "]-child");
        expectedNames.add(PARENT_SAMPLE_05 + "-child");
        expectedNames.add(PARENT_SAMPLE_06 + "-child");
        expectedNames.add("[" + PARENT_SAMPLE_07 + ", " + PARENT_SAMPLE_05 + "]-child");
        assertEquals("Sample names are not as expected", expectedNames, names);

        log("Verify importing tsv to create sample with # should work, as long as this is not the 1st field in the row");
        data = "Description\tName\n";
        data += "should succeed\t#RootSample1\n";
        sampleHelper.bulkImport(data);
        names = materialTable.getColumnDataAsText("Name");
        Collections.reverse(names);
        assertEquals(11, names.size());
        expectedNames.add("#RootSample1");

        log("Verify importing tsv to create sample should ignore lines starting with #");
        data = "Name\tDescription\n";
        data += "#RootSample2\tshould be ignored\n";
        sampleHelper.startTsvImport(data, "IMPORT").submitExpectingErrorContaining("No rows were inserted. Please check to make sure your data is formatted properly.");
    }

    // Coverage for Issue 47504
    /**
     * Validate that a name expression works correctly with commas and other 'tricky' characters.
     */
    @Test
    public void testWithTrickyCharacters()
    {

        goToProjectHome();

        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);

        final String sampleTypeName = TestDataGenerator.randomDomainName("Tricky_");

        CreateSampleTypePage createPage = sampleTypeHelper.goToCreateNewSampleType();

        createPage.setName(sampleTypeName);

        String tricky01 = "S\u00f8\u03bc\u2211,String,,,";
        String tricky02 = "NE\u222b,";
        String tricky03 = "@-\\*-";
        String tricky04 = "+{My'Text}=%#$"; // These curly braces cause the warning when saving.
        String tricky05 = String.format("@\"%s", tricky01);
        String dateFormat = "yy-MM-dd";
        int counterStart = 500;

        //Søµ∑,String,,,${NE∫,:withCounter(500)}@-\\*-${genId:number('000,000')}+{My'Text}=%#$${now:date('yy-MM-dd')}@\"Søµ∑,String,,,
        String nameExpression = String.format("%s${%s:withCounter(%d)}%s${genId:number('000,000')}%s${now:date('%s')}%s",
                tricky01, tricky02, counterStart, tricky03, tricky04, dateFormat, tricky05);

        log(String.format("Use name expression: '%s'.", nameExpression));

        createPage.setNameExpression(nameExpression);

        log("Click the 'Save' button and wait for the warning dialog.");
        Locator.button("Save").findElement(getDriver()).click();

        ModalDialog dialog = null;

        try
        {
            dialog = new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Naming Pattern Warning(s)").waitFor();
        }
        catch (TimeoutException toe)
        {
            checker().withScreenshot("No_Warning_Dialog")
                    .error("The expected warning dialog for this name expression did not show up.");
        }

        if(null != dialog)
        {
            log("Verify that you can save with the warning.");
            dialog.dismiss("Save anyway", 2_500);
        }

        log("Validate save worked. Should be sent back to the project begin page and there should be a link with the sample type name.");

        checker().fatal()
                .verifyTrue(String.format("Did not find link with text '%s' for sample type. Fatal error.", sampleTypeName),
                        waitFor(()->Locator.linkWithText(sampleTypeName).findWhenNeeded(getDriver()).isDisplayed(), 5_000));

        log("Create some samples and validate the name(s).");

        int samplesCount = 4;
        String pasteData = """
                Description
                A
                B
                C
                D""";

        clickAndWait(Locator.linkWithText(sampleTypeName));
        var dataRegion = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        var importDataPage = dataRegion.clickImportBulkData();
        importDataPage.setCopyPasteInsertOption(false);
        importDataPage.selectCopyPaste()
                .setFormat(ImportDataPage.Format.TSV)
                .setText(pasteData)
                .submit();

        // Get the date as soon after sample creation as possible.
        LocalDateTime ldt = LocalDateTime.now();
        String date = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH).format(ldt);

        var sampleTypeGrid = new DataRegionTable.DataRegionFinder(getDriver())
                .withName("Material").waitFor();

        checker().verifyEquals("Number of samples created not as expected.",
                samplesCount, sampleTypeGrid.getDataRowCount());

        // Only going to validate the name of the last sample created. It's counter and GenID values, in combination
        // with the count of samples created, should be enough.
        String expectedName = String.format("%s%s%d%s000,%03d%s%s%s",
                tricky01, tricky02, counterStart + (samplesCount - 1), tricky03, samplesCount, tricky04, date, tricky05);
        String actualName = sampleTypeGrid.getDataAsText(0, "Name");

        checker().verifyEquals("Sample name not as expected.", expectedName, actualName);

        checker().screenShotIfNewError("SampleCreationError");
    }

    /**
     * <p>
     *     Automation to cover issue 52180. The name expression for the sample type will reference fields that contain
     *     "special" characters.
     * </p>
     */
    @Test
    public void testWithTrickyFieldNames() throws IOException, CommandException
    {
        goToProjectHome();

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        final String sampleType = "Issue 52180 Sample Type";

        StringBuilder sbNameExpression = new StringBuilder();

        // Covers Issue 52180
        sbNameExpression.append("Trick-Field ${genId} ");
        sbNameExpression.append(String.format("${materialInputs/%s/%s} ", PARENT_SAMPLE_TYPE_INPUT, PARENT_FIELD_BACKSLASH.getExpName()));
        sbNameExpression.append(String.format("${materialInputs/%s/%s} ", PARENT_SAMPLE_TYPE_INPUT, PARENT_FIELD_DOLLAR_DATE.getExpName()));
        sbNameExpression.append(String.format("${materialInputs/%s/%s} ", PARENT_SAMPLE_TYPE_INPUT, PARENT_FIELD_FORWARDSLASH.getExpName()));
        sbNameExpression.append(String.format("${materialInputs/%s/%s} ", PARENT_SAMPLE_TYPE_INPUT, PARENT_FIELD_PERIOD.getExpName()));
        sbNameExpression.append(String.format("${materialInputs/%s/%s} ", PARENT_SAMPLE_TYPE_INPUT, PARENT_FIELD_AMPERSAND.getExpName()));
        sbNameExpression.append(String.format("${materialInputs/%s/%s} ", PARENT_SAMPLE_TYPE_INPUT, PARENT_FIELD_CURLY_LEFT.getExpName()));
        sbNameExpression.append(String.format("${materialInputs/%s/%s} ", PARENT_SAMPLE_TYPE_INPUT, PARENT_FIELD_CURLY_RIGHT_INT.getExpName()));
        sbNameExpression.append(String.format("${materialInputs/%s/%s} ", PARENT_SAMPLE_TYPE_INPUT, PARENT_FIELD_TILDE.getExpName()));
        sbNameExpression.append(String.format("${materialInputs/%s/%s}", PARENT_SAMPLE_TYPE_INPUT, PARENT_FIELD_COMMA.getExpName()));

        log(String.format("Create a sample type named '%s' with a name expression of '%s'.", sampleType, sbNameExpression));

        CreateSampleTypePage createPage = sampleHelper.goToCreateNewSampleType();

        createPage.setName(sampleType);

        createPage.setNameExpression(sbNameExpression.toString());

        // Issue 53306 There is a problem with the derived sample form and field names that contain "special" characters.
//        String intField = TestDataGenerator.randomFieldName("Int");
//        String strField = TestDataGenerator.randomFieldName("Str");
        String intField = "Int";
        String strField = "Str";
        createPage.addFields(Arrays.asList(
                new FieldDefinition(intField, FieldDefinition.ColumnType.Integer),
                new FieldDefinition(strField, FieldDefinition.ColumnType.String)));

        createPage.clickSave();

        String flagString = "Hello, I'm a derived sample.";
        String intVal = "678";
        String derivedSampleName = deriveSample(PARENT_SAMPLE_01, PARENT_SAMPLE_TYPE, sampleType,
                Map.of(intField, intVal,
                        strField, flagString));

        checker().verifyTrue("Name of derived sample doesn't look correct. Should start with 'Trick-Field '.",
                derivedSampleName.startsWith("Trick-Field "));
        checker().verifyTrue(String.format("Doesn't look like there is a link to the parent sample '%s'.", PARENT_SAMPLE_01),
                isElementPresent(Locator.linkWithText(PARENT_SAMPLE_01)));

    }

    // Field used in the testInputsExpression test.
    private static final String SHARED_FIELD_NAME = "FieldB";

    @Test
    public void testInputsExpression() throws IOException, CommandException
    {

        verifyNames(
                "InputsExpressionTest",
                "Name\t" + SHARED_FIELD_NAME + "\tMaterialInputs/InputsExpressionTest",
                "${Inputs:first:defaultValue('" + DEFAULT_SAMPLE_PARENT_VALUE + "')}_${batchRandomId}",
                null, "Pat");

        final String urlLikeDefaultVal = "a+b%c#d";
        verifyNames(
                "InputsExpressionTest2",
                "Name\t" + SHARED_FIELD_NAME + "\tMaterialInputs/InputsExpressionTest2",
                "${Inputs:first:defaultValue('" + urlLikeDefaultVal + "')}_${batchRandomId}",
                null, "Fed", true, urlLikeDefaultVal);

        verifyNames(
                "Inputs/ExpressionTest2",
                "Name\t" + SHARED_FIELD_NAME + "\tMaterialInputs/Inputs$SExpressionTest2",
                "${Inputs:defaultValue('" + DEFAULT_SAMPLE_PARENT_VALUE + "')}_${batchRandomId}",
                null, "Bat", false);

        verifyNames(
                "Inputs/WithDataTypeExpression",
                "Name\t" + SHARED_FIELD_NAME + "\tMaterialInputs/Inputs/WithDataTypeExpression",
                "${Inputs/Inputs$SWithDataTypeExpression:first:defaultValue('" + DEFAULT_SAMPLE_PARENT_VALUE + "')}_${batchRandomId}",
                null, "Red");

        verifyNames(
                "Inputs/WithDataTypeExpression2",
                "Name\t" + SHARED_FIELD_NAME + "\tMaterialInputs/Inputs$SWithDataTypeExpression2",
                "${Inputs/Inputs$SWithDataTypeExpression2:defaultValue('" + DEFAULT_SAMPLE_PARENT_VALUE + "')}_${batchRandomId}",
                null, "Ted", false);

        verifyNames(
                "Material/WithDataTypeExpression",
                "Name\t" + SHARED_FIELD_NAME + "\tMaterialInputs/Material/WithDataTypeExpression",
                "${MaterialInputs/Material$SWithDataTypeExpression:first:defaultValue('" + DEFAULT_SAMPLE_PARENT_VALUE + "')}_${batchRandomId}",
                null, "Ned");

        verifyNames(
                "Material/WithDataTypeExpression2",
                "Name\t" + SHARED_FIELD_NAME + "\tMaterialInputs/Material$SWithDataTypeExpression2",
                "${MaterialInputs/Material$SWithDataTypeExpression2:defaultValue('" + DEFAULT_SAMPLE_PARENT_VALUE + "')}_${batchRandomId}",
                null, "Med", false);

        // Test that a name expression using a generic "Inputs" and a field name can resolve the field name in Material and Data classes.
        log("Test a name expression that uses \"Inputs/SomeFieldName\".");

        final String dataParentClass = "InputsDataParentFieldExpression";
        log(String.format("Create a DataClass, %s to use as a parent.", dataParentClass));
        DataClassDefinition dpDef = new DataClassDefinition(dataParentClass);
        dpDef.setFields(List.of(new FieldDefinition(SHARED_FIELD_NAME, ColumnType.String)));
        TestDataGenerator dpGen = dpDef.create(createDefaultConnection(), getProjectName());
        dpGen.addCustomRow(Map.of("Name", "DS-1",
                SHARED_FIELD_NAME, "ABC"));
        dpGen.addCustomRow(Map.of("Name", "DS-2",
                SHARED_FIELD_NAME, "DEF"));
        dpGen.insertRows(createDefaultConnection(), dpGen.getRows());

        final String nameExpression = "Gen-${Inputs/" + SHARED_FIELD_NAME + ":defaultValue('none')}-${genId}";

        StringBuilder data = new StringBuilder();
        data.append(SHARED_FIELD_NAME);
        data.append("\tDataInputs/");
        data.append(dataParentClass);
        data.append("\n");
        data.append("a\tDS-1\n");
        data.append("b\tDS-2\n");
        data.append("c\tDS-1, DS-2\n");
        data.append("d\n");

        goToProjectHome();
        final String sampleTypeName = "InputsExpressionTestData";
        log(String.format("Create a SampleType, %s, with a name expression of %s. Test name expression using a DataClass, %s as a parent.",
                sampleTypeName, nameExpression, dataParentClass));
        createSampleTypeForNameValidation(sampleTypeName,
                nameExpression, null, data.toString());

        DataRegionTable materialTable = new DataRegionTable("Material", this);
        List<String> actualNames = materialTable.getColumnDataAsText("Name");

        List<String> dataSourceParentNames = List.of(
                Pattern.quote("Gen-none-") + ".*",
                Pattern.quote("Gen-[ABC, DEF]-") + ".*",
                Pattern.quote("Gen-DEF-") + ".*",
                Pattern.quote("Gen-ABC-") + ".*"
                );
        validateNamesGenerated(actualNames, dataSourceParentNames);

        data = new StringBuilder();
        data.append("Name\t");
        data.append(SHARED_FIELD_NAME);
        data.append("\n");
        data.append("S-1\tUVW\n");
        data.append("S-2\tXYZ\n");

        goToProjectHome();
        final String parentSampleType = "InputsMaterialParentFieldExpression";
        log(String.format("Create a SampleType, %s to use as a parent.", parentSampleType));
        createSampleTypeForNameValidation(parentSampleType,
                "S-${genId}", null, data.toString());

        data = new StringBuilder();
        data.append(SHARED_FIELD_NAME);
        data.append("\tMaterialInputs/");
        data.append(parentSampleType);
        data.append("\n");
        data.append("i\tS-1\n");
        data.append("j\tS-2\n");
        data.append("k\tS-1, S-2\n");
        data.append("l\n");

        log(String.format("Again using SampleType, %s, test name expression using a SampleType, %s as a parent.",
                sampleTypeName, parentSampleType));
        goToProjectHome();
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.goToSampleType(sampleTypeName).bulkImport(data.toString());

        List<String> sampleTypeParentNames = new ArrayList<>();
        sampleTypeParentNames.add(Pattern.quote("Gen-none-") + ".*");
        sampleTypeParentNames.add(Pattern.quote("Gen-[UVW, XYZ]-") + ".*");
        sampleTypeParentNames.add(Pattern.quote("Gen-XYZ-") + ".*");
        sampleTypeParentNames.add(Pattern.quote("Gen-UVW-") + ".*");
        sampleTypeParentNames.addAll(dataSourceParentNames);

        materialTable = new DataRegionTable("Material", this);
        actualNames = materialTable.getColumnDataAsText("Name");

        validateNamesGenerated(actualNames, sampleTypeParentNames);

        data = new StringBuilder();
        data.append(SHARED_FIELD_NAME);
        data.append("\tMaterialInputs/");
        data.append(parentSampleType);
        data.append("\tDataInputs/");
        data.append(dataParentClass);
        data.append("\n");
        data.append("m\tS-1\n");
        data.append("n\t\tDS-1\n");
        data.append("o\tS-2\tDS-2\n");
        data.append("p\tS-1, S-2\tDS-1\n");
        data.append("p\tS-1\tDS-1, DS-2\n");
        data.append("q\tS-1, S-2\tDS-1, DS-2\n");
        data.append("r\n");

        log(String.format("Finally using SampleType, %s, test name expression using both a SampleType and a DataClass as a parent.",
                sampleTypeName));
        goToProjectHome();
        sampleHelper = new SampleTypeHelper(this);
        sampleHelper.goToSampleType(sampleTypeName).bulkImport(data.toString());

        List<String> combinedParentNames = new ArrayList<>();
        combinedParentNames.add(Pattern.quote("Gen-none-") + ".*");
        combinedParentNames.add(Pattern.quote("Gen-[ABC, DEF, UVW, XYZ]-") + ".*");
        combinedParentNames.add(Pattern.quote("Gen-[ABC, DEF, UVW]-") + ".*");
        combinedParentNames.add(Pattern.quote("Gen-[ABC, UVW, XYZ]-") + ".*");
        combinedParentNames.add(Pattern.quote("Gen-[DEF, XYZ]-") + ".*");
        combinedParentNames.add(Pattern.quote("Gen-ABC-") + ".*");
        combinedParentNames.add(Pattern.quote("Gen-UVW-") + ".*");
        combinedParentNames.addAll(sampleTypeParentNames);

        materialTable = new DataRegionTable("Material", this);
        actualNames = materialTable.getColumnDataAsText("Name");

        validateNamesGenerated(actualNames, combinedParentNames);
    }

    @Test
    public void testParentAliasExpression()
    {
        verifyNames(
                "ParentAliasInputsExpressionTest",
                "Name\tFieldB\tParentAlias",
                "${ParentAlias:first:defaultValue('" + DEFAULT_SAMPLE_PARENT_VALUE + "')}_${batchRandomId}",
                "ParentAlias", "Jessi");
    }

    // Issue 42857: samples: bulk import with name expression containing lookup fails to convert lookup string value
    @Test
    public void testLookupNameExpression() throws Exception
    {
        String lookupList = "Colors";
        FieldDefinition.LookupInfo colorsLookup = new FieldDefinition.IntLookup(getProjectName(), "lists", lookupList);
        String nameExpSamples = "NameExpressionSamples";

        // begin by creating a lookupList of colors, the sampleType will reference it
        TestDataGenerator colorsGen = new TestDataGenerator("lists", lookupList, getProjectName())
                .withColumns(List.of(new FieldDefinition("ColorName", FieldDefinition.ColumnType.String),
                        new FieldDefinition("ColorCode", FieldDefinition.ColumnType.String)));
        colorsGen.addCustomRow(Map.of("ColorName", "green", "ColorCode", "gr"));
        colorsGen.addCustomRow(Map.of("ColorName", "yellow", "ColorCode", "yl"));
        colorsGen.addCustomRow(Map.of("ColorName", "red", "ColorCode", "rd"));
        colorsGen.addCustomRow(Map.of("ColorName", "blue", "ColorCode", "bl"));
        colorsGen.createList(createDefaultConnection(), "Key");
        colorsGen.insertRows();

        String pasteData = """
                ColorLookup\tNoun
                red\tryder
                green\tgiant
                blue\tangel
                yellow\tjersey""";

        // now create a sampleType with a Color column that looks up to Colors
        var sampleTypeDef = new SampleTypeDefinition(nameExpSamples)
                .setFields(List.of(new FieldDefinition("ColorLookup", colorsLookup),
                        new FieldDefinition("Noun", ColumnType.String)))
                .setNameExpression("TEST-${ColorLookup/ColorCode}");   // hopefully this will resolve the 'ColorCode' column from the list
        SampleTypeAPIHelper.createEmptySampleType(getProjectName(), sampleTypeDef);

        SampleTypeHelper.beginAtSampleTypesList(this, getProjectName());
        clickAndWait(Locator.linkWithText(nameExpSamples));
        var dataRegion = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        var importDataPage = dataRegion.clickImportBulkData();
        importDataPage.selectCopyPaste()
                .setImportLookupByAlternateKey(true)
                .setFormat(ImportDataPage.Format.TSV)
                .setText(pasteData)
                .submit();

        var sampleTypeGrid = new DataRegionTable.DataRegionFinder(getDriver())
                .withName("Material").waitFor();
        assertThat("Expect lookup to force name-generation that resolves colorCodes from the colorLookup",
                sampleTypeGrid.getColumnDataAsText("Name"), hasItems("TEST-yl", "TEST-bl", "TEST-gr", "TEST-rd"));
    }

    @Test
    public void testMaterialInputsExpressionWithParentAliasData()
    {
        verifyNames(
                "MaterialInputsExpressionWithParentAliasData",
                "Name\tFieldB\tParentAlias",
                "${MaterialInputs:first:defaultValue('" + DEFAULT_SAMPLE_PARENT_VALUE + "')}_${batchRandomId}",
                "ParentAlias", "Sam");
    }

    private void verifyNames(String sampleTypeName, String header, String nameExpression, @Nullable String currentTypeAlias, String namePrefix)
    {
        verifyNames(sampleTypeName, header, nameExpression, currentTypeAlias, namePrefix, true);
    }

    private void verifyNames(String sampleTypeName, String header, String nameExpression, @Nullable String currentTypeAlias, String namePrefix, boolean useFirst)
    {
        verifyNames(sampleTypeName, header, nameExpression, currentTypeAlias, namePrefix, useFirst, DEFAULT_SAMPLE_PARENT_VALUE);
    }

    private void verifyNames(String sampleTypeName, String header, String nameExpression, @Nullable String currentTypeAlias, String namePrefix, boolean useFirst, String defaultValue)
    {
        goToProjectHome();

        String name1 = namePrefix + "_1";
        String name2 = namePrefix + "_2";
        String data = header + "\n" +

                // Name provided
                name1 + "\tb\t\n" +
                name2 + "\tb\t\n" +

                // Name generated, uses first input "Bob" if useFirst or [name1, name2] if not useFirst
                "\tb\t" + name1 + "," + name2 + "\n" +

                // Name generated: should be name2 without [] regardless of useFirst
                "\tb\t" + name2 + "\n" +

                // Name generated and uses defaultValue('SS')
                "\tb\t\n";

        createSampleTypeForNameValidation(sampleTypeName, nameExpression, currentTypeAlias, data);

        DataRegionTable materialTable = new DataRegionTable("Material", this);
        List<String> actualNames = materialTable.getColumnDataAsText("Name");

        List<String> expectedNames = new ArrayList<>();

        expectedNames.add(Pattern.quote(defaultValue + "_") + ".*");

        String batchRandomId = actualNames.getFirst().substring(actualNames.getFirst().lastIndexOf("_") + 1);

        expectedNames.add(Pattern.quote(name2 + "_" + batchRandomId));

        String pattern = useFirst ? name1 : "[" + name1 + ", " + name2 + "]";
        pattern = pattern + "_" + batchRandomId;
        expectedNames.add(Pattern.quote(pattern));

        expectedNames.add(Pattern.quote(name2));

        expectedNames.add(Pattern.quote(name1));

        validateNamesGenerated(actualNames, expectedNames);
    }

    private void createSampleTypeForNameValidation(String sampleTypeName, String nameExpression, @Nullable String currentTypeAlias, String data)
    {
        log(String.format("Create a sample type %s with a name expression of %s and populate it using the name expression.",
                sampleTypeName, nameExpression));

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        SampleTypeDefinition definition = new SampleTypeDefinition(sampleTypeName)
                .setNameExpression(nameExpression);
        if (currentTypeAlias != null)
            definition = definition.setParentAliases(Map.of(currentTypeAlias, "(Current Sample Type)"));
        definition = definition.setFields(List.of(new FieldDefinition(SHARED_FIELD_NAME, ColumnType.String)));
        sampleHelper.createSampleType(definition, data);

        assertTextPresent(nameExpression);

    }

    /**
     * <p>
     *     Validate that every name in {@code actualNames} matches one of the regex patterns in
     *     {@code expectedNames}, using a multiset diff: every expected pattern must match at least
     *     one actual name, every actual name must be matched by some expected pattern, and the
     *     counts must be equal. Order is intentionally NOT verified.
     * </p>
     * <p>
     *     On a mismatch the failure reports both the patterns that found no match and the actual
     *     names that found no pattern, so a "missing row" or "extra row" failure is immediately
     *     diagnosable instead of just a "wrong count" error.
     * </p>
     * <p>
     *     This method was generated by Claude.
     * </p>
     */
    private void validateNamesGenerated(List<String> actualNames, List<String> expectedNames)
    {
        checker().verifyEquals("Number of samples not as expected.",
                expectedNames.size(), actualNames.size());

        List<String> unmatchedActuals = new ArrayList<>(actualNames);
        List<String> unmatchedPatterns = new ArrayList<>();
        for (String pattern : expectedNames)
        {
            Optional<String> match = unmatchedActuals.stream()
                    .filter(a -> Pattern.matches(pattern, a))
                    .findFirst();
            if (match.isPresent())
                unmatchedActuals.remove(match.get());
            else
                unmatchedPatterns.add(pattern);
        }

        checker().verifyTrue("Expected patterns with no matching actual name: " + unmatchedPatterns,
                unmatchedPatterns.isEmpty());
        checker().verifyTrue("Actual names not matching any expected pattern (extras): " + unmatchedActuals,
                unmatchedActuals.isEmpty());

        checker().screenShotIfNewError("Generated_Names_Error");
    }

    /**
     * <p>
     *     Verify that a derived sample, using a name expression, can be created by clicking the "Derive Samples" link
     *     from a sample's detail page.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Create a derived sample using the UI and the name expression that reference parent property to name it.</li>
     *         <li>Create a derived sample using the UI and the name expression that references grandparent property to name it.</li>
     *         <li>Create a derived sample using the bulk import and the name expression that references grandparent property to name it.</li>
     *     </ul>
     * </p>
     * @throws Exception Can be thrown by test helper.
     */
    @Test
    public void testDeriveSampleFromSampleDetailsPage() throws Exception
    {

        goToProjectHome();

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        final String sampleType = "DerivedUI_SampleType";
        // Covers Issue 44760
        final String nameExpression = String.format("DUI_${genId}_${materialInputs/%s/%s}", PARENT_SAMPLE_TYPE_INPUT, PARENT_FIELD_CURLY_LEFT.getExpName());

        log(String.format("Create a sample type named '%s' with a name expression of '%s'.", sampleType, nameExpression));

        CreateSampleTypePage createPage = sampleHelper.goToCreateNewSampleType();

        createPage.setName(sampleType);

        createPage.setNameExpression(nameExpression);

        createPage.addFields(Arrays.asList(
                COL_DINT.getFieldDefinition(),
                COL_DSTR.getFieldDefinition()));

        createPage.clickSave();

        String flagString = "Hello, I'm a derived sample.";
        String intVal = "987";
        String derivedSampleName = deriveSample(PARENT_SAMPLE_01, PARENT_SAMPLE_TYPE, sampleType,
                Map.of(COL_DSTR.getName(), flagString,
                        COL_DINT.getName(), intVal));

        checker().verifyTrue(String.format("Name of derived sample doesn't look correct. Should contain %s.", PARENT_FIELD_VALUE_PREFIX),
                derivedSampleName.contains(PARENT_FIELD_VALUE_PREFIX));
        checker().verifyTrue(String.format("Doesn't look like there is a link to the parent sample '%s'.", PARENT_SAMPLE_01),
                isElementPresent(Locator.linkWithText(PARENT_SAMPLE_01)));

        // Covers Issue 44760
        final String ancestorNameExpression = String.format("GrandChild_${MaterialInputs/%s/..[MaterialInputs/%s]/%s}_${genId}", sampleType, PARENT_SAMPLE_TYPE_INPUT, PARENT_FIELD_CURLY_LEFT.getExpName());
        log("Change the sample type name expression to support grandparent property lookup: " + ancestorNameExpression);
        goToProjectHome();
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);
        sampleTypeHelper.goToSampleType(sampleType);
        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        UpdateSampleTypePage updatePage = new UpdateSampleTypePage(getDriver());
        updatePage.setNameExpression(ancestorNameExpression);
        updatePage.clickSave();

        String flagStringGD = "grand child sample.";
        String intValGD = "567";
        String grandChildSampleName = deriveSample(derivedSampleName, sampleType, sampleType,
                Map.of(COL_DSTR.getName(), flagStringGD,
                        COL_DINT.getName(), intValGD));
        checker().verifyTrue(String.format("Name of derived sample doesn't look correct. Should contain %s and not contain '%s'.", PARENT_FIELD_VALUE_PREFIX, flagString),
                grandChildSampleName.contains(PARENT_FIELD_VALUE_PREFIX) && !grandChildSampleName.contains(flagString));
        checker().verifyTrue(String.format("Doesn't look like there is a link to the parent sample '%s'.", derivedSampleName),
                isElementPresent(Locator.linkWithText(derivedSampleName)));

        String flagStringBulkImport = "bulk imported grand child.";
        log("Derive a sample using bulk import but give it no name. The name expression should be used to name the derived sample.");
        String importData = TestDataUtils.stringFromRows(List.of(
            List.of("MaterialInputs/%s".formatted(sampleType), COL_DSTR.getName()),
            List.of(derivedSampleName, flagStringBulkImport)));
        sampleHelper.goToSampleType(sampleType);
        sampleHelper.getSamplesDataRegionTable()
                .clickImportBulkData()
                .setText(importData)
                .submit();

        waitForElement(Locator.tagWithText("td", flagStringBulkImport));

        DataRegionTable table = sampleHelper.getSamplesDataRegionTable();
        int newSampleRowInd = table.getRowIndex(COL_DSTR.getName(), flagStringBulkImport);
        String grandImportChildSampleName = table.getDataAsText(newSampleRowInd, "Name");
        click(Locator.tagWithText("td", grandImportChildSampleName));
        waitForElement(Locator.tagWithText("td", flagStringBulkImport));

        checker().verifyTrue(String.format("Name of derived sample doesn't look correct. Should contain %s and not contain '%s'.", PARENT_FIELD_VALUE_PREFIX, flagString),
                grandChildSampleName.contains(PARENT_FIELD_VALUE_PREFIX) && !grandChildSampleName.contains(flagString));
        checker().verifyTrue(String.format("Doesn't look like there is a link to the parent sample '%s'.", derivedSampleName),
                isElementPresent(Locator.linkWithText(derivedSampleName)));
    }

    private String deriveSample(String parentSampleName, String parentSampleType, String targetSampleType, Map<String, String> setField) throws IOException, CommandException
    {
        log(String.format("Go to the 'overview' page for sample '%s' in sample type '%s'", parentSampleName, parentSampleType));
        Integer sampleRowNum = SampleTypeAPIHelper.getRowIdsForSamples(getProjectName(), parentSampleType, Arrays.asList(parentSampleName)).get(parentSampleName);

        String url = WebTestHelper.buildRelativeUrl("experiment", getCurrentContainerPath(), "showMaterial", Map.of("rowId", sampleRowNum));
        beginAt(url);

        log("Derive a sample from this sample but give it no name. The name expression should be used to name the derived sample.");

        waitForElement(Locator.linkWithText("derive samples from this sample"));

        clickAndWait(Locator.linkWithText("derive samples from this sample"));

        selectOptionByText(Locator.name("targetSampleTypeId"),  String.format("%s in /%s", targetSampleType, getProjectName()));
        clickButton("Next");

        String flagString = "";
        for(Map.Entry<String, String> entry : setField.entrySet())
        {
            setFormElement(Locator.name(String.format("Output Sample 1_%s", entry.getKey())), entry.getValue());
            flagString = entry.getValue();
        }
        clickButton("Submit");

        waitForElement(Locator.tagWithText("td", flagString));

        return Locator.tagWithText("td", "Name:").followingSibling("td").findElement(getDriver()).getText();
    }

    /**
     * Simple helper to build the expected text in the tool-tip for the name expression.
     *
     * @param expectedPreview What the expected preview example should look like. If null will return default tool-tip.
     * @return The expected text in the tool-tip including header and other text.
     */
    private String generateExpectedToolTip(@Nullable String expectedPreview)
    {

        StringBuilder expectedToolTip = new StringBuilder();

        expectedToolTip.append("Naming Pattern\n");
        expectedToolTip.append("Pattern used for generating unique IDs for this sample type.\n");

        if(expectedPreview != null)
        {
            expectedToolTip.append("Example of name that will be generated from the current pattern: ");
            expectedToolTip.append(TextUtils.normalizeSpace(expectedPreview));
            expectedToolTip.append("\n");
        }

        expectedToolTip.append("More info");

        return expectedToolTip.toString();
    }

    /**
     * <p>
     *     Test the preview/tool-tip for a name expression.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Validate the default tool-tip (no name expression present).</li>
     *         <li>With a name expression that uses fields from a parent samples.</li>
     *         <li>With a name expression that has a formatted date.</li>
     *         <li>Validate the tool-tip on the create sample page</li>
     *     </ul>
     * </p>
     * @throws IOException Can be thrown by the test helper.
     */
    @Test
    public void testNameExpressionPreview() throws IOException
    {

        goToProjectHome();

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        final String sampleType = "Preview_SampleType";

        CreateSampleTypePage createPage = sampleHelper.goToCreateNewSampleType();

        createPage.setName(sampleType);

        log("Check the default tool-tip.");
        String expectedMsg = generateExpectedToolTip(null);
        String actualMsg = createPage.getNameExpressionPreview();

        checker().withScreenshot("Default_Preview_Error")
                .verifyEquals("Default tool-tip message not as expected.", expectedMsg, actualMsg);

        // Make the tooltip go away.
        mouseOver(createPage.getComponentElement());

        log("Set a named parent.");

        final String parentAlias = "My_Parent";

        createPage.addParentAlias(parentAlias, String.format("Sample Type: %1$s (%2$s)", PARENT_SAMPLE_TYPE, PROJECT_NAME));

        log("Use a name expression using a field from the named parent, with parent type not encoded.");
        String nameExpressionBad = String.format("SNP_${genId}_${%s/%s}_${materialInputs/%s/%s}", parentAlias, PARENT_FIELD_CURLY_RIGHT_INT.getExpName(), PARENT_SAMPLE_TYPE, PARENT_FIELD_CURLY_LEFT.getExpName());
        createPage.setNameExpression(nameExpressionBad);
        actualMsg = createPage.getNameExpressionPreview();
        checker().withScreenshot("Parent_Fields_Preview_Error")
                .verifyTrue("Tool-tip message does not contain expected example.", actualMsg.contains("Unable to generate example name from the current pattern. Check for syntax errors."));
        // Make the tooltip go away.
        mouseOver(createPage.getComponentElement());

        log("Use a name expression using a field from the named parent, with parent type not encoded.");
        nameExpressionBad = String.format("SNP_${genId}_${%s/%s}_${materialInputs/%s/%s}", parentAlias, PARENT_FIELD_CURLY_RIGHT_INT.getExpName(), PARENT_SAMPLE_TYPE, PARENT_FIELD_CURLY_LEFT.getName());
        createPage.setNameExpression(nameExpressionBad);
        actualMsg = createPage.getNameExpressionPreview();
        checker().withScreenshot("Parent_Fields_Preview_Error")
            .verifyTrue("Tool-tip message does not contain expected example.", actualMsg.contains("Unable to generate example name from the current pattern. Check for syntax errors."));
        // Make the tooltip go away.
        mouseOver(createPage.getComponentElement());

        log("Use a name expression using a field from the named parent, with parent type encoded correctly.");
        String nameExpression = String.format("SNP_${genId}_${%s/%s}_${materialInputs/%s/%s}", parentAlias, PARENT_FIELD_CURLY_RIGHT_INT.getExpName(), PARENT_SAMPLE_TYPE_INPUT, PARENT_FIELD_CURLY_LEFT.getExpName());
        createPage.setNameExpression(nameExpression);
        expectedMsg = generateExpectedToolTip("SNP_1001_3_parent%sValue".formatted(PARENT_FIELD_CURLY_LEFT.getName()));
        actualMsg = createPage.getNameExpressionPreview();
        log("Verify that the preview shows the fields as expected.");
        checker().withScreenshot("Parent_Fields_Preview_Error")
                .verifyEquals("Tool-tip message does not contain expected example.", expectedMsg, actualMsg);

        // Make the tooltip go away.
        mouseOver(createPage.getComponentElement());

        log("Use a name expression with a formatted date.");

        nameExpression = String.format("SNP_${genId}_${%s/%s:date('yyyy-MM-dd')}", parentAlias, PARENT_FIELD_DOLLAR_DATE.getExpName());

        createPage.setNameExpression(nameExpression);

        String dateExample = "SNP_1001_2021-04-28";
        expectedMsg = generateExpectedToolTip(dateExample);
        actualMsg = createPage.getNameExpressionPreview();

        log("Verify that the preview shows the formatted date field as expected.");
        checker().withScreenshot("With_Date_Preview_Error")
                .verifyEquals("Tool-tip message does not contain expected date example.", expectedMsg, actualMsg);

        // Make the tooltip go away.
        mouseOver(createPage.getComponentElement());

        log(String.format("Save the sample type (%s) with the name expression.", sampleType));
        createPage.clickSave();

        waitAndClickAndWait(Locator.linkWithText(sampleType));

        log("Create a new sample using the UI and validate the tool-tip for the name field on the create page.");

        DataRegionTable sampleTypeList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        sampleTypeList.clickInsertNewRow();

        WebElement popUpLink = Locator.tagWithClass("span", "labkey-help-pop-up").findElements(getDriver()).getFirst();
        mouseOver(popUpLink);

        waitForElement(Locator.tagWithId("div", "helpDiv"));
        actualMsg = Locator.tagWithId("span", "helpDivBody").findElement(getDriver()).getText();

        checker().verifyTrue(
                String.format("Tool-tip on create page does not contain the name expression '%s'.", nameExpression),
                actualMsg.contains(nameExpression));

        checker().verifyTrue(
                String.format("Tool-tip on create page does not contain the example '%s'.", dateExample),
                actualMsg.contains(dateExample));

        checker().screenShotIfNewError("Create_Sample_Preview_Error");

        // Cleanly cancel the sample creation.
        clickAndWait(Locator.lkButton("Cancel"));

    }

    /**
     * <p>
     *     Test error and warning scenarios with the name expression.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Validate tool-tip indicates that the name expression has an error.</li>
     *         <li>Trying to save sample type design fails if there is an error in the name expression.</li>
     *         <li>Validate tool-tip is unaffected by a warning (it can generate an example name).</li>
     *         <li>Saving with a warning shows a dialog, and user can still save design.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testErrorsAndWarnings()
    {

        goToProjectHome();

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        final String sampleType = "PreviewErrors_SampleType";

        CreateSampleTypePage createPage = sampleHelper.goToCreateNewSampleType();

        createPage.setName(sampleType);

        String nameExpression = "ERROR_${genId";
        log(String.format("Use a name expression, '%s', that has an error (missing }).", nameExpression));

        createPage.setNameExpression(nameExpression);

        String expectedMsg = """
                Naming Pattern
                Pattern used for generating unique IDs for this sample type.
                Unable to generate example name from the current pattern. Check for syntax errors.
                More info""";

        String actualMsg = createPage.getNameExpressionPreview();

        log("Validate tool-tip shows there is an error, and cannot generate an example name.");
        checker().withScreenshot("Error_ToolTip_Incorrect").verifyEquals("Tool tip not as expected.", expectedMsg, actualMsg);

        log("Validate save fails if there is an error in the name expression.");
        List<String> errors = createPage.clickSaveExpectingErrors();

        if(checker().verifyFalse("Save did not generate an error message.", errors.isEmpty()))
        {
            String expectedError = "Name Pattern error: No closing brace found for the substitution pattern starting at position 7.";
            checker().verifyEquals("Error message on saving is not as expected.",
                    expectedError, errors.getFirst());
        }

        checker().screenShotIfNewError("Error_On_Save_Failure");

        nameExpression = "ERROR_genId";
        log(String.format("Change name expression to '%s' which is valid but should generate a warning.", nameExpression));

        createPage.setNameExpression(nameExpression);

        expectedMsg = generateExpectedToolTip(nameExpression);
        actualMsg = createPage.getNameExpressionPreview();

        log("Validate the tool-tip is unaffected and shows an example name.");
        checker().withScreenshot("Warning_ToolTip_Incorrect").verifyEquals("Tool tip not as expected.", expectedMsg, actualMsg);

        log("Click the 'Save' button and wait for the warning dialog.");
        Locator.button("Save").findElement(getDriver()).click();

        ModalDialog dialog = new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Naming Pattern Warning(s)").waitFor();

        actualMsg = dialog.getBodyText();
        log("Dialog text: " + actualMsg);

        checker().verifyTrue(String.format("Warning dialog does not have example text '%s'.", nameExpression),
                actualMsg.contains(nameExpression));

        expectedMsg = "'genId' is recognized as a substitution token. Use ${genId} if you want to include the substitution token value in the naming pattern.";

        checker().verifyTrue(String.format("Warning dialog does not have expected warning message '%s'.", expectedMsg),
                actualMsg.contains(expectedMsg));

        checker().screenShotIfNewError("Warning_Dialog_Error");

        log("Verify that you can save with the warning.");
        dialog.dismiss("Save anyway", 2_500);

        log("Validate save worked by looking for a link with the sample type name.");
        waitForElement(Locator.linkWithText(sampleType));

    }

    /**
     * <p>
     *     Test that GenId works as expected when bulk import is used.
     * </p>
     * <p>
     *     This test will Use a simple genId parameter in the name expression and will:
     *     <ul>
     *         <li>Use bulk import to create the initial samples in the sample type.</li>
     *         <li>Validate sample names are as expected.</li>
     *         <li>Validate that the next genId is as expected.</li>
     *         <li>Change the genId to some other (larger) number.</li>
     *         <li>Use bulk import again and validate that the new genId was used to name the samples.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testGenIdWithBulkImport()
    {

        goToProjectHome();

        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);

        final String sampleType = "Test_GenId_Bulk_Import";

        log(String.format("Create a sample type named '%s'.", sampleType));

        CreateSampleTypePage createPage = sampleTypeHelper.goToCreateNewSampleType();

        String namePrefix = "BulkGenId_";

        String nameExpression = String.format("%s${genId}", namePrefix);

        log(String.format("Use a basic genId name expression '%s'.", nameExpression));

        createPage.setName(sampleType);
        createPage.setNameExpression(nameExpression);

        createPage.clickSave();

        log("Use bulk import to creat the first few samples.");
        List<Map<String, String>> sampleData = new ArrayList<>();
        sampleData.add(Map.of("Description", "D1"));
        sampleData.add(Map.of("Description", "D2"));
        sampleData.add(Map.of("Description", "D3"));
        sampleData.add(Map.of("Description", "D4"));
        sampleData.add(Map.of("Description", "D5"));

        sampleTypeHelper.goToSampleType(sampleType);
        sampleTypeHelper.bulkImport(sampleData);

        DataRegionTable drt = sampleTypeHelper.getSamplesDataRegionTable();

        int nextGenId = 1;
        List<String> expectedSamples = Arrays.asList(String.format("%s%d", namePrefix, nextGenId++),
                String.format("%s%d", namePrefix, nextGenId++),
                String.format("%s%d", namePrefix, nextGenId++),
                String.format("%s%d", namePrefix, nextGenId++),
                String.format("%s%d", namePrefix, nextGenId++));

        List<String> actualSamples = drt.getColumnDataAsText("Name");

        Collections.sort(expectedSamples);
        Collections.sort(actualSamples);

        checker()
                .withScreenshot("Bulk_Import_GenId_Error")
                .verifyEquals("Names of samples that were imported by file/bulk are not as expected.",
                        expectedSamples, actualSamples);

        // To avoid repeatedly flagging the same unexpected sample name, take the current actual results and make them
        // part of the expected results for the next part of the test.
        expectedSamples = actualSamples;

        log("Check that the next genId is as expected.");
        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        UpdateSampleTypePage updatePage = new UpdateSampleTypePage(getDriver());

        checker()
                .withScreenshot("Bulk_Import_Next_GenId_Error")
                .verifyEquals("The value for the next genId is not as expected.",
                        Integer.toString(nextGenId), updatePage.getCurrentGenId());

        nextGenId = 501;
        log(String.format("Update genId to '%d'.", nextGenId));

        EntityTypeDesigner.GenIdDialog idDialog = updatePage.clickEditGenId();
        idDialog.setGenId(Integer.toString(nextGenId));
        idDialog.dismiss("Update");

        log("Validate that the banner has been updated.");

        checker()
                .withScreenshot("Bulk_Import_Updated_Banner_Error")
                .verifyEquals("Banner does not show the expected genId",
                        Integer.toString(nextGenId), updatePage.getCurrentGenId());

        updatePage.clickSave();

        log("Now create a few more sample with the updated genId.");

        sampleData = new ArrayList<>();
        sampleData.add(Map.of("Description", "D1"));
        sampleData.add(Map.of("Description", "D2"));

        expectedSamples.add(String.format("%s%d", namePrefix, nextGenId++));
        expectedSamples.add(String.format("%s%d", namePrefix, nextGenId));

        sampleTypeHelper.bulkImport(sampleData);

        drt = sampleTypeHelper.getSamplesDataRegionTable();

        actualSamples = drt.getColumnDataAsText("Name");

        Collections.sort(expectedSamples);
        Collections.sort(actualSamples);

        checker()
                .withScreenshot("Bulk_Import_Changed_GenId_Error")
                .verifyEquals("Names of samples with updated genId are not as expected.",
                        expectedSamples, actualSamples);

    }

    /**
     * <p>
     *     Test that using a name expression with 'genId:minValue()' works as expected.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Set minValue to 100 and create some samples. Validate the names.</li>
     *         <li>Validate that the next genId is as expected.</li>
     *         <li>Change the minValue to 50, and validate that genId continues with the next larger (100+) value.</li>
     *         <li>Change minValue to 500 and validate that new samples start with genId 500.</li>
     *         <li>Validate that the displayed next genId is as expected.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testGenIdMinValue()
    {

        goToProjectHome();

        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);

        final String sampleType = "Test_GenId_MinValue";

        log(String.format("Create a sample type named '%s'.", sampleType));

        CreateSampleTypePage createPage = sampleTypeHelper.goToCreateNewSampleType();

        createPage.setName(sampleType);

        int minValue = 100;
        int nextGenId = minValue + 1;
        String namePrefix = "MinValueGenId_";

        String nameExpression = String.format("%s${genId:minValue(%d)}", namePrefix, minValue);

        log(String.format("Set the name expression with a min value: '%s'.", nameExpression));

        createPage.setNameExpression(nameExpression);

        createPage.clickSave();

        // Issue 44844 causes problems with genId if bulk import is used to create the initial samples in a sample type.
        // Because of that, create these first sample "manually".
        log("Create some samples and validate that the min value is used for the generated name.");
        sampleTypeHelper.goToSampleType(sampleType);
        sampleTypeHelper.insertRow(Map.of("Description", "This is the first sample with no name."));
        sampleTypeHelper.insertRow(Map.of("Description", "This is the second sample with no name."));

        DataRegionTable drt = sampleTypeHelper.getSamplesDataRegionTable();

        List<String> expectedSamples = Arrays.asList(String.format("%s%d", namePrefix, nextGenId++),
                String.format("%s%d", namePrefix, nextGenId++));

        List<String> actualSamples = drt.getColumnDataAsText("Name");

        Collections.sort(expectedSamples);
        Collections.sort(actualSamples);

        checker()
                .withScreenshot("GenId_MinValue_Sample_Name_Error")
                .verifyEquals("Sample names are not as expected.", expectedSamples, actualSamples);

        // To avoid repeatedly flagging the same unexpected sample name, take the current actual results and make them
        // part of the expected results for the next part of the test.
        expectedSamples = actualSamples;

        log("Validate that the next genId value is incremented as expected.");

        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        UpdateSampleTypePage updatePage = new UpdateSampleTypePage(getDriver());

        // Hopefully the genId will be consistent from run to run.
        checker()
                .withScreenshot("GenId_MinValue_Next_GenId_Error")
                .verifyEquals("The value shown for the next genId is not as expected.",
                Integer.toString(nextGenId), updatePage.getCurrentGenId());

        minValue = 50;
        nameExpression = String.format("%s${genId:minValue(%d)}", namePrefix, minValue);

        log(String.format("Update minValue to something smaller: '%s'", nameExpression));

        updatePage.setNameExpression(nameExpression);
        updatePage.clickSave();

        log("Create a few more sample now that the minValue is smaller than the next genId value.");

        List<Map<String, String>> sampleData = new ArrayList<>();
        sampleData.add(Map.of("Description", "This is the third sample with no name. The min value is smaller."));
        sampleData.add(Map.of("Description", "This is the fourth sample with no name. The min value is smaller."));

        sampleTypeHelper.bulkImport(sampleData);

        expectedSamples.add(String.format("%s%d", namePrefix, nextGenId++));
        expectedSamples.add(String.format("%s%d", namePrefix, nextGenId++));

        drt = sampleTypeHelper.getSamplesDataRegionTable();

        actualSamples = drt.getColumnDataAsText("Name");

        Collections.sort(expectedSamples);
        Collections.sort(actualSamples);

        checker()
                .withScreenshot("GenId_Smaller_MinValue_Sample_Name_Error")
                .verifyEquals("Sample names when the minValue is smaller than the genId are not as expected.",
                        expectedSamples, actualSamples);

        expectedSamples = actualSamples;

        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        updatePage = new UpdateSampleTypePage(getDriver());

        log("The displayed genId should have incremented normally regardless of the minValue.");
        checker().verifyEquals("After making minValue smaller the value for the next genId is not as expected.",
                Integer.toString(nextGenId), updatePage.getCurrentGenId());

        minValue = 500;
        nextGenId = minValue + 1;
        nameExpression = String.format("%s${genId:minValue(%d)}", namePrefix, minValue);

        log(String.format("Now update minValue to something larger: '%s'", nameExpression));

        updatePage.setNameExpression(nameExpression);
        updatePage.clickSave();

        log("Create a few more sample with the new larger minValue.");

        sampleData = new ArrayList<>();
        sampleData.add(Map.of("Description", "This is the fifth sample with no name. The min value is larger."));
        sampleData.add(Map.of("Description", "This is the sixth sample with no name. The min value is larger."));

        sampleTypeHelper.bulkImport(sampleData);

        expectedSamples.add(String.format("%s%d", namePrefix, nextGenId++));
        expectedSamples.add(String.format("%s%d", namePrefix, nextGenId++));

        drt = sampleTypeHelper.getSamplesDataRegionTable();

        actualSamples = drt.getColumnDataAsText("Name");

        Collections.sort(expectedSamples);
        Collections.sort(actualSamples);

        checker().withScreenshot("GenId_Larger_MinValue_Sample_Name_Error")
                .verifyEquals("Sample names with a larger minValue are not as expected.", expectedSamples, actualSamples);

        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        updatePage = new UpdateSampleTypePage(getDriver());

        log("The displayed next genId should be updated to the new larger value.");
        checker().verifyEquals("The value for the next genId has not increased as expected.",
                Integer.toString(nextGenId), updatePage.getCurrentGenId());

        // Don't stay in the edit state.
        updatePage.clickCancel();
    }

    /**
     * <p>
     *     Test setting and resetting the genId value.
     * </p>
     * <p>
     *     This test will use a simple genId name expression (no minValue).
     *     <ul>
     *         <li>Validate that initial sample start with genId of 1.</li>
     *         <li>Update genId to 100 and validate banner shows new genId.</li>
     *         <li>Validate new sample start at genId 100.</li>
     *         <li>Validate genId cannot be reset if the sample type contains samples.</li>
     *         <li>Delete the samples from the sample type and validate that genId can be reset and starts at 1.</li>
     *         <li>Validate that trying to set the genId to a value less than the current genId causes an error.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testSetAndResetOfGenId()
    {

        goToProjectHome();

        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);

        final String sampleType = "Test_Set_GenId";

        log(String.format("Create a sample type named '%s'.", sampleType));

        CreateSampleTypePage createPage = sampleTypeHelper.goToCreateNewSampleType();

        createPage.setName(sampleType);

        String namePrefix = "SGId_";

        String nameExpression = String.format("%s${genId}", namePrefix);

        log(String.format("Set the name expression with no min value: '%s'.", nameExpression));

        createPage.setNameExpression(nameExpression);

        log("Validate that the genId banner is not present at creation time.");

        checker()
                .withScreenshot("Create_Time_GenId_Error")
                .verifyFalse("The genId banner should not be shown on the create page.", createPage.isGenIdVisible());

        createPage.clickSave();

        // Issue 44844 causes problems with genId if bulk import is used to create the initial samples in a sample type.
        // Because of that, create these first sample "manually".
        log("Creat a couple of samples and validate genId used started at 1.");
        sampleTypeHelper.goToSampleType(sampleType);
        sampleTypeHelper.insertRow(Map.of("Description", "This is the first sample with no name."));
        sampleTypeHelper.insertRow(Map.of("Description", "This is the second sample with no name."));

        DataRegionTable drt = sampleTypeHelper.getSamplesDataRegionTable();

        int nextGenId = 1;
        List<String> expectedSamples = Arrays.asList(String.format("%s%d", namePrefix, nextGenId++),
                String.format("%s%d", namePrefix, nextGenId++));

        List<String> actualSamples = drt.getColumnDataAsText("Name");

        Collections.sort(expectedSamples);
        Collections.sort(actualSamples);

        checker()
                .withScreenshot("Initial_GenId_Sample_Name_Error")
                .verifyEquals("Sample names with default genId value are not as expected.",
                        expectedSamples, actualSamples);

        // To avoid repeatedly flagging the same unexpected sample name, take the current actual results and make them
        // part of the expected results for the next part of the test.
        expectedSamples = actualSamples;

        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        UpdateSampleTypePage updatePage = new UpdateSampleTypePage(getDriver());

        nextGenId = 100;
        log(String.format("Update genId to a larger value '%d'", nextGenId));

        EntityTypeDesigner.GenIdDialog idDialog = updatePage.clickEditGenId();
        idDialog.setGenId(Integer.toString(nextGenId));
        idDialog.dismiss("Update");

        // check audit log
        AuditLogHelper.DetailedAuditEventRow expectedDomainEvent = new AuditLogHelper.DetailedAuditEventRow(null, sampleType, null,
                "The genId for domain Test_Set_GenId has been updated to " + (nextGenId - 1) + ".", null, null,
                null, null);
        boolean pass = _auditLogHelper.validateLastDomainAuditEvents(sampleType, getProjectName(), expectedDomainEvent, null);
        checker().verifyTrue("Result Domain event not as expected after updating genId", pass);

        log("Validate that the banner has been updated.");

        checker()
                .withScreenshot("Updated_GenId_Banner_Error")
                .verifyEquals("Banner does not show the expected genId",
                        Integer.toString(nextGenId), updatePage.getCurrentGenId());

        updatePage.clickSave();

        log("Now create a few more sample with the updated genId.");

        List<Map<String, String>> sampleData = new ArrayList<>();
        sampleData.add(Map.of("Description", "This is the third sample with no name. The genId has been updated."));
        sampleData.add(Map.of("Description", "This is the fourth sample with no name. The genId has been updated."));

        sampleTypeHelper.bulkImport(sampleData);

        expectedSamples.add(String.format("%s%d", namePrefix, nextGenId++));
        expectedSamples.add(String.format("%s%d", namePrefix, nextGenId++));

        drt = sampleTypeHelper.getSamplesDataRegionTable();

        actualSamples = drt.getColumnDataAsText("Name");

        Collections.sort(expectedSamples);
        Collections.sort(actualSamples);

        checker()
                .withScreenshot("Updated_GenId_Samples_Error")
                .verifyEquals("Sample names with updated genId are not as expected.", expectedSamples, actualSamples);

        log("Before resetting the genId first validate that the reset button is not currently visible.");
        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        updatePage = new UpdateSampleTypePage(getDriver());

        checker()
                .withScreenshot("Update_GenId_Rest_Button_Hidden_Error")
                .verifyFalse("The 'Reset GenId' button should not be visible if the sample type has samples.",
                        updatePage.isResetGenIdVisible());

        updatePage.clickCancel();

        log("Delete all of the samples and validate that the 'Reset GenId' button is now visible.");
        sampleTypeHelper.getSamplesDataRegionTable().checkAllOnPage();
        sampleTypeHelper.deleteSamples(sampleTypeHelper.getSamplesDataRegionTable(), "Permanently delete 4 samples");

        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        updatePage = new UpdateSampleTypePage(getDriver());

        waitFor(updatePage::isResetGenIdVisible,
                "The 'Reset GenId' button should now be visible if the sample type is empty. Fatal error.", 500);

        ModalDialog genIdDialog = updatePage.clickResetGenId();

        String expectedMsg = String.format("The current genId is at %d. Resetting will reset genId back to 1 and cannot be undone.", nextGenId);

        checker()
                .withScreenshot("Reset_GenId_Dialog_Error")
                .verifyEquals("Message in the reset confirm dialog is not as expected.",
                        expectedMsg, genIdDialog.getBodyText());

        log("Click 'Cancel' and verify banner/genId does not change.");
        genIdDialog.dismiss("Cancel");

        checker()
                .withScreenshot("Reset_GenId_Cancel_Error")
                .verifyEquals("Next genId should not be changed after canceling out of the reset dialog.",
                        Integer.toString(nextGenId), updatePage.getCurrentGenId());

        log("Click 'Rest GenId' again and this time reset the genId.");

        genIdDialog = updatePage.clickResetGenId();
        genIdDialog.dismiss("Reset");

        expectedDomainEvent = new AuditLogHelper.DetailedAuditEventRow(null, sampleType, null,
                "The genId for domain " + sampleType + " has been updated to 0.", null, null,
                null, null);
        pass = _auditLogHelper.validateLastDomainAuditEvents(sampleType, getProjectName(), expectedDomainEvent, null);
        checker().verifyTrue("Result Domain event not as expected after resetting genId", pass);


        nextGenId = 1;
        checker()
                .withScreenshot("Reset_GenId_Rest_Error")
                .verifyEquals(String.format("Next genId should have been reset to %d.", nextGenId),
                        Integer.toString(nextGenId), updatePage.getCurrentGenId());

        updatePage.clickSave();

        log(String.format("Now create some more samples and validate that the genId is starting at %d", nextGenId));

        expectedSamples = new ArrayList<>();
        sampleData = new ArrayList<>();
        for(int i = 1; i <= 15; i++)
        {
            sampleData.add(Map.of("Description", String.format("Sample %d with rest genId.", i)));
            expectedSamples.add(String.format("%s%d", namePrefix, nextGenId++));
        }

        sampleTypeHelper.bulkImport(sampleData);

        drt = sampleTypeHelper.getSamplesDataRegionTable();

        actualSamples = drt.getColumnDataAsText("Name");

        Collections.sort(expectedSamples);
        Collections.sort(actualSamples);

        checker()
                .withScreenshot("Reset_GenId_Samples_Error")
                .verifyEquals("Sample names after genId has been reset are not as expected.",
                        expectedSamples, actualSamples);

        log("Now set the genId to a smaller value and verify that an error is generated.");

        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        updatePage = new UpdateSampleTypePage(getDriver());

        idDialog = updatePage.clickEditGenId();

        int badGenId = nextGenId / 2;
        idDialog.setGenId(Integer.toString(badGenId));

        String actualMsg = idDialog.clickUpdateExpectError();
        expectedMsg = String.format("Unable to set genId to %d due to conflict with existing samples.", badGenId);

        checker()
                .withScreenshot("Invalid_GenId_Warning_Error")
                .verifyEquals("Warning in reset dialog not as expected.", expectedMsg, actualMsg);

        idDialog.dismiss("Cancel");
        updatePage.clickCancel();

    }

}
