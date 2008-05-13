/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
import org.labkey.test.BaseSeleniumWebTest;

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
        private LookupInfo _lookup;

        public ListColumn(String name, String label, ListColumnType type, String description, String format, LookupInfo lookup)
        {
            _name = name;
            _label = label;
            _type = type;
            _description = description;
            _format = format;
            _lookup = lookup;
        }

        public ListColumn(String name, String label, ListColumnType type, String description, LookupInfo lookup)
        {
            this(name, label, type, description, null, lookup);
        }

        public ListColumn(String name, String label, ListColumnType type, String description, String format)
        {
            this(name, label, type, description, format, null);
        }

        public ListColumn(String name, String label, ListColumnType type, String description)
        {
            this(name, label, type, description, null, null);
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
        test.setFormElement("ff_name", listName);
        test.selectOptionByText("ff_keyType", listKeyType.toString());
        test.setFormElement("ff_keyName", listKeyName);
        test.clickNavButton("Create List");

        test.log("Check that list was created correctly");
        test.assertTextPresent(listName);
        test.assertTextPresent(listKeyType.toString());
        test.assertTextPresent(listKeyName);

        test.log("Add columns");
        test.clickLinkWithText("edit fields");
        for (int i = 0; i < cols.length; i++)
        {
            ListColumn col = cols[i];
            test.waitForElement(Locator.id("button_Add Field"), BaseSeleniumWebTest.WAIT_FOR_GWT);
            test.click(Locator.id("button_Add Field"));
            test.setFormElement(Locator.id("ff_name" + i),  col.getName());
            test.setFormElement(Locator.id("ff_label" + i), col.getLabel());
            test.selectOptionByText("ff_type" + i, col.getType().toString());
            test.setFormElement(Locator.id("propertyDescription"), col.getDescription());
            if (col.getFormat() != null)
            {
                test.setFormElement("propertyFormat", col.getFormat());
            }
            LookupInfo lookup = col.getLookup();
            if (lookup != null)
            {
                test.click(Locator.id("partdown_" + i));
                if (lookup.getFolder() != null)
                {
                    test.setFormElement("folder", lookup.getFolder());
                }
                test.setFormElement("schema", lookup.getSchema());
                test.setFormElement("table", lookup.getTable());
                test.click(Locator.id("button_Close"));
            }
        }
        test.click(Locator.id("button_Save"));
        test.waitForPageToLoad();

        test.log("Check that they were added");
        if (cols.length > 0)
        {
            test.waitForText(cols[0].getName(), BaseSeleniumWebTest.WAIT_FOR_GWT);
            for (ListColumn col : cols)
            {
                test.assertTextPresent(col.getName());
            }
        }
    }
}
