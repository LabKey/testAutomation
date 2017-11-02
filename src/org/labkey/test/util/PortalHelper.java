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
package org.labkey.test.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.SideWebPart;
import org.labkey.test.components.WebPart;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.components.labkey.PortalTab;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsDriver;

import java.util.ArrayList;
import java.util.List;

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
        new SiteNavBar(getDriver()).enterPageAdminMode();
    }

    public void disableTabEditMode()
    {
        new SiteNavBar(getDriver()).exitPageAdminMode();
    }

    public PortalTab activateTab(String tabText)
    {
        return PortalTab.find(tabText, getDriver()).activate();
    }

    @LogMethod(quiet = true)
    private void clickTabMenuItem(@LoggedParam String tabText, boolean wait, @LoggedParam String... items)
    {
        BootstrapMenu tabMenu = new BootstrapMenu(getDriver(),
                BootstrapMenu.Locators.bootstrapMenuContainer()
                        .withChild(Locator.linkWithText(tabText)).findElement(getDriver()));
        tabMenu.clickSubMenu(wait,  items);
    }

    @LogMethod(quiet = true)
    public void moveTab(@LoggedParam String tabText, @LoggedParam Direction direction)
    {
        if (direction.isVertical())
            throw new IllegalArgumentException("Can't move folder tabs vertically.");

        String tabId = tabText.replace(" ", "") + "Tab";
        Locator.XPathLocator tabList = Locator.xpath("//ul[contains(@class, 'lk-nav-tabs-admin')]//li[@role='presentation']");
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
        PortalTab.find(tabText, getDriver()).hide();

        disableTabEditMode();
        assertElementNotPresent(Locator.xpath("//div[@class='lk-nav-tabs-ct']//ul//li//a[text()='" + tabText +"']"));
        enableTabEditMode();
    }

    @LogMethod(quiet = true)
    public void showTab(@LoggedParam String tabText)
    {
        PortalTab.find(tabText, getDriver()).show();
        disableTabEditMode();
        assertElementVisible(Locator.xpath("//div[@class='lk-nav-tabs-ct']//ul//li//a[contains(text(),'" + tabText +"')]"));
        enableTabEditMode();
    }

    @LogMethod(quiet = true)
    public void deleteTab(@LoggedParam String tabText)
    {
        Locator tabLocator = Locator.xpath("//div[@class='lk-nav-tabs-ct']//ul//li//a[text()='" + tabText +"']");
        PortalTab.find(tabText, getDriver()).delete();
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
        mouseOver(Locator.folderTab("+"));
        click(Locator.folderTab("+"));
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
        boolean wasAlreadyInEditMode = enterAdminMode();
        PortalTab portalTab = PortalTab.find(tabText, getDriver());
        portalTab.rename(newName);

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

        if (wasAlreadyInEditMode)
            exitAdminMode();
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
        List<WebElement> webPartElements = Locator.css("div[name=webpart]").findElements(getDriver());
        List<BodyWebPart> bodyWebParts = new ArrayList<>();
        
        for (WebElement el : webPartElements)
        {
            bodyWebParts.add(new BodyWebPart(getDriver(), el));
        }
        
        return bodyWebParts;
    }

    public BodyWebPart getBodyWebPart(String partName)
    {
        return new BodyWebPart(getDriver(), partName);
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
        WebPart webPart = new BodyWebPart(getDriver(), webPartTitle);
        webPart.clickMenuItem(wait, items);
    }

    public boolean enterAdminMode()
    {
        SiteNavBar navBar = new SiteNavBar(getDriver());
        boolean wasInAdminModeAlready = navBar.isInPageAdminMode();
        navBar.enterPageAdminMode();
        return wasInAdminModeAlready;
    }

    public void exitAdminMode()
    {
        new SiteNavBar(getDriver()).exitPageAdminMode();
    }

    public boolean isInAdminMode()
    {
        return new SiteNavBar(getDriver()).isInPageAdminMode();
    }

    @LogMethod(quiet = true)
    public void addWebPart(@LoggedParam String webPartName)
    {
        boolean wasInAdminModeAlready = enterAdminMode();
        waitForElement(Locator.xpath("//option").withText(webPartName));
        WebElement form = Locator.xpath("//form[contains(@action,'addWebPart.view')][.//option[text()='"+webPartName+"']]").findElement(getDriver());
        selectOptionByText(Locator.tag("select").findElement(form), webPartName);
        doAndWaitForPageToLoad(form::submit);
        if (!wasInAdminModeAlready)
            exitAdminMode();
    }

    @LogMethod(quiet = true)
    public void removeWebPart(@LoggedParam String webPartTitle)
    {
        SiteNavBar navBar =new SiteNavBar(getDriver());
        boolean wasInAdminMode = navBar.isInPageAdminMode();
        navBar.enterPageAdminMode();
        WebPart webPart = new BodyWebPart(getDriver(), webPartTitle);
        webPart.remove();
        if (!wasInAdminMode)                // leave the test in the prior state
            navBar.exitPageAdminMode();
    }

    public void addQueryWebPart(@LoggedParam String schemaName)
    {
        addQueryWebPart(null, schemaName, null, null);
    }

    @LogMethod(quiet = true)
    public void addQueryWebPart(@LoggedParam @Nullable String title, @LoggedParam String schemaName, @LoggedParam @Nullable String queryName, @Nullable String viewName)
    {
        addQueryWebPart(title, schemaName, queryName, viewName, WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod(quiet = true)
    public void addQueryWebPart(@LoggedParam @Nullable String title, @LoggedParam String schemaName, @LoggedParam @Nullable String queryName, @Nullable String viewName, int wait)
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

    @LogMethod(quiet = true)
    public void addReportWebPart(@LoggedParam @Nullable String title, @LoggedParam @NotNull String reportId, @Nullable Boolean showTabs, String... visibleSections)
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

    @LogMethod(quiet = true)
    public void moveWebPart(@LoggedParam String webPartTitle, @LoggedParam Direction direction)
    {
        if (direction.isHorizontal())
            throw new IllegalArgumentException("Can't move webpart horizontally.");

        SiteNavBar navBar = new SiteNavBar(getDriver());
        navBar.enterPageAdminMode();
        new BodyWebPart<>(getDriver(), webPartTitle).moveWebPart(direction==Direction.DOWN);
        navBar.exitPageAdminMode();
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
        boolean wasInAdminMode = enterAdminMode();
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

        if (!wasInAdminMode)
            exitAdminMode();
    }

    /**
     * @param webpart
     * @param expectedPermission The permission that is expected to be set.
     * @param expectedFolder The folder that is expected to be selected, null=current folder
     */
    public void checkWebpartPermission(String webpart, String expectedPermission, String expectedFolder)
    {
        new SiteNavBar(getDriver()).enterPageAdminMode();
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
        new SiteNavBar(getDriver()).exitPageAdminMode();
    }

    public enum Direction
    {
        UP("Up", Axis.VERTICAL),
        DOWN("Down", Axis.VERTICAL),
        LEFT("Left", Axis.HORIZONTAL),
        RIGHT("Right", Axis.HORIZONTAL);

        private String _dir;
        private Axis _axis;

        Direction (String dir, Axis axis)
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

        public enum Axis
        {
            HORIZONTAL,
            VERTICAL
        }
    }

    public static class Locators
    {
        public static Locator.XPathLocator webPartTitle()
        {
            return webPart().child(webPartTitleContainer());
        }

        public static Locator.XPathLocator webPartTitle(String title)
        {
            return webPart().child(webPartTitleContainer().withText(title));
        }

        public static Locator.XPathLocator webPart(String title)
        {
            return webPart().withChild(webPartTitleContainer().withAttribute("title", title));
        }

        public static Locator.XPathLocator webPartWithTitleContaining(String partialTitle)
        {
            return webPart().withChild(webPartTitleContainer().withAttributeContaining("title", partialTitle));
        }

        public static Locator.CssLocator activeTab()
        {
            return Locator.css(".lk-nav-tabs-ct > .nav > .active");
        }

        private static Locator.XPathLocator webPartTitleContainer()
        {
            return Locator.xpath("div/div/*").withClass("panel-title");
        }

        public static Locator.XPathLocator webPart()
        {
            return Locator.tagWithName("div", "webpart");
        }
    }
}
