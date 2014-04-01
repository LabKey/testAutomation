/*
 * Copyright (c) 2011-2013 LabKey Corporation
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
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Wiki;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.ResetTracker;
import org.labkey.test.util.UIContainerHelper;

import static org.junit.Assert.*;

@Category({DailyA.class, Wiki.class})
public class EmbeddedWebPartTest extends BaseWebDriverTest
{
    ResetTracker resetTracker = null;

    protected  final String PROJECT_NAME = TRICKY_CHARACTERS_FOR_PROJECT_NAMES + "Embedded web part test";

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    public EmbeddedWebPartTest()
    {
        setContainerHelper(new UIContainerHelper(this));
    }

    public void configure()
    {
        RReportHelper _rReportHelper = new RReportHelper(this);
        _rReportHelper.ensureRConfig();
        log("Setup project and list module");
        _containerHelper.createProject(getProjectName(), null);
        resetTracker = new ResetTracker(this);
    }

    @Test
    public void testSteps()
    {
        configure();
        embeddedQueryWebPartDoesNotRefreshOnChange();

        //configure for refresh monitoring
    }

    private void embeddedQueryWebPartDoesNotRefreshOnChange()
    {
        log("testing that embedded query web part does not refresh on change");

        //embed query part in wiki page
        addWebPart("Wiki");
        createNewWikiPage();
        click(Locator.linkContainingText("Source", 0));
        setFormElement(Locator.name("name"), TRICKY_CHARACTERS + "wiki page");

        setWikiBody(getFileContents("server/test/data/api/EmbeddedQueryWebPart.html"));

        clickButton("Save & Close");
        waitForText("Display Name");

        String rViewName = TRICKY_CHARACTERS + "new R view";
        _customizeViewsHelper.createRView(null, rViewName);

        waitForElement(Locator.xpath("//table[contains(@class, 'labkey-data-region')]"), WAIT_FOR_JAVASCRIPT);

        resetTracker.startTrackingRefresh();

        clickMenuButtonAndContinue("Views", rViewName);

        resetTracker.assertWasNotRefreshed();
    }


    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(PROJECT_NAME, false);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        fail("Not implemented");
        return null;
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
