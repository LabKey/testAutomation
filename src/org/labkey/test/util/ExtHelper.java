/*
 * Copyright (c) 2009-2010 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * User: klum
 * Date: Apr 6, 2009
 * Time: 5:21:53 PM
 */
public class ExtHelper
{
    /**
     * Clicks the labkey menu item from the submenu specified by the menu object's text
     */
    public static void clickMenuButton(BaseSeleniumWebTest test, boolean wait, String MenusLabel, String ... subMenuLabels)
    {
        test.clickAndWait(Locator.navButton(MenusLabel), 0);
        for (int i = 0; i < subMenuLabels.length - 1; i++)
        {
            Locator parentLocator = Locator.menuItem(subMenuLabels[i]);
            test.waitForElement(parentLocator, 1000);
            test.mouseOver(parentLocator);
        }
        Locator itemLocator = Locator.menuItem(subMenuLabels[subMenuLabels.length - 1]);
        test.waitForElement(itemLocator, 1000);
        if(wait)
            test.clickAndWait(itemLocator);
        else
            test.click(itemLocator);
    }

    /**
     * Clicks the ext menu item from the submenu specified by the ext object's text
     */
    public static void clickExtMenuButton(BaseSeleniumWebTest test, boolean wait, String MenusLabel, String ... subMenuLabels)
    {
        test.clickAndWait(Locator.extButton(MenusLabel), 0);
        for (int i = 0; i < subMenuLabels.length - 1; i++)
        {
            Locator parentLocator = Locator.menuItem(subMenuLabels[i]);
            test.waitForElement(parentLocator, 1000);
            test.mouseOver(parentLocator);
        }
        Locator itemLocator = Locator.menuItem(subMenuLabels[subMenuLabels.length - 1]);
        test.waitForElement(itemLocator, 1000);
        if(wait)
            test.clickAndWait(itemLocator);
        else
            test.click(itemLocator);
    }

    /**
     * Returns a DOM Element id from an ext object id. Assumes that the ext component
     * has already been rendered.
     */
    public static String getExtElementId(BaseSeleniumWebTest test, String extId)
    {
        for (int attempt = 0; attempt < 5; attempt++)
        {
            String id = test.getWrapper().getEval("selenium.getExtElementId('" + extId + "');");
            test.log("Element id for ext component id: " + extId + " is: " + id);
            if (id != null)
                return id;
            test.sleep(500);
        }

        test.fail("Failed to get element id for Ext component '" + extId + "'");
        return null;
    }

    public static void waitForExtDialog(final BaseSeleniumWebTest test, String title, int timeout)
    {
        final Locator locator = Locator.xpath("//span[normalize-space(@class) = 'x-window-header-text' and string() = '" + title + "']");
        
        test.waitFor(new BaseSeleniumWebTest.Checker()
        {
            public boolean check()
            {
                return test.isElementPresent(locator);
            }
        }, "Ext Dialog with title '" + title + "' did not appear after " + timeout + "ms", timeout);
    }

    public static Locator locateBrowserFileCheckbox(String fileName)
    {
        return Locator.xpath("//div[contains(@class, 'x-grid3-row')]//td/div[text()='" + fileName + "']//..//..//div[@class='x-grid3-row-checker']");
    }

    public static Locator locateBrowserFileName(String fileName)
    {
        return Locator.xpath("//div[contains(@class, 'x-grid3-row')]//td/div[text()='" + fileName + "']");
    }

    public static void clickFileBrowserFileCheckbox(BaseSeleniumWebTest test, String fileName)
    {
        Locator file = locateBrowserFileCheckbox(fileName);

        test.waitForElement(file, 60000);
        test.mouseDown(file);
    }

    /**
     * Select a <b>single</b> row in the file browser by clicking on the file name.
     * Use {@link ExtHelper#clickFileBrowserFileCheckbox(BaseSeleniumWebTest, String)} to click the checkbox for multi-select.
     */
    public static void selectFileBrowserFile(BaseSeleniumWebTest test, String fileName)
    {
        Locator file = locateBrowserFileName(fileName);

        test.waitForElement(file, 60000);
        test.mouseDown(file);
    }

    public static void waitForImportDataEnabled(BaseSeleniumWebTest test)
    {
        test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-import-enabled')]"), 6 * test.WAIT_FOR_JAVASCRIPT);
    }
    
    public static void selectAllFileBrowserFiles(BaseSeleniumWebTest test)
    {
        Locator file = Locator.xpath("//tr[@class='x-grid3-hd-row']//div[@class='x-grid3-hd-checker']");
        test.waitForElement(file, 60000);
        test.sleep(1000);
        test.mouseClick(file.toString());

        file = Locator.xpath("//tr[@class='x-grid3-hd-row']//div[@class='x-grid3-hd-inner x-grid3-hd-checker x-grid3-hd-checker-on']");
        test.waitForElement(file, 60000);
    }
}
