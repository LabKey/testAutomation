package org.labkey.test.components.labkey;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CheckboxFormItem extends FormItem<Boolean>
{
    protected CheckboxFormItem(WebElement rowEl, WebDriver driver)
    {
        super(rowEl, driver);
    }

    public static CheckboxFormItemFinder CheckboxFormItem(WebDriver driver)
    {
        return new CheckboxFormItemFinder(driver);
    }

    @Override
    public Boolean getValue()
    {
        return elements().item.isSelected();
    }

    @Override
    public void setValue(Boolean value)
    {
        getDriverWrapper().setCheckbox(elements().item, value);
    }

    protected Locator itemLoc()
    {
        return Locator.checkbox();
    }

    public static class CheckboxFormItemFinder extends FormItemFinder<CheckboxFormItem, CheckboxFormItemFinder>
    {
        public CheckboxFormItemFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected CheckboxFormItem construct(WebElement el, WebDriver driver)
        {
            return new CheckboxFormItem(el, driver);
        }
    }
}
