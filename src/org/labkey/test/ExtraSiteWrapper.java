/*
 * Copyright (c) 2015-2017 LabKey Corporation
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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.service.DriverService;

import java.io.File;

public class ExtraSiteWrapper extends LabKeySiteWrapper implements AutoCloseable
{
    private final Pair<WebDriver, DriverService> extraDriver;

    public ExtraSiteWrapper(BrowserType browserType, File downloadDir)
    {
        super();
        this.extraDriver = createNewWebDriver(browserType, downloadDir);
    }

    public ExtraSiteWrapper(WebDriver driver)
    {
        super();
        this.extraDriver = new ImmutablePair<>(driver, null);
    }

    @Override
    public void close()
    {
        getDriver().quit();
        if (extraDriver.getRight() != null)
        {
            extraDriver.getRight().stop();
        }
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return extraDriver.getLeft();
    }

    @Override
    public void pauseJsErrorChecker(){}
    @Override
    public void resumeJsErrorChecker(){}
}

