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

public class PageFactory<P extends LabKeyPage<?>>
{
    private final RelativeUrl _url;
    private final Function<WebDriver, P> _pageConstructor;

    PageFactory(RelativeUrl url, Function<WebDriver, P> pageConstructor)
    {
        this._url = url.copy();
        this._pageConstructor = pageConstructor;
    }

    public final PageFactory<P> setContainerPath(String containerPath)
    {
        _url.setContainerPath(containerPath);
        return this;
    }

    public final <T> PageFactory<P> addParameter(String name, T value)
    {
        _url.addParameter(name, value);
        return this;
    }

    public final P navigate(WebDriverWrapper driverWrapper)
    {
        return navigate(driverWrapper, _url);
    }

    public final P navigate(WebDriverWrapper driverWrapper, String containerPath)
    {
        return navigate(driverWrapper, _url.copy().setContainerPath(containerPath));
    }

    public final P navigate(WebDriverWrapper driverWrapper, Integer msTimeout)
    {
        return navigate(driverWrapper, _url.copy().setTimeout(msTimeout));
    }

    protected P navigate(WebDriverWrapper driverWrapper, RelativeUrl url)
    {
        if (url.getContainerPath() == null)
        {
            url = url.copy().setContainerPath(driverWrapper.getCurrentContainerPath());
        }
        url.navigate(driverWrapper);
        return _pageConstructor.apply(driverWrapper.getDriver());
    }
}
