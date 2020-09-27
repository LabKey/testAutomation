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
    final ConditionalFormatDialog _dialog;

    public ConditionalFormatPanel(WebElement element, ConditionalFormatDialog dialog)
    {
        _el = element;
        _dialog = dialog;
    }

    public ConditionalFormatPanel setFirstCondition(Filter.Operator operator)
    {
        expand();
        elementCache().firstConditionSelect().selectByValue(operator.getUrlKey());
        return this;
    }
    public ConditionalFormatPanel setFirstValue(String value)
    {
        expand();
        elementCache().firstFilterValueInput().setValue(value);
        return this;
    }

    public ConditionalFormatPanel setSecondCondition(Filter.Operator operator)
    {
        expand();
        elementCache().secondConditionSelect().selectByValue(operator.getUrlKey());
        return this;
    }
    public ConditionalFormatPanel setSecondValue(String value)
    {
        expand();
        elementCache().secondFilterValueInput().setValue(value);
        return this;
    }

    public ConditionalFormatPanel setBoldCheckbox(boolean checked)
    {
        expand();
        elementCache().boldCheckbox().set(checked);
        return this;
    }
    public ConditionalFormatPanel setItalicsCheckbox(boolean checked)
    {
        expand();
        elementCache().italicsCheckbox().set(checked);
        return this;
    }
    public ConditionalFormatPanel setStrikethroughCheckbox(boolean checked)
    {
        expand();
        elementCache().strikethroughCheckbox().set(checked);
        return this;
    }

    public ConditionalFormatPanel setTextColor(String colorHex)
    {
        expand();
        elementCache().textColor.click();
        getWrapper().click(Locator.tagWithAttribute("div", "title", colorHex));
        getWrapper().click(Locator.tagWithClass("div", "domain-validator-color-cover")); // click elsewhere on the dialog to close the color picker
        return this;
    }

    public ConditionalFormatPanel setFillColor(String colorHex)
    {
        expand();
        elementCache().fillColor.click();
        getWrapper().click(Locator.tagWithAttribute("div", "title", colorHex));
        getWrapper().click(Locator.tagWithClass("div", "domain-validator-color-cover")); // click elsewhere on the dialog to close the color picker
        return this;
    }

    public ConditionalFormatDialog clickRemove()
    {
        expand();
        elementCache().removeButton.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(getComponentElement()));
        return _dialog;
    }

    public ConditionalFormatPanel expand()
    {
        if (!isExpanded())
        {
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().collapseIconLocator));
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
        return _dialog.getDriver();
    }


    @Override
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
        final WebElement textColor = Locator.tagWithName("button", "domainpropertiesrow-textColor").findWhenNeeded(this);
        final WebElement fillColor = Locator.tagWithName("button", "domainpropertiesrow-backgroundColor").findWhenNeeded(this);
        final WebElement removeButton = Locator.button("Remove Formatting").findWhenNeeded(this);
    }


    static class ConditionalFormatPanelFinder extends WebDriverComponentFinder<ConditionalFormatPanel, ConditionalFormatPanelFinder>
    {
        private ConditionalFormatDialog _dialog;
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "domain-validator-panel");
        private String _id = null;

        public ConditionalFormatPanelFinder(ConditionalFormatDialog dialog)
        {
            super(dialog.getDriver());
            _dialog = dialog;
        }

        public ConditionalFormatPanelFinder withIndex(int index)
        {
            _id = "domain-condition-format-" + Integer.toString(index);
            return this;
        }

        @Override
        protected ConditionalFormatPanel construct(WebElement el, WebDriver driver)
        {
            return new ConditionalFormatPanel(el, _dialog);
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
