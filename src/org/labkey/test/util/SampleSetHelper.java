package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SampleSetHelper
{
    private static final String SAMPLE_SETS_WEB_PART_TITLE = "Sample Sets";
    private static final String SAMPLES_DATA_WEB_PART_TITLE = "Sample Set Contents";
    public static final String BULK_IMPORT_MENU_TEXT = "Import bulk data";
    private BaseWebDriverTest _test;
    private boolean _inWebPart = true;
    private Map<String, FieldDefinition.ColumnType> _fields;
    public static final String IMPORT_DATA_OPTION = "IMPORT";
    public static final String MERGE_DATA_OPTION = "MERGE";

    public SampleSetHelper(BaseWebDriverTest test)
    {
        this(test, true);
    }

    public SampleSetHelper(BaseWebDriverTest test, boolean inWebPart)
    {
        _test = test;
        _inWebPart = inWebPart;
    }

    public void setInWebPart(boolean inWebPart)
    {
        _inWebPart = inWebPart;
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
        Locator.XPathLocator createNewLoc = Locators.createSampleSet;
        if (_inWebPart)
        {
            Locator.XPathLocator webPartLoc = Locators.sampleSetsWebPart;
            WebElement element = webPartLoc.findElement(_test.getDriver());
            _test.scrollIntoView(element);
            createNewLoc = webPartLoc.descendant(createNewLoc);
        }
        _test.click(createNewLoc);
        return this;
    }


    public SampleSetHelper setNameExpression(String nameExpression)
    {
        _test.setFormElement(Locator.id("nameExpression"), nameExpression);
        return this;
    }

    public SampleSetHelper setNameAndExpression(String name, @Nullable String nameExpression)
    {
        _test.setFormElement(Locator.id("name"), name);
        if (nameExpression != null)
        {
            _test.setFormElement(Locator.id("nameExpression"), nameExpression);
        }

        _test.clickButton("Create");
        return this;
    }

    public SampleSetHelper selectImportOption(String value, int index)
    {
        List<WebElement> buttons = Locator.radioButtonByNameAndValue("insertOption", value).findElements(_test.getDriver());
        buttons.get(index).click();
        return this;
    }


    public SampleSetHelper goToSampleSet(String name)
    {
        _test.log("Go to the sample set '" + name + "'");
        _test.click(Locator.linkWithText(name));
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
            PropertiesEditor fieldProperties = new PropertiesEditor.PropertiesEditorFinder(_test.getDriver()).withTitle("Field Properties").waitFor();
            fields.forEach((name, type) -> {
                fieldProperties.addField(new FieldDefinition(name).setType(type));
            });
            _test.clickButton("Save");
        }
        else
            _test.clickButton("Cancel");
        return this;
    }

    public SampleSetHelper addFields(List<FieldDefinition> fields)
    {
        if(null != fields && !fields.isEmpty())
        {
            PropertiesEditor fieldProperties = new PropertiesEditor.PropertiesEditorFinder(_test.getDriver()).withTitle("Field Properties").waitFor();
            fields.forEach(fieldDefinition -> {
                fieldProperties.addField(fieldDefinition);
            });
            _test.clickButton("Save");
        }
        else
            _test.clickButton("Cancel");

        return this;
    }

    public SampleSetHelper verifyFields()
    {
        _test.log("Verify that the fields for the sample set are as expected");
        List<String> actualNames = getSampleSetFields();
        for (String name : _fields.keySet())
            assertTrue("'" + name + "' should be one of the fields", actualNames.contains(name));
        return this;
    }

    public DataRegionTable getSamplesDataRegionTable()
    {
        return DataRegionTable.findDataRegionWithinWebpart(_test, SAMPLES_DATA_WEB_PART_TITLE);
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
            _test.setFormElement(Locator.name("quf_"+ fieldValue.getKey()), fieldValue.getValue());
        }
        _test.clickButton("Submit");
    }

    public void bulkImport(File dataFile)
    {
        bulkImport(dataFile, IMPORT_DATA_OPTION);
    }

    public void bulkImport(File dataFile, String importOption)
    {
        if (dataFile != null)
        {
            DataRegionTable drt = getSamplesDataRegionTable();
            _test.log("Adding data from file");
            drt.clickHeaderMenu("Insert data", BULK_IMPORT_MENU_TEXT);
            _test.click(Locators.fileUpload);
            selectImportOption(importOption, 0);
            _test.setFormElement(Locator.tagWithName("input", "file"), dataFile);
            _test.clickButton("Submit");
        }
    }

    public void setTsvData(String tsvData)
    {
        _test.setFormElement(Locator.name("text"), tsvData);
    }

    public void bulkImport(String tsvData)
    {
        bulkImport(tsvData, IMPORT_DATA_OPTION);
    }

    public void bulkImport(String tsvData, String importOption)
    {
        if (tsvData.length() > 0)
        {

            DataRegionTable drt = getSamplesDataRegionTable();
            _test.log("Adding tsv data via bulk import");
            drt.clickHeaderMenu("Insert data", BULK_IMPORT_MENU_TEXT);
            selectImportOption(importOption, 1);
            setTsvData(tsvData);
            _test.clickButton("Submit");
        }
    }

    public void bulkImport(List<Map<String, String>> data)
    {
        bulkImport(data, IMPORT_DATA_OPTION);
    }

    public void bulkImport(List<Map<String, String>> data, String importOption)
    {
        if (data.size() > 0)
        {
            _test.log ("Adding " + data.size() + " rows via bulk import");
            DataRegionTable drt = getSamplesDataRegionTable();
            drt.clickHeaderMenu("Insert data", BULK_IMPORT_MENU_TEXT);
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
            _test.clickButton("Submit");
        }
    }

    public SampleSetHelper verifyDataRow(Map<String, String> data, int index)
    {
        return verifyDataRow(data, index, DataRegionTable.findDataRegionWithinWebpart(_test, "Sample Set Contents"));
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
        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(_test, "Sample Set Contents");
        Map<String, String> rowData = drt.getRowDataAsMap(0);
        for (Map<String, String> expectedRow : data)
        {
            int index = drt.getRowIndex(keyField, expectedRow.get(keyField));
            verifyDataRow(expectedRow, index, drt);
        }
        return this;
    }

    public static class Locators
    {
        public static final Locator.XPathLocator createSampleSet = Locator.tagWithClassContaining("i", "fa-plus");
        public static final Locator.XPathLocator sampleSetsWebPart =  PortalHelper.Locators.webPart(SAMPLE_SETS_WEB_PART_TITLE);
        public static final Locator.XPathLocator createSampleSetFromWebPart = sampleSetsWebPart.descendant(createSampleSet);
        public static final Locator.XPathLocator fileUpload = Locator.tagWithText("h3", "Upload file (.xlsx, .xls, .csv, .txt)");
    }

}
