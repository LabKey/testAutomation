/*
 * Copyright (c) 2012 LabKey Corporation
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

import junit.framework.Assert;
import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.Map;

/**
 * User: jeckels
 * Date: Nov 21, 2007
 */
public class ListHelperWD extends ListHelper
{
    BaseWebDriverTest _test;

    public ListHelperWD(BaseWebDriverTest test)
    {
        super(test);
        _test = test;
    }

    public void uploadData(String folderName, String listName, String listData)
    {
        _test.clickButton("Import Data");
        _test.setFormElement(Locator.id("tsv3"), listData);
        _submitImportTsv(null);
    }

    public void submitTsvData(String listData)
    {
        _test.setFormElement(Locator.id("tsv3"), listData);
        _submitImportTsv(null);
    }


    public void submitImportTsv_success()
    {
        _submitImportTsv(null);
    }

    // null means any error
    public void submitImportTsv_error(String error)
    {
        _submitImportTsv(null == error ? "" : error);
    }

    private void _submitImportTsv(String error)
    {
        _test.clickButton("Submit", 0);
        if (null != error)
        {
            if (0<error.length())
                _test.waitForElement(Locator.css(".labkey-error").containing(error));
        }
        else
        {
            _test.waitForPageToLoad();
        }
    }

    /**
     * From the list data grid, insert a new entry into the current list
     *
     * @param data key = the the name of the field, value = the value to enter in that field
     */

