/*
 * Copyright (c) 2010-2011 LabKey Corporation
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
package org.labkey.test.bvt;

import org.labkey.test.WebTestHelper;
import org.labkey.test.Locator;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.RReportHelper;

import java.io.File;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: Mar 9, 2010
 * Time: 3:44:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class RlabkeyTest extends SimpleApiTest
{
    private static final String PROJECT_NAME = "RlabkeyVerifyProject";
    private static final String LIST_NAME = "AllTypes";
    private static final String LIBPATH_OVERRIDE = ".libPaths(\"%s\")";
    private static final String FOLDER_NAME = "RlabkeyTest";
    private static final String ISSUE_TITLE_0 = "An issue entered at the Project level";
    private static final String ISSUE_TITLE_1 = "An issue inserted in the subfolder";

    @Override
    public void runUITests() throws Exception
    {
        log("Create Project");
        createProject(PROJECT_NAME);
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Lists");

        log("Import Lists");
        File listArchive = new File(WebTestHelper.getLabKeyRoot(), "/sampledata/rlabkey/listArchive.zip");

        if (!listArchive.exists())
            fail("Unable to locate the list archive: " + listArchive.getName());

        ListHelper.importListArchive(this, PROJECT_NAME, listArchive);
        // create an issues list in a project and subfolder to test ContainerFilters.

        clickLinkWithText(PROJECT_NAME);        
        addWebPart("Issues List");
        clickNavButton("Admin");
        uncheckCheckbox("requiredFields", "AssignedTo");
        clickNavButton("Update");
        clickNavButton("Back to Issues");
        clickNavButton("New Issue");
        setFormElement("title", ISSUE_TITLE_0);
        clickNavButton("Submit");
        createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[0]);

        clickLinkWithText(FOLDER_NAME);
        addWebPart("Issues List");
        clickNavButton("Admin");
        uncheckCheckbox("requiredFields", "AssignedTo");
        clickNavButton("Update");
        clickNavButton("Back to Issues");
        clickNavButton("New Issue");
        setFormElement("title", ISSUE_TITLE_1);
        clickNavButton("Submit");

        RReportHelper.ensureRConfig(this);
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
                clickLinkWithText(PROJECT_NAME);
                clickLinkWithText(LIST_NAME);
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

                    if (!RReportHelper.executeScript(this, sb.toString(), verify))
                        fail("Failed executing R script for test case: " + test.getName());
                }
                RReportHelper.saveReport(this, "dummy");
            }
        }
    }

    @Override
    protected File[] getTestFiles()
    {
        return new File[]{new File(getLabKeyRoot() + "/server/test/data/api/rlabkey-api.xml")};
    }

    @Override
    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }
}
