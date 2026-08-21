/*
 * Copyright (c) 2008-2026 LabKey Corporation
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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.assay.AssayConstants;
import org.labkey.test.components.domain.AdvancedSettingsDialog;
import org.labkey.test.components.domain.BaseDomainDesigner;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.core.admin.BaseSettingsPage;
import org.labkey.test.pages.experiment.CreateSampleTypePage;
import org.labkey.test.pages.experiment.UpdateSampleTypePage;
import org.labkey.test.pages.query.UpdateQueryRowPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.params.FieldDefinition.LookupInfo;
import org.labkey.test.params.FieldInfo;
import org.labkey.test.params.experiment.InventoryMetricUnit;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.AuditLogHelper;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ExcelHelper;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.TestUser;
import org.labkey.test.util.data.TestDataUtils;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.params.FieldDefinition.DOMAIN_TRICKY_CHARACTERS;
import static org.labkey.test.util.DataRegionTable.DataRegion;
import static org.labkey.test.util.PermissionsHelper.FOLDER_ADMIN_ROLE;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 20)
public class SampleTypeTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleTypeTestProject";
    private static final String FOLDER_NAME = "SampleTypeTestFolder";
    private static final String LOOKUP_FOLDER = "LookupSampleTypeFolder";
    private static final String CASE_INSENSITIVE_SAMPLE_TYPE = "CaseInsensitiveSampleType";
    private static final String LOWER_CASE_SAMPLE_TYPE = CASE_INSENSITIVE_SAMPLE_TYPE.toLowerCase();
    private static final String UPPER_CASE_SAMPLE_TYPE = CASE_INSENSITIVE_SAMPLE_TYPE.toUpperCase();
    private static final TestUser USER_FOR_FILTERTEST = new TestUser("filter_user@sampletypetest.test");
    boolean IS_POSTGRES = WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL;

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
        SampleTypeTest init = getCurrentTest();

        // Comment out this line (after you run once) it will make iterating on tests much easier.
        init.doSetup();
    }

    private void doSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(PROJECT_NAME, null);
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Sample Types");

        _containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME, "Collaboration");
        portalHelper.addWebPart("Sample Types");

        _containerHelper.createSubfolder(PROJECT_NAME, LOOKUP_FOLDER, "Collaboration");
        portalHelper.addWebPart("Sample Types");
        portalHelper.exitAdminMode();
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        // If you are debugging tests change this function to do nothing.
        // It can make re-running faster but you need to valid the integrity of the test data on your own.
//        log("Do nothing.");
        _userHelper.deleteUsers(false, USER_FOR_FILTERTEST.getEmail());
    }

    // Issue 52390: milliseconds are truncated from time fields on update or reshow
    @Test
    public void testDateAndTimeValueUpdates() throws Exception
    {
        var projectSettingsPage = goToProjectSettings();
        projectSettingsPage.setDefaultDateDisplayInherited(false);
        projectSettingsPage.setDefaultDateDisplay(BaseSettingsPage.DATE_FORMAT.MMMM_dd_yyyy);
        projectSettingsPage.setDefaultDateTimeDisplayInherited(false);
        projectSettingsPage.setDefaultDateTimeDisplay(BaseSettingsPage.DATE_FORMAT.dd_MMM_yyyy,
                BaseSettingsPage.TIME_FORMAT.HH_mm_ss_SSS);
        projectSettingsPage.setDefaultTimeDisplayInherited(false);
        projectSettingsPage.setDefaultTimeDisplay(BaseSettingsPage.TIME_FORMAT.HH_mm_ss_SSS);
        projectSettingsPage.save();

        final String sampleTypeName = "dateTimeEditSamples";
        final FieldDefinition txtField = new FieldDefinition(
                TestDataGenerator.randomFieldName("text"), ColumnType.String).setRequired(true);
        final FieldDefinition dateField = new FieldDefinition(
                TestDataGenerator.randomFieldName("date"), ColumnType.Date);
        final FieldDefinition timeField = new FieldDefinition(
                TestDataGenerator.randomFieldName("time"), ColumnType.Time);
        final FieldDefinition dateTimeField = new FieldDefinition(
                TestDataGenerator.randomFieldName("dateTime"),  ColumnType.DateAndTime);
        final List<FieldDefinition> fields = List.of(txtField, dateField, timeField, dateTimeField);

        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(sampleTypeName).setFields(fields);
        sampleTypeDefinition.create(createDefaultConnection(), getProjectName());

        goToProjectHome();
        waitAndClickAndWait(Locator.linkWithText(sampleTypeName));
        var dataRegion = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        var updatePage = dataRegion.clickInsertNewRow();
        String date = "January 01 2025";
        String time = "23:56:54.123";
        String dateTime = "06-May-1986 23:58:34.123";
        updatePage.setField("Name", "sample01");
        updatePage.setField(dateField.getName(), date);
        updatePage.setField(timeField.getName(), time);
        updatePage.setField(dateTimeField.getName(), dateTime);
        updatePage.submitExpectingError();

        checker().wrapAssertion(()-> Assertions.assertThat(updatePage.getTextInputValue(dateField.getName()))
                .isEqualTo("2025-01-01")); // expect reformat of date
        checker().wrapAssertion(()-> Assertions.assertThat(updatePage.getTextInputValue(timeField.getName()))
                .as("expect time to retain milliseconds")
                .isEqualTo(time));
        checker().wrapAssertion(()-> Assertions.assertThat(updatePage.getTextInputValue(dateTimeField.getName()))
                .as("expect dateTime to post back as entered")
                .isEqualTo("1986-05-06 23:58:34.123"));
        checker().screenShotIfNewError("unexpected data update");

        // fill in the required text field and submit
        updatePage.setField(txtField.getName(), "sample01");
        updatePage.submit();

        var afterSubmit = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        var rowData = afterSubmit.getRowDataAsText(0);
        checker().withScreenshot("unexpected data persisted")
                .wrapAssertion(()-> Assertions.assertThat(rowData)
                .as("Issue 52390 expect date, time, dateTime to be shown as entered")
                .contains(date, time, dateTime));

        var cleanupDateFormatsPage = goToProjectSettings();
        cleanupDateFormatsPage.setDefaultDateDisplayInherited(true);
        cleanupDateFormatsPage.setDefaultDateTimeDisplayInherited(true);
        cleanupDateFormatsPage.setDefaultTimeDisplayInherited(true);
        cleanupDateFormatsPage.save();
    }

    @Test
    public void testCreateSampleTypeNoExpression()
    {
        final String sampleTypeName = "SimpleCreateNoExp";
        FieldInfo stringCol = FieldInfo.random("StringValue", ColumnType.String);
        FieldInfo intCol = FieldInfo.random("IntValue", ColumnType.Integer);
        final List<FieldDefinition> fields = List.of(stringCol.getFieldDefinition(), intCol.getFieldDefinition());

        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(sampleTypeName).setFields(fields);

        log("Create a new sample type with a name and no name expression");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);
        sampleTypeHelper.createSampleType(sampleTypeDefinition);
        sampleTypeHelper.goToSampleType(sampleTypeName);
        sampleTypeHelper.verifyFields(fields);

        log("Add a single row to the sample type");
        Map<String, String> fieldMap = Map.of("Name", "S-1", stringCol.getName(), "Ess", intCol.getName(), "1");
        sampleTypeHelper.insertRow(fieldMap);

        log("Verify values were saved");
        fieldMap = Map.of("Name", "S-1", stringCol.toString(), "Ess", intCol.toString(), "1");
        sampleTypeHelper.verifyDataValues(Collections.singletonList(fieldMap));

        List<Map<String, String>> data = new ArrayList<>();
        data.add(Map.of("Name", "S-2", stringCol.getName(), "Tee", intCol.getName(), "2"));
        data.add(Map.of("Name", "S-3", stringCol.getName(), "Ewe", intCol.getName(), "3"));
        sampleTypeHelper.bulkImport(data);

        assertEquals("Number of samples not as expected", 3, sampleTypeHelper.getSampleCount());
        data = new ArrayList<>();
        data.add(Map.of("Name", "S-2", stringCol.toString(), "Tee", intCol.toString(), "2"));
        data.add(Map.of("Name", "S-3", stringCol.toString(), "Ewe", intCol.toString(), "3"));
        sampleTypeHelper.verifyDataValues(data);
    }

    // Issue 53313: LKS doesn't show Sample Type fields with special characters in Custom Properties
    @Test
    public void testCustomProperties()
    {
        final String sampleTypeName = "SampleTypeCustomProps" + DOMAIN_TRICKY_CHARACTERS;
        FieldInfo stringCol1 = FieldInfo.random("StringColPlain", ColumnType.String);
        FieldInfo stringCol2 = FieldInfo.random("StringCol%", ColumnType.String);
        // Used to make sure the details page shows properties with null values
        FieldInfo stringCol3 = FieldInfo.random("StringColNull", ColumnType.String);
        FieldInfo calcCol = FieldInfo.random("CalcCol", ColumnType.Calculation);
        final List<FieldDefinition> fields = List.of(
                stringCol1.getFieldDefinition(),
                stringCol2.getFieldDefinition(),
                stringCol3.getFieldDefinition(),
                calcCol.getFieldDefinition().setValueExpression(EscapeUtil.getSqlQuotedValue(stringCol1.getName()) + " || 'Concat'")
        );

        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(sampleTypeName).setFields(fields);

        SampleTypeAPIHelper.createEmptySampleType(getProjectName(), sampleTypeDefinition);

        log("Create a new sample type");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);
        sampleTypeHelper.goToSampleType(sampleTypeName);

        log("Add a single row to the sample type, with trailing spaces");
        Map<String, String> fieldMap = Map.of("Name", "CustomPropsSample", stringCol1.getName(), "PlainValue", stringCol2.getName(), "PercentValue");
        sampleTypeHelper.insertRow(fieldMap);

        log("Verify custom properties, both name and values, are shown in both the grid and detail pages");
        var dataRegion = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        checker().verifyEquals("Row data does not contain expected custom properties", "PlainValue", dataRegion.getDataAsText(0, stringCol1.getLabel()));
        checker().verifyEquals("Row data does not contain expected custom properties", "PercentValue", dataRegion.getDataAsText(0, stringCol2.getLabel()));
        checker().verifyEquals("Row data does not contain expected custom properties", " ", dataRegion.getDataAsText(0, stringCol3.getLabel()));
        checker().verifyEquals("Row data does not contain expected custom properties", "PlainValueConcat", dataRegion.getDataAsText(0, calcCol.getLabel()));
        clickAndWait(Locator.linkWithText("CustomPropsSample"));
        assertTextPresent(stringCol1.getLabel(), stringCol2.getLabel(), stringCol3.getLabel(), calcCol.getLabel(), "PlainValue", "PercentValue", "PlainValueConcat");
    }

    @Test  // GH Issue 1257
    public void testOverlappingAliases()
    {
        final String sampleTypeName = "OverlappingAliasSampleType";
        final String fieldOne = "AliasFieldOne";
        final String fieldTwo = "AliasFieldTwo";
        final String sharedAlias = "sharedAlias";

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        clickProject(PROJECT_NAME);
        CreateSampleTypePage createPage = sampleHelper
                .goToCreateNewSampleType()
                .setName(sampleTypeName);
        DomainFormPanel fieldsPanel = createPage.getFieldsPanel();

        log("Verify there can be no duplicate import aliases for different fields in a sample type (case-insensitive). ");
        fieldsPanel.addField(fieldOne).setImportAliases(sharedAlias);
        fieldsPanel.addField(fieldTwo).setImportAliases(sharedAlias);

        checker().verifyThat("Expected an error when two fields share an import alias",
                String.join("\n", createPage.clickSaveExpectingErrors()),
                containsString("You have 2 field errors."));
        checker().screenShotIfNewError("duplicateImportAlias");

        log("Aliases differing only by case are still duplicates.");
        createPage = new CreateSampleTypePage(this.getDriver());
        fieldsPanel.getField(fieldTwo).setImportAliases(sharedAlias.toUpperCase());
        checker().verifyThat("Expected an error when two fields share an import alias differing only by case",
                String.join("\n", createPage.clickSaveExpectingErrors()),
                containsString("You have 2 field errors."));
        checker().screenShotIfNewError("duplicateImportAliasIgnoringCase");

        log("Verify there can be no import aliases that collide with field names (case-insensitive).");
        createPage = new CreateSampleTypePage(this.getDriver());
        fieldsPanel.getField(fieldTwo).setImportAliases("");
        fieldsPanel.getField(fieldOne).setImportAliases(fieldTwo);
        checker().verifyThat("Expected an error when an import alias matches another field's name",
                String.join("\n", createPage.clickSaveExpectingErrors()),
                containsString("Import alias '" + fieldTwo + "' on field '" + fieldOne + "' conflicts with a field name."));
        checker().screenShotIfNewError("importAliasConflictsWithFieldName");

        log("An alias that matches a field name except for case is still a conflict.");
        createPage = new CreateSampleTypePage(this.getDriver());
        fieldsPanel.getField(fieldOne).setImportAliases(fieldTwo.toLowerCase());
        checker().verifyThat("Expected an error when an import alias matches another field's name except for case",
                String.join("\n", createPage.clickSaveExpectingErrors()),
                containsString("Import alias '" + fieldTwo.toLowerCase() + "' on field '" + fieldOne + "' conflicts with a field name."));
        checker().screenShotIfNewError("importAliasConflictsWithFieldNameIgnoringCase");

        // GH Issue 1474: import aliases must not collide with a reserved field name
        log("Verify an import alias cannot collide with a reserved field name. GH Issue 1474");
        for (String reservedName : Arrays.asList("Folder", "Container", "genId", "CpasType", "FreezeThawCount"))
        {
            createPage = new CreateSampleTypePage(this.getDriver());
            fieldsPanel.getField(fieldOne).setImportAliases(reservedName);
            checker().verifyThat("Expected an error when an import alias matches reserved field name '" + reservedName + "'",
                    String.join("\n", createPage.clickSaveExpectingErrors()),
                    containsString("Import alias '" + reservedName + "' on field '" + fieldOne + "' conflicts with a reserved field name."));
            checker().screenShotIfNewError("importAliasConflictsWithReservedName_" + reservedName);
        }

        log("An alias that repeats its own field's name is redundant but not ambiguous, so it should be allowed.");
        fieldsPanel.getField(fieldOne).setImportAliases(fieldOne);
        createPage.clickSave();

        clickProject(PROJECT_NAME);
        UpdateSampleTypePage updatePage = sampleHelper.goToEditSampleType(sampleTypeName);
        checker().verifyEquals("Import alias matching its own field name was not saved",
                fieldOne, updatePage.getFieldsPanel().getField(fieldOne).getImportAliases());
        checker().screenShotIfNewError("selfReferencingImportAlias");
        updatePage.clickCancel();
    }

    // Issue 47280: LKSM: Trailing/Leading whitespace in Source name won't resolve when deriving samples
    @Test
    public void testImportSamplesWithTrailingSpace() throws IOException, CommandException
    {
        final String sampleTypeName = "SampleTypeWithProvidedName";
        final List<FieldDefinition> fields = List.of(
                new FieldDefinition("IntCol", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("StringCol", FieldDefinition.ColumnType.String),
                new FieldDefinition("DateCol", FieldDefinition.ColumnType.DateAndTime),
                new FieldDefinition("BoolCol", FieldDefinition.ColumnType.Boolean));

        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(sampleTypeName).setFields(fields);

        log("Create a new sample type with no name expression");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);
        sampleTypeHelper.createSampleType(sampleTypeDefinition);
        sampleTypeHelper.goToSampleType(sampleTypeName);

        log("Add a single row to the sample type, with trailing spaces");
        Map<String, String> fieldMap = Map.of("Name", " S-1 ", "StringCol", "Ess ", "IntCol", "1 ");
        sampleTypeHelper.insertRow(fieldMap);

        AuditLogHelper auditLogHelper = new AuditLogHelper(this);
        int transactionId = auditLogHelper.checkAuditEventDiffCountForLastTransaction(getProjectName(), AuditLogHelper.AuditEvent.SAMPLE_TIMELINE_EVENT, 21, 1);
        Map<String, Object>expectedValues = new HashMap<>();
        expectedValues.put("Comment", "Sample was registered.");
        auditLogHelper.checkAuditEventValuesForTransactionId(getProjectName(), AuditLogHelper.AuditEvent.SAMPLE_TIMELINE_EVENT, transactionId, 1, expectedValues);

        log("Verify values were saved are without trailing spaces");
        sampleTypeHelper.verifyDataValues(Collections.singletonList(fieldMap));

        log("Bulk insert into to the sample type, with trailing spaces");
        List<Map<String, String>> data = new ArrayList<>();
        data.add(Map.of("Name", " S-2 ", "StringCol", "Tee ", "IntCol", "2 ", "BoolCol", "true "));
        data.add(Map.of("Name", " S-3 ", "StringCol", "Ewe ", "IntCol", "3 ", "BoolCol", "false "));
        sampleTypeHelper.bulkImport(data);
        assertEquals("Number of samples not as expected", 3, sampleTypeHelper.getSampleCount());

        log("Verify values were saved are without trailing spaces");
        sampleTypeHelper.verifyDataValues(data);

        log("Import samples from file, with trialing spaces in Name, String, and Bool fields");
        data = new ArrayList<>();
        data.add(Map.of("Name", "SampleSetBVT1 ", "StringCol", "a ", "IntCol", "100 ", "BoolCol", "true "));
        sampleTypeHelper.bulkImport(TestFileUtils.getSampleData("sampleType.xlsx"));

        log("Verify values were imported are without trailing spaces");
        sampleTypeHelper.verifyDataValues(data);
    }

    @Test
    public void testMeFilterOnSampleType()
    {
        USER_FOR_FILTERTEST.create(this)
                .addPermission(FOLDER_ADMIN_ROLE, getProjectName());
        String sampleType = "meFilterSamples";
        FieldInfo sizeField = FieldInfo.random("size", ColumnType.Integer);
        FieldInfo userField = FieldInfo.random("user", ColumnType.User);
        var domainDesigner = CreateSampleTypePage.beginAt(this, getProjectName());
        domainDesigner.setName(sampleType)
                .addField(sizeField.getFieldDefinition())
                .addField(userField.getFieldDefinition());
        var formatDialog = domainDesigner.getFieldsPanel().getField(userField.getName()).clickConditionalFormatButton();
        formatDialog.getOpenFormatPanel()
                .setFirstCondition(Filter.Operator.EQUAL)
                .setFirstValue("~me~")
                .setFillColor("#F44E3B")    // red
                .setBoldCheckbox(true);
        formatDialog.clickApply();
        domainDesigner.clickSave();

        var sampleHelper = new SampleTypeHelper(this).goToSampleType(sampleType);

        var insertPage = sampleHelper.getSamplesDataRegionTable().clickInsertNewRow();
        insertPage.setField("Name", "me")
                    .setField(sizeField.getName(), 2)
                    .setField(userField.getName(), OptionSelect.SelectOption.textOption(getDisplayName()))
                    .submit();
        insertPage = sampleHelper.getSamplesDataRegionTable().clickInsertNewRow();
        insertPage.setField("Name", "not me")
                .setField(sizeField.getName(), 3)
                .setField(userField.getName(), OptionSelect.SelectOption.textOption(USER_FOR_FILTERTEST.getUserDisplayName()))
                .submit();

        var meCell = Locator.tag("td").withChild(Locator.tagWithText("a", getDisplayName()))
                .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        var notMeCell = Locator.tag("td").withChild(Locator.tagWithText("a", USER_FOR_FILTERTEST.getUserDisplayName()))
                .findElement(getDriver());
        assertEquals("expect custom format for me filter",
                "rgb(244, 78, 59)", meCell.getCssValue("background-color"));
        mouseOver(meCell);
        WebElement helpDivBody = shortWait().until(ExpectedConditions.visibilityOfElementLocated(Locator.id("helpDivBody")));
        assertEquals("expect custom format popup for me filter",
                "Formatting applied because column = ~me~.", helpDivBody.getText());
        assertNotEquals("expect cell for other user not to get custom format",
                "rgb(244, 78, 59)", notMeCell.getCssValue("background-color"));
    }

    @Test
    public void testCreateSampleTypeWithExpression()
    {
        String sampleTypeName = "SimpleCreateWithExp";
        List<String> fieldNames = Arrays.asList("StringValue", "FloatValue");
        List<FieldDefinition> fields = Arrays.asList(new FieldDefinition(fieldNames.get(0), ColumnType.String), new FieldDefinition(fieldNames.get(1), ColumnType.Decimal));
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);
        log("Create a new sample type with a name and name expression");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        SampleTypeDefinition definition = new SampleTypeDefinition(sampleTypeName).setNameExpression("${" + fields.getFirst().getName() + "}-${batchRandomId}-${randomId}").setFields(fields);
        sampleTypeHelper.createSampleType(definition);
        sampleTypeHelper.goToSampleType(sampleTypeName);
        sampleTypeHelper.verifyFields(fields);

        log("Add data without supplying the name");
        Map<String, String> fieldMap = Map.of(fieldNames.get(0), "Vee", fieldNames.get(1), "1.6");
        sampleTypeHelper.insertRow(fieldMap);

        log("Verify values are as expected with name expression saved");
        DataRegionTable drt = sampleTypeHelper.getSamplesDataRegionTable();
        int index = drt.getRowIndex(fieldNames.getFirst(), "Vee");
        assertTrue("Did not find row containing data", index >= 0);
        Map<String, String> rowData = drt.getRowDataAsMap(index);
        assertTrue("Name not as expected", rowData.get("Name").startsWith("Vee-"));
        assertEquals(fieldNames.get(0) + " not as expected", "Vee", rowData.get(fieldNames.get(0)));
        assertEquals(fieldNames.get(1) + "not as expected", "1.6", rowData.get(fieldNames.get(1)));

        log("Add data with name provided");
        sampleTypeHelper.insertRow(Map.of("Name", "NoExpression"));

        log("Verify values are as expected with name value saved");
        drt = sampleTypeHelper.getSamplesDataRegionTable();
        index = drt.getRowIndex("Name", "NoExpression");
        assertTrue("Did not find row with inserted name", index >= 0);

        log ("Add multiple rows via simple (default) import mechanism");
        List<Map<String, String>> data = new ArrayList<>();
        data.add(Map.of(fieldNames.get(0), "Dubya", fieldNames.get(1), "2.1"));
        data.add(Map.of(fieldNames.get(0), "Ex", fieldNames.get(1), "4.2"));
        sampleTypeHelper.bulkImport(data);

        assertEquals("Number of samples not as expected", 4, sampleTypeHelper.getSampleCount());

        assertTrue("Should have row with first imported value", drt.getRowIndex(fieldNames.getFirst(), "Dubya") >= 0);
        assertTrue("Should have row with second imported value", drt.getRowIndex(fieldNames.getFirst(), "Ex") >= 0);
    }

    @Test
    public void testImportTypeOptions()
    {
        String sampleTypeName = "ImportErrors";
        List<String> fieldNames = Arrays.asList("StringValue");

        log("Create a new sample type with a name");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition(sampleTypeName).addField(new FieldDefinition("StringValue", ColumnType.String)));

        log("Go to the sample type and add some data");
        clickAndWait(Locator.linkWithText(sampleTypeName));
        UpdateQueryRowPage updateQueryRowPage = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents")
            .clickInsertNewRow();
        updateQueryRowPage.setField("Name", "Name1");
        updateQueryRowPage.setField(fieldNames.getFirst(), "Bee");
        updateQueryRowPage.submit();

        log("Try to import overlapping data with TSV");

        DataRegionTable drt = sampleHelper.getSamplesDataRegionTable();
        ImportDataPage importDataPage = drt.clickImportBulkData();
        String header = "Name\t" + fieldNames.getFirst() + "\n";
        String overlap =  "Name1\tToBee\n";
        String newData = "Name2\tSee\n";
        importDataPage.setText(header + overlap + newData);
        Assertions.assertThat(importDataPage.submitExpectingError()).contains("duplicate key");

        log("Switch to 'Insert and Replace'");
        importDataPage.setCopyPasteMerge(true);
        importDataPage.submit();

        log("Validate data was updated and new data added");
        drt = sampleHelper.getSamplesDataRegionTable();
        assertEquals("Number of samples not as expected", 2, drt.getDataRowCount());

        int index = drt.getRowIndex("Name", "Name1");
        assertTrue("Should have row with first sample name", index >= 0);
        Map<String, String> rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.getFirst() + " for sample 'Name1' not as expected", "ToBee", rowData.get(fieldNames.getFirst()));

        index = drt.getRowIndex("Name", "Name2");
        assertTrue("Should have a row with the second sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.getFirst() + " for sample 'Name2' not as expected", "See", rowData.get(fieldNames.getFirst()));

        log("Try to import overlapping data from file");
        final File sampleData = TestFileUtils.getSampleData("simpleSampleType.xls");
        importDataPage = drt.clickImportBulkData();
        importDataPage.setFile(sampleData);
        final String errorText = importDataPage.submitExpectingError();
        Assert.assertTrue("Wrong error when importing duplicate samples. " + errorText, errorText.contains("duplicate key"));
        // TODO: Regression check for Issue 44202: Ugly error when data import fails due to duplicate key
        // Assert.assertTrue("Wrong error when importing duplicate samples. " + errorText, errorText.length() < 100);

        log ("Switch to 'Insert and Replace'");
        importDataPage.setFileMerge(true);
        importDataPage
                .setFile(sampleData)
                .submit();
        log ("Validate data was updated and new data added");
        assertEquals("Number of samples not as expected", 3, drt.getDataRowCount());

        index = drt.getRowIndex("Name", "Name1");
        assertTrue("Should have row with first sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.getFirst() + " for sample 'Name1' not as expected", "NotTwoBee", rowData.get(fieldNames.getFirst()));

        index = drt.getRowIndex("Name", "Name2");
        assertTrue("Should have a row with the second sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.getFirst() + " for sample 'Name2' not as expected", "Sea", rowData.get(fieldNames.getFirst()));

        index = drt.getRowIndex("Name", "Name3");
        assertTrue("Should have a row with the third sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.getFirst() + " for sample 'Name' not as expected", "Dee", rowData.get(fieldNames.getFirst()));
    }

    // I don't think this test is doing what was intended. I'm unclear if this is intended to be a lineage test or a
    // test of a sample type with a look-up column to another sample-type. It behaves as the latter, but that is not
    // working as expected, and the check at the end of the test fails to capture it.
    // I think this test should just be deleted.
    // Tracking in test issue: https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=40475
    // Marking as Ignore
    @Test
    @Ignore
    public void testSamplesWithLookups() throws IOException, CommandException
    {
        // create a basic sample type
        navigateToFolder(getProjectName(), LOOKUP_FOLDER);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", "sampleData", getCurrentContainerPath())
                .withColumns(List.of(
                        new FieldDefinition("name", ColumnType.String),
                        new FieldDefinition("strData", ColumnType.String),
                        new FieldDefinition("intData", ColumnType.Integer),
                        new FieldDefinition("floatData", ColumnType.Decimal)
                ));
        dgen.createDomain(createDefaultConnection(), SampleTypeAPIHelper.SAMPLE_TYPE_DOMAIN_KIND);
        dgen.addCustomRow(Map.of("name", "A", "strData", "argy", "intData", 6, "floatData", 2.5));
        dgen.addCustomRow(Map.of("name", "B", "strData", "bargy","intData", 7, "floatData", 3.5));
        dgen.addCustomRow(Map.of("name", "C", "strData", "foofoo","intData", 8, "floatData", 4.5));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

        // create the lookup sample type in a different folder- configured to look to the first one
        String lookupContainer = getProjectName() + "/" + LOOKUP_FOLDER;
        navigateToFolder(getProjectName(), FOLDER_NAME);
        // create another with a lookup to it
        TestDataGenerator lookupDgen = new TestDataGenerator("exp.materials", "sampleLookups", getCurrentContainerPath())
                .withColumns(List.of(
                        new FieldDefinition("name", ColumnType.String),
                        new FieldDefinition("strLookup", new LookupInfo(lookupContainer, "exp.materials", "sampleData")
                                .setTableType(ColumnType.String)),
                        new FieldDefinition("intLookup", new LookupInfo(lookupContainer, "exp.materials", "sampleData")
                                .setTableType(ColumnType.Integer)),
                        new FieldDefinition("floatLooky", new LookupInfo(lookupContainer, "exp.materials", "sampleData")
                                .setTableType(ColumnType.Decimal))
                ));
        lookupDgen.createDomain(createDefaultConnection(), SampleTypeAPIHelper.SAMPLE_TYPE_DOMAIN_KIND);
        lookupDgen.addCustomRow(Map.of("name", "B"));

        // If this is to be a look-up to another sample type I believe the values should be the row index and not the name.
        lookupDgen.addCustomRow(Map.of("strLookup", "B"));
        lookupDgen.addCustomRow(Map.of("intLookup", "B"));
        lookupDgen.addCustomRow(Map.of("floatLooky", "B"));
        lookupDgen.insertRows(createDefaultConnection(), dgen.getRows());

        refresh();
        DataRegionTable.DataRegion(getDriver()).withName(SampleTypeAPIHelper.SAMPLE_TYPE_DOMAIN_KIND).waitFor();
        waitAndClick(Locator.linkWithText("sampleLookups"));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        // This only checks the number of rows returned but does not check the values in the rows.
        assertEquals(3, materialsList.getDataRowCount());

        // Not sure why this is being deleted, it makes the test hard to debug.
        lookupDgen.deleteDomain(createDefaultConnection());
        dgen.deleteDomain(createDefaultConnection());
    }

    @Test
    public void testDeleteMultipleSamplesNoDependencies()
    {
        final String SAMPLE_TYPE_NAME = "DeleteIndependentSamples";
        List<String> sampleNames = Arrays.asList("I-1", "I-2", "I-3");
        List<Map<String, String>> sampleData = new ArrayList<>();
        sampleNames.forEach(name -> sampleData.add(Map.of("Name", name)));

        clickProject(PROJECT_NAME);
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition(SAMPLE_TYPE_NAME)
                        .setFields(List.of(FieldInfo.random("Field01").getFieldDefinition())),
                sampleData);

        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        log("Delete all the samples that have been created");
        sampleNames.forEach(name -> drtSamples.checkCheckbox(drtSamples.getRowIndex("Name", name)));
        sampleHelper.deleteSamples(drtSamples, "Permanently delete " + sampleNames.size() + " samples");
        assertEquals("Should have removed all the selected samples", 0, sampleHelper.getSamplesDataRegionTable().getDataRowCount());
    }

    @Test
    public void testDeleteSamplesSomeWithAssayData()
    {
        final PortalHelper portalHelper = new PortalHelper(this);
        final String SAMPLE_TYPE_NAME = "DeleteSamplesWithAssayData";
        final String SAMPLE_ID_FIELD_NAME = "sampleId";
        final String DATA_ID_ASSAY = "GPAT - SampleId Data";
        final String RUN_ID_ASSAY = "GPAT - SampleId Run";
        List<String> sampleNames = Arrays.asList("P-1", "P-2", "P-3", "P-4", "P-5");
        final String BATCH_SAMPLE_NAME = sampleNames.get(1);
        final String RUN_SAMPLE_NAME = sampleNames.get(2);

        int expectedSampleCount = sampleNames.size();

        final String SAMPLE_ID_TEST_RUN_DATA = SAMPLE_ID_FIELD_NAME + "\n" +
                sampleNames.get(0) + "\n" +
                sampleNames.get(3) + "\n" +
                sampleNames.get(1) + "\n";

        final String TEST_RUN_DATA = "specimenID\n" +
                "Specimen-01\n" +
                "Specimen-02\n" +
                "Specimen-03\n";

        List<Map<String, String>> sampleData = new ArrayList<>();
        sampleNames.forEach(name -> sampleData.add(Map.of("Name", name)));
        goToProjectHome();
        portalHelper.addWebPart("Assay List");
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        log("Create a sample type");
        sampleHelper.createSampleType(new SampleTypeDefinition(SAMPLE_TYPE_NAME), sampleData);

//  Note that we currently will not find runs where the batch id references a sampleId.  See Issue 37918.
//        log("Create an assay with sampleId in the batch fields");
//        goToProjectHome();
//        ReactAssayDesignerPage designerPage = _assayHelper.createAssayDesign("General", BATCH_ID_ASSAY);
//        designerPage.goToBatchFields()
//            .addField(SAMPLE_ID_FIELD_NAME)
//            .setType(FieldDefinition.ColumnType.Lookup)
//            .setFromSchema("samples")
//            .setFromTargetTable(SAMPLE_TYPE_NAME + " (Integer)");
//        designerPage.clickFinish();
//
//        log("Upload assay data for batch-level sampleId");
//        goToProjectHome();
//        clickAndWait(Locator.linkWithText("Assay List"));
//        clickAndWait(Locator.linkWithText(BATCH_ID_ASSAY));
//        clickButton("Import Data");
//        setFormElement(Locator.name(SAMPLE_ID_FIELD_NAME), BATCH_SAMPLE_NAME);
//        clickButton("Next");
//        setFormElement(AssayTest.TEXT_AREA_DATA_COLLECTOR_LOCATOR, TEST_RUN_DATA);
//        clickButton("Save and Finish");
//
//
//        log("Try to delete the sample referenced in the batch");
//        goToProjectHome();
//        click(Locator.linkWithText(SAMPLE_TYPE_NAME));
//        DataRegionTable sampleTable = sampleHelper.getSamplesDataRegionTable();
//        sampleTable.checkCheckbox(sampleTable.getIndexWhereDataAppears(BATCH_SAMPLE_NAME, "Name"));
//        sampleTable.clickHeaderButton("Delete");
//        Window.Window(getDriver()).withTitle("No samples can be deleted").waitFor()
//                .clickButton("Dismiss", true);

        log("Create an assay with sampleId in the data field");
        goToProjectHome();
        ReactAssayDesignerPage assayDesignerPage = _assayHelper.createAssayDesign("General", DATA_ID_ASSAY);
        assayDesignerPage.goToResultsFields()
            .addField(SAMPLE_ID_FIELD_NAME)
            .setType(ColumnType.Lookup)
            .setFromSchema("samples")
            .setFromTargetTable(SAMPLE_TYPE_NAME + " (Integer)");
        assayDesignerPage.clickFinish();

        log("Upload assay data referencing sampleId");
        clickAndWait(Locator.linkWithText(DATA_ID_ASSAY));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(AssayConstants.TEXT_AREA_DATA_COLLECTOR_LOCATOR, SAMPLE_ID_TEST_RUN_DATA);
        clickButton("Save and Finish");

        log("Try to delete all samples");
        goToProjectHome();
        click(Locator.linkWithText(SAMPLE_TYPE_NAME));
        DataRegionTable sampleTable = sampleHelper.getSamplesDataRegionTable();
        sampleTable.checkAllOnPage();
        sampleTable.clickHeaderButton("Delete");
        Window.Window(getDriver()).withTitle("Permanently delete 2 samples").waitFor()
                .clickButton("Cancel", true);
        log("Uncheck the ones that can be deleted and try to delete again");
        sampleTable.uncheckCheckbox(sampleTable.getRowIndex("Name", sampleNames.get(2)));
        sampleTable.uncheckCheckbox(sampleTable.getRowIndex("Name", sampleNames.get(4)));
        sampleTable.clickHeaderButton("Delete");
        Window.Window(getDriver()).withTitle("No samples can be deleted").waitFor()
                .clickButton("Dismiss", true);


        log("Create an assay with sampleId in the run fields");
        goToProjectHome();
        assayDesignerPage = _assayHelper.createAssayDesign("General", RUN_ID_ASSAY);
        assayDesignerPage.goToRunFields()
                .addField(SAMPLE_ID_FIELD_NAME)
                .setType(ColumnType.Lookup)
                .setFromSchema("samples")
                .setFromTargetTable(SAMPLE_TYPE_NAME + " (Integer)");
        assayDesignerPage.clickFinish();

        log("Upload assay data for run-level sampleId");
        clickAndWait(Locator.linkWithText(RUN_ID_ASSAY));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name(SAMPLE_ID_FIELD_NAME), RUN_SAMPLE_NAME);
        setFormElement(AssayConstants.TEXT_AREA_DATA_COLLECTOR_LOCATOR, TEST_RUN_DATA);
        clickButton("Save and Finish");

        log("Try to delete the sampleId referenced in the run field");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE_NAME));
        sampleTable = sampleHelper.getSamplesDataRegionTable();
        sampleTable.uncheckAllOnPage();
        sampleTable.checkCheckbox(sampleTable.getRowIndex("Name", RUN_SAMPLE_NAME));

        sampleTable.clickHeaderButton("Delete");
        Window.Window(getDriver()).withTitle("No samples can be deleted").waitFor()
                .clickButton("Dismiss", true);

        log("Delete the un-referenced samples");
        sampleTable.checkAllOnPage();
        sampleHelper.deleteSamples(sampleTable, "Permanently delete 1 sample");
        expectedSampleCount--;
        assertEquals("Number of samples not as expected after deletion", expectedSampleCount, sampleTable.getDataRowCount());

        log("Delete the assay run referencing the sample in run properties");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(RUN_ID_ASSAY));
        DataRegionTable runsTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Runs").find();
        runsTable.checkAllOnPage();
        runsTable.clickHeaderButton("Delete");
        clickButton("Confirm Delete");

        log("Now try to delete the sample that was referenced in the run properties");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE_NAME));
        sampleTable.uncheckAllOnPage();
        sampleTable.checkCheckbox(sampleTable.getRowIndex("Name", RUN_SAMPLE_NAME));
        sampleHelper.deleteSamples(sampleTable, "Permanently delete 1 sample");
        expectedSampleCount--;
        assertEquals("Number of samples not as expected after deletion", expectedSampleCount, sampleTable.getDataRowCount());

//        log("Delete the assay run referencing the sample in the batch properties");
//        goToProjectHome();
//        clickAndWait(Locator.linkWithText(BATCH_ID_ASSAY));
//        runsTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Runs").find();
//        runsTable.checkAllOnPage();
//        runsTable.clickHeaderButton("Delete");
//        clickButton("Confirm Delete");

//        log("Now try to delete the sample that was referenced in the batch properties, but still referenced in the data of another assay");
        log("Now try to delete the sample is referenced in the data of an assay");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE_NAME));
        sampleTable.uncheckAllOnPage();
        sampleTable.checkCheckbox(sampleTable.getRowIndex("Name", BATCH_SAMPLE_NAME));
        sampleTable.clickHeaderButton("Delete");
        Window.Window(getDriver()).withTitle("No samples can be deleted").waitFor()
                .clickButton("Dismiss", true);

        log("Delete the assay run referencing the samples in the data");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(DATA_ID_ASSAY));
        runsTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Runs").find();
        runsTable.checkAllOnPage();
        runsTable.clickHeaderButton("Delete");
        clickButton("Confirm Delete");

        log("Try to delete the rest of the samples");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE_NAME));
        sampleTable.checkAllOnPage();
        sampleHelper.deleteSamples(sampleTable, "Permanently delete 3 samples");
        assertEquals("Number of samples not as expected after deletion", 0, sampleTable.getDataRowCount());

    }

    @Test
    public void testUpdateAndDeleteWithCommentsAndFlags() throws IOException
    {
        final String SAMPLE_TYPE_NAME = "UpdateAndDeleteFields";
        final String SAMPLE_NAME_TO_DELETE = "ud01";
        final String SAMPLE_FLAG_UPDATE = "ud02";
        final String FLAG_UPDATE = "Updated Flag Value";
        final String SAMPLE_DESC_UPDATE = "ud03";
        final String DESC_UPDATE = "This is the updated description";
        final String SAMPLE_UPDATE_BOTH = "ud04";
        final String FLAG_UPDATE_1 = "New Flag Value";
        final String DESC_UPDATE_1 = "New description when one did not exist before.";
        final String FLAG_UPDATE_2 = "Flag Value Updated After Add";
        final String DESC_UPDATE_2 = "Updated description after adding a description.";
        FieldInfo field01 = FieldInfo.random("Field01", ColumnType.String);

        log("Validate that update and delete works correctly with the Comment and Flag fields.");

        clickProject(PROJECT_NAME);

        // Using Map.of() creates an immutable collection I want to be able to update these data/collection items.
        Map<String, String> descriptionUpdate = new HashMap<>();
        descriptionUpdate.put("Name", SAMPLE_DESC_UPDATE);
        descriptionUpdate.put(field01.getName(), "cc");
        descriptionUpdate.put("Description", "Here is the second description.");
        descriptionUpdate.put("Flag", "");

        Map<String, String> flagUpdate = new HashMap<>();
        flagUpdate.put("Name", SAMPLE_FLAG_UPDATE);
        flagUpdate.put(field01.getName(), "bb");
        flagUpdate.put("Description", "");
        flagUpdate.put("Flag", "Flag Value 2");

        Map<String, String> updateBoth = new HashMap<>();
        updateBoth.put("Name", SAMPLE_UPDATE_BOTH);
        updateBoth.put(field01.getName(), "dd");
        updateBoth.put("Description", "");
        updateBoth.put("Flag", "");

        Map<String, String> deleteSample = new HashMap<>();
        deleteSample.put("Name", SAMPLE_NAME_TO_DELETE);
        deleteSample.put(field01.getName(), "aa");
        deleteSample.put("Description", "This is description number 1.");
        deleteSample.put("Flag", "Flag Value 1");

        // Some extra samples not really sure I will need them.
        Map<String, String> canarySample01 = new HashMap<>();
        canarySample01.put("Name", "ud05");
        canarySample01.put(field01.getName(), "ee");
        canarySample01.put("Description", "This is description for sample 5.");
        canarySample01.put("Flag", "Flag Value 5");

        Map<String, String> canarySample02 = new HashMap<>();
        canarySample02.put("Name", "ud06");
        canarySample02.put(field01.getName(), "ff");
        canarySample02.put("Description", "This is description for sample 6.");
        canarySample02.put("Flag", "Flag Value 6");

        List<Map<String, String>> sampleData = new ArrayList<>();
        sampleData.add(deleteSample);
        sampleData.add(flagUpdate);
        sampleData.add(descriptionUpdate);
        sampleData.add(updateBoth);
        sampleData.add(canarySample01);
        sampleData.add(canarySample02);

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition(SAMPLE_TYPE_NAME)
                        .setFields(List.of(field01.getFieldDefinition())),
                sampleData);

        List<String> dbFieldsToCheck = Arrays.asList("Name", "Flag/Comment", field01.toString(), "Description");
        Map<String, String> fieldKeyMap = Map.of("Flag/Comment", "Flag", field01.toString(), field01.getName());
        List<Map<String, String>> resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck, fieldKeyMap);

        checker().fatal().verifyTrue("Newly inserted SampleType data not as expected. Fatal error.",
                areDataListEqual(resultsFromDB, sampleData));

        // Change the view so screen shot on failure is helpful.
        sampleHelper = new SampleTypeHelper(this);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        CustomizeView cv = drtSamples.openCustomizeGrid();
        cv.addColumn("Description");
        cv.saveCustomView();

        log("Delete a record that has a description and a flag/comment");
        int rowIndex = drtSamples.getRowIndexStrict("Name", SAMPLE_NAME_TO_DELETE);
        drtSamples.checkCheckbox(rowIndex);
        sampleHelper.deleteSamples(drtSamples, "Permanently delete 1 sample");

        // Remove the same row from the Sample Type input data.
        int testDataIndex = getSampleIndexFromTestInput(SAMPLE_NAME_TO_DELETE, sampleData);
        sampleData.remove(testDataIndex);

        log("Check that the Sample has been removed.");
        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck, fieldKeyMap);
        checker().verifyTrue("Sample Type data is not as expected after a delete.", areDataListEqual(resultsFromDB, sampleData));

        log("Now update a sample's description.");

        testDataIndex = getSampleIndexFromTestInput(SAMPLE_DESC_UPDATE, sampleData);
        sampleData.get(testDataIndex).replace("Description", DESC_UPDATE);

        updateSampleType(sampleData.get(testDataIndex));

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck, fieldKeyMap);
        checker().verifyTrue("Sample Type data is not as expected after a update of Description.", areDataListEqual(resultsFromDB, sampleData));

        log("Now delete the sample's description.");
        sampleData.get(testDataIndex).replace("Description", "");

        updateSampleType(sampleData.get(testDataIndex));

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck, fieldKeyMap);
        checker().verifyTrue("Sample Type data is not as expected after deleting the Description.", areDataListEqual(resultsFromDB, sampleData));

        log("Let's repeat it all again for a sample's flag/comment.");
        testDataIndex = getSampleIndexFromTestInput(SAMPLE_FLAG_UPDATE, sampleData);
        sampleData.get(testDataIndex).replace("Flag", FLAG_UPDATE);

        updateSampleType(sampleData.get(testDataIndex));

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck, fieldKeyMap);
        checker().verifyTrue("Sample Type data is not as expected after a update of Flag/Comment.", areDataListEqual(resultsFromDB, sampleData));

        log("Now delete the sample's Flag/Comment.");
        sampleData.get(testDataIndex).replace("Flag", "");

        updateSampleType(sampleData.get(testDataIndex));

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck, fieldKeyMap);
        checker().verifyTrue("Sample Type data is not as expected after deleting the Flag/Comment.", areDataListEqual(resultsFromDB, sampleData));

        log("Finally update and delete both flag and description for a sample.");
        testDataIndex = getSampleIndexFromTestInput(SAMPLE_UPDATE_BOTH, sampleData);
        sampleData.get(testDataIndex).replace("Flag", FLAG_UPDATE_1);
        sampleData.get(testDataIndex).replace("Description", DESC_UPDATE_1);

        updateSampleType(sampleData.get(testDataIndex));

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck, fieldKeyMap);
        checker().verifyTrue("Sample Type data is not as expected after a adding a Description and a Flag/Comment to an existing sample.",
                areDataListEqual(resultsFromDB, sampleData));

        log("Now update both values.");

        sampleData.get(testDataIndex).replace("Flag", FLAG_UPDATE_2);
        sampleData.get(testDataIndex).replace("Description", DESC_UPDATE_2);

        updateSampleType(sampleData.get(testDataIndex));

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck, fieldKeyMap);
        checker().verifyTrue("Sample Type data is not as expected after a updating both a Description and a Flag/Comment.",
                areDataListEqual(resultsFromDB, sampleData));

        log("Now delete both the Description and Flag/Comment from the sample.");
        sampleData.get(testDataIndex).replace("Flag", "");
        sampleData.get(testDataIndex).replace("Description", "");

        updateSampleType(sampleData.get(testDataIndex));

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck, fieldKeyMap);
        checker().verifyTrue("Sample Type data is not as expected after deleting the Description and Flag/Comment.",
                areDataListEqual(resultsFromDB, sampleData));

        // Check for Issue 40385: Can't Update Samples Using a File
        log("Now use a file import to update the samples.");

        for(Map<String, String> sample : sampleData)
        {
            String fieldValue = sample.get(field01.getName());
            sample.replace(field01.getName(), fieldValue.toUpperCase());
        }

        List<List<String>> fileData = new ArrayList<>();
        fileData.add(List.of("Name", field01.getLabel()));
        for(Map<String, String> sample : sampleData)
        {
            fileData.add(List.of(sample.get("Name"), sample.get(field01.getName())));
        }

        String fileName = "SampleTypeTest_UpdateSamples.tsv";
        if (!TestFileUtils.getTestTempDir().exists())
            TestFileUtils.getTestTempDir().mkdirs();
        File importFile = TestDataUtils.writeRowsToFile(fileName, fileData);

        sampleHelper.mergeImport(importFile);

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck, fieldKeyMap);
        checker().verifyTrue("SampleType data is not as expected after using a file to update samples..",
                areDataListEqual(resultsFromDB, sampleData));

    }

    private void updateSampleType(Map<String, String> updatedFields)
    {
        List<Map<String, String>> updateSampleData = new ArrayList<>();
        updateSampleData.add(updatedFields);

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.mergeImport(updateSampleData);

    }

    protected boolean areDataListEqual(List<Map<String, String>> list01, List<Map<String, String>> list02)
    {
        return areDataListEqual(list01, list02, true);
    }

    protected boolean areDataListEqual(List<Map<String, String>> list01, List<Map<String, String>> list02, boolean logMismatch)
    {
        if( list01.size() != list02.size())
            return false;

        // Order the two lists so compare can be done by index and not by searching the two lists.
        list01.sort(Comparator.comparing((Map<String, String> o) -> o.get("Name")));

        list02.sort(Comparator.comparing((Map<String, String> o) -> o.get("Name")));

        for(int i = 0; i < list01.size(); i++)
        {
            if(!list01.get(i).equals(list02.get(i)))
            {
                if(logMismatch)
                {
                    log("Found a mismatch in the lists.");
                    log("list01(" + i + "): " + list01.get(i));
                    log("list02(" + i + "): " + list02.get(i));
                }
                return false;
            }
        }

        return true;
    }

    protected int getSampleIndexFromTestInput(String sampleName, List<Map<String, String>> testData)
    {
        int index;
        for(index = 0; index < testData.size(); index++)
        {
            if(testData.get(index).get("Name").equalsIgnoreCase(sampleName))
                break;
        }

        if(index < testData.size())
            return index;

        Assert.fail("Ummm... I couldn't find a sample with the name '" + sampleName + "' in the test data, are you sure it should be there?");

        // Need this otherwise I get a red squiggly.
        return -1;

    }

    protected List<Map<String, String>> getSampleDataFromDB(String folderPath, String sampleTypeName, List<String> fields, Map<String, String> fieldKeyMap)
    {
        List<Map<String, String>> results = new ArrayList<>(6);
        Map<String, String> tempRow;

        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("samples", sampleTypeName);
        cmd.setColumns(fields);

        try
        {
            SelectRowsResponse response = cmd.execute(cn, folderPath);

            for (Map<String, Object> row : response.getRows())
            {

                tempRow = new HashMap<>();

                for(String key : row.keySet())
                {

                    if (fields.contains(key) || fieldKeyMap.containsValue(key))
                    {

                        String mappedKey = fieldKeyMap.getOrDefault(key, key);

                        if (null == row.get(key))
                        {
                            tempRow.put(mappedKey, "");
                        }
                        else
                        {
                            tempRow.put(mappedKey, row.get(key).toString());
                        }

                    }

                }

                results.add(tempRow);

            }

        }
        catch(CommandException | IOException excp)
        {
            Assert.fail(excp.getMessage());
        }

        return results;
    }

    @Test
    public void testMissingFieldIndicatorAndRequiredFields()
    {
        final String SAMPLE_TYPE_NAME = "MissingValues";
        final String INDICATOR_ONLY_SAMPLE_NAME = "mv02";
        final String VALUE_ONLY_SAMPLE_NAME = "mv04";
        final String BOTH_FIELDS_SAMPLE_NAME = "mv06";
        final String INCONSISTENT_SAMPLE_NAME = "mv07";
        final String UPDATE_SAMPLE_NAME = "mv08";

        final String REQUIRED_FIELD_NAME = "field01";
        final String MISSING_FIELD_NAME = "field02";
        final String INDICATOR_FIELD_NAME = MISSING_FIELD_NAME + "MVIndicator";

        log("Validate missing values and required fields in a Sample Type.");

        log("Create expected missing value indicators.");
        clickProject(PROJECT_NAME);

        final String MV_INDICATOR_01 = "Q";
        final String MV_DESCRIPTION_01 = "Data currently under quality control review.";
        final String MV_INDICATOR_02 = "N";
        final String MV_DESCRIPTION_02 = "Required field marked by site as 'data not available'.";
        final String MV_INDICATOR_03 = "X";
        final String MV_DESCRIPTION_03 = "Here is a non system one.";

        List<Map<String, String>> missingValueIndicators = new ArrayList<>();
        missingValueIndicators.add(Map.of("indicator", MV_INDICATOR_01, "description", MV_DESCRIPTION_01));
        missingValueIndicators.add(Map.of("indicator", MV_INDICATOR_02, "description", MV_DESCRIPTION_02));
        missingValueIndicators.add(Map.of("indicator", MV_INDICATOR_03, "description", MV_DESCRIPTION_03));

        setupMVIndicators(missingValueIndicators);

        clickProject(PROJECT_NAME);

        int expectedMissingCount = 0;
        List<Map<String, String>> sampleData = new ArrayList<>();

        Map<String, String> indicatorOnlySample = new HashMap<>();
        indicatorOnlySample.put("Name", INDICATOR_ONLY_SAMPLE_NAME);
        indicatorOnlySample.put(REQUIRED_FIELD_NAME, "bb_mv01");
        indicatorOnlySample.put(MISSING_FIELD_NAME, "");
        indicatorOnlySample.put(INDICATOR_FIELD_NAME, "Q");
        expectedMissingCount++;

        Map<String, String> valueOnlySample = new HashMap<>();
        valueOnlySample.put("Name", VALUE_ONLY_SAMPLE_NAME);
        valueOnlySample.put(REQUIRED_FIELD_NAME, "dd_mv01");
        valueOnlySample.put(MISSING_FIELD_NAME, "X");
        valueOnlySample.put(INDICATOR_FIELD_NAME, "");
        expectedMissingCount++;

        Map<String, String> bothFieldsSample = new HashMap<>();
        bothFieldsSample.put("Name", BOTH_FIELDS_SAMPLE_NAME);
        bothFieldsSample.put(REQUIRED_FIELD_NAME, "ff_mv01");
        bothFieldsSample.put(MISSING_FIELD_NAME, "N");
        bothFieldsSample.put(INDICATOR_FIELD_NAME, "N");
        expectedMissingCount++;

        // This may actually be a redundant test case. It is basically the same as the "both" test case.
        Map<String, String> inconsistentSample = new HashMap<>();
        inconsistentSample.put("Name", INCONSISTENT_SAMPLE_NAME);
        inconsistentSample.put(REQUIRED_FIELD_NAME, "gg_mv01");
        inconsistentSample.put(MISSING_FIELD_NAME, "Here is a valid string value.");
        inconsistentSample.put(INDICATOR_FIELD_NAME, "Q");
        expectedMissingCount++;

        Map<String, String> updateSample = new HashMap<>();
        updateSample.put("Name", UPDATE_SAMPLE_NAME);
        updateSample.put(REQUIRED_FIELD_NAME, "hh_mv01");
        updateSample.put(MISSING_FIELD_NAME, "X");
        updateSample.put(INDICATOR_FIELD_NAME, "X");
        expectedMissingCount++;

        sampleData.add(Map.of("Name", "mv01", REQUIRED_FIELD_NAME, "aa_mv01", MISSING_FIELD_NAME, "This value is here.", INDICATOR_FIELD_NAME, ""));
        sampleData.add(indicatorOnlySample);
        sampleData.add(Map.of("Name", "mv03", REQUIRED_FIELD_NAME, "cc_mv01", MISSING_FIELD_NAME, "Just to break things up.", INDICATOR_FIELD_NAME, ""));
        sampleData.add(valueOnlySample);
        sampleData.add(Map.of("Name", "mv05", REQUIRED_FIELD_NAME, "ee_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, ""));
        sampleData.add(bothFieldsSample);
        sampleData.add(inconsistentSample);
        sampleData.add(updateSample);

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        List<FieldDefinition> fields = new ArrayList<>();
        fields.add(new FieldDefinition(REQUIRED_FIELD_NAME, ColumnType.String)
                .setMvEnabled(false)
                .setRequired(true));
        fields.add(new FieldDefinition(MISSING_FIELD_NAME, ColumnType.String)
                .setMvEnabled(true)
                .setRequired(false));
        SampleTypeDefinition def = new SampleTypeDefinition(SAMPLE_TYPE_NAME).setFields(fields);
        sampleHelper.createSampleType(def);
        sampleHelper.goToSampleType(SAMPLE_TYPE_NAME);
        sampleHelper.bulkImport(sampleData);

        // Change the view so the missing value indicator is there and for the screen shot is useful on failure.
        sampleHelper = new SampleTypeHelper(this);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        CustomizeView cv = drtSamples.openCustomizeGrid();
        cv.showHiddenItems();
        cv.addColumn(INDICATOR_FIELD_NAME);
        cv.saveCustomView();

        List<Map<String, String>> resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, Arrays.asList("Name", REQUIRED_FIELD_NAME, MISSING_FIELD_NAME, INDICATOR_FIELD_NAME), Map.of());

        // After doing a bulk upload it looks like the value field is stored as an empty field in the DB.
        // Need to update the sample data to reflect what is expected from the DB.
        int testDataIndex = getSampleIndexFromTestInput(VALUE_ONLY_SAMPLE_NAME, sampleData);
        sampleData.get(testDataIndex).replace(INDICATOR_FIELD_NAME, sampleData.get(testDataIndex).get(MISSING_FIELD_NAME));
        sampleData.get(testDataIndex).replace(MISSING_FIELD_NAME, "");

        testDataIndex = getSampleIndexFromTestInput(BOTH_FIELDS_SAMPLE_NAME, sampleData);
        sampleData.get(testDataIndex).replace(MISSING_FIELD_NAME, "");

        testDataIndex = getSampleIndexFromTestInput(INCONSISTENT_SAMPLE_NAME, sampleData);
        sampleData.get(testDataIndex).replace(MISSING_FIELD_NAME, "");

        testDataIndex = getSampleIndexFromTestInput(UPDATE_SAMPLE_NAME, sampleData);
        sampleData.get(testDataIndex).replace(MISSING_FIELD_NAME, "");

        checker().fatal().verifyTrue("Newly inserted sample type data not as expected. Fatal error.",
                areDataListEqual(resultsFromDB, sampleData));

        checker().verifyEquals("Number of missing value UI indicators is not as expected.",
                Locator.xpath("//td[contains(@class, 'labkey-mv-indicator')]").findElements(getDriver()).size(),
                expectedMissingCount);

        log("Now update a sample (give a value in the missing value field) and validate.");
        final String UPDATED_VALUE = "This should remove the unknown value indicator.";
        testDataIndex = getSampleIndexFromTestInput(UPDATE_SAMPLE_NAME, sampleData);
        sampleData.get(testDataIndex).replace(MISSING_FIELD_NAME, UPDATED_VALUE);
        sampleData.get(testDataIndex).replace(INDICATOR_FIELD_NAME, "");

        // TODO: Need to pass in all of the columns so as not to lose any data. See TODO comment below.
        List<Map<String, String>> updateSampleData = new ArrayList<>();
        updateSampleData.add(sampleData.get(testDataIndex));
        sampleHelper.mergeImport(updateSampleData);
        expectedMissingCount--;

        // TODO: Need to revisit. When doing a bulk update if a field is missing the update views it as a request to
        //  set the value to empty. Why not view this as make no changes to the field value? And if we want to set
        //  the field to empty add the column to the update but give no value.
        // The commented out code below does this (set only the column I want to update.
//        Map<String, String> tempSample = new HashMap<>();
//        tempSample.put("Name", UPDATE_SAMPLE_NAME);
//        tempSample.put(MISSING_FIELD_NAME, UPDATED_VALUE);
//
//        List<Map<String, String>> updateSampleData = new ArrayList<>();
//        updateSampleData.add(tempSample);
//
//        sampleHelper.bulkImport(updateSampleData, SampleTypeHelper.MERGE_DATA_LABEL);

        checker().verifyEquals("After updating a value the number of missing UI indicators is not as expected.",
                Locator.xpath("//td[contains(@class, 'labkey-mv-indicator')]").findElements(getDriver()).size(),
                expectedMissingCount);

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, Arrays.asList("Name", REQUIRED_FIELD_NAME, MISSING_FIELD_NAME, INDICATOR_FIELD_NAME), Map.of());

        checker().verifyTrue("After updating a value the data in the DB is not as expected.",
                areDataListEqual(resultsFromDB, sampleData));

        // Not really sure this is useful, we can remove in the future.
        log("Validate that the help div is shown when mouse over a missing value.");
        mouseOver(Locator.linkWithText(MV_INDICATOR_03));
        sleep(500);
        checker().verifyTrue(
                String.format("Expected a value pop-up helper (div control) with the text '%s'.",
                        MV_DESCRIPTION_03),
                isElementVisible(Locator.xpath("//span[@id='helpDivBody'][text()='" + MV_DESCRIPTION_03 + "']")));

        log("Now add a single sample via the UI");
        final String UI_INSERT_SAMPLE_NAME = "mv09";
        final String UI_STATIC_FIELD_TEXT = "This sample was added from the UI.";

        DataRegionTable drt = sampleHelper.getSamplesDataRegionTable();
        UpdateQueryRowPage updateQueryRowPage = drt.clickInsertNewRow();

        updateQueryRowPage.setField("Name", UI_INSERT_SAMPLE_NAME);
        updateQueryRowPage.setField(REQUIRED_FIELD_NAME, UI_STATIC_FIELD_TEXT);
        updateQueryRowPage.setField(INDICATOR_FIELD_NAME, OptionSelect.SelectOption.valueOption(MV_INDICATOR_03));
        updateQueryRowPage.submit();
        expectedMissingCount++;

        // Add this element to expected sample data.
        sampleData.add(Map.of("Name", UI_INSERT_SAMPLE_NAME, REQUIRED_FIELD_NAME, UI_STATIC_FIELD_TEXT, MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, MV_INDICATOR_03));

        checker().verifyEquals("After adding a sample with a missing value through the UI the number of missing UI indicators is not as expected.",
                Locator.xpath("//td[contains(@class, 'labkey-mv-indicator')]").findElements(getDriver()).size(),
                expectedMissingCount);

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, Arrays.asList("Name", REQUIRED_FIELD_NAME, MISSING_FIELD_NAME, INDICATOR_FIELD_NAME), Map.of());

        checker().verifyTrue("After adding a sample with a missing value through the UI the data in the DB is not as expected.",
                areDataListEqual(resultsFromDB, sampleData));

        log("Validate that the required field check works as expected.");
        updateSampleData = new ArrayList<>();
        updateSampleData.add(Map.of("Name", "mv10", REQUIRED_FIELD_NAME, "", MISSING_FIELD_NAME, "There should be no value in the required field.", INDICATOR_FIELD_NAME, ""));
        String error = sampleHelper.bulkImportExpectingError(updateSampleData, SampleTypeHelper.IMPORT_OPTION);

        checker().wrapAssertion(() -> Assertions.assertThat(error).as("Import error").contains("Missing value for required property"));
        clickButton("Cancel");

        log("Now validate that adding a single row from the UI has the same behavior.");
        final String UI_MISSING_REQ_SAMPLE_NAME = "mv10";
        final String UI_MISSING_FIELD_TEXT = "This should generate an error.";
        drt = sampleHelper.getSamplesDataRegionTable();
        updateQueryRowPage = drt.clickInsertNewRow();

        updateQueryRowPage.setField("Name", UI_MISSING_REQ_SAMPLE_NAME);
        updateQueryRowPage.setField(MISSING_FIELD_NAME, UI_MISSING_FIELD_TEXT);
        updateQueryRowPage.submit();

        try
        {
            waitForElementToBeVisible(Locator.xpath("//span[contains(@class, 'help-block')]/font[@class='labkey-error'][text()='This field is required']"));
            clickButton("Cancel");
        }
        catch(NoSuchElementException nse)
        {
            checker().error("No error message was shown when a required field is missing in the UI.");
        }

        // How about automation that updates an existing field?

        log("All done.");
    }

    private void setupMVIndicators(List<Map<String, String>> missingValueIndicators)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Missing Values"));
        uncheckCheckbox(Locator.checkboxById("inherit"));

        // Delete all site-level settings
        for (WebElement deleteButton : Locator.tagWithAttribute("img", "alt", "delete").findElements(getDriver()))
        {
            deleteButton.click();
            shortWait().until(ExpectedConditions.stalenessOf(deleteButton));
        }

        for(int index = 0; index < missingValueIndicators.size(); index++)
        {
            clickButton("Add", 0);
            WebElement mvInd = Locator.css("#mvIndicatorsDiv input[name=mvIndicators]").index(index).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
            setFormElement(mvInd, missingValueIndicators.get(index).get("indicator"));
            WebElement mvLabel = Locator.css("#mvIndicatorsDiv input[name=mvLabels]").index(index).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
            setFormElement(mvLabel, missingValueIndicators.get(index).get("description"));
        }
        clickButton("Save");
    }

    @Test
    public void testAuditLog()
    {
        String sampleTypeName = "TestAuditLogSampleType";
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        SampleTypeHelper helper = new SampleTypeHelper(this);
        helper.createSampleType(new SampleTypeDefinition(sampleTypeName).setFields(
                List.of(
                        new FieldDefinition("First", ColumnType.String),
                        new FieldDefinition("Second", ColumnType.Integer))),
                "Name\tFirst\tSecond\n" +
                        "Audit-1\tsome\t100");

        goToModule("Query");
        viewQueryData("auditLog", "SampleSetAuditEvent");
        assertTextPresent(
                "Samples inserted in: " + sampleTypeName);

    }

    @Test
    public void testSampleTypeNames()
    {
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        // make sure we are case-sensitive when creating sample types -- regression coverage for issue 33743
        clickProject(PROJECT_NAME);
        sampleHelper.createSampleType(new SampleTypeDefinition(CASE_INSENSITIVE_SAMPLE_TYPE));

        log("Creating sample type with same name but different casing should fail");
        clickProject(PROJECT_NAME);
        List<String> errors = sampleHelper
                .goToCreateNewSampleType()
                .setName(LOWER_CASE_SAMPLE_TYPE)
                .clickSaveExpectingErrors();
        assertEquals("Sample Type creation error", Arrays.asList("A Sample Type with name '" + LOWER_CASE_SAMPLE_TYPE + "' already exists."), errors);
        clickProject(PROJECT_NAME);
        assertElementPresent(Locator.linkWithText(CASE_INSENSITIVE_SAMPLE_TYPE));
        assertElementNotPresent(Locator.linkWithText(LOWER_CASE_SAMPLE_TYPE));

        log("Sample type can be renamed");
        goToProjectHome();
        final String anotherSampleType = "AnotherSampleType";
        sampleHelper.createSampleType(new SampleTypeDefinition(anotherSampleType));

        final String updatedSampleType = "UpdatedSampleType";
        goToProjectHome();
        UpdateSampleTypePage updatePage = sampleHelper.goToEditSampleType(anotherSampleType);
        updatePage.setName(updatedSampleType).clickSave();

        log("Sample type cannot be renamed to an existing name");
        goToProjectHome();
        updatePage = sampleHelper.goToEditSampleType(updatedSampleType);
        updatePage.setName(UPPER_CASE_SAMPLE_TYPE);
        assertTrue("Sample type rename conflict error",
                updatePage.clickSaveExpectingErrors().contains("A Sample Type with name '" + UPPER_CASE_SAMPLE_TYPE + "' already exists."));
        updatePage.clickCancel();
    }

    @Test
    public void testReservedFieldNames()
    {
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        clickProject(PROJECT_NAME);
        CreateSampleTypePage createPage = sampleHelper
            .goToCreateNewSampleType()
            .setName("ReservedFieldNameValidation");

        DomainFormPanel domainFormPanel = createPage.getFieldsPanel();

        log("Verify error message for reserved field names");
        domainFormPanel.manuallyDefineFields("created");
        checker().verifyEquals("Sample Type reserved field name error",
                Arrays.asList("'created' is a reserved field name in 'ReservedFieldNameValidation'.",
                        "Please correct errors in Fields before saving."),
                createPage.clickSaveExpectingErrors());
        domainFormPanel.removeAllFields(false);

        domainFormPanel.manuallyDefineFields("rowid");
        checker().verifyEquals("Sample Type reserved field name error",
                Arrays.asList("'rowid' is a reserved field name in 'ReservedFieldNameValidation'.",
                        "Please correct errors in Fields before saving."),
                createPage.clickSaveExpectingErrors());
        domainFormPanel.removeAllFields(false);

        log("Verify error message for a few other special field names");
        domainFormPanel.manuallyDefineFields("name");
        checker().verifyEquals("Sample Type 'name' field name error",
                Arrays.asList("The field name 'Name' is already taken. Please provide a unique name for each field.",
                        "Please correct errors in Fields before saving."),
                createPage.clickSaveExpectingErrors());
        domainFormPanel.removeAllFields(false);

        log("Verify error message for a few other special field names");
        domainFormPanel.manuallyDefineFields("sampleid");
        checker().verifyEquals("Sample Type SampleId field name error",
                Arrays.asList("The SampleID field name is reserved for imported or generated sample ids."),
                createPage.clickSaveExpectingErrors());
    }

    @Test
    public void testIgnoreReservedFieldNames() throws Exception
    {
        final String expectedInfoMsg = BaseDomainDesigner.RESERVED_FIELDS_WARNING_PREFIX +
                "These fields are already used by LabKey to support this sample type: " +
                "Name, Created, createdBy, Modified, modifiedBy, container, SampleId, created, createdby, modified, modifiedBy, Container, SampleID.";

        List<String> lines = new ArrayList<>();
        lines.add("Name,TextField1,DecField1,DateField1,Created,createdBy,Modified,modifiedBy,container,SampleId,created,createdby,modified,modifiedBy,Container,SampleID");

        if (!TestFileUtils.getTestTempDir().exists())
            FileUtils.forceMkdir(TestFileUtils.getTestTempDir());
        File inferenceFile = TestFileUtils.writeTempFile("InferFieldsForSampleType.csv", String.join(System.lineSeparator(), lines));

        log("Create a Sample Type.");
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        clickProject(PROJECT_NAME);
        String name = "Infer Fields";
        CreateSampleTypePage createPage = sampleHelper
                .goToCreateNewSampleType()
                .setName(name);

        log("Infer fields from a file that contains some reserved fields.");

        DomainFormPanel domainForm = createPage
                .getFieldsPanel()
                .setInferFieldFile(inferenceFile);
        checker().verifyThat("Reserved field warning not as expected", domainForm.getPanelAlertTexts(), CoreMatchers.hasItem(expectedInfoMsg));
        createPage.clickSave();
        DataRegionTable drt = DataRegion(getDriver()).find();
        checker().verifyTrue("Sample type not found in list of sample types", drt.getColumnDataAsText("Name").contains(name));

        log("End of test.");
    }

    @Test
    public void testLookUpValidatorForSampleTypes()
    {
        final String SAMPLE_TYPE= "Sample with lookup validator";
        final String listName = "Fruits from Excel";
        final String lookupColumnLabel = "Label for lookup column";

        log("Infer from excel file, then import data");
        _listHelper.createListFromFile(getProjectName(), listName, TestFileUtils.getSampleData("dataLoading/excel/fruits.xls"));
        _listHelper.goToList(listName);
        waitForElement(Locator.linkWithText("pomegranate"));
        assertNoLabKeyErrors();
        int listRowCount = new DataRegionTable.DataRegionFinder(getDriver()).withName("query")
                .find()
                .getDataRowCount();

        goToProjectHome();
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        SampleTypeDefinition definition = new SampleTypeDefinition(SAMPLE_TYPE);
        definition.addField(new FieldDefinition("Key",
                new FieldDefinition.IntLookup(null, "lists", listName)).setLabel(lookupColumnLabel).setLookupValidatorEnabled(true));
        sampleHelper.createSampleType(definition);

        goToProjectHome();
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE));
        DataRegionTable table = sampleHelper.getSamplesDataRegionTable();
        UpdateQueryRowPage updateQueryRowPage = table.clickInsertNewRow();

        updateQueryRowPage.setField("Name","1");
        updateQueryRowPage.setField("Key", OptionSelect.SelectOption.textOption("apple"));
        updateQueryRowPage.submit();

        assertEquals("Single row inserted",1, table.getDataRowCount());
        assertElementPresent(Locator.linkWithText("apple"));

        String missingPk = String.valueOf(listRowCount + 1);
        String tsvString =
                "Name\tKey\n" +
                "2\t" + missingPk;
        ImportDataPage importDataPage = table.clickImportBulkData();
        importDataPage.setText(tsvString);
        importDataPage.submitExpectingError("Value '" + missingPk + "' was not present in lookup target 'lists." + listName + "' for field '" + lookupColumnLabel + "'");
    }

    @Test
    public void testFileAttachment()
    {
        File experimentFilePath = TestFileUtils.getSampleData("fileTypes/xml_sample.xml");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);

        String sampleTypeName = "FileAttachmentSampleType";
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition(sampleTypeName).setFields(
                List.of(new FieldDefinition("OtherProp", ColumnType.String),
                        new FieldDefinition("FileAttachment", ColumnType.File))),
                "Name\tOtherProp\n" +
                        "FA-1\tOne\n" +
                        "FA-2\tTwo\n");

        Set<String> expectedHeaders = new HashSet<>();
        expectedHeaders.add("Name");
        expectedHeaders.add("Expiration Date");
        expectedHeaders.add("Flag");
        expectedHeaders.add("Other Prop");
        expectedHeaders.add("File Attachment");
        expectedHeaders.add("Amount");
        expectedHeaders.add("Units");

        setFileAttachment(0, experimentFilePath);
        setFileAttachment(1, TestFileUtils.getSampleData( "RawAndSummary~!@#$%^&()_+-[]{};',..xlsx"));

        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        UpdateQueryRowPage updateQueryRowPage = drt.clickInsertNewRow();
        updateQueryRowPage.setField("Name", "SampleTypeInsertedManually");
        updateQueryRowPage.setField("FileAttachment", experimentFilePath);
        updateQueryRowPage.submit();
        //a double upload causes the file to be appended with a count
        assertTextPresent("xml_sample-1.xml");
        int attachIndex = drt.getColumnIndex("File Attachment");

        // Added these last two test to check for regressions with exporting a grid with a file attachment column and deleting a file attachment column.
        exportGridWithAttachment(3, expectedHeaders, attachIndex, "xml_sample-1.xml", "xml_sample.xml", "rawandsummary~!@#$%^&()_+-[]{};',..xlsx");

        log("Remove the attachment columns and validate that everything still works.");
        clickFolder(FOLDER_NAME);
        UpdateSampleTypePage domainDesignerPage = sampleHelper.goToEditSampleType(sampleTypeName);
        domainDesignerPage.getFieldsPanel().removeField("FileAttachment", true);
        domainDesignerPage.clickSave();

        expectedHeaders.remove("File Attachment");
        exportGridVerifyRowCountAndHeader(3, expectedHeaders);
    }

    @Test // Issue 49830
    public void testFilePathOnBulkImport()
    {
        new ApiPermissionsHelper(this)
                .setSiteRoleUserPermissions(PasswordUtil.getUsername(), "See Absolute File Paths");

        goToProjectHome();

        String fileFieldName = "FileField";
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        String sampleTypeNameHome = "FilePathValidationHome";
        sampleHelper.createSampleType(new SampleTypeDefinition(sampleTypeNameHome).setFields(
                List.of(new FieldDefinition(fileFieldName, ColumnType.File))
        ));

        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);

        String sampleTypeNameSub = "FilePathValidationSub";
        sampleHelper.createSampleType(new SampleTypeDefinition(sampleTypeNameSub).setFields(
            List.of(new FieldDefinition(fileFieldName, ColumnType.File))
        ));

        // add a file system file that isn't under the current container dir, i.e. in the parent dir
        goToProjectHome();
        goToModule("FileContent");

        String testFileHomeName = "Update_Lineage_A.tsv";
        String testFileHomeNameB = "Update_Lineage_B.tsv";
        String homeFileDirectory = "homeDir1";
        _fileBrowserHelper.uploadFile(TestFileUtils.getSampleData(testFileHomeName));
        _fileBrowserHelper.uploadFile(TestFileUtils.getSampleData(testFileHomeNameB));
        _fileBrowserHelper.createFolder(homeFileDirectory);
        FileBrowserHelper.FileDetailInfo homeFileInfo = FileBrowserHelper.getFileDetailInfo(PROJECT_NAME, testFileHomeName);
        FileBrowserHelper.FileDetailInfo homeFileBInfo = FileBrowserHelper.getFileDetailInfo(PROJECT_NAME, testFileHomeNameB);
        FileBrowserHelper.FileDetailInfo homeDirInfo = FileBrowserHelper.getFileDetailInfo(PROJECT_NAME, homeFileDirectory);

        String folderContainerPath = PROJECT_NAME + "/" + FOLDER_NAME;
        String testFileSubName = "sampleType.tsv";
        String subFileDirectory = "subDir1";
        goToProjectFolder(PROJECT_NAME, FOLDER_NAME);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(TestFileUtils.getSampleData(testFileSubName));
        _fileBrowserHelper.createFolder(subFileDirectory);
        FileBrowserHelper.FileDetailInfo subFileInfo = FileBrowserHelper.getFileDetailInfo(folderContainerPath, testFileSubName);
        FileBrowserHelper.FileDetailInfo subDirInfo = FileBrowserHelper.getFileDetailInfo(folderContainerPath, subFileDirectory);

        goToProjectHome();
        clickAndWait(Locator.linkWithText(sampleTypeNameHome));
        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        var importDataPage = drt.clickImportBulkData();

        // error cases for home sample type:
        // importing directory that does exist under current project root into project
        importSampleTypeFilePathDataError("Fail", homeDirInfo.absoluteFilePath());
        importSampleTypeFilePathDataError("Fail", homeDirInfo.webDavUrl());
        importSampleTypeFilePathDataError("Fail", homeDirInfo.dataFileUrl());
        importSampleTypeFilePathDataError("Fail", homeDirInfo.webDavUrlRelative());
        importSampleTypeFilePathDataError("Fail", homeDirInfo.fileName());
        // importing directory that's not under current project root
        importSampleTypeFilePathDataError("Fail", "/");
        importSampleTypeFilePathDataError("Fail", "../");
        importSampleTypeFilePathDataError("Fail", "../@files");
        importSampleTypeFilePathDataError("Fail", subDirInfo.absoluteFilePath());
        importSampleTypeFilePathDataError("Fail", subDirInfo.webDavUrl());
        importSampleTypeFilePathDataError("Fail", subDirInfo.dataFileUrl());
        importSampleTypeFilePathDataError("Fail", subDirInfo.webDavUrlRelative());
        // importing file that does exist, but not under current root
        importSampleTypeFilePathDataError("Fail", subFileInfo.absoluteFilePath());
        importSampleTypeFilePathDataError("Fail", subFileInfo.webDavUrl());
        importSampleTypeFilePathDataError("Fail", subFileInfo.dataFileUrl());
        importSampleTypeFilePathDataError("Fail", "../" + FOLDER_NAME + "/@files/" + subDirInfo.webDavUrlRelative());
        // importing file that does not exist
        importSampleTypeFilePathDataError("Fail", homeFileInfo.absoluteFilePath() + "bad");
        importSampleTypeFilePathDataError("Fail", homeFileInfo.webDavUrl() + "bad");
        importSampleTypeFilePathDataError("Fail", homeFileInfo.dataFileUrl() + "bad");
        importSampleTypeFilePathDataError("Fail", homeFileInfo.webDavUrlRelative() + "bad");
        importSampleTypeFilePathDataError("Fail", homeFileInfo.fileName() + "bad");
        // happy cases: create new records using valid relative or absolute file in Project/Child
        List<String> header = List.of("Name", fileFieldName);
        List<List<String>> homeSampleContent = List.of(
            header,
            List.of("S-home-fullPath", homeFileInfo.absoluteFilePath()),
            List.of("S-home-relativeDav", homeFileInfo.webDavUrlRelative()),
            List.of("S-home-dataUrl", homeFileInfo.dataFileUrl()),
            List.of("S-home-davUrl", homeFileInfo.webDavUrl()),
            List.of("S-home-relative", "../@files/" + homeFileInfo.fileName()));
        importDataPage.setText(TestDataUtils.stringFromRows(homeSampleContent));
        importDataPage.submit();
        drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        String fName = " " + homeFileInfo.fileName();
        checker().verifyEqualsSorted("File field not imported as expected", List.of(fName, fName, fName, fName, fName), drt.getColumnDataAsText(fileFieldName));
        // error case for update
        importDataPage = drt.clickImportBulkData();
        importDataPage.setCopyPasteMerge(false, true);
        importSampleTypeFilePathDataError("S-home-fullPath", homeDirInfo.absoluteFilePath());
        importSampleTypeFilePathDataError("S-home-fullPath", homeDirInfo.fileName());
        importSampleTypeFilePathDataError("S-home-fullPath", "../");
        importSampleTypeFilePathDataError("S-home-fullPath", subDirInfo.webDavUrl());
        importSampleTypeFilePathDataError("S-home-fullPath", subDirInfo.dataFileUrl());
        importSampleTypeFilePathDataError("S-home-fullPath", homeFileInfo.absoluteFilePath() + "bad");
        // happy cases for update
        importDataPage.setText(TestDataUtils.stringFromRows(homeSampleContent)); // no change
        importDataPage.submit();
        drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        checker().verifyEqualsSorted("File field not imported as expected", List.of(fName, fName, fName, fName, fName), drt.getColumnDataAsText(fileFieldName));
        importDataPage = drt.clickImportBulkData();
        importDataPage.setCopyPasteMerge(false, true);
        List<List<String>> homeSampleUpdateContent = List.of(
            header,
            List.of("S-home-fullPath", homeFileBInfo.absoluteFilePath()),
            List.of("S-home-relativeDav", ""),
            List.of("S-home-dataUrl", homeFileBInfo.dataFileUrl()),
            List.of("S-home-davUrl", homeFileBInfo.webDavUrl()),
            List.of("S-home-relative", "../@files/" + homeFileBInfo.fileName()));
        importDataPage.setText(TestDataUtils.stringFromRows(homeSampleUpdateContent));
        importDataPage.submit();
        String fNameUpdated = " " + homeFileBInfo.fileName();
        drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        checker().verifyEqualsSorted("File field not imported as expected", List.of(fNameUpdated, fNameUpdated, fNameUpdated, " "/*removed*/, fNameUpdated), drt.getColumnDataAsText(fileFieldName));
        // error case for merge
        importDataPage = drt.clickImportBulkData();
        importDataPage.setCopyPasteMerge(true, true);
        importSampleTypeFilePathDataError("S-home-fullPath", homeDirInfo.absoluteFilePath());
        importSampleTypeFilePathDataError("S-home-fullPath", subDirInfo.webDavUrl());
        importSampleTypeFilePathDataError("Bad", subDirInfo.webDavUrlRelative());
        // happy case for merge
        List<List<String>> homeSampleMergeContent = new ArrayList<>(homeSampleContent);
        homeSampleMergeContent.add(List.of("S-home-merge1", "../@files/" + homeFileBInfo.fileName()));
        importDataPage.setText(TestDataUtils.stringFromRows(homeSampleMergeContent));
        importDataPage.submit();
        drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        checker().verifyEqualsSorted("File field not imported as expected", List.of(fNameUpdated, fName, fName, fName, fName, fName), drt.getColumnDataAsText(fileFieldName));

        // error cases for child sample type
        goToProjectFolder(PROJECT_NAME, FOLDER_NAME);
        clickAndWait(Locator.linkWithText(sampleTypeNameSub));
        drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        importDataPage = drt.clickImportBulkData();
        // import data in subfolder with home folder file absolute path, or invalid relative path, or directory
        importSampleTypeFilePathDataError("Fail", homeFileInfo.absoluteFilePath());
        importSampleTypeFilePathDataError("Fail", homeFileInfo.webDavUrl());
        importSampleTypeFilePathDataError("Fail", homeFileInfo.dataFileUrl());
        importSampleTypeFilePathDataError("Fail", "../" + testFileHomeName);
        importSampleTypeFilePathDataError("Fail", "../../" + testFileHomeName);
        importSampleTypeFilePathDataError("Fail", "../");
        importSampleTypeFilePathDataError("Fail", "../../@files");
        importSampleTypeFilePathDataError("Fail", "../../@files/" + homeFileDirectory);
        importSampleTypeFilePathDataError("Fail", homeDirInfo.absoluteFilePath());
        importSampleTypeFilePathDataError("Fail", homeDirInfo.webDavUrl());
        importSampleTypeFilePathDataError("Fail", homeDirInfo.dataFileUrl());
        // import data in subfolder with directory that's under current root
        importSampleTypeFilePathDataError("Fail", subDirInfo.absoluteFilePath());
        importSampleTypeFilePathDataError("Fail", subDirInfo.webDavUrl());
        importSampleTypeFilePathDataError("Fail", subDirInfo.dataFileUrl());
        importSampleTypeFilePathDataError("Fail", subDirInfo.webDavUrlRelative());
        importSampleTypeFilePathDataError("Fail", subDirInfo.fileName());
        // happy case for creating child sample
        List<List<String>> childSampleContent = List.of(
            header,
            List.of("S-child-fullPath", subFileInfo.absoluteFilePath()),
            List.of("S-child-relativeDav", subFileInfo.webDavUrlRelative()),
            List.of("S-child-dataUrl", subFileInfo.dataFileUrl()),
            List.of("S-child-davUrl", subFileInfo.webDavUrl()),
            List.of("S-child-relative", "../@files/" + subFileInfo.fileName()));
        importDataPage.setText(TestDataUtils.stringFromRows(childSampleContent));
        importDataPage.submit();
        drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        fName = " " + subFileInfo.fileName();
        checker().verifyEqualsSorted("File field not imported as expected", List.of(fName, fName, fName, fName, fName), drt.getColumnDataAsText(fileFieldName));
    }


    private void importSampleTypeFilePathDataError(String sampleName, String filePath)
    {
        ImportDataPage importDataPage = new ImportDataPage(getDriver());
        final String fileFieldName = "FileField";
        String pasteData = TestDataUtils.tsvStringFromRowMaps(List.of(Map.of("Name", sampleName, fileFieldName, filePath)),
                List.of("Name", fileFieldName), true);
        importDataPage.setText(pasteData);
        String error = importDataPage.submitExpectingError();

        Assertions.assertThat(error).as("Error message").contains("Invalid file path: " + filePath);
    }
    
    @Test
    public void testCreateViaScript()
    {
        String sampleTypeName = "Created_by_Script";
        String createScript = String.format("""
                LABKEY.Domain.create({
                  domainKind: "SampleSet",
                  domainDesign: {
                    name: "%s",
                    fields: [{
                       name: "name", rangeURI: "string"
                    },{
                       name: "intField", rangeURI: "int"
                    },{
                       name: "strField", rangeURI: "string"
                    }]
                  },
                  success: callback,
                  failure: callback
                });
                """, sampleTypeName);

        log("Go to project home.");
        goToProjectHome();

        log("Create a Sample Type using script.");
        Map<String, Object> response = executeAsyncScript(createScript, Map.class);
        Assertions.assertThat(response).as("'LABKEY.Domain.create' response")
                .containsEntry("success", true);

        List<String> sampleNames = Arrays.asList("P-1", "P-2", "P-3", "P-4", "P-5");
        List<Map<String, String>> sampleData = new ArrayList<>();
        sampleNames.forEach(name -> sampleData.add(Map.of("Name", name, "intField", "42", "strField", "Sample: " + name)));

        log("Refresh the browser so the new sample type is shown.");
        goToHome();
        goToProjectHome();

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);

        log("Add samples to the sample type.");
        sampleHelper.goToSampleType(sampleTypeName);
        sampleHelper.bulkImport(sampleData);

        log("Check that the samples were added.");
        checker().verifyEquals("Number of samples not as expected.",
                sampleNames.size(),
                sampleHelper.getSampleCount());

    }

    @Test
    public void testFieldUniqueConstraint()
    {
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        String sampleTypeName = "Unique Constraint Test";

        clickProject(PROJECT_NAME);
        CreateSampleTypePage createPage = sampleHelper
                .goToCreateNewSampleType()
                .setName(sampleTypeName);

        log("Add a field with a non-unique constraint");
        String fieldName1 = "field Name1";
        DomainFormPanel domainFormPanel = createPage.getFieldsPanel();
        domainFormPanel.manuallyDefineFields(fieldName1)
                .setType(ColumnType.Integer)
                .expand().clickAdvancedSettings().setSingleFieldIndex(AdvancedSettingsDialog.SingleFieldIndexType.INDEX).apply();
        log("Add another field with a unique constraint");
        String fieldName2 = "fieldName_2";
        domainFormPanel.addField(fieldName2)
                .setType(ColumnType.DateAndTime)
                .expand().clickAdvancedSettings().setSingleFieldIndex(AdvancedSettingsDialog.SingleFieldIndexType.UNIQUE_INDEX).apply();
        log("Add another field which does not have a unique constraint");
        String fieldName3 = "FieldName@3";
        domainFormPanel.addField(fieldName3)
                .setType(ColumnType.Boolean);
        createPage.clickSave();

        viewRawTableMetadata(sampleTypeName);
        assertTextPresentCaseInsensitive("fk_rowid_"); // GitHub Issue 1117
        verifyTableIndices("unique_constraint_test_", List.of("field_Name1", "fieldName_2"));
        assertTextNotPresent("unique_constraint_test_fieldname_3");
        verifyTableIndexNonUnique("unique_constraint_test_", "field_Name1", false);
        verifyTableIndexNonUnique("unique_constraint_test_", "fieldName_2", true);

        log("Remove a field unique constraint and add a new one");
        goToProjectHome();
        UpdateSampleTypePage updatePage = sampleHelper.goToEditSampleType(sampleTypeName);
        domainFormPanel = updatePage.getFieldsPanel();
        domainFormPanel.getField(fieldName2)
                .expand().clickAdvancedSettings().setSingleFieldIndex(AdvancedSettingsDialog.SingleFieldIndexType.INDEX)
                .apply();
        domainFormPanel.getField(fieldName3)
                .expand().clickAdvancedSettings().setSingleFieldIndex(AdvancedSettingsDialog.SingleFieldIndexType.UNIQUE_INDEX)
                .apply();
        updatePage.clickSave();
        viewRawTableMetadata(sampleTypeName);
        verifyTableIndices("unique_constraint_test_", List.of("field_name1", "FieldName_3"));
        verifyTableIndexNonUnique("unique_constraint_test_", "field_Name1", false);
        verifyTableIndexNonUnique("unique_constraint_test_", "fieldName_2", false);
        verifyTableIndexNonUnique("unique_constraint_test_", "FieldName_3", true);
    }

    @Test
    public void testAmountsAndUnitsWithDisplayUnit()
    {
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        String sampleTypeName = "Sample Amounts and Units with Display Unit Test";

        clickProject(PROJECT_NAME);
        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(sampleTypeName);
        sampleTypeDefinition.setInventoryMetricUnit(InventoryMetricUnit.L);
        SampleTypeAPIHelper.createEmptySampleType(getProjectName(), sampleTypeDefinition);
        refresh();
        sampleHelper.goToSampleType(sampleTypeName);

        log("verify error when inserting a row with an amount but no unit");
        sampleHelper.insertRow(Map.of("Name", "AU-ERR-1", "StoredAmount", "0.0"));
        assertTextPresent("No 'Units' value provided for Amount '0.0'.");
        clickButton("Cancel");
        log("verify error when inserting a row with a unit but no amount");
        sampleHelper.insertRow(Map.of("Name", "AU-ERR-2", "Units", "mL"));
        assertTextPresent("No 'Amount' value provided for Units 'mL'.");
        clickButton("Cancel");

        log("verify error when inserting a row with incompatible units");
        sampleHelper.insertRow(Map.of("Name", "AU-ERR-1", "StoredAmount", "5.0", "Units", "mg"));
        assertTextPresent("Units value (mg) is not compatible with the " + sampleTypeName + " display units (mL).");
        clickButton("Cancel");
        sampleHelper.insertRow(Map.of("Name", "AU-ERR-1", "StoredAmount", "5.0", "Units", "unit"));
        assertTextPresent("Units value (unit) is not compatible with the " + sampleTypeName + " display units (mL).");
        clickButton("Cancel");

        log("verify inserting a row with compatible units succeeds and are converted");
        sampleHelper.insertRow(Map.of("Name", "AU-SUCCESS-1", "StoredAmount", "1.0", "Units", "mL"));
        verifySampleAmountUnitValues("AU-SUCCESS-1", "0.001", "L");
        sampleHelper.insertRow(Map.of("Name", "AU-SUCCESS-2", "StoredAmount", "0.002", "Units", "L"));
        verifySampleAmountUnitValues("AU-SUCCESS-2", "0.002", "L");
        sampleHelper.insertRow(Map.of("Name", "AU-SUCCESS-3", "StoredAmount", "3000", "Units", "uL"));
        verifySampleAmountUnitValues("AU-SUCCESS-3", "0.003", "L");

        log("verify updating a row with incompatible units fails");
        sampleHelper.updateRow(0, Map.of("Units", "mg"));
        assertTextPresent("Units value (mg) is not compatible with the " + sampleTypeName + " display units (mL).");
        clickButton("Cancel");

        log("verify updating a row with compatible units succeeds and are converted");
        sampleHelper.updateRow(2, Map.of("StoredAmount", "0.00123", "Units", "mL"));
        verifySampleAmountUnitValues("AU-SUCCESS-1", "1.23E-6", "L");

        log("verify rounding precision for display units");
        sampleHelper.updateRow(2, Map.of("StoredAmount", "0.12345678999", "Units", "L"));
        verifySampleAmountUnitValues("AU-SUCCESS-1", "0.12345679", "L");

        log("verify bulk import with incompatible units fails");
        sampleHelper.bulkImportExpectingError(List.of(Map.of("Name", "AU-BULK-ERR-1", "StoredAmount", "0", "Units", "kg")), SampleTypeHelper.IMPORT_OPTION);
        assertTextPresent("Units value (kg) is not compatible with the " + sampleTypeName + " display units (mL).");
        clickButton("Cancel");

        log("verify bulk import with compatible units succeeds and are converted");
        sampleHelper.bulkImport(List.of(Map.of("Name", "AU-BULK-SUCCESS-1", "StoredAmount", "0", "Units", "mL")));
        verifySampleAmountUnitValues("AU-BULK-SUCCESS-1", "0.0", "L");
        sampleHelper.bulkImport(List.of(Map.of("Name", "AU-BULK-SUCCESS-2", "StoredAmount", "0.005", "Units", "L")));
        verifySampleAmountUnitValues("AU-BULK-SUCCESS-2", "0.005", "L");
        sampleHelper.bulkImport(List.of(Map.of("Name", "AU-BULK-SUCCESS-3", "StoredAmount", "4000", "Units", "uL")));
        verifySampleAmountUnitValues("AU-BULK-SUCCESS-3", "0.004", "L");

        log("verify sorting on converted amounts works as expected");
        sampleHelper.getSamplesDataRegionTable().setSort("Amount", SortDirection.ASC);
        assertEquals("Sample order sorted asc not as expected",
                List.of("AU-BULK-SUCCESS-1", "AU-SUCCESS-2", "AU-SUCCESS-3", "AU-BULK-SUCCESS-3", "AU-BULK-SUCCESS-2", "AU-SUCCESS-1"),
                sampleHelper.getSamplesDataRegionTable().getColumnDataAsText("Name"));

        log("verify filtering on converted amounts works as expected");
        sampleHelper.getSamplesDataRegionTable().setFilter("Amount", "Is Greater Than", "0.004");
        assertEquals("Sample order filtered not as expected",
                List.of("AU-BULK-SUCCESS-2", "AU-SUCCESS-1"),
                sampleHelper.getSamplesDataRegionTable().getColumnDataAsText("Name"));
    }

    @Test
    public void testAmountsAndUnitsWithoutDisplayUnit()
    {
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        String sampleTypeName = "Sample Amounts and Units without Display Unit Test";

        clickProject(PROJECT_NAME);
        CreateSampleTypePage createPage = sampleHelper
                .goToCreateNewSampleType()
                .setName(sampleTypeName);
        assertTextNotPresent("Display Units");
        createPage.clickSave();
        sampleHelper.goToSampleType(sampleTypeName);

        log("verify that inserting a row with an amount or unit requires both fields to be filled in");
        // insert row with amount but not unit (error expected)
        sampleHelper.insertRow(Map.of("Name", "AU-ERR-1", "StoredAmount", "5.0"));
        assertTextPresent("No 'Units' value provided for Amount '5.0'.");
        clickButton("Cancel");
        // insert row with unit but not amount (error expected)
        sampleHelper.insertRow(Map.of("Name", "AU-ERR-2", "Units", "mg"));
        assertTextPresent("No 'Amount' value provided for Units 'mg'.");
        clickButton("Cancel");
        // insert row with both amount and unit (success)
        sampleHelper.insertRow(Map.of("Name", "AU-SUCCESS-1", "StoredAmount", "5.0", "Units", "mg"));
        verifySampleAmountUnitValues("AU-SUCCESS-1", "5.0", "mg");

        log("verify that updating a row with an amount or unit requires both fields to be filled in");
        // update row with amount but not unit (error expected)
        sampleHelper.updateRow(0, Map.of("Units", ""));
        assertTextPresent("No 'Units' value provided for Amount '5.0'.");
        clickButton("Cancel");
        // update row with unit but not amount (error expected)
        sampleHelper.updateRow(0, Map.of("StoredAmount", ""));
        assertTextPresent("No 'Amount' value provided for Units 'mg'.");
        clickButton("Cancel");
        // update row with both amount and unit (success)
        sampleHelper.updateRow(0, Map.of("StoredAmount", "10.0123", "Units", "g"));
        verifySampleAmountUnitValues("AU-SUCCESS-1", "10.0123", "g");

        log("verify that bulk import with an amount or unit requires both fields to be filled in");
        // bulk import with amount but not unit (error expected)
        sampleHelper.bulkImportExpectingError(List.of(Map.of("Name", "AU-BULK-ERR-1", "StoredAmount", "0")), SampleTypeHelper.IMPORT_OPTION);
        assertTextPresent("A 'Units' value must be provided when 'Amounts' are provided");
        clickButton("Cancel");
        // bulk import with unit but not amount (error expected)
        sampleHelper.bulkImportExpectingError(List.of(Map.of("Name", "AU-BULK-ERR-2", "Units", "mL")), SampleTypeHelper.IMPORT_OPTION);
        assertTextPresent("An 'Amount' value must be provided when 'Units' are provided.");
        clickButton("Cancel");
        // bulk import with both amount and unit (success expected)
        sampleHelper.bulkImport(List.of(Map.of("Name", "AU-BULK-SUCCESS-1", "StoredAmount", "0", "Units", "L")));
        verifySampleAmountUnitValues("AU-BULK-SUCCESS-1", "0.0", "L");

        log("verify the bulk import with RawAmount and RawUnits are ignored");
        sampleHelper.bulkImport(List.of(Map.of("Name", "AU-BULK-SUCCESS-2", "RawAmount", "1000", "RawUnits", "kg")));
        verifySampleAmountUnitValues("AU-BULK-SUCCESS-2", " ", " ");
    }

    private void verifySampleAmountUnitValues(String name, String expectedAmount, String expectedUnits)
    {
        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        drt.setFilter("Name", "Equals", name);
        checker().verifyEquals("StoredAmount value not as expected for sample " + name, expectedAmount, drt.getDataAsText(0, "StoredAmount"));
        checker().verifyEquals("Units value not as expected for sample " + name, expectedUnits, drt.getDataAsText(0, "Units"));
        drt.clearAllFilters();
    }

    private void viewRawTableMetadata(String sampleTypeName)
    {
        beginAt(WebTestHelper.buildURL("query", getProjectName(), "rawTableMetaData", Map.of("schemaName", "samples", "query.queryName", sampleTypeName)));
    }

    private void verifyTableIndices(String prefix, List<String> indexSuffixes)
    {
        List<String> suffixes = new ArrayList<>();
        suffixes.add("name");
        suffixes.addAll(indexSuffixes);

        for (String suffix : suffixes)
            assertTextPresentCaseInsensitive(prefix + suffix);
    }

    private void verifyTableIndexNonUnique(String prefix, String suffix, boolean isUnique)
    {
        String boolDisplay = isUnique ? "0" : "1";
        if (IS_POSTGRES) boolDisplay = isUnique ? "false" : "true";
        String fieldKey = prefix + suffix;
        if (IS_POSTGRES) fieldKey = fieldKey.toLowerCase();
        Locator locator = Locator.xpath("//td[contains(text(), '" + fieldKey + "')]/preceding-sibling::td[2][text()='" + boolDisplay + "']");
        checker().verifyTrue("Non_Unique value not as expected in metadata for locator: " + locator, locator.existsIn(getDriver()));
    }

    private void setFileAttachment(int index, File attachment)
    {
        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        UpdateQueryRowPage updateQueryRowPage = drt.clickEditRow(index);
        updateQueryRowPage.setField("FileAttachment",  attachment);
        updateQueryRowPage.submit();

        String path = drt.getDataAsText(index, "File Attachment");
        assertNotNull("Path shouldn't be null", path);
        assertTrue("Path didn't contain " + attachment.getName() + ", but was: " + path, path.contains(attachment.getName()));
    }

    private Sheet exportGridVerifyRowCountAndHeader(int numRows, Set<String> expectedHeaders)
    {
        DataRegionTable list = new DataRegionTable("Material", this.getDriver());
        DataRegionExportHelper exportHelper = new DataRegionExportHelper(list);
        return exportHelper.exportXLSAndVerifyRowCountAndHeader(numRows, expectedHeaders);
    }

    private void exportGridWithAttachment(int numOfRows, Set<String> expectedHeaders, int exportColumn, String... expectedFilePaths)
    {
        Sheet sheet = exportGridVerifyRowCountAndHeader(numOfRows, expectedHeaders);
        List<String> exportedColumn;
        int row;

        log("Validate that the value for the attachment columns is as expected.");
        exportedColumn = ExcelHelper.getColumnData(sheet, exportColumn);
        row = 1;
        for (String filePath : expectedFilePaths)
        {
            if (filePath.isEmpty())
            {
                assertEquals("Value of attachment column for row " + row + " not exported as expected.", "", exportedColumn.get(row).trim());
            }
            else
            {
                assertThat("Value of attachment column for row " + row + " not exported as expected.", exportedColumn.get(row).trim().toLowerCase(), containsString(filePath));
            }
            row++;
        }
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
