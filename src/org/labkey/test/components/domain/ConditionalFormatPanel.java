/*
 * Copyright (c) 2019-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.components.domain;

import org.labkey.remoteapi.domain.ConditionalFormat;
import org.labkey.remoteapi.query.Filter;
import org.labkey.test.Locator;
import org.labkey.test.components.ColorPickerInput;
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

    public ConditionalFormatPanel setConditionalFormat(ConditionalFormat conditionalFormat)
    {
        for (int i = 0; i < conditionalFormat.getQueryFilter().size(); i++)
        {
            var filterCondition = conditionalFormat.getQueryFilter().get(i);
            if (i==0)
            {
                setFirstCondition(filterCondition.getOperator());
                if (filterCondition.getValue() != null)
                    setFirstValue(filterCondition.getValue().toString());
            }
            if (i==1)
            {
                setSecondCondition(filterCondition.getOperator());
                if (filterCondition.getValue() != null)
                    setSecondValue(filterCondition.getValue().toString());
            }
        }
        setBoldCheckbox(conditionalFormat.getBold());
        setItalicsCheckbox(conditionalFormat.getItalic());
        setStrikethroughCheckbox(conditionalFormat.getStrikethrough());
        setTextColor(conditionalFormat.getTextColor());
        setFillColor(conditionalFormat.getBackgroundColor());
        return this;
    }

    public ConditionalFormatPanel setFirstCondition(Filter.Operator operator)
    {
        expand();
        elementCache().firstConditionSelect().selectByValue(operator.getUrlKey());
        return this;
    }
    public String getFirstCondition()
    {
        return elementCache().firstConditionSelect().getFirstSelectedOption().getText();
    }

    public ConditionalFormatPanel setFirstValue(String value)
    {
        expand();
        elementCache().firstFilterValueInput().setValue(value);
        return this;
    }
    public String getFirstValue()
    {
        return elementCache().firstFilterValueInput().getValue();
    }

    public ConditionalFormatPanel setSecondCondition(Filter.Operator operator)
    {
        expand();
        elementCache().secondConditionSelect().selectByValue(operator.getUrlKey());
        return this;
    }
    public String getSecondCondition()
    {
        return elementCache().secondConditionSelect().getFirstSelectedOption().getText();
    }

    public ConditionalFormatPanel setSecondValue(String value)
    {
        expand();
        elementCache().secondFilterValueInput().setValue(value);
        return this;
    }
    public String getSecondValue()
    {
        return elementCache().secondFilterValueInput().getValue();
    }

    public ConditionalFormatPanel setBoldCheckbox(boolean checked)
    {
        expand();
        elementCache().boldCheckbox().set(checked);
        return this;
    }

    public boolean getBoldChecked()
    {
        return elementCache().boldCheckbox().get();
    }

    public ConditionalFormatPanel setItalicsCheckbox(boolean checked)
    {
        expand();
        elementCache().italicsCheckbox().set(checked);
        return this;
    }

    public boolean getItalicsChecked()
    {
        return elementCache().italicsCheckbox().get();
    }

    public ConditionalFormatPanel setStrikethroughCheckbox(boolean checked)
    {
        expand();
        elementCache().strikethroughCheckbox().set(checked);
        return this;
    }

    public boolean getStrikethroughChecked()
    {
        return elementCache().strikethroughCheckbox().get();
    }

    public ConditionalFormatPanel setTextColor(String colorHex)
    {
        expand();
        setColor(elementCache().textColorPicker, colorHex);
        return this;
    }

    public String getTextStyle()
    {
        expand();
        return Locator.byClass("color-picker__chip").findElement(elementCache().textColorPicker)
                .getAttribute("style");
    }

    public ConditionalFormatPanel setFillColor(String colorHex)
    {
        expand();
        setColor(elementCache().fillColorPicker, colorHex);
        return this;
    }

    private void setColor(WebElement colorPickerContainer, String colorHex)
    {
        Locator.tagWithClassContaining("button", "color-picker__button").findElement(colorPickerContainer).click();
        ColorPickerInput colorInput = new ColorPickerInput.ColorPickerInputFinder(getDriver()).findWhenNeeded();
        colorInput.setHexValue(colorHex);
        colorPickerContainer.click(); // need to click outside the color picker to close it
    }

    public String getFillStyle()
    {
        expand();
        return Locator.byClass("color-picker__chip").findElement(elementCache().fillColorPicker)
                .getAttribute("style");
    }

    public String getPreviewTextStyle()
    {
        expand();
        return elementCache().previewTextInput.getAttribute("style");
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
        final WebElement textColorPicker = Locator.byClass("color-picker").withAttribute("data-name", "textColor").findWhenNeeded(this);
        final WebElement fillColorPicker = Locator.byClass("color-picker").withAttribute("data-name", "backgroundColor").findWhenNeeded(this);
        final WebElement previewTextInput = Locator.tagWithAttribute("input", "value", "Preview Text").findWhenNeeded(this);
        final WebElement removeButton = Locator.button("Remove Formatting").findWhenNeeded(this);
    }


    static class ConditionalFormatPanelFinder extends WebDriverComponentFinder<ConditionalFormatPanel, ConditionalFormatPanelFinder>
    {
        private final ConditionalFormatDialog _dialog;
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
