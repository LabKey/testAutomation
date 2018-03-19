/*
 * Copyright (c) 2017 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.RReportHelper;

import java.util.Arrays;
import java.util.List;

@Category({DailyC.class, Reports.class})
public class ReportSharingTest extends BaseWebDriverTest
{
    RReportHelper _rReportHelper = new RReportHelper(this);

    private static final String USER_NON_EDITOR = "labkey_non_editor@reportsharing.test";
    private static final String USER_EDITOR = "labkey_user@reportsharing.test";
    private static final String USER_DEV = "labkey_dev@reportsharing.test";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(),afterTest);
        _userHelper.deleteUsers(false, USER_DEV,USER_EDITOR,USER_NON_EDITOR);
    }

    @BeforeClass
    public static void setupProject()
    {
        ReportSharingTest init = (ReportSharingTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _rReportHelper.ensureRConfig();

        _containerHelper.createProject(getProjectName(), null);

        _userHelper.createUser(USER_EDITOR);
        _userHelper.createUser(USER_DEV);
        _userHelper.createUser(USER_NON_EDITOR);

        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addUserToProjGroup(USER_EDITOR, getProjectName(), "Users");
        apiPermissionsHelper.setPermissions("Users", "Editor");
        apiPermissionsHelper.setUserPermissions(USER_EDITOR,"Reader");
        apiPermissionsHelper.setUserPermissions(USER_NON_EDITOR,"Submitter");
        apiPermissionsHelper.addUserToSiteGroup(USER_DEV, "Site Administrators");
    }

    @Before
    public void preTest() throws Exception
    {
        goToProjectHome();
    }

    @Test
    public void testSharingWithDevelopers() throws Exception
    {
        // go to core.Containers view
        goToSchemaBrowser();
        viewQueryData("core","Containers");

        //create R report
        String REPORT_NAME = "shared report";
        _rReportHelper.createRReport(REPORT_NAME);

        // open new report from view and share it
        goToContainersReport(REPORT_NAME);
        shareReportAndConfirm(USER_DEV, null);

        // impersonate user and go to report
        impersonate(USER_DEV);
        goToProjectHome();
        goToContainersReport(REPORT_NAME);

        //confirm recipient can Save As
        _rReportHelper.clickSourceTab();
        String REPORT_NAME_SAVED_AS = "shared report saved";
        _rReportHelper.saveAsReport(REPORT_NAME_SAVED_AS);

        //confirm recipient can share report saved from shared
        goToContainersReport(REPORT_NAME_SAVED_AS);
        shareReportAndConfirm(USER_EDITOR, null);
    }

    @Test
    public void testSharingWithNonEditor() throws Exception
    {
        // go to core.Containers view
        goToSchemaBrowser();
        viewQueryData("core","Containers");

        //create R report
        String REPORT_NAME_FOR_FAIL_SHARE = "shared report fail";
        _rReportHelper.createRReport(REPORT_NAME_FOR_FAIL_SHARE);

        // open new report from view and attempt to share it
        goToContainersReport(REPORT_NAME_FOR_FAIL_SHARE);
        shareReportAndConfirm(USER_NON_EDITOR, "User does not have permissions to this folder: labkey_non_editor@reportsharing.test");
    }

    private void goToContainersReport(String reportName)
    {
        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("core","Containers");
        table.goToReport(reportName);
    }

    private void shareReportAndConfirm(String recipient, @Nullable String expectedErrorMsg)
    {
        DataRegionTable table = new DataRegionTable("query", getDriver());
        table.clickHeaderButton("Share report");
        setFormElement(Locator.textarea("recipientList"), recipient);
        clickButton("Submit");

        if (expectedErrorMsg != null)
        {
            assertTextPresent(expectedErrorMsg);
        }
        else
        {
            //confirm recipient is included in Users list and checked
            assertChecked(Locator.tagWithText("td", recipient).followingSibling("td").childTag("input"));
        }
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "ReportSharingTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }

}