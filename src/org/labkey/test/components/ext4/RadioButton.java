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
package org.labkey.test.components.ext4;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebElement;

public class RadioButton extends Checkbox
{
    private RadioButton(WebElement radio)
    {
        super(radio);
    }

    public static RadioButtonFinder RadioButton()
    {
        return new RadioButtonFinder();
    }

    protected void assertElementType()
    {
        String backgroundImage = getComponentElement().getCssValue("background-image");
        Assert.assertTrue("Not a radio button: " + getComponentElement().toString(), backgroundImage.contains("radio"));
    }

    public static class RadioButtonFinder extends FormItemFinder<RadioButton, RadioButtonFinder>
    {
        public RadioButtonFinder() {}

        @Override
        protected RadioButton construct(WebElement el)
        {
            return new RadioButton(el);
        }

        protected Locator.XPathLocator itemLoc()
        {
            return Locator.tagWithClass("input", Ext4Helper.getCssPrefix() + "form-radio");
        }
    }
}
