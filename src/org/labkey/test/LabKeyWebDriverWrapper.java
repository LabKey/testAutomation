/*
 * Copyright (c) 2015 LabKey Corporation
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

