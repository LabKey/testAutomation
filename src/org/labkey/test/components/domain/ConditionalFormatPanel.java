package org.labkey.test.components.domain;

import org.labkey.remoteapi.query.Filter;
import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.SelectWrapper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import static org.labkey.test.components.html.Input.Input;

public class ConditionalFormatPanel extends WebDriverComponent<ConditionalFormatPanel.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public ConditionalFormatPanel(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    public ConditionalFormatPanel setFirstCondition(Filter.Operator operator)
    {
        elementCache().firstConditionSelect().selectByValue(operator.getUrlKey());
        return this;
    }
    public ConditionalFormatPanel setFirstValue(String value)
    {
        elementCache().firstFilterValueInput().setValue(value);
        return this;
    }

    public ConditionalFormatPanel setSecondCondition(Filter.Operator operator)
    {
        elementCache().secondConditionSelect().selectByValue(operator.getUrlKey());
        return this;
    }
    public ConditionalFormatPanel setSecondValue(String value)
    {
        elementCache().secondFilterValueInput().setValue(value);
        return this;
    }

    public ConditionalFormatPanel setBoldCheckbox(boolean checked)
    {
        elementCache().boldCheckbox().set(checked);
        return this;
    }
    public ConditionalFormatPanel setItalicsCheckbox(boolean checked)
    {
        elementCache().italicsCheckbox().set(checked);
        return this;
    }
    public ConditionalFormatPanel setStrikethroughCheckbox(boolean checked)
    {
        elementCache().strikethroughCheckbox().set(checked);
        return this;
    }

    public void clickRemove()
    {
        elementCache().removeButton.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(getComponentElement()));
    }

    public ConditionalFormatPanel expand()
    {
        if (!isExpanded())
        {
            elementCache().collapseIconLocator.findElement(this).click();
            getWrapper().waitFor(()-> isExpanded(),
                    "conditional format panel did not become expanded", 1000);
        }
        return this;
    }

    public boolean isExpanded()
    {
        return !elementCache().collapseIconLocator.existsIn(this);
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

        public Checkbox boldCheckbox()
        {
            return new Checkbox(Locator.checkboxByName("domainpropertiesrow-bold").waitForElement(this, 1000));
        }
        public Checkbox italicsCheckbox()
        {
            return new Checkbox(Locator.checkboxByName("domainpropertiesrow-italic").waitForElement(this, 1000));
        }
        public Checkbox strikethroughCheckbox()
        {
            return new Checkbox(Locator.checkboxByName("domainpropertiesrow-strikethrough").waitForElement(this, 1000));
        }

        final Locator collapseIconLocator = Locator.tagWithClass("div", "domain-validator-collapse-icon");
        final WebElement removeButton = Locator.button("Remove Validator").findWhenNeeded(this);
    }


    public static class ConditionalFormatPanelFinder extends WebDriverComponentFinder<ConditionalFormatPanel, ConditionalFormatPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "domain-validator-panel");
        private String _title = null;

        public ConditionalFormatPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public ConditionalFormatPanelFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected ConditionalFormatPanel construct(WebElement el, WebDriver driver)
        {
            return new ConditionalFormatPanel(el, driver);
        }

        /**
         * TODO:
         * Add methods and fields, as appropriate, to build a Locator that will find the element(s)
         * that this component represents
         */
        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withAttribute("title", _title);
            else
                return _baseLocator;
        }
    }
}
