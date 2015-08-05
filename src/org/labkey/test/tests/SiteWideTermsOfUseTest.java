/*
 * Copyright (c) 2015 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.PasswordUtil;

@Category({DailyB.class})
public class SiteWideTermsOfUseTest extends BaseTermsOfUseTest
{
    protected static final String SITE_WIDE_TERMS_TEXT = "Site-wide terms of use text for the win";
    protected static final String NON_PUBLIC_NO_TERMS_PROJECT_NAME = "Non-public No Terms Project";


    @Override
    public void preTest()
    {
        assureSiteWideTermsOfUsePage();
        if(!isElementPresent(Locator.linkWithText(getProjectName())))
        {
            goToHome();
            acceptTermsOfUse(null, true);
        }
        clickProject(getProjectName());
    }


    @BeforeClass
    public static void setupProject()
    {
        SiteWideTermsOfUseTest init = (SiteWideTermsOfUseTest) getCurrentTest();

        init.doSetup();
    }

    @Override
    protected void doSetup()
    {
        super.doSetup();

        createUser(USER);

        createProjectWithTermsOfUse(PUBLIC_TERMS_PROJECT_NAME, "The first rule of fight club is do not talk about fight club.", true);
        createProjectWithTermsOfUse(NON_PUBLIC_TERMS_PROJECT_NAME, "The second rule of fight club is do not talk about fight club.", false);
        createProjectWithTermsOfUse(NON_PUBLIC_TERMS_PROJECT2_NAME, "The third rule of fight club is do not talk about fight club.", false);
        log("Create project" + NON_PUBLIC_NO_TERMS_PROJECT_NAME);
        _containerHelper.createProject(NON_PUBLIC_NO_TERMS_PROJECT_NAME, null);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteSiteWideTermsOfUsePage();
        log("Deleting test projects");

        deleteProject(PUBLIC_TERMS_PROJECT_NAME, false);
        deleteProject(NON_PUBLIC_TERMS_PROJECT_NAME, false);
        deleteProject(NON_PUBLIC_TERMS_PROJECT2_NAME, false);
        deleteProject(NON_PUBLIC_NO_TERMS_PROJECT_NAME, false);
        super.doCleanup(afterTest);
    }


    // test that the console link exists and directs to a page for adding terms when it doesn't exist
    @Test
    public void createTermsAdminConsoleLinkTest()
    {
        deleteSiteWideTermsOfUsePage();
        goToAdminConsole();
        Locator.XPathLocator link = Locator.linkContainingText("site-wide terms of use");
        assertElementPresent(link);
        clickAndWait(link);
        Locator.CssLocator button = Locator.css(".labkey-disabled-button");
        assertElementContains(button, "DELETE PAGE");
    }

    // test that the admin console link directs to a page for editing when page has already been created.
    @Test
    public void editTermsAdminConsoleLinkTest()
    {
        goToAdminConsole();
        clickAndWait(Locator.linkContainingText("site-wide terms of use"));
        assertElementContains(Locator.id("labkey-nav-trail-current-page"), "Edit");
    }

    protected void assureSiteWideTermsOfUsePage()
    {
        createTermsOfUsePage(null, SITE_WIDE_TERMS_TEXT);
    }

    // Test that the site-wide terms appear when you log out, even if you've accepted the terms when logged in
    @Test
    public void siteWideTermsOfUseLogOutTest()
    {
        signOut(SITE_WIDE_TERMS_TEXT);
    }

    // make sure you do not have to accept site-wide terms of use twice
    @Test
    public void acceptSiteWideTermsOfUseOnlyOnceTest()
    {
        acceptSiteWideTerms(false);
        goToProjectHome();
        assertTextNotPresent(SITE_WIDE_TERMS_TEXT);
    }

    // Do no accept site-wide terms.  Go to public project without terms.  Should show site-wide terms.
    @Test
    public void siteWideTermsAtProjectLevelTest()
    {
        doNotAcceptSiteWideTerms();
        goToProjectBegin(PUBLIC_NO_TERMS_PROJECT_NAME);
        assertTextPresent(SITE_WIDE_TERMS_TEXT);
    }


    // Do not accept site-wide terms.  Go to public project with terms.  Should show project terms.
    @Test
    public void showProjectTermsAtProjectLevelWithSiteTermsTest()
    {
        doNotAcceptSiteWideTerms();
        goToProjectBegin(PUBLIC_TERMS_PROJECT_NAME);
        assertTextPresent(PROJECT_TERMS_SNIPPET);
    }

    // Do not accept site-wide terms. Go to non-public project with terms.  Should show project terms.
    @Test
    public void nonPublicProjectWithSiteWideWithProjectTermsTest()
    {
        doNotAcceptSiteWideTerms();
        goToProjectBegin(NON_PUBLIC_TERMS_PROJECT_NAME);
        assertTextPresent(PROJECT_TERMS_SNIPPET);
    }

    // Do not accept site-wide terms. Go to non-public project without terms.  Should show side-wide terms.
    @Test
    public void nonPublicProjectSiteWideWithoutProjectTermsTest()
    {
        doNotAcceptSiteWideTerms();
        goToProjectBegin(NON_PUBLIC_NO_TERMS_PROJECT_NAME);
        assertTextPresent(SITE_WIDE_TERMS_TEXT);
    }

    // Accept site-wide terms.  Go to public project with terms. Should show project-level terms.
    @Test
    public void acceptedSiteWideWithProjectLevelTermsTest()
    {
        acceptSiteWideTerms(false);
        log("Going to project page");
        clickProject(PUBLIC_TERMS_PROJECT_NAME, false);
        assertTextPresent(PROJECT_TERMS_SNIPPET);
    }

    // Accept site-wide terms.  Go to public project without terms and should not show terms.
    @Test
    public void acceptedSiteWideWithoutProjectLevelTermsTest()
    {
        acceptSiteWideTerms(false);
        log("Going to project page");
        goToProjectBegin(PUBLIC_NO_TERMS_PROJECT_NAME);
        assertTextNotPresent(SITE_WIDE_TERMS_TEXT);
    }


    // Accept site-wide terms.  Go to non-public project with terms. Should show project-level terms on login page.
    @Test
    public void acceptedSiteWideWithNonPublicProjectLevelTermsTest()
    {
        acceptSiteWideTerms(false);
        goToProjectBegin(NON_PUBLIC_TERMS_PROJECT_NAME);
        assertTextPresent(PROJECT_TERMS_SNIPPET);
    }

    // Accept site-wide terms.  Go to non-public project without terms. Should show site-level terms.
    @Test
    public void acceptedSiteWideNonPublicNoProjectLevelTermsLoggedInTest()
    {
        acceptSiteWideTerms(false);
        log("Going to project page");
        goToProjectBegin(NON_PUBLIC_NO_TERMS_PROJECT_NAME);
        assertTextPresent(SITE_WIDE_TERMS_TEXT);
    }

    // Accept site-wide terms logging in.  Go to non-public project with terms and should show project-level terms.
    @Test
    public void acceptedSiteWideLoggedInWithProjectLevelTermsTest()
    {
        acceptSiteWideTerms(true);
        clickProject(NON_PUBLIC_TERMS_PROJECT_NAME, false);
        assertTextPresent(PROJECT_TERMS_SNIPPET);
        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);
        // now go home and make sure we don't see the site-wide terms again
        goToHome();
        assertTextNotPresent(SITE_WIDE_TERMS_TEXT);
        // go back to the project with terms and should not see terms there either
        clickProject(NON_PUBLIC_TERMS_PROJECT_NAME, false);
        assertTextNotPresent(PROJECT_TERMS_SNIPPET);
        // go to separate project with terms
        clickProject(PUBLIC_TERMS_PROJECT_NAME, false);
        assertTextPresent(PROJECT_TERMS_SNIPPET);
    }

    // Accept site-wide terms logging in.  Go to non-public project without terms and should not show terms.
    @Test
    public void acceptedSiteWideLoggedInWithoutProjectLevelTermsTest()
    {
        acceptSiteWideTerms(true);
        clickProject(NON_PUBLIC_NO_TERMS_PROJECT_NAME, false);
        assertTextNotPresent(PROJECT_TERMS_SNIPPET);
    }

    // Attempt to log in without accepting site-wide terms.  Should show terms of use again.
    @Test
    public void testFailedLoginNoAccept()
    {
        signOutWithSiteWideTerms(SITE_WIDE_TERMS_TEXT, true); // agrees to terms of use as guest
        signInShouldFail(PasswordUtil.getUsername(), PasswordUtil.getPassword(), "To use this site, you must check the box to approve the terms of use.");
        assertTextPresent(SITE_WIDE_TERMS_TEXT); // should show
    }

    // Attempt to log in with bad password.  Should show the terms of use again.
    @Test
    public void testFailedLoginBadPassword()
    {
        signOutWithSiteWideTerms(SITE_WIDE_TERMS_TEXT, true); // agrees to terms of use as guest
        signInShouldFail(PasswordUtil.getUsername(), "baaaaaaaad", "To use this site, you must check the box to approve the terms of use.");
        assertTextPresent(SITE_WIDE_TERMS_TEXT); // should show
    }

    protected void signOutWithSiteWideTerms(String termsText, boolean acceptTerms)
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


    protected void doNotAcceptSiteWideTerms()
    {
        signIn();
        log("Signing out without accepting site-wide terms");
        signOutWithSiteWideTerms(SITE_WIDE_TERMS_TEXT, false);  // should end on the page to accept terms of use.
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
            signOutWithSiteWideTerms(SITE_WIDE_TERMS_TEXT, true);
        }
    }

}
