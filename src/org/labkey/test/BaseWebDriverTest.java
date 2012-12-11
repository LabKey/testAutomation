/*
 * Copyright (c) 2012 LabKey Corporation
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

import com.thoughtworks.selenium.SeleniumException;
import junit.framework.AssertionFailedError;
import net.jsourcerer.webdriver.jserrorcollector.JavaScriptError;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.util.APIAssayHelper;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.AbstractAssayHelper;
import org.labkey.test.util.AbstractContainerHelper;
import org.labkey.test.util.AbstractUserHelper;
import org.labkey.test.util.ComponentQuery;
import org.labkey.test.util.Crawler;
import org.labkey.test.util.CustomizeViewsHelperWD;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.Ext4HelperWD;
import org.labkey.test.util.ExtHelperWD;
import org.labkey.test.util.ListHelperWD;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.StudyHelperWD;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.ext4cmp.Ext4CmpRefWD;
import org.labkey.test.util.ext4cmp.Ext4FieldRefWD;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import static org.labkey.test.WebTestHelper.DEFAULT_TARGET_SERVER;
import static org.labkey.test.WebTestHelper.GC_ATTEMPT_LIMIT;
import static org.labkey.test.WebTestHelper.MAX_LEAK_LIMIT;
import static org.labkey.test.WebTestHelper.getHttpGetResponse;
import static org.labkey.test.WebTestHelper.getTabLinkId;
import static org.labkey.test.WebTestHelper.getTargetServer;
import static org.labkey.test.WebTestHelper.leakCRC;
import static org.labkey.test.WebTestHelper.logToServer;

/**
 * User: Mark Igra
 * Date: Feb 7, 2007
 * Time: 5:31:38 PM
 */
public abstract class BaseWebDriverTest extends BaseSeleniumWebTest implements Cleanable, WebTest
{
    public static final String ADMIN_MENU_XPATH = "//a/span[text() = 'Admin']";
    public static final Locator USER_MENU_LOC = Locator.id("userMenuPopupLink");
    /**
     * @deprecated Refactor usages to use {@link #_driver}
     */
    @Deprecated protected DefaultSeleniumWrapper selenium;
    public WebDriver _driver; // TODO: Refactor to private with getter
    private String _lastPageTitle = null;
    private URL _lastPageURL = null;
    private String _lastPageText = null;
    private Stack<String> _locationStack = new Stack<String>();
    private String _savedLocation = null;
    private Stack<String> _impersonationStack = new Stack<String>();
    private List<WebTestHelper.FolderIdentifier> _createdFolders = new ArrayList<WebTestHelper.FolderIdentifier>();
    protected boolean _testFailed = true;
    protected boolean _testTimeout = false;
    public final static int WAIT_FOR_PAGE = 30000;
    public int defaultWaitForPage = WAIT_FOR_PAGE;
    public final static int WAIT_FOR_JAVASCRIPT = 10000;
    public int longWaitForPage = defaultWaitForPage * 5;
    private boolean _fileUploadAvailable;
    protected long _startTime;
    private ArrayList<JavaScriptError> _jsErrors;
    public WebDriverWait _shortWait; // TODO: Refactor to private with getter
    public WebDriverWait _longWait; // TODO: Refactor to private with getter

    public AbstractContainerHelper _containerHelper = new APIContainerHelper(this);
    public ExtHelperWD _extHelper = new ExtHelperWD(this);
    public Ext4HelperWD _ext4Helper = new Ext4HelperWD(this);
    public CustomizeViewsHelperWD _customizeViewsHelper = new CustomizeViewsHelperWD(this);
    public StudyHelperWD _studyHelper = new StudyHelperWD(this);
    public ListHelperWD _listHelper = new ListHelperWD(this);
    public AbstractUserHelper _userHelper = new APIUserHelper(this);
    public AbstractAssayHelper _assayHelper = new APIAssayHelper(this);

    private static final int MAX_SERVER_STARTUP_WAIT_SECONDS = 60;
    protected static final int MAX_WAIT_SECONDS = 10 * 60;

    public static final String TRICKY_CHARACTERS = "><&/%\\' \"1";
    public static final String TRICKY_CHARACTERS_NO_QUOTES = "></% 1";
    public static final String TRICKY_CHARACTERS_FOR_PROJECT_NAMES = "\u2603~!@$&()_+{}-=[],.#";

    public static final String INJECT_CHARS_1 = "\"'>--><script>alert('8(');</script>;P";
    public static final String INJECT_CHARS_2 = "\"'>--><img src=xss onerror=alert(\"8(\")>\u2639";

    public final static String FIREFOX_BROWSER = "*firefox";
    private final static String FIREFOX_UPLOAD_BROWSER = "*chrome";
    public final static String IE_BROWSER = "*iexploreproxy";
    //protected final static String IE_UPLOAD_BROWSER = "*iehta";

    /** Have we already done a memory leak and error check in this test harness VM instance? */
    protected static boolean _checkedLeaksAndErrors = false;
    private static final String ACTION_SUMMARY_TABLE_NAME = "springActions";


    protected static final String PERMISSION_ERROR = "401: User does not have permission to perform this operation";

    protected boolean isPerfTest = false;


    public BaseWebDriverTest()
    {
        _extHelper = new ExtHelperWD(this);
        _ext4Helper = new Ext4HelperWD(this);
        _listHelper = new ListHelperWD(this);
        _customizeViewsHelper = new CustomizeViewsHelperWD(this);
        _jsErrors = new ArrayList<JavaScriptError>();
    }

    protected void setIsPerfTest(boolean isPerfTest)
    {
        this.isPerfTest = isPerfTest;
    }

    protected boolean useNativeEvents()
    {
        return false;
    }

    public static String getLabKeyRoot()
    {
        return WebTestHelper.getLabKeyRoot();
    }

    public static String getSampledataPath()
    {
        File path = new File(getLabKeyRoot(), "sampledata");
        return path.toString();
    }
    public static String getContextPath()
    {
        return WebTestHelper.getContextPath();
    }

    protected abstract @Nullable String getProjectName();

    @Before
    public void setUp() throws Exception
    {
        if (getBrowser().startsWith("*ie")) //experimental
        {
            _driver = new InternetExplorerDriver();
        }
        else if (getBrowser().startsWith("*html")) //experimental
        {
            _driver = new HtmlUnitDriver(true);
        }
        else if (getBrowser().startsWith("*googlechrome")) //experimental
        {
            _driver = new ChromeDriver();
        }
        else
        {
            final FirefoxProfile profile = new FirefoxProfile();
            profile.setPreference("app.update.auto", false);
            profile.setPreference("extensions.update.autoUpdate", false);
            profile.setPreference("extensions.update.enabled", false);
            if (enableScriptCheck())
            {
                try
                    {JavaScriptError.addExtension(profile);}
                catch(IOException e)
                    {Assert.fail("Failed to load JS error checker: " + e.getMessage());}
            }
            if (firefoxExtensionsEnabled() && !onTeamCity()) // Firebug just clutters up screenshots on TeamCity
            {
                try
                {
                    profile.addExtension(new File(getLabKeyRoot() + "/server/test/selenium/firebug-1.11.0.xpi"));
                    profile.addExtension(new File(getLabKeyRoot() + "/server/test/selenium/fireStarter-0.1a6.xpi"));
                    profile.setPreference("extensions.firebug.currentVersion", "1.11.0"); // prevent upgrade spash page
                    profile.setPreference("extensions.firebug.allPagesActivation", "on");
                    profile.setPreference("extensions.firebug.previousPlacement", 3);
                    profile.setPreference("extensions.firebug.net.enabledSites", true);
                    if (firebugPanelsEnabled()) // Enabling Firebug panels slows down test and is usually not needed
                    {
                        profile.setPreference("extensions.firebug.net.enableSites", true);
                        profile.setPreference("extensions.firebug.script.enableSites", true);
                        profile.setPreference("extensions.firebug.console.enableSites", true);
                    }
                }
                catch(IOException e)
                    {Assert.fail("Failed to load Firebug: " + e.getMessage());}
            }

            profile.setEnableNativeEvents(useNativeEvents());

            _driver = new FirefoxDriver(profile);
        }

        BrowserInfo browserInfo = BrowserInfo.getBrowserInfo(this);
        String version = browserInfo.getVersion();
        log("Browser: " + browserInfo.getType() + " " + version);

        _driver.manage().timeouts().implicitlyWait(100, TimeUnit.MILLISECONDS);

        selenium = new DefaultSeleniumWrapper(_driver, WebTestHelper.getBaseURL());
        selenium.setTimeout(Integer.toString(defaultWaitForPage));

        _shortWait = new WebDriverWait(_driver, WAIT_FOR_JAVASCRIPT/1000);
        _longWait = new WebDriverWait(_driver, WAIT_FOR_PAGE/1000);
    }

    public Object executeScript(String script, Object... arguments)
    {
        JavascriptExecutor exec = (JavascriptExecutor) _driver;
        return exec.executeScript(script, arguments);
    }

    public void resetJsErrorChecker()
    {
        if (this.enableScriptCheck())
        {
            _jsErrors = new ArrayList<JavaScriptError>();
            JavaScriptError.readErrors(_driver); // clear errors
            jsCheckerPaused = false;
        }
    }

    private boolean jsCheckerPaused = false;
    private static int _jsErrorPauseCount = 0; // To keep track of nested pauses
    public void pauseJsErrorChecker()
    {
        if (this.enableScriptCheck())
        {
            _jsErrorPauseCount++;
            if (!jsCheckerPaused)
            {
                jsCheckerPaused = true;
                _jsErrors.addAll(JavaScriptError.readErrors(_driver));
            }
        }
    }

    public void resumeJsErrorChecker()
    {
        if (this.enableScriptCheck())
        {
            if (--_jsErrorPauseCount < 1 && jsCheckerPaused)
            {
                jsCheckerPaused = false;
                JavaScriptError.readErrors(_driver); // clear errors
            }
        }
    }

    /**
     * Override if using file upload features in the test. Returning true will attempt to use
     * a version of the browser that allows file upload fields to be set. Defaults to false.
     * Use isFileUploadAvailable to see if request worked.
     */
    protected boolean isFileUploadTest()
    {
        return false;
    }

    /**
     * @Deprecated WebDriver doesn't have file upload limitations
     * @return
     */
    @Deprecated
    public boolean isFileUploadAvailable()
    {
        return _fileUploadAvailable;
    }

    protected boolean isPipelineToolsTest()
    {
        return false;
    }

    /**
     * Set pipeline tools directory to the default location if the current location does not exist.
      */
    @LogMethod
    private void fixPipelineToolsDirectory()
    {
        log("Ensuring pipeline tools directory points to the right place");
        goToHome();
        goToSiteSettings();
        File currentToolsDirectory = new File(getFormElement("pipelineToolsDirectory"));
        if(!currentToolsDirectory.exists())
        {
            log("Pipeline tools directory does not exist: " + currentToolsDirectory);
            File defaultToolsDirectory = new File(getLabKeyRoot() + "/build/deploy/bin");
            log("Setting to default tools directory" + defaultToolsDirectory.toString());
            setFormElement("pipelineToolsDirectory", defaultToolsDirectory.toString());
            clickButton("Save");
        }
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

        _fileUploadAvailable = true;
        return browser + browserPath;
    }

    public void refreshIfIE()
    {
        if(getBrowser().startsWith(IE_BROWSER))
            refresh();
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

    public void copyFile(String original, String copy)
    {
        copyFile(new File(original), new File(copy));
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
            Assert.fail(e.getMessage());
        }
        finally
        {
            if (fis != null) try { fis.close(); } catch (IOException e) {}
            if (fos != null) try { fos.close(); } catch (IOException e) {}
        }
    }

    @After
    public void tearDown() throws Exception
    {
        boolean skipTearDown = _testFailed && System.getProperty("close.on.fail", "true").equalsIgnoreCase("false");
        if ((!skipTearDown || onTeamCity())&& _driver != null)
        {
            _driver.quit();
        }
    }

    public void tearDown(boolean forceTeardown) throws Exception
    {
       if (forceTeardown)
           _driver.quit();
        else
           tearDown();
    }

    private boolean validateJsError(JavaScriptError error)
    {
        return !error.getErrorMessage().contains("setting a property that has only a getter") &&
                !error.getErrorMessage().contains("records[0].get is not a function") &&
                !error.getErrorMessage().contains("{file: \"chrome://") &&
                !error.getErrorMessage().contains("ext-all-sandbox-debug.js") && // Ignore error caused by project webpart
                !error.getErrorMessage().contains("ext-all-sandbox.js") && // Ignore error that's junking up the weekly
                !error.getErrorMessage().contains("ext-all-sandbox-dev.js") && // Ignore error that's junking up the weekly
                !error.getErrorMessage().contains("XULElement.selectedIndex") && // Ignore known Firefox Issue
                !error.getErrorMessage().contains("Failed to decode base64 string!") && // Firefox issue
                !error.getErrorMessage().contains("xulrunner-1.9.0.14/components/FeedProcessor.js") && // Firefox problem
                !error.getErrorMessage().contains("Image corrupt or truncated: <unknown>");  // Selenium problem with pages that lack a favicon (e.g., errors since reset)
    }

    public void log(String str)
    {
        str = str.replace(Locator.NOT_HIDDEN, "NOT_HIDDEN"); // This xpath fragment really clutters up the log
        TestLogger.log(str);
    }

    private static final Pattern LABKEY_ERROR_TITLE_PATTERN = Pattern.compile("\\d\\d\\d\\D.*Error.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOMCAT_ERROR_PATTERN = Pattern.compile("HTTP Status\\s*(\\d\\d\\d)\\D");

    public int getResponseCode()
    {
        //We can't seem to get response codes via javascript, so we rely on default titles for error pages
        String title = _driver.getTitle();
        if (!title.toLowerCase().contains("error"))
            return 200;

        Matcher m = LABKEY_ERROR_TITLE_PATTERN.matcher(title);
        if (m.matches())
            return Integer.parseInt(title.substring(0, 3));

        //Now check the Tomcat page. This is going to be unreliable over time
        m = TOMCAT_ERROR_PATTERN.matcher(_driver.getPageSource());
        if (m.find())
            return Integer.parseInt(m.group(1));

        return 200;
    }

    public URL getURL()
    {
        try
        {
            return new URL(_driver.getCurrentUrl());
        }
        catch (MalformedURLException x)
        {
            throw new RuntimeException("Bad location from selenium tester: " + _driver.getCurrentUrl(), x);
        }
    }

    public String[] getLinkAddresses()
    {
        String js =
                "getLinkAddresses = function () {\n" +
                "        var links = window.document.links;\n" +
                "        var addresses = new Array();\n" +
                "        for (var i = 0; i < links.length; i++)\n" +
                "          addresses[i] = links[i].getAttribute('href');\n" +
                "        return addresses;\n" +
                "};" +
                "return getLinkAddresses();";
        List<String> linkArray = (ArrayList<String>)executeScript(js);
        ArrayList<String> links = new ArrayList<String>();
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


    public String getCurrentRelativeURL()
    {
        URL url = getURL();
        String urlString = _driver.getCurrentUrl();
        if ("80".equals(WebTestHelper.getWebPort()) && url.getAuthority().endsWith(":-1"))
        {
            int portIdx = urlString.indexOf(":-1");
            urlString = urlString.substring(0, portIdx) + urlString.substring(portIdx + (":-1".length()));
        }

        String baseURL = WebTestHelper.getBaseURL();
        Assert.assertTrue("Expected URL to begin with " + baseURL + ", but found " + urlString, urlString.indexOf(baseURL) == 0);
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
        Assert.assertNotNull("Cannot pop without a push.", location);
        beginAt(location, millis);
    }

    public void saveLocation()
    {
        _savedLocation = getCurrentRelativeURL();
    }

    public void recallLocation()
    {
        recallLocation(defaultWaitForPage);
    }

    public void recallLocation(int wait)
    {
        Assert.assertNotNull("Cannot recall without saving first.", _savedLocation);
        beginAt(_savedLocation, wait);
    }

    public void refresh()
    {
        refresh(defaultWaitForPage);
    }

    public void refresh(int millis)
    {
        _driver.navigate().refresh();
        waitForPageToLoad(millis);
    }

    public void goBack(int millis)
    {
        _driver.navigate().back();
        waitForPageToLoad(millis);
    }

    public void goBack()
    {
        goBack(defaultWaitForPage);
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

    @LogMethod
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
            Assert.fail("Unable to ensure credentials: " + e.getMessage());
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
        ensureSignedOut();
        if (!isTitleEqual("Sign In"))
            beginAt("/login/login.view");

        // Sign in if browser isn't already signed in.  Otherwise, we'll be on the home page.
        if (isTitleEqual("Sign In"))
        {
            assertTitleEquals("Sign In");
            waitForElement(Locator.id("email"), defaultWaitForPage);
            assertFormPresent("login");
            setText("email", PasswordUtil.getUsername());
            setText("password", PasswordUtil.getPassword());
            clickButton("Sign In");

            if (isTextPresent("Type in your email address and password"))
                Assert.fail("Could not log in with the saved credentials.  Please verify that the test user exists on this installation or reset the credentials using 'ant setPassword'");
        }

        assertSignOutAndMyAccountPresent();
    }

    public void assertSignOutAndMyAccountPresent()
    {
        click(Locator.id("userMenuPopupLink"));
        assertTextNotPresent("Sign In");
//        assertTextPresent("My Account");
    }

    // Just sign in & verify -- don't check for startup, upgrade, admin mode, etc.
    public void signIn(String email, String password, boolean failOnError)
    {
        if ( !isLinkPresentWithText("Sign In") )
            Assert.fail("You need to be logged out to log in.  Please log out to log in.");

        attemptSignIn(email, password);

        if ( failOnError )
        {
            if ( isTextPresent("Type in your email address and password") )
                Assert.fail("Could not log in with the saved credentials.  Please verify that the test user exists on this installation or reset the credentials using 'ant setPassword'");

            assertSignOutAndMyAccountPresent();
        }
    }


    public void attemptSignIn(String email, String password)
    {
        clickLinkWithText("Sign In");

        assertTitleEquals("Sign In");
        assertFormPresent("login");
        setFormElement(Locator.id("email"), email);
        setFormElement(Locator.id("password"), password, true);
        clickButton("Sign In");
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
        Assert.assertNotNull("Link for found", link);

        String emailSubject = link.getText();
        link.click();

        WebElement resetLink = _shortWait.until(ExpectedConditions.elementToBeClickable(By.xpath("//table[@id='dataregion_EmailRecord']//a[text() = '" + emailSubject + "']/..//a[contains(@href, 'setPassword.view')]")));
        resetLink.click();
        waitForPageToLoad();

        setFormElement(Locator.id("password"), password);
        setFormElement(Locator.id("password2"), password);

        clickButton("Set Password");
    }

    protected String getPasswordResetUrl(String username)
    {
        goToHome();
        goToModule("Dumbster");
        String emailSubject = "Reset Password Notification from the LabKey Server Web Site";
        WebElement email = _driver.findElement(By.xpath("//table[@id='dataregion_EmailRecord']//td[text() = '" + username + "']/..//a[text() = '"+emailSubject+"']"));
        email.click();
        WebElement resetLink = _shortWait.until(ExpectedConditions.elementToBeClickable(By.xpath("//table[@id='dataregion_EmailRecord']//td[text() = '" + username + "']/..//a[contains(@href, 'setPassword.view')]")));
        return resetLink.getText();
    }

    protected void resetPassword(String resetUrl, String username, String newPassword)
    {
        if(resetUrl!=null)
            beginAt(resetUrl);

        assertTextPresent(username, "Choose a password you'll use to access this server", "six non-whitespace characters or more, cannot match email address");

        setFormElement(Locator.id("password"), newPassword);
        setFormElement(Locator.id("password2"), newPassword);

        clickButton("Set Password");

        if(!isElementPresent(Locator.id("userMenuPopupLink")))
        {
            clickButtonContainingText("Submit", defaultWaitForPage*3);
            clickButton("Done");

            signOut();
            signIn(username, newPassword, true);
        }
    }

    protected void changePassword(String oldPassword, String password)
    {
        goToMyAccount();
        clickButton("Change Password");

        setFormElement(Locator.id("oldPassword"), oldPassword);
        setFormElement(Locator.id("password"), password);
        setFormElement(Locator.id("password2"), password);

        clickButton("Set Password");
    }


    /**
     * change user's e-mail from userEmail to newUserEmail from admin console
     */
    protected void changeUserEmail(String userEmail, String newUserEmail)
    {
        log("Attempting to change user email from " + userEmail + " to " + newUserEmail);


        goToSiteUsers();
        clickLinkContainingText(displayNameFromEmail(userEmail));

        clickButton("Change Email");

        setFormElement(Locator.name("newEmail"), newUserEmail);
        clickButton("Submit");
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
            curExpiration = PasswordExpiration.valueOf(getFormElement(Locator.name("expiration")));
        }

        if ( strength != null && curStrength != strength)
        {
            if ( oldStrength == null ) oldStrength = curStrength;
            click(Locator.radioButtonByNameAndValue("strength", strength.toString()));
        }

        if ( expiration != null && curExpiration != expiration)
        {
            if ( oldExpiration == null ) oldExpiration = curExpiration;
            setFormElement(Locator.name("expiration"), expiration.toString());
        }

        clickButton("Save");

        popLocation();
    }

    @LogMethod
    protected void resetDbLoginConfig()
    {
        if ( oldStrength != null || oldExpiration != null )
        {
            pushLocation();

            if (isElementPresent(Locator.id("userMenuPopupLink")))
            {
                click(Locator.id("userMenuPopupLink"));
                if (isTextPresent("Stop Impersonating"))
                {
                     stopImpersonating();
                }
            }

            beginAt("/login/configureDbLogin.view");

            if ( oldStrength != null ) click(Locator.radioButtonByNameAndValue("strength", oldStrength.toString()));
            if ( oldExpiration != null ) setFormElement(Locator.name("expiration"), oldExpiration.toString());

            clickButton("Save");

            if ( oldStrength != null ) Assert.assertEquals("Unable to reset password strength.", oldStrength, PasswordRule.valueOf(getText(Locator.xpath("//input[@name='strength' and @value='Weak']/.."))));
            if ( oldExpiration != null ) Assert.assertEquals("Unable to reset password expiration.", oldExpiration, PasswordExpiration.valueOf(getFormElement(Locator.name("expiration"))));

            // Back to default.
            oldStrength = null;
            oldExpiration = null;

            popLocation();
        }
    }

    protected void setSystemMaintenance(boolean enable)
    {
        // Not available in production mode
        if (enableDevMode())
        {
            goToAdminConsole();
            clickLinkWithText("system maintenance");

            if (enable)
                checkCheckbox("enableSystemMaintenance");
            else
                uncheckCheckbox("enableSystemMaintenance");

            clickButton("Save");
        }
    }

    public void ensureAdminMode()
    {
        //Now switch to admin mode if available
        //TODO:  this is causing all kinds of problems
//        if (!isElementPresent(Locator.id("leftmenupanel")) && !(isElementPresent(Locator.id("Admin ConsoleTab"))))
//            clickAdminMenuItem("Show Navigation Bar");
        assertElementPresent(Locator.css("#adminMenuPopupText"));
        assertElementPresent(Locator.css(".labkey-expandable-nav-panel"));
    }

    public void goToAdminConsole()
    {
        goToHome();
        clickAdminMenuItem("Site", "Admin Console");
    }

    public void goToSiteSettings()
    {
        goToAdminConsole();
        clickLinkWithText("site settings");
    }

    public void goToAuditLog()
    {
        goToAdminConsole();
        clickLinkWithText("audit log");
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
        _extHelper.clickExtMenuButton(wait, Locator.xpath("//img[@id='more-" + webPartTitle.toLowerCase() + "']"), items);
    }

    // Clicks admin menu items. Tests should use helpers to make admin menu changes less disruptive.
    protected void clickAdminMenuItem(String... items)
    {
        waitForElement(Locator.xpath(ADMIN_MENU_XPATH));
        Ext4HelperWD.clickExt4MenuButton(this, true, Locator.xpath(ADMIN_MENU_XPATH), false, items);
    }

    public void clickUserMenuItem(String... items)
    {
        clickUserMenuItem(true, false, items);
    }

    public void clickUserMenuItem(boolean wait, boolean onlyOpen, String... items)
    {
        waitForElement(USER_MENU_LOC);
        Ext4HelperWD.clickExt4MenuButton(this, true, USER_MENU_LOC, onlyOpen, items);
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

    public void goToFolderManagement()
    {
        clickAdminMenuItem("Folder", "Management");
    }

    public void goToProjectSettings()
    {
        clickAdminMenuItem("Folder", "Project Settings");
    }

    public void goToSiteUsers()
    {
        clickAdminMenuItem("Site", "Site Users");
    }

    public void goToSiteGroups()
    {
        clickAdminMenuItem("Site", "Site Groups");
    }

    public void goToSiteDevelopers()
    {
        clickAdminMenuItem("Site", "Site Developers");
    }

    public void goToSiteAdmins()
    {
        clickAdminMenuItem("Site", "Site Admins");
    }

    public void goToManageViews()
    {
        clickAdminMenuItem("Manage Views");
    }

    public void goToManageStudy()
    {
        clickAdminMenuItem("Manage Study");
    }

    public void goToManageAssays()
    {
        clickAdminMenuItem("Manage Assays");
    }

    public void goToCreateProject()
    {
        clickAdminMenuItem("Site", "Create Project");
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
            Assert.fail("Webapp failed to start up after " + MAX_SERVER_STARTUP_WAIT_SECONDS + " seconds.");
        }
        log("Server is running.");
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

            log("Attempting to register another initial user");
            popLocation();
            // Make sure we got redirected to the module status page, since we already have a user
            assertTextNotPresent("Retype Password");
            assertTextPresent("Please wait, this page will automatically update with progress information");
            goToHome();
        }

        if (bootstrapped || isTitleEqual("Sign In"))
        {
            // if the logout page takes us to the sign-in page, then we may have a schema update to do:
            if (isTitleEqual("Sign In"))
                simpleSignIn();

            String upgradeText = "Please wait, this page will automatically update with progress information.";
            boolean performingUpgrade = isTextPresent(upgradeText);

            if (performingUpgrade)
            {
                verifyRedirectBehavior(upgradeText);

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
                            Assert.fail("A startup failure occurred.");
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
                    Assert.fail("Script runner took more than 10 minutes to complete.");

                if (isNavButtonPresent("Next"))
                {
                    clickButton("Next");

                    // check for any additional upgrade pages inserted after module upgrade
                    if (isNavButtonPresent("Next"))
                        clickButton("Next");
                }

                if (isLinkPresentContainingText("Go directly to the server's Home page"))
                {
                    clickLinkContainingText("Go directly to the server's Home page");
                }
            }

            // Will fail if left navbar is not enabled in Home project. TODO: allow this, see #xxxx
            clickLinkWithText("Home");
        }
    }


