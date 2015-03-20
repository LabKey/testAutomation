package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;

import java.util.Arrays;
import java.util.List;

@Category({InDevelopment.class})
public class TermsOfUseTest extends BaseWebDriverTest
{
    protected static final String PUBLIC_NO_TERMS_PROJECT_NAME = "Public No Terms Project";
    protected static final String PUBLIC_TERMS_PROJECT_NAME = "TermsOfUse Public Project";
    protected static final String NON_PUBLIC_NO_TERMS_PROJECT_NAME = "Non-public No Terms Project";
    protected static final String NON_PUBLIC_TERMS_PROJECT2_NAME = "TermsOfUse Non-Public Project 2";
    protected static final String NON_PUBLIC_TERMS_PROJECT_NAME = "TermsOfUse Non-Public Project";
    protected static final String USERS_GROUP = "Users";
    protected static final String USER2 = "termsofuse_user2@termsofuse.test";
    protected final PortalHelper _portalHelper = new PortalHelper(this);
    protected WikiHelper _wikiHelper = new WikiHelper(this);
    protected static final String WIKI_TERMS_TITLE = "Terms of Use";
    protected static final String PROJECT_TERMS_SNIPPET = "fight club";
    protected static final String TERMS_OF_USE_NAME = "_termsOfUse";
    protected static final String SITE_WIDE_TERMS_TEXT = "Site-wide terms of use text for the win";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsersIfPresent(USER2);
        log("Deleting test projects");

        deleteProject(PUBLIC_TERMS_PROJECT_NAME, false);
        deleteProject(NON_PUBLIC_TERMS_PROJECT_NAME, false);
        deleteProject(NON_PUBLIC_TERMS_PROJECT2_NAME, false);
        deleteProject(NON_PUBLIC_NO_TERMS_PROJECT_NAME, false);

        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        TermsOfUseTest init = (TermsOfUseTest) getCurrentTest();

        init.doSetup();
    }

    protected void doSetup()
    {

        log("Create initial project and create users");
        _containerHelper.createProject(PUBLIC_NO_TERMS_PROJECT_NAME, null);
        _securityHelper.setProjectPerm(USERS_GROUP, "Editor");
        _permissionsHelper.setSiteGroupPermissions("All Site Users", "Reader");
        _permissionsHelper.setSiteGroupPermissions("Guests", "Reader");
        goToHome();

        clickProject(PUBLIC_NO_TERMS_PROJECT_NAME);
        log("Create wiki tab");
        _portalHelper.addWebPart("Wiki");
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkCheckbox(Locator.checkboxByTitle("Wiki"));
        clickButton("Update Folder");

        log("Create user and permissions");
        _securityHelper.setProjectPerm(USERS_GROUP, "Editor");
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.clickManageGroup(USERS_GROUP);
        setFormElement(Locator.name("names"), USER2);
        uncheckCheckbox(Locator.checkboxByName("sendEmail"));
        clickButton("Update Group Membership");

        createProjectsWithTermsOfUse();
//        goToHome();
//        createSubfolder(NON_PUBLIC_TERMS_PROJECT_NAME, "subfolder", null);
//        pushLocation(); // For attempting to bypass Terms of Use

        log("Create project" + NON_PUBLIC_NO_TERMS_PROJECT_NAME);
        _containerHelper.createProject(NON_PUBLIC_NO_TERMS_PROJECT_NAME, null);

//        assureSiteWideTermsOfUsePage();
    }

    protected void createProjectWithTermsOfUse(String name, String termsText, boolean isPublic)
    {
        log("Create project " + name);
        _containerHelper.createProject(name, null);
        createTermsOfUsePage(name, termsText);
        pushLocation(); // For attempting to bypass Terms of Use
        _permissionsHelper.setSiteGroupPermissions("All Site Users", "Reader");
        if (isPublic)
        {
            _permissionsHelper.setSiteGroupPermissions("Guests", "Reader");
        }
    }

    protected void createProjectsWithTermsOfUse()
    {
        createProjectWithTermsOfUse(PUBLIC_TERMS_PROJECT_NAME, "The first rule of fight club is do not talk about fight club.", true);
        createProjectWithTermsOfUse(NON_PUBLIC_TERMS_PROJECT_NAME, "The second rule of fight club is do not talk about fight club.", false);
        createProjectWithTermsOfUse(NON_PUBLIC_TERMS_PROJECT2_NAME, "The third rule of fight club is do not talk about fight club.", false);
    }

    @Before
    public void preTest()
    {
        if(!isElementPresent(Locator.linkWithText(getProjectName())))
        {
            goToHome();
            acceptTermsOfUse(null, true);
        }
        clickProject(getProjectName());
    }

    protected void createTermsOfUsePage(String projectName, String body)
    {
        String message = null;
        if (null != projectName)
        {
            message = "Create terms of use page for project " + projectName;
            clickProject(projectName);
            goToModule("Wiki");
        }
        else // site-wide terms of use page
        {
            message = "Create site-wide terms of use page";
            beginAt("/wiki/page.view?name=_termsOfUse");
        }
        if (isElementPresent(Locator.linkContainingText("add a new page")))
        {
            log(message);
            _wikiHelper.createNewWikiPage("RADEOX");
            setFormElement(Locator.name("name"), TERMS_OF_USE_NAME);
            setFormElement(Locator.name("title"), WIKI_TERMS_TITLE);
            setFormElement(Locator.name("body"), body);
            _wikiHelper.saveWikiPage();
        }
    }

