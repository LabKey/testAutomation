/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.SeleniumException;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.labkey.test.util.Crawler;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PasswordUtil;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import static org.labkey.test.WebTestHelper.DEFAULT_TARGET_SERVER;
import static org.labkey.test.WebTestHelper.FolderIdentifier;
import static org.labkey.test.WebTestHelper.GC_ATTEMPT_LIMIT;
import static org.labkey.test.WebTestHelper.MAX_LEAK_LIMIT;
import static org.labkey.test.WebTestHelper.getTabLinkId;
import static org.labkey.test.WebTestHelper.getTargetServer;
import static org.labkey.test.WebTestHelper.leakCRC;
import static org.labkey.test.WebTestHelper.logToServer;

/**
 * User: Mark Igra
 * Date: Feb 7, 2007
 * Time: 5:31:38 PM
 */
public abstract class BaseSeleniumWebTest extends TestCase implements Cleanable, WebTest
{
    protected DefaultSeleniumWrapper selenium;
    private static final int DEFAULT_SELENIUM_PORT = 4444;
    private static final String DEFAULT_SELENIUM_SERVER = "localhost";
    private String _lastPageTitle = null;
    private URL _lastPageURL = null;
    private String _lastPageText = null;
    private Stack<String> _locationStack = new Stack<String>();
    private Stack<String> _impersonationStack = new Stack<String>();
    private List<String> _createdProjects = new ArrayList<String>();
    private List<FolderIdentifier> _createdFolders = new ArrayList<FolderIdentifier>();
    protected boolean _testFailed = true;
    protected boolean _testTimeout = false;
    public final static int WAIT_FOR_PAGE = 60000;
    protected int defaultWaitForPage = WAIT_FOR_PAGE;
    public final static int WAIT_FOR_JAVASCRIPT = 10000;
    protected int longWaitForPage = defaultWaitForPage * 5;
    private boolean _fileUploadAvailable;
    protected long _startTime;

    private static final int MAX_SERVER_STARTUP_WAIT_SECONDS = 60;
    protected static final int MAX_WAIT_SECONDS = 10 * 60;

    public final static String FIREFOX_BROWSER = "*firefox";
    private final static String FIREFOX_UPLOAD_BROWSER = "*chrome";
    public final static String IE_BROWSER = "*iexploreproxy";
    //protected final static String IE_UPLOAD_BROWSER = "*iehta";

    /** Have we already done a memory leak and error check in this test harness VM instance? */
    protected static boolean _checkedLeaksAndErrors = false;
    private static final String ACTION_SUMMARY_TABLE_NAME = "springActions";

    public BaseSeleniumWebTest()
    {

    }

    public static int getSeleniumServerPort() {
        String portString = System.getProperty("selenium.server.port", "" + DEFAULT_SELENIUM_PORT);
        return Integer.parseInt(portString);
    }

    public static String getSeleniumServer() {
        return System.getProperty("selenium.server", DEFAULT_SELENIUM_SERVER);
    }

    public DefaultSeleniumWrapper getWrapper()
    {
        return selenium;
    }

    public String getLabKeyRoot()
    {
        return WebTestHelper.getLabKeyRoot();
    }

    public String getContextPath()
    {
        return WebTestHelper.getContextPath();
    }

    @Override
    public void setUp() throws Exception
    {
        selenium = new DefaultSeleniumWrapper();
        selenium.start();
        selenium.setTimeout(Integer.toString(defaultWaitForPage));
        //Now inject our standard javascript functions...
        InputStream inputStream = BaseSeleniumWebTest.class.getResourceAsStream("seleniumHelpers.js");
        String script = getStreamContentsAsString(inputStream);
        System.out.println("Loading scripts from seleniumHelpers.js");
        System.out.println(selenium.getEval(script));

        if (this.enableScriptCheck())
            beginJsErrorChecker();
    }

    public void beginJsErrorChecker()
    {
        selenium.getEval("selenium.doBeginJsErrorChecker();");
    }

    public void endJsErrorChecker()
    {
        selenium.getEval("selenium.doEndJsErrorChecker();");
    }

    /**
     * Override if using file upload features in the test. Returning true will attempt to use
     * a version of the browser that allows file upload fields to be set. Defaults to false.
     * Use isFileUploadAvailable to see if request worked.
     * @return
     */
    protected boolean isFileUploadTest()
    {
        return false;
    }

    public boolean isFileUploadAvailable()
    {
        return _fileUploadAvailable;
    }

    public String getBrowserType()
    {
        return System.getProperty("selenium.browser", FIREFOX_BROWSER);
    }

    public String getBrowser()
    {
        String browser = System.getProperty("selenium.browser", FIREFOX_BROWSER);
        String browserPath = System.getProperty("selenium.browser.path", "");
        if (browserPath.length() > 0)
            browserPath = " " + browserPath;

        //File upload is "experimental" in selenium, so only use it when
        //necessary
        if (isFileUploadTest())
        {
            // IE is currently unable to do a file upload
            if (browser.startsWith(IE_BROWSER))
            {
                log("Warning: Internet Explorer cannot do file uploads!");
                //browser = IE_UPLOAD_BROWSER;
                //_fileUploadAvailable = true;
            }
            else if (browser.startsWith(FIREFOX_BROWSER))
            {
                browser = FIREFOX_UPLOAD_BROWSER;
                _fileUploadAvailable = true;
            }
        }
        return browser + browserPath;
    }

