/*
 * Copyright (c) 2011-2019 LabKey Corporation
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
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimpleGetCommand;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.remoteapi.query.GetQueryDetailsCommand;
import org.labkey.remoteapi.query.GetQueryDetailsResponse;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.study.CreateStudyPage;
import org.labkey.test.pages.study.ManageStudyPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.StudyHelper;
import org.labkey.test.util.TestDataUtils;
import org.labkey.test.util.UIUserHelper;
import org.labkey.test.util.WikiHelper;
import org.labkey.test.util.query.QueryUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.WebTestHelper.getHttpResponse;

@Category({BVT.class})
@BaseWebDriverTest.ClassTimeout(minutes = 14)
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
    private static final String ASSAY_STUDY_FOLDER = "generalAssayFolder";
    private static final String VISIT_STUDY_NAME = "visitStudyName";
    public final static String LIST_NAME = "People";
    private final static String QUERY_LIST_NAME = "NewPeople";
    private final static File TEST_XLS_DATA_FILE = TestFileUtils.getSampleData("dataLoading/excel/ClientAPITestList.xls");
    private final static String SUBFOLDER_LIST = "subfolderList"; // for cross-folder query test
    private static final String OTHER_PROJECT_LIST = "otherProjectList"; // for cross-project query test
    // Add tricky characters to assay name to check for regressions
    // Issue 36077: SelectRows: SchemaKey decoding of public schema name causes request failure
    protected static final String TEST_ASSAY = "TestAssay1" + TRICKY_CHARACTERS;
    protected static final String TEST_ASSAY_DESC = "Description for assay 1";
    private static final List<FieldDefinition> LIST_COLUMNS = List.of(
        new FieldDefinition("FirstName", FieldDefinition.ColumnType.String).setLabel("First Name").setDescription("The first name").setRequired(true),
        new FieldDefinition("LastName", FieldDefinition.ColumnType.String).setLabel("Last Name").setDescription("The last name").setRequired(true),
        new FieldDefinition("Age", FieldDefinition.ColumnType.Integer).setLabel("Age").setDescription("The age")
    );

    public final static String TEST_DATA =
        "Key\tFirstName\tLastName\tAge\n" +
        "1\tBill\tBillson\t34\n" +
        "2\tJane\tJaneson\t42\n" +
        "3\tJohn\tJohnson\t17\n" +
        "4\tMandy\tMandy;son\t32\n" +
        "5\tNorbert\tNorbertson\t28\n" +
        "6\tPenny\tPennyson\t38\n" +
        "7\tYak\tYakson\t88\n";

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

    private static final String DOMAIN_FIELDS =
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
            "       },{\n" +
            "           name : \"dateFieldOne\",\n" +
            "           rangeURI : \"dateTime\"\n" +
            "       }]\n";

    public static String getFullSource(String testFragment)
    {
        return SRC_PREFIX + "\n" + testFragment + "\n" + SRC_SUFFIX;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, EMAIL_API_USERS);
        _userHelper.deleteUser(AUTOCOMPLETE_USER);
        _containerHelper.deleteProject(PROJECT_NAME, afterTest);
        _containerHelper.deleteProject(OTHER_PROJECT, afterTest);
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        ClientAPITest init = (ClientAPITest)getCurrentTest();
        init._containerHelper.createProject(OTHER_PROJECT, null);
        init._containerHelper.createProject(PROJECT_NAME, null);

        init._containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME);
        init._containerHelper.createSubfolder(PROJECT_NAME, TIME_STUDY_FOLDER);
        init._containerHelper.createSubfolder(PROJECT_NAME, VISIT_STUDY_FOLDER);
        init._containerHelper.createSubfolder(PROJECT_NAME, ASSAY_STUDY_FOLDER);

        init._containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME, SUBFOLDER_NAME, "None", null);

        init.createStudies();

        init.clickFolder(FOLDER_NAME);

        init.createWiki();

        init.createLists();

        init.createUsers();
    }

    private static boolean dirtyList = false;

    @Before
    public void preTest() throws Exception
    {
        if (dirtyList)
        {
            refreshPeopleList(LIST_NAME, createDefaultConnection(), getProjectName() + "/" + FOLDER_NAME);
            dirtyList = false;
        }
        navigateToFolder(getProjectName(), FOLDER_NAME);
    }

    @NotNull
    public static ListDefinition getPeopleListDefinition(String listName)
    {
        ListDefinition listDef = new IntListDefinition(listName, "Key");
        listDef.setFields(LIST_COLUMNS);
        return listDef;
    }

    @Override
    protected void checkLinks()
    {
        //delete the test page so the crawler doesn't refetch a test and cause errors
        getHttpResponse(WebTestHelper.buildURL("wiki", getProjectName() + "/" + FOLDER_NAME, "delete", Maps.of("name", WIKIPAGE_NAME)), "POST");
        super.checkLinks();
    }

    @LogMethod
    private void createLists() throws Exception
    {
        Connection connection = createDefaultConnection();
        createPeopleList(LIST_NAME, connection, PROJECT_NAME + "/" + FOLDER_NAME);
        createLargePeopleList();

        // Create lists for cross-folder query test.
        createPeopleList(SUBFOLDER_LIST, connection, PROJECT_NAME + "/" + FOLDER_NAME + "/" + SUBFOLDER_NAME);
        createPeopleList(OTHER_PROJECT_LIST, connection, OTHER_PROJECT);
    }

    @LogMethod
    public static void createPeopleList(String listName, Connection connection, @LoggedParam String containerPath) throws Exception
    {
        ListDefinition listDef = getPeopleListDefinition(listName);
        listDef.create(connection, containerPath);
        refreshPeopleList(listName, connection, containerPath);
    }

    private static void refreshPeopleList(String listName, Connection connection, String containerPath) throws Exception
    {
        QueryUtils.truncateTable(containerPath, "lists", listName);
        final List<Map<String, Object>> rowData = TestDataUtils.rowMapsFromTsv(TEST_DATA);
        final InsertRowsCommand insertCommand = new InsertRowsCommand("lists", listName);
        insertCommand.setRows(rowData);
        insertCommand.execute(connection, containerPath);
    }

    private void createLargePeopleList()
    {
        // Create Larger list for query test.
        _listHelper.createListFromFile(getProjectName() + "/" + FOLDER_NAME, QUERY_LIST_NAME, TEST_XLS_DATA_FILE);
        _listHelper.goToList(QUERY_LIST_NAME);
        waitForElement(Locator.linkWithText("Norbert"));
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
    public void validateStudyDatasetVisitWithTimeKeyField()
    {
        // this test case attempts to create a time-keyed dataset in a visit-based study folder, which should
        // fail on create.
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String dataSetName = "ThisShouldBlowUp";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                DOMAIN_FIELDS +
                "   },\n" +
                "   options : {\n" +
                "       useTimeKeyField : true,\n" +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";

        Map<String, Object> error = (Map<String,Object>)executeAsyncScript(create);
        assertEquals("Unexpected error message", "Additional key property cannot be Time (from Date/Time) for visit based studies.", error.get("exception"));
    }

    @Test
    public void validateStudyDatasetVisitDomainKindInDateBasedStudy()
    {
        // make sure you can't use StudyDatasetVisit domain kind in a date-based study
        projectMenu().navigateToFolder(getProjectName(), TIME_STUDY_FOLDER);
        String dataSetName = "ThisShouldBlowUp";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                DOMAIN_FIELDS +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";

        Map<String, Object> error = (Map<String,Object>)executeAsyncScript(create);
        assertEquals("Unexpected error message", "Date based studies require a date based dataset domain. Please specify a kind name of : StudyDatasetDate.", error.get("exception"));
    }

    @Test
    public void validateStudyDatasetDateDomainKindInVisitBasedStudy()
    {
        // make sure you can't use StudyDatasetDate domain kind in a visit-based study
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String dataSetName = "ThisShouldBlowUp";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetDate\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                DOMAIN_FIELDS +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";

        Map<String, Object> error = (Map<String,Object>)executeAsyncScript(create);
        assertEquals("Unexpected error message", "Visit based studies require a visit based dataset domain. Please specify a kind name of : StudyDatasetVisit.", error.get("exception"));
    }

    @Test
    public void validateUseTimeKeyFieldWithKeyPropertyName()
    {
        // make sure you can't set the useTimeKeyField and keyPropertyName at the same time
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String dataSetName = "ThisShouldBlowUp";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                DOMAIN_FIELDS +
                "   },\n" +
                "   options : {\n" +
                "       keyPropertyName : 'intFieldOne',\n" +
                "       useTimeKeyField : true,\n" +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";

        Map<String, Object> error = (Map<String,Object>)executeAsyncScript(create);
        assertEquals("Unexpected error message", "KeyPropertyName should not be provided when using additional key of Time (from Date/Time).", error.get("exception"));
    }

    @Test
    public void validateUseTimeKeyFieldWithKeyPropertyManaged()
    {
        // make sure you can't set the useTimeKeyField and keyPropertyName at the same time
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String dataSetName = "ThisShouldBlowUp";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                DOMAIN_FIELDS +
                "   },\n" +
                "   options : {\n" +
                "       keyPropertyName : 'intFieldOne',\n" +
                "       keyPropertyManaged : true,\n" +
                "       useTimeKeyField : true,\n" +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";

        Map<String, Object> error = (Map<String,Object>)executeAsyncScript(create);
        assertEquals("Unexpected error message", "Additional key cannot be a managed field if KeyPropertyName is Time (from Date/Time).", error.get("exception"));
    }

    @Test
    public void validateDemographicDataWithKeyPropertyName()
    {
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String dataSetName = "ThisShouldBlowUp";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                DOMAIN_FIELDS +
                "   },\n" +
                "   options : {\n" +
                "       keyPropertyName : 'intFieldOne',\n" +
                "       demographicData : true\n" +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";

        Map<String, Object> error = (Map<String,Object>)executeAsyncScript(create);
        assertEquals("Unexpected error message", "There cannot be an Additional Key Column if the dataset is Demographic Data.", error.get("exception"));
    }

    @Test
    public void validateKeyPropertyManagedDataType()
    {
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String dataSetName = "ThisShouldBlowUp";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                DOMAIN_FIELDS +
                "   },\n" +
                "   options : {\n" +
                "       keyPropertyName : 'dateFieldOne',\n" +
                "       keyPropertyManaged : true\n" +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";

        Map<String, Object> error = (Map<String,Object>)executeAsyncScript(create);
        assertEquals("Unexpected error message", "If Additional Key Column is managed, the column type must be numeric or text-based.", error.get("exception"));
    }

    @Test
    public void validateKeyPropertyManagedWithoutKeyPropertyName()
    {
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String dataSetName = "ThisShouldBlowUp";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                DOMAIN_FIELDS +
                "   },\n" +
                "   options : {\n" +
                "       keyPropertyManaged : true\n" +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";

        Map<String, Object> error = (Map<String,Object>)executeAsyncScript(create);
        assertEquals("Unexpected error message", "Additional Key Column name must be specified if field is managed.", error.get("exception"));
    }

    @Test
    public void validateKeyPropertyNameExistsInDomain()
    {
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String dataSetName = "ThisShouldBlowUp";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                DOMAIN_FIELDS +
                "   },\n" +
                "   options : {\n" +
                "       keyPropertyName : 'dateFieldOneDoesNotExist'\n" +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";

        Map<String, Object> error = (Map<String,Object>)executeAsyncScript(create);
        assertEquals("Unexpected error message", "Additional Key Column name \"dateFieldOneDoesNotExist\" must be the name of a column.", error.get("exception"));
    }

    @Test
    public void validateKeyPropertyNameIsNotBlank()
    {
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String dataSetName = "ThisShouldBlowUp";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                DOMAIN_FIELDS +
                "   },\n" +
                "   options : {\n" +
                "       keyPropertyName : ''\n" +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";

        Map<String, Object> error = (Map<String,Object>)executeAsyncScript(create);
        assertEquals("Unexpected error message", "Please select a field name for the additional key.", error.get("exception"));
    }

    @Test
    public void validateDatasetDuplicateName()
    {
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String dataSetName = "DatasetDuplicateNameCheck";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";

        executeAsyncScript(create); // first call should succeed and create the dataset
        Map<String, Object> error = (Map<String,Object>)executeAsyncScript(create);
        assertEquals("Unexpected error message", "A Dataset or Query already exists with the name \"" + dataSetName + "\".", error.get("exception"));
    }

    @Test
    public void validateDatasetDuplicateLabel()
    {
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String dataSetName = "DatasetDuplicateLabelCheck";
        String dataSetLabel = "Dataset Duplicate Label Check";
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                DOMAIN_FIELDS +
                "   },\n" +
                "   options : {\n" +
                "       label : '"+dataSetLabel+"',\n" +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";

        executeAsyncScript(create); // first call should succeed and create the dataset

        dataSetName = "DatasetDuplicateLabelCheck2"; // need to change the name so that validation check passes
        create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '"+dataSetName+"',\n" +
                DOMAIN_FIELDS +
                "   },\n" +
                "   options : {\n" +
                "       label : '"+dataSetLabel+"',\n" +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";
        Map<String, Object> error = (Map<String,Object>)executeAsyncScript(create);
        assertEquals("Unexpected error message", "A Dataset already exists with the label \"" + dataSetLabel + "\".", error.get("exception"));
    }

    @Test
    public void validateDatasetNameIsNotBlank()
    {
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : '',\n" +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";

        Map<String, Object> error = (Map<String,Object>)executeAsyncScript(create);
        assertEquals("Unexpected error message", "Dataset name cannot be empty.", error.get("exception"));
    }

    @Test
    public void validateDatasetNameMaxLength()
    {
        // max length allowed should be 200, this name's length is 201
        projectMenu().navigateToFolder(getProjectName(), VISIT_STUDY_FOLDER);
        String create = "LABKEY.Domain.create({\n" +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : 'Test567890Test567890Test567890Test567890Test567890Test567890Test567890Test567890Test567890Test567890Test567890Test567890Test567890Test567890Test567890Test567890Test567890Test567890Test567890Test567890X',\n" +
                "   },\n" +
                "   success: function(data){callback(data); },\n" +
                "   failure: function(e){callback(e); }\n" +
                "});\n";

        Map<String, Object> error = (Map<String,Object>)executeAsyncScript(create);
        assertEquals("Unexpected error message", "Dataset name must be under 200 characters.", error.get("exception"));
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
                DOMAIN_FIELDS +
                "   },\n" +
                "   options : {\n" +
                "       keyPropertyName : 'intFieldOne',\n" +
                "       useTimeKeyField : false,\n" +
                "       demographics : false\n" +
                "   },\n" +
                "   success: callback," +
                "   failure: callback"+
                "});\n";

        Map<String, Object> createResult = (Map<String,Object>)executeAsyncScript(create);
        List<Map<String, Object>> fields = (List<Map<String, Object>>)createResult.get("fields");
        assertTrue(fields.size() == 6);
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
        assertTrue(updatedFields.size() == 7);
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
                DOMAIN_FIELDS +
                "   },\n" +
                "   options : {\n" +
                "       keyPropertyName : 'intFieldOne',\n" +
                "   },\n" +
                "   success: callback," +
                "   failure: callback"+
                "});\n";

        Map<String, Object> createResult = (Map<String,Object>)executeAsyncScript(create);
        List<Map<String, Object>> fields = (List<Map<String, Object>>)createResult.get("fields");
        assertEquals(6, fields.size());
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
        assertEquals(7, updatedFields.size());
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
        String paginationText = Locator.byClass("xtb-text").containing("Displaying")
                .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT).getText();
        assertEquals("Wrong pagination for ext grid", "Displaying 1 - 2 of 6", paginationText);
        assertTextPresent("Janeson");
        assertTextNotPresent("Johnson");
    }

    @Test
    public void createDataset()
    {
        projectMenu().navigateToFolder(PROJECT_NAME, VISIT_STUDY_FOLDER);
        String create = "LABKEY.Domain.create({\n" +
                "   success: callback," +
                "   error: callback," +
                "   kind: \"StudyDatasetVisit\",\n" +
                "   domainDesign: {\n" +
                "       name : 'My Test Dataset',\n" +
                DOMAIN_FIELDS +
                "   },\n" +
                "   options : {\n" +
                "       datasetId : 81005\n" +
                "   }\n" +
                "});\n";
        executeAsyncScript(create);
        log(create);
    }

    @Test
    public void createAssayUsingDomainAPI()
    {
        log("Adding the assay list web part");
        projectMenu().navigateToFolder(PROJECT_NAME, ASSAY_STUDY_FOLDER);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Assay List");
        String assayName = "Assay from API";

        log("Creating the assay using save protocol");
        String SAVEPROTOCOL = "LABKEY.Ajax.request({                        \n" +
                "   method: 'POST',                                         \n" +
                "   success: function(data){                                \n" +
                "       callback('Success!');                               \n" +
                "   },                                                      \n" +
                "   failure: function(e){                                   \n" +
                "       callback(e.exception); },                           \n" +
                "   url: LABKEY.ActionURL.buildURL('assay', 'saveProtocol.api'), \n" +
                "   jsonData: {                                                  \n" +
                "        protocolId: undefined,                                  \n" +
                "        name: '" + assayName + "',                             \n" +
                "        providerName: 'General',                             \n" +
                "        domains: [                                           \n" +
                "            {                                                \n" +
                "                name: 'API Batch Fields',                    \n" +
                "                domainURI: 'urn:lsid:${LSIDAuthority}:AssayDomain-Batch.Folder-${Container.RowId}:Gen-Assay',\n" +
                "                fields: [                                     \n" +
                "                    {                                         \n" +
                "                        name: 'ParticipantVisitResolver',     \n" +
                "                        description: null,                    \n" +
                "                        rangeURI: 'http://www.w3.org/2001/XMLSchema#string'\n" +
                "                    },                                          \n" +
                "                    {                                           \n" +
                "                        name: 'TargetStudy',                  \n" +
                "                        description: null,                    \n" +
                "                        rangeURI: 'http://www.w3.org/2001/XMLSchema#string'\n" +
                "                    }                                           \n" +
                "                ]                                               \n" +
                "            },                                                  \n" +
                "            {                                                   \n" +
                "                name: 'API Run Fields',                       \n" +
                "                domainURI: 'urn:lsid:${LSIDAuthority}:AssayDomain-Run.Folder-${Container.RowId}:Gen-Assay',\n" +
                "                fields: []                                      \n" +
                "            },                                                  \n" +
                "            {                                                   \n" +
                "                name: 'API Data Fields',                        \n" +
                "                domainURI: 'urn:lsid:${LSIDAuthority}:AssayDomain-Data.Folder-${Container.RowId}:Gen-Assay',\n" +
                "                fields: [                                     \n" +
                "                    {                                         \n" +
                "                        name: 'SpecimenID',                   \n" +
                "                        rangeURI: 'http://www.w3.org/2001/XMLSchema#string'\n" +
                "                    }                                           \n" +
                "                  ]                                             \n" +
                "            }                                                   \n" +
                "        ]                                                       \n" +
                "    }                                                           \n" +
                "})";

        log(SAVEPROTOCOL);
        executeAsyncScript(SAVEPROTOCOL);

        log("Extracting the protocol ID from URL");
        navigateToFolder(PROJECT_NAME, ASSAY_STUDY_FOLDER);
        clickAndWait(Locator.linkWithText(assayName));
        String protocolID = getCurrentRelativeURL().split("=")[1];

        log("Getting the assay information from the protocolID");
        String GETPROTOCOL = "LABKEY.Ajax.request({                             \n" +
                "   method: 'GET',                                              \n" +
                "    success: function(){ callback('Success!'); },              \n" +
                "   url: LABKEY.ActionURL.buildURL('assay', 'getProtocol.api'), \n" +
                "   params : {                                                  \n" +
                "        protocolId: " + protocolID + "                         \n" +
                "    }                                                          \n" +
                "})";
        log(GETPROTOCOL);
        String result = (String) executeAsyncScript(GETPROTOCOL);
        assertEquals("JavaScript API failure.", "Success!", result);

        log("Deleting the assay using delete protocol");
        String DELETEPROTOCOL = "LABKEY.Ajax.request({                              \n" +
                "   method: 'POST',                                                 \n" +
                "   success: function(){ callback('Success!'); },                   \n" +
                "   url: LABKEY.ActionURL.buildURL('assay', 'deleteProtocol.api'),  \n" +
                "   jsonData: {                                                     \n" +
                "        protocolId: " + protocolID + "                             \n" +
                "    }                                                              \n" +
                "})";

        log(DELETEPROTOCOL);
        result = (String) executeAsyncScript(DELETEPROTOCOL);
        assertEquals("JavaScript API failure.", "Success!", result);

    }

    @Test
    public void webpartTest()
    {
        setSourceFromFile("webPartTest.js");

        assertTextPresent("Webpart Title");
        for (FieldDefinition column : LIST_COLUMNS)
            assertTextPresent(column.getLabel());
    }

    @Test
    public void assayTest()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Assay List");

        //copied from old test
        clickButton("Manage Assays");
        ReactAssayDesignerPage assayDesignerPage = _assayHelper.createAssayDesign("General", TEST_ASSAY)
            .setDescription(TEST_ASSAY_DESC);
        assayDesignerPage.goToRunFields()
            .addField("RunDate")
            .setType(FieldDefinition.ColumnType.DateAndTime)
            .setLabel("Run Date");
        assayDesignerPage.clickFinish();

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

        ManageStudyPage manageStudyPage = new ManageStudyPage(getDriver());
        DomainDesignerPage domainDesignerPage = manageStudyPage.clickEditAdditionalProperties();
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        domainFormPanel.addField("customfield1").setType(FieldDefinition.ColumnType.String).setLabel("Custom Field 1");
        domainFormPanel.addField("color").setType(FieldDefinition.ColumnType.String).setLabel("Color");
        domainDesignerPage.clickFinish();

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
        return setSource(TestFileUtils.getFileContents(TestFileUtils.getSampleData("api/" + fileName)), excludeTags);
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
        assertEquals("Wrong number of results", 24, testResults.length);
    }

    @Test
    public void queryRegressionTest()
    {
        String script = TestFileUtils.getFileContents(TestFileUtils.getSampleData("api/queryRegressionTest.js"));
        List<String> testResults = new ArrayList<>();
        // TODO: 35526: Ext4.Ajax.request doesn't trigger callbacks when invoked by Geckodriver
        int testCount = getDriver().getClass().isAssignableFrom(FirefoxDriver.class) ? 3 : 4;
        for (int i = 0; i < testCount; i++)
        {
            log("JavaScript test case #" + i);
            List<String> testResult = Arrays.asList(((String) executeAsyncScript(script, i)).trim().split("\n"));
            testResults.addAll(testResult);
            for (String line : testResult)
                log(line);
        }
        String scriptResult = String.join("\n", testResults);
        assertFalse(scriptResult, scriptResult.contains("ERROR"));
        assertEquals("Wrong number of results", testCount + 1, testResults.size());
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

        apiPermissionsHelper.addMemberToRole(EMAIL_SENDER, "Application Admin", PermissionsHelper.MemberType.user, "/");

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
        assertEquals("Wrong views available for: " + EMAIL_SUBJECT_ALL, Arrays.asList("HTML", "Text", "Raw"), emailMessage.getViews());
        emailMessage = mailTable.getMessage(EMAIL_SUBJECT_PLAIN);
        assertEquals("Wrong views available for: " + EMAIL_SUBJECT_PLAIN, Arrays.asList("Text", "Raw"), emailMessage.getViews());
        emailMessage = mailTable.getMessage(EMAIL_SUBJECT_HTML);
        assertEquals("Wrong views available for: " + EMAIL_SUBJECT_HTML, Arrays.asList("HTML", "Raw"), emailMessage.getViews());
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
        int count = getServerErrorCount();

        impersonate(EMAIL_SENDER);
        {
            assertEquals("If the recipient list only contains users who have been filtered out, we should fail.", "Error sending email: No recipient addresses",
                    executeEmailScript(PasswordUtil.getUsername(), EMAIL_SUBJECT_ERROR, new String[]{EMAIL_NO_LOGIN}, null, EMAIL_BODY_HTML));
            assertEquals("Attempting to send from a user that hasn't logged in.", "success",
                    executeEmailScript(EMAIL_NO_LOGIN, EMAIL_SUBJECT_FROM_NEW_USER, new String[]{EMAIL_RECIPIENT1}, null, EMAIL_BODY_HTML));
        }
        stopImpersonating();
        assertTrue("We should have recorded a server side error if no recipients are present.", getServerErrors().contains("No recipient addresses"));
        checkExpectedErrors(count + 1);

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
            if (NumberUtils.isCreatable(email))
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
    private String _autocompleteUserDisplayName = null;

    @Test
    public void usernameAutocompleteValuesTest()
    {
        _autocompleteUserDisplayName = _userHelper.getDisplayNameForEmail(AUTOCOMPLETE_USER);
        Connection cn = getConnection(true);
        verifyAutocompletionResponse(cn, "security", "completeUser", false, true);
        verifyAutocompletionResponse(cn, "security", "completeUserRead", false, true);
        verifyAutocompletionResponse(cn, "announcements", "completeUser", false, true);

        // Impersonate a role that shouldn't be allowed to see emails, and shouldn't even be allowed the action for security & pipeline
        SimplePostCommand command = new SimplePostCommand("user", "impersonateRoles") {
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
    }

    private Connection getConnection(boolean validPassword)
    {
        if (validPassword)
            return WebTestHelper.getRemoteApiConnection();
        else
            return new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), "bad connection password");
    }

    @LogMethod
    private void verifyAutocompletionResponse(Connection cn, String controller, String action, boolean displayNameOnly, boolean actionAllowed)
    {
        SimpleGetCommand command = new SimpleGetCommand(controller, action);
        Map<String, Object> completionValues;
        String errMsg = "for controller " + controller + ", displayNameOnly == " + displayNameOnly;
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

        // Awful lot of casting going on here, but if any of it fails that's an indication we've unintentionally
        // changed this API response format.
        List<Map<String, String>> list = (List<Map<String, String>>)completionValues.get("completions");
        assertFalse("No autocompletion entries returned" + errMsg, list.isEmpty());
        boolean testPassed = false;

        for (Map<String, String> map : list)
        {
            // The order in the response isn't guaranteed. Loop to find one we know should be in the list.
            String responseValue = map.get("value");
            if (StringUtils.startsWith(responseValue, _autocompleteUserDisplayName))
            {
                String correctValue = displayNameOnly ? _autocompleteUserDisplayName : AUTOCOMPLETE_USER + " (" + _autocompleteUserDisplayName + ")";
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
        SimpleGetCommand command = new SimpleGetCommand("bam", "boozled");

        final String bogusContainerPath = "/BOGUS---CONTAINER---PATH";

        runCommand(cn, command, "application/json", "text/html", 404, null);
        runCommand(cn, command, "text/xml", "text/html", 404, null);
        runCommand(cn, command, "text/html", "text/html", 404, null);
        runCommand(cn, command, "application/json", "text/html", 404, null);
        runCommand(cn, command, "text/html", "text/html", 404, null, bogusContainerPath);

        // An API location/command that requires permissions
        log("Testing response content type for 401");
        cn = getConnection(false);
        command = new SimpleGetCommand("core", "getExtContainerAdminTree.api");

        Map<String, Object> expectedProps = new HashMap<>();
        expectedProps.put("success", false);
        expectedProps.put("exception", "The email address and password you entered did not match any accounts on file.");

        validateUnauthorizedResponses(cn, command, expectedProps);

        command = new SimpleGetCommand("query", "selectRows.api");
        validateUnauthorizedResponses(cn, command, expectedProps);

        expectedProps.put("exception", "No such project: " + bogusContainerPath);

        // Reset command so that it's not requesting XML responses
        command = new SimpleGetCommand("query", "selectRows.api");

        // Check that a bad container path produces the right kind of response
        runCommand(cn, command, "application/json", "application/json", 404, expectedProps, bogusContainerPath);
        // Try again requesting an XML response
        command.setParameters(Map.of("respFormat", "xml"));
        runCommand(cn, command, "application/json", "text/xml", 404, null, bogusContainerPath);

        command = new SimpleGetCommand("query", "selectRows.api");
        // Check that an valid container with bogus parameters also does the right thing for selectRows
        expectedProps.put("exception", "The specified schema does not exist");
        cn = getConnection(true);
        runCommand(cn, command, "application/json", "application/json", 404, expectedProps);

        // Try again requesting an XML response
        command.setParameters(Map.of("respFormat", "xml"));
        runCommand(cn, command, "application/json", "text/xml", 404, null);
    }

    private void validateUnauthorizedResponses(Connection cn, SimpleGetCommand command, Map<String, Object> expectedProps)
    {
        runCommand(cn, command, "application/json", "application/json", 401, expectedProps);
        runCommand(cn, command, "text/xml", "application/json", 401, expectedProps);
        runCommand(cn, command, "text/html", "application/json", 401, expectedProps);

        command.setParameters(Map.of("respFormat", "xml"));

        runCommand(cn, command, "application/json", "text/xml", 401, expectedProps);
        runCommand(cn, command, "text/xml", "text/xml", 401, expectedProps);
        runCommand(cn, command, "text/html", "text/xml", 401, expectedProps);
    }

    /**
     * Run a Command that makes a request using the specified contentType. That contentType, expectedStatus,
     * and expectedProps are checked against the response to validate if an expected response was received.
     */
    private void runCommand(Connection cn, SimpleGetCommand source, String requestContentType, String expectedResponseContentType, int expectedStatus, @Nullable Map<String, Object> expectedProps)
    {
        runCommand(cn, source, requestContentType, expectedResponseContentType, expectedStatus, expectedProps, "/" + getProjectName());
    }

    private void runCommand(Connection cn, SimpleGetCommand source, String requestContentType, String expectedResponseContentType, int expectedStatus, @Nullable Map<String, Object> expectedProps, String folderPath)
    {
        CommandException exception = null;

        try
        {
            SimpleGetCommand cmd = new SimpleGetCommand(source.getControllerName(), source.getActionName())
            {
                @Override
                protected HttpGet getHttpRequest(Connection connection, String folderPath) throws URISyntaxException
                {
                    HttpGet request = super.getHttpRequest(connection, folderPath);
                    request.setHeader("Content-Type", requestContentType);

                    return request;
                }
            };
            cmd.setParameters(source.getParameters());
            cmd.execute(cn, folderPath);
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
            assertThat(responseContentType, containsString(expectedResponseContentType));

            // Command only supports parsing the body of a JSON response.
            if ("application/json".equalsIgnoreCase(expectedResponseContentType) && expectedProps != null && !expectedProps.isEmpty())
            {
                Map<String, Object> body = exception.getProperties();

                assertNotNull("Expected properties in the response", body);
                for (Map.Entry<String, Object> prop : expectedProps.entrySet())
                {
                    assertTrue("Expected body property not available: " + prop + ", available properties are: " + body.keySet(), body.containsKey(prop.getKey()));
                    assertEquals("Expected body value for \"" + prop.getKey() + "\"", prop.getValue(), body.get(prop.getKey()));
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
        assumeTestModules();

        final String PATH = getProjectName() + "/" + FOLDER_NAME;
        final String SCHEMA = "vehicle";
        final String QUERY = "UserDefinedEmissions";
        beginAt(PATH);
        _containerHelper.enableModules(Collections.singletonList("simpletest"));
        Connection cn = getConnection(true);

        log("Verify default behavior to include suggested columns in details of user defined queries.");
        List<GetQueryDetailsResponse.Column> columns = new GetQueryDetailsCommand(SCHEMA, QUERY).execute(cn, PATH).getColumns();
        assertTrue("Suggested column 'Container' for user defined query was missing in response.",
                columns.stream().anyMatch(col -> col.getName().equalsIgnoreCase("Container")));
        log("Verify optional behavior to exclude suggested columns from details of user defined queries.");
        columns = new GetQueryDetailsCommand(SCHEMA, QUERY, false).execute(cn, PATH).getColumns();
        assertTrue("Suggested column 'Container' for user defined query was included in response.",
                columns.stream().noneMatch(col -> col.getName().equalsIgnoreCase("Container")));
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
