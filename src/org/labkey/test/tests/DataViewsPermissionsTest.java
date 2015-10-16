/*
 * Copyright (c) 2014 LabKey Corporation
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
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.PortalHelper;

@Category({DailyA.class})
public class DataViewsPermissionsTest extends StudyBaseTest
{

    PortalHelper portalHelper = new PortalHelper(this);

    protected void doCreateSteps()
    {
        importStudy();

        clickFolder("My Study");
        portalHelper.addWebPart("Data Views");
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.uncheckInheritedPermissions();
        clickButton("Save and Finish", defaultWaitForPage);
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.createPermissionsGroup("Editor Group");
        _permissionsHelper.assertPermissionSetting("Editor Group", "No Permissions");
        _permissionsHelper.setPermissions("Editor Group", "Editor");
        createUserInProjectForGroup("Editor@test.com", "StudyVerifyProject", "Editor Group", false);
        clickFolder("My Study");
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.createPermissionsGroup("Author Group");
        _permissionsHelper.assertPermissionSetting("Author Group", "No Permissions");
        _permissionsHelper.setPermissions("Author Group", "Author");
        createUserInProjectForGroup("Author@test.com", "StudyVerifyProject", "Author Group", false);

        clickFolder("My Study");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Views"));
        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Add Report"), "Grid View");
        setFormElement(Locator.id("label"), "Report 1");
        clickButton("Create View", defaultWaitForPage);
        clickFolder("My Study");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Views"));
        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Add Report"), "Grid View");
        setFormElement(Locator.id("label"), "Report 2");
        clickButton("Create View", defaultWaitForPage);
        clickFolder("My Study");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Views"));
        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Add Report"), "Grid View");
        setFormElement(Locator.id("label"), "Report 3");
        clickButton("Create View", defaultWaitForPage);
        clickFolder("My Study");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Views"));
        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Add Report"), "Grid View");
        setFormElement(Locator.id("label"), "Report 4");
        clickButton("Create View", defaultWaitForPage);
        clickFolder("My Study");
        portalHelper.removeWebPart("Views");
        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        openEditPanel("Report 1");
        //_ext4Helper.selectRadioButton ("Visibility","Hidden");
        _ext4Helper.uncheckCheckbox("Shared");
        _ext4Helper.clickWindowButton("Report 1","Save",0,0);
        sleep(500);
        clickFolder("My Study");
        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        openEditPanel("Report 2");
        _ext4Helper.selectComboBoxItem("Author","author");
        _ext4Helper.checkCheckbox("Shared");
        _ext4Helper.clickWindowButton("Report 2","Save",0,0);
        sleep(500);
        clickFolder("My Study");
        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        openEditPanel("Report 3");
        _ext4Helper.selectComboBoxItem("Author","editor");
        _ext4Helper.checkCheckbox("Shared");
        _ext4Helper.clickWindowButton("Report 3","Save",0,0);
    }

    protected void doVerifySteps()

    {
        sleep(500);
        impersonate ("editor@test.com");
        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        openEditPanel("Report 4");
        _ext4Helper.clickWindowButton("Report 4","Save",0,0);
        sleep(500);
        stopImpersonating();

        clickProject("StudyVerifyProject");
        clickFolder("My Study");
        sleep(500);
        impersonate("author@test.com");
        PortalHelper portalHelper1 = new PortalHelper(this);
        portalHelper1.clickWebpartMenuItem("Data Views", true, "Add Report", "Link Report");
        setFormElement(Locator.name("viewName"), "Report 5");
        setFormElement(Locator.name("linkUrl"), "http://www.google.com");
        clickButton("Save", defaultWaitForPage);

        click(Locator.tag("a").withAttributeContaining("href", "editDataViews"));
        openEditPanel("Report 5");
        _ext4Helper.clickWindowButton("Report 5","Save",0,0);


    }

    private void openEditPanel(String itemName)
    {
        waitAndClick(Locators.editViewsLink(itemName));
        waitForElement(Ext4Helper.Locators.window(itemName));
    }
    public static class Locators
    {
        public static Locator.XPathLocator editViewsLink(String dataset)
        {
            return Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").withDescendant(Locator.xpath("td/div/a[normalize-space()="+Locator.xq(dataset)+"]")).append("//span").withClass("edit-views-link");
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