    private void verifyInitialUserError(@Nullable String email, @Nullable String password1, @Nullable String password2, @Nullable String expectedText)
    {
        if (null != email)
            setFormElement(Locator.id("email"), email);

        if (null != password1)
            setFormElement(Locator.id("password"), password1);

        if (null != password2)
            setFormElement(Locator.id("password2"), password2);

        clickLinkWithText("Next");

        if (null != expectedText)
            Assert.assertEquals("Wrong error message.", expectedText, Locator.css(".labkey-error").findElement(_driver).getText());
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
    private void verifyRedirectBehavior(String upgradeText)
    {
        // Do these checks via direct http requests the primary upgrade window seems to interfere with this test, #15853

        HttpClient client = WebTestHelper.getHttpClient();
        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpResponse response = null;
        HttpUriRequest method;
        int status;

        try
        {
            // These requests should NOT redirect to the upgrade page

            method = new HttpGet(getBaseURL() + "/login/resetPassword.view");
            response = client.execute(method, context);
            status = response.getStatusLine().getStatusCode();
            Assert.assertEquals("Unexpected response", HttpStatus.SC_OK, status);
            Assert.assertFalse("Upgrade text found", WebTestHelper.getHttpResponseBody(response).contains(upgradeText));
            EntityUtils.consume(response.getEntity());

            method = new HttpGet(getBaseURL() + "/admin/maintenance.view");
            response = client.execute(method, context);
            status = response.getStatusLine().getStatusCode();
            Assert.assertEquals("Unexpected response", HttpStatus.SC_OK, status);
            Assert.assertFalse("Upgrade text found", WebTestHelper.getHttpResponseBody(response).contains(upgradeText));
            EntityUtils.consume(response.getEntity());


            // Check that sign out and sign in work properly during upgrade/install (once initial user is configured)

            ((DefaultHttpClient)client).setRedirectStrategy(new DefaultRedirectStrategy()
            {
                @Override
                public boolean isRedirected(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws ProtocolException
                {
                    boolean isRedirect=false;
                    try {
                        isRedirect = super.isRedirected(httpRequest, httpResponse, httpContext);
                    } catch (ProtocolException e) {}
                    if (!isRedirect) {
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
            });
            method = new HttpPost(getBaseURL() + "/login/logout.view");
            List<NameValuePair> args = new ArrayList<NameValuePair>();
            args.add(new BasicNameValuePair("login", PasswordUtil.getUsername()));
            args.add(new BasicNameValuePair("password", PasswordUtil.getPassword()));
            ((HttpPost)method).setEntity(new UrlEncodedFormEntity(args));
            response = client.execute(method, context);
            status = response.getStatusLine().getStatusCode();
            Assert.assertEquals("Unexpected response", HttpStatus.SC_OK, status);
            // TODO: check login, once http-equiv redirect is sorted out
            Assert.assertFalse("Upgrade text found", WebTestHelper.getHttpResponseBody(response).contains(upgradeText));
            EntityUtils.consume(response.getEntity());
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Switch to the initial test window
     */
    public void switchToMainWindow()
    {
        Set<String> windows = new HashSet<String>(_driver.getWindowHandles());
        _driver.switchTo().window((String) windows.toArray()[0]);
    }

    /**
     * Open new window via javascript
     * @return ID of created window, for use with WebDriver.TargetLocator.window interface
     */
    public String newWindow()
    {
        HashSet<String> initialWindows = new HashSet<String>(_driver.getWindowHandles());
        executeScript("window.open();");
        HashSet<String> windows = new HashSet<String>(_driver.getWindowHandles());
        windows.removeAll(initialWindows);
        Assert.assertEquals("Unexpected number of new windows", 1, windows.size());
        return (String)windows.toArray()[0];
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

    private long smStart = 0;

    public void startSystemMaintenance()
    {
        startSystemMaintenance("Run all tasks");
    }

    public void startSystemMaintenance(String task)
    {
        goToAdminConsole();
        clickLinkWithText("system maintenance");
        clickLinkWithText(task, false);
        smStart = System.currentTimeMillis();
    }

    public void waitForSystemMaintenanceCompletion()
    {
        Assert.assertTrue("Must call startSystemMaintenance() before waiting for completion", smStart > 0);
        long elapsed = System.currentTimeMillis() - smStart;

        // Ensure that at least 5 seconds has passed since system maintenance was started
        if (elapsed < 5000)
        {
            log("Sleeping for " + (5000 - elapsed) + " ms");
            sleep(5000 - elapsed);
        }

        Object[] windows =_driver.getWindowHandles().toArray();
        _driver.switchTo().window((String)windows[1]);
        log("Waiting for system maintenance to complete");

        // Page updates automatically via AJAX... keep checking (up to 10 minutes from the start of the test) for system maintenance complete text
        waitFor(new Checker() {
            public boolean check()
            {
                return isTextPresent("System maintenance complete");
            }
        }, "System maintenance failed to complete in 10 minutes.", 10 * 60 * 1000 - ((Long)elapsed).intValue());

        _driver.close();
        _driver.switchTo().window((String)windows[0]);
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
            if (null != _driver.getTitle())
                return _driver.getTitle();
            else
                return "[no title: content type is not html]";
        }
        return _lastPageTitle;
    }

    public String getLastPageText()
    {
        return _lastPageText != null ? _lastPageText : _driver.getPageSource();
    }

    public boolean isPageEmpty()
    {
        //IE and Firefox have different notions of empty.
        //IE returns html for all pages even empty text...
        String text = _driver.getPageSource();
        if (null == text || text.trim().length() == 0)
            return true;

        text = getBodyText();
        return null == text || text.trim().length() == 0;
    }

    public URL getLastPageURL()
    {
        try
        {
            return _lastPageURL != null ? _lastPageURL : new URL(_driver.getCurrentUrl());
        }
        catch (MalformedURLException x)
        {
            return null;
        }
    }

    public String getBodyText()
    {
        try
        {
            return _driver.findElement(By.cssSelector("body")).getText();
        }
        catch (StaleElementReferenceException ex)
        {
            try
            {
                return _shortWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body"))).getText();
            }
            catch (TimeoutException tex)
            {
                return getHtmlSource(); // probably viewing a tsv or text file
            }

        }
        catch (NoSuchElementException ex)
        {
            try
            {
                return _shortWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body"))).getText();
            }
            catch (TimeoutException tex)
            {
                return getHtmlSource(); // probably viewing a tsv or text file
            }
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
     * Override this method to skip running this test for a given configuration.
     * @return true to run the test, false to skip. Empty info should return false for overrides.
     */
    protected boolean isConfigurationSupported()
    {
        return true;
    }

    @Test(timeout=2700000) // 45 minute default test timeout
    public void testSteps() throws Exception
    {
        try
        {
            log("\n\n=============== Starting " + getClass().getSimpleName() + Runner.getProgress() + " =================");

            _startTime = System.currentTimeMillis();

            logToServer("=== Starting " + getClass().getSimpleName() + Runner.getProgress() + " ===");
            signIn();
			resetErrors();

            if (isMaintenanceDisabled())
            {
                // Disable scheduled system maintenance to prevent timeouts during nightly tests.
                disableMaintenance();
            }


            if ( !isGuestModeTest() )
            {
                if (!isConfigurationSupported()) // skip this check if it returns true with no database info.
                {
                    log("** Skipping " + getClass().getSimpleName() + " test for unsupported configurarion");
                    _testFailed = false;
                    return;
                }
            }

            log("Pre-cleaning " + getClass().getSimpleName());
            doCleanup(false);

            // Only do this as part of test startup if we haven't already checked. Since we do this as the last
            // step in the test, there's no reason to bother doing it again at the beginning of the next test
            if (!_checkedLeaksAndErrors)
            {
                checkLeaksAndErrors();
            }

            doTestSteps();

            //make sure you're signed in as admin, because this won't work otherwise
            ensureSignedInAsAdmin();

            checkQueries();

            checkViews();


            if(!isPerfTest)
                checkActionCoverage();

            if (enableLinkCheck())
            {
                pauseJsErrorChecker();
                Crawler crawler = new Crawler(this, Runner.getTestSet().getCrawlerTimeout());
                crawler.crawlAllLinks(enableInjectCheck());
                resumeJsErrorChecker();
            }

            _testFailed = false;

            try
            {
                if (!skipCleanup())
                {
                    goToHome();
                    doCleanup(true);
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
        catch (Exception e)
        {
            // Log the failure before we attempt any other cleanup in the finally block below
            // This ensures that we don't lose the original failure if the cleanup fails for
            // some reason with a new exception
            e.printStackTrace();
            throw e;
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
                if (_lastPageTitle != null && !_lastPageTitle.startsWith("404"))
                {
                    try
                    {
                        // On failure, re-invoke the last action with _debug paramter set, which lets the action log additional debugging information
                        String lastPage = _lastPageURL.toString();
                        URL url = new URL(lastPage + (lastPage.contains("?") ? "&" : "?") + "_debug=1");
                        log("Re-invoking last action with _debug parameter set: " + url.toString());
                        url.getContent();
                    }
                    catch (Exception t)
                    {
                        System.out.println("Unable to re-invoke last page");
                        t.printStackTrace();
                    }
                }
                try
                {
                    dump();
                    if (onTeamCity())
                    {
                        dumpPipelineFiles(getLabKeyRoot() + "/sampledata");
                        dumpPipelineLogFiles(getLabKeyRoot() + "/build/deploy/files");
                    }
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
                    try
                    {
                        resetDbLoginConfig(); // Make sure to return DB config to its pre-test state.
                    }
                    catch(Throwable t){log("Failed to reset DB login config after test failure");}

                    try
                    {
                        if (isPipelineToolsTest()) // Get DB back in a good state after failed pipeline tools test.
                            fixPipelineToolsDirectory();
                    }
                    catch(Throwable t){log("Failed to fix pipeline tools directory after test failure");}
                }
            }

            checkJsErrors();

            logToServer("=== Completed " + getClass().getSimpleName() + Runner.getProgress() + " ===");

            log("=============== Completed " + getClass().getSimpleName() + Runner.getProgress() + " =================");
        }
    }

    @LogMethod
    public void ensureSignedInAsAdmin()
    {
        goToHome();
        if(isTextPresent("Admin"))
            return;

        if (isElementPresent(Locator.id("userMenuPopupLink")))
            signOut();

        signIn();
    }

    protected abstract void doTestSteps() throws Exception;

    // Standard cleanup: delete the project
    protected void doCleanup(boolean afterTest)
    {
        String projectName = getProjectName();

        if (null != projectName)
            deleteProject(projectName, afterTest);
    }

    public void cleanup() throws Exception
    {
        try
        {
            log("========= Cleaning up " + getClass().getSimpleName() + " =========");

            // explicitly go back to the site, just in case we're on a 404 or crash page:
            beginAt("");
            signIn();
            doCleanup(false);   // User requested cleanup... could be before or after tests have run (or some intermediate state). False generally means ignore errors.

            beginAt("");

            // The following checks verify that the test deleted all projects and folders that it created.
            for (WebTestHelper.FolderIdentifier folder : _createdFolders)
                assertLinkNotPresentWithText(folder.getFolderName());

            for (String projectName : _containerHelper.getCreatedProjects())
                assertLinkNotPresentWithText(projectName);
            log("========= " + getClass().getSimpleName() + " cleanup complete =========");
        }
        finally
        {
            tearDown();
        }
    }

    protected boolean skipCleanup()
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
        return false; //"true".equals(System.getProperty("scriptCheck")) && getBrowser().startsWith("*firefox"); TODO: hanging tests on TeamCity
    }

    public boolean enableDevMode()
    {
        return "true".equals(System.getProperty("devMode"));
    }

    public boolean firebugPanelsEnabled()
    {
        return "true".equals(System.getProperty("enableFirebugPanels"));
    }

    public boolean firefoxExtensionsEnabled()
    {
        return "true".equals(System.getProperty("enableFirefoxExtensions"));
    }

    public boolean onTeamCity()
    {
        return System.getProperty("teamcity.buildType.id") != null;
    }

    public boolean skipLeakCheck()
    {
        return "false".equals(System.getProperty("memCheck"));
    }

    public boolean skipQueryCheck()
    {
        return "false".equals(System.getProperty("queryCheck"));
    }

    public boolean skipViewCheck()
    {
        return "false".equals(System.getProperty("viewCheck"));
    }

    public boolean isMaintenanceDisabled()
    {
        return "never".equals(System.getProperty("systemMaintenance"));
    }

    public String getDatabaseType()
    {
        return System.getProperty("databaseType");
    }

    public String getDatabaseVersion()
    {
        return System.getProperty("databaseVersion");
    }

    public WebDriver getDriver()
    {
        return _driver;
    }

    public boolean isGroupConcatSupported()
    {
        if ("pg".equals(getDatabaseType()))
            return true;

        if ("mssql".equals(getDatabaseType()) && !"2005".equals(getDatabaseVersion()))
            return true;

        return false;
    }

    public boolean isGuestModeTest()
    {
        return false;
    }

    protected boolean isQuickTest = false;

    protected boolean isQuickTest()
    {
        return isQuickTest;//"DRT".equals(System.getProperty("suite"));
    }

    protected void setIsQuickTest(boolean isQuickTest)
    {
        this.isQuickTest = isQuickTest;
    }

    @LogMethod
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
                log("Found " + leakCount + " in-use objects; rerunning GC. ("
                        + (GC_ATTEMPT_LIMIT - attempt) + " attempt(s) remaining.)");

                // If another thread (e.g., SearchService) is doing work then give it 10 seconds before trying again
                if (isTextPresent("Warning: active thread"))
                {
                    log("Pausing 10 seconds to wait for active thread");
                    sleep(10000);
                }
            }
            beginAt("/admin/memTracker.view?gc=1&clearCaches=1", 120000);
            if (!isTextPresent("In-Use Objects"))
                Assert.fail("Asserts must be enabled to track memory leaks; please add -ea to your server VM params and restart.");
            leakCount = getImageWithAltTextCount("expand/collapse");
        }

        if (leakCount > MAX_LEAK_LIMIT)
        {
            String leaks = Locator.name("leaks").findElement(_driver).getText();
            CRC32 crc = new CRC32();
            crc.update(leaks.getBytes());

            if (leakCRC != crc.getValue())
            {
                leakCRC = crc.getValue();
                dumpHeap();
                Assert.fail(leakCount + " in-use objects exceeds allowed limit of " + MAX_LEAK_LIMIT + ".");
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

       Assert.assertTrue("There were errors during the test run", isPageEmpty());
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
        String text = getHtmlSource();
        if (null == text)
            text = "";
        text = text.trim();
        if ("".equals(text))
        {
            text = getBodyText();
            if (null == text)
                text = "";
            text = text.trim();
        }

        Assert.assertTrue("Expected " + count + " errors during this run", StringUtils.countMatches(text, "ERROR") == count);
        log("Found " + count + " expected errors.");

        // Clear the errors to prevent the test from failing.
        resetErrors();

        popLocation();
    }

    @LogMethod
    protected void checkQueries()
    {
        if (skipQueryCheck())
            return;
        if(getProjectName() != null)
        {
            clickFolder(getProjectName());
            if(!"Query Schema Browser".equals(_driver.getTitle()))
                goToSchemaBrowser();
            validateQueries(true);
//            validateLabAuditTrail();
        }
    }

    @LogMethod
    protected void checkViews()
    {
        if (skipViewCheck())
            return;

        List<String> checked = new ArrayList<String>();

        for (String projectName : _containerHelper.getCreatedProjects())
        {
            doViewCheck(projectName);
            checked.add(projectName);
        }

        for (WebTestHelper.FolderIdentifier folderId : _createdFolders)
        {
            String project = folderId.getProjectName();
            String folder = folderId.getFolderName();
            if(!checked.contains(project))
            {
                doViewCheck(project);
                checked.add(project);
            }
            if(!checked.contains(folder))
            {
                clickLinkWithText(project);
                if(isLinkPresentWithText(folder))
                {
                    doViewCheck(folder);
                    checked.add(folder);
                }
            }
        }
    }

    private void doViewCheck(String folder)
    {
        clickLinkWithText(folder);
        try{
            goToManageViews();
        }
        catch (SeleniumException e)
        {
            return; // No manage views option
        }

        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        String viewXpath = "//div[contains(@class, 'x-grid-group-body')]/div[contains(@class, 'x-grid3-row')]";
        int viewCount = getElementCount(Locator.xpath(viewXpath));
        for (int i = 1; i <= viewCount; i++)
        {
            pushLocation();
            String thisViewXpath = "("+viewXpath+")["+i+"]";
            waitForElement(Locator.xpath(thisViewXpath));
            String viewName = getText(Locator.xpath(thisViewXpath + "//td[contains(@class, 'x-grid3-cell-first')]"));
            click(Locator.xpath(thisViewXpath));
            waitAndClick(Locator.linkWithText("VIEW"));
            waitForText(viewName);
            popLocation();
            _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        }
    }

    @LogMethod
    private void checkJsErrors()
    {
        if (this.enableScriptCheck())
        {
            try
            {
                _jsErrors.addAll(JavaScriptError.readErrors(_driver));
            }
            catch (WebDriverException ex)
            {
                return; // Error checker has not been initialized
            }
            List<JavaScriptError> validErrors = new ArrayList<JavaScriptError>();
            for (JavaScriptError error : _jsErrors)
            {
                if (!validErrors.contains(error) && validateJsError(error))
                {
                    if (validErrors.size() == 0)
                        log("<<<<<<<<<<<<<<<JAVASCRIPT ERRORS>>>>>>>>>>>>>>>"); // first error
                    validErrors.add(error);
                    log(error.toString());
                }
            }
            if (validErrors.size() > 0)
            {
                String errorCtStr = "";
                if (validErrors.size() > 1)
                    errorCtStr = " (1 of " + validErrors.size() + ") ";
                if (!_testFailed) // Don't clobber existing failures. Just log them.
                    Assert.fail("JavaScript error" + errorCtStr + ": " + validErrors.get(0));
            }
        }
    }

    protected void validateLabAuditTrail()
    {
        int auditEventRowCount = 0;
        SelectRowsCommand selectCmd = new SelectRowsCommand("auditLog", "tobereplaced");
        selectCmd.setMaxRows(-1);
        selectCmd.setContainerFilter(ContainerFilter.CurrentAndSubfolders);
        selectCmd.setColumns(Arrays.asList("*"));
        Connection cn = getDefaultConnection();
        SelectRowsResponse selectResp = null;

        for(String query : new String[] {"ExperimentAuditEvent", "SampleSetAuditEvent", "ContainerAuditEvent"})
        {
            selectCmd.setQueryName(query);
            int rowCount = 0;
            try
            {
                selectResp = selectCmd.execute(cn,  "/" +  getProjectName());
                rowCount =   selectResp.getRowCount().intValue();
            }
            catch (IOException e)
            {
               Assert.fail("Unable to retrieve query: " + query);
            }
            catch (CommandException e)
            {
               Assert.fail("Unable to retrieve query: " + query);
            }
            log(query + " row count: " + rowCount);
            auditEventRowCount += rowCount;
        }

        //file system events are batched
        String query =  "FileSystem";
        try
        {
            selectCmd.setQueryName(query);
            int rowCount = 0;

            selectResp = selectCmd.execute(cn,  "/" +  getProjectName());
            rowCount =   selectResp.getRowCount().intValue();

            //if we ever have a test generating more than one batch of files, this will need to be updated, but it will
            //do for now
            if(rowCount > 0)
                auditEventRowCount++;
        }
        catch (IOException e)
        {
           Assert.fail("Unable to retrieve query: " + query);
        }
        catch (CommandException e)
        {
           Assert.fail("Unable to retrieve query: " + query);
        }
        try
        {
            selectCmd.setQueryName("LabAuditEvents");
            selectResp = selectCmd.execute(cn,  "/" +  getProjectName());
        }
        catch (IOException e)
        {
               Assert.fail("Unable to retrieve query: LabAuditEvents");
        }
        catch (CommandException e)
        {
               Assert.fail("Unable to retrieve query: LabAuditEvents");
        }
        Assert.assertEquals("Number of rows in LabAuditEvents did not equal sum of component event types", auditEventRowCount, selectResp.getRowCount().intValue());
    }

    public Connection getDefaultConnection()
    {
        return new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }

    @LogMethod
    private void checkActionCoverage()
    {
        if ( isGuestModeTest() )
            return;

        pushLocation();
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
        popLocation();
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

    protected File ensureDumpDir()
    {
        File dumpDir = new File(Runner.getDumpDir(), getClass().getSimpleName());
        if ( !dumpDir.exists() )
            dumpDir.mkdirs();

        return dumpDir;
    }

    public void dump()
    {
        try
        {
            File dumpDir = ensureDumpDir();
            FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMddHHmm");
            String baseName = dateFormat.format(new Date()) + getClass().getSimpleName();

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
            File destDir = ensureDumpDir();
            String dumpMsg = Locator.css("#bodypanel > div").findElement(_driver).getText();
            String filename = dumpMsg.substring(dumpMsg.indexOf("HeapDump_"));
            File heapDump = new File(getLabKeyRoot() + "/build/deploy", filename);
            File destFile = new File(destDir, filename);

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
        if (file != null && onTeamCity())
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
            File tempScreen = ((TakesScreenshot)_driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(tempScreen, screenFile);
            return screenFile;
        }
        catch (IOException ioe)
        {
            log("Failed to copy screenshot file: " + ioe.getMessage());
        }

        return null;
    }

    public void windowMaximize()
    {
        _driver.manage().window().maximize();
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
        return saveFile(dir, baseName + ".tsv");
    }

    public File saveFile(File dir, String fileName)
    {
        return saveFile(dir, fileName, getBodyText());
    }

    public File saveFile(File dir, String fileName, String contents)
    {
        FileWriter writer = null;
        try
        {
            File tsvFile = new File(dir, fileName);
            writer = new FileWriter(tsvFile);
            writer.write(contents);
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

    public static class BrowserInfo
    {
        private static BrowserInfo instance = null;
        private static String _type;
        private static String _version;

        private BrowserInfo(){}

        private BrowserInfo(String type, String version)
        {
            _type = type;
            _version = version;
        }

        public static BrowserInfo getBrowserInfo(BaseWebDriverTest test)
        {
            if (instance == null)
            {
                List<String> browserInfoArray = (ArrayList<String>)test.executeScript(
                        "    var N= navigator.appName, ua= navigator.userAgent, tem;\n" +
                        "    var M= ua.match(/(opera|chrome|safari|firefox|msie)\\/?\\s*(\\.?\\d+(\\.\\d+)*)/i);\n" +
                        "    if(M && (tem= ua.match(/version\\/([\\.\\d]+)/i))!= null) M[2]= tem[1];\n" +
                        "    M= M? [M[1], M[2]]: [N, navigator.appVersion, '-?'];\n" +
                        "    return M;");
                instance = new BrowserInfo(browserInfoArray.get(0), browserInfoArray.get(1));
            }
            return instance;
        }

        public String getType()
        {
            return _type;
        }

        public String getVersion()
        {
            return _version;
        }
    }

    public String getBaseURL()
    {
        return WebTestHelper.getBaseURL();
    }

    public String getProjectUrl()
    {
        return "/project/" + EscapeUtil.encode(getProjectName()) + "/begin.view?";
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
        pauseJsErrorChecker();
        _driver.navigate().to(getBaseURL() + relativeURL);
        resumeJsErrorChecker();
    }

    public String getContainerId(String url)
    {
        pushLocation();
        beginAt(url);
        String containerId = (String)executeScript("return LABKEY.container.id;");
        popLocation();
        return containerId;
    }

    public String getConfirmationAndWait()
    {
        Alert alert = _driver.switchTo().alert();
        String confirmation = alert.getText();
        alert.accept();
        waitForPageToLoad();
        return confirmation;
    }

    /**
     * @deprecated Use {@link #assertAlert(String)}
     * @param msg
     */
    @Deprecated
    public void assertConfirmation(String msg)
    {
        assertAlert(msg);
    }

    public void assertAlert(String msg)
    {
        Alert alert = _driver.switchTo().alert();
        Assert.assertEquals(msg, alert.getText());
        alert.accept();
    }

    public void dismissAlerts()
    {
        while (isAlertPresent()){
            Alert alert = _driver.switchTo().alert();
            log("Found unexpected alert: " + alert.getText());
            alert.accept();
        }
    }

    public void logJavascriptAlerts()
    {
        while (isAlertPresent())
        {
            Alert alert = _driver.switchTo().alert();
            log("JavaScript Alert Ignored: " + alert.getText());
            alert.accept();
        }
    }

	public boolean isAlertPresent()
	{
        try {
            _driver.switchTo().alert();
            switchToMainWindow();
            return true;
        }
        catch (NoAlertPresentException ex)
        {
            return false;
        }
	}

	public String getAlert()
	{
        Alert alert = _driver.switchTo().alert();
        String text = alert.getText();
        alert.accept();
        return text;
	}

    public void assertExtMsgBox(String title, String text)
    {
        String actual = _extHelper.getExtMsgBoxText(title);
        Assert.assertTrue("Expected Ext.Msg box text '" + text + "', actual '" + actual + "'", actual.indexOf(text) != -1);
    }

    public enum SeleniumEvent
    {blur,change,mousedown,mouseup,click,reset,select,submit,abort,error,load,mouseout,mouseover,unload,keyup}

    /**
     * Create and fire a JavaScript UIEvent
     * @param l event target
     * @param event event
     */
    public void fireEvent(Locator l, SeleniumEvent event)
    {
        executeScript(
                "var element = arguments[0];" +
                        "var eventType = arguments[1];" +
                        "var myEvent = document.createEvent('UIEvent');\n" +
                        "myEvent.initEvent(\n" +
                        "   eventType      // event type\n" +
                        "   ,true      // can bubble?\n" +
                        "   ,true      // cancelable?\n" +
                        ");\n" +
                        "element.dispatchEvent(myEvent);", l.findElement(_driver), event.toString());
    }

    @LogMethod
    public void startCreateGlobalPermissionsGroup(String groupName, boolean failIfAlreadyExists)
    {

        goToHome();
        goToSiteGroups();
        if(isElementPresent(Locator.tagWithText("div", groupName)))
        {
            if(failIfAlreadyExists)
                Assert.fail("Group already exists");
            else
                return;
        }

        Locator l = Locator.xpath("//input[contains(@name, 'sitegroupsname')]");
        waitForElement(l, defaultWaitForPage);

        setFormElement(l,groupName);
        clickButton("Create New Group", 0);
        _extHelper.waitForExtDialog(groupName + " Information");
    }

    public void createGlobalPermissionsGroup(String groupName, String... users)
    {
        createGlobalPermissionsGroup(groupName, true, users);
    }

    public void createGlobalPermissionsGroup(String groupName, boolean failIfAlreadyExists, String... users )
    {
        startCreateGlobalPermissionsGroup(groupName, failIfAlreadyExists);
        StringBuilder namesList = new StringBuilder();
        for(String member : users)
        {
            namesList.append(member).append("\n");
        }

        log("Adding\n" + namesList.toString() + " to group " + groupName + "...");
        waitAndClick(Locator.tagContainingText("a","manage group"));
        waitForPageToLoad();
        setFormElement("names", namesList.toString());
        uncheckCheckbox("sendEmail");
        clickButton("Update Group Membership");
    }

    public void createPermissionsGroup(String groupName)
    {
        log("Creating permissions group " + groupName);
        if (!isElementPresent(Locator.permissionRendered()))
            enterPermissionsUI();
        waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
        _ext4Helper.clickTabContainingText("Project Groups");
        setFormElement(Locator.xpath("//input[contains(@name, 'projectgroupsname')]"), groupName);
        clickButton("Create New Group", 0);
        sleep(500);
        waitForText("Group " + groupName);
        waitAndClick(Locator.xpath("//div[contains(@class, 'x4-tool')]//img[contains(@class, 'x4-tool-close')]"));
        waitForElement(Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner') and text()='" + groupName + "']"), WAIT_FOR_JAVASCRIPT);
    }

    public void createPermissionsGroup(String groupName, String... memberNames)
    {
        createPermissionsGroup(groupName);
        clickManageGroup(groupName);

        StringBuilder namesList = new StringBuilder();
        for(String member : memberNames)
        {
            namesList.append(member).append("\n");
        }

        log("Adding\n" + namesList.toString() + " to group " + groupName + "...");
        addUserToGroupFromGroupScreen(namesList.toString());

        enterPermissionsUI();
    }

    public void openGroupPermissionsDisplay(String groupName)
    {
        _ext4Helper.clickTabContainingText("Project Groups");
        // warning Adminstrators can apper multiple times
        List<Ext4CmpRefWD> refs = _ext4Helper.componentQuery("grid", Ext4CmpRefWD.class);
        Ext4CmpRefWD ref = refs.get(0);
        Long idx = (Long)ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        Assert.assertFalse("Unable to locate group: \"" + groupName + "\"", idx < 0);
        ref.eval("getSelectionModel().select(" + idx + ")");
    }

    public void clickManageGroup(String groupName)
    {
        openGroupPermissionsDisplay(groupName);
        waitAndClick(Locator.tagContainingText("a","manage group"));
        waitForPageToLoad();
    }

    public void clickManageSiteGroup(String groupName)
    {
        _ext4Helper.clickTabContainingText("Site Groups");
        // warning Adminstrators can apper multiple times
        List<Ext4CmpRefWD> refs = _ext4Helper.componentQuery("grid", Ext4CmpRefWD.class);
        Ext4CmpRefWD ref = refs.get(0);
        Long idx = (Long)ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        Assert.assertFalse("Unable to locate group: \"" + groupName + "\"", idx < 0);
        ref.eval("getSelectionModel().select(" + idx + ")");
        waitAndClick(Locator.tagContainingText("a","manage group"));
        waitForPageToLoad();
    }

    public void dragGroupToRole(String group, String srcRole, String destRole)
    {
        Actions builder = new Actions(_driver);
        builder
            .clickAndHold(Locator.permissionButton(group, srcRole).findElement(_driver))
            .moveToElement(Locator.xpath("//div[contains(@class, 'rolepanel')][.//h3[text()='" + destRole + "']]/div/div").findElement(_driver))
            .release()
            .build().perform();

        waitForElementToDisappear(Locator.permissionButton(group, srcRole), WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.permissionButton(group, destRole));
    }

    public void createSubFolderFromTemplate(String project, String child, String template, @Nullable String[] objectsToCopy)
    {
        createSubfolder(project, project, child, "Create From Template Folder", template, objectsToCopy, false);

    }


    public void createSubfolder(String project, String child, String[] tabsToAdd)
    {
        // create a child of the top-level project folder:
        createSubfolder(project, project, child, "None", tabsToAdd);
    }


    public void createSubfolder(String project, String parent, String child, String folderType, @Nullable String[] tabsToAdd)
    {
        createSubfolder(project, parent, child, folderType, tabsToAdd, false);
    }


    private  void startCreateFolder(String project, String parent, String child)
    {

        ensureAdminMode();
        if (isLinkPresentWithText(child))
            Assert.fail("Cannot create folder; A link with text " + child + " already exists.  " +
                    "This folder may already exist, or the name appears elsewhere in the UI.");
        assertLinkNotPresentWithText(child);
        log("Creating subfolder " + child + " under project " + parent);
        String _active = (!parent.equals(project)? parent : project);
        if (!getText(Locator.css(".nav-tree-selected")).equals(_active))
            clickLinkWithText(_active);
        goToFolderManagement();
        waitForExt4FolderTreeNode(parent, 10000);
        clickButton("Create Subfolder", 0);
        waitForElement(Locator.name("name"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("name"), child);
    }

    public void createSubfolder(String project, String parent, String child, String folderType, String[] tabsToAdd, boolean inheritPermissions)
    {
        createSubfolder(project, parent, child, folderType, null, tabsToAdd, inheritPermissions);
    }

    /**
     *
     * @param project project in which to create new folder
     * @param parent immediate parent of the new folder (project, if it's a top level subfolder)
     * @param child name of folder to create
     * @param folderType type of folder (null for custom)
     * @param templateFolder if folderType = "create from Template Folder", this is the template folder used.  Otherwise, ignored
     * @param tabsToAdd module tabs to add iff foldertype=null,  or the copy related checkboxes iff foldertype=create from template
     * @param inheritPermissions should folder inherit permissions from parent?
     */
    @LogMethod
    public void createSubfolder(String project, String parent, String child, String folderType, String templateFolder, String[] tabsToAdd, boolean inheritPermissions)
    {
        startCreateFolder(project, parent, child);
        if (null != folderType && !folderType.equals("None"))
        {
            click(Locator.xpath("//td[./label[text()='"+folderType+"']]/input[@type='button' and contains(@class, 'radio')]"));
            if(folderType.equals("Create From Template Folder"))
            {
                log("create from template");
                click(Locator.xpath("//td[./label[text()='"+folderType+"']]/input[@type='button' and contains(@class, 'radio')]"));
                _ext4Helper.waitForMaskToDisappear();
                Locator.XPathLocator l = Locator.xpath("//tr[./td/input[@name='templateSourceId']]");
                _ext4Helper.selectComboBoxItem(l, templateFolder);
                _ext4Helper.checkCheckbox("Include Subfolders");

                //TODO:  the checkboxes.  I don't need this right now so I haven't written it, but my intention is to use tabsToAdd
            }
        }
        else {
            click(Locator.xpath("//td[./label[text()='Custom']]/input[@type='button' and contains(@class, 'radio')]"));


            if (tabsToAdd != null)
            {
                for (String tabname : tabsToAdd)
                    waitAndClick(Locator.xpath("//td[./label[text()='"+tabname+"']]/input[@type='button' and contains(@class, 'checkbox')]"));
            }
        }

        waitAndClickButton("Next");
        _createdFolders.add(new WebTestHelper.FolderIdentifier(project, child));

        //second page of the wizard
        waitForElement(Locator.css(".labkey-nav-page-header").withText("Users / Permissions"));
        if (inheritPermissions)
        {
            //nothing needed, this is the default
        }
        else {
            waitAndClick(Locator.xpath("//td[./label[text()='My User Only']]/input"));
        }

        waitAndClickButton("Finish");
        waitForElement(Locator.css(".nav-tree-selected").withText(child));

        //unless we need addtional tabs, we end here.
        if (null == tabsToAdd || tabsToAdd.length == 0)
            return;


        if (null != folderType && !folderType.equals("None")) // Added in the wizard for custom folders
        {
            goToFolderManagement();
            clickLinkWithText("Folder Type");

            for (String tabname : tabsToAdd)
                checkCheckbox(Locator.checkboxByTitle(tabname));

            submit();
            if ("None".equals(folderType))
            {
                for (String tabname : tabsToAdd)
                    assertTabPresent(tabname);
            }

            // verify that there's a link to our new folder:
            assertLinkPresentWithText(child);
        }
    }

    protected void deleteDir(File dir)
    {
        log("Deleting from filesystem: " + dir.toString());
        if (!dir.exists())
            return;

        try
        {
            FileUtils.deleteDirectory(dir);
            log("Deletion successful.");
        }
        catch (IOException e)
        {
            log("WARNING: Exception deleting directory -- " + e.getMessage());
        }
    }

    @LogMethod
    public void deleteFolder(String project, String folderName)
    {
        log("Deleting folder " + folderName + " under project " + project);
        clickLinkWithText(project);
        clickLinkWithText(folderName);
        ensureAdminMode();
        goToFolderManagement();
        waitForExt4FolderTreeNode(folderName, 10000);
        clickButton("Delete");
        // confirm delete subfolders if present
        if(isTextPresent("This folder has subfolders."))
            clickButton("Delete All Folders");
        // confirm delete:
        clickButton("Delete");
        // verify that we're not on an error page with a check for a project link:
        assertLinkPresentWithText(project);
        assertLinkNotPresentWithText(folderName);
    }

    /**
     * from the file management page, select a file and rename it
     *
     * @param oldFilename the name of the file to select
     * @param newFilename the new file name
     */
    public void renameFile(String oldFilename, String newFilename)
    {
        Locator l = Locator.xpath("//div[text()='" + oldFilename + "']");
        click(l);
        click(Locator.css("button.iconRename"));

        waitForDraggableMask();
        _extHelper.setExtFormElementByLabel("Filename:", newFilename);
        Locator btnLocator = Locator.extButton("Rename");
        click(btnLocator);
    }

    @LogMethod
    public void renameFolder(String project, String folderName, String newFolderName, boolean createAlias)
    {
        log("Renaming folder " + folderName + " under project " + project + " -> " + newFolderName);
        clickLinkWithText(project);
        clickLinkWithText(folderName);
        ensureAdminMode();
        goToFolderManagement();
        waitForExt4FolderTreeNode(folderName, 10000);
        clickButton("Rename");
        setFormElement(Locator.name("name"), newFolderName);
        if (createAlias)
            checkCheckbox("addAlias");
        else
            uncheckCheckbox("addAlias");
        // confirm rename:
        clickButton("Rename");
        // verify that we're not on an error page with a check for a new folder link:
        assertLinkPresentWithText(newFolderName);
        assertLinkNotPresentWithText(folderName);
    }

    @LogMethod
    public void moveFolder(String projectName, String folderName, String newParent, boolean createAlias)
    {
        log("Moving folder [" + folderName + "] under project [" + projectName + "] to [" + newParent + "]");
        clickLinkWithText(projectName);
        clickLinkWithText(folderName);
        ensureAdminMode();
        goToFolderManagement();
        waitForExt4FolderTreeNode(folderName, 10000);
        clickButton("Move");
        if (createAlias)
            checkCheckbox("addAlias");
        else
            uncheckCheckbox("addAlias");
        // Select Target
        waitForElement(Locator.permissionsTreeNode(newParent), 10000);
        selectFolderTreeItem(newParent);
        // move:
        clickButton("Confirm Move");
        // verify that we're not on an error page with a check for folder link:
        assertLinkPresentWithText(folderName);
        assertLinkPresentWithText(newParent);
    }

    public void expandFolder(String folder)
    {
        waitForElement(Locator.css(".labkey-expandable-nav-panel"));
        String xpath = "//tr[not(ancestor-or-self::*[contains(@style,'none')]) and following-sibling::tr[contains(@style,'none')]//a[text()='"+folder+"']]//a/img[contains(@src,'plus')]";
        List<WebElement> possibleAncestors = _driver.findElements(By.xpath(xpath));
        int depth = 0;
        while(possibleAncestors.size() > 0 && depth < 10)
        {
            possibleAncestors.get(possibleAncestors.size()-1).click(); // the last one in the list is the actual ancestor.
            possibleAncestors = _driver.findElements(By.xpath(xpath));
            depth++;
        }
        assertElementVisible(Locator.css(".labkey-nav-tree-text").withText(folder));
    }

    /**
     * Expand any necessary nodes in the left nav bar and click a link to a project or folder
     * @param folder
     */
    public void clickFolder(String folder)
    {
        expandFolder(folder);
        clickLinkWithText(folder);
    }

    /**
     * Delete specified project during test
     * Note: Use {@link #deleteProject(String, boolean)} for test cleanup
     * @param project Project display name
     */
    public void deleteProject(String project)
    {
        deleteProject(project, true, 90000); // Wait for 90 seconds for project deletion
    }

    public void deleteProject(String project, boolean failIfFail)
    {
        deleteProject(project, failIfFail, 90000); // Wait for 90 seconds for project deletion
    }

    public void deleteProject(String project, Boolean failIfFail, int wait)
    {
        _containerHelper.deleteProject(project, failIfFail, wait);
    }

    @LogMethod
    public void enableEmailRecorder()
    {
        try {
            getHttpGetResponse(WebTestHelper.getBaseURL() + "/dumbster/setRecordEmail.view?record=true", PasswordUtil.getUsername(), PasswordUtil.getPassword());}
        catch (IOException e) {
            Assert.fail("Failed to enable email recorder");}
        catch (HttpException e) {
            Assert.fail("Failed to enable email recorder");}
    }

    public void addWebPart(String webPartName)
    {
        Locator.css("option").withText(webPartName).waitForElmement(_driver, WAIT_FOR_JAVASCRIPT);
        Locator.XPathLocator form = Locator.xpath("//form[contains(@action,'addWebPart.view')][.//option[text()='"+webPartName+"']]");
        selectOptionByText(form.append("//select"), webPartName);
        submit(form);
    }

    public void removeWebPart(String webPartTitle)
    {
        Locator.XPathLocator removeButton = Locator.xpath("//tr[th[@title='"+webPartTitle+"']]//a[img[@title='Remove From Page']]");
        int startCount = getElementCount(removeButton);
        click(removeButton);
        waitForElementToDisappear(removeButton.index(startCount), WAIT_FOR_JAVASCRIPT);
    }

    public boolean isTitleEqual(String match)
    {
        return match.equals(_driver.getTitle());
    }

    public void assertTitleEquals(String match)
    {
        Assert.assertEquals("Wrong page title", match, _driver.getTitle());
    }

    public void assertTitleContains(String match)
    {
        String title = _driver.getTitle();
        Assert.assertTrue("Page title: '"+title+"' doesn't contain '"+match+"'", title.contains(match));
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
        Assert.assertTrue("Form '" + form + "' was not present", isFormPresent(form));
    }

    public void assertNoLabkeyErrors()
    {
        assertElementNotPresent(Locator.xpath("//div[@class='labkey-error']"));
        assertElementNotPresent(Locator.xpath("//font[@class='labkey-error']"));
    }

    public void assertLabkeyErrorPresent()
    {
        Assert.assertTrue("No errors found", isElementPresent(Locator.xpath("//div[@class='labkey-error']")) ||
            isElementPresent(Locator.xpath("//font[@class='labkey-error']")));

    }

    public boolean isTextPresent(String... texts)
    {
        if(texts==null || texts.length == 0)
            return true;

        String source = getHtmlSource();

        for (String text : texts)
        {
            text = text.replace("&", "&amp;");
            text = text.replace("<", "&lt;");
            text = text.replace(">", "&gt;");
            if (!source.contains(text))
                return false;
        }
        return true;
    }

    public List<String> getMissingTexts(String... texts)
    {
        List<String> missingTexts = new ArrayList<String>();
        if(texts==null || texts.length == 0)
            return missingTexts;

        String source = getHtmlSource();

        for (String text : texts)
        {
            text = text.replace("&", "&amp;");
            text = text.replace("<", "&lt;");
            text = text.replace(">", "&gt;");
            if (!source.contains(text))
                missingTexts.add(text);
        }
        return missingTexts;
    }

    public String getText(Locator elementLocator)
    {
        WebElement el = elementLocator.findElement(_driver);
        return el.getText();
    }

    /**
     * Verifies that all the strings are present in the page html source
     * @param texts
     */
    public void assertTextPresent(String... texts)
    {
        if(texts==null)
            return;

        List<String> missingTexts = getMissingTexts(texts);

        if (missingTexts.size() > 0)
        {
            String failMsg = (missingTexts.size() == 1 ? "Text '" : "Texts ['") + missingTexts.get(0) + "'";
            for (int i = 1; i < missingTexts.size(); i++)
            {
                failMsg += ", '" + missingTexts.get(i) + "'";
            }
            failMsg += missingTexts.size() == 1 ? " was not present" : "] were not present";
            Assert.fail(failMsg);
        }
    }

    /**
     * Verifies that all the strings are present in the page html source
     * @param texts
     */
    public void assertTextPresent(List<String> texts)
    {
        String[] textsArray = {};
        textsArray = texts.toArray(textsArray);
        assertTextPresent(textsArray);
    }

    //takes the arguments used to set a filter and transforms them into the description in the grid view
    //then verifies that this description is present
    public void assertFilterTextPresent(String column, String type, String value)
    {
        String desc = type + value;
        if(type.contains("Equals One Of"))
        {
            desc = "IS ONE OF (" + value.replace(";", ", ") + "))";
        }
        else if(type.contains("Contains One Of"))
        {
            desc = "CONTAINS ONE OF (" + value.replace(";", ", ") + "))";
        }
        else if(type.contains("Does Not Equal Any Of"))
        {
            desc = "IS NOT ANY OF (" + value.replace(";", ", ") + "))";
        }
        else if(type.contains("Does Not Contain Any Of"))
        {
            desc = "DOES NOT CONTAIN ANY OF (" + value.replace(";", ", ") + "))";
        }
        else if(type.equals("Equals"))
        {
            desc = column +  " = " + value;
        }
        else if(type.contains("Start") || type.contains("Contain"))    //Starts With, Does Not Start With, Contains, Does not Contain
        {
            desc = column + " " + type.toUpperCase() + " " + value;
        }
        else if(type.equals("Does Not Equal"))
        {
            desc = column + " <> " + value;
        }
        else if(type.contains("Greater"))
        {
            desc = column + " >";
            if(type.contains("Equal To"))
                desc+="=";
            desc += " " + value;
        }
        else if(type.contains("Less"))
        {
            desc = column + " <";
            if(type.contains("Equal To"))
                desc+="=";
            desc += " " + value;
        }
        else if(type.contains("Blank"))
        {
            desc = "NULL";
        }

        assertTextPresent(desc);

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
                    Assert.assertTrue("Text '" + text + "' was not present " + amount + " times.  It was present " + count + " times", count == amount);
            }
            else
                Assert.assertTrue("Text '" + text + "' was not present " + amount + " times.  It was present " + count + " times", count == amount);
        }
    }

    public int countText(String text)
    {
        text = text.replace("&nbsp;", " ");
        String html = getHtmlSource();
        // Strip all JavaScript tags; in particular, the selenium-injected javascript tag, which can foul up the expected occurrences
        String source = html.replaceAll("(?msi)<script type=\"text/javascript\">.*?</script>", "");
        int current_index = 0;
        int count = 0;

        while ((current_index = source.indexOf(text, current_index + 1)) != -1)
            count++;
        return count;
    }

    public void assertTextNotPresent(String... texts)
    {
        if(texts==null)
            return;

        for(String text : texts)
        {
            text = text.replace("&nbsp;", " ");
            Assert.assertFalse("Text '" + text + "' was present", isTextPresent(text));
        }
    }

    public String getTextInTable(String dataRegion, int row, int column)
    {
        String id = Locator.xq(dataRegion);
        return _driver.findElement(By.xpath("//table[@id="+id+"]/tbody/tr["+row+"]/td["+column+"]")).getText();
    }

    public void assertTextAtPlaceInTable(String textToCheck, String dataRegion, int row, int column)
    {
       Assert.assertTrue(textToCheck+" is not at that place in the table", textToCheck.compareTo(getTextInTable(dataRegion, row, column))==0);
    }

    /**
     * Searches only the displayed text in the body of the page, not the HTML source.
     */
    public boolean isTextBefore(String text1, String text2)
    {
        String source = getBodyText();
        return (source.indexOf(text1) < source.indexOf(text2));
    }

    /**
     *
     * @param text
     * @return null = yes, present in this order
     * otherwise returns out of order string and explanation of error
     */
    public String isPresentInThisOrder(String... text)
    {
        String source = getBodyText();
        int previousIndex = -1;
        String previousString = null;

        for (String s : text)
        {
            int index = source.indexOf(s);

            if(index == -1)
                return s + " not found";
            if(index <= previousIndex)
                return s + " occured out of order";
            previousIndex = index;
            previousString = s;
        }
        return null;
    }
    // Searches only the displayed text in the body of the page, not the HTML source.
    public void assertTextPresentInThisOrder(String... text)
    {
        String success = isPresentInThisOrder(text);
        Assert.assertTrue(success, success==null);
    }

    public void assertTextBefore(String text1, String text2)
    {
        assertTextPresentInThisOrder(text1, text2);
    }

    /**
     * @deprecated Wait for specific elements on the the target page. selenium.waitForPageToLoad is unpredictable under WebDriver
     * @param millis milliseconds to wait before timing out
     */
    @Deprecated public void waitForPageToLoad(int millis)
    {
        _testTimeout = true;
        selenium.waitForPageToLoad(Integer.toString(millis));
        _testTimeout = false;
    }

    /**
     * @deprecated Wait for specific elements on the the target page. selenium.waitForPageToLoad is unpredictable under WebDriver
     */
    @Deprecated public void waitForPageToLoad()
    {
        waitForPageToLoad(defaultWaitForPage);
    }


    public void waitForExtReady()
    {
        waitForElement(Locator.id("seleniumExtReady"), defaultWaitForPage);
    }

    public boolean doesElementAppear(Checker checker, int wait)
    {
        Long startTime = System.currentTimeMillis();
        while ( (System.currentTimeMillis() - startTime) < wait )
        {
            if( checker.check() )
                return true;
            sleep(100);
        }
        if (!checker.check())
        {
            _testTimeout = true;
            return false;
        }
        return false;
    }

    public interface ElementChecker
    {
        public boolean check(WebElement el);
    }

    public List<WebElement> filterElements(ElementChecker checker, List<WebElement> elements)
    {
        List<WebElement> filteredElements = new ArrayList<WebElement>();
        for (WebElement el : elements)
        {
            if (checker.check(el))
                filteredElements.add(el);
        }
        return filteredElements;
    }

    public void waitFor(Checker checker, String failMessage, int wait)
    {
        if (!doesElementAppear(checker, wait))
            Assert.fail(failMessage + " ["+wait+"ms]");
    }

    //like wait for ExtMask, but waits for a draggable mask (for example, the file rename mask)
    public void waitForDraggableMask()
    {
        waitForDraggableMask(WAIT_FOR_JAVASCRIPT);
    }

    //like wait for ExtMask, but waits for a draggable mask (for example, the file rename mask)
    public void waitForDraggableMask(int wait)
    {
        waitForElement(Locator.xpath("//div[contains(@class, 'x-window-draggable')]"), wait);

    }

    public void waitForAlert(String alertText, int wait)
    {
        waitFor(new Checker(){public boolean check(){return isAlertPresent();}}, "No alert appeared.", wait);
        assertAlert(alertText);
    }

    protected File getTestTempDir()
    {
        File buildDir = new File(getLabKeyRoot(), "build");
        return new File(buildDir, "testTemp");
    }

    /**
     * pre-condition: on Views and Scripting Configuration page
     * @return is a Perl enginge configured?
     */
    public boolean isPerlEngineConfigured()
    {
        waitForElement(Locator.xpath("//div[@id='enginesGrid']//td//div[.='js']"), WAIT_FOR_JAVASCRIPT);

        return isElementPresent(Locator.xpath("//div[@id='enginesGrid']//td//div[.='pl']"));
    }

    /**
     * pre-condition: on Views and Scripting Configuration page
     * @return is an R enginge configured?
     */
    public boolean isREngineConfigured()
    {
        // need to allow time for the server to return the engine list and the ext grid to render
        // wait for mozilla rhino (should be automatically included for all installations)
        waitForElement(Locator.xpath("//div[@id='enginesGrid']//td//div[.='js']"), WAIT_FOR_JAVASCRIPT);

        return isElementPresent(Locator.xpath("//div[@id='enginesGrid']//td//div[.='R,r']"));
    }

    protected void setSelectedFields(String containerPath, String schema, String query, String viewName, String[] fields)
    {
        pushLocation();
        beginAt("/query" + containerPath + "/internalNewView.view");
        setFormElement(Locator.name("ff_schemaName"), schema);
        setFormElement(Locator.name("ff_queryName"), query);
        if (viewName != null)
            setFormElement(Locator.name("ff_viewName"), viewName);
        submit();
        StringBuilder strFields = new StringBuilder(fields[0]);
        for (int i = 1; i < fields.length; i ++)
        {
            strFields.append("&");
            strFields.append(fields[i]);
        }
        setFormElement(Locator.name("ff_columnList"), strFields.toString());
        submit();
        popLocation();
    }

    protected void clickExportToText()
    {
        clickButton("Export", 0);
        _extHelper.clickSideTab("Text");
        clickButton("Export to Text");
    }

    /**
     * Use UI to export data region
     * note that Selenium/Firefox currently can't handle the dialogue that will pop up if you choose anything but script
     * @param tab   Excel, Text, or Script
     * @param type the specific radiobutton to choose
     */
    protected void exportDataRegion(String tab, String type)
    {
        clickButton("Export", 0);
        waitForText("Script");
        sleep(1500);
        _extHelper.clickSideTab(tab);
        if(type!=null)
        {
            click(Locator.xpath("//tr[td[contains(text()," +  type + ")]]/td/input"));
        }
        if(tab.equals("Script"))
        {
            clickButtonContainingText("Create Script", 0);
        }
        else
        {
            clickButtonContainingText("Export To " + tab, 0);
        }
    }

    protected void exportFolderAsZip()
    {
        goToFolderManagement();
        clickLinkWithText("Export");
        checkRadioButton("location", 1);

    }

    public void setModuleProperties(Map<String, List<String[]>> props)
    {
        goToFolderManagement();
        log("setting module properties");
        clickLinkWithText("Module Properties");
        waitForText("Save Changes");
        boolean changed = false;
        for (String moduleName : props.keySet())
        {
            for (String[] array : props.get(moduleName))
            {
                log("setting property: " + array[1] + " for container: " + array[0] + " to value: " + array[2]);
                Map<String, String> map = new HashMap<String, String>();
                map.put("moduleName", moduleName);
                map.put("containerPath", array[0]);
                map.put("propName", array[1]);
                waitForText(array[1]); //wait for the property name to appear
                String query = ComponentQuery.fromAttributes("field", map);
                Ext4FieldRefWD ref = _ext4Helper.queryOne(query, Ext4FieldRefWD.class);
                String val = (String)ref.getValue();
                if(StringUtils.isEmpty(val) || !val.equals(array[2]))
                {
                    changed = true;
                    ref.setValue(array[2]);
                }
            }
        }
        if (changed)
        {
            clickButton("Save Changes", 0);
            waitForText("Properties saved");
            clickButton("OK", 0);
        }
        else
        {
            log("properties were already set, no changed needed");
        }
    }

    public interface Checker extends BaseSeleniumWebTest.Checker
    {
        public boolean check();
    }

    public void waitForExt4FolderTreeNode(String nodeText, int wait)
    {
        final Locator locator = Locator.xpath("//tr[contains(@class, 'x4-grid-row')]/td/div[text()=" + Locator.xq(nodeText) + "]");
        String failMessage = "Ext 4 Tree Node with locator " + locator + " did not appear.";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return isElementPresent(locator);
            }
        }, failMessage, wait);
    }

    public void waitForElement(final Locator locator)
    {
        waitForElement(locator, WAIT_FOR_JAVASCRIPT);
    }

    /**
     *
     * @param locator Element to wait for
     * @param elementTimeout amount of time to wait for
     * @param failIfNotFound should fail if element is not found?  If not, will return false
     * @return
     */
    public boolean waitForElement(final Locator locator, int elementTimeout, boolean failIfNotFound)
    {
        if (failIfNotFound)
        {
            locator.waitForElmement(_driver, elementTimeout);
        }
        else
        {
            try
            {
                locator.waitForElmement(_driver, elementTimeout);
            }
            catch(Exception e)
            {
                /*ignore*/
                return false;
            }
        }
        return true;
    }

    public void waitForElement(final Locator locator, int wait)
    {
        waitForElement(locator, wait, true);
    }

    public void waitForElementToDisappear(final Locator locator, int wait)
    {
        locator.waitForElmementToDisappear(_driver, wait);
    }

    public void waitForTextToDisappear(final String text)
    {
        waitForTextToDisappear(text, WAIT_FOR_JAVASCRIPT);
    }

    public void waitForTextToDisappear(final String text, int wait)
    {
        String failMessage = "Text: " + text + " was still present after [" + wait + "ms]";
//        if(getBrowser().equals("*iexploreproxy"))
//        {
//            // IE can't detect some ext elements disappearing
//            sleep(10000);
//            return;
//        }
        waitFor(new Checker()
        {
            public boolean check()
            {
                return !isTextPresent(text);
            }
        }, failMessage, wait);
    }

    public void waitForText(final String text)
    {
         waitForText(text, WAIT_FOR_JAVASCRIPT);
    }

    public void waitForTextWithRefresh(String text, int wait)
    {
        for(int i=0; i<wait; i+=1000)
        {
            if(isTextPresent(text))
                return;
            else
                sleep(1000);
            refresh();
        }
        Assert.fail(text + " did not appear");
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

    public void waitForText(final String text, final int count, int wait)
    {
        final String failMessage = "'"+text+"' was not present "+count+" times.";
        waitFor(new Checker()
        {
            public boolean check()
            {
                int actualCount = countText(text);
                if (actualCount != count)
                {
                    return false;
                }
                return true;
            }
        }, failMessage, wait);
    }

    public void submit()
    {
        WebElement form = _driver.findElement(By.xpath("//td[@id='bodypanel']//form[1]"));
        submit(form);
    }

    public void submit(Locator formLocator)
    {
        WebElement form = formLocator.findElement(_driver);
        submit(form);
    }

    public void submit(WebElement form)
    {
        executeScript("arguments[0].submit()", form);
        waitForPageToLoad();
    }

    public void submit(String buttonName)
    {
        Locator l = findButton(buttonName);

        Assert.assertTrue("Button with name '" + buttonName + "' not found", null != l);

        click(l);
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
        return loc.findElements(_driver).size() > 0;
    }

    public void assertElementPresent(Locator loc)
    {
        Assert.assertTrue("Element '" + loc + "' is not present", isElementPresent(loc));
    }

    /**
     * @deprecated Use {@link #assertElementPresent(Locator, int)}
     * @param loc
     * @param amount
     */
    @Deprecated
    public void assertElementPresent(Locator.XPathLocator loc, int amount)
    {
        Assert.assertEquals("Xpath '" + loc.getPath() + "' not present expected number of times.", amount, getXpathCount(loc));
    }

    public void assertElementPresent(Locator loc, int amount)
    {
        Assert.assertEquals("Element '" + loc + "' is not present " + amount + " times", amount, getElementCount(loc));
    }

    public void assertElementContains(Locator loc, String text)
    {
        String elemText = loc.findElement(_driver).getText();
        if(elemText == null)
            Assert.fail("The element at location " + loc.toString() + " contains no text! Expected '" + text + "'.");
        if(!elemText.contains(text))
            Assert.fail("The element at location '" + loc.toString() + "' contains '" + elemText + "'; expected '" + text + "'.");
    }

    public boolean elementContains(Locator loc, String text)
    {
        String elemText = loc.findElement(_driver).getText();
        return (elemText != null && elemText.contains(text));
    }

    public void assertFormElementEquals(String elementName, String value)
    {
        assertFormElementEquals(Locator.name(elementName), value);
    }

    public void waitForFormElementToEqual(final Locator locator, final String value)
    {
        String failMessage = "Field with locator " + locator + " did not equal " + value + ".";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return value.equals(getFormElement(locator));
            }
        }, failMessage, WAIT_FOR_JAVASCRIPT);
    }

    public void waitForFormElementToNotEqual(final Locator locator, final String value)
    {
        String failMessage = "Field with locator " + locator + " did not equal " + value + ".";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return !value.equals(getFormElement(locator));
            }
        }, failMessage, WAIT_FOR_JAVASCRIPT);
    }

    public String getFormElement(Locator loc)
    {
        return loc.findElement(_driver).getAttribute("value");
    }

    /**
     * @deprecated Use explicit Locator: {@link #getFormElement(Locator)}
     */
    @Deprecated public String getFormElement(String elementName)
    {
        Locator loc = Locator.name(elementName);
        return getFormElement(loc);
    }

    public void assertFormElementEquals(Locator loc, String value)
    {
        Assert.assertEquals("Form element '" + loc + "' was not equal to '" + value + "'", value, getFormElement(loc));
    }

    public void assertFormElementNotEquals(Locator loc, String value)
    {
        Assert.assertNotSame("Form element '" + loc + "' was equal to '" + value + "'", value, getFormElement(loc));
    }

    public boolean isFormElementPresent(String elementName)
    {
        return isElementPresent(Locator.xpath(firstForm + "['" + elementName + "']"));
    }

    public void assertFormElementPresent(String elementName)
    {
        Assert.assertTrue("Form element '" + elementName + "' was not present", isFormElementPresent(elementName));
    }

    /**
     * @deprecated Use specific locator for form element {@link BaseWebDriverTest#assertOptionEquals(Locator, String)}
     * @param selectName
     * @param value
     */
    @Deprecated
    public void assertOptionEquals(String selectName, String value)
    {
        assertOptionEquals(Locator.name(selectName), value);
    }

    public void assertOptionEquals(Locator loc, String value)
    {
        Assert.assertEquals("Option '" + loc + "' was not equal '" + value + "'", value, getSelectedOptionText(loc));
    }

    public String getSelectedOptionText(Locator loc)
    {
        Select select = new Select(loc.findElement(_driver));
        return select.getFirstSelectedOption().getText();
    }

    public String getSelectedOptionValue(Locator loc)
    {
        Select select = new Select(loc.findElement(_driver));
        return select.getFirstSelectedOption().getAttribute("value");
    }

    /**
     * @deprecated Use {@link #getSelectedOptionText(Locator)}
     * @param selectName
     * @return
     */
    @Deprecated
    public String getSelectedOptionText(String selectName)
    {
        return getSelectedOptionText(Locator.name(selectName));
    }

    public void assertElementNotPresent(String errorMsg, Locator loc)
    {
        Assert.assertFalse(errorMsg, isElementPresent(loc));
    }

    public void assertElementNotPresent(Locator loc)
    {
        assertElementNotPresent("Element was present in page: " + loc, loc);
    }

    public void assertElementNotVisible(Locator loc)
    {
        Assert.assertFalse("Element was visible in page: " + loc, loc.findElement(_driver).isDisplayed());
    }

    public void assertElementVisible(Locator loc)
    {
        Assert.assertTrue("Element was not visible: " + loc, loc.findElement(_driver).isDisplayed());
    }

    public boolean isLinkPresent(String linkId)
    {
        return isElementPresent(Locator.tagWithId("a", linkId));
    }

    public void assertLinkPresent(String linkId)
    {
        Assert.assertTrue("Link with id '" + linkId + "' was not present", isLinkPresent(linkId));
    }

    public void assertLinkNotPresent(String linkId)
    {
        Assert.assertFalse("Link with id '" + linkId + "' was present", isLinkPresent(linkId));
    }

    public boolean isLinkPresentWithText(String text)
    {
        log("Checking for link with exact text '" + text + "'");
        return isElementPresent(Locator.linkWithText(text)) || isElementPresent(Locator.linkWithText(text.toUpperCase()));
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
        Assert.assertTrue("Could not find link containing text '" + text + "'", isLinkPresentContainingText(text));
    }

    public void assertLinkPresentWithText(String text)
    {
        Assert.assertTrue("Could not find link with text '" + text + "'", isLinkPresentWithText(text));
    }

    public void assertLinkNotPresentWithText(String text)
    {
        Assert.assertFalse("Found a link with text '" + text + "'", isLinkPresentWithText(text));
    }

    public void assertAtUserUserLacksPermissionPage()
    {
        assertTextPresent(PERMISSION_ERROR);
        assertTitleEquals("401: Error Page -- 401: User does not have permission to perform this operation");
    }

    public boolean isLinkPresentWithTitle(String title)
    {
        log("Checking for link with exact title '" + title + "'");
        return isElementPresent(Locator.linkWithTitle(title));
    }

    public void assertLinkPresentWithTitle(String title)
    {
        Assert.assertTrue("Could not find link with title '" + title + "'", isLinkPresentWithTitle(title));
    }

    public void assertLinkNotPresentWithTitle(String title)
    {
        Assert.assertFalse("Found a link with title '" + title + "'", isLinkPresentWithTitle(title));
    }

    /** Find a link with the exact text specified, click it, and wait for the page to load */
    public void clickLinkWithText(String text)
    {
        clickLinkWithText(text, true);
    }

    /** Find nth link with the exact text specified, click it, and wait for the page to load */
    public void clickLinkWithText(String text, int index)
    {
        Locator l = Locator.linkWithText(text, index);
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
        clickLinkWithText(text, index, wait ? defaultWaitForPage : 0);
    }

    /** Find nth link with the exact text specified, click it, and wait up to millis for the page to load */
    public void clickLinkWithText(String text, int index, int millis)
    {
        log("Clicking link with text '" + text + "'");

        if (_driver.findElements(By.linkText(text)).size() == 0 && _driver.findElements(By.linkText(text.toUpperCase())).size() > 0)
            clickAndWait(Locator.linkWithText(text.toUpperCase()).index(index), millis); // Links might be all caps after CSS is applied
        else
            clickAndWait(Locator.linkWithText(text).index(index), millis);
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
        return Locator.linkWithText(text).findElements(_driver).size();
    }

    public void assertLinkPresentWithTextCount(String text, int count)
    {
        Assert.assertEquals("Link with text '" + text + "' was not present the expected number of times", count, countLinksWithText(text));
    }

    public boolean isLinkPresentWithImage(String imageName)
    {
        return isElementPresent(Locator.linkWithImage(imageName));
    }

    public void assertLinkPresentWithImage(String imageName)
    {
        Assert.assertTrue("Link with image '" + imageName + "' was not present", isLinkPresentWithImage(imageName));
    }

    public void assertLinkNotPresentWithImage(String imageName)
    {
        Assert.assertFalse("Link with image '" + imageName + "' was present", isLinkPresentWithImage(imageName));
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

    /**
     * @deprecated Use {@link #clickAt(Locator, int, int)}
     */
    @Deprecated public void clickAt(Locator l, String coord)
    {
        String[] splitCoord = coord.split(",");
        Integer xCoord = Integer.parseInt(splitCoord[0]);
        Integer yCoord = Integer.parseInt(splitCoord[1]);

        clickAt(l, xCoord, yCoord);
    }

    public void clickAt(Locator l, int xCoord, int yCoord)
    {
        clickAt(l, xCoord, yCoord, WAIT_FOR_PAGE);
    }

    public void clickAt(Locator l, int xCoord, int yCoord, int pageTimeout)
    {
        WebElement el = l.waitForElmement(_driver, WAIT_FOR_JAVASCRIPT);

        Actions builder = new Actions(_driver);
        builder.moveToElement(el, xCoord, yCoord)
                .click()
                .build()
                .perform();

        waitForPageToLoad(pageTimeout);
    }

    public void clickAndWait(Locator l)
    {
        clickAndWait(l, defaultWaitForPage);
    }

    public static final int WAIT_FOR_EXT_MASK_TO_DISSAPEAR = -1;
    public static final int WAIT_FOR_EXT_MASK_TO_APPEAR = -2;

    public void clickAndWait(Locator l, int pageTimeoutMs)
    {
        WebElement el;
        el = l.findElement(_driver);

        el.click();

        if (pageTimeoutMs > 0)
            waitForPageToLoad(pageTimeoutMs);
        else if(pageTimeoutMs==WAIT_FOR_EXT_MASK_TO_APPEAR)
            _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);
        else if(pageTimeoutMs==WAIT_FOR_EXT_MASK_TO_DISSAPEAR)
            _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
    }

    /**
     * @deprecated Use {@link #clickAt(Locator, int, int, int)}
     */
    @Deprecated public void clickAtAndWait(Locator l, String coord, int millis)
    {
        String[] splitCoord = coord.split(",");
        Integer xCoord = Integer.parseInt(splitCoord[0]);
        Integer yCoord = Integer.parseInt(splitCoord[1]);

        clickAt(l, xCoord, yCoord, millis);
    }

    public void doubleClick(Locator l)
    {
        doubleClickAndWait(l, 0);
    }

    public void doubleClickAndWait(Locator l, int millis)
    {
        Actions action = new Actions(_driver);
        action.doubleClick(l.findElement(_driver)).perform();
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
        Actions builder = new Actions(_driver);
        builder.moveToElement(l.findElement(_driver)).build().perform();
    }

    /**
     * @deprecated Click shenanigans shouldn't be necessary with WebDriver. If click doesn't work, try WebDriver 'Actions'
     */
    @Deprecated public void mouseDown(Locator l)
    {
        l.findElement(_driver).click();
    }

    /**
     * @deprecated Click shenanigans shouldn't be necessary with WebDriver. If click doesn't work, try WebDriver 'Actions'
     */
    @Deprecated
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
        dragAndDrop(from, to, Position.top);
    }

    public enum Position
    {top, bottom, middle}

    public void dragAndDrop(Locator from, Locator to, Position pos)
    {
        WebElement fromEl = from.findElement(_driver);
        WebElement toEl = to.findElement(_driver);

        int y;
        if ( pos == Position.top )
            y = 1;
        else if ( pos == Position.bottom )
            y = toEl.getSize().getHeight() - 1;
        else // pos == Position.middle
            y = toEl.getSize().getHeight() / 2;

        Actions builder = new Actions(_driver);
        builder.clickAndHold(fromEl).moveToElement(toEl, 1, y).release().build().perform();
    }

    public void dragAndDrop(Locator el, int xOffset, int yOffset)
    {
        WebElement fromEl = el.findElement(_driver);

        Actions builder = new Actions(_driver);
        builder.clickAndHold(fromEl).moveByOffset(xOffset + 1, yOffset + 1).release().build().perform();
    }

    public void clickTab(String tabname)
    {
        log("Selecting tab " + tabname);
        clickLink(getTabLinkId(tabname));
    }

    public void verifyTabSelected(String caption)
    {
        Assert.assertTrue("Tab not selected: " + caption, isElementPresent(Locator.xpath("//li[contains(@class, labkey-tab-active)]/a[text() = '"+caption+"']")));
    }

    public void clickImageWithTitle(String title, int mills)
    {
        Locator l = Locator.tagWithAttribute("img", "title", title);
        clickAndWait(l, mills);
    }

//    public void clickImageWithSrc(String src)
//    {
//        Locator l = Locator.tagWithAttribute("img", "title", title);
//    }

    public void clickImageWithAltText(String altText, int millis)
    {
        log("Clicking first image with alt text " + altText );
        Locator l = Locator.tagWithAttribute("img", "alt", altText);
        boolean present = isElementPresent(l);
        if (!present)
            Assert.fail("Unable to find image with altText " + altText);
        clickAndWait(l, millis);
    }

    public void clickImageWithAltText(String altText)
    {
        clickImageWithAltText(altText, defaultWaitForPage);
    }

    public int getImageWithAltTextCount(String altText)
    {
        String js = "function countImagesWithAlt(txt) {" +
                        "var doc=document;" +
                        "var count = 0;" +
                        "for (var i = 0; i < doc.images.length; i++) {" +
                            "if (doc.images[i].alt == txt) " +
                                "count++;" +
                            "}" +
                        "return count;" +
                    "};" +
                    "return countImagesWithAlt('" + altText + "');";
        return Integer.parseInt(executeScript(js).toString());
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
        Assert.assertTrue(isImagePresentWithSrc(src));
    }

    public void assertImagePresentWithSrc(String src, boolean substringMatch)
    {
        Assert.assertTrue(isImagePresentWithSrc(src, substringMatch));
    }

    public String getTableCellText(String tableId, int row, int column)
    {
        return getText(Locator.xpath("//table[@id="+Locator.xq(tableId)+"]/tbody/tr["+(row+1)+"]/*[(name()='TH' or name()='TD' or name()='th' or name()='td') and position() = "+(column+1)+"]"));
    }

    public Locator getSimpleTableCell(Locator.XPathLocator table, int row, int column)
    {
        return Locator.xpath(table.toXpath() + "/tbody/tr["+(row+1)+"]/td[" + (column +1)  + "]");
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
        Assert.assertEquals(tableName + "." + String.valueOf(row) + "." + String.valueOf(column) + " != \"" + value + "\"", value, getTableCellText(tableName, row, column));
    }

    public void assertTableCellTextEquals(String tableName, int row, String columnTitle, String value)
    {
        assertTableCellTextEquals(tableName, row, getColumnIndex(tableName, columnTitle), value);
    }

    public void assertTableCellContains(String tableName, int row, int column, String... strs)
    {
        String cellText = getTableCellText(tableName, row, column);

        for (String str : strs)
        {
            Assert.assertTrue(tableName + "." + row + "." + column + " should contain \'" + str + "\' (actual value is " + cellText + ")", cellText.contains(str));
        }
    }

    public void assertTableCellContains(String tableName, int row, String columnTitle, String... strs)
    {
        assertTableCellContains(tableName, row, getColumnIndex(tableName, columnTitle), strs);
    }

    public void assertTableCellNotContains(String tableName, int row, int column, String... strs)
    {
        String cellText = getTableCellText(tableName, row, column);

        for (String str : strs)
        {
            Assert.assertFalse(tableName + "." + row + "." + column + " should not contain \'" + str + "\'", cellText.contains(str));
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
        Assert.assertTrue("Table cells not equal: " + tableNameA + "." + String.valueOf(rowA) + "." + String.valueOf(columnA) + " & " + tableNameB + "." + String.valueOf(rowB) + "." + String.valueOf(columnB), areTableCellsEqual(tableNameA, rowA, columnA, tableNameB, rowB, columnB));
    }

    /*
    getColumnIndex works for standard labkey data grids
     */
    public int getColumnIndex(String tableName, String columnTitle)
    {
        int col = Locator.xpath("//table[@id='"+tableName+"']/tbody/tr[contains(@id, 'dataregion_column_header_row') and not(contains(@id, 'spacer'))]/td[./div/.='"+columnTitle+"']/preceding-sibling::*").findElements(_driver).size();
        if(col == 0)
            Assert.fail("Column '" + columnTitle + "' not found in table '" + tableName + "'");

        return col;
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
        List<String> statusValues = getTableColumnValues("dataregion_StatusFiles", "Status");
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
        File dumpDir = ensureDumpDir();

        // moves all files under @path, created by the test, to the TeamCity publish directory
        ArrayList<File> files = listFilesRecursive(new File(path), new NonSVNFilter());
        for (File file : files)
        {
            if ( file.isFile() )
            {
                File dest = new File(dumpDir, file.getParent().substring(path.length()));
                if (!dest.exists())
                    dest.mkdirs();
                file.renameTo(new File(dest, file.getName()));
            }
        }
    }

    private void dumpPipelineLogFiles(String path)
    {
        File dumpDir = ensureDumpDir();

        // moves all .log files under @path, created by the test, to the TeamCity publish directory
        ArrayList<File> files = listFilesRecursive(new File(path), new NonSVNFilter());
        for (File file : files)
        {
            if ( file.isFile() && file.getName().endsWith(".log") )
            {
                File dest = new File(dumpDir, file.getParent().substring(path.length()));
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
        log("Set pipeline to: " + rootPath);
        goToModule("Pipeline");
        clickButton("Setup");

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
        log("Finished setting pipeline to: " + rootPath);
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

    // Returns count of "COMPLETE" and "ERROR"
    public int getFinishedbCount(List<String> statusValues)
    {
        int finsihed = 0;
        for (String statusValue : statusValues)
            if ("COMPLETE".equals(statusValue) || "ERROR".equals(statusValue))
                finsihed++;
        return finsihed;
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

    public List<String> getTableColumnValues(String tableName, String columnName)
    {
        int index = getColumnIndex(tableName, columnName);
        return getTableColumnValues(tableName, index);
    }



    public void showAllInTable()
    {
        showNumberInTable("All");
    }

    public void showNumberInTable(String shareValue)
    {
        clickButton("Page Size", 0);
        waitForText("100 per page");
        Locator l = Locator.id("Page Size:" + shareValue);
        clickAndWait(l);
    }


    public void show100InTable()
    {
        showNumberInTable("100");
    }

    /**get values for all specifed columns for all pages of the table
     * preconditions:  must be on start page of table
     * postconditions:  at start of table
     * @param tableName
     * @param columnNames
     * @return
     */
    protected  List<List<String>> getColumnValues(String tableName, String... columnNames)
    {
        boolean moreThanOnePage = isTextPresent("Next");
        if(moreThanOnePage)
        {
            showAllInTable();
        }
        List<List<String>> columns = new ArrayList<List<String>>();
        for(int i=0; i<columnNames.length; i++)
        {
            columns.add(new ArrayList<String>());
        }

        DataRegionTable table = new DataRegionTable(tableName, this);
        for(int i=0; i<columnNames.length; i++)
        {
            columns.get(i).addAll(table.getColumnDataAsText(columnNames[i]));
        }

        if(moreThanOnePage)
        {
            show100InTable();
        }
        return columns;
    }

    // Returns the number of rows (both <tr> and <th>) in the specified table
    public int getTableRowCount(String tableName)
    {
        return Locator.xpath("//table[@id=" + Locator.xq(tableName) + "]/thead/tr").findElements(_driver).size() +
               Locator.xpath("//table[@id=" + Locator.xq(tableName) + "]/tbody/tr").findElements(_driver).size();
    }

    public int getTableColumnCount(String tableId)
    {
        return getXpathCount(Locator.xpath("//table[@id="+Locator.xq(tableId)+"]/colgroup/col"));
    }

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
        Assert.assertTrue("Image map '" + imageMapName + "' did not have an area title of '" + areaTitle + "'", isImageMapAreaPresent(imageMapName, areaTitle));
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
            Assert.fail("No button found with text \"" + text + "\" at index " + index);
    }

    public Locator.XPathLocator getButtonLocator(String text, int index)
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

        // check for Ext 4 button:
        locator = Locator.ext4Button(text, index);
        if (isElementPresent(locator))
            return locator;

        return null;
    }

    public Locator.XPathLocator getButtonLocator(String text)
    {
        // check for normal button:
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

        // check for Ext 4 button:
        locator = Locator.ext4Button(text);
        if (isElementPresent(locator))
            return locator;

        // check for GWT button:
        locator = Locator.gwtButton(text);
        if (isElementPresent(locator))
            return locator;

        if (text.equals(text.toUpperCase()))
            return null;
        else
        {
            log("WARNING: Update test. Possible wrong casing for button: " + text);
            return getButtonLocator(text.toUpperCase());
        }
    }

    protected Locator.XPathLocator getButtonLocatorContainingText(String text)
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
    // use clickButton(text, 0) to click a button and continue immediately
    public void clickButton(String text)
    {
        clickButton(text, defaultWaitForPage);
    }

    /**
     * click a button with text text and wait for text waitForText to appear
     * @param text
     * @param waitForText
     */
    public void clickButton(String text, String waitForText)
        {
            clickButton(text, 0);
            waitForText(waitForText);
        }


    public void clickButton(String text, int waitMillis)
    {
        Locator.XPathLocator buttonLocator = getButtonLocator(text);
        if (buttonLocator != null)
            clickAndWait(buttonLocator, waitMillis);
        else if(waitMillis==WAIT_FOR_EXT_MASK_TO_APPEAR)
            _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);

        else if(waitMillis==WAIT_FOR_EXT_MASK_TO_DISSAPEAR)
            _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        else
            Assert.fail("No button found with text \"" + text + "\"");
    }


    public void clickButtonAt(String text, int waitMillis, String coord)
    {
        Locator.XPathLocator buttonLocator = getButtonLocator(text);
        if (buttonLocator != null)
            clickAtAndWait(buttonLocator, coord, waitMillis);
        else
            Assert.fail("No button found with text \"" + text + "\"");
    }

    public void clickButtonContainingText(String text)
    {
        clickButtonContainingText(text, defaultWaitForPage);
    }

    public void clickButtonContainingText(String text, int waitMills)
    {
        Locator.XPathLocator buttonLocator = getButtonLocatorContainingText(text);
        if (buttonLocator != null)
            clickAndWait(buttonLocator, waitMills);
        else
            Assert.fail("No button found with text \"" + text + "\"");
    }

    public void clickButtonContainingText(String buttonText, String textShouldAppearAfterLoading)
    {
        clickButtonContainingText(buttonText, 0);
        waitForText(textShouldAppearAfterLoading, defaultWaitForPage);
    }

    /**
     *  wait for button to appear, click it, wait for page to load
     */
    public void waitAndClickButton(final String text)
    {
        waitAndClickButton(text, defaultWaitForPage);
    }

    public void waitAndClickButton(final String text, final int wait)
    {
        String failMessage = "Button with text '" + text + "' did not appear";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return null != getButtonLocator(text);
            }
        }, failMessage, WAIT_FOR_JAVASCRIPT);
        clickButton(text, wait);
    }


    /**
     *  wait for element, click it, return immediately
     */
    public void waitAndClick(Locator l)
    {
        waitAndClick(WAIT_FOR_JAVASCRIPT, l, 0);
    }


    /**
     *  wait for element, click it, wait for page to load
     */
    public void waitAndClick(int waitFor, Locator l, int waitForPageToLoad)
    {
        waitForElement(l, waitFor);
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
        clickButton(buttonText, 0);
    }

    /**
     * @deprecated Use {@link #setFormElement(Locator, String)}
     * @param elementName
     * @param text
     */
    @Deprecated
    public void setText(String elementName, String text)
    {
        setFormElement(Locator.id(elementName), text, elementName.toLowerCase().contains("password"));
    }

    /**
     * @deprecated Use explicit Locator: {@link #setFormElement(Locator, String)}
     */
    @Deprecated public void setFormElement(String name, String text)
    {
        if(_driver.findElements(By.name(name)).size() > 0)
            setFormElement(Locator.name(name), text, false);
        else
            setFormElement(Locator.id(name), text, false);
    }

    public void setFormElement(Locator l, String text, boolean suppressValueLogging)
    {
        WebElement el = l.findElement(_driver);

        if (text.length() < 1000)
        {
            try {el.clear();} catch(WebDriverException e) {/*Probably a file input*/}
            el.sendKeys(text);
        }
        else
        {
            setFormElementJS(l, text.substring(0, text.length()-1));
            //Retype the last character manually to trigger events
            el.sendKeys(text.substring(text.length()-1));
        }

        if (el.getAttribute("class").contains("gwt-TextBox"))
            fireEvent(l, SeleniumEvent.blur); // Make GWT form elements behave better
    }

    /**
     * Set form element directly via JavaScript rather than WebElement.sendKeys
     * @param l Locator for form element
     * @param text text to set
     */
    public void setFormElementJS(Locator l, String text)
    {
        WebElement el = l.findElement(_driver);

        executeScript("arguments[0].value = arguments[1]", el, text);
    }

    /**
     * @deprecated Use {@link #setFormElement(Locator, File)}
     * @param element
     * @param file
     */
    @Deprecated
    public void setFormElement(String element, File file)
    {
        setFormElement(Locator.name(element), file);
    }

    public void setFormElement(Locator loc, File file)
    {
        WebElement el = loc.findElement(_driver);
        int i = 1;
        int zIndex;
        while (!el.isDisplayed() && i < 5 )
        {
            zIndex = Integer.parseInt(el.getCssValue("z-index")) * i++;
            executeScript("arguments[0].style.zIndex = arguments[1];", el, zIndex); // force element to the front
        }
        setFormElement(loc, file.getAbsolutePath());
    }

    public void setFormElement(Locator element, String text)
    {
        setFormElement(element, text, false);
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
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        waitForElement(header, WAIT_FOR_JAVASCRIPT);
        String id = EscapeUtil.filter(regionName + ":" + columnName + ":clear");
        runMenuItemHandler(id);
        waitForPageToLoad(wait);
    }

    public void setSort(String regionName, String columnName, SortDirection direction, int wait)
    {
        log("Setting sort in " + regionName + " for " + columnName + " to " + direction.toString());
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        waitForElement(header, WAIT_FOR_JAVASCRIPT);
        String id = EscapeUtil.filter(regionName + ":" + columnName + ":" + direction.toString().toLowerCase());
        runMenuItemHandler(id);
        waitForPageToLoad(wait);
    }

    public void setFilter(String regionName, String columnName, String filterType)
    {
        setUpFilter(regionName, columnName, filterType, null);
        clickButton("OK");
    }

    public void setFilter(String regionName, String columnName, String filterType, String filter)
    {
        setFilter(regionName, columnName, filterType, filter, WAIT_FOR_PAGE);
    }

    public void setFilter(String regionName, String columnName, String filterType, String filter, int waitMillis)
    {
        setUpFilter(regionName, columnName, filterType, filter);
        clickButton("OK", waitMillis);
    }

    public void setUpFilter(String regionName, String columnName, String filterType, String filter)
    {
        setUpFilter(regionName, columnName, filterType, filter, null, null);
    }

    public void setFilterAndWait(String regionName, String columnName, String filterType, String filter, int milliSeconds)
    {
        setUpFilter(regionName, columnName, filterType, filter);
        clickButton("OK", milliSeconds);
    }

    public void setUpFilter(String regionName, String columnName, String filter1Type, String filter1, String filter2Type, String filter2)
    {
        String log =    "Setting filter in " + regionName + " for " + columnName+" to " + filter1Type.toLowerCase() + (filter1!=null?" " + filter1:"");
        if(filter2Type!=null)
        {
            log+= " and " + filter2Type.toLowerCase() + (filter2!=null?" " + filter2:"");
        }
        log( log );
        String id = EscapeUtil.filter(regionName + ":" + columnName + ":filter");
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        waitForElement(header, WAIT_FOR_JAVASCRIPT);
        String columnLabel = getText(header);
        runMenuItemHandler(id);
        _extHelper.waitForExtDialog("Show Rows Where " + columnLabel + "...");
        waitForTextToDisappear("Loading...");

        if (isTextPresent("Choose Values"))
        {
            log("Switching to advanced filter UI");
            _extHelper.clickExtTab("Choose Filters");
            waitForElement(Locator.xpath("//span["+Locator.NOT_HIDDEN+" and text()='Filter Type:']"), WAIT_FOR_JAVASCRIPT);
        }
        _extHelper.selectComboBoxItem("Filter Type:", filter1Type); //Select combo box item.
        if(filter1 != null && !filter1Type.contains("Blank"))
            setFormElement(Locator.id("value_1"), filter1);
        if(filter2Type!=null && !filter2Type.contains("Blank"))
        {
            _extHelper.selectComboBoxItem("and:", filter2Type); //Select combo box item.
            if(filter2 != null) setFormElement(Locator.id("value_2"), filter2);
        }
    }

    public void setFilter(String regionName, String columnName, String filter1Type, String filter1, String filter2Type, String filter2)
    {
        setUpFilter(regionName, columnName, filter1Type, filter1, filter2Type, filter2);
        clickButton("OK");
    }

    public void setUpFacetedFilter(String regionName, String columnName, String... values)
    {
        String log = "Setting filter in " + regionName + " for " + columnName+" to one of: [";
        for(String v : values)
        {
            log += v + ", ";
        }
        log = log.substring(0, log.length() - 2) + "]";
        log(log);
        String id = EscapeUtil.filter(regionName + ":" + columnName + ":filter");
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        waitForElement(header, WAIT_FOR_JAVASCRIPT);
        String columnLabel = getText(header);
        runMenuItemHandler(id);
        _extHelper.waitForExtDialog("Show Rows Where " + columnLabel + "...");

        sleep(500);

        // Clear selections.
        Assert.assertEquals("Faceted filter tab should be selected.", "Choose Values", getText(Locator.css(".x-tab-strip-active")));
        if(!isElementPresent(Locator.xpath("//div[contains(@class, 'x-grid3-hd-checker-on')]")))
            clickLinkWithText("[All]", false);
        clickLinkWithText("[All]", false);

        if(values.length > 1)
        {
            for(String v : values)
            {
                click(Locator.xpath(_extHelper.getExtDialogXPath("Show Rows Where "+columnLabel+"...")+
                    "//div[contains(@class,'x-grid3-row') and .//span[text()='"+v+"']]//div[@class='x-grid3-row-checker']"));
            }
        }
        else if (values.length == 1)
        {
            click(Locator.xpath(_extHelper.getExtDialogXPath("Show Rows Where "+columnLabel+"...")+
                    "//div[contains(@class,'x-grid3-row')]//span[text()='"+values[0]+"']"));
        }
    }

    public void setFacetedFilter(String regionName, String columnName, String... values)
    {
        setUpFacetedFilter(regionName, columnName, values);
        clickButton("OK");
    }

    public void clearFilter(String regionName, String columnName)
    {
        clearFilter(regionName, columnName, WAIT_FOR_PAGE);
    }

    public void clearFilter(String regionName, String columnName, int waitForPageLoad)
    {
        log("Clearing filter in " + regionName + " for " + columnName);
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        waitForElement(header, WAIT_FOR_JAVASCRIPT);
        String id = EscapeUtil.filter(regionName + ":" + columnName + ":clear-filter");
        runMenuItemHandler(id);
        if(waitForPageLoad > 0)
            waitForPageToLoad(waitForPageLoad);
    }

    /**
     * @param columnName only used to find something to click on, as all the filters on all the columns will be cleared
     */
    public void clearAllFilters(String regionName, String columnName)
    {
        log("Clearing filter in " + regionName + " for " + columnName);
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        waitForElement(header, WAIT_FOR_JAVASCRIPT);
        String id = EscapeUtil.filter(regionName + ":" + columnName + ":filter");
        runMenuItemHandler(id);
        clickButton("CLEAR ALL FILTERS");
    }

    final static int MAX_TEXT_LENGTH = 2000;

    public String getPropertyXPath(String propertyHeading)
    {
        return "//td[contains(text(), '" + propertyHeading + "')]/../..";
    }

    /**
     * @deprecated Use {@link #getElementCount(org.labkey.test.Locator)}
     * @param xpath
     * @return
     */
    @Deprecated
    public int getXpathCount(Locator.XPathLocator xpath)
    {
        return xpath.findElements(_driver).size();
    }

    public int getElementCount(Locator locator)
    {
        return locator.findElements(_driver).size();
    }

    /**
     * From the assay design page, add a field with the given name, label, and type
     * @param name
     * @param label
     * @param type
     */
    public void addRunField(String name, String label, ListHelperWD.ListColumnType type)
    {
        String xpath = ("//input[starts-with(@name, 'ff_name");
        int newFieldIndex = getXpathCount(Locator.xpath(xpath + "')]"));
        clickButtonByIndex("Add Field", 1, 0);
        _listHelper.setColumnName(newFieldIndex, name);
        _listHelper.setColumnLabel(newFieldIndex, label);
        _listHelper.setColumnType(newFieldIndex, type);
    }

    /**
     * @deprecated Move usages to use ListHelperWD
     */
    @Deprecated public void addField(String areaTitle, int index, String name, String label, ListHelperWD.ListColumnType type)
    {
        String prefix = getPropertyXPath(areaTitle);
        String addField = prefix + "//span" + Locator.navButton("Add Field").getPath();
        click(Locator.xpath(addField));
        waitForElement(Locator.xpath(prefix + "//input[@name='ff_name" + index + "']"), WAIT_FOR_JAVASCRIPT);
        _listHelper.setColumnName(prefix, index, name);
        _listHelper.setColumnLabel(prefix, index, label);
        _listHelper.setColumnType(prefix, index, type);
    }

    /**
     * @deprecated Move usages to use ListHelperWD
     */
    @Deprecated public void addLookupField(String areaTitle, int index, String name, String label, ListHelperWD.LookupInfo type)
    {
        String prefix = areaTitle==null ? "" : getPropertyXPath(areaTitle);
        String addField = prefix + "//span" + Locator.navButton("Add Field").getPath();
        click(Locator.xpath(addField));
        waitForElement(Locator.xpath(prefix + "//input[@name='ff_name" + index + "']"), WAIT_FOR_JAVASCRIPT);
        _listHelper.setColumnName(prefix, index, name);
        _listHelper.setColumnLabel(prefix, index, label);
        _listHelper.setColumnType(prefix, index, type);
    }

    /**
     * @deprecated Move usages to use ListHelperWD
     */
    @Deprecated public void deleteField(String areaTitle, int index)
    {
        String prefix = getPropertyXPath(areaTitle);
        click(Locator.xpath(prefix + "//div[@id='partdelete_" + index + "']"));

        // If domain hasn't been saved yet, the 'OK' prompt will not appear.
        Locator.XPathLocator buttonLocator = getButtonLocator("OK");
        // TODO: Be smarter about this.  Might miss the OK that should be there.
        if (buttonLocator != null)
        {
            // Confirm the deletion
            clickButton("OK", 0);
            waitForElement(Locator.xpath("//td/img[@id='partstatus_" + index + "' and contains(@src, 'deleted')]"), WAIT_FOR_JAVASCRIPT);
        }
    }

    /**
     * @deprecated Use explicit Locator: {@link #setLongTextField(Locator, String)}
     * @param element
     * @param text
     */
    @Deprecated public void setFormElementAndVerify(final Locator element, final String text)
    {
        setFormElement(element, text, true);

        waitFor(new Checker()
        {
            public boolean check()
            {
                return getFormElement(element).replace("\r", "").trim().equals(text.replace("\r", "").trim()); // Ignore carriage-returns, which are present in IE but absent in firefox
            }
        }, "Form element was not set.", WAIT_FOR_JAVASCRIPT);
    }

    /**
     * Set a form element and verify
     * @param loc Selenium locator of text field to be set
     * @param text Value to be inserted into text field
     */
    public void setLongTextField(final Locator loc, final String text)
    {
        setFormElement(loc, text, true);

        waitFor(new Checker()
        {
            public boolean check()
            {
                return getFormElement(loc).replace("\r", "").trim().equals(text.replace("\r", "").trim()); // Ignore carriage-returns, which are present in IE but absent in firefox
            }
        }, "Text was not set.", WAIT_FOR_JAVASCRIPT);
    }


    public boolean isNavButtonPresent(String buttonText)
    {
        return isButtonPresent(buttonText);
    }

    public boolean isMenuButtonPresent(String buttonText)
    {
        return isButtonPresent(buttonText);
    }

    public void assertButtonPresent(String buttonText)
    {
        Assert.assertTrue("Button not present with text: " + buttonText, isButtonPresent(buttonText));
    }

    public void assertNavButtonPresent(String buttonText)
    {
        Assert.assertTrue("Nav button '" + buttonText + "' was not present", isNavButtonPresent(buttonText));
    }

    public void assertNavButtonNotPresent(String buttonText)
    {
        Assert.assertFalse("Nav button '" + buttonText + "' was present", isNavButtonPresent(buttonText));
    }

    public void assertMenuButtonPresent(String buttonText)
    {
        Assert.assertTrue("Nav button '" + buttonText + "' was not present", isMenuButtonPresent(buttonText));
    }

    public void assertMenuButtonNotPresent(String buttonText)
    {
        Assert.assertFalse("Menu button '" + buttonText + "' was present", isMenuButtonPresent(buttonText));
    }

    /**
     * Executes an Ext.menu.Item's handler.
     */
    public boolean runMenuItemHandler(String id)
    {
        log("Invoking Ext menu item handler '" + id + "'");
        return _extHelper.clickExtComponent(EscapeUtil.filter(id));
    }

    /**
     * Clicks the labkey menu item and optional submenu labels (for cascading menus)
     */
    public void clickMenuButton(String menusLabel, String ... subMenusLabels)
    {
        _extHelper.clickMenuButton(true, menusLabel, subMenusLabels);
    }

    /**
     * Clicks the ext menu item and optional submenu labels's (for cascading menus)
     * Does not wait for page load.
     */
    public void clickMenuButtonAndContinue(String menusLabel, String ... subMenusLabels)
    {
        _extHelper.clickMenuButton(false, menusLabel, subMenusLabels);
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
        String id = Locator.xq("dataregion_header_" + dataRegionName);
        clickAndWait(Locator.xpath("//table[@id=" + id + "]//div/a[@title='" + title + "']"));
    }

    public int getDataRegionRowCount(String dataRegionName)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        return Locator.xpath("//table[@id=" + id + "]/tbody/tr[contains(@class, 'labkey-row') or contains(@class, 'labkey-alternate-row')]").findElements(_driver).size();
    }

    /** Sets selection state for rows of the data region on the current page. */
    public void checkAllOnPage(String dataRegionName)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        WebElement toggle = Locator.xpath("//table[@id=" + id + "]//input[@name='.toggle']").findElement(_driver);
        checkCheckbox(toggle);
    }

