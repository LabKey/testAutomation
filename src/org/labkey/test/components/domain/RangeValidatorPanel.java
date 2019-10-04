package org.labkey.test.components.domain;

import org.labkey.remoteapi.query.Filter;
import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.SelectWrapper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import static org.labkey.test.components.html.Input.Input;

public class RangeValidatorPanel extends WebDriverComponent<RangeValidatorPanel.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public RangeValidatorPanel(WebElement element, WebDriver driver)
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


    public RangeValidatorPanel setFirstCondition(Filter.Operator operator)
    {
        elementCache().firstConditionSelect().selectByValue(operator.getUrlKey());
        return this;
    }

    public RangeValidatorPanel setFirstValue(String value)
    {
        elementCache().firstFilterValueInput().setValue(value);
        return this;
    }

    public RangeValidatorPanel setSecondCondition(Filter.Operator operator)
    {
        elementCache().secondConditionSelect().selectByValue(operator.getUrlKey());
        return this;
    }

    public RangeValidatorPanel setSecondValue(String value)
    {
        elementCache().secondFilterValueInput().setValue(value);
        return this;
    }

    public RangeValidatorPanel setDescription(String value)
    {
        elementCache().descriptionInput().setValue(value);
        return this;
    }

    public RangeValidatorPanel setErrorMessage(String value)
    {
        elementCache().errorMessageInput().setValue(value);
        return this;
    }

    public RangeValidatorPanel setName(String name)
    {
        elementCache().nameInput().setValue(name);
        return this;
    }

    public void clickRemove()
    {
        elementCache().removeButton.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(getComponentElement()));
    }

    public RangeValidatorPanel expand() // note: there isn't a corresponding 'collapse' on this object;
    {                                   // to collapse this one, expand another one
        if (!isExpanded())
        {
            elementCache().collapseIconLocator.findElement(this).click();
            getWrapper().waitFor(()-> isExpanded(),
                    "validator panel did not become expanded", 1000);
        }
        return this;
    }

    public boolean isExpanded()
    {
        return !elementCache().collapseIconLocator.existsIn(this);
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends WebDriverComponent.ElementCache
    {

        public Select firstConditionSelect()        // these elements are transient; when the panel is collapsed they disappear
        {
            return SelectWrapper.Select(
                    Locator.tagWithAttribute("select", "name", "domainpropertiesrow-firstFilterType"))
                    .waitFor(this);
        }
        public Input firstFilterValueInput()
        {
            return Input(Locator.input("domainpropertiesrow-firstFilterValue"), getDriver()).waitFor(this);
        }

        public Select secondConditionSelect()
        {
            return SelectWrapper.Select(
                    Locator.tagWithAttribute("select", "name", "domainpropertiesrow-secondFilterType"))
                    .waitFor(this);
        }
        public Input secondFilterValueInput()
        {
            return Input(Locator.input("domainpropertiesrow-secondFilterValue"), getDriver()).waitFor(this);
        }

        public Input descriptionInput()
        {
            return Input(Locator.textarea("domainpropertiesrow-description"), getDriver()).waitFor(this);
        }
        public Input errorMessageInput()
        {
            return Input(Locator.textarea("domainpropertiesrow-errorMessage"), getDriver()).waitFor(this);
        }
        public Input nameInput()
        {
            return Input(Locator.input("domainpropertiesrow-name"), getDriver()).waitFor(this);
        }

        final Locator collapseIconLocator = Locator.tagWithClass("div", "domain-validator-collapse-icon");
        final WebElement removeButton = Locator.button("Remove Validator").findWhenNeeded(this);
    }


    public static class RangeValidatorPanelFinder extends WebDriverComponentFinder<RangeValidatorPanel, RangeValidatorPanelFinder>
    {
        private Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "domain-validator-panel");
        private String _name = null;

        public RangeValidatorPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public RangeValidatorPanelFinder closedByName(String name)          //finds
        {
            _name = name;
            return this;
        }
        public RangeValidatorPanelFinder openedByName(String name)          //finds the currently-expanded
        {
            _baseLocator = _baseLocator.withDescendant(Locator.input("domainpropertiesrow-name")
                    .withAttribute("value", name));
            return this;
        }

        @Override
        protected RangeValidatorPanel construct(WebElement el, WebDriver driver)
        {
            return new RangeValidatorPanel(el, driver);
        }


        @Override
        protected Locator locator()
        {
            if (_name != null)
                return _baseLocator.withDescendant(Locator.tagWithClass("div", "domain-validator-collapse-icon"))
                        .startsWith(_name + ":");
            else
                return _baseLocator;
        }
    }
}
