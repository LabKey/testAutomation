/*
 * Copyright (c) 2015-2016 LabKey Corporation
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

import com.google.common.collect.ImmutableList;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PlateSummary extends Component
{
    protected final BaseWebDriverTest _test;
    protected Measurement _measurement;
    private final WebElement summaryGrid;
    private Elements _elements;

    public PlateSummary(BaseWebDriverTest test, int plateSummaryIndex)
    {
        _test = test;
        summaryGrid = _test.waitForElement(Locator.css("#plate-summary-div-1 div.x4-panel[id^='lk_platepanel-']").index(plateSummaryIndex));
        _measurement = Measurement.SPOTCOUNT;
    }

    @Override
    public WebElement getComponentElement()
    {
        return summaryGrid;
    }

    public String getCellValue(Row row, int col)
    {
        return elements().getCells(row.index).get(col).getText();
    }

    public List<String> getRowValues(Row row)
    {
        return _test.getTexts(elements().getCells(row.index));
    }

    public List<String> getColumnValues(int col)
    {
        List<String> values = new ArrayList<>();
        for(Row row : Row.values())
        {
            values.add(getCellValue(row, col));
        }
        return values;
    }

    public void selectSampleWellGroup(String sampleWellGroup)
    {
        _test._ext4Helper.selectRadioButton("Sample Well Groups", sampleWellGroup);
    }

    public void selectAntigenWellGroup(String antigenWellGroup)
    {
        _test._ext4Helper.selectRadioButton("Antigen Well Groups", antigenWellGroup);
    }

    public void selectMeasurement(Measurement measurement)
    {
        _test._ext4Helper.selectRadioButton("Measurement", measurement.label);
        _measurement = measurement;
        _test.shortWait().until(ExpectedConditions.visibilityOf(elements().getCells(0).get(0)));
    }

    private int getRowCount()
    {
        return elements().getDataRows().size();
    }

    public enum Row
    {
        A(0), B(1), C(2), D(3), E(4), F(5), G(6), H(7);

        public final int index;
        Row(int index)
        {
            this.index = index;
        }
    }

    public enum Measurement
    {
        SPOTCOUNT("labkey-cls-spotcount", "Spot Count"),
        ACTIVITY("labkey-cls-activity", "Activity"),
        INTENSITY("labkey-cls-intensity", "Intensity (fluorescence)"),
        SPOT_SIZE("labkey-cls-spotsize", "Spot Size (microns)");

        public final String locatorClass;
        public final String label;
        Measurement(String locatorClass, String label)
        {
            this.locatorClass = locatorClass;
            this.label = label;
        }
    }

    private Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    protected class Elements extends ComponentElements
    {
        private List<WebElement> dateRows;
        private Map<Measurement, Map<Integer, List<WebElement>>> dataCells;

        @Override
        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        public List<WebElement> getDataRows()
        {
            if (dateRows == null)
                dateRows = Locator.css("tr:not(:first-child)").findElements(this);
            return dateRows;
        }

        protected WebElement getDataRow(int row)
        {
            return getDataRows().get(row);
        }

        protected List<WebElement> getCells(int row)
        {
            if (dataCells == null)
                dataCells = new TreeMap<>();
            if (dataCells.get(_measurement) == null)
                dataCells.put(_measurement, new TreeMap<>());
            if (dataCells.get(_measurement).get(row) == null)
                dataCells.get(_measurement).put(row, ImmutableList.copyOf(Locator.css("td:not(:first-child) a." + _measurement.locatorClass).findElements(getDataRow(row))));
            return dataCells.get(_measurement).get(row);
        }

        protected WebElement getCell(int row, int col)
        {
            return getCells(row).get(col);
        }
    }
}
