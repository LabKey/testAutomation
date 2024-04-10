package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ColorPickerInput extends WebDriverComponent<Component<?>.ElementCache>
{
    private final WebDriver _driver;
    final WebElement _componentElement;

    public ColorPickerInput(WebElement element, WebDriver driver)
    {
        _driver = driver;
        _componentElement = element;
    }
    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    public void setHexValue(String hexValue)
    {
        List<WebElement> inputs = Locator.tag("input").findElements(this);
        if (inputs.isEmpty())
            throw new NoSuchElementException("Input tag not found in color picker");
        WebElement hexInput = inputs.get(0);
        new Input(hexInput, getDriver()).setWithPaste(hexValue);
    }

    public String getHexValue()
    {
        List<WebElement> inputs = Locator.tag("input").findElements(this);
        if (inputs.isEmpty())
            throw new NoSuchElementException("Input tag not found in color picker");
        WebElement hexInput = inputs.get(0);
        return new Input(hexInput, getDriver()).getValue();
    }

    public static class ColorPickerInputFinder extends WebDriverComponentFinder<ColorPickerInput, ColorPickerInput.ColorPickerInputFinder>
    {
        private final Locator _locator;

        public ColorPickerInputFinder(WebDriver driver)
        {
            super(driver);
            _locator = Locator.tagWithClass("div", "compact-picker");
        }

        @Override
        protected ColorPickerInput construct(WebElement element, WebDriver driver)
        {
            return new ColorPickerInput(element, driver);
        }

        @Override
        protected Locator locator() { return _locator; }
    }
}
