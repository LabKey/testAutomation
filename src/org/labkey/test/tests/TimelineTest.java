/*
 * Copyright (c) 2008-2012 LabKey Corporation
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
package org.labkey.test.tests;

import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.ListHelper;

/**
 * User: brittp
 * Created: Mar 12, 2008 9:36:47 AM
 */
public class TimelineTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "TimelineTestProject";
    private static final String FOLDER_NAME = "timeline folder";
    private final static String LIST_NAME = "People";
    private final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.AutoInteger;
    private final static String LIST_KEY_NAME = "Key";

    private final static ListHelper.ListColumn[] LIST_COLUMNS = new ListHelper.ListColumn[]
    {
        new ListHelper.ListColumn("FirstName", "First Name", ListHelper.ListColumnType.String, "The first name"),
        new ListHelper.ListColumn("LastName", "Last Name", ListHelper.ListColumnType.String, "The last name"),
        new ListHelper.ListColumn("DOB", "DOB", ListHelper.ListColumnType.DateTime, "Date of Birth"),
        new ListHelper.ListColumn("DOD", "DOD", ListHelper.ListColumnType.DateTime, "Date of Death"),
    };

    private final static String[][] TEST_DATA =
    {
        { "1", "John", "Johnson", "1908-01-01", "2008-01-01" },
        { "2", "Bill", "Billson", "2006-01-01", ""},
        { "3", "Jane", "Janeson", "2007-09-01", "" }
    };

    private static final String WIKIPAGE_NAME = "TimelineTestPage";

    private static final String CLIENTAPI_HEADER =
        "<script type=\"text/javascript\">LABKEY.requiresClientAPI();</script>\n" +
        "<script src='/labkey/timeline.js'></script>\n" +
        "<script src='/labkey/similetimeline/bundle.js'></script>\n" +
        "<script src='/labkey/similetimeline/scripts/l10n/en/timeline.js'></script>\n" +
        "<script src='/labkey/similetimeline/scripts/l10n/en/labellers.js'></script>\n";

    private static final String TEST_DIV_NAME = "testDiv";


    private static final String SRC_PREFIX = CLIENTAPI_HEADER + "\n<script type=\"text/javascript\">\n" +
            "    Ext.namespace('demoNamespace'); //define namespace with some 'name'\n" +
            "    demoNamespace.myModule = function(){//creates a property 'myModule' of the namespace object\n" +
            "        return {\n" +
            "            init : function()\n" +
            "            {";

    private static final String SRC_SUFFIX = "            }\n" +
            "        }\n" +
            "    }();\n" +
            "    // Since the above code has already executed, we can access the init method immediately:\n" +
            "    Ext.onReady(demoNamespace.myModule.init, demoNamespace.myModule, true);\n" +
            "</script>\n" +
            "<div id=\"" + TEST_DIV_NAME + "\" style='height:400px'></div>";

    private static final String TIMELINE_TEST_SRC = "    var tl = LABKEY.Timeline.create({\n" +
            "        renderTo:'testDiv',\n" +
            "        start:'DOB',\n" +
            "        end:function(row){if (row.DOD) return row.DOD; else return new Date();},\n" +
            "        title:function(row) {return row.FirstName + ' ' + row.LastName;},\n" +
            "        description:function(row) {return 'Hi ' + row.FirstName + ' I am the description';},\n" +
            "        query:{schemaName:'lists', queryName:'People', viewName:null}});";

    protected String getFullSource(String testFragment)
    {
        return SRC_PREFIX + "\n" + testFragment + "\n" + SRC_SUFFIX;
    }

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/timeline";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    protected void doTestSteps() throws Exception
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[]{"Timeline"});

        clickLinkWithText(FOLDER_NAME);

        createWiki();

        createList();

        createWebPart();

        apiTest();
        //clear the test page so the crawler doesn't refetch a test and cause errors
        removeTestPage();
    }

    private void createWebPart()
    {
        addWebPart("Timeline");
        selectOptionByText("schemaName", "lists");
        waitFor(new Checker(){
            public boolean check()
            {
                return isTextPresent("FirstName");
            }
        }, "Could not find field", 10000);
        selectOptionByText("titleField", "FirstName");
        selectOptionByText("descriptionField", "LastName");
        submit();
        waitForElement(Locator.tagContainingText("div", "Jane"), 10000);
        assertTextNotPresent("Janeson");
        //Click on jane and make sure the bubble comes up
        selenium.mouseDown(Locator.tagContainingText("div", "Jane").toString());
        assertTextPresent("Janeson");
    }

    private void apiTest()
    {
        setSource(TIMELINE_TEST_SRC);
        clickLinkWithText(WIKIPAGE_NAME);
        waitForElement(Locator.tagContainingText("div", "Jane Janeson"), 10000);
        selenium.mouseDown(Locator.tagContainingText("div", "Jane Janeson").toString());
        assertTextPresent("Hi Jane I am the description");
    }

    private void removeTestPage()
    {
        if (enableLinkCheck())
        {
            clickLinkWithText(FOLDER_NAME);
            removeWebPart(WIKIPAGE_NAME);
        }
    }

    private void createList()
    {
        _listHelper.createList(FOLDER_NAME, LIST_NAME, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);

        StringBuilder data = new StringBuilder();
        data.append(LIST_KEY_NAME).append("\t");
        for (int i = 0; i < LIST_COLUMNS.length; i++)
        {
            data.append(LIST_COLUMNS[i].getName());
            data.append(i < LIST_COLUMNS.length - 1 ? "\t" : "\n");
        }
        for (String[] rowData : TEST_DATA)
        {
            for (int col = 0; col < rowData.length; col++)
            {
                data.append(rowData[col]);
                data.append(col < rowData.length - 1 ? "\t" : "\n");
            }
        }

        clickButton("Import Data");
        _listHelper.submitTsvData(data.toString());
        for (String[] rowData : TEST_DATA)
        {
            // check that all the data is in the grid (skipping the key column at index 0)
            for (int col = 1; col < rowData.length; col++)
            {
                assertTextPresent(rowData[col]);
            }
        }
        clickLinkWithText(FOLDER_NAME);
    }

    private String waitForDivPopulation()
    {
        return waitForDivPopulation(30);
    }

    private String waitForDivPopulation(int waitSeconds)
    {
        while (waitSeconds-- > 0)
        {
            log("Waiting for div to render...");
            String divHtml = selenium.getEval("this.browserbot.getCurrentWindow().document.getElementById('" + TEST_DIV_NAME + "').innerHTML;");
            if (divHtml.length() > 0)
                return divHtml;
            sleep(1000);
        }
        Assert.fail("Div failed to render.");
        return null;
    }

    private void createWiki()
    {
        addWebPart("Wiki");
        createNewWikiPage("HTML");
        setFormElement("name", WIKIPAGE_NAME);
        setFormElement("title", WIKIPAGE_NAME);
        setWikiBody("placeholder text");
        saveWikiPage();
    }


    private String setSource(String srcFragment)
    {
        if (!isTextPresent(WIKIPAGE_NAME))
            clickLinkWithText(FOLDER_NAME);
        clickWebpartMenuItem(WIKIPAGE_NAME, "Edit");

        String fullSource = getFullSource(srcFragment);
        log("Setting wiki page source:");
        log(fullSource);
        setWikiBody(fullSource);
        saveWikiPage();
        return waitForDivPopulation();
    }

}
