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
}
