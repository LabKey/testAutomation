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

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.components.ext4.RadioButton.RadioButton;

public class SampleSetHelper extends WebDriverWrapper
{
    private final WebDriver _driver;
    private Map<String, FieldDefinition.ColumnType> _fields;
    public static final String IMPORT_DATA_LABEL = "Insert";
    public static final String MERGE_DATA_LABEL = "Insert and Replace";

    public SampleSetHelper(WebDriverWrapper driverWrapper)
    {
        this(driverWrapper.getDriver());
    }

    public SampleSetHelper(WebDriver driver)
    {
        _driver = driver;
    }

    public static SampleSetHelper beginAtSampleSetsList(WebDriverWrapper dWrapper, String containerPath)
    {
        dWrapper.beginAt(WebTestHelper.buildURL("experiment", containerPath, "listMaterialSources"));
        return new SampleSetHelper(dWrapper.getDriver());
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return _driver;
    }

    public SampleSetHelper createSampleSet(String name)
    {
        return createSampleSet(name, null);
    }

    public SampleSetHelper createSampleSet(String name, @Nullable String nameExpression)
    {
        goToCreateNewSampleSet();
        setNameAndExpression(name, nameExpression);
        return this;
    }

    public void createSampleSet(String name, @Nullable String nameExpression, Map<String, FieldDefinition.ColumnType> fields)
    {
        this.goToCreateNewSampleSet()
                .setNameAndExpression(name, nameExpression)
                .addFields(fields);
    }

    public void createSampleSet(String name, @Nullable String nameExpression, Map<String, FieldDefinition.ColumnType> fields, File dataFile)
    {
        this.goToCreateNewSampleSet()
                .setNameAndExpression(name, nameExpression)
                .addFields(fields)
                .goToSampleSet(name)
                .bulkImport(dataFile);
    }

    public void createSampleSet(String name, @Nullable String nameExpression, Map<String, FieldDefinition.ColumnType> fields, String data)
    {
        this.goToCreateNewSampleSet()
                .setNameAndExpression(name, nameExpression)
                .addFields(fields)
                .goToSampleSet(name)
                .bulkImport(data);
    }

    public void createSampleSet(String name, @Nullable String nameExpression, Map<String, FieldDefinition.ColumnType> fields, List<Map<String, String>> data)
    {
        this.goToCreateNewSampleSet()
                .setNameAndExpression(name, nameExpression)
                .addFields(fields)
                .goToSampleSet(name)
                .bulkImport(data);
    }

    public SampleSetHelper goToCreateNewSampleSet()
    {
        getSampleSetsList().clickHeaderButtonAndWait("Create New Sample Set");
        return this;
    }


    public SampleSetHelper setNameExpression(String nameExpression)
    {
        setFormElement(Locator.id("nameExpression"), nameExpression);
        return this;
    }

    public SampleSetHelper setNameAndExpression(String name, @Nullable String nameExpression)
    {
        setFormElement(Locator.id("name"), name);
        if (nameExpression != null)
        {
            setFormElement(Locator.id("nameExpression"), nameExpression);
        }

        clickButton("Create");
        return this;
    }

    public SampleSetHelper selectImportOption(String label, int index)
    {
        RadioButton().withLabel(label).index(index).find(getDriver()).check();
        return this;
    }

    public SampleSetHelper addParentColumnAlias(Map<String, String> aliases)
    {
        for(String importHeader : aliases.keySet())
        {
            addParentColumnAlias(importHeader, aliases.get(importHeader));
        }

        return this;
    }

