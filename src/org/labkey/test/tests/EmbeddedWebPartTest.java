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
package org.labkey.test.tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Wiki;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.UIContainerHelper;
import org.labkey.test.util.WikiHelper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.List;

@Category({DailyA.class, Wiki.class})
public class EmbeddedWebPartTest extends BaseWebDriverTest
{
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
        PortalHelper portalHelper = new PortalHelper(this);
        WikiHelper wikiHelper = new WikiHelper(this);
        log("testing that embedded query web part does not refresh on change");

        //embed query part in wiki page
        portalHelper.addWebPart("Wiki");
        wikiHelper.createNewWikiPage();
        click(Locator.linkContainingText("Source"));
        setFormElement(Locator.name("name"), TRICKY_CHARACTERS + "wiki page");

        wikiHelper.setWikiBody(TestFileUtils.getFileContents("server/test/data/api/EmbeddedQueryWebPart.html"));

        clickButton("Save & Close");
        waitForText("Display Name");
        pushLocation();

        String rReportName = TRICKY_CHARACTERS + "new R report";
        new RReportHelper(this).createRReport(rReportName);
        popLocation();

        WebElement el = Locator.css(":root").findElement(getDriver());

        DataRegionTable.DataRegion(getDriver()).find().goToReport(false, rReportName);

        try
        {
            el.isDisplayed();
        }
        catch (NoSuchElementException fail)
        {
            Assert.fail("Query Web Part triggered an unexpected page load");
        }
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
