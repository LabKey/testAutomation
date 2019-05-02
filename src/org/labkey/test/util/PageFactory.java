package org.labkey.test.util;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

import java.util.function.Function;

public class PageFactory<P extends LabKeyPage>
{
    private final RelativeUrl url;
    private final Function<WebDriver, P> pageConstructor;
    private String containerPath = null;

    PageFactory(RelativeUrl url, Function<WebDriver, P> pageConstructor)
    {
        this.url = url.copy();
        this.pageConstructor = pageConstructor;
    }

    public final PageFactory<P> setContainerPath(String containerPath)
    {
        url.setContainerPath(containerPath);
        return this;
    }

    public final P navigate(WebDriverWrapper driverWrapper)
    {
        return navigate(driverWrapper, url);
    }

    public final P navigate(WebDriverWrapper driverWrapper, Integer msTimeout)
    {
        return navigate(driverWrapper, url.copy().setTimeout(msTimeout));
    }

    protected P navigate(WebDriverWrapper driverWrapper, RelativeUrl url)
    {
        if (url.getContainerPath() == null)
        {
            url = url.copy().setContainerPath(driverWrapper.getCurrentContainerPath());
        }
        url.navigate(driverWrapper);
        return pageConstructor.apply(driverWrapper.getDriver());
    }
}
