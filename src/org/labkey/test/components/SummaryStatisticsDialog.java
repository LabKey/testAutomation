package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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
        return elementCache().statCellLoc.startsWith(statLabel).findElements(this).size() == 1;
    }

    public boolean isSelected(String statLabel)
    {
        return elementCache().checkbox(statLabel).isSelected();
    }

    public String getValue(String statLabel)
    {
        return elementCache().value(statLabel).getText();
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
        Locator.XPathLocator statRowLoc = statTableLoc.append(Locator.tagWithClass("tr", "row"));
        Locator.XPathLocator statCellLoc = statRowLoc.append(Locator.tagWithClass("td", "label"));
        Locator.XPathLocator statValueLoc = Locator.tagWithClass("td", "value");
        Locator.XPathLocator statCheckLoc = Locator.tagWithClass("td", "check");

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