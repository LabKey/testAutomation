package org.labkey.test.components.labkey;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SelectFormItem extends FormItem<String>
{
    protected SelectFormItem(WebElement rowEl, WebDriver driver)
    {
        super(rowEl, driver);
    }

    public static SelectFormItemFinder SelectFormItem(WebDriver driver)
    {
        return new SelectFormItemFinder(driver);
    }

    @Override
    public String getValue()
    {
        return getDriverWrapper().getSelectedOptionText(elements().item);
    }

    @Override
    public void setValue(String value)
    {
        getDriverWrapper().selectOptionByText(elements().item, value);
    }

    protected Locator itemLoc()
    {
        return Locator.css("select");
    }
    
    public static class SelectFormItemFinder extends FormItemFinder<SelectFormItem, SelectFormItemFinder>
    {
        public SelectFormItemFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected SelectFormItem construct(WebElement el, WebDriver driver)
        {
            return new SelectFormItem(el, driver);
        }
    }
}
