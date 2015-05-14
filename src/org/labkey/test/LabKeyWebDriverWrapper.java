package org.labkey.test;

import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * TODO: Move basic page interactions from BWDT to this class (or something like it)
 */
public class LabKeyWebDriverWrapper extends BaseWebDriverTest implements AutoCloseable
{
    private WebDriver extraDriver;

    public LabKeyWebDriverWrapper()
    {
        super();
        this.extraDriver = createNewWebDriver(null);
    }

    @Override
    public void close()
    {
        quit();
    }

    public void quit()
    {
        getDriver().quit();
    }

    @Override
    public WebDriver getDriver()
    {
        return extraDriver;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public void pauseJsErrorChecker(){}
    @Override
    public void resumeJsErrorChecker(){}

    @Nullable
    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}

