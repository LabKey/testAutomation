package org.labkey.test.components.html;

import org.labkey.test.selenium.SelectWrapper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * Wrap Selenium Select to avoid inspecting it until it is actually used
 */
public class LazySelect extends SelectWrapper
{
    private final WebElement wrappedElement;
    private Select wrappedSelect;

    public LazySelect(WebElement element)
    {
        super();
        wrappedElement = element;
    }

    protected Select getWrappedSelect()
    {
        if (wrappedSelect == null)
            wrappedSelect = new Select(wrappedElement);
        return wrappedSelect;
    }
}
