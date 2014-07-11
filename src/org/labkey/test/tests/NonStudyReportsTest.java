/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.ext4cmp.Ext4FileFieldRef;

import java.io.File;

import static org.junit.Assert.*;

@Category({DailyA.class, Reports.class})
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
        deleteUsers(afterTest, ATTACHMENT_USER);
        super.doCleanup(afterTest);
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        enableEmailRecorder();

        _containerHelper.createProject(getProjectName(), "Study");

        RReportHelper rReportHelper = new RReportHelper(this);
        rReportHelper.ensureRConfig();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
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
        clickAddReport("Attachment Report");
        clickButton("Cancel");

        clickAddReport("Attachment Report");
        setFormElement("viewName", ATTACHMENT_REPORT_NAME);
        setFormElement("description", ATTACHMENT_REPORT_DESCRIPTION);
        setFormElement(Locator.id("uploadFile-button-fileInputEl"), ATTACHMENT_REPORT_FILE);

        Ext4FileFieldRef ref = Ext4FileFieldRef.create(this);
        ref.setToFile(ATTACHMENT_REPORT_FILE);
        clickButton("Save");
        // save should return back to manage views page
        waitForText("Manage Views");

        // test creation from Data Views menu option
        clickTab("Overview");
        portalHelper.addWebPart("Data Views");
        portalHelper.clickWebpartMenuItem("Data Views", true, "Add Report", "Attachment Report");
        setFormElement("viewName", ATTACHMENT_REPORT2_NAME);
        setFormElement("description", ATTACHMENT_REPORT2_DESCRIPTION);
        click(Locator.xpath("//input[../label[string()='Full file path on server']]"));
        setFormElement("filePath", ATTACHMENT_REPORT2_FILE.toString());
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
        createUser(ATTACHMENT_USER, null);
        clickProject(getProjectName());
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setUserPermissions(ATTACHMENT_USER, "Editor");
        impersonate(ATTACHMENT_USER);
        clickProject(getProjectName());

        // can edit local
        clickTab("Overview");
        portalHelper.clickWebpartMenuItem("Data Views", true, "Manage Views");
        waitForText("Manage Views");
        clickReportDetailsLink(ATTACHMENT_REPORT_NAME);
        waitForText("Report Details");
        Locator.XPathLocator l = getButtonLocator("Edit Report");
        assertTrue("Expected 'Edit Report' button to be present", l != null);
        clickButton("Edit Report");
        clickButton("Save");
        waitForText("Report Details");

        // cannot edit server
        clickTab("Overview");
        clickReportDetailsLink(ATTACHMENT_REPORT2_NAME);
        waitForText("Report Details");
        l = getButtonLocator("Edit Report");
        assertTrue("Expected 'Edit Report' button to not be present", l == null);
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
        setFormElement("viewName", ATTACHMENT_REPORT3_NAME);
        clickButton("Save");
        waitForText(ATTACHMENT_REPORT3_NAME);

        // verify can rename to same name
        goToManageViews();
        clickReportDetailsLink(ATTACHMENT_REPORT3_NAME);
        clickAndWait(Locator.linkContainingText("Edit Report"));
        setFormElement("viewName", ATTACHMENT_REPORT3_NAME);
        clickButton("Save");
        waitForText(ATTACHMENT_REPORT3_NAME);

        Locator statusElement = Locator.input("status");

        // verify we can set a property
        clickAndWait(Locator.linkContainingText("Edit Report"));
        waitForText(UPDATE_ATTACHMENT_REPORT);
        assertFalse("Locked".equals(getFormElement(statusElement)));
        setFormElement("status", "Locked");
        clickButton("Save");
        waitForText(ATTACHMENT_REPORT3_NAME);

        clickAndWait(Locator.linkContainingText("Edit Report"));
        waitForText(UPDATE_ATTACHMENT_REPORT);
        assertTrue("Locked".equals(getFormElement(statusElement)));
        clickButton("Cancel");
        waitForText(ATTACHMENT_REPORT3_NAME);
    }

    @LogMethod
    private void doThumbnailChangeTest()
    {
        clickTab("Overview");
        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        DataViewsTest.clickCustomizeView(ATTACHMENT_REPORT_NAME, this);
        assertTextPresent("Share this report with all users");

        //set change thumbnail
//        setFormElement(Locator.xpath("//input[contains(@id, 'customThumbnail')]"), ATTACHMENT_REPORT2_FILE.toString(), false);

        _ext4Helper.clickExt4Tab("Images");
        Ext4FileFieldRef ref = Ext4FileFieldRef.create(this);
        ref.setToFile(ATTACHMENT_REPORT2_FILE);
        clickButton("Save", 0);

        //no way to verify, unfortunately
    }

    @LogMethod
    private void doReportDiscussionTest()
    {
        clickProject(getProjectName());

        goToManageViews();
        clickAddReport("R View");
        RReportHelper RReportHelper = new RReportHelper(this);
        RReportHelper.executeScript("# Placeholder script for discussion", "");
        click(Locator.linkWithText("Source"));
        RReportHelper.saveReport(DISCUSSED_REPORT);
        clickReportGridLink(DISCUSSED_REPORT);

        _extHelper.clickExtMenuButton(true, Locator.id("discussionMenuToggle"), "Start new discussion");

        waitForElement(Locator.id("title"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("title"), DISCUSSION_TITLE_1);
        setFormElement("body", DISCUSSION_BODY_1);
        clickButton("Submit");

        _extHelper.clickExtMenuButton(true, Locator.id("discussionMenuToggle"), DISCUSSION_TITLE_1);

        waitForText(DISCUSSION_TITLE_1);
        assertTextPresent(DISCUSSION_BODY_1);

        clickButton("Respond");
        waitForElement(Locator.id("body"));
        setFormElement("body", DISCUSSION_BODY_2);
        clickButton("Submit");

        assertTextPresent(DISCUSSION_BODY_2);

        clickAndWait(Locator.linkContainingText("edit"));
        waitForElement(Locator.id("body"));
        setFormElement("body", DISCUSSION_BODY_3);
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

        clickAddReport("Link Report");
        waitForElement(Locator.tag("li").containing("URL must be absolute"));
        assertElementPresent(Locator.tag("li").containing("This field is required"), 2);
        setFormElement("viewName", LINK_REPORT1_NAME);
        waitForElementToDisappear(Locator.tag("li").containing("This field is required").index(1));
        setFormElement("description", LINK_REPORT1_DESCRIPTION);
        setFormElement("linkUrl", "mailto:kevink@example.com");
        waitForElementToDisappear(Locator.tag("li").containing("This field is required"));
        assertElementPresent(Locator.tag("li").containing("URL must be absolute"));
        setFormElement("linkUrl", WebTestHelper.getContextPath() + LINK_REPORT1_URL);
        waitForElementToDisappear(Locator.tag("li").containing("URL must be absolute"));
        assertTrue("Expected targetNewWindow checkbox to be checked", _ext4Helper.isChecked("Open link report in new window?"));
        _ext4Helper.uncheckCheckbox("Open link report in new window?");
        clickButton("Save");
        // save should return back to manage views page
        waitForText("Manage Views");

        // test creation from menu option on Data Views webpart
        clickTab("Overview");
        portalHelper.clickWebpartMenuItem("Data Views", true, "Add Report", "Link Report");
        setFormElement("viewName", LINK_REPORT2_NAME);
        setFormElement("description", LINK_REPORT2_DESCRIPTION);
        setFormElement("linkUrl", getBaseURL() + LINK_REPORT1_URL);
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
