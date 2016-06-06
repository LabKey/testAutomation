package org.labkey.test.components.labkey;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ReadOnlyFormItem extends FormItem<String>
{
    private String _value;
    private boolean _isCached;

    protected ReadOnlyFormItem(WebElement rowEl, WebDriver driver)
    {
        this(rowEl, driver, true);
    }

    protected ReadOnlyFormItem(WebElement rowEl, WebDriver driver, boolean isCached)
    {
        super(rowEl, driver);
        _isCached = isCached;
    }

    public static ReadOnlyFormItemFinder ReadOnlyFormItem(WebDriver driver)
    {
        return new ReadOnlyFormItemFinder(driver);
    }

    public String getValue()
    {
        if (!_isCached || null == _value)
            _value = elements().itemTd.getText();
        return _value;
    }

    public void setValue(String value)
    {
        throw new UnsupportedOperationException(getLabel() + " field is read-only or unknown");
    }

    public static class ReadOnlyFormItemFinder extends FormItemFinder<ReadOnlyFormItem, ReadOnlyFormItemFinder>
    {
        boolean isCached = true;

        public ReadOnlyFormItemFinder(WebDriver driver)
        {
            super(driver);
        }

        public void withoutValueCache()
        {
            this.isCached = false;
        }

        @Override
        protected ReadOnlyFormItem construct(WebElement el, WebDriver driver)
        {
            return new ReadOnlyFormItem(el, driver, isCached);
        }
    }
}
