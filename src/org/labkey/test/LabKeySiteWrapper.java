/*
 * Copyright (c) 2015-2019 LabKey Corporation
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
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Assume;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.components.core.ProjectMenu;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.components.ui.navigation.UserMenu;
import org.labkey.test.pages.core.admin.CustomizeSitePage;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.pages.user.UserDetailsPage;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExperimentalFeaturesHelper;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SimpleHttpRequest;
import org.labkey.test.util.SimpleHttpResponse;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.TextSearcher;
import org.labkey.test.util.Timer;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.TestProperties.isDevModeEnabled;
import static org.labkey.test.WebTestHelper.buildURL;
import static org.labkey.test.WebTestHelper.getBaseURL;
import static org.labkey.test.WebTestHelper.getHttpClientBuilder;
import static org.labkey.test.WebTestHelper.getHttpResponse;
import static org.labkey.test.WebTestHelper.logToServer;

/**
 * TODO: Move non-JUnit related methods from BWDT.
 * Many existing helpers, components, and page classes will need refactor as well
 */
public abstract class LabKeySiteWrapper extends WebDriverWrapper
{
    private static final int MAX_SERVER_STARTUP_WAIT_SECONDS = TestProperties.getServerStartupTimeout();
    private static final String CLIENT_SIDE_ERROR = "Client exception detected";
    public final APIUserHelper _userHelper = new APIUserHelper(this);

    public boolean isGuestModeTest()
    {
        return false;
    }

