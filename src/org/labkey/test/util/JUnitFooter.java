/*
 * Copyright (c) 2011-2017 LabKey Corporation
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

import org.junit.Test;
import org.labkey.test.BaseWebDriverTest;

import java.util.List;

public class JUnitFooter extends BaseWebDriverTest
{
    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Test
    public void afterJUnit()
    {
        log("** This should follow JUnitTest.");
        log("** It will check for any errors or memory leaks caused by server-side tests");

        PipelineToolsHelper pipelineToolsHelper = new PipelineToolsHelper(this);
        pipelineToolsHelper.resetPipelineToolsDirectory();

        try{
            _containerHelper.deleteFolder("Shared", "_junit");
        }catch(Throwable e){/*ignore*/}

        waitForSystemMaintenanceCompletion();
    }

    @Override
    public void checkErrors()
    {
        // Skip. Need a way to ignore expected errors.
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
