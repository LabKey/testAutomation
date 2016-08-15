package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

public class ChartQueryDialog<EC extends Component.ElementCache> extends Component<EC>
{
    private  final String DIALOG_XPATH = "//div[contains(@class, 'chart-wizard-dialog')]//div[contains(@class, 'chart-query-panel')]";

    protected WebElement _chartQueryDialog;
    protected BaseWebDriverTest _test;

    public ChartQueryDialog(BaseWebDriverTest test)
    {
        _test = test;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _chartQueryDialog;
    }

    public boolean isDialogVisible()
    {
        return elements().dialog.isDisplayed();
    }

    public void waitForDialog()
    {
        _test.waitForElement(Locator.xpath(DIALOG_XPATH + "//div[text()='Select a query']"));
    }

    public void selectSchema(String schemaName)
    {
        _test._ext4Helper.selectComboBoxItem("Schema:", schemaName);
    }

    public void selectQuery(String queryName)
    {
        _test._ext4Helper.selectComboBoxItem("Query:", queryName);
    }

    public void clickCancel()
    {
        Window w = new Window(elements().dialog, _test.getDriver());
        w.clickButton("Cancel", 0);
    }

    public void clickOk()
    {
        Window w = new Window(elements().dialog, _test.getDriver());
        w.clickButton("OK", 0);
    }

    public Elements elements()
    {
        return new Elements();
    }

    class Elements extends ElementCache
    {
        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        public WebElement dialog = new LazyWebElement(Locator.xpath(DIALOG_XPATH), _test.getDriver());
    }
}
