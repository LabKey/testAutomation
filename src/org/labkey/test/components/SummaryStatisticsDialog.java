/*
 * Copyright (c) 2017 LabKey Corporation
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
import org.labkey.test.components.ext4.Window;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SummaryStatisticsDialog extends Window<SummaryStatisticsDialog.ElementCache>
{
    private static final String DIALOG_TITLE = "Summary Statistics";

    public SummaryStatisticsDialog(WebDriver driver)
    {
        super(DIALOG_TITLE, driver);
        elementCache().statTableLoc.waitForElement(this, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void apply()
    {
        clickButton("Apply", BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void cancel()
    {
        clickButton("Cancel", true);
    }

    public boolean isPresent(String statLabel)
    {
        return elementCache().statCellLoc.startsWith(statLabel).findElements(this).size() >= 1;
    }

    public boolean isSelected(String statLabel)
    {
        return elementCache().checkbox(statLabel).isSelected();
    }

    public String getValue(String statLabel)
    {
        Locator.XPathLocator statCellLoc = elementCache().statCellLoc.startsWith(statLabel);
        String ret;
        try
        {
            ret = statCellLoc.parent().append(elementCache().statValueLoc).findElement(this).getText();
        }
        catch(NoSuchElementException tryAlt)
        {
            int index = indexOfText(elementCache().statCellIndentLoc.findElements(this), statLabel);
            ret = elementCache().statCellIndentValueLoc.findElements(this).get(index).getText();
        }
        return ret;
    }

    private int indexOfText(List<WebElement> els, String text)
    {
        int ret = -1;
        for (WebElement el : els)
        {
            if (el.getText().equals(text)) ret = els.indexOf(el);
        }
        return ret;
    }

    public SummaryStatisticsDialog select(String statLabel)
    {
        assertSelected(statLabel, false);
        elementCache().checkbox(statLabel).click();
        return this;
    }

    public SummaryStatisticsDialog deselect(String statLabel)
    {
        assertSelected(statLabel, true);
        elementCache().checkbox(statLabel).click();
        return this;
    }

    private void assertSelected(String statLabel, boolean expectedToBeSelected)
    {
        boolean statItemIsChecked = isSelected(statLabel);
        assertEquals(String.format("Stat item %s is %s checked", statLabel, !statItemIsChecked ? "not" : "already"), expectedToBeSelected, statItemIsChecked);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Window.ElementCache
    {
        Locator.XPathLocator statTableLoc = Locator.tagWithClass("table", "stat-table");
        Locator.XPathLocator statRowLoc = statTableLoc.append(Locator.tagWithClassContaining("tr", "lk-stats-row"));
        Locator.XPathLocator statCellLoc = statRowLoc.append(Locator.tagWithClass("td", "lk-stats-label"));
        Locator.XPathLocator statCellIndentLoc = statCellLoc.append(Locator.tagWithClass("div", "lk-stats-indent"));
        Locator.XPathLocator statCellIndentValueLoc = statRowLoc.append(Locator.tagWithClass("td", "lk-stats-value")).append(Locator.tag("div"));
        Locator.XPathLocator statRowSelectedLoc = statRowLoc.withClass("x4-item-selected");

        Locator.XPathLocator statValueLoc = Locator.tagWithClass("td", "lk-stats-value");
        Locator.XPathLocator statCheckLoc = Locator.tagWithClass("td", "lk-stats-check");

        public WebElement checkbox(String statLabel)
        {
            Locator.XPathLocator labelLoc = statCellLoc.startsWith(statLabel);
            Locator.XPathLocator cbInputLoc = labelLoc.parent().append(statCheckLoc.append(Locator.tag("input")));
            return cbInputLoc.findElement(this);
        }

        public WebElement label(String statLabel)
        {
            return statCellLoc.startsWith(statLabel).findElement(this);
        }

        public WebElement value(String statLabel)
        {
            Locator.XPathLocator labelLoc = statCellLoc.startsWith(statLabel);
            return labelLoc.parent().append(statValueLoc).findElement(this);
        }
    }
}