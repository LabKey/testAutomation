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

import org.json.simple.JSONObject;
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
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.pages.assay.AssayImportPage;
import org.labkey.test.pages.assay.AssayRunsPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        String missingValueTable = "mvSamples";

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

        /* SQL and PG handle casing differently- and labkey passes the differently-cased field names straight through.
        * Until we figure out a way to do case-insensitive matching over xpath, fork in the test code based on which DB we're running */
        if(WebTestHelper.getDatabaseType().equals(WebTestHelper.DatabaseType.MicrosoftSQLServer))
            selectOptionByText(Locator.tagWithAttribute("select", "name", "quf_mvStringData_MVIndicator"), "Q");
        else
            selectOptionByText(Locator.tagWithAttribute("select", "name", "quf_mvstringdata_mvindicator"), "Q");
        clickButton("Submit");

        materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        materialsList.clickEditRow(1);
        setFormElement(Locator.input("quf_mvStringData"), "otherValue");
        if(WebTestHelper.getDatabaseType().equals(WebTestHelper.DatabaseType.MicrosoftSQLServer))
            selectOptionByText(Locator.tagWithAttribute("select", "name", "quf_mvStringData_MVIndicator"), "N");
        else
            selectOptionByText(Locator.tagWithAttribute("select", "name", "quf_mvstringdata_mvindicator"), "N");
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

    @Test
    public void insertSamplesOverSaveBatchAPI() throws Exception
    {
        String missingValueTable = "mvAssaySamples";
        String missingValueSamplesFolder = "mvAssaySamplesFolder";
        _containerHelper.createSubfolder(getProjectName(), missingValueSamplesFolder);

        // create assay
        String assaySubfolder = "TestAssayFolder";
        String assayName = "AssayForSampleDerivation";
        generateAssay(assaySubfolder, assayName)
                .addDataField("SampleName", "SampleName", FieldDefinition.ColumnType.String)
                .addDataField("SampleVolume", "SampleVolume", FieldDefinition.ColumnType.Double)
                .saveAndClose();
        List<TestDataGenerator> dataGenerators = generateAssayData(new FieldDefinition.LookupInfo(getProjectName() + "/" + assaySubfolder,
                "assay.General.AssayForSampleDerivation_assay", "Runs"));
        insertAssayData(assayName, dataGenerators);

        // get the sampleID, it will be needed later
        waitAndClickAndWait(Locator.linkWithText("Assay List"));
        DataRegionTable assayList = DataRegionTable.DataRegion(getDriver()).withName("AssayList").waitFor();
        CustomizeView customizeView = assayList.openCustomizeGrid();
        customizeView.showHiddenItems();
        customizeView.addColumn("ROWID");
        customizeView.clickSave().save();   // make this the default view
        String assayIdStringValue = assayList.getRowDataAsMap(0).get("RowId");

        // create sampleset
        navigateToFolder(getProjectName(), missingValueSamplesFolder);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", missingValueTable, getProjectName() + "/" + missingValueSamplesFolder)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("mvStringData", FieldDefinition.ColumnType.String)
                                .setMvEnabled(true).setLabel("MV Field"),
                        TestDataGenerator.simpleFieldDef("volume", FieldDefinition.ColumnType.Double)
                ));
        dgen.createDomain(createDefaultConnection(true), "SampleSet");
        dgen.addCustomRow(Map.of("name", "1st", "volume", 13.5));
        dgen.addCustomRow(Map.of("name", "2nd", "volume", 15.5));
        dgen.addCustomRow(Map.of("name", "3rd", "volume", 16.5));
        dgen.addCustomRow(Map.of("name", "4th", "volume", 17.5));
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());

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
                "        // create some new samples in the target sample set if it doesn't exist\n" +
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
        new PortalHelper(getDriver()).addBodyWebPart("Sample Sets");
        DataRegionTable sampleSetList =  DataRegionTable.DataRegion(getDriver()).withName("SampleSet").waitFor();
        waitAndClick(Locator.linkWithText(missingValueTable));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        // grab the new samples
        SelectRowsResponse sampleSetResponse = dgen.getRowsFromServer(createDefaultConnection(true));
        Map<String, Object> firstAddedSample = sampleSetResponse.getRows()
                .stream()
                .filter(a-> a.get("Name").equals("another new one"))
                .findFirst().orElse(null);;
        Map<String, Object> secondAddedSample = sampleSetResponse.getRows()
                .stream()
                .filter(a-> a.get("Name").equals("a new one"))
                .findFirst().orElse(null);;

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
                "        // create/update samples in the target sample set\n" +
                "        { rowId: "+secondAddedRowId+", sampleSet: { name: 'mvAssaySamples' }, properties: { 'mvStringData': 'newer from api', volume: 7 } },\n" +
                "        { name: 'another really new one', sampleSet: { name: 'mvAssaySamples' }, properties: { 'mvStringData': 'also new from api', volume:17 } }\n" +
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

        SelectRowsResponse updateResponse = dgen.getRowsFromServer(createDefaultConnection(true));
        Map<String, Object> thirdAddedSample = updateResponse.getRows()
                .stream()
                .filter(a-> a.get("Name").equals("another really new one"))
                .findFirst().orElse(null);

        assertNotNull("expect a new sample to be added to the set", thirdAddedSample);
        assertEquals(17.0, thirdAddedSample.get("volume"));
        assertEquals("also new from api", thirdAddedSample.get("mvStringData"));
    }


    @Test
    @Ignore
    public void deriveSamplesOverSaveBatchAPI() throws Exception
    {
        String sampleSetName = "DerivedSamples";
        String samplesFolder = "DerivedSamplesFolder";
        _containerHelper.createSubfolder(getProjectName(), samplesFolder);

        // create assay
        String assaySubfolder = "DeriveAssayFolder";
        String assayName = "AssayForSaveBatchDerivation";
        generateAssay(assaySubfolder, assayName)
                .addDataField("SampleName", "SampleName", FieldDefinition.ColumnType.String)
                .addDataField("SampleVolume", "SampleVolume", FieldDefinition.ColumnType.Double)
                .saveAndClose();
        List<TestDataGenerator> dataGenerators = generateAssayData(new FieldDefinition.LookupInfo(getProjectName() + "/" + assaySubfolder,
                "assay.General.AssayForSaveBatchDerivation_assay", "Runs"));
        insertAssayData(assayName, dataGenerators);

        // get the sampleID, it will be needed later
        waitAndClickAndWait(Locator.linkWithText("Assay List"));
        DataRegionTable assayList = DataRegionTable.DataRegion(getDriver()).withName("AssayList").waitFor();
        CustomizeView customizeView = assayList.openCustomizeGrid();
        customizeView.showHiddenItems();
        customizeView.addColumn("ROWID");
        customizeView.clickSave().save();   // make this the default view
        String assayIdStringValue = assayList.getRowDataAsMap(0).get("RowId");

        // create sampleset
        navigateToFolder(getProjectName(), samplesFolder);
        TestDataGenerator dgen = new TestDataGenerator("exp.materials", sampleSetName, getProjectName() + "/" + samplesFolder)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("volume", FieldDefinition.ColumnType.Double),
                        TestDataGenerator.simpleFieldDef("color", FieldDefinition.ColumnType.String)
                ));
        dgen.createDomain(createDefaultConnection(true), "SampleSet");
        dgen.addCustomRow(Map.of("name", "1st", "volume", 13.5));
        dgen.addCustomRow(Map.of("name", "2nd", "volume", 15.5));
        dgen.addCustomRow(Map.of("name", "3rd", "volume", 16.5));
        dgen.addCustomRow(Map.of("name", "4th", "volume", 17.5));
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        // materialoutputs reference the sampleset by name
        JSONObject sampleSet = new JSONObject();
        sampleSet.put("sampleSet", sampleSetName);

        // the batch
        Batch batch = new Batch();
        batch.setName("derivation 1");

        // material one
        JSONObject fifthMaterial = new JSONObject();
        fifthMaterial.put("name", "5th");
        fifthMaterial.put("sampleSet", sampleSet);
        JSONObject fifthProps = new JSONObject();
        fifthProps.put("volume", 18.5);
        fifthProps.put("color", "blue");
        fifthMaterial.put("properties", fifthProps);
        Material material5 = new Material(fifthMaterial);

        // material 2
        JSONObject sixthMaterial = new JSONObject();
        sixthMaterial.put("name", "6th");
        sixthMaterial.put("sampleSet", sampleSet);
        JSONObject sixthProps = new JSONObject();
        sixthProps.put("volume", 21.5);
        sixthProps.put("color", "green");
        sixthMaterial.put("properties", sixthProps);
        Material material6 = new Material(sixthMaterial);

        JSONObject sampleMaterial7 = new JSONObject();
        sampleMaterial7.put("name", "7th");
        sampleMaterial7.put("sampleSet", sampleSet);
        Material material7 = new Material(sampleMaterial7);

        JSONObject sampleMaterial4 = new JSONObject();
        sampleMaterial4.put("name", "4th");
        sampleMaterial4.put("sampleSet", sampleSet);
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
        saveAssayBatchResponse = saveAssayBatchCommand.execute(createDefaultConnection(true), getProjectName() + "/" + samplesFolder);

        refresh();
        // add a bodyWebPart here to make viewing the sampleset easier while debugging
        new PortalHelper(getDriver()).addBodyWebPart("Sample Sets");
        refresh();
        DataRegionTable sampleSetList =  DataRegionTable.DataRegion(getDriver()).withName("SampleSet").waitFor();
        waitAndClick(Locator.linkWithText(sampleSetName));
        DataRegionTable materialsList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        // get the samples from the sampleset
        SelectRowsResponse sampleSetResponse = dgen.getRowsFromServer(createDefaultConnection(true));

        // at this point, the API either doesn't work or requires some hack to make it work if you aren't
        // importing from a file.
        // the Javascript API works if you feed it dummy datarows and a per-run properties, the Java api
        // treats that sort of input as invalid and throws errors.
        // If you provide a valid pipeline root that doesn't find files or datas, the API does not
        // convert materialoutputs into created sample materials
        // the materialoutputs are present, but the API does not create samples derived from the runs.

    }

    private AssayDesignerPage generateAssay(String subfolderName, String assayName)
    {
        _containerHelper.createSubfolder(getProjectName(), subfolderName);
        navigateToFolder(getProjectName(), subfolderName);
        goToManageAssays();
        AssayDesignerPage designerPage = _assayHelper.createAssayAndEdit("General", assayName);

        return designerPage;
    }

    /**
     * generates data (runs) for the gpat assay used by tests in this class.
     * @param assayLookup : specifies the container, schema, name of the assay's run table
     * @return
     */
    private List<TestDataGenerator> generateAssayData(FieldDefinition.LookupInfo assayLookup)
    {
        List<FieldDefinition> resultsFieldset = List.of(
                TestDataGenerator.simpleFieldDef("ParticipantID",FieldDefinition.ColumnType.String),
                TestDataGenerator.simpleFieldDef("Date", FieldDefinition.ColumnType.DateTime),
                TestDataGenerator.simpleFieldDef("SampleName", FieldDefinition.ColumnType.String),
                TestDataGenerator.simpleFieldDef("SampleVolume", FieldDefinition.ColumnType.Double));

        TestDataGenerator dgen1 = new TestDataGenerator(assayLookup)
                .withColumnSet(resultsFieldset)
                .addCustomRow(Map.of("ParticipantID", "Jeff", "Date", "11/11/2018", "SampleName", "Green", "SampleVolume", 12.5))
                .addCustomRow(Map.of("ParticipantID", "Jim", "Date", "11/12/2018", "SampleName", "Red", "SampleVolume", 14.5))
                .addCustomRow(Map.of("ParticipantID", "Billy", "Date", "11/13/2018", "SampleName", "Yellow", "SampleVolume", 17.5))
                .addCustomRow(Map.of("ParticipantID", "Michael", "Date", "11/14/2018", "SampleName", "Orange", "SampleVolume", 11.5));

        TestDataGenerator dgen2 = new TestDataGenerator(assayLookup)
                .withColumnSet(resultsFieldset)
                .addCustomRow(Map.of("ParticipantID", "Harry", "Date", "10/11/2018", "SampleName", "Green", "SampleVolume", 12.5))
                .addCustomRow(Map.of("ParticipantID", "William", "Date", "10/12/2018", "SampleName", "Red", "SampleVolume", 14.5))
                .addCustomRow(Map.of("ParticipantID", "Jenny", "Date", "10/13/2018", "SampleName", "Yellow", "SampleVolume", 17.5))
                .addCustomRow(Map.of("ParticipantID", "Hermione", "Date", "10/14/2018", "SampleName", "Orange", "SampleVolume", 11.5));

        TestDataGenerator dgen3 = new TestDataGenerator(assayLookup)
                .withColumnSet(resultsFieldset)
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
            new AssayImportPage(getDriver()).setNamedTextAreaValue("TextAreaDataCollector.textArea",
                    dataGen.writeTsvContents());
            imported++;

            if(imported < limit)
                clickButton("Save and Import Another Run");
            else
                clickButton("Save and Finish");
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
        return "SampleSetRemoteAPITest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }
}