    protected void assumeTestModules()
    {
        Assume.assumeFalse("Test modules are needed but not installed. Skipping test.",
            TestProperties.isWithoutTestModules());
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
            waitForAnyElement("Should be on login or Home portal", Locator.id("email"), SiteNavBar.Locators.userMenu,
                    UserMenu.appUserMenu());
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
                if(isElementPresent(UserMenu.appUserMenu()))
                {
                    goToHome();
                }

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
     * Call the LogoutApi to sign out
     */
    public void simpleSignOut()
    {
        signOutHTTP();
        goToHome();
    }

    @LogMethod
    public void signOut(@Nullable String termsText)
    {
        log("Signing out");
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

    public void stopImpersonatingHTTP()
    {
        String stopImpersonatingUrl = WebTestHelper.buildURL("login", "stopImpersonating.api");
        SimpleHttpRequest logOutRequest = new SimpleHttpRequest(stopImpersonatingUrl, "POST");
        logOutRequest.copySession(getDriver());

        try
        {
            SimpleHttpResponse response = logOutRequest.getResponse();
            if (HttpStatus.SC_OK != response.getResponseCode() && HttpStatus.SC_UNAUTHORIZED != response.getResponseCode())
            {
                fail("Failed to stop impersonating. " + response.getResponseCode());
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @LogMethod
    public void ensureSignedInAsPrimaryTestUser()
    {
        boolean wasImpersonating = isImpersonating(); // To make sure browser isn't in an apparent impersonation state
        stopImpersonatingHTTP();
        if (!isSignedInAsPrimaryTestUser())
        {
            if (isSignedIn())
                signOutHTTP();
            simpleSignIn();
        }
        else if (wasImpersonating || !onLabKeyPage() || isOnServerErrorPage())
        {
            goToHome();
        }
        WebTestHelper.saveSession(getCurrentUser(), getDriver()); // In case a test signed in without using a helper
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
            String configId = getUrlParameters().get("configuration");

            //Select radio Yes
            checkRadioButton(Locator.radioButtonByNameAndValue("valid", "1"));

            //Click on button 'TestSecondary'
            clickAndWait(Locator.input("TestSecondary"));

            // delete the current secondaryAuth configuration
            deleteAuthenticationConfiguration(configId);
        }
        catch (NoSuchElementException ignored)
        {

        }
    }

    protected void acceptTermsOfUse(String termsText, boolean clickAgree)
    {
        Optional<WebElement> optionalCheckbox = Locators.termsOfUseCheckbox().findOptionalElement(getDriver());
        optionalCheckbox.ifPresent(termsCheckbox ->
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
        });
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
        assertEquals("Signed in as wrong user.", PasswordUtil.getUsername(), getCurrentUser());
    }

    // Just sign in & verify -- don't check for startup, upgrade, admin mode, etc.
    public void signIn(String email, String password)
    {
        attemptSignIn(email, password);
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
        WebElement signInButton = Locator.lkButton("Sign In").findElement(getDriver());
        signInButton.click();
        shortWait().until(ExpectedConditions.invisibilityOfElementLocated(Locator.byClass("signing-in-msg")));
        shortWait().until(ExpectedConditions.or(
                ExpectedConditions.stalenessOf(signInButton), // Successful login
                ExpectedConditions.presenceOfElementLocated(Locators.labkeyError.withText()))); // Error during sign-in
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
        beginAt(WebTestHelper.buildURL("security", "showRegistrationEmail", Map.of("email", user)));
        // Get setPassword URL from notification email.
        WebElement resetLink = Locator.linkWithHref("setPassword.view").findElement(getDriver());

        clickAndWait(resetLink, WAIT_FOR_PAGE);

        setFormElement(Locator.id("password"), password);
        setFormElement(Locator.id("password2"), password);

        clickButton("Set Password");
    }

    protected String getPasswordResetUrl(String username)
    {
        beginAt(WebTestHelper.buildURL("security", "showResetEmail", Map.of("email", username)));

        WebElement resetLink = Locator.xpath("//a[contains(@href, 'setPassword.view')]").findElement(getDriver());
        shortWait().until(ExpectedConditions.elementToBeClickable(resetLink));
        return resetLink.getText();
    }

    protected void resetPassword(String resetUrl, String username, String newPassword)
    {
        if (PasswordUtil.getUsername().equals(username))
            throw new IllegalArgumentException("Don't change the primary site admin user's password");

        if (resetUrl != null)
            beginAt(resetUrl);

        assertTextPresent(username,
                "has been verified! Create an account password below.",
                "Your password must be at least six characters and cannot contain spaces or match your email address."
        );

        setFormElement(Locator.id("password"), newPassword);
        setFormElement(Locator.id("password2"), newPassword);

        clickButton("Set Password");
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

            setCheckbox(waitForElement(Locator.name("enableSystemMaintenance")), enable);

            clickButton("Save");
        }
    }

    /**
     * @deprecated This method is mostly unnecessary and the end state is
     * inconsistent. It may or may not navigate and may or may not stop
     * impersonating.
     */
    @Deprecated
    public void ensureAdminMode()
    {
        if (!onLabKeyPage())
            goToHome();
        if (!isSignedIn())
            simpleSignIn();
        else if (!isUserSystemAdmin() && isImpersonating())
            stopImpersonating(false);
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
        Boolean hitFirstPage = null;
        log("Verifying that server has started...");
        Timer startupTimer = new Timer(Duration.ofSeconds(MAX_SERVER_STARTUP_WAIT_SECONDS));
        Throwable lastError = null;
        do
        {
            if (hitFirstPage == null)
            {
                hitFirstPage = false;
            }
            else
            {
                // retrying
                log("Server is not ready.  Waiting " + startupTimer.timeRemaining().getSeconds() + " more seconds...");
                sleep(1000);
            }
            try
            {
                String startPage = buildURL("project", "home", "start");
                SimpleHttpResponse httpResponse = WebTestHelper.getHttpResponse(startPage);
                if (httpResponse.getResponseCode() >= 400)
                {
                    log("Waiting for server: " + httpResponse.getResponseCode());
                    // Don't try to interact with the WebDriver while the site is unresponsive. It can cause tests to hang
                    continue;
                }
                else
                {
                    log("Response: " + httpResponse.getResponseCode());
                }
                getDriver().manage().timeouts().pageLoadTimeout(Duration.ofMillis(WAIT_FOR_PAGE));
                getDriver().get(startPage);

                try
                {
                    waitForElement(Locator.CssLocator.union(Locator.css("table.labkey-main"), Locator.css("#permalink"), Locator.css("#headerpanel")));
                    hitFirstPage = true;
                }
                catch (NoSuchElementException e)
                {
                    lastError = e;
                }
            }
            catch (WebDriverException e)
            {
                // ignore timeouts that occur during startup; a poorly timed request
                // as the webapp is loading may hang forever, causing a timeout.
                log("Waiting for server: " + e.getMessage());
                lastError = e;
            }
            catch (RuntimeException e)
            {
                if (e.getCause() != null && e.getCause() instanceof IOException)
                {
                    log("Waiting for server: " + e.getCause().getMessage());
                    lastError = e;
                }
                else
                    throw e;
            }
        } while (!hitFirstPage && !startupTimer.isTimedOut());
        if (!hitFirstPage)
        {
            throw new RuntimeException("Webapp failed to start up after " + MAX_SERVER_STARTUP_WAIT_SECONDS + " seconds.", lastError);
        }
        log("Server is running.");
        WebTestHelper.setUseContainerRelativeUrl((Boolean)executeScript("return LABKEY.experimental.containerRelativeURL;"));
    }

    @LogMethod
    private void checkForUpgrade()
    {
        final String upgradeText = "Please wait, this page will automatically update with progress information.";
        boolean bootstrapped = false;
        boolean performingUpgrade = false;

        // check to see if we're the first user:
        if (isTextPresent("Welcome! We see that this is your first time logging in."))
        {
            bootstrapped = true;
            assertTitleEquals("Account Setup");
            log("Need to bootstrap");
            verifyInitialUserRedirects();

            log("Testing bad email addresses");
            verifyInitialUserError(null, null, null, "Invalid email address");
            verifyInitialUserError("bogus@bogus@bogus", null, null, "Invalid email address: bogus@bogus@bogus");

            log("Testing bad passwords");
            String email = PasswordUtil.getUsername();
            verifyInitialUserError(email, null, null, "You must enter a password.");
            verifyInitialUserError(email, "LongEnough", null, "You must enter a password.");
            verifyInitialUserError(email, null, "LongEnough", "You must enter a password.");
            verifyInitialUserError(email, "short", "short", "Your password must be at least six characters and cannot contain spaces.");
            verifyInitialUserError(email, email, email, "Your password must not match your email address.");
            verifyInitialUserError(email, "LongEnough", "ButDontMatch", "Your password entries didn't match.");

            log("Register the first user");
            pushLocation();
            assertTextPresent("Confirm Password");
            verifyInitialUserError(email, PasswordUtil.getPassword(), PasswordUtil.getPassword(), null);

            // Runner was unable to log the test start prior to initial user creation
            logToServer("=== Starting " + getClass().getSimpleName() + " ===");

            log("Attempting to register another initial user");
            popLocation();
            // Make sure we got redirected to the module status page, since we already have a user
            assertTextNotPresent("Confirm Password");
            assertTextPresent("Please wait, this page will automatically update with progress information");

            WebTestHelper.saveSession(email, getDriver());
        }
        else if (getDriver().getTitle().startsWith("Sign In"))
        {
            // if the logout page takes us to the sign-in page, then we may have a schema update to do:
            if (getDriver().getTitle().startsWith("Sign In"))
                simpleSignIn();

            performingUpgrade = isTextPresent(upgradeText);
        }

        if (bootstrapped || performingUpgrade)
        {
            RuntimeException redirectCheckError = null;

            try
            {
                verifyRedirectBehavior(upgradeText);
            }
            catch (IOException | AssertionError fail)
            {
                // Delay throwing failure so that upgrade can finish
                redirectCheckError = new RuntimeException(fail);
            }

            int waitMs = 10 * 60 * 1000; // we'll wait at most ten minutes
            long startTime = System.currentTimeMillis();
            long elapsed = 0;

            while (elapsed < waitMs && (!isElementPresent(Locator.lkButton("Next"))))
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
                    Optional<WebElement> progressBar = Locator.id("status-progress-bar").findOptionalElement(getDriver());
                    log(bootstrapped ? "Bootstrapping" : "Upgrading" + (progressBar.map(webElement -> (": \"" + webElement.getText() + "\"")).orElse("")));
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

                // Tests hit Home portal a lot. Make it load as fast as possible
                new PortalHelper(this).removeAllWebParts();
                _userHelper.setInjectionDisplayName(PasswordUtil.getUsername());

                if (!TestProperties.isDevModeEnabled())
                {
                    TestLogger.log("Disable mothership reporting when bootstrapping in production mode");
                    CustomizeSitePage customizeSitePage = CustomizeSitePage.beginAt(this);
                    customizeSitePage.setUsageReportingLevel(CustomizeSitePage.ReportingLevel.NONE); // Don't report usage to labkey.org
                    customizeSitePage.setExceptionReportingLevel(CustomizeSitePage.ReportingLevel.NONE); // Don't report exceptions to labkey.org
                    // Note: leave the self-report setting unchanged
                    customizeSitePage.save();
                }
            }
            else // Just upgrading
            {
                Optional<WebElement> header = Locator.css(".labkey-nav-page-header").findOptionalElement(getDriver());
                if (header.isPresent() && Arrays.asList("Start Modules", "Upgrade Modules").contains(header.get().getText().trim()))
                {
                    waitForElement(Locator.id("status-progress-bar").withText("Module startup complete"), WAIT_FOR_PAGE);
                    clickAndWait(Locator.lkButton("Next"));
                    Locator.lkButton("Next")
                            .findOptionalElement(getDriver())
                            .ifPresent(button ->
                                    doAndWaitForPageToLoad(() ->
                                            shortWait().until(LabKeyExpectedConditions.clickUntilStale(button))));
                }
                else
                {
                    goToHome();
                }
            }

            PipelineStatusTable.goToAllJobsPage(this);
            log("Wait for any upgrade/bootstrap pipeline jobs");
            waitForRunningPipelineJobs(false, 120000);

            checkErrors(); // Check for errors from bootstrap/upgrade

            if (redirectCheckError != null)
                throw redirectCheckError;
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
                List<NameValuePair> loginParams = new ArrayList<>();
                loginParams.add(new BasicNameValuePair("email", PasswordUtil.getUsername()));
                loginParams.add(new BasicNameValuePair("password", PasswordUtil.getPassword()));

                // Login to get CSRF token
                HttpPost loginMethod = new HttpPost(getBaseURL() + "/login/loginApi.api");
                loginMethod.setEntity(new UrlEncodedFormEntity(loginParams));
                HttpClientContext httpContext = WebTestHelper.getBasicHttpContext();
                response = redirectClient.execute(loginMethod, httpContext);
                status = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected response to login: \n" + TestFileUtils.getStreamContentsAsString(response.getEntity().getContent()), HttpStatus.SC_OK, status);
                EntityUtils.consume(response.getEntity());

                List<NameValuePair> logoutParams = new ArrayList<>();
                Optional<Cookie> csrfToken = httpContext.getCookieStore().getCookies().stream().filter(c -> c.getName().equals(Connection.X_LABKEY_CSRF)).findAny();
                csrfToken.ifPresent(cookie -> logoutParams.add(new BasicNameValuePair(Connection.X_LABKEY_CSRF, cookie.getValue())));
                // Logout to verify redirect
                HttpPost logoutMethod = new HttpPost(getBaseURL() + "/login/logout.view");
                logoutMethod.setEntity(new UrlEncodedFormEntity(logoutParams));
                response = redirectClient.execute(logoutMethod, httpContext);
                status = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected response to logout: \n" + TestFileUtils.getStreamContentsAsString(response.getEntity().getContent()), HttpStatus.SC_OK, status);
                // TODO: check login, once http-equiv redirect is sorted out
                assertFalse("Upgrade text found", WebTestHelper.getHttpResponseBody(response).contains(upgradeText));
                EntityUtils.consume(response.getEntity());
            }
        }
        finally
        {
            if (null != response)
                EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    public static final Pattern ERROR_PATTERN = Pattern.compile("^(ERROR|FATAL)", Pattern.MULTILINE);

    public void checkErrors()
    {
        if (isGuestModeTest())
            return;

        ensureSignedInAsPrimaryTestUser();
        String serverErrors = getServerErrors();
        if (!serverErrors.isEmpty())
        {
            TestLogger.error("Server errors:");
            TestLogger.increaseIndent();

            final Iterator<String> iterator = Arrays.stream(serverErrors.split("\\n")).iterator();
            while (iterator.hasNext())
            {
                String line = iterator.next();
                if ((line.startsWith("ERROR") || line.startsWith("FATAL")) && !line.endsWith("Additional exception info:"))
                {
                    TestLogger.error(line);
                    if (iterator.hasNext())
                    {
                        // Line after the ERROR usually has the exception type and error message
                        TestLogger.error("    " + iterator.next());
                    }
                }
                if (line.startsWith("Caused by:"))
                {
                    // Append all nested exception messages
                    TestLogger.error("  " + line);
                }
            }

            TestLogger.decreaseIndent();

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

    @LogMethod
    public void checkExpectedErrors(@LoggedParam int expectedErrors)
    {
        int count = getServerErrorCount();

        if (expectedErrors != count)
        {
            beginAt(buildURL("admin", "showErrorsSinceMark"));
            resetErrors();
            assertEquals("Expected error count does not match actual count for this run.", expectedErrors, count);
        }

        // Clear expected errors to prevent the test from failing.
        resetErrors();
    }

    protected int getServerErrorCount()
    {
        String text = getServerErrors();
        Matcher errorMatcher = ERROR_PATTERN.matcher(text);
        int count = 0;
        while (errorMatcher.find())
        {
            count++;
        }
        return count;
    }

    public void resetErrors()
    {
        if (isGuestModeTest())
            return;

        invokeApiAction(null, "admin", "resetErrorMark", "Failed to reset server errors");
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
        waitForTextWithRefresh(Math.max(timeLeft, 0), "System maintenance complete");
    }

    public void goToProjectHome(String projectName)
    {
        beginAt(buildURL("project", projectName, "begin"));
    }

    public void goToProjectFolder(String projectName, String subfolder)
    {
        beginAt(buildURL("project", projectName + "/" + subfolder, "begin"));
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

    public UserDetailsPage goToMyAccount()
    {
        clickUserMenuItem("My Account");
        return new UserDetailsPage(getDriver());
    }

    public PipelineStatusTable goToDataPipeline()
    {
        goToModule("Pipeline");
        return new PipelineStatusTable(this);
    }

    public void goToExternalToolPage()
    {
        clickUserMenuItem("External Tool Access");
    }

    protected WebElement openMenu(String menuText)
    {
        WebElement menu = Locator.menuBarItem(menuText).findElement(getDriver());
        menu.click();
        return menu;
    }

    private static Map<String, Boolean> _originalFeatureFlags = new HashMap<>();

    /**
     * Enable/disable the experimental features specified by the test properties. Intended for use by base tests only.
     * Currently only {@link BaseWebDriverTest}.
     */
    protected void setExperimentalFlags()
    {
        Map<String, Boolean> flags = TestProperties.getExperimentalFeatures();

        if (flags.isEmpty())
            return;

        TestLogger.log("Setting experimental flags for duration of the test:");
        TestLogger.increaseIndent();

        Connection cn = createDefaultConnection();
        for (Map.Entry<String, Boolean> flag : flags.entrySet())
        {
            String feature = flag.getKey();
            Boolean value = flag.getValue();
            Boolean previouslyEnabled = ExperimentalFeaturesHelper.setExperimentalFeature(cn, feature, value);

            // When setting a feature flag the first time, remember the previous setting
            if (!_originalFeatureFlags.containsKey(feature))
            {
                _originalFeatureFlags.put(feature, previouslyEnabled);
            }
        }
        TestLogger.decreaseIndent();
    }

    /**
     * Reset the experimental features specified by the test properties. Intended for use by base tests only.
     * Currently only {@link BaseWebDriverTest}.
     */
    protected void resetExperimentalFlags()
    {
        if (_originalFeatureFlags.isEmpty() || TestProperties.isTestRunningOnTeamCity())
        {
            return;
        }

        TestLogger.log("Resetting experimental flags to their original value:");

        TestLogger.increaseIndent();
        Connection cn = createDefaultConnection();
        for (Map.Entry<String, Boolean> features : _originalFeatureFlags.entrySet())
        {
            ExperimentalFeaturesHelper.setExperimentalFeature(cn, features.getKey(), features.getValue());
        }
        TestLogger.decreaseIndent();

        _originalFeatureFlags = new HashMap<>();
    }

    @LogMethod(quiet = true)
    public boolean disableMiniProfiler()
    {
        boolean restoreMiniProfiler = isMiniProfilerEnabled();
        if (restoreMiniProfiler)
            setMiniProfilerEnabled(false);
        return restoreMiniProfiler;
    }

    @LogMethod(quiet = true)
    public boolean isMiniProfilerEnabled()
    {
        Connection cn = createDefaultConnection();
        Command<?> command = new Command<>("mini-profiler", "isEnabled");
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
        catch (IOException e)
        {
            throw new RuntimeException("Failed to get mini-profiler enabled state", e);
        }
        catch (CommandException e)
        {
            TestLogger.log("Unable to get miniProfiler state. Ignoring: " + e.getStatusCode());
        }

        return false;
    }

    @LogMethod
    public void setMiniProfilerEnabled(boolean enabled)
    {
        Connection cn = createDefaultConnection();
        PostCommand<?> setEnabled = new PostCommand<>("mini-profiler", "enable");
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
        assumeTestModules();
        int responseCode = getHttpResponse(buildURL("dumbster", "setRecordEmail", Maps.of("record", "true")), "POST").getResponseCode();
        assertEquals("Failed to enable email recording", HttpStatus.SC_OK, responseCode);
    }

    public EmailRecordTable goToEmailRecord()
    {
        beginAt(buildURL("dumbster", "begin"));
        return new EmailRecordTable(getDriver());
    }

    public void setAuthenticationProvider(String provider, boolean enabled)
    {
        setAuthenticationProvider(provider, enabled, createDefaultConnection());
    }

    @LogMethod(quiet = true)
    public void deleteAuthenticationConfiguration(@LoggedParam String id)
    {
        String url = WebTestHelper.buildURL("login", "deleteConfiguration", Maps.of("configuration", id));
        SimpleHttpRequest deleteRequest = new SimpleHttpRequest(url, "POST");
        deleteRequest.copySession(getDriver());

        try
        {
            SimpleHttpResponse response = deleteRequest.getResponse();
            assertEquals(HttpStatus.SC_OK, response.getResponseCode());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @LogMethod(quiet = true)
    public void setAuthenticationProvider(@LoggedParam String provider, @LoggedParam boolean enabled, Connection cn)
    {
        Command<?> command = new PostCommand<>("login", "setProviderEnabled");
        command.setParameters(new HashMap<>(Maps.of("provider", provider, "enabled", enabled)));
        try
        {
            command.execute(cn, null);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to " + (enabled ? "enable" : "disable") + " login provider: " + provider, e);
        }
    }

    // Simple helper to invoke an API action that takes no parameters via POST
    @LogMethod(quiet = true)
    protected void invokeApiAction(@Nullable String folderPath, String controllerName, String actionName, String failureMessage)
    {
        Command<CommandResponse> command = new PostCommand<>(controllerName, actionName);
        Connection connection = WebTestHelper.getRemoteApiConnection();

        try
        {
            command.execute(connection, folderPath);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(failureMessage, e);
        }
    }

    @LogMethod(quiet = true)
    public int setAuthenticationParameter(String parameter, boolean enabled)
    {
        return WebTestHelper.getHttpResponse(WebTestHelper.buildURL("login", "setAuthenticationParameter", Map.of("parameter", parameter, "enabled", String.valueOf(enabled))), "POST").getResponseCode();
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
     * @deprecated Use {@link ProjectMenu#open}
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
    public void stopImpersonating(boolean goHome)
    {
        navBar().stopImpersonating();
        if (goHome)
            goToHome();
    }

    /**
     * Impersonate a user and perform some action.
     * Stops impersonating and returns to initial page when complete.
     * @param email User to impersonate
     * @param action Runnable to invoke while impersonating
     */
    public void doAsUser(String email, Runnable action)
    {
        pushLocation();
        impersonate(email);
        action.run();
        stopImpersonating(false);
        popLocation();
    }

    public void goToHome()
    {
        beginAt(WebTestHelper.buildURL("project", "home", "begin"));
        waitFor(this::onLabKeyPage, "Home project didn't seem to load. JavaScript 'LABKEY' namespace not found.", 10000);
    }

    public void clickPortalTab(String tabText)
    {
        new PortalHelper(this).activateTab(tabText);
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
        return executeSelectRowCommand(schemaName, queryName, containerFilter, path, filters, List.of("*"));
    }

    protected SelectRowsResponse executeSelectRowCommand(String schemaName, String queryName, ContainerFilter containerFilter,
                                                         String path, @Nullable List<Filter> filters, @Nullable List<String> requestedColumns)
    {
        Connection cn = createDefaultConnection();
        SelectRowsCommand selectCmd = new SelectRowsCommand(schemaName, queryName);
        selectCmd.setMaxRows(-1);
        selectCmd.setContainerFilter(containerFilter);
        selectCmd.setColumns(requestedColumns);
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

    // Returns the text contents of every "Status" cell in the pipeline StatusFiles grid
    public List<String> getPipelineStatusValues()
    {
        PipelineStatusTable status = new PipelineStatusTable(this);
        try
        {
            return status.getColumnDataAsText("Status");
        }
        catch (StaleElementReferenceException retry) // Page auto-refreshes
        {
            return status.getColumnDataAsText("Status");
        }
    }

    // Returns count of "COMPLETE" and "ERROR"
    private int getFinishedCount(List<String> statusValues)
    {
        List<String> finishedStates = new ArrayList<>(Arrays.asList("COMPLETE", "ERROR", "CANCELLED"));
        if (statusValues.contains("ERROR"))
            finishedStates.add("SPLIT WAITING"); // Split jobs never "finish" if subjobs have errors
        return (int) statusValues.stream().filter(finishedStates::contains).count();
    }

    public void waitForPipelineJobsToComplete(final int finishedJobsExpected, final boolean expectError)
    {
        waitForPipelineJobsToComplete(finishedJobsExpected, null, expectError);
    }

    public void waitForPipelineJobsToComplete(final int finishedJobsExpected, final String description, final boolean expectError)
    {
        waitForPipelineJobsToComplete(finishedJobsExpected, description, expectError, BaseWebDriverTest.MAX_WAIT_SECONDS * 1000);
    }

    /**
     * Wait for all in-progress pipeline jobs to finish, then assert the quantity and lack of or presence of an error
     * @param finishedJobsExpected Exact number of jobs to expect
     * @param description "Description" field for at least one of the finished jobs (Not currently a strict check)
     * @param expectError If true, at least one job must have an ERROR state; otherwise all must be COMPLETE
     * @param timeoutMilliseconds Maximum time to wait for pipeline jobs to finish (default 10 minutes)
     */
    @LogMethod
    public void waitForPipelineJobsToComplete(@LoggedParam final int finishedJobsExpected, @LoggedParam final String description, final boolean expectError, int timeoutMilliseconds)
    {
        final List<String> statusValues = waitForPipelineJobsToFinish(finishedJobsExpected, Duration.ofMillis(timeoutMilliseconds));

        if (expectError)
            assertTrue("Did not find expected pipeline error.", statusValues.contains("ERROR"));
        else
            assertFalse("Found unexpected pipeline error.", statusValues.contains("ERROR"));

        if (description != null)
        {
            DataRegionTable status = new DataRegionTable("StatusFiles", getDriver());
            final List<String> actualDescriptions = status.getColumnDataAsText("Description");
            if (actualDescriptions.parallelStream().noneMatch(desc -> desc.contains(description)))
            {
                log("WARNING: Did not find a job with expected description: " + description); // TODO: change to fail state?
            }
        }
    }

    public List<String> waitForPipelineJobsToFinish(@LoggedParam int jobsExpected)
    {
        return waitForPipelineJobsToFinish(jobsExpected, Duration.ofSeconds(BaseWebDriverTest.MAX_WAIT_SECONDS));
    }

    /**
     * Wait until pipeline UI shows that all jobs have finished then assert the quantity
     * @param jobsExpected Exact number of jobs to expect
     * @param timeout {@link Duration} to wait for pipeline jobs to finish (default 10 minutes)
     * @return {@link List} of status values for all pipeline jobs
     */
    @LogMethod
    private List<String> waitForPipelineJobsToFinish(@LoggedParam int jobsExpected, @LoggedParam Duration timeout)
    {
        log("Waiting for " + jobsExpected + " pipeline jobs to finish");
        Timer timer = new Timer(timeout);
        List<String> statusValues = waitForRunningPipelineJobs(timeout.toMillis());
        while (statusValues.size() < jobsExpected && !timer.isTimedOut())
        {
            sleep(1000);
            refresh();
            statusValues = waitForRunningPipelineJobs(timer.timeRemaining().toMillis());
        }
        assertEquals("Did not find correct number of finished pipeline jobs.", jobsExpected, getFinishedCount(statusValues));
        return statusValues;
    }

    /**
     * Wait until pipeline UI shows that all jobs have finished
     * If the timeout is exceeded and one of the pipeline jobs is in a "WAIT" state, we show the pipeline status grid
     * filtered to ALL_FOLDERS and exclude and finished or waiting jobs to get more informative failure information
     * @param timeoutMilliseconds Maximum time to wait for pipeline jobs to finish (default 10 minutes)
     * @return {@link List} of status values for all pipeline jobs
     */
    @LogMethod
    public List<String> waitForRunningPipelineJobs(long timeoutMilliseconds)
    {
        List<String> statusValues = getPipelineStatusValues();
        long start = System.currentTimeMillis();
        while (statusValues.size() > getFinishedCount(statusValues) && System.currentTimeMillis() - start < timeoutMilliseconds)
        {
            log("[" + StringUtils.join(statusValues,",") + "]");
            log("Waiting for " + (statusValues.size() - getFinishedCount(statusValues)) + " job(s) to complete...");
            sleep(1000);
            refresh();
            statusValues = getPipelineStatusValues();
        }
        log("Final: [" + StringUtils.join(statusValues,",") + "]");

        boolean waitingJobs = statusValues.stream().anyMatch(status -> status.contains("WAIT"));
        if (waitingJobs)
        {
            log("WARNING: Pipeline appears stalled. Showing all unfinished jobs.");
            PipelineStatusTable pipelineStatusTable = new PipelineStatusTable(this);
            pipelineStatusTable.setContainerFilter(DataRegionTable.ContainerFilterType.ALL_FOLDERS);
            addUrlParameter(pipelineStatusTable.getDataRegionName() + ".Status~notin=COMPLETE;CANCELLED;ERROR&" +
                    pipelineStatusTable.getDataRegionName() + ".Status~doesnotcontain=WAIT");
            final List<String> descriptions = pipelineStatusTable.getColumnDataAsText("Description");
            fail("Timed out waiting for pipeline job to start. Waiting on " + (descriptions.isEmpty() ? "<unknown>" : descriptions));
        }
        assertEquals("Running pipeline jobs were found.  Timeout:" + timeoutMilliseconds + "sec", 0, statusValues.size() - getFinishedCount(statusValues));

        return statusValues;
    }

    public List<String> waitForRunningPipelineJobs(boolean expectError, long timeoutMilliseconds)
    {
        List<String> statusValues = waitForRunningPipelineJobs(timeoutMilliseconds);
        if (expectError)
            assertTrue("Didn't find expected pipeline error", statusValues.contains("ERROR"));
        else
            assertFalse("Found unexpected pipeline error", statusValues.contains("ERROR"));
        return statusValues;
    }

    @LogMethod
    protected void deletePipelineJob(@LoggedParam String jobDescription, @LoggedParam boolean deleteRuns)
    {
        deletePipelineJob(jobDescription, deleteRuns, false);
    }

    protected void deleteAllPipelineJobs() {
        goToModule("Pipeline");
        PipelineStatusTable table = new PipelineStatusTable(this);
        table.deleteAllPipelineJobs();
    }

    @LogMethod
    protected void deletePipelineJob(@LoggedParam String jobDescription, @LoggedParam boolean deleteRuns, @LoggedParam boolean descriptionStartsWith)
    {
        goToModule("Pipeline");

        PipelineStatusTable table = new PipelineStatusTable(this);
        int tableJobRow = table.getJobRow(jobDescription, descriptionStartsWith);
        assertNotEquals("Failed to find job rowid", -1, tableJobRow);
        table.checkCheckbox(tableJobRow);
        table.clickHeaderButton("Delete");
        assertElementPresent(Locator.linkContainingText(jobDescription));
        if (deleteRuns && isElementPresent(Locator.id("deleteRuns")))
            checkCheckbox(Locator.id("deleteRuns"));
        clickButton("Confirm Delete");
    }

    // Note: Keep in sync with ConvertHelper.getStandardConversionErrorMessage()
    // Example: "Could not convert value '2.34' (Double) for Boolean field 'Medical History.Dep Diagnosed in Last 18 Months'"
    public String getConversionErrorMessage(Object value, String fieldName, Class<?> targetClass)
    {
        return "Could not convert value '" + value + "' (" + value.getClass().getSimpleName() + ") for " + targetClass.getSimpleName() + " field '" + fieldName + "'";
    }
}
