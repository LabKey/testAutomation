package org.labkey.test.tests.announcements;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.List;

@Category({InDevelopment.class})
public class AnnouncementsPermissionTest extends BaseWebDriverTest
{
    PortalHelper portalHelper = new PortalHelper(this);
    public static final String NOT_CONTRIBUTOR_ONLY_TITLE = "Not-Contributor-only title";
    public static final String NOT_CONTRIBUTOR_ONLY_MESSAGE = "Not-Contributor-only message";
    private static final String CONTRIBUTOR = "contributor@messages.test";
    private static final String MSG5_TITLE = "test message 5";
    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        AnnouncementsPermissionTest init = (AnnouncementsPermissionTest) getCurrentTest();

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
    public void doTestMessageContributorRole(){
        clickProject(getProjectName());
        createUserWithPermissions(CONTRIBUTOR, getProjectName(), "Message Board Contributor");
        clickButton("Save and Finish");

        //As other role add a message
        clickProject(getProjectName());
        portalHelper.addWebPart("Messages");
        portalHelper.clickWebpartMenuItem("Messages", true, "New");
        setFormElement(Locator.name("title"), NOT_CONTRIBUTOR_ONLY_TITLE);
        setFormElement(Locator.id("body"), NOT_CONTRIBUTOR_ONLY_MESSAGE);
        clickButton("Submit", longWaitForPage);
        //Confirm message
        impersonate(CONTRIBUTOR);
        portalHelper.clickWebpartMenuItem("Messages", true, "New");
        setFormElement(Locator.name("title"), MSG5_TITLE);
        setFormElement(Locator.id("body"), "Contributor message");
        clickButton("Submit", longWaitForPage);
        assertTextPresent(MSG5_TITLE);
        clickAndWait(Locator.linkWithText("view message or respond"));
        assertElementPresent(Locator.linkWithSpan("Delete Message"));//Confirm here to legitimize not-present assert later.
        clickButton("Delete Message");
        clickButton("Delete");

        //confirm can read other user's message
        clickAndWait(Locator.linkWithText(NOT_CONTRIBUTOR_ONLY_TITLE));

        //confirm cannot delete other user's message
        assertElementNotPresent(Locator.linkWithSpan("Delete Message"));//Confirm here to legitimize not-present assert later.

        //confirm can respond to other user's message
        clickButton("Respond");
        String contributorResponse = "Contributor response";
        setFormElement(Locator.id("body"), contributorResponse);
        clickButton("Submit");
        assertTextPresent(contributorResponse);
    }


    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "AnnouncementsPermissionTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}