/*
 * Copyright (c) 2008-2014 LabKey Corporation
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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.Wiki;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;

import java.io.File;

import static org.junit.Assert.*;

@Category({BVT.class, Wiki.class})
public class ClientAPITest extends BaseWebDriverTest
{
    public WikiHelper _wikiHelper = new WikiHelper(this);

    private static final String PROJECT_NAME = "ClientAPITestProject";
    private static final String OTHER_PROJECT = "OtherClientAPITestProject"; // for cross-project query test
    protected static final String FOLDER_NAME = "api folder";
    private static final String SUBFOLDER_NAME = "subfolder";
    protected final static String LIST_NAME = "People";
    private final static String QUERY_LIST_NAME = "NewPeople";
    private final static String TEST_XLS_DATA_FILE = getLabKeyRoot() + "/sampledata/dataLoading/excel/ClientAPITestList.xls";
    private final static String SUBFOLDER_LIST = "subfolderList"; // for cross-folder query test
    private static final String OTHER_PROJECT_LIST = "otherProjectList"; // for cross-project query test
    protected final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.AutoInteger;
    protected final static String LIST_KEY_NAME = "Key";
    protected static final String TEST_ASSAY = "TestAssay1";
    protected static final String TEST_ASSAY_DESC = "Description for assay 1";
    protected final static ListHelper.ListColumn[] LIST_COLUMNS = new ListHelper.ListColumn[]
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

    protected final static String[][] TEST_DATA =
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

    public static final String SRC_PREFIX = CLIENTAPI_HEADER + "\n<script type=\"text/javascript\">\n" +
            "    Ext.namespace('demoNamespace'); //define namespace with some 'name'\n" +
            "    demoNamespace.myModule = function(){//creates a property 'myModule' of the namespace object\n" +
            "        return {\n" +
            "            init : function()\n" +
            "            {";

    public static final String SRC_SUFFIX = "            }\n" +
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

    public static String getFullSource(String testFragment)
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

    @Test
    public void testSteps()
    {
        _containerHelper.createProject(OTHER_PROJECT, null);
        _containerHelper.createProject(PROJECT_NAME, null);

        enableEmailRecorder();

        _containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME, null);

        createSubfolder(PROJECT_NAME, FOLDER_NAME, SUBFOLDER_NAME, "None", null); // for cross-folder query

        clickFolder(FOLDER_NAME);

        createWiki();

        createLists();

        gridTest();

        webpartTest();

        clearTestPage("WebPart Test Complete.");

        assayTest();

        queryTest();

        queryRegressionTest();

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
            clickFolder(FOLDER_NAME);
        clickWebpartMenuItem(WIKIPAGE_NAME, "Edit");
        _wikiHelper.setWikiBody("<p>" + message + "</p>");
        _wikiHelper.saveWikiPage();
    }

    protected String getListData(String listKeyName, ListHelper.ListColumn[] listColumns, String[][] listData)
    {
        StringBuilder data = new StringBuilder();
        data.append(listKeyName).append("\t");
        for (int i = 0; i < listColumns.length; i++)
        {
            data.append(listColumns[i].getName());
            data.append(i < listColumns.length - 1 ? "\t" : "\n");
        }
        for (String[] rowData : listData)
        {
            for (int col = 0; col < rowData.length; col++)
            {
                data.append(rowData[col]);
                data.append(col < rowData.length - 1 ? "\t" : "\n");
            }
        }

        return data.toString();
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void createLists()
    {
        createPeopleList();
        createLargePeopleList();
        createCrossFolderLists();
    }

    protected void createPeopleList()
    {
        String data = getListData(LIST_KEY_NAME, LIST_COLUMNS, TEST_DATA);

        _listHelper.createList(FOLDER_NAME, LIST_NAME, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);
        _listHelper.clickImportData();
        setFormElement(Locator.name("text"), data);
        _listHelper.submitImportTsv_success();
    }

    protected void createLargePeopleList()
    {
        // Create Larger list for query test.
        File listFile = new File(TEST_XLS_DATA_FILE);
        _listHelper.createListFromFile(FOLDER_NAME, QUERY_LIST_NAME, listFile);
        waitForElement(Locator.linkWithText("Norbert"));
    }

    protected void createCrossFolderLists()
    {
        String data = getListData(LIST_KEY_NAME, LIST_COLUMNS, TEST_DATA);

        // Create lists for cross-folder query test.
        _listHelper.createList(SUBFOLDER_NAME, SUBFOLDER_LIST, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);
        _listHelper.clickImportData();
        setFormElement(Locator.name("text"), data);
        _listHelper.submitImportTsv_success();

        // Create lists for cross-folder query test.
        _listHelper.createList(OTHER_PROJECT, OTHER_PROJECT_LIST, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);
        _listHelper.clickImportData();
        setFormElement(Locator.name("text"), data);
        _listHelper.submitImportTsv_success();
    }

    protected String waitForDivPopulation()
    {
        return waitForDivPopulation(30);
    }

    protected String waitForDivPopulation(int waitSeconds)
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
        fail("Div failed to render.");
        return null;
    }

    @LogMethod
    private void createWiki()
    {
        addWebPart("Wiki");
        _wikiHelper.createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), WIKIPAGE_NAME);
        setFormElement(Locator.name("title"), WIKIPAGE_NAME);
        _wikiHelper.setWikiBody("placeholder text");
        _wikiHelper.saveWikiPage();
    }

    @LogMethod
    private void webpartTest()
    {
        setSourceFromFile("webPartTest.js");

        waitForDivPopulation();
        assertTextPresent("Webpart Title");
        for (ListHelper.ListColumn column : LIST_COLUMNS)
            assertTextPresent(column.getLabel());
    }

    @LogMethod
    private void gridTest()
    {
        clickProject(PROJECT_NAME);
        clickFolder(FOLDER_NAME);

        setSourceFromFile("gridTest.js");

        assertTextPresent(GRIDTEST_GRIDTITLE);

        for (ListHelper.ListColumn col : LIST_COLUMNS)
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

        String activeCellId = getActiveEditorId();
        setFormElementJS(Locator.id(activeCellId), "Abe");
        pressTab(Locator.id(activeCellId));

        // enter a new last name
        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        assertNotEquals("Failed to advance to next edit field", prevActiveCellId, activeCellId);
        setFormElementJS(Locator.id(activeCellId), "Abeson");
        pressTab(Locator.id(activeCellId));

        // enter a new age
        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        assertNotEquals("Failed to advance to next edit field", prevActiveCellId, activeCellId);
        setFormElementJS(Locator.id(activeCellId), "51");
        pressTab(Locator.id(activeCellId));

        waitUntilGridUpdateComplete();

        // on the next row, change 'Bill' to 'Billy'
        doubleClick(Locator.xpath("//div[contains(@class,'x-grid3-row-selected')]//div[contains(@class,'x-grid3-col-1')]"));
        sleep(500);
        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        assertNotEquals("Failed to advance to next edit field", prevActiveCellId, activeCellId);
        setFormElementJS(Locator.id(activeCellId), "Billy");
        pressTab(Locator.id(activeCellId));

        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        assertNotEquals("Failed to advance to next edit field", prevActiveCellId, activeCellId);
        setFormElementJS(Locator.id(activeCellId), "Billyson");
        pressTab(Locator.id(activeCellId));

        prevActiveCellId = activeCellId;
        activeCellId = getActiveEditorId();
        assertNotEquals("Failed to advance to next edit field", prevActiveCellId, activeCellId);
        pressTab(Locator.id(activeCellId));
        sleep(500);

        // delete the row below Billy (which should contain Jane)
        click(Locator.id("delete-records-button"));
        click(Locator.xpath("//div[contains(@class, 'x-window-dlg')]//button[text()='Delete']"));
        waitForTextToDisappear("Jane");
        assertTextPresent("Abeson", "Billyson");
        assertTextNotPresent("Billson", "Jane");

        //test paging
        click(Locator.xpath("//button[contains(@class, 'x-tbar-page-next')]"));
        waitForText("Norbert");
        assertTextPresent("Norbert", "Penny", "Yak");

        click(Locator.xpath("//button[contains(@class, 'x-tbar-page-prev')]"));
        waitForText("Abe");
        assertTextPresent("Abeson", "Billyson", "Johnson");

        //test sorting
        click(Locator.xpath("//div[contains(@class, 'x-grid3-hd-2')]"));
        waitForText("Yak");
        assertTextPresentInThisOrder("Yakson", "Pennyson", "Norbertson");
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
            fail("Insert or update via the Ext grid did not complete!");
    }

    @LogMethod
    private void assayTest()
    {
        addWebPart("Assay List");

        //copied from old test
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkCheckbox(Locator.radioButtonByNameAndValue("providerName", "General"));
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        setFormElement(Locator.xpath("//input[@id='AssayDesignerName']"), TEST_ASSAY);
        setFormElement(Locator.xpath("//textarea[@id='AssayDesignerDescription']"), TEST_ASSAY_DESC);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);

        click(Locator.xpath(getPropertyXPath("Run Fields") + Locator.navButton("Add Field").getPath()));
        _listHelper.setColumnName(getPropertyXPath("Run Fields"), 0, "RunDate");
        _listHelper.setColumnLabel(getPropertyXPath("Run Fields"), 0, "Run Date");
        _listHelper.setColumnType(getPropertyXPath("Run Fields"), 0, ListHelper.ListColumnType.DateTime);

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

        _listHelper.setColumnName(0, "customfield1");
        _listHelper.setColumnLabel(0, "Custom Field 1");
        
        clickButton("Add Field", 0);

        _listHelper.setColumnName(1, "color");
        _listHelper.setColumnLabel(1, "Color");

        sleep(1000);
        clickButton("Save", WAIT_FOR_JAVASCRIPT);

        setSourceFromFile("domainTest.js");
        waitForElement(Locator.id(TEST_DIV_NAME).containing("Finished DomainTests."), 30000);

        assertElementContains(Locator.id(TEST_DIV_NAME), "Updated StudyProperties domain");
        assertElementContains(Locator.id(TEST_DIV_NAME), "Did not find the");
        assertElementContains(Locator.id(TEST_DIV_NAME), "Successfully updated the description");
    }

    protected File getApiFileRoot()
    {
        return new File(getLabKeyRoot(), "server/test/data/api/");
    }

    /**
     * Given a file name sets the page contents to a *wrapped* version of file in server/test/data/api
     * Wrapped version puts everything inside a function and inserts a div id="testDiv" for output to be placed in
     * @param fileName file will be found in server/test/data/api
     * @return
     */
    protected String setSourceFromFile(String fileName)
    {
        return setSourceFromFile(fileName, false);
    }

    protected String setSourceFromFile(String fileName, boolean excludeTags)
    {
        return setSource(getFileContents("server/test/data/api/" + fileName ), excludeTags);
    }

    protected String setSource(String srcFragment)
    {
        return setSource(srcFragment, false);
    }

    @LogMethod
    protected String setSource(String srcFragment, boolean excludeTags)
    {
        if (!isElementPresent(Locator.linkWithText(WIKIPAGE_NAME)))
            clickFolder(FOLDER_NAME);
        clickWebpartMenuItem(WIKIPAGE_NAME, "Edit");

        String fullSource = srcFragment;
        if (!excludeTags)
            fullSource = getFullSource(srcFragment);
        log("Setting wiki page source:");
        log(fullSource);
        _wikiHelper.setWikiBody(fullSource);
        _wikiHelper.saveWikiPage();
        return waitForDivPopulation();
    }

    protected String createAPITestWiki(String wikiName, File testSource, Boolean wrapSource)
    {
        if (!isElementPresent(Locator.folderTab("Wiki")))
            goToModule("Wiki");

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.clickWebpartMenuItem("Pages", "New");

        String fullSource;
        String srcFragment = getFileContents(testSource);

        if (wrapSource)
            fullSource = getFullSource(srcFragment);
        else
            fullSource = srcFragment;

        setFormElement(Locator.name("name"), wikiName);
        _wikiHelper.setWikiBody(fullSource);
        _wikiHelper.saveWikiPage();
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
        assertElementContains(loc, "SUCCESS: Created Custom View: 'QueryTestView' for list");
        assertElementContains(loc, "SUCCESS: SelectDistinctRows returned correct result set");
        assertElementContains(loc, "SUCCESS: SelectDistinctRows returned correct custom view filtered result set");
        clearTestPage("Query portion of test page complete");
    }

    @LogMethod
    private void queryRegressionTest()
    {
        setSourceFromFile("queryRegressionTest.js");
        Locator loc = Locator.id(TEST_DIV_NAME);
        assertElementContains(loc, "SUCCESS: executeSql 0 returned 125 rows");
        assertElementContains(loc, "SUCCESS: executeSql 1 returned 93 rows");
        assertElementContains(loc, "SUCCESS: executeSql 2 returned 10 rows");
        assertElementContains(loc, "SUCCESS: executeSql 2 returned with requested v9.1");
        assertElementContains(loc, "SUCCESS: selectRows 3 returned 96 rows with mixed sort parameters");
        clearTestPage("Query Regression portion of test complete");
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

        clickProject(PROJECT_NAME);
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
            if (NumberUtils.isDigits(email))
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
        assertFalse(loc.findElement(getDriver()).getText().contains("ERROR"));
        clearTestPage("WebDav Client API Test complete.");
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

}
