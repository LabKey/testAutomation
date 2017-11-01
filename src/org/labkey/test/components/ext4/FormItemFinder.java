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

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.openqa.selenium.SearchContext;

public abstract class FormItemFinder<C, F extends FormItemFinder<C, F>> extends Component.ComponentFinder<SearchContext, C, F>
{
    private String labelText = null;
    private boolean partialText = true;

    public F withLabel(String text)
    {
        this.labelText = text;
        partialText = false;
        return (F)this;
    }

    public F withLabelContaining(String text)
    {
        this.labelText = text;
        partialText = true;
        return (F)this;
    }

    protected Locator locator()
    {
        if (labelText == null)
            return itemLoc();
        return itemLoc().withPredicate(
                partialText
                        ? labelLoc().containing(labelText)
                        : labelLoc().withText(labelText));
    }

    protected abstract Locator.XPathLocator itemLoc();

    protected Locator.XPathLocator labelLoc()
    {
        return Locator.xpath("(../label|../../td/label|../td/label)"); // Slightly different DOM structure across versions of Ext4
    }
}
