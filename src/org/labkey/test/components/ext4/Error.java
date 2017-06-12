package org.labkey.test.components.ext4;

import org.openqa.selenium.WebDriver;

/**
 * Wrapper for common Ext4 "Error" alert with an OK button
 */
public class Error extends Message
{
    public Error(WebDriver driver)
    {
        super("Error", driver);
    }

    public void clickOk()
    {
        clickButton("OK", true);
    }
}
