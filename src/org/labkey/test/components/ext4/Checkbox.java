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

public class Checkbox extends org.labkey.test.components.html.Checkbox
{
    public Checkbox(WebElement checkbox)
    {
        super(checkbox);
    }

    public static CheckboxFinder Ext4Checkbox()
    {
        return new CheckboxFinder();
    }

    public boolean isChecked()
    {
        assertElementType();

        String atlasPosition = getComponentElement().getCssValue("background-position");
        String atlasYOffset = atlasPosition.split(" ")[1];
        return atlasYOffset.contains("-"); // Probably '-13px' or '-26px'. Unchecked states are all at offset zero
    }

    protected void assertElementType()
    {
        String backgroundImage = getComponentElement().getCssValue("background-image");
        Assert.assertTrue("Not a checkbox or radio button: " + getComponentElement().toString(), backgroundImage.contains("checkbox") || backgroundImage.contains("radio"));
    }

    public static class CheckboxFinder extends FormItemFinder<Checkbox, CheckboxFinder>
    {
        @Override
        protected Checkbox construct(WebElement el)
        {
            return new Checkbox(el);
        }

        protected Locator.XPathLocator itemLoc()
        {
            return Locator.tagWithClass("input", Ext4Helper.getCssPrefix() + "form-checkbox");
        }
    }
}
