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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ListHelperWD;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * User: brittp
 * Created: Mar 12, 2008 9:36:47 AM
 */
public class ClientAPITest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ClientAPITestProject";
    private static final String OTHER_PROJECT = "OtherClientAPITestProject"; // for cross-project query test
    private static final String FOLDER_NAME = "api folder";
    private static final String SUBFOLDER_NAME = "subfolder";
    private final static String LIST_NAME = "People";
    private final static String SUBFOLDER_LIST = "subfolderList"; // for cross-folder query test
    private static final String OTHER_PROJECT_LIST = "otherProjectList"; // for cross-project query test
    private final static ListHelperWD.ListColumnType LIST_KEY_TYPE = ListHelperWD.ListColumnType.AutoInteger;
    private final static String LIST_KEY_NAME = "Key";
    protected static final String TEST_ASSAY = "TestAssay1";
    protected static final String TEST_ASSAY_DESC = "Description for assay 1";
    protected static final String[] CHARTING_API_TITLES = {
            "Line Plot - no y-scale defined",
            "Line Plot - y-scale defined, no legend, no shape aes",
            "Line Plot - No Layer AES, Changed Opacity",
            "Two Axis Scatter, plot null points",
            "Discrete X Scale Scatter No Geom Config",
            "Discrete X Scale Scatter, Log Y",
            "Boxplot no Geom Config",
            "Boxplot No Outliers",
            "Boxplot No Outliers, All Points"
    };

    private final static ListHelperWD.ListColumn[] LIST_COLUMNS = new ListHelperWD.ListColumn[]
    {
        new ListHelperWD.ListColumn("FirstName", "First Name", ListHelperWD.ListColumnType.String, "The first name"),
        new ListHelperWD.ListColumn("LastName", "Last Name", ListHelperWD.ListColumnType.String, "The last name"),
        new ListHelperWD.ListColumn("Age", "Age", ListHelperWD.ListColumnType.Integer, "The age")
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

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsers(afterTest, EMAIL_RECIPIENTS);

        deleteProject(PROJECT_NAME, afterTest);
        deleteProject(OTHER_PROJECT, afterTest);
    }

    protected void doTestSteps() throws Exception
    {
        _containerHelper.createProject(OTHER_PROJECT, null);
        _containerHelper.createProject(PROJECT_NAME, null);

        enableEmailRecorder();

        _containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME, null);

        createSubfolder(PROJECT_NAME, FOLDER_NAME, SUBFOLDER_NAME, "None", null); // for cross-folder query

        clickAndWait(Locator.linkWithText(FOLDER_NAME));

        createWiki();

        chartAPITest();

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

    @LogMethod
    private void clearTestPage(String message)
    {
        if (!isTextPresent(WIKIPAGE_NAME))
            clickAndWait(Locator.linkWithText(FOLDER_NAME));
        clickWebpartMenuItem(WIKIPAGE_NAME, "Edit");
        setWikiBody("<p>" + message + "</p>");
        saveWikiPage();
    }

    @LogMethod
    private void createLists()
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

        _listHelper.clickImportData();
        setFormElement(Locator.name("text"), data.toString());
        _listHelper.submitImportTsv_success();
        for (String[] rowData : TEST_DATA)
        {
            // check that all the data is in the grid (skipping the key column at index 0)
            for (int col = 1; col < rowData.length; col++)
            {
                assertTextPresent(rowData[col]);
            }
        }

        // Create lists for cross-folder query test.
        _listHelper.createList(SUBFOLDER_NAME, SUBFOLDER_LIST, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);
        _listHelper.clickImportData();
        setFormElement(Locator.name("text"), data.toString());
        _listHelper.submitImportTsv_success();

        // Create lists for cross-folder query test.
        _listHelper.createList(OTHER_PROJECT, OTHER_PROJECT_LIST, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);
        _listHelper.clickImportData();
        setFormElement(Locator.name("text"), data.toString());
        _listHelper.submitImportTsv_success();

        clickFolder(PROJECT_NAME);
        clickFolder(FOLDER_NAME);
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
            if (isElementPresent(Locator.id(TEST_DIV_NAME)))
            {
                String divHtml = (String)executeScript("return document.getElementById('" + TEST_DIV_NAME + "').innerHTML;");
                if (divHtml.length() > 0)
                    return divHtml;
            }
            sleep(1000);
        }
        Assert.fail("Div failed to render.");
        return null;
    }

    @LogMethod
    private void createWiki()
    {
        addWebPart("Wiki");
        createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), WIKIPAGE_NAME);
        setFormElement(Locator.name("title"), WIKIPAGE_NAME);
        setWikiBody("placeholder text");
        saveWikiPage();
    }

    @LogMethod
    private void webpartTest()
    {
        setSourceFromFile("webPartTest.js");

        waitForDivPopulation();
        assertTextPresent("Webpart Title");
        for (ListHelperWD.ListColumn column : LIST_COLUMNS)
            assertTextPresent(column.getLabel());
    }

    @LogMethod
    private void chartTest()
    {
        String chartHtml = setSourceFromFile("chartTest.js");
        if (!chartHtml.contains("<img") && !chartHtml.contains("<IMG"))
            Assert.fail("Test div does not contain an image:\n" + chartHtml);
    }

    @LogMethod
    private void chartAPITest() throws Exception
    {
        setSourceFromFile("chartingAPITest.js");

        //Some things we know about test 0. After this we loop through some others and just test to see if they convert
        waitForText("Current Config", WAIT_FOR_JAVASCRIPT);

        String testCountStr = getFormElement(Locator.id("configCount"));
        int testCount = Integer.parseInt(testCountStr);
        for (int currentTest = 0; currentTest < testCount; currentTest++)
        {
            waitForText(CHARTING_API_TITLES[currentTest], WAIT_FOR_JAVASCRIPT);
            checkSVGConversion();
            click(Locator.buttonContainingText("Next"));
        }
    }

    private void checkSVGConversion() throws Exception
    {
        //The server side svg converter is fairly strict and will fail with bad inputs
        clickButton("Get SVG", 0);
        String svgText = getFormElement(Locator.id("svgtext"));

        String url = WebTestHelper.getBaseURL() + "/visualization/" + PROJECT_NAME + "/" + EscapeUtil.encode(FOLDER_NAME)+ "/exportPDF.view";
        HttpClient httpClient = WebTestHelper.getHttpClient();
        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpPost method;
        HttpResponse response = null;

        try
        {
            method = new HttpPost(url);
            List<NameValuePair> args = new ArrayList<NameValuePair>();
            args.add(new BasicNameValuePair("svg", svgText));
            method.setEntity(new UrlEncodedFormEntity(args));
            response = httpClient.execute(method, context);
            int status = response.getStatusLine().getStatusCode();
            Assert.assertEquals("SVG Downloaded", HttpStatus.SC_OK, status);
            Assert.assertTrue(response.getHeaders("Content-Disposition")[0].getValue().startsWith("attachment;"));
            Assert.assertTrue(response.getHeaders("Content-Type")[0].getValue().startsWith("application/pdf"));
        }
        finally
        {
            if (null != response)
                EntityUtils.consume(response.getEntity());
            if (httpClient != null)
                httpClient.getConnectionManager().shutdown();
        }
    }

    @LogMethod
    private void gridTest()
    {
        setSourceFromFile("gridTest.js");

        assertTextPresent(GRIDTEST_GRIDTITLE);

        for (ListHelperWD.ListColumn col : LIST_COLUMNS)
            waitForText(col.getLabel());

        for (int row = 0; row < PAGE_SIZE; ++row)
        {
            // check that the first PAGE_SIZE rows of the data is in the grid (skipping the key column at index 0)
            for (int col = 1; col < TEST_DATA[row].length; col++)
                assertTextPresent(TEST_DATA[row][col]);
        }

        assertTextPresent("Displaying 1 - " + PAGE_SIZE);

        click(Locator.id("add-record-button"));
        String prevActiveCellId;
        // enter a new first name
//        sleep(50);
        String activeCellId = getActiveEditorId();
        setFormElement(Locator.id(activeCellId), "Abe");
        pressTab(Locator.id(activeCellId));
//        sleep(50);
        // enter a new last name
        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        if (prevActiveCellId.equals(activeCellId))
            Assert.fail("Failed to advance to next edit field");
        setFormElement(Locator.id(activeCellId), "Abeson");
        pressTab(Locator.id(activeCellId));
//        sleep(50);
        // enter a new age
        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        if (prevActiveCellId.equals(activeCellId))
            Assert.fail("Failed to advance to next edit field");
        setFormElement(Locator.id(activeCellId), "51");
        pressTab(Locator.id(activeCellId));

        waitUntilGridUpdateComplete();

        // on the next row, change 'Bill' to 'Billy'
        doubleClick(Locator.xpath("//div[contains(@class,'x-grid3-row-selected')]//div[contains(@class,'x-grid3-col-1')]"));
        sleep(500);
        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        if (prevActiveCellId.equals(activeCellId))
            Assert.fail("Failed to advance to next edit field");
        setFormElement(Locator.id(activeCellId), "Billy");
        pressTab(Locator.id(activeCellId));
//        sleep(50);
        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        if (prevActiveCellId.equals(activeCellId))
            Assert.fail("Failed to advance to next edit field");
        setFormElement(Locator.id(activeCellId), "Billyson");
        pressTab(Locator.id(activeCellId));
//        sleep(50);
        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        if (prevActiveCellId.equals(activeCellId))
            Assert.fail("Failed to advance to next edit field");
        pressTab(Locator.id(activeCellId));
        sleep(500);

        // delete the row below Billy (which should contain Jane)
        click(Locator.id("delete-records-button"));
//        sleep(50);
        click(Locator.xpath("//div[contains(@class, 'x-window-dlg')]//button[text()='Delete']"));
//        sleep(50);

        int limit = 30;
        while (isTextPresent("Jane") && limit-- > 0)
            sleep(1000);

        assertTextPresent("Abeson", 1);
        assertTextPresent("Billy", 2);
        assertTextNotPresent("Billson");
        assertTextNotPresent("Jane");

        //test paging
        click(Locator.xpath("//button[contains(@class, 'x-tbar-page-next')]"));

        limit = 30;
        while (!isTextPresent("Norbert") && limit-- > 0)
            sleep(1000);

        assertTextPresent("Norbert");
        assertTextPresent("Penny");
        assertTextPresent("Yak");

        click(Locator.xpath("//button[contains(@class, 'x-tbar-page-prev')]"));
        limit = 30;
        while (!isTextPresent("Abe") && limit-- > 0)
            sleep(1000);

        assertTextPresent("Abeson");
        assertTextPresent("Billyson");
        assertTextPresent("Johnson");

        //test sorting
        click(Locator.xpath("//div[contains(@class, 'x-grid3-hd-2')]"));
        limit = 30;
        while (!isTextPresent("Yak") && limit-- > 0)
            sleep(1000);

        assertTextBefore("Yakson", "Pennyson");
        assertTextBefore("Pennyson", "Norbertson");
    }

    private String getActiveEditorId()
    {
        int limit = 3;
        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                return (Boolean)executeScript("return window.gridView.activeEditor != null;");
            }
        }, "Could not get the id of the active editor in the grid!", limit * 50);

        return (String)executeScript("return window.gridView.activeEditor.field.id;");
    }

    private void waitUntilGridUpdateComplete()
    {
        int tries = 20;
        Long numDirty;
        do{
            numDirty = (Long)executeScript("return window.gridView.getStore().getModifiedRecords().length;");
            log("getModifiedRecords().length returned " + numDirty);
            sleep(500);
        }while(--tries > 0 && numDirty != 0L);
        if(tries == 0)
            Assert.fail("Insert or update via the Ext grid did not complete!");
    }

    @LogMethod
    private void assayTest()
    {
        addWebPart("Assay List");

        //copied from old test
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkRadioButton("providerName", "General");
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        setFormElement(Locator.xpath("//input[@id='AssayDesignerName']"), TEST_ASSAY);
        setFormElement(Locator.xpath("//textarea[@id='AssayDesignerDescription']"), TEST_ASSAY_DESC);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);

        click(Locator.xpath(getPropertyXPath("Run Fields") + Locator.navButton("Add Field").getPath()));
        _listHelper.setColumnName(getPropertyXPath("Run Fields"), 0, "RunDate");
        _listHelper.setColumnLabel(getPropertyXPath("Run Fields"), 0, "Run Date");
        _listHelper.setColumnType(getPropertyXPath("Run Fields"), 0, ListHelperWD.ListColumnType.DateTime);

        sleep(1000);
        clickButton("Save", 0);
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

    @LogMethod
    private void domainTest()
    {
        addWebPart("Study Overview");

        clickButton("Create Study");
        // next page
        clickButton("Create Study");
        clickAndWait(Locator.linkWithText("Edit Definition"));
        waitForText("No fields have been defined.", 10000);

        clickButton("Add Field", 0);

        _listHelper.setColumnName(0, "species");
        _listHelper.setColumnLabel(0, "Species");
        
        clickButton("Add Field", 0);

        _listHelper.setColumnName(1, "color");
        _listHelper.setColumnLabel(1, "Color");

        sleep(1000);
        clickButton("Save", 10000);

        setSourceFromFile("domainTest.js");
        waitForElement(Locator.id(TEST_DIV_NAME).containing("Finished DomainTests."), 30000);

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

    @LogMethod
    private String setSource(String srcFragment, boolean excludeTags)
    {
        if (!isTextPresent(WIKIPAGE_NAME))
            clickAndWait(Locator.linkWithText(FOLDER_NAME));
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

    @LogMethod
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
        assertElementContains(loc, "SUCCESS: Bad insert generated exception: Data does not contain required field: LastName");
        assertElementContains(loc, "SUCCESS: Bad query generated exception: Could not find schema: lists-badname");
        assertElementContains(loc, "SUCCESS: executeSql returned 7 rows");
        assertElementContains(loc, "SUCCESS: executeSql returned a session-based query");
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
    @LogMethod
    private void emailApiTest()
    {
        // create the users for this test
        for (String user : EMAIL_RECIPIENTS)
            createUser(user, null);

        clickFolder(PROJECT_NAME);
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

    @LogMethod
    private void extIntegrationTest()
    {
        setSourceFromFile("extIntegrationTest.html", true);
        Locator loc = Locator.id(TEST_DIV_NAME);
        assertElementContains(loc, "Month of the Year");
        clearTestPage("Ext integration Test complete.");
    }

    @LogMethod
    private void webdavAPITest()
    {
        setSourceFromFile("webdavTest.html", true);
        Locator loc = Locator.id(TEST_DIV_NAME);
        assertElementContains(loc, "Test Started");
        waitForText("Test Complete");
        Assert.assertFalse(loc.findElement(_driver).getText().contains("ERROR"));
        clearTestPage("WebDav Client API Test complete.");
    }
}
