package org.labkey.test.components.labkey;

import org.labkey.test.components.Component;
import org.labkey.test.components.html.FormItem;
import org.openqa.selenium.WebElement;

public class ReadOnlyFormItem extends Component implements FormItem<String>
{
    private WebElement _el;

    protected ReadOnlyFormItem(WebElement el)
    {
        _el = el;
    }

    public static FormItemFinder<ReadOnlyFormItem> ReadOnlyFormItem()
    {
        return new FormItemFinder<ReadOnlyFormItem>()
        {
            @Override
            protected ReadOnlyFormItem construct(WebElement el)
            {
                return new ReadOnlyFormItem(el);
            }

            @Override
            protected String itemTag()
            {
                return ".";
            }
        };
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    public String get()
    {
        return getComponentElement().getText();
    }

    public void set(String value)
    {
        throw new UnsupportedOperationException("Field is read-only or needs special automation");
    }
}
