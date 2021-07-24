package org.labkey.test.components.ui.edit;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.labkey.test.components.html.Input.Input;

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

    public EditInlineField setInput(String value)
    {
        getEdit().set(value);
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

    public boolean isInReadMode()
    {
        return !isOpen();
    }

    public boolean isOpen()
    {
        return !elementCache().toggleLoc.existsIn(this) &&
                elementCache().inputLoc.existsIn(this);
    }

    private EditInlineField open()
    {
        if (!isOpen())
        {
            WebElement toggle = elementCache().toggle();
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(toggle));
            toggle.click();
            getWrapper().waitFor(()-> isOpen(),
                    "the edit inline field did not open", 2000);
        }
        return this;
    }

    private EditInlineField close()
    {
        for (int i=0; i < 3; i++)
        {
            if (isOpen())
            {
                getWrapper().mouseOver(elementCache().inputGroupAddOn());
                elementCache().inputGroupAddOn().click();

                getWrapper().waitFor(() -> !isOpen(), 1500);
            }
        }
        return this;
    }

    private Input getEdit()
    {
        open();
        WebElement inputEl = elementCache().inputLoc.waitForElement(this, 2_000);
        return new Input(inputEl, getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        final Locator toggleLoc = Locator.tagWithClass("span", "edit-inline-field__toggle")
                .withChild(Locator.tagWithClass("i", "fa-pencil"));
        final Locator inputLoc = Locator.tagWithClass("input", "form-control");
        final WebElement label = Locator.tagWithClass("span", "edit-inline-field__label")
                .findWhenNeeded(this);
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
