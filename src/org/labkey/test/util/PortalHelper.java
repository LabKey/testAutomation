/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
package org.labkey.test.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.SideWebPart;
import org.labkey.test.components.WebPart;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.SiteNavBar;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsDriver;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * TODO: Move appropriate functionality into {@link org.labkey.test.pages.PortalBodyPanel} and {@link org.labkey.test.components.WebPart}
 */
public class PortalHelper extends WebDriverWrapper
{
    protected WrapsDriver _driverWrapper;

    public PortalHelper(WrapsDriver driverWrapper)
    {
        _driverWrapper = driverWrapper;
    }

    public PortalHelper(WebDriver driver)
    {
        this(() -> driver);
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return _driverWrapper.getWrappedDriver();
    }

    public void enableTabEditMode()
    {
        click(Locator.linkWithTitle("Toggle Edit Mode"));
        waitForElement(Locator.xpath("//div[@class='button-bar tab-edit-mode-enabled']"));
    }

    public void disableTabEditMode()
    {
        click(Locator.linkWithTitle("Toggle Edit Mode"));
        waitForElement(Locator.xpath("//div[@class='button-bar tab-edit-mode-disabled']"));
    }

    @LogMethod(quiet = true)
    private void clickTabMenuItem(@LoggedParam String tabText, boolean wait, @LoggedParam String... items)
    {
        mouseOver(Locator.linkWithText(tabText));
        Locator tabMenuXPath = Locator.xpath("//div[@class='labkey-app-bar']//ul//li//a[text()='" + tabText +"']/following-sibling::span//a");
        waitForElement(tabMenuXPath);
        _extHelper.clickExtMenuButton(wait, tabMenuXPath, items);
    }

    @LogMethod(quiet = true)
    public void moveTab(@LoggedParam String tabText, @LoggedParam Direction direction)
    {
        if (direction.isVertical())
            throw new IllegalArgumentException("Can't move folder tabs vertically.");

        String tabId = tabText.replace(" ", "") + "Tab";
        Locator.XPathLocator tabList = Locator.xpath("//ul[contains(@class, 'tab-nav')]//li");
        Locator.XPathLocator tabLink = Locator.xpath("//a[@id=" + Locator.xq(tabId) + "]");

        int tabCount = getElementCount(tabList) - 1;
        int startIndex = getElementIndex(tabList.withDescendant(tabLink)); // zero-based
        int expectedEndIndex = startIndex;

        clickTabMenuItem(tabText, false, "Move", direction.toString());

        switch (direction)
        {
            case LEFT:
                if (startIndex > 0)
                    expectedEndIndex = startIndex - 1;
                break;
            case RIGHT:
                if (startIndex < (tabCount - 2)) // offset by 2 due to the '+' and the 'pencil' tabs
                    expectedEndIndex = startIndex + 1;
                break;
        }

        waitForElement(tabList.index(expectedEndIndex).withDescendant(tabLink));
    }

    @LogMethod(quiet = true)
    public void hideTab(@LoggedParam String tabText)
    {
        clickTabMenuItem(tabText, true, "Hide");
        disableTabEditMode();
        assertElementNotVisible(Locator.xpath("//div[@class='labkey-app-bar']//ul//li//a[text()='" + tabText +"']"));
        enableTabEditMode();
    }

    @LogMethod(quiet = true)
    public void showTab(@LoggedParam String tabText)
    {
        clickTabMenuItem(tabText, true, "Show");
        disableTabEditMode();
        assertElementVisible(Locator.xpath("//div[@class='labkey-app-bar']//ul//li//a[text()='" + tabText +"']"));
        enableTabEditMode();
    }

    @LogMethod(quiet = true)
    public void deleteTab(@LoggedParam String tabText)
    {
        Locator tabLocator = Locator.xpath("//div[@class='labkey-app-bar']//ul//li//a[text()='" + tabText +"']");
        clickTabMenuItem(tabText, true, "Delete");
        waitForElementToDisappear(tabLocator);
        assertElementNotPresent(tabLocator);
    }

    @LogMethod(quiet = true)
    public void addTab(@LoggedParam String tabName)
    {
        addTab(tabName, null);
    }

