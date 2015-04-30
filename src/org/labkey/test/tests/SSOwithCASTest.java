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

import org.apache.http.HttpException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestCredentials;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.credentials.Login;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.labkey.test.WebTestHelper.getHttpGetResponse;

@Category({DailyA.class})
public class SSOwithCASTest extends BaseWebDriverTest
{
    private static final File HEADER_LOGO_FILE = TestFileUtils.getSampleData("SSO/CAS/cas_small.png");
    private static final File LOGIN_LOGO_FILE = TestFileUtils.getSampleData("SSO/CAS/cas_big.png");
    private static final String credentialKey = "CAS";
    private static final String CAS_HOST = TestCredentials.getHost(credentialKey);
    private static final Login EXISTING_USER_LOGIN = TestCredentials.getLogins(credentialKey).get(0);
    private static final Login NEW_USER_LOGIN = TestCredentials.getLogins(credentialKey).get(1);

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        deleteUsersIfPresent(EXISTING_USER_LOGIN.getEmail(), NEW_USER_LOGIN.getEmail());
    }

    @BeforeClass
    public static void setupProject()
    {
        SSOwithCASTest init = (SSOwithCASTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        configureCASServer();
        createUser(EXISTING_USER_LOGIN.getEmail(), null);
    }

    @Before
    public void preTest()
    {
        enableCAS();
    }

    @Test
    public void testNewUserSSO()
    {
        signOut();

        //Click on CAS link
        clickAndWait(Locator.linkWithHref("/labkey/login/ssoRedirect.view?provider=CAS"));

        casLogin(NEW_USER_LOGIN);

        assertTrue("Not on customize user page after new user CAS login", getDriver().getCurrentUrl().contains("/user/showUpdate.view"));
        assertEquals("Wrong email for new user.", NEW_USER_LOGIN.getEmail(), getText(Locator.css(".labkey-nav-page-header")));
        String displayName = getFormElement(Locator.name("quf_DisplayName"));
        assertEquals("Wrong display name for new user.", displayNameFromEmail(NEW_USER_LOGIN.getEmail()), displayName);
    }

    @Test
    public void testCASIcons()
    {
        //set logos
        beginAt("login/pickAuthLogo.view?provider=CAS");

        setFormElement(Locator.name("auth_header_logo_file"), HEADER_LOGO_FILE );
        setFormElement(Locator.name("auth_login_page_logo_file"), LOGIN_LOGO_FILE);
        clickButton("Save");

        //sign out
        signOut();

        //check for image on the header
        String imageHeader = getAttribute(Locator.imageWithAltText("Sign in using CAS", false), "src");
        assertNotEquals("CAS image not found in the header", imageHeader, null);

        //check for image on the SignIn page
        clickAndWait(Locators.signInButtonOrLink);//Go to Labkey Sign-in page
        String imageLogin = getAttribute(Locator.imageWithAltText("Sign in using CAS", false), "src");
        assertNotEquals("CAS image not found in the header", imageHeader, null);

        signIn();

        //Delete logos
        beginAt("login/pickAuthLogo.view?provider=CAS");
        click(Locator.linkWithText("delete"));
        click(Locator.linkWithText("delete"));

        //Save
        clickButton("Save");

        //TODO: Verify logo deletion
    }

    @Test
    public void testSSOWithCASFromLoginPage()
    {
        testCAS(true);
    }

    @Test
    public void testSSOwithCASFromHeaderLink()
    {
        testCAS(false);
    }

    @Test
    public void testBogusTicket()
    {
        signOut();

        clickAndWait(Locators.signInButtonOrLink);//Go to Labkey Sign-in page
        String relativeURLSignInPage = getCurrentRelativeURL();

        beginAt("/cas/validate.view?&ticket=randomstringforbogusticket");

        assertTextPresent("Invalid ticket");
    }

    @Test
    public void testCASUnreachable()
    {
        //Configure CAS with "wrong" url
        beginAt("cas/configure.view?");
        setFormElement(Locator.name("serverUrl"), "www.labkey.org/cas"); //adding "/cas" in the end otherwise it doesn't allow me to save. Also, cannot save empty strings if configured previously.
        clickButton("Save");

        signOut();

        String beforeClickingOnCASLink = getCurrentRelativeURL();

        //Click on CAS link - nothing happens and user stays on the same page.
        click(Locator.linkWithHref("/labkey/login/ssoRedirect.view?provider=CAS"));

        String afterClickingOnCASLink = getCurrentRelativeURL();

        assertEquals("CAS server is configured properly. This test is to check mis-configured server address", beforeClickingOnCASLink, afterClickingOnCASLink);

        signIn();

        configureCASServer();//configure CAS correctly for the other tests to run.
    }

    private void testCAS(boolean loginPage)
    {
        signOut();

        clickFolder("support");
        String relativeURLBeforeSignIn = getCurrentRelativeURL();

        if(loginPage)
            clickAndWait(Locators.signInButtonOrLink);//Go to Labkey Sign-in page

        //Click on CAS link
        clickAndWait(Locator.linkWithHref("/labkey/login/ssoRedirect.view?provider=CAS"));

        //CAS login page - Sign in using CAS
        casLogin(EXISTING_USER_LOGIN);

        if (getDriver().getCurrentUrl().contains("/user/showUpdate.view")) // Redirects to customize user on first login
            clickButton("Submit");

        //Should be re-directed the page user was previously on
        String relativeURLAfterSignIn = getCurrentRelativeURL();
        assertEquals("After successful SSO with CAS, user should be redirected to the same URL they were on before Sign In",
                relativeURLBeforeSignIn, relativeURLAfterSignIn);

        //User should be CAS user
        assertEquals("User should be signed in with CAS userId", displayNameFromEmail(EXISTING_USER_LOGIN.getEmail()), getDisplayName());

        //Sign out CAS user, should sign out from Labkey, but should remained Sign In into CAS.
        signOut();

        String relativeURLBeforeSignIn2 = getCurrentRelativeURL();

        if(loginPage)
            clickAndWait(Locators.signInButtonOrLink);//Go to Labkey Sign-in page

        //Click on CAS link
        clickAndWait(Locator.linkWithHref("/labkey/login/ssoRedirect.view?provider=CAS"));

        String relativeURLAfterSignIn2 = getCurrentRelativeURL();

        assertEquals("User should be redirected to the same URL they were previously on after Signing In via CAS link",
                relativeURLBeforeSignIn2, relativeURLAfterSignIn2);

        //User should be CAS user
        assertEquals("User should be still signed in with CAS userId", displayNameFromEmail(EXISTING_USER_LOGIN.getEmail()), getDisplayName());
    }

    @LogMethod(quiet = true)
    private void enableCAS()
    {
        try
        {
            assertEquals("Failed to enable SSO with CAS", 200, getHttpGetResponse(WebTestHelper.getBaseURL() + "/login/enable.view?provider=CAS", PasswordUtil.getUsername(), PasswordUtil.getPassword()));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to enable SSO with CAS", e);
        }
    }

    @LogMethod(quiet = true)
    private void disableCAS()
    {
        try
        {
            assertEquals("Failed to disable SSO with CAS", 200, getHttpGetResponse(WebTestHelper.getBaseURL() + "/login/disable.view?provider=CAS", PasswordUtil.getUsername(), PasswordUtil.getPassword()));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to disable SSO with CAS", e);
        }
    }

    private void configureCASServer()
    {
        beginAt("cas/configure.view?");
        setFormElement(Locator.name("serverUrl"), CAS_HOST);
        clickButton("Save");
    }

    private void casLogin(Login login)
    {
        setFormElement(Locator.input("username"), login.getUsername());
        setFormElement(Locator.input("password"), login.getPassword());
        clickAndWait(Locator.input("submit"));
    }

    private void casLogout()
    {
        getDriver().navigate().to(CAS_HOST + "/logout");
    }

    @After
    public void postTest()
    {
        disableCAS();
        casLogout();
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("authentication");
    }
}