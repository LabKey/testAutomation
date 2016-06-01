package org.labkey.test.components.labkey;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class TextFormItem extends FormItem<String>
{
    protected TextFormItem(WebElement rowEl, WebDriver driver)
    {
        super(rowEl, driver);
    }

    public static TextFormItemFinder TextFormItem(WebDriver driver)
    {
        return new TextFormItemFinder(driver);
    }

    @Override
    public String getValue()
    {
        return getDriverWrapper().getFormElement(elements().item);
    }

    @Override
    public void setValue(String value)
    {
        getDriverWrapper().setFormElement(elements().item, value);
    }

    protected Locator itemLoc()
    {
        return Locator.css("input, textarea");
    }
    
    public static class TextFormItemFinder extends FormItemFinder<TextFormItem, TextFormItemFinder>
    {
        public TextFormItemFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected TextFormItem construct(WebElement el, WebDriver driver)
        {
            return new TextFormItem(el, driver);
        }
    }
}
