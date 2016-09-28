package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ChartQueryDialog extends ChartWizardDialog<ChartQueryDialog.ElementCache>
{
    public ChartQueryDialog(WebDriver driver)
    {
        super("Select a query", driver);
    }

    @Deprecated
    public ChartQueryDialog(BaseWebDriverTest test)
    {
        this(test.getDriver());
    }

    @Deprecated // TODO: Remove
    public void waitForDialog()
    {
    }

    public void selectSchema(String schemaName)
    {
        getWrapper()._ext4Helper.selectComboBoxItem("Schema:", schemaName);
    }

    public void selectQuery(String queryName)
    {
        getWrapper()._ext4Helper.selectComboBoxItem("Query:", queryName);
    }

    public void clickCancel()
    {
        clickButton("Cancel", 0);
        waitForClose();
    }

    public ChartTypeDialog clickOk()
    {
        clickButton("OK", 0);
        waitForClose();
        return new ChartTypeDialog(getWrapper().getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    class ElementCache extends ChartWizardDialog.ElementCache
    {
    }
}
