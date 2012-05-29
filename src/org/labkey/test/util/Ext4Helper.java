/*
 * Copyright (c) 2012 LabKey Corporation
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
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: Jan 3, 2012
 * Time: 3:34:16 PM
 */
public class Ext4Helper
{
    public static void selectComboBoxItem(BaseSeleniumWebTest test, Locator.XPathLocator parentLocator, String selection)
    {
        test.clickAt(Locator.xpath(parentLocator.getPath() + "//div[contains(@class, 'x4-form-arrow-trigger')]"), "1,1");
        if(test.getBrowser().startsWith(test.IE_BROWSER))
        {
            test.sleep(500);
            test.clickAt(Locator.xpath("//div/div/div[text()='" + selection + "']"), "1,1");
            test.mouseDownAt(Locator.xpath("/html/body"), 1,1);
        }
        else
        {
            // wait for the dropdown to open
            test.waitForElement(Locator.xpath(parentLocator.getPath() + "//div[contains(@class, 'x4-pickerfield-open')]"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

            // select the list item
            test.click(Locator.xpath("//li[contains(@class, 'x4-boundlist-item') and contains( text(), '" + selection + "')]"));
            //test.mouseDown(Locator.xpath("/html/body"));
        }
    }

    public static void selectComboBoxItem(BaseSeleniumWebTest test, String label, String selection)
    {
        selectComboBoxItem(test, Locator.xpath("//div[./label[text()='" + label + "']]"), selection);
    }

    public static void selectRadioButton(BaseSeleniumWebTest test, String label, String selection)
    {
        Locator l = Locator.xpath("//div[div/label[text()='" + label + "']]//label[text()='" + selection + "']");
        test.click(l);
    }
}
