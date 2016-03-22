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
import org.openqa.selenium.WebDriver;

// TODO: Tons of missing functionality
// TODO: Much ListHelper functionality belongs here
// TODO: Create subclasses for particular domain editors (e.g. Metadata)
public abstract class BaseDesignerPage extends LabKeyPage
{
    protected static final String DESIGNER_SIGNAL = "designerDirty"; //org.labkey.api.gwt.client.AbstractDesignerMainPanel.java
    private static final String ROW_HIGHLIGHT = "238"; // rgb(238,238,238)

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
        if (Boolean.valueOf(doAndWaitForPageSignal(func, DESIGNER_SIGNAL)))
            return;
        waitForElement(Locators.pageSignal(DESIGNER_SIGNAL, "true"));
    }

    protected void doAndExpectClean(Runnable func)
    {
        if (!Boolean.valueOf(doAndWaitForPageSignal(func, DESIGNER_SIGNAL)))
            return;
        waitForElement(Locators.pageSignal(DESIGNER_SIGNAL, "false"));
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

    public void save()
    {
        sleep(1000); // Wait for GWT form to have all changes
        doAndExpectClean(() -> clickButton("Save", 0));
        waitForElement(Locator.tagWithClass("div", "gwt-HTML").withText("Save successful."), 20000);
    }

    public void saveAndClose()
    {
        Locators.pageSignal(DESIGNER_SIGNAL).waitForElement(getDriver(), 1000);
        try
        {
            clickButton("Save & Close");
        }
        catch (UnhandledAlertException alert)
        {
            if (alert.getAlertText().contains("data you have entered may not be saved."))
            {
                dismissAllAlerts();
                sleep(2000); // Wait for GWT form to not be dirty
                clickButton("Save & Close");
            }
            else
                throw alert;
        }
    }

    public static class Locators extends org.labkey.test.Locators
    {
        public static Locator.XPathLocator fieldPanel(String panelTitle)
        {
            return Locator.tagWithClass("table", "labkey-wp").withPredicate(PortalHelper.Locators.webPartTitle(panelTitle));
        }
    }
}
