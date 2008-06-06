package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ListHelper;

/**
 * Copyright (c) 2008 LabKey Corporation
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * User: brittp
 * Created: Mar 12, 2008 9:36:47 AM
 */
public class ClientAPITest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "ClientAPITestProject";
    private static final String FOLDER_NAME = "api folder";
    private final static String LIST_NAME = "People";
    private final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.AutoInteger;
    private final static String LIST_KEY_NAME = "Key";
    protected static final String TEST_ASSAY = "TestAssay1";
    protected static final String TEST_ASSAY_DESC = "Description for assay 1";

    private final static ListHelper.ListColumn[] LIST_COLUMNS = new ListHelper.ListColumn[]
    {
        new ListHelper.ListColumn("FirstName", "First Name", ListHelper.ListColumnType.String, "The first name"),
        new ListHelper.ListColumn("LastName", "Last Name", ListHelper.ListColumnType.String, "The last name"),
        new ListHelper.ListColumn("Age", "Age", ListHelper.ListColumnType.Integer, "The age")
    };

    private final static String[][] TEST_DATA =
    {
        { "1", "John", "Johnson", "17" },
        { "2", "Bill", "Billson", "34" },
        { "3", "Jane", "Janeson", "42" }
    };

    private static final String WIKIPAGE_NAME = "ClientAPITestPage";

    private static final String CLIENTAPI_HEADER =
        "<script type=\"text/javascript\">LABKEY.requiresClientAPI();</script>\n";

    private static final String TEST_DIV_NAME = "testDiv";

    private static final String GRIDTEST_GRIDTITLE = "ClientAPITest Grid Title";

    private static final String GRIDTEST_SRC =
                "// create new grid over a list named 'People'\n" +
                "var gridView = new LABKEY.GridView({\n" +
                        "    schemaName : 'lists',\n" +
                        "    queryName : 'People',\n" +
                        "    renderTo : '" + TEST_DIV_NAME + "',\n" +
                        "    editable : true,\n" +
                        "    gridPanelConfig : {\n" +
                        "        title :'" + GRIDTEST_GRIDTITLE + "',\n" +
                        "        autoHeight : true,\n" +
                        "    }\n" +
                        "});\n" +
                "// place editable grid in 'grid-example':\n" +
                "gridView.render();\n";

    private static final String CHARTTEST_SRC = "var chartConfig = {\n" +
            "    queryName: 'People',\n" +
            "    schemaName: 'lists',\n" +
            "    chartType: LABKEY.Chart.XY,\n" +
            "    columnXName: 'Key',\n" +
            "    columnYName: ['Age'],\n" +
            "    renderTo: '" + TEST_DIV_NAME + "'\n" +
            "};\n" +
            "var chart = new LABKEY.Chart(chartConfig);\n" +
            "chart.render();";

    private static final String WEBPARTTEST_SRC =
            "var renderer = new LABKEY.WebPart({partName: 'query',\n" +
                    "renderTo: '" + TEST_DIV_NAME + "',\n"+
                    "partConfig: {\n" +
                    "        title: 'Webpart Title',\n" +
                    "        schemaName: 'lists',\n" +
                    "        queryName: 'People'\n" +
                    "    }});\n" +
                    "renderer.render();";

    private static final String ASSAYTEST_SRC =
            "function renderer(assayArray)\n" +
            "{\n" +
            "    var html = '';\n" +
            "    for (var defIndex = 0; defIndex  < assayArray.length; defIndex ++)\n" +
            "    {\n" +
            "\tvar definition = assayArray[defIndex ];\n" +
            "\thtml += '<b>' + definition.type + '</b>: ' + definition.name + '<br>';\n" +
            "        for (var domain in definition.domains)\n" +
            "        {\n" +
            "            html += '&nbsp;&nbsp;&nbsp;' + domain + '<br>';\n" +
            "            var properties = definition.domains[domain];\n" +
            "            for (var propertyIndex = 0; propertyIndex < properties.length; propertyIndex++)\n" +
            "            {\n" +
            "                var property = properties[propertyIndex];\n" +
            "                html += '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' +\n" +
            "                         property.name + ' - ' + property.typeName + '<br>';\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "    document.getElementById('" + TEST_DIV_NAME + "').innerHTML = html;\n" +
            "}\n" +
            "\n" +
            "function errorHandler(error)\n" +
            "{\n" +
            "    alert('An error occurred retrieving data.');\n" +
            "}\n" +
            "\n" +
            "LABKEY.Assay.getAll(renderer, errorHandler);";

    private static final String QUERYTEST_SRC =
            "var schemaName = 'lists';\n" +
            "var queryName = 'People';\n" +
            "\n" +
            "var testResults = [];\n" +
            "var testFunctions = [\n" +
            "    function()\n" +
            "    {\n" +
            "        LABKEY.Query.selectRows(schemaName, queryName, successHandler, failureHandler);\n" +
            "    },\n" +
            "    \n" +
            "\n" +
            "    function()\n" +
            "    {\n" +
            "        LABKEY.Query.selectRows(schemaName, queryName, successHandler, failureHandler, [ LABKEY.Filter.create('FirstName', 'Jonny') ]);\n" +
            "    },\n" +
            "\n" +
            "    function()\n" +
            "    {\n" +
            "        // get the result from the single-row select call:\n" +
            "        var prevRowset = testResults[1].rows;\n" +
            "        var rowCopy = {};\n" +
            "        for (var prop in prevRowset[0])\n" +
            "            rowCopy[prop] = prevRowset[0][prop];\n" +
            "        rowCopy.Age = 99;\n" +
            "        LABKEY.Query.updateRows(schemaName, queryName, [ rowCopy ], successHandler, failureHandler);        \n" +
            "    },\n" +
            "\n" +
            "    function()\n" +
            "    {\n" +
            "        // get the result from the single-row select call:\n" +
            "        var prevRowset = testResults[1].rows;\n" +
            "        LABKEY.Query.deleteRows(schemaName, queryName, prevRowset, successHandler, failureHandler);        \n" +
            "    },\n" +
            "\n" +
            "    function()\n" +
            "    {\n" +
            "        // get the result from the single-row select call:\n" +
            "        var prevRowset = testResults[1].rows;\n" +
            "        LABKEY.Query.insertRows(schemaName, queryName, prevRowset, successHandler, failureHandler);        \n" +
            "    },\n" +
            "\n" +
            "    function()\n" +
            "    {\n" +
            "        LABKEY.Query.selectRows(schemaName + '_badname', queryName, successHandler, failureHandler);\n" +
            "    },\n" +
            "\n" +
            "    // last function sets the contents of the results div.\n" +
            "    function()\n" +
            "    {\n" +
            "        var html = '';\n" +
            "        if (testResults[0].rowCount == 3)\n" +
            "            html += 'SUCCESS: Select 1 returned 3 rows<br>';\n" +
            "        else\n" +
            "            html += 'FAILURE: Select 1 returned ' + testResults[0].rowCount + ' rows, expected 3.  Error value = ' + testResults[0].exception + '<br>';\n" +
            "\n" +
            "        if (testResults[1].rowCount == 1)\n" +
            "            html += 'SUCCESS: Select 2 returned 1 rows<br>';\n" +
            "        else\n" +
            "            html += 'FAILURE: Select 2 returned ' + testResults[1].rowCount + ' rows, expected 1.  Error value = ' + testResults[1].exception + '<br>';\n" +
            "\n" +
            "        if (testResults[2].rowsAffected == 1)\n" +
            "            html += 'SUCCESS: Update affected 1 rows<br>';\n" +
            "        else\n" +
            "            html += 'FAILURE: Update affected ' + testResults[2].rowCount + ' rows, expected 1.  Error value = ' + testResults[2].exception + '<br>';\n" +
            "\n" +
            "        if (testResults[3].rowsAffected == 1)\n" +
            "            html += 'SUCCESS: Delete affected 1 rows<br>';\n" +
            "        else\n" +
            "            html += 'FAILURE: Delete affected ' + testResults[3].rowCount + ' rows, expected 1.  Error value = ' + testResults[3].exception + '<br>';\n" +
            "\n" +
            "        if (testResults[4].rowsAffected == 1)\n" +
            "            html += 'SUCCESS: Insert created 1 rows<br>';\n" +
            "        else\n" +
            "            html += 'FAILURE: Insert created ' + testResults[4].rowCount + ' rows, expected 1.  Error value = ' + testResults[4].exception + '<br>';\n" +
            "\n" +
            "        if (testResults[5].exception)\n" +
            "            html += 'SUCCESS: bad query generated exception: ' + testResults[5].exception + '<br>';\n" +
            "        else\n" +
            "            html += 'FAILURE: bad query did not generate expected exception.<br>';\n" +
            "\n" +
            "        document.getElementById('testDiv').innerHTML = html;        \n" +
            "    }\n" +
            "];\n" +
            "\n" +
            "function executeNext()\n" +
            "{\n" +
            "    var currentFn = testFunctions[testResults.length];\n" +
            "    currentFn();\n" +
            "}\n" +
            "\n" +
            "function failureHandler(responseObj)\n" +
            "{\t\t\n" +
            "    testResults[testResults.length] = responseObj;\n" +
            "    executeNext();\n" +
            "}\n" +
            "\n" +
            "function successHandler(responseObj)\n" +
            "{\n" +
            "    testResults[testResults.length] = responseObj;\n" +
            "    executeNext();\n" +
            "}\n" +
            "\n" +
            "executeNext();";

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
            "<div id=\"" + TEST_DIV_NAME + "\" />";

    protected String getFullSource(String testFragment)
    {
        return SRC_PREFIX + "\n" + testFragment + "\n" + SRC_SUFFIX;
    }

    public String getAssociatedModuleDirectory()
    {
        return "none";
    }

    protected void doCleanup() throws Exception
    {
        try {deleteFolder(PROJECT_NAME, FOLDER_NAME); } catch (Throwable t) {}
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps() throws Exception
    {
        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, FOLDER_NAME, null);

        clickLinkWithText(FOLDER_NAME);
        addWebPart("Wiki");
        createNewWikiPage("HTML");
        setFormElement("name", WIKIPAGE_NAME);
        setFormElement("title", WIKIPAGE_NAME);
        setWikiBody("placeholder text");
        saveWikiPage();

        createList();

        gridTest();

        chartTest();

        webpartTest();

        assayTest();

        queryTest();

        //clear the test page so the crawler doesn't refetch a test and cause errors
        clearTestPage();
    }

    private void clearTestPage()
    {
        if (!isTextPresent(WIKIPAGE_NAME))
            clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("edit");
        setWikiBody("<p>Test Complete.</p>");
        saveWikiPage();
    }

    private void createList()
    {
        ListHelper.createList(this, FOLDER_NAME, LIST_NAME, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);

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

        clickLinkWithText("import data");
        setFormElement("ff_data", data.toString());
        submit();
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
        fail("Div failed to render.");
        return null;
    }

    private void webpartTest()
    {
        setSource(WEBPARTTEST_SRC);

        waitForDivPopulation();
        assertTextPresent("Webpart Title");
        assertLinkPresentWithText("Customize View");
        for (ListHelper.ListColumn column : LIST_COLUMNS)
            assertTextPresent(column.getLabel());
    }

    private void chartTest()
    {
        String chartHtml = setSource(CHARTTEST_SRC);
        if (chartHtml.indexOf("<img") < 0)
            fail("Test div does not contain an image:\n" + chartHtml);
    }

    private void gridTest()
    {
        setSource(GRIDTEST_SRC);

        assertTextPresent(GRIDTEST_GRIDTITLE);

        for (ListHelper.ListColumn col : LIST_COLUMNS)
            assertTextPresent(col.getLabel());

        for (String[] rowData : TEST_DATA)
        {
            // check that all the data is in the grid (skipping the key column at index 0)
            for (int col = 1; col < rowData.length; col++)
                assertTextPresent(rowData[col]);
        }

        selenium.click("add-record-button");
        String prevActiveCellId;
//        sleep(500);
        // enter a new first name
        String activeCellId = selenium.getEval("this.browserbot.getCurrentWindow().document.ActiveExtGridViewCellId;");
        selenium.type(Locator.id(activeCellId).toString(), "Fred");
        selenium.keyPress(Locator.id(activeCellId).toString(), "\t");
        sleep(50);
        // enter a new last name
        prevActiveCellId = activeCellId;
        activeCellId = selenium.getEval("this.browserbot.getCurrentWindow().document.ActiveExtGridViewCellId;");
        if (prevActiveCellId.equals(activeCellId))
            fail("Failed to advance to next edit field");
        selenium.type(Locator.id(activeCellId).toString(), "Fredson");
        selenium.keyPress(Locator.id(activeCellId).toString(), "\t");
        sleep(50);
        // enter a new age
        prevActiveCellId = activeCellId;
        activeCellId = selenium.getEval("this.browserbot.getCurrentWindow().document.ActiveExtGridViewCellId;");
        if (prevActiveCellId.equals(activeCellId))
            fail("Failed to advance to next edit field");
        selenium.type(Locator.id(activeCellId).toString(), "51");
        selenium.keyPress(Locator.id(activeCellId).toString(), "\t");
        sleep(50);
        // on the next row, change 'John' to 'Jonny'
        prevActiveCellId = activeCellId;
        activeCellId = selenium.getEval("this.browserbot.getCurrentWindow().document.ActiveExtGridViewCellId;");
        if (prevActiveCellId.equals(activeCellId))
            fail("Failed to advance to next edit field");
        selenium.type(Locator.id(activeCellId).toString(), "Jonny");
        selenium.keyPress(Locator.id(activeCellId).toString(), "\t");
        sleep(50);
        prevActiveCellId = activeCellId;
        activeCellId = selenium.getEval("this.browserbot.getCurrentWindow().document.ActiveExtGridViewCellId;");
        if (prevActiveCellId.equals(activeCellId))
            fail("Failed to advance to next edit field");
        selenium.type(Locator.id(activeCellId).toString(), "Jonnyson");
        selenium.keyPress(Locator.id(activeCellId).toString(), "\t");
        sleep(50);
        prevActiveCellId = activeCellId;
        activeCellId = selenium.getEval("this.browserbot.getCurrentWindow().document.ActiveExtGridViewCellId;");
        if (prevActiveCellId.equals(activeCellId))
            fail("Failed to advance to next edit field");
        selenium.keyPress(Locator.id(activeCellId).toString(), "\t");
        sleep(50);

        // delete the row below Jonny (which should contain Bill)
        selenium.click("delete-records-button");
        selenium.getConfirmation();
        sleep(50);
        selenium.click("refresh-button");

        int limit = 30;
        while (isTextPresent("Bill") && limit-- > 0)
            sleep(1000);

        assertTextPresent("Fredson", 1);
        assertTextPresent("Jonny", 2);
        assertTextNotPresent("John");
        assertTextNotPresent("Bill");
    }

    private String getPropertyXPath(String propertyHeading)
    {
        return "//td[contains(text(), '" + propertyHeading + "')]/../..";
    }

    private void assayTest()
    {
        addWebPart("Assay List");

        //copied from old test
        clickLinkWithText("Manage Assays");
        clickNavButton("New Assay Design");
        setFormElement("providerName", "General");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@type='text']"), WAIT_FOR_GWT);

        selenium.type("//input[@type='text']", TEST_ASSAY);
        selenium.type("//textarea", TEST_ASSAY_DESC);
        selenium.mouseOver(getPropertyXPath("Run Fields") + "//img[contains(@src, 'Add+Field.button')]");
        selenium.mouseDown(getPropertyXPath("Run Fields") + "//img[contains(@src, 'Add+Field.button')]");
        selenium.mouseUp(getPropertyXPath("Run Fields") + "//img[contains(@src, 'Add+Field.button')]");
        selenium.type(getPropertyXPath("Run Fields") + "//input[@id='ff_name0']", "RunDate");
        selenium.type(getPropertyXPath("Run Fields") + "//input[@id='ff_label0']", "Run Date");
        selenium.select(getPropertyXPath("Run Fields") + "//select[@id='ff_type0']", "DateTime");

        sleep(1000);
        clickNavButton("    Save    ", 0);
        waitForText("Save successful.", 20000);
        
        setSource(ASSAYTEST_SRC);

        assertTextPresent(TEST_ASSAY);
        assertTextPresent(TEST_ASSAY + " Run Fields");
        assertTextPresent("RunDate - DateTime");
        assertTextPresent(TEST_ASSAY + " Upload Set Fields");
        assertTextPresent("TargetStudy - String");
        assertTextPresent(TEST_ASSAY + " Data Fields");
        assertTextPresent("VisitID - Double");
    }

    private String setSource(String srcFragment)
    {
        if (!isTextPresent(WIKIPAGE_NAME))
            clickLinkWithText(FOLDER_NAME);
        clickLinkWithText("edit");

        String fullSource = getFullSource(srcFragment);
        log("Setting wiki page source:");
        log(fullSource);
        setWikiBody(fullSource);
        saveWikiPage();
        return waitForDivPopulation();
    }

    private void queryTest()
    {
        setSource(QUERYTEST_SRC);
        assertTextPresent("SUCCESS: Select 1 returned 3 rows");
        assertTextPresent("SUCCESS: Select 2 returned 1 rows");
        assertTextPresent("SUCCESS: Update affected 1 rows");
        assertTextPresent("SUCCESS: Delete affected 1 rows");
        assertTextPresent("SUCCESS: Insert created 1 rows");
        assertTextPresent("SUCCESS: bad query generated exception: Could not find schema: lists_badname");
    }
}
