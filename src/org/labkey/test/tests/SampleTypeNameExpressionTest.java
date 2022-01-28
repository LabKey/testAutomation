/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.experiment.CreateSampleTypePage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class SampleTypeNameExpressionTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleTypeNameExprTest";
    private static final String DEFAULT_SAMPLE_PARENT_VALUE = "SS";

    private static final String PARENT_SAMPLE_TYPE = "Parent_SampleType";

    private static final String PARENT_SAMPLE_01 = "parent01";
    private static final String PARENT_SAMPLE_02 = "parent02";
    private static final String PARENT_SAMPLE_03 = "parent03";

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
        SampleTypeNameExpressionTest test = (SampleTypeNameExpressionTest)getCurrentTest();
        test.doSetup();
    }

    private void doSetup() throws IOException, CommandException
    {
        _containerHelper.createProject(getProjectName(), null);
        new PortalHelper(this).addWebPart("Sample Types");

        goToProjectHome();

        log(String.format("Create a 'parent' sample type named '%s'.", PARENT_SAMPLE_TYPE));

        SampleTypeDefinition definition = new SampleTypeDefinition(PARENT_SAMPLE_TYPE);
        definition = definition.setFields(List.of(
                new FieldDefinition("Str", FieldDefinition.ColumnType.String),
                new FieldDefinition("Int", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("Date", FieldDefinition.ColumnType.DateAndTime)));

        TestDataGenerator dataGenerator = SampleTypeAPIHelper.createEmptySampleType(getCurrentContainerPath(), definition);

        log(String.format("Give the parent sample type '%1$s' two samples named '%2$s' and '%3$s'",
                PARENT_SAMPLE_TYPE, PARENT_SAMPLE_01, PARENT_SAMPLE_02));

        Map<String, Object> sampleData = Map.of(
                "name", PARENT_SAMPLE_01,
                "Int", 1,
                "Str", "Parent Sample A",
                "Date", "7/14/2020");
        dataGenerator.addCustomRow(sampleData);

        sampleData = Map.of(
                "name", PARENT_SAMPLE_02,
                "Int", 2,
                "Str", "Parent Sample B",
                "Date", "11/21/2019");
        dataGenerator.addCustomRow(sampleData);

        sampleData = Map.of(
                "name", PARENT_SAMPLE_03,
                "Int", 3,
                "Str", "Parent Sample C",
                "Date", "12/25/2015");
        dataGenerator.addCustomRow(sampleData);

        dataGenerator.insertRows();

        // Just want to get an updated view of the sample types.
        refresh();

    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testSimpleNameExpression()
    {
        String nameExpression = "${A}-${B}.${genId}.${batchRandomId}.${randomId}";
        String data = "A\tB\tC\n" +
                "a\tb\tc\n" +
                "a\tb\tc\n" +
                "a\tb\tc\n";
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition("SimpleNameExprTest")
                        .setNameExpression(nameExpression)
                        .setFields(List.of(new FieldDefinition("A", FieldDefinition.ColumnType.String),
                                new FieldDefinition("B", FieldDefinition.ColumnType.String),
                                new FieldDefinition("C", FieldDefinition.ColumnType.String))),
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
        assertThat(names.get(0), startsWith("a-b.3." + batchRandomId + "."));
        assertThat(names.get(1), startsWith("a-b.2." + batchRandomId + "."));
        assertThat(names.get(2), startsWith("a-b.1." + batchRandomId + "."));
    }

    @Test
    public void testInputsExpression()
    {
        verifyNames(
                "InputsExpressionTest",
                "Name\tB\tMaterialInputs/InputsExpressionTest",
                "${Inputs:first:defaultValue('" + DEFAULT_SAMPLE_PARENT_VALUE + "')}_${batchRandomId}",
                null, "Pat");
    }

    @Test
    public void testParentAliasExpression()
    {
        verifyNames(
                "ParentAliasInputsExpressionTest",
                "Name\tB\tParent",
                "${Parent:first:defaultValue('" + DEFAULT_SAMPLE_PARENT_VALUE + "')}_${batchRandomId}",
                "Parent", "Jessi");
    }

    // Issue 42857: samples: bulk import with name expression containing lookup fails to convert lookup string value
    @Test
    public void testLookupNameExpression() throws Exception
    {
        String lookupList = "Colors";
        FieldDefinition.LookupInfo colorsLookup = new FieldDefinition.LookupInfo(getProjectName(), "lists", lookupList)
                .setTableType(FieldDefinition.ColumnType.Integer);
        String nameExpSamples = "NameExpressionSamples";

        // begin by creating a lookupList of colors, the sampleType will reference it
        TestDataGenerator colorsGen = new TestDataGenerator(colorsLookup)
                .withColumns(List.of(new FieldDefinition("ColorName", FieldDefinition.ColumnType.String),
                        new FieldDefinition("ColorCode", FieldDefinition.ColumnType.String)));
        colorsGen.addCustomRow(Map.of("ColorName", "green", "ColorCode", "gr"));
        colorsGen.addCustomRow(Map.of("ColorName", "yellow", "ColorCode", "yl"));
        colorsGen.addCustomRow(Map.of("ColorName", "red", "ColorCode", "rd"));
        colorsGen.addCustomRow(Map.of("ColorName", "blue", "ColorCode", "bl"));
        colorsGen.createList(createDefaultConnection(), "Key");
        colorsGen.insertRows();

        String pasteData = "ColorLookup\tNoun\n" +
                "red\tryder\n" +
                "green\tgiant\n" +
                "blue\tangel\n" +
                "yellow\tjersey";

        // now create a sampleType with a Color column that looks up to Colors
        var sampleTypeDef = new SampleTypeDefinition(nameExpSamples)
                .setFields(List.of(new FieldDefinition("ColorLookup", colorsLookup),
                        new FieldDefinition("Noun", FieldDefinition.ColumnType.String)))
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
                "Name\tB\tParent",
                "${MaterialInputs:first:defaultValue('" + DEFAULT_SAMPLE_PARENT_VALUE + "')}_${batchRandomId}",
                "Parent", "Sam");
    }

    private void verifyNames(String sampleTypeName, String header, String nameExpression, @Nullable String currentTypeAlias, String namePrefix)
    {
        String name1 = namePrefix + "_1";
        String name2 = namePrefix + "_2";
        String data = header + "\n" +

                // Name provided
                name1 + "\tb\t\n" +
                name2 + "\tb\t\n" +

                // Name generated and uses first input "Bob"
                "\tb\t" + name1 + "," + name2 + "\n" +

                // Name generated and uses defaultValue('SS')
                "\tb\t\n";

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        SampleTypeDefinition definition = new SampleTypeDefinition(sampleTypeName)
                .setNameExpression(nameExpression);
        if (currentTypeAlias != null)
            definition = definition.setParentAliases(Map.of(currentTypeAlias, "(Current Sample Type)"));
        definition = definition.setFields(List.of(new FieldDefinition("B", FieldDefinition.ColumnType.String)));
        sampleHelper.createSampleType(definition, data);

        assertTextPresent(nameExpression);

        DataRegionTable materialTable = new DataRegionTable("Material", this);
        List<String> names = materialTable.getColumnDataAsText("Name");

        assertTrue("First name (" + names.get(0) + ") not as expected", names.get(0).startsWith(DEFAULT_SAMPLE_PARENT_VALUE + "_"));
        String batchRandomId = names.get(0).split("_")[1];

        assertEquals("Second name not as expected",  name1 + "_" + batchRandomId, names.get(1));

        assertEquals("Third name not as expected", name2,  names.get(2));
        assertEquals("Fourth name not as expected", name1, names.get(3));
    }

    /**
     * <p>
     *     Verify that a derived sample, using a name expression, can be created through the UI.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Create a derived sample using the UI and the name expression to name it.</li>
     *     </ul>
     * </p>
     * @throws Exception Can be thrown by test helper.
     */
    @Test
    public void testCreateDerivedThroughUI() throws Exception
    {

        // This test is for Issue 44760.
        goToProjectHome();

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        final String sampleType = "DerivedUI_SampleType";
        final String nameExpression = String.format("DUI_${genId}_${materialInputs/%s/Str}", PARENT_SAMPLE_TYPE);

        log(String.format("Create a sample type named '%s' with a name expression of '%s'.", sampleType, nameExpression));

        CreateSampleTypePage createPage = sampleHelper.goToCreateNewSampleType();

        createPage.setName(sampleType);

        createPage.setNameExpression(nameExpression);

        createPage.addFields(Arrays.asList(
                new FieldDefinition("Int", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("Str", FieldDefinition.ColumnType.String)));

        createPage.clickSave();

        log(String.format("Go to the 'overview' page for sample '%s' in sample type '%s'", PARENT_SAMPLE_01, PARENT_SAMPLE_TYPE));
        Long sampleRowNum = SampleTypeAPIHelper.getSampleIdFromName(getProjectName(), PARENT_SAMPLE_TYPE, Arrays.asList(PARENT_SAMPLE_01)).get(PARENT_SAMPLE_01);

        String url = WebTestHelper.buildRelativeUrl("experiment", getCurrentContainerPath(), "showMaterial", Map.of("rowId", sampleRowNum));
        beginAt(url);

        log("Derive a sample from this sample but give it no name. The name expression should be used to name the derived sample.");

        waitForElement(Locator.linkWithText("derive samples from this sample"));

        clickAndWait(Locator.linkWithText("derive samples from this sample"));

        selectOptionByText(Locator.name("targetSampleTypeId"),  String.format("%s in /%s", sampleType, getProjectName()));
        clickButton("Next");

        String flagString = "Hello, I'm a derived sample.";
        setFormElement(Locator.name("outputSample1_Int"), "987");
        setFormElement(Locator.name("outputSample1_Str"), flagString);
        clickButton("Submit");

        waitForElement(Locator.tagWithText("td", flagString));

        String derivedSampleName = Locator.tagWithText("td", "Name:").followingSibling("td").findElement(getDriver()).getText();

        checker().verifyTrue("Name of derived sample doesn't look correct.", derivedSampleName.contains("Parent Sample"));

        checker().verifyTrue("Doesn't look like there is a link to the parent sample.", isElementPresent(Locator.linkWithText(PARENT_SAMPLE_01)));

    }

    /**
     * Simple helper to build the expected text in the tool-tip for the name expression.
     *
     * @param expectedPreview What the expected preview example should look like. If null will return default tool-tip.
     * @param error Should the name expression generate an error?
     * @return The expected text in the tool-tip including header and other text.
     */
    private String generateExpectedToolTip(@Nullable String expectedPreview, boolean error)
    {

        StringBuilder expectedToolTip = new StringBuilder();

        expectedToolTip.append("Naming Pattern\n");
        expectedToolTip.append("Pattern used for generating unique IDs for this sample type.\n");

        if(expectedPreview != null)
        {
            if(!error)
            {
                expectedToolTip.append("Example of name that will be generated from the current pattern: ");
            }

            expectedToolTip.append(expectedPreview);
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
     * @throws CommandException Can be thrown by test helper.
     */
    @Test
    public void testNameExpressionPreview() throws IOException, CommandException
    {

        goToProjectHome();

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        final String sampleType = "Preview_SampleType";

        CreateSampleTypePage createPage = sampleHelper.goToCreateNewSampleType();

        createPage.setName(sampleType);

        log("Check the default tool-tip.");
        String expectedMsg = generateExpectedToolTip(null, false);
        String actualMsg = createPage.getNameExpressionPreview();

        checker().withScreenshot("Default_Preview_Error")
                .verifyEquals("Default tool-tip message not as expected.", expectedMsg, actualMsg);

        // Make the tooltip go away.
        mouseOver(createPage.getComponentElement());

        log("Set a named parent.");

        final String parentAlias = "My_Parent";

        createPage.addParentAlias(parentAlias, String.format("Sample Type: %1$s (%2$s)", PARENT_SAMPLE_TYPE, PROJECT_NAME));

        log("Use a name expression using a field from the named parent.");
        String nameExpression = String.format("SNP_${genId}_${%1$s/Int}_${materialInputs/%2$s/Str}", parentAlias, PARENT_SAMPLE_TYPE);

        createPage.setNameExpression(nameExpression);

        expectedMsg = generateExpectedToolTip("SNP_1001_3_parentStrValue", false);
        actualMsg = createPage.getNameExpressionPreview();

        log("Verify that the preview shows the fields as expected.");
        checker().withScreenshot("Parent_Fields_Preview_Error")
                .verifyEquals("Tool-tip message does not contain expected example.", expectedMsg, actualMsg);

        // Make the tooltip go away.
        mouseOver(createPage.getComponentElement());

        log("Use a name expression with a formatted date.");

        nameExpression = String.format("SNP_${genId}_${%s/Date:date('yyyy-MM-dd')}", parentAlias);

        createPage.setNameExpression(nameExpression);

        String dateExample = "SNP_1001_2021-04-28";
        expectedMsg = generateExpectedToolTip(dateExample, false);
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

        WebElement popUpLink = Locator.tagWithClass("span", "labkey-help-pop-up").findElements(getDriver()).get(0);
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

        String expectedMsg = generateExpectedToolTip("Unable to generate example name from the current pattern. Check for syntax errors.", true);
        String actualMsg = createPage.getNameExpressionPreview();

        log("Validate tool-tip shows there is an error, and cannot generate an example name.");
        checker().withScreenshot("Error_ToolTip_Incorrect").verifyEquals("Tool tip not as expected.", expectedMsg, actualMsg);

        log("Validate save fails if there is an error in the name expression.");
        List<String> errors = createPage.clickSaveExpectingErrors();

        if(checker().verifyFalse("Save did not generate an error message.", errors.isEmpty()))
        {
            String expectedError = "Name Pattern error: No closing brace found for the substitution pattern starting at position 7.";
            checker().verifyEquals("Error message on saving is not as expected.",
                    expectedError, errors.get(0));
        }

        checker().screenShotIfNewError("Error_On_Save_Failure");

        nameExpression = "ERROR_genId";
        log(String.format("Change name expression to '%s' which is valid but should generate a warning.", nameExpression));

        createPage.setNameExpression(nameExpression);

        expectedMsg = generateExpectedToolTip(nameExpression, false);
        actualMsg = createPage.getNameExpressionPreview();

        log("Validate the tool-tip is unaffected and shows an example name.");
        checker().withScreenshot("Warning_ToolTip_Incorrect").verifyEquals("Tool tip not as expected.", expectedMsg, actualMsg);

        log("Click the 'Save' button and wait for the warning dialog.");
        Locator.button("Save").findElement(getDriver()).click();

        ModalDialog dialog = new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Naming Patten Warning(s)").waitFor();

        actualMsg = dialog.getBodyText();
        log("Dialog text: " + actualMsg);

        checker().verifyTrue("Warning dialog does not have example name.", actualMsg.contains(nameExpression));

        expectedMsg = "The 'genId' substitution pattern starting at position 6 should be preceded by the string '${'.";

        checker().verifyTrue("Warning dialog does not have expected warning message.", actualMsg.contains(expectedMsg));

        checker().screenShotIfNewError("Warning_Dialog_Error");

        log("Verify that you can save with the warning.");
        dialog.dismiss("Save anyways...", 2_500);

        log("Validate save worked by looking for a link with the sample type name.");
        waitForElement(Locator.linkWithText(sampleType));

    }

}
