package org.labkey.test.tests.announcements;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.pages.core.admin.ProjectSettingsPage;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({InDevelopment.class})
public class DiscussionLinkTest extends BaseWebDriverTest
{

    public static final String WIKI_NAME = "Link test";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        DiscussionLinkTest init = (DiscussionLinkTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
    }

    @Before
    public void preTest() throws Exception
    {
        goToProjectHome();
    }

    @Test
    public void testDiscussionLink() throws Exception
    {
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Wiki");
        //Create wiki using WikiHelper
        WikiHelper wikiHelper = new WikiHelper(this);
        wikiHelper.createNewWikiPage();
        setFormElement(Locator.name("name"), WIKI_NAME);
        wikiHelper.saveWikiPage();
        //Confirm link present using AssertElementPresent by text
        click(Locator.linkContainingText(WIKI_NAME));
        assertElementPresent(Locator.linkContainingText("discussion"));
        //goto l and feel
        ProjectSettingsPage projectSettingsPage = goToProjectSettings();
        //confirm Enable discussion enabled checked
        org.labkey.test.components.html.Checkbox enableDiscussionCheckbox = projectSettingsPage.getEnableDiscussionCheckbox();
        assertEquals("Enable Discussion should be checked.",true, enableDiscussionCheckbox.isChecked());

        //un-check Enabled
        enableDiscussionCheckbox.uncheck();
        projectSettingsPage.save();

        //Confirm Discussion link is not present
        goToProjectHome();
        click(Locator.linkContainingText(WIKI_NAME));
        assertElementNotPresent(Locator.linkContainingText("discussion"));

    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "DiscussionLinkTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("announcement");
    }
}