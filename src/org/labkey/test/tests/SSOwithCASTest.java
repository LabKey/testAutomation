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
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.labkey.test.WebTestHelper.getHttpGetResponse;

@Category({DailyA.class})
public class SSOwithCASTest extends BaseWebDriverTest
{
    private static final File HEADER_LOGO_FILE = TestFileUtils.getSampleData("SSO/CAS/cas_small.png");
    private static final File LOGIN_LOGO_FILE = TestFileUtils.getSampleData("SSO/CAS/cas_big.png");
    private static final String credentialKey = "CAS";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        deleteUsersIfPresent(TestCredentials.getUsername(credentialKey));
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
    }

    @Before
    public void preTest()
    {
        enableCAS();
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

        String relativeURLBeforeSignIn = getCurrentRelativeURL();

        if(loginPage)
            clickAndWait(Locators.signInButtonOrLink);//Go to Labkey Sign-in page

        //Click on CAS link
        clickAndWait(Locator.linkWithHref("/labkey/login/ssoRedirect.view?provider=CAS"));

        //CAS login page - Sign in using CAS
        casLogin();

        //Should be re-directed the page user was previously on
        String relativeURLAfterSignIn = getCurrentRelativeURL();
        assertEquals("After successful SSO with CAS, user should be redirected to the same URL they were on before Sign In",
                relativeURLBeforeSignIn, relativeURLAfterSignIn);

        //User should be CAS user
        assertEquals("User should be signed in with CAS userId", TestCredentials.getUsername(credentialKey), getDisplayName());

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
        assertEquals("User should be still signed in with CAS userId", TestCredentials.getUsername(credentialKey), getDisplayName());
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
        setFormElement(Locator.name("serverUrl"), TestCredentials.getHost(credentialKey));
        clickButton("Save");
    }

    private void casLogin()
    {
        setFormElement(Locator.input("username"), TestCredentials.getUsername(credentialKey));
        setFormElement(Locator.input("password"), TestCredentials.getPassword(credentialKey));
        clickAndWait(Locator.input("submit"));
    }

    private void casLogout()
    {
        try
        {
            String httpGetResponseBody = WebTestHelper.getHttpGetResponseBody(TestCredentials.getHost(credentialKey) + "/logout");
        }
        catch (HttpException | IOException e)
        {
            throw new RuntimeException(e) ;
        }
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