    @LogMethod(quiet = true)
    public void addTab(@LoggedParam String tabName, @Nullable @LoggedParam String expectedError)
    {
        click(Locator.linkWithText("+"));
        waitForText("Add Tab");
        setFormElement(Locator.input("tabName"), tabName);

        if (expectedError != null)
        {
            clickButton("Ok", 0);
            waitForText(expectedError);
            clickButton("OK", 0);
        }
        else
        {
            clickButton("Ok");
            waitForElement(Locator.folderTab(tabName));
        }
    }

    @LogMethod(quiet = true)
    public void renameTab(@LoggedParam String tabText, @LoggedParam String newName)
    {
        renameTab(tabText, newName, null);
    }

    @LogMethod(quiet = true)
    public void renameTab(@LoggedParam String tabText, @LoggedParam String newName, @Nullable @LoggedParam String expectedError)
    {
        clickTabMenuItem(tabText, false, "Rename");
        waitForText("Rename Tab");
        setFormElement(Locator.input("tabName"),newName);
        clickButton("Ok", 0);
        if (expectedError != null)
        {
            waitForText(expectedError);
            clickButton("OK", 0);
            // Close the rename tab window.
            clickButton("Cancel", 0);
        }
        else
        {
            waitForElement(Locator.linkWithText(newName));
            assertElementNotPresent(Locator.linkWithText(tabText));
        }
    }

    public List<String> getWebPartTitles()
    {
        List<WebPart> webparts = new ArrayList<>();
        webparts.addAll(getBodyWebParts());
        webparts.addAll(getSideWebParts());

        List<String> webpartTitles = new ArrayList<>();

        for (WebPart wp : webparts)
        {
            webpartTitles.add(wp.getTitle());
        }

        return webpartTitles;
    }

    public List<BodyWebPart> getBodyWebParts()
    {
        List<WebElement> webPartElements = Locator.css("#bodypanel > table[name=webpart]").findElements(getDriver());
        List<BodyWebPart> bodyWebParts = new ArrayList<>();
        
        for (WebElement el : webPartElements)
        {
            bodyWebParts.add(new BodyWebPart(getDriver(), el));
        }
        
        return bodyWebParts;
    }

    public BodyWebPart getBodyWebPart(String partName)
    {
        WebElement el = Locator.css("#bodypanel > table[name=webpart]").containing(partName).findElement(getDriver());
        return new BodyWebPart(getDriver(), el);
    }

    public List<SideWebPart> getSideWebParts()
    {
        List<WebElement> webPartElements = Locator.css(".labkey-side-panel > table[name=webpart]").findElements(getDriver());
        List<SideWebPart> sideWebParts = new ArrayList<>();

        for (WebElement el : webPartElements)
        {
            sideWebParts.add(new SideWebPart(getDriver(), el));
        }

        return sideWebParts;
    }

    /**
     * Allows test code to navigate to a Webpart Ext-based navigation menu.
     * @param webPartTitle title (not name) of webpart to be clicked.  Multiple web parts with the same title not supported.
     * @param items
     */
    public void clickWebpartMenuItem(String webPartTitle, String... items)
    {
        clickWebpartMenuItem(webPartTitle, true, items);
    }

    public void clickWebpartMenuItem(String webPartTitle, boolean wait, String... items)
    {
        if (LabKeySiteWrapper.IS_BOOTSTRAP_LAYOUT)
        {
            WebPart webPart = new BodyWebPart(getDriver(), webPartTitle);
            webPart.clickMenuItem(wait, items);
        }
        else
        {
            _extHelper.clickExtMenuButton(wait, Locator.xpath("//span[@id='more-" + webPartTitle.toLowerCase() + "']"), items);
        }
    }

    public void enterAdminMode()
    {
        if (LabKeySiteWrapper.IS_BOOTSTRAP_LAYOUT)
            new SiteNavBar(getDriver()).enterAdminMode();
    }

    public void exitAdminMode()
    {
        if (LabKeySiteWrapper.IS_BOOTSTRAP_LAYOUT)
            new SiteNavBar(getDriver()).exitAdminMode();
    }

    public boolean isInAdminMode()
    {
        if (LabKeySiteWrapper.IS_BOOTSTRAP_LAYOUT)
            return new SiteNavBar(getDriver()).isInAdminMode();
        return false;
    }

