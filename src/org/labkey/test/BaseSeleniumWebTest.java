/*
 * Copyright (c) 2007-2010 LabKey Corporation
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
import org.apache.commons.lang.time.FastDateFormat;
import org.labkey.test.util.Crawler;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.PasswordUtil;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
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

import static org.labkey.test.WebTestHelper.*;

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
    protected int defaultWaitForPage = 60000;
    public final static int WAIT_FOR_PAGE = 60000;
    public final static int WAIT_FOR_JAVASCRIPT = 5000;
    protected int longWaitForPage = defaultWaitForPage * 5;
    private boolean _fileUploadAvailable;
    protected long _startTime;

    private static final int MAX_SERVER_STARTUP_WAIT_SECONDS = 60;
    protected static final int MAX_WAIT_SECONDS = 10 * 60;

    public final static String FIREFOX_BROWSER = "*firefox";
    private final static String FIREFOX_UPLOAD_BROWSER = "*chrome";
    public final static String IE_BROWSER = "*iexplore";
    //protected final static String IE_UPLOAD_BROWSER = "*iehta";
    public static final String CUSTOMIZE_VIEW = "Customize View";

    /** Have we already done a memory leak and error check in this test harness VM instance? */
    private static boolean _checkedLeaksAndErrors = false;
    private static final String ACTION_SUMMARY_TABLE_NAME = "springActions";

    public BaseSeleniumWebTest()
    {

    }

    public static int getSeleniumServerPort() {
        String portString = System.getProperty("selenium.server.port", "" + DEFAULT_SELENIUM_PORT);
        return Integer.parseInt(portString);
    }

    public static int getSeleniumServer() {
        String portString = System.getProperty("selenium.server", DEFAULT_SELENIUM_SERVER);
        return Integer.parseInt(portString);
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

    public void setUp() throws Exception {
        selenium = new DefaultSeleniumWrapper();
        selenium.start();
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


    public void tearDown() throws Exception {
        if (this.enableScriptCheck())
            endJsErrorChecker();

        boolean skipTearDown = _testFailed && System.getProperty("close.on.fail", "true").equalsIgnoreCase("false");
        if (!skipTearDown)
        {
            selenium.close();
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

    public void ensureAdminMode()
    {
        //Now switch to admin mode if available
        if (!isLinkPresentWithText("Projects"))
            clickAdminMenuItem("Show Navigation Bar");
    }

    public void hideNavigationBar()
    {
        clickAndWait(Locator.xpath("//a[@class='labkey-header']/span[text() = 'Admin']"), 0);
        waitForElement(Locator.tagContainingText("span", "Navigation Bar"), 1000);
        if (isElementPresent(Locator.tagContainingText("span", "Hide Navigation Bar")))
            clickAndWait(Locator.tagContainingText("span", "Hide Navigation Bar"));
    }
    
    public void clickAdminMenuItem(String... items)
    {
        clickAndWait(Locator.xpath("//a[@class='labkey-header']/span[text() = 'Admin']"), 0);
        for (int i = 0; i < items.length - 1; i++)
        {
            Locator parentLocator = Locator.menuItem(items[i]);
            waitForElement(parentLocator, 1000);
            mouseOver(parentLocator);
        }
        Locator itemLocator = Locator.menuItem(items[items.length - 1]);
        waitForElement(itemLocator, 1000);
        clickAndWait(itemLocator);
    }

    // Click on a module listed on the admin menu
    public void goToModule(String moduleName)
    {
        clickAdminMenuItem("Go To Module", moduleName);
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

        text = selenium.getText("//body");
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

            signIn();
			resetErrors();

            DatabaseInfo info = getDatabaseInfo();
            if (!isDatabaseSupported(info))
            {
                log("** Skipping " + getClass().getSimpleName() + " test for unsupported database: " + info.productName + " " + info.productVersion);
                return;
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
				boolean injectTest = enableInjectCheck();
                Crawler crawler = new Crawler(this, Runner.getTestSet().getCrawlerTimeout());
                crawler.crawlAllLinks(injectTest);
				if (!injectTest)
					checkLeaksAndErrors();
				else
					resetErrors();
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

            try
            {
                signOut();
            }
            catch (Throwable t)
            {
                // fall through
            }
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
            finally
            {
                if (_testFailed)
                {
                    dump();
                    dumpPipelineLogFiles(getLabKeyRoot() + "/sampledata");
                }
                log("=============== Completed " + getClass().getSimpleName() + Runner.getProgress() + " =================");
            }
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

    public void checkLeaksAndErrors()
    {
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

        log("Starting memory leak check...");
        int leakCount = MAX_LEAK_LIMIT + 1;
        for (int attempt = 0; attempt < GC_ATTEMPT_LIMIT && leakCount > MAX_LEAK_LIMIT; attempt++)
        {
            if (attempt > 0)
            {
                log("Found " + leakCount + " in-use objects; rerunning GC.  ("
                        + (GC_ATTEMPT_LIMIT - attempt) + " attempt(s) remaining.)");
            }
            beginAt("/admin/memTracker.view?gc=true&clearCaches=true");
            if (!isTextPresent("In-Use Objects"))
                fail("Asserts must be enabled to track memory leaks; please add -ea to your server VM params and restart.");
            leakCount = getImageWithAltTextCount("expand/collapse");
        }

        if (leakCount > MAX_LEAK_LIMIT)
            fail(leakCount + " in-use objects exceeds allowed limit of " + MAX_LEAK_LIMIT + ".");
        else
            log("Found " + leakCount + " in-use objects.  This is within the expected number of " + MAX_LEAK_LIMIT + ".");
    }


    public void checkErrors()
    {
		if (!getTargetServer().equals(DEFAULT_TARGET_SERVER))
			return;
        beginAt("/admin/showErrorsSinceMark.view");

        assertTrue("There were errors during the test run", isPageEmpty());
        log("No new errors found.");
        goToHome();         // Don't leave on an empty page
    }


    private void checkActionCoverage()
    {
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

    // Publish artifacts while the build is still in progrss:
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
        setFormElement("newGroupForm$input",groupName);
        clickButton("Create new group", 0);
        sleep(500);
        waitAndClick(Locator.xpath("//div[@id='userInfoPopup']//div[contains(@class,'x-tool-close')]"));
        waitForElement(Locator.tagWithText("td",groupName), defaultWaitForPage);
    }

    public void createPermissionsGroup(String groupName, String... memberNames)
    {
        log("Creating permissions group " + groupName);
        if (!isElementPresent(Locator.permissionRendered()))
            enterPermissionsUI();
        waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
        setFormElement("newGroupForm$input",groupName);
        clickButton("Create new group", 0);
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
        waitAndClick(Locator.xpath("//div[@id='groupsFrame']//td[contains(text()," + Locator.xq(groupName) + ")]"));
        sleep(100);
        waitAndClick(Locator.tagContainingText("a","manage group"));
        waitForPageToLoad();
    }


    public void clickManageSiteGroup(String groupName)
    {
        // warning Adminstrators can apper multiple times
        waitAndClick(Locator.xpath("//div[@id='siteGroupsFrame']//td[contains(text()," + Locator.xq(groupName) + ")]"));
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
        clickLinkWithText(project);
        clickLinkWithText("Folders");
        // click last index, since this text appears in the nav tree
        clickLinkWithText(parent, countLinksWithText(parent) - 1);
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
                toggleCheckboxByTitle(tabname);
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
        }


        if (tabsToAdd != null)
        {
            for (String tabname : tabsToAdd)
                assertTabPresent(tabname);
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
        ensureAdminMode();
        clickLinkWithText("Folders");
        // click index 1, since this text appears in the nav tree as well as the folder management tree:
        clickLinkWithText(folderName, 1);
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
        ensureAdminMode();
        clickLinkWithText("Folders");
        // click index 1, since this text appears in the nav tree as well as the folder management tree:
        clickLinkWithText(folderName, 1);
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

    public void deleteProject(String project)
    {
        log("Deleting project " + project);
        clickLinkWithText(project);
        waitForPageToLoad();
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
        selenium.waitForPageToLoad(Integer.toString(millis));
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
        while (!checker.check() && time < wait)
        {
            sleep(100);
            time += 100;
        }
        if (!checker.check())
            fail(failMessage);
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

    public void waitForElement(final Locator locator, int wait)
    {
        String failMessage = "Element with locator " + locator + " did not appear";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return isElementPresent(locator);
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
        return selenium.isElementPresent(loc.toString());
    }

    public void assertElementPresent(Locator loc)
    {
        assertTrue("Element '" + loc + "' is not present", isElementPresent(loc));
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

    public String getSelectedOptionText(String selectName)
    {
        return getSelectedOptionText(new Locator(selectName));
    }

    public void assertElementNotPresent(Locator loc)
    {
        assertFalse("Element was present in page: " + loc, isElementPresent(loc));
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

    /** Find a link with the exact text specified, clicks it, and waits for the page to load */
    public void clickLinkWithText(String text)
    {
        assertLinkPresentWithText(text);
        clickLinkWithText(text, true);
    }

    /** Find a nth link with the exact text specified, clicks it, and waits for the page to load */
    public void clickLinkWithText(String text, int index)
    {
        Locator l = Locator.linkWithText(text, index);
        assertElementPresent(l);
        clickAndWait(l, defaultWaitForPage);
    }

    /** Find a link with the exact text specified, clicks it, optionally waiting for the page to load */
    public void clickLinkWithText(String text, boolean wait)
    {
        clickLinkWithText(text, 0, wait);
    }

    /** Find a nth link with the exact text specified, clicks it, optionally waiting for the page to load */
    public void clickLinkWithText(String text, int index, boolean wait)
    {
        log("Clicking link with text '" + text + "'");
        Locator l;

        if(index > 0)
            l = Locator.linkWithText(text, index);
        else
            l = Locator.linkWithText(text);

        assertElementPresent(l);
        if (wait)
            clickAndWait(l, defaultWaitForPage);
        else
            clickAndWait(l, 0);
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
        Locator l  = Locator.linkContainingText(text, index);
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
        //TODO: Figure out how to count with a locator. For now still need to escape javascript string...
        String js = "selenium.countLinksWithText('" + text + "');";
        String count = selenium.getEval(js);
        return Integer.parseInt(count);
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
        log("Clicking link with image: " + image);
        clickAndWait(Locator.linkWithImage(image, index), defaultWaitForPage);
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

    public void clickTab(String tabname)
    {
        log("Selecting tab " + tabname);
        assertLinkPresent(getTabLinkId(tabname));
        clickLink(getTabLinkId(tabname));
    }

    public void closeExtTab(String tabName)
    {
        log("Closing Ext tab " + tabName);
        mouseDownAt(Locator.xpath("//a[contains(@class,'x-tab-strip-close') and ..//span[contains(@class,'x-tab-strip-text') and text()='" + tabName + "']]"), 0, 0);
    }

    public void clickExtTab(String tabname)
    {
        log("Selecting Ext tab " + tabname);
        mouseDownAt(Locator.xpath("//span[contains(@class, 'x-tab-strip-text') and text() = '" + tabname + "']"), 0, 0);
    }

    public void clickExtToolbarButton(String caption)
    {
        clickExtToolbarButton(caption, defaultWaitForPage);
    }

    public void clickExtToolbarButton(String caption, int wait)
    {
        log("Clicking Ext button with caption: " + caption);
        Locator loc = Locator.xpath("//button[contains(./@class, 'x-btn-text') and text()='" + caption + "']");
        waitForElement(loc, WAIT_FOR_JAVASCRIPT);
        if (wait > 0)
            clickAndWait(loc, wait);
        else
            click(loc);
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
        assertTrue(tableName + "." + String.valueOf(row) + "." + String.valueOf(column) + " != \"" + value + "\"", isTableCellEqual(tableName, row, column, value));
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
            if(getTableCellText(tableName, 2, col).equals(columnTitle))
            {
                return col;
            }
        }
        return -1;
    }

    public void assertTableRowsEqual(String tableName, int startRow, String[][] cellValues)
    {
        for (int row = 0; row < cellValues.length; row++)
            for (int col = 0; col < cellValues[row].length; col++)
                assertTableCellTextEquals(tableName, row + startRow, col, cellValues[row][col]);
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

    private void dumpPipelineLogFiles(String path)
    {
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

    private ArrayList<File> listFilesRecursive(File path, FilenameFilter filter)
    {
        File[] files = path.listFiles(filter);
        ArrayList<File> allFiles = new ArrayList<File>();
        for (File file : files)
        {
            if ( file.isDirectory() )
                allFiles.addAll(listFilesRecursive(file, filter));
            else // file.isFile()
                allFiles.add(file);
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
        // check for normal labkey nav button:
        Locator.XPathLocator locator = Locator.navButton(text);
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
        // check for normal labkey nav button:
        Locator.XPathLocator locator = Locator.navButtonContainingText(text);
        if (isElementPresent(locator))
            return locator;

        // check for normal labkey submit button:
        locator = Locator.navButtonContainingText(text);
        if (isElementPresent(locator))
            return locator;

        // check for Ext button:
        locator = Locator.extButtonContainingText(text);
        if (isElementPresent(locator))
            return locator;

        return null;
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

    public void clickNavButtonContainingText(String buttonText)
    {
        clickButtonContainingText(buttonText);
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
        }, failMessage, 10000);
        clickNavButton(text);
    }


    /**
     *  wait for element, clickit, return immediately
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


    public String goToNavButton(String buttonText, String controller, String folderPath)
    {
        // Returns address of NavButton
        Locator navButton;
        if (isElementPresent(Locator.navButton(buttonText)))
            navButton = Locator.navButton(buttonText);
        else
            navButton = Locator.navSubmitButton(buttonText);
        Locator navButtonLink = Locator.raw(navButton.toString().concat("/.."));
        String localAddress = getButtonHref(navButtonLink);
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
        log("Setting sort in " + regionName + " for " + columnName + " to " + direction.toString());
        if (runMenuItemHandler(regionName + ":" + columnName + ":" + direction.toString().toLowerCase()))
            waitForPageToLoad(defaultWaitForPage);
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

    public void addField(String areaTitle, int index, String name, String label, String type)
    {
        String xpath = getPropertyXPath(areaTitle) + "//span" + Locator.navButton("Add Field").getPath();
        selenium.click(xpath);
        setFormElement(getPropertyXPath(areaTitle) + "//td/input[@id='ff_name" + index + "']", name);
        setFormElement(getPropertyXPath(areaTitle) + "//td/input[@id='ff_label" + index + "']", label);
        selectOptionByText(getPropertyXPath(areaTitle) + "//td/select[@id='ff_type" + index + "']", type);
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
     * Clicks the ext menu item and optional submenu labels's (for cascading menus)
     */
    public void clickMenuButton(String MenusLabel, String ... subMenusLabels)
    {
        ExtHelper.clickMenuButton(this, true, MenusLabel, subMenusLabels);
    }

    /**
     * Clicks the ext menu item and optional submenu labels's (for cascading menus)
     * Does not wait for page load.
     */
    public void clickMenuButtonAndContinue(String MenusLabel, String ... subMenusLabels)
    {
        ExtHelper.clickMenuButton(this, false, MenusLabel, subMenusLabels);
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

    public void uncheckCheckbox(Locator checkBoxLocator)
    {
        log("Unchecking checkbox " + checkBoxLocator);
        //NOTE: We don't use selenium.uncheck() because it doesn't fire click events.
        if (isChecked(checkBoxLocator))
            click(checkBoxLocator);
        logJavascriptAlerts();
    }

    public boolean isChecked(Locator checkBoxLocator)
    {
        return selenium.isChecked(checkBoxLocator.toString());
    }

    public void selectOptionByValue(String selectName, String value)
    {
        selenium.select(selectName, "value=" + value);
    }

    public void selectOptionByValue(Locator loc, String value)
    {
        selectOptionByValue(loc.toString(), value);
    }

    public void selectOptionByText(String selectName, String text)
    {
        selenium.select(selectName, text);
    }

    public void addCustomizeViewOption(String tab, String column_name)
    {
        addCustomizeViewOption(tab, column_name, column_name);
    }

    public void addCustomizeViewOption(String tab, String column_id, String column_name)
    {
        // column_id refers to the form of the name used after "column_" and is necessary to specify if it is different
        // than the column_name that appears to the user

        selenium.click(tab + ".tab");
        selenium.click("column_" + column_id);
        clickNavButton("Add >>", 0);
        int millis = 0;
        while(!selenium.isElementPresent("//div[@id='" + tab + ".list.div']//tr/td[text()='" + column_name + "']") && millis < defaultWaitForPage)
        {
            log("If this message is appearing multiple times, you probably need to specify the column_id");
            millis = millis + 100;
            sleep(100);
        }
        if (millis >= defaultWaitForPage)
            fail("Did not recognize addition of " + column_name);

    }

    public void addCustomizeViewColumn(String column_name)
    {
        addCustomizeViewColumn(column_name, column_name);
    }

    public void addCustomizeViewColumn(String column_id, String column_name)
    {
        // column_id refers to the form of the name used after "column_" and is necessary to specify if it is different
        // than the column_name that appears to the user

        log("Adding " + column_name + " column");
        addCustomizeViewOption("columns", column_id, column_name);
    }

    public void addCustomizeViewFilter(String column_name, String filter_type)
    {
        addCustomizeViewFilter(column_name, column_name, filter_type, "");
    }

    public void addCustomizeViewFilter(String column_name, String filter_type, String filter)
    {
        addCustomizeViewFilter(column_name, column_name, filter_type, filter);
    }

    public void addCustomizeViewFilter(String column_id, String column_name, String filter_type, String filter)
    {
        // column_id refers to the form of the name used after "column_" and is necessary to specify if it is different
        // than the column_name that appears to the user

        if (filter.compareTo("") == 0)
            log("Adding " + column_name + " filter of " + filter_type);
        else
            log("Adding " + column_name + " filter of " + filter_type + " " + filter);

        if (selenium.isElementPresent("//div[@id='filter.list.div']//tr/td[text()='" + column_name + "'][1]"))
            log("This test method does not support adding multiple filters of the same type");

        addCustomizeViewOption("filter", column_id, column_name);
        selenium.click("//div[@id='filter.list.div']//tr/td[text()='" + column_name + "']/../td[2]/select");
        selenium.select("//div[@id='filter.list.div']//tr/td[text()='" + column_name + "']/../td[2]/select", filter_type);

        if (filter.compareTo("") != 0)
        {
            selenium.type("//div[@id='filter.list.div']//tr/td[text()='" + column_name + "']/../td[3]/input", filter);
            selenium.fireEvent("//div[@id='filter.list.div']//tr/td[text()='" + column_name + "']/../td[3]/input", "blur");
        }
    }

    public void addCustomizeViewSort(String column_name, String order)
    {
        addCustomizeViewSort(column_name, column_name, order);
    }

    public void addCustomizeViewSort(String column_id, String column_name, String order)
    {
        // column_id refers to the form of the name used after "column_" and is necessary to specify if it is different
        // than the column_name that appears to the user

        log("Adding " + column_name + " sort");
        addCustomizeViewOption("sort", column_id, column_name);
        selenium.click("//div[@id='sort.list.div']//tr/td[text()='" + column_name + "']/../td[2]/select");
        selenium.select("//div[@id='sort.list.div']//tr/td[text()='" + column_name + "']/../td[2]/select", "label=" + order);
    }

    public void removeCustomizeViewOption(String tab, String column_name)
    {
        selenium.click(tab + ".tab");
        selenium.click("//div[@id='" + tab + ".list.div']//tr/td[text()='" + column_name + "']");
        selenium.click("//img[@alt='Delete']");
    }

    public void removeCustomizeViewColumn(String column_name)
    {
        log("Removing " + column_name + " column");
        removeCustomizeViewOption("columns", column_name);
    }

    public void removeCustomizeViewFilter(String column_name)
    {
        log("Removing " + column_name + " filter");
        selenium.click("filter.tab");

        selenium.click("//div[@id='filter.list.div']//tr/td[text()='" + column_name + "']");
        selenium.click("//img[@alt='Delete']");
    }

    public void removeCustomizeViewFilter(int filter_place)
    {
        log("Removing filter at position " + filter_place);
        selenium.click("filter.tab");
        selenium.click("//div[@id='filter.list.div']/table/tbody/tr[" + (filter_place * 2) + "]/td[1]");
        selenium.click("//img[@alt='Delete']");
    }

    public void removeCustomizeViewSort(String column_name)
    {
        log("Removing " + column_name + " sort");
        removeCustomizeViewOption("sort", column_name);
    }

    public void clearCustomizeViewFilters()
    {
        selenium.click("filter.tab");
        while (selenium.isElementPresent("//div[@id='filter.list.div']/table/tbody/tr[2]/td[1]"))
            selenium.click("//img[@alt='Delete']");
    }

    public void clearCustomizeViewSorts()
    {
        selenium.click("sort.tab");
        while (selenium.isElementPresent("//div[@id='sort.list.div']/table/tbody/tr[1]/td[1]"))
            selenium.click("//img[@alt='Delete']");
    }

    public void clearCustomizeViewColumns()
    {
        selenium.click("columns.tab");
        while (selenium.isElementPresent("//div[@id='columns.list.div']/table/tbody/tr[1]/td[1]"))
            selenium.click("//img[@alt='Delete']");
    }

    public void moveCustomizeViewColumn(String column_name, boolean moveUp)
    {
        moveCustomizeViewOption("columns", column_name, moveUp);
    }

    public void moveCustomizeViewFilter(String column_name, boolean moveUp)
    {
        moveCustomizeViewOption("filter", column_name, moveUp);
    }

    public void moveCustomizeViewSort(String column_name, boolean moveUp)
    {
        moveCustomizeViewOption("sort", column_name, moveUp);
    }

    public void moveCustomizeViewOption(String tab, String column_name, boolean moveUp)
    {
        selenium.click(tab + ".tab");
        selenium.click("//div[@id='" + tab + ".list.div']//tr/td[text()='" + column_name + "']");
        selenium.click("//td[@id='" + tab + ".controls']//a/img[@alt='Move " + (moveUp ? "Up" : "Down") + "']");
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
        String R = "org.labkey.api.security.roles.";
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
            if ("org.labkey.api.security.roles.NoPermissionsRole".equals(role))
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
            String combo = "$combo$" + role;
            //selenium.type(name, groupName + "\n");
            click(Locator.xpath("//td[@id='" + combo + "']//img[contains(@class,'x-form-trigger')]"));
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
        String role = toRole(permissionString);

        waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
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
            assertTrue("Failed to add user " + userName, isTextPresent(userName + " added as a new user to the system, but no email was sent."));
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
            assertTextPresent(userEmail);
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
        clickExtTab("Groups for project " + projectName);
        boolean ret = isElementPresent(Locator.xpath("//div[contains(@class, 'pGroup')]//td[text()='" + groupName + "']"));
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
        clickExtTab("Groups for project " + projectName);
        click(Locator.xpath("//div[contains(@class, 'pGroup')]//td[text()='" + groupName + "']"));
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
        selenium.open(redirUrl);
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
     * Switches the wiki edit page to source view when the format type is HTML.
     */
    public void switchWikiToSourceView()
    {
        Locator sourceTab = Locator.tagContainingText("td", "Source");
        if(null != sourceTab)
            click(sourceTab);
    }

    public void enableModule(String projectName, String moduleName)
    {
        ensureAdminMode();
        clickLinkWithText(projectName);
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
        while (wait < defaultWaitForPage)
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
        selenium.open(href);
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
        selenium.open(url);
    }

    public void validateQueries()
    {
        clickExtToolbarButton("Validate Queries", 0);
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

    public class DefaultSeleniumWrapper extends DefaultSelenium
    {
        DefaultSeleniumWrapper()
        {
            super("localhost", getSeleniumServerPort(), getBrowser(), WebTestHelper.getBaseURL());
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
            super.open(url);
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
        private final File _specimenArchive;
        private final File _tempDir;
        private final String _studyFolderName;
        private final int _completeJobsExpected;
        private final File _copiedArchive;

        public SpecimenImporter(File pipelineRoot, File specimenArchive, File tempDir, String studyFolderName, int completeJobsExpected)
        {
            _pipelineRoot = pipelineRoot;
            _specimenArchive = specimenArchive;
            _tempDir = tempDir;
            _studyFolderName = studyFolderName;
            _completeJobsExpected = completeJobsExpected;
            _copiedArchive = new File(_tempDir, FastDateFormat.getInstance("MMddHHmmss").format(new Date()) + ".specimens");
        }

        public void importAndWaitForComplete()
        {
            startImport();
            waitForComplete();
        }

        public void startImport()
        {
            log("Starting import of specimen archive " + _specimenArchive);

            // copy the file into its own directory
            copyFile(_specimenArchive, _copiedArchive);

            clickLinkWithText(_studyFolderName);
            clickLinkWithText("Data Pipeline");
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

            ExtHelper.selectFileBrowserFile(BaseSeleniumWebTest.this, _copiedArchive.getName());
            selectImportDataAction("Import Specimen Data");
            waitForPageToLoad();
            clickNavButton("Start Import");
        }

        public void waitForComplete()
        {
            log("Waiting for completion of specimen archive " + _specimenArchive + " import");

            clickLinkWithText(_studyFolderName);
            clickLinkWithText("Data Pipeline");

            waitForPipelineJobsToComplete(_completeJobsExpected, "specimen import");

            if (!_copiedArchive.delete())
                fail("Couldn't delete copied specimen archive");
        }
    }


    // Wait until the pipeline UI shows the requested number of complete jobs.  Fail if any job status becomes "ERROR".
    protected void waitForPipelineJobsToComplete(int completeJobsExpected, String description)
    {
        log("Waiting for " + completeJobsExpected + " pipeline jobs to complete");
        List<String> statusValues = getPipelineStatusValues();

        // Short circuit in case we already have too many COMPLETE jobs
        assertTrue("Number of COMPLETE jobs already exceeds desired count", getCompleteCount(statusValues) <= completeJobsExpected);

        startTimer();

        while (getCompleteCount(statusValues) < completeJobsExpected && !hasError(statusValues) && elapsedSeconds() < MAX_WAIT_SECONDS)
        {
            log("Waiting for " + description);
            sleep(1000);
            refresh();
            statusValues = getPipelineStatusValues();
        }

        assertLinkNotPresentWithText("ERROR");  // Must be surrounded by an anchor tag.
        assertEquals(getCompleteCount(statusValues), completeJobsExpected);
    }

    /**
     * For invoking pipeline actions from the file web part. Displays the import data
     * dialog and selects and submits the specified action.
     *
     * @param actionName
     */
    public void selectImportDataAction(String actionName)
    {
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
