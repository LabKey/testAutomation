/*
 * Copyright (c) 2019 LabKey Corporation
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

import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.assay.Batch;
import org.labkey.remoteapi.assay.Data;
import org.labkey.remoteapi.assay.Material;
import org.labkey.remoteapi.assay.Run;
import org.labkey.remoteapi.assay.SaveAssayBatchCommand;
import org.labkey.remoteapi.assay.SaveAssayBatchResponse;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.assay.AssayImportPage;
import org.labkey.test.pages.assay.AssayRunsPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.AbstractDataRegionExportOrSignHelper;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.TestDataValidator;
import org.labkey.test.util.TextSearcher;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.labkey.test.util.exp.SampleTypeAPIHelper.SAMPLE_TYPE_DATA_REGION_NAME;
import static org.labkey.test.util.exp.SampleTypeAPIHelper.SAMPLE_TYPE_DOMAIN_KIND;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 10)
public class SampleTypeRemoteAPITest extends BaseWebDriverTest
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
        SampleTypeRemoteAPITest init = (SampleTypeRemoteAPITest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME, new String[]{"Experiment"});
        _containerHelper.createSubfolder(getProjectName(), LINEAGE_FOLDER, new String[]{"Experiment"});

        projectMenu().navigateToProject(getProjectName());
        portalHelper.addWebPart("Sample Types");

        projectMenu().navigateToFolder(getProjectName(), FOLDER_NAME);
        portalHelper.addWebPart("Sample Types");

        projectMenu().navigateToFolder(getProjectName(), LINEAGE_FOLDER);
        portalHelper.addWebPart("Sample Types");
    }

    @Before
    public void preTest() throws Exception
    {
        goToProjectHome();
    }

    /**
     * regression coverage for Issue 37514 - sample type lookup to exp.Files crashes when viewing the sample type
     * @throws IOException
     * @throws CommandException
     */
    @Test
    public void samplesWithLookupsToExpFilesTest() throws IOException, CommandException
    {
        // create a basic sample type with a lookup reference to exp.Files
        String lookupContainer = getProjectName() + "/" + LINEAGE_FOLDER;
        navigateToFolder(getProjectName(), FOLDER_NAME);
        // create another with a lookup to it
        TestDataGenerator lookupDgen = new TestDataGenerator("exp.materials", "expFileSampleLookups", getCurrentContainerPath())
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("strLookup", new FieldDefinition.LookupInfo(lookupContainer, "exp", "Files"))
                ));
        lookupDgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        lookupDgen.addCustomRow(Map.of("name", "B"));
        lookupDgen.insertRows(createDefaultConnection(), lookupDgen.getRows());

        refresh();
        DataRegionTable sampleTypeList =  DataRegionTable.DataRegion(getDriver()).withName(SAMPLE_TYPE_DATA_REGION_NAME).waitFor();

        // now attempt to view the table- should trip Issue 37514 and crash
        waitAndClick(Locator.linkWithText("expFileSampleLookups"));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        // clean up on success
        lookupDgen.deleteDomain(createDefaultConnection());
    }


    /**
     * generates a small sample type, pastes data into it via the UI, including "Q" and "N" missing value indicators
     * @throws IOException
     * @throws CommandException
     */
    @Test
    public void importMissingValueSampleType() throws IOException, CommandException
    {
        String missingValueTable = "mvSamplesForImport";

        navigateToFolder(getProjectName(), FOLDER_NAME);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", missingValueTable, getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("mvStringData", FieldDefinition.ColumnType.String)
                                .setMvEnabled(true).setLabel("MV Field"),
                        TestDataGenerator.simpleFieldDef("volume", FieldDefinition.ColumnType.Decimal)
                ));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        dgen.addCustomRow(Map.of("name", "First", "mvStringData", "Q", "volume", 13.5));
        dgen.addCustomRow(Map.of("name", "Second", "mvStringData", "Q", "volume", 15.5));
        dgen.addCustomRow(Map.of("name", "Third","mvStringData", "N", "volume", 16.5));
        dgen.addCustomRow(Map.of("name", "Fourth","mvStringData", "N", "volume", 17.5));
        dgen.addCustomRow(Map.of("name", "Fifth","mvStringData", "ABCDEF", "volume", 17.5));
        dgen.addCustomRow(Map.of("name", "Sixth","mvStringData", "GHIJKL", "volume", 17.5));
        dgen.addCustomRow(Map.of("name", "Seventh","mvStringData", "ValidValue", "volume", 17.5));
        dgen.addCustomRow(Map.of("name", "Eighth","mvStringData", "ActualData", "volume", 17.5));

        // write the domain data into TSV format, for import via the UI
        String importTsv = dgen.writeTsvContents();

        refresh();
        DataRegionTable sampleTypeList =  DataRegionTable.DataRegion(getDriver()).withName(SAMPLE_TYPE_DATA_REGION_NAME).waitFor();
        waitAndClick(Locator.linkWithText(missingValueTable));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        // paste the TSV data into the form
        materialsList.clickImportBulkData();
        setFormElement(Locator.textarea("text"), importTsv);
        clickButton("Submit");

        // re-find materialsList after importing MV data
        materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        List<Map<String, String>> insertedRows = new ArrayList<>();
        for (int i=0; i<dgen.getRows().size(); i++)
        {
            insertedRows.add(materialsList.getRowDataAsMap(i));
        }

        // confirm that firstRow has an MV indicator supplied
        int firstIndex = materialsList.getRowIndex("Name", "First");
        WebElement firstRow = materialsList.findRow(firstIndex);
        Locator.tagWithClass("td", "labkey-mv-indicator")
                .withDescendant(Locator.tagWithText("a", "Q")).findElement(firstRow);
        int fourthIndex = materialsList.getRowIndex("Name", "Fourth");
        WebElement fourthRow = materialsList.findRow(fourthIndex);
        Locator.tagWithClass("td", "labkey-mv-indicator")
                .withDescendant(Locator.tagWithText("a", "N")).findElement(fourthRow);

        // confirm that every one of the initially-created rows were present with all values in the materialsList dataregion
        TestDataValidator validator = dgen.getValidator();      // validator has a copy of
        String error = validator.enumerateMissingRows(insertedRows, Arrays.asList("Flag"));  // ensure all rows made it with expected values
        assertEquals("", error);    // if any expected rows are absent, error will describe what it expected but did not find

        dgen.deleteDomain(createDefaultConnection());
    }

    @Test
    public void updateMissingValueSampleData() throws IOException, CommandException
    {
        String missingValueTable = "mvSamplesForUpdate";

        navigateToFolder(getProjectName(), FOLDER_NAME);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", missingValueTable, getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("mvStringData", FieldDefinition.ColumnType.String)
                                .setMvEnabled(true).setLabel("MV Field"),
                        TestDataGenerator.simpleFieldDef("volume", FieldDefinition.ColumnType.Decimal)
                ));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        dgen.addCustomRow(Map.of("name", "A", "mvStringData", "Q", "volume", 13.5));
        dgen.addCustomRow(Map.of("name", "B", "mvStringData", "Q", "volume", 15.5));
        dgen.addCustomRow(Map.of("name", "C","mvStringData", "N", "volume", 16.5));
        dgen.addCustomRow(Map.of("name", "D","mvStringData", "N", "volume", 17.5));
        dgen.addCustomRow(Map.of("name", "E","mvStringData", "ABCDEF", "volume", 17.5));
        dgen.addCustomRow(Map.of("name", "F","mvStringData", "GHIJKL", "volume", 17.5));
        dgen.addCustomRow(Map.of("name", "G","mvStringData", "ValidValue", "volume", 17.5));
        dgen.addCustomRow(Map.of("name", "H","mvStringData", "ActualData", "volume", 17.5));

        SaveRowsResponse insertResponse = dgen.insertRows(createDefaultConnection(), dgen.getRows());

        refresh();
        DataRegionTable sampleTypeList =  DataRegionTable.DataRegion(getDriver()).withName(SAMPLE_TYPE_DATA_REGION_NAME).waitFor();
        waitAndClick(Locator.linkWithText(missingValueTable));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        List<Map<String, String>> mapsFromDataRegion = new ArrayList<>();
        for(int i=0; i< dgen.getRows().size(); i++)
        {
            mapsFromDataRegion.add(materialsList.getRowDataAsMap(i));
        }
        String missingRowsError = dgen.getValidator().enumerateMissingRows(mapsFromDataRegion, Arrays.asList("Flag"));
        assertEquals("", missingRowsError);

        // now update
        Map<String, Object> rowE = insertResponse.getRows().stream().filter(a -> a.get("name").equals("E")).findFirst().orElse(null);
        Map<String, Object> rowD = insertResponse.getRows().stream().filter(a -> a.get("name").equals("D")).findFirst().orElse(null);
        rowE.put("mvstringdatamvindicator", "Q");
        rowD.put("mvstringdatamvindicator", null);
        rowD.put("mvStringData", "updatedValue");

        SaveRowsResponse updateResponse = dgen.updateRows(createDefaultConnection(), Arrays.asList(rowD, rowE));

        // get a look at the result
        refresh();
        materialsList = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        // confirm that firstRow has an MV indicator supplied
        int eIndex = materialsList.getRowIndex("Name", "E");
        WebElement eRow = materialsList.findRow(eIndex);
        Locator.tagWithClass("td", "labkey-mv-indicator")
                .withDescendant(Locator.tagWithText("a", "Q")).findElement(eRow);
        int dIndex = materialsList.getRowIndex("Name", "D");
        WebElement fourthRow = materialsList.findRow(dIndex);
        assertFalse("expect literal value, not MV", Locator.tagWithClass("td", "labkey-mv-indicator")
                .withDescendant(Locator.tagWithText("a", "N")).existsIn(fourthRow));
        WebElement dCell = materialsList.findCell(dIndex, "MV Field");
        assertEquals("cell should reflect literal value", "updatedValue", dCell.getText());

        // now update rows via the UI
        String mvIndicatorColName = "mvStringDataMVIndicator";
        materialsList.updateRow(dIndex, Map.of("mvStringData", "reallyUpdatedValue", mvIndicatorColName, "Q"));  // update the underlying value but set it mv-Q
        materialsList.clickEditRow(eIndex);
        selectOptionByText(Locator.name("quf_"+mvIndicatorColName), "");    // clear the mv value, reveal underlying value
        clickButton("Submit");

        eIndex = materialsList.getRowIndex("Name", "E");
        WebElement eCell = materialsList.findCell(eIndex, "MV Field");
        assertEquals("expect underlying value to be present when mv-flag has been cleared","ABCDEF", eCell.getText());
        dIndex = materialsList.getRowIndex("Name", "D");
        dCell = materialsList.findCell(dIndex, "MV Field");
        assertAttributeContains(dCell, "class", "labkey-mv-indicator");
        assertEquals("expect mv-indicator value instead of underlying value", "Q", dCell.getText());

        dgen.deleteDomain(createDefaultConnection());
    }

    @Test
    public void importMVDataWithEmptyValues() throws IOException, CommandException
    {
        String missingValueTable = "mvSamplesWithEmptyValues";

        navigateToFolder(getProjectName(), FOLDER_NAME);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", missingValueTable, getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("mvStringData", FieldDefinition.ColumnType.String)
                                .setMvEnabled(true).setLabel("MV Field"),
                        TestDataGenerator.simpleFieldDef("volume", FieldDefinition.ColumnType.Decimal)
                ))
                .withGeneratedRows(30);
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        dgen.addCustomRow(Map.of("name", "First", "mvStringData", "Q", "volume", 13.5));
        dgen.addCustomRow(Map.of("name", "Second", "mvStringData", "Q", "volume", 15.5));
        dgen.addCustomRow(Map.of("name", "Third","mvStringData", "N", "volume", 16.5));
        dgen.addCustomRow(Map.of("name", "Fourth","mvStringData", "N", "volume", 17.5));
        dgen.addCustomRow(Map.of("name", "Fifth","mvStringData", "", "volume", 17.5));
        dgen.addCustomRow(Map.of("name", "Sixth","mvStringData", "", "volume", 17.5));
        dgen.addCustomRow(Map.of("name", "Seventh","mvStringData", "ValidValue", "volume", 17.5));
        dgen.addCustomRow(Map.of("name", "Eighth","mvStringData", "ActualData", "volume", 17.5));

        // insert them via API
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

        // now find the samlpleset's dataregion
        refresh();
        DataRegionTable sampleTypeList =  DataRegionTable.DataRegion(getDriver()).withName(SAMPLE_TYPE_DOMAIN_KIND).waitFor();
        waitAndClick(Locator.linkWithText(missingValueTable));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        // confirm that fifthRow has no MV indicator supplied
        int mvColumnIndex = materialsList.getColumnIndex("MV Field");
        int fifthRowIndex = materialsList.getRowIndex("Name", "Fifth");
        WebElement fifthRowMvCell = materialsList.findCell(mvColumnIndex, fifthRowIndex);
        assertEquals("", fifthRowMvCell.getAttribute("class"));
        // ensure that fourthRow has an MV indicator with value N
        int fourthIndex = materialsList.getRowIndex("Name", "Fourth");
        WebElement fourthRow = materialsList.findRow(fourthIndex);
        Locator.tagWithClass("td", "labkey-mv-indicator")
                .withDescendant(Locator.tagWithText("a", "N")).findElement(fourthRow);

        dgen.deleteDomain(createDefaultConnection());
    }

    /**
     * regression for https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=38436
     * @throws CommandException
     * @throws IOException
     */
    @Test
    @Ignore("ignoring result until issue 38436 can be resolved.")
    public void exportMissingValueSampleTypeToTSV() throws CommandException, IOException
    {
        String missingValueTable = "mvSamplesForExport";
        navigateToFolder(getProjectName(), FOLDER_NAME);

        navigateToFolder(getProjectName(), FOLDER_NAME);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", missingValueTable, getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("mvStringData", FieldDefinition.ColumnType.String)
                                .setMvEnabled(true).setLabel("MV Field"),
                        TestDataGenerator.simpleFieldDef("vol", FieldDefinition.ColumnType.Decimal)
                ));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        dgen.addCustomRow(Map.of("name", "1st", "mvStringData", "Q", "vol", 17.5));
        dgen.addCustomRow(Map.of("name", "2nd", "mvStringData", "Q", "vol", 19.5));
        dgen.addCustomRow(Map.of("name", "3rd","mvStringData", "N", "vol", 22.25));
        dgen.addCustomRow(Map.of("name", "4th","mvStringData", "N", "vol", 38.75));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());     // insert data via API rather than UI

        // prepare expected values-
        String expectedTSVData = dgen.writeTsvContents();
        String[] tsvRows = expectedTSVData.split("\n");
        List<String> dataRows = new ArrayList();
        for (int i=1; i < tsvRows.length; i++) // don't validate columns; we expect labels instead of column names
        {
            dataRows.add(tsvRows[i]);
        }

        refresh();
        DataRegionTable sampleTypeList =  DataRegionTable.DataRegion(getDriver()).withName(SAMPLE_TYPE_DATA_REGION_NAME).waitFor();
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
    }


    @Test
    public void sampleTypeWithMissingValueField() throws IOException, CommandException
    {
        String missingValueTable = "mvSamples";

        navigateToFolder(getProjectName(), FOLDER_NAME);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", missingValueTable, getCurrentContainerPath())
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("mvStringData", FieldDefinition.ColumnType.String)
                                .setMvEnabled(true).setLabel("MV Field"),
                        TestDataGenerator.simpleFieldDef("volume", FieldDefinition.ColumnType.Decimal)
                ));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        dgen.addCustomRow(Map.of("name", "First", "volume", 13.5));
        dgen.addCustomRow(Map.of("name", "Second", "volume", 15.5));
        dgen.addCustomRow(Map.of("name", "Third", "volume", 16.5));
        dgen.addCustomRow(Map.of("name", "Fourth", "volume", 17.5));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

        refresh();
        DataRegionTable sampleTypeList =  DataRegionTable.DataRegion(getDriver()).withName(SAMPLE_TYPE_DATA_REGION_NAME).waitFor();
        waitAndClick(Locator.linkWithText(missingValueTable));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        materialsList.setSort("Volume", SortDirection.ASC);    // fix the order here so later when we edit a row by index it stays there on page refresh

        materialsList.clickEditRow(0);
        setFormElement(Locator.input("quf_mvStringData"), "testValue");

        selectOptionByText(Locator.tagWithAttribute("select", "name", "quf_mvStringDataMVIndicator"), "Q");
        clickButton("Submit");

        materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        materialsList.clickEditRow(1);
        setFormElement(Locator.input("quf_mvStringData"), "otherValue");
        selectOptionByText(Locator.tagWithAttribute("select", "name", "quf_mvStringDataMVIndicator"), "N");
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

        dgen.deleteDomain(createDefaultConnection());
    }

    @Test
    @Ignore ("Issue 37786: Sample written to SampleSet via LABKEY.Experiment.saveBatch does not receive missingvalue_mvindicator value")
    public void insertSamplesOverSaveBatchAPI() throws Exception
    {
        String missingValueTable = "mvAssaySamples";
        String missingValueSamplesFolder = "mvAssaySamplesFolder";
        _containerHelper.createSubfolder(getProjectName(), missingValueSamplesFolder);

        // create assay
        String assaySubfolder = "TestAssayFolder";
        String assayName = "AssayForSampleDerivation";
        generateAssay(assaySubfolder, assayName);
        List<TestDataGenerator> dataGenerators = generateAssayData(new FieldDefinition.LookupInfo(getProjectName() + "/" + assaySubfolder,
                "assay.General.AssayForSampleDerivation_assay", "Runs"));
        insertAssayData(assayName, dataGenerators);

        // get the sampleID, it will be needed later
        waitAndClickAndWait(Locator.linkWithText("Assay List"));
        DataRegionTable assayList = DataRegionTable.DataRegion(getDriver()).withName("AssayList").waitFor();
        CustomizeView customizeView = assayList.openCustomizeGrid();
        customizeView.showHiddenItems();
        customizeView.addColumn("RowId");
        customizeView.clickSave().save();   // make this the default view
        String assayIdStringValue = assayList.getRowDataAsMap(0).get("RowId");

        // create sampleset
        navigateToFolder(getProjectName(), missingValueSamplesFolder);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", missingValueTable, getProjectName() + "/" + missingValueSamplesFolder)
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("mvStringData", FieldDefinition.ColumnType.String)
                                .setMvEnabled(true).setLabel("MV Field"),
                        TestDataGenerator.simpleFieldDef("volume", FieldDefinition.ColumnType.Decimal)
                ));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        dgen.addCustomRow(Map.of("name", "1st", "volume", 13.5));
        dgen.addCustomRow(Map.of("name", "2nd", "volume", 15.5));
        dgen.addCustomRow(Map.of("name", "3rd", "volume", 16.5));
        dgen.addCustomRow(Map.of("name", "4th", "volume", 17.5));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

        // todo: do this using the SaveAssayBatchCommand; it currently blows up if you attempt to get a Material without supplying the SampleSetID
        String createScript = "LABKEY.Experiment.saveBatch({\n" +
                "  success: callback, failure:callback, \n" +
                "  assayId: "+assayIdStringValue+",\n" +
                "  batch: {\n" +
                "    name: 'foo',\n" +
                "    runs: [{\n" +
                "      name: 'bar',\n" +
                "\n" +
                "      materialOutputs: [\n" +
                "        // create some new samples in the target sample type if it doesn't exist\n" +
                "        { name: 'a new one', sampleSet: { name: 'mvAssaySamples' }, properties: { 'mvStringData': 'new from api' } },\n" +
                "        { name: 'another new one', sampleSet: { name: 'mvAssaySamples' }, properties: { 'mvStringData': 'also new from api' } }\n" +
                "      ],\n" +
                "\n" +
                "      properties: {\n" +
                "        runFileField: 'blah.png'\n" +
                "      },\n" +
                "\n" +
                "      dataRows: [{\n" +
                "        dataFileField: 'S3',\n" +
                "        sampleId: 'def'\n" +
                "      }]\n" +
                "    }]\n" +
                "  }\n" +
                "})";
        executeAsyncScript(createScript);

        // add a bodyWebPart here to make viewing the sampleset easier while debugging
        new PortalHelper(getDriver()).addBodyWebPart("Sample Types");
        DataRegionTable sampleTypeList =  DataRegionTable.DataRegion(getDriver()).withName(SAMPLE_TYPE_DATA_REGION_NAME).waitFor();
        waitAndClick(Locator.linkWithText(missingValueTable));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        // grab the new samples
        SelectRowsResponse sampleTypeResponse = dgen.getRowsFromServer(createDefaultConnection());
        Map<String, Object> firstAddedSample = sampleTypeResponse.getRows()
                .stream()
                .filter(a-> a.get("Name").equals("another new one"))
                .findFirst().orElse(null);
        Map<String, Object> secondAddedSample = sampleTypeResponse.getRows()
                .stream()
                .filter(a-> a.get("Name").equals("a new one"))
                .findFirst().orElse(null);

        assertNotNull("expect new samples as outputs from batch save", firstAddedSample);
        assertNotNull("expect new samples as outputs from batch save", secondAddedSample);
        int secondAddedRowId = (Integer)secondAddedSample.get("RowId");
        sleep(2000); // ugh, give it a moment

        // now update one of the new rows and add another
        String updateScript = "LABKEY.Experiment.saveBatch({\n" +
                "  success: callback, failure:callback, \n" +
                "  assayId: "+assayIdStringValue+",\n" +
                "  batch: {\n" +
                "    name: 'foo',\n" +
                "    runs: [{\n" +
                "      name: 'bar2',\n" +
                "\n" +
                "      materialOutputs: [\n" +
                "        // create/update samples in the target sample type\n" +
                "        { rowId: "+secondAddedRowId+", sampleSet: { name: 'mvAssaySamples' }, properties: { 'mvStringData': 'newer from api', volume: 7 } },\n" +
                "        { name: 'another really new one', sampleSet: { name: 'mvAssaySamples' }, " +
                "           properties: { 'mvStringData': 'also new from api', volume:17, mvstringdata_mvindicator : 'N' } }\n" +
                "      ],\n" +
                "\n" +
                "      properties: {\n" +
                "        runFileField: 'blah.png'\n" +
                "      },\n" +
                "\n" +
                "      dataRows: [{\n" +
                "        dataFileField: 'S4',\n" +
                "        sampleId: 'def'\n" +
                "      }]\n" +
                "    }]\n" +
                "  }\n" +
                "})";
        executeAsyncScript(updateScript);

        SelectRowsResponse updateResponse = dgen.getRowsFromServer(createDefaultConnection());
        Map<String, Object> thirdAddedSample = updateResponse.getRows()
                .stream()
                .filter(a-> a.get("Name").equals("another really new one"))
                .findFirst().orElse(null);

        assertNotNull("expect a new sample to be added to the set", thirdAddedSample);
        assertEquals(17.0, thirdAddedSample.get("volume"));
        assertEquals("also new from api", thirdAddedSample.get("mvStringData"));
        assertEquals("N", thirdAddedSample.get("mvstringdata_mvindicator"));

        //TODO: validate the update sample scenario if it ever works
        // thirdAddedSample receiving no mvIndicator value is a repro of https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=37786
    }


    @Test
    @Ignore
    public void deriveSamplesOverSaveBatchAPI() throws Exception
    {
        String sampleTypeName = "DerivedSamples";
        String samplesFolder = "DerivedSamplesFolder";
        _containerHelper.createSubfolder(getProjectName(), samplesFolder);

        // create assay
        String assaySubfolder = "DeriveAssayFolder";
        String assayName = "AssayForSaveBatchDerivation";
        generateAssay(assaySubfolder, assayName);
        List<TestDataGenerator> dataGenerators = generateAssayData(new FieldDefinition.LookupInfo(getProjectName() + "/" + assaySubfolder,
                "assay.General.AssayForSaveBatchDerivation_assay", "Runs"));
        insertAssayData(assayName, dataGenerators);

        // get the sampleID, it will be needed later
        waitAndClickAndWait(Locator.linkWithText("Assay List"));
        DataRegionTable assayList = DataRegionTable.DataRegion(getDriver()).withName("AssayList").waitFor();
        CustomizeView customizeView = assayList.openCustomizeGrid();
        customizeView.showHiddenItems();
        customizeView.addColumn("RowId");
        customizeView.clickSave().save();   // make this the default view
        String assayIdStringValue = assayList.getRowDataAsMap(0).get("RowId");

        // create sampleset
        navigateToFolder(getProjectName(), samplesFolder);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", sampleTypeName, getProjectName() + "/" + samplesFolder)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("volume", FieldDefinition.ColumnType.Decimal),
                        new FieldDefinition("color", FieldDefinition.ColumnType.String)
                ));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        dgen.addCustomRow(Map.of("name", "1st", "volume", 13.5));
        dgen.addCustomRow(Map.of("name", "2nd", "volume", 15.5));
        dgen.addCustomRow(Map.of("name", "3rd", "volume", 16.5));
        dgen.addCustomRow(Map.of("name", "4th", "volume", 17.5));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

        // materialoutputs reference the sampleset by name
        JSONObject sampleType = new JSONObject();
        sampleType.put("sampleSet", sampleTypeName);

        // the batch
        Batch batch = new Batch();
        batch.setName("derivation 1");

        // material one
        JSONObject fifthMaterial = new JSONObject();
        fifthMaterial.put("name", "5th");
        fifthMaterial.put("sampleSet", sampleType);
        JSONObject fifthProps = new JSONObject();
        fifthProps.put("volume", 18.5);
        fifthProps.put("color", "blue");
        fifthMaterial.put("properties", fifthProps);
        Material material5 = new Material(fifthMaterial);

        // material 2
        JSONObject sixthMaterial = new JSONObject();
        sixthMaterial.put("name", "6th");
        sixthMaterial.put("sampleSet", sampleType);
        JSONObject sixthProps = new JSONObject();
        sixthProps.put("volume", 21.5);
        sixthProps.put("color", "green");
        sixthMaterial.put("properties", sixthProps);
        Material material6 = new Material(sixthMaterial);

        JSONObject sampleMaterial7 = new JSONObject();
        sampleMaterial7.put("name", "7th");
        sampleMaterial7.put("sampleSet", sampleType);
        Material material7 = new Material(sampleMaterial7);

        JSONObject sampleMaterial4 = new JSONObject();
        sampleMaterial4.put("name", "4th");
        sampleMaterial4.put("sampleSet", sampleType);
        Material material4 = new Material(sampleMaterial4);

        Run runA = new Run();
        runA.setProperties(Collections.singletonMap("RunFileField", "foo.xls"));
        Data runAData = new Data();
        runAData.setName("joe");
        runAData.setPipelinePath("/");
        runA.setDataInputs(Arrays.asList(runAData));
        runA.setName("saveBatchMatrials A");
        runA.setMaterialOutputs(Arrays.asList(material5, material6));       // 5 and 6 don't exist yet, they should be created as outputs

        Run runB = new Run();
        runB.setProperties(Collections.singletonMap("RunFileField", "foo.xls"));
        Data runBData = new Data();
        runBData.setName("billy");
        runBData.setPipelinePath("/");
        runB.setDataInputs(Arrays.asList(runBData));
        runB.setName("saveBatchMaterials B");
        runB.setMaterialInputs(Arrays.asList(material4)); // material4 should already exist
        runB.setMaterialOutputs(Arrays.asList(material7));

        batch.getRuns().add(runA);
        batch.getRuns().add(runB);

        SaveAssayBatchCommand saveAssayBatchCommand = new SaveAssayBatchCommand(SaveAssayBatchCommand.SAMPLE_DERIVATION_PROTOCOL, batch);
        //saveAssayBatchCommand.setAssayId(Integer.parseInt(assayIdStringValue));
        SaveAssayBatchResponse saveAssayBatchResponse;
        saveAssayBatchResponse = saveAssayBatchCommand.execute(createDefaultConnection(), getProjectName() + "/" + samplesFolder);

        refresh();
        // add a bodyWebPart here to make viewing the sampleset easier while debugging
        new PortalHelper(getDriver()).addBodyWebPart("Sample Types");
        refresh();
        DataRegionTable sampleTypeList =  DataRegionTable.DataRegion(getDriver()).withName(SAMPLE_TYPE_DATA_REGION_NAME).waitFor();
        waitAndClick(Locator.linkWithText(sampleTypeName));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        // get the samples from the sample type
        SelectRowsResponse sampleTypeResponse = dgen.getRowsFromServer(createDefaultConnection());

        // at this point, the API either doesn't work or requires some hack to make it work if you aren't
        // importing from a file.
        // the Javascript API works if you feed it dummy datarows and a per-run properties, the Java api
        // treats that sort of input as invalid and throws errors.
        // If you provide a valid pipeline root that doesn't find files or datas, the API does not
        // convert materialoutputs into created sample materials
        // the materialoutputs are present, but the API does not create samples derived from the runs.

    }

    private void generateAssay(String subfolderName, String assayName)
    {
        _containerHelper.createSubfolder(getProjectName(), subfolderName);
        navigateToFolder(getProjectName(), subfolderName);
        goToManageAssays();
        ReactAssayDesignerPage assayDesignerPage = _assayHelper.createAssayDesign("General", assayName);
        DomainFormPanel dataPropertiesPanel = assayDesignerPage.goToResultsFields();
        dataPropertiesPanel.addField("SampleName");
        dataPropertiesPanel.addField("SampleVolume").setType(FieldDefinition.ColumnType.Decimal);
        assayDesignerPage.clickFinish();
    }

    /**
     * generates data (runs) for the gpat assay used by tests in this class.
     * @param assayLookup : specifies the container, schema, name of the assay's run table
     * @return
     */
    private List<TestDataGenerator> generateAssayData(FieldDefinition.LookupInfo assayLookup)
    {
        List<PropertyDescriptor> resultsFieldset = List.of(
                new FieldDefinition("ParticipantID",FieldDefinition.ColumnType.String),
                new FieldDefinition("Date", FieldDefinition.ColumnType.DateAndTime),
                new FieldDefinition("SampleName", FieldDefinition.ColumnType.String),
                new FieldDefinition("SampleVolume", FieldDefinition.ColumnType.DateAndTime));

        TestDataGenerator dgen1 = new TestDataGenerator(assayLookup)
                .withColumns(resultsFieldset)
                .addCustomRow(Map.of("ParticipantID", "Jeff", "Date", "11/11/2018", "SampleName", "Green", "SampleVolume", 12.5))
                .addCustomRow(Map.of("ParticipantID", "Jim", "Date", "11/12/2018", "SampleName", "Red", "SampleVolume", 14.5))
                .addCustomRow(Map.of("ParticipantID", "Billy", "Date", "11/13/2018", "SampleName", "Yellow", "SampleVolume", 17.5))
                .addCustomRow(Map.of("ParticipantID", "Michael", "Date", "11/14/2018", "SampleName", "Orange", "SampleVolume", 11.5));

        TestDataGenerator dgen2 = new TestDataGenerator(assayLookup)
                .withColumns(resultsFieldset)
                .addCustomRow(Map.of("ParticipantID", "Harry", "Date", "10/11/2018", "SampleName", "Green", "SampleVolume", 12.5))
                .addCustomRow(Map.of("ParticipantID", "William", "Date", "10/12/2018", "SampleName", "Red", "SampleVolume", 14.5))
                .addCustomRow(Map.of("ParticipantID", "Jenny", "Date", "10/13/2018", "SampleName", "Yellow", "SampleVolume", 17.5))
                .addCustomRow(Map.of("ParticipantID", "Hermione", "Date", "10/14/2018", "SampleName", "Orange", "SampleVolume", 11.5));

        TestDataGenerator dgen3 = new TestDataGenerator(assayLookup)
                .withColumns(resultsFieldset)
                .addCustomRow(Map.of("ParticipantID", "George", "Date", "10/11/2018", "SampleName", "Green", "SampleVolume", 12.5))
                .addCustomRow(Map.of("ParticipantID", "Arthur", "Date", "10/12/2018", "SampleName", "Red", "SampleVolume", 14.5))
                .addCustomRow(Map.of("ParticipantID", "Colin", "Date", "10/13/2018", "SampleName", "Yellow", "SampleVolume", 17.5))
                .addCustomRow(Map.of("ParticipantID", "Ronald", "Date", "10/14/2018", "SampleName", "Orange", "SampleVolume", 11.5));

        return Arrays.asList(dgen1, dgen2, dgen3);
    }

    private void insertAssayData(String assayName, List<TestDataGenerator> dataGenerators)
    {
        // assumes we are at the assay list
        waitAndClickAndWait(Locator.linkWithText(assayName));
        AssayRunsPage assayRunsPage = new AssayRunsPage(getDriver());
        DataRegionTable runsTable = assayRunsPage.getTable();
        runsTable.clickHeaderButton("Import Data");
        clickButton("Next");

        int limit = dataGenerators.size();
        int imported = 0;
        for(TestDataGenerator dataGen : dataGenerators)
        {
            AssayImportPage page = new AssayImportPage(getDriver())
                    .setNamedTextAreaValue("TextAreaDataCollector.textArea",
                    dataGen.writeTsvContents());
            imported++;

            if(imported < limit)
                page.clickSaveAndImportAnother();
            else
                page.clickSaveAndFinish();
        }
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "SampleTypeRemoteAPITest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }
}
