package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

public class MapNetworkDrivePage extends LabKeyPage<MapNetworkDrivePage.ElementCache>
{
    public MapNetworkDrivePage(WebDriver driver)
    {
        super(driver);
    }

    public MapNetworkDrivePage setDriveLetter(String value)
    {
        elementCache().networkDriveLetter.set(value);
        return this;
    }

    public MapNetworkDrivePage setPath(String value)
    {
        elementCache().networkDrivePath.set(value);
        return this;
    }

    public MapNetworkDrivePage setUser(String value)
    {
        elementCache().networkDriveUser.set(value);
        return this;
    }

    public MapNetworkDrivePage setPassword(String value)
    {
        elementCache().networkDrivePassword.set(value);
        return this;
    }

    protected MapNetworkDrivePage.ElementCache newElementCache()
    {
        return new MapNetworkDrivePage.ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected final Input networkDriveLetter = Input.Input(Locator.id("networkDriveLetter"), getDriver()).findWhenNeeded(this);
        protected final Input networkDrivePath = Input.Input(Locator.id("networkDrivePath"), getDriver()).findWhenNeeded(this);
        protected final Input networkDriveUser = Input.Input(Locator.id("networkDriveUser"), getDriver()).findWhenNeeded(this);
        protected final Input networkDrivePassword = Input.Input(Locator.id("networkDrivePassword"), getDriver()).findWhenNeeded(this);
    }
}
