package org.labkey.test.util;

import org.labkey.test.BaseSeleniumWebTest;
import org.apache.commons.lang.BooleanUtils;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: Apr 6, 2009
 * Time: 5:21:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtHelper
{
    /**
     * Clicks the ext menu item from the submenu specified by the ext object id
     */
    public static void clickMenuButton(BaseSeleniumWebTest test, String buttonName, String subMenuId, String itemId)
    {
        test.clickNavButton(buttonName, 0);
        // allow the DOM to be updated
        test.sleep(1000);
        if (subMenuId != null)
        {
            String id = ExtHelper.getExtElementId(test, subMenuId);
            if (id != null)
            {
                // render the submenu
                test.getWrapper().mouseOver("//a[@id='" + id + "']");
                test.sleep(1000);
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
        String id = test.getWrapper().getEval("selenium.getExtElementId('" + extId + "');");
        test.log("Element id for ext component id: " + extId + " is: " + id);

        return id;
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
}
