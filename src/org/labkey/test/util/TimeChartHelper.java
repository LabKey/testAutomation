package org.labkey.test.util;

import org.labkey.remoteapi.query.BaseSelect;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 12/3/12
 * Time: 1:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class TimeChartHelper
{
    ExtHelper _extHelper = null;
    BaseSeleniumWebTest _test = null;

    public TimeChartHelper(BaseSeleniumWebTest test)
    {
        _test = test;
        _extHelper = _test._extHelper;
    }

    public void addAMeasure(String measure)
    {
        _test.log("Adding measure " + measure + " to time chart");
        _test.waitForElement(Locator.button("Choose a Measure"), _test.WAIT_FOR_JAVASCRIPT);
        _test.clickButton("Choose a Measure", "mem naive");

        _test.setFormElement(Locator.name("filterSearch"), measure);
        String measureXpath = _extHelper.getExtDialogXPath("Add Measure...") + "//table/tbody/tr/td[div[starts-with(text(), '"+ measure +"')]]";
        _test.mouseDown(Locator.xpath(measureXpath));
        _test.clickButton("Select", _test.WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        _test.assertTextPresent(measure);
    }

    public void save(String reportName)
    {
        _test.clickButton("Save", 0);
        _test.waitForText("Viewable By");
        _test.setFormElement(Locator.name("reportName"), reportName);
        _test.clickButtonByIndex("Save", 1);
    }
}
