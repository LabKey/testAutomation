package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.openqa.selenium.WebDriver;

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
        return elementCache().statCellSelectedLoc.startsWith(statLabel).findElements(this).size() == 1;
    }

    public String getValue(String statLabel)
    {
        Locator.XPathLocator statCellLoc = elementCache().statCellLoc.startsWith(statLabel);
        return statCellLoc.parent().append(elementCache().statValueLoc).findElement(this).getText();
    }

    public SummaryStatisticsDialog select(String statLabel)
    {
        assertSelected(statLabel, false);
        elementCache().statCellLoc.startsWith(statLabel).findElement(this).click();
        return this;
    }

    public SummaryStatisticsDialog deselect(String statLabel)
    {
        assertSelected(statLabel, true);
        elementCache().statCellLoc.startsWith(statLabel).findElement(this).click();
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
        Locator.XPathLocator statRowSelectedLoc = statRowLoc.withClass("x4-item-selected");
        Locator.XPathLocator statCellSelectedLoc = statRowSelectedLoc.append(Locator.tagWithClass("td", "label"));
        Locator.XPathLocator statValueLoc = Locator.tagWithClass("td", "value");
    }
}