    public void insertNewRow(Map<String, String> data)
    {

        _test.clickButton("Insert New");
        for(String key : data.keySet())
        {
            _test.setFormElement(Locator.name("quf_" + key), data.get(key));
        }
        _test.clickButton("Submit");
        _test.assertTextPresent(data.get(data.keySet().iterator().next()));  //make sure some text from the map is present
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
/*

    public static class LookupInfo
    {
        private String _folder;
        private String _schema;
        private String _table;

        public LookupInfo(String folder, String schema, String table)
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

    public enum ListColumnType
    {
        MutliLine("Multi-Line Text"), Integer("Integer"), String("Text (String)"), DateTime("DateTime"), Boolean("Boolean"),
        Double("Number (Double)"), File("File"), AutoInteger("Auto-Increment Integer"), Flag("Flag");

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

        public ListColumn(String name, String label, ListColumnType type, String description, String format, LookupInfo lookup, FieldValidator validator, String url)
        {
            _name = name;
            _label = label;
            _type = type;
            _description = description;
            _format = format;
            _lookup = lookup;
            _validator = validator;
            _url = url;
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
    }

*/

    @LogMethod
    public void createListFromTab(String tabName, String listName, ListColumnType listKeyType, String listKeyName, ListColumn... cols)
    {
        beginCreateListFromTab(tabName, listName);
        createListHelper(listName, listKeyType, listKeyName, cols);
    }

    @LogMethod
    public void createList(String folderName, String listName, ListColumnType listKeyType, String listKeyName, ListColumn... cols)
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

        _test.log("Check that they were added");
        if (cols.length > 0)
        {
            _test.waitForElement(Locator.navButton("Export Fields"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            for (ListColumn col : cols)
            {
                _test.assertTextPresent(col.getName());
                if (!StringUtils.isEmpty(col.getLabel()) && !col.getName().equals(col.getLabel()))
                    _test.assertTextPresent(col.getLabel());
            }
        }
    }

    public void addField(ListColumn col)
    {
        int i = _test.getElementCount(Locator.css(".labkey-pad-cells > tbody > tr")) - 1;
        _test.waitForElement(Locator.id("button_Add Field"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.clickButton("Add Field", 0);
        _test.setFormElement(Locator.name("ff_name" + i),  col.getName());
        _test.setFormElement(Locator.name("ff_label" + i), col.getLabel());
        // Set type.
        LookupInfo lookup = col.getLookup();
        // click the combobox trigger image
        _test.click(Locator.xpath("//input[@name='ff_type" + i + "']/../div[contains(@class, 'x-form-trigger-arrow')]"));
        // click lookup checkbox
        _test._extHelper.waitForExtDialog("Choose Field Type", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.checkRadioButton(Locator.xpath("//label[text()='" + (lookup != null ? "Lookup" : col.getType().toString()) + "']/../input[@name = 'rangeURI']"));

        if (lookup != null)
        {
            _test._shortWait.until(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver driver)
                {
                    return driver.findElement(By.name("lookupContainer")).isEnabled();
                }
            });

            if (lookup.getFolder() != null)
            {
                _test.click(Locator.xpath("//input[@name = 'lookupContainer']/following-sibling::div[contains(@class, 'x-form-trigger-arrow')]"));
                Locator.css("div.x-combo-list-item").withText(lookup.getFolder()).waitForElmement(_test._driver, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT).click();
            }
            _test.sleep(500);

            _test.click(Locator.xpath("//input[@name = 'schema']/following-sibling::div[contains(@class, 'x-form-trigger-arrow')]"));
            Locator.css("div.x-combo-list-item").withText(lookup.getSchema()).waitForElmement(_test._driver, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT).click();
            _test.sleep(500);

            _test.click(Locator.xpath("//input[@name = 'table']/following-sibling::div[contains(@class, 'x-form-trigger-arrow')]"));
            Locator.css("div.x-combo-list-item").containing(lookup.getTable() + " (").waitForElmement(_test._driver, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT).click();
        }

        _test.clickButton("Apply", 0);

        _test._extHelper.waitForExtDialogToDisappear("Choose Field Type");
        // wait a while to make sure rangeURI is set (async check)
//        _test.sleep(1000);

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
    }

    public void beginCreateListFromTab(String tabName, String listName)
    {
        _test.clickTab(tabName);
        beginCreateListHelper(listName);
    }

    // initial "create list" steps common to both manual and import from file scenarios
    public void beginCreateList(String folderName, String listName)
    {
        _test.clickFolder(folderName);
        beginCreateListHelper(listName);
    }

    private void beginCreateListHelper(String listName)
    {
        if (!_test.isLinkPresentWithText("Lists"))
        {
            _test.addWebPart("Lists");
        }

        _test.clickLinkWithText("manage lists");

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

        _test.setFormElement("uploadFormElement", inputFile);

        _test.waitForElement(Locator.xpath("//span[@id='button_Import']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        _test.clickButton("Import");
    }

    public void importListArchive(String folderName, File inputFile)
    {
        Assert.assertTrue("Unable to locate input file: " + inputFile, inputFile.exists());

        _test.clickFolder(folderName);
        if (!_test.isLinkPresentWithText("Lists"))
        {
            _test.addWebPart("Lists");
        }

        _test.clickLinkWithText("manage lists");

        _test.log("Import List Archive");
        _test.clickButton("Import List Archive");
        _test.waitForElement(Locator.xpath("//input[@name='listZip']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        _test.setFormElement("listZip", inputFile);
        _test.clickButton("Import List Archive");
    }



    public void clickImportData()
    {
        _test.waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.navButton("Import Data"), BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void clickEditDesign()
    {
        _test.waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.navButton("Edit Design"), 0);
        _test.waitForElement(Locator.navButton("Cancel"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElement(Locator.id("ff_description"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElement(Locator.navButton("Add Field"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void clickSave()
    {
        _test.waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.navButton("Save"), 0);
        _test.waitForElement(Locator.navButton("Edit Design"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElement(Locator.navButton("Done"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void clickDeleteList()
    {
        _test.waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.navButton("Delete List"), BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void clickRow(int index)
    {
        clickRow(null, index);
    }

    public void clickRow(String prefix, int index)
    {
        Locator l = Locator.xpath((null==prefix?"":prefix) + "//input[@name='ff_name" + index + "']");
        _test.click(l);
    }

    public void setColumnName(int index, String name)
    {
        setColumnName(null, index, name);
    }
    public void setColumnName(String prefix, int index, String name)
    {
        Locator l = Locator.xpath((null==prefix?"":prefix) + "//input[@name='ff_name" + index + "']");
        _test.setFormElement(l, name);
        _test.pressTab(l);
    }
    public void setColumnLabel(int index, String label)
    {
        setColumnLabel(null,index,label);
    }
    public void setColumnLabel(String prefix, int index, String label)
    {
        Locator l = Locator.xpath((null==prefix?"":prefix) + "//input[@name='ff_label" + index + "']");
        _test.setFormElement(l, label);
        _test.pressTab(l);
    }
    public void setColumnType(int index, ListColumnType type)
    {
        setColumnType(null, index, type);
    }
    public void setColumnType(String prefix, int index, ListColumnType type)
    {
        Locator l = Locator.xpath((null==prefix?"":prefix) + "//input[@name='ff_type" + index + "']");
        _test.setFormElement(l, type.toString());
        _test.pressTab(l);
    }
    public void setColumnType(int index, LookupInfo lookup)
    {
        setColumnType(null, index, lookup);
    }
    public void setColumnType(String prefix, int index, LookupInfo lookup)
    {
        //test.click(Locator.xpath((null==prefix?"":prefix) + "//input[@name='ff_type" + index + "']"));
        _test.click(Locator.xpath((null==prefix?"":prefix) + "//input[@name='ff_type" + index + "']/../div[contains(@class, 'x-form-trigger-arrow')]"));
        if ( _test.isAlertPresent() ) _test.getAlert(); // Don't worry about schema alert until saving.
        _test.click(Locator.xpath("//div[./label[text() = 'Lookup']]/input[@type = 'radio']"));
        if ( lookup.getFolder() != null ) _test.setFormElement(Locator.name("lookupContainer"), lookup.getFolder());
        if ( lookup.getSchema() != null ) _test.setFormElement(Locator.name("schema"), lookup.getSchema());
        if ( lookup.getTable() != null ) _test.setFormElement(Locator.name("table"), lookup.getTable());
        _test.clickButton("Apply", 0);
        _test.sleep(1000);
    }

    public void selectPropertyTab(String name)
    {
        selectPropertyTab(null, name);
    }
    public void selectPropertyTab(String prefix, String name)
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

    public void addField(String areaTitle, int index, String name, String label, ListHelperWD.ListColumnType type)
    {
        String prefix = _test.getPropertyXPath(areaTitle);
        String addField = prefix + "//span" + Locator.navButton("Add Field").getPath();
        _test.click(Locator.xpath(addField));
        _test.waitForElement(Locator.xpath(prefix + "//input[@name='ff_name" + index + "']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        setColumnName(prefix, index, name);
        setColumnLabel(prefix, index, label);
        setColumnType(prefix, index, type);
    }

    public void addLookupField(String areaTitle, int index, String name, String label, ListHelperWD.LookupInfo type)
    {
        String prefix = areaTitle==null ? "" : _test.getPropertyXPath(areaTitle);
        String addField = prefix + "//span" + Locator.navButton("Add Field").getPath();
        _test.click(Locator.xpath(addField));
        _test.waitForElement(Locator.xpath(prefix + "//input[@name='ff_name" + index + "']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        setColumnName(prefix, index, name);
        setColumnLabel(prefix, index, label);
        setColumnType(prefix, index, type);
    }

    public void deleteField(String areaTitle, int index)
    {
        String prefix = _test.getPropertyXPath(areaTitle);
        _test.click(Locator.xpath(prefix + "//div[@id='partdelete_" + index + "']"));

        // If domain hasn't been saved yet, the 'OK' prompt will not appear.
        Locator.XPathLocator buttonLocator = _test.getButtonLocator("OK");
        // TODO: Be smarter about this.  Might miss the OK that should be there.
        if (buttonLocator != null)
        {
            // Confirm the deletion
            _test.clickButton("OK", 0);
            _test.waitForElement(Locator.xpath("//td/img[@id='partstatus_" + index + "' and contains(@src, 'deleted')]]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }
    }
    
}
