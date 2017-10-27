/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.PortalHelper;

import static org.junit.Assert.assertTrue;

@Category({DailyC.class})
public class DataViewsPermissionsTest extends StudyBaseTest
{
    public static final String AUTHOR_USER = "author@dataviews.test";
    public static final String EDITOR_USER = "editor@dataviews.test";
    private final PortalHelper portalHelper = new PortalHelper(this);

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(afterTest, AUTHOR_USER, EDITOR_USER);
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    protected String getFolderName()
    {
        return "My Study Data Views";
    }

    protected void doCreateSteps()
    {
        importStudy();

        clickFolder(getFolderName());
        portalHelper.addWebPart("Data Views");
        portalHelper.enterAdminMode();
        BodyWebPart dataViewsWebPart = new BodyWebPart(getDriver(), "Data Views");
        dataViewsWebPart.moveUp();
        dataViewsWebPart.moveUp();
        dataViewsWebPart.moveUp();
        dataViewsWebPart.moveUp();
        dataViewsWebPart.moveUp();
        portalHelper.exitAdminMode();

        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.uncheckInheritedPermissions();
        clickButton("Save and Finish", defaultWaitForPage);
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.createPermissionsGroup("Editor Group");
        _permissionsHelper.assertPermissionSetting("Editor Group", "No Permissions");
        _permissionsHelper.setPermissions("Editor Group", "Editor");
        createUserInProjectForGroup(EDITOR_USER, "StudyVerifyProject", "Editor Group", false);
        clickFolder(getFolderName());
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.createPermissionsGroup("Author Group");
        _permissionsHelper.assertPermissionSetting("Author Group", "No Permissions");
        _permissionsHelper.setPermissions("Author Group", "Author");
        createUserInProjectForGroup(AUTHOR_USER, "StudyVerifyProject", "Author Group", false);

        clickFolder(getFolderName());
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Views"));
        BootstrapMenu.find(getDriver(),"Add Report").clickSubMenu(true,"Grid View");
        setFormElement(Locator.id("label"), "Report 1");
        clickButton("Create View", defaultWaitForPage);
        clickFolder(getFolderName());
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Views"));
        BootstrapMenu.find(getDriver(),"Add Report").clickSubMenu(true,"Grid View");
        setFormElement(Locator.id("label"), "Report 2");
        clickButton("Create View", defaultWaitForPage);
        clickFolder(getFolderName());
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Views"));
        BootstrapMenu.find(getDriver(),"Add Report").clickSubMenu(true,"Grid View");
        setFormElement(Locator.id("label"), "Report 3");
        clickButton("Create View", defaultWaitForPage);
        clickFolder(getFolderName());
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Views"));
        BootstrapMenu.find(getDriver(),"Add Report").clickSubMenu(true,"Grid View");
        setFormElement(Locator.id("label"), "Report 4");
        clickButton("Create View", defaultWaitForPage);
        clickFolder(getFolderName());
        portalHelper.removeWebPart("Views");
        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        openEditPanel("Report 1");
        //_ext4Helper.selectRadioButton ("Visibility","Hidden");
        _ext4Helper.uncheckCheckbox("Shared");
        sleep(1000);
        _ext4Helper.clickWindowButton("Report 1","Save",0,0);
        _ext4Helper.waitForMaskToDisappear();
        clickFolder(getFolderName());
        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        openEditPanel("Report 2");
        _ext4Helper.selectComboBoxItem("Author","author");
        _ext4Helper.checkCheckbox("Shared");
        sleep(1000);
        _ext4Helper.clickWindowButton("Report 2","Save",0,0);
        _ext4Helper.waitForMaskToDisappear();
        clickFolder(getFolderName());
        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        openEditPanel("Report 3");
        _ext4Helper.selectComboBoxItem("Author","editor");
        _ext4Helper.checkCheckbox("Shared");
        sleep(1000);
        _ext4Helper.clickWindowButton("Report 3","Save",0,0);
        _ext4Helper.waitForMaskToDisappear();
    }

