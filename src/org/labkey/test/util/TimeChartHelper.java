/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

public class TimeChartHelper
{
    ExtHelperWD _extHelper = null;
    BaseWebDriverTest _test = null;

    public TimeChartHelper(BaseWebDriverTest test)
    {
        _test = test;
        _extHelper = _test._extHelper;
    }

    public void addAMeasure(String measure)
    {
        _test.log("Adding measure " + measure + " to time chart");
        _test.waitAndClickButton("Choose a Measure", 0);
        _test.waitForText("mem naive");

        _test.setFormElement(Locator.name("filterSearch"), measure);
        String measureXpath = _extHelper.getExtDialogXPath("Add Measure...") + "//table/tbody/tr/td[div[starts-with(text(), '"+ measure +"')]]";
        _test.waitForElement(Locator.css("a.x4-btn span.iconDelete"));
        _test.mouseDown(Locator.xpath(measureXpath));
        _test.clickButton("Select", BaseWebDriverTest.WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        _test.assertTextPresent(measure);
    }

    public void save(String reportName)
    {
        _test.sleep(4000);//tODO
        _test.clickButton("Save", 0);
        _test.waitForText("Viewable By");
        _test.setFormElement(Locator.name("reportName"), reportName);
        _test.sleep(4000);//tODO
        _test.clickButtonByIndex("Save", 1);
    }
}
