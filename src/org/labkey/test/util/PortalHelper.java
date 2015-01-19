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
package org.labkey.test.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.SideWebPart;
import org.labkey.test.components.WebPart;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * TODO: Move appropriate functionality into {@link org.labkey.test.pages.PortalBodyPanel} and {@link org.labkey.test.components.WebPart}
 */
public class PortalHelper extends AbstractHelper
{
    public PortalHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    public void enableTabEditMode()
    {
        _test.click(Locator.linkWithTitle("Toggle Edit Mode"));
        _test.waitForElement(Locator.xpath("//div[@class='button-bar tab-edit-mode-enabled']"));
    }

    public void disableTabEditMode()
    {
        _test.click(Locator.linkWithTitle("Toggle Edit Mode"));
        _test.waitForElement(Locator.xpath("//div[@class='button-bar tab-edit-mode-disabled']"));
    }

    @LogMethod(quiet = true)
    private void clickTabMenuItem(@LoggedParam String tabText, boolean wait, @LoggedParam String... items)
    {
        _test.mouseOver(Locator.linkWithText(tabText));
        Locator tabMenuXPath = Locator.xpath("//div[@class='labkey-app-bar']//ul//li//a[text()='" + tabText +"']/following-sibling::span//a");
        _test.waitForElement(tabMenuXPath);
        _test._extHelper.clickExtMenuButton(wait, tabMenuXPath, items);
    }

    @LogMethod(quiet = true)
    public void moveTab(@LoggedParam String tabText, @LoggedParam Direction direction)
    {
        if (direction.isVertical())
            throw new IllegalArgumentException("Can't move folder tabs vertically.");

        String tabId = tabText.replace(" ", "") + "Tab";
        Locator.XPathLocator tabList = Locator.xpath("//ul[contains(@class, 'tab-nav')]//li");
        Locator.XPathLocator tabLink = Locator.xpath("//a[@id=" + Locator.xq(tabId) + "]");

        int tabCount = _test.getElementCount(tabList) - 1;
        int startIndex = _test.getElementIndex(tabList.withDescendant(tabLink)); // zero-based
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

        _test.waitForElement(tabList.index(expectedEndIndex).withDescendant(tabLink));
    }

    @LogMethod(quiet = true)
    public void hideTab(@LoggedParam String tabText)
    {
        clickTabMenuItem(tabText, true, "Hide");
        disableTabEditMode();
        _test.assertElementNotVisible(Locator.xpath("//div[@class='labkey-app-bar']//ul//li//a[text()='" + tabText +"']"));
        enableTabEditMode();
    }

    @LogMethod(quiet = true)
    public void showTab(@LoggedParam String tabText)
    {
        clickTabMenuItem(tabText, true, "Show");
        disableTabEditMode();
        _test.assertElementVisible(Locator.xpath("//div[@class='labkey-app-bar']//ul//li//a[text()='" + tabText +"']"));
        enableTabEditMode();
    }

    @LogMethod(quiet = true)
    public void deleteTab(@LoggedParam String tabText)
    {
        Locator tabLocator = Locator.xpath("//div[@class='labkey-app-bar']//ul//li//a[text()='" + tabText +"']");
        clickTabMenuItem(tabText, true, "Delete");
        _test.waitForElementToDisappear(tabLocator);
        _test.assertElementNotPresent(tabLocator);
    }

    @LogMethod(quiet = true)
    public void addTab(@LoggedParam String tabName)
    {
        addTab(tabName, null);
    }

