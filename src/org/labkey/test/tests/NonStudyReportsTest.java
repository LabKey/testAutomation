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
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.Reports;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.ext4cmp.Ext4FileFieldRef;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({DailyC.class, Reports.class})
public class NonStudyReportsTest extends ReportTest
{
    protected final PortalHelper portalHelper = new PortalHelper(this);
    protected static final String ATTACHMENT_USER = "attachment_user1@report.test";

    private static final String ATTACHMENT_REPORT_NAME = "Attachment Report1";
    private static final String ATTACHMENT_REPORT_DESCRIPTION = "This attachment report uploads a file";
    private static final File ATTACHMENT_REPORT_FILE = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Microarray/", "test1.jpg"); // arbitrary image file


    private static final String ATTACHMENT_REPORT2_NAME = "Attachment Report2";
    private static final String ATTACHMENT_REPORT3_NAME = "Attachment Report3";
    private static final String UPDATE_ATTACHMENT_REPORT = "Update Attachment Report";

    private static final String ATTACHMENT_REPORT2_DESCRIPTION= "This attachment report points at a file on the server.";
    private static final File ATTACHMENT_REPORT2_FILE = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Microarray/", "test2.jpg"); // arbitrary image file


    private static final String DISCUSSED_REPORT = "Blank R Report";
    private static final String DISCUSSION_BODY_1 = "Starting a discussion";
    private static final String DISCUSSION_TITLE_1 = "Discussion about R report";
    private static final String DISCUSSION_BODY_2 = "Responding to a discussion";
    private static final String DISCUSSION_BODY_3 = "Editing a discussion response";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, ATTACHMENT_USER);
        super.doCleanup(afterTest);
    }

    @LogMethod
    protected void doCreateSteps()
    {
        enableEmailRecorder();

        _containerHelper.createProject(getProjectName(), "Study");

        RReportHelper rReportHelper = new RReportHelper(this);
        rReportHelper.ensureRConfig();
    }

    @LogMethod
    protected void doVerifySteps()
    {
        doAttachmentReportTest();
        doLinkReportTest();
        doThumbnailChangeTest();
        doReportDiscussionTest();
    }

    @LogMethod
    private void doAttachmentReportTest()
    {
        clickProject(getProjectName());
        goToManageViews();
        BootstrapMenu.find(getDriver(),"Add Report").clickSubMenu(true,"Attachment Report");
        clickButton("Cancel");

        BootstrapMenu.find(getDriver(),"Add Report")
                .clickSubMenu(true,"Attachment Report");
        setFormElement(Locator.name("viewName"), ATTACHMENT_REPORT_NAME);
        setFormElement(Locator.name("description"), ATTACHMENT_REPORT_DESCRIPTION);
        setFormElement(Locator.id("uploadFile-button-fileInputEl"), ATTACHMENT_REPORT_FILE);

        Ext4FileFieldRef ref = Ext4FileFieldRef.create(this);
        ref.setToFile(ATTACHMENT_REPORT_FILE);
        clickButton("Save");
        // save should return back to manage views page
        waitForText("Manage Views");

        // test creation from Data Views menu option
        goToProjectHome();
        portalHelper.addWebPart("Data Views");
        portalHelper.clickWebpartMenuItem("Data Views", true, "Add Report", "Attachment Report");
        setFormElement(Locator.name("viewName"), ATTACHMENT_REPORT2_NAME);
        setFormElement(Locator.name("description"), ATTACHMENT_REPORT2_DESCRIPTION);
        click(Locator.xpath("//input[../label[string()='Full file path on server']]"));
        setFormElement(Locator.name("filePath"), ATTACHMENT_REPORT2_FILE.toString());
        clickButton("Save");
        // save should return to the Portal
        waitForText("Data Views");

        waitForText(ATTACHMENT_REPORT_NAME);
        waitForText(ATTACHMENT_REPORT2_NAME);

        clickReportGridLink(ATTACHMENT_REPORT_NAME);
        goBack();
        clickReportGridLink(ATTACHMENT_REPORT2_NAME);
        goBack();

        // relies on reports created in this function so
        // call from here
        doUpdateAttachmentReportTest();
    }

    @LogMethod
    private void doUpdateAttachmentReportTest()
    {
        clickProject(getProjectName());

        //
        // verify source URL works, share the local attachment report (REPORT)
        //
        clickReportDetailsLink(ATTACHMENT_REPORT_NAME);
        clickAndWait(Locator.linkContainingText("Edit Report"));
        click(Locator.xpath("//input[../label[string()='Share this report with all users?']]"));
        clickButton("Save");
        waitForText("Report Details");

        //
        // verify details edit button works, share the server attachment report (REPORT2)
        //
        goToManageViews();
        clickReportDetailsLink(ATTACHMENT_REPORT2_NAME);
        clickAndWait(Locator.linkContainingText("Edit Report"));
        click(Locator.xpath("//input[../label[string()='Share this report with all users?']]"));
        clickButton("Save");
        waitForText("Report Details");

        //
        // verify a non-admin can edit a local attachment report but not a
        // server attachment report
        //
        _userHelper.createUser(ATTACHMENT_USER);
        clickProject(getProjectName());
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setUserPermissions(ATTACHMENT_USER, "Editor");
        impersonate(ATTACHMENT_USER);
        clickProject(getProjectName());

        // can edit local
        goToProjectHome();
        portalHelper.clickWebpartMenuItem("Data Views", true, "Manage Views");
        waitForText("Manage Views");
        clickReportDetailsLink(ATTACHMENT_REPORT_NAME);
        waitForElement(Locator.lkButton("View Report"));
        clickAndWait(Locator.lkButton("Edit Report"));
        waitForElement(Locators.pageSignal("category-loaded")); // DataViewPropertiesPanel.js
        clickButton("Save");
        waitForText("Report Details");

        // cannot edit server
        goToProjectHome();
        clickReportDetailsLink(ATTACHMENT_REPORT2_NAME);
        waitForElement(Locator.lkButton("View Report"));
        assertElementNotPresent(Locator.lkButton("Edit Report"));
        stopImpersonating();

        goToHome();
        clickProject(getProjectName());
        goToManageViews();

        //
        // verify we can change a server attachment type to a local attachment type
        //
        clickReportDetailsLink(ATTACHMENT_REPORT2_NAME);
        clickAndWait(Locator.linkContainingText("Edit Report"));
        // change this from a server attachment report to a local attachment report
        click(Locator.xpath("//input[../label[string()='Upload file to server']]"));
        Ext4FileFieldRef ref = Ext4FileFieldRef.create(this);
        ref.setToFile(ATTACHMENT_REPORT2_FILE);
        clickButton("Save");
        // save should return back to the details page
        waitForText("Report Details");

        // verify rename
        goToManageViews();
        clickReportDetailsLink(ATTACHMENT_REPORT2_NAME);
        clickAndWait(Locator.linkContainingText("Edit Report"));
        setFormElement(Locator.name("viewName"), ATTACHMENT_REPORT3_NAME);
        clickButton("Save");
        waitForText(ATTACHMENT_REPORT3_NAME);

        // verify can rename to same name
        goToManageViews();
        clickReportDetailsLink(ATTACHMENT_REPORT3_NAME);
        clickAndWait(Locator.linkContainingText("Edit Report"));
        setFormElement(Locator.name("viewName"), ATTACHMENT_REPORT3_NAME);
        clickButton("Save");
        waitForText(ATTACHMENT_REPORT3_NAME);

        Locator statusElement = Locator.input("status");

        // verify we can set a property
        clickAndWait(Locator.linkContainingText("Edit Report"));
        waitForText(UPDATE_ATTACHMENT_REPORT);
        assertFalse("Locked".equals(getFormElement(statusElement)));
        setFormElement(Locator.name("status"), "Locked");
        clickButton("Save");
        waitForText(ATTACHMENT_REPORT3_NAME);

        clickAndWait(Locator.linkContainingText("Edit Report"));
        waitForText(UPDATE_ATTACHMENT_REPORT);
        assertEquals("Locked", getFormElement(statusElement));
        clickButton("Cancel");
        waitForText(ATTACHMENT_REPORT3_NAME);
    }

    @LogMethod
    private void doThumbnailChangeTest()
    {
        goToProjectHome();
        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        DataViewsTest.clickCustomizeView(ATTACHMENT_REPORT_NAME, this);
        assertTextPresent("Share this report with all users");

        _ext4Helper.clickExt4Tab("Images");
        setFormElement(Locator.name("customThumbnail"), ATTACHMENT_REPORT2_FILE);
        clickButton("Save", 0);

        _ext4Helper.waitForMaskToDisappear();

        //no way to verify, unfortunately
    }

    @LogMethod
    private void doReportDiscussionTest()
    {
        clickProject(getProjectName());

        goToManageViews();
        BootstrapMenu.find(getDriver(),"Add Report").clickSubMenu(true,"R Report");
        RReportHelper RReportHelper = new RReportHelper(this);
        RReportHelper.executeScript("# Placeholder script for discussion", "");
        click(Locator.linkWithText("Source"));
        RReportHelper.saveReport(DISCUSSED_REPORT);
        clickReportGridLink(DISCUSSED_REPORT);

        clickMenuButton(true, Locator.tagWithClass("div", "discussion-toggle").findElement(getDriver()),false,"Start new discussion");
        waitForElement(Locator.id("title"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.id("title"), DISCUSSION_TITLE_1);
        setFormElement(Locator.id("body"), DISCUSSION_BODY_1);
        clickButton("Submit");

        clickMenuButton(true, Locator.tagWithClass("div", "discussion-toggle").findElement(getDriver()),false,DISCUSSION_TITLE_1);
        waitForText(DISCUSSION_TITLE_1);
        assertTextPresent(DISCUSSION_BODY_1);

        clickButton("Respond");
        waitForElement(Locator.id("body"));
        setFormElement(Locator.id("body"), DISCUSSION_BODY_2);
        clickButton("Submit");

        assertTextPresent(DISCUSSION_BODY_2);

        clickAndWait(Locator.linkContainingText("edit"));
        waitForElement(Locator.id("body"));
        setFormElement(Locator.id("body"), DISCUSSION_BODY_3);
        clickButton("Submit");

        assertTextPresent(DISCUSSION_BODY_3);
    }

    private static final String LINK_REPORT1_NAME = "Link Report1";
    private static final String LINK_REPORT1_DESCRIPTION= "This link report points links to an internal page.";
    private static final String LINK_REPORT1_URL = "/project/home/begin.view";

    private static final String LINK_REPORT2_NAME = "Link Report2";
    private static final String LINK_REPORT2_DESCRIPTION= "This link report points links to an external page.";

    @LogMethod
    private void doLinkReportTest()
    {
        clickProject(getProjectName());
        goToManageViews();

        BootstrapMenu.find(getDriver(),"Add Report").clickSubMenu(true,"Link Report");
        waitForElement(Locator.tag("li").containing("URL must be absolute"));
        assertElementPresent(Locator.tag("li").containing("This field is required"), 2);
        setFormElement(Locator.name("viewName"), LINK_REPORT1_NAME);
        waitForElementToDisappear(Locator.tag("li").containing("This field is required").index(1));
        setFormElement(Locator.name("description"), LINK_REPORT1_DESCRIPTION);
        setFormElement(Locator.name("linkUrl"), "mailto:kevink@linkreport.test");
        waitForElementToDisappear(Locator.tag("li").containing("This field is required"));
        assertElementPresent(Locator.tag("li").containing("URL must be absolute"));
        setFormElement(Locator.name("linkUrl"), WebTestHelper.getContextPath() + LINK_REPORT1_URL);
        waitForElementToDisappear(Locator.tag("li").containing("URL must be absolute"));
        assertTrue("Expected targetNewWindow checkbox to be checked", _ext4Helper.isChecked("Open link report in new window?"));
        _ext4Helper.uncheckCheckbox("Open link report in new window?");
        clickButton("Save");
        // save should return back to manage views page
        waitForText("Manage Views");

        // test creation from menu option on Data Views webpart
        goToProjectHome();
        portalHelper.clickWebpartMenuItem("Data Views", true, "Add Report", "Link Report");
        setFormElement(Locator.name("viewName"), LINK_REPORT2_NAME);
        setFormElement(Locator.name("description"), LINK_REPORT2_DESCRIPTION);
        setFormElement(Locator.name("linkUrl"), getBaseURL() + LINK_REPORT1_URL);
        waitForElementToDisappear(Locator.tag("li").containing("URL must be absolute"));
        assertTrue("Expected targetNewWindow checkbox to be checked", _ext4Helper.isChecked("Open link report in new window?"));
        clickButton("Save");
        // save should return back to Portal
        waitForText("Data Views");

        goToManageViews();
        pushLocation();
        clickReportGridLink(LINK_REPORT1_NAME);
        assertTrue("Expected link report to go to '" + LINK_REPORT1_URL + "', but was '" + getCurrentRelativeURL() + "'",
                getURL().toString().contains(LINK_REPORT1_URL));
        popLocation();

        // Clicking on LINK_REPORT2_NAME "view" link will open a new browser window.
        // To avoid opening a new browser window, let's just check that the link has the target="_blank" attribute.
        //Locator link = getReportGridLink(LINK_REPORT2_NAME);
        //String target = getAttribute(link, "target");
        //assertEquals("_blank", target);
    }
}