    @LogMethod(quiet = true)public void addWebPart(@LoggedParam String webPartName)
    {
        enterAdminMode();
        waitForElement(Locator.xpath("//option").withText(webPartName));
        Locator.XPathLocator form = Locator.xpath("//form[contains(@action,'addWebPart.view')][.//option[text()='"+webPartName+"']]");
        selectOptionByText(form.append("//select"), webPartName);
        clickAndWait(form.append(Locator.lkButton("Add")));
        exitAdminMode();
    }

    @LogMethod(quiet = true)public void removeWebPart(@LoggedParam String webPartTitle)
    {
        if (LabKeySiteWrapper.IS_BOOTSTRAP_LAYOUT)
        {
            WebPart webPart = new BodyWebPart(getDriver(), webPartTitle);
            webPart.delete();
        }
        else
        {
            int startCount = getElementCount(Locators.webPartTitle(webPartTitle));
            clickWebpartMenuItem(webPartTitle, false, "Remove From Page");
            waitForElementToDisappear(Locators.webPartTitle(webPartTitle).index(startCount), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            waitForElementToDisappear(Locator.css("div.x4-form-display-field").withText("Saving..."));
        }
    }

    public void addQueryWebPart(@LoggedParam String schemaName)
    {
        addQueryWebPart(null, schemaName, null, null);
    }

    @LogMethod(quiet = true)public void addQueryWebPart(@LoggedParam @Nullable String title, @LoggedParam String schemaName, @LoggedParam @Nullable String queryName, @Nullable String viewName)
    {
        addQueryWebPart(title, schemaName, queryName, viewName, WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod(quiet = true)public void addQueryWebPart(@LoggedParam @Nullable String title, @LoggedParam String schemaName, @LoggedParam @Nullable String queryName, @Nullable String viewName, int wait)
    {
        addWebPart("Query");

        waitForElement(Locator.css(".schema-loaded-marker"));

        if (title != null)
            setFormElement(Locator.name("title"), title);

        _ext4Helper.selectComboBoxItem(Locator.id("schemaName"), schemaName);

        if (queryName != null)
        {
            click(Locator.xpath("//input[@type='button' and @id='selectQueryContents-inputEl']"));
            waitForElement(Locator.css(".query-loaded-marker"), wait);
            _ext4Helper.selectComboBoxItem(Locator.id("queryName"), queryName);
            waitForElement(Locator.css(".view-loaded-marker"), wait);

            if (viewName != null)
                _ext4Helper.selectComboBoxItem(Locator.id("viewName"), viewName);
        }

        clickButton("Submit");

        if (title == null)
        {
            if (queryName == null)
                title = schemaName.substring(0, 1).toUpperCase() + schemaName.substring(1) + " Queries";
            else
                title = queryName;
        }

        waitForElement(Locator.xpath("//span").withClass("labkey-wp-title-text").withText(title));
    }

    public void addReportWebPart(@LoggedParam String reportId)
    {
        addReportWebPart(null, reportId, null);
    }

    @LogMethod(quiet = true)public void addReportWebPart(@LoggedParam @Nullable String title, @LoggedParam @NotNull String reportId, @Nullable Boolean showTabs, String... visibleSections)
    {
        addWebPart("Report");

        if (title != null)
            setFormElement(Locator.name("title"), title);

        selectOptionByText(Locator.id("reportId"), reportId);

        if (showTabs != null)
        {
            if(showTabs)
                checkCheckbox(Locator.id("showTabs"));
            else
                uncheckCheckbox(Locator.id("showTabs"));
        }

        if (visibleSections.length > 0)
            waitForElement(Locator.id("showSection"));
        for (String section : visibleSections)
        {
            selectOptionByValue(Locator.id("showSection"), section);
        }

        clickButton("Submit");

        waitForElement(Locator.xpath("//span").withClass("labkey-wp-title-text").withText(title != null ? title : "Reports"));
    }

    @LogMethod(quiet = true)public void moveWebPart(@LoggedParam String webPartTitle, @LoggedParam Direction direction)
    {
        if (direction.isHorizontal())
            throw new IllegalArgumentException("Can't move webpart horizontally.");

        Locator.XPathLocator webPartLoc = Locator.xpath("//table[@name='webpart']").withPredicate(Locator.tagWithClass("span", "labkey-wp-title-text").withText(webPartTitle));
        final WebElement webPart = webPartLoc.findElement(getDriver());

        int initialIndex = (getElementIndex(webPart) / 2); // Each webpart has an adjacent <br>

        Locator.XPathLocator portalPanel = Locator.xpath("//td[./table[@name='webpart']//span[contains(@class, 'labkey-wp-title-text') and text()="+Locator.xq(webPartTitle)+"]]");
        String panelClass = portalPanel.findElement(getDriver()).getAttribute("class");
        if (panelClass.contains("labkey-side-panel") || panelClass.contains("labkey-body-panel"))
        {
            clickWebpartMenuItem(webPartTitle, false, "Move " + direction.toString());
        }
        else
        {
            fail("Unable to analyze webpart type. PortalHelper.java needs updating.");
        }

        final int expectedIndex = initialIndex + (direction == Direction.DOWN ? 1 : -1);
        waitFor(() -> (getElementIndex(webPart) / 2) == expectedIndex,
                "Move WebPart failed", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void openWebpartPermissionWindow(String webpart)
    {
        clickWebpartMenuItem(webpart, false, "Permissions");
        _ext4Helper.waitForMask();
        waitForText("Check Permission");
    }

    /**
     * @param webpart
     * @param permission
     * @param folder null=current folder
     */
    public void setWebpartPermission(String webpart, String permission, String folder)
    {
        openWebpartPermissionWindow(webpart);

        _ext4Helper.selectComboBoxItem("Required Permission:", permission);

        if(folder==null)
            _ext4Helper.selectRadioButton("Check Permission On:", "Current Folder");
        else
        {
            _ext4Helper.selectRadioButton("Check Permission On:", "Choose Folder");
            click(Locator.tagWithText("div", folder));
        }
        click(Locator.tagWithText("span", "Save"));
        _ext4Helper.waitForMaskToDisappear();
    }

    /**
     * @param webpart
     * @param expectedPermission The permission that is expected to be set.
     * @param expectedFolder The folder that is expected to be selected, null=current folder
     */
    public void checkWebpartPermission(String webpart, String expectedPermission, String expectedFolder)
    {
        openWebpartPermissionWindow(webpart);

        assertFormElementEquals(Locator.name("permission"), expectedPermission);

        if(expectedFolder == null)
        {
            assertFormElementEquals(Locator.name("permissionContainer"), "");
        }
        else
        {
            assertFormElementEquals(Locator.name("permissionContainer"), expectedFolder);
        }

        click(Locator.tagWithText("span", "Cancel"));
        _ext4Helper.waitForMaskToDisappear();
    }

    public static enum Direction
    {
        UP("Up", Axis.VERTICAL),
        DOWN("Down", Axis.VERTICAL),
        LEFT("Left", Axis.HORIZONTAL),
        RIGHT("Right", Axis.HORIZONTAL);

        private String _dir;
        private Axis _axis;

        private Direction (String dir, Axis axis)
        {
            _dir = dir;
            _axis = axis;
        }

        public String toString()
        {
            return _dir;
        }

        public Boolean isHorizontal()
        {
            return _axis == Axis.HORIZONTAL;
        }

        public Boolean isVertical()
        {
            return _axis == Axis.VERTICAL;
        }

        public static enum Axis
        {
            HORIZONTAL,
            VERTICAL
        }
    }

    public static class Locators
    {
        public static Locator.XPathLocator webPartTitle = Locator.xpath("//span").withClass("labkey-wp-title-text");
        public static final Locator.XPathLocator webPart = Locator.tagWithName("table", "webpart");

        public static Locator.XPathLocator webPartTitle(String title)
        {
            return webPartTitle.withText(title);
        }

        public static Locator.XPathLocator webPartTitleMenu(String title)
        {
            return Locator.xpath("//span[@id='more-" + title.toLowerCase() + "']");
        }

        public static Locator.CssLocator bodyWebpartTitle = Locator.css("#bodypanel .labkey-wp-title-text");
        public static Locator.CssLocator sideWebpartTitle = Locator.css(".labkey-side-panel .labkey-wp-title-text");

        public static Locator.XPathLocator webPart(String title)
        {
            return webPart.withPredicate(Locator.xpath("tbody/tr/th").withAttribute("title", title));
        }

        public static Locator.XPathLocator webPartWithTitleContaining(String partialTitle)
        {
            return webPart.withPredicate(Locator.xpath("tbody/tr/th").withAttributeContaining("title", partialTitle));
        }

        public static Locator.CssLocator activeTab = Locator.css(".tab-nav-active");

    }
}
