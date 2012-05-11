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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PasswordUtil;

/**
 * User: brittp
 * Created: Mar 12, 2008 9:36:47 AM
 */
public class ClientAPITest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "ClientAPITestProject";
    private static final String OTHER_PROJECT = "OtherClientAPITestProject"; // for cross-project query test
    private static final String FOLDER_NAME = "api folder";
    private static final String SUBFOLDER_NAME = "subfolder";
    private final static String LIST_NAME = "People";
    private final static String SUBFOLDER_LIST = "subfolderList"; // for cross-folder query test
    private static final String OTHER_PROJECT_LIST = "otherProjectList"; // for cross-project query test
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
    static
    {
        LIST_COLUMNS[0].setRequired(true);
        LIST_COLUMNS[1].setRequired(true);
    }

    private final static String[][] TEST_DATA =
    {
        { "1", "Bill", "Billson", "34" },
        { "2", "Jane", "Janeson", "42" },
        { "3", "John", "Johnson", "17" },
        { "4", "Mandy", "Mandyson", "32" },
        { "5", "Norbert", "Norbertson", "28" },
        { "6", "Penny", "Pennyson", "38" },
        { "7", "Yak", "Yakson", "88" },
    };

    private static final String WIKIPAGE_NAME = "ClientAPITestPage";

    private static final String CLIENTAPI_HEADER =
        "<script type=\"text/javascript\">LABKEY.requiresClientAPI();</script>\n";

    private static final String TEST_DIV_NAME = "testDiv";

    private static final String GRIDTEST_GRIDTITLE = "ClientAPITest Grid Title";

    private static final int PAGE_SIZE = 4;

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
            "<div id=\"" + TEST_DIV_NAME + "\"></div>";

    private static final String EMAIL_SRC_TEMPLATE =
            "function errorHandler(errorInfo, options, responseObj)\n" +
            "{\n" +
            "   document.getElementById('" + TEST_DIV_NAME + "').innerHTML = 'failure';\n" +
            "}\n" +
            "\n" +
            "function onSuccess(result)\n" +
            "{\n" +
            "   document.getElementById('" + TEST_DIV_NAME + "').innerHTML = 'success';\n" +
            "}\n" +
            "LABKEY.Message.sendMessage({\n" +
            "   msgFrom: '%s',\n" +
            "   msgSubject: '%s',\n" +
            "   msgRecipients: [%s],\n" +
            "   msgContent: [%s],\n" +
            "   allowUnregisteredUser: '%s',\n" +
            "   successCallback: onSuccess,\n" +
            "   errorCallback: errorHandler,\n" +
            "});";

    protected String getFullSource(String testFragment)
    {
        return SRC_PREFIX + "\n" + testFragment + "\n" + SRC_SUFFIX;
    }

    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doCleanup() throws Exception
    {
        for (String user : EMAIL_RECIPIENTS)
            deleteUser(user);

        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
        try {deleteProject(OTHER_PROJECT); } catch (Throwable t) {}
    }

    protected void doTestSteps() throws Exception
    {
        createProject(OTHER_PROJECT);
        createProject(PROJECT_NAME);

        enableEmailRecorder();

        createSubfolder(PROJECT_NAME, FOLDER_NAME, null);

        createSubfolder(PROJECT_NAME, FOLDER_NAME, SUBFOLDER_NAME, "None", null); // for cross-folder query

        clickLinkWithText(FOLDER_NAME);

        createWiki();

        lineChartTest();

        scatterChartTest();

        createLists();

        gridTest();

        chartTest();

        webpartTest();

        clearTestPage("WebPart Test Complete.");

        assayTest();

        queryTest();

        domainTest();

        emailApiTest();

        extIntegrationTest();

        webdavAPITest();

        //clear the test page so the crawler doesn't refetch a test and cause errors
        clearTestPage("Test Complete.");
    }

    private void clearTestPage(String message)
    {
        if (!isTextPresent(WIKIPAGE_NAME))
            clickLinkWithText(FOLDER_NAME);
        clickWebpartMenuItem(WIKIPAGE_NAME, "Edit");
        setWikiBody("<p>" + message + "</p>");
        saveWikiPage();
    }

    private void createLists()
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

        ListHelper.clickImportData(this);
        setFormElement("text", data.toString());
        ListHelper.submitImportTsv_success(this);
        for (String[] rowData : TEST_DATA)
        {
            // check that all the data is in the grid (skipping the key column at index 0)
            for (int col = 1; col < rowData.length; col++)
            {
                assertTextPresent(rowData[col]);
            }
        }

        // Create lists for cross-folder query test.
        ListHelper.createList(this, SUBFOLDER_NAME, SUBFOLDER_LIST, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);
        ListHelper.clickImportData(this);
        setFormElement("text", data.toString());
        ListHelper.submitImportTsv_success(this);

        // Create lists for cross-folder query test.
        ListHelper.createList(this, OTHER_PROJECT, OTHER_PROJECT_LIST, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);
        ListHelper.clickImportData(this);
        setFormElement("text", data.toString());
        ListHelper.submitImportTsv_success(this);

        clickLinkWithText(PROJECT_NAME);
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
            log("Waiting for " + TEST_DIV_NAME + " div to render...");
            String divHtml = selenium.getEval("this.browserbot.getCurrentWindow().document.getElementById('" + TEST_DIV_NAME + "').innerHTML;");
            if (divHtml.length() > 0)
                return divHtml;
            sleep(1000);
        }
        fail("Div failed to render.");
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

    private void webpartTest()
    {
        setSourceFromFile("webPartTest.js");

        waitForDivPopulation();
        assertTextPresent("Webpart Title");
        for (ListHelper.ListColumn column : LIST_COLUMNS)
            assertTextPresent(column.getLabel());
    }

    private void chartTest()
    {
        String chartHtml = setSourceFromFile("chartTest.js");
        if (chartHtml.indexOf("<img") < 0 && chartHtml.indexOf("<IMG") < 0)
            fail("Test div does not contain an image:\n" + chartHtml);
    }

    private void lineChartTest() throws Exception
    {
        setSourceFromFile("lineChartTest.js");

        //Some things we know about test 0. After this we loop through some others and just test to see if they convert
        assertTextPresent("Test Chart");
        assertTextPresent("Y Axis");
        assertTextPresent("X Axis");
        assertTextPresent("Series1");
        assertTextPresent("Series2");
        checkSVGConversion();

        String testCountStr = getFormElement(Locator.id("configCount"));
        int testCount = Integer.parseInt(testCountStr);
        for (int currentTest = 1; currentTest < testCount; currentTest++)
        {
            setFormElement("config", "" + currentTest);
            clickAndWait(Locator.input("submit"));
            waitForDivPopulation();
            checkSVGConversion();
        }
    }

    private void scatterChartTest() throws Exception
    {
        setFormElement("config", "");
        clickAndWait(Locator.input("submit"));
        setSourceFromFile("scatterChartTest.js");

        //Some things we know about test 0. After this we loop through some others and just test to see if they convert
        assertTextPresent("Test Scatter Chart");
        assertTextPresent("Left Y Axis");
        assertTextPresent("Bottom Axis");
        assertTextPresent("Series1");
        assertTextPresent("Series2");

        String testCountStr = getFormElement(Locator.id("configCount"));
        int testCount = Integer.parseInt(testCountStr);
        for (int currentTest = 1; currentTest < testCount; currentTest++)
        {
            setFormElement("config", "" + currentTest);
            clickAndWait(Locator.input("submit"));
            waitForDivPopulation();
            checkSVGConversion();
        }
    }

    private void checkSVGConversion() throws Exception
    {
        //The server side svg converter is fairly strict and will fail with bad inputs
        clickButton("Get SVG", 0);
        String svgText = getFormElement(Locator.id("svgtext"));

        String url = WebTestHelper.getBaseURL() + "/visualization/" + PROJECT_NAME + "/" + EscapeUtil.encode(FOLDER_NAME)+ "/exportPDF.view";
        HttpClient httpClient = WebTestHelper.getHttpClient(url);
        PostMethod method = null;

        try
        {
            method = new PostMethod(url);
            method.addParameter("svg", svgText);
            int status = httpClient.executeMethod(method);
            assertTrue("SVG Downloaded", status == HttpStatus.SC_OK);
            assertTrue(method.getResponseHeader("Content-Disposition").getValue().startsWith("attachment;"));
            assertTrue(method.getResponseHeader("Content-Type").getValue().startsWith("application/pdf"));
            method.getResponseBodyAsString();
        }
        finally
        {
            if (null != method)
                method.releaseConnection();
        }
    }

    private void gridTest()
    {
        setSourceFromFile("gridTest.js");

        assertTextPresent(GRIDTEST_GRIDTITLE);

        for (ListHelper.ListColumn col : LIST_COLUMNS)
            assertTextPresent(col.getLabel());

        for (int row = 0; row < PAGE_SIZE; ++row)
        {
            // check that the first PAGE_SIZE rows of the data is in the grid (skipping the key column at index 0)
            for (int col = 1; col < TEST_DATA[row].length; col++)
                assertTextPresent(TEST_DATA[row][col]);
        }

        assertTextPresent("Displaying 1 - " + PAGE_SIZE);

        selenium.click("add-record-button");
        String prevActiveCellId;
//        sleep(500);
        // enter a new first name
        sleep(50);
        String activeCellId = getActiveEditorId();
        selenium.type(Locator.id(activeCellId).toString(), "Abe");
        selenium.keyPress(Locator.id(activeCellId).toString(), "\t");
        selenium.keyDown(Locator.id(activeCellId).toString(), "\t");
        selenium.keyUp(Locator.id(activeCellId).toString(), "\t");
        sleep(50);
        // enter a new last name
        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        if (prevActiveCellId.equals(activeCellId))
            fail("Failed to advance to next edit field");
        selenium.type(Locator.id(activeCellId).toString(), "Abeson");
        selenium.keyPress(Locator.id(activeCellId).toString(), "\t");
        selenium.keyDown(Locator.id(activeCellId).toString(), "\t");
        selenium.keyUp(Locator.id(activeCellId).toString(), "\t");
        sleep(50);
        // enter a new age
        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        if (prevActiveCellId.equals(activeCellId))
            fail("Failed to advance to next edit field");
        selenium.type(Locator.id(activeCellId).toString(), "51");
        selenium.keyPress(Locator.id(activeCellId).toString(), "\t");
        selenium.keyDown(Locator.id(activeCellId).toString(), "\t");
        selenium.keyUp(Locator.id(activeCellId).toString(), "\t");

        waitUntilGridUpdateComplete();

        // on the next row, change 'Bill' to 'Billy'
        selenium.doubleClick("//div[contains(@class,'x-grid3-row-selected')]//div[contains(@class,'x-grid3-col-1')]");
        sleep(500);
        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        if (prevActiveCellId.equals(activeCellId))
            fail("Failed to advance to next edit field");
        selenium.type(Locator.id(activeCellId).toString(), "Billy");
        selenium.keyPress(Locator.id(activeCellId).toString(), "\t");
        selenium.keyDown(Locator.id(activeCellId).toString(), "\t");
        selenium.keyUp(Locator.id(activeCellId).toString(), "\t");
        sleep(50);
        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        if (prevActiveCellId.equals(activeCellId))
            fail("Failed to advance to next edit field");
        selenium.type(Locator.id(activeCellId).toString(), "Billyson");
        selenium.keyPress(Locator.id(activeCellId).toString(), "\t");
        selenium.keyDown(Locator.id(activeCellId).toString(), "\t");
        selenium.keyUp(Locator.id(activeCellId).toString(), "\t");
        sleep(50);
        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        if (prevActiveCellId.equals(activeCellId))
            fail("Failed to advance to next edit field");
        selenium.keyPress(Locator.id(activeCellId).toString(), "\t");
        selenium.keyDown(Locator.id(activeCellId).toString(), "\t");
        selenium.keyUp(Locator.id(activeCellId).toString(), "\t");
        sleep(500);

        // delete the row below Billy (which should contain Jane)
        selenium.click("delete-records-button");
        sleep(50);
        selenium.click("//div[contains(@class, 'x-window-dlg')]//button[text()='Delete']");
        sleep(50);
        //selenium.click("refresh-button");

        int limit = 30;
        while (isTextPresent("Jane") && limit-- > 0)
            sleep(1000);

        assertTextPresent("Abeson", 1);
        assertTextPresent("Billy", 2);
        assertTextNotPresent("Billson");
        assertTextNotPresent("Jane");

        //test paging
        selenium.click("//button[contains(@class, 'x-tbar-page-next')]");

        limit = 30;
        while (!isTextPresent("Norbert") && limit-- > 0)
            sleep(1000);

        assertTextPresent("Norbert");
        assertTextPresent("Penny");
        assertTextPresent("Yak");

        selenium.click("//button[contains(@class, 'x-tbar-page-prev')]");
        limit = 30;
        while (!isTextPresent("Abe") && limit-- > 0)
            sleep(1000);

        assertTextPresent("Abeson");
        assertTextPresent("Billyson");
        assertTextPresent("Johnson");

        //test sorting
        selenium.click("//div[contains(@class, 'x-grid3-hd-2')]");
        limit = 30;
        while (!isTextPresent("Yak") && limit-- > 0)
            sleep(1000);

        assertTextBefore("Yakson", "Pennyson");
        assertTextBefore("Pennyson", "Norbertson");
    }

    private String getActiveEditorId()
    {
        int limit = 3;
        String activeEditor = selenium.getEval("this.browserbot.getCurrentWindow().gridView.activeEditor;");
        while(null == activeEditor && limit-- > 0)
        {
            sleep(50);
            activeEditor = selenium.getEval("this.browserbot.getCurrentWindow().gridView.activeEditor;");
        }
        if(null == activeEditor)
            fail("Could not get the id of the active editor in the grid!");

        return selenium.getEval("this.browserbot.getCurrentWindow().gridView.activeEditor.field.id;");
    }

    private void waitUntilGridUpdateComplete()
    {
        int tries = 20;
        String numDirty = "";
        while(tries > 0 && "0".compareTo(numDirty) != 0)
        {
            numDirty = selenium.getEval("this.browserbot.getCurrentWindow().gridView.getStore().getModifiedRecords().length;");
            log("getModifiedRecords().length returned " + numDirty);
            sleep(500);
            --tries;
        }
        if(tries == 0)
            fail("Insert or update via the Ext grid did not complete!");

    }

    private void assayTest()
    {
        addWebPart("Assay List");

        //copied from old test
        clickNavButton("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "General");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        selenium.type("//input[@id='AssayDesignerName']", TEST_ASSAY);
        selenium.type("//textarea[@id='AssayDesignerDescription']", TEST_ASSAY_DESC);

        selenium.click(getPropertyXPath("Run Fields") + Locator.navButton("Add Field").getPath());
        ListHelper.setColumnName(this, getPropertyXPath("Run Fields"), 0, "RunDate");
        ListHelper.setColumnLabel(this, getPropertyXPath("Run Fields"), 0, "Run Date");
        ListHelper.setColumnType(this, getPropertyXPath("Run Fields"), 0, ListHelper.ListColumnType.DateTime);

        sleep(1000);
        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);
        
        setSourceFromFile("assayTest.js");

        assertTextPresent(TEST_ASSAY);
        assertTextPresent(TEST_ASSAY + " Run Fields");
        assertTextPresent("RunDate - DateTime");
        assertTextPresent(TEST_ASSAY + " Batch Fields");
        assertTextPresent("TargetStudy - String");
        assertTextPresent(TEST_ASSAY + " Data Fields");
        assertTextPresent("VisitID - Double");
    }

    private void domainTest()
    {
        addWebPart("Study Overview");

        clickNavButton("Create Study");
        // next page
        clickNavButton("Create Study");
        clickLinkWithText("Edit Definition");
        waitForText("No fields have been defined.", 10000);

        clickNavButton("Add Field", 0);

        ListHelper.setColumnName(this, 0, "species");
        ListHelper.setColumnLabel(this, 0, "Species");
        
        clickNavButton("Add Field", 0);

        ListHelper.setColumnName(this, 1, "color");
        ListHelper.setColumnLabel(this, 1, "Color");

        sleep(1000);
        clickNavButton("Save", 10000);

        setSourceFromFile("domainTest.js");
        waitForText("Finished DomainTests.", 30000);

        assertElementContains(Locator.id(TEST_DIV_NAME), "Updated StudyProperties domain");
        assertElementContains(Locator.id(TEST_DIV_NAME), "Did not find the");
        assertElementContains(Locator.id(TEST_DIV_NAME), "Successfully updated the description");
    }


    /**
     * Given a file name sets the page contents to a *wrapped* version of file in server/test/data/api
     * Wrapped version puts everything inside a function and inserts a div id="testDiv" for output to be placed in
     * @param fileName file will be found in server/test/data/api
     * @return
     */
    private String setSourceFromFile(String fileName)
    {
        return setSourceFromFile(fileName, false);
    }

    private String setSourceFromFile(String fileName, boolean excludeTags)
    {
        return setSource(getFileContents("server/test/data/api/" + fileName ), excludeTags);
    }

    private String setSource(String srcFragment)
    {
        return setSource(srcFragment, false);
    }

    private String setSource(String srcFragment, boolean excludeTags)
    {
        if (!isTextPresent(WIKIPAGE_NAME))
            clickLinkWithText(FOLDER_NAME);
        clickWebpartMenuItem(WIKIPAGE_NAME, "Edit");

        String fullSource = srcFragment;
        if (!excludeTags)
            fullSource = getFullSource(srcFragment);
        log("Setting wiki page source:");
        log(fullSource);
        setWikiBody(fullSource);
        saveWikiPage();
        return waitForDivPopulation();
    }

    private void queryTest()
    {
        setSourceFromFile("queryTest.js");
        Locator loc = Locator.id(TEST_DIV_NAME);
        assertElementContains(loc, "SUCCESS: Select 1 returned 7 rows");
        assertElementContains(loc, "SUCCESS: Select 2 returned 1 rows");
        assertElementContains(loc, "SUCCESS: Bad update generated exception: The field 'LastName' is required.");
        assertElementContains(loc, "SUCCESS: Update affected 1 rows");
        assertElementContains(loc, "SUCCESS: Delete affected 1 rows");
        assertElementContains(loc, "SUCCESS: Insert created 1 rows");
        assertElementContains(loc, "SUCCESS: Bad insert generated exception: The field 'LastName' is required.");
        assertElementContains(loc, "SUCCESS: Bad query generated exception: Failed to convert property value of type");
        assertElementContains(loc, "SUCCESS: executeSql returned 7 rows");
        assertElementContains(loc, "SUCCESS: cross-folder executeSql succeeded");
        assertElementContains(loc, "SUCCESS: cross-project executeSql succeeded");
        clearTestPage("Query portion of test page complete");
    }

    private static final String EMAIL_SUBJECT = "Testing the email API";
    private static final String EMAIL_SUBJECT_1 = "Testing the email API (all params)";
    private static final String EMAIL_SUBJECT_2 = "Testing the email API (plain txt body)";
    private static final String EMAIL_SUBJECT_3 = "Testing the email API (html txt body)";
    private static final String EMAIL_BODY_PLAIN = "This is a test message.";
    private static final String EMAIL_BODY_HTML = "<h2>This is a test message.<\\\\/h2>";
    private static final String[] EMAIL_RECIPIENTS = {"user1@clientapi.test", "user2@clientapi.test", "user3@clientapi.test"};

    private void emailApiTest()
    {
        // create the users for this test
        for (String user : EMAIL_RECIPIENTS)
            createUser(user, null);

        clickLinkWithText(PROJECT_NAME);
        enableEmailRecorder();

        // test failure cases: no from email
        setSource(createEmailSource("", EMAIL_SUBJECT, EMAIL_RECIPIENTS, EMAIL_BODY_PLAIN, EMAIL_BODY_HTML, false));
        assertTextPresent("failure");

        // no recipients
        setSource(createEmailSource(PasswordUtil.getUsername(), EMAIL_SUBJECT, new String[0], EMAIL_BODY_PLAIN, EMAIL_BODY_HTML, false));
        assertTextPresent("failure");

        // no message body
        setSource(createEmailSource(PasswordUtil.getUsername(), EMAIL_SUBJECT, EMAIL_RECIPIENTS, null, null, false));
        assertTextPresent("failure");

        // user not in system
        setSource(createEmailSource(PasswordUtil.getUsername(), EMAIL_SUBJECT, new String[]{"user4@clientapi.test"}, EMAIL_BODY_PLAIN, EMAIL_BODY_HTML, false));
        assertTextPresent("failure");
        setSource(createEmailSource(PasswordUtil.getUsername(), EMAIL_SUBJECT, new String[]{"user4@clientapi.test"}, EMAIL_BODY_PLAIN, EMAIL_BODY_HTML, true));
        assertTextPresent("success");

        setSource(createEmailSource(PasswordUtil.getUsername(), EMAIL_SUBJECT_1, EMAIL_RECIPIENTS, EMAIL_BODY_PLAIN, EMAIL_BODY_HTML, false));
        assertTextPresent("success");
        setSource(createEmailSource(PasswordUtil.getUsername(), EMAIL_SUBJECT_2, EMAIL_RECIPIENTS, EMAIL_BODY_PLAIN, null, true));
        assertTextPresent("success");
        setSource(createEmailSource(PasswordUtil.getUsername(), EMAIL_SUBJECT_3, EMAIL_RECIPIENTS, null, EMAIL_BODY_HTML, false));
        assertTextPresent("success");
        setSource(createEmailSource(PasswordUtil.getUsername(), null, EMAIL_RECIPIENTS, null, EMAIL_BODY_HTML, true));
        assertTextPresent("success");

        // verify principalId only allowed from a server side script
        setSource(createEmailSource(PasswordUtil.getUsername(), EMAIL_SUBJECT, new String[]{"-1", "-2"}, EMAIL_BODY_PLAIN, EMAIL_BODY_HTML, false));
        assertTextPresent("failure");

        goToModule("Dumbster");

        assertTextPresent(EMAIL_SUBJECT_1);
        assertTextPresent(EMAIL_SUBJECT_2);
        assertTextPresent(EMAIL_SUBJECT_3);

        // clear mail record messages
        uncheckCheckbox("emailRecordOn");
        checkCheckbox("emailRecordOn");
        uncheckCheckbox("emailRecordOn");
    }

    private String createEmailSource(String from, String subject, String[] recipients, String plainTxtBody, String htmlTxtBody, boolean allowUnregisteredUser)
    {
        StringBuilder recipientStr = new StringBuilder();
        StringBuilder contentStr = new StringBuilder();

        for (String email : recipients)
        {
            if (NumberUtils.isNumber(email))
            {
                // principal id
                recipientStr.append("LABKEY.Message.createPrincipalIdRecipient(LABKEY.Message.recipientType.to, '");
                recipientStr.append(email);
                recipientStr.append("'),");
            }
            else
            {
                recipientStr.append("LABKEY.Message.createRecipient(LABKEY.Message.recipientType.to, '");
                recipientStr.append(email);
                recipientStr.append("'),");
            }
        }

        if (plainTxtBody != null)
        {
            contentStr.append("LABKEY.Message.createMsgContent(LABKEY.Message.msgType.plain, '");
            contentStr.append(plainTxtBody);
            contentStr.append("'),");
        }

        if (htmlTxtBody != null)
        {
            contentStr.append("LABKEY.Message.createMsgContent(LABKEY.Message.msgType.html, '");
            contentStr.append(htmlTxtBody);
            contentStr.append("'),");
        }
        return String.format(EMAIL_SRC_TEMPLATE, from, StringUtils.trimToEmpty(subject), recipientStr.toString(),
                contentStr.toString(), String.valueOf(allowUnregisteredUser));
    }

    private void extIntegrationTest()
    {
        setSourceFromFile("extIntegrationTest.html", true);
        Locator loc = Locator.id(TEST_DIV_NAME);
        assertElementContains(loc, "Month of the Year");
        clearTestPage("Ext integration Test complete.");
    }

    private void webdavAPITest()
    {
        setSourceFromFile("webdavTest.html", true);
        Locator loc = Locator.id(TEST_DIV_NAME);
        assertElementContains(loc, "Test Started");
        waitForText("Test Complete");
        assertTextNotPresent("ERROR");
        clearTestPage("WebDav Client API Test complete.");
    }
}
