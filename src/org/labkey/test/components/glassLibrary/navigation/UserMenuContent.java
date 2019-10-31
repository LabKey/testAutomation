/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.navigation;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class UserMenuContent extends WebDriverComponent
{
    private final WebDriver _driver;
    private final WebElement _menuContentElement;

    // TODO Revisit This looks very similar to the ProductMenuContent constructor. Should they share a base class?
    public UserMenuContent(WebDriver driver)
    {

        this(Locators.menuList.findElement(driver), driver);
        getWrapper().waitForElementToBeVisible(Locators.menuList);
        int columnCount = Locators.menuListItem.findElements(getDriver()).size();
        getWrapper().log("User Menu Column count before: " + columnCount);
        getWrapper().shortWait().until(LabKeyExpectedConditions.animationIsDone(Locators.menuList));

        // Even though the animation is done the data may not be there, so wait until some is.
        for(int tries = 0; tries < 5; tries++)
        {
            if(Locators.menuListItem.findElements(getDriver()).size() > columnCount)
            {
                break;
            }
            else
            {
                WebDriverWrapper.sleep(500);
            }
        }

        getWrapper().log("User Menu Column count after: " + columnCount);
    }

    protected UserMenuContent(WebElement element, WebDriver driver)
    {
        _menuContentElement = element;
        _driver = driver;
    }

    public List<String> getMenuItems()
    {
        List<String> menuItems = new ArrayList<>();

        List<WebElement> menuElements = getMenuItemElements();
        menuElements.forEach(w->{menuItems.add(w.getText());});

        return menuItems;
    }

    public void clickMenuItem(String menuText)
    {
        List<WebElement> menuElements = getMenuItemElements();
        for(WebElement w : menuElements)
        {
            if(w.getText().equalsIgnoreCase(menuText))
            {
                w.click();
                break;
            }
        }

        // TODO need a good way to wait until page is loaded.
    }

    private List<WebElement> getMenuItemElements()
    {
        return Locators.menuListItem.findElements(getDriver());
    }

    @Override
    public WebElement getComponentElement()
    {
        return _menuContentElement;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    static protected class Locators
    {
        static Locator menuList = Locator.xpath("//div[contains(@class,'dropdown')][contains(@class,'open')]//a[@id='user-menu-dropdown']");
        static Locator menuListItem = Locator.xpath("//div[contains(@class,'dropdown')][contains(@class,'open')]//a[@id='user-menu-dropdown']/following-sibling::ul//li");
    }

}