    /** Clears selection state for rows of the data region on the current page. */
    public void uncheckAllOnPage(String dataRegionName)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        WebElement toggle = Locator.xpath("//table[@id=" + id + "]//input[@name='.toggle']").findElement(_driver);
        checkCheckbox(toggle);
        uncheckCheckbox(toggle);
    }

    /** Sets selection state for single rows of the data region. */
    public void checkDataRegionCheckbox(String dataRegionName, String value)
    {
        String id = Locator.xq(dataRegionName);
        checkCheckbox(Locator.xpath("//form[@id=" + id + "]//input[@name='.select' and @value='" + value + "']"));
    }

    /** Sets selection state for single rows of the data region. */
    public void checkDataRegionCheckbox(String dataRegionName, int index)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        List<WebElement> selects = Locator.xpath("//table[@id=" + id + "]//input[@name='.select']").findElements(_driver);
        checkCheckbox(selects.get(index));
    }

    /** Sets selection state for single rows of the data region. */
    public void uncheckDataRegionCheckbox(String dataRegionName, int index)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        List<WebElement> selects = Locator.xpath("//table[@id=" + id + "]//input[@name='.select']").findElements(_driver);
        uncheckCheckbox(selects.get(index));
    }

    public void clickCheckbox(String name)
    {
        click(Locator.checkboxByName(name));
    }

    public void clickRadioButtonById(String id)
    {
        click(Locator.radioButtonById(id));
    }

    public void clickRadioButtonById(String id, int millis)
    {
        clickAndWait(Locator.radioButtonById(id), millis);

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
        checkCheckbox(Locator.checkboxByName(name));
    }

    public void checkCheckboxByNameInDataRegion(String name)
    {
        checkCheckbox(Locator.xpath("//a[contains(text(), '" + name + "')]/../..//td/input"));
    }

    public void checkButtonByText(String text)
    {
        Locator l = Locator.xpath("//*[text()='" + text + "']/../input[contains(@type,'button')]");
        click(l);
    }

    public void checkRadioButton(Locator radioButtonLocator)
    {
        checkCheckbox(radioButtonLocator);
    }

    public void checkCheckbox(Locator checkBoxLocator)
    {
        log("Checking checkbox " + checkBoxLocator);
        if (!isChecked(checkBoxLocator))
            click(checkBoxLocator);
        logJavascriptAlerts();
        Assert.assertTrue("Checking checkbox failed", isChecked(checkBoxLocator));
    }

    public void checkCheckbox(WebElement el)
    {
        if (!(Boolean)executeScript("return arguments[0].checked", el))
        {
            el.click();
        }
    }

    public void uncheckCheckbox(WebElement el)
    {
        if ((Boolean)executeScript("return arguments[0].checked", el))
        {
            el.click();
        }
    }

    public void checkRadioButton(String name, int index)
    {
        checkCheckbox(Locator.radioButtonByName(name).index(index));
    }

    public void assertRadioButtonSelected(String name, String value)
    {
        assertRadioButtonSelected(Locator.radioButtonByNameAndValue(name, value));
    }

    public void assertRadioButtonSelected(String name, int index)
    {
        assertRadioButtonSelected(Locator.radioButtonByName(name).index(index));
    }

    public void assertRadioButtonSelected(Locator radioButtonLocator)
    {
        Assert.assertTrue("Radio Button is not selected at " + radioButtonLocator.toString(), isChecked(radioButtonLocator));
    }

    public void checkCheckbox(String name, int index)
    {
        checkCheckbox(Locator.checkboxByName(name).index(index));
    }

    public void uncheckCheckbox(String name)
    {
        uncheckCheckbox(Locator.checkboxByName(name));
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
        if (isChecked(checkBoxLocator))
            click(checkBoxLocator);
        logJavascriptAlerts();
    }

    public void assertChecked(Locator checkBoxLocator)
    {
        Assert.assertTrue("Checkbox not checked at " + checkBoxLocator.toString(), isChecked(checkBoxLocator));
    }

    public void assertNotChecked(Locator checkBoxLocator)
    {
        Assert.assertFalse("Checkbox checked at " + checkBoxLocator.toString(), isChecked(checkBoxLocator));
    }

    public boolean isChecked(Locator checkBoxLocator)
    {
        return checkBoxLocator.findElement(_driver).isSelected();
    }

    /**
     * @deprecated Use {@link #selectOptionByValue(Locator, String)}
     * @param selectName
     * @param value
     */
    @Deprecated
    public void selectOptionByValue(String selectName, String value)
    {
        selectOptionByValue(Locator.name(selectName), value);
    }

    public void selectOptionByValue(Locator locator, String value)
    {
        Select select = new Select(locator.findElement(_driver));
        select.selectByValue(value);
    }

    /**
     * @deprecated Use {@link #selectOptionByText(Locator, String)}
     * @param selectName
     * @param text
     */
    @Deprecated
    public void selectOptionByText(String selectName, String text)
    {
        selectOptionByText(Locator.name(selectName), text);
    }

    public void selectOptionByText(Locator locator, String text)
    {
        Select select = new Select(locator.findElement(_driver));
        select.selectByVisibleText(text);
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
        waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.permissionButton(groupName,permissionSetting), WAIT_FOR_JAVASCRIPT);
    }

    public void assertPermissionSetting(String groupName, String permissionSetting)
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
        assertElementPresent(Locator.permissionButton(groupName,permissionSetting));
    }

    public void checkInheritedPermissions()
    {
        _ext4Helper.checkCheckbox("Inherit permissions from parent");
    }

    public void uncheckInheritedPermissions()
    {
        _ext4Helper.uncheckCheckbox("Inherit permissions from parent");
    }

    public void savePermissions()
    {
        clickButton("Save", 0);
        waitForElement(Locator.permissionRendered(),defaultWaitForPage);
    }

    @LogMethod
    public void setPermissions(String groupName, String permissionString)
    {
        _setPermissions(groupName, permissionString, "pGroup");
    }

    @LogMethod
    public void setSiteGroupPermissions(String groupName, String permissionString)
    {
        _setPermissions(groupName, permissionString, "pSite");
    }

    @LogMethod
    public void setUserPermissions(String userName, String permissionString)
    {
        log(new Date().toString());
        _setPermissions(userName, permissionString, "pUser");

        log(new Date().toString());
    }

    public void _setPermissions(String userOrGroupName, String permissionString, String className)
    {
        String role = toRole(permissionString);
        if ("org.labkey.api.security.roles.NoPermissionsRole".equals(role))
        {
            Assert.fail("call removePermission()");
        }
        else
        {
            log("Setting permissions for group " + userOrGroupName + " to " + role);

            if (!isElementPresent(Locator.permissionRendered()))
                enterPermissionsUI();
            _ext4Helper.clickTabContainingText("Permissions");

            waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
            String group = userOrGroupName;
            if (className.equals("pSite"))
                group = "Site: " + group;
            click(Locator.xpath("//div[contains(@class, 'rolepanel')][.//h3[text()='" + permissionString + "']]//div[contains(@class, 'x4-form-trigger')]"));
            click(Locator.xpath("//div[contains(@class, 'x4-boundlist')]//li[contains(@class, '" + className + "') and text()='" + group + "']"));
            waitForElement(Locator.permissionButton(userOrGroupName, permissionString));
            savePermissions();
            assertPermissionSetting(userOrGroupName, permissionString);
            _ext4Helper.waitForMaskToDisappear();
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
        Locator close = Locator.closePermissionButton(groupName,permissionString);
        if (isElementPresent(close))
        {
            click(close);
            savePermissions();
            assertNoPermission(groupName, permissionString);
        }
    }

    protected void addUserToSiteGroup(String userName, String groupName)
    {
        goToHome();
        goToSiteGroups();
        Locator.XPathLocator groupLoc = Locator.tagWithText("div", groupName);
        waitForElement(groupLoc, defaultWaitForPage);
        click(groupLoc);
        clickLinkContainingText("manage group");
        addUserToGroupFromGroupScreen(userName);
    }

    protected void addUserToGroupFromGroupScreen(String userName)
    {
        setFormElement(Locator.name("names"), userName );
        uncheckCheckbox("sendEmail");
        clickButton("Update Group Membership");

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
        addUserToGroupFromGroupScreen(userName);
    } //addUserToProjGroup()

    public void enterPermissionsUI()
    {
        //if the following assert triggers, you were already in the permissions UI when this was called
        if (!isElementPresent(Locator.permissionRendered()))
        {
            clickAdminMenuItem("Folder", "Permissions");
            waitForElement(Locator.permissionRendered());
        }
    }

    public void exitPermissionsUI()
    {
        _ext4Helper.clickTabContainingText("Permissions");
        clickButton("Save and Finish");
    }

    public void impersonateGroup(String group, boolean isSiteGroup)
    {
        goToHome();
        clickUserMenuItem("Impersonate", "Group", (isSiteGroup ? "Site: " : "") + group);
    }

    public void impersonateRole(String role)
    {
        clickUserMenuItem("Impersonate", "Role", role);
    }

    public void stopImpersonatingRole()
    {
        clickUserMenuItem("Stop Impersonating");
        assertSignOutAndMyAccountPresent();
        goToHome();
    }

    public void stopImpersonatingGroup()
    {
        clickUserMenuItem("Stop Impersonating");
        assertSignOutAndMyAccountPresent();
        goToHome();
    }

    public void impersonate(String fakeUser)
    {
        if (isElementPresent(Locator.id("userMenuPopupLink")))
        {
            click(Locator.id("userMenuPopupLink"));
        }
        assertTextNotPresent("Stop Impersonating");
        goToAdminConsole();
        selectOptionByText(Locator.id("email"), fakeUser);
        clickButton("Impersonate");
        _impersonationStack.push(fakeUser);
    }


    public void stopImpersonating()
    {
        String fakeUser = _impersonationStack.pop();
        Assert.assertEquals(displayNameFromEmail(fakeUser), getDisplayName());
        clickUserMenuItem("Stop Impersonating");
        assertSignOutAndMyAccountPresent();
        goToHome();
        Assert.assertFalse(displayNameFromEmail(fakeUser).equals(getDisplayName()));
    }

    public void impersonateAtProjectLevel(String fakeUser)
    {
        if (isElementPresent(Locator.id("userMenuPopupLink")))
        {
            click(Locator.id("userMenuPopupLink"));
        }
        assertTextNotPresent("Stop Impersonating");
        ensureAdminMode();
        enterPermissionsUI();
        _ext4Helper.clickTabContainingText("Impersonate");
        selectOptionByText(Locator.id("email"), fakeUser);
        clickLinkWithText("Impersonate");
        _impersonationStack.push(fakeUser);
    }


    // assumes there are not collisions in the database causing unique numbers to be appended
    public static String displayNameFromEmail(String email)
    {
        String display = email.contains("@") ? email.substring(0,email.indexOf('@')) : email;
        display = display.replace('_', ' ');
        display = display.replace('.', ' ');
        return display.trim();
    }


    /** create a user with the specified permissions for the specified project
     *
     * @param userName
     * @param projectName
     * @param permissions
     */
    public void createUserWithPermissions(String userName, String projectName, String permissions)
    {
        createUser(userName, null);
        if(projectName==null)
            goToProjectHome();
        else
            clickLinkWithText(projectName);
        setUserPermissions(userName, permissions);

    }

    public void createUser(String userName, @Nullable String cloneUserName)
    {
        createUser(userName, cloneUserName, true);
    }

    public void createUser(String userName, String cloneUserName, boolean verifySuccess)
    {
        if(cloneUserName == null)
        {
            _userHelper.createUser(userName, verifySuccess);
        }
        else
        {
            Assert.fail("cloneUserName support has been removed"); //not in use, so was not implemented in new user
            //helpers
        }

    }

    public void createUserAndNotify(String userName, String cloneUserName)
    {
        createUserAndNotify(userName, cloneUserName, true);
    }

    public void createUserAndNotify(String userName, String cloneUserName, boolean verifySuccess)
    {
        ensureAdminMode();
        goToSiteUsers();
        clickButton("Add Users");

        setFormElement(Locator.name("newUsers"), userName);
        if (cloneUserName != null)
        {
            checkCheckbox("cloneUserCheck");
            setFormElement(Locator.name("cloneUser"), cloneUserName);
        }
        clickButton("Add Users");

        if (verifySuccess)
            Assert.assertTrue("Failed to add user " + userName, isTextPresent(userName + " added as a new user to the system"));
    }

    public void createSiteDeveloper(String userEmail)
    {
        ensureAdminMode();
        goToSiteDevelopers();

        if (!isElementPresent(Locator.xpath("//input[@value='" + userEmail + "']")))
        {
            setFormElement(Locator.name("names"), userEmail);
            uncheckCheckbox("sendEmail");
            clickButton("Update Group Membership");
        }
    }

    public void deleteGroup(String groupName)
    {
        deleteGroup(groupName, false);
    }

    @LogMethod
    public void deleteGroup(String groupName, boolean failIfNotFound)
    {
        log("Attempting to delete group: " + groupName);
        if (selectGroup(groupName, failIfNotFound))
        {
            deleteAllUsersFromGroup();
            click(Locator.xpath("//td/a/span[text()='Delete Empty Group']"));
            waitForElementToDisappear(Locator.xpath("//div[@class='pGroup' and text()=" + Locator.xq(groupName) + "]"), WAIT_FOR_JAVASCRIPT);
        }
    }

    private void deleteAllUsersFromGroup()
    {
        Locator.XPathLocator l = Locator.xpath("//td/a/span[text()='remove']");

        while(isElementPresent(l))
        {
            int i = getElementCount(l);
            click(l);
            waitForElementToDisappear(l.index(i), WAIT_FOR_JAVASCRIPT);
        }
    }


    public void removeUserFromGroup(String groupName, String userName)
    {
         if(!isTextPresent("Group " + groupName))
             selectGroup(groupName);

        Locator l = Locator.xpath("//td[text()='" + userName +  "']/..//td/a/span[text()='remove']");
        click(l);
    }

    public void addUserToGroup(String groupName, String userName)
    {
         if(!isTextPresent("Group " + groupName))
             selectGroup(groupName);
        String dialogTitle = groupName + " Information";

        _ext4Helper.selectComboBoxItem(Locator.xpath(_extHelper.getExtDialogXPath(dialogTitle) + "//table[contains(@id, 'labkey-principalcombo')]"), userName);
        Locator.css(".userinfo td").withText(userName).waitForElmement(_driver, WAIT_FOR_JAVASCRIPT);
        _extHelper.clickExtButton(dialogTitle, "Done", 0);
        _extHelper.waitForExtDialogToDisappear(dialogTitle);

        clickButton("Done");
    }

    public boolean selectGroup(String groupName)
    {
        return selectGroup(groupName, false);
    }

    public boolean selectGroup(String groupName, boolean failIfNotFound)
    {
        if(!isElementPresent(Locator.css(".x4-tab-active").withText("Site Groups")))
            goToSiteGroups();

        waitForElement(Locator.css(".groupPicker .x4-grid-body"), WAIT_FOR_JAVASCRIPT);
        if (isElementPresent(Locator.xpath("//div[text()='" + groupName + "']")))
        {
            click(Locator.xpath("//div[text()='" + groupName + "']"));
            _extHelper.waitForExtDialog(groupName + " Information");
            return true;
        }
        else if (failIfNotFound)
            Assert.fail("Group not found:" + groupName);

        return false;
    }

    @LogMethod
    public void deleteUsers(boolean failIfNotFound, String... userEmails)
    {
        int checked = 0;
        List<String> displayNames = new ArrayList<String>();
        ensureAdminMode();
        goToSiteUsers();

        if(isLinkPresentWithText("INCLUDE INACTIVE USERS"))
            clickLinkWithText("INCLUDE INACTIVE USERS");

        DataRegionTable usersTable = new DataRegionTable("Users", this, true, true);

        for(String userEmail : userEmails)
        {
            int row = usersTable.getRow("Email", userEmail);

            boolean isPresent = row != -1;

            // If we didn't find the user and we have more than one page, then show all pages and try again
            if (!isPresent && isLinkPresentContainingText("Next") && isLinkPresentContainingText("Last"))
            {
                clickButton("Page Size", 0);
                clickLinkWithText("Show All");
                row = usersTable.getRow("Email", userEmail);
                isPresent = row != -1;
            }

            if (failIfNotFound)
                Assert.assertTrue(userEmail + " was not present", isPresent);

            if (isPresent)
            {
                usersTable.checkCheckbox(row);
                checked++;
                displayNames.add(usersTable.getDataAsText(row, "Display Name"));
            }
        }

        if(checked > 0)
        {
            clickButton("Delete");
            assertTextPresent(displayNames);
            assertTextPresent("permanently delete");
            clickButton("Permanently Delete");
            assertTextNotPresent(userEmails);
        }
    }

    public void assertUserExists(String email)
    {
        log("asserting that user " + email + " exists...");
        ensureAdminMode();
        goToSiteUsers();
        assertTextPresent(email);
        log("user " + email + " exists.");
    }

    public boolean doesGroupExist(String groupName, String projectName)
    {
        ensureAdminMode();
        clickLinkWithText(projectName);
        enterPermissionsUI();
        _ext4Helper.clickTabContainingText("Project Groups");
        waitForText("Member Groups");
        List<Ext4CmpRefWD> refs = _ext4Helper.componentQuery("grid", Ext4CmpRefWD.class);
        Ext4CmpRefWD ref = refs.get(0);
        Long idx = (Long)ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        exitPermissionsUI();
        return (idx >= 0);
    }

    public void assertGroupExists(String groupName, String projectName)
    {
        log("asserting that group " + groupName + " exists in project " + projectName + "...");
        if (!doesGroupExist(groupName, projectName))
            Assert.fail("group " + groupName + " does not exist in project " + projectName);
    }

    public void assertGroupDoesNotExist(String groupName, String projectName)
    {
        log("asserting that group " + groupName + " exists in project " + projectName + "...");
        if (doesGroupExist(groupName, projectName))
            Assert.fail("group " + groupName + " exists in project " + projectName);
    }

    public boolean isUserInGroup(String email, String groupName, String projectName)
    {
        ensureAdminMode();
        clickLinkWithText(projectName);
        enterPermissionsUI();
        _ext4Helper.clickTabContainingText("Project Groups");
        waitForElement(Locator.css(".groupPicker"), WAIT_FOR_JAVASCRIPT);
        waitAndClick(Locator.xpath("//div[text()='" + groupName + "']"));
        _extHelper.waitForExtDialog(groupName + " Information");
        boolean ret = isElementPresent(Locator.xpath("//table[contains(@class, 'userinfo')]//td[text()='" + email +  "']"));
        clickButton("Done");
        return ret;
    }

    public void assertUserInGroup(String email, String groupName, String projectName)
    {
        log("asserting that user " + email + " is in group " + projectName + "/" + groupName + "...");
        if (!isUserInGroup(email, groupName, projectName))
            Assert.fail("user " + email + " was not in group " + projectName + "/" + groupName);
    }

    public void assertUserNotInGroup(String email, String groupName, String projectName)
    {
        log("asserting that user " + email + " is not in group " + projectName + "/" + groupName + "...");
        if (isUserInGroup(email, groupName, projectName))
            Assert.fail("user " + email + " was found in group " + projectName + "/" + groupName);
    }

    public void saveWikiPage()
    {
        String title = Locator.id("wiki-input-title").findElement(_driver).getText();
        if (title.equals("")) title = Locator.id("wiki-input-name").findElement(_driver).getText();
        clickButton("Save & Close", 0);
        waitForElement(Locator.linkWithText(title));
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
            Assert.fail("Could not find a link on the current page to create a new wiki page." +
                    " Ensure that you navigate to the wiki controller home page or an existing wiki page" +
                    " before calling this method.");

        convertWikiFormat(format);
    }



    //TODO
    protected void importSpecimen(String file)
    {
        _extHelper.selectFileBrowserItem(file);
        selectImportDataActionNoWaitForGrid("Import Specimen Data");
        clickButton("Start Import");
    }

    //must already be on wiki page
    public void setWikiValuesAndSave(String name, String title, String body)
    {

        setFormElement("name", name);
        setFormElement("title", title);
        setWikiBody(body);
        clickButtonContainingText("Save & Close");
    }

    /**
     * Converts the current wiki page being edited to the specified format.
     * If the page is already in that format, it will no-op.
     * @param format The desired format ("RADEOX", "HTML", or "TEXT_WITH_LINKS")
     */
    public void convertWikiFormat(String format)
    {
        String curFormat = (String)executeScript("return window._wikiProps.rendererType");
        if(curFormat.equalsIgnoreCase(format))
            return;


        clickButton("Convert To...", 0);
        sleep(500);
        selectOptionByValue("wiki-input-window-change-format-to", format);
        clickButton("Convert", 0);
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
        setFormElement(Locator.name("body"), body);
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Returns the data region for the the cohort table to enable setting
     * or verifying the enrolled status of the cohort
     */
    public DataRegionTable getCohortDataRegionTable(String projectName)
    {
        clickLinkWithText(projectName);
        clickTab("Manage");
        clickLinkWithText("Manage Cohorts");
        return new DataRegionTable("Cohort", this, false);
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Verifies the enrolled status of a cohort
     */
    public void verifyCohortStatus(DataRegionTable table, String cohort, boolean  enrolled)
    {
        int row = getCohortRow(table, cohort);
        String s = table.getDataAsText(row, "Enrolled");
        Assert.assertTrue("Enrolled column should be " + String.valueOf(enrolled), (0 == s.compareToIgnoreCase(String.valueOf(enrolled))));
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Retrieves the row for the cohort matching the label passed in
     */
    public int getCohortRow(DataRegionTable cohortTable, String cohort)
    {
        int row;
        for (row = 0; row < cohortTable.getDataRowCount(); row++)
        {
            String s = cohortTable.getDataAsText(row, "Label");
            if (0 == s.compareToIgnoreCase(cohort))
            {
                break;
            }
        }
        return row;
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Changes the enrolled status of the passed in cohort name
     */
    public void changeCohortStatus(DataRegionTable cohortTable, String cohort, boolean enroll)
    {
        int row = getCohortRow(cohortTable, cohort);
        // if the row does not exist then most likely the cohort passed in is incorrect
        cohortTable.clickLink(row, 0);

        if (!enroll)
        {
            uncheckCheckbox("quf_enrolled");
        }
        else
        {
            checkCheckbox("quf_enrolled");
        }

        clickButton("Submit");
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
            Assert.fail("Could not find the Wiki '" + wikiName + "'. Please create the Wiki before attempting to set the source.");
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
        enableModule(moduleName, true);
    }

    public void enableModule(String moduleName, boolean isProject)
    {
        enableModules(Collections.singletonList(moduleName), isProject);
    }

    public void enableModules(List<String> moduleNames, boolean isProject)
    {
        goToFolderManagement();
        clickLinkWithText("Folder Type");
        for (String moduleName : moduleNames)
        {
            checkCheckbox(Locator.checkboxByTitle(moduleName));
        }
        clickButton("Update Folder");
    }

    public void disableModules(List<String> moduleNames)
    {
        goToFolderManagement();
        clickLinkWithText("Folder Type");
        for (String moduleName : moduleNames)
        {
            uncheckCheckbox(Locator.checkboxByTitle(moduleName));
        }
        clickButton("Update Folder");
    }

    public void goToProjectHome()
    {
        clickFolder(getProjectName());
    }

    public void goToHome()
    {
        beginAt("/project/home/begin.view");
    }

    /**
     * go to the project settings page of a project, or of the current project if argument=null
     * @param project project name, or null if current project
     */
    public void goToProjectSettings(String project)
    {
        if(!isLinkPresentWithText(project))
            goToHome();
        clickLinkWithText(project);
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

    public void goToPipelineItem(String item)
    {
        int time = 0;
        while (getText(Locator.xpath("//td[contains(text(),'" + item + "')]/../td[2]/a")).compareTo("WAITING") == 0
                && time < defaultWaitForPage)
        {
            sleep(100);
            time += 100;
            refresh();
        }
        clickAndWait(Locator.xpath("//td[contains(text(),'" + item + "')]/../td[2]/a"));
        waitForElement(Locator.xpath("//input[@value='Data']"), WAIT_FOR_JAVASCRIPT);
        clickButton("Data");
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

    protected void startImportStudyFromZip(String studyFile)
    {
        clickButton("Import Study");
        setFormElement(Locator.name("folderZip"), studyFile);
        clickButton("Import Study From Local Zip Archive");
        assertTextNotPresent("You must select a .study.zip file to import.");
    }

    protected void importStudyFromZip(String studyFile)
    {
        startImportStudyFromZip(studyFile);
        waitForPipelineJobsToComplete(1, "Study import", false);
    }

    protected void importFolderFromZip(String folderFile)
    {
        goToFolderManagement();
        clickLinkWithText("Import");
        sleep(2000);
        setFormElement(Locator.name("folderZip"), new File(folderFile));
        clickButtonContainingText("Import Folder From Local Zip Archive");
        waitForText("Data Pipeline");
        waitForPipelineJobsToComplete(1, "Folder import", false);
    }

    protected void importFolderFromPipeline(String folderFile)
    {
        goToFolderManagement();
        clickLinkWithText("Import");
        clickButtonContainingText("Import Folder Using Pipeline");
        _extHelper.selectFileBrowserItem(folderFile);
        selectImportDataAction("Import Folder");
        waitForPipelineJobsToComplete(1, "foo", false);
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
            Assert.fail(e.getMessage());
            return null;
        }
        finally
        {
            if (reader != null) try { reader.close(); } catch (IOException e) {}
            if (fis != null) try { fis.close(); } catch (IOException e) {}
        }
    }

    @LogMethod
    public void signOut()
    {
        log("Signing out");
        beginAt("/login/logout.view");
        waitForElement(Locator.xpath("//a[string()='Sign In']")); // Will recognize link [BeginAction] or button [LoginAction]
    }

    /*
     * This assumes that you have added the "search" webpart to your project
     */
    public void searchFor(String projectName, String searchFor, int expectedResults, String titleName)
    {
        log("Searching Project : " + projectName + " for \"" + searchFor + "\".  Expecting to find : " + expectedResults + " results");
        clickLinkWithText(projectName);
        assertElementPresent(Locator.name("q"));
        setFormElement(Locator.id("query"), searchFor);
        clickButton("Search");
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
        Assert.assertEquals("Expected attribute '" + locator + "@" + attributeName + "' value to be '" + value + "', but was '" + actual + "' instead.", value, actual);
    }

    public void assertAttributeContains(Locator locator, String attributeName, String value)
    {
        String actual = getAttribute(locator, attributeName);
        Assert.assertTrue("Expected attribute '" + locator + "@" + attributeName + "' value to contain '" + value + "', but was '" + actual + "' instead.", actual != null && actual.contains(value));
    }

    public void assertAttributeNotContains(Locator locator, String attributeName, String value)
    {
        String actual = getAttribute(locator, attributeName);
        Assert.assertTrue("Expected attribute '" + locator + "@" + attributeName + "' value to not contain '" + value + "', but was '" + actual + "' instead.", actual != null && !actual.contains(value));
    }

    public void assertSetsEqual(String firstSet, String secondSet, String delimiterRegEx)
    {
        String[] firstArray = firstSet.split(delimiterRegEx);
        String[] secondArray = secondSet.split(delimiterRegEx);
        Assert.assertTrue("Sets are not equal.  First set:\n" + firstSet + "\nSecond set:\n" + secondSet, firstArray.length == secondArray.length);
        Set<String> firstHash= new HashSet<String>();
        Collections.addAll(firstHash, firstArray);
        Set<String> secondHash= new HashSet<String>();
        Collections.addAll(secondHash, secondArray);
        Assert.assertTrue("Sets are not equal.  First set:\n" + firstSet + "\nSecond set:\n" + secondSet, firstHash.equals(secondHash));
    }


    public String getAttribute(Locator locator, String attributeName)
    {
        return locator.findElement(_driver).getAttribute(attributeName);
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
        return getText(Locator.id("userMenuPopupText"));
    }

	public String getHtmlSource()
	{
		return _driver.getPageSource();
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
        String[] schemaParts = schemaName.split("\\.");
        if (isExtTreeNodeSelected(schemaParts[schemaParts.length - 1]))
            return;

        String schemaWithParents = "";
        String separator = "";
        for (String schemaPart : schemaParts)
        {
            schemaWithParents += separator + schemaPart;
            separator = ".";

            log("Selecting schema " + schemaWithParents + " in the schema browser...");
            Locator loc = Locator.schemaTreeNode(schemaPart);

            //first load of schemas might a few seconds
            waitForElement(loc, 30000);
            if (isExtTreeNodeExpanded(schemaPart))
                click(loc);
            else
            {
                doubleClick(loc);
                sleep(1000);
                click(loc);
            }
            waitForElement(Locator.xpath("//div[contains(./@class,'x-tree-selected')]/a/span[text()='" + schemaPart + "']"), 1000);
            waitForText(schemaWithParents + " Schema");
        }
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
        viewQueryData(schemaName, queryName, null);
    }

    public void viewQueryData(String schemaName, String queryName, String moduleName)
    {
        selectQuery(schemaName, queryName);
        Locator loc = Locator.xpath("//div[contains(@class, 'lk-qd-name')]/a[text()='" + schemaName + "." + queryName + "']");
        waitForElement(loc, WAIT_FOR_JAVASCRIPT);
        String href = getAttribute(loc, "href");
        if (moduleName != null) // 12474
            assertTextPresent("Defined in " + moduleName + " module");
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
        String url = (String)executeScript("return window._browser.getCreateQueryUrl('" + schemaName + "')");
        if (null == url || url.length() == 0)
            Assert.fail("Could not get the URL for creating a new query in schema " + schemaName);
        beginAt(url);
    }


    protected void createQuery(String container, String name, String schemaName, String sql, String xml, boolean inheritable)
    {
        String queryURL = "query/" + container + "/begin.view?schemaName=" + schemaName;
        beginAt(queryURL);
        createNewQuery(schemaName);
        setFormElement(Locator.name("ff_newQueryName"), name);
        clickButton("Create and Edit Source", 0);
        waitForElement(Locator.id("labkey-nav-trail-current-page").withText("Edit " + name));
//        toggleSQLQueryEditor();
        setQueryEditorValue("queryText", sql);
//        setFormElement("queryText", sql);
        if (xml != null)
        {
            _extHelper.clickExtTab("XML Metadata");
            setQueryEditorValue("metadataText", xml);
//        toggleMetadataQueryEditor();
//        setFormElement("metadataText", xml);
        }
        clickButton("Save", 0);
        waitForElement(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);
        if (inheritable)
        {
            beginAt(queryURL);
            editQueryProperties("flow", name);
            selectOptionByValue(Locator.name("inheritable"), "true");
            submit();
        }
    }

    public void validateQueries(boolean validateSubfolders)
    {
        _extHelper.clickExtButton("Validate Queries", 0);
        Locator locButton = Locator.xpath("//button[text()='Start Validation']");
        Locator locFinishMsg = Locator.xpath("//div[contains(@class, 'lk-vq-status-all-ok') or contains(@class, 'lk-vq-status-error')]");
        waitForElement(locButton, WAIT_FOR_JAVASCRIPT);
        if (validateSubfolders)
            checkCheckbox(Locator.id("lk-vq-subfolders"));
        checkCheckbox(Locator.id("lk-vq-systemqueries"));
        click(locButton);
        waitForElement(locFinishMsg, 120000);
        //test for success
        if (!isElementPresent(Locator.xpath("//div[contains(@class, 'lk-vq-status-all-ok')]")))
        {
            Assert.fail("Some queries did not pass validation. See error log for more details.");
        }
    }

    public void pressTab(Locator l)
    {
        WebElement el = l.findElement(_driver);
        el.sendKeys(Keys.TAB);
    }

    public void pressEnter(Locator l)
    {
        WebElement el = l.findElement(_driver);
        el.sendKeys(Keys.ENTER);
    }

    public void pressDownArrow(Locator l)
    {
        WebElement el = l.findElement(_driver);
        el.sendKeys(Keys.DOWN);
    }

    public class DefaultSeleniumWrapper extends WebDriverBackedSelenium
    {
        DefaultSeleniumWrapper()
        {
            super(getBrowser().startsWith("*ie") ? new InternetExplorerDriver() : new FirefoxDriver(), WebTestHelper.getBaseURL());
        }

        public DefaultSeleniumWrapper(com.google.common.base.Supplier<org.openqa.selenium.WebDriver> maker, java.lang.String baseUrl)
        {
            super(maker, baseUrl);
        }

        public DefaultSeleniumWrapper(WebDriver baseDriver, java.lang.String baseUrl)
        {
            super(baseDriver, baseUrl);
        }

        private void log(String s)
        {
            BaseWebDriverTest.this.log("selenium - " + s);
        }

        @Override
        public void fireEvent(String locator, String eventName)
        {
            log("Firing event " + eventName + " on element: " + locator);
            super.fireEvent(locator, eventName);
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

        public void mouseUp(Locator l)
        {
            mouseUp(l.toString());
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
            open(url, BaseWebDriverTest.this.defaultWaitForPage);
        }

        public void open(String url, int millis)
        {
            setTimeout("" + millis);
            _testTimeout = true;
            try
            {
                super.open(url);
            }
            catch (SeleniumException e)
            {
                // fall through if we get a 'livemark' exception, which occurs when running offline
                if (e.getMessage() == null || !e.getMessage().contains("Livemark Service"))
                    throw e;
            }
            // commandProcessor.doCommand("open", new String[] {url,"true"}); // Workaround for XHR errors. http://code.google.com/p/selenium/issues/detail?id=408
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
            Alert alert = _driver.switchTo().alert();
            String confirmation = alert.getText();
            alert.accept();
            return confirmation;
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

        @LogMethod
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
            waitAndClickButton("Process and Import Data");

            // TempDir is somewhere underneath the pipeline root.  Determine each subdirectory we need to navigate to reach it.
            File testDir = _tempDir;
            List<String> dirNames = new ArrayList<String>();

            while (!_pipelineRoot.equals(testDir))
            {
                dirNames.add(0, testDir.getName());
                testDir = testDir.getParentFile();
            }

            //Build folder path.
            String path = "/";
            for (String dir : dirNames)
                path += dir + "/";

            _extHelper.selectFileBrowserItem(path);

            for (File copiedArchive : _copiedArchives)
                _extHelper.clickFileBrowserFileCheckbox(copiedArchive.getName());
            selectImportDataAction("Import Specimen Data");
            clickButton("Start Import");
        }

        @LogMethod
        public void waitForComplete()
        {
            log("Waiting for completion of specimen archives");

            clickLinkWithText(_studyFolderName);
            clickLinkWithText("Manage Files");

            waitForPipelineJobsToComplete(_completeJobsExpected, "specimen import", _expectError);

            for (File copiedArchive : _copiedArchives)
                if (!copiedArchive.delete())
                    Assert.fail("Couldn't delete copied specimen archive: " + copiedArchive.getAbsolutePath());
        }
    }


    // Wait until the pipeline UI shows the requested number of complete jobs.  Fail if any job status becomes "ERROR".
    @LogMethod
    public void waitForPipelineJobsToComplete(int completeJobsExpected, String description, boolean expectError)
    {
        log("Waiting for " + completeJobsExpected + " pipeline jobs to complete");
        List<String> statusValues = getPipelineStatusValues();

        // Short circuit in case we already have too many COMPLETE jobs
        Assert.assertTrue("Number of COMPLETE jobs already exceeds desired count", getCompleteCount(statusValues) <= completeJobsExpected);

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
        Assert.assertEquals("Did not find correct number of completed pipeline jobs.", completeJobsExpected, getCompleteCount(statusValues));
    }

    // wait until pipeline UI shows that all jobs have finished (either COMPLETE or ERROR status)
    @LogMethod
    protected void waitForPipelineJobsToFinish(int jobsExpected)
    {
        log("Waiting for " + jobsExpected + " pipeline jobs to finish");
        List<String> statusValues = getPipelineStatusValues();
        startTimer();
        while (getFinishedbCount(statusValues) < jobsExpected && elapsedSeconds() < MAX_WAIT_SECONDS)
        {
            sleep(1000);
            refresh();
            statusValues = getPipelineStatusValues();
        }
        Assert.assertEquals("Did not find correct number of finished pipeline jobs.", jobsExpected, getFinishedbCount(statusValues));
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
        Assert.fail("Running pipeline jobs were found.  Timeout:" + wait);
    }

    protected void toggleMetadataQueryEditor()
    {
        toggleEditAreaOff("metadataText");
    }

    public void toggleScriptReportEditor()
    {
        toggleEditAreaOff("script");
    }

    public void toggleEditAreaOff(final String underlyingTextAreaId)
    {
        Locator toggleCheckBoxId = Locator.id("edit_area_toggle_checkbox_" + underlyingTextAreaId);
        waitForElement(toggleCheckBoxId, WAIT_FOR_JAVASCRIPT);
        uncheckCheckbox(toggleCheckBoxId);
        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                String style = getAttribute(Locator.id(underlyingTextAreaId), "style");
                return style.contains("display: inline");
            }
        }, "Expected to toggle edit_area off and display textarea " + underlyingTextAreaId, WAIT_FOR_JAVASCRIPT);
    }

    public void setQueryEditorValue(String id, String value)
    {
        Locator toggleCheckBoxId = Locator.id("edit_area_toggle_checkbox_" + id);
        waitForElement(toggleCheckBoxId, WAIT_FOR_JAVASCRIPT);
        _extHelper.setQueryEditorValue(id, value);
    }

    /**
     * For invoking pipeline actions from the file web part. Displays the import data
     * dialog and selects and submits the specified action.
     *
     * @param actionName
     */
    @LogMethod
    public void selectImportDataAction(String actionName)
    {
        sleep(100);
        _extHelper.waitForFileGridReady();
        _extHelper.waitForImportDataEnabled();
        selectImportDataActionNoWaitForGrid(actionName);
    }

    //TODO
    public void importSpecimenData(String file, int pipelineJobs)
    {
        selectPipelineFileAndImportAction(file, "Import Specimen Data");
        clickButton("Start Import");
        waitForPipelineJobsToFinish(pipelineJobs);
    }

    public void selectPipelineFileAndImportAction(String file, String actionName)
    {
        _extHelper.selectFileBrowserItem(file);
        selectImportDataAction(actionName);
    }

    public void selectImportDataActionNoWaitForGrid(String actionName)
    {
        clickButton("Import Data", 0);

        waitAndClick(Locator.xpath("//input[@type='radio' and @name='importAction' and not(@disabled)]/../label[text()=" + Locator.xq(actionName) + "]"));
        String id = _extHelper.getExtElementId("btn_submit");
        clickAndWait(Locator.id(id));
    }

    public void clickManageSubjectCategory(String subjectNoun)
    {
        clickLinkContainingText("Manage " + subjectNoun + " Groups");
    }

    public void ensureSignedOut()
    {
        if(isElementPresent(Locator.id("userMenuPopupLink")))
            signOut();
    }

    public Collection<String> getNavTrailEntries()
    {
        return getAsUnderXpath("//span[@id='navTrailAncestors']/a");
//        String navTrailXpath =   "//span[@id='navTrailAncestors']/a";
//        int count = getXpathCount(Locator.xpath(navTrailXpath));
//        ArrayList<String> al = new ArrayList<String>(count);
//        for(int i=1; i<=count; i++)
//        {
//            al.add(getAttribute(Locator.xpath(navTrailXpath + "[" + i + "]"), "href"));
//        }
//        return al;
    }

    protected Collection<String> getAsUnderXpath(String xpath)
    {
//        String navTrailXpath =   "//span[@id='navTrailAncestors']/a";
        int count = getXpathCount(Locator.xpath(xpath));
        ArrayList<String> al = new ArrayList<String>(count);
        for(int i=1; i<=count; i++)
        {
            al.add(getAttribute(Locator.xpath(xpath + "[" + i + "]"), "href"));
        }
        return al;
    }


    public  String getFolderUrl()
    {
        Locator l = Locator.xpath("//div[@class='labkey-folder-title']/a");
        return getAttribute(l, "href");
    }

    public Collection<String> getTabEntries() throws Exception
    {
        Collection<String> tabs = getTabUrls(false);
        Collection<String> activeTabs = getTabUrls(true);
        if(activeTabs.size()>0 && !tabs.addAll(activeTabs))
            throw new Exception("unable to combine tab groups");
        return tabs;
//        int count = getXpathCount(Locator.xpath(tabPath));
//        ArrayList<String> al = new ArrayList<String>(count);
//        for(int i=1; i<=count; i++)
//        {
//            al.add(getAttribute(Locator.xpath(tabPath + "[" + i + "]"), "href"));
//        }
//        return al;
    }

    public Collection<String> getTabUrls(boolean active)
    {
        String xpath = "(//li[@class = 'labkey-tab-inactive'])";
        if(active)
            xpath.replace("inactive", "active");

        int count = getXpathCount(Locator.xpath(xpath));
        ArrayList<String> al = new ArrayList<String>(count);
        for(int i=1; i<=count; i++)
        {
            al.add(getAttribute(Locator.xpath(xpath + "[" + i + "]/a"), "href"));
        }
        return al;

    }

    public static Collection collectionIntersection(Collection s1, Collection s2)
    {
        Set intersect = new TreeSet(s1);
        intersect.retainAll(s2);

        return intersect;
    }


    protected void reloadStudyFromZip(String studyFile)
    {
        goToManageStudy();
        clickButton("Reload Study");
        setFormElement("studyZip", studyFile);
        clickButton("Reload Study From Local Zip Archive");
        waitForPipelineJobsToComplete(2, "Study Reload", false);

    }

    public AbstractContainerHelper getContainerHelper()
    {
        return _containerHelper;
    }

    public void setContainerHelper(AbstractContainerHelper containerHelper)
    {
        _containerHelper = containerHelper;
    }


    //hopefully we'll come up with a better solution soon
    public void waitForSaveAssay()
    {
        sleep(5000);
    }


}