    public SampleSetHelper addParentColumnAlias(String parentAlias, String inputName)
    {
        List<WebElement> importAliasInputs = Locator.tagWithName("input", "importAliasKeys").findElements(getDriver());
        List<WebElement> importAliasSelects = Locator.tagWithName("select", "importAliasValues").findElements(getDriver());

        int countOfInputs = importAliasInputs.size();

        click(Locator.linkWithText("add parent column import alias"));
        waitFor(()-> Locator.tagWithName("input", "importAliasKeys").findElements(getDriver()).size() > countOfInputs, 1000);
        waitFor(()-> Locator.tagWithName("input", "importAliasKeys").findElements(getDriver()).size() == Locator.tagWithName("select", "importAliasValues").findElements(getDriver()).size(), 1000);

        importAliasInputs = Locator.tagWithName("input", "importAliasKeys").findElements(getDriver());
        importAliasSelects = Locator.tagWithName("select", "importAliasValues").findElements(getDriver());

        int index = importAliasInputs.size() - 1;

        WebElement aliasInput = importAliasInputs.get(index);

        setFormElement(aliasInput, parentAlias);

        WebElement aliasSelect = importAliasSelects.get(index);
        selectOptionByTextContaining(aliasSelect, inputName);

        return this;
    }

    public SampleSetHelper removeParentColumnAlias(String parentAlias)
    {

        List<WebElement> importAliasInputs = Locator.tagWithName("input", "importAliasKeys").findElements(getDriver());

        int countOfInputs = importAliasInputs.size();

        waitFor(()-> Locator.tagWithName("input", "importAliasKeys").findElements(getDriver()).size() > countOfInputs, 1000);

        importAliasInputs = Locator.tagWithName("input", "importAliasKeys").findElements(getDriver());

        int index = 0;
        for(WebElement input : importAliasInputs)
        {
            if(getFormElement(input).trim().equalsIgnoreCase(parentAlias.trim()))
            {
                break;
            }
            index++;
        }

        if(index == importAliasInputs.size())
            throw new NoSuchElementException("No 'Parent Alias' with the value of '" + parentAlias + "' was found.");

        importAliasInputs = Locator.tagWithClass("a", "removeAliasTrigger").findElements(getDriver());

        importAliasInputs.get(index).click();

        clickButton("Update");

        return this;
    }

    public SampleSetHelper goToSampleSet(String name)
    {
        TestLogger.log("Go to the sample set '" + name + "'");
        click(Locator.linkWithText(name));
        return this;
    }

    public SampleSetHelper goToEditSampleSet(String name)
    {
        goToSampleSet(name);
        waitAndClickAndWait(Locator.lkButton("Edit Set"));
        return this;
    }

    public void setFields(Map<String, FieldDefinition.ColumnType> fields)
    {
        _fields = fields;
    }

    @LogMethod
    public SampleSetHelper addFields(Map<String, FieldDefinition.ColumnType> fields)
    {
        _fields = fields;
        if (fields != null && !fields.isEmpty())
        {
            PropertiesEditor fieldProperties = new PropertiesEditor.PropertiesEditorFinder(getDriver()).withTitle("Field Properties").waitFor();
            fields.forEach((name, type) -> {
                fieldProperties.addField(new FieldDefinition(name).setType(type));
            });
            clickButton("Save");
        }
        else
            clickButton("Cancel");
        return this;
    }

    public SampleSetHelper addFields(List<FieldDefinition> fields)
    {
        if(null != fields && !fields.isEmpty())
        {
            PropertiesEditor fieldProperties = new PropertiesEditor.PropertiesEditorFinder(getDriver()).withTitle("Field Properties").waitFor();
            fields.forEach(fieldDefinition -> {
                fieldProperties.addField(fieldDefinition);
            });
            clickButton("Save");
        }
        else
            clickButton("Cancel");

        return this;
    }

    public SampleSetHelper verifyFields()
    {
        TestLogger.log("Verify that the fields for the sample set are as expected");
        List<String> actualNames = getSampleSetFields();
        for (String name : _fields.keySet())
            assertTrue("'" + name + "' should be one of the fields", actualNames.contains(name));
        return this;
    }

    public DataRegionTable getSampleSetsList()
    {
        return new DataRegionTable.DataRegionFinder(getDriver()).withName("SampleSet").find();
    }

    public DataRegionTable getSamplesDataRegionTable()
    {
        return new DataRegionTable.DataRegionFinder(getDriver()).withName("Material").find();
    }

    public int getSampleCount()
    {
        return getSamplesDataRegionTable().getDataRowCount();
    }

