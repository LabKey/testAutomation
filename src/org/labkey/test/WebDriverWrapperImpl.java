package org.labkey.test;

import org.openqa.selenium.WebDriver;

public class WebDriverWrapperImpl extends WebDriverWrapper
{
    WebDriver driver;

    public WebDriverWrapperImpl(WebDriver driver)
    {
        this.driver = driver;
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return driver;
    }
}
