package org.labkey.test.util;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 2/11/13
 * Time: 3:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChartHelper  extends AbstractHelper
{

    public ChartHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    /**
     * on a page with a drt, clicks the (row)th edit button and set the named
     *
     */
    public void editDrtRow(int row, Map<String, String> nameAndValue)
    {
        _test.clickAndWait(Locator.linkWithText("edit", row));
        for(String name : nameAndValue.keySet())
            _test.setFormElement(Locator.name(name), nameAndValue.get(name));
        _test.clickButton("Submit");
    }
}
