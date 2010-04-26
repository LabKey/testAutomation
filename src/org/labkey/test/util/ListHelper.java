/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

import org.apache.commons.lang.StringUtils;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;

/**
 * User: jeckels
 * Date: Nov 21, 2007
 */
public class ListHelper
{
    public static void uploadData(BaseSeleniumWebTest test, String folderName, String listName, String listData)
    {
        test.clickLinkWithText(folderName);

        test.clickLinkWithText(listName);

        test.clickNavButton("Import Data");
        test.setFormElement("ff_data", listData);
        test.submit();
    }

    public static class LookupInfo
    {
        private String _folder;
        private String _schema;
        private String _table;

        public LookupInfo(String folder, String schema, String table)
        {
            _folder = folder;
            _schema = schema;
            _table = table;
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
        Equals("Equals"), NE("Does not Equal"), GT("Greater than"), GTE("Greater than or Equals"), LT("Less than"), LTE("Less than or Equals");
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
        Integer("Integer"), String("Text (String)"), DateTime("DateTime"), Boolean("Boolean"), Double("Number (Double)"), File("File"), AutoInteger("Auto-Increment Integer");

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

        public void setURL(String url)
        {
            _url = url;
        }

        public String getURL()
        {
            return _url;
        }
    }


    public static void createList(BaseSeleniumWebTest test, String folderName, String listName, ListHelper.ListColumnType listKeyType, String listKeyName, ListColumn... cols)
    {
        test.clickLinkWithText(folderName);
        test.waitForPageToLoad();
        if (!test.isLinkPresentWithText("Lists"))
        {
            test.addWebPart("Lists");
        }

        test.clickLinkWithText("manage lists");

        test.log("Add List");
        test.clickNavButton("Create New List");
        test.waitForElement(Locator.id("ff_name"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        test.setFormElement("ff_name", listName);
        test.selectOptionByText("ff_keyType", listKeyType.toString());
        test.setFormElement("ff_keyName", listKeyName);
        test.clickNavButton("Create List", 0);
        test.waitForElement(Locator.id("ff_description"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        test.waitForElement(Locator.id("ff_name0"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        test.log("Check that list was created correctly");
        test.assertFormElementEquals("ff_name", listName);
        test.assertFormElementEquals("ff_name0", listKeyName);

        test.log("Add columns");
//        test.clickLinkWithText("edit fields");

        // i==0 is the key column
        for (int i = 1; i <= cols.length; i++)
        {
            ListColumn col = cols[i-1];
            test.waitForElement(Locator.id("button_Add Field"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
            test.clickNavButton("Add Field", 0);
            test.setFormElement(Locator.id("ff_name" + i),  col.getName());
            test.setFormElement(Locator.id("ff_label" + i), col.getLabel());
            test.selectOptionByText("ff_type" + i, col.getType().toString());
            test.setFormElement(Locator.id("propertyDescription"), col.getDescription());
            if (col.getFormat() != null)
            {
                test.setFormElement("propertyFormat", col.getFormat());
            }

            if (col.isMvEnabled())
            {
                test.checkCheckbox("mvEnabled");
            }

            LookupInfo lookup = col.getLookup();
            if (lookup != null)
            {
                test.mouseClick(Locator.id("partdown_lookup" + i).toString());
                if (lookup.getFolder() != null)
                {
                    test.setFormElement("folder", lookup.getFolder());
                }
                test.setFormElement("schema", lookup.getSchema());
                test.setFormElement("table", lookup.getTable());
                test.clickNavButton("Close", 0);
            }

            FieldValidator validator = col.getValidator();
            if (validator != null)
            {
                if (validator instanceof RegExValidator)
                    test.clickNavButton("Add Regular Expression Validator", 0);
                else
                    test.clickNavButton("Add Range Validator", 0);
                test.setFormElement("name", validator.getName());
                test.setFormElement("description", validator.getDescription());
                test.setFormElement("errorMessage", validator.getMessage());

                if (validator instanceof RegExValidator)
                {
                    test.setFormElement("expression", ((RegExValidator)validator).getExpression());
                }
                else if (validator instanceof RangeValidator)
                {
                    test.setFormElement("firstRangeValue", ((RangeValidator)validator).getFirstRange());
                }
                test.clickNavButton("OK", 0);
            }

            if (null != col.getURL())
            {
                test.setFormElement("url", col.getURL());
            }
        }

        clickSave(test);

        test.log("Check that they were added");
        if (cols.length > 0)
        {
            test.waitForElement(Locator.navButton("Export Fields"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
            for (ListColumn col : cols)
            {
                test.assertTextPresent(col.getName());
                if (!StringUtils.isEmpty(col.getLabel()) && !col.getName().equals(col.getLabel()))
                    test.assertTextPresent(col.getLabel());
            }
        }
    }

    public static void createListFromFile(BaseSeleniumWebTest test, String folderName, String listName, File inputFile)
    {
        test.clickLinkWithText(folderName);
        test.waitForPageToLoad();
        if (!test.isLinkPresentWithText("Lists"))
        {
            test.addWebPart("Lists");
        }

        test.clickLinkWithText("manage lists");

        test.log("Add List");
        test.clickNavButton("Create New List");
        test.setFormElement("ff_name", listName);

        test.click(Locator.xpath("//span[@id='fileImport']/input[@type='checkbox']"));
        //test.clickCheckbox("fileImport");

        test.clickNavButton("Create List", 0);

        test.waitForElement(Locator.xpath("//input[@name='uploadFormElement']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        test.setFormElement("uploadFormElement", inputFile);

        test.waitForElement(Locator.xpath("//span[@id='button_Import']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        test.clickNavButton("Import");

        test.waitForPageToLoad();
    }

    public static void importListArchive(BaseSeleniumWebTest test, String folderName, File inputFile)
    {
        test.clickLinkWithText(folderName);
        test.waitForPageToLoad();
        if (!test.isLinkPresentWithText("Lists"))
        {
            test.addWebPart("Lists");
        }

        test.clickLinkWithText("manage lists");

        test.log("Import List Archive");
        test.clickNavButton("Import List Archive");
        test.waitForElement(Locator.xpath("//input[@name='listZip']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        test.setFormElement("listZip", inputFile);
        test.clickNavButton("Import List Archive");

        test.waitForPageToLoad();
    }



    public static void clickImportData(BaseSeleniumWebTest test)
    {
        test.waitAndClick(BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT, Locator.navButton("Import Data"), BaseSeleniumWebTest.WAIT_FOR_PAGE);
    }

    public static void clickEditDesign(BaseSeleniumWebTest test)
    {
        test.waitAndClick(BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT, Locator.navButton("Edit Design"), 0);
        test.waitForElement(Locator.navButton("Cancel"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        test.waitForElement(Locator.id("ff_description"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        test.waitForElement(Locator.navButton("Add Field"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public static void clickSave(BaseSeleniumWebTest test)
    {
        test.waitAndClick(BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT, Locator.navButton("Save"), 0);
        test.waitForElement(Locator.navButton("Edit Design"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        test.waitForElement(Locator.navButton("Done"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public static void clickDeleteList(BaseSeleniumWebTest test)
    {
        test.waitAndClick(BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT, Locator.navButton("Delete List"), BaseSeleniumWebTest.WAIT_FOR_PAGE);
    }
}
