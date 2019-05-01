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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.reports.SaveCategoriesCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.study.CreateStudyPage;
import org.labkey.test.util.APITestHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.Maps;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.StudyHelper;
import org.labkey.test.util.TestLogger;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    private static final File testData = TestFileUtils.getSampleData("api/rlabkey-api.xml");

    @BeforeClass
    public static void setupProject()
    {
        RlabkeyTest init = (RlabkeyTest)getCurrentTest();
        init.doInit();
    }

    public void doInit()
    {
        _rReportHelper.ensureRConfig();

        _userHelper.createUser(USER);
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        log("Create Projects");
        _containerHelper.createProject(PROJECT_NAME_2, null);
        apiPermissionsHelper.addUserToProjGroup(USER, PROJECT_NAME_2, "Users");
        apiPermissionsHelper.setPermissions("Users", "Editor");
        _containerHelper.createProject(PROJECT_NAME, "Study");
        CreateStudyPage createStudyPage = _studyHelper.startCreateStudy();
        createStudyPage.setLabel("Rlabkey Study")
                .setTimepointType(StudyHelper.TimepointType.VISIT)
                .createStudy();

        apiPermissionsHelper.addUserToProjGroup(USER, PROJECT_NAME, "Users");
        apiPermissionsHelper.setPermissions("Users", "Editor");

        log("Import Lists");
        File listArchive = new File(_rReportHelper.getRLibraryPath(), "/listArchive.zip");

        if (!listArchive.exists())
            fail("Unable to locate the list archive: " + listArchive.getName());

        goToProjectHome();
        _listHelper.importListArchive(listArchive);
        // create an issues list in a project and subfolder to test ContainerFilters.

        goToModule("FileContent");
        // Dummy files for saveBatch API test
        _fileBrowserHelper.createFolder("data.tsv");
        _fileBrowserHelper.createFolder("result.txt");

        IssuesHelper issuesHelper = new IssuesHelper(this);

        // create an site wide issues list
        clickProject("Shared");
        issuesHelper.createNewIssuesList(ISSUE_LIST_NAME, _containerHelper);

        clickProject(PROJECT_NAME);
        createNewIssueList(issuesHelper, ISSUE_LIST_NAME);
        issuesHelper.addIssue(Maps.of("assignedTo", _userHelper.getDisplayNameForEmail(USER), "title", ISSUE_TITLE_0));

        _containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME);
        apiPermissionsHelper.checkInheritedPermissions();

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
        // need to setup some view categories first
        createCategoriesViaApi();

        if (testData.exists())
        {
            // cheating here, to use the api test framework to store rlabkey tests
            List<APITestHelper.ApiTestCase> tests = APITestHelper.parseTests(testData);

            if (!tests.isEmpty())
            {
                clickProject(getProjectName());
                waitAndClickAndWait(Locator.linkWithText(LIST_NAME));
                DataRegionTable table = new DataRegionTable("query", getDriver());
                table.goToReport("Create R Report");

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
                    sb.append(testScript);
                    String verify = test.getResponse().trim().replaceAll("%projectName%", getProjectName());

                    log("execute test: " + test.getName());
                    TestLogger.increaseIndent();
                    final boolean success = _rReportHelper.executeScript(sb.toString(), verify);
                    TestLogger.decreaseIndent();
                    assertTrue("Failed executing R script for test case: " + test.getName(), success);
                }
                _rReportHelper.clickSourceTab();
                _rReportHelper.saveReport("dummy");
            }
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
