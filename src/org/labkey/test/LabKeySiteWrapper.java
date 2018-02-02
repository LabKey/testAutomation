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
package org.labkey.test;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.SideWebPart;
import org.labkey.test.components.api.ProjectMenu;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.AbstractUserHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SimpleHttpRequest;
import org.labkey.test.util.SimpleHttpResponse;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.TextSearcher;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.TestProperties.isDevModeEnabled;
import static org.labkey.test.WebTestHelper.buildURL;
import static org.labkey.test.WebTestHelper.getBaseURL;
import static org.labkey.test.WebTestHelper.getHttpClientBuilder;
import static org.labkey.test.WebTestHelper.getHttpResponse;
import static org.labkey.test.WebTestHelper.isLocalServer;
import static org.labkey.test.WebTestHelper.logToServer;

/**
 * TODO: Move non-JUnit related methods from BWDT.
 * Many existing helpers, components, and page classes will need refactor as well
 */
public abstract class LabKeySiteWrapper extends WebDriverWrapper
{
    private static final int MAX_SERVER_STARTUP_WAIT_SECONDS = 60;
    private static final String CLIENT_SIDE_ERROR = "Client exception detected";
    public AbstractUserHelper _userHelper = new APIUserHelper(this);

    public boolean isGuestModeTest()
    {
        return false;
    }

    // Just sign in & verify -- don't check for startup, upgrade, admin mode, etc.
    public void simpleSignIn()
    {
        if ( isGuestModeTest() )
        {
            goToHome();
            return;
        }

        if (!getDriver().getTitle().startsWith("Sign In"))
        {
            executeScript("window.onbeforeunload = null;"); // Just get logged in, ignore 'unload' alerts
            beginAt(WebTestHelper.buildURL("login", "login"));
            waitForAnyElement("Should be on login or Home portal", Locator.id("email"), SiteNavBar.Locators.userMenu);
        }

        if (PasswordUtil.getUsername().equals(getCurrentUser()))
        {
            log("Already logged in as " +  PasswordUtil.getUsername());
            goToHome();
        }
        else
        {
            log("Signing in as " + PasswordUtil.getUsername());
            assertElementPresent(Locator.tagWithName("form", "login"));
            setFormElement(Locator.name("email"), PasswordUtil.getUsername());
            setFormElement(Locator.name("password"), PasswordUtil.getPassword());
            acceptTermsOfUse(null, false);
            clickButton("Sign In", 0);

            // verify we're signed in now
            if (!waitFor(() ->
            {
                if (isElementPresent(SiteNavBar.Locators.userMenu))
                    return true;
                bypassSecondaryAuthentication();
                return false;
            }, defaultWaitForPage))
            {
                bypassSecondaryAuthentication();
                String errors = StringUtils.join(getTexts(Locator.css(".labkey-error").findElements(getDriver())), "\n");

                // If we get redirected here the message is not indicated as an error
                if (errors.length() == 0 && null != getUrlParam("message", true))
                    errors = getUrlParam("message", true);

                if (errors.contains("The email address and password you entered did not match any accounts on file."))
                    throw new IllegalStateException("Could not log in with the saved credentials.  Please verify that the test user exists on this installation or reset the credentials using 'gradlew :server:test:setPassword'");
                else if (errors.contains("Your password does not meet the complexity requirements; please choose a new password."))
                    throw new IllegalStateException("Password complexity requirement was left on by a previous test");
                else if (errors.contains("log in and approve the terms of use."))
                    throw new IllegalStateException("Terms of use not accepted at login");
                else
                    throw new IllegalStateException("Unexpected error(s) during login." + errors);
            }
        }

        assertSignedInNotImpersonating();

        _userHelper.saveCurrentDisplayName();
        WebTestHelper.saveSession(PasswordUtil.getUsername(), getDriver());
    }

    /**
     * Just hit the logout action to sign out or stop impersonating
     */
    public void simpleSignOut()
    {
        beginAt(buildURL("login", "logout"));
    }

    @LogMethod
    public void signOut(@Nullable String termsText)
    {
        log("Signing out");
        if (isImpersonating())
        {
            simpleSignOut(); // LogoutAction will stop impersonation and we can't always count on SiteNavBar being present when this is called
            acceptTermsOfUse(termsText, true);
        }
        simpleSignOut();
        acceptTermsOfUse(termsText, true);
        waitForElement(Locators.signInLink);
    }

    @LogMethod
    public void signOut()
    {
        signOut(null);
    }

