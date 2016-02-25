/*
 * Copyright (c) 2012-2016 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class ListHelper
{
    public static final String EDITOR_CHANGE_SIGNAL = "propertiesEditorChange";
    BaseWebDriverTest _test;

    public ListHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void uploadCSVData(String listData)
    {
        clickImportData();
        _test.setFormElement(Locator.id("tsv3"), listData);
        _test._extHelper.selectComboBoxItem("Format:", "Comma-separated text (csv)");
        submitImportTsv_success();
    }

    public void uploadData(String listData)
    {
        clickImportData();
        _test.setFormElement(Locator.id("tsv3"), listData);
        submitImportTsv_success();
    }

    public void submitTsvData(String listData)
    {
        _test.setFormElement(Locator.id("tsv3"), listData);
        submitImportTsv_success();
    }


    public void submitImportTsv_success()
    {
        _test.clickButton("Submit");
        _test.waitForElement(Locator.css(".labkey-data-region"));
    }

    // null means any error
    public void submitImportTsv_error(String error)
    {
        _test.clickButton("Submit", 0);
        if (error == null) error = "";
        _test.waitForElement(Locator.css(".labkey-error").containing(error));
    }

    @LogMethod
    public void importDataFromFile(@LoggedParam File inputFile)
    {
        importDataFromFile(inputFile, BaseWebDriverTest.WAIT_FOR_PAGE * 5);
    }

    @LogMethod
    public void importDataFromFile(@LoggedParam File inputFile, int wait)
    {
        clickImportData();
        _test.click(Locator.tagWithClass("span", "labkey-wp-title-text").containing("Upload file"));
        _test.setFormElement(Locator.name("file"), inputFile);
        _test.clickButton("Submit", wait);
    }

    /**
     * From the list data grid, insert a new entry into the current list
     *
     * @param data key = the the name of the field, value = the value to enter in that field
     */

    public void insertNewRow(Map<String, String> data)
    {
        insertNewRow(data, true);
    }

    public void insertNewRow(Map<String, String> data, boolean validateText)
    {
        _test.clickButton("Insert New");
        setRowData(data, validateText);
    }

    private void setRowData(Map<String, String> data, boolean validateText)
    {
        for(String key : data.keySet())
        {
            _test.setFormElement(Locator.name("quf_" + key), data.get(key));
        }
        _test.clickButton("Submit");

        if(validateText)
        {
            _test.assertTextPresent(data.get(data.keySet().iterator().next()));  //make sure some text from the map is present
        }

    }

    /**
     * From the list data grid, edit an existing row
     *
     * @param id the row number (1 based)
     */
    public void updateRow(int id, Map<String, String> data)
    {
        updateRow(id, data, true);
    }

    public void updateRow(int id, Map<String, String> data, boolean validateText)
    {
        DataRegionTable dr = new DataRegionTable("query", _test);
        _test.clickAndWait(dr.updateLink(id - 1));
        setRowData(data, validateText);
    }

    /**
     * Starting at the grid view of a list, delete it
     */
    public void deleteList()
    {
        String url = _test.getCurrentRelativeURL().replace("grid.view", "deleteListDefinition.view");
        _test.beginAt(url);
        _test.clickButton("OK");
    }

    @LogMethod
    public void createListFromTab(String tabName, String listName, ListColumnType listKeyType, String listKeyName, ListColumn... cols)
    {
        beginCreateListFromTab(tabName, listName);
        createListHelper(listName, listKeyType, listKeyName, cols);
    }

    @LogMethod
    public void createList(String folderName, @LoggedParam String listName, ListColumnType listKeyType, String listKeyName, ListColumn... cols)
    {
        beginCreateList(folderName, listName);
        createListHelper(listName, listKeyType, listKeyName, cols);
    }

    private void createListHelper(String listName, ListColumnType listKeyType, String listKeyName, ListColumn... cols)
    {
        _test.selectOptionByText(Locator.id("ff_keyType"), listKeyType.toString());
        _test.setFormElement(Locator.id("ff_keyName"), listKeyName);
        _test.fireEvent(Locator.id("ff_keyName"), BaseWebDriverTest.SeleniumEvent.blur);

        _test.clickButton("Create List", 0);
        _test.waitForElement(Locator.name("ff_description"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElement(Locator.name("ff_name0"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        _test.log("Check that list was created correctly");
        _test.waitForFormElementToEqual(Locator.name("ff_name"), listName);
        _test.waitForFormElementToEqual(Locator.name("ff_name0"), listKeyName);

        _test.log("Add columns");

        // i==0 is the key column
        for (int i = 1; i <= cols.length; i++)
        {
            ListColumn col = cols[i-1];

            addField(col);
        }

        clickSave();

        for (ListColumn col : cols)
        {
            _test.assertTextPresent(col.getName());
            if (!StringUtils.isEmpty(col.getLabel()) && !col.getName().equals(col.getLabel()))
                _test.assertTextPresent(col.getLabel());
        }
    }

    public void addField(ListColumn col)
    {
        _test.waitForElement(Locator.id("button_Add Field"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        int lastFieldIndex = _test.getElementCount(Locator.xpath("//input[starts-with(@name, 'ff_label')]")) - 1;
        if (lastFieldIndex > 0)
        {
            Locator lastField = Locator.xpath("//input[@name='ff_label" + lastFieldIndex + "']");
            _test.click(lastField);
        }
        _test.clickButton("Add Field", 0);
        lastFieldIndex++;
        _test.setFormElement(Locator.name("ff_name" + lastFieldIndex),  col.getName());
        _test.setFormElement(Locator.name("ff_label" + lastFieldIndex), col.getLabel());

        setColumnType(null, col.getLookup(), col.getType(), lastFieldIndex);

        _test._extHelper.clickExtTab("Display");
        if (col.getDescription() != null)
        {
            _test.setFormElement(Locator.id("propertyDescription"), col.getDescription());
        }

        if (col.getFormat() != null)
        {
            _test._extHelper.clickExtTab("Format");
            _test.setFormElement(Locator.id("propertyFormat"), col.getFormat());
        }

        if (null != col.getURL())
        {
            _test.setFormElement(Locator.id("url"), col.getURL());
        }

        if (col.isRequired())
        {
            _test._extHelper.clickExtTab("Validators");
            clickRequired("");
        }

        FieldValidator validator = col.getValidator();
        if (validator != null)
        {
            _test._extHelper.clickExtTab("Validators");
            if (validator instanceof RegExValidator)
                _test.clickButton("Add RegEx Validator", 0);
            else
                _test.clickButton("Add Range Validator", 0);
            _test.setFormElement(Locator.name("name"), validator.getName());
            _test.setFormElement(Locator.name("description"), validator.getDescription());
            _test.setFormElement(Locator.name("errorMessage"), validator.getMessage());

            if (validator instanceof RegExValidator)
            {
                _test.setFormElement(Locator.name("expression"), ((RegExValidator)validator).getExpression());
            }
            else if (validator instanceof RangeValidator)
            {
                _test.setFormElement(Locator.name("firstRangeValue"), ((RangeValidator)validator).getFirstRange());
            }
            _test.clickButton("OK", 0);
        }

        if (col.isMvEnabled())
        {
            _test._extHelper.clickExtTab("Advanced");
            clickMvEnabled("");
        }

        if (col.getScale() != null)
        {
            _test._extHelper.clickExtTab("Advanced");
            setColumnScale(col.getScale());
        }
    }

    public void beginCreateListFromTab(String tabName, String listName)
    {
        _test.clickTab(tabName.replace(" ", ""));
        beginCreateListHelper(listName);
    }

    // initial "create list" steps common to both manual and import from file scenarios
    public void beginCreateList(String folderName, String listName)
    {
        try
        {
            _test.clickFolder(folderName);
        }
        catch (WebDriverException ex)
        {
            _test.clickProject(folderName);
        }

        beginCreateListHelper(listName);
    }

    private void beginCreateListHelper(String listName)
    {
        if (!_test.isElementPresent(Locator.linkWithText("Lists")))
        {
            PortalHelper portalHelper = new PortalHelper(_test);
            portalHelper.addWebPart("Lists");
        }

        _test.clickAndWait(Locator.linkWithText("manage lists"));

        _test.log("Add List");
        _test.clickButton("Create New List");
        _test.waitForElement(Locator.id("ff_name"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.setFormElement(Locator.id("ff_name"), listName);
        _test.fireEvent(Locator.id("ff_name"), BaseWebDriverTest.SeleniumEvent.blur);
    }


    public void createListFromFile(String folderName, String listName, File inputFile)
    {
        beginCreateList(folderName, listName);

        _test.click(Locator.xpath("//span[@id='fileImport']/input[@type='checkbox']"));
        //test.clickCheckbox("fileImport");

        _test.clickButton("Create List", 0);

        _test.waitForElement(Locator.xpath("//input[@name='uploadFormElement']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        _test.setFormElement(Locator.name("uploadFormElement"), inputFile);

        _test.waitForElement(Locator.xpath("//span[@id='button_Import']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        _test.clickButton("Import");
    }

    /**
     * Import a list archive to a target folder
     * @param folderName target folder
     * @param inputFile Full path/filename to list archive
     */
    public void importListArchive(String folderName, String inputFile)
    {
        importListArchive(folderName, new File(inputFile));
    }

    public void importListArchive(String folderName, File inputFile)
    {
        _test.clickFolder(folderName);
        importListArchive(inputFile);
    }

    public void importListArchive(File inputFile)
    {
        assertTrue("Unable to locate input file: " + inputFile, inputFile.exists());

        if (!_test.isElementPresent(Locator.linkWithText("Lists")))
        {
            PortalHelper portalHelper = new PortalHelper(_test);
            portalHelper.addWebPart("Lists");
        }

        _test.clickAndWait(Locator.linkWithText("manage lists"));

        _test.log("Import List Archive");
        _test.clickButton("Import List Archive");
        _test.waitForElement(Locator.xpath("//input[@name='listZip']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        _test.setFormElement(Locator.name("listZip"), inputFile);
        _test.clickButton("Import List Archive");
        _test.assertElementNotPresent(Locator.tagWithClass("div", "labkey-error"));
    }



    public void clickImportData()
    {
        _test.waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Import Data"), BaseWebDriverTest.WAIT_FOR_PAGE);
        _test.waitForElement(Locator.id("tsv3"));
    }

    public void clickEditDesign()
    {
        _test.waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Edit Design"), 0);
        _test.waitForElement(Locator.lkButton("Cancel"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElement(Locator.id("ff_description"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElement(Locator.lkButton("Add Field"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void clickEditFields()
    {
        _test.waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Edit Fields"), 0);
        _test.waitForElement(Locator.lkButton("Cancel"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElement(Locator.lkButton("Add Field"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void clickSave()
    {
        _test.waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Save"), 0);
        _test.waitForElement(Locator.lkButton("Edit Design"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElement(Locator.lkButton("Done"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        // TODO: Remove workaround. Project menu is opening after save on TeamCity for some reason
        _test.mouseOver(Locator.css("body"));
        _test.waitForElementToDisappear(Locator.id("projectBar_menu").notHidden());
    }

    public void clickDeleteList()
    {
        _test.waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Delete List"), BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void clickRow(int index)
    {
        clickRow(null, index);
    }

    public void clickRow(@Nullable String prefix, int index)
    {
        Locator l = Locator.xpath((null==prefix?"":prefix) + "//input[@name='ff_label" + index + "']");
        _test.click(l);
    }

    public void setColumnName(int index, String name)
    {
        setColumnName(null, index, name);
    }

    public void setColumnName(@Nullable String prefix, int index, String name)
    {
        Locator l = Locator.xpath((null==prefix?"":prefix) + "//input[@name='ff_name" + index + "']");
        _test.setFormElement(l, name);
        _test.pressTab(l);
    }

    public void setColumnLabel(int index, String label)
    {
        setColumnLabel(null, index, label);
    }

    public void setColumnLabel(@Nullable String prefix, int index, String label)
    {
        Locator l = Locator.xpath((null==prefix?"":prefix) + "//input[@name='ff_label" + index + "']");
        _test.setFormElement(l, label);
        _test.pressTab(l);
    }

    public void setColumnType(int index, ListColumnType type)
    {
        setColumnType(null, index, type);
    }

    public void setColumnType(@Nullable String prefix, int index, ListColumnType type)
    {
        setColumnType(prefix, null, type, index);
    }

    public void setColumnType(int index, LookupInfo lookup)
    {
        setColumnType(null, index, lookup);
    }

    public void setColumnType(@Nullable String prefix, int index, LookupInfo lookup)
    {
        setColumnType(prefix, lookup, null, index);
    }

    @LogMethod(quiet = true)
    private void setColumnType(@Nullable String prefix, @Nullable LookupInfo lookup, @LoggedParam @Nullable ListColumnType colType, @LoggedParam int i)
    {
        // click the combobox trigger image
        _test.click(Locator.xpath((null==prefix?"":prefix) + "//input[@name='ff_type" + i + "']/../div[contains(@class, 'x-form-trigger-arrow')]"));
        // click lookup checkbox
        _test._extHelper.waitForExtDialog("Choose Field Type", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.checkRadioButton(Locator.xpath("//label[text()='" + (lookup != null ? "Lookup" : colType) + "']/../input[@name = 'rangeURI']"));
        if (lookup != null)
        {
            _test.waitForElement(Locator.xpath("//input[@name='lookupContainer'][not(@disabled)]"));

            if (lookup.getFolder() != null)
            {
                selectLookupComboItem("lookupContainer", lookup.getFolder());
            }

            if (!lookup.getSchema().equals(_test.getFormElement(Locator.css("input[name=schema]"))))
            {
                selectLookupComboItem("schema", lookup.getSchema());
            }
            else
                _test.waitForElement(Locator.xpath("//div").withClass("test-marker-" + lookup.getSchema()).append("/input[@name='schema']"));

            selectLookupTableComboItem(lookup.getTable(), lookup.getTableType());
        }

        _test.clickButton("Apply", 0);
        _test._extHelper.waitForExtDialogToDisappear("Choose Field Type");
    }

    private void selectLookupComboItem(String fieldName, String value)
    {
        selectLookupComboItem(fieldName, value, 1);
    }

    private void selectLookupComboItem(String fieldName, String value, int attempt)
    {
        _test.log("Select lookup combo item '" + fieldName + "', value=" + value + ", attempt=" + attempt);
        _test.click(Locator.css("input[name=" + fieldName + "] + div.x-form-trigger"));
        try
        {
            _test.scrollIntoView(Locator.tag("div").withClass("x-combo-list-item").withText(value), false);
            _test.waitAndClick(500 * attempt, Locator.tag("div").withClass("x-combo-list-item").withText(value), 0);
            _test.log(".. selected");
        }
        catch (NoSuchElementException retry) // Workaround: sometimes fails on slower machines
        {
            // Stop after 4 attempts
            if (attempt == 4)
                throw retry;

            _test.fireEvent(Locator.css("input[name=" + fieldName + "]"), BaseWebDriverTest.SeleniumEvent.blur);
            selectLookupComboItem(fieldName, value, attempt + 1);
        }

        try
        {
            _test.waitForElement(Locator.xpath("//div").withClass("test-marker-" + value).append("/input[@name='" + fieldName + "']"));
            _test.log(".. test-marker updated");
        }
        catch (NoSuchElementException ignore)
        {
            _test.log(".. failed to update test-marker, soldier on anyway");
        }
    }

    private void selectLookupTableComboItem(String table, String tableType)
    {
        selectLookupTableComboItem(table, tableType, 1);
    }

    private void selectLookupTableComboItem(String table, String tableType, int attempt)
    {
        final String comboSubstring =
                null == tableType || tableType.isEmpty() ?
                        table + " (" :
                        String.format("%s (%s)", table, tableType);
        _test.log("Select lookup table combo item '" + table + "', attempt=" + attempt);
        String fieldName = "table";
        _test.click(Locator.css("input[name="+fieldName+"] + div.x-form-trigger"));
        try
        {
            _test.waitAndClick(500*attempt, Locator.tagWithClass("div", "x-combo-list-item").startsWith(comboSubstring), 0);
        }
        catch (NoSuchElementException retry) // Workaround: sometimes fails on slower machines
        {
            // Stop after 4 attempts
            if (attempt == 4)
                throw retry;

            _test.fireEvent(Locator.css("input[name=" + fieldName + "]"), BaseWebDriverTest.SeleniumEvent.blur);
            selectLookupTableComboItem(table, tableType, attempt + 1);
        }
        _test.waitForElement(Locator.xpath("//div").withClass("test-marker-" + table).append("/input[@name='" + fieldName + "']"));
    }

    public void selectPropertyTab(String name)
    {
        selectPropertyTab(null, name);
    }

    public void selectPropertyTab(@Nullable String prefix, String name)
    {
        _test.click(Locator.xpath((null == prefix ? "" : prefix) + "//span[contains(@class,'x-tab-strip-text') and text()='" + name + "']"));
    }

    public void clickRequired(String prefix)
    {
        selectPropertyTab(prefix, "Validators");
        Locator l = Locator.xpath((null==prefix?"":prefix) + "//input[@name='required']");
        _test.waitForElement(l, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.checkCheckbox(l);
    }

    public void clickMvEnabled(String prefix)
    {
        selectPropertyTab(prefix, "Advanced");
        Locator l = Locator.xpath((null==prefix?"":prefix) + "//input[@name='mvEnabled']");
        _test.waitForElement(l, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.checkCheckbox(l);
    }

    /**
     * Set the value on the List Designer Scale widget
     * @param value
     */
    public void setColumnScale(Integer value)
    {
        if (value == null)
            return;

        selectPropertyTab("Advanced");
        Locator l = DesignerLocators.scaleTextbox;
        _test.waitForElement(l, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        if (value == Integer.MAX_VALUE)
        {
            _test.checkCheckbox(DesignerLocators.maxCheckbox);
        }
        else
        {
            _test.uncheckCheckbox(DesignerLocators.maxCheckbox);
            _test.getElement(l).clear();
            _test.setFormElement(l, value.toString());
        }
    }

    /**
     * Gets the value from the Scale Textbox on the List Designer
     */
    public Integer getColumnScale()
    {
        selectPropertyTab("Advanced");
        String value = _test.getFormElement(DesignerLocators.scaleTextbox);
        return Integer.valueOf(value.replace(",",""));
    }

    @LogMethod(quiet = true)
    public void addField(String areaTitle, @LoggedParam String name, String label, ListColumnType type)
    {
        String prefix = _test.getPropertyXPath(areaTitle);
        Locator addField = Locator.xpath(prefix + "//span" + Locator.lkButton("Add Field").getPath());
        _test.waitForElement(addField);

        clickLastFieldIfExists(prefix);

        // click the add field button
        _test.click(addField);
        int newFieldIndex = findNewFieldIndex(prefix);

        // set the field values
        setColumnName(prefix, newFieldIndex, name);
        setColumnLabel(prefix, newFieldIndex, label);
        setColumnType(prefix, newFieldIndex, type);
    }

    /*
     *  click the last Field in the section if a selectable field is present
     *  calling this method ensures that the new field will be last in the list
     *  @param a prefix generated by getPropertyXPath(areaTitle)
     */
    private void clickLastFieldIfExists(String prefix)
    {
        Locator fieldLoc = Locator.xpath(prefix + "//input[starts-with(@name, 'ff_name')]");
        List<WebElement> fieldList = fieldLoc.findElements(_test.getDriver());
        if (fieldList.size() > 0)
        {
            String lastField = fieldList.get(fieldList.size() -1 ).getAttribute("name");
            Locator lastFieldLoc = Locator.xpath(prefix + "//input[@name='" + lastField + "']");
            _test.click(lastFieldLoc);
        }
    }

    /*
     *  find the new field at the end of the list
     *  @param a prefix generated by getPropertyXPath(areaTitle)
     */
    private int findNewFieldIndex(String prefix)
    {
        int newFieldIndex = 0;
        Locator fieldLoc = Locator.xpath(prefix + "//input[starts-with(@name, 'ff_name')]");
        List<WebElement> fieldList = fieldLoc.findElements(_test.getDriver());
        String lastField = fieldList.get(fieldList.size() -1 ).getAttribute("name");
        // extract the last field index
        Pattern p = Pattern.compile("[0-9]+$");
        Matcher m = p.matcher(lastField);
        if (m.find()) {
            String result = m.group();
            newFieldIndex = Integer.parseInt(result);
        }
        return newFieldIndex;
    }

    public void addLookupField(String areaTitle, int index, String name, String label, LookupInfo type)
    {
        String prefix = areaTitle==null ? "" : _test.getPropertyXPath(areaTitle);
        String addField = prefix + "//span" + Locator.lkButton("Add Field").getPath();
        _test.click(Locator.xpath(addField));
        _test.waitForElement(Locator.xpath(prefix + "//input[@name='ff_name" + index + "']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        setColumnName(prefix, index, name);
        setColumnLabel(prefix, index, label);
        setColumnType(prefix, index, type);
    }

    public void deleteField(String areaTitle, int index)
    {
        String prefix = _test.getPropertyXPathContains(areaTitle);
        WebElement deleteButton = _test.waitForElement(Locator.xpath(prefix + "//div[@id='partdelete_" + index + "']"));
        deleteButton.click();

        _test.waitFor(() ->
        {
            if (ExpectedConditions.stalenessOf(deleteButton).apply(_test.getDriver()))
                return true;

            try
            {
                WebElement okButton = Locator.lkButton("OK").findElement(_test.getDriver());
                okButton.click();
                _test.shortWait().until(ExpectedConditions.stalenessOf(okButton));
            }
            catch (NoSuchElementException ignore) {}
            return false;
        }, "Failed to delete field #" + index, WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public void addFieldsNoImport(String fieldList)
    {
        String name;
        String label;
        String type;
        String format;
        String hidden;
        String required;
        String mvenabled;
        String description;

        Scanner reader = new Scanner(fieldList);
        while (reader.hasNextLine())
        {
            String line = reader.nextLine();
            Scanner lineReader = new Scanner(line);
            lineReader.useDelimiter("\t");

            name = lineReader.next();
            if ("Property".equals(name))
            {
                line = reader.nextLine();
                lineReader = new Scanner(line);
                lineReader.useDelimiter("\t");
                name = lineReader.next();
            }
            label = lineReader.next();
            type = lineReader.next();
            format = lineReader.next();
            required = lineReader.next();
            hidden = lineReader.next();
            mvenabled = lineReader.next();
            if (lineReader.hasNext())
            {
                description = lineReader.next();
            }
            else description = "";
            if (type.equals("http://www.w3.org/2001/XMLSchema#string")) type = "String";
            if (type.equals("http://www.w3.org/2001/XMLSchema#double")) type = "Double";
            if (type.equals("http://www.w3.org/2001/XMLSchema#int")) type = "Integer";
            if (type.equals("http://www.w3.org/2001/XMLSchema#dateTime")) type = "DateTime";
            if (type.equals("http://www.w3.org/2001/XMLSchema#multiLine")) type = "MultiLine";
            if (type.equals("http://www.w3.org/2001/XMLSchema#boolean")) type = "Boolean";


            ListColumnType typeEnum = ListColumnType.valueOf(type);
            ListColumn newCol = new ListColumn(name, label, typeEnum, description);

            if (required.equals("TRUE")) newCol.setRequired(true);
            if (mvenabled.equals("TRUE")) newCol.setMvEnabled(true);
            addField(newCol);
        }
    }

    public enum RangeType
    {
        Equals("Equals"), NE("Does Not Equal"), GT("Greater than"), GTE("Greater than or Equals"), LT("Less than"), LTE("Less than or Equals");
        private final String _description;

        private RangeType(String description)
        {
            _description = description;
        }

        public String toString()
        {
            return _description;
        }
    }

    public enum ListColumnType
    {
        MultiLine("Multi-Line Text"), Integer("Integer"), String("Text (String)"), Subject("Subject/Participant (String)"), DateTime("DateTime"), Boolean("Boolean"),
        Double("Number (Double)"), File("File"), AutoInteger("Auto-Increment Integer"), Flag("Flag (String)"), Attachment("Attachment");

        private final String _description;

        private ListColumnType(String description)
        {
            _description = description;
        }

        public String toString()
        {
            return _description;
        }
    }

    public static class LookupInfo
    {
        private String _folder;
        private String _schema;
        private String _table;
        private String _tableType;

        public LookupInfo(@Nullable String folder, String schema, String table)
        {
            _folder = ("".equals(folder) ? null : folder);
            //container must exactly match an item in the dropdown
            if(_folder != null && !_folder.startsWith("/"))
                _folder = "/" + _folder;

            _schema = ("".equals(schema) ? null : schema);
            _table = ("".equals(table) ? null : table);
        }

        public String getFolder()
        {
            return _folder;
        }

        public String getSchema()
        {
            return _schema;
        }

        public String getTable()
        {
            return _table;
        }

        public String getTableType()
        {
            return _tableType;
        }

        public void setTableType(String tableType)
        {
            _tableType = tableType;
        }

    }

    public static abstract class FieldValidator
    {
        private String _name;
        private String _description;
        private String _message;

        public FieldValidator(String name, String description, String message)
        {
            _name = name;
            _description = description;
            _message = message;
        }

        public String getName()
        {
            return _name;
        }

        public String getDescription()
        {
            return _description;
        }

        public String getMessage()
        {
            return _message;
        }
    }

    public static class RegExValidator extends FieldValidator
    {
        private String _expression;

        public RegExValidator(String name, String description, String message, String expression)
        {
            super(name, description, message);
            _expression = expression;
        }

        public String getExpression()
        {
            return _expression;
        }
    }

    public static class RangeValidator extends FieldValidator
    {
        private RangeType _firstType;
        private String _firstRange;
        private RangeType _secondType;
        private String _secondRange;

        public RangeValidator(String name, String description, String message, RangeType firstType, String firstRange)
        {
            super(name, description, message);
            _firstType = firstType;
            _firstRange = firstRange;
        }

        public RangeValidator(String name, String description, String message, RangeType firstType, String firstRange, RangeType secondType, String secondRange)
        {
            this(name, description, message, firstType, firstRange);
            _secondType = secondType;
            _secondRange = secondRange;
        }

        public RangeType getFirstType()
        {
            return _firstType;
        }

        public String getFirstRange()
        {
            return _firstRange;
        }

        public RangeType getSecondType()
        {
            return _secondType;
        }

        public String getSecondRange()
        {
            return _secondRange;
        }
    }

    public static class ListColumn
    {
        private String _name;
        private String _label;
        private ListColumnType _type;
        private String _description;
        private String _format;
        private boolean _mvEnabled;
        private boolean _required;
        private LookupInfo _lookup;
        private FieldValidator _validator;
        private String _url;
        private Integer _scale;

        public ListColumn(String name, String label, ListColumnType type, String description, String format, LookupInfo lookup, FieldValidator validator, String url, Integer scale)
        {
            _name = name;
            _label = label;
            _type = type;
            _description = description;
            _format = format;
            _lookup = lookup;
            _validator = validator;
            _url = url;
            _scale = scale;
        }

        public ListColumn(String name, String label, ListColumnType type, String description, String format, LookupInfo lookup, FieldValidator validator, String url)
        {
            this(name, label, type, description, format, lookup, validator, url, null);
        }

        public ListColumn(String name, String label, ListColumnType type, String description, LookupInfo lookup)
        {
            this(name, label, type, description, null, lookup, null, null);
        }

        public ListColumn(String name, String label, ListColumnType type, String description, String format)
        {
            this(name, label, type, description, format, null, null, null);
        }

        public ListColumn(String name, String label, ListColumnType type, String description)
        {
            this(name, label, type, description, null, null, null, null);
        }

        public ListColumn(String name, String label, ListColumnType type, String description, FieldValidator validator)
        {
            this(name, label, type, description, null, null, validator, null);
        }

        public String getName()
        {
            return _name;
        }

        public String getLabel()
        {
            return _label;
        }

        public ListColumnType getType()
        {
            return _type;
        }

        public String getDescription()
        {
            return _description;
        }

        public String getFormat()
        {
            return _format;
        }

        public LookupInfo getLookup()
        {
            return _lookup;
        }

        public FieldValidator getValidator()
        {
            return _validator;
        }

        public boolean isMvEnabled()
        {
            return _mvEnabled;
        }

        public void setMvEnabled(boolean mvEnabled)
        {
            _mvEnabled = mvEnabled;
        }

        public boolean isRequired()
        {
            return _required;
        }

        public void setRequired(boolean required)
        {
            _required = required;
        }

        public void setURL(String url)
        {
            _url = url;
        }

        public String getURL()
        {
            return _url;
        }

        public void setScale(Integer value)
        {
            _scale = value;
        }

        public Integer getScale()
        {
            return _scale;
        }
    }

    /**
     * Set of locators for navigating the List Designer page
     */
    public static class DesignerLocators extends org.labkey.test.Locators
    {
        public static Locator.XPathLocator maxCheckbox = Locator.xpath("//input[@name='isMaxText']");
        public static Locator.XPathLocator scaleTextbox = Locator.xpath("//input[@name='scale']");

    }

}
