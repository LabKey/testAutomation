/*
 * Copyright (c) 2010-2018 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.reports.SaveCategoriesCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.study.CreateStudyPage;
import org.labkey.test.util.APITestHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.StudyHelper;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.core.webdav.WebDavUploadHelper;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@Category({DailyB.class})
@BaseWebDriverTest.ClassTimeout(minutes = 8)
public class RlabkeyTest extends BaseWebDriverTest
{
    RReportHelper _rReportHelper = new RReportHelper(this);
    //private static final String PROJECT_NAME = "RlabkeyTest Project\u2603";
    private static final String PROJECT_NAME = "RlabkeyTest Project";
    private static final String PROJECT_NAME_2 = PROJECT_NAME + "2";
    private static final String LIST_NAME = "AllTypes";
    private static final String LIBPATH_OVERRIDE = ".libPaths(\"%s\")";
    private static final String FOLDER_NAME = "RlabkeyTest";
    private static final String ISSUE_TITLE_0 = "Rlabkey: Issue at the Project level";
    private static final String ISSUE_TITLE_1 = "Rlabkey: Issue in the subfolder";
    private static final String ISSUE_TITLE_2 = "Rlabkey: Issue in another project";
    private static final String USER = "rlabkey_user@rlabkey.test";
    private static final String ISSUE_LIST_NAME = "rlabkeyissues";
    private static final File RLABKEY_API = TestFileUtils.getSampleData("api/rlabkey-api.xml");
    private static final File RLABKEY_API_EXPERIMENT = TestFileUtils.getSampleData("api/rlabkey-api-experiment.xml");
    private static final File RLABKEY_API_ISSUES = TestFileUtils.getSampleData("api/rlabkey-api-issues.xml");
    private static final File RLABKEY_API_LIST = TestFileUtils.getSampleData("api/rlabkey-api-list.xml");
    private static final File RLABKEY_API_QUERY = TestFileUtils.getSampleData("api/rlabkey-api-query.xml");
    private static final File RLABKEY_API_STUDY = TestFileUtils.getSampleData("api/rlabkey-api-study.xml");
    private static final File RLABKEY_API_WEBDAV = TestFileUtils.getSampleData("api/rlabkey-api-webdav.xml");

    @BeforeClass
    public static void setupProject()
    {
        RlabkeyTest init = (RlabkeyTest)getCurrentTest();
        init.doInit();
    }

    public void doInit()
    {
        _rReportHelper.ensureRConfig();

        _containerHelper.createProject(PROJECT_NAME, "Study");
        CreateStudyPage createStudyPage = _studyHelper.startCreateStudy();
        createStudyPage.setLabel("Rlabkey Study")
                .setTimepointType(StudyHelper.TimepointType.VISIT)
                .createStudy();

        _containerHelper.createProject(PROJECT_NAME_2, null);
        _containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME);
        new ApiPermissionsHelper(this).checkInheritedPermissions();
    }

    // create an issues list in projects and subfolder to test ContainerFilters.
    @LogMethod
    private void setupIssues()
    {
        log("Create user to assign issues to");
        _userHelper.createUser(USER);
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addUserToProjGroup(USER, PROJECT_NAME_2, "Users");
        apiPermissionsHelper.addMemberToRole("Users", "Editor", PermissionsHelper.MemberType.group, PROJECT_NAME_2);
        apiPermissionsHelper.addUserToProjGroup(USER, PROJECT_NAME, "Users");
        apiPermissionsHelper.addMemberToRole("Users", "Editor", PermissionsHelper.MemberType.group, PROJECT_NAME);
        IssuesHelper issuesHelper = new IssuesHelper(this);

        // create an site wide issues list
        clickProject("Shared");
        issuesHelper.createNewIssuesList(ISSUE_LIST_NAME, _containerHelper);

        clickProject(PROJECT_NAME);
        createNewIssueList(issuesHelper, ISSUE_LIST_NAME);
        issuesHelper.addIssue(Maps.of("assignedTo", _userHelper.getDisplayNameForEmail(USER), "title", ISSUE_TITLE_0));

        clickFolder(FOLDER_NAME);
        createNewIssueList(issuesHelper, ISSUE_LIST_NAME);
        issuesHelper.addIssue(Maps.of("assignedTo", _userHelper.getDisplayNameForEmail(USER), "title", ISSUE_TITLE_1));

        clickProject(PROJECT_NAME_2);
        createNewIssueList(issuesHelper, ISSUE_LIST_NAME);
        issuesHelper.addIssue(Maps.of("assignedTo", _userHelper.getDisplayNameForEmail(USER), "title", ISSUE_TITLE_2));
    }

    /**
     * Create a new issues list and override the default assigned to group
     */
    private void createNewIssueList(IssuesHelper issuesHelper, String name)
    {
        issuesHelper.createNewIssuesList(name, _containerHelper);

        goToModule("Issues");
        issuesHelper.goToAdmin();
        issuesHelper.setIssueAssignmentList(null);
        clickButton("Save");
    }

    @Test
    public void testRlabkey() throws Exception
    {
        doRLabkeyTest(RLABKEY_API);
    }

    @Test
    public void testRlabkeyExperimentApi() throws Exception
    {
        goToProjectHome();
        goToModule("FileContent");
        // Dummy files for saveBatch API test
        _fileBrowserHelper.createFolder("data.tsv");
        _fileBrowserHelper.createFolder("result.txt");

        doRLabkeyTest(RLABKEY_API_EXPERIMENT);
    }

    @Test
    public void testRlabkeyIssuesApi() throws Exception
    {
        doRLabkeyTest(RLABKEY_API_ISSUES);
    }

    @Test
    public void testRlabkeyListApi() throws Exception
    {
        doRLabkeyTest(RLABKEY_API_LIST);
    }

    @Test
    public void testRlabkeyQueryApi() throws Exception
    {
        log("Import Lists");
        File listArchive = new File(_rReportHelper.getRLibraryPath(), "/listArchive.zip");
        goToProjectHome();
        _listHelper.importListArchive(listArchive);

        setupIssues();
        doRLabkeyTest(RLABKEY_API_QUERY);
    }

    @Test
    public void testRlabkeyStudyApi() throws Exception
    {
        createCategoriesViaApi();

        doRLabkeyTest(RLABKEY_API_STUDY);
    }

    @Test @Ignore("Coming in RLabKey 2.2.6")
    public void testRlabkeyWebDavApi() throws Exception
    {
        Map<String, String> scriptReplacements = new HashMap<>();

        WebDavUploadHelper webDav = new WebDavUploadHelper(getProjectName());

        // Setup dir to simulate local R environment
        File downloadDir = new File(TestFileUtils.getDefaultFileRoot(getProjectName()), "webdav_download");
        webDav.mkDir(downloadDir.getName());
        scriptReplacements.put("downloadDir", downloadDir.getAbsolutePath());

        // Setup files in simulated target server
        webDav.putRandomBytes("remote/readChecks/getMe.txt");
        webDav.putRandomBytes("remote/readChecks/dir_a/a.txt");
        webDav.mkDir("remote/readChecks/empty_dir");
        webDav.mkDir("remote/writeChecks");
        webDav.mkDir("remote/writeChecks/deleteMe_empty");
        webDav.putRandomBytes("remote/writeChecks/deleteMe/file.txt");
        webDav.putRandomBytes("remote/writeChecks/deleteMe.txt");
        File remoteDir = new File(downloadDir.getParentFile(), "remote");
        scriptReplacements.put("remoteDir", remoteDir.getAbsolutePath());

        doRLabkeyTest(RLABKEY_API_WEBDAV, scriptReplacements);
    }

    private void doRLabkeyTest(File testData) throws Exception
    {
        doRLabkeyTest(testData, Collections.emptyMap());
    }

    private void doRLabkeyTest(File testData, Map<String, String> scriptReplacements) throws Exception
    {
        // cheating here, to use the api test framework to store rlabkey tests
        List<APITestHelper.ApiTestCase> tests = APITestHelper.parseTests(testData, false);

        if (!tests.isEmpty())
        {
            clickProject(getProjectName());
            goToManageViews().clickAddReport("R Report");

            // we want to load the Rlabkey package from the override location
            File libPath = _rReportHelper.getRLibraryPath();
            String pathCmd = String.format(LIBPATH_OVERRIDE, libPath.getAbsolutePath().replaceAll("\\\\", "/"));

            for (APITestHelper.ApiTestCase test : tests)
            {
                StringBuilder sb = new StringBuilder(pathCmd);

                sb.append('\n');
                String testScript = test.getUrl().trim()
                        .replaceAll("%baseUrl%", WebTestHelper.getBaseURL())
                        .replaceAll("%projectName%", getProjectName());
                if (WebTestHelper.getBaseURL().startsWith("https")) // Allow self-signed certificate
                    testScript = testScript.replace("library(Rlabkey)", "library(Rlabkey)\nlabkey.acceptSelfSignedCerts()");
                String verify = test.getResponse().trim().replaceAll("%projectName%", getProjectName());
                for (String key : scriptReplacements.keySet())
                {
                    String token = "%" + key + "%";
                    String replacement = scriptReplacements.get(key);
                    testScript = testScript.replaceAll(token, replacement);
                    verify = verify.replaceAll(token, replacement);
                }
                sb.append(testScript);

                log("execute test: " + test.getName());
                TestLogger.increaseIndent();
                final boolean success = _rReportHelper.executeScript(sb.toString(), verify);
                TestLogger.decreaseIndent();
                assertTrue("Failed executing R script for test case: " + test.getName(), success);
            }
            _rReportHelper.clickSourceTab();
            clickButton("Cancel");
        }
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        // delete the shared issue list definition
        IssuesHelper issuesHelper = new IssuesHelper(this);
        issuesHelper.deleteIssueLists("Shared", this);

        _containerHelper.deleteProject(getProjectName(), afterTest);
        _containerHelper.deleteProject(PROJECT_NAME_2, afterTest);
        _userHelper.deleteUsers(afterTest, USER);
    }

    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    private void createCategoriesViaApi() throws Exception
    {
        Connection connection = WebTestHelper.getRemoteApiConnection();
        SaveCategoriesCommand command = new SaveCategoriesCommand();
        command.setCategories("Control Group A", "Control Group B");

        command.execute(connection, getProjectName());
    }
}
