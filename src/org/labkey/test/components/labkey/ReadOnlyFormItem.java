package org.labkey.test.components.labkey;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ReadOnlyFormItem extends FormItem<String>
{
    protected ReadOnlyFormItem(WebElement rowEl, WebDriver driver)
    {
        super(rowEl, driver);
    }

    public static ReadOnlyFormItemFinder ReadOnlyFormItem(WebDriver driver)
    {
        return new ReadOnlyFormItemFinder(driver);
    }

    public String getValue()
    {
        return elements().itemTd.getText();
    }

    public void setValue(String value)
    {
        throw new UnsupportedOperationException(getLabel() + " field is read-only or unknown");
    }

    public static class ReadOnlyFormItemFinder extends FormItemFinder<ReadOnlyFormItem, ReadOnlyFormItemFinder>
    {
        public ReadOnlyFormItemFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected ReadOnlyFormItem construct(WebElement el, WebDriver driver)
        {
            return new ReadOnlyFormItem(el, driver);
        }
    }
}
