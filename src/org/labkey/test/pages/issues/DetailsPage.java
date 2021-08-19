/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.pages.issues;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class DetailsPage extends BaseIssuePage<DetailsPage.ElementCache>
{
    public DetailsPage(WebDriver driver)
    {
        super(driver);
    }

    public static DetailsPage beginAt(WebDriverWrapper driver, String issueId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueId);
    }

    public static DetailsPage beginAt(WebDriverWrapper driver, String containerPath, String issueId)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "details", Maps.of("issueId", issueId)));
        return new DetailsPage(driver.getDriver());
    }

    public InsertPage clickNewIssue()
    {
        clickAndWait(elementCache().newIssueLink);
        return new InsertPage(getDriver());
    }

    public ListPage clickReturnToGrid()
    {
        clickAndWait(elementCache().returnLink);
        return new ListPage(getDriver());
    }

    public UpdatePage clickUpdate()
    {
        clickAndWait(elementCache().updateLink);
        return new UpdatePage(getDriver());
    }

    public ResolvePage clickResolve()
    {
        clickAndWait(elementCache().resolveLink);
        return new ResolvePage(getDriver());
    }

    public ClosePage clickClose()
    {
        clickAndWait(elementCache().closeLink);
        return new ClosePage(getDriver());
    }

    public ReopenPage clickReOpen()
    {
        clickAndWait(elementCache().reopenLink);
        return new ReopenPage(getDriver());
    }

    public LabKeyPage clickPrint()
    {
        elementCache().getMoreMenu().clickSubMenu(true, "Print");
        return new LabKeyPage(getDriver());
    }

    public EmailPrefsPage clickEmailPrefs()
    {
        elementCache().getMoreMenu().clickSubMenu(true, "Email preferences");
        return new EmailPrefsPage(getDriver());
    }

    public InsertPage clickCreateRelatedIssue(String projectName, String issueName)
    {
        elementCache().getMoreMenu().clickSubMenu(false, "Create related issue");
        new Window.WindowFinder(getDriver()).withTitle("Create Related Issue").waitFor();
        _ext4Helper.selectComboBoxItem("Folder:", "/" + projectName + " (" + issueName + ")");
        clickButton("Create Issue");
        return new InsertPage(getDriver());
    }

    public DetailsPage clickShowRelatedIssueComment()
    {
        elementCache().getMoreMenu().clickSubMenu(false, "Show related comments");
        return this;
    }

    public DetailsPage clickHideRelatedIssueComment()
    {
        elementCache().getMoreMenu().clickSubMenu(false, "Hide related comments");
        return this;
    }

    public DataRegionTable getRelatedIssueTable()
    {
        return new DataRegionTable("related", getDriver());
    }

    @Override
    public ElementCache newElementCache()
    {
        return new ElementCache();
    }

    public class ElementCache extends BaseIssuePage.ElementCache
    {
        protected WebElement searchButton = Locator.tagWithAttribute("a", "data-original-title", "Search").findWhenNeeded(this);
        protected WebElement newIssueLink = Locator.lkButton("New Issue").findWhenNeeded(this);
        protected WebElement returnLink = Locator.linkWithText("return to grid").findWhenNeeded(this); //gone in newUI
        protected WebElement updateLink = Locator.lkButton("Update").findWhenNeeded(this);
        protected WebElement resolveLink = Locator.lkButton("Resolve").findWhenNeeded(this);
        protected WebElement closeLink = Locator.lkButton("Close").findWhenNeeded(this);
        protected WebElement reopenLink = Locator.lkButton("Reopen").findWhenNeeded(this);
        protected WebElement printLink = Locator.linkWithText("print").findWhenNeeded(this); //in menu in newUI
        protected WebElement emailPrefsLink = Locator.linkWithText("email prefs").findWhenNeeded(this); //in menu in newUI

        public ElementCache()
        {
            Locator.byClass("currentIssue").waitForElement(this, 10000);
        }

        protected BootstrapMenu getMoreMenu()
        {
            return BootstrapMenu.find(getDriver(), "More");
        }
    }
}
