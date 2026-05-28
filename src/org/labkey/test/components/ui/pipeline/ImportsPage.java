/*
 * Copyright (c) 2023-2026 LabKey Corporation
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
package org.labkey.test.components.ui.pipeline;

import org.apache.commons.lang3.StringUtils;
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
                15_000);
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
        QueryGrid grid = elementCache().pipelineJobsGrid();
        waitFor(grid::isLoaded, "Imports grid did not become active in time.", 2_500);
        return grid;
    }

    public ImportsPage clickCancel()
    {
        elementCache().cancelButton.click();
        return new ImportsPage(this);
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

        final WebElement cancelButton = Locator.button("Cancel").findWhenNeeded(this);

        final QueryGrid pipelineJobsGrid()
        {
            return new QueryGrid.QueryGridFinder(getDriver()).find(this);
        }

    }

}
