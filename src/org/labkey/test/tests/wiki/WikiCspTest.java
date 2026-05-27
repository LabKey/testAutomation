/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.test.tests.wiki;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locators;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Wiki;
import org.labkey.test.pages.core.admin.SiteValidationPage;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;
import org.labkey.test.util.CspLogUtil;
import org.labkey.test.util.TextSearcher;
import org.labkey.test.util.WikiHelper;
import org.labkey.test.util.core.admin.CspConfigHelper;

import java.util.Arrays;
import java.util.List;

@Category({Daily.class, Wiki.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class WikiCspTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private static final String WIKI_PAGE_TITLE = "TOC_with_inline";
    private static final String WIKI_PAGE_BODY =
        // Issue 52483: HTML substitution patterns can throw errors during wiki validation
        "${labkey.webPart(partName='Query', schemaName='core', queryName='Users')}\n" +
        // Trigger wiki validation warning
            "<div onclick=\"alert('bad page')\">Click me</div>";

    @BeforeClass
    public static void setupProject()
    {
        WikiCspTest init = getCurrentTest();
        init.doSetup();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return List.of("Wiki");
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return PROJECT_NAME;
    }

    private void doSetup()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        _containerHelper.enableModules(Arrays.asList("Wiki"));
        goToProjectHome();
        CspConfigHelper.debugCspWarnings();  // Ensure that CSP violation logs aren't suppressed by de-duping efforts
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        if (afterTest)
        {
            CspConfigHelper.infoCspWarnings();
        }
    }

    @Override
    protected void checkLinks()
    {
        // No-op to avoid triggering the CSP violation during the crawl
    }

    @Test
    public void testCspChecks()
    {
        goToProjectHome(PROJECT_NAME);

        // Issue 51749 - Create a CSP problem and a Wiki table of contents that caused a problem when checked in the background
        WikiHelper wikiHelper = new WikiHelper(this);
        wikiHelper.createNewWikiPage("HTML");
        wikiHelper.setWikiName(WIKI_PAGE_TITLE);
        wikiHelper.setWikiTitle(WIKI_PAGE_TITLE);
        wikiHelper.setWikiBody(WIKI_PAGE_BODY);
        wikiHelper.saveWikiPage();

        waitForText("Click me");

        waitFor(() ->
        {
            try
            {
                CspLogUtil.checkNewCspWarnings(getArtifactCollector());
                return false;
            }
            catch (CspLogUtil.CspWarningDetectedException ignore)
            {
                return true;
            }
        }, "Should have triggered a CSP error", WAIT_FOR_PAGE);

        SiteValidationPage validationPage = goToAdminConsole().clickSiteValidation();
        validationPage.setAllValidators(false);
        validationPage.setWikiValidator(true);

        PipelineStatusDetailsPage jobPage = validationPage.clickValidateInBackground();

        jobPage.waitForComplete(60_000)
                .assertLogTextContains("Site validation complete");
        jobPage.clickDataLink();

        assertNoLabKeyErrors();
        TextSearcher textSearcher = new TextSearcher(getText(Locators.bodyPanel()));
        assertTextPresent(textSearcher,
                "Wiki Validator");

        assertTextNotPresent(textSearcher, "Error");

        // Issue 51749 - check for expected CSP problem
        assertTextPresent(textSearcher, WIKI_PAGE_TITLE + " (" + WIKI_PAGE_TITLE + "): onclick");
    }
}
