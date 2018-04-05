/*
 * Copyright (c) 2008-2017 LabKey Corporation
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
import org.apache.http.client.methods.HttpUriRequest;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.query.GetQueryDetailsCommand;
import org.labkey.remoteapi.query.GetQueryDetailsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.Wiki;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.pages.study.CreateStudyPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.StudyHelper;
import org.labkey.test.util.UIUserHelper;
import org.labkey.test.util.WikiHelper;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.WebTestHelper.getHttpResponse;

@Category({BVT.class, Wiki.class})
public class ClientAPITest extends BaseWebDriverTest
{
    public WikiHelper _wikiHelper = new WikiHelper(this);
    public StudyHelper _studyHelper = new StudyHelper(this);

    private static final String PROJECT_NAME = "ClientAPITestProject";
    private static final String OTHER_PROJECT = "OtherClientAPITestProject"; // for cross-project query test
    protected static final String FOLDER_NAME = "api folder";
    private static final String SUBFOLDER_NAME = "subfolder";
    private static final String TIME_STUDY_FOLDER = "timeStudyFolder";
    private static final String TIME_STUDY_NAME = "timeStudyName";
    private static final String VISIT_STUDY_FOLDER = "visitStudyFolder";
    private static final String VISIT_STUDY_NAME = "visitStudyName";
    public final static String LIST_NAME = "People";
    private final static String QUERY_LIST_NAME = "NewPeople";
    private final static String TEST_XLS_DATA_FILE = TestFileUtils.getLabKeyRoot() + "/sampledata/dataLoading/excel/ClientAPITestList.xls";
    private final static String SUBFOLDER_LIST = "subfolderList"; // for cross-folder query test
    private static final String OTHER_PROJECT_LIST = "otherProjectList"; // for cross-project query test
    public final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.AutoInteger;
    public final static String LIST_KEY_NAME = "Key";
    protected static final String TEST_ASSAY = "TestAssay1";
    protected static final String TEST_ASSAY_DESC = "Description for assay 1";
    public final static ListHelper.ListColumn[] LIST_COLUMNS = new ListHelper.ListColumn[]
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

    public final static String[][] TEST_DATA =
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

    public static final String TEST_DIV_NAME = "testDiv";

    private static final String GRIDTEST_GRIDTITLE = "ClientAPITest Grid Title";

    private static final int PAGE_SIZE = 4;

    public static final String SRC_PREFIX = "<script type=\"text/javascript\">\n" +
            "    LABKEY.requiresExt3ClientAPI(true /* left on purpose to test old signature */, function() {\n" +
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
            "    });\n" +
            "</script>\n" +
            "<div id=\"" + TEST_DIV_NAME + "\"></div>";

    public static String getFullSource(String testFragment)
    {
        return SRC_PREFIX + "\n" + testFragment + "\n" + SRC_SUFFIX;
    }

    public List<String> getAssociatedModules()
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
        _userHelper.deleteUsers(false, EMAIL_API_USERS);
        _userHelper.deleteUser(AUTOCOMPLETE_USER);
        _containerHelper.deleteProject(PROJECT_NAME, afterTest);
        _containerHelper.deleteProject(OTHER_PROJECT, afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        ClientAPITest init = (ClientAPITest)getCurrentTest();
        init._containerHelper.createProject(OTHER_PROJECT, null);
        init._containerHelper.createProject(PROJECT_NAME, null);

        init.enableEmailRecorder();

        init._containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME);
        init._containerHelper.createSubfolder(PROJECT_NAME, TIME_STUDY_FOLDER);
        init._containerHelper.createSubfolder(PROJECT_NAME, VISIT_STUDY_FOLDER);

        init._containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME, SUBFOLDER_NAME, "None", null);

        init.createStudies();

        init.clickFolder(FOLDER_NAME);

        init.createWiki();

        init.createLists();

        init.createUsers();
    }

    private static boolean dirtyList = false;

    @Before
    public void preTest()
    {
        navigateToFolder(getProjectName(), FOLDER_NAME);

        if (dirtyList)
        {
            refreshPeopleList();
            navigateToFolder(getProjectName(), FOLDER_NAME);
        }
    }

    @Override
    protected void checkLinks()
    {
        //delete the test page so the crawler doesn't refetch a test and cause errors
        getHttpResponse(WebTestHelper.buildURL("wiki", getProjectName() + "/" + FOLDER_NAME, "delete", Maps.of("name", WIKIPAGE_NAME)), "POST");
        super.checkLinks();
    }

    public static String getListData(String listKeyName, ListHelper.ListColumn[] listColumns, String[][] listData)
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

    @LogMethod
    private void createLists()
    {
        createPeopleList();
        createLargePeopleList();
        createCrossFolderLists();
    }

    private void createPeopleList()
    {
        String data = getListData(LIST_KEY_NAME, LIST_COLUMNS, TEST_DATA);

        _listHelper.createList(PROJECT_NAME + "/" + FOLDER_NAME, LIST_NAME, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);
        _listHelper.clickImportData();
        setFormElement(Locator.name("text"), data);
        _listHelper.submitImportTsv_success();
    }

    private void refreshPeopleList()
    {
        String data = getListData(LIST_KEY_NAME, LIST_COLUMNS, TEST_DATA);

        goToModule("List");
        waitForElement(Locator.linkWithText(LIST_NAME));
        clickAndWait(Locator.linkWithText(LIST_NAME));
        final DataRegionTable list = new DataRegionTable("query", this);
        list.checkAll();

        doAndWaitForPageToLoad(() ->
        {
            list.clickHeaderButton("Delete");
            assertAlertContains("Are you sure you want to delete the selected rows?");
        });

        list.clickImportBulkData();
        setFormElement(Locator.name("text"), data);
        _listHelper.submitImportTsv_success();
    }

    private void createLargePeopleList()
    {
        // Create Larger list for query test.
        File listFile = new File(TEST_XLS_DATA_FILE);
        _listHelper.createListFromFile(getProjectName() + "/" + FOLDER_NAME, QUERY_LIST_NAME, listFile);
        waitForElement(Locator.linkWithText("Norbert"));
    }

    private void createCrossFolderLists()
    {
        String data = getListData(LIST_KEY_NAME, LIST_COLUMNS, TEST_DATA);

        // Create lists for cross-folder query test.
        _listHelper.createList(PROJECT_NAME + "/" + FOLDER_NAME + "/" + SUBFOLDER_NAME, SUBFOLDER_LIST, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);
        _listHelper.clickImportData();
        setFormElement(Locator.name("text"), data);
        _listHelper.submitImportTsv_success();

        // Create lists for cross-folder query test.
        clickProject(OTHER_PROJECT);
        _listHelper.createList(OTHER_PROJECT, OTHER_PROJECT_LIST, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);
        _listHelper.clickImportData();
        setFormElement(Locator.name("text"), data);
        _listHelper.submitImportTsv_success();
    }

    private void createUsers()
    {
        // create the users for emailApiTest
        _userHelper.createUser(EMAIL_SENDER);
        _userHelper.createUser(EMAIL_RECIPIENT1);
        _userHelper.createUser(EMAIL_RECIPIENT2);
        new UIUserHelper(this).cloneUser(AUTOCOMPLETE_USER, PasswordUtil.getUsername());
    }

    private void createStudies()
    {
        // create time-based study
        projectMenu().navigateToFolder(getProjectName(), TIME_STUDY_FOLDER);
        navBar().goToModule("Study");
        CreateStudyPage createStudyPage = _studyHelper.startCreateStudy();
        createStudyPage.setLabel(TIME_STUDY_NAME)
                .setTimepointType(StudyHelper.TimepointType.DATE)
                .createStudy();

        // create a visit-based study in a different container
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        navBar().goToModule("Study");
        CreateStudyPage createVisitStudyPage = _studyHelper.startCreateStudy();
        createVisitStudyPage.setLabel(VISIT_STUDY_NAME)
                .setTimepointType(StudyHelper.TimepointType.VISIT)
                .createStudy();
    }

    protected String waitForDivPopulation()
    {
        return waitForWikiDivPopulation(TEST_DIV_NAME, 30);
    }

    @LogMethod
    private void createWiki()
    {
        beginAt(WebTestHelper.buildURL("wiki", getProjectName() + "/" + FOLDER_NAME, "edit"));
        setFormElement(Locator.name("name"), WIKIPAGE_NAME);
        setFormElement(Locator.name("title"), WIKIPAGE_NAME);
        _wikiHelper.setWikiBody("placeholder text");
        _wikiHelper.saveWikiPage();
    }

    @Test
    public void createStudyDatasetVisitWithWithTimeKeyField()
    {
        // this test case attempts to create a time-keyed dataset in a visit-based study folder, which should
        // fail on create.
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String dataSetName = "ThisShouldBlowUp";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                "       fields: [{\n" +
                "           name: \"intFieldOne\",\n" +
                "           rangeURI: \"int\"\n" +
                "       },{\n" +
                "           name: \"stringFieldOne\",\n" +
                "           rangeURI: \"string\"\n" +
                "       },{\n" +
                "           name: \"labName\",\n" +
                "           rangeURI: \"string\"\n" +
                "       },{\n" +
                "           name : \"labLocation\",\n" +
                "           rangeURI : \"string\"\n" +
                "       }]\n" +
                "   },\n" +
                "   options : {\n" +
                "       keyPropertyName : 'intFieldOne',\n" +
                "       useTimeKeyField : true,\n" +
                "       demographics : true\n" +
                "   },\n" +
                "   success: callback," +
                "   failure: callback"+
                "});\n";
        checkErrors();
        try
        {
            executeAsyncScript(create);
            fail("Should not have successfully created the domain");
        }
        catch(AssertionError expected)
        {
            log("success: " + expected.getMessage());
            resetErrors();
        }
    }

    @Test
    public void createStudyDatasetDateWithoutTimeKeyField()
    {
        projectMenu().navigateToFolder(getProjectName(), TIME_STUDY_FOLDER);
        String dataSetName = "ThisShouldBlowUp";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetDate\",\n" +
                "   domainDesign: {\n" +
                "       name : '" + dataSetName + "',\n" +
                "       fields: [{\n" +
                "           name: \"intFieldOne\",\n" +
                "           rangeURI: \"int\"\n" +
                "       },{\n" +
                "           name: \"intFieldTwo\",\n" +
                "           rangeURI: \"int\"\n" +
                "       },{\n" +
                "           name: \"stringFieldOne\",\n" +
                "           rangeURI: \"string\"\n" +
                "       },{\n" +
                "           name: \"labName\",\n" +
                "           rangeURI: \"string\"\n" +
                "       },{\n" +
                "           name : \"labLocation\",\n" +
                "           rangeURI : \"string\"\n" +
                "       }]\n" +
                "   },\n" +
                "   options : {\n" +
                "       keyPropertyName : 'intFieldOne',\n" +
                "       useTimeKeyField : false\n" +
                "   },\n" +
                "   success: callback," +
                "   failure: callback" +
                "});\n";
        checkErrors();
        try
        {
            executeAsyncScript(create);
            fail("Should not have created this domain");
        }
        catch(AssertionError expected)
        {
            log("success." + expected.getMessage());
            resetErrors();
        }
    }

    @Test
    public void createVisitBasedDomain()
    {
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String dataSetName = "MyVisitBasedDataSet";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                "       fields: [{\n" +
                "           name: \"intFieldOne\",\n" +
                "           rangeURI: \"int\"\n" +
                "       },{\n" +
                "           name: \"stringFieldOne\",\n" +
                "           rangeURI: \"string\"\n" +
                "       },{\n" +
                "           name: \"labName\",\n" +
                "           rangeURI: \"string\"\n" +
                "       },{\n" +
                "           name : \"labLocation\",\n" +
                "           rangeURI : \"string\"\n" +
                "       }]\n" +
                "   },\n" +
                "   options : {\n" +
                "       keyPropertyName : 'intFieldOne',\n" +
                "       useTimeKeyField : false,\n" +
                "       demographics : true\n" +
                "   },\n" +
                "   success: callback," +
                "   failure: callback"+
                "});\n";

        Map<String, Object> createResult = (Map<String,Object>)executeAsyncScript(create);
        List<Map<String, Object>> fields = (List<Map<String, Object>>)createResult.get("fields");
        assertTrue(fields.size() == 4);
        assertTrue(fields.stream().anyMatch(a-> a.get("name").toString().equals("intFieldOne")));
        assertTrue(fields.stream().anyMatch(a-> a.get("name").toString().equals("stringFieldOne")));
        assertTrue(fields.stream().anyMatch(a-> a.get("name").toString().equals("labName")));
        assertTrue(fields.stream().anyMatch(a-> a.get("name").toString().equals("labLocation")));

        String updateDomain = "// fetch the original domain definition\n" +
                "LABKEY.Domain.get({\n" +
                "    schemaName: \"study\",\n" +
                "    queryName: '"+dataSetName+"',\n" +
                "    success: function(domain) {\n" +
                "        if (domain) {\n" +
                "            domain.fields.push({\n" +
                "                name: \"extraCustomStringField\",\n" +
                "                rangeURI: \"string\"\n" +
                "            });\n" +
                "\n" +
                "            // add a field and save the modification to the domain\n" +
                "            LABKEY.Domain.save({\n" +
                "                schemaName: \"study\",\n" +
                "                queryName: '"+dataSetName+"',\n" +
                "                domainDesign: domain,\n" +
                "                success: function(saveResult) {\n" +
                "\n" +
                "                    // return the modified domain\n" +
                "                    LABKEY.Domain.get({\n" +
                "                        schemaName: \"study\",\n" +
                "                        queryName: '"+dataSetName+"',\n" +
                "                        success: callback,\n" +
                "                        failure: callback\n" +
                "                    });\n" +
                "                },\n" +
                "                failure: callback\n" +
                "            });\n" +
                "        }\n" +
                "        else {\n" +
                "            callback({\n" +
                "                success: false,\n" +
                "                error: \"Domain not available.\"\n" +
                "            });\n" +
                "        }\n" +
                "    },\n" +
                "    failure: callback\n" +
                "});";

        Map<String, Object> updateResult = (Map<String, Object>)executeAsyncScript(updateDomain);
        List<Map<String, Object>> updatedFields = (List<Map<String, Object>>) updateResult.get("fields");
        assertTrue(updatedFields.size() == 5);
        assertTrue(updatedFields.stream().anyMatch(a -> a.get("name").toString().equals("intFieldOne")));
        assertTrue(updatedFields.stream().anyMatch(a -> a.get("name").toString().equals("stringFieldOne")));
        assertTrue(updatedFields.stream().anyMatch(a -> a.get("name").toString().equals("labName")));
        assertTrue(updatedFields.stream().anyMatch(a -> a.get("name").toString().equals("labLocation")));
        assertTrue(updatedFields.stream().anyMatch(a -> a.get("name").toString().equals("extraCustomStringField")
                && a.get("rangeURI").toString().endsWith("string")));

        // now delete it
        String deleteScript = "LABKEY.Domain.drop({\n" +
                "   schemaName : \"study\",\n" +
                "   queryName : '"+dataSetName+"',\n" +
                "   success: callback," +
                "   failure: callback"+
                "});\n";
        Map<String, Object> deleteResult = (Map<String,Object>)executeAsyncScript(deleteScript);
        assertEquals("true", deleteResult.get("success").toString());
        assertEquals("Domain deleted", deleteResult.get("message").toString());
    }

    @Test
    public void createTimeBasedDomain()
    {
        projectMenu().navigateToFolder(getProjectName(), TIME_STUDY_FOLDER);
        String dataSetName = "MyTestTimeBasedDataset";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetDate\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                "       fields: [{\n" +
                "           name: \"intFieldOne\",\n" +
                "           rangeURI: \"int\"\n" +
                "       },{\n" +
                "           name: \"intFieldTwo\",\n" +
                "           rangeURI: \"int\"\n" +
                "       },{\n" +
                "           name: \"stringFieldOne\",\n" +
                "           rangeURI: \"string\"\n" +
                "       },{\n" +
                "           name: \"labName\",\n" +
                "           rangeURI: \"string\"\n" +
                "       },{\n" +
                "           name : \"labLocation\",\n" +
                "           rangeURI : \"string\"\n" +
                "       }]\n" +
                "   },\n" +
                "   options : {\n" +
                "       keyPropertyName : 'intFieldOne',\n" +
                "   },\n" +
                "   success: callback," +
                "   failure: callback"+
                "});\n";

        Map<String, Object> createResult = (Map<String,Object>)executeAsyncScript(create);
        List<Map<String, Object>> fields = (List<Map<String, Object>>)createResult.get("fields");
        assertEquals(5, fields.size());
        assertTrue(fields.stream().anyMatch(a-> a.get("name").toString().equals("intFieldOne")
                    && a.get("rangeURI").toString().endsWith("int")));
        assertTrue(fields.stream().anyMatch(a-> a.get("name").toString().equals("intFieldTwo")));
        assertTrue(fields.stream().anyMatch(a-> a.get("name").toString().equals("stringFieldOne")
                    && a.get("rangeURI").toString().endsWith("string")));
        assertTrue(fields.stream().anyMatch(a-> a.get("name").toString().equals("labName")));
        assertTrue(fields.stream().anyMatch(a-> a.get("name").toString().equals("labLocation")));

        // edit domain && validate edits
        projectMenu().navigateToFolder(getProjectName(), TIME_STUDY_FOLDER);
        String updateDomain = "// fetch the original domain definition\n" +
                "LABKEY.Domain.get({\n" +
                "    schemaName: \"study\",\n" +
                "    queryName: '"+dataSetName+"',\n" +
                "    success: function(domain) {\n" +
                "        if (domain) {\n" +
                "            domain.fields.push({\n" +
                "                name: \"extraCustomStringField\",\n" +
                "                rangeURI: \"string\"\n" +
                "            });\n" +
                "\n" +
                "            // add a field and save the modification to the domain\n" +
                "            LABKEY.Domain.save({\n" +
                "                schemaName: \"study\",\n" +
                "                queryName: '"+dataSetName+"',\n" +
                "                domainDesign: domain,\n" +
                "                success: function(saveResult) {\n" +
                "\n" +
                "                    // return the modified domain\n" +
                "                    LABKEY.Domain.get({\n" +
                "                        schemaName: \"study\",\n" +
                "                        queryName: '"+dataSetName+"',\n" +
                "                        success: callback,\n" +
                "                        failure: callback\n" +
                "                    });\n" +
                "                },\n" +
                "                failure: callback\n" +
                "            });\n" +
                "        }\n" +
                "        else {\n" +
                "            callback({\n" +
                "                success: false,\n" +
                "                error: \"Domain not available.\"\n" +
                "            });\n" +
                "        }\n" +
                "    },\n" +
                "    failure: callback\n" +
                "});";
        Map<String, Object> updateResult = (Map<String, Object>)executeAsyncScript(updateDomain);
        List<Map<String, Object>> updatedFields = (List<Map<String, Object>>)updateResult.get("fields");
        assertEquals(6, updatedFields.size());
        assertTrue(updatedFields.stream().anyMatch(a-> a.get("name").toString().equals("intFieldOne")
                && a.get("rangeURI").toString().endsWith("int")));
        assertTrue(updatedFields.stream().anyMatch(a-> a.get("name").toString().equals("intFieldTwo")));
        assertTrue(updatedFields.stream().anyMatch(a-> a.get("name").toString().equals("stringFieldOne")
                && a.get("rangeURI").toString().endsWith("string")));
        assertTrue(updatedFields.stream().anyMatch(a-> a.get("name").toString().equals("labName")));
        assertTrue(updatedFields.stream().anyMatch(a-> a.get("name").toString().equals("labLocation")));
        assertTrue(updatedFields.stream().anyMatch(a-> a.get("name").toString().equals("extraCustomStringField")
                && a.get("rangeURI").toString().endsWith("string")));

        // now delete the domain
        String deleteScript = "LABKEY.Domain.drop({\n" +
                "   schemaName:'study',\n" +
                "   queryName:'"+dataSetName+"',\n" +
                "   success: callback," +
                "   failure: callback"+
                "});\n";
        Map<String, Object> deleteResult = (Map<String,Object>)executeAsyncScript(deleteScript);
        assertEquals("true", deleteResult.get("success").toString());
        assertEquals("Domain deleted", deleteResult.get("message").toString());
    }

    @Test
    public void maxRowsTest()
    {
        setSourceFromFile("maxRows.js");
        waitForText (5000, "maxRows Test");
        assertTextPresent("Janeson");
        assertTextNotPresent("Johnson");

    }
    @Test
    public void webpartTest()
    {
        setSourceFromFile("webPartTest.js");

        assertTextPresent("Webpart Title");
        for (ListHelper.ListColumn column : LIST_COLUMNS)
            assertTextPresent(column.getLabel());
    }

    @Test
    public void gridTest()
    {
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
        waitFor(() -> (Boolean)executeScript("return window.gridView.activeEditor != null;"),
                "Could not get the id of the active editor in the grid!", limit * 50);

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

    @Test
    public void assayTest()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Assay List");

        //copied from old test
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkCheckbox(Locator.radioButtonByNameAndValue("providerName", "General"));
        clickButton("Next");

        AssayDesignerPage assayDesigner = new AssayDesignerPage(getDriver());
        assayDesigner
                .setName(TEST_ASSAY)
                .setDescription(TEST_ASSAY_DESC);
        assayDesigner.runFields()
                .addField(new FieldDefinition("RunDate").setLabel("Run Date").setType(FieldDefinition.ColumnType.DateTime));
        assayDesigner.saveAndClose();

        setSourceFromFile("assayTest.js");

        assertTextPresent(
                TEST_ASSAY,
                TEST_ASSAY + " Run Fields",
                "RunDate - DateTime",
                TEST_ASSAY + " Batch Fields",
                "TargetStudy - String",
                TEST_ASSAY + " Data Fields",
                "VisitID - Double");
    }

    @Test
    public void domainTest()
    {
        goToModule("Study");

        clickButton("Create Study");
        // next page
        clickButton("Create Study");
        clickAndWait(Locator.linkWithText("Edit Additional Properties"));
        waitForText(10000, "No fields have been defined.");

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

    /**
     * Given a file name sets the page contents to a *wrapped* version of file in server/test/data/api
     * Wrapped version puts everything inside a function and inserts a div id="testDiv" for output to be placed in
     * @param fileName file will be found in server/test/data/api
     */
    protected String setSourceFromFile(String fileName)
    {
        return setSourceFromFile(fileName, false);
    }

    protected String setSourceFromFile(String fileName, boolean excludeTags)
    {
        return setSource(TestFileUtils.getFileContents("server/test/data/api/" + fileName), excludeTags);
    }

    protected String setSource(String srcFragment)
    {
        return setSource(srcFragment, false);
    }

    @LogMethod
    protected String setSource(String srcFragment, boolean excludeTags)
    {
        beginAt(WebTestHelper.buildURL("wiki", getProjectName() + "/" + FOLDER_NAME, "edit", Maps.of("name", WIKIPAGE_NAME)));

        String fullSource = srcFragment;
        if (!excludeTags)
            fullSource = getFullSource(srcFragment);
        log("Setting wiki page source:");
//        log(fullSource);
        _wikiHelper.setWikiBody(fullSource);
        _wikiHelper.saveWikiPage();
        return waitForDivPopulation();
    }

    @Test
    public void queryTest()
    {
        dirtyList = true;
        String script = TestFileUtils.getFileContents(TestFileUtils.getSampleData("api/queryTest.js"));
        String scriptResult = (String)((JavascriptExecutor) getDriver()).executeAsyncScript(script);
        String[] testResults = scriptResult.split("\n");

        for (String result : testResults)
        {
            log(result);
        }
        assertFalse(scriptResult.contains("FAILURE"));
        assertEquals("Wrong number of results", 22, testResults.length);
    }

    @Test
    public void queryRegressionTest()
    {
        String script = TestFileUtils.getFileContents(TestFileUtils.getSampleData("api/queryRegressionTest.js"));
        String scriptResult = (String)((JavascriptExecutor) getDriver()).executeAsyncScript(script);
        String[] testResults = scriptResult.split("\n");

        for (String result : testResults)
        {
            log(result);
        }
        assertFalse(scriptResult.contains("ERROR"));
        assertEquals("Wrong number of results", 5, testResults.length);
    }

    private static final String EMAIL_SENDER = "sender@clientapi.test";
    private static final String EMAIL_RECIPIENT1 = "recipient1@clientapi.test";
    private static final String EMAIL_RECIPIENT2 = "recipient2@clientapi.test";
    private static final String EMAIL_NO_LOGIN = "no_login@clientapi.test";
    private static final String[] EMAIL_API_USERS = {EMAIL_SENDER, EMAIL_RECIPIENT1, EMAIL_RECIPIENT2, EMAIL_NO_LOGIN};

    @Test
    public void emailApiTest()
    {
        final String EMAIL_SUBJECT_ERROR = "Testing the email API (should error)";
        final String EMAIL_SUBJECT_NON_USER = "Testing the email API (to non-user)";
        final String EMAIL_SUBJECT_ALL = "Testing the email API (all params)";
        final String EMAIL_SUBJECT_PLAIN = "Testing the email API (plain txt body)";
        final String EMAIL_SUBJECT_HTML = "Testing the email API (html txt body)";
        final String EMAIL_SUBJECT_FROM_NEW_USER = "Testing the email API (sent from never logged in user)";
        final String EMAIL_BODY_PLAIN = "This is a test message.";
        final String EMAIL_BODY_HTML = "<h2>" + EMAIL_BODY_PLAIN + "</h2>";
        final String[] EMAIL_RECIPIENTS = {EMAIL_RECIPIENT1, EMAIL_RECIPIENT2};

        final ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);

        enableEmailRecorder();

        // TODO: Increase permission level to App Admin after Issue 33831 has been resolved
        apiPermissionsHelper.addMemberToRole(EMAIL_SENDER, "Reader", PermissionsHelper.MemberType.user, "Home");

        goToHome();

        impersonate(EMAIL_SENDER);
        {
            assertEquals("sendMessage API without correct role", "The current user does not have permission to use the SendMessage API.", executeEmailScript(PasswordUtil.getUsername(), EMAIL_SUBJECT_ALL, EMAIL_RECIPIENTS, EMAIL_BODY_PLAIN, EMAIL_BODY_HTML));
        }
        stopImpersonating();

        apiPermissionsHelper.addMemberToRole(EMAIL_SENDER, "Use SendMessage API", PermissionsHelper.MemberType.user, "/");

        impersonate(EMAIL_SENDER);
        final String NON_USER_EMAIL = "non_user@clientapi.test";
        {
            assertEquals("sendMessage API with no sender", "Invalid email format.", executeEmailScript("", EMAIL_SUBJECT_ERROR, EMAIL_RECIPIENTS, EMAIL_BODY_PLAIN, EMAIL_BODY_HTML));
            assertEquals("sendMessage API with no recipients", "No message recipients supplied.", executeEmailScript(PasswordUtil.getUsername(), EMAIL_SUBJECT_ERROR, new String[0], EMAIL_BODY_PLAIN, EMAIL_BODY_HTML));
            assertEquals("sendMessage API with no message body", "No message contents supplied.", executeEmailScript(PasswordUtil.getUsername(), EMAIL_SUBJECT_ERROR, EMAIL_RECIPIENTS, null, null));
            assertEquals("sendMessage API to non-user", "The email address '" + NON_USER_EMAIL + "' is not associated with a user account, and the current user does not have permission to send to it.", executeEmailScript(PasswordUtil.getUsername(), EMAIL_SUBJECT_ERROR, new String[]{NON_USER_EMAIL}, EMAIL_BODY_PLAIN, EMAIL_BODY_HTML));
            assertEquals("sendMessage API using principal ids instead of emails", "Use of principalId is allowed only for server side scripts", executeEmailScript(PasswordUtil.getUsername(), EMAIL_SUBJECT_ERROR, new String[]{"-1", "-2"}, EMAIL_BODY_PLAIN, EMAIL_BODY_HTML));
            assertEquals("sendMessage API from non-user", "The email address '" + NON_USER_EMAIL + "' is not associated with a user account, and the current user does not have permission to send to it.",
                    executeEmailScript(NON_USER_EMAIL, EMAIL_SUBJECT_ERROR, new String[]{EMAIL_RECIPIENT1}, null, EMAIL_BODY_HTML));

            assertEquals("sendMessage API with all fields", "success", executeEmailScript(PasswordUtil.getUsername(), EMAIL_SUBJECT_ALL, EMAIL_RECIPIENTS, EMAIL_BODY_PLAIN, EMAIL_BODY_HTML));
            assertEquals("sendMessage API without HTML body", "success", executeEmailScript(PasswordUtil.getUsername(), EMAIL_SUBJECT_PLAIN, EMAIL_RECIPIENTS, EMAIL_BODY_PLAIN, null));
            assertEquals("sendMessage API without plain text body", "success", executeEmailScript(PasswordUtil.getUsername(), EMAIL_SUBJECT_HTML, EMAIL_RECIPIENTS, null, EMAIL_BODY_HTML));
            assertEquals("sendMessage API without subject", "success", executeEmailScript(PasswordUtil.getUsername(), null, EMAIL_RECIPIENTS, null, EMAIL_BODY_HTML));
        }
        stopImpersonating();

        assertEquals("sendMessage API to non-user as site admin", "success", executeEmailScript(PasswordUtil.getUsername(), EMAIL_SUBJECT_NON_USER, new String[]{NON_USER_EMAIL}, EMAIL_BODY_PLAIN, EMAIL_BODY_HTML));

        goToModule("Dumbster");
        EmailRecordTable mailTable = new EmailRecordTable(this);
        assertTextNotPresent(EMAIL_SUBJECT_ERROR);
        EmailRecordTable.EmailMessage emailMessage;
        emailMessage = mailTable.getMessage(EMAIL_SUBJECT_ALL);
        assertEquals("Wrong views available for: " + EMAIL_SUBJECT_ALL, Arrays.asList("HTML", "Text"), emailMessage.getViews());
        emailMessage = mailTable.getMessage(EMAIL_SUBJECT_PLAIN);
        assertEquals("Wrong views available for: " + EMAIL_SUBJECT_PLAIN, Arrays.asList("Text"), emailMessage.getViews());
        emailMessage = mailTable.getMessage(EMAIL_SUBJECT_HTML);
        assertEquals("Wrong views available for: " + EMAIL_SUBJECT_HTML, Arrays.asList("HTML"), emailMessage.getViews());
        emailMessage = mailTable.getMessage("");
        assertEquals("Wrong recipients for email with blank subject", Arrays.asList(EMAIL_RECIPIENTS), Arrays.asList(emailMessage.getTo()));
        emailMessage = mailTable.getMessage(EMAIL_SUBJECT_NON_USER);
        assertEquals("Wrong recipient for: " + EMAIL_SUBJECT_NON_USER, Arrays.asList(NON_USER_EMAIL), Arrays.asList(emailMessage.getTo()));
        assertEquals("Wrong email count", 5, mailTable.getEmailCount());


        log("Verify behavior for ");
        final String FILTER_USER_SUBJECT = "Should exclude never-logged-in recipient: " + EMAIL_NO_LOGIN;
        _userHelper.createUserAndNotify(EMAIL_NO_LOGIN);  // This will create a user but will not log them in.

        enableEmailRecorder(); // Clear previously verified emails and new user notifications

        impersonate(EMAIL_SENDER);
        {
            assertEquals("A user who has never signed in should be filtered out of the recipient list.", "success",
                    executeEmailScript(PasswordUtil.getUsername(), FILTER_USER_SUBJECT, new String[]{EMAIL_RECIPIENT1, EMAIL_NO_LOGIN, EMAIL_RECIPIENT2}, null, EMAIL_BODY_HTML));
        }
        stopImpersonating();

        // The next test will cause server errors, so check first if there are any
        checkErrors();

        impersonate(EMAIL_SENDER);
        {
            assertEquals("If the recipient list only contains users who have been filtered out, we should fail.", "Error sending email: No recipient addresses",
                    executeEmailScript(PasswordUtil.getUsername(), EMAIL_SUBJECT_ERROR, new String[]{EMAIL_NO_LOGIN}, null, EMAIL_BODY_HTML));
            assertEquals("Attempting to send from a user that hasn't logged in.", "success",
                    executeEmailScript(EMAIL_NO_LOGIN, EMAIL_SUBJECT_FROM_NEW_USER, new String[]{EMAIL_RECIPIENT1}, null, EMAIL_BODY_HTML));
        }
        stopImpersonating();
        assertTrue("We should have recorded a server side error if no recipients are present.", getServerErrors().contains("Error sending email: No recipient addresses"));
        checkExpectedErrors(1);

        signOut();
        assertEquals("api requires user in system for guests", "The current user does not have permission to use the SendMessage API.", executeEmailScript(PasswordUtil.getUsername(), EMAIL_SUBJECT_ERROR, new String[]{EMAIL_RECIPIENTS[1]}, EMAIL_BODY_PLAIN, EMAIL_BODY_HTML));
        signIn();

        goToModule("Dumbster");

        assertTextNotPresent(EMAIL_SUBJECT_ERROR);

        mailTable = new EmailRecordTable(this);
        emailMessage = mailTable.getMessage(FILTER_USER_SUBJECT);
        assertEquals(FILTER_USER_SUBJECT, Arrays.asList(EMAIL_RECIPIENT1, EMAIL_RECIPIENT2), Arrays.asList(emailMessage.getTo()));
        emailMessage = mailTable.getMessage(EMAIL_SUBJECT_FROM_NEW_USER);
        assertEquals(EMAIL_SUBJECT_FROM_NEW_USER, Arrays.asList(EMAIL_NO_LOGIN), Arrays.asList(emailMessage.getFrom()));

        assertEquals("Number of notification emails", 2, mailTable.getEmailCount());
    }

    private String executeEmailScript(String from, String subject, String[] recipients, String plainTxtBody, String htmlTxtBody)
    {
        final String emailScriptTemplate =
                "var callback = arguments[arguments.length - 1];" +
                "function errorHandler(errorInfo, options, responseObj)\n" +
                "{\n" +
                "   console.log(errorInfo);" +
                "   callback(errorInfo.exception);\n" +
                "}\n" +
                "\n" +
                "function onSuccess(result)\n" +
                "{\n" +
                "   console.log(result);" +
                "   callback('success');\n" +
                "}\n" +
                "LABKEY.Message.sendMessage({\n" +
                "   msgFrom: '%s',\n" +
                "   msgSubject: '%s',\n" +
                "   msgRecipients: [%s],\n" +
                "   msgContent: [%s],\n" +
                "   successCallback: onSuccess,\n" +
                "   errorCallback: errorHandler,\n" +
                "});";

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

        String emailScript = String.format(emailScriptTemplate, from, StringUtils.trimToEmpty(subject), recipientStr.toString(),
                contentStr.toString());

        return (String)((JavascriptExecutor) getDriver()).executeAsyncScript(emailScript);
    }

    @Test
    public void extIntegrationTest()
    {
        setSourceFromFile("extIntegrationTest.html", true);
        Locator loc = Locator.id(TEST_DIV_NAME);
        waitForText("Month of the Year");
        assertElementContains(loc, "Month of the Year");
    }

    @Test
    public void webDavAPITest()
    {
        setSourceFromFile("webdavTest.html", true);
        Locator loc = Locator.id(TEST_DIV_NAME);
        assertElementContains(loc, "Test Started");
        waitForElement(loc.containing("Test Complete"));
        List<String> errors = getTexts(Locators.labkeyError.findElements(getDriver()));
        assertTrue(String.join("\n", errors), errors.isEmpty());
        assertFalse("Unknown webDav API error", loc.findElement(getDriver()).getText().contains("ERROR"));
    }

    @Test @Ignore("WebDavFileSystem doesn't fire 'ready' event")
    public void webDavAPITestJS()
    {
        String script = TestFileUtils.getFileContents(TestFileUtils.getSampleData("api/webdavTest.js"));
        String scriptResult = "";
        try
        {
            scriptResult = (String)((JavascriptExecutor) getDriver()).executeAsyncScript(script);
        }
        catch (TimeoutException ex)
        {
            scriptResult = (String)(executeScript("return window.result;"));
        }

        String[] testResults = scriptResult.split("\n");

        for (String result : testResults)
        {
            log(result);
        }
        assertFalse(scriptResult.contains("ERROR"));
        assertTrue("WebDav test did not complete", scriptResult.contains("Test Complete"));
    }

    private static final String AUTOCOMPLETE_USER = "autocomplete1@clientapi.test";

    @Test
    public void usernameAutocompleteValuesTest()
    {
        Connection cn = getConnection(true);
        verifyAutocompletionResponse(cn, "security", "completeUser", false, true);
        verifyAutocompletionResponse(cn, "security", "completeUserRead", false, true);
        verifyAutocompletionResponse(cn, "announcements", "completeUser", false, true);

        // Impersonate a role that shouldn't be allowed to see emails, and shouldn't even be allowed the action for security & pipeline
        PostCommand command = new PostCommand("user", "impersonateRoles") {
            @Override
            public JSONObject getJsonObject()
            {
                JSONObject json = new JSONObject();
                json.put("roleNames", Arrays.asList("org.labkey.api.security.roles.EditorRole"));
                return json;
            }
        };
        try
        {
            command.execute(cn, "/" + getProjectName());
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Impersonation error", e);
        }
        verifyAutocompletionResponse(cn, "security", "completeUser", true, false);
        verifyAutocompletionResponse(cn, "security", "completeUserRead", true, true);
        verifyAutocompletionResponse(cn, "announcements", "completeUser", true, true);
        // Probably overkill to stop impersonating on the connection as it's about to go out of scope, but just to be safe...
        try
        {
            new Command("login", "logout").execute(cn, "/" + getProjectName());
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Stop Impersonating error", e);
        }
    }

    private Connection getConnection(boolean validPassword)
    {
        return new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), validPassword ? PasswordUtil.getPassword() : "bad connection password");
    }

    @LogMethod
    private void verifyAutocompletionResponse(Connection cn, String controller, String action, boolean displayNameOnly, boolean actionAllowed)
    {
        Command command = new Command(controller, action);
        Map<String, Object> completionValues;
        String errMsg = "for controller " + controller + ", displayNameOnly == " + Boolean.toString(displayNameOnly);
        try
        {
            completionValues = command.execute(cn, "/" + getProjectName()).getParsedData();
        }
        catch (IOException | CommandException e)
        {
            if (e instanceof CommandException && !actionAllowed && ((CommandException) e).getStatusCode() == 403)
                return; // This is OK. Properly validated action wasn't allowed
            throw new RuntimeException("Command execution error " + errMsg, e);
        }

        // Awful lot of casting going on here, but if any of it fails that's indication we've unintentionally
        // changed this API response format.
        JSONArray entries = (JSONArray)completionValues.get("completions");
        assertTrue("No autocompletion entries returned" + errMsg, entries.size() > 0);
        boolean testPassed = false;
        String displayName = _userHelper.getDisplayNameForEmail(AUTOCOMPLETE_USER);
        for (JSONObject entry : (List<JSONObject>)entries)
        {
            // The order in the response isn't guaranteed. Loop to find one we know should be in the list.
            String responseValue = (String)entry.get("value");
            if (StringUtils.startsWith(responseValue, displayName))
            {
                String correctValue = displayNameOnly ? displayName : AUTOCOMPLETE_USER + " (" + displayName + ")";
                assertEquals("Incorrect autocomplete value from for user " + AUTOCOMPLETE_USER + errMsg, correctValue, responseValue);
                testPassed = true;
                break;
            }
        }
        assertTrue("No autocomplete value found for " + AUTOCOMPLETE_USER + errMsg, testPassed);
    }

    @Test
    public void contentTypeResponseTest()
    {
        // 30509: For 401 and 404, respond with same ContentType as request
        log("Testing response content type for 404");
        Connection cn = getConnection(true);
        Command command = new Command("bam", "boozled");

        Map<String, Object> expectedProps = new HashMap<>();
        expectedProps.put("success", false);
        expectedProps.put("exception", "No LabKey Server module registered to handle request for controller: bam");

        runCommand(cn, command, "application/json", 404, expectedProps);
        runCommand(cn, command, "text/xml", 404, expectedProps);
        runCommand(cn, command, "text/html", 404, null);

        // An API location/command that requires permissions
        log("Testing response content type for 401");
        cn = getConnection(false);
        command = new Command("core", "getExtContainerAdminTree.api");

        expectedProps = new HashMap<>();
        expectedProps.put("success", false);
        expectedProps.put("exception", "User does not have permission to perform this operation");

        runCommand(cn, command, "application/json", 401, null);
        runCommand(cn, command, "text/xml", 401, null);
        runCommand(cn, command, "text/html", 401, null);
    }

    /**
     * Run a Command that makes a request using the specified contentType. That contentType, expectedStatus,
     * and expectedProps are checked against the response to validate if an expected response was received.
     */
    private void runCommand(Connection cn, Command source, String contentType, int expectedStatus, @Nullable Map<String, Object> expectedProps)
    {
        CommandException exception = null;

        try
        {
            new Command(source)
            {
                @Override
                protected HttpUriRequest getHttpRequest(Connection connection, String folderPath) throws CommandException, URISyntaxException
                {
                    HttpUriRequest request = super.getHttpRequest(connection, folderPath);
                    request.setHeader("Content-Type", contentType);

                    return request;
                }
            }.execute(cn, "/" + getProjectName());
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to runCommand", e);
        }
        catch (CommandException e)
        {
            exception = e;
        }

        if (exception != null)
        {
            String responseContentType = exception.getContentType() == null ? "null" : exception.getContentType();

            assertEquals("Expected status code to match", expectedStatus, exception.getStatusCode());
            assertTrue("Expected contentType to match", responseContentType.contains(contentType));

            // Command only supports parsing the body of a JSON response.
            if ("application/json".equalsIgnoreCase(contentType) && expectedProps != null && expectedProps.size() > 0)
            {
                Map<String, Object> body = exception.getProperties();

                assertTrue("Expected properties in the response", body != null);
                for (Map.Entry<String, Object> prop : expectedProps.entrySet())
                {
                    assertTrue("Expected body property not available", body.containsKey(prop.getKey()));
                    assertTrue("Expected body value for \"" + prop.getKey() + "\"", prop.getValue().equals(body.get(prop.getKey())));
                }
            }
        }
        else
        {
            throw new RuntimeException("runCommand currently expects the command to fail. Valid responses not yet supported.");
        }
    }

    @Test
    public void suggestedColumnsInQueryDetailsTest() throws Exception
    {
        final String PATH = getProjectName() + "/" + FOLDER_NAME;
        final String SCHEMA = "vehicle";
        final String QUERY = "UserDefinedEmissions";
        beginAt(PATH);
        _containerHelper.enableModules(Collections.singletonList("simpletest"));
        Connection cn = getConnection(true);

        log("Verify default behavior to include suggested columns in details of user defined queries.");
        List<GetQueryDetailsResponse.Column> columns = new GetQueryDetailsCommand(SCHEMA, QUERY).execute(cn, PATH).getColumns();
        assertTrue("Sugggested column 'Container' for user defined query was missing in response.",
                columns.stream().anyMatch(col -> col.getName().equalsIgnoreCase("Container")));
        log("Verify optional behavior to exclude suggested columns from details of user defined queries.");
        columns = new GetQueryDetailsCommand(SCHEMA, QUERY, false).execute(cn, PATH).getColumns();
        assertTrue("Sugggested column 'Container' for user defined query was included in response.",
                columns.stream().noneMatch(col -> col.getName().equalsIgnoreCase("Container")));
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
