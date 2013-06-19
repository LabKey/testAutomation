/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.ext4cmp.Ext4FileFieldRef;

import java.io.File;

/**
 * User: tchadick
 * Date: 6/11/13
 */
public class NonStudyReportsTest extends ReportTest
{
    protected static final String ATTACHMENT_USER = "attachment_user1@report.test";

    private static final String ATTACHMENT_REPORT_NAME = "Attachment Report1";
    private static final String ATTACHMENT_REPORT_DESCRIPTION = "This attachment report uploads a file";
    private static final File ATTACHMENT_REPORT_FILE = new File(getLabKeyRoot() + "/sampledata/Microarray/", "test1.jpg"); // arbitrary image file

    private static final String ATTACHMENT_REPORT2_NAME = "Attachment Report2";
    private static final String ATTACHMENT_REPORT3_NAME = "Attachment Report3";
    private static final String UPDATE_ATTACHMENT_REPORT = "Update Attachment Report";

    private static final String ATTACHMENT_REPORT2_DESCRIPTION= "This attachment report points at a file on the server.";
    private static final File ATTACHMENT_REPORT2_FILE = new File(getLabKeyRoot() + "/sampledata/Microarray/", "test2.jpg"); // arbitrary image file

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
        clickMenuButton("Create", "Attachment Report");
        clickButton("Cancel");

        clickMenuButton("Create", "Attachment Report");
        setFormElement("viewName", ATTACHMENT_REPORT_NAME);
        setFormElement("description", ATTACHMENT_REPORT_DESCRIPTION);
        setFormElement("uploadFile", ATTACHMENT_REPORT_FILE.toString());

        Ext4FileFieldRef ref = Ext4FileFieldRef.create(this);
        ref.setToFile(ATTACHMENT_REPORT_FILE.toString());
        clickButton("Save");
        // save should return back to manage views page
        waitForText("Manage Views");

        // test creation from Data Views menu option
        clickTab("Overview");
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Data Views");
        clickWebpartMenuItem("Data Views", true, "Add Report", "From File");
        setFormElement("viewName", ATTACHMENT_REPORT2_NAME);
        setFormElement("description", ATTACHMENT_REPORT2_DESCRIPTION);
        click(Locator.xpath("//input[../label[string()='Full file path on server']]"));
        setFormElement("filePath", ATTACHMENT_REPORT2_FILE.toString());
        clickButton("Save");
        // save should return to the Portal
        waitForText("Data Views");

        waitForText(ATTACHMENT_REPORT_NAME);
        waitForText(ATTACHMENT_REPORT2_NAME);

        clickReportGridLink(ATTACHMENT_REPORT_NAME, "view");
        goBack();
        clickReportGridLink(ATTACHMENT_REPORT2_NAME, "view");
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
        clickReportGridLink(ATTACHMENT_REPORT_NAME, "source");
        click(Locator.xpath("//input[../label[string()='Share this report with all users?']]"));
        clickButton("Save");
        waitForText("Manage Views");

        //
        // verify details edit button works, share the server attachment report (REPORT2)
        //
        clickReportGridLink(ATTACHMENT_REPORT2_NAME, "details");
        clickButton("Edit Report");
        click(Locator.xpath("//input[../label[string()='Share this report with all users?']]"));
        clickButton("Save");
        waitForText("Report Details");

        //
        // verify a non-admin can edit a local attachment report but not a
        // server attachment report
        //
        createUser(ATTACHMENT_USER, null);
        clickProject(getProjectName());
        enterPermissionsUI();
        setUserPermissions(ATTACHMENT_USER, "Editor");
        impersonate(ATTACHMENT_USER);
        clickProject(getProjectName());

        // can edit local
        clickTab("Overview");
        clickWebpartMenuItem("Data Views", true, "Manage Views");
        waitForText("Manage Views");
        clickReportGridLink(ATTACHMENT_REPORT_NAME, "details", false /*isAdmin*/);
        waitForText("Report Details");
        Locator.XPathLocator l = getButtonLocator("Edit Report");
        Assert.assertTrue("Expected 'Edit Report' button to be present", l != null);
        clickButton("Edit Report");
        clickButton("Save");
        waitForText("Report Details");

        // cannot edit server
        clickTab("Overview");
        clickWebpartMenuItem("Data Views", true, "Manage Views");
        clickReportGridLink(ATTACHMENT_REPORT2_NAME, "details", false /*isAdmin*/);
        waitForText("Report Details");
        l = getButtonLocator("Edit Report");
        Assert.assertTrue("Expected 'Edit Report' button to not be present", l == null);
        stopImpersonating();

        goToHome();
        clickProject(getProjectName());
        goToManageViews();

        //
        // verify we can change a server attachment type to a local attachment type
        //
        clickReportGridLink(ATTACHMENT_REPORT2_NAME, "source");
        // change this from a server attachment report to a local attachment report
        click(Locator.xpath("//input[../label[string()='Upload file to server']]"));
        Ext4FileFieldRef ref = Ext4FileFieldRef.create(this);
        ref.setToFile(ATTACHMENT_REPORT2_FILE.toString());
        clickButton("Save");
        // save should return back to the details page
        waitForText("Manage Views");

        // verify rename
        clickReportGridLink(ATTACHMENT_REPORT2_NAME, "source");
        setFormElement("viewName", ATTACHMENT_REPORT3_NAME);
        clickButton("Save");
        waitForText(ATTACHMENT_REPORT3_NAME);

