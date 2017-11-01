/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.components.html;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

public class RadioButton extends Checkbox
{
    public RadioButton(WebElement element)
    {
        super(element);
    }

    public static SimpleComponentFinder<RadioButton> RadioButton(Locator loc)
    {
        return finder().locatedBy(loc);
    }

    public static RadioButtonFinder finder()
    {
        return new RadioButtonFinder();
    }

    protected void assertElementType()
    {
        String type = getComponentElement().getCssValue("type");
        Assert.assertEquals("Not a checkbox: " + getComponentElement().toString(), "checkbox", type);
    }

    public static class RadioButtonFinder extends ComponentFinder<SearchContext, RadioButton, RadioButtonFinder>
    {
        private Locator loc = Locator.radioButton();

        public SimpleComponentFinder<RadioButton> withName(String name)
        {
            return super.locatedBy(Locator.radioButtonByName(name));
        }

        public SimpleComponentFinder<RadioButton> withNameAndValue(String name, String value)
        {
            return super.locatedBy(Locator.radioButtonByNameAndValue(name, value));
        }

        @Override
        protected Locator locator()
        {
            return loc;
        }

        @Override
        protected RadioButton construct(WebElement el)
        {
            return new RadioButton(el);
        }
    }
}
