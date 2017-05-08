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
