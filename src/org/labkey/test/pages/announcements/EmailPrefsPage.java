/*
 * Copyright (c) 2018 LabKey Corporation
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
package org.labkey.test.pages.announcements;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.html.Checkbox.Checkbox;

public class EmailPrefsPage extends LabKeyPage<EmailPrefsPage.ElementCache>
{
    public EmailPrefsPage(WebDriver driver)
    {
        super(driver);
    }

    public static EmailPrefsPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static EmailPrefsPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("announcements", containerPath, "emailPreferences"));
        return new EmailPrefsPage(driver.getDriver());
    }

    public EmailPrefsPage setNoNotify()
    {
        elementCache().notifyNone.click();
        return new EmailPrefsPage(getDriver());
    }

    public EmailPrefsPage setNotifyOnMine()
    {
        elementCache().notifyMine.click();
        return new EmailPrefsPage(getDriver());
    }

    public EmailPrefsPage setNotifyOnAll()
    {
        elementCache().notifyAll.click();
        return new EmailPrefsPage(getDriver());
    }

    public EmailPrefsPage setTypeIndividual()
    {
        elementCache().notifyAll.click();
        return new EmailPrefsPage(getDriver());
    }

    public EmailPrefsPage setTypeDigest()
    {
        elementCache().notifyAll.click();
        return new EmailPrefsPage(getDriver());
    }

    public EmailPrefsPage reset(boolean reset)
    {
        if (reset)
            elementCache().resetCheckbox.check();
        else
            elementCache().resetCheckbox.uncheck();
        return new EmailPrefsPage(getDriver());
    }

    public EmailPrefsPage update()
    {
        elementCache().updateButton.click();
        return new EmailPrefsPage(getDriver());
    }

    public LabKeyPage done()
    {
        clickAndWait(elementCache().doneButton);
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage cancel()
    {
        clickAndWait(elementCache().cancelButton);
        return new LabKeyPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private Locator.XPathLocator notify = Locator.radioButtonByName("emailPreference");
        protected WebElement notifyNone = notify.withAttribute("value", "0").findWhenNeeded(this);
        protected WebElement notifyMine = notify.withAttribute("value", "2").findWhenNeeded(this);
        protected WebElement notifyAll = notify.withAttribute("value", "1").findWhenNeeded(this);

        private Locator.XPathLocator type = Locator.radioButtonByName("notificationType");
        protected WebElement typeIndividual = notify.withAttribute("value", "3").findWhenNeeded(this);
        protected WebElement typeDigest = notify.withAttribute("value", "4").findWhenNeeded(this);

        Checkbox resetCheckbox = Checkbox(Locator.tagWithName("input", "emailPreference").withAttribute("value", "1")).findWhenNeeded(this);

        WebElement updateButton = Locator.lkButton("Update").findWhenNeeded(this);
        WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
        WebElement doneButton = Locator.lkButton("Done").findWhenNeeded(this);
    }
}