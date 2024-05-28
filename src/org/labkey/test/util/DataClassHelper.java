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

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.experiment.CreateDataClassPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.util.exp.DataClassAPIHelper;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.labkey.test.util.exp.DataClassAPIHelper.DATA_CLASS_DATA_REGION_NAME;

/**
 * Helper methods for creating Data Classes and importing data into them through standard LabKey Server UI
 */
public class DataClassHelper extends WebDriverWrapper
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

    public DataClassHelper(WebDriver driver)
    {
        _driver = driver;
    }

    public static DataClassHelper beginAtDataClassesList(WebDriverWrapper dWrapper, String containerPath)
    {
        dWrapper.beginAt(WebTestHelper.buildURL("experiment", containerPath, "listDataClass"));
        return new DataClassHelper(dWrapper.getDriver());
    }
    

    @Override
    public WebDriver getWrappedDriver()
    {
        return _driver;
    }

    public void createDataClass(DataClassDefinition props)
    {
        CreateDataClassPage createPage = goToCreateNewDataClass();

        createPage.setName(props.getName());

        if (props.getNameExpression() != null)
        {
            createPage.setNameExpression(props.getNameExpression());
        }
        if (props.getDescription() != null)
        {
            createPage.setDescription(props.getDescription());
        }

        createPage.addFields(props.getFields());
        createPage.clickSave();
    }

    public void createDataClass(DataClassDefinition definition, List<Map<String, String>> data)
    {
        createDataClass(definition);
        goToDataClass(definition.getName()).bulkImport(data);
    }

    public CreateDataClassPage goToCreateNewDataClass()
    {
        getDataClassesList().clickHeaderButtonAndWait("New Data Class");
        return new CreateDataClassPage(getDriver());
    }

    public DataClassHelper goToDataClass(String name)
    {
        TestLogger.log("Go to the data class '" + name + "'");
        clickAndWait(Locator.linkWithText(name));
        return this;
    }

    public void verifyFields(List<FieldDefinition> _fields)
    {
        TestLogger.log("Verify that the fields for the data class are as expected");
        Set<String> actualNames = new HashSet<>(getDataClassFields());
        Set<String> expectedNames = _fields.stream().map(FieldDefinition::getName).collect(Collectors.toSet());
        expectedNames.add("Name");
        expectedNames.add("Description");
        assertEquals("Fields in data class not as expected.", expectedNames, actualNames);
    }

    public DataRegionTable getDataClassesList()
    {
        return new DataRegionTable.DataRegionFinder(getDriver()).withName(DATA_CLASS_DATA_REGION_NAME).find();
    }

    public DataRegionTable getDataClassDataRegionTable()
    {
        return new DataRegionTable.DataRegionFinder(getDriver()).withName("query").find();
    }

    public int getDataCount()
    {
        return getDataClassDataRegionTable().getDataRowCount();
    }

    private List<String> getDataClassFields()
    {
        return getDataClassDataRegionTable().getColumnNames();
    }

    public void insertRow(Map<String, String> fieldValues)
    {
        getDataClassDataRegionTable()
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
        DataRegionTable drt = getDataClassDataRegionTable();
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

    public void bulkImport(List<Map<String, String>> data)
    {
        startTsvImport(data, IMPORT_OPTION)
                .submit();
    }

    public void mergeImport(List<Map<String, String>> data)
    {
        startTsvImport(data, DataClassHelper.MERGE_OPTION)
                .submit();
    }

    private ImportDataPage startTsvImport(String tsv, String importOption)
    {
        DataRegionTable drt = getDataClassDataRegionTable();
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
        return startTsvImport(DataClassAPIHelper.convertMapToTsv(data), importOption);
    }
}
