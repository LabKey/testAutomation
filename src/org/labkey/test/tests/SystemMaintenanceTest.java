/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
package org.labkey.test.tests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;

import java.util.List;

@Category({InDevelopment.class})
public class SystemMaintenanceTest extends BaseWebDriverTest
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

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {

    }

    @Test
    public void testSteps()
    {
        // Disable scheduled system maintenance
        setSystemMaintenance(false);

        // Manually start system maintenance... we'll check for completion below
        startSystemMaintenance();

        // Ensure that system maintenance is complete...
        waitForSystemMaintenanceCompletion();

        // Verify scheduled system maintenance is disabled.
        goToAdminConsole().clickRunningThreads();
        assertTextNotPresent("SystemMaintenance");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
