/*
 * Copyright (c) 2009 LabKey Corporation
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
import org.apache.commons.lang.BooleanUtils;

/**
 * User: klum
 * Date: Apr 6, 2009
 * Time: 5:21:53 PM
 */
public class ExtHelper
{
    /**
     * Clicks the ext menu item from the submenu specified by the ext object id
     */
    public static void clickMenuButton(BaseSeleniumWebTest test, String buttonName, String itemId, String ... subMenuIds)
    {
        test.clickNavButton(buttonName, 0);
        // allow the DOM to be updated
        test.sleep(1000);
        if (subMenuIds != null)
        {
            for (String id : subMenuIds)
            {
                String elementId = ExtHelper.getExtElementId(test, id);
                if (elementId != null)
                {
                    // render the submenu
                    test.getWrapper().mouseOver("//a[@id='" + elementId + "']");
                    test.sleep(1000);
                }
            }
        }
        String menuItemId = ExtHelper.getExtElementId(test, itemId);
        if (menuItemId != null)
            test.clickLink(menuItemId);
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

    public static void waitForExtDialog(BaseSeleniumWebTest test, int timeout)
    {
        for (int time=0; time < timeout; time+= 500)
        {
            if (BooleanUtils.toBoolean(test.getWrapper().getEval("this.browserbot.getCurrentWindow().Ext.MessageBox.getDialog().isVisible();")))
                return;
            test.sleep(500);
        }
        test.fail("Failed waiting for Ext dialog to appear");
    }

    public static Locator locateBrowserFile(String fileName)
    {
        return Locator.xpath("//td/div[text()='" + fileName + "']//..//..//div[@class='x-grid3-row-checker']");
    }

    public static void selectFileBrowserFile(BaseSeleniumWebTest test, String fileName)
    {
        Locator file = locateBrowserFile(fileName);

        test.waitForElement(file, 60000);
        test.mouseDown(file);
    }

    public static void waitForImportDataEnabled(BaseSeleniumWebTest test)
    {
        test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-import-enabled')]"), test.WAIT_FOR_JAVASCRIPT);
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
