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
package org.labkey.test.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.experiment.CreateSampleTypePage;
import org.labkey.test.pages.experiment.UpdateSampleTypePage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.labkey.test.util.exp.SampleTypeAPIHelper.SAMPLE_TYPE_DATA_REGION_NAME;

/**
 * Helper methods for create Sample Types and import data into them through standard LabKey Server UI
 */
public class SampleTypeHelper extends WebDriverWrapper
{
    public static final String IMPORT_OPTION = "IMPORT";
    public static final String MERGE_OPTION = "MERGE";
    public static final String UPDATE_OPTION = "UPDATE";
    private final WebDriver _driver;

    public enum StatusType {
        Available,
        Consumed,
        Locked
    }

    public SampleTypeHelper(WebDriverWrapper driverWrapper)
    {
        this(driverWrapper.getDriver());
    }

    public SampleTypeHelper(WebDriver driver)
    {
        _driver = driver;
    }

    public static SampleTypeHelper beginAtSampleTypesList(WebDriverWrapper dWrapper, String containerPath)
    {
        dWrapper.beginAt(WebTestHelper.buildURL("experiment", containerPath, "listSampleTypes"));
        return new SampleTypeHelper(dWrapper.getDriver());
    }

    @NotNull
    private static String convertMapToTsv(@NotNull List<Map<String, String>> data)
    {
        // first the header
        List<String> rows = new ArrayList<>();
        rows.add(String.join("\t", data.get(0).keySet()));
        data.forEach(dataMap -> {
            StringBuilder row = new StringBuilder();
            data.get(0).keySet().forEach(key -> {
                row.append(dataMap.get(key));
                row.append("\t");
            });
            rows.add(row.substring(0, row.lastIndexOf("\t")));
        });
        return String.join("\n", rows);
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return _driver;
    }

    public void createSampleType(SampleTypeDefinition props)
    {
        CreateSampleTypePage createPage = goToCreateNewSampleType();

        createPage.setName(props.getName());
        if (props.getAutoLinkDataToStudy() != null)
        {
            createPage.setAutoLinkDataToStudy(props.getAutoLinkDataToStudy());
        }
        if (props.getLinkedDatasetCategory() != null)
        {
            createPage.setLinkedDatasetCategory(props.getLinkedDatasetCategory());
        }
        if (props.getNameExpression() != null)
        {
            createPage.setNameExpression(props.getNameExpression());
        }
        if (props.getDescription() != null)
        {
            createPage.setDescription(props.getDescription());
        }

        for (String importHeader : props.getParentAliases().keySet())
        {
            createPage.addParentAlias(importHeader, props.getParentAliases().get(importHeader));
        }

        createPage.addFields(props.getFields());
        createPage.clickSave();
    }

    public void createSampleType(SampleTypeDefinition definition, File dataFile)
    {
        createSampleType(definition);
        goToSampleType(definition.getName()).bulkImport(dataFile);
    }

    public void createSampleType(SampleTypeDefinition definition, String data)
    {
        createSampleType(definition);
        goToSampleType(definition.getName()).bulkImport(data);
    }

    public void createSampleType(SampleTypeDefinition definition, List<Map<String, String>> data)
    {
        createSampleType(definition);
        goToSampleType(definition.getName()).bulkImport(data);
    }

    public CreateSampleTypePage goToCreateNewSampleType()
    {
        getSampleTypesList().clickHeaderButtonAndWait("New Sample Type");
        return new CreateSampleTypePage(getDriver());
    }

    public SampleTypeHelper goToSampleType(String name)
    {
        TestLogger.log("Go to the sample type '" + name + "'");
        clickAndWait(Locator.linkWithText(name));
        return this;
    }

    public UpdateSampleTypePage goToEditSampleType(String name)
    {
        goToSampleType(name);
        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        return new UpdateSampleTypePage(getDriver());
    }

