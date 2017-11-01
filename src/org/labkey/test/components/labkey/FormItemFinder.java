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
package org.labkey.test.components.labkey;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.html.FormItem;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.Locator.xq;

public abstract class FormItemFinder<C> extends Component.ComponentFinder<SearchContext, C, FormItemFinder<C>>
{
    protected String name = null;
    protected String labelText = "";
    protected boolean partialText = true;

    public static FormItemFinder<FormItem> FormItem(WebDriver driver)
    {
        return new FormItemFinder<FormItem>()
        {
            @Override
            protected FormItem construct(WebElement el)
            {
                return new GenericFormItem(el, driver);
            }

            @Override
            protected String itemTag()
            {
                return "descendant-or-self::*";
            }
        };
    }

    protected abstract String itemTag();

    public FormItemFinder<C> withName(@NotNull String name)
    {
        this.name = name;
        return this;
    }

    public FormItemFinder<C> withLabel(@NotNull String text)
    {
        this.labelText = text;
        partialText = false;
        return this;
    }

    public FormItemFinder<C> withLabelContaining(@NotNull String text)
    {
        this.labelText = text;
        partialText = true;
        return this;
    }

    @Override
    protected Locator locator()
    {
        Locator.XPathLocator itemTd = labelLoc().followingSibling("div").position(1);
        if (name != null && !name.isEmpty())
            return itemTd.child(Locator.tagWithName(itemTag(), name));
        else
            return itemTd.childTag(itemTag());
    }

    protected Locator.XPathLocator labelLoc()
    {
        Locator.XPathLocator loc = Locator.tagWithClass("label", "control-label");
        if (partialText) // Don't match nested elements (e.g. '?' for help)
        {
            if (labelText.isEmpty())
                return loc;
            return loc.withPredicate("contains(text(), " + xq(labelText) + ")");
        }
        else
            return loc.withPredicate("text() = " + xq(labelText) + " or text() = " + xq(labelText + " *"));
    }
}
