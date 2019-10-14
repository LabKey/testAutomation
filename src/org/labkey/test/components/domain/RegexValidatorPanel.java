package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.labkey.test.components.html.Input.Input;

public class RegexValidatorPanel extends WebDriverComponent<RegexValidatorPanel.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public RegexValidatorPanel(WebElement element, WebDriver driver)
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

    public RegexValidatorPanel expand()
    {
        if (!isExpanded())
        {
            WebElement expando = elementCache().collapseIconLocator.findElement(this);
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(expando));
            expando.click();
            getWrapper().waitFor(()-> isExpanded(),
                    "validator panel did not become expanded", 2000);
        }
        return this;
    }

    public boolean isExpanded()
    {
        return !elementCache().collapseIconLocator.existsIn(this);
    }


    public RegexValidatorPanel setExpression(String expression)
    {
        expand();
        elementCache().expressionInput().setValue(expression);
        return this;
    }

    public RegexValidatorPanel setDescription(String description)
    {
        expand();
        elementCache().descriptionInput().setValue(description);
        return this;
    }
    public String getDescription()
    {
        expand();
        return elementCache().descriptionInput().getValue();
    }

    public RegexValidatorPanel setErrorMessage(String message)
    {
        expand();
        elementCache().errorMessageInput().setValue(message);
        return this;
    }
    public String getErrorMessage()
    {
        return elementCache().errorMessageInput().getValue();
    }

    public RegexValidatorPanel setFailOnMatch(boolean checked)
    {
        expand();
        elementCache().failOnMatchCheckbox().set(checked);
        return this;
    }
    public boolean getFailOnMatch()
    {
        return elementCache().failOnMatchCheckbox().get();
    }

    public RegexValidatorPanel setName(String name)
    {
        expand();
        elementCache().nameInput().setValue(name);
        return this;
    }
    public String getName()
    {
        return elementCache().nameInput().getValue();
    }

    public void clickRemove()
    {
        expand();
        elementCache().removeButton.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(getComponentElement()));
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends WebDriverComponent.ElementCache
    {

        public Input expressionInput()
        {
            return Input(Locator.textarea("domainpropertiesrow-expression"), getDriver()).waitFor(this);
        }
        public Input descriptionInput()
        {
            return Input(Locator.textarea("domainpropertiesrow-description"), getDriver()).waitFor(this);
        }
        public Input errorMessageInput()
        {
            return Input(Locator.textarea("domainpropertiesrow-errorMessage"), getDriver()).waitFor(this);
        }

        public Checkbox failOnMatchCheckbox()
        {
            return new Checkbox(Locator.checkboxByName("domainpropertiesrow-failOnMatch").waitForElement(getComponentElement(), 2000));
        }
        public Input nameInput()
        {
            return Input(Locator.input("domainpropertiesrow-name"), getDriver()).waitFor(this);
        }

        final Locator collapseIconLocator = Locator.tagWithClass("div", "domain-validator-collapse-icon");
        final WebElement removeButton = Locator.button("Remove Validator").findWhenNeeded(this);
    }


    public static class RegexValidatorPanelFinder extends WebDriverComponentFinder<RegexValidatorPanel, RegexValidatorPanelFinder>
    {
        private Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "domain-validator-panel");
        private String _id = null;

        public RegexValidatorPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public RegexValidatorPanelFinder withIndex(int index)
        {
            _id = "domain-regex-validator-" + Integer.toString(index);
            return this;
        }
        public RegexValidatorPanelFinder openedByName(String name)          //finds the currently-expanded
        {
            _baseLocator = _baseLocator.withDescendant(Locator.input("domainpropertiesrow-name")
                    .withAttribute("value", name));
            return this;
        }

        @Override
        protected RegexValidatorPanel construct(WebElement el, WebDriver driver)
        {
            return new RegexValidatorPanel(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_id != null)
                return Locator.id(_id);
            else
                return _baseLocator;
        }
    }
}