    public void verifyFields(List<FieldDefinition> _fields)
    {
        TestLogger.log("Verify that the fields for the sample type are as expected");
        Set<String> actualNames = new HashSet<>(getSampleTypeFields());
        Set<String> expectedNames = _fields.stream().map(FieldDefinition::getName).collect(Collectors.toSet());
        expectedNames.add("Name");
        expectedNames.add("MaterialExpDate");
        expectedNames.add("StoredAmount");
        expectedNames.add("Units");
        expectedNames.add("Flag");
        expectedNames.add("AliquotCount");
        expectedNames.add("AliquotVolume");
        assertEquals("Fields in sample type.", expectedNames, actualNames);
    }

    public DataRegionTable getSampleTypesList()
    {
        return new DataRegionTable.DataRegionFinder(getDriver()).withName(SAMPLE_TYPE_DATA_REGION_NAME).find();
    }

    public DataRegionTable getSamplesDataRegionTable()
    {
        return new DataRegionTable.DataRegionFinder(getDriver()).withName("Material").find();
    }

    public int getSampleCount()
    {
        return getSamplesDataRegionTable().getDataRowCount();
    }

    private List<String> getSampleTypeFields()
    {
        return getSamplesDataRegionTable().getColumnNames();
    }

    public void insertRow(Map<String, String> fieldValues)
    {
        getSamplesDataRegionTable()
                .clickInsertNewRow();
        for (Map.Entry<String, String> fieldValue : fieldValues.entrySet())
        {
            setFormElement(Locator.name("quf_" + fieldValue.getKey()), fieldValue.getValue());
        }
        clickButton("Submit");
    }

    public void bulkImport(File dataFile)
    {
        fileImport(dataFile, IMPORT_OPTION);
    }

    public void mergeImport(File dataFile)
    {
        fileImport(dataFile, MERGE_OPTION);
    }

    private void fileImport(File dataFile, String importOption)
    {
        DataRegionTable drt = getSamplesDataRegionTable();
        TestLogger.log("Adding data from file");
        ImportDataPage importDataPage = drt.clickImportBulkData();
        importDataPage.setFile(dataFile);
        if (MERGE_OPTION.equals(importOption))
            importDataPage.setFileMerge(true);
        else if (UPDATE_OPTION.equals(importOption))
            importDataPage.setFileInsertOption(true);
        importDataPage.submit();
    }

    public void bulkImport(String tsvData)
    {
        startTsvImport(tsvData, IMPORT_OPTION)
                .submit();
    }

    public void mergeImport(String tsvData)
    {
        startTsvImport(tsvData, SampleTypeHelper.MERGE_OPTION)
                .submit();
    }

    public void bulkImport(List<Map<String, String>> data)
    {
        startTsvImport(data, IMPORT_OPTION)
                .submit();
    }

    public void bulkImportExpectingError(List<Map<String, String>> data, String importOption)
    {
        startTsvImport(data, importOption)
                .submitExpectingError();
    }

    public void mergeImport(List<Map<String, String>> data)
    {
        startTsvImport(data, SampleTypeHelper.MERGE_OPTION)
                .submit();
    }

    public void mergeImportExpectingError(List<Map<String, String>> data)
    {
        startTsvImport(data, SampleTypeHelper.MERGE_OPTION)
                .submitExpectingError();
    }

    private ImportDataPage startTsvImport(String tsv, String importOption)
    {
        DataRegionTable drt = getSamplesDataRegionTable();
        ImportDataPage importDataPage = drt.clickImportBulkData();
        if (MERGE_OPTION.equals(importOption))
            importDataPage.setCopyPasteMerge(true);
        else if (UPDATE_OPTION.equals(importOption))
            importDataPage.setCopyPasteInsertOption(true);
        importDataPage.setText(tsv);
        return importDataPage;
    }

    private ImportDataPage startTsvImport(List<Map<String, String>> data, String importOption)
    {
        if (data == null || data.isEmpty())
        {
            throw new IllegalArgumentException("No data provided");
        }
        return startTsvImport(convertMapToTsv(data), importOption);
    }

