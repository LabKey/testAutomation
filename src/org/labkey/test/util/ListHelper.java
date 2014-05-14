/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class ListHelper extends AbstractHelper
{
    BaseWebDriverTest _test;

    public ListHelper(BaseWebDriverTest test)
    {
        super(test);
        _test = test;
    }

    public void uploadData(String folderName, String listName, String listData)
    {
        _test.clickButton("Import Data");
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
        catch (NoSuchElementException ex)
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

    public void importListArchive(String folderName, File inputFile)
    {
        assertTrue("Unable to locate input file: " + inputFile, inputFile.exists());

        _test.clickFolder(folderName);
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
    }



    public void clickImportData()
    {
        _test.waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.navButton("Import Data"), BaseWebDriverTest.WAIT_FOR_PAGE);
        _test.waitForElement(Locator.id("tsv3"));
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
        setColumnLabel(null,index,label);
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

    @LogMethod
    private void setColumnType(@Nullable String prefix, @Nullable LookupInfo lookup, @Nullable ListColumnType colType, int i)
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

            selectLookupTableComboItem(lookup.getTable());
        }

        _test.clickButton("Apply", 0);

        _test._extHelper.waitForExtDialogToDisappear("Choose Field Type");
    }

    private void selectLookupComboItem(String fieldName, String value)
    {
        _test.click(Locator.css("input[name="+fieldName+"] + div.x-form-trigger"));
        try
        {
            _test.waitAndClick(500, Locator.tag("div").withClass("x-combo-list-item").withText(value), 0);
        }
        catch (NoSuchElementException retry) // Workaround: sometimes fails on slower machines
        {
            _test.fireEvent(Locator.css("input[name=" + fieldName + "]"), BaseWebDriverTest.SeleniumEvent.blur);
            _test.click(Locator.css("input[name=" + fieldName + "] + div.x-form-trigger"));
            _test.waitAndClick(1000, Locator.tag("div").withClass("x-combo-list-item").withText(value), 0);
        }
        _test.waitForElement(Locator.xpath("//div").withClass("test-marker-" + value).append("/input[@name='" + fieldName + "']"));
    }

    private void selectLookupTableComboItem(String table)
    {
        String fieldName = "table";
        _test.click(Locator.css("input[name="+fieldName+"] + div.x-form-trigger"));
        try
        {
            _test.waitAndClick(Locator.tag("div").withClass("x-combo-list-item").withPredicate("starts-with(normalize-space(), " + Locator.xq(table + " (")  + ")"));
        }
        catch (NoSuchElementException retry) // Workaround: sometimes fails on slower machines
        {
            _test.fireEvent(Locator.css("input[name=" + fieldName + "]"), BaseWebDriverTest.SeleniumEvent.blur);
            _test.click(Locator.css("input[name=" + fieldName + "] + div.x-form-trigger"));
            _test.waitAndClick(Locator.tag("div").withClass("x-combo-list-item").withPredicate("starts-with(normalize-space(), " + Locator.xq(table + " (")  + ")"));
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

    public void addField(String areaTitle, int index, String name, String label, ListColumnType type)
    {
        addField(areaTitle, name, label, type);
    }

    public void addField(String areaTitle, String name, String label, ListColumnType type)
    {
        String prefix = _test.getPropertyXPath(areaTitle);
        Locator addField = Locator.xpath(prefix + "//span" + Locator.navButton("Add Field").getPath());
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

    public void addLookupField(String areaTitle, int index, String name, String label, ListHelper.LookupInfo type)
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
        String prefix = _test.getPropertyXPathContains(areaTitle);
        _test.waitAndClick(Locator.xpath(prefix + "//div[@id='partdelete_" + index + "']"));

        // If domain hasn't been saved yet, the 'OK' prompt will not appear.
        Locator.XPathLocator buttonLocator = _test.getButtonLocator("OK");
        // TODO: Be smarter about this.  Might miss the OK that should be there.
        if (buttonLocator != null)
        {
            // Confirm the deletion
            _test.clickButton("OK", 0);
            _test.waitForElement(Locator.xpath("//img[@id='partstatus_" + index + "'][contains(@src, 'deleted')]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }
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
        Double("Number (Double)"), File("File"), AutoInteger("Auto-Increment Integer"), Flag("Flag (String)");

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
}