//    @Test
    public void projectTermsOfUseTest()
    {
        log("Terms don't come into play until you log out");
        clickProject(NON_PUBLIC_TERMS_PROJECT2_NAME);
        assertTextNotPresent("fight club");
        signOut();

        log("Access project with guest user");
        clickProject(PUBLIC_TERMS_PROJECT_NAME, false);
        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);

        goToHome();
        clickProject(PUBLIC_TERMS_PROJECT_NAME);
        assertTextNotPresent("fight club");

        signIn();
        log("Attempt to bypass terms with saved URLs");
        popLocation();
        assertTextPresent(PROJECT_TERMS_SNIPPET); // PROJECT_NAME
        popLocation();
        assertTextPresent(PROJECT_TERMS_SNIPPET); // PUBLIC_TERMS_PROJECT_NAME
        popLocation();
        assertTextPresent(PROJECT_TERMS_SNIPPET); // NON_PUBLIC_TERMS_PROJECT_NAME
        popLocation();
        assertTextPresent(PROJECT_TERMS_SNIPPET); // NON_PUBLIC_TERMS_PROJECT_NAME/subfolder

        goToHome();
        clickProject(NON_PUBLIC_TERMS_PROJECT2_NAME, false);
        assertTextPresent(PROJECT_TERMS_SNIPPET);
        log("Submit without agreeing");
        clickButton("Agree");
        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);

        clickProject(NON_PUBLIC_TERMS_PROJECT_NAME, false);
        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);

        log("Check terms with impersonated user");
        clickProject(NON_PUBLIC_TERMS_PROJECT2_NAME, false);
        impersonate(USER2);

        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);

        clickProject(PUBLIC_TERMS_PROJECT_NAME, false);
        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);

        clickProject(NON_PUBLIC_TERMS_PROJECT_NAME, false);
        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);

        stopImpersonating();
        clickProject(PUBLIC_TERMS_PROJECT_NAME, false);
        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);

        clickProject(NON_PUBLIC_TERMS_PROJECT2_NAME);
        clickTab("Wiki");
        clickAndWait(Locator.linkWithText("Edit"));
        deleteWikiPage();
        assertTextNotPresent(WIKI_TERMS_TITLE);
    }

    // test that the console link exists and directs to a page for adding terms when it doesn't exist
//    @Test
    public void createTermsAdminConsoleLinkTest()
    {
        // TODO check Locator.id("labkey-nav-trail-current-page") perhaps<span class="labkey-nav-page-header" id="labkey-nav-trail-current-page" style="visibility: visible;">Edit</span>
        // or may be better to check if the delete page button is disabled
        //<a class="labkey-disabled-button" onclick="if (this.className.indexOf('labkey-disabled-button') != -1){ return false; }this.form = document.getElementById('1e159d96-b093-1032-a938-01a67b296794').form; if (isTrueOrUndefined(function(){return false;}.call(this))) {submitForm(document.getElementById('1e159d96-b093-1032-a938-01a67b296794').form); return false;}" id="wiki-input-button-delete"><span>Delete Page</span></a>
    }

    // test that the admin console link directs to a page for editing when page has already been created.