    @LogMethod(quiet = true)
    public void addTab(@LoggedParam String tabName, @Nullable @LoggedParam String expectedError)
    {
        _test.click(Locator.linkWithText("+"));
        _test.waitForText("Add Tab");
        _test.setFormElement(Locator.input("tabName"), tabName);

        if (expectedError != null)
        {
            _test.clickButton("Ok", 0);
            _test.waitForText(expectedError);
            _test.clickButton("OK", 0);
        }
        else
        {
            _test.clickButton("Ok");
            _test.waitForElement(Locator.folderTab(tabName));
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
        _test.waitForText("Rename Tab");
        _test.setFormElement(Locator.input("tabName"),newName);
        _test.clickButton("Ok", 0);
        if (expectedError != null)
        {
            _test.waitForText(expectedError);
            _test.clickButton("OK", 0);
            // Close the rename tab window.
            _test.clickButton("Cancel", 0);
        }
        else
        {
            _test.waitForElement(Locator.linkWithText(newName));
            _test.assertElementNotPresent(Locator.linkWithText(tabText));
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
        List<WebElement> webPartElements = Locator.css("#bodypanel > table[name=webpart]").findElements(_test.getDriver());
        List<BodyWebPart> bodyWebParts = new ArrayList<>();
        
        for (WebElement el : webPartElements)
        {
            bodyWebParts.add(new BodyWebPart(_test, el));
        }
        
        return bodyWebParts;
    }

    public List<SideWebPart> getSideWebParts()
    {
        List<WebElement> webPartElements = Locator.css(".labkey-side-panel > table[name=webpart]").findElements(_test.getDriver());
        List<SideWebPart> sideWebParts = new ArrayList<>();

        for (WebElement el : webPartElements)
        {
            sideWebParts.add(new SideWebPart(_test, el));
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
        _test._extHelper.clickExtMenuButton(wait, Locator.xpath("//img[@id='more-" + webPartTitle.toLowerCase() + "']"), items);
    }

    @LogMethod(quiet = true)public void addWebPart(@LoggedParam String webPartName)
    {
        _test.waitForElement(Locator.xpath("//option").withText(webPartName));
        Locator.XPathLocator form = Locator.xpath("//form[contains(@action,'addWebPart.view')][.//option[text()='"+webPartName+"']]");
        _test.selectOptionByText(form.append("//select"), webPartName);
        _test.submit(form);
    }

    @LogMethod(quiet = true)public void removeWebPart(@LoggedParam String webPartTitle)
    {
        int startCount = _test.getElementCount(Locators.webPartTitle(webPartTitle));
        if (_test.isElementPresent(Locators.sideWebpartTitle.withText(webPartTitle)))
        {
            clickWebpartMenuItem(webPartTitle, false, "Remove From Page");
        }
        else
        {
            Locator.XPathLocator removeButton = Locator.xpath("//tr[th[@title='"+webPartTitle+"']]//a[img[@title='Remove From Page']]");
            _test.click(removeButton);
        }
        _test.waitForElementToDisappear(Locators.webPartTitle(webPartTitle).index(startCount), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElementToDisappear(Locator.css("div.x4-form-display-field").withText("Saving..."));
    }

    public void addQueryWebPart(@LoggedParam String schemaName)
    {
        addQueryWebPart(null, schemaName, null, null);
    }

    @LogMethod(quiet = true)public void addQueryWebPart(@LoggedParam @Nullable String title, @LoggedParam String schemaName, @LoggedParam @Nullable String queryName, @Nullable String viewName)
    {
        addWebPart("Query");

        if (title != null)
            _test.setFormElement(Locator.name("title"), title);

        _test.waitForElement(Locator.css(".schema-loaded-marker"));
        _test._ext4Helper.selectComboBoxItem(Locator.id("schemaName"), schemaName);

        if (queryName != null)
        {
            _test.click(Locator.xpath("//input[@type='button' and @id='selectQueryContents-inputEl']"));
            _test.waitForElement(Locator.css(".query-loaded-marker"));
            _test._ext4Helper.selectComboBoxItem(Locator.id("queryName"), queryName);
            _test.waitForElement(Locator.css(".view-loaded-marker"));

            if (viewName != null)
                _test._ext4Helper.selectComboBoxItem(Locator.id("viewName"), viewName);
        }

        _test.clickButton("Submit");

        if (title == null)
        {
            if (queryName == null)
                title = schemaName.substring(0, 1).toUpperCase() + schemaName.substring(1) + " Queries";
            else
                title = queryName;
        }

        _test.waitForElement(Locator.xpath("//span").withClass("labkey-wp-title-text").withText(title));
    }

    public void addReportWebPart(@LoggedParam String reportId)
    {
        addReportWebPart(null, reportId, null);
    }

    @LogMethod(quiet = true)public void addReportWebPart(@LoggedParam @Nullable String title, @LoggedParam @NotNull String reportId, @Nullable Boolean showTabs, String... visibleSections)
    {
        addWebPart("Report");

        if (title != null)
            _test.setFormElement(Locator.name("title"), title);

        _test.selectOptionByText(Locator.id("reportId"), reportId);

        if (showTabs != null)
        {
            if(showTabs)
                _test.checkCheckbox(Locator.id("showTabs"));
            else
                _test.uncheckCheckbox(Locator.id("showTabs"));
        }

        if (visibleSections.length > 0)
            _test.waitForElement(Locator.id("showSection"));
        for (String section : visibleSections)
        {
            _test.selectOptionByValue(Locator.id("showSection"), section);
        }

        _test.clickButton("Submit");

        _test.waitForElement(Locator.xpath("//span").withClass("labkey-wp-title-text").withText(title != null ? title : "Reports"));
    }

    @LogMethod(quiet = true)public void moveWebPart(@LoggedParam String webPartTitle, @LoggedParam Direction direction)
    {
        if (direction.isHorizontal())
            throw new IllegalArgumentException("Can't move webpart horizontally.");

        Locator.XPathLocator webPart = Locator.xpath("//table[@name='webpart'][.//span[contains(@class, 'labkey-wp-title-text') and text()="+Locator.xq(webPartTitle)+"]]");

        int initialIndex = (_test.getElementIndex(webPart) / 2);

        Locator.XPathLocator portalPanel = Locator.xpath("//td[./table[@name='webpart']//span[contains(@class, 'labkey-wp-title-text') and text()="+Locator.xq(webPartTitle)+"]]");
        String panelClass = portalPanel.findElement(((BaseWebDriverTest)_test).getDriver()).getAttribute("class");
        if (panelClass.contains("labkey-body-panel"))
        {
            _test.click(webPart.append("//img[@title='Move "+direction+"']"));
        }
        else if (panelClass.contains("labkey-side-panel"))
        {
            clickWebpartMenuItem(webPartTitle, false, "Move " + direction.toString());
        }
        else
        {
            fail("Unable to analyze webpart type. PortalHelper.java needs updating.");
        }

        // TODO: Check final webpart index

        _test._ext4Helper.waitForMaskToDisappear();
    }

    public void openWebpartPermissionWindow(String webpart)
    {
        clickWebpartMenuItem(webpart, false, "Permissions");
        _test._ext4Helper.waitForMask();
        _test.waitForText("Check Permission");
    }

    /**
     * @param webpart
     * @param permission
     * @param folder null=current folder
     */
    public void setWebpartPermission(String webpart, String permission, String folder)
    {
        openWebpartPermissionWindow(webpart);

        _test._ext4Helper.selectComboBoxItem("Required Permission:", permission);

        if(folder==null)
            _test._ext4Helper.selectRadioButton("Check Permission On:", "Current Folder");
        else
        {
            _test._ext4Helper.selectRadioButton("Check Permission On:", "Choose Folder");
            _test.click(Locator.tagWithText("div", folder));
        }
        _test.click(Locator.tagWithText("span", "Save"));
    }

    /**
     * @param webpart
     * @param expectedPermission The permission that is expected to be set.
     * @param expectedFolder The folder that is expected to be selected, null=current folder
     */
    public void checkWebpartPermission(String webpart, String expectedPermission, String expectedFolder)
    {
        openWebpartPermissionWindow(webpart);

        _test.assertFormElementEquals("permission", expectedPermission);

        if(expectedFolder == null)
        {
            _test.assertFormElementEquals("permissionContainer", "");
        }
        else
        {
            _test.assertFormElementEquals("permissionContainer", expectedFolder);
        }

        _test.click(Locator.tagWithText("span", "Cancel"));
        _test._ext4Helper.waitForMaskToDisappear();
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
            return Locator.xpath("//img[@id='more-" + title.toLowerCase() + "']");
        }

        public static Locator.CssLocator bodyWebpartTitle = Locator.css("#bodypanel .labkey-wp-title-text");
        public static Locator.CssLocator sideWebpartTitle = Locator.css(".labkey-side-panel .labkey-wp-title-text");

        public static Locator.XPathLocator webPart(String title)
        {
            return webPart.withPredicate(Locator.xpath("tbody/tr/th").withAttribute("title", title));
        }
    }
}
