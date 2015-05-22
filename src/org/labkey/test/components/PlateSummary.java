/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class PlateSummary
{
    protected BaseWebDriverTest _test;
    protected Measurement _measurement;
    protected int _index;

    public PlateSummary(BaseWebDriverTest test, int plateSummaryIndex)
    {
        _test = test;
        _test.waitForElement(Locator.css("#plate-summary-div-1 table"));
        _measurement = Measurement.SPOTCOUNT;
        _index = plateSummaryIndex;
    }

    public String getCellValue(int row, int col)
    {
        return getRowValues(row).get(col);
    }

    public List<String> getRowValues(int row)
    {
        //first row is header, so add 1 to skip it
        row = row + 1;
        List<String> values = new ArrayList<>();
        List<WebElement> rowEl = Locator.xpath("(//table[@class='plate-summary-grid'])[" + _index +  "]//tr[" + row + "]/td/div/a[@class='" + _measurement.locatorClass + "']").findElements(_test.getDriver());
        for(WebElement el : rowEl)
        {
            values.add(el.getText());
        }
        return values;
    }

    public List<String> getColumnValues(int col)
    {
        List<String> values = new ArrayList<>();
        for(int row = 2; row < getRowCount(); row++)
        {
            values.add(getRowValues(row).get(col));
        }
        return values;
    }

    public void selectSampleWellGroup(String sampleWellGroup)
    {
        _test._ext4Helper.selectRadioButton(sampleWellGroup);
    }

    public void selectAntigenWellGroup(String antigenWellGroup)
    {
        _test._ext4Helper.selectRadioButton(antigenWellGroup);
    }

    public void selectMeasurement(Measurement measurement)
    {
        _test._ext4Helper.selectRadioButton(measurement.label);
        _measurement = measurement;
        //_test.longWait().until(ExpectedConditions.visibilityOfElementLocated(Locator.tagWithClass("a", measurement.locatorClass).toBy()));
        _test.waitForElement(Locator.tagWithClass("a", measurement.locatorClass).notHidden());
    }

    private int getRowCount()
    {
        return Locator.xpath("(//table[@class='plate-summary-grid'])[" + _index + "]//tr").findElements(_test.getDriver()).size();
    }

    public enum Measurement
    {
        SPOTCOUNT("labkey-cls-spotcount", "Spot Count"), ACTIVITY("labkey-cls-activity", "Activity"), INTENSITY("labkey-cls-intensity", "Intensity");
        public String locatorClass;
        public String label;
        private Measurement(String locatorClass, String label)
        {
            this.locatorClass = locatorClass;
            this.label = label;
        }
    }
}