    public void signOutHTTP()
    {
        String logOutUrl = WebTestHelper.buildURL("login", "logout");
        SimpleHttpRequest logOutRequest = new SimpleHttpRequest(logOutUrl, "POST");
        logOutRequest.copySession(getDriver());

        try
        {
            SimpleHttpResponse response = logOutRequest.getResponse();
            assertEquals(HttpStatus.SC_OK, response.getResponseCode());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @LogMethod
    public void ensureSignedInAsPrimaryTestUser()
    {
        if (!onLabKeyPage() || isOnServerErrorPage())
            goToHome();
        if (isImpersonating())
            simpleSignOut();
        if (!isSignedInAsPrimaryTestUser())
        {
            if (isSignedIn())
                signOut();
            simpleSignIn();
        }
        WebTestHelper.saveSession(PasswordUtil.getUsername(), getDriver());
    }

    @LogMethod
    public void deleteSiteWideTermsOfUsePage()
    {
        getHttpResponse(WebTestHelper.buildURL("wiki", "delete", Maps.of("name", "_termsOfUse")), "POST").getResponseCode();
    }

    protected void bypassSecondaryAuthentication()
    {
        try
        {
            //Select radio Yes
            checkRadioButton(Locator.radioButtonByNameAndValue("valid", "1"));

            //Click on button 'TestSecondary'
            clickAndWait(Locator.input("TestSecondary"));

            disableSecondaryAuthentication();
        }
        catch (NoSuchElementException ignored)
        {

        }
    }

    protected void acceptTermsOfUse(String termsText, boolean clickAgree)
    {
        WebElement termsCheckbox = Locators.termsOfUseCheckbox().findElementOrNull(getDriver());
        if (termsCheckbox != null)
        {
            if (termsCheckbox.isDisplayed())
            {
                checkCheckbox(termsCheckbox);
                if (null != termsText)
                {
                    assertTextPresent(termsText);
                }
                if (clickAgree)
                    clickButton("Agree");
            }
        }
    }

    @LogMethod
    public void signIn()
    {
        if (isGuestModeTest())
        {
            waitForStartup();
            log("Skipping sign in.  Test runs as guest.");
            simpleSignOut();
            return;
        }

        try
        {
            PasswordUtil.ensureCredentials();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to ensure credentials", e);
        }
        waitForStartup();
        log("Signing in");
        simpleSignOut();
        checkForUpgrade();
        simpleSignIn();
        ensureAdminMode();
    }

    // Just sign in & verify -- don't check for startup, upgrade, admin mode, etc.
    public void signIn(String email, String password)
    {
        attemptSignIn(email, password);
        waitForElementToDisappear(Locator.lkButton("Sign In"));
        Assert.assertEquals("Logged in as wrong user", email, getCurrentUser());
        WebTestHelper.saveSession(email, getDriver());
    }

    public void attemptSignIn(String email, String password)
    {
        if (isSignedIn())
            throw new IllegalStateException("You need to be logged out to log in. Please log out to log in.");

        if (!getDriver().getTitle().contains("Sign In"))
        {
            try
            {
                // attempt to navigate to login page
                clickAndWait(Locator.linkWithText("Sign In"));
            }
            catch (NoSuchElementException error)
            {
                throw new IllegalStateException("Unable to find \"Sign In\" link on current page.", error);
            }
        }

        assertTitleContains("Sign In");

        assertElementPresent(Locator.tagWithName("form", "login"));
        setFormElement(Locator.id("email"), email);
        setFormElement(Locator.id("password"), password);
        clickButton("Sign In", 0);
    }

    public void signInShouldFail(String email, String password, String... expectedMessages)
    {
        attemptSignIn(email, password);
        String errorText = waitForElement(Locator.id("errors").withText()).getText();
        assertElementPresent(Locator.tagWithName("form", "login"));

        List<String> missingErrors = getMissingTexts(new TextSearcher(errorText), expectedMessages);
        assertTrue(String.format("Wrong errors.\nExpected: ['%s']\nActual: '%s'", String.join("',\n'", expectedMessages), errorText), missingErrors.isEmpty());
    }

    protected void setInitialPassword(String user, String password)
    {
        // Get setPassword URL from notification email.
        beginAt("/dumbster/begin.view?");

        //the name of the installation can vary, so we need to infer the email subject
        WebElement link = null;
        String linkPrefix = user + " : Welcome to the ";
        String linkSuffix = "new user registration";
        for (WebElement el : getDriver().findElements(By.partialLinkText(linkPrefix)))
        {
            String text = el.getText();
            if (text.startsWith(linkPrefix) && text.endsWith(linkSuffix))
            {
                link = el;
                break;
            }
        }
        assertNotNull("Link for '" + user + "' not found", link);

        String emailSubject = link.getText();
        link.click();

        EmailRecordTable emailRecordTable = new EmailRecordTable(getDriver());
        WebElement resetLink = Locator.tagWithText("a", emailSubject).append("/..//a[contains(@href, 'setPassword.view')]").findElement(emailRecordTable);
        clickAndWait(resetLink, WAIT_FOR_PAGE);

        setFormElement(Locator.id("password"), password);
        setFormElement(Locator.id("password2"), password);

        clickButton("Set Password");
    }

    protected String getPasswordResetUrl(String username)
    {
        goToHome();
        goToModule("Dumbster");
        String emailSubject = "Reset Password Notification";

        EmailRecordTable emailRecordTable = new EmailRecordTable(getDriver());
        WebElement email = Locator.xpath("//td[text() = '" + username + "']/..//a[starts-with(text(), '" + emailSubject + "')]").findElement(emailRecordTable);
        email.click();

        WebElement resetLink = Locator.xpath("//td[text() = '" + username + "']/..//a[contains(@href, 'setPassword.view')]").findElement(emailRecordTable);
        shortWait().until(ExpectedConditions.elementToBeClickable(resetLink));
        return resetLink.getText();
    }

    protected void resetPassword(String resetUrl, String username, String newPassword)
    {
        if (PasswordUtil.getUsername().equals(username))
            throw new IllegalArgumentException("Don't change the primary site admin user's password");

        if (resetUrl != null)
            beginAt(resetUrl);

        assertTextPresent(username, "Your email address has been verified! Create an account password below.", "six non-whitespace characters or more, cannot match email address");

        setFormElement(Locator.id("password"), newPassword);
        setFormElement(Locator.id("password2"), newPassword);

        clickButton("Set Password");

        if (!isElementPresent(Locator.id("userMenuPopupLink")))
        {
            if (isButtonPresent("Submit"))
                    clickButtonContainingText("Submit", defaultWaitForPage*3);
            if (isButtonPresent("Done"))
                clickButton("Done");

            signOut();
            signIn(username, newPassword);
        }
    }

    @LogMethod protected void changePassword(String oldPassword, @LoggedParam String password)
    {
        if (PasswordUtil.getUsername().equals(getCurrentUser()))
            throw new IllegalArgumentException("Don't change the primary site admin user's password");

        goToMyAccount();
        clickButton("Change Password");

        setFormElement(Locator.id("oldPassword"), oldPassword);
        setFormElement(Locator.id("password"), password);
        setFormElement(Locator.id("password2"), password);

        clickButton("Set Password");
    }

    /**
     * change user's email from userEmail to newUserEmail from admin console
     */
    protected void changeUserEmail(String userEmail, String newUserEmail)
    {
        log("Attempting to change user email from " + userEmail + " to " + newUserEmail);

        goToSiteUsers();
        clickAndWait(Locator.linkContainingText(_userHelper.getDisplayNameForEmail(userEmail)));

        clickButton("Change Email");

        setFormElement(Locator.name("requestedEmail"), newUserEmail);
        setFormElement(Locator.name("requestedEmailConfirmation"), newUserEmail);
        clickButton("Submit");
    }


    protected void setSystemMaintenance(boolean enable)
    {
        // Not available in production mode
        if (isDevModeEnabled())
        {
            goToAdminConsole().clickSystemMaintenance();

            waitFor(()-> Locator.name("enableSystemMaintenance").findElementOrNull(getDriver()) != null, WAIT_FOR_JAVASCRIPT );
            if (enable)
                checkCheckbox(Locator.name("enableSystemMaintenance"));
            else
                uncheckCheckbox(Locator.name("enableSystemMaintenance"));

            clickButton("Save");
        }
    }

    public void ensureAdminMode()
    {
        if (!onLabKeyPage())
            goToHome();
        if (!isSignedIn())
            simpleSignIn();
        else if (!isUserSystemAdmin() && isImpersonating())
            simpleSignOut();
        Locator projectMenu = ProjectMenu.Locators.menuProjectNav;
        if (!isElementPresent(projectMenu))
        {
            goToHome();
            waitForElement(projectMenu, WAIT_FOR_PAGE);
        }
        assertTrue("Test user '" + getCurrentUser() + "' is not an admin", isUserAdmin());
    }

    public ShowAdminPage goToAdminConsole()
    {
        return ShowAdminPage.beginAt(this);
    }

    protected void createDefaultStudy()
    {
        clickButton("Create Study");
        clickButton("Create Study");
    }

    private void waitForStartup()
    {
        boolean hitFirstPage = false;
        log("Verifying that server has started...");
        long ms = System.currentTimeMillis();
        do
        {
            try
            {
                WebTestHelper.getHttpResponse(buildURL("login", "logout"));
                getDriver().manage().timeouts().pageLoadTimeout(WAIT_FOR_PAGE, TimeUnit.MILLISECONDS);
                getDriver().get(buildURL("login", "logout"));

                try
                {
                    waitForElement(Locator.CssLocator.union(Locator.css("table.labkey-main"), Locator.css("#permalink"), Locator.css("#headerpanel")));
                    hitFirstPage = true;
                }
                catch (NoSuchElementException notReady)
                {
                    long elapsedMs = System.currentTimeMillis() - ms;
                    log("Server is not ready.  Waiting " + (MAX_SERVER_STARTUP_WAIT_SECONDS -
                            (elapsedMs / 1000)) + " more seconds...");
                }
            }
            catch (WebDriverException e)
            {
                // ignore timeouts that occur during startup; a poorly timed request
                // as the webapp is loading may hang forever, causing a timeout.
                log("Waiting for server: " + e.getMessage());
            }
            catch (RuntimeException e)
            {
                if (e.getCause() != null && e.getCause() instanceof IOException)
                    log("Waiting for server: " + e.getCause().getMessage());
                else
                    throw e;
            }
            finally
            {
                if (!hitFirstPage)
                {
                    sleep(1000);
                }
            }

        } while (!hitFirstPage && ((System.currentTimeMillis() - ms)/1000) < MAX_SERVER_STARTUP_WAIT_SECONDS);
        if (!hitFirstPage)
        {
            throw new RuntimeException("Webapp failed to start up after " + MAX_SERVER_STARTUP_WAIT_SECONDS + " seconds.");
        }
        log("Server is running.");
        WebTestHelper.setUseContainerRelativeUrl((Boolean)executeScript("return LABKEY.experimental.containerRelativeURL;"));
    }

    @LogMethod
    private void checkForUpgrade()
    {
        boolean bootstrapped = false;

        // check to see if we're the first user:
        if (isTextPresent("Welcome! We see that this is your first time logging in."))
        {
            bootstrapped = true;
            assertTitleEquals("Account Setup");
            log("Need to bootstrap");
            verifyInitialUserRedirects();

            log("Testing bad email addresses");
            verifyInitialUserError(null, null, null, "Invalid email address:");
            verifyInitialUserError("bogus@bogus@bogus", null, null, "Invalid email address: bogus@bogus@bogus");

            log("Testing bad passwords");
            String email = PasswordUtil.getUsername();
            verifyInitialUserError(email, null, null, "You must enter a password.");
            verifyInitialUserError(email, "LongEnough", null, "You must enter a password.");
            verifyInitialUserError(email, null, "LongEnough", "You must enter a password.");
            verifyInitialUserError(email, "short", "short", "Your password must be six non-whitespace characters or more.");
            verifyInitialUserError(email, email, email, "Your password must not match your email address.");
            verifyInitialUserError(email, "LongEnough", "ButDontMatch", "Your password entries didn't match.");

            log("Register the first user");
            pushLocation();
            assertTextPresent("Retype Password");
            verifyInitialUserError(email, PasswordUtil.getPassword(), PasswordUtil.getPassword(), null);

            // Runner was unable to log the test start prior to initial user creation
            try{logToServer("=== Starting " + getClass().getSimpleName() + " ===");} catch (CommandException | IOException ignore){}

            log("Attempting to register another initial user");
            popLocation();
            // Make sure we got redirected to the module status page, since we already have a user
            assertTextNotPresent("Retype Password");
            assertTextPresent("Please wait, this page will automatically update with progress information");
            goToHome();

            WebTestHelper.saveSession(email, getDriver());
        }

        if (bootstrapped || "Sign In".equals(getDriver().getTitle()))
        {
            // if the logout page takes us to the sign-in page, then we may have a schema update to do:
            if ("Sign In".equals(getDriver().getTitle()))
                simpleSignIn();

            String upgradeText = "Please wait, this page will automatically update with progress information.";
            boolean performingUpgrade = isTextPresent(upgradeText);

            if (performingUpgrade)
            {
                try
                {
                    verifyRedirectBehavior(upgradeText);
                }
                catch (IOException fail)
                {
                    throw new RuntimeException(fail);
                }

                int waitMs = 10 * 60 * 1000; // we'll wait at most ten minutes
                long startTime = System.currentTimeMillis();
                long elapsed = 0;

                while (elapsed < waitMs && (!isButtonPresent("Next")))
                {
                    try
                    {
                        // Pound the server aggressively with requests for the home page to test synchronization
                        // in the sql script runner.
                        for (int i = 0; i < 5; i++)
                        {
                            int responseCode = WebTestHelper.getHttpResponse(buildURL("project", "Home", "begin")).getResponseCode();
                            TestLogger.log("Home: " + responseCode);
                            sleep(200);
                        }
                        sleep(2000);
                        if (isTextPresent("error occurred") || isTextPresent("failure occurred"))
                            throw new RuntimeException("A startup failure occurred.");
                    }
                    catch (WebDriverException ignore)
                    {
                        // Do nothing -- this page will sometimes auto-navigate out from under selenium
                    }
                    finally
                    {
                        elapsed = System.currentTimeMillis() - startTime;
                    }
                }

                if (elapsed > waitMs)
                    throw new TestTimeoutException("Script runner took more than 10 minutes to complete.");

                if (bootstrapped)
                {
                    // admin-moduleStatus
                    assertEquals("Progress bar text", "Module startup complete", getText(Locator.id("status-progress-bar")));
                    clickAndWait(Locator.lkButton("Next"));
                    // admin-newInstallSiteSettings
                    assertElementPresent(Locator.id("rootPath"));
                    clickAndWait(Locator.lkButton("Next"));
                    // admin-installComplete
                    clickAndWait(Locator.linkContainingText("Go to the server's Home page"));
                    assertEquals("Landed on wrong project after bootstrapping", "home", getCurrentProject().toLowerCase());
                }
                else
                {
                    WebElement header = Locator.css(".labkey-nav-page-header").findElementOrNull(getDriver());
                    if (header != null && Arrays.asList("Start Modules", "Upgrade Modules").contains(header.getText().trim()))
                    {
                        waitForElement(Locator.id("status-progress-bar").withText("Module startup complete"), WAIT_FOR_PAGE);
                        clickAndWait(Locator.lkButton("Next"));
                    }
                    else
                    {
                        goToHome();
                    }
                }

                checkErrors(); // Check for errors from bootstrap/upgrade
            }

            // Tests hit Home portal a lot. Make it load as fast as possible
            PortalHelper portalHelper = new PortalHelper(this);
            for (BodyWebPart webPart : portalHelper.getBodyWebParts())
                webPart.remove();
            for (SideWebPart webPart : portalHelper.getSideWebParts())
                webPart.remove();
            if (bootstrapped)
                _userHelper.setDisplayName(PasswordUtil.getUsername(), AbstractUserHelper.getDefaultDisplayName(PasswordUtil.getUsername()) + BaseWebDriverTest.INJECT_CHARS_1);
        }
    }

    private void verifyInitialUserError(@Nullable String email, @Nullable String password1, @Nullable String password2, @Nullable String expectedError)
    {
        if (null != email)
            setFormElement(Locator.id("email"), email);

        if (null != password1)
            setFormElement(Locator.id("password"), password1);

        if (null != password2)
            setFormElement(Locator.id("password2"), password2);

        clickAndWait(Locator.linkWithText("Next"), 90000); // Initial user creation blocks during upgrade script execution

        if (null != expectedError)
            assertEquals("Wrong error message.", expectedError, Locator.css(".labkey-error").findElement(getDriver()).getText());
    }

    private void verifyInitialUserRedirects()
    {
        String initialText = "Welcome! We see that this is your first time logging in.";

        // These requests should redirect to the initial user page
        beginAt("/login/resetPassword.view");
        assertTextPresent(initialText);
        beginAt("/admin/maintenance.view");
        assertTextPresent(initialText);
    }

    @LogMethod
    private void verifyRedirectBehavior(String upgradeText) throws IOException
    {
        // Do these checks via direct http requests the primary upgrade window seems to interfere with this test, #15853

        HttpResponse response = null;
        HttpUriRequest method;
        int status;

        try (CloseableHttpClient client = (CloseableHttpClient)WebTestHelper.getHttpClient())
        {
            // These requests should NOT redirect to the upgrade page

            method = new HttpGet(getBaseURL() + "/login/resetPassword.view");
            response = client.execute(method, WebTestHelper.getBasicHttpContext());
            status = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected response", HttpStatus.SC_OK, status);
            assertFalse("Upgrade text found", WebTestHelper.getHttpResponseBody(response).contains(upgradeText));
            EntityUtils.consume(response.getEntity());

            method = new HttpGet(getBaseURL() + "/admin/maintenance.view");
            response = client.execute(method, WebTestHelper.getBasicHttpContext());
            status = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected response", HttpStatus.SC_OK, status);
            assertFalse("Upgrade text found", WebTestHelper.getHttpResponseBody(response).contains(upgradeText));
            EntityUtils.consume(response.getEntity());


            // Check that sign out and sign in work properly during upgrade/install (once initial user is configured)

            DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy()
            {
                @Override
                public boolean isRedirected(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws ProtocolException
                {
                    boolean isRedirect = false;
                    try
                    {
                        isRedirect = super.isRedirected(httpRequest, httpResponse, httpContext);
                    }
                    catch (ProtocolException ignore)
                    {
                    }
                    if (!isRedirect)
                    {
                        int responseCode = httpResponse.getStatusLine().getStatusCode();
                        if (responseCode == 301 || responseCode == 302)
                            return true;
//                        if (WebTestHelper.getHttpResponseBody(httpResponse).contains("http-equiv=\"Refresh\""))
//                            return true;
                    }
                    return isRedirect;
                }

                //TODO: Generate HttpRequest for 'http-equiv' redirect
//                @Override
//                public HttpUriRequest getRedirect(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws ProtocolException
//                {
//                    HttpUriRequest redirectRequest = null;
//                    ProtocolException ex = null;
//                    try
//                    {
//                        return super.getRedirect(httpRequest, httpResponse, httpContext);
//                    }
//                    catch (ProtocolException e){ex = e;}
//                    redirectRequest = httpRequest.;
//
//                    if (redirectRequest == null)
//                        throw ex;
//                    else
//                        return redirectRequest;
//                }
            };
            try (CloseableHttpClient redirectClient = getHttpClientBuilder()
                    .setRedirectStrategy(redirectStrategy) /* Clear cookies so that we don't actually log out */
                    .setDefaultCookieStore(null).build())
            {
                method = new HttpPost(getBaseURL() + "/login/logout.view");
                List<NameValuePair> args = new ArrayList<>();
                args.add(new BasicNameValuePair("login", PasswordUtil.getUsername()));
                args.add(new BasicNameValuePair("password", PasswordUtil.getPassword()));
                ((HttpPost) method).setEntity(new UrlEncodedFormEntity(args));
                response = redirectClient.execute(method, WebTestHelper.getBasicHttpContext());
                status = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected response", HttpStatus.SC_OK, status);
                // TODO: check login, once http-equiv redirect is sorted out
                assertFalse("Upgrade text found", WebTestHelper.getHttpResponseBody(response).contains(upgradeText));
            }
        }
        finally
        {
            if (null != response)
                EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    public void checkErrors()
    {
        if (!isLocalServer())
            return;
        if (isGuestModeTest())
            return;

        ensureSignedInAsPrimaryTestUser();
        String serverErrors = getServerErrors();
        if (!serverErrors.isEmpty())
        {
            beginAt(buildURL("admin", "showErrorsSinceMark"));
            resetErrors();
            if (serverErrors.toLowerCase().contains(CLIENT_SIDE_ERROR.toLowerCase()))
                fail("There were client-side errors during the test run. Check labkey.log and/or labkey-errors.log for details.");
            else
                fail("There were server-side errors during the test run. Check labkey.log and/or labkey-errors.log for details.");
        }
        log("No new errors found.");
    }

    public String getServerErrors()
    {
        SimpleHttpResponse httpResponse = WebTestHelper.getHttpResponse(buildURL("admin", "showErrorsSinceMark"), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        assertEquals("Failed to fetch server errors: " + httpResponse.getResponseMessage(), HttpStatus.SC_OK, httpResponse.getResponseCode());
        return httpResponse.getResponseBody();
    }

    public void resetErrors()
    {
        if (isGuestModeTest() || !isLocalServer())
            return;

        SimpleHttpResponse httpResponse = WebTestHelper.getHttpResponse(buildURL("admin", "resetErrorMark"), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        assertEquals("Failed to reset server errors: [" + httpResponse.getResponseBody().split("\n")[0] + "].", HttpStatus.SC_OK, httpResponse.getResponseCode());
    }

    @LogMethod
    public void disableMaintenance()
    {
        if ( isGuestModeTest() )
            return;
        beginAt("/admin/customizeSite.view");
        click(Locator.radioButtonByNameAndValue("systemMaintenanceInterval", "never"));
        clickButton("Save");
    }

    private static long smStart = 0;
    private static String smUrl = null;

    public void startSystemMaintenance()
    {
        startSystemMaintenance("");
    }

    public void startSystemMaintenance(String taskName)
    {
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("test", "true");
        if (!taskName.isEmpty())
            urlParams.put("taskName", taskName);
        String maintenanceTriggerUrl = WebTestHelper.buildURL("admin", "systemMaintenance", urlParams);

        smStart = System.currentTimeMillis();
        SimpleHttpRequest request = new SimpleHttpRequest(maintenanceTriggerUrl);
        request.setRequestMethod("POST");
        request.copySession(getDriver());
        try
        {
            SimpleHttpResponse response = request.getResponse();
            assertEquals("Failed to start system maintenance", HttpStatus.SC_OK, response.getResponseCode());
            smUrl = response.getResponseBody();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void waitForSystemMaintenanceCompletion()
    {
        assertTrue("Must call startSystemMaintenance() before waiting for completion", smStart > 0);
        long elapsed = System.currentTimeMillis() - smStart;

        // Navigate to pipeline details page, then refresh page and check for system maintenance complete, up to 10 minutes from the start of the test
        beginAt(smUrl);
        int timeLeft = 10 * 60 * 1000 - ((Long)elapsed).intValue();
        waitForTextWithRefresh(timeLeft > 0 ? timeLeft : 0, "System maintenance complete");
    }

    public void goToProjectHome(String projectName)
    {
        beginAt(buildURL("project", projectName, "begin"));
    }

    /**
     * go to the project settings page of a project
     * @param project project name
     */
    public void goToProjectSettings(String project)
    {
        goToProjectHome(project);
        goToProjectSettings();
    }

    public void goToAdmin()
    {
        beginAt("/admin/showAdmin.view");
    }

    public void goToMyAccount()
    {
        clickUserMenuItem("My Account");
    }

    protected WebElement openMenu(String menuText)
    {
        WebElement menu = Locator.menuBarItem(menuText).findElement(getDriver());
        menu.click();
        return menu;
    }

    @LogMethod(quiet = true)
    public boolean disableMiniProfiler()
    {
        boolean restoreMiniProfiler = isMiniProfilerEnabled();
        setMiniProfilerEnabled(false);
        return restoreMiniProfiler;
    }

    @LogMethod(quiet = true)
    public boolean isMiniProfilerEnabled()
    {
        Connection cn = createDefaultConnection(false);
        Command command = new Command("mini-profiler", "enabled");
        try
        {
            CommandResponse r = command.execute(cn, null);
            Map<String, Object> response = r.getParsedData();
            if (response.containsKey("success") && (Boolean)response.get("success"))
            {
                Map<String, Object> data = (Map<String, Object>)response.get("data");
                return (Boolean)data.get("enabled");
            }
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to get mini-profiler enabled state", e);
        }

        return false;
    }

    @LogMethod
    public void setMiniProfilerEnabled(boolean enabled)
    {
        Connection cn = createDefaultConnection(false);
        PostCommand setEnabled = new PostCommand("mini-profiler", "enabled");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("enabled", enabled);
        setEnabled.setJsonObject(jsonObject);
        try
        {
            CommandResponse r = setEnabled.execute(cn, null);
            Map<String, Object> response = r.getParsedData();
            if (response.containsKey("success") && (Boolean)response.get("success"))
            {
                Map<String, Object> data = (Map<String, Object>)response.get("data");
                log("MiniProfiler state updated, enabled=" + data.get("enabled"));
            }
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to " + (enabled ? "enable" : "disable") + " mini-profiler", e);
        }
    }

    @LogMethod(quiet = true)
    public void enableEmailRecorder()
    {
        assertEquals("Failed to enable email recording", HttpStatus.SC_OK, getHttpResponse(WebTestHelper.buildURL("dumbster", "setRecordEmail", Maps.of("record", "true"))).getResponseCode());
    }

    @LogMethod(quiet = true)
    public void enableSecondaryAuthentication()
    {
        Connection cn = createDefaultConnection(true);
        Command command = new Command("login", "enable");
        command.setParameters(new HashMap<>(Maps.of("provider", "Test Secondary Authentication")));
        try
        {
            command.execute(cn, null);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to enable Secondary Authentication", e);
        }
    }

    @LogMethod(quiet = true)
    public void disableSecondaryAuthentication()
    {
        Connection cn = createDefaultConnection(true);
        Command command = new Command("login", "disable");
        command.setParameters(new HashMap<>(Maps.of("provider", "Test Secondary Authentication")));
        try
        {
            command.execute(cn, null);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to disable Secondary Authentication", e);
        }
    }

    @LogMethod(quiet = true)
    public void disableLoginAttemptLimit()
    {
        goToAdminConsole().goToAdminConsoleLinksSection();
        if (isElementPresent(Locator.linkWithText("Compliance Settings")))
        {
            log("Compliance module present, ensuring login attempt limit is off");
            clickAndWait(Locator.linkWithText("Compliance Settings"));
            clickAndWait(Locator.tagWithClass("div", "tab").childTag("a").withText("Login"));
            uncheckCheckbox(Locator.checkboxById("enableLogin"));
            click(Locator.linkWithSpan("Save"));
        }
    }

    public ProjectMenu projectMenu()
    {
        return new ProjectMenu(getDriver());
    }

    public SiteNavBar navBar()
    {
        return new SiteNavBar(getDriver());
    }

    /**
     * @deprecated Use {@link #projectMenu().open()}
     */
    @Deprecated
    public void openProjectMenu()
    {
        projectMenu().open();
    }

    public void clickProject(String project)
    {
        clickProject(project, true);
    }

    public void clickProject(String project, boolean assertDestination)
    {
        projectMenu().navigateToProject(project);
        if (assertDestination)
        {
            acceptTermsOfUse(null, true);
            assertEquals("In wrong project", project.toLowerCase(), getCurrentContainer().toLowerCase());
        }
    }

    public WebElement openFolderMenu()
    {
        return projectMenu().expandProjectFully(getCurrentProject());
    }

    /**
     * @deprecated Old UX only. Used by {@link #clickFolder(String)} below.
     */
    @Deprecated
    public void expandFolderTree(String folder)
    {
        Locator.XPathLocator folderNav = Locator.id("folderBar_menu").append("/div/div/div/ul").withClass("folder-nav-top");
        Locator.XPathLocator treeAncestor = folderNav.append("//li").withClass("collapse-folder").withDescendant(Locator.linkWithText(folder)).append("/span").withClass("marked");
        List<WebElement> els = treeAncestor.findElements(getDriver());
        for (WebElement el : els)
        {
            el.click();
        }
    }

    public void clickFolder(String folder)
    {
        projectMenu().navigateToFolder(getCurrentProject(), folder);
    }

    public void navigateToFolder(String project, String folderName)
    {
        projectMenu().navigateToFolder(project, folderName);
    }

    public String getCurrentContainer()
    {
        return (String) executeScript( "return LABKEY.container.title;");
    }

    /**
     * @deprecated Only used by old UX
     */
    @Deprecated
    private void waitForFolderNavigationReady()
    {
        waitForHoverNavigationReady();
        waitFor(() -> (boolean) executeScript("if (HoverNavigation._folder.webPartName == 'foldernav') return true; else return false;"),
                "HoverNavigation._folder not ready", WAIT_FOR_JAVASCRIPT);
    }

    /**
     * @deprecated Only used by old UX
     */
    @Deprecated
    private void waitForHoverNavigationReady()
    {
        waitFor(() -> (boolean) executeScript("if (window.HoverNavigation) return true; else return false;"),
                "HoverNavigation not ready", WAIT_FOR_JAVASCRIPT);
    }

    public void impersonateGroup(String group, boolean isSiteGroup)
    {
        navBar().userMenu().impersonateGroup(group, isSiteGroup);
    }

    public void impersonateRole(String role)
    {
        impersonateRoles(role);
    }

    public void impersonateRoles(String oneRole, String... roles)
    {
        navBar().userMenu().impersonateRoles(oneRole, roles);
    }

    public void impersonate(String fakeUser)
    {
        navBar().userMenu().impersonate(fakeUser);
    }

    @Deprecated
    public void stopImpersonatingRole()
    {
        stopImpersonating();
    }

    @Deprecated
    public void stopImpersonatingGroup()
    {
        stopImpersonating();
    }

    public void stopImpersonating()
    {
        stopImpersonating(true);
    }

    /**
     * Stop impersonating user
     * @param goHome go to Server Home or return to page where impersonation started
     */
    public void stopImpersonating(Boolean goHome)
    {
        navBar().stopImpersonating();
        if (goHome)
            goToHome();
    }

    public void goToHome()
    {
        beginAt("/project/home/begin.view");
    }

    /**
     * @deprecated Use {@link #clickButton(String)} or {@link #submit(Locator)}
     */
    @Deprecated
    public void submit()
    {
        WebElement form = Locators.bodyPanel().append("//form").findElement(getDriver());
        doAndWaitForPageToLoad(form::submit);
    }

    public void submit(Locator formLocator)
    {
        WebElement form = formLocator.findElement(getDriver());
        doAndWaitForPageToLoad(form::submit);
    }

    protected SelectRowsResponse executeSelectRowCommand(String schemaName, String queryName, ContainerFilter containerFilter, String path, @Nullable List<Filter> filters)
    {
        Connection cn = createDefaultConnection(false);
        SelectRowsCommand selectCmd = new SelectRowsCommand(schemaName, queryName);
        selectCmd.setMaxRows(-1);
        selectCmd.setContainerFilter(containerFilter);
        selectCmd.setColumns(Arrays.asList("*"));
        if (filters != null)
            selectCmd.setFilters(filters);

        SelectRowsResponse selectResp;

        try
        {
            selectResp = selectCmd.execute(cn, path);
        }
        catch (CommandException | IOException e)
        {
            throw new RuntimeException(e);
        }

        return selectResp;
    }
}
