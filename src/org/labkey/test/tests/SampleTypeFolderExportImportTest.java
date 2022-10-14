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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.GetProtocolCommand;
import org.labkey.remoteapi.assay.ImportRunCommand;
import org.labkey.remoteapi.assay.Protocol;
import org.labkey.remoteapi.assay.ProtocolResponse;
import org.labkey.remoteapi.assay.SaveProtocolCommand;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.pages.admin.ExportFolderPage;
import org.labkey.test.pages.admin.FolderManagementPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.DataClassAPIHelper;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category({Daily.class})
public class SampleTypeFolderExportImportTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleTypeExportFolderTest";
    private static final String IMPORT_PROJECT_NAME = "SampleTypeImportFolderTest";

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
        SampleTypeFolderExportImportTest init = (SampleTypeFolderExportImportTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        // Delete the import project if it exists.
        _containerHelper.deleteProject(IMPORT_PROJECT_NAME, false);
        // create the import project because we'll need it
        _containerHelper.createProject(IMPORT_PROJECT_NAME);

        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(PROJECT_NAME);

        projectMenu().navigateToProject(PROJECT_NAME);
        portalHelper.addWebPart("Sample Types");
        portalHelper.addWebPart("Experiment Runs");

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
        Collections.sort(list01, (Map<String, String> o1, Map<String, String> o2)->
                {
                    return o1.get("Name").compareTo(o2.get("Name"));
                }
        );

        Collections.sort(list02, (Map<String, String> o1, Map<String, String> o2)->
                {
                    return o1.get("Name").compareTo(o2.get("Name"));
                }
        );

        boolean areEqual = true;

        for(int i = 0; i < list01.size(); i++)
        {
            if(!list01.get(i).equals(list02.get(i)))
            {
                if(logMismatch)
                {
                    StringBuilder errorMsg = new StringBuilder();
                    errorMsg.append("\n*************** ERROR ***************");
                    errorMsg.append("\nFound a mismatch in the lists.");
                    errorMsg.append("\nlist01(" + i + "): " + list01.get(i));
                    errorMsg.append("\nlist02(" + i + "): " + list02.get(i));
                    errorMsg.append("\n*************** ERROR ***************");
                    log(errorMsg.toString());
                }
                areEqual = false;
            }
        }

        return areEqual;
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
    public void testExportAndImportWithMissingAndRequiredFields()
    {
        final String SAMPLE_TYPE_NAME = "ExportMissingValues";

        final String REQUIRED_FIELD_NAME = "field01";
        final String REQUIRED_FIELD_DISPLAY_NAME = "Field01";
        final String MISSING_FIELD_NAME = "field02";
        final String MISSING_FIELD_DISPLAY_NAME = "Field02";
        final String INDICATOR_FIELD_NAME = MISSING_FIELD_NAME + "MVIndicator";

        StringBuilder errorLog = new StringBuilder();

        log("Create a Sample Type that has missing values.");

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

        List<Map<String, String>> sampleData = new ArrayList<>();
        List<Map<String, String>> expectedValuesInDB = new ArrayList<>();

        String[] sampleNames = {"mv01", "mv02", "mv03", "mv04", "mv05", "mv06", "mv07", "mv08", "DerivedSample01"};

        // Later the test will query the DB to validate that the imported data is as expected. This becomes a little
        // tricky because the missing value fields only have a value in the _mvindicator field, any value in the filed is removed.
        // To work around this I used a second list (expectedValuesInDB) to check against the DB.
        sampleData.add(Map.of("Name", sampleNames[0], REQUIRED_FIELD_NAME, "aa_mv01", MISSING_FIELD_NAME, "This value is here.", INDICATOR_FIELD_NAME, ""));
        expectedValuesInDB.add(Map.of("Name", sampleNames[0], REQUIRED_FIELD_NAME, "aa_mv01", MISSING_FIELD_NAME, "This value is here.", INDICATOR_FIELD_NAME, ""));

        sampleData.add(Map.of("Name", sampleNames[1], REQUIRED_FIELD_NAME, "bb_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, "Q"));
        expectedValuesInDB.add(Map.of("Name", sampleNames[1], REQUIRED_FIELD_NAME, "bb_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, "Q"));

        sampleData.add(Map.of("Name", sampleNames[2], REQUIRED_FIELD_NAME, "cc_mv01", MISSING_FIELD_NAME, "Just to break things up.", INDICATOR_FIELD_NAME, ""));
        expectedValuesInDB.add(Map.of("Name", sampleNames[2], REQUIRED_FIELD_NAME, "cc_mv01", MISSING_FIELD_NAME, "Just to break things up.", INDICATOR_FIELD_NAME, ""));

        sampleData.add(Map.of("Name", sampleNames[3], REQUIRED_FIELD_NAME, "ee_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, ""));
        expectedValuesInDB.add(Map.of("Name", sampleNames[3], REQUIRED_FIELD_NAME, "ee_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, ""));

        sampleData.add(Map.of("Name", sampleNames[4], REQUIRED_FIELD_NAME, "dd_mv01", MISSING_FIELD_NAME, "X", INDICATOR_FIELD_NAME, ""));
        expectedValuesInDB.add(Map.of("Name", sampleNames[4], REQUIRED_FIELD_NAME, "dd_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, ""));

        sampleData.add(Map.of("Name", sampleNames[5], REQUIRED_FIELD_NAME, "ff_mv01", MISSING_FIELD_NAME, "N", INDICATOR_FIELD_NAME, "N"));
        expectedValuesInDB.add(Map.of("Name", sampleNames[5], REQUIRED_FIELD_NAME, "ff_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, "N"));

        sampleData.add(Map.of("Name", sampleNames[6], REQUIRED_FIELD_NAME, "gg_mv01", MISSING_FIELD_NAME, "Here is a valid string value.", INDICATOR_FIELD_NAME, "Q"));
        expectedValuesInDB.add(Map.of("Name", sampleNames[6], REQUIRED_FIELD_NAME, "gg_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, "Q"));

        sampleData.add(Map.of("Name", sampleNames[7], REQUIRED_FIELD_NAME, "hh_mv01", MISSING_FIELD_NAME, "X", INDICATOR_FIELD_NAME, "X"));
        expectedValuesInDB.add(Map.of("Name", sampleNames[7], REQUIRED_FIELD_NAME, "hh_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, "X"));

        log("Create the sample type named '" + SAMPLE_TYPE_NAME + "' and add the fields.");
        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        List<FieldDefinition> fields = new ArrayList<>();
        fields.add(new FieldDefinition(REQUIRED_FIELD_NAME, FieldDefinition.ColumnType.String)
                .setMvEnabled(false)
                .setRequired(true));
        fields.add(new FieldDefinition(MISSING_FIELD_NAME, FieldDefinition.ColumnType.String)
                .setMvEnabled(true)
                .setRequired(false));
        SampleTypeDefinition definition = new SampleTypeDefinition(SAMPLE_TYPE_NAME).setFields(fields);
        sampleHelper.createSampleType(definition);

        clickAndWait(Locator.linkWithText(SAMPLE_TYPE_NAME));
        sampleHelper = new SampleTypeHelper(this);

        log("Bulk import the samples.");
        sampleHelper.bulkImport(sampleData);

        log("Change the view so the missing value indicator is there and for the screen shot is useful on failure.");
        sampleHelper = new SampleTypeHelper(this);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        CustomizeView cv = drtSamples.openCustomizeGrid();
        cv.showHiddenItems();
        cv.addColumn(INDICATOR_FIELD_NAME);
        cv.saveCustomView();

        log("Derive a sample from the given samples, this will create an experiment run which is needed for export.");

        drtSamples.checkAllOnPage();
        clickAndWait(Locator.lkButtonContainingText("Derive Sample"));

        selectOptionByText(Locator.name("targetSampleTypeId"), SAMPLE_TYPE_NAME + " in /" + PROJECT_NAME);
        clickButtonContainingText("Next");

        // TODO: Should validate that the Derive Samples action shows the various fields as expected. That is the required and missing value fields should have the correct input type. Will be fixed in 19.2.
        setFormElement(Locator.tagWithName("input", "outputSample1_Name"), sampleNames[8]);
        setFormElement(Locator.tagWithName("input", "outputSample1_" + REQUIRED_FIELD_NAME), "Required text for this field.");
        setFormElement(Locator.tagWithName("input", "outputSample1_" + MISSING_FIELD_NAME), "Q");
        clickButtonContainingText("Submit");

        // TODO: There is a bug where derived values do not honor missing value fields (treat them as a text field). So the indicator field for this sample will be empty. Will be fixed in 19.2.
        expectedValuesInDB.add(Map.of("Name", sampleNames[8], REQUIRED_FIELD_NAME, "Required text for this field.", MISSING_FIELD_NAME, "Q", INDICATOR_FIELD_NAME, ""));

        // Wait for the header to show up, and view this as success.
        waitForElementToBeVisible(Locator.tagWithText("h3", "Sample DerivedSample01"));

        goToProjectHome();

        log("Export folder. Sample Types export should be selected by default.");
        FolderManagementPage folderManagementPage = goToFolderManagement();
        folderManagementPage.goToExportTab();

        File exportedFolderFile = doAndWaitForDownload(()->findButton("Export").click());

        log("Folder should have been exported!");

        log("Import the previously exported folder.");

        goToProjectHome(IMPORT_PROJECT_NAME);

        importFolderFromZip(exportedFolderFile, false, 1);

        log("Folder should now have been imported.");

        goToProjectHome(IMPORT_PROJECT_NAME);

        log("Validate that the number of Sample Types in the imported folder is the expected value. If not fail test.");
        assertTrue("Does not look like the Sample Type has been imported.", isElementVisible(Locator.linkWithText(SAMPLE_TYPE_NAME)));

        DataRegionTable sampleTypesDataRegion = new DataRegionTable(SampleTypeAPIHelper.SAMPLE_TYPE_DATA_REGION_NAME, getWrappedDriver());

        assertEquals("Number of Sample Types not as expected.", 1, sampleTypesDataRegion.getDataRowCount());

        String sampleCount = sampleTypesDataRegion.getDataAsText(0, "SampleCount" );

        log("Check some of the other expected value. If they are not just log an error but continue testing.");
        String errorMsg;
        if(!sampleCount.trim().equalsIgnoreCase("9"))
        {
            errorMsg = "Number of samples imported not as expected.\nExpected '9', found '" + sampleCount.trim() +"'.";
            log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        clickAndWait(Locator.linkWithText(SAMPLE_TYPE_NAME));

        sampleHelper = new SampleTypeHelper(this);
        DataRegionTable samplesDataRegion = sampleHelper.getSamplesDataRegionTable();

        log("Validated that all of the fields are present in the UI.");

        List<String> columnLabels = samplesDataRegion.getColumnLabels();

        log("Here are the fields visible: " + columnLabels);

        errorLog.append(checkDisplayFields("Name", columnLabels));
        errorLog.append(checkDisplayFields("Flag", columnLabels));
        errorLog.append(checkDisplayFields(REQUIRED_FIELD_DISPLAY_NAME, columnLabels));
        errorLog.append(checkDisplayFields(MISSING_FIELD_DISPLAY_NAME, columnLabels));

        log("Validate that there is a link to each of the samples.");

        log("Validated that the imported data is as expected by looking at the database");

        for(String sampleName : sampleNames)
        {

            if(!isElementVisible(Locator.linkWithText(sampleName)))
            {
                errorMsg = "Did not find a link for sample named '" + sampleName + "'.";
                log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

                errorLog.append(errorMsg);
                errorLog.append("\n");
            }

        }

        // TODO: Checking the field values in the DB will fail because export/import for Sample Types doesn't honor missing value fields. This will be fixed in 19.2.
        /*
        List<Map<String, String>> resultsFromDB = getSampleDataFromDB("/" + IMPORT_PROJECT_NAME, SAMPLE_TYPE_NAME, Arrays.asList("Name", REQUIRED_FIELD_NAME, MISSING_FIELD_NAME, INDICATOR_FIELD_NAME));

        Assert.assertTrue("Imported Sample Type data not as expected.", areDataListEqual(resultsFromDB, expectedValuesInDB));
        */

        if(errorLog.length() > 0)
            Assert.fail(errorLog.toString());

        log("All done.");
    }

    @Test
    public void testExportImportDerivedSamples() throws Exception
    {
        String subfolder = "derivedSamplesExportFolder";
        String subfolderPath = getProjectName() + "/" + subfolder;
        String dataClass = "parentDataClass";
        String parentSampleType = "parentSamples";
        String testSamples = "testSamples";
        String importFolder = "derivedSamplesImportFolder";

        // create a subfolder in the import destination project
        _containerHelper.createSubfolder(IMPORT_PROJECT_NAME, importFolder);
        // create a subfolder in the project to hold types we'll export in this test method
        _containerHelper.createSubfolder(getProjectName(), subfolder);

        // arrange - 2 sample types, one with samples derived from parents in the other (and also parents in the same one)
        List<FieldDefinition> testFields = SampleTypeAPIHelper.sampleTypeTestFields();
        DataClassDefinition dataClassType = new DataClassDefinition(dataClass).setFields(DataClassAPIHelper.dataClassTestFields());
        SampleTypeDefinition parentType = new SampleTypeDefinition(parentSampleType).setFields(testFields);
        SampleTypeDefinition testSampleType = new SampleTypeDefinition(testSamples).setFields(testFields)
                .addParentAlias("Parent", parentSampleType) // to derive from parent sampleType
                .addDataParentAlias("DataClassParent", dataClass)
                .addParentAlias("SelfParent"); // to derive from samles in the current type

        TestDataGenerator dataClassDgen = DataClassAPIHelper.createEmptyDataClass(subfolderPath, dataClassType);
        dataClassDgen.addCustomRow(Map.of("Name", "data1", "intColumn", 1, "stringColumn", "one"));
        dataClassDgen.addCustomRow(Map.of("Name", "data2", "intColumn", 2, "stringColumn", "two"));
        dataClassDgen.addCustomRow(Map.of("Name", "data3", "intColumn", 3, "stringColumn", "three"));
        dataClassDgen.insertRows();

        TestDataGenerator parentDgen = SampleTypeAPIHelper.createEmptySampleType(subfolderPath, parentType);
        parentDgen.addCustomRow(Map.of("Name", "Parent1", "intColumn", 1, "floatColumn", 1.1, "stringColumn", "one"));
        parentDgen.addCustomRow(Map.of("Name", "Parent2", "intColumn", 2, "floatColumn", 2.2, "stringColumn", "two"));
        parentDgen.addCustomRow(Map.of("Name", "Parent3", "intColumn", 3, "floatColumn", 3.3, "stringColumn", "three"));
        parentDgen.insertRows();

        TestDataGenerator testDgen = SampleTypeAPIHelper.createEmptySampleType(subfolderPath, testSampleType);
        testDgen.addCustomRow(Map.of("Name", "Child1", "intColumn", 1, "decimalColumn", 1.1, "stringColumn", "one",
                "Parent", "Parent1"));
        testDgen.addCustomRow(Map.of("Name", "Child2", "intColumn", 2, "decimalColumn", 2.2, "stringColumn", "two",
                "Parent", "Parent2"));
        testDgen.addCustomRow(Map.of("Name", "Child3", "intColumn", 3, "decimalColumn", 3.3, "stringColumn", "three",
                "Parent", "Parent3", "DataClassParent", "data1"));
        testDgen.addCustomRow(Map.of("Name", "Child4", "intColumn", 4, "decimalColumn", 4.4, "stringColumn", "four",
                "Parent", "Parent3, Parent2"));
        testDgen.addCustomRow(Map.of("Name", "Child5", "intColumn", 5, "decimalColumn", 5.5, "stringColumn", "five",
                "Parent", "Parent1, Parent2"));
        testDgen.addCustomRow(Map.of("Name", "Child6", "intColumn", 6, "decimalColumn", 6.6, "stringColumn", "six",
                "Parent", "Parent3, Parent2", "SelfParent", "Child5"));
        testDgen.addCustomRow(Map.of("Name", "Child7", "intColumn", 7, "decimalColumn", 7.7, "stringColumn", "seven",
                "Parent", "Parent3, Parent2", "SelfParent", "Child5", "DataClassParent", "data2, data3"));
        testDgen.insertRows();

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Sample Types");
        portalHelper.addWebPart("Experiment Runs");

        DataRegionTable sourceRunsTable = DataRegionTable.DataRegion(getDriver()).withName("Runs").waitFor();
        List<String> runNames = sourceRunsTable.getColumnDataAsText("Name");

        clickAndWait(Locator.linkWithText(testSamples));
        DataRegionTable sourceTable = new SampleTypeHelper(this).getSamplesDataRegionTable();
        CustomizeView cv = sourceTable.openCustomizeGrid();
        cv.showHiddenItems();
        cv.addColumn("INPUTS/MATERIALS/PARENTSAMPLES");
        cv.addColumn("INPUTS/DATA/PARENTDATACLASS");
        cv.clickSave().save();
        List<Map<String, String>> sourceRowData = sourceTable.getTableData();

        // act - export to file and import the file to our designated import directory
        goToFolderManagement()
                .goToExportTab();
        File exportedFolderFile = doAndWaitForDownload(()->findButton("Export").click());

        goToProjectFolder(IMPORT_PROJECT_NAME, importFolder);
        importFolderFromZip(exportedFolderFile, false, 1);
        goToProjectFolder(IMPORT_PROJECT_NAME, importFolder);

        List<String> importedRunNames = DataRegionTable.DataRegion(getDriver()).withName("Runs").waitFor()
                .getColumnDataAsText("Name");
        for (String sourceRun : runNames)
        {
            assertThat("expect all runs to come through", importedRunNames, hasItems(sourceRun));
        }

        // we expect the sample types and experiment runs webparts to come across along with the folder import
        clickAndWait(Locator.linkWithText(testSamples));
        DataRegionTable destSamplesTable = new SampleTypeHelper(this).getSamplesDataRegionTable();
        CustomizeView cv2 = destSamplesTable.openCustomizeGrid();
        cv2.showHiddenItems();
        cv2.addColumn("INPUTS/MATERIALS/PARENTSAMPLES");
        cv.addColumn("INPUTS/DATA/PARENTDATACLASS");
        cv2.clickSave().save();

        // capture the data in the exported sampleType
        List<Map<String, String>> destRowData = destSamplesTable.getTableData();

        // now ensure expected data in the sampleType made it to the destination folder
        for (Map exportedRow : sourceRowData)
        {
            // find the map from the exported project with the same name
            Map<String, String> matchingMap = destRowData.stream().filter(a-> a.get("Name").equals(exportedRow.get("Name")))
                    .findFirst().orElse(null);
            assertNotNull("expect all matching rows to come through", matchingMap);

            assertThat("expect export and import values to be equivalent",
                    exportedRow.get("intColumn"), equalTo(matchingMap.get("intColumn")));
            assertThat("expect export and import values to be equivalent",
                    exportedRow.get("stringColumn"), equalTo(matchingMap.get("stringColumn")));
            assertThat("expect export and import values to be equivalent",
                    exportedRow.get("decimalColumn"), equalTo(matchingMap.get("decimalColumn")));

            List<String> sourceParents = Arrays.asList(exportedRow.get("Inputs/Materials/parentSamples").toString()
                    .replace(" ", "").split(","));
            String[] importedParents = matchingMap.get("Inputs/Materials/parentSamples").replace(" ", "").split(",");
            assertThat("expect parent sampleType derivation to round trip with equivalent values", sourceParents, hasItems(importedParents));
            List<String> sourceDataParents = Arrays.asList(exportedRow.get("Inputs/Data/parentDataClass").toString()
                    .replace(" ", "").split(","));
            String[] importedDataParents = matchingMap.get("Inputs/Data/parentDataClass").replace(" ", "").split(",");
            assertThat("expect parent dataClass derivation to round trip with equivalent values", sourceDataParents, hasItems(importedDataParents));
        }
    }

    @Test
    public void testExportImportSampleTypesWithAssayRuns() throws Exception
    {
        String subfolder = "samplesWithAssayRunsFolder";
        String subfolderPath = getProjectName() + "/" + subfolder;
        String testSamples = "testSamples";
        String assayName = "testAssay";
        String importFolder = "assaySamplesImportFolder";

        // create subfolders in export and import projects
        _containerHelper.createSubfolder(IMPORT_PROJECT_NAME, importFolder);
        _containerHelper.createSubfolder(getProjectName(), subfolder);

        // create a test sampleType
        List<FieldDefinition> testFields = SampleTypeAPIHelper.sampleTypeTestFields();
        SampleTypeDefinition testSampleType = new SampleTypeDefinition(testSamples).setFields(testFields)
                .addParentAlias("SelfParent"); // to derive from samles in the current type

        TestDataGenerator parentDgen = SampleTypeAPIHelper.createEmptySampleType(subfolderPath, testSampleType);
        parentDgen.addCustomRow(Map.of("Name", "sample1", "intColumn", 1, "decimalColumn", 1.1, "stringColumn", "one"));
        parentDgen.addCustomRow(Map.of("Name", "sample2", "intColumn", 2, "decimalColumn", 2.2, "stringColumn", "two"));
        parentDgen.addCustomRow(Map.of("Name", "sample3", "intColumn", 3, "decimalColumn", 3.3, "stringColumn", "three"));
        parentDgen.insertRows();

        // now define an assay that references it
        GetProtocolCommand getProtocolCommand = new GetProtocolCommand("General");
        ProtocolResponse getProtocolResponse = getProtocolCommand.execute(createDefaultConnection(), getProjectName());

        Protocol assayProtocol = getProtocolResponse.getProtocol();
        assayProtocol.setName(assayName)
                .setDescription("Just a test assay");

        Domain batchesDomain =  assayProtocol.getDomains().stream().filter(a->a.getName().equals("Batch Fields")).findFirst()
                .orElseThrow(()-> new IllegalStateException("The protocol template did not supply a [Batch Fields] domain"));
        List<PropertyDescriptor> batchFields = batchesDomain.getFields();   // keep the template-supplied fields, add the following
        batchFields.add(new PropertyDescriptor("batchField", "string"));
        batchesDomain.setFields(batchFields);
        Domain runsDomain =  assayProtocol.getDomains().stream().filter(a->a.getName().equals("Run Fields")).findFirst()
                .orElseThrow(()-> new IllegalStateException("The protocol template did not supply a [Run Fields] domain"));
        List<PropertyDescriptor> runFields = runsDomain.getFields();
        runFields.add(new PropertyDescriptor("runField", "string"));

        runsDomain.setFields(runFields);
        Domain resultsDomain = assayProtocol.getDomains().stream().filter(a->a.getName().equals("Data Fields")).findFirst()
                .orElseThrow(()-> new IllegalStateException("The protocol template did not supply a [Data Fields] domain"));
        List<PropertyDescriptor> resultsFields = resultsDomain.getFields();
        resultsFields.add(new PropertyDescriptor("resultData", "Result Data", "string"));
        resultsFields.add(new PropertyDescriptor("sampleId", "Sample Id", "string").setLookup("exp.materials", testSamples, subfolderPath));
        resultsDomain.setFields(resultsFields);
        SaveProtocolCommand saveProtocolCommand = new SaveProtocolCommand(assayProtocol);
        Protocol serverProtocol = saveProtocolCommand.execute(createDefaultConnection(), subfolderPath).getProtocol();

        // now save a couple of runs
        Integer protocolId = serverProtocol.getProtocolId();
        List<Map<String, Object>> runRecords1 = new ArrayList<>();
        runRecords1.add(Map.of("sampleId", "sample1", "resultData", "this thing"));
        runRecords1.add(Map.of("sampleId", "sample2", "resultData", "that thing"));
        runRecords1.add(Map.of("sampleId", "sample3", "resultData", "the other thing"));

        ImportRunCommand importRunCommand = new ImportRunCommand(protocolId, runRecords1);
        importRunCommand.setName("firstRun");
        importRunCommand.setBatchId(123);
        importRunCommand.execute(createDefaultConnection(), subfolderPath);

        List<Map<String, Object>> runRecords2 = new ArrayList<>();
        runRecords2.add(Map.of("sampleId", "sample1", "resultData", "more thing"));
        runRecords2.add(Map.of("sampleId", "sample2", "resultData", "less thing"));
        runRecords2.add(Map.of("sampleId", "sample3", "resultData", "the other other thing"));

        ImportRunCommand importRunCommand2 = new ImportRunCommand(protocolId, runRecords1);
        importRunCommand2.setName("secondRun");
        importRunCommand2.setBatchId(124);
        importRunCommand2.execute(createDefaultConnection(), subfolderPath);

        //
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Sample Types");
        portalHelper.addWebPart("Experiment Runs");
        portalHelper.addWebPart("Assay List");

        // capture the run data pre-export
        clickAndWait(Locator.linkWithText(assayName));
        DataRegionTable.DataRegion(getDriver()).withName("Runs").waitFor();
        clickAndWait(Locator.linkWithText("secondRun"));
        DataRegionTable dataTable = DataRegionTable.DataRegion(getDriver()).withName("Data").waitFor();
        List<Map<String, String>> exportData = new ArrayList<>();
        for (int i=0; i < dataTable.getDataRowCount(); i++)
        {
            exportData.add(dataTable.getRowDataAsMap(i));
        }

        // now export the current folder and import it to importProject
        goToFolderManagement()
                .goToExportTab();

        Checkbox checkbox = new Checkbox(Locator.tagWithText("label", ExportFolderPage.EXPERIMENTS_AND_RUNS)
                .precedingSibling("input").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT));
        checkbox.check();
        File exportedFolderFile = doAndWaitForDownload(()->findButton("Export").click());

        goToProjectFolder(IMPORT_PROJECT_NAME, importFolder);
        importFolderFromZip(exportedFolderFile, false, 1);
        goToProjectFolder(IMPORT_PROJECT_NAME, importFolder);

        // now validate run data made it
        clickAndWait(Locator.linkWithText(assayName));
        DataRegionTable.DataRegion(getDriver()).withName("Runs").waitFor();
        clickAndWait(Locator.linkWithText("secondRun"));
        DataRegionTable importedDataTable = DataRegionTable.DataRegion(getDriver()).withName("Data").waitFor();
        List<Map<String, String>> importData = new ArrayList<>();
        for (int i=0; i < importedDataTable.getDataRowCount(); i++)
        {
            importData.add(importedDataTable.getRowDataAsMap(i));
        }

        for (Map exportedRow : exportData)
        {
            // find the map from the exported project with the same name
            Map<String, String> matchingMap = importData.stream().filter(a -> a.get("sampleId").equals(exportedRow.get("sampleId")))
                    .findFirst().orElse(null);
            assertNotNull("expect all matching rows to come through", matchingMap);
            assertThat("expect export and import values to be equivalent",
                    exportedRow.get("resultData"), equalTo(matchingMap.get("resultData")));
        }
    }


    private StringBuilder checkDisplayFields(String displayField, List<String> columnLabels)
    {
        StringBuilder tmpString = new StringBuilder();

        if(!columnLabels.contains("Name"))
        {
            String errorMsg = "Did not find the 'Name' column.";
            log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

            tmpString.append(errorMsg);
            tmpString.append("\n");
        }

        return tmpString;
    }

    @LogMethod
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

}
