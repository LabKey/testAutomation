package org.labkey.test.components.ui.pipeline;

import org.apache.tika.utils.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.URLBuilder;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

public class ImportsPage extends LabKeyPage<LabKeyPage<?>.ElementCache>
{

    public ImportsPage(WebDriverWrapper driver)
    {
        super(driver);
    }


    @Override
    protected void waitForPage()
    {
        waitFor(()-> {
                    try
                    {
                        return elementCache().pageHeader().isDisplayed() &&
                                elementCache().pipelineJobsGrid().isLoaded();
                    }
                    catch(NoSuchElementException | StaleElementReferenceException nse)
                    {
                        return false;
                    }
                },
                "The 'Background Imports' page did not load in time.",
                2_500);
    }

    public static ImportsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static ImportsPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        if (StringUtils.isBlank(containerPath) || "home".equalsIgnoreCase(containerPath))
        {
            throw new IllegalArgumentException("Invalid app containerPath: " + containerPath);
        }
        webDriverWrapper.beginAt(new URLBuilder("sampleManager", "app", containerPath)
                .setAppResourcePath("pipeline")
                .buildURL());
        return new ImportsPage(webDriverWrapper);
    }

    public String getPageHeader()
    {
        return elementCache().pageHeader().getText();
    }

    public QueryGrid getImportsGrid()
    {
        return elementCache().pipelineJobsGrid();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {

        final WebElement pageHeader()
        {
            return Locator.tagWithClass("div", "page-header")
                    .child(Locator.tagWithClass("h2", "no-margin-top"))
                    .findWhenNeeded(this);
        }

        final QueryGrid pipelineJobsGrid()
        {
            return new QueryGrid.QueryGridFinder(getDriver()).find(this);
        }

    }

}
