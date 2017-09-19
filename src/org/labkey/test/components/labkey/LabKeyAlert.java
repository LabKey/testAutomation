package org.labkey.test.components.labkey;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.security.Credentials;

/**
 * Component for bootstrap modal created by LABKEY.Utils.alert()
 * clientapi/dom/Utils.js
 */
public class LabKeyAlert extends ModalDialog implements Alert
{
    public LabKeyAlert(WebDriver driver)
    {
        this(driver, 0);
    }

    public LabKeyAlert(WebDriver driver, long timeout)
    {
        super(Locator.id("lk-utils-modal").findWhenNeeded(driver).withTimeout(timeout), driver);
    }

    @Override
    public void dismiss()
    {
        close();
    }

    @Override
    public void accept()
    {
        close();
    }

    @Override
    public String getText()
    {
        return getTitle() + " : " + getBodyText();
    }

    @Override
    public void sendKeys(String keysToSend) { }

    @Override
    public void setCredentials(Credentials credentials) { }

    @Override
    public void authenticateUsing(Credentials credentials) { }
}
