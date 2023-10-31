/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.assertj.core.api.Assertions;
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
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.domain.BaseDomainDesigner;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.experiment.CreateSampleTypePage;
import org.labkey.test.pages.experiment.UpdateSampleTypePage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.params.FieldDefinition.LookupInfo;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ExcelHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.TestUser;
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
import static org.labkey.test.util.DataRegionTable.DataRegion;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 20)
public class SampleTypeTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleTypeTestProject";
    private static final String FOLDER_NAME = "SampleTypeTestFolder";
    private static final String LOOKUP_FOLDER = "LookupSampleTypeFolder";
    private static final String CASE_INSENSITIVE_SAMPLE_TYPE = "CaseInsensitiveSampleType";
    private static final String LOWER_CASE_SAMPLE_TYPE = "caseinsensitivesampletype";
    private static final TestUser USER_FOR_FILTERTEST = new TestUser("filter_user@sampletypetest.test");

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
        SampleTypeTest init = (SampleTypeTest) getCurrentTest();

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

    @Test
    public void testCreateSampleTypeNoExpression()
    {
        final String sampleTypeName = "SimpleCreateNoExp";
        final List<FieldDefinition> fields = List.of(
                new FieldDefinition("StringValue", ColumnType.String),
                new FieldDefinition("IntValue", ColumnType.Integer));

        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(sampleTypeName).setFields(fields);

        log("Create a new sample type with a name and no name expression");
        projectMenu().navigateToFolder(PROJECT_NAME, FOLDER_NAME);
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);
        sampleTypeHelper.createSampleType(sampleTypeDefinition);
        sampleTypeHelper.goToSampleType(sampleTypeName);
        sampleTypeHelper.verifyFields(fields);

        log("Add a single row to the sample type");
        Map<String, String> fieldMap = Map.of("Name", "S-1", "StringValue", "Ess", "IntValue", "1");
        sampleTypeHelper.insertRow(fieldMap);

        log("Verify values were saved");
        sampleTypeHelper.verifyDataValues(Collections.singletonList(fieldMap));

        List<Map<String, String>> data = new ArrayList<>();
        data.add(Map.of("Name", "S-2", "StringValue", "Tee", "IntValue", "2"));
        data.add(Map.of("Name", "S-3", "StringValue", "Ewe", "IntValue", "3"));
        sampleTypeHelper.bulkImport(data);

        assertEquals("Number of samples not as expected", 3, sampleTypeHelper.getSampleCount());

        sampleTypeHelper.verifyDataValues(data);
    }

    // Issue 47280: LKSM: Trailing/Leading whitespace in Source name won't resolve when deriving samples
    @Test
    public void testImportSamplesWithTrailingSpace()
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
                .addPermission("Folder Administrator", getProjectName());
        String sampleType = "meFilterSamples";
        var domainDesigner = CreateSampleTypePage.beginAt(this, getProjectName());
        domainDesigner.setName(sampleType)
                .addField(new FieldDefinition("size", ColumnType.Integer))
                .addField(new FieldDefinition("user", ColumnType.User));
        var formatDialog = domainDesigner.getFieldsPanel().getField("user").clickConditionalFormatButton();
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
                    .setField("size", 2)
                    .setField("user", OptionSelect.SelectOption.textOption(getDisplayName()))
                    .submit();
        insertPage = sampleHelper.getSamplesDataRegionTable().clickInsertNewRow();
        insertPage.setField("Name", "not me")
                .setField("size", 3)
                .setField("user", OptionSelect.SelectOption.textOption(USER_FOR_FILTERTEST.getUserDisplayName()))
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
                helpDivBody.getText(), "Formatting applied because column = ~me~.");
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
        SampleTypeDefinition definition = new SampleTypeDefinition(sampleTypeName).setNameExpression("${" + fields.get(0).getName() + "}-${batchRandomId}-${randomId}").setFields(fields);
        sampleTypeHelper.createSampleType(definition);
        sampleTypeHelper.goToSampleType(sampleTypeName);
        sampleTypeHelper.verifyFields(fields);

        log("Add data without supplying the name");
        Map<String, String> fieldMap = Map.of(fieldNames.get(0), "Vee", fieldNames.get(1), "1.6");
        sampleTypeHelper.insertRow(fieldMap);

        log("Verify values are as expected with name expression saved");
        DataRegionTable drt = sampleTypeHelper.getSamplesDataRegionTable();
        int index = drt.getRowIndex(fieldNames.get(0), "Vee");
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

        assertTrue("Should have row with first imported value", drt.getRowIndex(fieldNames.get(0), "Dubya") >= 0);
        assertTrue("Should have row with second imported value", drt.getRowIndex(fieldNames.get(0), "Ex") >= 0);
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
        DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents")
                .clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), "Name1");
        setFormElement(Locator.name("quf_" + fieldNames.get(0)), "Bee");
        clickButton("Submit");

        log("Try to import overlapping data with TSV");

        DataRegionTable drt = sampleHelper.getSamplesDataRegionTable();
        ImportDataPage importDataPage = drt.clickImportBulkData();
        String header = "Name\t" + fieldNames.get(0) + "\n";
        String overlap =  "Name1\tToBee\n";
        String newData = "Name2\tSee\n";
        setFormElement(Locator.name("text"), header + overlap + newData);
        clickButton("Submit", "duplicate key");

        log("Switch to 'Insert and Replace'");
        importDataPage.setCopyPasteMerge(true);
        clickButton("Submit");

        log("Validate data was updated and new data added");
        drt = sampleHelper.getSamplesDataRegionTable();
        assertEquals("Number of samples not as expected", 2, drt.getDataRowCount());

        int index = drt.getRowIndex("Name", "Name1");
        assertTrue("Should have row with first sample name", index >= 0);
        Map<String, String> rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for sample 'Name1' not as expected", "ToBee", rowData.get(fieldNames.get(0)));

        index = drt.getRowIndex("Name", "Name2");
        assertTrue("Should have a row with the second sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for sample 'Name2' not as expected", "See", rowData.get(fieldNames.get(0)));

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
        assertEquals(fieldNames.get(0) + " for sample 'Name1' not as expected", "NotTwoBee", rowData.get(fieldNames.get(0)));

        index = drt.getRowIndex("Name", "Name2");
        assertTrue("Should have a row with the second sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for sample 'Name2' not as expected", "Sea", rowData.get(fieldNames.get(0)));

        index = drt.getRowIndex("Name", "Name3");
        assertTrue("Should have a row with the third sample name", index >= 0);
        rowData = drt.getRowDataAsMap(index);
        assertEquals(fieldNames.get(0) + " for sample 'Name' not as expected", "Dee", rowData.get(fieldNames.get(0)));
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
                        .setFields(List.of(new FieldDefinition("Field01",  ColumnType.String))),
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
//        setFormElement(Locator.id("TextAreaDataCollector.textArea"), TEST_RUN_DATA);
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
        setFormElement(Locator.id("TextAreaDataCollector.textArea"), SAMPLE_ID_TEST_RUN_DATA);
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
        setFormElement(Locator.id("TextAreaDataCollector.textArea"), TEST_RUN_DATA);
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

        log("Validate that update and delete works correctly with the Comment and Flag fields.");

        clickProject(PROJECT_NAME);

        // Using Map.of() creates an immutable collection I want to be able to update these data/collection items.
        Map<String, String> descriptionUpdate = new HashMap<>();
        descriptionUpdate.put("Name", SAMPLE_DESC_UPDATE);
        descriptionUpdate.put("Field01", "cc");
        descriptionUpdate.put("Description", "Here is the second description.");
        descriptionUpdate.put("Flag", "");

        Map<String, String> flagUpdate = new HashMap<>();
        flagUpdate.put("Name", SAMPLE_FLAG_UPDATE);
        flagUpdate.put("Field01", "bb");
        flagUpdate.put("Description", "");
        flagUpdate.put("Flag", "Flag Value 2");

        Map<String, String> updateBoth = new HashMap<>();
        updateBoth.put("Name", SAMPLE_UPDATE_BOTH);
        updateBoth.put("Field01", "dd");
        updateBoth.put("Description", "");
        updateBoth.put("Flag", "");

        Map<String, String> deleteSample = new HashMap<>();
        deleteSample.put("Name", SAMPLE_NAME_TO_DELETE);
        deleteSample.put("Field01", "aa");
        deleteSample.put("Description", "This is description number 1.");
        deleteSample.put("Flag", "Flag Value 1");

        // Some extra samples not really sure I will need them.
        Map<String, String> canarySample01 = new HashMap<>();
        canarySample01.put("Name", "ud05");
        canarySample01.put("Field01", "ee");
        canarySample01.put("Description", "This is description for sample 5.");
        canarySample01.put("Flag", "Flag Value 5");

        Map<String, String> canarySample02 = new HashMap<>();
        canarySample02.put("Name", "ud06");
        canarySample02.put("Field01", "ff");
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
                        .setFields(List.of(new FieldDefinition("Field01",  ColumnType.String))),
                sampleData);

        List<String> dbFieldsToCheck = Arrays.asList("Name", "Flag/Comment", "Field01", "Description");
        List<Map<String, String>> resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck);

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
        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck);
        checker().verifyTrue("Sample Type data is not as expected after a delete.", areDataListEqual(resultsFromDB, sampleData));

        log("Now update a sample's description.");

        testDataIndex = getSampleIndexFromTestInput(SAMPLE_DESC_UPDATE, sampleData);
        sampleData.get(testDataIndex).replace("Description", DESC_UPDATE);

        updateSampleType(sampleData.get(testDataIndex));

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck);
        checker().verifyTrue("Sample Type data is not as expected after a update of Description.", areDataListEqual(resultsFromDB, sampleData));

        log("Now delete the sample's description.");
        sampleData.get(testDataIndex).replace("Description", "");

        updateSampleType(sampleData.get(testDataIndex));

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck);
        checker().verifyTrue("Sample Type data is not as expected after deleting the Description.", areDataListEqual(resultsFromDB, sampleData));

        log("Let's repeat it all again for a sample's flag/comment.");
        testDataIndex = getSampleIndexFromTestInput(SAMPLE_FLAG_UPDATE, sampleData);
        sampleData.get(testDataIndex).replace("Flag", FLAG_UPDATE);

        updateSampleType(sampleData.get(testDataIndex));

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck);
        checker().verifyTrue("Sample Type data is not as expected after a update of Flag/Comment.", areDataListEqual(resultsFromDB, sampleData));

        log("Now delete the sample's Flag/Comment.");
        sampleData.get(testDataIndex).replace("Flag", "");

        updateSampleType(sampleData.get(testDataIndex));

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck);
        checker().verifyTrue("Sample Type data is not as expected after deleting the Flag/Comment.", areDataListEqual(resultsFromDB, sampleData));

        log("Finally update and delete both flag and description for a sample.");
        testDataIndex = getSampleIndexFromTestInput(SAMPLE_UPDATE_BOTH, sampleData);
        sampleData.get(testDataIndex).replace("Flag", FLAG_UPDATE_1);
        sampleData.get(testDataIndex).replace("Description", DESC_UPDATE_1);

        updateSampleType(sampleData.get(testDataIndex));

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck);
        checker().verifyTrue("Sample Type data is not as expected after a adding a Description and a Flag/Comment to an existing sample.",
                areDataListEqual(resultsFromDB, sampleData));

        log("Now update both values.");

        sampleData.get(testDataIndex).replace("Flag", FLAG_UPDATE_2);
        sampleData.get(testDataIndex).replace("Description", DESC_UPDATE_2);

        updateSampleType(sampleData.get(testDataIndex));

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck);
        checker().verifyTrue("Sample Type data is not as expected after a updating both a Description and a Flag/Comment.",
                areDataListEqual(resultsFromDB, sampleData));

        log("Now delete both the Description and Flag/Comment from the sample.");
        sampleData.get(testDataIndex).replace("Flag", "");
        sampleData.get(testDataIndex).replace("Description", "");

        updateSampleType(sampleData.get(testDataIndex));

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck);
        checker().verifyTrue("Sample Type data is not as expected after deleting the Description and Flag/Comment.",
                areDataListEqual(resultsFromDB, sampleData));

        // Check for Issue 40385: Can't Update Samples Using a File
        log("Now use a file import to update the samples.");

        for(Map<String, String> sample : sampleData)
        {
            String fieldValue = sample.get("Field01");
            sample.replace("Field01", fieldValue.toUpperCase());
        }

        List<String> fileData = new ArrayList<>();
        fileData.add(String.format("%s\t%s", "Name", "Field01"));
        for(Map<String, String> sample : sampleData)
        {
            fileData.add(String.format("%s\t%s", sample.get("Name"), sample.get("Field01")));
        }

        String fileName = "SampleTypeTest_UpdateSamples.tsv";
        if (!TestFileUtils.getTestTempDir().exists())
            TestFileUtils.getTestTempDir().mkdirs();
        File importFile = TestFileUtils.writeTempFile(fileName, String.join(System.lineSeparator(), fileData));

        sampleHelper.mergeImport(importFile);

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, dbFieldsToCheck);
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

    protected List<Map<String, String>> getSampleDataFromDB(String folderPath, String sampleTypeName, List<String> fields)
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

                    if (fields.contains(key))
                    {

                        String tmpFlag = key;

                        if(key.equalsIgnoreCase("Flag/Comment"))
                            tmpFlag = "Flag";

                        if (null == row.get(key))
                        {
                            tempRow.put(tmpFlag, "");
                        }
                        else
                        {
                            tempRow.put(tmpFlag, row.get(key).toString());
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

        List<Map<String, String>> resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, Arrays.asList("Name", REQUIRED_FIELD_NAME, MISSING_FIELD_NAME, INDICATOR_FIELD_NAME));

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

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, Arrays.asList("Name", REQUIRED_FIELD_NAME, MISSING_FIELD_NAME, INDICATOR_FIELD_NAME));

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
        drt.clickInsertNewRow();

        Locator sampleNameElement = Locator.name("quf_Name");
        Locator sampleStaticFieldElement = Locator.name("quf_" + REQUIRED_FIELD_NAME);
        Locator sampleMissingFieldElement = Locator.name("quf_" + MISSING_FIELD_NAME);
        Locator sampleMissingFieldIndElement = Locator.name("quf_" + INDICATOR_FIELD_NAME);
        waitForElementToBeVisible(sampleNameElement);

        setFormElement(sampleNameElement, UI_INSERT_SAMPLE_NAME);
        setFormElement(sampleStaticFieldElement, UI_STATIC_FIELD_TEXT);
        selectOptionByValue(sampleMissingFieldIndElement, MV_INDICATOR_03);
        clickButton("Submit");
        expectedMissingCount++;

        // Add this element to expected sample data.
        sampleData.add(Map.of("Name", UI_INSERT_SAMPLE_NAME, REQUIRED_FIELD_NAME, UI_STATIC_FIELD_TEXT, MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, MV_INDICATOR_03));

        checker().verifyEquals("After adding a sample with a missing value through the UI the number of missing UI indicators is not as expected.",
                Locator.xpath("//td[contains(@class, 'labkey-mv-indicator')]").findElements(getDriver()).size(),
                expectedMissingCount);

        resultsFromDB = getSampleDataFromDB(getCurrentContainerPath(), SAMPLE_TYPE_NAME, Arrays.asList("Name", REQUIRED_FIELD_NAME, MISSING_FIELD_NAME, INDICATOR_FIELD_NAME));

        checker().verifyTrue("After adding a sample with a missing value through the UI the data in the DB is not as expected.",
                areDataListEqual(resultsFromDB, sampleData));

        log("Validate that the required field check works as expected.");
        updateSampleData = new ArrayList<>();
        updateSampleData.add(Map.of("Name", "mv10", REQUIRED_FIELD_NAME, "", MISSING_FIELD_NAME, "There should be no value in the required field.", INDICATOR_FIELD_NAME, ""));
        sampleHelper.bulkImportExpectingError(updateSampleData, SampleTypeHelper.IMPORT_OPTION);

        try
        {
            waitForElementToBeVisible(Locator.xpath("//div[contains(@class, 'labkey-error')][contains(text(),'Missing value for required property')]"));
            clickButton("Cancel");
        }
        catch(NoSuchElementException nse)
        {
            checker().error("No error message was shown when a required field is missing.");
        }

        log("Now validate that adding a single row from the UI has the same behavior.");
        final String UI_MISSING_REQ_SAMPLE_NAME = "mv10";
        final String UI_MISSING_FIELD_TEXT = "This should generate an error.";
        drt = sampleHelper.getSamplesDataRegionTable();
        drt.clickInsertNewRow();
        waitForElementToBeVisible(sampleNameElement);

        setFormElement(sampleNameElement, UI_MISSING_REQ_SAMPLE_NAME);
        setFormElement(sampleMissingFieldElement, UI_MISSING_FIELD_TEXT);
        clickButton("Submit", 0);

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
        assertEquals("Sample Type creation error", Arrays.asList("A Sample Type with that name already exists."), errors);
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
        updatePage.setName(CASE_INSENSITIVE_SAMPLE_TYPE.toUpperCase());
        assertTrue("Sample type rename conflict error",
                updatePage.clickSaveExpectingErrors().contains("A Sample Type with name 'CASEINSENSITIVESAMPLETYPE' already exists."));
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
                Arrays.asList("Property name 'created' is a reserved name."),
                createPage.clickSaveExpectingErrors());
        domainFormPanel.removeAllFields(false);

        domainFormPanel.manuallyDefineFields("rowid");
        checker().verifyEquals("Sample Type reserved field name error",
                Arrays.asList("Property name 'rowid' is a reserved name."),
                createPage.clickSaveExpectingErrors());
        domainFormPanel.removeAllFields(false);

        log("Verify error message for a few other special field names");
        domainFormPanel.manuallyDefineFields("name");
        checker().verifyEquals("Sample Type 'name' field name error",
                Arrays.asList(
                "The field name 'Name' is already taken. Please provide a unique name for each field.",
                "Please correct errors in Fields before saving."),
                createPage.clickSaveExpectingErrors());
        domainFormPanel.removeAllFields(false);

        log("Verify error message for a few other special field names");
        domainFormPanel.manuallyDefineFields("sampleid");
        checker().verifyEquals("Sample Type SampleId field name error",
                Arrays.asList("The SampleID field name is reserved for imported or generated sample ids."),
                createPage.clickSaveExpectingErrors());
        domainFormPanel.removeAllFields(false);
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
        checker().verifyEquals("Reserved field warning not as expected",  expectedInfoMsg, domainForm.getPanelAlertText());
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
                new LookupInfo(null, "lists", listName)
                        .setTableType(ColumnType.Integer)).setLabel(lookupColumnLabel).setLookupValidatorEnabled(true));
        sampleHelper.createSampleType(definition);

        goToProjectHome();
        clickAndWait(Locator.linkWithText(SAMPLE_TYPE));
        DataRegionTable table = sampleHelper.getSamplesDataRegionTable();
        table.clickInsertNewRow();

        setFormElement(Locator.name("quf_Name"),"1");
        selectOptionByText(Locator.name("quf_Key"),"apple");
        clickButton("Submit");

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
        drt.clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), "SampleTypeInsertedManually");
        setFormElement(Locator.name("quf_FileAttachment"), experimentFilePath);
        clickButton("Submit");
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

        log("Add a field with a unique constraint");
        String fieldName1 = "field Name1";
        DomainFormPanel domainFormPanel = createPage.getFieldsPanel();
        domainFormPanel.manuallyDefineFields(fieldName1)
                .setType(ColumnType.Integer)
                .expand().clickAdvancedSettings().setUniqueConstraint(true).apply();
        log("Add another field with a unique constraint");
        String fieldName2 = "fieldName_2";
        domainFormPanel.addField(fieldName2)
                .setType(ColumnType.DateAndTime)
                .expand().clickAdvancedSettings().setUniqueConstraint(true).apply();
        log("Add another field which does not have a unique constraint");
        String fieldName3 = "FieldName@3";
        domainFormPanel.addField(fieldName3)
                .setType(ColumnType.Boolean);
        createPage.clickSave();

        viewRawTableMetadata(sampleTypeName);
        verifyTableIndices("unique_constraint_test_", List.of("field_name1", "fieldname_2"));

        log("Remove a field unique constraint and add a new one");
        goToProjectHome();
        UpdateSampleTypePage updatePage = sampleHelper.goToEditSampleType(sampleTypeName);
        domainFormPanel = updatePage.getFieldsPanel();
        domainFormPanel.getField(fieldName2)
                .expand().clickAdvancedSettings().setUniqueConstraint(false)
                .apply();
        domainFormPanel.getField(fieldName3)
                .expand().clickAdvancedSettings().setUniqueConstraint(true)
                .apply();
        updatePage.clickSave();
        viewRawTableMetadata(sampleTypeName);
        verifyTableIndices("unique_constraint_test_", List.of("field_name1", "fieldname_3"));
        assertTextNotPresent("unique_constraint_test_fieldname_2");
    }

    private void viewRawTableMetadata(String sampleTypeName)
    {
        beginAt("/" + EscapeUtil.encode(getProjectName()) + "/query-rawTableMetaData.view?schemaName=samples&query.queryName=" + sampleTypeName);
    }

    private void verifyTableIndices(String prefix, List<String> indexSuffixes)
    {
        List<String> suffixes  = new ArrayList<>();
        suffixes.add("lsid");
        suffixes.add("name");
        suffixes.addAll(indexSuffixes);

        for (String suffix : suffixes)
            assertTextPresentCaseInsensitive(prefix + suffix);
    }

    private void setFileAttachment(int index, File attachment)
    {
        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        drt.clickEditRow(index);
        setFormElement(Locator.name("quf_FileAttachment"),  attachment);
        clickButton("Submit");

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
            if (filePath.length() == 0)
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