    protected void doVerifySteps()
    {
        impersonate(EDITOR_USER);
        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        openEditPanel("Report 4");
        sleep(1000);
        _ext4Helper.clickWindowButton("Report 4", "Save", 0, 0);
        _ext4Helper.waitForMaskToDisappear();
        stopImpersonating();

        navigateToFolder("StudyVerifyProject", getFolderName());
        sleep(500);
        impersonate(AUTHOR_USER);
        PortalHelper portalHelper1 = new PortalHelper(this);
        portalHelper1.clickWebpartMenuItem("Data Views", true, "Add Report", "Link Report");
        setFormElement(Locator.name("viewName"), "Report 5");
        setFormElement(Locator.name("linkUrl"), "http://www.google.com");
        sleep(1000);
        clickButton("Save", defaultWaitForPage);

        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        openEditPanel("Report 5");
        sleep(1000);
        _ext4Helper.clickWindowButton("Report 5", "Save", 0, 0);
        _ext4Helper.waitForMaskToDisappear();

        verifyMineCheckbox();
    }

    private void openEditPanel(String itemName)
    {
        waitAndClick(Locators.editViewsLink(itemName));
        waitForElement(Ext4Helper.Locators.window(itemName));
    }

    private void verifyMineCheckbox()
    {
        final int myItemCount = 2;
        int initialCount = visibleItemCount();
        assertTrue("Initial item count not greater than test value of 'myItemCount'; test would be invalid", initialCount > myItemCount);
        _ext4Helper.checkCheckbox(Locators.mineCheckbox());
        waitForItemCount("Item count incorrect after checking 'Mine' checkbox", myItemCount);
        // collapse section
        click(Locators.expanderForUncategorized());
        // give time for section to collapse
        waitForItemCount("'Uncategorized' section did not collapse", 0);
        // expand section
        click(Locators.expanderForUncategorized());
        waitForItemCount("'Mine' item count incorrect after collapse and expand", myItemCount);
        _ext4Helper.uncheckCheckbox(Locators.mineCheckbox());
        waitForItemCount("Full item count incorrect after unchecking 'Mine' checkbox", initialCount);
    }

    private void waitForItemCount(String errMsg, int expectedCount)
    {
        waitForEquals(errMsg, ()-> expectedCount, this::visibleItemCount, 500);
    }

    private int visibleItemCount()
    {
        return getElementCount(Locator.tagWithClass("a", "x4-tree-node-text").notHidden());
    }

    public static class Locators
    {
        static Locator.XPathLocator editViewsLink(String dataset)
        {
            return Locator.tag("tr").withClass("x4-grid-tree-node-leaf").withDescendant(Locator.xpath("td/div/a[normalize-space()="+Locator.xq(dataset)+"]")).append("//span").withClass("edit-views-link");
        }

        static Locator.XPathLocator mineCheckbox()
        {
            // The innerHtml of the label for this checkbox is a span (with a qtip) and text '&nbsp;Mine'
            return Locator.tag("span").withText("\u00a0Mine").parent().followingSibling("input").withClass("x4-form-checkbox");
        }

        static Locator.XPathLocator expanderForUncategorized()
        {
            return Locator.tag("span").withText("Uncategorized").precedingSibling("img").withClass("x4-tree-expander");
        }
    }

    private void createUserInProjectForGroup(String userName, String projectName, String groupName, boolean sendEmail)
    {
        if (isElementPresent(Locator.permissionRendered()))
        {
            _permissionsHelper.exitPermissionsUI();
            clickProject(projectName);
        }
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.clickManageGroup(groupName);
        setFormElement(Locator.name("names"), userName);
        if (!sendEmail)
            uncheckCheckbox(Locator.checkboxByName("sendEmail"));
        clickButton("Update Group Membership");
    }
}

