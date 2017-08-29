/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
import org.labkey.test.Locators;
import org.labkey.test.selenium.RefindingWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

// TODO: Tons of missing functionality
// TODO: Much ListHelper functionality belongs here
// TODO: Create subclasses for particular designer pages (e.g. Metadata)
public abstract class BaseDesignerPage<EC extends BaseDesignerPage.ElementCache> extends LabKeyPage<EC>
{
    protected static final String DESIGNER_DIRTY_SIGNAL = "designerDirty"; //org.labkey.api.gwt.client.AbstractDesignerMainPanel.java

    public BaseDesignerPage(WebDriver driver)
    {
        super(driver);
        waitForReady();
    }

    public void waitForReady()
    {
        waitForElement(Locator.lkButton("Cancel"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.lkButton("Add Field"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    protected void doAndExpectDirty(Runnable func)
    {
        if (Boolean.valueOf(doAndWaitForPageSignal(func, DESIGNER_DIRTY_SIGNAL)))
            return;
        waitForElement(Locators.pageSignal(DESIGNER_DIRTY_SIGNAL, "true"));
    }

    protected void doAndExpectClean(Runnable func)
    {
        if (!Boolean.valueOf(doAndWaitForPageSignal(func, DESIGNER_DIRTY_SIGNAL)))
            return;
        waitForElement(Locators.pageSignal(DESIGNER_DIRTY_SIGNAL, "false"));
    }

    public void selectField(int index)
    {
        selectField(Locator.tagWithClass("tr", "editor-field-row").withDescendant(Locator.xpath("td/div").withAttribute("id", "partstatus_" + index)));
    }

    private void selectField(final Locator.XPathLocator rowLocator)
    {
        click(rowLocator.append("/td")); // First td (status) should be safe to click

        waitForElement(rowLocator.withClass("selected-field-row"));
    }

    public void clickTab(String tab)
    {
        click(Locator.tagWithAttribute("ul", "role", "tablist").append("/li").withText(tab));
        waitForElement(Locator.tagWithClass("li", "x-tab-strip-active").withText(tab));
    }

    public LabKeyPage save()
    {
        doAndExpectClean(() -> clickButton("Save", 0));
        waitForElement(Locator.tagWithClass("div", "gwt-HTML").withText("Save successful."), 20000);
        clearCache();
        return null;
    }

    public LabKeyPage saveAndClose()
    {
        clickButton("Save & Close");
        return null;
    }

    @Override
    protected EC newElementCache()
    {
        return (EC)new ElementCache();
    }

    public class ElementCache extends LabKeyPage.ElementCache
    {
        public WebElement saveButton = new RefindingWebElement(Locator.lkButton("Save"), this);
        public WebElement cancelButton = new RefindingWebElement(Locator.lkButton("Cancel"), this);
    }
}
