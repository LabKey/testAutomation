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
package org.labkey.test.components.internal;

import org.labkey.test.components.ext4.Window;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

public abstract class ImpersonateWindow extends Window<ImpersonateWindow.ElementCache>
{
    protected ImpersonateWindow(String windowTitle, WebDriver driver)
    {
        super(windowTitle, driver);
    }

    public void clickImpersonate()
    {
        getWrapper().doAndWaitForPageToLoad(() ->
        {
            elementCache().button.click();
            if (getDriver() instanceof FirefoxDriver)
            {
                final Alert alert = getWrapper().getAlertIfPresent();
                if (alert != null)
                {
                    final String alertText = alert.getText();
                    if (alertText.contains("Firefox must send information that will repeat any action"))
                    {
                        TestLogger.log("Ignoring alert on impersonation: " + alertText);
                        alert.accept();
                    }
                }
            }
        });
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Window.ElementCache
    {
        WebElement button = Ext4Helper.Locators.ext4Button("Impersonate").findWhenNeeded(this);
    }
}
