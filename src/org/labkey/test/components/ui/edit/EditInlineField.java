package org.labkey.test.components.ui.edit;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class EditInlineField extends WebDriverComponent<EditInlineField.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected EditInlineField(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    public EditInlineField setValue(String value)
    {
        open();
        // 'setFormElement' calls 'WebElement.clear()' which can close the edit-in-place input
        getWrapper().setFormElementJS(elementCache().findInput(), value);
        close();
        return this;
    }

    public String getLabel()
    {
        return elementCache().label.getText();
    }

    public String getValue()
    {
        close();
        return elementCache().toggle().getText();
    }

    public boolean isOpen()
    {
        return !elementCache().toggleLoc.existsIn(this) &&
                elementCache().findInput().isDisplayed();
    }

    private void open()
    {
        if (!isOpen())
        {
            WebElement toggle = elementCache().toggle();
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(toggle));
            toggle.click();
            WebDriverWrapper.waitFor(this::isOpen,
                    "the edit inline field did not open", 2000);
        }
    }

    private void close()
    {
        for (int i=0; i < 3 && isOpen(); i++)
        {
            getWrapper().mouseOver(elementCache().inputGroupAddOn());
            elementCache().inputGroupAddOn().click();

            WebDriverWrapper.waitFor(() -> !isOpen(), 1500);
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement label = Locator.tagWithClass("span", "edit-inline-field__label")
                .refindWhenNeeded(this);
        final Locator toggleLoc = Locator.tagWithClass("span", "edit-inline-field__toggle")
                .withChild(Locator.tagWithClass("i", "fa-pencil"));
        final WebElement findInput()
        {
            return Locator.css("input.form-control, textarea.form-control").findWhenNeeded(this);
        }

        WebElement placeholderElement()
        {
            return Locator.tagWithClass("span", "edit-inline-field__placeholder")
                    .findElement(this);
        }

        WebElement toggle()
        {
            return toggleLoc.waitForElement(this, 1_000);
        }
        WebElement inputGroupAddOn()
        {
            return Locator.tagWithClass("span", "input-group-addon").findElement(this);
        }
    }


    public static class EditInlineFieldFinder extends WebDriverComponentFinder<EditInlineField, EditInlineFieldFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "edit-inline-field");
        private String _label = null;

        public EditInlineFieldFinder(WebDriver driver)
        {
            super(driver);
        }

        public EditInlineFieldFinder withLabel(String label)
        {
            _label = label;
            return this;
        }

        @Override
        protected EditInlineField construct(WebElement el, WebDriver driver)
        {
            return new EditInlineField(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_label != null)
                return _baseLocator.withChild(
                        Locator.tagWithClass("span", "edit-inline-field__label")
                        .containing(_label));
            else
                return _baseLocator;
        }
    }
}
