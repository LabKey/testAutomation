/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestProperties;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Base;
import org.labkey.test.categories.DRT;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Git;
import org.labkey.test.categories.Hosting;
import org.labkey.test.categories.Smoke;
import org.labkey.test.util.Order;

import java.util.List;

/**
 * Short test to verify installed modules are well-formed
 */
@Category({Base.class, DRT.class, Daily.class, Git.class, Hosting.class, Smoke.class})
@Order(-1)
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class BasicTest extends BaseWebDriverTest
{
    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Test
    public void testScripts()
    {
        Assume.assumeTrue(TestProperties.isDevModeEnabled());
        // Check for unrecognized scripts on the orphaned scripts page (only available in dev mode)
        beginAt(WebTestHelper.buildURL("admin-sql", "orphanedScripts"));
        assertTextNotPresent("WARNING:");
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
