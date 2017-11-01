/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.test.pages.issues;

import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.components.html.FormItem;
import org.labkey.test.components.labkey.FormItemFinder;
import org.labkey.test.components.labkey.GenericFormItem;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.Locator.xq;

/**
 * Needed to extend to work with some of the issue list specific DOM layout
 * @param <C>
 */
public abstract class IssuesFormItemFinder<C> extends org.labkey.test.components.labkey.FormItemFinder<C>
{
    public static FormItemFinder<FormItem> IssueFormItem(WebDriver driver)
    {
        return new IssuesFormItemFinder<FormItem>()
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

    @Override
    protected Locator locator()
    {
        Locator.XPathLocator itemTd = labelLoc().followingSibling("td").position(1);
        if (name != null && !name.isEmpty())
            return itemTd.child(Locator.tagWithName(itemTag(), name));
        else
            return itemTd.childTag(itemTag());
    }

    protected Locator.XPathLocator labelLoc()
    {
        return _labelLoc();
    }

    private Locator.XPathLocator _labelLoc()
    {
        Locator.XPathLocator loc = Locator.tag("td").withClass("lk-form-label");
        if (partialText) // Don't match nested elements (e.g. '?' for help)
        {
            if (labelText.isEmpty())
                return loc;
            return loc.withPredicate("contains(text(), " + xq(labelText) + ")");
        }
        else
            return loc.withPredicate("text() = " + xq(labelText) + " or text() = " + xq(labelText + " *") + " or text() = " + xq(labelText + ":"));
    }
}
