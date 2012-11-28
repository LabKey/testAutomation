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
import org.labkey.test.module.EHRReportingAndUITest;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 8/6/12
 * Time: 10:49 AM
 */
public class EHRTestHelper
{
    private BaseSeleniumWebTest _test;

    public EHRTestHelper(BaseSeleniumWebTest test)
    {
        _test = test;
    }

    public String getAnimalHistoryDataRegionName(String title)
    {
        // Specific to the EHR Animal History page.
        _test.waitForElement(Locator.xpath("//table[@name='webpart' and ./*/*/*/a//span[text()='"+title+"' or starts-with(text(), '"+title+":')]]//table[starts-with(@id,'dataregion_') and not(contains(@id, 'header'))]"), _test.WAIT_FOR_JAVASCRIPT * 3);
        return _test.getAttribute(Locator.xpath("//table[@name='webpart' and ./*/*/*/a//span[text()='" + title + "' or starts-with(text(), '" + title + ":')]]//table[starts-with(@id,'dataregion_') and not(contains(@id, 'header'))]"), "id").substring(11);
    }

    public void selectDataEntryRecord(String query, String Id, boolean keepExisting)
    {
        _test.getWrapper().getEval("selenium.selectExtGridItem('Id','" + Id + "', -1, 'ehr-" + query + "-records-grid', " + keepExisting + ");");
        if(!keepExisting)_test.waitForElement(Locator.xpath("//div[@id='Id']/a[text()='"+Id+"']"), _test.WAIT_FOR_JAVASCRIPT);
    }

    public void clickVisibleButton(String text)
    {
        _test.click(Locator.xpath("//button[text()='"+text+"' and "+ EHRReportingAndUITest.VISIBLE+" and not(contains(@class, 'x-hide-display'))]"));
    }

    public void setDataEntryField(String tabName, String fieldName, String value)
    {
        _test.setFormElement(Locator.xpath("//div[./div/span[text()='" + tabName + "']]//*[(self::input or self::textarea) and @name='" + fieldName + "']"), value);
        _test.fireEvent(Locator.xpath("//div[./div/span[text()='" + tabName + "']]//*[(self::input or self::textarea) and @name='" + fieldName + "']"), BaseSeleniumWebTest.SeleniumEvent.blur);
    }
}

