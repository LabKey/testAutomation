package org.labkey.test.components.labkey;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class GenericFormItem extends FormItem
{
    FormItem wrappedItem;

    protected GenericFormItem(WebElement rowEl, WebDriver driver)
    {
        super(rowEl, driver);
    }

    @Override
    public Object getValue()
    {
        if (wrappedItem == null)
            wrappedItem = getSpecificFormItem();
        return wrappedItem.getValue();
    }

    @Override
    public void setValue(Object value)
    {
        if (wrappedItem == null)
            wrappedItem = getSpecificFormItem();
        wrappedItem.setValue(value);
    }

    private FormItem getSpecificFormItem()
    {
        List<WebElement> children = elements().itemTd.findElements(By.cssSelector("*"));
        if (children.isEmpty())
            return new ReadOnlyFormItem(getComponentElement(), getDriver());

        WebElement element = children.get(0);
        String tagName = element.getTagName();
        switch (tagName)
        {
            case "select":
                return new SelectFormItem(getComponentElement(), getDriver());
            case "input":
                if ("checkbox".equals(element.getAttribute("type")))
                    return new CheckboxFormItem(getComponentElement(), getDriver());
                else
                    return new TextFormItem(getComponentElement(), getDriver());
            case "textarea":
                return new TextFormItem(getComponentElement(), getDriver());
            default:
                return new ReadOnlyFormItem(getComponentElement(), getDriver());
        }
    }
}
