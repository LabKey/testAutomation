/*
 * Copyright (c) 2014-2015 LabKey Corporation
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
package org.labkey.test.pages;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.UnhandledAlertException;

// TODO: Tons of missing functionality
// TODO: Much ListHelper functionality belongs here
// TODO: Create subclasses for particular domain editors (e.g. Metadata)
public abstract class DomainEditor
{
    protected BaseWebDriverTest _test;

    private static final String ROW_HIGHLIGHT = "238"; // rgb(238,238,238)

    public DomainEditor(BaseWebDriverTest test)
    {
        _test = test;
        waitForReady();
    }

    public void waitForReady()
    {
        _test.waitForElement(Locator.lkButton("Cancel"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElement(Locator.lkButton("Add Field"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void selectField(int index)
    {
        selectField(Locator.tagWithClass("tr", "editor-field-row").withDescendant(Locator.xpath("td/div").withAttribute("id", "partstatus_" + index)));
    }

    private void selectField(final Locator.XPathLocator rowLocator)
    {
        _test.click(rowLocator.append("/td")); // First td (status) should be safe to click

        _test.waitForElement(rowLocator.withClass("selected-field-row"));
    }

    public void clickTab(String tab)
    {
        _test.click(Locator.tagWithAttribute("ul", "role", "tablist").append("/li").withText(tab));
        _test.waitForElement(Locator.tagWithClass("li", "x-tab-strip-active").withText(tab));
    }

    public void save()
    {
        _test.sleep(500); // Wait for GWT form to not be dirty
        _test.clickButton("Save", 0);
        _test.waitForElement(Locator.tagWithClass("div", "gwt-HTML").withText("Save successful."), 20000);
        _test.sleep(500); // Wait for GWT form to not be dirty
    }

    public void saveAndClose()
    {
        _test.sleep(500); // Wait for GWT form to not be dirty
        try
        {
            _test.clickButton("Save & Close");
        }
        catch (UnhandledAlertException alert)
        {
            if (alert.getAlertText().contains("data you have entered may not be saved."))
            {
                _test.clickButton("Save & Close");
            }
            else
                throw alert;
        }
    }

    public static class Locators
    {
        public static Locator.XPathLocator fieldPanel(String panelTitle)
        {
            return Locator.tagWithClass("table", "labkey-wp").withPredicate(PortalHelper.Locators.webPartTitle(panelTitle));
        }
    }
}