    private List<String> getSampleSetFields()
    {
        return getSamplesDataRegionTable().getColumnNames();
    }

    public void insertRow(Map<String, String> fieldValues)
    {
        getSamplesDataRegionTable()
                .clickInsertNewRow();
        for (Map.Entry<String, String> fieldValue : fieldValues.entrySet())
        {
            setFormElement(Locator.name("quf_"+ fieldValue.getKey()), fieldValue.getValue());
        }
        clickButton("Submit");
    }

    public void bulkImport(File dataFile)
    {
        bulkImport(dataFile, IMPORT_DATA_LABEL);
    }

    public void bulkImport(File dataFile, String importOption)
    {
        if (dataFile != null)
        {
            DataRegionTable drt = getSamplesDataRegionTable();
            TestLogger.log("Adding data from file");
            drt.clickImportBulkData();
            click(Locators.fileUpload);
            selectImportOption(importOption, 0);
            setFormElement(Locator.tagWithName("input", "file"), dataFile);
            clickButton("Submit");
        }
    }

    public void setTsvData(String tsvData)
    {
        setFormElement(Locator.name("text"), tsvData);
    }

    public void bulkImport(String tsvData)
    {
        bulkImport(tsvData, IMPORT_DATA_LABEL);
    }

    public void bulkImport(String tsvData, String importOption)
    {
        if (tsvData.length() > 0)
        {
            DataRegionTable drt = getSamplesDataRegionTable();
            TestLogger.log("Adding tsv data via bulk import");
            drt.clickImportBulkData();
            selectImportOption(importOption, 1);
            setTsvData(tsvData);
            clickButton("Submit");
        }
    }

    public void bulkImport(List<Map<String, String>> data)
    {
        bulkImport(data, IMPORT_DATA_LABEL, null);
    }

    public void bulkImport(List<Map<String, String>> data, int waitTime)
    {
        bulkImport(data, IMPORT_DATA_LABEL, waitTime);
    }

    public void bulkImport(List<Map<String, String>> data, String importOption)
    {
        bulkImport(data, importOption, null);
    }

    public void bulkImport(List<Map<String, String>> data, String importOption, @Nullable Integer waitTime)
    {
        if (data.size() > 0)
        {
            TestLogger.log ("Adding " + data.size() + " rows via bulk import");
            DataRegionTable drt = getSamplesDataRegionTable();
            drt.clickImportBulkData();
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

            selectImportOption(importOption, 1);
            setTsvData(String.join("\n", rows));

            // If an error is expected after clicking submit the page won't navigate. A 0 waitTime will avoid the "Page didn't navigate" error.
            if(null != waitTime)
                clickButton("Submit", waitTime);
            else
                clickButton("Submit");

        }
    }

    public SampleSetHelper verifyDataRow(Map<String, String> data, int index)
    {
        return verifyDataRow(data, index, DataRegionTable.findDataRegionWithinWebpart(this, "Sample Set Contents"));
    }

    private SampleSetHelper verifyDataRow(Map<String, String> expectedData, int index, DataRegionTable drt)
    {
        Map<String, String> actualData = drt.getRowDataAsMap(index);

        for (Map.Entry<String, String> field : expectedData.entrySet())
        {
            assertEquals(field.getKey() + " not as expected at index " + index, field.getValue(), actualData.get(field.getKey()));
        }
        return this;
    }

    public SampleSetHelper verifyDataValues(List<Map<String, String>> data)
    {
        return verifyDataValues(data, "Name");
    }

    public SampleSetHelper verifyDataValues(List<Map<String, String>> data, String keyField)
    {
        DataRegionTable drt = getSamplesDataRegionTable();
        for (Map<String, String> expectedRow : data)
        {
            int index = drt.getRowIndex(keyField, expectedRow.get(keyField));
            verifyDataRow(expectedRow, index, drt);
        }
        return this;
    }

    public static class Locators
    {
        public static final Locator.XPathLocator fileUpload = Locator.tagWithText("h3", "Upload file (.xlsx, .xls, .csv, .txt)");
    }

}