    static String getStreamContentsAsString(InputStream is) throws IOException
    {
        StringBuilder contents = new StringBuilder();
        BufferedReader input = null;

        try
        {
            input = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = input.readLine()) != null)
            {
                contents.append(line);
                contents.append("\n");
            }
        }
        finally
        {
            try
            {
                if (input != null) input.close();
            }
            catch (IOException e)
            {
                // Do nothing.
            }
        }
        return contents.toString();
    }

    public void copyFile(File original, File copy)
    {
        InputStream fis = null;
        OutputStream fos = null;
        try
        {
            copy.getParentFile().mkdirs();
            fis = new BufferedInputStream(new FileInputStream(original));
            fos = new BufferedOutputStream(new FileOutputStream(copy));
            int read;
            byte[] buffer = new byte[1024];
            while ((read = fis.read(buffer, 0, buffer.length)) > 0)
                fos.write(buffer, 0, read);
        }
        catch (IOException e)
        {
            fail(e.getMessage());
        }
        finally
        {
            if (fis != null) try { fis.close(); } catch (IOException e) {}
            if (fos != null) try { fos.close(); } catch (IOException e) {}
        }
    }


    @Override
    public void tearDown() throws Exception
    {
        if (this.enableScriptCheck())
            endJsErrorChecker();

        boolean skipTearDown = _testFailed && System.getProperty("close.on.fail", "true").equalsIgnoreCase("false");
        if (!skipTearDown)
        {
            //selenium.close(); // unnecessary since selenium.stop will close windows.
            selenium.stop();
        }
    }

    public void log(String str)
    {
        String d = new SimpleDateFormat("HH:mm:ss,SSS").format(new Date());      // Include time with log entry.  Use format that matches labkey log.
        System.out.println(d + " " + str);
    }

    private static final Pattern LABKEY_ERROR_TITLE_PATTERN = Pattern.compile("\\d\\d\\d\\D.*Error.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOMCAT_ERROR_PATTERN = Pattern.compile("HTTP Status\\s*(\\d\\d\\d)\\D");

    public int getResponseCode()
    {
        //We can't seem to get response codes via javascript, so we rely on default titles for error pages
        String title = selenium.getTitle();
        if (!title.toLowerCase().contains("error"))
            return 200;

        Matcher m = LABKEY_ERROR_TITLE_PATTERN.matcher(title);
        if (m.matches())
            return Integer.parseInt(title.substring(0, 3));

        //Now check the Tomcat page. This is going to be unreliable over time
        m = TOMCAT_ERROR_PATTERN.matcher(getResponseText());
        if (m.find())
            return Integer.parseInt(m.group(1));

        return 200;
    }

    public String getResponseText()
    {
        return selenium.getHtmlSource();
    }

    public URL getURL()
    {
        try
        {
            return new URL(selenium.getLocation());
        }
        catch (MalformedURLException x)
        {
            throw new RuntimeException("Bad location from selenium tester: " + selenium.getLocation(), x);
        }
    }

    public String[] getLinkAddresses()
    {
        String js = "selenium.getLinkAddresses();";
        String linkStr = selenium.getEval(js);
        String[] linkArray = linkStr.split("\\\\n");
        ArrayList<String> links = new ArrayList<String>(linkArray.length);
        for (String link : linkArray)
        {
            if (link.contains("#"))
            {
                link = link.substring(0, link.indexOf("#"));
            }
            if (link != null && link.trim().length() > 0)
            {
                links.add(link);
            }
        }

        return links.toArray(new String[links.size()]);
    }


    public List<String> getCreatedProjects()
    {
        return _createdProjects;
    }

    public String getCurrentRelativeURL()
    {

        URL url = getURL();
        String urlString = selenium.getLocation();
        if ("80".equals(WebTestHelper.getWebPort()) && url.getAuthority().endsWith(":-1"))
        {
            int portIdx = urlString.indexOf(":-1");
            urlString = urlString.substring(0, portIdx) + urlString.substring(portIdx + (":-1".length()));
        }

        String baseURL = WebTestHelper.getBaseURL();
        assertTrue("Expected URL to begin with " + baseURL + ", but found " + urlString, urlString.indexOf(baseURL) == 0);
        return urlString.substring(baseURL.length());
    }

    public void pushLocation()
    {
        _locationStack.push(getCurrentRelativeURL());
    }

    public void popLocation()
    {
        popLocation(defaultWaitForPage);
    }

    public void popLocation(int millis)
    {
        String location = _locationStack.pop();
        assertNotNull("Cannot pop without a push.", location);
        beginAt(location, millis);
    }

    public void refresh()
    {
        refresh(defaultWaitForPage);
    }

    public void refresh(int millis)
    {
        selenium.refresh();
        waitForPageToLoad(millis);
    }


    public void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException e)
        {
        }
    }

    public void signIn()
    {
        if ( isGuestModeTest() )
        {
            waitForStartup();
            log("Skipping sign in.  Test runs as guest.");
            beginAt("/login/logout.view");
            return;
        }

        try
        {
            PasswordUtil.ensureCredentials();
        }
        catch (IOException e)
        {
            fail("Unable to ensure credentials: " + e.getMessage());
        }
        waitForStartup();
        log("Signing in");
        //
        beginAt("/login/logout.view");
        checkForUpgrade();
        simpleSignIn();
        ensureAdminMode();
    }

    // Just sign in & verify -- don't check for startup, upgrade, admin mode, etc.
    public void simpleSignIn()
    {
        if ( isGuestModeTest() )
            return;
        if (!isTitleEqual("Sign In"))
            beginAt("/login/login.view");

        // Sign in if browser isn't already signed in.  Otherwise, we'll be on the home page.
        if (isTitleEqual("Sign In"))
        {
            assertTitleEquals("Sign In");
            assertFormPresent("login");
            setText("email", PasswordUtil.getUsername());
            setText("password", PasswordUtil.getPassword());
            clickLinkWithText("Sign In");

            if (isTextPresent("Type in your email address and password"))
                fail("Could not log in with the saved credentials.  Please verify that the test user exists on this installation or reset the credentials using 'ant setPassword'");
        }

        assertTextPresent("Sign Out");
        assertTextPresent("My Account");
    }

    // Just sign in & verify -- don't check for startup, upgrade, admin mode, etc.
    public void signIn(String email, String password, boolean failOnError)
    {
        if ( !isLinkPresentWithText("Sign In") )
            fail("You need to be logged out to log in.  Please log out to log in.");

        clickLinkWithText("Sign In");

        attemptSignIn(email, password);

        if ( failOnError )
        {
            if ( isTextPresent("Type in your email address and password") )
                fail("Could not log in with the saved credentials.  Please verify that the test user exists on this installation or reset the credentials using 'ant setPassword'");

            assertTextPresent("Sign Out");
            assertTextPresent("My Account");
        }
    }

    public void attemptSignIn(String email, String password)
    {

        clickLinkWithText("Sign In");

        assertTitleEquals("Sign In");
        assertFormPresent("login");
        setText("email", email);
        setText("password", password);
        clickLinkWithText("Sign In");
    }

    public void signInShouldFail(String email, String password, String... expectedMessages)
    {
        attemptSignIn(email, password);
        assertTitleEquals("Sign In");
        assertFormPresent("login");

        assertTextPresent(expectedMessages);
    }
                 
    protected void setInitialPassword(String user, String password)
    {
        // Get setPassword URL from notification email.
        goToModule("Dumbster");
        clickLink(Locator.xpath("//table[@id='dataregion_EmailRecord']//td[text() = '" + user + "']/..//a[contains(@href, 'setPassword.view')]"));

        setFormElement("password", password);
        setFormElement("password2", password);

        clickNavButton("Set Password");
    }

    protected void changePassword(String oldPassword, String password)
    {
        clickLinkWithText("My Account");
        clickNavButton("Change Password");

        setFormElement("oldPassword", oldPassword);
        setFormElement("password", password);
        setFormElement("password2", password);

        clickNavButton("Set Password");
    }


    /**
     * change user's e-mail from userEmail to newUserEmail from admin console
     */
    protected void changeUserEmail(String userEmail, String newUserEmail)
    {
        log("Attempting to change user email from " + userEmail + " to " + newUserEmail);


        clickLinkContainingText("Site Users");
        clickLinkContainingText(userEmail);

        clickNavButton("Change Email");

        setFormElement("newEmail", newUserEmail);
        clickNavButton("Submit");
    }

    protected enum PasswordRule {Weak, Strong}
    protected enum PasswordExpiration {Never, FiveSeconds, ThreeMonths, SixMonths, OneYear}

    private PasswordRule oldStrength = null;
    private PasswordExpiration oldExpiration = null;
    protected void setDbLoginConfig(PasswordRule strength, PasswordExpiration expiration)
    {
        PasswordRule curStrength = null;
        PasswordExpiration curExpiration = null;

        pushLocation();

        beginAt("/login/configureDbLogin.view");


        if ( oldStrength == null || oldExpiration == null )
        {
            // Remember old login settings.
            curStrength = PasswordRule.valueOf(getText(Locator.xpath("//input[@name='strength' and @value='Weak']/.."))); // getAttribute broken on IE
            curExpiration = PasswordExpiration.valueOf(getFormElement("expiration"));
        }

        if ( strength != null && curStrength != strength)
        {
            if ( oldStrength == null ) oldStrength = curStrength;
            click(Locator.radioButtonByNameAndValue("strength", strength.toString()));
        }

        if ( expiration != null && curExpiration != expiration)
        {
            if ( oldExpiration == null ) oldExpiration = curExpiration;
            setFormElement("expiration", expiration.toString());
        }

        clickNavButton("Save");

        popLocation();
    }

    protected void resetDbLoginConfig()
    {
        if ( oldStrength != null || oldExpiration != null )
        {
            pushLocation();

            if ( isElementPresent(Locator.linkWithText("Stop Impersonating") )) stopImpersonating();

            beginAt("/login/configureDbLogin.view");

            if ( oldStrength != null ) click(Locator.radioButtonByNameAndValue("strength", oldStrength.toString()));
            if ( oldExpiration != null ) setFormElement("expiration", oldExpiration.toString());

            clickNavButton("Save");

            if ( oldStrength != null ) assertEquals("Unable to reset password strength.", oldStrength, PasswordRule.valueOf(getText(Locator.xpath("//input[@name='strength' and @value='Weak']/.."))));
            if ( oldExpiration != null ) assertEquals("Unable to reset password expiration.", oldExpiration, PasswordExpiration.valueOf(getFormElement("expiration")));

            // Back to default.
            oldStrength = null;
            oldExpiration = null;

            popLocation();
        }
    }

    private Boolean initialSystemMaintenanceSchedule = null; // true = daily : false = never
    protected void setSystemMaintenance(boolean enable)
    {
        ensureAdminMode();
        clickLinkWithText("Admin Console");
        clickLinkWithText("site settings");
        if (initialSystemMaintenanceSchedule == null) initialSystemMaintenanceSchedule = getFormElement("systemMaintenanceInterval").equals("daily");
        checkRadioButton("systemMaintenanceInterval", enable ? "daily" : "never");
        clickNavButton("Save");
    }

    protected void resetSystemMaintenance()
    {
        if (initialSystemMaintenanceSchedule != null)
        {
            ensureAdminMode();
            clickLinkWithText("Admin Console");
            clickLinkWithText("site settings");
            checkRadioButton("systemMaintenanceInterval", initialSystemMaintenanceSchedule ? "daily" : "never");
            clickNavButton("Save");
        }
    }

    public void ensureAdminMode()
    {
        //Now switch to admin mode if available
        if (!isLinkPresentWithText("Projects"))
            clickAdminMenuItem("Show Navigation Bar");
    }

    public void hideNavigationBar()
    {
        clickAndWait(Locator.xpath("//a/span[text() = 'Admin']"), 0);
        waitForElement(Locator.tagContainingText("span", "Navigation Bar"), 1000);
        if (isElementPresent(Locator.tagContainingText("span", "Hide Navigation Bar")))
            clickAndWait(Locator.tagContainingText("span", "Hide Navigation Bar"));
    }

    /**
     * Allows test code to navigate to a Webpart Ext-based navigation menu.
     * @param webPartTitle title (not name) of webpart to be clicked.  Multiple web parts with the same title not supported.
     * @param items
     */
    public void clickWebpartMenuItem(String webPartTitle, String... items)
    {
        clickWebpartMenuItem(webPartTitle, true, items);
    }

    public void clickWebpartMenuItem(String webPartTitle, boolean wait, String... items)
    {
        ExtHelper.clickExtMenuButton(this, wait, Locator.xpath("//img[@id='more-" + webPartTitle.toLowerCase() + "']"), items);
    }

    public void clickAdminMenuItem(String... items)
    {
        ExtHelper.clickExtMenuButton(this, true, Locator.xpath("//a/span[text() = 'Admin']"), items);
    }

    // Click on a module listed on the admin menu
    public void goToModule(String moduleName)
    {
        clickAdminMenuItem("Go To Module", "More Modules", moduleName);
    }

    public void goToSchemaBrowser()
    {
        goToModule("Query");
    }

    private void waitForStartup()
    {
        boolean hitFirstPage = false;
        log("Verifying that server has started...");
        long ms = System.currentTimeMillis();
        while (!hitFirstPage && ((System.currentTimeMillis() - ms)/1000) < MAX_SERVER_STARTUP_WAIT_SECONDS)
        {
            try
            {
                beginAt("/login/logout.view");

                if (getResponseCode() != 404)
                {
                    hitFirstPage = true;
                }
                else
                {
                    long elapsedMs = System.currentTimeMillis() - ms;
                    log("Server is not ready.  Waiting " + (MAX_SERVER_STARTUP_WAIT_SECONDS -
                            (elapsedMs / 1000)) + " more seconds...");
                }
            }
            catch (SeleniumException e)
            {
                // ignore timeouts that occur during startup; a poorly timed request
                // as the webapp is loading may hang forever, causing a timeout.
                log("Ignoring selenium exception: " + e.getMessage());
            }
            finally
            {
                if (!hitFirstPage)
                {
                    sleep(1000);
                }
            }

        }
        if (!hitFirstPage)
        {
            fail("Webapp failed to start up after " + MAX_SERVER_STARTUP_WAIT_SECONDS + " seconds.");
        }
        log("Server is running.");
    }

    private void checkForUpgrade()
    {
        boolean bootstrapped = false;
        // check to see if we're the first user:
        if (isTextPresent("You are the first user"))
        {
            bootstrapped = true;
            assertTitleEquals("Register First User");
            log("Need to bootstrap");
            verifyInitialUserRedirects();
            log("Trying to register some bad email addresses");
            pushLocation();
            setFormElement("email", "bogus@bogus@bogus");
            clickLinkWithText("Register");
            assertTextPresent("The string 'bogus@bogus@bogus' is not a valid email address. Please enter an email address in this form: user@domain.tld");
            setFormElement("email", "");
            clickLinkWithText("Register");
            assertTextPresent("The string '' is not a valid email address. Please enter an email address in this form: user@domain.tld");

            log("Registering with the test email address");
            setText("email", PasswordUtil.getUsername());
            clickLinkWithText("Register");

            log("Attempting to register another initial user");
            popLocation();
            assertTextPresent("Initial user has already been created.");

            selenium.goBack();
            waitForPageToLoad();
            assertTextPresent("Choose a password you'll use to access this server.");
            assertTitleEquals("Choose a Password");

            log("Testing bad passwords");
            clickLinkWithText("Set Password");
            assertTextPresent("You must enter two passwords.");

            setFormElement("password", "LongEnough");
            clickLinkWithText("Set Password");
            assertTextPresent("You must enter two passwords.");

            setFormElement("password2", "LongEnough");
            clickLinkWithText("Set Password");
            assertTextPresent("You must enter two passwords.");

            setFormElement("password", "short");
            setFormElement("password2", "short");
            clickLinkWithText("Set Password");
            assertTextPresent("Your password must be six characters or more.");

            setFormElement("password", PasswordUtil.getUsername());
            setFormElement("password2", PasswordUtil.getUsername());
            clickLinkWithText("Set Password");
            assertTextPresent("Your password must not match your email address.");

            setFormElement("password", "LongEnough");
            setFormElement("password2", "ButDontMatch");
            clickLinkWithText("Set Password");
            assertTextPresent("Your password entries didn't match.");

            log("Set the test password");
            setText("password", PasswordUtil.getPassword());
            setText("password2", PasswordUtil.getPassword());
            clickLinkWithText("Set Password");
        }

        if (bootstrapped || isTitleEqual("Sign In"))
        {
            // if the logout page takes us to the sign-in page, then we may have a schema update to do:
            if (isTitleEqual("Sign In"))
                simpleSignIn();

            String upgradeButtonText = (isNavButtonPresent("Install") ? "Install" : (isNavButtonPresent("Upgrade") ? "Upgrade" : null));

            if (null != upgradeButtonText)
            {
                verifyUpgradeRedirect(upgradeButtonText);

                // Check that sign out and sign in work properly during upgrade/install (once initial user is configured)
                signOut();
                simpleSignIn();

                clickNavButton(upgradeButtonText);
            }

            int waitMs = 10 * 60 * 1000; // we'll wait at most ten minutes
            while (waitMs > 0 && (!(isNavButtonPresent("Next") || isLinkPresentWithText("Home"))))
            {
                try
                {
                    // Pound the server aggressively with requests for the home page to test synchronization
                    // in the sql script runner.
                    for (int i = 0; i < 5; i++)
                    {
                        goToHome();
                        Thread.sleep(200);
                        waitMs -= 200;
                    }
                    Thread.sleep(2000);
                    waitMs -= 2000;
                    if (isTextPresent("error occurred") || isTextPresent("failure occurred"))
                        fail("A startup failure occurred.");
                }
                catch (InterruptedException e)
                {
                    log(e.getMessage());
                }
                catch (SeleniumException e)
                {
                    // Do nothing -- this page will sometimes auto-navigate out from under selenium
                }
            }

            if (waitMs <= 0)
                fail("Script runner took more than 10 minutes to complete.");

            if (isNavButtonPresent("Next"))
            {
                clickNavButton("Next");

                // check for any additional upgrade pages inserted after module upgrade
                if (isNavButtonPresent("Next"))
                    clickNavButton("Next");

                // Save the default site config properties
                clickNavButton("Save");
            }
            else
            {
                clickLinkWithText("Home");
            }
        }
    }


    private void verifyInitialUserRedirects()
    {
        String initialText = "You are the first user";

        // These should NOT redirect to the upgrade page
        beginAt("/login/resetPassword.view");
        assertTextPresent(initialText);
        beginAt("/admin/maintenance.view");
        assertTextPresent(initialText);

        verifyStandardRedirects(initialText);
    }


    private void verifyUpgradeRedirect(String buttonText)
    {
        // These should NOT redirect to the upgrade page
        beginAt("/login/resetPassword.view");
        assertTextNotPresent(buttonText);
        beginAt("/admin/maintenance.view");
        assertTextNotPresent(buttonText);

        verifyStandardRedirects(buttonText);
    }


    private void verifyStandardRedirects(String assertText)
    {
        // These should all redirect to the upgrade page
        goToHome();
        assertTextPresent(assertText);
        beginAt("/nab/home/begin.view");  // A Spring action
        assertTextPresent(assertText);
        beginAt("/test/begin.view");      // Another Spring action
        assertTextPresent(assertText);
        beginAt("/test/permNone.view");   // A Spring action with no permissions
        assertTextPresent(assertText);
        beginAt("/admin/credits.view");   // An admin Spring action with no permissions
        assertTextPresent(assertText);
        beginAt("/admin/begin.view");     // An admin Spring action requiring admin permissions
        assertTextPresent(assertText);

        // Back to upgrade process
        goToHome();
    }

    public void disableMaintenance()
    {
        if ( isGuestModeTest() )
            return;
        beginAt("/admin/customizeSite.view");
        click(Locator.radioButtonByNameAndValue("systemMaintenanceInterval", "never"));
        clickNavButton("Save");
    }

    private long smStart = 0;

    public void startSystemMaintenance()
    {
        ensureAdminMode();
        clickLinkWithText("Admin Console");
        clickLinkWithText("site settings");
        selenium.openWindow("", "systemMaintenance");
        clickLinkWithText("Run system maintenance now", false);
        smStart = System.currentTimeMillis();
    }

    public void waitForSystemMaintenanceCompletion()
    {
        assertTrue("Must call startSystemMaintenance() before waiting for completion", smStart > 0);
        long elapsed = System.currentTimeMillis() - smStart;

        // Ensure that at least 5 seconds has passed since system maintenance was started
        if (elapsed < 5000)
        {
            log("Sleeping for " + (5000 - elapsed) + " ms");
            sleep(5000 - elapsed);
        }

        selenium.selectWindow("systemMaintenance");

        // Page updates automatically via AJAX... keep checking (up to 10 minutes) for system maintenance complete text
        waitFor(new Checker() {
            public boolean check()
            {
                return isTextPresent("System maintenance complete");
            }
        }, "System maintenance failed to complete in 10 minutes.", 10 * 60 * 1000);

        selenium.close();
        selenium.selectWindow(null);
    }

    public void populateLastPageInfo()
    {
        _lastPageTitle = getLastPageTitle();
        _lastPageURL = getLastPageURL();
        _lastPageText = getLastPageText();
    }

    public String getLastPageTitle()
    {
        if (_lastPageTitle == null)
        {
            if (null != selenium.getTitle())
                return selenium.getTitle();
            else
                return "[no title: content type is not html]";
        }
        return _lastPageTitle;
    }

    public String getLastPageText()
    {
        return _lastPageText != null ? _lastPageText : selenium.getHtmlSource();
    }

    public boolean isPageEmpty()
    {
        //IE and Firefox have different notions of empty.
        //IE returns html for all pages even empty text...
        String text = selenium.getHtmlSource();
        if (null == text || text.trim().length() == 0)
            return true;

        text = selenium.getBodyText();
        return null == text || text.trim().length() == 0;
    }

    public URL getLastPageURL()
    {
        try
        {
            return _lastPageURL != null ? _lastPageURL : new URL(selenium.getLocation());
        }
        catch (MalformedURLException x)
        {
            return null;
        }
    }

    public void resetErrors()
    {
        if ( isGuestModeTest() )
            return;
		if (getTargetServer().equals(DEFAULT_TARGET_SERVER))
        	beginAt("/admin/resetErrorMark.view");
    }

    /**
     * Override this method to skip running this test for a given database version.
     * @param info
     * @return true to run the test, false to skip.
     */
    protected boolean isDatabaseSupported(DatabaseInfo info)
    {
        return true;
    }

    public void testSteps() throws Exception
    {
        try
        {
            log("\n\n=============== Starting " + getClass().getSimpleName() + Runner.getProgress() + " =================");

            _startTime = System.currentTimeMillis();

            logToServer("=== Starting " + getClass().getSimpleName() + Runner.getProgress() + " ===");
            signIn();
			resetErrors();

            if( isMaintenanceDisabled() )
            {
                // Disable scheduled system maintenance to prevent timeouts during nightly tests.
                disableMaintenance();
            }


            if ( !isGuestModeTest() )
            {
                DatabaseInfo info = getDatabaseInfo();
                if (!isDatabaseSupported(info))
                {
                    log("** Skipping " + getClass().getSimpleName() + " test for unsupported database: " + info.productName + " " + info.productVersion);
                    return;
                }
            }

            try
            {
                log("Pre-cleaning " + getClass().getSimpleName());
                doCleanup();
            }
            catch (Throwable t)
            {
                t.printStackTrace();
                // fall through
            }

            // Only do this as part of test startup if we haven't already checked. Since we do this as the last
            // step in the test, there's no reason to bother doing it again at the beginning of the next test
            if (!_checkedLeaksAndErrors)
            {
                checkLeaksAndErrors();
            }

            doTestSteps();

            checkLeaksAndErrors();

            checkActionCoverage();

            if (enableLinkCheck())
            {
                Crawler crawler = new Crawler(this, Runner.getTestSet().getCrawlerTimeout());
                crawler.crawlAllLinks(enableInjectCheck());
            }

            _testFailed = false;

            try
            {
                if (!skipCleanup())
                {
                    goToHome();
                    doCleanup();
                }
                else
                {
                    log("Skipping test cleanup as requested.");
                }
            }
            catch (Throwable t)
            {
                log("WARNING: an exception occurred while cleaning up: " + t.getMessage());
                // fall through
            }

            checkLeaksAndErrors();
        }
        finally
        {
            try
            {
                populateLastPageInfo();
            }
            catch (Throwable t)
            {
                System.out.println("Unable to determine information about the last page: server not started or -Dlabkey.port incorrect?");
            }

            if (_testFailed)
            {
                try
                {
                    dump();
                    dumpPipelineFiles(getLabKeyRoot() + "/sampledata");
                    dumpPipelineLogFiles(getLabKeyRoot() + "/build/deploy/files");
                    if (_testTimeout)
                        dumpThreads();
                }
                catch (Exception t)
                {

                    System.out.println("Unable to dump failure information");
                    t.printStackTrace();
                }
                finally
                {
                    resetDbLoginConfig(); // Make sure to return DB config to its pre-test state.
                    resetSystemMaintenance(); // Return system maintenance config to its pre-test state.
                }
            }

            logToServer("=== Completed " + getClass().getSimpleName() + Runner.getProgress() + " ===");

            log("=============== Completed " + getClass().getSimpleName() + Runner.getProgress() + " =================");
        }
    }

    protected abstract void doTestSteps() throws Exception;

    protected abstract void doCleanup() throws Exception;

    public void cleanup() throws Exception
    {
        boolean tearDown = false;
        try
        {
            log("========= Cleaning up " + getClass().getSimpleName() + " =========");
            if (selenium == null)
            {
                setUp();
                tearDown = true;
            }

            // explicitly go back to the site, just in case we're on a 404 or crash page:
            beginAt("");
            signIn();
            doCleanup();

            beginAt("");

            // The following checks verify that the test deleted all projects and folders that it created.
            for (FolderIdentifier folder : _createdFolders)
                assertLinkNotPresentWithText(folder.getFolderName());

            for (String projectName : _createdProjects)
                assertLinkNotPresentWithText(projectName);
            log("========= " + getClass().getSimpleName() + " cleanup complete =========");
        }
        finally
        {
            if (tearDown)
                tearDown();
        }
    }

    private boolean skipCleanup()
    {
        return "false".equals(System.getProperty("clean"));
    }

    public boolean enableLinkCheck()
    {
        return "true".equals(System.getProperty("linkCheck")) || enableInjectCheck();
    }

	public boolean enableInjectCheck()
	{
		return "true".equals(System.getProperty("injectCheck"));
	}

    public boolean enableScriptCheck()
    {
        return "true".equals(System.getProperty("scriptCheck"));
    }

    public boolean enableDevMode()
    {
        return "true".equals(System.getProperty("devMode"));
    }

    public boolean skipLeakCheck()
    {
        return "false".equals(System.getProperty("memCheck"));
    }

    public boolean isMaintenanceDisabled()
    {
        return "never".equals(System.getProperty("systemMaintenance"));
    }

    public boolean isGuestModeTest()
    {
        return false;
    }

    public void checkLeaksAndErrors()
    {
        if ( isGuestModeTest() )
            return;
		checkErrors();
		checkLeaks();
        _checkedLeaksAndErrors = true;
    }

    public void checkLeaks()
    {
		if (!getTargetServer().equals(DEFAULT_TARGET_SERVER))
			return;
        if (skipLeakCheck())
            return;
        if (isGuestModeTest())
            return;

        log("Starting memory leak check...");
        int leakCount = MAX_LEAK_LIMIT + 1;

        for (int attempt = 0; attempt < GC_ATTEMPT_LIMIT && leakCount > MAX_LEAK_LIMIT; attempt++)
        {
            if (attempt > 0)
            {
                log("Found " + leakCount + " in-use objects; rerunning GC.  ("
                        + (GC_ATTEMPT_LIMIT - attempt) + " attempt(s) remaining.)");
            }
            beginAt("/admin/memTracker.view?gc=1&clearCaches=1", 120000);
            if (!isTextPresent("In-Use Objects"))
                fail("Asserts must be enabled to track memory leaks; please add -ea to your server VM params and restart.");
            leakCount = getImageWithAltTextCount("expand/collapse");
        }

        if (leakCount > MAX_LEAK_LIMIT)
        {
            String leaks = selenium.getText(Locator.xpath("//table[@name = 'leaks']").toString());
            CRC32 crc = new CRC32();
            crc.update(leaks.getBytes());

            if (leakCRC != crc.getValue())
            {
                leakCRC = crc.getValue();
                dumpHeap();
                fail(leakCount + " in-use objects exceeds allowed limit of " + MAX_LEAK_LIMIT + ".");
            }

            log("Found " + leakCount + " in-use objects.  They appear to be from a previous test.");
        }
        else
            log("Found " + leakCount + " in-use objects.  This is within the expected number of " + MAX_LEAK_LIMIT + ".");
    }


    public void checkErrors()
    {
		if (!getTargetServer().equals(DEFAULT_TARGET_SERVER))
			return;
        if ( isGuestModeTest() )
            return;
        beginAt("/admin/showErrorsSinceMark.view");

        assertTrue("There were errors during the test run", isPageEmpty());
        log("No new errors found.");
        goToHome();         // Don't leave on an empty page
    }


    public void checkExpectedErrors(int count)
    {
        // Need to remember our location or the next test could start with a blank page
        pushLocation();
        beginAt("/admin/showErrorsSinceMark.view");

        //IE and Firefox have different notions of empty.
        //IE returns html for all pages even empty text...
        String text = selenium.getHtmlSource();
        if (null == text)
            text = "";
        text = text.trim();
        if ("".equals(text))
        {
            text = selenium.getText("//body");
            if (null == text)
                text = "";
            text = text.trim();
        }

        assertTrue("Expected " + count + " errors during this run", StringUtils.countMatches(text, "ERROR") == count);
        log("Found " + count + " expected errors.");

        // Clear the errors to prevent the test from failing.
        resetErrors();

        popLocation();
    }


    private void checkActionCoverage()
    {
        if ( isGuestModeTest() )
            return;
        int rowCount, coveredActions, totalActions;
        Double actionCoveragePercent;
        String actionCoveragePercentString;
        beginAt("/admin/actions.view");

        rowCount = getTableRowCount(ACTION_SUMMARY_TABLE_NAME);
        if (getTableCellText(ACTION_SUMMARY_TABLE_NAME, rowCount - 1, 0).equals("Total"))
        {
            totalActions = Integer.parseInt(getTableCellText(ACTION_SUMMARY_TABLE_NAME, rowCount - 1, 1));
            coveredActions = Integer.parseInt(getTableCellText(ACTION_SUMMARY_TABLE_NAME, rowCount - 1, 2));
            actionCoveragePercentString = getTableCellText(ACTION_SUMMARY_TABLE_NAME, rowCount - 1, 3);
            actionCoveragePercent =  Double.parseDouble(actionCoveragePercentString.substring(0, actionCoveragePercentString.length() - 1 ));
            writeActionStatistics(totalActions, coveredActions, actionCoveragePercent);
        }

        // Download full action coverage table and add to TeamCity artifacts.
        beginAt("/admin/exportActions.view?asWebPage=true");
        publishArtifact(saveTsv(Runner.getDumpDir(), "ActionCoverage"));
    }

    private void writeActionStatistics(int totalActions, int coveredActions, Double actionCoveragePercent)
    {
        // TODO: Create static class for managing teamcity-info.xml file.
        FileWriter writer = null;
        try
        {
            File xmlFile = new File(getLabKeyRoot(), "teamcity-info.xml");
            xmlFile.createNewFile();
            writer = new FileWriter(xmlFile);

            writer.write("<build>\n");
            writer.write("\t<statisticValue key=\"totalActions\" value=\"" + totalActions + "\"/>\n");
            writer.write("\t<statisticValue key=\"coveredActions\" value=\"" + coveredActions + "\"/>\n");
            writer.write("\t<statisticValue key=\"actionCoveragePercent\" value=\"" + actionCoveragePercent + "\"/>\n");
            writer.write("</build>");
        }
        catch (IOException e)
        {
            return;
        }
        finally
        {
            if (writer != null)
                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                }
        }
    }

    public void dump()
    {
        try
        {
            FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMddHHmm");
            String baseName = dateFormat.format(new Date()) + getClass().getSimpleName();

            File dumpDir = new File(Runner.getDumpDir(), getClass().getSimpleName());
            if ( !dumpDir.exists() )
                dumpDir.mkdirs();

            publishArtifact(dumpFullScreen(dumpDir, baseName));
            publishArtifact(dumpScreen(dumpDir, baseName));
            publishArtifact(dumpHtml(dumpDir, baseName));
        }
        catch (Exception e)
        {
            log("Error executing dump()");
        }
    }

    public void dumpHeap()
    {
		if (!getTargetServer().equals(DEFAULT_TARGET_SERVER))
			return;
        if ( isGuestModeTest() )
            return;
        pushLocation();
        try
        {
            // Use dumpHeapAction rather that touching file so that we can get file name and publish artifact.
            beginAt("/admin/dumpHeap.view");
            File destDir = new File(Runner.getDumpDir(), getClass().getSimpleName());
            String dumpMsg = selenium.getText("xpath=//td[@id='bodypanel']/div");
            String filename = dumpMsg.substring(dumpMsg.indexOf("HeapDump_"));
            File heapDump = new File(getLabKeyRoot() + "/build/deploy", filename);
            File destFile = new File(destDir, filename);

            if (!destDir.exists())
                destDir.mkdirs();
            if ( heapDump.renameTo(destFile) )
                publishArtifact(destFile);
            else
                log("Unable to move HeapDump file to test logs directory.");
        }
        catch (Exception e)
        {
            log("Error dumping heap: " + e.getMessage());
        }
        popLocation(); // go back to get screenshot if needed.
    }

    public void dumpThreads()
    {
		if (!getTargetServer().equals(DEFAULT_TARGET_SERVER))
			return;

        try
        {
            File threadDumpRequest = new File(getLabKeyRoot() + "/build/deploy", "threadDumpRequest");
            threadDumpRequest.setLastModified(System.currentTimeMillis()); // Touch file to trigger automatic thread dump.
        }
        catch (Exception e)
        {
            log("Error dumping threads: " + e.getMessage());
        }

        log("Threads dumped to standard labkey log file");
    }

    // Publish artifacts while the build is still in progress:
    // http://www.jetbrains.net/confluence/display/TCD4/Build+Script+Interaction+with+TeamCity#BuildScriptInteractionwithTeamCity-PublishingArtifactswhiletheBuildisStillinProgress
    public void publishArtifact(File file)
    {
        if (file != null && System.getProperty("teamcity.buildType.id") != null)
        {
            // relativize path to labkey project root
            String labkeyRoot = WebTestHelper.getLabKeyRoot();
            labkeyRoot = new File(labkeyRoot).getAbsolutePath();
            String strFile = file.getAbsolutePath();
            if (labkeyRoot != null && strFile.toLowerCase().startsWith(labkeyRoot.toLowerCase()))
            {
                String path = strFile.substring(labkeyRoot.length());
                if (path.startsWith(File.separator))
                    path = path.substring(1);
                System.out.println("##teamcity[publishArtifacts '" + path + "']");
            }
        }
    }

    public File dumpScreen(File dir, String baseName)
    {
        File screenFile = new File(dir, baseName + ".png");
        try
        {
            selenium.captureEntirePageScreenshot(screenFile.getAbsolutePath(), "");
            return screenFile;
        }
        catch (SeleniumException se)
        {
            // too bad.
            log("Failed to take screenshot using selenium.captureEntirePageScreenshot: " + se.getMessage());

            try
            {
                selenium.windowFocus();
                selenium.windowMaximize();
                selenium.captureScreenshot(screenFile.getAbsolutePath());
                return screenFile;
            }
            catch (SeleniumException se2)
            {
                // so sad.
                log("Failed to take screenshot using selenium.captureScreenshot: " + se2.getMessage());
            }
        }

        return null;
    }

    public File dumpFullScreen(File dir, String baseName)
    {
        File screenFile = new File(dir, baseName + "Fullscreen.png");

        try
        {
            // capture entire screen
            BufferedImage fullscreen = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            ImageIO.write(fullscreen, "png", screenFile);

            return screenFile;
        }
        catch (Exception e)
        {
            log("Failed to take full screenshot: " + e.getMessage());
        }

        return null;
    }

    public File dumpHtml(File dir, String baseName)
    {
        FileWriter writer = null;
        try
        {
            File htmlFile = new File(dir, baseName + ".html");
            writer = new FileWriter(htmlFile);
            writer.write(getLastPageText());
            return htmlFile;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        finally
        {
            if (writer != null)
                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
        }
    }

    public File saveTsv(File dir, String baseName)
    {
        FileWriter writer = null;
        try
        {
            File tsvFile = new File(dir, baseName + ".tsv");
            writer = new FileWriter(tsvFile);
            writer.write(selenium.getBodyText());
            return tsvFile;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        finally
        {
            if (writer != null)
                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
        }
    }

    public String getBaseURL()
    {
        return WebTestHelper.getBaseURL();
    }

    public static String stripContextPath(String url)
    {
        String root = WebTestHelper.getContextPath() + "/";
        int rootLoc = url.indexOf(root);
        int endOfAction = url.indexOf("?");
        if ((rootLoc != -1) && (endOfAction == -1 || rootLoc < endOfAction))
            url = url.substring(rootLoc + root.length());
        else if (url.indexOf("/") == 0)
            url = url.substring(1);
        return url;
    }

    public void beginAt(String relativeURL)
    {
        beginAt(relativeURL, defaultWaitForPage);
    }

    public void beginAt(String relativeURL, int millis)
    {
        relativeURL = stripContextPath(relativeURL);
        if (relativeURL.length() == 0)
            log("Navigating to root");
        else
        {
            log("Navigating to " + relativeURL);
            if (relativeURL.charAt(0) != '/')
            {
                relativeURL = "/" + relativeURL;
            }
        }
        selenium.open(getBaseURL() + relativeURL, millis);
    }

    public String getContainerId(String url)
    {
        pushLocation();
        beginAt(url);
        String containerId = selenium.getEval("selenium.getContainerId()");
        popLocation();
        return containerId;
    }

    public String getConfirmationAndWait()
    {
        String confirmation = selenium.getConfirmation();
        waitForPageToLoad();
        return confirmation;
    }

    public void assertConfirmation(String msg)
    {
        assertEquals(msg, selenium.getConfirmation());
    }

    public void assertAlert(String msg)
    {
        assertEquals(msg, selenium.getAlert());
    }

    public void dismissAlerts()
    {
        boolean present = false;
        while (selenium.isAlertPresent())
            log("Found unexpected alert: " + selenium.getAlert());
    }

    public void logJavascriptAlerts()
    {
        while (selenium.isAlertPresent())
        {
            log("JavaScript Alert Ignored: " + selenium.getAlert());
        }
    }

	public boolean isAlertPresent()
	{
		return selenium.isAlertPresent();
	}

	public String getAlert()
	{
		return selenium.getAlert();
	}

    public void assertExtMsgBox(String title, String text)
    {
        String actual = ExtHelper.getExtMsgBoxText(this, title);
        assertTrue("Expected Ext.Msg box text '" + text + "', actual '" + actual + "'", actual.indexOf(text) != -1);
    }

    public enum SeleniumEvent
    {blur,change,mousedown,mouseup,click,reset,select,submit,abort,error,load,mouseout,mouseover,unload}

    public void fireEvent(Locator loc, SeleniumEvent event)
    {
        selenium.fireEvent(loc.toString(), event.toString());
    }

    public void createProject(String projectName)
    {
        createProject(projectName, null);
    }

    public void createProject(String projectName, String folderType)
    {
        ensureAdminMode();
        log("Creating project with name " + projectName);
        if (isLinkPresentWithText(projectName))
            fail("Cannot create project; A link with text " + projectName + " already exists.  " +
                    "This project may already exist, or its name appears elsewhere in the UI.");
        clickLinkWithText("Create Project");
        setText("name", projectName);
        if (null != folderType)
            checkRadioButton("folderType", folderType);
        else
            checkRadioButton("folderType", "None");
        submit();
        clickNavButton("Save and Finish");
        _createdProjects.add(projectName);
    }

    public void createPermissionsGroup(String groupName)
    {
        log("Creating permissions group " + groupName);
        if (!isElementPresent(Locator.permissionRendered()))
            enterPermissionsUI();
        waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
        ExtHelper.clickExtTabContainingText(this, "Groups for project");
        setFormElement("newGroupForm$input",groupName);
        clickButton("Create New Group", 0);
        sleep(500);
        waitAndClick(Locator.xpath("//div[@id='userInfoPopup']//div[contains(@class,'x-tool-close')]"));
        waitForElement(Locator.xpath("//div[@id='groupsFrame']//div[contains(@class,'pGroup') and text()='" + groupName + "']"), WAIT_FOR_JAVASCRIPT);
    }

    public void createPermissionsGroup(String groupName, String... memberNames)
    {
        log("Creating permissions group " + groupName);
        if (!isElementPresent(Locator.permissionRendered()))
            enterPermissionsUI();
        waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
        ExtHelper.clickExtTabContainingText(this, "Groups for project");
        setFormElement("newGroupForm$input",groupName);
        clickButton("Create New Group", 0);
        sleep(500);

        StringBuilder namesList = new StringBuilder();
        for(String member : memberNames)
        {
            namesList.append(member).append("\n");
        }

        log("Adding\n" + namesList.toString() + " to group " + groupName + "...");
        waitAndClick(Locator.tagContainingText("a","manage group"));
        waitForPageToLoad();
        setFormElement("names", namesList.toString());
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");
        enterPermissionsUI();
    }


    public void clickManageGroup(String groupName)
    {
        // warning Adminstrators can apper multiple times
        waitAndClick(Locator.xpath("//div[@id='groupsFrame']//div[contains(text()," + Locator.xq(groupName) + ")]"));
        sleep(100);
        waitAndClick(Locator.tagContainingText("a","manage group"));
        waitForPageToLoad();
    }


    public void clickManageSiteGroup(String groupName)
    {
        // warning Adminstrators can apper multiple times
        waitAndClick(Locator.xpath("//div[@id='siteGroupsFrame']//div[contains(text()," + Locator.xq(groupName) + ")]"));
        sleep(100);
        waitAndClick(Locator.tagContainingText("a","manage group"));
        waitForPageToLoad();
    }


    public void createSubfolder(String project, String child, String[] tabsToAdd)
    {
        // create a child of the top-level project folder:
        createSubfolder(project, project, child, "None", tabsToAdd);
    }

    public void createSubfolder(String project, String parent, String child, String folderType, String[] tabsToAdd)
    {
        createSubfolder(project, parent, child, folderType, tabsToAdd, false);
    }

    public void createSubfolder(String project, String parent, String child, String folderType, String[] tabsToAdd, boolean inheritPermissions)
    {
        ensureAdminMode();
        if (isLinkPresentWithText(child))
            fail("Cannot create folder; A link with text " + child + " already exists.  " +
                    "This folder may already exist, or the name appears elsewhere in the UI.");
        assertLinkNotPresentWithText(child);
        log("Creating subfolder " + child + " under project " + parent);
        String _active = (!parent.equals(project)? parent : project);
        clickLinkWithText(_active);
        clickLinkWithText("Folders");
        // click last index, since this text appears in the nav tree
        waitForExtFolderTreeNode(_active, 10000);
        clickNavButton("Create Subfolder");
        setText("name", child);
        checkRadioButton("folderType", folderType);
        submit();

        _createdFolders.add(new FolderIdentifier(project, child));
        if (!"None".equals(folderType))
        {
            if (inheritPermissions)
            {
                checkInheritedPermissions();
                savePermissions();
            }
            waitAndClickNavButton("Save and Finish"); //Leave permissions where they are
            if (null == tabsToAdd || tabsToAdd.length == 0)
                return;

            clickLinkWithText("Folder Settings");
        }
        // verify that we're on the customize tabs page, then submit:
        assertTextPresent("Folder Settings: /" + project);

        if (tabsToAdd != null)
        {
            for (String tabname : tabsToAdd)
                checkCheckbox(Locator.checkboxByTitle(tabname));
        }

        submit();
        if ("None".equals(folderType))
        {
            if (inheritPermissions)
            {
                checkInheritedPermissions();
                savePermissions();
            }
            waitAndClickNavButton("Save and Finish");

            if (tabsToAdd != null)
            {
                for (String tabname : tabsToAdd)
                    assertTabPresent(tabname);
            }
        }

        // verify that there's a link to our new folder:
        assertLinkPresentWithText(child);
    }

    protected void deleteDir(File dir)
    {
        if (!dir.exists())
            return;

        try
        {
            FileUtils.deleteDirectory(dir);
        }
        catch (IOException e)
        {
            log("WARNING: Exception deleting directory -- " + e.getMessage());
        }
    }

    public void deleteFolder(String project, String folderName)
    {
        log("Deleting folder " + folderName + " under project " + project);
        clickLinkWithText(project);
        clickLinkWithText(folderName);
        ensureAdminMode();
        clickLinkWithText("Folders");
        waitForExtFolderTreeNode(folderName, 10000);
        clickNavButton("Delete");
        // confirm delete:
        clickNavButton("Delete");
        // verify that we're not on an error page with a check for a project link:
        assertLinkPresentWithText(project);
        assertLinkNotPresentWithText(folderName);
    }

    public void renameFolder(String project, String folderName, String newFolderName, boolean createAlias)
    {
        log("Renaming folder " + folderName + " under project " + project + " -> " + newFolderName);
        clickLinkWithText(project);
        clickLinkWithText(folderName);
        ensureAdminMode();
        clickLinkWithText("Folders");
        waitForExtFolderTreeNode(folderName, 10000);
        clickNavButton("Rename");
        setText("name", newFolderName);
        if (createAlias)
            checkCheckbox("addAlias");
        else
            uncheckCheckbox("addAlias");
        // confirm rename:
        clickNavButton("Rename");
        // verify that we're not on an error page with a check for a new folder link:
        assertLinkPresentWithText(newFolderName);
        assertLinkNotPresentWithText(folderName);
    }

    public void moveFolder(String projectName, String folderName, String newParent, boolean createAlias)
    {
        log("Moving folder [" + folderName + "] under project [" + projectName + "] to [" + newParent + "]");
        clickLinkWithText(projectName);
        clickLinkWithText(folderName);
        ensureAdminMode();
        clickLinkWithText("Folders");
        waitForExtFolderTreeNode(folderName, 10000);
        clickNavButton("Move");
        if (createAlias)
            checkCheckbox("addAlias");
        else
            uncheckCheckbox("addAlias");
        // Select Target
        selectFolderTreeItem(newParent);
        // move:
        clickNavButton("Confirm Move");
        // verify that we're not on an error page with a check for folder link:
        assertLinkPresentWithText(folderName);
        assertLinkPresentWithText(newParent);
    }

    public void deleteProject(String project)
    {
        log("Deleting project " + project);
        clickLinkWithText(project);
        //Delete even if terms of use is required
        if (isElementPresent(Locator.name("approvedTermsOfUse")))
        {
            clickCheckbox("approvedTermsOfUse");
            clickNavButton("Agree");
        }
        ensureAdminMode();

        clickLinkWithText("Folders");
        clickNavButton("Delete");
        // in case there are sub-folders
        if (isNavButtonPresent("Delete All Folders"))
        {
            clickNavButton("Delete All Folders");
        }
        // confirm delete:
        clickNavButton("Delete");
        // verify that we're not on an error page with a check for a project link:
        assertLinkNotPresentWithText(project);
    }

    public void enableEmailRecorder()
    {
        log("Enable email recorder");
        pushLocation();
        goToHome();
        goToModule("Dumbster");
        waitForElement(Locator.checkboxByName("emailRecordOn"), WAIT_FOR_JAVASCRIPT);
        if ( initialRecorderSetting == null )
            initialRecorderSetting = getFormElement("emailRecordOn");
        uncheckCheckbox("emailRecordOn");
        checkCheckbox("emailRecordOn");
        popLocation();
    }

    public void disableEmailRecorder()
    {
        log("Disable email recorder");
        pushLocation();
        goToModule("Dumbster");
        waitForElement(Locator.checkboxByName("emailRecordOn"), WAIT_FOR_JAVASCRIPT);
        if ( initialRecorderSetting == null )
            initialRecorderSetting = getFormElement("emailRecordOn");
        uncheckCheckbox("emailRecordOn");
        popLocation();
    }

    private static String initialRecorderSetting = null;
    public void resetEmailRecorder()
    {
        if ( initialRecorderSetting.equals("true") )
            enableEmailRecorder();
        else if ( initialRecorderSetting.equals("false") )
            disableEmailRecorder();
    }

    public void addWebPart(String webPartName)
    {
        Locator.XPathLocator selects = Locator.xpath("//form[contains(@action,'addWebPart.view')]//tr/td/select[@name='name']");

        for (int i = 0; i <= 1; i++)
        {
            Locator loc = selects.index(i);
            String[] labels = selenium.getSelectOptions(loc.toString());
            for (String label : labels)
            {
                if (label.equals(webPartName))
                {
                    selenium.select(loc.toString(), webPartName);
                    submit(Locator.xpath("//form[contains(@action,'addWebPart.view')]").index(i));
                    return;
                }
            }
        }

        throw new RuntimeException("Could not find webpart with name: " + webPartName);
    }


    public boolean isTitleEqual(String match)
    {
        return match.equals(selenium.getTitle());
    }

    public void assertTitleEquals(String match)
    {
        assertEquals("Wrong page title", match, selenium.getTitle());
    }

    public void assertTitleContains(String match)
    {
        String title = selenium.getTitle();
        assertTrue("Page title: '"+title+"' doesn't contain '"+match+"'", title.contains(match));
    }

    public boolean isFormPresent(String form)
    {
        boolean present = isElementPresent(Locator.tagWithName("form", form));
        if (!present)
            present = isElementPresent(Locator.tagWithId("form", form));

        return present;
    }

    public void assertFormPresent(String form)
    {
        assertTrue("Form '" + form + "' was not present", isFormPresent(form));
    }

    public void assertNoLabkeyErrors()
    {
        assertElementNotPresent(Locator.xpath("//div[@class='labkey-error']"));
        assertElementNotPresent(Locator.xpath("//font[@class='labkey-error']"));
    }

    public void assertLabkeyErrorPresent()
    {
        assertTrue("No errors found", isElementPresent(Locator.xpath("//div[@class='labkey-error']")) ||
            isElementPresent(Locator.xpath("//font[@class='labkey-error']")));

    }

    public boolean isTextPresent(String text)
    {
        //Need to unencode here? Selenium turns &nbsp; into space???
        text = text.replace("&nbsp;", " ");
        return selenium.isTextPresent(text);
    }

    public String getText(Locator elementLocator)
    {
        return selenium.getText(elementLocator.toString());
    }

    /** Verifies that all the strings are present in the page */
    public void assertTextPresent(String... texts)
    {
        for (String text : texts)
        {
            text = text.replace("&nbsp;", " ");
            assertTrue("Text '" + text + "' was not present", isTextPresent(text));
        }
    }

    public void assertTextPresent(String text, int amount)
    {
        assertTextPresent(text, amount, false);
    }

    public void assertTextPresent(String text, int amount, boolean browserDependent)
    {
        // IE doesn't getHtmlSource the same as Firefox, it replaces \t and \n with spaces, so skip if IE
        if (!getBrowserType().equals(IE_BROWSER))
        {
            int count = countText(text);

            if (browserDependent)
            {
                if (count == 0)
                    log("Your browser is probably out of date");
                else
                    assertTrue("Text '" + text + "' was not present " + amount + " times.  It was present " + count + " times", count == amount);
            }
            else
                assertTrue("Text '" + text + "' was not present " + amount + " times.  It was present " + count + " times", count == amount);
        }
    }

    public int countText(String text)
    {
        text = text.replace("&nbsp;", " ");
        String html = selenium.getHtmlSource();
        // Strip all JavaScript tags; in particular, the selenium-injected javascript tag, which can foul up the expected occurrences
        String source = html.replaceAll("(?msi)<script type=\"text/javascript\">.*?</script>", "");
        int current_index = 0;
        int count = 0;

        while ((current_index = source.indexOf(text, current_index + 1)) != -1)
            count++;
        return count;
    }

    public void assertTextNotPresent(String text)
    {
        text = text.replace("&nbsp;", " ");
        assertFalse("Text '" + text + "' was present", isTextPresent(text));
    }

    public String getTextInTable(String dataRegion, int row, int column)
    {
        return selenium.getText("//table[@id='"+dataRegion+"']/tbody/tr["+row+"]/td["+column+"]");
    }

    public void assertTextAtPlaceInTable(String textToCheck, String dataRegion, int row, int column)
    {
       assertTrue(textToCheck+" is not at that place in the table", textToCheck.compareTo(getTextInTable(dataRegion, row, column))==0);
    }

    /**
     * Searches only the displayed text in the body of the page, not the HTML source.
     */
    public boolean isTextBefore(String text1, String text2)
    {
        String source = selenium.getBodyText();
        return (source.indexOf(text1) < source.indexOf(text2));
    }

    // Searches only the displayed text in the body of the page, not the HTML source.
    public void assertTextPresentInThisOrder(String... text)
    {
        String source = selenium.getBodyText();
        int previousIndex = -1;
        String previousString = null;

        for (String s : text)
        {
            int index = source.indexOf(s);

            assertTrue("'" + s + "' is not present", index > -1);
            assertTrue("'" + previousString + "' appears after '" + s + "'", index > previousIndex);
            previousIndex = index;
            previousString = s;
        }
    }

    public void assertTextBefore(String text1, String text2)
    {
        assertTextPresentInThisOrder(text1, text2);
    }

    public void waitForPageToLoad(int millis)
    {
        if( selenium.isAlertPresent() )
            fail("ERROR: Unexpected alert.\n" + selenium.getAlert());
        else
        {
            _testTimeout = true;
            selenium.waitForPageToLoad(Integer.toString(millis));
            _testTimeout = false;
        }
    }

    public void waitForPageToLoad()
    {
        waitForPageToLoad(defaultWaitForPage);
    }


    public void waitForExtReady()
    {
        waitForElement(Locator.id("seleniumExtReady"), defaultWaitForPage);
    }

    public void waitFor(Checker checker, String failMessage, int wait)
    {
        int time = 0;
        while ( time < wait )
        {
            if( checker.check() )
                return;
            sleep(100);
            time += 100;
        }
        if (!checker.check())
        {
            _testTimeout = true;
            fail(failMessage);
        }
    }

    public void waitForExtMaskToDisappear()
    {
        waitForExtMaskToDisappear( WAIT_FOR_JAVASCRIPT );
    }

    public void waitForExtMaskToDisappear(int wait)
    {
        waitForElementToDisappear(Locator.xpath("//div[contains(@class, 'ext-el-mask') and contains(@style, 'display: block')]"), wait);
    }

    public void waitForExtMask()
    {
        waitForExtMask( WAIT_FOR_JAVASCRIPT );
    }

    public void waitForExtMask(int wait)
    {
        waitForElement(Locator.xpath("//div[contains(@class, 'ext-el-mask') and contains(@style, 'display: block')]"), wait);
    }

    protected File getTestTempDir()
    {
        File buildDir = new File(getLabKeyRoot(), "build");
        return new File(buildDir, "testTemp");
    }

    public boolean isREngineConfigured()
    {
        // need to allow time for the server to return the engine list and the ext grid to render
        Locator engine = Locator.xpath("//div[@id='enginesGrid']//td//div[.='R,r']");
        int time = 0;
        while (!isElementPresent(engine) && time < WAIT_FOR_JAVASCRIPT)
        {
            sleep(100);
            time += 100;
        }
        return isElementPresent(engine);
    }

    public void mouseClick(String locator)
    {
        selenium.mouseClick(locator);
    }

    protected void setSelectedFields(String containerPath, String schema, String query, String viewName, String[] fields)
    {
        pushLocation();
        beginAt("/query" + containerPath + "/internalNewView.view");
        setFormElement("ff_schemaName", schema);
        setFormElement("ff_queryName", query);
        if (viewName != null)
            setFormElement("ff_viewName", viewName);
        submit();
        StringBuilder strFields = new StringBuilder(fields[0]);
        for (int i = 1; i < fields.length; i ++)
        {
            strFields.append("&");
            strFields.append(fields[i]);
        }
        setFormElement("ff_columnList", strFields.toString());
        submit();
        popLocation();
    }

    protected void clickExportToText()
    {
        clickNavButton("Export", 0);
        selenium.mouseDown("//a//span[contains(text(), \"Text\")]");
        clickNavButton("Export to Text");
    }

    public interface Checker
    {
        public boolean check();
    }

    public void waitForExtFolderTreeNode(String nodeText, int wait)
    {
        final Locator locator = new Locator("//a[contains(@class, 'x-tree-node-anchor')]/span[text() = " + Locator.xq(nodeText) + "]");
        String failMessage = "Ext NodeTree with locator " + locator + " did not appear in [" + wait + "ms]";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return isElementPresent(locator);
            }
        }, failMessage, wait);
    }
    
    public void waitForElement(final Locator locator, int wait)
    {
        String failMessage = "Element with locator " + locator + " did not appear [" + wait + "ms]";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return isElementPresent(locator);
            }
        }, failMessage, wait);
    }

    public void waitForElementToDisappear(final Locator locator, int wait)
    {
        String failMessage = "Element with locator " + locator + " was still present after [" + wait + "ms]";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return !isElementPresent(locator);
            }
        }, failMessage, wait);
    }

    public void waitForText(final String text, int wait)
    {
        String failMessage = text + " did not appear";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return isTextPresent(text);
            }
        }, failMessage, wait);
    }

    protected final String firstForm = "//td[@id='bodypanel']//form[1]";

    public void submit()
    {
        selenium.submit(firstForm);
        waitForPageToLoad();
    }

    public void submit(Locator formLocator)
    {
        selenium.submit(formLocator.toString());
        waitForPageToLoad();
    }

    public void submit(String buttonName)
    {
        Locator l = findButton(buttonName);

        assertTrue("Button with name '" + buttonName + "' not found", null != l);

        selenium.click(l.toString());
        waitForPageToLoad();
    }

    public Locator findButton(String name)
    {
        // Note: we do not use inputs anymore, but instead links spans inside
        Locator l = Locator.tagWithName("a", name);
        if (isElementPresent(l))
            return l;

        return null;
    }

    public boolean isElementPresent(Locator loc)
    {
        try
        {
            return selenium.isElementPresent(loc.toString());
        }
        catch(SeleniumException e)
        {
            /*ignore permission denied errors in IE when page refreshes during this check*/
        }
        return false;
    }

    public void assertElementPresent(Locator loc)
    {
        assertTrue("Element '" + loc + "' is not present", isElementPresent(loc));
    }

    public void assertElementPresent(Locator.XPathLocator loc, int amount)
    {
        assertEquals("Xpath '" + loc.getPath() + "' not present expected number of times.", amount, getXpathCount(loc));
    }

    public void assertElementContains(Locator loc, String text)
    {
        String elemText = selenium.getText(loc.toString());
        if(elemText == null)
            fail("The element at location " + loc.toString() + " contains no text! Expected '" + text + "'.");
        if(!elemText.contains(text))
            fail("The element at location '" + loc.toString() + "' contains '" + elemText + "'; expected '" + text + "'.");
    }

    public boolean elementContains(Locator loc, String text)
    {
        String elemText = selenium.getText(loc.toString());
        return (elemText != null && elemText.contains(text));
    }

    public void assertFormElementEquals(String elementName, String value)
    {
        assertFormElementEquals(new Locator(elementName), value);
    }

    public String getFormElement(Locator loc)
    {
        return selenium.getValue(loc.toString());
    }

    public String getFormElement(String elementName)
    {
        Locator loc = new Locator(elementName);
        return selenium.getValue(loc.toString());
    }

    public void assertFormElementEquals(Locator loc, String value)
    {
        assertElementPresent(loc);
        assertEquals("Form element '" + loc + "' was not equal to '" + value + "'", value, selenium.getValue(loc.toString()));
    }

    public void assertFormElementNotEquals(Locator loc, String value)
    {
        assertElementPresent(loc);
        assertNotSame("Form element '" + loc + "' was equal to '" + value + "'", value, selenium.getValue(loc.toString()));
    }


    public boolean isFormElementPresent(String elementName)
    {
        return isElementPresent(Locator.dom(firstForm + "['" + elementName + "']"));
    }

    public void assertFormElementPresent(String elementName)
    {
        assertTrue("Form element '" + elementName + "' was not present", isFormElementPresent(elementName));
    }


    public void assertOptionEquals(String selectName, String value)
    {
        assertOptionEquals(new Locator(selectName), value);
    }

    public void assertOptionEquals(Locator loc, String value)
    {
        assertElementPresent(loc);
        assertEquals("Option '" + loc + "' was not equal '" + value + "'", selenium.getSelectedLabel(loc.toString()), value);
    }

    public String getSelectedOptionText(Locator loc)
    {
        return selenium.getSelectedLabel(loc.toString());
    }

    public String getSelectedOptionValue(Locator loc)
    {
        return selenium.getSelectedValue(loc.toString());
    }

    public String getSelectedOptionText(String selectName)
    {
        return getSelectedOptionText(new Locator(selectName));
    }

    public void assertElementNotPresent(Locator loc)
    {
        assertFalse("Element was present in page: " + loc, isElementPresent(loc));
    }

    public void assertElementNotVisible(Locator loc)
    {
        assertFalse("Element was visible in page: " + loc, selenium.isVisible(loc.toString()));
    }

    public boolean isLinkPresent(String linkId)
    {
        return isElementPresent(Locator.tagWithId("a", linkId));
    }

    public void assertLinkPresent(String linkId)
    {
        assertTrue("Link with id '" + linkId + "' was not present", isLinkPresent(linkId));
    }

    public void assertLinkNotPresent(String linkId)
    {
        assertFalse("Link with id '" + linkId + "' was present", isLinkPresent(linkId));
    }

    public boolean isLinkPresentWithText(String text)
    {
        log("Checking for link with exact text '" + text + "'");
        return isElementPresent(Locator.linkWithText(text));
    }

    public boolean isLinkPresentWithTextCount(String text, int count)
    {
        log("Checking for " + count + " links with exact text '" + text + "'");
        return countLinksWithText(text) == count;
    }

    // TODO: Clarify or fix this.  Requires number of links > index!?
    public boolean isLinkPresentWithText(String text, int index)
    {
        return countLinksWithText(text) > index;
    }

    public boolean isLinkPresentContainingText(String text)
    {
        log("Checking for link containing text '" + text + "'");
        return isElementPresent(Locator.linkContainingText(text));
    }

    public void assertLinkPresentContainingText(String text)
    {
        assertTrue("Could not find link containing text '" + text + "'", isLinkPresentContainingText(text));
    }

    public void assertLinkPresentWithText(String text)
    {
        assertTrue("Could not find link with text '" + text + "'", isLinkPresentWithText(text));
    }

    public void assertLinkNotPresentWithText(String text)
    {
        assertFalse("Found a link with text '" + text + "'", isLinkPresentWithText(text));
    }

    public boolean isLinkPresentWithTitle(String title)
    {
        log("Checking for link with exact title '" + title + "'");
        return isElementPresent(Locator.linkWithTitle(title));
    }

    public void assertLinkPresentWithTitle(String title)
    {
        assertTrue("Could not find link with title '" + title + "'", isLinkPresentWithTitle(title));
    }

    public void assertLinkNotPresentWithTitle(String title)
    {
        assertFalse("Found a link with title '" + title + "'", isLinkPresentWithTitle(title));
    }

    /** Find a link with the exact text specified, click it, and wait for the page to load */
    public void clickLinkWithText(String text)
    {
        assertLinkPresentWithText(text);
        clickLinkWithText(text, true);
    }

    /** Find nth link with the exact text specified, click it, and wait for the page to load */
    public void clickLinkWithText(String text, int index)
    {
        Locator l = Locator.linkWithText(text, index);
        assertElementPresent(l);
        clickAndWait(l, defaultWaitForPage);
    }

    /** Find a link with the exact text specified and click it, optionally waiting for the page to load */
    public void clickLinkWithText(String text, boolean wait)
    {
        clickLinkWithText(text, 0, wait);
    }

    /** Find nth link with the exact text specified and click it, optionally waiting for the page to load */
    public void clickLinkWithText(String text, int index, boolean wait)
    {
        clickLinkWithText(text, index, wait ? defaultWaitForPage: 0);
    }

    /** Find nth link with the exact text specified, click it, and wait up to millis for the page to load */
    public void clickLinkWithText(String text, int index, int millis)
    {
        log("Clicking link with text '" + text + "'");
        Locator l;

        if (index > 0)
            l = Locator.linkWithText(text, index);
        else
            l = Locator.linkWithText(text);

        assertElementPresent(l);
        clickAndWait(l, millis);
    }

    public void clickLinkContainingText(String text)
    {
        clickLinkContainingText(text, true);
    }

    public void clickLinkContainingText(String text, int index)
    {
        clickLinkContainingText(text, index, true);
    }

    public void clickLinkContainingText(String text, int index, boolean wait)
    {
        log("Clicking link " + index + " containing text: " + text);
        Locator l = Locator.linkContainingText(text, index);
        if ( wait )
            clickAndWait(l, defaultWaitForPage);
        else
            click(l);
    }

    public void clickLinkContainingText(String text, boolean wait)
    {
        log("Clicking link containing text: " + text);
        Locator l  = Locator.linkContainingText(text);
        if ( wait )
            clickAndWait(l, defaultWaitForPage);
        else
            click(l);
    }

    public int countLinksWithText(String text)
    {
        return selenium.getXpathCount("//a[text() = '"+text+"']").intValue();
    }

    public void assertLinkPresentWithTextCount(String text, int count)
    {
        assertEquals("Link with text '" + text + "' was not present the expected number of times", count, countLinksWithText(text));
    }

    public boolean isLinkPresentWithImage(String imageName)
    {
        return isElementPresent(Locator.linkWithImage(imageName));
    }

    public void assertLinkPresentWithImage(String imageName)
    {
        assertTrue("Link with image '" + imageName + "' was not present", isLinkPresentWithImage(imageName));
    }

    public void assertLinkNotPresentWithImage(String imageName)
    {
        assertFalse("Link with image '" + imageName + "' was present", isLinkPresentWithImage(imageName));
    }

    public void clickLinkWithImage(String image)
    {
        clickLinkWithImage(image, defaultWaitForPage);
    }

    public void clickLinkWithImage(String image, int millis)
    {
        log("Clicking link with image: " + image);
        clickAndWait(Locator.linkWithImage(image), millis);
    }

    public void clickLinkWithImageByIndex(String image, int index)
    {
        clickLinkWithImageByIndex(image, index, true);
    }

    public void clickLinkWithImageByIndex(String image, int index, boolean wait)
    {
        log("Clicking link with image: " + image);
        clickAndWait(Locator.linkWithImage(image, index), wait ? defaultWaitForPage : 0);
    }

    public void click(Locator l)
    {
        clickAndWait(l, 0);
    }

    public void clickAndWait(Locator l)
    {
        clickAndWait(l, defaultWaitForPage);
    }

    public void clickAndWait(Locator l, int millis)
    {
        assertElementPresent(l);
        selenium.click(l.toString());
        if (millis > 0)
            waitForPageToLoad(millis);
    }

    public void clickLink(String linkId)
    {
        clickLink(Locator.id(linkId));
    }

    public void clickLink(Locator l)
    {
        clickAndWait(l, defaultWaitForPage);
    }

    public void selectFolderTreeItem(String folderName)
    {
        click(Locator.permissionsTreeNode(folderName));
    }

    public void mouseOut(Locator l)
    {
        selenium.mouseOut(l.toString());
    }

    public void mouseOver(Locator l)
    {
        selenium.mouseOver(l.toString());
    }

    public void mouseDown(Locator l)
    {
        selenium.mouseDown(l.toString());
    }

    public void mouseDownAt(Locator l, int x, int y)
    {
        selenium.mouseDownAt(l.toString(), x + "," + y);
    }

    public int getElementIndex(Locator l)
    {
        return selenium.getElementIndex(l.toString()).intValue();
    }

    public void dragAndDrop(Locator from, Locator to)
    {
        selenium.mouseDownAt(from.toString(), "1,1");
        selenium.mouseMoveAt(to.toString(), "1,1");
        selenium.mouseOver(to.toString());
        selenium.mouseUpAt(to.toString(), "1,1");
    }

    public enum Position
    {top, bottom}

    public void dragAndDrop(Locator from, Locator to, Position pos)
    {
        int y;
        if ( pos == Position.top )
            y = 1;
        else // pos == Position.bottom
            y = selenium.getElementHeight(to.toString()).intValue() - 1;

        selenium.mouseDownAt(from.toString(), "1,1");
        selenium.mouseMoveAt(to.toString(), "1," + y);
        selenium.mouseOver(to.toString());
        selenium.mouseUpAt(to.toString(), "1," + y);
    }

    public void clickTab(String tabname)
    {
        log("Selecting tab " + tabname);
        assertLinkPresent(getTabLinkId(tabname));
        clickLink(getTabLinkId(tabname));
    }

    public void clickImageWithAltText(String altText)
    {
        log("Clicking first image with alt text " + altText );
        Locator l = Locator.tagWithAttribute("img", "alt", altText);
        boolean present = isElementPresent(l);
        if (!present)
            fail("Unable to find image with altText " + altText);
        clickAndWait(l, defaultWaitForPage);
    }

    public int getImageWithAltTextCount(String altText)
    {
        String js = "function countImagesWithAlt(txt) {var doc=selenium.browserbot.getCurrentWindow().document; var count = 0; for (var i = 0; i < doc.images.length; i++) {if (doc.images[i].alt == txt) count++;} return count}; ";
        js = js + "countImagesWithAlt('" + altText + "');";
        String count = selenium.getEval(js);
        return Integer.parseInt(count);
    }

    public boolean isImagePresentWithSrc(String src)
    {
        return isImagePresentWithSrc(src, false);
    }

    public boolean isImagePresentWithSrc(String src, boolean substringMatch)
    {
        return isElementPresent(Locator.imageWithSrc(src, substringMatch));
    }

    public void assertImagePresentWithSrc(String src)
    {
        assertTrue(isImagePresentWithSrc(src));
    }

    public void assertImagePresentWithSrc(String src, boolean substringMatch)
    {
        assertTrue(isImagePresentWithSrc(src, substringMatch));
    }

    public String getTableCellText(String tableName, int row, int column)
    {
        return selenium.getTable(tableName + "." + row + "." + column);
    }

    public String getTableCellText(String tableName, int row, String columnTitle)
    {
        return getTableCellText(tableName, row, getColumnIndex(tableName, columnTitle));
    }

    public boolean isTableCellEqual(String tableName, int row, int column, String value)
    {
        return value.equals(getTableCellText(tableName, row, column));
    }

    public boolean isTableCellEqual(String tableName, int row, String columnTitle, String value)
    {
        return value.equals(getTableCellText(tableName, row, columnTitle));
    }

    public boolean areTableCellsEqual(String tableNameA, int rowA, int columnA, String tableNameB, int rowB, int columnB)
    {
        return getTableCellText(tableNameA, rowA, columnA).equals(getTableCellText(tableNameB, rowB, columnB));
    }

    public void assertTableCellTextEquals(String tableName, int row, int column, String value)
    {
        assertEquals(tableName + "." + String.valueOf(row) + "." + String.valueOf(column) + " != \"" + value + "\"", value, getTableCellText(tableName, row, column));
    }

    public void assertTableCellTextEquals(String tableName, int row, String columnTitle, String value)
    {
        assertTableCellTextEquals(tableName, row, getColumnIndex(tableName, columnTitle), value);
    }

    public void assertTableCellContains(String tableName, int row, int column, String... strs)
    {
        String cellAddress = tableName + "." + String.valueOf(row) +  "." + String.valueOf(column);
        String cellText = selenium.getTable(cellAddress);

        for (String str : strs)
        {
            assertTrue(cellAddress + " should contain \'" + str + "\'", cellText.contains(str));
        }
    }

    public void assertTableCellContains(String tableName, int row, String columnTitle, String... strs)
    {
        assertTableCellContains(tableName, row, getColumnIndex(tableName, columnTitle), strs);
    }

    public void assertTableCellNotContains(String tableName, int row, int column, String... strs)
    {
        String cellAddress = tableName + "." + String.valueOf(row) +  "." + String.valueOf(column);
        String cellText = selenium.getTable(cellAddress);

        for (String str : strs)
        {
            assertFalse(cellAddress + " should not contain \'" + str + "\'", cellText.contains(str));
        }
    }

    public void assertTableCellNotContains(String tableName, int row, String columnTitle, String... strs)
    {
        assertTableCellNotContains(tableName, row, getColumnIndex(tableName, columnTitle), strs);
    }

    public void assertTableCellsEqual(String tableName, int rowA, int columnA, int rowB, int columnB)
    {
        assertTableCellsEqual(tableName, rowA, columnA, tableName, rowB, columnB);
    }

    public void assertTableCellsEqual(String tableName, int rowA, String columnTitleA, int rowB, String columnTitleB)
    {
        assertTableCellsEqual(tableName, rowA, getColumnIndex(tableName, columnTitleA), tableName, rowB, getColumnIndex(tableName, columnTitleB));
    }

    public void assertTableCellsEqual(String tableNameA, int rowA, String columnTitleA, String tableNameB, int rowB, String columnTitleB)
    {
        assertTableCellsEqual(tableNameA, rowA, getColumnIndex(tableNameA, columnTitleA), tableNameB, rowB, getColumnIndex(tableNameB, columnTitleB));
    }

    public void assertTableCellsEqual(String tableNameA, int rowA, int columnA, String tableNameB, int rowB, int columnB)
    {
        assertTrue("Table cells not equal: " + tableNameA + "." + String.valueOf(rowA) + "." + String.valueOf(columnA) + " & " + tableNameB + "." + String.valueOf(rowB) + "." + String.valueOf(columnB), areTableCellsEqual(tableNameA, rowA, columnA, tableNameB, rowB, columnB));
    }

    public int getColumnIndex(String tableName, String columnTitle)
    {
        assertTextPresent(columnTitle);
        for(int col = 0; col < 100; col++) // TODO: Find out how wide the table is.
        {
            if(getTableCellText(tableName, 1, col).equals(columnTitle))
            {
                return col;
            }
        }
        return -1;
    }

    // Specifies cell values in a TSV string -- values are separated by tabs, rows are separated by \n
    public void assertTableRowsEqual(String tableId, int startRow, String cellValuesTsv)
    {
        String[] lines = cellValuesTsv.split("\n");
        String[][] cellValues = new String[lines.length][];

        for (int row = 0; row < cellValues.length; row++)
            cellValues[row] = lines[row].split("\t");

        assertTableRowsEqual(tableId, startRow, cellValues);
    }

    public void assertTableRowsEqual(String tableId, int startRow, String[][] cellValues)
    {
        for (int row = 0; row < cellValues.length; row++)
            for (int col = 0; col < cellValues[row].length; col++)
                assertTableCellTextEquals(tableId, row + startRow, col, cellValues[row][col]);
    }

    // Returns the text contents of every "Status" cell in the pipeline StatusFiles grid
    public List<String> getPipelineStatusValues()
    {
        List<String> statusValues = getTableColumnValues("dataregion_StatusFiles", 1);
        if (!statusValues.isEmpty())
            statusValues.remove(0);  // Remove the header

        return statusValues;
    }

    public void setPipelineRoot(String rootPath)
    {
        setPipelineRoot(rootPath, false);
    }

    private void dumpPipelineFiles(String path)
    {
        // moves all files under @path, created by the test, to the TeamCity publish directory
        ArrayList<File> files = listFilesRecursive(new File(path), new NonSVNFilter());
        for (File file : files)
        {
            if ( file.isFile() )
            {
                File dest = new File( Runner.getDumpDir() + "/" + getClass().getSimpleName() + "/" + file.getParent().substring(path.length()));
                if (!dest.exists())
                    dest.mkdirs();
                file.renameTo(new File(dest, file.getName()));
            }
        }
    }

    private void dumpPipelineLogFiles(String path)
    {
        // moves all .log files under @path, created by the test, to the TeamCity publish directory
        ArrayList<File> files = listFilesRecursive(new File(path), new NonSVNFilter());
        for (File file : files)
        {
            if ( file.isFile() && file.getName().endsWith(".log") )
            {
                File dest = new File( Runner.getDumpDir() + "/" + getClass().getSimpleName() + "/" + file.getParent().substring(path.length()));
                if (!dest.exists())
                    dest.mkdirs();
                file.renameTo(new File(dest, file.getName()));
            }
        }
    }

    private ArrayList<File> listFilesRecursive(File path, FilenameFilter filter)
    {
        File[] files = path.listFiles(filter);
        ArrayList<File> allFiles = new ArrayList<File>();
        if (files != null)
        {
            for (File file : files)
            {
                if ( file.isDirectory() )
                    allFiles.addAll(listFilesRecursive(file, filter));
                else // file.isFile()
                    allFiles.add(file);
            }
        }
        return allFiles;
    }

    private class NonSVNFilter implements FilenameFilter
    {
        SVNStatusClient svn = new SVNStatusClient((ISVNAuthenticationManager)null, null);

        public NonSVNFilter() { }

        public boolean accept(File directory, String filename)
        {
            File file = new File(directory, filename);
            try
            {
                return (!file.isHidden() && file.isDirectory() ||
                        _startTime < file.lastModified() && svn.doStatus(file, false).getContentsStatus().equals(SVNStatusType.STATUS_UNVERSIONED));
            }
            catch (SVNException e)
            {
                return e.getMessage().contains("is not a working copy");
            }
        }
    }

    public void setPipelineRoot(String rootPath, boolean inherit)
    {
        clickAdminMenuItem("Go To Module", "More Modules", "Pipeline");
        clickNavButton("Setup");

        if (isLinkPresentWithText("override"))
        {
            if (inherit)
                clickLinkWithText("modify the setting for all folders");
            else
                clickLinkWithText("override");
        }
        clickRadioButtonById("pipeOptionProjectSpecified");
        setFormElement("pipeProjectRootPath", rootPath);

        submit();
    }

    // Returns true if any status value is "ERROR"
    public boolean hasError(List<String> statusValues)
    {
        return statusValues.contains("ERROR");
    }

    // Returns count of "COMPLETE"
    public int getCompleteCount(List<String> statusValues)
    {
        int complete = 0;

        for (String statusValue : statusValues)
            if ("COMPLETE".equals(statusValue))
                complete++;

        return complete;
    }

    // Returns the value of all cells in the specified column
    public List<String> getTableColumnValues(String tableName, int column)
    {
        int rowCount = getTableRowCount(tableName);

        List<String> values = new ArrayList<String>(rowCount);

        for (int i = 0; i < rowCount; i++)
        {
            try
            {
                values.add(getTableCellText(tableName, i, column));
            }
            catch(Exception ignore) {}
        }

        return values;
    }

    // Returns the number of rows (both <tr> and <th>) in the specified table
    public int getTableRowCount(String tableName)
    {
        return selenium.getXpathCount("//table[@id='" + tableName + "']/thead").intValue() + selenium.getXpathCount("//table[@id='" + tableName + "']/tbody/tr").intValue();
    }

