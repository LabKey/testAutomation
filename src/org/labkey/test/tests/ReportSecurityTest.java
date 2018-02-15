/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.Reports;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.admin.PermissionsPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

@Category({DailyC.class, Reports.class})
public class ReportSecurityTest extends ReportTest
{
    private static final String TEST_GRID_VIEW = "Test Grid View";

    protected static final String TEST_GROUP = "firstGroup";
    protected static final String TEST_USER = "report_user1@report.test";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, TEST_USER);
        super.doCleanup(afterTest);
    }

    @LogMethod
    protected void doCreateSteps()
    {
        enableEmailRecorder();

        // import study and wait; no specimens needed
        importStudy();
        waitForPipelineJobsToComplete(1, "study import", false);

        // need this to turn off the demographic bit in the DEM-1 dataset
        clickFolder(getFolderName());
        setDemographicsBit("DEM-1: Demographics", false);
    }

    @Override
    @LogMethod
    public void doVerifySteps()
    {
        // additional report and security tests
        setupDatasetSecurity();
        doReportSecurity();
    }

    @LogMethod
    protected void setupDatasetSecurity()
    {
        navigateToFolder(getProjectName(), "My Study");

        // create a test group and give it container read perms
        PermissionsPage permissionsPage = navBar().goToPermissionsPage();

        permissionsPage.createPermissionsGroup(TEST_GROUP);

        // add user to the first test group
        permissionsPage.clickManageGroup(TEST_GROUP);
        setFormElement(Locator.name("names"), TEST_USER);
        uncheckCheckbox(Locator.checkboxByName("sendEmail"));
        clickButton("Update Group Membership");

        navBar().goToPermissionsPage();
        permissionsPage.setPermissions(TEST_GROUP, "Reader");
        permissionsPage.clickSaveAndFinish();

        // give the test group read access to only the DEM-1 dataset
        goToProjectHome();
        clickFolder("My Study");
        enterStudySecurity();

        // enable advanced study security
        doAndWaitForPageToLoad(() -> {
            selectOptionByValue(Locator.name("securityString"), "ADVANCED_READ");
            click(Locator.lkButton("Update Type"));
        });

        click(Locator.xpath("//td[.='" + TEST_GROUP + "']/..//th/input[@value='READOWN']"));
        clickAndWait(Locator.id("groupUpdateButton"));

        selectOptionByText(Locator.name("dataset.1"), "Read");
        clickAndWait(Locator.xpath("//form[@id='datasetSecurityForm']").append(Locator.lkButton("Save")));
    }

    @LogMethod
    protected void doReportSecurity()
    {
        // create charts
        navigateToFolder(getProjectName(), getFolderName());

        clickAndWait(Locator.linkWithText("APX-1: Abbreviated Physical Exam"));
        DataRegionTable dt = new DataRegionTable("Dataset", getDriver());
        dt.goToReport("Create Chart View (deprecated)");
        waitForElement(Locator.xpath("//select[@name='columnsX']"), WAIT_FOR_JAVASCRIPT);
        selectOptionByText(Locator.name("columnsX"), "1. Weight");
        selectOptionByText(Locator.name("columnsY"), "4. Pulse");
        fireEvent(Locator.name("columnsY"), SeleniumEvent.change);
        checkCheckbox(Locator.name("participantChart"));
        clickButton("Save", 0);
        sleep(2000);

        setFormElement(Locator.name("reportName"), "participant chart");
        clickButton("OK", 0);

        dt.goToView("default");
        dt.goToReport("Create Chart View (deprecated)");
        waitForElement(Locator.xpath("//select[@name='columnsX']"), WAIT_FOR_JAVASCRIPT);

        // create a non-participant chart
        selectOptionByText(Locator.name("columnsX"), "1. Weight");
        selectOptionByText(Locator.name("columnsY"), "4. Pulse");
        fireEvent(Locator.name("columnsY"), SeleniumEvent.change);
        clickButton("Save", 0);
        sleep(2000);

        setFormElement(Locator.name("reportName"), "non participant chart");
        setFormElement(Locator.name("description"), "a private chart");
        checkCheckbox(Locator.checkboxByName("shareReport"));
        clickButton("OK", 0);

        // create grid view
        clickFolder(getFolderName());
        goToManageViews();

        BootstrapMenu.find(getDriver(), "Add Report")
                .clickSubMenu(true, "Grid View");

        //clickAddReport("Grid View");
        setFormElement(Locator.name("label"), TEST_GRID_VIEW);
        selectOptionByText(Locator.id("datasetSelection"), "APX-1 (APX-1: Abbreviated Physical Exam)");
        clickButton("Create View");

        // test security
        navigateToFolder(getProjectName(), "My Study");

        goToManageViews();
        clickReportPermissionsLink("participant chart");
        click(Locator.id("useCustom"));
        checkCheckbox(Locator.xpath("//td[.='" + TEST_GROUP + "']/..//td/input[@type='checkbox']"));
        clickButton("Save");

        goToManageViews();
        clickReportPermissionsLink(TEST_GRID_VIEW);
        click(Locator.id("useCustom"));
        checkCheckbox(Locator.xpath("//td[.='" + TEST_GROUP + "']/..//td/input[@type='checkbox']"));
        clickButton("Save");

        goToAdminConsole();
        impersonate(TEST_USER);
        navigateToFolder(getProjectName(), "My Study");

        assertElementNotPresent(Locator.linkWithText("APX-1: Abbreviated Physical Exam"));
        clickAndWait(Locator.linkWithText("participant chart"));

        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(TEST_GRID_VIEW));
        assertTextPresent("999320016");
        pushLocation();
        dt.goToView("default");
        assertTextPresent("User does not have read permission on this dataset.");
        stopImpersonating();
    }
}
