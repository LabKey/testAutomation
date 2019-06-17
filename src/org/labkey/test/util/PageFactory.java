/*
 * Copyright (c) 2019 LabKey Corporation
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