//TODO: getTableColumnCount.

    public void clickImageMapLinkByTitle(String imageMapName, String areaTitle)
    {
        clickAndWait(Locator.imageMapLinkByTitle(imageMapName, areaTitle), defaultWaitForPage);
    }

    public boolean isImageMapAreaPresent(String imageMapName, String areaTitle)
    {
        System.out.println("Checking for image map area " + imageMapName + ":" + areaTitle);
        return isElementPresent(Locator.imageMapLinkByTitle(imageMapName, areaTitle));
    }

    public void assertImageMapAreaPresent(String imageMapName, String areaTitle)
    {
        assertTrue("Image map '" + imageMapName + "' did not have an area title of '" + areaTitle + "'", isImageMapAreaPresent(imageMapName, areaTitle));
    }

    public void assertTabPresent(String tabText)
    {
        assertLinkPresent(getTabLinkId(tabText));
    }

    public void assertTabNotPresent(String tabText)
    {
        assertLinkNotPresent(getTabLinkId(tabText));
    }

    public boolean isButtonPresent(String text)
    {
        return (getButtonLocator(text) != null);
    }

    public boolean isButtonDisabled(String text)
    {
        return (isElementPresent(Locator.navButtonDisabled(text)));
    }

    public void clickButtonByIndex(String text, int index)
    {
        clickButtonByIndex(text, index, defaultWaitForPage);
    }

    public void clickButtonByIndex(String text, int index, int wait)
    {
        Locator.XPathLocator buttonLocator = getButtonLocator(text, index);
        if (buttonLocator != null)
            clickAndWait(buttonLocator, wait);
        else
            fail("No button found with text \"" + text + "\" at index " + index);
    }

    private Locator.XPathLocator getButtonLocator(String text, int index)
    {
        // check for normal labkey nav button:
        Locator.XPathLocator locator = Locator.navButton(text, index);
        if (isElementPresent(locator))
            return locator;

        // check for normal labkey submit button:
        locator = Locator.navButton(text, index);
        if (isElementPresent(locator))
            return locator;

        // check for Ext button:
        locator = Locator.extButton(text, index);
        if (isElementPresent(locator))
            return locator;

        return null;
    }

    public Locator.XPathLocator getButtonLocator(String text)
    {
        // check for narmal button:
        Locator.XPathLocator locator = Locator.button(text);
        if (isElementPresent(locator))
            return locator;

        // check for normal labkey nav button:
        locator = Locator.navButton(text);
        if (isElementPresent(locator))
            return locator;

        // check for Ext button:
        locator = Locator.extButton(text);
        if (isElementPresent(locator))
            return locator;

        return null;
    }

    private Locator.XPathLocator getButtonLocatorContainingText(String text)
    {
        // check for normal button:
        Locator.XPathLocator locator = Locator.buttonContainingText(text);
        if (isElementPresent(locator))
            return locator;

        // check for normal labkey submit/nav button:
        locator = Locator.navButtonContainingText(text);
        if (isElementPresent(locator))
            return locator;

        // check for Ext button:
        locator = Locator.extButtonContainingText(text);
        if (isElementPresent(locator))
            return locator;

        return null;
    }

    // waits for page to load after button is clicked
    // use clickButton(text, 0) to click a button and continure immediately
    public void clickButton(String text)
    {
        clickButton(text, defaultWaitForPage);
    }

    public void clickButton(String text, int waitMillis)
    {
        Locator.XPathLocator buttonLocator = getButtonLocator(text);
        if (buttonLocator != null)
            clickAndWait(buttonLocator, waitMillis);
        else
            fail("No button found with text \"" + text + "\"");
    }

    public void clickButtonContainingText(String text)
    {
        Locator.XPathLocator buttonLocator = getButtonLocatorContainingText(text);
        if (buttonLocator != null)
            clickAndWait(buttonLocator, defaultWaitForPage);
        else
            fail("No button found with text \"" + text + "\"");
    }


    public void clickNavButton(String buttonText)
    {
        clickNavButton(buttonText, defaultWaitForPage);
    }

    public void clickNavButton(String buttonText, int waitMillis)
    {
        clickButton(buttonText, waitMillis);
    }

    public void clickNavButtonByIndex(String buttonText, int index, int wait)
    {
        clickButtonByIndex(buttonText, index, wait);
    }

    public void clickNavButtonByIndex(String buttonText, int index)
    {
        clickButtonByIndex(buttonText, index);
    }


    /**
     *  wait for button to appear, click it, wait for page to load
     */
    public void waitAndClickNavButton(final String text)
    {
        String failMessage = "Button with text '" + text + "' did not appear";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return null != getButtonLocator(text);
            }
        }, failMessage, defaultWaitForPage);
        clickNavButton(text);
    }


    /**
     *  wait for element, click it, return immediately
     */
    public void waitAndClick(Locator l)
    {
        waitAndClick(10000, l, 0);
    }


    /**
     *  wait for element, click it, wait for page to load
     */
    public void waitAndClick(int waitFor, Locator l, int waitForPageToLoad)
    {
        waitForElement(l, waitFor);
        sleep(500);
        clickAndWait(l, waitForPageToLoad);
    }

    /** @return target of link */
    public String getLinkHref(String linkText, String controller, String folderPath)
    {
        Locator link = Locator.linkWithText(linkText);
        String localAddress = getButtonHref(link);
        // IE puts the entire link in href, not just the local address
        if (localAddress.contains("/"))
        {
            int location = localAddress.lastIndexOf("/");
            if (location < localAddress.length() - 1)
                localAddress = localAddress.substring(location + 1);
        }
        return (getContextPath() + "/" + controller + folderPath + "/" + localAddress);
    }


    public String getButtonHref(Locator buttonLoc)
    {
        String address = getAttribute(buttonLoc, "href");
        // IE puts the entire link in href, not just the local address
        if (address.contains("/"))
        {
            int location = address.lastIndexOf("/");
            if (location < address.length() - 1)
                address = address.substring(location + 1);
        }
        return address;
    }

    public void clickImgButtonNoNav(String buttonText)
    {
        clickNavButton(buttonText, 0);
    }

    public void setText(String elementName, String text)
    {
        if (elementName.toLowerCase().indexOf("password") >= 0)
            log("Setting text of " + elementName + " to ******");
        else
            log("Setting text of " + elementName + " to " + text);

        selenium.typeSilent(elementName, text);
    }

    public void setFormElement(String elementName, String text)
    {
        setFormElement(elementName, text, false);
    }

    public void setFormElement(String elementName, String text, boolean suppressValueLogging)
    {
        try
        {
            selenium.type(elementName, text, suppressValueLogging);
        }
        catch (SeleniumException e)
        {
             fail(e.getMessage() + "\nWarning: 'setFormElement()' is not supported for Combo Boxes in IE");
        }
    }

    public void setFormElement(String elementName, File file)
    {
        assertTrue("Test must be declared as file upload by overriding isFileUploadTest().", isFileUploadAvailable());
        selenium.type(elementName, file.getAbsolutePath());
    }

    public void setFormElement(Locator element, String text)
    {
        setFormElement(element.toString(), text);
    }

    public void setFormElement(Locator element, String text, boolean suppressValueLogging)
    {
        setFormElement(element.toString(), text, suppressValueLogging);
    }

    public void setFormElements(String tagName, String formElementName, String[] values)
    {
        for (int i = 0; i < values.length; i++)
        {
            //  (//td[@id='bodypanel']//form[1]//.[@name='inputs'])[2] BROKEN?
            //  //td[@id='bodypanel']//form[1]//descendant::textarea[@name='inputs'][1]
            setFormElement(Locator.xpath("//descendant::" + tagName + "[@name='" + formElementName + "'][" + (i+1) + "]"), values[i]);
        }
    }

    public void setSort(String regionName, String columnName, SortDirection direction)
    {
        setSort(regionName, columnName, direction, defaultWaitForPage);
    }

    //clear sort from a column
    public void clearSort(String regionName, String columnName)
    {
        clearSort(regionName, columnName, defaultWaitForPage);
    }

    public void clearSort(String regionName, String columnName, int wait)
    {
        log("Clearing sort in " + regionName + " for " + columnName);
        if (runMenuItemHandler(regionName + ":" + columnName + ":" + "clear"));
            waitForPageToLoad(wait);
    }

    public void setSort(String regionName, String columnName, SortDirection direction, int wait)
    {
        log("Setting sort in " + regionName + " for " + columnName + " to " + direction.toString());
        if (runMenuItemHandler(regionName + ":" + columnName + ":" + direction.toString().toLowerCase()))
            waitForPageToLoad(wait);
    }

    public void setFilter(String regionName, String columnName, String filterType)
    {
        log("Setting filter in " + regionName + " for " + columnName+" to " + filterType.toLowerCase());
        runMenuItemHandler(regionName + ":" + columnName + ":filter");
        selenium.select("compare_1", "label=" + filterType);
        clickNavButton("OK");
    }

    public void setFilter(String regionName, String columnName, String filterType, String filter)
    {
        log("Setting filter in " + regionName + " for " + columnName + " to " + filterType.toLowerCase() + " " + filter);
        runMenuItemHandler(regionName + ":" + columnName + ":filter");
        selenium.select("compare_1", "label=" + filterType);
        setFormElement("value_1", filter);
        clickNavButton("OK");
    }

    public void setFilterAndWait(String regionName, String columnName, String filterType, String filter, int milliSeconds)
    {
        log("Setting filter in " + regionName + " for " + columnName + " to " + filterType.toLowerCase() + " " + filter);
        runMenuItemHandler(regionName + ":" + columnName + ":filter");
        selenium.select("compare_1", "label=" + filterType);
        setFormElement("value_1", filter);
        clickNavButton("OK", milliSeconds);
    }

    public void setFilter(String regionName, String columnName, String filter1Type, String filter1, String filter2Type, String filter2)
    {
        log("Setting filter in " + regionName + " for " + columnName+" to " + filter1Type.toLowerCase() + " " + filter1 + " and " + filter2Type.toLowerCase() + " " + filter2);
        runMenuItemHandler(regionName + ":" + columnName + ":filter");
        selenium.select("compare_1", "label=" + filter1Type);
        setFormElement("value_1", filter1);
        selenium.select("compare_2", "label=" + filter2Type);
        setFormElement("value_2", filter2);
        clickNavButton("OK");
    }

    public void clearFilter(String regionName, String columnName)
    {
        log("Clearing filter in " + regionName + " for " + columnName);
        runMenuItemHandler(regionName + ":" + columnName + ":clear-filter");
        waitForPageToLoad();
    }

    /**
     * @param columnName only used to find something to click on, as all the filters on all the columns will be cleared
     */
    public void clearAllFilters(String regionName, String columnName)
    {
        log("Clearing filter in " + regionName + " for " + columnName);
        runMenuItemHandler(regionName + ":" + columnName + ":filter");
        clickNavButton("Clear All Filters");
    }

    final static int MAX_TEXT_LENGTH = 2000;

    public String getPropertyXPath(String propertyHeading)
    {
        return "//td[contains(text(), '" + propertyHeading + "')]/../..";
    }

    public int getXpathCount(Locator.XPathLocator xpath)
    {
        return selenium.getXpathCount(xpath.getPath()).intValue();
    }

    // UNDONE: move usages to use ListHelper
    public void addField(String areaTitle, int index, String name, String label, ListHelper.ListColumnType type)
    {
        String prefix = getPropertyXPath(areaTitle);
        String addField = prefix + "//span" + Locator.navButton("Add Field").getPath();
        selenium.click(addField);
        waitForElement(Locator.xpath(prefix + "//input[@name='ff_name" + index + "']"), WAIT_FOR_JAVASCRIPT);
        ListHelper.setColumnName(this, prefix, index, name);
        ListHelper.setColumnLabel(this, prefix, index, label);
        ListHelper.setColumnType(this, prefix, index, type);
    }

    // UNDONE: move usages to use ListHelper
    public void addLookupField(String areaTitle, int index, String name, String label, ListHelper.LookupInfo type)
    {
        String prefix = areaTitle==null ? "" : getPropertyXPath(areaTitle);
        String addField = prefix + "//span" + Locator.navButton("Add Field").getPath();
        selenium.click(addField);
        waitForElement(Locator.xpath(prefix + "//input[@name='ff_name" + index + "']"), WAIT_FOR_JAVASCRIPT);
        ListHelper.setColumnName(this, prefix, index, name);
        ListHelper.setColumnLabel(this, prefix, index, label);
        ListHelper.setColumnType(this, prefix, index, type);
    }

    // UNDONE: move usages to ListHelper
    public void deleteField(String areaTitle, int index)
    {
        String prefix = getPropertyXPath(areaTitle);
        selenium.mouseClick(prefix + "//div[@id='partdelete_" + index + "']");

        // If domain hasn't been saved yet, the 'OK' prompt will not appear.
        Locator.XPathLocator buttonLocator = getButtonLocator("OK");
        if (buttonLocator != null)
        {
            // Confirm the deletion
            clickNavButton("OK", 0);
            waitForElement(Locator.raw("//td/img[@id='partdeleted_" + index + "']"), WAIT_FOR_JAVASCRIPT);
        }
    }

    public void setLongTextField(String elementName, String text)
    {
        setFormElement(elementName, "");
        int offset = 0;
        text = text.replace("'", "\\'").replace("\r\n", "\\n").replace("\n", "\\n");
        while (offset < text.length())
        {
            String postString = text.substring(offset, Math.min(offset + MAX_TEXT_LENGTH, text.length()));
            if (postString.length() > 1 && postString.charAt(postString.length() - 1) == '\\' && postString.charAt(postString.length() - 2) != '\\')
                postString = postString.substring(0, postString.length() -1);

            String evalString = "selenium.appendToFormField('" + elementName + "', '" + postString + "')";
            selenium.getEval(evalString);
            offset += postString.length();
        }
    }

    public void setLongTextField(Locator loc, String text)
    {
        setLongTextField(loc.toString(), text);
    }


    public boolean isNavButtonPresent(String buttonText)
    {
        return isButtonPresent(buttonText);
    }

    public boolean isMenuButtonPresent(String buttonText)
    {
        return isButtonPresent(buttonText);
    }

    public void assertNavButtonPresent(String buttonText)
    {
        assertTrue("Nav button '" + buttonText + "' was not present", isNavButtonPresent(buttonText));
    }

    public void assertNavButtonNotPresent(String buttonText)
    {
        assertFalse("Nav button '" + buttonText + "' was present", isNavButtonPresent(buttonText));
    }

    public void assertMenuButtonPresent(String buttonText)
    {
        assertTrue("Nav button '" + buttonText + "' was not present", isMenuButtonPresent(buttonText));
    }

    public void assertMenuButtonNotPresent(String buttonText)
    {
        assertFalse("Menu button '" + buttonText + "' was present", isMenuButtonPresent(buttonText));
    }

    /**
     * Executes an Ext.menu.Item's handler.
     */
    public boolean runMenuItemHandler(String id)
    {
        log("Invoking Ext menu item handler '" + id + "'");
        //selenium.getEval("selenium.browserbot.getCurrentWindow().Ext.getCmp('" + id + "').handler();");
        String result = selenium.getEval("selenium.clickExtComponent('" + id + "');");
        if (result != null)
            return Boolean.parseBoolean(result);
        return false;
    }

    /**
     * Clicks the labkey menu item and optional submenu labels (for cascading menus)
     */
    public void clickMenuButton(String menusLabel, String ... subMenusLabels)
    {
        ExtHelper.clickMenuButton(this, true, menusLabel, subMenusLabels);
    }

    /**
     * Clicks the ext menu item and optional submenu labels's (for cascading menus)
     * Does not wait for page load.
     */
    public void clickMenuButtonAndContinue(String menusLabel, String ... subMenusLabels)
    {
        ExtHelper.clickMenuButton(this, false, menusLabel, subMenusLabels);
    }

    public void dataRegionPageFirst(String dataRegionName)
    {
        log("Clicking page first on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "First Page");
    }

    public void dataRegionPageLast(String dataRegionName)
    {
        log("Clicking page last on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "Last Page");
    }

    public void dataRegionPageNext(String dataRegionName)
    {
        log("Clicking page next on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "Next Page");
    }

    public void dataRegionPagePrev(String dataRegionName)
    {
        log("Clicking page previous on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "Previous Page");
    }

    private void clickDataRegionPageLink(String dataRegionName, String title)
    {
        clickAndWait(Locator.xpath("//table[@id='dataregion_header_" + dataRegionName + "']//div/a[@title='" + title + "']"));
    }

    public int getDataRegionRowCount(String dataRegionName)
    {
        return selenium.getXpathCount("//table[@id='dataregion_" + dataRegionName + "']/tbody/tr[contains(@class, 'labkey-row') or contains(@class, 'labkey-alternate-row')]").intValue();
    }

    /** Sets selection state for rows of the data region on the current page. */
    public void checkAllOnPage(String dataRegionName)
    {
        checkCheckbox(Locator.raw("document.forms['" + dataRegionName + "'].elements['.toggle']"));
    }

    /** Clears selection state for rows of the data region on the current page. */
    public void uncheckAllOnPage(String dataRegionName)
    {
        Locator toggle = Locator.raw("document.forms['" + dataRegionName + "'].elements['.toggle']");
        checkCheckbox(toggle);
        uncheckCheckbox(toggle);
    }

    /** Sets selection state for single rows of the data region. */
    public void checkDataRegionCheckbox(String dataRegionName, String value)
    {
        checkCheckbox(Locator.xpath("//form[@id='" + dataRegionName + "']//input[@name='.select' and @value='" + value + "']"));
    }

    /** Sets selection state for single rows of the data region. */
    public void checkDataRegionCheckbox(String dataRegionName, int index)
    {
        checkCheckbox(Locator.raw("document.forms['" + dataRegionName + "'].elements['.select'][" + index + "]"));
    }

    /** Sets selection state for single rows of the data region. */
    public void uncheckDataRegionCheckbox(String dataRegionName, int index)
    {
        uncheckCheckbox(Locator.raw("document.forms['" + dataRegionName + "'].elements['.select'][" + index + "]"));
    }

    public void toggleCheckboxByTitle(String title)
    {
        log("Clicking checkbox with title " + title);
        Locator l = Locator.checkboxByTitle(title);
        click(l);
    }

    public void clickCheckbox(String name)
    {
        click(Locator.checkboxByName(name));
    }

    public void clickRadioButtonById(String id)
    {
        click(Locator.radioButtonById(id));
    }

    public void clickCheckboxById(String id)
    {
        click(Locator.checkboxById(id));
    }

    public void checkRadioButton(String name, String value)
    {
        checkCheckbox(Locator.radioButtonByNameAndValue(name, value));
    }

    public void checkCheckbox(String name, String value)
    {
        checkCheckbox(Locator.checkboxByNameAndValue(name, value));
    }

    public void checkCheckbox(String name)
    {
        checkCheckbox(Locator.name(name));
    }

    public void checkCheckboxByNameInDataRegion(String name)
    {
        checkCheckbox(Locator.raw("//a[contains(text(), '" + name + "')]/../..//td/input"));
    }

    public void checkRadioButton(Locator radioButtonLocator)
    {
        checkCheckbox(radioButtonLocator);
    }

    public void checkCheckbox(Locator checkBoxLocator)
    {
        log("Checking checkbox " + checkBoxLocator);
       //NOTE: We don't use selenium.check() because it doesn't fire click events.
        if (!isChecked(checkBoxLocator))
            click(checkBoxLocator);
        logJavascriptAlerts();
        assertTrue("Checking checkbox failed", isChecked(checkBoxLocator));
    }

    public void checkRadioButton(String name, int index)
    {
        checkCheckbox(Locator.radioButtonByName(name).index(index));
    }

    public void checkCheckbox(String name, int index)
    {
        checkCheckbox(Locator.checkboxByName(name).index(index));
    }

    public void uncheckCheckbox(String name)
    {
        uncheckCheckbox(Locator.name(name));
    }

    public void uncheckCheckbox(String name, String value)
    {
        uncheckCheckbox(Locator.checkboxByNameAndValue(name, value));
    }

    public void uncheckCheckbox(String name, int index)
    {
        uncheckCheckbox(Locator.checkboxByName(name).index(index));
    }

    public void uncheckCheckbox(Locator checkBoxLocator)
    {
        log("Unchecking checkbox " + checkBoxLocator);
        //NOTE: We don't use selenium.uncheck() because it doesn't fire click events.
        if (isChecked(checkBoxLocator))
            click(checkBoxLocator);
        logJavascriptAlerts();
    }

    public void assertChecked(Locator checkBoxLocator)
    {
        assertTrue("Checkbox not checked at " + checkBoxLocator.toString(), isChecked(checkBoxLocator));
    }

    public void assertNotChecked(Locator checkBoxLocator)
    {
        assertFalse("Checkbox checked at " + checkBoxLocator.toString(), isChecked(checkBoxLocator));
    }

    public boolean isChecked(Locator checkBoxLocator)
    {
        return selenium.isChecked(checkBoxLocator.toString());
    }

    public void selectOptionByValue(String selectId, String value)
    {
        selenium.select(selectId, "value=" + value);
    }

    public void selectOptionByValue(Locator loc, String value)
    {
        selectOptionByValue(loc.toString(), value);
    }

    public void selectOptionByText(String selectId, String text)
    {
        selenium.select(selectId, text);
    }

    public void selectOptionByText(Locator locator, String text)
    {
        selenium.select(locator.toString(), text);
    }

    public void addUrlParameter(String parameter)
    {
        if (!getCurrentRelativeURL().contains(parameter))
            if (getCurrentRelativeURL().contains("?"))
                beginAt(getCurrentRelativeURL().concat("&" + parameter));
            else
                beginAt(getCurrentRelativeURL().concat("?" + parameter));
    }

    String toRole(String perm)
    {
        String R = "security.roles.";
        if ("No Permissions".equals(perm))
            return R + "NoPermissionsRole";
        if ("Project Administrator".equals(perm))
            return R + "ProjectAdminRole";
        else if (-1 == perm.indexOf("."))
            return R + perm + "Role";
        return perm;
    }

    public void assertNoPermission(String groupName, String permissionSetting)
    {
        String role = toRole(permissionSetting);
        waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
        assertElementNotPresent(Locator.permissionButton(groupName,role));
    }

    public void assertPermissionSetting(String groupName, String permissionSetting)
    {
        if (1==0)
        {
            log("Checking permission setting for group " + groupName + " equals " + permissionSetting);
            assertEquals("Permission for '" + groupName + "' was not '" + permissionSetting + "'", selenium.getSelectedLabel(Locator.permissionSelect(groupName).toString()), permissionSetting);
        }
        else
        {
            String role = toRole(permissionSetting);
            if ("security.roles.NoPermissionsRole".equals(role))
            {
                assertNoPermission(groupName,"Reader");
                assertNoPermission(groupName,"Editor");
                assertNoPermission(groupName,"Project Administrator");
                return;
            }
            log("Checking permission setting for group " + groupName + " equals " + role);
            waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
            assertElementPresent(Locator.permissionButton(groupName,role));
            //assertEquals("'" + groupName + "' is not in role '" + role + "'", selenium.getSelectedLabel(Locator.permissionSelect(groupName).toString()), permissionSetting);
        }
    }


    Locator inherited = Locator.name("inheritedCheckbox");
    Locator.XPathLocator inheritedParent = Locator.xpath("//input[@name='inheritedCheckbox']/..");


    public void checkInheritedPermissions()
    {
        waitForElement(Locator.permissionRendered(), defaultWaitForPage);
        waitForElement(inherited, 2000);
        if (!isChecked(inherited))
            click(inherited);
        waitForElement(Locator.permissionRendered(), defaultWaitForPage);
        assertTrue(isChecked(inherited));
    }


    public void uncheckInheritedPermissions()
    {
        waitForElement(Locator.permissionRendered(),defaultWaitForPage);
        waitForElement(inherited,1000);
        if (isChecked(inherited))
            click(inherited);
        waitForElement(Locator.permissionRendered(),defaultWaitForPage);
        assertFalse(isChecked(inherited));
    }

    public void savePermissions()
    {
        waitForElement(Locator.permissionRendered(),defaultWaitForPage);
        clickNavButton("Save", 0);
        waitForElement(Locator.permissionRendered(),defaultWaitForPage);
    }

    public void setPermissions(String groupName, String permissionString)
    {
        _setPermissions(groupName, permissionString, "pGroup");
    }

    public void setSiteGroupPermissions(String groupName, String permissionString)
    {
        _setPermissions(groupName, permissionString, "pSite");
    }

    public void setUserPermissions(String groupName, String permissionString)
    {
        _setPermissions(groupName, permissionString, "pUser");
    }

    public void _setPermissions(String groupName, String permissionString, String className)
    {
        if (1==0)
        {
            log("Setting permissions for group " + groupName + " to " + permissionString);
            //setWorkingForm("updatePermissions");
            selenium.select(Locator.permissionSelect(groupName).toString(), permissionString);
            clickNavButton("Update");
            assertPermissionSetting(groupName, permissionString);
        }
        else
        {
            if (!isElementPresent(Locator.permissionRendered()))
                enterPermissionsUI();

            String role = toRole(permissionString);
            if ("org.labkey.api.security.roles.NoPermissionsRole".equals(role))
            {
                fail("call removePermission()");
                return;
            }
            log("Setting permissions for group " + groupName + " to " + role);

            waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
            String input = "$add$" + role;
            String combo = "$combo$";
            //selenium.type(name, groupName + "\n");
            click(Locator.xpath("//td[contains(@id, '" + combo + "') and contains(@id, '" + role + "')]//img[contains(@class,'x-form-trigger')]"));
            click(Locator.xpath("//div[contains(@class,'x-combo-list') and contains(@style,'visible')]//div[contains(@class,'" + className + "') and contains(text(),'" + groupName + "')]"));
            //selenium.type(name, "\n");
            //selenium.focus("//body");
            sleep(100);
            savePermissions();
            assertPermissionSetting(groupName, permissionString);
        }
    }


    public void removeSiteGroupPermission(String groupName, String permissionString)
    {
        _removePermission(groupName, permissionString, "pSite");
    }

    public void removePermission(String groupName, String permissionString)
    {
        _removePermission(groupName, permissionString, "pGroup");
    }


    public void _removePermission(String groupName, String permissionString, String className)
    {
        if (!isElementPresent(Locator.permissionRendered()))
            enterPermissionsUI();

        String role = toRole(permissionString);
        Locator close = Locator.closePermissionButton(groupName,role);
        if (isElementPresent(close))
        {
            click(close);
            savePermissions();
            assertNoPermission(groupName, role);
        }
    }

    /**
     * Adds a new or existing user to an existing group within an existing project
     *
     * @param userName new or existing user name
     * @param projectName existing project name
     * @param groupName existing group within the project to which we should add the user
     */
    protected void addUserToProjGroup(String userName, String projectName, String groupName)
    {
        if (isElementPresent(Locator.permissionRendered()))
        {
            exitPermissionsUI();
            clickLinkWithText(projectName);
        }
        enterPermissionsUI();
        clickManageGroup(groupName);
        setFormElement("names", userName );
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");
    } //addUserToProjGroup()

    public void enterPermissionsUI()
    {
        //if the following assert triggers, you were already in the permissions UI when this was called
        assertElementNotPresent(Locator.permissionRendered());
        clickLinkWithText("Permissions");
        waitForElement(Locator.permissionRendered(), 60000);
    }

    public void exitPermissionsUI()
    {
        clickNavButton("Save and Finish");
    }


    public void impersonate(String fakeUser)
    {
        log("impersonating user : " + fakeUser);
        assertTextNotPresent("Stop Impersonating");
        ensureAdminMode();
        clickLinkWithText("Admin Console");
        selectOptionByText(Locator.id("email").toString(), fakeUser);
        clickNavButton("Impersonate");
        _impersonationStack.push(fakeUser);
    }


    public void stopImpersonating()
    {
        String fakeUser = _impersonationStack.pop();
        log("Ending impersonation");
        assertEquals(fakeUser,getDisplayName());
        clickLinkWithText("Stop Impersonating");
        assertTextPresent("Sign Out");
        goToHome();
        assertFalse(fakeUser.equals(getDisplayName()));
    }

    public void projectImpersonate(String fakeUser)
    {
        log("impersonating user at project level : " + fakeUser);
        assertTextNotPresent("Stop Impersonating");
        ensureAdminMode();
        enterPermissionsUI();
        ExtHelper.clickExtTab(this, "Impersonate");
        selectOptionByText(Locator.id("email").toString(), fakeUser);
        clickNavButton("Impersonate");
        _impersonationStack.push(fakeUser);
    }

    public void createUser(String userName, String cloneUserName)
    {
        createUser(userName, cloneUserName, true);
    }

    public void createUser(String userName, String cloneUserName, boolean verifySuccess)
    {
        ensureAdminMode();
        clickLinkWithText("Site Users");
        clickNavButton("Add Users");

        setFormElement("newUsers", userName);
        uncheckCheckbox("sendMail");
        if (cloneUserName != null)
        {
            checkCheckbox("cloneUserCheck");
            setFormElement("cloneUser", cloneUserName);
        }
        clickNavButton("Add Users");

        if (verifySuccess)
            assertTrue("Failed to add user " + userName, isTextPresent(userName + " added as a new user to the system"));
    }

    public void createUserAndNotify(String userName, String cloneUserName)
    {
        createUserAndNotify(userName, cloneUserName, true);
    }

    public void createUserAndNotify(String userName, String cloneUserName, boolean verifySuccess)
    {
        ensureAdminMode();
        clickLinkWithText("Site Users");
        clickNavButton("Add Users");

        setFormElement("newUsers", userName);
        if (cloneUserName != null)
        {
            checkCheckbox("cloneUserCheck");
            setFormElement("cloneUser", cloneUserName);
        }
        clickNavButton("Add Users");

        if (verifySuccess)
            assertTrue("Failed to add user " + userName, isTextPresent(userName + " added as a new user to the system"));
    }

    public void createSiteDeveloper(String userEmail)
    {
        ensureAdminMode();
        clickLinkWithText("Site Developers");

        if (!isElementPresent(Locator.xpath("//input[@value='" + userEmail + "']")))
        {
            setFormElement("names", userEmail);
            uncheckCheckbox("sendEmail");
            clickNavButton("Update Group Membership");
        }
    }

    public void deleteUser(String userEmail)
    {
        deleteUser(userEmail, false);
    }

    public void deleteUser(String userEmail, boolean failIfNotFound)
    {
        ensureAdminMode();
        clickLinkWithText("Site Users");
        String userXPath = "//a[text()=\"details\"]/../../td[text()=\"" + userEmail + "\"]";

        boolean isPresent = isElementPresent(new Locator(userXPath));

        // If we didn't find the user and we have more than one page, then show all pages and try again
        if (!isPresent && isLinkPresentContainingText("Next") && isLinkPresentContainingText("Last"))
        {
            clickNavButton("Page Size", 0);
            clickLinkWithText("Show All");
            isPresent = isElementPresent(new Locator(userXPath));
        }

        if (failIfNotFound)
            assertTrue(userEmail + " was not present", isPresent);

        if (isPresent)
        {
            checkCheckbox(new Locator(userXPath + "/../td[1]/input"));
            clickNavButton("Delete");
            //TODO:  this causes test failures when displayName !=userEmail.  Need a long term fix
//            assertTextPresent(userEmail);
            assertTextPresent("permanently delete");
            clickNavButton("Permanently Delete");
        }
    }

    public void assertUserExists(String email)
    {
        log("asserting that user " + email + " exists...");
        ensureAdminMode();
        clickLinkWithText("Site Users");
        assertTextPresent(email);
        log("user " + email + " exists.");
    }

    public boolean doesGroupExist(String groupName, String projectName)
    {
        ensureAdminMode();
        clickLinkWithText(projectName);
        enterPermissionsUI();
        ExtHelper.clickExtTab(this, "Groups for project " + projectName);
        boolean ret = isElementPresent(Locator.xpath("//div[contains(@class, 'pGroup') and text()='" + groupName + "']"));
        exitPermissionsUI();
        return ret;
    }

    public void assertGroupExists(String groupName, String projectName)
    {
        log("asserting that group " + groupName + " exists in project " + projectName + "...");
        if (!doesGroupExist(groupName, projectName))
            fail("group " + groupName + " does not exist in project " + projectName);
    }

    public void assertGroupDoesNotExist(String groupName, String projectName)
    {
        log("asserting that group " + groupName + " exists in project " + projectName + "...");
        if (doesGroupExist(groupName, projectName))
            fail("group " + groupName + " exists in project " + projectName);
    }

    public boolean isUserInGroup(String email, String groupName, String projectName)
    {
        ensureAdminMode();
        clickLinkWithText(projectName);
        enterPermissionsUI();
        ExtHelper.clickExtTab(this, "Groups for project " + projectName);
        click(Locator.xpath("//div[contains(@class, 'pGroup') and text()='" + groupName + "']"));
        boolean ret = isElementPresent(Locator.xpath("//div[@id='userInfoPopup']//td[text()='" + email +  "']"));
        click(Locator.xpath("//div[@id='userInfoPopup']//button[text()='Done']"));
        exitPermissionsUI();
        return ret;
    }

    public void assertUserInGroup(String email, String groupName, String projectName)
    {
        log("asserting that user " + email + " is in group " + projectName + "/" + groupName + "...");
        if (!isUserInGroup(email, groupName, projectName))
            fail("user " + email + " was not in group " + projectName + "/" + groupName);
    }

    public void assertUserNotInGroup(String email, String groupName, String projectName)
    {
        log("asserting that user " + email + " is not in group " + projectName + "/" + groupName + "...");
        if (isUserInGroup(email, groupName, projectName))
            fail("user " + email + " was found in group " + projectName + "/" + groupName);
    }

    /**
     * Saves a wiki page that is currently being created or edited. Because
     * the wiki edit page now uses AJAX to save the page, use this function to
     * reliably save the page and wait for the browser to redirect to where it would
     * normally go next.
     */
    public void saveWikiPage()
    {
        String curUrl = selenium.getLocation();

        //get the redir parameter
        String redirUrl = getUrlParam(curUrl, "redirect", true);
        if(null == redirUrl || redirUrl.length() == 0)
        {
            String pageName = getUrlParam(curUrl, "name", true);
            if(null == pageName)
                pageName = selenium.getValue("wiki-input-name");
            assert null != pageName && pageName.length() > 0;
            int idxStart = curUrl.indexOf("/wiki/");
            int idxEnd = curUrl.indexOf("/editWiki.view?", idxStart);
            redirUrl = getContextPath() + curUrl.substring(idxStart, idxEnd) + "/page.view?name=" + pageName;
        }

        log("Saving wiki...");
        clickNavButton("Save", 0);
        log("Waiting for AJAX save return...");
        //waitForText("Saved.", 10000);
        waitFor(new WikiSaveChecker(), "Wiki page failed to save!", 10000);
        //sleep(100);
        log("Navigating to " + redirUrl);
        beginAt(redirUrl);
    }

    public String getUrlParam(String url, String paramName, boolean decode)
    {
        String paramStart = paramName + "=";
        int idxStart = url.indexOf(paramStart);
        if(idxStart > 0)
        {
            idxStart += paramStart.length();
            int idxEnd = url.indexOf("&", idxStart);
            if(idxEnd < 0)
                idxEnd = url.length();
            String ret = url.substring(idxStart, idxEnd);
            if(decode)
            {
                ret = ret.replace("+", "%20");
                try {ret = URLDecoder.decode(ret, "UTF-8");} catch(UnsupportedEncodingException e) {}
            }
            return ret.trim();
        }
        else
            return null;
    }

    public class WikiSaveChecker implements Checker
    {
        private Locator _locator = Locator.id("status");
        public boolean check()
        {
            return "Saved.".equals(getText(_locator));
        }
    }

    private long start = 0;

    protected void startTimer()
    {
        start = System.currentTimeMillis();
    }

    protected int elapsedSeconds()
    {
        return (int)((System.currentTimeMillis() - start) / 1000);
    }

    /**
     * Creates a new wiki page, assuming that the [new page] link is available
     * somewhere on the current page. This link is typically displayed above
     * the Wiki table of contents, which is shown on collaboration portal pages,
     * the wiki module home page, as well as any wiki page.
     * @param format The format for the new page. Allowed values are "RADEOX" (for wiki),
     * "HTML", and "TEXT_WITH_LINKS". Note that these are the string names for the
     * WikiRendererType enum values.
     */
    public void createNewWikiPage(String format)
    {
        if(isLinkPresentWithText("new page"))
            clickLinkWithText("new page");
        else if(isLinkPresentWithText("Create a new wiki page"))
            clickLinkWithText("Create a new wiki page");
        else if(isLinkPresentWithText("add content"))
            clickLinkWithText("add content");
        else if(isTextPresent("Pages"))
            clickWebpartMenuItem("Pages", "New");
        else
            fail("Could not find a link on the current page to create a new wiki page." +
                    " Ensure that you navigate to the wiki controller home page or an existing wiki page" +
                    " before calling this method.");

        convertWikiFormat(format);
    }

    /**
     * Converts the current wiki page being edited to the specified format.
     * If the page is already in that format, it will no-op.
     * @param format The desired format ("RADEOX", "HTML", or "TEXT_WITH_LINKS")
     */
    public void convertWikiFormat(String format)
    {
        String curFormat = selenium.getEval("this.browserbot.getCurrentWindow()._wikiProps.rendererType");
        if(curFormat.equalsIgnoreCase(format))
            return;


        clickNavButton("Convert To...", 0);
        sleep(500);
        selectOptionByValue("wiki-input-window-change-format-to", format);
        clickNavButton("Convert", 0);
        sleep(500);
    }

    /**
     * Creates a new wiki page using HTML as the format. See {@link #createNewWikiPage(String)}
     * for more details.
     */
    public void createNewWikiPage()
    {
        createNewWikiPage("HTML");
    }

    /**
     * Sets the wiki page body, automatically switching to source view if necessary
     * @param body The body text to set
     */
    public void setWikiBody(String body)
    {
        switchWikiToSourceView();
        setLongTextField("body", body);
    }

    /**
     * Given a file name sets the wikiName page contents to a file in server/test/data/api
     * @param fileName file will be found in server/test/data/api
     * @param wikiName Name of the wiki where the source should be placed
     * @return The source found in the file.
     */
    public String setSourceFromFile(String fileName, String wikiName)
    {
        return setSource(getFileContents("server/test/data/api/" + fileName), wikiName);
    }

    private String setSource(String srcFragment, String wikiName)
    {
        if (!isTextPresent(wikiName))
        {
            fail("Could not find the Wiki '" + wikiName + "'. Please create the Wiki before attempting to set the source.");
        }
        clickWebpartMenuItem(wikiName, "Edit");

        setWikiBody(srcFragment);
        saveWikiPage();
        return srcFragment;
    }
    
    /**
     * Switches the wiki edit page to source view when the format type is HTML.
     */
    public void switchWikiToSourceView()
    {
        Locator sourceTab = Locator.xpath("//li[@id='wiki-tab-source']/a");
        if(null != sourceTab)
            click(sourceTab);
    }

    public void enableModule(String projectName, String moduleName)
    {
        ensureAdminMode();
        clickLinkWithText(projectName);
        enableModule(moduleName);
    }

    public void enableModule(String moduleName)
    {
        clickLinkWithText("Folder Settings");
        checkCheckbox(Locator.checkboxByTitle(moduleName));
        clickNavButton("Update Folder");
    }


    public void goToHome()
    {
        beginAt("/project/home/begin.view");
    }

    public void goToAdmin()
    {
        beginAt("/admin/showAdmin.view");
    }


    public void goToPipelineItem(String item)
    {
        int time = 0;
        while (getText(Locator.raw("//td[contains(text(),'" + item + "')]/../td[2]/a")).compareTo("WAITING") == 0
                && time < defaultWaitForPage)
        {
            sleep(100);
            time += 100;
            refresh();
        }
        clickAndWait(Locator.raw("//td[contains(text(),'" + item + "')]/../td[2]/a"));
        waitForElement(Locator.raw("//input[@value='Data']"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Data");
    }

    public List<Locator> findAllMatches(Locator.XPathLocator loc)
    {
        List<Locator> locators = new ArrayList<Locator>();
        for (int i = 0; ; i++)
        {
            if (isElementPresent(loc.index(i)))
                locators.add(loc.index(i));
            else
                return locators;
        }
    }

    protected void importStudyFromZip(String studyFile)
    {
        clickNavButton("Import Study");
        setFormElement("studyZip", studyFile);
        clickNavButton("Import Study From Local Zip Archive");
        assertTextNotPresent("You must select a .study.zip file to import.");
        waitForPipelineJobsToComplete(1, "Study import", false);
    }

    public String getFileContents(String rootRelativePath)
    {
        if (rootRelativePath.charAt(0) != '/')
            rootRelativePath = "/" + rootRelativePath;
        File file = new File(getLabKeyRoot() + rootRelativePath);
        return getFileContents(file);
    }

    public String getFileContents(File file)
    {
        FileInputStream fis = null;
        BufferedReader reader = null;
        try
        {
            fis = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder content = new StringBuilder();
            int read;
            char[] buffer = new char[1024];
            while ((read = reader.read(buffer, 0, buffer.length)) > 0)
                content.append(buffer, 0, read);
            return content.toString();
        }
        catch (IOException e)
        {
            fail(e.getMessage());
            return null;
        }
        finally
        {
            if (reader != null) try { reader.close(); } catch (IOException e) {}
            if (fis != null) try { fis.close(); } catch (IOException e) {}
        }
    }

    public void signOut()
    {
        log("Signing out");
        beginAt("/login/logout.view");
        waitForPageToLoad();
        assertLinkPresentWithText("Sign In");
    }

    /*
     * This assumes that you have added the "search" webpart to your project
     */
    public void searchFor(String projectName, String searchFor, int expectedResults, String titleName)
    {
        log("Searching Project : " + projectName + " for \"" + searchFor + "\".  Expecting to find : " + expectedResults + " results");
        clickLinkWithText(projectName);
        assertElementPresent(Locator.name("q"));
        setFormElement("query", searchFor);
        clickNavButton("Search");
        long wait = 0;
        while (wait < 5*defaultWaitForPage)
        {
            if ((titleName == null && isTextPresent("Found " + expectedResults + " result")) ||
                (titleName != null && isLinkPresentContainingText(titleName)))
                break;
            sleep(500);
            wait += 500;
            refresh();
        }
        if (titleName == null)
        {
            assertTextPresent("Found " + expectedResults + " result");
            log("found \"" + expectedResults + "\" result of " + searchFor);
        }
        else
        {
            clickLinkContainingText(titleName);
            assertTextPresent(searchFor);
        }
    }

    public void searchFor(String projectName, String searchFor, int expectedResults)
    {
        searchFor(projectName, searchFor, expectedResults, null);
    }


    public void assertAttributeEquals(Locator locator, String attributeName, String value)
    {
        String actual = getAttribute(locator, attributeName);
        assertEquals("Expected attribute '" + locator + "@" + attributeName + "' value to be '" + value + "', but was '" + actual + "' instead.", value, actual);
    }

    public void assertAttributeContains(Locator locator, String attributeName, String value)
    {
        String actual = getAttribute(locator, attributeName);
        assertTrue("Expected attribute '" + locator + "@" + attributeName + "' value to contain '" + value + "', but was '" + actual + "' instead.", actual != null && actual.contains(value));
    }

    public void assertAttributeNotContains(Locator locator, String attributeName, String value)
    {
        String actual = getAttribute(locator, attributeName);
        assertTrue("Expected attribute '" + locator + "@" + attributeName + "' value to not contain '" + value + "', but was '" + actual + "' instead.", actual != null && !actual.contains(value));
    }

    public String getAttribute(Locator locator, String attributeName)
    {
        return selenium.getAttribute(locator.toString() + "@" + attributeName);
    }

    public int getDefaultWaitForPage()
    {
        return defaultWaitForPage;
    }

    public void setDefaultWaitForPage(int defaultWaitForPage)
    {
        this.defaultWaitForPage = defaultWaitForPage;
    }

    // Return display name that's currently shown in the header
    public String getDisplayName()
    {
        return getText(Locator.id("header.user.friendlyName"));
    }


	public String getHtmlSource()
	{
		return selenium.getHtmlSource();
	}

    public boolean isExtTreeNodeSelected(String nodeCaption)
    {
        Locator loc = Locator.xpath("//div[contains(./@class,'x-tree-selected')]/a/span[text()='" + nodeCaption + "']");
        return isElementPresent(loc);
    }

    public boolean isExtTreeNodeExpanded(String nodeCaption)
    {
        Locator loc = Locator.xpath("//div[contains(./@class,'x-tree-node-expanded')]/a/span[text()='" + nodeCaption + "']");
        return isElementPresent(loc);
    }

    // Helper methods for interacting with the query schema browser
    public void selectSchema(String schemaName)
    {
        if (isExtTreeNodeSelected(schemaName))
            return;

        log("Selecting schema " + schemaName + " in the schema browser...");
        Locator loc = Locator.schemaTreeNode(schemaName);

        //first load of schemas might a few seconds
        waitForElement(loc, 30000);
        if (isExtTreeNodeExpanded(schemaName))
            click(loc);
        else
        {
            selenium.doubleClick(loc.toString());
            sleep(1000);
            click(loc);
        }
        waitForElement(Locator.xpath("//div[contains(./@class,'x-tree-selected')]/a/span[text()='" + schemaName + "']"), 1000);
    }

    public boolean isQueryPresent(String schemaName, String queryName)
    {
        return isQueryPresent(schemaName, queryName, 0);
    }

    public boolean isQueryPresent(String schemaName, String queryName, int wait)
    {
        selectSchema(schemaName);
        Locator loc = Locator.queryTreeNode(schemaName, queryName);
        try
        {
            waitForElement(loc, wait);
        }
        catch(AssertionFailedError ignore){}
        return isElementPresent(loc);
    }

    public void selectQuery(String schemaName, String queryName)
    {
        log("Selecting query " + schemaName + "." + queryName + " in the schema browser...");
        selectSchema(schemaName);
        Locator loc = Locator.queryTreeNode(schemaName, queryName);
        waitForElement(loc, WAIT_FOR_JAVASCRIPT);
        click(loc);
        waitForElement(Locator.xpath("//div[contains(./@class,'x-tree-selected')]/a/span[text()='" + queryName + "']"), 1000);
    }

    public boolean isLookupLinkPresent(String schemaName, String queryName, String pkName)
    {
        return isElementPresent(Locator.lookupLink(schemaName, queryName, pkName));
    }

    public void clickLookupLink(String schemaName, String queryName, String pkName)
    {
        click(Locator.lookupLink(schemaName, queryName, pkName));
    }

    public void clickFkExpando(String schemaName, String queryName, String columnName)
    {
        String queryLabel = schemaName + "." + queryName;
        click(Locator.xpath("//div/a[text()='" + queryLabel + "']/../../../table/tbody/tr/td/img[(contains(@src, 'plus.gif') or contains(@src, 'minus.gif')) and ../../td[text()='" + columnName + "']]"));
    }

    public void viewQueryData(String schemaName, String queryName)
    {
        selectQuery(schemaName, queryName);
        Locator loc = Locator.xpath("//div[@class='lk-qd-name']/a[text()='" + schemaName + "." + queryName + "']");
        waitForElement(loc, WAIT_FOR_JAVASCRIPT);
        String href = getAttribute(loc, "href");
        log("Navigating to " + href);
        beginAt(href);
    }

    public void editQueryProperties(String schemaName, String queryName)
    {
        selectQuery(schemaName, queryName);
        Locator loc = Locator.linkWithText("edit properties");
        waitForElement(loc, WAIT_FOR_JAVASCRIPT);
        clickAndWait(loc);
    }

    public void createNewQuery(String schemaName)
    {
        selectSchema(schemaName);
        String url = selenium.getEval("selenium.browserbot.getCurrentWindow()._browser.getCreateQueryUrl('" + schemaName + "')");
        if (null == url || url.length() == 0)
            fail("Could not get the URL for creating a new query in schema " + schemaName);
        beginAt(url);
    }

    public void validateQueries()
    {
        ExtHelper.clickExtButton(this, "Validate Queries", 0);
        Locator locButton = Locator.xpath("//button[text()='Start Validation']");
        Locator locFinishMsg = Locator.xpath("//div[contains(@class, 'lk-vq-status-all-ok') or contains(@class, 'lk-vq-status-error')]");
        waitForElement(locButton, WAIT_FOR_JAVASCRIPT);
        click(locButton);
        waitForElement(locFinishMsg, 120000);
        //test for success
        if (!isElementPresent(Locator.xpath("//div[contains(@class, 'lk-vq-status-all-ok')]")))
        {
            fail("Some queries did not pass validation. See error log for more details.");
        }
    }

    public void pressTab(String xpath)
    {
        selenium.keyDown(xpath, "\\9"); // For Windows
        selenium.keyPress(xpath, "\\9"); // For Linux
        selenium.keyUp(xpath, "\\9");
    }

    public void pressEnter(String xpath)
    {
        selenium.keyDown(xpath, "\\13"); // For Windows
        selenium.keyPress(xpath, "\\13"); // For Linux
        selenium.keyUp(xpath, "\\13");
    }

    public void pressDownArrow(String xpath)
    {
        selenium.keyDown(xpath, "\\40"); // For Windows
        selenium.keyPress(xpath, "\\40"); // For Linux
        selenium.keyUp(xpath, "\\40");
    }

    public class DefaultSeleniumWrapper extends DefaultSelenium
    {
        DefaultSeleniumWrapper()
        {
            super("localhost", getSeleniumServerPort(), getBrowser(), "http://localhost:"+getSeleniumServerPort()+"/selenium-server/RemoteRunner.html");
        }

        private void log(String s)
        {
            BaseSeleniumWebTest.this.log("selenium - " + s);
        }

        @Override
        public void click(String locator)
        {
            log("Clicking on element: " + locator);
            super.click(locator);
        }

        @Override
        public void doubleClick(String locator)
        {
            log("Double clicking on element: " + locator);
            super.doubleClick(locator);
        }

        @Override
        public void clickAt(String locator, String coordString)
        {
            log("Clicking on element " + locator + " at location " + coordString);
            super.clickAt(locator, coordString);
        }

        @Override
        public void doubleClickAt(String locator, String coordString)
        {
            log("Double clicking on element " + locator + " at location " + coordString);
            super.doubleClickAt(locator, coordString);
        }

        @Override
        public void fireEvent(String locator, String eventName)
        {
            log("Firing event " + eventName + " on element: " + locator);
            super.fireEvent(locator, eventName);
        }

        @Override
        public void keyPress(String locator, String keySequence)
        {
            log("Pressing key sequence " + keySequence + " on element: " + locator);
            super.keyPress(locator, keySequence);
        }

        @Override
        public void keyDown(String locator, String keySequence)
        {
            log("Sending key down " + keySequence + " on element " + locator);
            super.keyDown(locator, keySequence);
        }

        @Override
        public void keyUp(String locator, String keySequence)
        {
            log("Sending key up " + keySequence + " on element " + locator);
            super.keyUp(locator, keySequence);
        }

        public void mouseClick(String locator)
        {
            log("MouseClick: " + locator);
            super.mouseOver(locator);
            super.mouseDown(locator);
            super.mouseUp(locator);
        }

        @Override
        public void mouseOver(String locator)
        {
            log("MouseOver: " + locator);
            super.mouseOver(locator);
        }

        @Override
        public void mouseOut(String locator)
        {
            log("MouseOut: " + locator);
            super.mouseOut(locator);
        }

        @Override
        public void mouseDown(String locator)
        {
            log("MouseDown: " + locator);
            super.mouseDown(locator);
        }

        @Override
        public void mouseDownAt(String locator, String coordString)
        {
            log("MouseDownAt " + coordString + " for element "+ locator);
            super.mouseDownAt(locator, coordString);
        }

        @Override
        public void mouseUp(String locator)
        {
            log("MouseUp: " + locator);
            super.mouseUp(locator);
        }

        @Override
        public void mouseUpAt(String locator, String coordString)
        {
            log("MouseUpAt " + coordString + " for element "+ locator);
            super.mouseUpAt(locator, coordString);
        }

        @Override
        public void mouseMove(String locator)
        {
            log("MouseMove: "+ locator);
            super.mouseMove(locator);
        }

        @Override
        public void mouseMoveAt(String locator, String coordString)
        {
            log("MouseMoveAt " + coordString + " for element "+ locator);
            super.mouseMoveAt(locator, coordString);
        }

        public void typeSilent(String locator, String value)
        {
            super.type(locator, value);
        }

        @Override
        public void type(String locator, String value)
        {
            type(locator, value, false);
        }

        public void type(String locator, String value, boolean suppressValueLogging)
        {
            log("Set value of element " + locator + " to "+ (suppressValueLogging ? "[logging suppressed]" : value));
            super.type(locator, value);
        }

        @Override
        public void check(String locator)
        {
            log("Check: " + locator);
            super.check(locator);
        }

        @Override
        public void uncheck(String locator)
        {
            log("Uncheck: " + locator);
            super.uncheck(locator);
        }

        @Override
        public void select(String selectLocator, String optionLocator)
        {
            log("Select " + optionLocator + " from element " + selectLocator);
            super.select(selectLocator, optionLocator);
        }

        @Override
        public void addSelection(String locator, String optionLocator)
        {
            log("Add Selection " + optionLocator + " from element " + locator);
            super.addSelection(locator, optionLocator);
        }

        @Override
        public void removeSelection(String locator, String optionLocator)
        {
            log("Remove Selection " + optionLocator + " from element " + locator);
            super.removeSelection(locator, optionLocator);
        }

        @Override
        public void submit(String formLocator)
        {
            log("Submit form " + formLocator);
            super.submit(formLocator);
        }

        @Override
        public void open(String url)
        {
            open(url, BaseSeleniumWebTest.this.defaultWaitForPage);
        }

        public void open(String url, int millis)
        {
            setTimeout("" + millis);
            _testTimeout = true;
            // Workaround for selenium issue 408 http://code.google.com/p/selenium/issues/detail?id=408
            // TODO: remove workaround when we upgrade to selenium 2.x (currently in alpha)
            // super.open(url);
            commandProcessor.doCommand("open", new String[] {url,"true"});
            _testTimeout = false;
        }

        @Override
        public void openWindow(String url, String windowID)
        {
            log("Open window " + windowID + " for url " + url);
            super.openWindow(url, windowID);
        }

        @Override
        public void selectWindow(String windowID)
        {
            log("Select window " + windowID);
            super.selectWindow(windowID);
        }

        @Override
        public void selectFrame(String locator)
        {
            log("Select frame " + locator);
            super.selectFrame(locator);
        }

        @Override
        public void waitForPopUp(String windowID, String timeout)
        {
            log("Waiting " + timeout + " ms for pop up " + windowID);
            super.waitForPopUp(windowID, timeout);
        }

        @Override
        public void goBack()
        {
            log("Go back");
            super.goBack();
        }

        @Override
        public void refresh()
        {
            log("Refresh ");
            super.refresh();
        }

        @Override
        public String getConfirmation()
        {
            return super.getConfirmation();
        }

        @Override
        public String getValue(String locator)
        {
            return super.getValue(locator);
        }

        @Override
        public void dragdrop(String locator, String movementsString)
        {
            log("dragdrop element " + locator + " movements: " + movementsString);
            super.dragdrop(locator, movementsString);
        }

        @Override
        public void dragAndDrop(String locator, String movementsString)
        {
            log("dragAndDrop element " + locator + " movements: " + movementsString);
            super.dragAndDrop(locator, movementsString);
        }

        @Override
        public void dragAndDropToObject(String locatorOfObjectToBeDragged, String locatorOfDragDestinationObject)
        {
            log("dragAndDrop element " + locatorOfObjectToBeDragged + " to element " + locatorOfDragDestinationObject);
            super.dragAndDropToObject(locatorOfObjectToBeDragged, locatorOfDragDestinationObject);
        }

        @Override
        public void setCursorPosition(String locator, String position)
        {
            log("Set cursor position for " + locator + " to " + position);
            super.setCursorPosition(locator, position);
        }
    }

    // This class makes it easier to start a specimen import early in a test and wait for completion later.
    public class SpecimenImporter
    {
        private final File _pipelineRoot;
        private final File[] _specimenArchives;
        private final File _tempDir;
        private final String _studyFolderName;
        private final int _completeJobsExpected;
        private final File[] _copiedArchives;
        private boolean _expectError = false;

        public SpecimenImporter(File pipelineRoot, File specimenArchive, File tempDir, String studyFolderName, int completeJobsExpected)
        {
            this(pipelineRoot, new File[] { specimenArchive }, tempDir, studyFolderName, completeJobsExpected);
        }

        public SpecimenImporter(File pipelineRoot, File[] specimenArchives, File tempDir, String studyFolderName, int completeJobsExpected)
        {
            _pipelineRoot = pipelineRoot;
            _specimenArchives = specimenArchives;
            _tempDir = tempDir;
            _studyFolderName = studyFolderName;
            _completeJobsExpected = completeJobsExpected;

            _copiedArchives = new File[_specimenArchives.length];
            for (int i = 0; i < _specimenArchives.length; i++)
            {
                File specimenArchive = _specimenArchives[i];
                String baseName = specimenArchive.getName();
                baseName = baseName.substring(0, baseName.length() - ".specimens".length());
                _copiedArchives[i] = new File(_tempDir, baseName + "_" + FastDateFormat.getInstance("MMddHHmmss").format(new Date()) + ".specimens");
            }
        }

        public void setExpectError(boolean expectError)
        {
            _expectError = expectError;
        }

        public void importAndWaitForComplete()
        {
            startImport();
            waitForComplete();
        }

        public void startImport()
        {
            log("Starting import of specimen archive(s):");
            for (File specimenArchive : _specimenArchives)
                log("  " + specimenArchive);

            // copy the file into its own directory
            for (int i = 0; i < _specimenArchives.length; i++)
            {
                File specimenArchive = _specimenArchives[i];
                copyFile(specimenArchive, _copiedArchives[i]);
            }

            clickLinkWithText(_studyFolderName);

            int total = 0;
            while( !isLinkPresentWithText("Manage Files") && total < WAIT_FOR_PAGE)
            {
                // Loop in case test is outrunning the study creator
                sleep(250);
                total += 250;
                refresh();
            }

            clickLinkWithText("Manage Files");
            clickNavButton("Process and Import Data");
            sleep(1000);

            // TempDir is somewhere underneath the pipeline root.  Determine each subdirectory we need to navigate to reach it.
            File testDir = _tempDir;
            List<String> dirNames = new ArrayList<String>();

            while (!_pipelineRoot.equals(testDir))
            {
                dirNames.add(0, testDir.getName());
                testDir = testDir.getParentFile();
            }

            // Now navigate to the temp dir
            for (String dirName : dirNames)
                waitAndClick(Locator.fileTreeByName(dirName));

            String tempDirShortName = dirNames.get(dirNames.size() - 1);

            int seconds = 0;
            sleep(1000);

/*
            while (!isNavButtonPresent("Import specimen data") && seconds < 20)
            {
                seconds++;
                click(Locator.fileTreeByName(tempDirShortName));
                sleep(1000);
            }
*/

            for (File copiedArchive : _copiedArchives)
                ExtHelper.clickFileBrowserFileCheckbox(BaseSeleniumWebTest.this, copiedArchive.getName());
            selectImportDataAction("Import Specimen Data");
            clickNavButton("Start Import");
        }

        public void waitForComplete()
        {
            log("Waiting for completion of specimen archives");

            clickLinkWithText(_studyFolderName);
            clickLinkWithText("Manage Files");

            waitForPipelineJobsToComplete(_completeJobsExpected, "specimen import", _expectError);

            for (File copiedArchive : _copiedArchives)
                if (!copiedArchive.delete())
                    fail("Couldn't delete copied specimen archive: " + copiedArchive.getAbsolutePath());
        }
    }


    // Wait until the pipeline UI shows the requested number of complete jobs.  Fail if any job status becomes "ERROR".
    protected void waitForPipelineJobsToComplete(int completeJobsExpected, String description, boolean expectError)
    {
        log("Waiting for " + completeJobsExpected + " pipeline jobs to complete");
        List<String> statusValues = getPipelineStatusValues();

        // Short circuit in case we already have too many COMPLETE jobs
        assertTrue("Number of COMPLETE jobs already exceeds desired count", getCompleteCount(statusValues) <= completeJobsExpected);

        startTimer();

        while (getCompleteCount(statusValues) < completeJobsExpected && elapsedSeconds() < MAX_WAIT_SECONDS)
        {
            if (!expectError && hasError(statusValues))
                break;
            log("Waiting for " + description);
            sleep(1000);
            refresh();
            statusValues = getPipelineStatusValues();
        }

        if (!expectError)
            assertLinkNotPresentWithText("ERROR");  // Must be surrounded by an anchor tag.
        assertEquals(getCompleteCount(statusValues), completeJobsExpected);
    }

    // Note: unverified
    protected void waitForRunningPipelineJobs(int wait)
    {
        log("Waiting for running pipeline jobs list to be empty.");
        startTimer();
        while (elapsedSeconds() < wait)
            if ( getPipelineStatusValues().size() == 0 )
                return;

        //else
        fail("Running pipeline jobs were found.  Timeout:" + wait);
    }

    /** Turns off the fancy SQL and XML editors for custom queries and sets them to be simple text areas which are easier
     * to manipulate through the tests */
//    protected void toggleQueryEditors()
//    {
//        toggleSQLQueryEditor();
//        toggleMetadataQueryEditor();
//    }

    protected void toggleSQLQueryEditor()
    {
        toggleEditAreaOff("queryText");
    }

    protected void toggleMetadataQueryEditor()
    {
        toggleEditAreaOff("metadataText");
    }

    public void toggleScriptReportEditor()
    {
        toggleEditAreaOff("script");
    }

    public void toggleEditAreaOff(String underlyingTextAreaId)
    {
        Locator toggleCheckBoxId = Locator.id("edit_area_toggle_checkbox_" + underlyingTextAreaId);
        waitForElement(toggleCheckBoxId, WAIT_FOR_JAVASCRIPT);
        uncheckCheckbox(toggleCheckBoxId);
    }

    /**
     * For invoking pipeline actions from the file web part. Displays the import data
     * dialog and selects and submits the specified action.
     *
     * @param actionName
     */
    public void selectImportDataAction(String actionName)
    {
        sleep(100);
        ExtHelper.waitForFileGridReady(this);
        ExtHelper.waitForImportDataEnabled(this);
        clickNavButton("Import Data", 0);

        waitAndClick(Locator.xpath("//input[@type='radio' and @name='importAction']/../label[text()=" + Locator.xq(actionName) + "]"));
        String id = ExtHelper.getExtElementId(this, "btn_submit");
        clickAndWait(Locator.id(id));
    }

    public DatabaseInfo getDatabaseInfo()
    {
        pushLocation();
        ensureAdminMode();
        goToAdmin();
        assertElementPresent(Locator.id("databaseProductName"));

        DatabaseInfo info = new DatabaseInfo();
        info.serverURL = getText(Locator.id("databaseServerURL"));
        info.productName = getText(Locator.id("databaseProductName"));
        info.productVersion = getText(Locator.id("databaseProductVersion"));
        info.driverName = getText(Locator.id("databaseDriverName"));
        info.driverVersion = getText(Locator.id("databaseDriverVersion"));

        popLocation();
        return info;
    }

    public static class DatabaseInfo
    {
        public String serverURL, productName, productVersion, driverName, driverVersion;
    }
}
