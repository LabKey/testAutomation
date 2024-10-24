/*
 * Copyright (c) 2020 LabKey Corporation
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

package org.labkey.test.tests.starterartifacts;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.pages.starterartifacts.BeginPage;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

@Category({InDevelopment.class})
public class StarterArtifactsTest extends BaseWebDriverTest
{
    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        StarterArtifactsTest init = (StarterArtifactsTest)getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testStarterArtifactsModule()
    {
        _containerHelper.enableModule("StarterArtifacts");
        BeginPage beginPage = BeginPage.beginAt(this, getProjectName());
        assertEquals(200, getResponseCode());
        final String expectedHello = "Hello, and welcome to the StarterArtifacts module.";
        assertEquals("Wrong hello message", expectedHello, beginPage.getHelloMessage());
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "StarterArtifactsTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("StarterArtifacts");
    }
}