    public void deleteSamples(DataRegionTable samplesTable, String expectedTitle)
    {
        doAndWaitForPageToLoad(() -> {
            samplesTable.clickHeaderButton("Delete");
            Window.Window(getDriver()).withTitle(expectedTitle).waitFor()
                    .clickButton("Yes, Delete", false);
            Window.Window(getDriver()).withTitleContaining("Delete sample").waitFor();
            _ext4Helper.waitForMaskToDisappear();
        });
    }

    private void verifyDataRow(Map<String, String> expectedData, int index, DataRegionTable drt)
    {
        Map<String, String> actualData = drt.getRowDataAsMap(index);

        for (Map.Entry<String, String> field : expectedData.entrySet())
        {
            assertEquals(field.getKey() + " not as expected at index " + index, field.getValue().trim(), actualData.get(field.getKey()));
        }
    }

    public void verifyDataValues(List<Map<String, String>> data)
    {
        DataRegionTable drt = getSamplesDataRegionTable();
        for (Map<String, String> expectedRow : data)
        {
            int index = drt.getRowIndex("Name", expectedRow.get("Name").trim());
            verifyDataRow(expectedRow, index, drt);
        }
    }

    public String getDetailsFieldValue(String label)
    {
        Locator loc = Locator.tag("td").withClass("lk-form-label").withText(label + ":").followingSibling("td");
        return loc.findElement(getDriver()).getText();
    }
    
    public void addSampleStates(String folderPath, Map<String, StatusType> states) throws IOException, CommandException
    {
        for (Map.Entry<String, StatusType> statePair : states.entrySet())
            insertSampleState(folderPath, statePair.getKey(), statePair.getValue().name());
    }

    // we use the string here for stateType instead of the enum to allow for setting values outside the enum (error conditions)
    private void insertSampleState(String folderPath, String label, @Nullable String stateType) throws IOException, CommandException
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
        InsertRowsCommand insertCmd = new InsertRowsCommand("core", "DataStates");
        Map<String,Object> rowMap = new HashMap<>();
        rowMap.put("label", label);
        rowMap.put("stateType", stateType);
        rowMap.put("publicData", false);
        insertCmd.addRow(rowMap);
        insertCmd.execute(cn, folderPath);
    }

    public DataRegionTable linkToStudy(String targetStudy, String sampleTypeName, List<String> sampleIds, @Nullable String categoryName)
    {
        clickAndWait(Locator.linkWithText(sampleTypeName));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        for (String sampleId : sampleIds)
        {
            int rowNum = samplesTable.getRowIndex("Name", sampleId);
            if (rowNum >= 0)
                samplesTable.checkCheckbox(rowNum);
            else
                fail(String.format("Could not find sample %s in table to link", sampleId));
        }
        return _linkToStudy(samplesTable, targetStudy, categoryName);
    }

    public DataRegionTable linkToStudy(String targetStudy, String sampleTypeName, int numOfRowsToBeLinked, @Nullable String categoryName)
    {
        clickAndWait(Locator.linkWithText(sampleTypeName));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        for (int i = 0; i < numOfRowsToBeLinked; i++)
            samplesTable.checkCheckbox(i);
        return _linkToStudy(samplesTable, targetStudy, categoryName);
    }

    private DataRegionTable _linkToStudy(DataRegionTable samplesTable, String targetStudy, String categoryName)
    {
        samplesTable.clickHeaderButtonAndWait("Link to Study");

        log("Link to study: Choose target");
        selectOptionByText(Locator.id("targetStudy"), "/" + targetStudy + " (" + targetStudy + " Study)");
        if (categoryName != null)
            setFormElement(Locator.name("autoLinkCategory"), categoryName);
        clickButton("Next");

        DataRegionTable table =  new DataRegionTable("query", getDriver());
        table.clickHeaderButtonAndWait("Link to Study");
        return table;
    }
}