        // verify can rename to same name
        clickReportGridLink(ATTACHMENT_REPORT3_NAME, "source");
        setFormElement("viewName", ATTACHMENT_REPORT3_NAME);
        clickButton("Save");
        waitForText(ATTACHMENT_REPORT3_NAME);

        Locator statusElement = Locator.input("status");

        // verify we can set a property
        clickReportGridLink(ATTACHMENT_REPORT3_NAME, "source");
        waitForText(UPDATE_ATTACHMENT_REPORT);
        Assert.assertFalse("Locked".equals(getFormElement(statusElement)));
        setFormElement("status", "Locked");
        clickButton("Save");
        waitForText(ATTACHMENT_REPORT3_NAME);
        clickReportGridLink(ATTACHMENT_REPORT3_NAME, "source");
        waitForText(UPDATE_ATTACHMENT_REPORT);
        Assert.assertTrue("Locked".equals(getFormElement(statusElement)));
        clickButton("Cancel");
        waitForText(ATTACHMENT_REPORT3_NAME);
    }

    @LogMethod
    private void doThumbnailChangeTest()
    {
        clickTab("Overview");
        clickWebpartMenuItem("Data Views", false, "Customize");
        DataViewsTest.clickCustomizeView(ATTACHMENT_REPORT_NAME, this);
        assertTextPresent("Share this report with all users");

        //set change thumbnail
//        setFormElement(Locator.xpath("//input[contains(@id, 'customThumbnail')]"), ATTACHMENT_REPORT2_FILE.toString(), false);

        Ext4FileFieldRef ref = Ext4FileFieldRef.create(this);
        ref.setToFile(ATTACHMENT_REPORT2_FILE.toString());
        clickButtonByIndex("Save", 1, 0);

        //no way to verify, unfortunately
    }

    @LogMethod
    private void doReportDiscussionTest()
    {
        clickProject(getProjectName());

        goToManageViews();
        _extHelper.clickMenuButton("Create", "R View");
        RReportHelper rReportHelper = new RReportHelper(this);
        rReportHelper.executeScript("# Placeholder script for discussion", "");
        rReportHelper.saveReport(DISCUSSED_REPORT);
        clickReportGridLink(DISCUSSED_REPORT, "view");

        _extHelper.clickExtDropDownMenu("discussionMenuToggle", "Start new discussion");
        waitForPageToLoad();

        waitForElement(Locator.id("title"), WAIT_FOR_JAVASCRIPT);
        setFormElement("title", DISCUSSION_TITLE_1);
        setFormElement("body", DISCUSSION_BODY_1);
        clickButton("Submit");
        waitForPageToLoad();

        _extHelper.clickExtDropDownMenu("discussionMenuToggle", DISCUSSION_TITLE_1);
        waitForPageToLoad();

        waitForText(DISCUSSION_TITLE_1);
        assertTextPresent(DISCUSSION_BODY_1);

        clickButton("Respond");
        waitForPageToLoad();
        waitForElement(Locator.id("body"));
        setFormElement("body", DISCUSSION_BODY_2);
        clickButton("Submit");
        waitForPageToLoad();

        assertTextPresent(DISCUSSION_BODY_2);

        clickAndWait(Locator.linkContainingText("edit"));
        waitForPageToLoad();
        waitForElement(Locator.id("body"));
        setFormElement("body", DISCUSSION_BODY_3);
        clickButton("Submit");
        waitForPageToLoad();

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

        clickMenuButton("Create", "Link Report");
        setFormElement("viewName", LINK_REPORT1_NAME);
        setFormElement("description", LINK_REPORT1_DESCRIPTION);
        assertTextNotPresent("URL must be absolute");
        setFormElement("linkUrl", "mailto:kevink@example.com");
        assertTextPresent("URL must be absolute");
        setFormElement("linkUrl", getContextPath() + LINK_REPORT1_URL);
        assertTextNotPresent("URL must be absolute");
        Assert.assertTrue("Expected targetNewWindow checkbox to be checked", _extHelper.isChecked("Open link report in new window?"));
        _extHelper.uncheckCheckbox("Open link report in new window?");
        clickButton("Save");
        // save should return back to manage views page
        waitForText("Manage Views");

        // test creation from menu option on Data Views webpart
        clickTab("Overview");
        clickWebpartMenuItem("Data Views", true, "Add Report", "From Link");
        setFormElement("viewName", LINK_REPORT2_NAME);
        setFormElement("description", LINK_REPORT2_DESCRIPTION);
        setFormElement("linkUrl", getBaseURL() + LINK_REPORT1_URL);
        assertTextNotPresent("URL must be absolute");
        Assert.assertTrue("Expected targetNewWindow checkbox to be checked", _extHelper.isChecked("Open link report in new window?"));
        clickButton("Save");
        // save should return back to Portal
        waitForText("Data Views");

        goToManageViews();
        pushLocation();
        clickReportGridLink(LINK_REPORT1_NAME, "view");
        Assert.assertTrue("Expected link report to go to '" + LINK_REPORT1_URL + "', but was '" + getCurrentRelativeURL() + "'",
                getURL().toString().contains(LINK_REPORT1_URL));
        popLocation();

        // Clicking on LINK_REPORT2_NAME "view" link will open a new browser window.
        // To avoid opening a new browser window, let's just check that the link has the target="_blank" attribute.
        Locator link = getReportGridLink(LINK_REPORT2_NAME, "view");
        String target = getAttribute(link, "target");
        Assert.assertEquals("_blank", target);
    }
}
