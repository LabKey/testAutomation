/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsDriver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ListHelper extends LabKeySiteWrapper
{
    public static final String IMPORT_ERROR_SIGNAL = "importFailureSignal"; // See query/import.jsp
    private WrapsDriver _wrapsDriver;
    private String _editorTitle = "List Fields"; // todo: capture the title of the first propertiesEditor and use it

    public ListHelper(WrapsDriver wrapsDriver)
    {
        _wrapsDriver = wrapsDriver;
    }

    public ListHelper(WebDriver driver)
    {
        this(() -> driver);
    }

    public ListHelper withEditorTitle(String editorTitle)
    {
        _editorTitle = editorTitle;
        return this;
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return _wrapsDriver.getWrappedDriver();
    }

    public void uploadCSVData(String listData)
    {
        clickImportData();
        setFormElement(Locator.id("tsv3"), listData);
        _extHelper.selectComboBoxItem("Format:", "Comma-separated text (csv)");
        submitImportTsv_success();
    }

    public PropertiesEditor getListFieldEditor()
    {
        return getSomeEditorByTitle(_editorTitle);
    }

    // TODO: Remove callers who use ListHelper to edit non-list PropertiesEditors (hence "some")
    private PropertiesEditor getSomeEditorByTitle(final String title)
    {
        return PropertiesEditor.PropertiesEditor(getDriver()).withTitleContaining(title).find();
    }

    public void uploadData(String listData)
    {
        clickImportData();
        submitTsvData(listData);
    }

    public void submitTsvData(String listData)
    {
        setFormElement(Locator.id("tsv3"), listData);
        submitImportTsv_success();
    }

    public void submitImportTsv_success()
    {
        clickButton("Submit");
        waitForElement(Locator.css(".labkey-data-region"));
    }

    // null means any error
    public void submitImportTsv_error(String error)
    {
        doAndWaitForPageSignal(() -> clickButton("Submit", 0),
                IMPORT_ERROR_SIGNAL);
        if (error != null)
        {
            String errors = String.join(", ", getTexts(Locators.labkeyError.findElements(getDriver())));
            assertTrue("Didn't find expected error ['" + error + "'] in [" + errors + "]", errors.contains(error));
        }
    }

    public void submitImportTsv_errors(List<String> errors)
    {
        doAndWaitForPageSignal(() -> clickButton("Submit", 0),
                IMPORT_ERROR_SIGNAL);
        if (errors == null || errors.isEmpty())
            waitForElement(Locator.css(".labkey-error"));
        else
        {
            for (String err : errors)
            {
                waitForElement(Locator.css(".labkey-error").containing(err));
            }
        }
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
        click(Locator.tagWithClass("h3", "panel-title").containing("Upload file"));
        setFormElement(Locator.name("file"), inputFile);
        clickButton("Submit", wait);
    }

    public void checkIndexFileAttachements(boolean index)
    {
        Locator indexFileAttachmentsCheckbox = Locator.checkboxByLabel("Index file attachments", false);
        if (index)
            checkCheckbox(indexFileAttachmentsCheckbox);
        else
            uncheckCheckbox(indexFileAttachmentsCheckbox);
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
        DataRegionTable list = new DataRegionTable("query", getDriver());
        list.clickInsertNewRow();
        setRowData(data, validateText);
    }

    protected void setRowData(Map<String, String> data, boolean validateText)
    {
        for (String key : data.keySet())
        {
            waitForElement(Locator.name("quf_" + key));
            WebElement field = Locator.name("quf_" + key).findElement(getDriver());
            String inputType = field.getAttribute("type");
            switch (inputType)
            {
                case "file":
                    setFormElement(field, new File(data.get(key)));
                    break;
                case "checkbox":
                    if(data.get(key).toLowerCase().equals("true"))
                    {
                        setCheckbox(field, true);
                    }
                    else
                    {
                        setCheckbox(field, false);
                    }
                    break;
                default:
                    setFormElement(field, data.get(key));
            }
        }
        clickButton("Submit");

        if (validateText)
        {
            assertTextPresent(data.values().iterator().next());  //make sure some text from the map is present
        }
    }

    /**
     * From the list data grid, edit an existing row
     *
     * @param id the row number (1 based)
     * @deprecated use {@link DataRegionTable#updateRow(String, Map)}
     */
    @Deprecated
    public void updateRow(int id, Map<String, String> data)
    {
        updateRow(id, data, true);
    }

    /**
     *
     * @deprecated use {@link DataRegionTable#updateRow(String, Map, boolean)}
     */
    @Deprecated
    public void updateRow(int id, Map<String, String> data, boolean validateText)
    {
        DataRegionTable dr = new DataRegionTable("query", getDriver());
        clickAndWait(dr.updateLink(id - 1));
        setRowData(data, validateText);
    }

    /**
     * Starting at the grid view of a list, delete it
     */
    public void deleteList()
    {
        String url = getCurrentRelativeURL().replace("grid.view", "deleteListDefinition.view");
        beginAt(url);
        clickButton("OK");
    }

    @LogMethod
    public void createListFromTab(String tabName, String listName, ListColumnType listKeyType, String listKeyName, ListColumn... cols)
    {
        beginCreateListFromTab(tabName, listName);
        createListHelper(listName, listKeyType, listKeyName, cols);
    }

    @LogMethod
    public void createList(String containerPath, @LoggedParam String listName, ListColumnType listKeyType, String listKeyName, ListColumn... cols)
    {
        beginCreateList(containerPath, listName);
        createListHelper(listName, listKeyType, listKeyName, cols);
    }

    private void createListHelper(String listName, ListColumnType listKeyType, String listKeyName, ListColumn... cols)
    {
        selectOptionByText(Locator.id("ff_keyType"), listKeyType.toString());
        setFormElement(Locator.id("ff_keyName"), listKeyName);
        fireEvent(Locator.id("ff_keyName"), BaseWebDriverTest.SeleniumEvent.blur);

        clickButton("Create List", 0);
        waitForElement(Locator.name("ff_description"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.name("ff_name0"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        log("Check that list was created correctly");
        waitForFormElementToEqual(Locator.name("ff_name"), listName);
        waitForFormElementToEqual(Locator.name("ff_name0"), listKeyName);

        log("Add columns");

        for (ListColumn col : cols)
        {
            addField(col);
        }

        clickSave();

        for (ListColumn col : cols)
        {
            assertTextPresent(col.getName());
            if (!StringUtils.isEmpty(col.getLabel()) && !col.getName().equals(col.getLabel()))
                assertTextPresent(col.getLabel());
        }
    }

    public void addField(ListColumn col)
    {
        PropertiesEditor listFieldEditor = getListFieldEditor();
        PropertiesEditor.FieldRow newField = listFieldEditor.addField();
        newField.setName(col.getName());
        newField.setLabel(col.getLabel());
        newField.setType(col.getLookup(), col.getType());

        listFieldEditor.fieldProperties().selectDisplayTab();
        if (col.getDescription() != null)
        {
            setFormElement(Locator.id("propertyDescription"), col.getDescription());
        }

        if (col.getFormat() != null)
        {
            listFieldEditor.fieldProperties().selectFormatTab().propertyFormat.set(col.getFormat());
        }

        if (null != col.getURL())
        {
            setFormElement(Locator.id("url"), col.getURL());
        }

        if (col.isRequired())
        {
            listFieldEditor.fieldProperties().selectValidatorsTab().required.set(true);
        }

        FieldDefinition.FieldValidator validator = col.getValidator();
        if (validator != null)
        {
            listFieldEditor.fieldProperties().selectValidatorsTab();
            if (validator instanceof RegExValidator)
                clickButton("Add RegEx Validator", 0);
            else
                clickButton("Add Range Validator", 0);
            setFormElement(Locator.name("name"), validator.getName());
            setFormElement(Locator.name("description"), validator.getDescription());
            setFormElement(Locator.name("errorMessage"), validator.getMessage());

            if (validator instanceof RegExValidator)
            {
                setFormElement(Locator.name("expression"), ((RegExValidator)validator).getExpression());
            }
            else if (validator instanceof RangeValidator)
            {
                setFormElement(Locator.name("firstRangeValue"), ((RangeValidator)validator).getFirstRange());
            }
            clickButton("OK", 0);
        }

        if (col.isMvEnabled())
        {
            listFieldEditor.fieldProperties().selectAdvancedTab();
            clickMvEnabled("");
        }

        if (col.getScale() != null)
        {
            listFieldEditor.fieldProperties().selectAdvancedTab();
            setColumnScale(col.getScale());
        }
    }

    public void beginCreateListFromTab(String tabName, String listName)
    {
        clickTab(tabName.replace(" ", ""));
        beginCreateListHelper(listName);
    }

    public void clickTab(String tabname)
    {
        log("Selecting tab " + tabname);
        clickAndWait(Locator.folderTab(tabname));
    }

    // initial "create list" steps common to both manual and import from file scenarios
    public void beginCreateList(String containerPath, String listName)
    {
        beginAt(WebTestHelper.buildURL("project", containerPath, "begin"));
        beginCreateListHelper(listName);
    }

    private void beginCreateListHelper(String listName)
    {
        if (!isElementPresent(Locator.linkWithText("Lists")))
        {
            PortalHelper portalHelper = new PortalHelper(getDriver());
            portalHelper.addWebPart("Lists");
        }

        waitAndClickAndWait(Locator.linkWithText("manage lists"));

        log("Add List");
        clickButton("Create New List");
        waitForElement(Locator.id("ff_name"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.id("ff_name"), listName);
        fireEvent(Locator.id("ff_name"), BaseWebDriverTest.SeleniumEvent.blur);
    }


    public void createListFromFile(String containerPath, String listName, File inputFile)
    {
        beginCreateList(containerPath, listName);

        click(Locator.xpath("//span[@id='fileImport']/input[@type='checkbox']"));
        //test.clickCheckbox("fileImport");

        clickButton("Create List", 0);

        waitForElement(Locator.xpath("//input[@name='uploadFormElement']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        setFormElement(Locator.name("uploadFormElement"), inputFile);

        waitForElement(Locator.xpath("//span[@id='button_Import']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        clickButton("Import");
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
        clickFolder(folderName);
        importListArchive(inputFile);
    }

    public void importListArchive(File inputFile)
    {
        assertTrue("Unable to locate input file: " + inputFile, inputFile.exists());

        if (!isElementPresent(Locator.linkWithText("Lists")))
        {
            PortalHelper portalHelper = new PortalHelper(getDriver());
            portalHelper.addWebPart("Lists");
        }

        waitAndClickAndWait(Locator.linkWithText("manage lists"));

        log("Import List Archive");
        clickButton("Import List Archive");
        waitForElement(Locator.xpath("//input[@name='listZip']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        setFormElement(Locator.name("listZip"), inputFile);
        clickButton("Import List Archive");
        assertElementNotPresent(Locator.tagWithClass("div", "labkey-error"));
    }

    public void clickImportData()
    {
        if(isElementPresent(Locator.lkButton("Import Data")))
            waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Import Data"), BaseWebDriverTest.WAIT_FOR_PAGE);
        else
        {
            log("Was not able to find the 'Import Data' button on the menu, trying the 'Insert/Import Data' menu item.");
            DataRegionTable list = DataRegionTable.DataRegion(getDriver()).find();
            list.clickImportBulkData();
        }
        waitForElement(Locator.id("tsv3"));
    }

    public void clickEditDesign()
    {
        waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Edit Design"), 0);
        waitForElement(Locator.lkButton("Cancel"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.id("ff_description"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.lkButton("Add Field"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void clickEditFields()
    {
        waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Edit Fields"), 0);
        waitForElement(Locator.lkButton("Cancel"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.lkButton("Add Field"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void clickSave()
    {
        waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Save"), 0);
        waitForElement(Locator.lkButton("Edit Design"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.lkButton("Done"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        mouseOut(); // After clicking save, sometimes the page scrolls so that the project menu is under the mouse
    }

    public void clickDeleteList()
    {
        waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Delete List"), BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void clickRow(int index)
    {
        clickRow(null, index);
    }

    public void clickRow(@Nullable String prefix, int index)
    {
        Locator l = Locator.xpath((null==prefix?"":prefix) + "//input[@name='ff_label" + index + "']");
        click(l);
    }

    public void setColumnName(int index, String name)
    {
        setColumnName(null, index, name);
    }

    public void setColumnName(@Nullable String prefix, int index, String name)
    {
        Locator l = Locator.xpath((null==prefix?"":prefix) + "//input[@name='ff_name" + index + "']");
        setFormElement(l, name);
        pressTab(l);
    }

    public void setColumnLabel(int index, String label)
    {
        setColumnLabel(null, index, label);
    }

    public void setColumnLabel(@Nullable String prefix, int index, String label)
    {
        Locator l = Locator.xpath((null==prefix?"":prefix) + "//input[@name='ff_label" + index + "']");
        setFormElement(l, label);
        pressTab(l);
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
    private void setColumnType(@Nullable String prefix, @Nullable FieldDefinition.LookupInfo lookup, @LoggedParam @Nullable ListColumnType colType, @LoggedParam int i)
    {
        Locator.XPathLocator typeField = Locator.xpath((null==prefix?"":prefix) + "//input[@name='ff_type" + i + "']");
        // click the combobox trigger image
        click(typeField.append("/../div[contains(@class, 'x-form-trigger-arrow')]"));
        // click lookup checkbox
        _extHelper.waitForExtDialog("Choose Field Type", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        checkRadioButton(Locator.xpath("//label[text()='" + (lookup != null ? "Lookup" : colType) + "']/../input[@name = 'rangeURI']"));
        if (lookup != null)
        {
            waitForElement(Locator.xpath("//input[@name='lookupContainer'][not(@disabled)]"));

            if (lookup.getFolder() != null)
            {
                selectLookupComboItem("lookupContainer", lookup.getFolder());
            }

            if (!lookup.getSchema().equals(getFormElement(Locator.css("input[name=schema]"))))
            {
                selectLookupComboItem("schema", lookup.getSchema());
            }
            else
                waitForElement(Locator.xpath("//div").withClass("test-marker-" + lookup.getSchema()).append("/input[@name='schema']"));

            selectLookupTableComboItem(lookup.getTable(), lookup.getTableType());
        }

        clickButton("Apply", 0);
        _extHelper.waitForExtDialogToDisappear("Choose Field Type");
        String actualType = typeField.findElement(getDriver()).getAttribute("value");
        if (lookup != null)
        {
            String expectedType = new StringBuilder()
                    .append(lookup.getSchema())
                    .append(".")
                    .append(lookup.getTable())
                    .append(" (").toString();
            assertTrue("Test error: Failed to define lookup column. Expected: " + expectedType + " Actual: " + actualType, actualType.contains(expectedType));
        }
        else
        {
            assertEquals("Test error: Failed to set column type", colType.toString(), actualType);
        }
    }

    private void selectLookupComboItem(String fieldName, String value)
    {
        selectLookupComboItem(fieldName, value, 1);
    }

    private void selectLookupComboItem(String fieldName, String value, int attempt)
    {
        log("Select lookup combo item '" + fieldName + "', value=" + value + ", attempt=" + attempt);
        click(Locator.css("input[name=" + fieldName + "] + div.x-form-trigger"));
        try
        {
            scrollIntoView(Locator.tag("div").withClass("x-combo-list-item").withText(value), false);
            waitAndClick(500 * attempt, Locator.tag("div").withClass("x-combo-list-item").withText(value), 0);
            log(".. selected");
        }
        catch (NoSuchElementException retry) // Workaround: sometimes fails on slower machines
        {
            // Stop after 4 attempts
            if (attempt == 4)
                throw retry;

            fireEvent(Locator.css("input[name=" + fieldName + "]"), BaseWebDriverTest.SeleniumEvent.blur);
            selectLookupComboItem(fieldName, value, attempt + 1);
        }

        try
        {
            waitForElement(Locator.xpath("//div").withClass("test-marker-" + value).append("/input[@name='" + fieldName + "']"));
            log(".. test-marker updated");
        }
        catch (NoSuchElementException ignore)
        {
            log(".. failed to update test-marker, soldier on anyway");
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
        log("Select lookup table combo item '" + table + "', attempt=" + attempt);
        String fieldName = "table";
        click(Locator.css("input[name="+fieldName+"] + div.x-form-trigger"));
        try
        {
            waitAndClick(500*attempt, Locator.tagWithClass("div", "x-combo-list-item").startsWith(comboSubstring), 0);
        }
        catch (NoSuchElementException retry) // Workaround: sometimes fails on slower machines
        {
            // Stop after 4 attempts
            if (attempt == 4)
                throw retry;

            fireEvent(Locator.css("input[name=" + fieldName + "]"), BaseWebDriverTest.SeleniumEvent.blur);
            selectLookupTableComboItem(table, tableType, attempt + 1);
        }
        waitForElement(Locator.xpath("//div").withClass("test-marker-" + table).append("/input[@name='" + fieldName + "']"));
    }

    public void selectPropertyTab(String name)
    {
        selectPropertyTab(null, name);
    }

    public void selectPropertyTab(@Nullable String prefix, String name)
    {
        click(Locator.xpath((null == prefix ? "" : prefix) + "//span[contains(@class,'x-tab-strip-text') and text()='" + name + "']"));
    }

    public void clickRequired(String prefix)
    {
        selectPropertyTab(prefix, "Validators");
        Locator l = Locator.xpath((null==prefix?"":prefix) + "//input[@name='required']");
        waitForElement(l, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        checkCheckbox(l);
    }

    public void clickMvEnabled(String prefix)
    {
        selectPropertyTab(prefix, "Advanced");
        Locator l = Locator.xpath((null==prefix?"":prefix) + "//input[@name='mvEnabled']");
        waitForElement(l, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        checkCheckbox(l);
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
        waitForElement(l, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        if (value == Integer.MAX_VALUE)
        {
            checkCheckbox(DesignerLocators.maxCheckbox);
        }
        else
        {
            uncheckCheckbox(DesignerLocators.maxCheckbox);
            l.findElement(getDriver()).clear();
            setFormElement(l, value.toString());
        }
    }

    /**
     * Gets the value from the Scale Textbox on the List Designer
     */
    public Integer getColumnScale()
    {
        selectPropertyTab("Advanced");
        String value = getFormElement(DesignerLocators.scaleTextbox);
        return Integer.valueOf(value.replace(",",""));
    }

    @LogMethod(quiet = true)
    public void addField(String areaTitle, @LoggedParam String name, String label, ListColumnType type)
    {
        addField(areaTitle, name, label, type, null);
    }

    @LogMethod(quiet = true)
    public void addField(String areaTitle, @LoggedParam String name, String label, ListColumnType type, FieldDefinition.FieldValidator validator)
    {
        addField(areaTitle, name, label, type, validator, false);
    }

    @LogMethod(quiet = true)
    public void addField(String areaTitle, @LoggedParam String name, String label, ListColumnType type, FieldDefinition.FieldValidator validator, boolean required)
    {
        getSomeEditorByTitle(areaTitle).addField(new FieldDefinition(name).setLabel(label).setType(type.toNew()).setValidator(validator).setRequired(required));
    }

    public void addLookupField(String areaTitle, int index, String name, String label, LookupInfo type)
    {
        String prefix = areaTitle==null ? "" : "//h3[text() = '" + areaTitle + "']/../..";
        String addField = prefix + "//span" + Locator.lkButton("Add Field").toXpath();
        click(Locator.xpath(addField));
        waitForElement(Locator.xpath(prefix + "//input[@name='ff_name" + index + "']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        setColumnName(prefix, index, name);
        setColumnLabel(prefix, index, label);
        setColumnType(prefix, index, type);
    }

    public void deleteField(String areaTitle, int index)
    {
        getSomeEditorByTitle(areaTitle).selectField(index).markForDeletion();
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

    public List<String> getColumnNames()
    {
        List<String> columns = new ArrayList<>();

        List<WebElement> nameFields = Locator.xpath("//input[contains(@name, 'ff_name')]").findElements(getDriver());
        for(WebElement webElement : nameFields)
        {
            // If it is not the list name element then add it to the list.
            if (!webElement.getAttribute("name").trim().toLowerCase().equals("ff_name"))
            {
                columns.add(webElement.getAttribute("value"));
            }
        }

        return columns;
    }

    public enum RangeType
    {
        Equals("Equals"), NE("Does Not Equal"), GT("Greater than"), GTE("Greater than or Equals"), LT("Less than"), LTE("Less than or Equals");
        private final String _description;

        RangeType(String description)
        {
            _description = description;
        }

        public String toString()
        {
            return _description;
        }

        private FieldDefinition.RangeType toNew()
        {
            for (FieldDefinition.RangeType thisType : FieldDefinition.RangeType.values())
            {
                if (name().equals(thisType.name()))
                    return thisType;
            }
            throw new IllegalArgumentException("Type mismatch: " + this);
        }
    }

    public enum ListColumnType
    {
        MultiLine("Multi-Line Text"), Integer("Integer"), String("Text (String)"), Subject("Subject/Participant (String)"), DateTime("DateTime"), Boolean("Boolean"),
        Double("Number (Double)"), File("File"), AutoInteger("Auto-Increment Integer"), Flag("Flag (String)"), Attachment("Attachment"), User("User");

        private final String _description;

        ListColumnType(String description)
        {
            _description = description;
        }

        public String toString()
        {
            return _description;
        }

        private FieldDefinition.ColumnType toNew()
        {
            for (FieldDefinition.ColumnType thisType : FieldDefinition.ColumnType.values())
            {
                if (name().equals(thisType.name()))
                    return thisType;
            }
            throw new IllegalArgumentException("Type mismatch: " + this);
        }

        public static ListColumnType fromNew(FieldDefinition.ColumnType newType)
        {
            for (ListColumnType thisType : values())
            {
                if (newType.name().equals(thisType.name()))
                    return thisType;
            }
            throw new IllegalArgumentException("Type mismatch: " + newType);
        }
    }

    public static class LookupInfo extends FieldDefinition.LookupInfo
    {
        public LookupInfo(@Nullable String folder, String schema, String table)
        {
            super(folder, schema, table);
        }
    }

    public static class RegExValidator extends FieldDefinition.RegExValidator
    {
        public RegExValidator(String name, String description, String message, String expression)
        {
            super(name, description, message, expression);
        }
    }

    public static class RangeValidator extends FieldDefinition.RangeValidator
    {
        public RangeValidator(String name, String description, String message, RangeType firstType, String firstRange)
        {
            super(name, description, message, firstType.toNew(), firstRange);
        }

        public RangeValidator(String name, String description, String message, RangeType firstType, String firstRange, RangeType secondType, String secondRange)
        {
            super(name, description, message, firstType.toNew(), firstRange, secondType.toNew(), secondRange);
        }
    }

    public static class ListColumn extends FieldDefinition
    {
        public ListColumn(String name, String label, ListColumnType type, String description, String format, LookupInfo lookup, FieldValidator validator, String url, Integer scale)
        {
            super(name);
            setLabel(label);
            setType(type.toNew());
            setDescription(description);
            setFormat(format);
            setLookup(lookup);
            setValidator(validator);
            setURL(url);
            setScale(scale);
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

        public ListColumn(String name, String label, ListColumnType type)
        {
            this(name, label, type, null, null, null, null, null);
        }

        public ListColumn(String name, ListColumnType type)
        {
            this(name, null, type);
        }
    }

    /**
     * Set of locators for navigating the List Designer page
     */
    public static class DesignerLocators
    {
        public static Locator.XPathLocator maxCheckbox = Locator.xpath("//input[@name='isMaxText']");
        public static Locator.XPathLocator scaleTextbox = Locator.xpath("//input[@name='scale']");
    }
}
