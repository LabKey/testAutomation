/*
 * Copyright (c) 2015-2017 LabKey Corporation
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

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.PasswordUtil;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@Category({DailyB.class})
public class SiteWideTermsOfUseTest extends BaseTermsOfUseTest
{
    protected static final String SITE_WIDE_TERMS_TEXT = "Site-wide terms of use text for the win";
    protected static final String NON_PUBLIC_NO_TERMS_PROJECT_NAME = "Non-public No Terms Project";

    @Before
    public void preTest()
    {
        assureSiteWideTermsOfUsePage();
        if(!isElementPresent(Locator.linkWithText(getProjectName())))
        {
            goToHome();
            acceptTermsOfUse(null, true);
        }
    }

    @Override
    protected void doSetup()
    {
        super.doSetup();

        log("Create project" + NON_PUBLIC_NO_TERMS_PROJECT_NAME);
        _containerHelper.createProject(NON_PUBLIC_NO_TERMS_PROJECT_NAME, null);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        _containerHelper.deleteProject(NON_PUBLIC_NO_TERMS_PROJECT_NAME, false);
        deleteSiteWideTermsOfUsePage();
    }


    // test that the console link exists and directs to a page for adding terms when it doesn't exist
    @Test
    public void createTermsAdminConsoleLinkTest()
    {
        deleteSiteWideTermsOfUsePage();
        goToAdminConsole().clickSiteWideTerms();
        WebElement button = Locator.css(".labkey-disabled-button").findElement(getDriver());
        assertEquals("Delete Page", button.getText());
        assertEquals("_termsOfUse", getFormElement(Locator.id("wiki-input-name").findElement(getDriver())));
    }

    // test that the admin console link directs to a page for editing when page has already been created.
    @Test
    public void editTermsAdminConsoleLinkTest()
    {
        goToAdminConsole().clickSiteWideTerms();
        assertEquals("Should be on wiki edit page for existing terms of use",
                "Edit",
                Locators.bodyTitle().findElement(getDriver()).getText());
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
        waitForText(SITE_WIDE_TERMS_TEXT);
    }


    // Do not accept site-wide terms.  Go to public project with terms.  Should show project terms.
    @Test
    public void showProjectTermsAtProjectLevelWithSiteTermsTest()
    {
        doNotAcceptSiteWideTerms();
        goToProjectBegin(PUBLIC_TERMS_PROJECT_NAME);
        waitForText(PROJECT_TERMS_SNIPPET);
    }

    // Do not accept site-wide terms. Go to non-public project with terms.  Should show project terms.
    @Test
    public void nonPublicProjectWithSiteWideWithProjectTermsTest()
    {
        doNotAcceptSiteWideTerms();
        goToProjectBegin(NON_PUBLIC_TERMS_PROJECT_NAME);
        waitForText(PROJECT_TERMS_SNIPPET);
    }

    // Do not accept site-wide terms. Go to non-public project without terms.  Should show side-wide terms.
    @Test
    public void nonPublicProjectSiteWideWithoutProjectTermsTest()
    {
        doNotAcceptSiteWideTerms();
        goToProjectBegin(NON_PUBLIC_NO_TERMS_PROJECT_NAME);
        waitForText(SITE_WIDE_TERMS_TEXT);
    }

    // Accept site-wide terms.  Go to public project with terms. Should show project-level terms.
    @Test
    public void acceptedSiteWideWithProjectLevelTermsTest()
    {
        acceptSiteWideTerms(false);
        log("Going to project page");
        clickProject(PUBLIC_TERMS_PROJECT_NAME, false);
        waitForText(PROJECT_TERMS_SNIPPET);
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
        waitForText(PROJECT_TERMS_SNIPPET);
    }

    // Accept site-wide terms.  Go to non-public project without terms. Should show site-level terms.
    @Test
    public void acceptedSiteWideNonPublicNoProjectLevelTermsLoggedInTest()
    {
        acceptSiteWideTerms(false);
        log("Going to project page");
        goToProjectBegin(NON_PUBLIC_NO_TERMS_PROJECT_NAME);
        waitForText(SITE_WIDE_TERMS_TEXT);
    }

    // Accept site-wide terms logging in.  Go to non-public project with terms and should show project-level terms.
    @Test
    public void acceptedSiteWideLoggedInWithProjectLevelTermsTest()
    {
        acceptSiteWideTerms(true);
        clickProject(NON_PUBLIC_TERMS_PROJECT_NAME, false);
        waitForText(PROJECT_TERMS_SNIPPET);
        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);
        // now go home and make sure we don't see the site-wide terms again
        goToHome();
        assertTextNotPresent(SITE_WIDE_TERMS_TEXT);
        // go back to the project with terms and should not see terms there either
        clickProject(NON_PUBLIC_TERMS_PROJECT_NAME, false);
        assertTextNotPresent(PROJECT_TERMS_SNIPPET);
        // go to separate project with terms
        clickProject(PUBLIC_TERMS_PROJECT_NAME, false);
        waitForText(PROJECT_TERMS_SNIPPET);
    }

    // Accept site-wide terms logging in.  Go to non-public project without terms and should not show terms.
    @Test
    public void acceptedSiteWideLoggedInWithoutProjectLevelTermsTest()
    {
        acceptSiteWideTerms(true);
        clickProject(NON_PUBLIC_NO_TERMS_PROJECT_NAME, false);
        assertTextNotPresent(PROJECT_TERMS_SNIPPET);
    }

    // Accept terms of use as a guest, then login. Shouldn't have to agree to terms again.
    @Test
    public void testAcceptTermsThenLogin()
    {
        signOutWithSiteWideTerms(SITE_WIDE_TERMS_TEXT, true); // agrees to terms of use as guest
        signIn(PasswordUtil.getUsername(), PasswordUtil.getPassword());
        String title = getDriver().getTitle();
        assertFalse("Unexpected title " + title, title.contains("Sign In") || title.contains("Terms of Use"));
        assertTextNotPresent("To use this site, you must check the box to approve the terms of use.", SITE_WIDE_TERMS_TEXT);
    }

    // Attempt to log in with bad password. Should show bad password error, not terms of use error.
    @Test
    public void testFailedLoginBadPassword()
    {
        signOutWithSiteWideTerms(SITE_WIDE_TERMS_TEXT, true); // agrees to terms of use as guest
        signInShouldFail(PasswordUtil.getUsername(), "baaaaaaaad", "The email address and password you entered did not match any accounts on file. Note: Passwords are case sensitive; make sure your Caps Lock is off.");
        waitForText(SITE_WIDE_TERMS_TEXT); // should show
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

    @Override
    public String getServerErrors()
    {
        String serverErrors = super.getServerErrors();
        if (serverErrors.contains("agreeToTerms.view?")) // Site terms not accepted. Let postamble do error checking
            return "";
        return serverErrors;
    }
}
