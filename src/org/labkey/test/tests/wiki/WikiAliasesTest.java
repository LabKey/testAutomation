package org.labkey.test.tests.wiki;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Wiki;
import org.labkey.test.pages.wiki.ManageWikiConfigurationPage;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;
import org.labkey.test.util.search.SearchAdminAPIHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Daily.class, Wiki.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class WikiAliasesTest extends BaseWebDriverTest
{
    private static String wikiName = "Sample Wiki for testing aliases";
    private static String wikiName2 = "Sample Wiki for testing aliases - Wiki 2";
    private static String wikiTitle = "Title for " + wikiName;
    private static String wikiBody = "Wiki body for " + wikiName;

    private static String SUBFOLDER = "Subfolder for wiki";

    @BeforeClass
    public static void setupProject()
    {
        WikiAliasesTest init = (WikiAliasesTest) getCurrentTest();
        init.doSetup();
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModules(Arrays.asList("Wiki"));

        _containerHelper.createSubfolder(getProjectName(), SUBFOLDER);
        _containerHelper.enableModules(Arrays.asList("Wiki"));

        SearchAdminAPIHelper.pauseCrawler(getDriver());

        goToProjectHome();
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addBodyWebPart("Wiki");

        navigateToFolder(getProjectName(), SUBFOLDER);
        portalHelper.addBodyWebPart("Wiki");
    }

    @Test
    public void testSteps()
    {
        goToProjectHome();
        WikiHelper wikiHelper = new WikiHelper(this);
        String duplicateAlias = "Alias5";
        String aliases = "Alias1\n" + "Alias2\n" + "Alias3\n" + "Alias4\n" + duplicateAlias;
        wikiHelper.createWikiPage(wikiName, null, wikiTitle, wikiBody, true, null, false);

        log("Verifying adding new alias with rename button");
        ManageWikiConfigurationPage manageWikiConfigurationPage = wikiHelper.manageWikiConfiguration();
        manageWikiConfigurationPage.rename(duplicateAlias + " notAlias", true).save();
        Assert.assertEquals("Incorrect value for alias after rename operation with add alias checked", wikiName,
                ManageWikiConfigurationPage.beginAt(this, getProjectName(), Map.of("name", duplicateAlias + " notAlias")).getAliases());

        log("Verifying the add alias checkbox");
        manageWikiConfigurationPage = wikiHelper.manageWikiConfiguration();
        manageWikiConfigurationPage.rename(duplicateAlias, false).save();
        Assert.assertEquals("Incorrect value for alias after rename operation without add alias checked", wikiName,
                ManageWikiConfigurationPage.beginAt(this, getProjectName(), Map.of("name", duplicateAlias)).getAliases());

        log("Setting aliases in aliases textarea");
        wikiHelper.manageWikiConfiguration().setAliases(aliases).save();

        log("Verifying  all the aliases added works with wiki-page.view");
        manageWikiConfigurationPage = wikiHelper.manageWikiConfiguration();
        String listOfAliases[] = (manageWikiConfigurationPage.getAliases() + "\n" + manageWikiConfigurationPage.getAliases().toLowerCase()).split("\n");
        for (int i = 0; i < listOfAliases.length; i++)
        {
            beginAt(WebTestHelper.buildURL("wiki", getProjectName(), "page", Map.of("name", listOfAliases[i])));
            Assert.assertEquals("Incorrect wiki body for alias " + listOfAliases[i], wikiBody,
                    Locator.tagWithClass("div", "labkey-wiki").findElement(getDriver()).getText());
        }

        log("Verifying same alias name is not allowed");
        manageWikiConfigurationPage = wikiHelper.manageWikiConfiguration();
        String errMsg = manageWikiConfigurationPage.setAliases("\n" + duplicateAlias)
                .saveExpectingErrors();
        Assert.assertEquals("Incorrect error message", "Warning: Alias '" + duplicateAlias + "' already exists in this folder.", errMsg);

        log("Testing case insensitive");
        manageWikiConfigurationPage = wikiHelper.manageWikiConfiguration();
        errMsg = manageWikiConfigurationPage.setAliases("\n" + duplicateAlias.toLowerCase()).saveExpectingErrors();
        Assert.assertEquals("Incorrect error message", "Warning: Alias '" + duplicateAlias.toLowerCase() + "' already exists in this folder.", errMsg);

        log("Testing adding same wiki alias on different wiki fails within same folder");
        wikiHelper.createWikiPage(wikiName2, null, wikiTitle, wikiBody, true, null, false);
        manageWikiConfigurationPage = ManageWikiConfigurationPage.beginAt(this, getProjectName(), Map.of("name", wikiName2));
        Assert.assertEquals("This alias creation should throw error", "Warning: Alias '" + duplicateAlias + "' already exists in this folder.",
                manageWikiConfigurationPage.setAliases(duplicateAlias).saveExpectingErrors());

        log("Testing creating new wiki with name same as one of the aliases of different wiki");
        wikiHelper.createWikiPage(wikiName, null, "Title: New wiki with same alias", "Testing creating new wiki with name same as one of the aliases of different wiki", true, null, false);
        beginAt(WebTestHelper.buildURL("wiki", getProjectName(), "page", Map.of("name", wikiName)));
        Assert.assertEquals("Incorrect wiki body for wiki " + wikiName, "Testing creating new wiki with name same as one of the aliases of different wiki",
                Locator.tagWithClass("div", "labkey-wiki").findElement(getDriver()).getText());

        log("Creating same name alias in different folder");
        navigateToFolder(getProjectName(), SUBFOLDER);
        wikiHelper.createWikiPage(wikiName, null, wikiTitle, wikiBody, true, null, false);

        manageWikiConfigurationPage = wikiHelper.manageWikiConfiguration();
        manageWikiConfigurationPage.setAliases(duplicateAlias).save();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("wiki");
    }
}
