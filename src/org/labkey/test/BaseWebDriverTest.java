/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.labkey.remoteapi.Connection;
import org.labkey.test.util.*;
import org.labkey.test.util.ext4cmp.Ext4CmpRefWD;
import org.labkey.test.util.ext4cmp.Ext4FieldRefWD;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import com.thoughtworks.selenium.webdriven.WebDriverBackedSelenium;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import static org.junit.Assert.*;
import static org.labkey.test.WebTestHelper.DEFAULT_TARGET_SERVER;
import static org.labkey.test.WebTestHelper.GC_ATTEMPT_LIMIT;
import static org.labkey.test.WebTestHelper.MAX_LEAK_LIMIT;
import static org.labkey.test.WebTestHelper.getHttpGetResponse;
import static org.labkey.test.WebTestHelper.getTargetServer;
import static org.labkey.test.WebTestHelper.leakCRC;
import static org.labkey.test.TestProperties.*;

public abstract class BaseWebDriverTest extends BaseSeleniumWebTest implements Cleanable, WebTest
{
    /**
     * @deprecated Refactor usages to use {@link #getDriver()}
     * private in order to block access of object in BSWT and force full migration
     */
    @Deprecated private WebDriverBackedSelenium selenium;
    public static BaseWebDriverTest currentTest;

    private static WebDriver _driver;
    private String _lastPageTitle = null;
    private URL _lastPageURL = null;
    private String _lastPageText = null;
    private Stack<String> _locationStack = new Stack<>();
    private String _savedLocation = null;
    private Stack<String> _impersonationStack = new Stack<>();
    private Set<WebTestHelper.FolderIdentifier> _createdFolders = new HashSet<>();
    protected static boolean _testFailed = false;
    protected static Boolean _anyTestCaseFailed;
    protected static boolean _setupFailed;
    protected boolean _testTimeout = false;
    public final static int WAIT_FOR_PAGE = 30000;
    public int defaultWaitForPage = WAIT_FOR_PAGE;
    public final static int WAIT_FOR_JAVASCRIPT = 10000;
    public int longWaitForPage = defaultWaitForPage * 5;
    protected static long _startTime;
    private List<JavaScriptError> _jsErrors;
    private static WebDriverWait _shortWait;
    private static WebDriverWait _longWait;
    private static JSErrorChecker _jsErrorChecker = null;

    public AbstractContainerHelper _containerHelper = new APIContainerHelper(this);
    public ExtHelperWD _extHelper = new ExtHelperWD(this);
    public Ext4HelperWD _ext4Helper = new Ext4HelperWD(this);
    public CustomizeViewsHelperWD _customizeViewsHelper = new CustomizeViewsHelperWD(this);
    public StudyHelperWD _studyHelper = new StudyHelperWD(this);
    public ListHelperWD _listHelper = new ListHelperWD(this);
    public AbstractUserHelper _userHelper = new APIUserHelper(this);
    public AbstractAssayHelper _assayHelper = new APIAssayHelper(this);
    public SecurityHelperWD _securityHelper = new SecurityHelperWD(this);
    public FileBrowserHelperWD _fileBrowserHelper = new FileBrowserHelperWD(this);
    private static File _downloadDir;

    private static final int MAX_SERVER_STARTUP_WAIT_SECONDS = 60;
    protected static final int MAX_WAIT_SECONDS = 10 * 60;

    public static final double DELTA = 10E-10;

    public static final String TRICKY_CHARACTERS = "><&/%\\' \"1\u00E4\u00F6\u00FC";
    public static final String TRICKY_CHARACTERS_NO_QUOTES = "></% 1\u00E4\u00F6\u00FC";

    public static String TRICKY_CHARACTERS_FOR_PROJECT_NAMES = "\u2603~!@$&()_+{}-=[],.#\u00E4\u00F6\u00FC";

    public static final String INJECT_CHARS_1 = "\"'>--><script>alert('8(');</script>;P";
    public static final String INJECT_CHARS_2 = "\"'>--><img src=xss onerror=alert(\"8(\")>\u2639";

    /** Have we already done a memory leak and error check in this test harness VM instance? */
    protected static boolean _checkedLeaksAndErrors = false;
    private static final String ACTION_SUMMARY_TABLE_NAME = "actions";

    private final BrowserType BROWSER_TYPE;

    protected static final String PERMISSION_ERROR = "User does not have permission to perform this operation";

    protected boolean isPerfTest = false;


    public BaseWebDriverTest()
    {
        _extHelper = new ExtHelperWD(this);
        _ext4Helper = new Ext4HelperWD(this);
        _listHelper = new ListHelperWD(this);
        _customizeViewsHelper = new CustomizeViewsHelperWD(this);
        _jsErrors = new ArrayList<>();
        _downloadDir = new File(ensureDumpDir(), "downloads");

        String seleniumBrowser = System.getProperty("selenium.browser");
        if (seleniumBrowser == null || "".equals(seleniumBrowser) || "*best".equals(seleniumBrowser.toLowerCase()))
        {
            if (isTestRunningOnTeamCity())
                BROWSER_TYPE = BrowserType.FIREFOX;
            else
                BROWSER_TYPE = bestBrowser();
        }
        else
        {
            if (seleniumBrowser.toLowerCase().startsWith("*firefox"))
                BROWSER_TYPE = BrowserType.FIREFOX;
            else if (seleniumBrowser.toLowerCase().startsWith("*ie"))
                BROWSER_TYPE = BrowserType.IE;
            else if (seleniumBrowser.toLowerCase().startsWith("*google"))
                BROWSER_TYPE = BrowserType.CHROME;
            else if (seleniumBrowser.toLowerCase().startsWith("*html"))
                BROWSER_TYPE = BrowserType.HTML;
            else
            {
                BROWSER_TYPE = bestBrowser();
                log("Unknown browser [" + seleniumBrowser + "]; Using best compatible browser [" + BROWSER_TYPE + "]");
            }
        }
    }


