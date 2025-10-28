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

import java.util.Arrays;
import java.util.List;

@Category({Daily.class, Wiki.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class WikiCspTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = TRICKY_CHARACTERS_FOR_PROJECT_NAMES + "WikiCspTest";
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

        try
        {
            CspLogUtil.checkNewCspWarnings(getArtifactCollector());
        }
        catch (CspLogUtil.CspWarningDetectedException ignore) {}

        goToAdminConsole().goToSettingsSection();

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
