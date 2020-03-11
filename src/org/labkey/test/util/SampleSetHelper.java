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
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.pages.experiment.CreateSampleSetPage;
import org.labkey.test.pages.experiment.UpdateSampleSetPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleSetDefinition;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class SampleSetHelper extends WebDriverWrapper
{
    private final WebDriver _driver;
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

    public void createSampleSet(SampleSetDefinition props)
    {
        CreateSampleSetPage createPage = goToCreateNewSampleSet();

        createPage.setName(props.getName());
        if (props.getNameExpression() != null)
        {
            createPage.setNameExpression(props.getNameExpression());
        }
        if (props.getDescription() != null)
        {
            createPage.setDescription(props.getDescription());
        }

        for (String importHeader : props.getImportAliases().keySet())
        {
            createPage.addParentColumnAlias(importHeader, props.getImportAliases().get(importHeader));
        }

        createPage.addFields(props.getFields());
        createPage.clickSave();
    }

    public SampleSetHelper createSampleSet(String name)
    {
        return createSampleSet(name, null);
    }

    public SampleSetHelper createSampleSet(String name, @Nullable String nameExpression)
    {
        return createSampleSet(name, nameExpression, null);
    }

    public SampleSetHelper createSampleSet(String name, @Nullable String nameExpression, Map<String, FieldDefinition.ColumnType> fields)
    {
        SampleSetDefinition props = new SampleSetDefinition();
        props.setName(name);
        props.setNameExpression(nameExpression);
        if (fields != null)
        {
            for (String fieldName : fields.keySet())
            {
                props.addField(new FieldDefinition(fieldName).setType(fields.get(fieldName)));
            }
        }

        createSampleSet(props);
        return this;
    }

    public void createSampleSet(String name, @Nullable String nameExpression, Map<String, FieldDefinition.ColumnType> fields, File dataFile)
    {
        createSampleSet(name, nameExpression, fields);
        goToSampleSet(name).bulkImport(dataFile);
    }

    public void createSampleSet(String name, @Nullable String nameExpression, Map<String, FieldDefinition.ColumnType> fields, String data)
    {
        createSampleSet(name, nameExpression, fields);
        goToSampleSet(name).bulkImport(data);
    }

    public void createSampleSet(String name, @Nullable String nameExpression, Map<String, FieldDefinition.ColumnType> fields, List<Map<String, String>> data)
    {
        createSampleSet(name, nameExpression, fields);
        goToSampleSet(name).bulkImport(data);
    }

    public CreateSampleSetPage goToCreateNewSampleSet()
    {
        getSampleSetsList().clickHeaderButtonAndWait("New Sample Set");
        return new CreateSampleSetPage(getDriver());
    }


    public SampleSetHelper setNameExpression(String nameExpression)
    {
        new CreateSampleSetPage(getDriver()).setNameExpression(nameExpression);
        return this;
    }

    public SampleSetHelper selectImportOption(String label, int index)
    {
        waitForText("Import Lookups by Alternate Key");
        boolean merge = MERGE_DATA_LABEL.equals(label);
        String componentId = "insertOption" + index;
        String script = "Ext4.ComponentManager.get('" + componentId + "').setValue(" + (merge?"1":"0") + ")";
        executeScript(script);
        return this;
    }

    public SampleSetHelper addParentColumnAlias(Map<String, String> aliases)
    {
        CreateSampleSetPage createSampleSetPage = new CreateSampleSetPage(getDriver());

        int i = 0;
        for(String importHeader : aliases.keySet())
        {
            createSampleSetPage.addParentColumnAlias(importHeader, aliases.get(importHeader));
            i++;
        }

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

    public UpdateSampleSetPage goToEditSampleSet(String name)
    {
        goToSampleSet(name);
        waitAndClickAndWait(Locator.lkButton("Edit Set"));
        return new UpdateSampleSetPage(getDriver());
    }

    public static List<FieldDefinition> convertFieldMap(Map<String, FieldDefinition.ColumnType> fields)
    {
        return fields.entrySet().stream()
                .map(entry -> new FieldDefinition(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @LogMethod
    public SampleSetHelper addFields(Map<String, FieldDefinition.ColumnType> fields)
    {
        DomainDesignerPage domainDesignerPage = new DomainDesignerPage(getDriver());

        if (fields != null && !fields.isEmpty())
        {
            DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
            fields.forEach((name, type) -> domainFormPanel.addField(new FieldDefinition(name, type)));
            domainDesignerPage.clickFinish();
        }
        else
            clickButton("Cancel");

        return this;
    }

    public SampleSetHelper addFields(List<FieldDefinition> fields)
    {
        DomainDesignerPage domainDesignerPage = new DomainDesignerPage(getDriver());

        if (null != fields && !fields.isEmpty())
        {
            DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
            fields.forEach(domainFormPanel::addField);
            domainDesignerPage.clickFinish();
        }
        else
            clickButton("Cancel");

        return this;
    }

    public void verifyFields(List<FieldDefinition> _fields)
    {
        TestLogger.log("Verify that the fields for the sample set are as expected");
        Set<String> actualNames = new HashSet<>(getSampleSetFields());
        Set<String> expectedNames = _fields.stream().map(FieldDefinition::getName).collect(Collectors.toSet());
        expectedNames.add("Name");
        expectedNames.add("Flag");
        assertEquals("Fields in sample set.", expectedNames, actualNames);
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

    public SampleSetHelper deleteSamples(DataRegionTable samplesTable, String expectedTitle)
    {
        samplesTable.doAndWaitForUpdate(() -> {
            samplesTable.clickHeaderButton("Delete");
            Window.Window(getDriver()).withTitle(expectedTitle).waitFor()
                    .clickButton("Yes, Delete", false);
            Window.Window(getDriver()).withTitleContaining("Delete sample").waitFor();
            _ext4Helper.waitForMaskToDisappear();
        });
        return this;
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