    public void pauseSearchCrawler()
    {
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("full-text search"));
        if (isTextPresent("pause crawler"))
            clickButton("pause crawler");
    }


    public void unpauseSearchCrawler()
    {
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("full-text search"));
        if (isTextPresent("unpause crawler"))
            clickButton("unpause crawler");
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

    public static File getDefaultFileRoot(String containerPath)
    {
        return new File(getLabKeyRoot(), "build/deploy/files/" + containerPath + "/@files");
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

    public void setUp() throws Exception
    {
        Boolean reusingDriver = false;

        switch (BROWSER_TYPE)
        {
            case IE: //experimental
            {
                if(_driver != null && !(_driver instanceof InternetExplorerDriver))
                {
                    _driver.quit();
                    _driver = null;
                }
                if(_driver == null)
                {
                    _driver = new InternetExplorerDriver();
                }
                else
                {
                    reusingDriver = true;
                }
                break;
            }
            case HTML: //experimental
            {
                if(_driver != null && !(_driver instanceof HtmlUnitDriver))
                {
                    _driver.quit();
                    _driver = null;
                }
                if(_driver == null)
                {
                    _driver = new HtmlUnitDriver(true);
                }
                else
                {
                    reusingDriver = true;
                }
                break;
            }
            case CHROME: //experimental
            {
                if(_driver != null && !(_driver instanceof ChromeDriver))
                {
                    _driver.quit();
                    _driver = null;
                }
                if(_driver == null)
                {
                    ChromeOptions options = new ChromeOptions();
                    Dictionary<String, Object> prefs = new Hashtable<>();

                    prefs.put("download.prompt_for_download", "false");
                    prefs.put("download.default_directory", getDownloadDir().getCanonicalPath());
                    options.setExperimentalOption("prefs", prefs);

                    if (isScriptCheckEnabled())
                    {
                        File jsErrorCheckerExtension = new File(getLabKeyRoot(), "server/test/chromeextensions/jsErrorChecker");
                        options.addArguments("load-extension=" + jsErrorCheckerExtension.toString());
                    }

                    DesiredCapabilities capabilities = DesiredCapabilities.chrome();
                    capabilities.setCapability(ChromeOptions.CAPABILITY, options);
                    _jsErrorChecker = new ChromeJSErrorChecker();
                    _driver = new ChromeDriver(capabilities);
                }
                else
                {
                    reusingDriver = true;
                }
                break;
            }
            case FIREFOX:
            {
                if(_driver != null && !(_driver instanceof FirefoxDriver))
                {
                    _driver.quit();
                    _driver = null;
                }
                if (_driver == null)
                {
                    final FirefoxProfile profile = new FirefoxProfile();
                    profile.setPreference("app.update.auto", false);
                    profile.setPreference("extensions.update.autoUpdate", false);
                    profile.setPreference("extensions.update.enabled", false);
                    profile.setPreference("dom.max_script_run_time", 0);
                    profile.setPreference("dom.max_chrome_script_run_time", 0);

                    profile.setPreference("browser.download.folderList", 2);
                    profile.setPreference("browser.download.downloadDir", getDownloadDir().getAbsolutePath());
                    profile.setPreference("browser.download.dir", getDownloadDir().getAbsolutePath());
                    profile.setPreference("browser.helperApps.alwaysAsk.force", false);
                    profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
                            "application/vnd.ms-excel," + // .xls
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet," + // .xlsx
                            "application/octet-stream," +
                            "application/x-gzip," +
                            "application/x-zip-compressed," +
                            "application/xml" +
                            "text/xml" +
                            "text/x-script.perl");
                    profile.setPreference("browser.download.manager.showWhenStarting",false);
                    if (isScriptCheckEnabled())
                    {
                        try
                            {JavaScriptError.addExtension(profile);}
                        catch(IOException e)
                            {throw new RuntimeException("Failed to load JS error checker", e);}
                    }
                    if (isFirefoxExtensionsEnabled() && !isTestRunningOnTeamCity()) // Firebug just clutters up screenshots on TeamCity
                    {
                        try
                        {
                            profile.addExtension(new File(getLabKeyRoot() + "/server/test/selenium/firebug-1.11.0.xpi"));
                            profile.addExtension(new File(getLabKeyRoot() + "/server/test/selenium/fireStarter-0.1a6.xpi"));
                            profile.setPreference("extensions.firebug.currentVersion", "1.11.0"); // prevent upgrade spash page
                            profile.setPreference("extensions.firebug.allPagesActivation", "on");
                            profile.setPreference("extensions.firebug.previousPlacement", 3);
                            profile.setPreference("extensions.firebug.net.enabledSites", true);

                            if (isFirebugPanelsEnabled()) // Enabling Firebug panels slows down test and is usually not needed
                            {
                                profile.setPreference("extensions.firebug.net.enableSites", true);
                                profile.setPreference("extensions.firebug.script.enableSites", true);
                                profile.setPreference("extensions.firebug.console.enableSites", true);
                            }
                        }
                        catch(IOException e)
                            {throw new RuntimeException("Failed to load Firebug", e);}
                    }

                    profile.setEnableNativeEvents(useNativeEvents());

                    String browserPath = System.getProperty("selenium.browser.path", "");
                    if (browserPath.length() > 0)
                    {
                        FirefoxBinary binary = new FirefoxBinary(new File(browserPath));
                        _driver = new FirefoxDriver(binary, profile);
                    }
                    else
                    {
                        _driver = new FirefoxDriver(profile);
                    }

                    _jsErrorChecker = new FirefoxJSErrorChecker();
                }
                else
                {
                    reusingDriver = true;
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Browser not yet implemented: " + BROWSER_TYPE);
        }

        if (!reusingDriver)
        {
            Capabilities caps = ((RemoteWebDriver) getDriver()).getCapabilities();
            String browserName = caps.getBrowserName();
            String browserVersion = caps.getVersion();
            log("Browser: " + browserName + " " + browserVersion);

            getDriver().manage().timeouts().setScriptTimeout(WAIT_FOR_JAVASCRIPT, TimeUnit.MILLISECONDS);
        }

        _shortWait = new WebDriverWait(getDriver(), WAIT_FOR_JAVASCRIPT/1000);
        _longWait = new WebDriverWait(getDriver(), WAIT_FOR_PAGE/1000);
    }

    public Object executeScript(String script, Object... arguments)
    {
        JavascriptExecutor exec = (JavascriptExecutor) getDriver();
        return exec.executeScript(script, arguments);
    }

    public void pauseJsErrorChecker()
    {
        if (_jsErrorChecker != null && isScriptCheckEnabled())
        {
            _jsErrorChecker.pause();
        }
    }

    public void resumeJsErrorChecker()
    {
        if (_jsErrorChecker != null && isScriptCheckEnabled())
        {
            _jsErrorChecker.resume();
        }
    }

    private interface JSErrorChecker
    {
        public void pause();
        public void resume();
        public @NotNull List<String> ignored();
    }

    private class FirefoxJSErrorChecker implements JSErrorChecker
    {
        private boolean jsCheckerPaused = false;
        private int _jsErrorPauseCount = 0; // To keep track of nested pauses

        @Override
        public void pause()
        {
            _jsErrorPauseCount++;
            if (!jsCheckerPaused)
            {
                jsCheckerPaused = true;
                _jsErrors.addAll(JavaScriptError.readErrors(getDriver()));
            }
        }

        @Override
        public void resume()
        {
            if (--_jsErrorPauseCount < 1 && jsCheckerPaused)
            {
                jsCheckerPaused = false;
                JavaScriptError.readErrors(getDriver()); // clear errors
            }
        }

        @Override @NotNull
        public List<String> ignored()
        {
            return Arrays.asList(
                    "[:0]", // Truncated JSON: "Ext.Error: You're trying to decode an invalid JSON String:"
                    "__webdriver_evaluate",
                    "setting a property that has only a getter",
                    "records[0].get is not a function",
                    "{file: \"chrome://",
                    "ext-all-sandbox-debug.js",
                    "ext-all-sandbox.js",
                    "ext-all-sandbox-dev.js",
                    "XULElement.selectedIndex", // Ignore known Firefox Issue
                    "Failed to decode base64 string!", // Firefox issue
                    "xulrunner-1.9.0.14/components/FeedProcessor.js", // Firefox problem
                    "Image corrupt or truncated:");
        }
    }

    private class ChromeJSErrorChecker implements JSErrorChecker
    {
        @Override
        public void pause()
        {
            executeScript(
                "window.dispatchEvent(new Event('pauseJsErrorChecker'))");
        }

        @Override
        public void resume()
        {
            executeScript(
                "window.dispatchEvent(new Event('resumeJsErrorChecker'))");
        }

        @Override @NotNull
        public List<String> ignored()
        {
            return Collections.emptyList(); // Add ignored chromedriver errors to jserrors.js
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
     */
    @Deprecated
    public boolean isFileUploadAvailable()
    {
        return true;
    }

    protected boolean isPipelineToolsTest()
    {
        return false;
    }

    /**
     * Set pipeline tools directory to the default location if the current location does not exist.
      */
    @LogMethod
    protected void fixPipelineToolsDirectory()
    {
        log("Ensuring pipeline tools directory points to the right place");
        goToHome();
        goToSiteSettings();
        File currentToolsDirectory = new File(getFormElement(Locator.name("pipelineToolsDirectory")));
        if(!currentToolsDirectory.exists())
        {
            log("Pipeline tools directory does not exist: " + currentToolsDirectory);
            File defaultToolsDirectory = new File(getLabKeyRoot() + "/build/deploy/bin");
            log("Setting to default tools directory" + defaultToolsDirectory.toString());
            setFormElement(Locator.name("pipelineToolsDirectory"), defaultToolsDirectory.toString());
            clickButton("Save");
        }
    }

    public enum BrowserType
    {
        FIREFOX("*firefox"),
        IE("*iexplore"),
        CHROME("*googlechrome"),
        HTML("*html");

        private String _seleniumBrowserString;

        private BrowserType(String seleniumBrowserString)
        {
            _seleniumBrowserString = seleniumBrowserString;
        }

        public String toString()
        {
            return _seleniumBrowserString;
        }
    }

    /**
     * The browser that can run the test fastest.
     * Firefox by default unless a faster browser (probably Chrome) has been verified.
     */
    protected BrowserType bestBrowser()
    {
        return BrowserType.FIREFOX;
    }

    public BrowserType getBrowserType()
    {
        return BROWSER_TYPE;
    }

    public String getBrowser()
    {
        return getBrowserType().toString();
    }

    public void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException ignore)
        {
        }
    }

    public static String getStreamContentsAsString(InputStream is) throws IOException
    {
        StringBuilder contents = new StringBuilder();
        try(BufferedReader input = new BufferedReader(new InputStreamReader(is)))
        {
            String line;
            while ((line = input.readLine()) != null)
            {
                contents.append(line);
                contents.append("\n");
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
        try
        {
            Files.createDirectories(Paths.get(copy.toURI()).getParent());
            Files.copy(Paths.get(original.toURI()), Paths.get(copy.toURI()),
                    StandardCopyOption.COPY_ATTRIBUTES,
                    StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @After
    public void tearDown() throws Exception
    {
        //Poorly named method to prevent tearDown after each test case
        checkJsErrors();
    }

    public void doTearDown()
    {
        boolean skipTearDown = _testFailed && "false".equalsIgnoreCase(System.getProperty("close.on.fail"));
        if ((!skipTearDown || isTestRunningOnTeamCity()) && getDriver() != null)
        {
            getDriver().quit();
            _driver = null;
        }
    }

    private boolean validateJsError(JavaScriptError error)
    {
        for (String ignoredText : _jsErrorChecker.ignored())
        {
            if(error.toString().contains(ignoredText))
                return false;
        }

        return true;
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
        String title = getDriver().getTitle();
        if (!title.toLowerCase().contains("error"))
            return 200;

        Matcher m = LABKEY_ERROR_TITLE_PATTERN.matcher(title);
        if (m.matches())
            return Integer.parseInt(title.substring(0, 3));

        //Now check the Tomcat page. This is going to be unreliable over time
        m = TOMCAT_ERROR_PATTERN.matcher(getDriver().getPageSource());
        if (m.find())
            return Integer.parseInt(m.group(1));

        return 200;
    }

    public URL getURL()
    {
        try
        {
            return new URL(getDriver().getCurrentUrl());
        }
        catch (MalformedURLException x)
        {
            throw new RuntimeException("Bad location from selenium tester: " + getDriver().getCurrentUrl(), x);
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
        @SuppressWarnings("unchecked")
        List<String> linkArray = (ArrayList<String>)executeScript(js);
        ArrayList<String> links = new ArrayList<>();
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
        String urlString = getDriver().getCurrentUrl();
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
        assertNotNull("Cannot recall without saving first.", _savedLocation);
        beginAt(_savedLocation, wait);
    }

    public void refresh()
    {
        refresh(defaultWaitForPage);
    }

    public void refresh(int millis)
    {
        prepForPageLoad();
        getDriver().navigate().refresh();
        newWaitForPageToLoad(millis);
    }

    public void goBack(int millis)
    {
        prepForPageLoad();
        getDriver().navigate().back();
        newWaitForPageToLoad(millis);
    }

    public void goBack()
    {
        goBack(defaultWaitForPage);
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
            throw new RuntimeException("Unable to ensure credentials", e);
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
            waitForElement(Locator.id("email"), defaultWaitForPage);
            assertElementPresent(Locator.tagWithName("form", "login"));
            setFormElement(Locator.name("email"), PasswordUtil.getUsername());
            setFormElement(Locator.name("password"), PasswordUtil.getPassword());
            clickButton("Sign In");

            if (isTextPresent("Type in your email address and password"))
                throw new IllegalStateException("Could not log in with the saved credentials.  Please verify that the test user exists on this installation or reset the credentials using 'ant setPassword'");
        }

        assertSignOutAndMyAccountPresent();

        if (isElementPresent(Locator.css(".labkey-nav-page-header").withText("Startup Modules"))||
            isElementPresent(Locator.css(".labkey-nav-page-header").withText("Upgrade Modules")))
        {
            waitForElement(Locator.id("status-progress-bar").withText("Module startup complete"));
            clickButton("Next");
        }
    }

    public void assertSignOutAndMyAccountPresent()
    {
        click(Locator.id("userMenuPopupLink"));
        assertElementPresent(Locator.id("userMenu").append(Locator.linkWithText("My Account")));
        assertElementPresent(Locator.id("userMenu").append(Locator.linkWithText("Sign Out")));
    }

    // Just sign in & verify -- don't check for startup, upgrade, admin mode, etc.
    public void signIn(String email, String password, boolean failOnError)
    {
        if ( !isElementPresent(Locator.linkWithText("Sign In")) )
            throw new IllegalStateException("You need to be logged out to log in.  Please log out to log in.");

        attemptSignIn(email, password);

        if ( failOnError )
        {
            if ( isTextPresent("Type in your email address and password") )
                throw new IllegalStateException("Could not log in with the saved credentials.  Please verify that the test user exists on this installation or reset the credentials using 'ant setPassword'");

            assertSignOutAndMyAccountPresent();
        }
    }


    public void attemptSignIn(String email, String password)
    {
        clickAndWait(Locator.linkWithText("Sign In"));

        assertTitleEquals("Sign In");
        assertElementPresent(Locator.tagWithName("form", "login"));
        setFormElement(Locator.id("email"), email);
        setFormElement(Locator.id("password"), password, true);
        clickButton("Sign In");
    }

    public void signInShouldFail(String email, String password, String... expectedMessages)
    {
        attemptSignIn(email, password);
        assertTitleEquals("Sign In");
        assertElementPresent(Locator.tagWithName("form", "login"));

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
        assertNotNull("Link for '" + user + "' not found", link);

        String emailSubject = link.getText();
        link.click();

        WebElement resetLink = shortWait().until(ExpectedConditions.elementToBeClickable(By.xpath("//table[@id='dataregion_EmailRecord']//a[text() = '" + emailSubject + "']/..//a[contains(@href, 'setPassword.view')]")));
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
        WebElement email = getDriver().findElement(By.xpath("//table[@id='dataregion_EmailRecord']//td[text() = '" + username + "']/..//a[starts-with(text(), '" + emailSubject + "')]"));
        email.click();
        WebElement resetLink = shortWait().until(ExpectedConditions.elementToBeClickable(By.xpath("//table[@id='dataregion_EmailRecord']//td[text() = '" + username + "']/..//a[contains(@href, 'setPassword.view')]")));
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

    @LogMethod protected void changePassword(String oldPassword, @LoggedParam String password)
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
        clickAndWait(Locator.linkContainingText(displayNameFromEmail(userEmail)));

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

            if ( oldStrength != null ) assertEquals("Unable to reset password strength.", oldStrength, PasswordRule.valueOf(getText(Locator.xpath("//input[@name='strength' and @value='Weak']/.."))));
            if ( oldExpiration != null ) assertEquals("Unable to reset password expiration.", oldExpiration, PasswordExpiration.valueOf(getFormElement(Locator.name("expiration"))));

            // Back to default.
            oldStrength = null;
            oldExpiration = null;

            popLocation();
        }
    }

    protected void setSystemMaintenance(boolean enable)
    {
        // Not available in production mode
        if (isDevModeEnabled())
        {
            goToAdminConsole();
            clickAndWait(Locator.linkWithText("system maintenance"));

            if (enable)
                checkCheckbox(Locator.name("enableSystemMaintenance"));
            else
                uncheckCheckbox(Locator.name("enableSystemMaintenance"));

            clickButton("Save");
        }
    }

    public void ensureAdminMode()
    {
        if (!isElementPresent(Locator.css("#adminMenuPopupText")))
            stopImpersonating();
        if (!isElementPresent(Locator.id("projectBar")))
        {
            goToHome();
            waitForElement(Locator.id("projectBar"), WAIT_FOR_PAGE);
        }
    }

    public void goToAdminConsole()
    {
        goToHome();
        clickAdminMenuItem("Site", "Admin Console");
    }

    public void goToSiteSettings()
    {
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("site settings"));
    }

    public void goToAuditLog()
    {
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("audit log"));
    }

    public void hideNavigationBar()
    {
        clickAndWait(Locator.xpath("//a/span[text() = 'Admin']"), 0);
        waitForElement(Locator.tagContainingText("span", "Navigation Bar"), 1000);
        if (isElementPresent(Locator.tagContainingText("span", "Hide Navigation Bar")))
            clickAndWait(Locator.tagContainingText("span", "Hide Navigation Bar"));
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.PortalHelper#clickWebpartMenuItem(String, String...)}
     * Allows test code to navigate to a Webpart Ext-based navigation menu.
     * @param webPartTitle title (not name) of webpart to be clicked.  Multiple web parts with the same title not supported.
     * @param items Text of cascading sub-menus
     */
    @Deprecated public void clickWebpartMenuItem(String webPartTitle, String... items)
    {
        clickWebpartMenuItem(webPartTitle, true, items);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.PortalHelper#clickWebpartMenuItem(String, boolean, String...)}
     */
    @Deprecated public void clickWebpartMenuItem(String webPartTitle, boolean wait, String... items)
    {
        _extHelper.clickExtMenuButton(wait, Locator.xpath("//img[@id='more-" + webPartTitle.toLowerCase() + "']"), items);
    }

    // Clicks admin menu items. Tests should use helpers to make admin menu changes less disruptive.
    protected void clickAdminMenuItem(String... items)
    {
        shortWait().until(ExpectedConditions.elementToBeClickable(Locators.ADMIN_MENU.toBy()));
        _ext4Helper.clickExt4MenuButton(true, Locators.ADMIN_MENU, false, items);
    }

    public void clickUserMenuItem(String... items)
    {
        clickUserMenuItem(true, false, items);
    }

    public void clickUserMenuItem(boolean wait, boolean onlyOpen, String... items)
    {
        waitForElement(Locators.USER_MENU);
        _ext4Helper.clickExt4MenuButton(true, Locators.USER_MENU, onlyOpen, items);
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
        if (!isElementPresent(Locator.id("labkey-nav-trail-current-page").withText("Site Users")))
            clickAdminMenuItem("Site", "Site Users");
    }

    public void goToSiteGroups()
    {
        if (!isElementPresent(Locator.tag("a").withClass("x4-tab-active").withText("Site Groups")))
            clickAdminMenuItem("Site", "Site Groups");
    }

    public void goToSiteDevelopers()
    {
        if (!isElementPresent(Locator.id("labkey-nav-trail-current-page").withText("Developers Group")))
        {
            clickAdminMenuItem("Site", "Site Developers");
            waitForElement(Locator.name("names"));
        }
    }

    public void goToSiteAdmins()
    {
        if (!isElementPresent(Locator.id("labkey-nav-trail-current-page").withText("Administrators Group")))
        {
            clickAdminMenuItem("Site", "Site Admins");
        }
    }

    public void goToManageViews()
    {
        clickAdminMenuItem("Manage Views");
        waitForElement(Locator.xpath("//*[starts-with(@id, 'dataviews-panel')]"));
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
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
            throw new RuntimeException("Webapp failed to start up after " + MAX_SERVER_STARTUP_WAIT_SECONDS + " seconds.");
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

                while (waitMs > 0 && (!(isNavButtonPresent("Next") || isElementPresent(Locator.linkWithText("Home")))))
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
                            throw new RuntimeException("A startup failure occurred.");
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
                    throw new TestTimeoutException("Script runner took more than 10 minutes to complete.");

                if (isNavButtonPresent("Next"))
                {
                    clickButton("Next");

                    // check for any additional upgrade pages inserted after module upgrade
                    if (isNavButtonPresent("Next"))
                        clickButton("Next");
                }

                if (isElementPresent(Locator.linkContainingText("Go directly to the server's Home page")))
                {
                    clickAndWait(Locator.linkContainingText("Go directly to the server's Home page"));
                }
            }

            // Will fail if left navbar is not enabled in Home project. TODO: allow this, see #xxxx
            clickAndWait(Locator.linkWithText("Home"));
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

        clickAndWait(Locator.linkWithText("Next"));

        if (null != expectedText)
            assertEquals("Wrong error message.", expectedText, Locator.css(".labkey-error").findElement(getDriver()).getText());
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
        HttpResponse response;
        HttpUriRequest method;
        int status;

        try
        {
            // These requests should NOT redirect to the upgrade page

            method = new HttpGet(getBaseURL() + "/login/resetPassword.view");
            response = client.execute(method, context);
            status = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected response", HttpStatus.SC_OK, status);
            assertFalse("Upgrade text found", WebTestHelper.getHttpResponseBody(response).contains(upgradeText));
            EntityUtils.consume(response.getEntity());

            method = new HttpGet(getBaseURL() + "/admin/maintenance.view");
            response = client.execute(method, context);
            status = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected response", HttpStatus.SC_OK, status);
            assertFalse("Upgrade text found", WebTestHelper.getHttpResponseBody(response).contains(upgradeText));
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
                    } catch (ProtocolException ignore) {}
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
            List<NameValuePair> args = new ArrayList<>();
            args.add(new BasicNameValuePair("login", PasswordUtil.getUsername()));
            args.add(new BasicNameValuePair("password", PasswordUtil.getPassword()));
            ((HttpPost)method).setEntity(new UrlEncodedFormEntity(args));
            response = client.execute(method, context);
            status = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected response", HttpStatus.SC_OK, status);
            // TODO: check login, once http-equiv redirect is sorted out
            assertFalse("Upgrade text found", WebTestHelper.getHttpResponseBody(response).contains(upgradeText));
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
        Set<String> windows = new HashSet<>(getDriver().getWindowHandles());
        getDriver().switchTo().window((String) windows.toArray()[0]);
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
        clickAndWait(Locator.linkWithText("system maintenance"));
        click(Locator.linkWithText(task));
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

        Object[] windows = getDriver().getWindowHandles().toArray();
        getDriver().switchTo().window((String)windows[1]);
        log("Waiting for system maintenance to complete");

        int timeLeft = 10 * 60 * 1000 - ((Long)elapsed).intValue();
        // Page updates automatically via AJAX... keep checking (up to 10 minutes from the start of the test) for system maintenance complete text
        waitFor(new Checker() {
            public boolean check()
            {
                return isTextPresent("System maintenance complete");
            }
        }, "System maintenance failed to complete in 10 minutes.", timeLeft > 0 ? timeLeft : 0);

        getDriver().close();
        getDriver().switchTo().window((String)windows[0]);
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
            if (null != getDriver().getTitle())
                return getDriver().getTitle();
            else
                return "[no title: content type is not html]";
        }
        return _lastPageTitle;
    }

    public String getLastPageText()
    {
        return _lastPageText != null ? _lastPageText : getDriver().getPageSource();
    }

    public boolean isPageEmpty()
    {
        //IE and Firefox have different notions of empty.
        //IE returns html for all pages even empty text...
        String text = getDriver().getPageSource();
        if (null == text || text.trim().length() == 0)
            return true;

        text = getBodyText();
        return null == text || text.trim().length() == 0;
    }

    public URL getLastPageURL()
    {
        try
        {
            return _lastPageURL != null ? _lastPageURL : new URL(getDriver().getCurrentUrl());
        }
        catch (MalformedURLException x)
        {
            return null;
        }
    }

    /**
     * Get rendered, visible page text.
     * @return All visible text from the 'body' of the page
     */
    public String getBodyText()
    {
        RunnableGetText getText = new RunnableGetText();
        final Thread t = new Thread(getText);
        t.start();

        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                return !t.isAlive();
            }
        }, "Timed out getting page text. Page is probably too complex. Refactor test to look for specific element(s) instead.", 60000);

        return getText.getResult();
    }

    /**
     * Get page text using a separate thread to avoid test timeouts when complex pages choke WebDriver
     */
    private class RunnableGetText implements Runnable
    {
        private String _text;

        public String getResult()
        {
            return this._text;
        }

        @Override
        public void run()
        {
            try
            {
                _text = getDriver().findElement(By.cssSelector("body")).getText();
            }
            catch (StaleElementReferenceException|NoSuchElementException ex)
            {
                try
                {
                    _text =  shortWait().until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body"))).getText();
                }
                catch (TimeoutException tex)
                {
                    _text =  getDriver().getPageSource(); // probably viewing a tsv or text file
                }

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

    @LogMethod @BeforeClass
    public static void performInitialChecks() throws Throwable
    {
        _setupFailed = true;
        _anyTestCaseFailed = false;

        _startTime = System.currentTimeMillis();
        WebDriverTestPreamble preamble = new WebDriverTestPreamble();

        try
        {
            preamble.setUp();
            preamble.preamble();
        }
        catch (Exception t)
        {
            AtomicReference<Throwable> errorRef = new AtomicReference<Throwable>(t);
            preamble.handleFailure(errorRef, "performInitialChecks");
            throw errorRef.get();
        }
    }

    @Before
    public void preClean() throws TestTimeoutException
    {
        simpleSignIn();

        // Perform initial cleanup for tests using doTestSteps.
        if (currentTest == null && getDriver() != null)
        {
            currentTest = this;
            log("Pre-cleaning " + getClass().getSimpleName());
            currentTest.doCleanup(false);
        }

        _setupFailed = false;
    }

    @Test
    public void testSteps() throws Exception
    {
        doTestSteps();
    }

    @Rule
    public Timeout globalTimeout = new Timeout(1800000); // 30 minutes max per method tested

    @Rule
    public TestWatcher _watcher = new TestWatcher()
    {
        @Override @LogMethod
        protected void failed(Throwable e, Description description)
        {
            handleFailure(new AtomicReference<Throwable>(e), description.getMethodName());

            super.failed(e, description);
        }
    };

    /**
     * Collect additional information about test failures and publish build artifacts for TeamCity
     * @param errorRef Reference to the cause of the test failure. Reference allows us to insert alert text into failure
     * @param testName The method name or short description of the failed test case or setup/teardown method
     */
    public void handleFailure(AtomicReference<Throwable> errorRef, String testName)
    {
        _testFailed = true;
        _anyTestCaseFailed = true;

        if (errorRef.get() instanceof UnhandledAlertException)    // Catch so we can record the alert's text
        {
            errorRef.set(new RuntimeException("Unexpected Alert: " + getAlert(), errorRef.get()));
        }
        else if (errorRef.get() instanceof TestTimeoutException)
        {
            _testTimeout = true;
        }

        try
        {
            populateLastPageInfo();
        }
        catch (Throwable t)
        {
            System.out.println("Unable to determine information about the last page: server not started or -Dlabkey.port incorrect?");
        }

        if (_lastPageTitle != null && !_lastPageTitle.startsWith("404") && _lastPageURL != null)
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
            dumpPageSnapshot(testName, null);
            if (isTestRunningOnTeamCity())
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
            catch(Throwable t){
                log("Failed to reset DB login config after test failure");
                dumpPageSnapshot(testName, "resetDbLogin");
            }

            try
            {
                if (isPipelineToolsTest()) // Get DB back in a good state after failed pipeline tools test.
                    fixPipelineToolsDirectory();
            }
            catch(Throwable t){
                // Assure that this failure is noticed
                // Regression check: https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=10732
                log("**************************ERROR*******************************");
                log("** SERIOUS ERROR: Failed to reset pipeline tools directory. **");
                log("** Server remains in a bad state.                           **");
                log("** Set tools directory manually or bootstrap to fix.        **");
                log("**************************ERROR*******************************");
                dumpPageSnapshot(testName, "fixPipelineToolsDir");
            }

            checkJsErrors();

            doTearDown();
            _driver = null;
        }
    }

    @LogMethod @AfterClass
    public static void performFinalChecks() throws Throwable
    {
        _testFailed = false;
        WebDriverTestPostamble postamble = new WebDriverTestPostamble();

        if (_setupFailed)
        {
            try
            {
                AtomicReference<Throwable> errorRef = new AtomicReference<Throwable>(null);
                postamble.setUp();
                postamble.handleFailure(errorRef, "TestSetup");
            }
            finally
            {
                currentTest = null;
                postamble.doTearDown();
            }
        }
        else
        {
            try
            {
                postamble.postamble();
            }
            catch (Throwable t)
            {
                AtomicReference<Throwable> errorRef = new AtomicReference<Throwable>(t);
                postamble.handleFailure(errorRef, "performFinalChecks");
                throw errorRef.get();
            }
            finally
            {
                currentTest = null;
                postamble.doTearDown();
            }
        }
    }

    @LogMethod
    public void ensureSignedInAsAdmin()
    {
        goToHome();

        if(isElementPresent(Locator.tagWithText("span", "Stop Impersonating")))
            stopImpersonatingRole();

        if(isElementPresent(Locator.id("adminMenuPopupText")))
            return;

        Locator.IdLocator userMenuPopupLink = Locator.id("userMenuPopupLink");
        if (isElementPresent(userMenuPopupLink))
        {
            click(userMenuPopupLink);

            if(isElementPresent(Locator.tagWithText("span", "Stop Impersonating")))
                stopImpersonatingRole();
            else
                signOut();
        }

        if(!isElementPresent(Locator.id("adminMenuPopupText")))
            signIn();
    }

    @LogMethod
    protected abstract void doTestSteps() throws Exception;

    // Standard cleanup: delete the project
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
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
            hoverFolderBar();
            for (WebTestHelper.FolderIdentifier folder : _createdFolders)
                assertElementNotPresent(Locator.linkWithText(folder.getFolderName()));

            hoverProjectBar();
            for (String projectName : _containerHelper.getCreatedProjects())
                assertElementNotPresent(Locator.linkWithText(projectName));
            log("========= " + getClass().getSimpleName() + " cleanup complete =========");
        }
        finally
        {
            tearDown();
        }
    }

    public WebDriver getDriver()
    {
        return _driver;
    }

    public static File getDownloadDir()
    {
        return _downloadDir;
    }

    public WebDriverWait shortWait()
    {
        return _shortWait;
    }

    public WebDriverWait longWait()
    {
        return _longWait;
    }

    public void downloadFileFromLink(Locator downloadLink)
    {
        if (BROWSER_TYPE == BrowserType.FIREFOX)
        {
            click(downloadLink);
        }
        else
        {
            String href = downloadLink.findElement(getDriver()).getAttribute("href");
            URL url;
            try
            {
                url = new URL(href);
            }
            catch (MalformedURLException ex)
            {
                throw new RuntimeException(ex);
            }

            String fileName = href.substring(href.lastIndexOf("/") + 1);

            try
            {
                FileUtils.copyURLToFile(url, new File(getDownloadDir(), fileName));
            }
            catch (IOException ex)
            {
                throw new RuntimeException(ex);
            }
        }
    }

    public boolean isGroupConcatSupported()
    {
        return "pg".equals(getDatabaseType()) ||
               "mssql".equals(getDatabaseType()) && !"2005".equals(getDatabaseVersion());
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
        if (isLeakCheckSkipped())
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
                throw new IllegalStateException("Asserts must be enabled to track memory leaks; please add -ea to your server VM params and restart.");
            leakCount = getImageWithAltTextCount("expand/collapse");
        }

        if (leakCount > MAX_LEAK_LIMIT)
        {
            String leaks = Locator.name("leaks").findElement(getDriver()).getText();
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

        assertEquals("Expected error count does not match actual count for this run.", count, StringUtils.countMatches(text, "ERROR"));
        log("Found " + count + " expected errors.");

        // Clear the errors to prevent the test from failing.
        resetErrors();

        popLocation();
    }

    @LogMethod
    protected void checkQueries()
    {
        if (isQueryCheckSkipped())
            return;
        if(getProjectName() != null)
        {
            clickProject(getProjectName());
            if(!"Query Schema Browser".equals(getDriver().getTitle()))
                goToSchemaBrowser();
            validateQueries(true);
//            validateLabAuditTrail();
        }
    }

    @LogMethod
    protected void checkViews()
    {
        if (isViewCheckSkipped())
            return;

        List<String> checked = new ArrayList<>();

        for (String projectName : _containerHelper.getCreatedProjects())
        {
            clickProject(projectName);

            doViewCheck(projectName);
            checked.add(projectName);
        }

        for (WebTestHelper.FolderIdentifier folderId : _createdFolders)
        {
            String project = folderId.getProjectName();
            String folder = folderId.getFolderName();
            if(!checked.contains(project))
            {
                clickProject(project);

                doViewCheck(project);
                checked.add(project);
            }
            if(!checked.contains(folder))
            {
                if (!getText(Locator.id("folderBar")).equals(project))
                    clickProject(project);
                clickFolder(folder);

                doViewCheck(folder);
                checked.add(folder);
            }
        }
    }

    /**
     * TODO: 7695: Custom views are not deleted when list is deleted
     * @return List of view names which are no longer valid
     */
    protected Set<String> getOrphanedViews()
    {
        return new HashSet<>();
    }

    /**
     * To be overloaded by tests
     * @return The Set of folder names to be excluded from the view check
     */
    protected Set<String> excludeFromViewCheck()
    {
        return new HashSet<>();
    }

    @LogMethod
    private void doViewCheck(@LoggedParam String folder)
    {
        if (excludeFromViewCheck().contains(folder))
        {
            log ("Skipping view check for folder");
            return;
        }

        try{
            goToManageViews();
        }
        catch (SeleniumException e)
        {
            log("No manage views option");
            return;
        }

        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        Locator.XPathLocator view = Locator.xpath("//div[contains(@class, 'x-grid-group-body')]/div[contains(@class, 'x-grid3-row')]");
        int viewCount = getElementCount(view);
        for (int i = 0; i < viewCount; i++)
        {
            Locator.XPathLocator thisView = view.index(i);
            waitForElement(thisView);
            String viewName = getText(thisView.append("//td[contains(@class, 'x-grid3-cell-first')]"));
            if (!getOrphanedViews().contains(viewName))
            {
                pushLocation();
                click(thisView);

                Locator.XPathLocator expandedReport = Locator.tag("div").withClass("x-grid3-row-expanded");

                String reportType = getAttribute(expandedReport.append("//div").withClass("x-grid3-col-1").append("/img"), "alt");
                String schemaName = getText(expandedReport.append("//td[normalize-space()='schema name']/following-sibling::td"));
                String queryName = getText(expandedReport.append("//td[normalize-space()='query name']/following-sibling::td"));
                String viewString = viewName + " of " + schemaName + "." + queryName;

                if ("Stand-alone views".equals(queryName))
                {
                    log("Checking view: " + viewName);
                    waitAndClick(Locator.linkWithText("VIEW"));
                    Set<String> windows = getDriver().getWindowHandles();
                    if (windows.size() > 1)
                    {
                        getDriver().switchTo().window((String)windows.toArray()[1]);
                        assertEquals(200, getResponseCode());
                        getDriver().close();
                        getDriver().switchTo().window((String)windows.toArray()[0]);
                    }
                    else
                    {
                        assertEquals(200, getResponseCode());
                    }
                }
                else
                {
                    log("Checking view: " + viewString);
                    waitAndClickAndWait(Locator.linkWithText("VIEW"));
                    waitForText(viewName);
                }

                popLocation();
                _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
            }
            else
                log("Skipping manually excluded view: " + viewName);
        }
    }

    @LogMethod
    protected void checkJsErrors()
    {
        if (isScriptCheckEnabled())
        {
            int duplicateCount = 0;
            try
            {
                _jsErrors = JavaScriptError.readErrors(getDriver());
            }
            catch (WebDriverException ex)
            {
                return; // Error checker has not been initialized
            }
            Set<JavaScriptError> validErrors = new HashSet<>();
            Set<JavaScriptError> ignoredErrors = new HashSet<>();
            for (JavaScriptError error : _jsErrors)
            {
                if (!validErrors.contains(error) && !ignoredErrors.contains(error)) // Don't log duplicate errors
                {
                    if (validErrors.size() + ignoredErrors.size() == 0)
                        log("<<<<<<<<<<<<<<<JAVASCRIPT ERRORS>>>>>>>>>>>>>>>"); // first error

                    if (validateJsError(error))
                    {
                        validErrors.add(error);
                        log(error.toString());
                    }
                    else // log ignored errors, but don't fail
                    {
                        ignoredErrors.add(error);
                        log("[Ignored] " + error.toString());
                    }
                }
                else
                    duplicateCount++;
            }
            if (duplicateCount > 0)
                log(duplicateCount + " duplicate errors.");

            if (validErrors.size() + ignoredErrors.size() > 0)
                log("<<<<<<<<<<<<<<<JAVASCRIPT ERRORS>>>>>>>>>>>>>>>");

            if (validErrors.size() > 0)
            {
                String errorCtStr = "";
                if (validErrors.size() > 1)
                    errorCtStr = " (1 of " + validErrors.size() + ") ";
                if (!_testFailed) // Don't clobber existing failures. Just log them.
                    fail("JavaScript error" + errorCtStr + ": " + validErrors.toArray()[0]);
            }
        }
    }

    public Connection getDefaultConnection()
    {
        return new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }

    @LogMethod
    protected void checkActionCoverage()
    {
        if ( isGuestModeTest() )
            return;

        pushLocation();
        int rowCount, coveredActions, totalActions;
        Double actionCoveragePercent;
        String actionCoveragePercentString;
        beginAt("/admin/actions.view");

        rowCount = getTableRowCount(ACTION_SUMMARY_TABLE_NAME);
        if (getTableCellText(Locator.id(ACTION_SUMMARY_TABLE_NAME), rowCount - 1, 0).equals("Total"))
        {
            totalActions = Integer.parseInt(getTableCellText(Locator.id(ACTION_SUMMARY_TABLE_NAME), rowCount - 1, 1));
            coveredActions = Integer.parseInt(getTableCellText(Locator.id(ACTION_SUMMARY_TABLE_NAME), rowCount - 1, 2));
            actionCoveragePercentString = getTableCellText(Locator.id(ACTION_SUMMARY_TABLE_NAME), rowCount - 1, 3);
            actionCoveragePercent =  Double.parseDouble(actionCoveragePercentString.substring(0, actionCoveragePercentString.length() - 1 ));
            writeActionStatistics(totalActions, coveredActions, actionCoveragePercent);
        }

        // Download full action coverage table and add to TeamCity artifacts.
        beginAt("/admin/exportActions.view?asWebPage=true");
        publishArtifact(saveTsv(TestProperties.getDumpDir(), "ActionCoverage"));
        popLocation();
    }

    @LogMethod
    protected void checkLinks()
    {
        if (isLinkCheckEnabled())
        {
            pauseJsErrorChecker();
            Crawler crawler = new Crawler(this, Runner.getTestSet().getCrawlerTimeout());
            crawler.crawlAllLinks(isInjectCheckEnabled());
            resumeJsErrorChecker();
        }
    }

    private void writeActionStatistics(int totalActions, int coveredActions, Double actionCoveragePercent)
    {
        // TODO: Create static class for managing teamcity-info.xml file.
        File xmlFile = new File(getLabKeyRoot(), "teamcity-info.xml");
        try (FileWriter writer = new FileWriter(xmlFile))
        {
            xmlFile.createNewFile();

            writer.write("<build>\n");
            writer.write("\t<statisticValue key=\"totalActions\" value=\"" + totalActions + "\"/>\n");
            writer.write("\t<statisticValue key=\"coveredActions\" value=\"" + coveredActions + "\"/>\n");
            writer.write("\t<statisticValue key=\"actionCoveragePercent\" value=\"" + actionCoveragePercent + "\"/>\n");
            writer.write("</build>");
        }
        catch (IOException ignore){}
    }

    protected File ensureDumpDir()
    {
        File dumpDir = new File(TestProperties.getDumpDir(), Runner.getCurrentTestName());
        if ( !dumpDir.exists() )
            dumpDir.mkdirs();

        return dumpDir;
    }

    private static final String DEFAULT_TEST_NAME = "testSteps";

    public void dumpPageSnapshot()
    {
        dumpPageSnapshot(null);
    }

    public void dumpPageSnapshot(@Nullable String subdir)
    {
        dumpPageSnapshot(DEFAULT_TEST_NAME, subdir);
    }

    public void dumpPageSnapshot(String testName, @Nullable String subdir)
    {
        File dumpDir = ensureDumpDir();
        if (subdir != null && subdir.length() > 0)
        {
            dumpDir = new File(dumpDir, subdir);
            if ( !dumpDir.exists() )
                dumpDir.mkdirs();
        }

        FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMddHHmm");
        String baseName = dateFormat.format(new Date()) + getClass().getSimpleName();
        if (!DEFAULT_TEST_NAME.equals(testName))
            baseName += "#" + testName;

        publishArtifact(dumpFullScreen(dumpDir, baseName));
        publishArtifact(dumpScreen(dumpDir, baseName));
        publishArtifact(dumpHtml(dumpDir, baseName));
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
            String dumpMsg = Locator.css("#bodypanel > div").findElement(getDriver()).getText();
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
        if (file != null && isTestRunningOnTeamCity())
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
            File tempScreen = ((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(tempScreen, screenFile);
            return screenFile;
        }
        catch (IOException ioe)
        {
            log("Failed to copy screenshot file: " + ioe.getMessage());
        }
        catch (Exception ignore)
        {
            try
            {
                // Use alternate method for screen grab. Page might be too tall for TakesScreenshot.getScreenshotAs()
                windowMaximize();
                return dumpFullScreen(dir, baseName);
            }
            catch (Exception ex) { log("Failed to take screenshot: " + ex.getMessage()); }
        }

        return null;
    }

    public void windowMaximize()
    {
        getDriver().manage().window().maximize();
    }

    public File dumpFullScreen(File dir, String baseName)
    {
        File screenFile = new File(dir, baseName + "Fullscreen.png");

        // Windows doesn't support OS level screenshots for headless environment
        if (!isTestRunningOnTeamCity() || !System.getProperty("os.name").toLowerCase().contains("win"))
        {
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
        }

        return null;
    }

    public File dumpHtml(File dir, String baseName)
    {
        if (getLastPageText() == null)
            return null;

        File htmlFile = new File(dir, baseName + ".html");
        try(FileWriter writer = new FileWriter(htmlFile))
        {
            writer.write(getLastPageText());
            return htmlFile;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
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
        File tsvFile = new File(dir, fileName);

        try(FileWriter writer = new FileWriter(tsvFile))
        {
            writer.write(contents);
            return tsvFile;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
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

    public long beginAt(String relativeURL)
    {
        return beginAt(relativeURL, defaultWaitForPage);
    }

    public long beginAt(String relativeURL, int millis)
    {
        relativeURL = stripContextPath(relativeURL);
        String logMessage = "";

        try
        {
            if (relativeURL.length() == 0)
                logMessage = "Navigating to root";
            else
            {
                logMessage = "Navigating to " + relativeURL;
                if (relativeURL.charAt(0) != '/')
                {
                    relativeURL = "/" + relativeURL;
                }
            }
            pauseJsErrorChecker();
            prepForPageLoad();

            long startTime = System.currentTimeMillis();
            getDriver().navigate().to(getBaseURL() + relativeURL);
            newWaitForPageToLoad(millis);
            long elapsedTime = System.currentTimeMillis() - startTime;
            logMessage += " [" + elapsedTime + " ms]";

            resumeJsErrorChecker();

            return elapsedTime;
        }
        finally
        {
            log(logMessage); // log after navigation to
        }
    }

    public long goToURL(URL url, int milliseconds)
    {
        String logMessage = "Navigating to " + url.toString();
        try
        {
            pauseJsErrorChecker();
            prepForPageLoad();
            long startTime = System.currentTimeMillis();
            getDriver().navigate().to(url);
            newWaitForPageToLoad(milliseconds);
            long elapsedTime = System.currentTimeMillis() - startTime;
            logMessage += " [" + elapsedTime + " ms]";

            resumeJsErrorChecker();
            return elapsedTime;
        }
        finally
        {
            log(logMessage);
        }
    }

    // Get the container id of the current page
    public String getContainerId()
    {
        return (String)executeScript("return LABKEY.container.id;");
    }

    public String getContainerId(String url)
    {
        pushLocation();
        beginAt(url);
        String containerId = getContainerId();
        popLocation();
        return containerId;
    }

    /**
     * @deprecated Use {@link #getAlert())}
     */
    public String getConfirmationAndWait()
    {
        throw new UnsupportedOperationException("Method deprecated, update test");
    }

    /**
     * @deprecated Use {@link #assertAlert(String)}
     */
    @Deprecated
    public void assertConfirmation(String msg)
    {
        throw new UnsupportedOperationException("Method deprecated, update test");
    }

    public void assertAlert(String msg)
    {
        Alert alert = getDriver().switchTo().alert();
        assertEquals(msg, alert.getText());
        alert.accept();
    }

    public void assertAlertContains(String partialMessage)
    {
        Alert alert = getDriver().switchTo().alert();
        assertTrue(alert.getText().contains(partialMessage));
        alert.accept();
    }

    public void dismissAlerts()
    {
        while (isAlertPresent()){
            Alert alert = getDriver().switchTo().alert();
            log("Found unexpected alert: " + alert.getText());
            alert.accept();
        }
    }

	public boolean isAlertPresent()
	{
        try {
            getDriver().switchTo().alert();
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
        Alert alert = getDriver().switchTo().alert();
        String text = alert.getText();
        alert.accept();
        return text;
	}

    public String cancelAlert()
    {
        Alert alert = getDriver().switchTo().alert();
        String text = alert.getText();
        alert.dismiss();
        return text;
    }

    public void assertExtMsgBox(String title, String text)
    {
        String actual = _extHelper.getExtMsgBoxText(title);
        assertTrue("Expected Ext.Msg box text '" + text + "', actual '" + actual + "'", actual.contains(text));
    }

    // TODO: Enable once WebDriver conversion is complete
//    public enum SeleniumEvent
//    {blur,change,mousedown,mouseup,click,reset,select,submit,abort,error,load,mouseout,mouseover,unload,keyup}

    /**
     * Create and fire a JavaScript UIEvent
     * @param l event target
     * @param event event
     */
    public void fireEvent(Locator l, SeleniumEvent event)
    {
        fireEvent(l.findElement(getDriver()), event);
    }

    /**
     * Create and fire a JavaScript UIEvent
     * @param el event target
     * @param event event
     */
    public void fireEvent(WebElement el, SeleniumEvent event)
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
                        "element.dispatchEvent(myEvent);", el, event.toString());
    }

    @LogMethod
    public void startCreateGlobalPermissionsGroup(@LoggedParam String groupName, boolean failIfAlreadyExists)
    {
        goToHome();
        goToSiteGroups();
        if(isElementPresent(Locator.tagWithText("div", groupName)))
        {
            if(failIfAlreadyExists)
                throw new IllegalArgumentException("Group already exists: " + groupName);
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

    @LogMethod
    public void createGlobalPermissionsGroup(@LoggedParam String groupName, boolean failIfAlreadyExists, @LoggedParam String... users )
    {
        startCreateGlobalPermissionsGroup(groupName, failIfAlreadyExists);
        StringBuilder namesList = new StringBuilder();
        for(String member : users)
        {
            namesList.append(member).append("\n");
        }

        log("Adding\n" + namesList.toString() + " to group " + groupName + "...");
        waitAndClickAndWait(Locator.tagContainingText("a","manage group"));
        waitForElement(Locator.name("names"));
        setFormElement(Locator.name("names"), namesList.toString());
        uncheckCheckbox(Locator.name("sendEmail"));
        clickButton("Update Group Membership");
    }

    public void createPermissionsGroup(String groupName)
    {
        log("Creating permissions group " + groupName);
        if (!isElementPresent(Locator.permissionRendered()))
            enterPermissionsUI();
        _ext4Helper.clickTabContainingText("Project Groups");
        setFormElement(Locator.xpath("//input[contains(@name, 'projectgroupsname')]"), groupName);
        clickButton("Create New Group", 0);
        _extHelper.waitForExtDialog(groupName + " Information");
        assertTextPresent("Group " + groupName);
        click(Locator.ext4Button("Done"));
        waitForElement(Locator.css(".groupPicker .x4-grid-cell-inner").withText(groupName), WAIT_FOR_JAVASCRIPT);
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
        assertFalse("Unable to locate group: \"" + groupName + "\"", idx < 0);
        ref.eval("getSelectionModel().select(" + idx + ")");
    }

    public void clickManageGroup(String groupName)
    {
        openGroupPermissionsDisplay(groupName);
        waitAndClickAndWait(Locator.tagContainingText("a","manage group"));
        waitForElement(Locator.name("names"));
    }

    public void clickManageSiteGroup(String groupName)
    {
        _ext4Helper.clickTabContainingText("Site Groups");
        // warning Adminstrators can apper multiple times
        List<Ext4CmpRefWD> refs = _ext4Helper.componentQuery("grid", Ext4CmpRefWD.class);
        Ext4CmpRefWD ref = refs.get(0);
        Long idx = (Long)ref.getEval("getStore().find(\"name\", \"" + groupName + "\")");
        assertFalse("Unable to locate group: \"" + groupName + "\"", idx < 0);
        ref.eval("getSelectionModel().select(" + idx + ")");
        waitAndClickAndWait(Locator.tagContainingText("a","manage group"));
        waitForElement(Locator.name("names"));
    }

    @LogMethod(quiet = true)
    public void dragGroupToRole(@LoggedParam String group, @LoggedParam String srcRole, @LoggedParam String destRole)
    {
        Actions builder = new Actions(getDriver());
        builder
            .clickAndHold(Locator.permissionButton(group, srcRole).findElement(getDriver()))
            .moveToElement(Locator.xpath("//div[contains(@class, 'rolepanel')][.//h3[text()='" + destRole + "']]/div/div").findElement(getDriver()))
            .release()
            .build().perform();

        waitForElementToDisappear(Locator.permissionButton(group, srcRole), WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.permissionButton(group, destRole));
    }

    public void createSubFolderFromTemplate(String project, String child, String template, @Nullable String[] objectsToSkip)
    {
        createSubfolder(project, project, child, "Create From Template Folder", template, objectsToSkip, null, false);
    }


    public void createSubfolder(String project, String child, String[] tabsToAdd)
    {
        // create a child of the top-level project folder:
        createSubfolder(project, project, child, "None", tabsToAdd);
    }


    public void createSubfolder(String project, String child, String folderType, String[] tabsToAdd)
    {
        // create a child of the top-level project folder:
        createSubfolder(project, project, child, folderType, tabsToAdd);
    }


    public void createSubfolder(String project, String parent, String child, String folderType, @Nullable String[] tabsToAdd)
    {
        createSubfolder(project, parent, child, folderType, tabsToAdd, false);
    }

    private  void startCreateFolder(String project, String parent, String child)
    {
        clickProject(project);
        if (!parent.equals(project))
        {
            clickFolder(parent);
        }
        hoverFolderBar();
        if (isElementPresent(Locator.id("folderBar_menu").append(Locator.linkWithText(child))))
            throw new IllegalArgumentException("Folder: " + child + " already exists in project: " + project);
        log("Creating subfolder " + child + " under " + parent);
        clickAndWait(Locator.xpath("//a[@title='New Subfolder']"));
        waitForElement(Locator.name("name"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("name"), child);
    }

    public void createSubfolder(String project, String parent, String child, @Nullable String folderType, @Nullable String[] tabsToAdd, boolean inheritPermissions)
    {
        createSubfolder(project, parent, child, folderType, null, tabsToAdd, inheritPermissions);
    }

    public void createSubfolder(String project, String parent, String child, String folderType, @Nullable String templateFolder, String[] tabsToAdd, boolean inheritPermissions)
    {
        createSubfolder(project, parent, child, folderType, templateFolder, null, tabsToAdd, inheritPermissions);
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
    public void createSubfolder(String project, String parent, String child, @Nullable String folderType, String templateFolder, @Nullable String[] templatePartsToUncheck, @Nullable String[] tabsToAdd, boolean inheritPermissions)
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
                _ext4Helper.selectComboBoxItem(Locator.xpath("//div").withClass("labkey-wizard-header").withText("Choose Template Folder:").append("/following-sibling::table[contains(@id, 'combobox')]"), templateFolder);
                _ext4Helper.checkCheckbox("Include Subfolders");
                if (templatePartsToUncheck != null)
                {
                    for(String part : templatePartsToUncheck)
                    {
                        click(Locator.xpath("//td[label[text()='" +  part + "']]/input"));
                    }
                }
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
        waitForElement(Locator.id("folderBar").withText(project));

        //unless we need addtional tabs, we end here.
        if (null == tabsToAdd || tabsToAdd.length == 0)
            return;


        if (null != folderType && !folderType.equals("None")) // Added in the wizard for custom folders
        {
            goToFolderManagement();
            clickAndWait(Locator.linkWithText("Folder Type"));

            for (String tabname : tabsToAdd)
                checkCheckbox(Locator.checkboxByTitle(tabname));

            submit();
            if ("None".equals(folderType))
            {
                for (String tabname : tabsToAdd)
                    assertElementPresent(Locator.folderTab(tabname));
            }

            // verify that there's a link to our new folder:
            assertElementPresent(Locator.linkWithText(child));
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
    public void deleteFolder(String project, @LoggedParam String folderName)
    {
        log("Deleting folder " + folderName + " under project " + project);
        clickProject(project);
        clickFolder(folderName);
        ensureAdminMode();
        goToFolderManagement();
        waitForElement(Ext4HelperWD.Locators.folderManagementTreeNode(folderName));
        clickButton("Delete");
        // confirm delete subfolders if present
        if(isTextPresent("This folder has subfolders."))
            clickButton("Delete All Folders");
        // confirm delete:
        clickButton("Delete");
        // verify that we're not on an error page with a check for a project link:
        assertElementPresent(Locator.currentProject().withText(project));
        hoverFolderBar();
        assertElementNotPresent(Locator.linkWithText(folderName));
    }

    @LogMethod
    public void renameFolder(String project, @LoggedParam String folderName, @LoggedParam String newFolderName, boolean createAlias)
    {
        log("Renaming folder " + folderName + " under project " + project + " -> " + newFolderName);
        clickProject(project);
        clickFolder(folderName);
        ensureAdminMode();
        goToFolderManagement();
        waitForElement(Ext4HelperWD.Locators.folderManagementTreeNode(folderName).notHidden());
        clickButton("Rename");
        setFormElement(Locator.name("name"), newFolderName);
        if (createAlias)
            checkCheckbox(Locator.name("addAlias"));
        else
            uncheckCheckbox(Locator.name("addAlias"));
        // confirm rename:
        clickButton("Rename");
        _createdFolders.remove(new WebTestHelper.FolderIdentifier(project, folderName));
        _createdFolders.add(new WebTestHelper.FolderIdentifier(project, newFolderName));
        assertElementPresent(Locator.currentProject().withText(project));
        hoverFolderBar();
        waitForElement(Locator.linkWithText(newFolderName));
        assertElementNotPresent(Locator.linkWithText(folderName));
    }

    @LogMethod
    public void moveFolder(String projectName, String folderName, String newParent, boolean createAlias)
    {
        log("Moving folder [" + folderName + "] under project [" + projectName + "] to [" + newParent + "]");
        clickProject(projectName);
        clickFolder(folderName);
        ensureAdminMode();
        goToFolderManagement();
        waitForElement(Ext4HelperWD.Locators.folderManagementTreeNode(folderName));
        clickButton("Move");
        if (createAlias)
            checkCheckbox(Locator.name("addAlias"));
        else
            uncheckCheckbox(Locator.name("addAlias"));
        // Select Target
        waitForElement(Locator.permissionsTreeNode(newParent), 10000);
        sleep(1000); // TODO: what is the right way to wait for the tree expanding animation to complete?
        selectFolderTreeItem(newParent);
        // move:
        clickButton("Confirm Move");

        // verify that we're not on an error page with a check for folder link:
        assertElementPresent(Locator.currentProject().withText(projectName));
        hoverFolderBar();
        waitForElement(Locator.xpath("//li").withClass("clbl").withPredicate(Locator.xpath("a").withText(newParent)).append("/ul/li/a").withText(folderName));
        String newProject = getText(Locator.currentProject());
        _createdFolders.remove(new WebTestHelper.FolderIdentifier(projectName, folderName));
        _createdFolders.add(new WebTestHelper.FolderIdentifier(newProject, folderName));
    }

    public void hoverProjectBar()
    {
        waitForElement(Locator.id("projectBar"));
        waitForHoverNavigationReady();
        executeScript("HoverNavigation._project.show();"); // mouseOver doesn't work on old Firefox
        waitForElement(Locator.css("#projectBar_menu .project-nav"));
    }

    public void clickProject(String project)
    {
        clickProject(project, true);
    }

    public void clickProject(String project, boolean assertDestination)
    {
        hoverProjectBar();
        WebElement projectLink = Locator.linkWithText(project).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        clickAt(projectLink, 1, 1, WAIT_FOR_PAGE); // Don't click hidden portion of long links
        if (assertDestination)
            waitForElement(Locator.id("folderBar").withText(project));
    }

    public void hoverFolderBar()
    {
        waitForElement(Locator.id("folderBar"));
        waitForFolderNavigationReady();
        executeScript("HoverNavigation._folder.show();"); // mouseOver doesn't work on old Firefox
        waitForElement(Locator.css("#folderBar_menu .folder-nav"));
    }

    public void clickFolder(String folder)
    {
        hoverFolderBar();
        expandFolderTree(folder);
        waitAndClickAndWait(Locator.linkWithText(folder));
    }

    public void waitForFolderNavigationReady()
    {
        waitForHoverNavigationReady();
        waitFor(new Checker(){
            @Override
            public boolean check()
            {
                return (Boolean)executeScript("if (HoverNavigation._folder.webPartName = 'foldernav') return true; else return false;");
            }
        }, "HoverNavigation._folder not ready", WAIT_FOR_JAVASCRIPT);
    }

    public void waitForHoverNavigationReady()
    {
        waitFor(new Checker(){
            @Override
            public boolean check()
            {
                return (Boolean)executeScript("if (HoverNavigation) return true; else return false;");
            }
        }, "HoverNavigation not ready", WAIT_FOR_JAVASCRIPT);
    }

    /**
     * Expand folder tree nodes to expose all folders with the given name
     * Expects folder menu to be open
     * @param folder Folder label
     */
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

    /**
     * Delete specified project during test
     * @param project Project display name
     * @param failIfFail if false, silently ignore any failures (if the project doesn't exist, for example)
     */
    public void deleteProject(String project, boolean failIfFail) throws TestTimeoutException
    {
        _containerHelper.deleteProject(project, failIfFail, 120000); // Wait 2 minutes for project deletion
    }

    @LogMethod (quiet = true)
    public void enableEmailRecorder()
    {
        try {
            getHttpGetResponse(WebTestHelper.getBaseURL() + "/dumbster/setRecordEmail.view?record=true", PasswordUtil.getUsername(), PasswordUtil.getPassword());}
        catch (IOException | HttpException e) {
            throw new RuntimeException("Failed to enable email recorder", e);}
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.PortalHelper#addWebPart(String)}
     */
    @Deprecated public void addWebPart(String webPartName)
    {
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart(webPartName);
    }

    public boolean isTitleEqual(String match)
    {
        return match.equals(getDriver().getTitle());
    }

    public void assertTitleEquals(String match)
    {
        assertEquals("Wrong page title", match, getDriver().getTitle());
    }

    public void assertTitleContains(String match)
    {
        String title = getDriver().getTitle();
        assertTrue("Page title: '"+title+"' doesn't contain '"+match+"'", title.contains(match));
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
        List<String> missingTexts = new ArrayList<>();
        if(texts==null || texts.length == 0)
            return missingTexts;

        String source = getHtmlSource();

        for (String text : texts)
        {
            String escapedText = text
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
            if (!source.contains(escapedText))
                missingTexts.add(text);
        }
        return missingTexts;
    }

    public List<String> getMissingTextsCaseInsensitive(String... texts)
    {
        List<String> missingTexts = new ArrayList<>();
        if(texts==null || texts.length == 0)
            return missingTexts;

        String source = getHtmlSource().toLowerCase();

        for (String text : texts)
        {
            String escapedText = text
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
            if (!source.contains(escapedText.toLowerCase()))
                missingTexts.add(text);
        }
        return missingTexts;
    }

    public String getText(Locator elementLocator)
    {
        WebElement el = elementLocator.findElement(getDriver());
        return el.getText();
    }

    /**
     * Verifies that all the strings are present in the page html source
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
            fail(failMsg);
        }
    }

    /**
     * Verifies that all the strings are present in the page html source, disregards casing discrepancies
     */
    public void assertTextPresentCaseInsensitive(String... texts)
    {
        if(texts==null)
            return;

        List<String> missingTexts = getMissingTextsCaseInsensitive(texts);

        if (missingTexts.size() > 0)
        {
            String failMsg = (missingTexts.size() == 1 ? "Text '" : "Texts ['") + missingTexts.get(0) + "'";
            for (int i = 1; i < missingTexts.size(); i++)
            {
                failMsg += ", '" + missingTexts.get(i) + "'";
            }
            failMsg += missingTexts.size() == 1 ? " was not present" : "] were not present";
            fail(failMsg);
        }
    }

    /**
     * Verifies that one of the strings is present in the page html source
     * @param texts
     */
    public void assertOneOfTheseTextsPresent(String... texts)
    {
        if(null==texts)
            return;

        String source = getHtmlSource();
        String targets = "";

        for (String text : texts)
        {
            targets.concat(text + ", ");
            String escapedText = text
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
            if(source.contains(escapedText))
            {
                return;
            }
        }
        fail("Did not find any of the following values on page " + targets);
    }

    /**
     * Verifies that all the strings are present in the page html source
     */
    public void assertTextPresent(List<String> texts)
    {
        String[] textsArray = {};
        textsArray = texts.toArray(textsArray);
        assertTextPresent(textsArray);
    }

    public void assertTextPresentCaseInsensitive(List<String> texts)
    {
        String[] textsArray = {};
        textsArray = texts.toArray(textsArray);
        assertTextPresentCaseInsensitive(textsArray);
    }

    //takes the arguments used to set a filter and transforms them into the description in the grid view
    //then verifies that this description is present
    public void assertFilterTextPresent(String column, String type, String value)
    {
        String desc = type + value;
        if(type.contains("Equals One Of"))
        {
            desc = column + " IS ONE OF (" + value.replace(";", ", ") + "))";
        }
        else if(type.contains("Contains One Of"))
        {
            desc = column + " CONTAINS ONE OF (" + value.replace(";", ", ") + "))";
        }
        else if(type.contains("Does Not Equal Any Of"))
        {
            desc = column + " IS NOT ANY OF (" + value.replace(";", ", ") + "))";
        }
        else if(type.contains("Does Not Contain Any Of"))
        {
            desc = column + " DOES NOT CONTAIN ANY OF (" + value.replace(";", ", ") + "))";
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
            assertFalse("Text '" + text + "' was present", isTextPresent(text));
        }
    }

    public String getTextInTable(String dataRegion, int row, int column)
    {
        String id = Locator.xq(dataRegion);
        return getDriver().findElement(By.xpath("//table[@id=" + id + "]/tbody/tr[" + row + "]/td[" + column + "]")).getText();
    }

    public void assertTextAtPlaceInTable(String textToCheck, String dataRegion, int row, int column)
    {
       assertEquals(textToCheck+" is not at that place in the table", textToCheck, getTextInTable(dataRegion, row, column));
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
     * @return null = yes, present in this order
     * otherwise returns out of order string and explanation of error
     */
    public String isPresentInThisOrder(Object... text)
    {
        String source = getBodyText();
        int previousIndex = -1;
        String previousString = null;

        for (Object o : text)
        {
            String s = o.toString();
            int index = source.indexOf(s);

            if(index == -1)
                return s + " not found";
            if(index <= previousIndex)
                return s + " occured out of order; came before " + previousString;
            previousIndex = index;
            previousString = s;
        }
        return null;
    }
    // Searches only the displayed text in the body of the page, not the HTML source.
    public void assertTextPresentInThisOrder(Object... text)
    {
        String success = isPresentInThisOrder(text);
        assertTrue(success, success==null);
    }

    public void assertTextBefore(String text1, String text2)
    {
        assertTextPresentInThisOrder(text1, text2);
    }

    /**
     * @deprecated Wait for specific elements on the the target page or Use {@link #prepForPageLoad()} and {@link #newWaitForPageToLoad(int)}
     * @param millis milliseconds to wait before timing out
     */
    @Deprecated public void waitForPageToLoad(int millis)
    {
        throw new UnsupportedOperationException("selenium.waitForPageToLoad is deprecated. Use BaseWebDriverTest.prepForPageLoad and BaseWebDriverTest.newWaitForPageToLoad");
    }

    /**
     * @deprecated Wait for specific elements on the the target page or Use {@link #prepForPageLoad()} and {@link #newWaitForPageToLoad(int)}
     */
    @Deprecated public void waitForPageToLoad()
    {
        throw new UnsupportedOperationException("selenium.waitForPageToLoad is deprecated. Use BaseWebDriverTest.prepForPageLoad and BaseWebDriverTest.newWaitForPageToLoad");
    }

    private Boolean _preppedForPageLoad = false;
    public void prepForPageLoad()
    {
        executeScript("window.preppedForPageLoadMarker = true;");
        _preppedForPageLoad = true;
    }

    public void newWaitForPageToLoad(int millis)
    {
        if (!_preppedForPageLoad) throw new IllegalStateException("Please call prepForPageLoad() before performing the action that would trigger the expected page load.");
        _testTimeout = true;
        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                // Wait for marker to disappear
                return (Boolean)executeScript("try {if(window.preppedForPageLoadMarker) return false; else return true;}" +
                                              "catch(e) {return false;}");
            }
        }, "Page failed to load", millis);
        _testTimeout = false;
        waitForExtOnReady();
        _preppedForPageLoad = false;
    }

    public void waitForExtOnReady()
    {
        try
        {
            ((JavascriptExecutor) getDriver()).executeAsyncScript(
                "var callback = arguments[arguments.length - 1];" +
                "if(window['Ext4'])" +
                "   Ext4.onReady(callback);" +
                "else if(window['Ext'])" +
                "   Ext.onReady(callback);" +
                "else" +
                "   callback();");
        }
        catch (TimeoutException to)
        {
            throw new RuntimeException("Page failed to load", to);
        }
    }

    public void newWaitForPageToLoad()
    {
        newWaitForPageToLoad(defaultWaitForPage);
    }

    public void waitForExtReady()
    {
        waitForElement(Locator.id("seleniumExtReady"), defaultWaitForPage);
    }

    public boolean doesElementAppear(Checker checker, int wait)
    {
        Long startTime = System.currentTimeMillis();
        do
        {
            try
            {
                if( checker.check() )
                    return true;
            }
            catch (Exception ignore) {} // Checker exceptions count as a false check
            sleep(100);
        } while ((System.currentTimeMillis() - startTime) < wait);
        try
        {
            if (!checker.check())
            {
                _testTimeout = true;
                return false;
            }
        }
        catch (Exception ignore){}
        return false;
    }

    public interface ElementChecker
    {
        public boolean check(WebElement el);
    }

    public List<WebElement> filterElements(ElementChecker checker, List<WebElement> elements)
    {
        List<WebElement> filteredElements = new ArrayList<>();
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
            fail(failMessage + " ["+wait+"ms]");
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
        shortWait().until(LabKeyExpectedConditions.dataRegionPanelIsExpanded(null));
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
            click(Locator.xpath("//tr[td[contains(text()," +  Locator.xq(type) + ")]]/td/input"));
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
        clickAndWait(Locator.linkWithText("Export"));
        checkRadioButton(Locator.radioButtonByName("location").index(1));
        clickButton("Export");
    }

    protected File exportFolderToBrowserAsZip()
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Export"));
        checkRadioButton(Locator.radioButtonByName("location").index(2));

        return clickAndWaitForDownload(Locator.extButton("Export"));
    }

    public File clickAndWaitForDownload(Locator elementToClick)
    {
        return clickAndWaitForDownload(elementToClick, 1)[0];
    }

    public File[] clickAndWaitForDownload(Locator elementToClick, final int expectedFileCount)
    {
        final long downloadTime = System.currentTimeMillis();
        click(elementToClick);

        final File downloadDir = getDownloadDir();
        final FileFilter newFiles = new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                return file.lastModified() > downloadTime;
            }
        };

        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                return downloadDir.listFiles(newFiles).length >= expectedFileCount;
            }
        }, "File(s) did not appear in download dir", WAIT_FOR_PAGE);

        return downloadDir.listFiles(newFiles);
    }

    @Override
    public void setModuleProperties(List<ModulePropertyValue> values)
    {
        goToFolderManagement();
        log("setting module properties");
        clickAndWait(Locator.linkWithText("Module Properties"));
        waitForText("Save Changes");
        waitForText("Property:");  //proxy for the panel actually loading

        boolean changed = false;
        for (ModulePropertyValue value : values)
        {
            log("setting property: " + value.getPropertyName() + " for container: " + value.getContainerPath() + " to value: " + value.getValue());
            Map<String, String> map = new HashMap<>();
            map.put("moduleName", value.getModuleName());
            map.put("containerPath", value.getContainerPath());
            map.put("propName", value.getPropertyName());
            waitForText(value.getPropertyName()); //wait for the property name to appear
            String query = ComponentQuery.fromAttributes("field", map);
            Ext4FieldRefWD ref = _ext4Helper.queryOne(query, Ext4FieldRefWD.class);
            String val = (String)ref.getValue();
            if((StringUtils.isEmpty(val) != StringUtils.isEmpty(value.getValue())) || !val.equals(value.getValue()))
            {
                changed = true;
                ref.setValue(value.getValue());
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
     * @return did element appear?
     */
    public boolean waitForElement(final Locator locator, int elementTimeout, boolean failIfNotFound)
    {
        if (failIfNotFound)
        {
            locator.waitForElement(getDriver(), elementTimeout);
        }
        else
        {
            try
            {
                locator.waitForElement(getDriver(), elementTimeout);
            }
            catch(Exception e)
            {
                /*ignore*/
                return false;
            }
        }
        return true;
    }

    public void waitForElementWithRefresh(Locator loc, int wait)
    {
        long startTime = System.currentTimeMillis();

        do
        {
            if(waitForElement(loc, 1000, false))
                return;
            refresh();
        }while(System.currentTimeMillis() - startTime < wait);

        waitForElement(loc, 1000);
    }

    public void waitForElement(final Locator locator, int wait)
    {
        waitForElement(locator, wait, true);
    }

    public void waitForElementToDisappear(final Locator locator)
    {
        waitForElementToDisappear(locator, WAIT_FOR_JAVASCRIPT);
    }

    public void waitForElementToDisappear(final Locator locator, int wait)
    {
        locator.waitForElementToDisappear(getDriver(), wait);
    }

    public void waitForTextToDisappear(final String text)
    {
        waitForTextToDisappear(text, WAIT_FOR_JAVASCRIPT);
    }

    public void waitForTextToDisappear(final String text, int wait)
    {
        String failMessage = "Text: " + text + " was still present after [" + wait + "ms]";
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
        startTimer();
        do
        {
            if(isTextPresent(text))
                return;
            else
                sleep(1000);
            refresh();
        }while(elapsedMilliseconds() < wait);
        assertTrue(text + " did not appear [" + wait + "ms]", isTextPresent(text));
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
                return countText(text) == count;
            }
        }, failMessage, wait);
    }

    /**
     * @deprecated Use {@link #clickButton(String)}
     */
    @Deprecated public void submit()
    {
        WebElement form = getDriver().findElement(By.xpath("//td[@id='bodypanel']//form[1]"));
        submit(form);
    }

    /**
     * @deprecated Use {@link #clickButton(String)}
     */
    @Deprecated public void submit(Locator formLocator)
    {
        WebElement form = formLocator.findElement(getDriver());
        submit(form);
    }

    /**
     * @deprecated Use {@link #clickButton(String)}
     */
    @Deprecated public void submit(WebElement form)
    {
        prepForPageLoad();
        executeScript("arguments[0].submit()", form);
        newWaitForPageToLoad();
    }

    /**
     * @deprecated Use {@link #clickButton(String)}
     */
    @Deprecated public void submit(String buttonName)
    {
        Locator l = findButton(buttonName);

        assertTrue("Button with name '" + buttonName + "' not found", null != l);

        clickAndWait(l);
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
        return loc.findElements(getDriver()).size() > 0;
    }

    public void assertElementPresent(Locator loc)
    {
        assertTrue("Element is not present: " + loc.getLoggableDescription(), isElementPresent(loc));
    }

    /**
     * Obscuring super.assertElementPresent(XpathLocator, int)
     * TODO: remove once WebDriver migration is complete
     */
    public void assertElementPresent(Locator.XPathLocator loc, int amount)
    {
        assertElementPresent((Locator) loc, amount);
    }

    public void assertElementPresent(Locator loc, int amount)
    {
        assertEquals("Element '" + loc + "' is not present " + amount + " times", amount, getElementCount(loc));
    }

    public void assertElementContains(Locator loc, String text)
    {
        String elemText = loc.findElement(getDriver()).getText();
        if(elemText == null)
            fail("The element at location " + loc.toString() + " contains no text! Expected '" + text + "'.");
        if(!elemText.contains(text))
            fail("The element at location '" + loc.toString() + "' contains '" + elemText + "'; expected '" + text + "'.");
    }

    public boolean elementContains(Locator loc, String text)
    {
        String elemText = loc.findElement(getDriver()).getText();
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
        return loc.findElement(getDriver()).getAttribute("value");
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
        assertEquals("Form element '" + loc + "' was not equal to '" + value + "'", value, getFormElement(loc));
    }

    public void assertFormElementNotEquals(Locator loc, String value)
    {
        assertNotSame("Form element '" + loc + "' was equal to '" + value + "'", value, getFormElement(loc));
    }

    public void assertOptionEquals(Locator loc, String value)
    {
        assertEquals("Option '" + loc + "' was not equal '" + value + "'", value, getSelectedOptionText(loc));
    }

    public String getSelectedOptionText(Locator loc)
    {
        Select select = new Select(loc.findElement(getDriver()));
        return select.getFirstSelectedOption().getText();
    }

    public String getSelectedOptionValue(Locator loc)
    {
        Select select = new Select(loc.findElement(getDriver()));
        return select.getFirstSelectedOption().getAttribute("value");
    }

    public List<String> getSelectedOptionValues(Locator loc)
    {
        Select select = new Select(loc.findElement(getDriver()));
        List<WebElement> selectedOptions =  select.getAllSelectedOptions();
        List<String> values = new ArrayList(selectedOptions.size());
        for (WebElement selectedOption : selectedOptions)
            values.add(selectedOption.getAttribute("value"));
        return values;
    }

    public void assertElementNotPresent(String errorMsg, Locator loc)
    {
        assertFalse(errorMsg, isElementPresent(loc));
    }

    public void assertElementNotPresent(Locator loc)
    {
        assertElementNotPresent("Element was present in page: " + loc, loc);
    }

    public void assertElementNotVisible(Locator loc)
    {
        assertFalse("Element was visible in page: " + loc, loc.findElement(getDriver()).isDisplayed());
    }

    public void assertElementVisible(Locator loc)
    {
        assertTrue("Element was not visible: " + loc, loc.findElement(getDriver()).isDisplayed());
    }

    /**
     * @deprecated Use {@link #isElementPresent(Locator)}
     */
    @Deprecated public boolean isLinkPresentWithText(String text)
    {
        log("Checking for link with exact text '" + text + "'");
        return isElementPresent(Locator.linkWithText(text));
    }

    /**
     * @deprecated Use {@link #isElementPresent(Locator)}
     */
    @Deprecated public boolean isLinkPresentWithTextCount(String text, int count)
    {
        log("Checking for " + count + " links with exact text '" + text + "'");
        return countLinksWithText(text) == count;
    }

    /**
     * @deprecated Use {@link #isElementPresent(Locator)}
     */
    @Deprecated public boolean isLinkPresentWithText(String text, int index)
    {
        return countLinksWithText(text) > index;
    }

    /**
     * @deprecated Use {@link #isElementPresent(Locator)}
     */
    @Deprecated public boolean isLinkPresentContainingText(String text)
    {
        log("Checking for link containing text '" + text + "'");
        return isElementPresent(Locator.linkContainingText(text));
    }

    /**
     * @deprecated Use {@link #assertElementPresent(Locator)}
     */
    @Deprecated public void assertLinkPresentContainingText(String text)
    {
        assertTrue("Could not find link containing text '" + text + "'", isLinkPresentContainingText(text));
    }

    /**
     * @deprecated Use {@link #assertElementPresent(Locator)}
     */
    @Deprecated public void assertLinkPresentWithText(String text)
    {
        assertTrue("Could not find link with text '" + text + "'", isElementPresent(Locator.linkWithText(text)));
    }

    /**
     * @deprecated Use {@link #assertElementNotPresent(Locator)}
     */
    @Deprecated public void assertLinkNotPresentWithText(String text)
    {
        assertFalse("Found a link with text '" + text + "'", isElementPresent(Locator.linkWithText(text)));
    }

    public void assertAtUserUserLacksPermissionPage()
    {
        assertTextPresent(PERMISSION_ERROR);
        assertTitleEquals("401: Error Page -- User does not have permission to perform this operation");
    }

    /**
     * @deprecated Use {@link #assertElementPresent(Locator)}
     */
    @Deprecated public boolean isLinkPresentWithTitle(String title)
    {
        log("Checking for link with exact title '" + title + "'");
        return isElementPresent(Locator.linkWithTitle(title));
    }

    /**
     * @deprecated Use {@link #assertElementPresent(Locator)}
     */
    @Deprecated public void assertLinkPresentWithTitle(String title)
    {
        assertTrue("Could not find link with title '" + title + "'", isLinkPresentWithTitle(title));
    }

    /**
     * @deprecated Use {@link #assertElementPresent(Locator)}
     */
    @Deprecated public void assertLinkNotPresentWithTitle(String title)
    {
        assertFalse("Found a link with title '" + title + "'", isLinkPresentWithTitle(title));
    }

    /**
     * @deprecated Use {@link #getElementCount(Locator)}
     */
    @Deprecated public int countLinksWithText(String text)
    {
        return getElementCount(Locator.linkWithText(text));
    }

    public void assertLinkPresentWithTextCount(String text, int count)
    {
        assertEquals("Link with text '" + text + "' was not present the expected number of times", count, countLinksWithText(text));
    }

    public void assertNavTrail(String... links)
    {
        ///TODO:  Would like this to be more sophisitcated
        assertTextPresentInThisOrder(links);

    }

    public void scrollIntoView(Locator loc)
    {
        scrollIntoView(loc.findElement(getDriver()));
    }

    public void scrollIntoView(WebElement el)
    {
        executeScript("arguments[0].scrollIntoView(true);", el);
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
        throw new UnsupportedOperationException("Method not supported for WebDriver");
    }

    public void clickAt(Locator l, int xCoord, int yCoord)
    {
        clickAt(l, xCoord, yCoord, WAIT_FOR_PAGE);
    }

    public void clickAt(Locator l, int xCoord, int yCoord, int pageTimeout)
    {
        WebElement el = l.waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        clickAt(el, xCoord, yCoord, pageTimeout);
    }

    public void clickAt(WebElement el, int xCoord, int yCoord, int pageTimeout)
    {
        if (pageTimeout > 0)
            prepForPageLoad();

        Actions builder = new Actions(getDriver());
        builder.moveToElement(el, xCoord, yCoord)
                .click()
                .build()
                .perform();

        if (pageTimeout > 0)
            newWaitForPageToLoad(pageTimeout);
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
        el = l.findElement(getDriver());

        clickAndWait(el, pageTimeoutMs);
    }

    public void clickAndWait(WebElement el, int pageTimeoutMs)
    {
        if (pageTimeoutMs > 0)
            prepForPageLoad();

        try
        {
            el.click();
        }
        catch (ElementNotVisibleException tryAgain)
        {
            scrollIntoView(el); // TODO: WebDriver 2.33 sometimes fails to do this automatically in Firefox
            el.click();
        }

        if (pageTimeoutMs > 0)
            newWaitForPageToLoad(pageTimeoutMs);
        else if(pageTimeoutMs==WAIT_FOR_EXT_MASK_TO_APPEAR)
            _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);
        else if(pageTimeoutMs==WAIT_FOR_EXT_MASK_TO_DISSAPEAR)
            _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
    }

    public void doubleClick(Locator l)
    {
        doubleClickAndWait(l, 0);
    }

    public void doubleClickAndWait(Locator l, int millis)
    {
        if (millis > 0)
            prepForPageLoad();
        Actions action = new Actions(getDriver());
        action.doubleClick(l.findElement(getDriver())).perform();
        if (millis > 0)
            newWaitForPageToLoad(millis);

    }

    public void selectFolderTreeItem(String folderName)
    {
        click(Locator.permissionsTreeNode(folderName));
    }

    /**
     * @deprecated Use {@link Actions} or {@link #fireEvent(Locator, org.labkey.test.BaseSeleniumWebTest.SeleniumEvent)}
     */
    @Deprecated public void mouseOut(Locator l)
    {
        throw new UnsupportedOperationException("This method is no longer supported. Refactor test to use WebDriver actions");
    }

    public void mouseOver(Locator l)
    {
        WebElement el = l.findElement(getDriver());

        Actions builder = new Actions(getDriver());
        builder.moveToElement(el).build().perform();
    }

    /**
     * @deprecated Use {@link #click(Locator)}
     */
    @Deprecated public void mouseDown(Locator l)
    {
        click(l);
    }

    public int getElementIndex(Locator.XPathLocator l)
    {
        return getElementCount(l.child("preceding-sibling::*"));
    }

    public void dragAndDrop(Locator from, Locator to)
    {
        dragAndDrop(from, to, Position.top);
    }

    public enum Position
    {top, bottom, middle}

    public void dragAndDrop(Locator from, Locator to, Position pos)
    {
        WebElement fromEl = from.findElement(getDriver());
        WebElement toEl = to.findElement(getDriver());

        int y;
        switch (pos)
        {
            case top:
                y = 1;
                break;
            case bottom:
                y = toEl.getSize().getHeight() - 1;
                break;
            case middle:
                y = toEl.getSize().getHeight() / 2;
                break;
            default:
                throw new IllegalArgumentException("Unexpected position: " + pos.toString());
        }

        Actions builder = new Actions(getDriver());
        builder.clickAndHold(fromEl).moveToElement(toEl, 1, y).release().build().perform();
    }

    public void dragAndDrop(Locator el, int xOffset, int yOffset)
    {
        WebElement fromEl = el.findElement(getDriver());

        Actions builder = new Actions(getDriver());
        builder.clickAndHold(fromEl).moveByOffset(xOffset + 1, yOffset + 1).release().build().perform();
    }

    public void clickTab(String tabname)
    {
        log("Selecting tab " + tabname);
        clickAndWait(Locator.folderTab(tabname));
    }

    public void verifyTabSelected(String caption)
    {
        assertTrue("Tab not selected: " + caption, isElementPresent(Locator.xpath("//li[contains(@class, labkey-tab-active)]/a[text() = '"+caption+"']")));
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

    /**
     * @deprecated Use {@link #getTableCellText(org.labkey.test.Locator.XPathLocator, int, int)}
     */
    @Deprecated
    public String getTableCellText(String tableId, int row, int column)
    {
        return getTableCellText(Locator.xpath("//table[@id="+Locator.xq(tableId)+"]"), row, column);
    }

    public String getTableCellText(Locator.XPathLocator table, int row, int column)
    {
        return getText(table.append("/tbody/tr[" + (row + 1) + "]/*[(name()='TH' or name()='TD' or name()='th' or name()='td') and position() = " + (column + 1) + "]"));
    }

    public Locator getSimpleTableCell(Locator.XPathLocator table, int row, int column)
    {
        return Locator.xpath(table.toXpath() + "/tbody/tr[" + (row + 1) + "]/td[" + (column + 1) + "]");
    }

    /**
     * @deprecated Use {@link DataRegionTable#getDataAsText(int, String)}
     */
    @Deprecated public String getTableCellText(String tableId, int row, String columnTitle)
    {
        return getTableCellText(tableId, row, getColumnIndex(tableId, columnTitle));
    }

    /**
     * @deprecated Use {@link #getTableCellText(org.labkey.test.Locator.XPathLocator, int, int)}
     */
    @Deprecated public boolean isTableCellEqual(String tableName, int row, int column, String value)
    {
        return value.equals(getTableCellText(tableName, row, column));
    }

    /**
     * @deprecated Use {@link DataRegionTable#getDataAsText(int, String)}
     */
    @Deprecated public boolean isTableCellEqual(String tableName, int row, String columnTitle, String value)
    {
        return value.equals(getTableCellText(tableName, row, columnTitle));
    }

    /**
     * @deprecated Use {@link #getTableCellText(org.labkey.test.Locator.XPathLocator, int, int)}
     */
    @Deprecated public boolean areTableCellsEqual(String tableNameA, int rowA, int columnA, String tableNameB, int rowB, int columnB)
    {
        return getTableCellText(tableNameA, rowA, columnA).equals(getTableCellText(tableNameB, rowB, columnB));
    }

    /**
     * @deprecated Use {@link #getTableCellText(org.labkey.test.Locator.XPathLocator, int, int)} and {@link org.junit.Assert#assertEquals(Object, Object)}
     */
    @Deprecated public void assertTableCellTextEquals(String tableName, int row, int column, String value)
    {
        assertEquals(tableName + "." + String.valueOf(row) + "." + String.valueOf(column) + " != \"" + value + "\"", value, getTableCellText(tableName, row, column));
    }

    /**
     * @deprecated Use {@link DataRegionTable#getDataAsText(int, String)} and {@link org.junit.Assert#assertEquals(Object, Object)}
     */
    @Deprecated public void assertTableCellTextEquals(String tableName, int row, String columnTitle, String value)
    {
        assertTableCellTextEquals(tableName, row, getColumnIndex(tableName, columnTitle), value);
    }

    /**
     * @deprecated Use {@link #getTableCellText(org.labkey.test.Locator.XPathLocator, int, int)} and {@link #assertElementContains(Locator, String)}
     */
    @Deprecated public void assertTableCellContains(String tableName, int row, int column, String... strs)
    {
        String cellText = getTableCellText(tableName, row, column);

        for (String str : strs)
        {
            assertTrue(tableName + "." + row + "." + column + " should contain \'" + str + "\' (actual value is " + cellText + ")", cellText.contains(str));
        }
    }

    /**
     * @deprecated Use {@link DataRegionTable#getDataAsText(int, String)} and {@link #assertElementContains(Locator, String)}
     */
    @Deprecated public void assertTableCellContains(String tableName, int row, String columnTitle, String... strs)
    {
        assertTableCellContains(tableName, row, getColumnIndex(tableName, columnTitle), strs);
    }

    /**
     * @deprecated Use {@link #getTableCellText(org.labkey.test.Locator.XPathLocator, int, int)}
     */
    @Deprecated public void assertTableCellNotContains(String tableName, int row, int column, String... strs)
    {
        String cellText = getTableCellText(tableName, row, column);

        for (String str : strs)
        {
            assertFalse(tableName + "." + row + "." + column + " should not contain \'" + str + "\'", cellText.contains(str));
        }
    }

    /**
     * @deprecated Use {@link DataRegionTable#getDataAsText(int, String)}
     */
    @Deprecated public void assertTableCellNotContains(String tableName, int row, String columnTitle, String... strs)
    {
        assertTableCellNotContains(tableName, row, getColumnIndex(tableName, columnTitle), strs);
    }

    /**
     * @deprecated Use {@link #getTableCellText(org.labkey.test.Locator.XPathLocator, int, int)}
     */
    @Deprecated public void assertTableCellsEqual(String tableName, int rowA, int columnA, int rowB, int columnB)
    {
        assertTableCellsEqual(tableName, rowA, columnA, tableName, rowB, columnB);
    }

    /**
     * @deprecated Use {@link DataRegionTable#getDataAsText(int, String)}
     */
    @Deprecated public void assertTableCellsEqual(String tableName, int rowA, String columnTitleA, int rowB, String columnTitleB)
    {
        assertTableCellsEqual(tableName, rowA, getColumnIndex(tableName, columnTitleA), tableName, rowB, getColumnIndex(tableName, columnTitleB));
    }

    /**
     * @deprecated Use {@link DataRegionTable#getDataAsText(int, String)}
     */
    @Deprecated public void assertTableCellsEqual(String tableNameA, int rowA, String columnTitleA, String tableNameB, int rowB, String columnTitleB)
    {
        assertTableCellsEqual(tableNameA, rowA, getColumnIndex(tableNameA, columnTitleA), tableNameB, rowB, getColumnIndex(tableNameB, columnTitleB));
    }

    /**
     * @deprecated Use {@link #getTableCellText(org.labkey.test.Locator.XPathLocator, int, int)}
     */
    @Deprecated public void assertTableCellsEqual(String tableNameA, int rowA, int columnA, String tableNameB, int rowB, int columnB)
    {
        assertTrue("Table cells not equal: " + tableNameA + "." + String.valueOf(rowA) + "." + String.valueOf(columnA) + " & " + tableNameB + "." + String.valueOf(rowB) + "." + String.valueOf(columnB), areTableCellsEqual(tableNameA, rowA, columnA, tableNameB, rowB, columnB));
    }

    /**
     * @deprecated Use {@link DataRegionTable#getColumn(String)}
     */
    @Deprecated public int getColumnIndex(String tableName, String columnTitle)
    {
        int col = Locator.xpath("//table[@id='"+tableName+"']/tbody/tr[contains(@id, 'dataregion_column_header_row') and not(contains(@id, 'spacer'))]/td[./div/.='"+columnTitle+"']/preceding-sibling::*").findElements(getDriver()).size();
        if(col == 0)
            throw new IllegalArgumentException("Column '" + columnTitle + "' not found in table '" + tableName + "'");

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
        DataRegionTable status = new DataRegionTable("StatusFiles", this, true, false);
        return status.getColumnDataAsText("Status");
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
        ArrayList<File> allFiles = new ArrayList<>();
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

        if (isElementPresent(Locator.linkWithText("override")))
        {
            if (inherit)
                clickAndWait(Locator.linkWithText("modify the setting for all folders"));
            else
                clickAndWait(Locator.linkWithText("override"));
        }
        checkRadioButton(Locator.radioButtonById("pipeOptionProjectSpecified"));
        setFormElement(Locator.id("pipeProjectRootPath"), rootPath);

        clickButton("Save");
        log("Finished setting pipeline to: " + rootPath);
    }

    public void setPipelineRootToDefault()
    {
        log("Set pipeline to default based on the site-level root");
        goToModule("Pipeline");
        clickButton("Setup");
        checkRadioButton(Locator.radioButtonById("pipeOptionSiteDefault"));
        clickButton("Save");
        log("Finished setting pipeline to default based on the site-level root");
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
            if ("COMPLETE".equals(statusValue) || "IMPORT FOLDER COMPLETE".equals(statusValue))
                complete++;

        return complete;
    }

    // Returns count of "COMPLETE" and "ERROR"
    public int getFinishedCount(List<String> statusValues)
    {
        int finsihed = 0;
        for (String statusValue : statusValues)
            if ("COMPLETE".equals(statusValue) || "ERROR".equals(statusValue) || "IMPORT FOLDER COMPLETE".equals(statusValue))
                finsihed++;
        return finsihed;
    }

    // Returns the value of all cells in the specified column
    public List<String> getTableColumnValues(String tableName, int column)
    {
        int rowCount = getTableRowCount(tableName);

        List<String> values = new ArrayList<>(rowCount);

        for (int i = 0; i < rowCount; i++)
        {
            try
            {
                values.add(getTableCellText(Locator.id(tableName), i, column));
            }
            catch(Exception ignore) {}
        }

        return values;
    }

    public void showNumberInTable(String shareValue)
    {
        clickButton("Page Size", 0);
        waitForText("100 per page");
        Locator l = Locator.id("Page Size:" + shareValue);
        clickAndWait(l);
    }

    /**get values for all specifed columns for all pages of the table
     * preconditions:  must be on start page of table
     * postconditions:  at start of table
     */
    protected  List<List<String>> getColumnValues(String tableName, String... columnNames)
    {
        boolean moreThanOnePage = isTextPresent("Next");
        if(moreThanOnePage)
        {
            showNumberInTable("All");
        }

        List<List<String>> columns = new ArrayList<>();
        DataRegionTable table = new DataRegionTable(tableName, this);
        for (String columnName : columnNames)
        {
            columns.add(table.getColumnDataAsText(columnName));
        }

        if(moreThanOnePage)
        {
            showNumberInTable("100");
        }
        return columns;
    }

    // Returns the number of rows (both <tr> and <th>) in the specified table
    public int getTableRowCount(String tableName)
    {
        return Locator.xpath("//table[@id=" + Locator.xq(tableName) + "]/thead/tr").findElements(getDriver()).size() +
               Locator.xpath("//table[@id=" + Locator.xq(tableName) + "]/tbody/tr").findElements(getDriver()).size();
    }

    public int getTableColumnCount(String tableId)
    {
        return getElementCount(Locator.xpath("//table[@id=" + Locator.xq(tableId) + "]/colgroup/col"));
    }

    public boolean isButtonPresent(String text)
    {
        return (getButtonLocator(text) != null);
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
            throw new NoSuchElementException("No button found with text \"" + text + "\" at index " + index);
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
        locator = Locator.ext4Button(text).index(index);
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

        // check for Ext 4 button:
        locator = Locator.ext4ButtonContainingText(text);
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
        else
            throw new NoSuchElementException("No button found with text \"" + text + "\"");
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
            throw new NoSuchElementException("No button found with text \"" + text + "\"");
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
    public void waitAndClickAndWait(Locator l)
    {
        waitAndClick(WAIT_FOR_JAVASCRIPT, l, WAIT_FOR_PAGE);
    }

    /**
     *  wait for element, click it, wait for page to load
     */
    public void waitAndClick(int waitFor, Locator l, int waitForPageToLoad)
    {
        WebElement el = l.waitForElement(getDriver(), waitFor);
        clickAndWait(el, waitForPageToLoad);
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

    /**
     * @deprecated Use {@link #setFormElement(Locator, String)}
     */
    @Deprecated public void setText(String elementName, String text)
    {
        setFormElement(Locator.id(elementName), text, elementName.toLowerCase().contains("password"));
    }

    /**
     * @deprecated Use explicit Locator: {@link #setFormElement(Locator, String)}
     */
    @Deprecated public void setFormElement(String name, String text)
    {
        if(getDriver().findElements(By.id(name)).size() > 0)
        {
            log("DEPRECATED: Form element has id: \"" + name + "\". Use Locator.id");
            setFormElement(Locator.id(name), text, false);
        }
        else
        {
            log("DEPRECATED: Form element is named: \"" + name + "\". Use Locator.name");
            setFormElement(Locator.name(name), text, false);
        }
    }

    public void setFormElement(Locator l, String text, boolean suppressValueLogging)
    {
        WebElement el = l.findElement(getDriver());
        setFormElement(el, text);
    }

    public void setFormElement(WebElement el, String text)
    {
        boolean isFileInput = "file".equals(el.getAttribute("type"));
        if (isFileInput)
        {
            log("DEPRECATED: Use File object to set file input");
            setFormElement(el, new File(text));
            return;
        }

        if (StringUtils.isEmpty(text))
        {
            el.clear();
        }
        else if (text.length() < 1000 && !text.contains("\n") && !text.contains("\t"))
        {
            el.clear();
            el.sendKeys(text);
        }
        else
        {
            setFormElementJS(el, text);
        }

        String elementClass = el.getAttribute("class");
        if (elementClass.contains("gwt-TextBox") || elementClass.contains("gwt-TextArea") || elementClass.contains("x-form-text"))
            fireEvent(el, SeleniumEvent.blur); // Make GWT and ExtJS form elements behave better
    }

    /**
     * Set form element directly via JavaScript rather than WebElement.sendKeys
     * @param l Locator for form element
     * @param text text to set
     */
    public void setFormElementJS(Locator l, String text)
    {
        WebElement el = l.findElement(getDriver());
        setFormElementJS(el, text);
    }

    public void setFormElementJS(WebElement el, String text)
    {
        executeScript("arguments[0].value = arguments[1]", el, text);
        fireEvent(el, SeleniumEvent.change);
    }

    /**
     * @deprecated Use {@link #setFormElement(Locator, File)}
     */
    @Deprecated public void setFormElement(String element, File file)
    {
        setFormElement(Locator.name(element), file);
    }

    public void setFormElement(Locator loc, File file)
    {
        WebElement el = loc.findElement(getDriver());
        setFormElement(el, file);
    }

    public void setFormElement(WebElement el, File file)
    {
        String cssString = "";
        if (!el.isDisplayed())
        {
            cssString = el.getAttribute("class");
            log("Remove class so that WebDriver can interact with concealed form element");
            executeScript("arguments[0].setAttribute('class', '');", el);
        }

        el.sendKeys(file.getAbsolutePath());

        if (!cssString.isEmpty())
        {
            executeScript("arguments[0].setAttribute('class', arguments[1]);", el, cssString);
        }
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
        prepForPageLoad();
        _extHelper.clickExtComponent(EscapeUtil.filter(id));
        newWaitForPageToLoad(wait);
    }

    public void setSort(String regionName, String columnName, SortDirection direction, int wait)
    {
        log("Setting sort in " + regionName + " for " + columnName + " to " + direction.toString());
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        waitForElement(header, WAIT_FOR_JAVASCRIPT);
        String id = EscapeUtil.filter(regionName + ":" + columnName + ":" + direction.toString().toLowerCase());
        prepForPageLoad();
        _extHelper.clickExtComponent(EscapeUtil.filter(id));
        newWaitForPageToLoad(wait);
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

    public void setUpFilter(String regionName, String columnName, String filterType, @Nullable String filter)
    {
        setUpFilter(regionName, columnName, filterType, filter, null, null);
    }

    public void setFilterAndWait(String regionName, String columnName, String filterType, String filter, int milliSeconds)
    {
        setUpFilter(regionName, columnName, filterType, filter);
        clickButton("OK", milliSeconds);
    }

    public void setUpFilter(String regionName, String columnName, String filter1Type, @Nullable String filter1, @Nullable String filter2Type, @Nullable String filter2)
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
        _extHelper.clickExtComponent(EscapeUtil.filter(id));
        _extHelper.waitForExtDialog("Show Rows Where " + columnLabel + "...");
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);

        if (isElementPresent(Locator.css("span.x-tab-strip-text").withText("Choose Values")))
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
        String log;
        if (values.length > 0)
        {
            log = "Setting filter in " + regionName + " for " + columnName + " to one of: [";
            for(String v : values)
            {
                log += v + ", ";
            }
            log = log.substring(0, log.length() - 2) + "]";
        }
        else
        {
            log = "Clear filter in " + regionName + " for " + columnName;
        }

        log(log);
        String id = EscapeUtil.filter(regionName + ":" + columnName + ":filter");
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        waitForElement(header, WAIT_FOR_JAVASCRIPT);
        String columnLabel = getText(header);
        _extHelper.clickExtComponent(EscapeUtil.filter(id));
        _extHelper.waitForExtDialog("Show Rows Where " + columnLabel + "...");

        sleep(500);

        // Clear selections.
        assertEquals("Faceted filter tab should be selected.", "Choose Values", getText(Locator.css(".x-tab-strip-active")));
        if(!isElementPresent(Locator.xpath("//div[contains(@class, 'x-grid3-hd-checker-on')]")))
            click(Locator.linkWithText("[All]"));
        click(Locator.linkWithText("[All]"));

        if(values.length > 1)
        {
            for(String v : values)
            {
                click(Locator.xpath(_extHelper.getExtDialogXPath("Show Rows Where " + columnLabel + "...") +
                        "//div[contains(@class,'x-grid3-row') and .//span[text()='" + v + "']]//div[@class='x-grid3-row-checker']"));
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
        if(waitForPageLoad > 0)
            prepForPageLoad();
        _extHelper.clickExtComponent(EscapeUtil.filter(id));
        if(waitForPageLoad > 0)
            newWaitForPageToLoad(waitForPageLoad);
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
        _extHelper.clickExtComponent(EscapeUtil.filter(id));
        clickButton("CLEAR ALL FILTERS");
    }

    final static int MAX_TEXT_LENGTH = 2000;

    public String getPropertyXPath(String propertyHeading)
    {
        return "//td[contains(text(), '" + propertyHeading + "')]/../..";
    }

    /**
     * @deprecated Use {@link #getElementCount(org.labkey.test.Locator)}
     */
    @Deprecated public int getXpathCount(Locator.XPathLocator xpath)
    {
        return getElementCount(xpath);
    }

    public int getElementCount(Locator locator)
    {
        return locator.findElements(getDriver()).size();
    }

    /**
     * From the assay design page, add a field with the given name, label, and type
     */
    public void addRunField(String name, String label, ListHelperWD.ListColumnType type)
    {
        String xpath = ("//input[starts-with(@name, 'ff_name");
        int newFieldIndex = getElementCount(Locator.xpath(xpath + "')]"));
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

    public void setFormElementAndVerify(final Locator element, final String text)
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
    /**
     * @deprecated Use {@link #setFormElementAndVerify(Locator, String)}
     */
    @Deprecated public void setLongTextField(final Locator loc, final String text)
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
     * @deprecated Use {@link ExtHelperWD#clickExtComponent(String)}
     */
    @Deprecated public boolean runMenuItemHandler(String id)
    {
        log("Invoking Ext menu item handler '" + id + "'");
        return _extHelper.clickExtComponent(EscapeUtil.filter(id));
    }

    /**
     * Clicks the labkey menu item and optional submenu labels (for cascading menus)
     * @deprecated Use {@link ExtHelperWD#clickMenuButton(boolean, String, String...)}
     */
    @Deprecated public void clickMenuButton(String menusLabel, String ... subMenusLabels)
    {
        _extHelper.clickMenuButton(true, menusLabel, subMenusLabels);
    }

    /**
     * Clicks the ext menu item and optional submenu labels's (for cascading menus)
     * Does not wait for page load.
     * @deprecated Use {@link ExtHelperWD#clickMenuButton(boolean, String, String...)}
     */
    @Deprecated public void clickMenuButtonAndContinue(String menusLabel, String ... subMenusLabels)
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
        return Locator.xpath("//table[@id=" + id + "]/tbody/tr[contains(@class, 'labkey-row') or contains(@class, 'labkey-alternate-row')]").findElements(getDriver()).size();
    }

    /** Sets selection state for rows of the data region on the current page. */
    public void checkAllOnPage(String dataRegionName)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        WebElement toggle = Locator.xpath("//table[@id=" + id + "]//input[@name='.toggle']").findElement(getDriver());
        checkCheckbox(toggle);
    }

    /** Clears selection state for rows of the data region on the current page. */
    public void uncheckAllOnPage(String dataRegionName)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        WebElement toggle = Locator.xpath("//table[@id=" + id + "]//input[@name='.toggle']").findElement(getDriver());
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
        List<WebElement> selects = Locator.xpath("//table[@id=" + id + "]//input[@name='.select']").findElements(getDriver());
        checkCheckbox(selects.get(index));
    }

    /** Sets selection state for single rows of the data region. */
    public void uncheckDataRegionCheckbox(String dataRegionName, int index)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        List<WebElement> selects = Locator.xpath("//table[@id=" + id + "]//input[@name='.select']").findElements(getDriver());
        uncheckCheckbox(selects.get(index));
    }

    /**
     * @deprecated Use {@link #click(Locator)}
     */
    @Deprecated public void clickCheckbox(String name)
    {
        click(Locator.checkboxByName(name));
    }

    /**
     * @deprecated Use {@link #click(Locator)}
     */
    @Deprecated public void clickRadioButtonById(String id)
    {
        click(Locator.radioButtonById(id));
    }

    /**
     * @deprecated Use {@link #clickAndWait(Locator, int)}
     */
    @Deprecated public void clickRadioButtonById(String id, int millis)
    {
        clickAndWait(Locator.radioButtonById(id), millis);

    }

    /**
     * @deprecated Use {@link #click(Locator)}
     */
    @Deprecated public void clickCheckboxById(String id)
    {
        click(Locator.checkboxById(id));
    }

    /**
     * @deprecated Use {@link #checkCheckbox(Locator)}
     */
    @Deprecated public void checkRadioButton(String name, String value)
    {
        checkCheckbox(Locator.radioButtonByNameAndValue(name, value));
    }

    /**
     * @deprecated Use {@link #checkCheckbox(Locator)}
     */
    @Deprecated public void checkCheckbox(String name, String value)
    {
        checkCheckbox(Locator.checkboxByNameAndValue(name, value));
    }

    /**
     * @deprecated Use {@link #checkCheckbox(Locator)}
     */
    @Deprecated public void checkCheckbox(String name)
    {
        checkCheckbox(Locator.checkboxByName(name));
    }

    /**
     * @deprecated Use {@link #checkCheckbox(Locator)}
     */
    @Deprecated public void checkCheckboxByNameInDataRegion(String name)
    {
        checkCheckbox(Locator.xpath("//a[contains(text(), '" + name + "')]/../..//td/input"));
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
        assertTrue("Checking checkbox failed", isChecked(checkBoxLocator));
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

    /**
     * @deprecated Use {@link #checkRadioButton(Locator)}
     */
    @Deprecated public void checkRadioButton(String name, int index)
    {
        checkCheckbox(Locator.radioButtonByName(name).index(index));
    }

    /**
     * @deprecated Use {@link #assertRadioButtonSelected(Locator)}
     */
    @Deprecated public void assertRadioButtonSelected(String name, String value)
    {
        assertRadioButtonSelected(Locator.radioButtonByNameAndValue(name, value));
    }

    /**
     * @deprecated Use {@link #assertRadioButtonSelected(Locator)}
     */
    @Deprecated public void assertRadioButtonSelected(String name, int index)
    {
        assertRadioButtonSelected(Locator.radioButtonByName(name).index(index));
    }

    public void assertRadioButtonSelected(Locator radioButtonLocator)
    {
        assertTrue("Radio Button is not selected at " + radioButtonLocator.toString(), isChecked(radioButtonLocator));
    }

    /**
     * @deprecated Use {@link #checkCheckbox(Locator)}
     */
    @Deprecated public void checkCheckbox(String name, int index)
    {
        checkCheckbox(Locator.checkboxByName(name).index(index));
    }

    /**
     * @deprecated Use {@link #uncheckCheckbox(Locator)}
     */
    @Deprecated public void uncheckCheckbox(String name)
    {
        uncheckCheckbox(Locator.checkboxByName(name));
    }

    /**
     * @deprecated Use {@link #uncheckCheckbox(Locator)}
     */
    @Deprecated public void uncheckCheckbox(String name, String value)
    {
        uncheckCheckbox(Locator.checkboxByNameAndValue(name, value));
    }

    /**
     * @deprecated Use {@link #uncheckCheckbox(Locator)}
     */
    @Deprecated public void uncheckCheckbox(String name, int index)
    {
        uncheckCheckbox(Locator.checkboxByName(name).index(index));
    }

    public void uncheckCheckbox(Locator checkBoxLocator)
    {
        log("Unchecking checkbox " + checkBoxLocator);
        if (isChecked(checkBoxLocator))
            click(checkBoxLocator);
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
        return checkBoxLocator.findElement(getDriver()).isSelected();
    }

    /**
     * @deprecated Use {@link #selectOptionByValue(Locator, String)}
     */
    @Deprecated public void selectOptionByValue(String selectName, String value)
    {
        selectOptionByValue(Locator.name(selectName), value);
    }

    public void selectOptionByValue(Locator locator, String value)
    {
        WebElement selectElement = locator.findElement(getDriver());
        selectOptionByValue(selectElement, value);
    }

    public void selectOptionByValue(WebElement selectElement, String value)
    {
        Select select = new Select(selectElement);
        select.selectByValue(value);
    }

    /**
     * @deprecated Use {@link #selectOptionByText(Locator, String)}
     */
    @Deprecated public void selectOptionByText(String selectName, String text)
    {
        selectOptionByText(Locator.name(selectName), text);
    }

    public void selectOptionByText(Locator locator, String text)
    {
        WebElement selectElement = locator.findElement(getDriver());
        selectOptionByText(selectElement, text);
    }

    public void selectOptionByText(WebElement selectElement, String value)
    {
        Select select = new Select(selectElement);
        select.selectByVisibleText(value);
    }

    public void addUrlParameter(String parameter)
    {
        String currentURL = getCurrentRelativeURL();
        // Strip off any '#' on the end of the URL
        String suffix = "";
        if (currentURL.contains("#"))
        {
            String[] parts = currentURL.split("#");
            currentURL = parts[0];
            // There might not be anything after the '#'
            suffix = "#" + (parts.length > 1 ? parts[1] : "");
        }
        if (!currentURL.contains(parameter))
            if (currentURL.contains("?"))
                beginAt(currentURL.concat("&" + parameter + suffix));
            else
                beginAt(currentURL.concat("?" + parameter + suffix));
    }

    String toRole(String perm)
    {
        String R = "security.roles.";
        if ("No Permissions".equals(perm))
            return R + "NoPermissionsRole";
        if ("Project Administrator".equals(perm))
            return R + "ProjectAdminRole";
        else if (!perm.contains("."))
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
        _ext4Helper.waitForMaskToDisappear();
    }

    @Deprecated
    //use _securityHelper.setPermissions
    @LogMethod
    public void setPermissions(@LoggedParam String groupName, @LoggedParam String permissionString)
    {
        _setPermissions(groupName, permissionString, "pGroup");
    }

    @Deprecated
    //use _securityHelper.setSiteGroupPermissions
    @LogMethod
    public void setSiteGroupPermissions(@LoggedParam String groupName, @LoggedParam String permissionString)
    {
        _setPermissions(groupName, permissionString, "pSite");
    }

    @LogMethod
    public void setUserPermissions(@LoggedParam String userName, @LoggedParam String permissionString)
    {
        log(new Date().toString());
        _setPermissions(userName, permissionString, "pUser");

        log(new Date().toString());
    }

    @LogMethod
    public void setSiteAdminRoleUserPermissions(@LoggedParam String userName, @LoggedParam String permissionString)
    {
        log(new Date().toString());
        goToSiteAdmins();
        clickAndWait(Locator.linkContainingText("Permissions"));
        _ext4Helper.clickTabContainingText("Permissions");
        _selectPermission(userName, userName, permissionString);
        log(new Date().toString());
    }


    private void _setPermissions(String userOrGroupName, String permissionString, String className)
    {
        String role = toRole(permissionString);
        if ("org.labkey.api.security.roles.NoPermissionsRole".equals(role))
        {
            throw new IllegalArgumentException("Can't set NoPermissionRole; call removePermission()");
        }
        else
        {
            log("Setting permissions for group " + userOrGroupName + " to " + role);

            if (!isElementPresent(Locator.permissionRendered()))
                enterPermissionsUI();
            _ext4Helper.clickTabContainingText("Permissions");

            String group = userOrGroupName;
            if (className.equals("pSite"))
                group = "Site: " + group;
            _selectPermission(userOrGroupName, group, permissionString);
        }
    }

    private void _selectPermission(String userOrGroupName, String group, String permissionString)
    {
        Locator.XPathLocator roleCombo = Locator.xpath("//div[contains(@class, 'rolepanel')][.//h3[text()='" + permissionString + "']]");
        waitForElement(roleCombo);
        _extHelper.selectExt4ComboBoxItem(roleCombo, group, true);
        waitForElement(Locator.permissionButton(userOrGroupName, permissionString));
        String oldId = getAttribute(Locator.permissionButton(userOrGroupName, permissionString), "id");
        savePermissions();
        _ext4Helper.waitForMaskToDisappear();
        waitForElementToDisappear(Locator.id(oldId), WAIT_FOR_JAVASCRIPT); // Elements get new ids after save
        assertPermissionSetting(userOrGroupName, permissionString);
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
            waitForElementToDisappear(close);
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
        clickAndWait(Locator.linkContainingText("manage group"));
        addUserToGroupFromGroupScreen(userName);
    }

    protected void addUserToGroupFromGroupScreen(String userName)
    {
        waitForElement(Locator.name("names"));
        setFormElement(Locator.name("names"), userName );
        uncheckCheckbox(Locator.name("sendEmail"));
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
            clickProject(projectName);
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
        if (isSiteGroup)
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
        _ext4Helper.clickExt4MenuButton(false, Locators.USER_MENU, false, "Impersonate", "User");
        waitForElement(Ext4HelperWD.Locators.window("Impersonate User"));
        _ext4Helper.selectComboBoxItem("User:", fakeUser + " (", true);
        clickAndWait(Ext4HelperWD.ext4WindowButton("Impersonate User", "Impersonate"));
        _impersonationStack.push(fakeUser);

        if (isElementPresent(Locator.navButton("Home")))
        {
            clickAndWait(Locator.navButton("Home"));
        }
    }


    public void stopImpersonating()
    {
        String fakeUser = _impersonationStack.pop();
        assertEquals(displayNameFromEmail(fakeUser), getDisplayName());
        clickUserMenuItem("Stop Impersonating");
        assertSignOutAndMyAccountPresent();
        goToHome();
        assertFalse(displayNameFromEmail(fakeUser).equals(getDisplayName()));
    }

    private HashMap<String, String> usersAndDisplayNames = new HashMap<>();

    protected void setDisplayName(String email, String newDisplayName)
    {
        String previousDisplayName = usersAndDisplayNames.get(email);
        String defaultDisplayName = getDefaultDisplayName(email);
        usersAndDisplayNames.remove(email);

        if (previousDisplayName == null && newDisplayName.equals(defaultDisplayName))
                return;
        else
        {
            if (!newDisplayName.equals(previousDisplayName))
            {
                goToSiteUsers();

                DataRegionTable users = new DataRegionTable("Users", this, true, true);
                int userRow = users.getRow("Email", email);
                assertFalse("No such user: " + email, userRow == -1);
                clickAndWait(users.detailsLink(userRow));

                clickButton("Edit");
                setFormElement(Locator.name("quf_DisplayName"), newDisplayName);
                clickButton("Submit");
            }
        }

        if (!newDisplayName.equals(defaultDisplayName))
            usersAndDisplayNames.put(email, newDisplayName);
    }

    protected void resetDisplayName(String email)
    {
        String defaultDisplayName = getDefaultDisplayName(email);

        setDisplayName(email, defaultDisplayName);
    }

    // assumes there are not collisions in the database causing unique numbers to be appended
    @Override
    protected String displayNameFromEmail(String email)
    {
        if (usersAndDisplayNames.containsKey(email))
            return usersAndDisplayNames.get(email);
        else
            return getDefaultDisplayName(email);
    }

    private String getDefaultDisplayName(String email)
    {
        String display = email.contains("@") ? email.substring(0,email.indexOf('@')) : email;
        display = display.replace('_', ' ');
        display = display.replace('.', ' ');
        return display.trim();
    }

    /**
     * Create a user with the specified permissions for the specified project
     */
    public void createUserWithPermissions(String userName, String projectName, String permissions)
    {
        createUser(userName, null);
        if(projectName==null)
            goToProjectHome();
        else
            clickProject(projectName);
        setUserPermissions(userName, permissions);
    }

    public void createUser(String userName, @Nullable String cloneUserName)
    {
        createUser(userName, cloneUserName, true);
    }

    public void createUser(String userName, @Nullable String cloneUserName, boolean verifySuccess)
    {
        if(cloneUserName == null)
        {
            _userHelper.createUser(userName, verifySuccess);
        }
        else
        {
            throw new IllegalArgumentException("cloneUserName support has been removed"); //not in use, so was not implemented in new user helpers
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
            checkCheckbox(Locator.id("cloneUserCheck"));
            setFormElement(Locator.name("cloneUser"), cloneUserName);
        }
        clickButton("Add Users");

        if (verifySuccess)
            assertTrue("Failed to add user " + userName, isTextPresent(userName + " added as a new user to the system"));
    }

    public void createSiteDeveloper(String userEmail)
    {
        ensureAdminMode();
        goToSiteDevelopers();

        if (!isElementPresent(Locator.xpath("//input[@value='" + userEmail + "']")))
        {
            setFormElement(Locator.name("names"), userEmail);
            uncheckCheckbox(Locator.name("sendEmail"));
            clickButton("Update Group Membership");
        }
    }

    public void deleteGroup(String groupName)
    {
        deleteGroup(groupName, false);
    }

    @LogMethod(quiet = true)
    public void deleteGroup(@LoggedParam String groupName, boolean failIfNotFound)
    {
        log("Attempting to delete group: " + groupName);
        if (selectGroup(groupName, failIfNotFound))
        {
            assertElementPresent(Locator.css(".x4-grid-cell-first").withText(groupName));
            deleteAllUsersFromGroup();
            click(Locator.xpath("//td/a/span[text()='Delete Empty Group']"));
            waitForElementToDisappear(Locator.css(".x4-grid-cell-first").withText(groupName), WAIT_FOR_JAVASCRIPT);
        }
    }

    private void deleteAllUsersFromGroup()
    {
        Locator.XPathLocator l = Locator.xpath("//td/a/span[text()='remove']");

        while(isElementPresent(l))
        {
            int i = getElementCount(l) - 1;
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
        Locator.css(".userinfo td").withText(userName).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
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
            throw new NoSuchElementException("Group not found:" + groupName);

        return false;
    }

    @LogMethod
    public void deleteUsers(boolean failIfNotFound, @LoggedParam String... userEmails)
    {
        int checked = 0;
        List<String> displayNames = new ArrayList<>();
        ensureAdminMode();
        goToSiteUsers();

        if(isElementPresent(Locator.linkWithText("INCLUDE INACTIVE USERS")))
            clickAndWait(Locator.linkWithText("INCLUDE INACTIVE USERS"));

        DataRegionTable usersTable = new DataRegionTable("Users", this, true, true);

        for(String userEmail : userEmails)
        {
            int row = usersTable.getRow("Email", userEmail);

            boolean isPresent = row != -1;

            // If we didn't find the user and we have more than one page, then show all pages and try again
            if (!isPresent && isElementPresent(Locator.linkContainingText("Next")) && isElementPresent(Locator.linkContainingText("Last")))
            {
                clickButton("Page Size", 0);
                clickAndWait(Locator.linkWithText("Show All"));
                row = usersTable.getRow("Email", userEmail);
                isPresent = row != -1;
            }

            if (failIfNotFound)
                assertTrue(userEmail + " was not present", isPresent);

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
        clickProject(projectName);
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
        clickProject(projectName);
        enterPermissionsUI();
        _ext4Helper.clickTabContainingText("Project Groups");
        waitForElement(Locator.css(".groupPicker"), WAIT_FOR_JAVASCRIPT);
        waitAndClick(Locator.xpath("//div[text()='" + groupName + "']"));
        _extHelper.waitForExtDialog(groupName + " Information");
        boolean ret = isElementPresent(Locator.xpath("//table[contains(@class, 'userinfo')]//td[starts-with(text(), '" + email +  "')]"));
        clickButton("Done", 0);
        _extHelper.waitForExtDialogToDisappear(groupName + " Information");
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

    public void saveWikiPage()
    {
        String title = Locator.id("wiki-input-title").findElement(getDriver()).getText();
        if (title.equals("")) title = Locator.id("wiki-input-name").findElement(getDriver()).getText();
        clickButton("Save & Close");
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
                try {ret = URLDecoder.decode(ret, "UTF-8");} catch(UnsupportedEncodingException ignore) {}
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

    protected long elapsedSeconds()
    {
        return (System.currentTimeMillis() - start) / 1000;
    }

    protected long elapsedMilliseconds()
    {
        return System.currentTimeMillis() - start;
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
        if(isElementPresent(Locator.linkWithText("new page")))
            clickAndWait(Locator.linkWithText("new page"));
        else if(isElementPresent(Locator.linkWithText("Create a new wiki page")))
            clickAndWait(Locator.linkWithText("Create a new wiki page"));
        else if(isElementPresent(Locator.linkWithText("add content")))
            clickAndWait(Locator.linkWithText("add content"));
        else if(isTextPresent("Pages"))
        {
            PortalHelper portalHelper = new PortalHelper(this);
            portalHelper.clickWebpartMenuItem("Pages", "New");
        }
        else
            throw new IllegalStateException("Could not find a link on the current page to create a new wiki page." +
                    " Ensure that you navigate to the wiki controller home page or an existing wiki page" +
                    " before calling this method.");

        convertWikiFormat(format);
    }

    //must already be on wiki page
    public void setWikiValuesAndSave(String name, String title, String body)
    {

        setFormElement(Locator.name("name"), name);
        setFormElement(Locator.name("title"), title);
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
        String curFormat = (String)executeScript("return window._wikiProps.rendererType;");
        if(curFormat.equalsIgnoreCase(format))
            return;

        clickButton("Convert To...", 0);
        _extHelper.waitForExtDialog("Change Format");
        selectOptionByValue(Locator.id("wiki-input-window-change-format-to"), format);
        waitAndClickButton("Convert", 0);
        waitForElement(Locator.id("status").containing("Converted."));
    }

    /**
     * Creates a new wiki page using HTML as the format. See {@link #createNewWikiPage(String)}
     * for more details.
     */
    @LogMethod(quiet = true)public void createNewWikiPage()
    {
        createNewWikiPage("HTML");
    }

    /**
     * Sets the wiki page body, automatically switching to source view if necessary
     * @param body The body text to set
     */
    @LogMethod(quiet = true)public void setWikiBody(String body)
    {
        switchWikiToSourceView();
        setFormElement(Locator.name("body"), body);
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Returns the data region for the the cohort table to enable setting
     * or verifying the enrolled status of the cohort
     */
    public DataRegionTable getCohortDataRegionTable()
    {
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Cohorts"));
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
        assertTrue("Enrolled column should be " + String.valueOf(enrolled), (0 == s.compareToIgnoreCase(String.valueOf(enrolled))));
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
            uncheckCheckbox(Locator.name("quf_enrolled"));
        }
        else
        {
            checkCheckbox(Locator.name("quf_enrolled"));
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
        waitForElement(Locator.linkWithText(wikiName));
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.clickWebpartMenuItem(wikiName, "Edit");

        setWikiBody(srcFragment);
        saveWikiPage();
        return srcFragment;
    }

    /**
     * Switches the wiki edit page to source view when the format type is HTML.
     */
    public void switchWikiToSourceView()
    {
        String curFormat = (String)executeScript("return window._wikiProps.rendererType;");
        if (curFormat.equalsIgnoreCase("HTML"))
        {
            if (isElementPresent(Locator.css("#wiki-tab-source.labkey-tab-inactive")))
            {
                click(Locator.css("#wiki-tab-source > a"));
                waitForElement(Locator.css("#wiki-tab-source.labkey-tab-active"));
            }
        }
    }

    public void switchWikiToVisualView()
    {
        String curFormat = (String)executeScript("return window._wikiProps.rendererType;");
        if (curFormat.equalsIgnoreCase("HTML"))
        {
            if (isElementPresent(Locator.css("#wiki-tab-visual.labkey-tab-inactive")))
            {
                click(Locator.css("#wiki-tab-visual > a"));
                waitForElement(Locator.css("#wiki-tab-visual.labkey-tab-active"));
            }
        }
    }

    public void enableModule(String projectName, String moduleName)
    {
        ensureAdminMode();
        clickProject(projectName);
        enableModule(moduleName, true);
    }

    public void enableModule(String moduleName, boolean isProject)
    {
        enableModules(Collections.singletonList(moduleName), isProject);
    }

    public void enableModules(List<String> moduleNames, boolean isProject)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        for (String moduleName : moduleNames)
        {
            checkCheckbox(Locator.checkboxByTitle(moduleName));
        }
        clickButton("Update Folder");
    }

    public void disableModules(String... moduleNames)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        for (String moduleName : moduleNames)
        {
            uncheckCheckbox(Locator.checkboxByTitle(moduleName));
        }
        clickButton("Update Folder");
    }

    public void goToProjectHome()
    {
        if(!isElementPresent(Locator.linkWithText(getProjectName())))
            goToHome();
        clickProject(getProjectName());
    }

    public void goToProjectHome(String projectName)
    {
        if(!isElementPresent(Locator.linkWithText(projectName)))
            goToHome();
        clickProject(projectName);
    }

    public void goToHome()
    {
        beginAt("/project/home/begin.view");
    }

    /**
     * go to the project settings page of a project
     * @param project project name
     */
    public void goToProjectSettings(String project)
    {
        if(!isElementPresent(Locator.id("projectBar")))
            goToHome();
        clickProject(project);

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

    protected void startImportStudyFromZip(File studyFile)
    {
        startImportStudyFromZip(studyFile, false);
    }

    protected void startImportStudyFromZip(File studyFile, boolean ignoreQueryValidation)
    {
        clickButton("Import Study");
        setFormElement(Locator.name("folderZip"), studyFile);
        if (ignoreQueryValidation)
        {
            click(Locator.checkboxByName("validateQueries"));
        }
        clickButton("Import Study From Local Zip Archive");
        if (isElementPresent(Locator.css(".labkey-error")))
        {
            String errorText = Locator.css(".labkey-error").findElement(getDriver()).getText();
            assertTrue("Error present: " + errorText, errorText.trim().length() == 0);
        }
    }

    protected void importStudyFromZip(File studyFile)
    {
        importStudyFromZip(studyFile, false);
    }

    protected void importStudyFromZip(File studyFile, boolean ignoreQueryValidation)
    {
        startImportStudyFromZip(studyFile, ignoreQueryValidation);
        waitForPipelineJobsToComplete(1, "Study import", false);
    }

    protected void importFolderFromZip(File folderFile, boolean validateQueries, int completedJobs)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Import"));
        waitForElement(Locator.name("folderZip"));
        setFormElement(Locator.name("folderZip"), folderFile);
        if (!validateQueries)
            uncheckCheckbox(Locator.name("validateQueries"));
        clickButtonContainingText("Import Folder From Local Zip Archive");
        waitForText("Data Pipeline");
        waitForPipelineJobsToComplete(completedJobs, "Folder import", false);
    }

    protected void importFolderFromPipeline(String folderFile)
    {
        importFolderFromPipeline(folderFile, 1);
    }

    protected void importFolderFromPipeline(String folderFile, int completedJobsExpected)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Import"));
        clickButtonContainingText("Import Folder Using Pipeline");
        _fileBrowserHelper.importFile(folderFile, "Import Folder");
        waitForPipelineJobsToComplete(completedJobsExpected, "Folder import", false);
    }

    public String getFileContents(String rootRelativePath)
    {
        if (rootRelativePath.charAt(0) != '/')
            rootRelativePath = "/" + rootRelativePath;
        File file = new File(getLabKeyRoot() + rootRelativePath);
        return getFileContents(file);
    }

    public String getFileContents(final File file)
    {
        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                return file.exists();
            }
        }, "File not found: " + file.getPath(), WAIT_FOR_JAVASCRIPT);

        try
        {
            return new String(Files.readAllBytes(Paths.get(file.toURI())));
        }
        catch (IOException fail)
        {
            throw new RuntimeException(fail);
        }
    }

    @LogMethod
    public void signOut()
    {
        log("Signing out");
        beginAt("/login/logout.view");
        waitForElement(Locator.xpath("//a").withText("Sign\u00a0In")); // Will recognize link [BeginAction] or button [LoginAction]
    }

    /*
     * This assumes that you have added the "search" webpart to your project
     */
    public void searchFor(String projectName, String searchFor, int expectedResults, @Nullable String titleName)
    {
        log("Searching Project : " + projectName + " for \"" + searchFor + "\".  Expecting to find : " + expectedResults + " results");
        clickProject(projectName);
        assertElementPresent(Locator.name("q"));
        setFormElement(Locator.id("query"), searchFor);
        clickButton("Search");
        long wait = 0;
        while (wait < 5*defaultWaitForPage)
        {
            if ((titleName == null && isTextPresent("Found " + expectedResults + " result")) ||
                (titleName != null && isElementPresent(Locator.linkContainingText(titleName))))
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
            clickAndWait(Locator.linkContainingText(titleName));
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

    public void assertSetsEqual(List<String> firstSet, List<String> secondSet)
    {
        Set<String> firstHash= new HashSet<>(firstSet);
        Set<String> secondHash= new HashSet<>(secondSet);
        assertTrue("Sets are not equal.  First set:\n" + firstSet + "\nSecond set:\n" + secondSet, firstHash.equals(secondHash));
    }

    public String getAttribute(Locator locator, String attributeName)
    {
        return locator.findElement(getDriver()).getAttribute(attributeName);
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
		return getDriver().getPageSource();
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
            Locator.XPathLocator loc = Locator.schemaTreeNode(schemaPart);

            //first load of schemas might a few seconds
            waitForElement(loc, 30000);
            click(loc);
            waitForElement(Locator.xpath("//div[contains(./@class,'x-tree-selected')]/a/span[text()='" + schemaPart + "']"), 1000);
            waitForElement(Locator.css(".lk-qd-name").withText(schemaWithParents + " Schema"));
        }
    }

    public void selectQuery(String schemaName, String queryName)
    {
        log("Selecting query " + schemaName + "." + queryName + " in the schema browser...");
        selectSchema(schemaName);
        // wait for tool tip to disappear, in case it is covering the element we want to click on
        waitForElement(Locator.xpath("//div[contains(@class, 'x-tip') and contains(@style, 'display: none')]//div[contains(@class, 'x-tip-body')]"));
        waitAndClick(Locator.queryTreeNode(schemaName, queryName));
        waitForElement(Locator.xpath("//div[contains(./@class,'x-tree-selected')]/a/span[text()='" + queryName + "']"), 1000);
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

    public void viewQueryData(String schemaName, String queryName, @Nullable String moduleName)
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
        Locator loc = Locator.tagWithText("a", "edit properties");
        waitForElement(loc, WAIT_FOR_JAVASCRIPT);
        clickAndWait(loc);
    }

    public void createNewQuery(String schemaName)
    {
        selectSchema(schemaName);
        clickButton("Create New Query");
    }


    protected void createQuery(String container, String name, String schemaName, String sql, String xml, boolean inheritable)
    {
        String queryURL = "query/" + container + "/begin.view?schemaName=" + schemaName;
        beginAt(queryURL);
        createNewQuery(schemaName);
        setFormElement(Locator.name("ff_newQueryName"), name);
        clickButton("Create and Edit Source", 0);
        waitForElement(Locator.id("labkey-nav-trail-current-page").withText("Edit " + name));
        setCodeEditorValue("queryText", sql);
        if (xml != null)
        {
            _extHelper.clickExtTab("XML Metadata");
            setCodeEditorValue("metadataText", xml);
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
        Locator locFinishMsg = Locator.xpath("//div[contains(@class, 'lk-vq-status-all-ok') or contains(@class, 'lk-vq-status-error')]");
        waitForElement(Locator.id("lk-sb-details__lk-vq-panel"), WAIT_FOR_JAVASCRIPT);
        if (validateSubfolders)
        {
            shortWait().until(ExpectedConditions.elementToBeClickable(By.id("lk-vq-subfolders")));
            checkCheckbox(Locator.id("lk-vq-subfolders"));
        }
//        if (!isViewCheckSkipped())
//            checkCheckbox(Locator.id("lk-vq-validatemetadata"));
        checkCheckbox(Locator.id("lk-vq-systemqueries"));
        clickButton("Start Validation", 0);
        waitForElement(locFinishMsg, 120000);
        //test for success
        if (!isElementPresent(Locator.xpath("//div[contains(@class, 'lk-vq-status-all-ok')]")))
        {
            fail("Some queries did not pass validation. See error log for more details.");
        }
    }


    public void pressTab(Locator l)
    {
        WebElement el = l.findElement(getDriver());
        el.sendKeys(Keys.TAB);
    }

    public void pressEnter(Locator l)
    {
        WebElement el = l.findElement(getDriver());
        el.sendKeys(Keys.ENTER);
    }

    public void pressDownArrow(Locator l)
    {
        WebElement el = l.findElement(getDriver());
        el.sendKeys(Keys.DOWN);
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

            clickFolder(_studyFolderName);

            int total = 0;
            while( !isElementPresent(Locator.linkWithText("Manage Files")) && total < WAIT_FOR_PAGE)
            {
                // Loop in case test is outrunning the study creator
                sleep(250);
                total += 250;
                refresh();
            }

            clickAndWait(Locator.linkWithText("Manage Files"));
            waitAndClickButton("Process and Import Data");

            // TempDir is somewhere underneath the pipeline root.  Determine each subdirectory we need to navigate to reach it.
            File testDir = _tempDir;
            List<String> dirNames = new ArrayList<>();

            while (!_pipelineRoot.equals(testDir))
            {
                dirNames.add(0, testDir.getName());
                testDir = testDir.getParentFile();
            }

            //Build folder path.
            String path = "/";
            for (String dir : dirNames)
                path += dir + "/";

            _fileBrowserHelper.selectFileBrowserItem(path);

            for (File copiedArchive : _copiedArchives)
                _fileBrowserHelper.clickFileBrowserFileCheckbox(copiedArchive.getName());
            _fileBrowserHelper.selectImportDataAction("Import Specimen Data");
            clickButton("Start Import");
        }

        @LogMethod
        public void waitForComplete()
        {
            log("Waiting for completion of specimen archives");

            clickFolder(_studyFolderName);
            clickAndWait(Locator.linkWithText("Manage Files"));

            if (_expectError)
                waitForPipelineJobsToFinish(_completeJobsExpected);
            else
                waitForPipelineJobsToComplete(_completeJobsExpected, "specimen import", _expectError);

            for (File copiedArchive : _copiedArchives)
                if (!copiedArchive.delete())
                    throw new RuntimeException("Couldn't delete copied specimen archive: " + copiedArchive.getAbsolutePath());
        }
    }


    // Wait until the pipeline UI shows the requested number of complete jobs.  Fail if any job status becomes "ERROR".
    @LogMethod
    public void waitForPipelineJobsToComplete(@LoggedParam final int completeJobsExpected, @LoggedParam final String description, final boolean expectError)
    {
        log("Waiting for " + completeJobsExpected + " pipeline jobs to complete");

        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                log("Waiting for " + description);
                List<String> statusValues = getPipelineStatusValues();
                log("[" + StringUtils.join(statusValues,",") + "]");
                if (!expectError)
                {
                    assertElementNotPresent(Locator.linkWithText("ERROR"));
                }
                if (statusValues.size() < completeJobsExpected || statusValues.size() != getFinishedCount(statusValues))
                {
                    refresh();
                    return false;
                }
                return true;
            }
        }, "Pipeline jobs did not complete.", MAX_WAIT_SECONDS * 1000);

        assertEquals("Did not find correct number of completed pipeline jobs.", completeJobsExpected, expectError ? getFinishedCount(getPipelineStatusValues()) : getCompleteCount(getPipelineStatusValues()));
    }

    // wait until pipeline UI shows that all jobs have finished (either COMPLETE or ERROR status)
    @LogMethod
    protected void waitForPipelineJobsToFinish(@LoggedParam int jobsExpected)
    {
        log("Waiting for " + jobsExpected + " pipeline jobs to finish");
        List<String> statusValues = getPipelineStatusValues();
        startTimer();
        while (getFinishedCount(statusValues) < jobsExpected && elapsedSeconds() < MAX_WAIT_SECONDS)
        {
            sleep(1000);
            refresh();
            statusValues = getPipelineStatusValues();
        }
        assertEquals("Did not find correct number of finished pipeline jobs.", jobsExpected, getFinishedCount(statusValues));
    }

    @LogMethod
    protected void waitForRunningPipelineJobs(int wait)
    {
        log("Waiting for running pipeline jobs list to be empty.");
        List<String> statusValues = getPipelineStatusValues();
        startTimer();
        while (statusValues.size() > 0 && elapsedSeconds() < wait)
        {
            log("[" + StringUtils.join(statusValues,",") + "]");
            log("Waiting for " + statusValues.size() + " jobs to complete...");
            sleep(1000);
            refresh();
            statusValues = getPipelineStatusValues();
        }

        assertTrue("Running pipeline jobs were found.  Timeout:" + wait, statusValues.size() == 0);
    }

    public void setCodeEditorValue(String id, String value)
    {
        _extHelper.setCodeMirrorValue(id, value);
    }

    public void ensureSignedOut()
    {
        if(isElementPresent(Locator.id("userMenuPopupLink")))
            signOut();
    }

    protected void reloadStudyFromZip(File studyFile)
    {
        goToManageStudy();
        clickButton("Reload Study");
        setFormElement(Locator.name("folderZip"), studyFile);
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

    public void assertSVG(final String expectedSvgText)
    {
        assertSVG(expectedSvgText, 0);
    }

    /**
     * Wait for an SVG with the specified text (Ignores thumbnails)
     * @param expectedSvgText exact text expected. Whitespace will be ignored on Firefox due to inconsistencies in getText results. Use getText value from Chrome.
     * @param svgIndex the zero-based index of the svg which is expected to match
     */
    public void assertSVG(final String expectedSvgText, final int svgIndex)
    {
        final String expectedText = prepareSvgText(expectedSvgText);
        final Locator svgLoc = Locator.css("div:not(.thumbnail) > svg").index(svgIndex);

        if (!isDumpSvgs())
        {
            doesElementAppear(new BaseWebDriverTest.Checker()
            {
                @Override
                public boolean check()
                {
                    if (isElementPresent(svgLoc))
                    {
                        String svgText = prepareSvgText(getText(svgLoc));
                        return expectedText.equals(svgText);
                    }
                    else
                        return false;
                }
            }, WAIT_FOR_JAVASCRIPT);

            String svgText = prepareSvgText(getText(svgLoc));
            assertEquals("SVG did not look as expected", expectedText, svgText);
        }
        else
        {
            waitForElement(svgLoc);
            scrollIntoView(svgLoc);
            String svgText = getText(svgLoc);

            File svgDir = new File(ensureDumpDir(), "svgs");
            String baseName;
            File svgFile;

            int i = 0;
            do
            {
                i++;
                baseName = String.format("%2d-svg[%d]", i, svgIndex);
                svgFile = new File(svgDir, baseName + ".txt");
            }while (svgFile.exists());

            dumpScreen(svgDir, baseName);

            try(FileWriter writer = new FileWriter(svgFile))
            {
                writer.write("Expected:\n");
                writer.write(expectedSvgText.replace("\\", "\\\\").replace("\n", "\\n"));
                writer.write("\n\nActual:\n");
                writer.write(svgText.replace("\\", "\\\\").replace("\n", "\\n"));
            }
            catch (IOException e){
                log("Failed to dump svg: " + svgFile.getName() + "Reason: " + e.getMessage());
            }
        }
    }

    public void waitForElements(Locator loc, int count)
    {
        waitForElementToDisappear(loc.index(count), WAIT_FOR_JAVASCRIPT);
        if (count > 0)
            waitForElement(loc.index(count - 1));
    }

    public String getTextFromElement(Locator loc)
    {
        return getText(loc);
    }

    private String prepareSvgText(String svgText)
    {
        final boolean isFirefox = getBrowserType() == BrowserType.FIREFOX;

        // Remove raphael credits to make this function work with Raphael and d3 renderers.
        final String ignoredRaphaelText = "Created with Rapha\u00ebl 2.1.0";
        svgText = svgText.replace(ignoredRaphaelText, "");
        svgText = svgText.trim();

        // Strip out all the whitespace on Firefox to deal with different return of getText from svgs
        return isFirefox ?
                svgText.replaceAll("[\n ]", "") :
                svgText;
    }

    private boolean isDumpSvgs()
    {
        return "true".equals(System.getProperty("dump.svgs"));
    }
}
