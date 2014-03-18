/*
 * Copyright (c) 2010-2013 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * User: klum
 * Date: Mar 9, 2010
 * Time: 3:44:04 PM
 */
@Category({DailyB.class})
public class RlabkeyTest extends SimpleApiTest
{
    RReportHelper _rReportHelper = new RReportHelper(this);
    private static final String PROJECT_NAME = "RlabkeyVerifyProject";
    private static final String PROJECT_NAME_2 = PROJECT_NAME + "2";
    private static final String LIST_NAME = "AllTypes";
    private static final String LIBPATH_OVERRIDE = ".libPaths(\"%s\")";
    private static final String FOLDER_NAME = "RlabkeyTest";
    private static final String ISSUE_TITLE_0 = "Rlabkey: Issue at the Project level";
    private static final String ISSUE_TITLE_1 = "Rlabkey: Issue in the subfolder";
    private static final String ISSUE_TITLE_2 = "Rlabkey: Issue in another project";

    @Override
    public void runUITests() throws Exception
    {
        log("Create Projects");
        _containerHelper.createProject(PROJECT_NAME, null);
        _containerHelper.createProject(PROJECT_NAME_2, null);
        clickProject(PROJECT_NAME);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Lists");
       
        log("Import Lists");
        File listArchive = new File(WebTestHelper.getLabKeyRoot(), "/sampledata/rlabkey/listArchive.zip");

        if (!listArchive.exists())
            fail("Unable to locate the list archive: " + listArchive.getName());

        _listHelper.importListArchive(PROJECT_NAME, listArchive);
        // create an issues list in a project and subfolder to test ContainerFilters.

        clickProject(PROJECT_NAME);
        portalHelper.addWebPart("Issues List");
        clickButton("Admin");
        uncheckCheckbox("requiredFields", "AssignedTo");
        clickButton("Update");
        clickButton("Back to Issues");
        clickButton("New Issue");
        setFormElement("title", ISSUE_TITLE_0);
        clickButton("Save");
        createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[0]);

        clickFolder(FOLDER_NAME);
        portalHelper.addWebPart("Issues List");
        clickButton("Admin");
        uncheckCheckbox("requiredFields", "AssignedTo");
        clickButton("Update");
        clickButton("Back to Issues");
        clickButton("New Issue");
        setFormElement("title", ISSUE_TITLE_1);
        clickButton("Save");

        clickProject(PROJECT_NAME_2);
        addWebPart("Issues List");
        clickButton("Admin");
        uncheckCheckbox("requiredFields", "AssignedTo");
        clickButton("Update");
        clickButton("Back to Issues");
        clickButton("New Issue");
        setFormElement("title", ISSUE_TITLE_2);
        clickButton("Save");
        
        _rReportHelper.ensureRConfig();
    }

    @Override
    public void runApiTests() throws Exception
    {
        File testData = new File(getLabKeyRoot() + "/server/test/data/api/rlabkey-api.xml");
        if (testData.exists())
        {
            // cheating here, to use the api test framework to store rlabkey tests
            List<ApiTestCase> tests = parseTests(testData);

            if (!tests.isEmpty())
            {
                clickProject(PROJECT_NAME);
                clickAndWait(Locator.linkWithText(LIST_NAME));
                clickMenuButton("Views", "Create", "R View");

                // we want to load the Rlabkey package from the override location
                File libPath = new File(getLabKeyRoot() + "/sampledata/rlabkey");
                String pathCmd = String.format(LIBPATH_OVERRIDE, libPath.getAbsolutePath().replaceAll("\\\\", "/"));

                for (ApiTestCase test : tests)
                {
                    StringBuilder sb = new StringBuilder(pathCmd);

                    sb.append('\n');
                    sb.append(test.getUrl().trim().replaceAll("%baseUrl%", WebTestHelper.getBaseURL()));
                    String verify = test.getReponse().trim();

                    log("exceute test: " + test.getName());
                    if (!_rReportHelper.executeScript(sb.toString(), verify))
                        fail("Failed executing R script for test case: " + test.getName());
                }
                _rReportHelper.clickSourceTab();
                _rReportHelper.saveReport("dummy");
            }
        }
    }

    @Override
    protected File[] getTestFiles()
    {
        return new File[]{new File(getLabKeyRoot() + "/server/test/data/api/rlabkey-api.xml")};
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
        deleteProject(PROJECT_NAME_2, afterTest);
    }

    public String getAssociatedModuleDirectory()
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

}