//    @Test
    public void editTermsAdminConsoleLinkTest()
    {
        // TODO
    }

    protected void assureSiteWideTermsOfUsePage()
    {
        createTermsOfUsePage(null, SITE_WIDE_TERMS_TEXT);
    }

    // Test that the site-wide terms appear when you log out, even if you've accepted the terms when logged in
    @Test
    public void siteWideTermsOfUseLogOutTest()
    {
        assureSiteWideTermsOfUsePage();
        signOut(SITE_WIDE_TERMS_TEXT);
    }

    // make sure you do not have to accept site-wide terms of use twice
    @Test
    public void acceptSiteWideTermsOfUseOnlyOnceTest()
    {
        assureSiteWideTermsOfUsePage();
        acceptSiteWideTerms(false);
        goToProjectHome();
        assertTextNotPresent(SITE_WIDE_TERMS_TEXT);
    }

    // Do no accept site-wide terms.  Go to public project without terms.  Should show site-wide terms.
    @Test
    public void siteWideTermsAtProjectLevelTest()
    {
        assureSiteWideTermsOfUsePage();
        doNotAcceptSiteWideTerms();
        goToProjectBegin(PUBLIC_NO_TERMS_PROJECT_NAME);
        assertTextPresent(SITE_WIDE_TERMS_TEXT);
    }


    // Do not accept site-wide terms.  Go to public project with terms.  Should show project terms.
    @Test
    public void showProjectTermsAtProjectLevelWithSiteTermsTest()
    {
        assureSiteWideTermsOfUsePage();
        doNotAcceptSiteWideTerms();
        goToProjectBegin(PUBLIC_TERMS_PROJECT_NAME);
        assertTextPresent(PROJECT_TERMS_SNIPPET);
    }

    // Do not accept site-wide terms. Go to non-public project with terms.  Should show project terms.
    @Test
    public void nonPublicProjectWithSiteWideWithProjectTermsTest()
    {
        assureSiteWideTermsOfUsePage();
        doNotAcceptSiteWideTerms();
        goToProjectBegin(NON_PUBLIC_TERMS_PROJECT_NAME);
        assertTextPresent(PROJECT_TERMS_SNIPPET);
    }

    // Do not accept site-wide terms. Go to non-public project without terms.  Should show side-wide terms.
    @Test
    public void nonPublicProjectSiteWideWithoutProjectTermsTest()
    {
        assureSiteWideTermsOfUsePage();
        doNotAcceptSiteWideTerms();
        goToProjectBegin(NON_PUBLIC_NO_TERMS_PROJECT_NAME);
        assertTextPresent(SITE_WIDE_TERMS_TEXT);
    }

    // Accept site-wide terms.  Go to public project with terms. Should show project-level terms.
    @Test
    public void acceptedSiteWideWithProjectLevelTermsTest()
    {
        assureSiteWideTermsOfUsePage();
        acceptSiteWideTerms(false);
        log("Going to project page");
        clickProject(PUBLIC_TERMS_PROJECT_NAME, false);
        assertTextPresent(PROJECT_TERMS_SNIPPET);
    }

    // Accept site-wide terms.  Go to public project without terms and should not show terms.
    @Test
    public void acceptedSiteWideWithoutProjectLevelTermsTest()
    {
        assureSiteWideTermsOfUsePage();
        acceptSiteWideTerms(false);
        log("Going to project page");
        goToProjectBegin(PUBLIC_NO_TERMS_PROJECT_NAME);
        assertTextNotPresent(SITE_WIDE_TERMS_TEXT);
    }


    // Accept site-wide terms.  Go to non-public project with terms. Should show project-level terms on login page.
    @Test
    public void acceptedSiteWideWithNonPublicProjectLevelTermsTest()
    {
        assureSiteWideTermsOfUsePage();
        acceptSiteWideTerms(false);
        goToProjectBegin(NON_PUBLIC_TERMS_PROJECT_NAME);
        assertTextPresent(PROJECT_TERMS_SNIPPET);
    }

    // Accept site-wide terms.  Go to non-public project without terms. Should show site-level terms.
    @Test
    public void acceptedSiteWideNonPublicNoProjectLevelTermsLoggedInTest()
    {
        assureSiteWideTermsOfUsePage();
        acceptSiteWideTerms(false);
        log("Going to project page");
        goToProjectBegin(NON_PUBLIC_NO_TERMS_PROJECT_NAME);
        assertTextPresent(SITE_WIDE_TERMS_TEXT);
    }

    // Accept site-wide terms logging in.  Go to non-public project with terms and should show project-level terms.
    @Test
    public void acceptedSiteWideLoggedInWithProjectLevelTermsTest()
    {
        assureSiteWideTermsOfUsePage();
        acceptSiteWideTerms(true);
        clickProject(NON_PUBLIC_TERMS_PROJECT_NAME, false);
        assertTextPresent(PROJECT_TERMS_SNIPPET);
    }

    // Accept site-wide terms logging in.  Go to non-public project without terms and should not show terms.
    @Test
    public void acceptedSiteWideLoggedInWithoutProjectLevelTermsTest()
    {
        assureSiteWideTermsOfUsePage();
        acceptSiteWideTerms(true);
        clickProject(NON_PUBLIC_NO_TERMS_PROJECT_NAME, false);
        assertTextNotPresent(PROJECT_TERMS_SNIPPET);
    }

    // TODO failed login test.  Should show terms of use again.

    protected void deleteWikiPage()
    {
        waitForElementToDisappear(Locator.xpath("//a[contains(@class, 'disabled')]/span[text()='Delete Page']"), WAIT_FOR_JAVASCRIPT);
        clickButton("Delete Page");
        clickButton("Delete");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return PUBLIC_NO_TERMS_PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }

    protected void signOutWithTerms(String termsText, boolean acceptTerms)
    {

        log("Signing out");
        beginAt("/login/logout.view");

        if (acceptTerms)
        {
            acceptTermsOfUse(termsText, true);
        }
        else
        {
            assertTextPresent(termsText);
        }
    }

    protected void goToProjectBegin(String projectName)
    {
        beginAt("project/" + projectName + "/begin.view?");
    }

    protected void doNotAcceptSiteWideTerms()
    {
        signIn();
        log("Signing out without accepting site-wide terms");
        signOutWithTerms(SITE_WIDE_TERMS_TEXT, false);  // should end on the page to accept terms of use.
    }

    protected void acceptSiteWideTerms(boolean login)
    {

        if (login)
        {
            log("Accepting site-wide terms of use for logged in user");
            signOut();
            signIn();
        }
        else
        {
            log("Accepting site-wide terms of use for guest user");
            signOutWithTerms(SITE_WIDE_TERMS_TEXT, true);
        }
    }
}

