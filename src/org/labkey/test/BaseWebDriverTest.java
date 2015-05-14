/*
 * Copyright (c) 2012-2015 LabKey Corporation
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

//TODO: Use java.lang.function.Function once Java 8 migration is complete
import com.google.common.base.Function;
import com.thoughtworks.selenium.SeleniumException;
import net.jsourcerer.webdriver.jserrorcollector.JavaScriptError;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.time.FastDateFormat;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.RuleChain;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.labkey.api.util.FileUtil;
import org.labkey.api.writer.UTF8PrintWriter;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.SideWebPart;
import org.labkey.test.util.*;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.labkey.test.util.ext4cmp.Ext4GridRef;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
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
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.TestProperties.isDevModeEnabled;
import static org.labkey.test.TestProperties.isInjectionCheckEnabled;
import static org.labkey.test.TestProperties.isLeakCheckSkipped;
import static org.labkey.test.TestProperties.isLinkCheckEnabled;
import static org.labkey.test.TestProperties.isQueryCheckSkipped;
import static org.labkey.test.TestProperties.isScriptCheckEnabled;
import static org.labkey.test.TestProperties.isSystemMaintenanceDisabled;
import static org.labkey.test.TestProperties.isTestCleanupSkipped;
import static org.labkey.test.TestProperties.isTestRunningOnTeamCity;
import static org.labkey.test.TestProperties.isViewCheckSkipped;
import static org.labkey.test.WebTestHelper.GC_ATTEMPT_LIMIT;
import static org.labkey.test.WebTestHelper.MAX_LEAK_LIMIT;
import static org.labkey.test.WebTestHelper.buildURL;
import static org.labkey.test.WebTestHelper.getHttpClientBuilder;
import static org.labkey.test.WebTestHelper.getHttpGetResponse;
import static org.labkey.test.WebTestHelper.getHttpPostResponse;
import static org.labkey.test.WebTestHelper.isLocalServer;
import static org.labkey.test.WebTestHelper.leakCRC;
import static org.labkey.test.WebTestHelper.stripContextPath;

/**
 * This class should be used as the base for all functional test classes
 * Test cases should be non-destructive and should not depend on a particular execution order
 *
 * Shared setup steps should be in a public static void method annotated with org.junit.BeforeClass
 * The name of the method is not important. The JUnit runner finds the method solely based on the BeforeClass annotation
 *
 * @BeforeClass
 * public static void doSetup() throws Exception
 * {
 *     MyTestClass initTest = (MyTestClass)getCurrentTest();
 *     initTest.setupProject(); // Perform shared setup steps here
 * }
 *
 * org.junit.AfterClass is also supported, but should not be used to perform any destructive cleanup as it is executed
 * before the base test class can perform its final checks -- link check, leak check, etc.
 * The doCleanup method should be overridden for initial and final project cleanup
 */
public abstract class BaseWebDriverTest implements Cleanable, WebTest
{
    private static BaseWebDriverTest currentTest;
    private static WebDriver _driver;

    private String _lastPageTitle = null;
    private URL _lastPageURL = null;
    private String _lastPageText = null;
    private Stack<String> _locationStack = new Stack<>();
    private String _savedLocation = null;
    private Stack<String> _impersonationStack = new Stack<>();
    protected static boolean _testFailed = false;
    protected static Boolean _anyTestCaseFailed = false;
    protected boolean _testTimeout = false;
    public final static int WAIT_FOR_PAGE = 30000;
    public int defaultWaitForPage = WAIT_FOR_PAGE;
    public final static int WAIT_FOR_JAVASCRIPT = 10000;
    public int longWaitForPage = defaultWaitForPage * 5;
    private List<JavaScriptError> _jsErrors;
    private static WebDriverWait _shortWait;
    private static WebDriverWait _longWait;
    private static JSErrorChecker _jsErrorChecker = null;
    private final ArtifactCollector _artifactCollector;

    public AbstractContainerHelper _containerHelper = new APIContainerHelper(this);
    public ExtHelper _extHelper = new ExtHelper(this);
    public Ext4Helper _ext4Helper = new Ext4Helper(this);
    public CustomizeViewsHelper _customizeViewsHelper = new CustomizeViewsHelper(this);
    public StudyHelper _studyHelper = new StudyHelper(this);
    public ListHelper _listHelper = new ListHelper(this);
    public AbstractUserHelper _userHelper = new APIUserHelper(this);
    public AbstractAssayHelper _assayHelper = new APIAssayHelper(this);
    public SecurityHelper _securityHelper = new SecurityHelper(this);
    public FileBrowserHelper _fileBrowserHelper = new FileBrowserHelper(this);
    public PermissionsHelper _permissionsHelper = new PermissionsHelper(this);
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
        _artifactCollector = new ArtifactCollector(this);
        _extHelper = new ExtHelper(this);
        _ext4Helper = new Ext4Helper(this);
        _listHelper = new ListHelper(this);
        _customizeViewsHelper = new CustomizeViewsHelper(this);
        _jsErrors = new ArrayList<>();
        _downloadDir = new File(getArtifactCollector().ensureDumpDir(), "downloads");

        String seleniumBrowser = System.getProperty("selenium.browser");
        if (seleniumBrowser == null || seleniumBrowser.length() == 0)
        {
            if (isTestRunningOnTeamCity() || (bestBrowser() == BrowserType.CHROME))
                BROWSER_TYPE = BrowserType.FIREFOX;
            else
                BROWSER_TYPE = bestBrowser();
        }
        else if (seleniumBrowser.toLowerCase().contains("best"))
        {
            BROWSER_TYPE = bestBrowser();
        }
        else
        {
            for (BrowserType bt : BrowserType.values())
            {
                if (seleniumBrowser.toLowerCase().contains(bt.name().toLowerCase()))
                {
                    BROWSER_TYPE = bt;
                    return;
                }
            }
            BROWSER_TYPE = bestBrowser();
            log("Unknown browser [" + seleniumBrowser + "]; Using best compatible browser [" + BROWSER_TYPE + "]");
        }
    }

    protected static BaseWebDriverTest getCurrentTest()
    {
        return currentTest;
    }

    public WebDriver getDriver()
    {
        return _driver;
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

    protected abstract @Nullable String getProjectName();

    @LogMethod
    public void setUp()
    {
        if (_testFailed)
        {
            // In case the previous test failed so catastrophically that it couldn't clean up after itself
            doTearDown();
        }

        _driver = createNewWebDriver(getDriver());

        getDriver().manage().timeouts().setScriptTimeout(WAIT_FOR_PAGE, TimeUnit.MILLISECONDS);
        getDriver().manage().timeouts().pageLoadTimeout(defaultWaitForPage, TimeUnit.MILLISECONDS);
        _shortWait = new WebDriverWait(getDriver(), WAIT_FOR_JAVASCRIPT/1000);
        _longWait = new WebDriverWait(getDriver(), WAIT_FOR_PAGE/1000);

        getDriver().manage().window().setSize(new Dimension(1280, 1024));
    }

    protected WebDriver createNewWebDriver(WebDriver oldWebDriver)
    {
        WebDriver newWebDriver = null;

        switch (BROWSER_TYPE)
        {
            case IE: //experimental
            {
                if(oldWebDriver != null && !(oldWebDriver instanceof InternetExplorerDriver))
                {
                    oldWebDriver.quit();
                    oldWebDriver = null;
                }
                if(oldWebDriver == null)
                {
                    newWebDriver = new InternetExplorerDriver();
                }
                break;
            }
            case HTML: //experimental
            {
                if(oldWebDriver != null && !(oldWebDriver instanceof HtmlUnitDriver))
                {
                    oldWebDriver.quit();
                    oldWebDriver = null;
                }
                if(oldWebDriver == null)
                {
                    newWebDriver = new HtmlUnitDriver(true);
                }
                break;
            }
            case CHROME:
            {
                if(oldWebDriver != null && !(oldWebDriver instanceof ChromeDriver))
                {
                    oldWebDriver.quit();
                    oldWebDriver = null;
                }
                if(oldWebDriver == null)
                {
                    TestProperties.ensureChromedriverExeProperty();
                    ChromeOptions options = new ChromeOptions();
                    Dictionary<String, Object> prefs = new Hashtable<>();

                    prefs.put("download.prompt_for_download", "false");
                    prefs.put("download.default_directory", getDownloadDir().getAbsolutePath());
                    prefs.put("profile.content_settings.pattern_pairs.*.multiple-automatic-downloads", 1); // Turns off multiple download warning
                    options.setExperimentalOption("prefs", prefs);
                    options.addArguments("test-type"); // Suppress '--ignore-certificate-errors' warning
                    options.addArguments("disable-xss-auditor");

                    if (isScriptCheckEnabled())
                    {
                        File jsErrorCheckerExtension = new File(TestFileUtils.getLabKeyRoot(), "server/test/chromeextensions/jsErrorChecker");
                        options.addArguments("load-extension=" + jsErrorCheckerExtension.toString());
                    }

                    DesiredCapabilities capabilities = DesiredCapabilities.chrome();
                    capabilities.setCapability(ChromeOptions.CAPABILITY, options);
                    _jsErrorChecker = new ChromeJSErrorChecker();
                    newWebDriver = new ChromeDriver(capabilities);
                }
                break;
            }
            case FIREFOX:
            {
                if(oldWebDriver != null && !(oldWebDriver instanceof FirefoxDriver))
                {
                    oldWebDriver.quit();
                    oldWebDriver = null;
                }
                if (oldWebDriver == null)
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
                    profile.setPreference("browser.download.manager.showAlertOnComplete", false);
                    profile.setPreference("browser.download.manager.showWhenStarting",false);
                    profile.setPreference("browser.helperApps.alwaysAsk.force", false);
                    profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
                            "application/vnd.ms-excel," + // .xls
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet," + // .xlsx
                                    "application/octet-stream," +
                                    "application/zip," +
                                    "application/x-gzip," +
                                    "application/x-zip-compressed," +
                                    "application/xml," +
                                    "text/xml," +
                                    "text/x-script.perl," +
                                    "text/tab-separated-values," +
                                    "text/csv");
                    if (isScriptCheckEnabled())
                    {
                        try
                        {JavaScriptError.addExtension(profile);}
                        catch(IOException e)
                        {throw new RuntimeException("Failed to load JS error checker", e);}
                    }

                    profile.setEnableNativeEvents(useNativeEvents());

                    DesiredCapabilities capabilities = DesiredCapabilities.firefox();
                    capabilities.setCapability(FirefoxDriver.PROFILE, profile);

                    String browserPath = System.getProperty("selenium.browser.path", "");
                    if (browserPath.length() > 0)
                    {
                        FirefoxBinary binary = new FirefoxBinary(new File(browserPath));
                        capabilities.setCapability(FirefoxDriver.BINARY, binary);
                    }
                    newWebDriver = new FirefoxDriver(capabilities);

                    _jsErrorChecker = new FirefoxJSErrorChecker();
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Browser not yet implemented: " + BROWSER_TYPE);
        }

        if (newWebDriver != null)
        {
            Capabilities caps = ((RemoteWebDriver) newWebDriver).getCapabilities();
            String browserName = caps.getBrowserName();
            String browserVersion = caps.getVersion();
            log("Browser: " + browserName + " " + browserVersion);
            return newWebDriver;
        }
        else
        {
            return oldWebDriver;
        }
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

    public ArtifactCollector getArtifactCollector()
    {
        return _artifactCollector;
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
                    "ext-base-debug.js",
                    "ext-all-sandbox-debug.js",
                    "ext-all-sandbox.js",
                    "ext-all-sandbox-dev.js",
                    "XULElement.selectedIndex", // Ignore known Firefox Issue
                    "Failed to decode base64 string!", // Firefox issue
                    "xulrunner-1.9.0.14/components/FeedProcessor.js", // Firefox problem
                    "Image corrupt or truncated:",
                    "mutating the [[Prototype]] of an object will cause your code to run very slowly", //d3, issue: 21717
                    "Using //@ to indicate", // jQuery
                    "CodeMirror is not defined", // There will be more severe errors than this if CodeMirror is actually broken
                    "NS_ERROR_FAILURE" // NS_ERROR_FAILURE:  [http://localhost:8111/labkey/vis/lib/d3pie.min.js:8]
            );
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

    public enum BrowserType
    {
        FIREFOX,
        IE,
        CHROME,
        HTML
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

    private static void doTearDown()
    {
        try
        {
            boolean skipTearDown = _testFailed && "false".equalsIgnoreCase(System.getProperty("close.on.fail"));
            if ((!skipTearDown || isTestRunningOnTeamCity()) && _driver != null)
            {
                _driver.quit();
            }
        }
        catch (UnreachableBrowserException ignore) {}
        finally
        {
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

    public String getResponseText()
    {
        return getHtmlSource();
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
            if (link.trim().length() > 0)
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
        if (80 == WebTestHelper.getWebPort() && url.getAuthority().endsWith(":-1"))
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

    public String getSavedLocation()
    {
        return _savedLocation;
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
        Function<Void, Void> func = new Function<Void, Void>()
        {
            @Override
            public Void apply(Void o)
            {
                getDriver().navigate().refresh();
                return null;
            }
        };

        applyAndWaitForPageToLoad(func, millis);
    }

    public void goBack(int millis)
    {
        Function<Void, Void> func = new Function<Void, Void>()
        {
            @Override
            public Void apply(Void o)
            {
                getDriver().navigate().back();
                return null;
            }
        };

        applyAndWaitForPageToLoad(func, millis);
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
            waitForElement(Locator.id("email"), defaultWaitForPage);
            assertElementPresent(Locator.tagWithName("form", "login"));
            setFormElement(Locator.name("email"), PasswordUtil.getUsername());
            setFormElement(Locator.name("password"), PasswordUtil.getPassword());
            acceptTermsOfUse(null, false);
            clickButton("Sign In");
            bypassSecondaryAuthentication();

            // If there are site-wide terms, you will have to do this again and accept the terms that are now displayed.
            if (isElementPresent(Locator.id("approvedTermsOfUse")))
            {
                if (isElementPresent(Locator.name("password")))
                {
                    setFormElement(Locator.name("email"), PasswordUtil.getUsername());
                    setFormElement(Locator.name("password"), PasswordUtil.getPassword());
                    acceptTermsOfUse(null, false);
                    clickButton("Sign In");
                }
                else // just need to accept the terms that are now displayed
                {
                    acceptTermsOfUse(null, true);
                }
            }

            if (!isElementPresent(Locator.id("userMenuPopupLink")))
            {
                String errors = StringUtils.join(getTexts(Locator.css(".labkey-error").findElements(getDriver())), "\n");

                if (errors.contains("The e-mail address and password you entered did not match any accounts on file."))
                    throw new IllegalStateException("Could not log in with the saved credentials.  Please verify that the test user exists on this installation or reset the credentials using 'ant setPassword'");
                else if (errors.contains("Your password does not meet the complexity requirements; please choose a new password."))
                    throw new IllegalStateException("Password complexity requirement was left on by a previous test");
                else if (errors.contains("log in and approve the terms of use."))
                    throw new IllegalStateException("Terms of use not accepted at login");
                else
                    throw new IllegalStateException("Unexpected error(s) during login." + errors);
            }
        }

        assertSignOutAndMyAccountPresent();

        if (isElementPresent(Locator.css(".labkey-nav-page-header").withText("Startup Modules"))||
            isElementPresent(Locator.css(".labkey-nav-page-header").withText("Upgrade Modules")))
        {
            waitForElement(Locator.id("status-progress-bar").withText("Module startup complete"), WAIT_FOR_PAGE);
            clickButton("Next");
        }
    }

    public void assertSignOutAndMyAccountPresent()
    {
        click(Locator.id("userMenuPopupLink"));
        assertElementPresent(Ext4Helper.Locators.menuItem("My Account"));
        assertElementPresent(Ext4Helper.Locators.menuItem("Sign Out"));
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
        setFormElement(Locator.id("password"), password);
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
        ensureAdminMode();
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

    public void clickAdminMenuItem(String... items)
    {
        longWait().until(ExpectedConditions.elementToBeClickable(Locators.ADMIN_MENU.toBy()));
        _ext4Helper.clickExt4MenuButton(true, Locators.ADMIN_MENU, false, items);
    }

    public void clickUserMenuItem(String... items)
    {
        clickUserMenuItem(true, false, items);
    }

    public void clickUserMenuItem(boolean wait, boolean onlyOpen, String... items)
    {
        waitForElement(Locators.USER_MENU);
        _ext4Helper.clickExt4MenuButton(wait, Locators.USER_MENU, onlyOpen, items);
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
    public void goToManageLists()
    {
        clickAdminMenuItem("Manage Lists");
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
                getDriver().manage().timeouts().pageLoadTimeout(WAIT_FOR_PAGE, TimeUnit.MILLISECONDS);
                getDriver().get(buildURL("login", "logout"));

                if (isElementPresent(Locator.css("table.labkey-main")) || isElementPresent(Locator.id("permalink")) || isElementPresent(Locator.id("headerpanel")))
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
            catch (SeleniumException | TimeoutException e)
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
                try
                {
                    verifyRedirectBehavior(upgradeText);
                }
                catch (IOException fail)
                {
                    throw new RuntimeException(fail);
                }

                int waitMs = 10 * 60 * 1000; // we'll wait at most ten minutes

                while (waitMs > 0 && (!(isButtonPresent("Next") || isElementPresent(Locator.linkWithText("Home")))))
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

                if (isButtonPresent("Next"))
                {
                    clickButton("Next");

                    // check for any additional upgrade pages inserted after module upgrade
                    if (isButtonPresent("Next"))
                        clickButton("Next");
                }

                if (isElementPresent(Locator.linkContainingText("Go directly to the server's Home page")))
                {
                    clickAndWait(Locator.linkContainingText("Go directly to the server's Home page"));
                }
            }

            // Tests hit this page a lot. Make it loads as fast as possible
            PortalHelper portalHelper = new PortalHelper(this);
            for (BodyWebPart webPart : portalHelper.getBodyWebParts())
                webPart.delete();
            for (SideWebPart webPart : portalHelper.getSideWebParts())
                webPart.delete();
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
    private void verifyRedirectBehavior(String upgradeText) throws IOException
    {
        // Do these checks via direct http requests the primary upgrade window seems to interfere with this test, #15853

        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpResponse response = null;
        HttpUriRequest method;
        int status;

        try (CloseableHttpClient client = (CloseableHttpClient)WebTestHelper.getHttpClient())
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
            try (CloseableHttpClient redirectClient = getHttpClientBuilder().setRedirectStrategy(redirectStrategy).build())
            {
                method = new HttpPost(getBaseURL() + "/login/logout.view");
                List<NameValuePair> args = new ArrayList<>();
                args.add(new BasicNameValuePair("login", PasswordUtil.getUsername()));
                args.add(new BasicNameValuePair("password", PasswordUtil.getPassword()));
                ((HttpPost) method).setEntity(new UrlEncodedFormEntity(args));
                response = redirectClient.execute(method, context);
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

    /**
     * Switch to the initial test window
     */
    public void switchToMainWindow()
    {
        switchToWindow(0);
    }

    public void switchToWindow(int index)
    {
        List<String> windows = new ArrayList<>(getDriver().getWindowHandles());
        getDriver().switchTo().window(windows.get(index));
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

    public void startSystemMaintenance()
    {
        startSystemMaintenance("");
    }

    public void startSystemMaintenance(String taskName)
    {
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("temp", "true");
        if (!taskName.isEmpty())
            urlParams.put("taskName", taskName);
        String maintenanceTriggerUrl = WebTestHelper.buildURL("admin", "systemMaintenance", urlParams);

        smStart = System.currentTimeMillis();
        SimpleHttpRequest request = new SimpleHttpRequest(maintenanceTriggerUrl);
        request.setRequestMethod("POST");
        request.copySession(getDriver());
        assertEquals("Failed to start system maintenance", HttpStatus.SC_OK, request.getResponse().getResponseCode());
    }

    public void waitForSystemMaintenanceCompletion()
    {
        assertTrue("Must call startSystemMaintenance() before waiting for completion", smStart > 0);
        long elapsed = System.currentTimeMillis() - smStart;

        beginAt(WebTestHelper.buildURL("admin", "systemmaintenance"));

        int timeLeft = 10 * 60 * 1000 - ((Long)elapsed).intValue();
        // Page updates automatically via AJAX... keep checking (up to 10 minutes from the start of the test) for system maintenance complete text
        waitFor(new Checker()
        {
            public boolean check()
            {
                return isTextPresent("System maintenance complete");
            }
        }, "System maintenance failed to complete in 10 minutes.", timeLeft > 0 ? timeLeft : 0);
    }

    private void populateLastPageInfo()
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

    /**
     * Get rendered, visible page text.
     * @return All visible text from the 'body' of the page
     */
    public String getBodyText()
    {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CallableGetText getText = new CallableGetText();
        Future<String> future = executor.submit(getText);

        try
        {
            return future.get(60, TimeUnit.SECONDS);
        }
        catch (java.util.concurrent.TimeoutException | InterruptedException | ExecutionException e)
        {
            throw new TestTimeoutException("Timed out getting page text. Page is probably too complex. Refactor test to look for specific element(s) instead.", e);
        }
        finally
        {
            executor.shutdownNow();
        }
    }

    /**
     * Get page text using a separate thread to avoid test timeouts when complex pages choke WebDriver
     */
    private class CallableGetText implements Callable<String>
    {
        @Override
        public String call()
        {
            try
            {
                return shortWait().until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body"))).getText();
            }
            catch (TimeoutException|NoSuchElementException tex)
            {
                return getDriver().getPageSource(); // probably viewing a tsv or text file
            }
        }
    }

    public void resetErrors()
    {
        if ( isGuestModeTest() )
            return;
        if (isLocalServer())
            beginAt("/admin/resetErrorMark.view");
    }

    private static final String BEFORE_CLASS = "BeforeClass";
    private static final String AFTER_CLASS = "AfterClass";
    private static boolean beforeClassSucceeded = false;
    private static Class testClass;
    private static int testCount;
    private static int currentTestNumber;

    @ClassRule
    public static TestWatcher testClassWatcher() {return new TestWatcher()
    {
        @Override
        public Statement apply(Statement base, Description description)
        {
            testClass = description.getTestClass();
            _driver = null;
            testCount = description.getChildren().size();
            currentTestNumber = 0;
            beforeClassSucceeded = false;
            _anyTestCaseFailed = false;

            return super.apply(base, description);
        }

        @Override
        public void starting(Description description)
        {
            TestLogger.resetLogger();
            TestLogger.log("// BeforeClass \\\\");
            TestLogger.increaseIndent();

            ArtifactCollector.init();

            try
            {
                currentTest = (BaseWebDriverTest)testClass.newInstance();
            }
            catch (InstantiationException | IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }

            currentTest.setUp();
        }

        @Override
        protected void failed(Throwable e, Description description)
        {
            String pseudoTestName = beforeClassSucceeded ? AFTER_CLASS : BEFORE_CLASS;

            if (currentTest != null)
            {
                try
                {
                    currentTest.handleFailure(e, pseudoTestName);
                }
                catch (Exception secondary)
                {
                    TestLogger.log("Error while collecting failure data");
                    secondary.printStackTrace(System.out);
                }
            }
        }

        @Override
        protected void finished(Description description)
        {
            doTearDown();

            TestLogger.resetLogger();
            TestLogger.log("\\\\ AfterClass Complete //");
        }
    };}

    public static Class getCurrentTestClass()
    {
        return testClass;
    }

    @BeforeClass
    public static void preamble()
    {
        currentTest.getContainerHelper().clearCreatedProjects();
        currentTest.doPreamble();
    }

    private void doPreamble()
    {
        signIn();
        enableEmailRecorder();
        disableMiniProfiler();
        resetErrors();

        if (isSystemMaintenanceDisabled())
        {
            // Disable scheduled system maintenance to prevent timeouts during nightly tests.
            disableMaintenance();
        }

        // Only do this as part of test startup if we haven't already checked. Since we do this as the last
        // step in the test, there's no reason to bother doing it again at the beginning of the next test
        if (!_checkedLeaksAndErrors && !"DRT".equals(System.getProperty("suite")))
        {
            checkLeaksAndErrors();
        }

        cleanup(false);
    }

    @Before
    public final void beforeTest() throws Exception
    {
        simpleSignIn();
    }

    @AfterClass
    public static void postamble() throws Exception
    {
        if (beforeClassSucceeded)
        {
            getCurrentTest().doPostamble();
        }
    }

    @ClassRule
    public static Timeout globalTimeout()
    {
        return new Timeout(2400000); // 40 minutes
    }

    @Rule
    public Timeout testTimeout = new Timeout(1800000); // 30 minutes

    private TestWatcher _watcher = new TestWatcher()
    {
        @Override
        protected void starting(Description description)
        {
            // We know that @BeforeClass methods are done now that we are in a non-static context
            beforeClassSucceeded = true;

            if (TestProperties.isNewWebDriverForEachTest())
                doTearDown();

            setUp(); // Instantiate new WebDriver if needed
            _testFailed = false;
        }

        @Override
        protected void succeeded(Description description)
        {
            checkJsErrors();
        }

        @Override
        protected void failed(Throwable e, Description description)
        {
            Ext4Helper.resetCssPrefix();
            try
            {
                handleFailure(e, description.getMethodName());
            }
            catch (Exception secondary)
            {
                log("Error while collecting failure data");
                secondary.printStackTrace(System.out);
            }
        }

        @Override
        protected void finished(Description description)
        {
            Ext4Helper.resetCssPrefix();
        }
    };

    private TestWatcher _logger = new TestWatcher()
    {
        private long testCaseStartTimeStamp;

        @Override
        protected void skipped(AssumptionViolatedException e, Description description)
        {
            if (currentTestNumber == 0)
            {
                TestLogger.resetLogger();
                TestLogger.log("\\\\ BeforeClass Complete //");
            }

            currentTestNumber++;

            String testCaseName = description.getMethodName();

            TestLogger.resetLogger();
            TestLogger.log("<< Test Case Skipped - " + testCaseName + " >>");
        }

        @Override
        protected void starting(Description description)
        {
            if (currentTestNumber == 0)
            {
                TestLogger.resetLogger();
                TestLogger.log("\\\\ BeforeClass Complete //");
            }

            currentTestNumber++;
            testCaseStartTimeStamp = System.currentTimeMillis();
            String testCaseName = description.getMethodName();

            TestLogger.resetLogger();
            TestLogger.log("// Begin Test Case - " + testCaseName + " \\\\");
            TestLogger.increaseIndent();
        }

        @Override
        protected void succeeded(Description description)
        {
            Long elapsed = System.currentTimeMillis() - testCaseStartTimeStamp;
            String testCaseName = description.getMethodName();

            TestLogger.resetLogger();
            TestLogger.log("\\\\ Test Case Complete - " + testCaseName + " [" + getElapsedString(elapsed) + "] //");
        }

        @Override
        protected void failed(Throwable e, Description description)
        {
            Long elapsed = System.currentTimeMillis() - testCaseStartTimeStamp;
            String testCaseName = description.getMethodName();

            TestLogger.resetLogger();
            TestLogger.log("\\\\ Failed Test Case - " + testCaseName + " [" + getElapsedString(elapsed) + "] //");
        }

        @Override
        protected void finished(Description description)
        {
            if (currentTestNumber == testCount)
            {
                TestLogger.resetLogger();
                TestLogger.log("// AfterClass \\\\");
                TestLogger.increaseIndent();
            }

        }

        private String getElapsedString(Long elapsed)
        {
            return String.format("%dm %d.%ds",
                    TimeUnit.MILLISECONDS.toMinutes(elapsed),
                    TimeUnit.MILLISECONDS.toSeconds(elapsed) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed)),
                    elapsed - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(elapsed)));
        }
    };

    @Rule
    public RuleChain _ruleChain = RuleChain.outerRule(_logger).around(_watcher);

    /**
     * Collect additional information about test failures and publish build artifacts for TeamCity
     */
    @LogMethod
    public void handleFailure(Throwable error, @LoggedParam String testName)
    {
        _testFailed = true;
        _anyTestCaseFailed = true;

        try
        {
            if (error instanceof UnreachableBrowserException || error instanceof InterruptedException || getDriver() == null)
            {
                return;
            }

            if (error instanceof TestTimeoutException)
            {
                _testTimeout = true;
            }
            else if (error instanceof UnhandledAlertException)
            {
                dismissAllAlerts();
            }

            try
            {
                populateLastPageInfo();

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
                    catch (IOException t)
                    {
                        log("Unable to re-invoke last page");
                        t.printStackTrace(System.out);
                    }
                }
            }
            catch (Exception e)
            {
                log("Unable to determine information about the last page");
                e.printStackTrace(System.out);
            }

            try
            {
                getArtifactCollector().dumpPageSnapshot(testName, null);
                if (isTestRunningOnTeamCity())
                {
                    getArtifactCollector().addArtifactLocation(new File(TestFileUtils.getLabKeyRoot(), "sampledata"));
                    getArtifactCollector().addArtifactLocation(new File(TestFileUtils.getLabKeyRoot(), "build/deploy/files"));
                    getArtifactCollector().dumpPipelineFiles();
                }
                if (_testTimeout)
                    getArtifactCollector().dumpThreads(this);
            }
            catch (Exception e)
            {
                log("Unable to dump failure information");
                e.printStackTrace(System.out);
            }

            dismissAllAlerts();
            checkJsErrors();
        }
        finally
        {
            try(TestScrubber scrubber = new TestScrubber())
            {
                scrubber.cleanSiteSettings();
            }

            doTearDown();
        }
    }

    private void doPostamble()
    {
        if (!_anyTestCaseFailed)
        {
            //make sure you're signed in as admin, because this won't work otherwise
            ensureSignedInAsAdmin();

            checkQueries();

            checkViews();

            if(!isPerfTest)
                checkActionCoverage();

            checkLinks();

            if (!isTestCleanupSkipped())
            {
                goToHome();
                cleanup(true);
            }
            else
            {
                log("Skipping test cleanup as requested.");
            }

            if (!"DRT".equals(System.getProperty("suite")) || Runner.isFinalTest())
            {
                checkLeaksAndErrors();
            }

            checkJsErrors();
        }
        else
        {
            log("Skipping post-test checks because a test case failed.");
        }

        if (!_anyTestCaseFailed && getDownloadDir().exists())
        {
            try{
                FileUtils.deleteDirectory(getDownloadDir());
            }
            catch (IOException ignore) { }
        }
    }

    public void deleteSiteWideTermsOfUsePage()
    {
        try
        {
            log("Removing site-wide terms of use page");
            getHttpPostResponse(getBaseURL() + "/wiki/delete.view?name=_termsOfUse");
        }
        catch (IOException e)
        {
            log("Problem removing site-wide terms of use page.  Perhaps it does not exist.");
        }
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

    protected void acceptTermsOfUse(@Nullable String text, Boolean doAgree)
    {
        try
        {
            checkCheckbox(Locator.id("approvedTermsOfUse"));
            if (null != text)
            {
                assertTextPresent(text);
            }
            if (doAgree)
                clickButton("Agree");

        }
        catch (NoSuchElementException e)
        {
            if (null != text) // terms were expected but the element was not found
                fail("Expected terms of use with text '" + text + "' but found no such element");
        }
    }

    @LogMethod
    public void ensureSignedInAsAdmin()
    {
        signOut();
        simpleSignIn();
    }

    private final void cleanup(boolean afterTest) throws TestTimeoutException
    {
        if(!ClassUtils.getAllInterfaces(getCurrentTestClass()).contains(ReadOnlyTest.class) || ((ReadOnlyTest)this).needsSetup())
            doCleanup(afterTest);
    }

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

            log("========= " + getClass().getSimpleName() + " cleanup complete =========");
        }
        finally
        {
            doTearDown();
        }
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

    public boolean isGuestModeTest()
    {
        return false;
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
        if (!isLocalServer())
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
                if (isElementPresent(Locators.labkeyError.containing("Active thread(s) may have objects in use:")))
                {
                    log("Pausing 10 seconds to wait for active thread");
                    sleep(10000);
                }
            }
            beginAt("/admin/memTracker.view?gc=1&clearCaches=1", 120000);
            if (!isTextPresent("In-Use Objects"))
                throw new IllegalStateException("Asserts must be enabled to track memory leaks; add -ea to your server VM params and restart or add -DmemCheck=false to your test VM params.");
            leakCount = getImageWithAltTextCount("expand/collapse");
        }

        if (leakCount > MAX_LEAK_LIMIT)
        {
            String leaks = Locator.name("leaks").findElement(getDriver()).getText();
            CRC32 crc = new CRC32();
            crc.update(leaks.getBytes(StandardCharsets.UTF_8));

            if (leakCRC != crc.getValue())
            {
                leakCRC = crc.getValue();
                getArtifactCollector().dumpHeap();
                fail(leakCount + " in-use objects exceeds allowed limit of " + MAX_LEAK_LIMIT + ".");
            }

            log("Found " + leakCount + " in-use objects.  They appear to be from a previous test.");
        }
        else
            log("Found " + leakCount + " in-use objects.  This is within the expected number of " + MAX_LEAK_LIMIT + ".");
    }

    public void checkErrors()
    {
        if (!isLocalServer())
            return;
        if ( isGuestModeTest() )
            return;
        beginAt("/admin/showErrorsSinceMark.view");

        assertTrue("There were server-side errors during the test run. Check labkey.log and/or labkey-errors.log for details.", isPageEmpty());
        log("No new errors found.");
        goToHome();         // Don't leave on an empty page
    }

    @LogMethod
    public void checkExpectedErrors(@LoggedParam int expectedErrors)
    {
        // Need to remember our location or the next test could start with a blank page
        pushLocation();
        beginAt("/admin/showErrorsSinceMark.view");

        String text = getBodyText();
        Pattern errorPattern = Pattern.compile("^ERROR", Pattern.MULTILINE);
        Matcher errorMatcher = errorPattern.matcher(text);
        int count = 0;
        while (errorMatcher.find())
        {
            count++;
        }
        assertEquals("Expected error count does not match actual count for this run.", expectedErrors, count);

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

        for (WebTestHelper.FolderIdentifier folderId : _containerHelper.getCreatedFolders())
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
        if (isScriptCheckEnabled() && getDriver() != null)
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

    /**
     * @param reuseSession true to have the Java API connection "hijack" the session from the Selenium browser window
     */
    public Connection createDefaultConnection(boolean reuseSession)
    {
        Connection connection = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        if (reuseSession)
        {
            Cookie cookie = getDriver().manage().getCookieNamed("JSESSIONID");
            if (cookie == null)
            {
                throw new IllegalStateException("No session cookie available to reuse.");
            }

            connection.addCookie(cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getExpiry(), cookie.isSecure());
        }

        return connection;
    }

    protected SelectRowsResponse executeSelectRowCommand(String schemaName, String queryName)
    {
        return executeSelectRowCommand(schemaName, queryName, null);
    }

    protected SelectRowsResponse executeSelectRowCommand(String schemaName, String queryName,  @Nullable List<Filter> filters)
    {
        return executeSelectRowCommand(schemaName, queryName, filters, false);
    }

    protected SelectRowsResponse executeSelectRowCommand(String schemaName, String queryName, @Nullable List<Filter> filters, boolean reuseSession)
    {
        return executeSelectRowCommand(schemaName, queryName, ContainerFilter.CurrentAndSubfolders, "/" + getProjectName(), filters, reuseSession);
    }

    protected SelectRowsResponse executeSelectRowCommand(String schemaName, String queryName, ContainerFilter containerFilter, String path, @Nullable List<Filter> filters, boolean reuseSession)
    {
        Connection cn = createDefaultConnection(reuseSession);
        SelectRowsCommand selectCmd = new SelectRowsCommand(schemaName, queryName);
        selectCmd.setMaxRows(-1);
        selectCmd.setContainerFilter(containerFilter);
        selectCmd.setColumns(Arrays.asList("*"));
        if (filters != null)
            selectCmd.setFilters(filters);

        SelectRowsResponse selectResp = null;

//        selectCmd.setQueryName(subQuery);
        try
        {
            selectResp = selectCmd.execute(cn, path);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return selectResp;
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
        getArtifactCollector().publishArtifact(saveTsv(TestProperties.getDumpDir(), "ActionCoverage"));
        popLocation();
    }

    @LogMethod
    protected void checkLinks()
    {
        if (isLinkCheckEnabled())
        {
            pauseJsErrorChecker();
            Crawler crawler = new Crawler(this, Runner.getTestSet().getCrawlerTimeout());
            crawler.addExcludedActions(getUncrawlableActions());
            crawler.setInjectionCheckEnabled(isInjectionCheckEnabled());
            crawler.crawlAllLinks();
            resumeJsErrorChecker();
        }
    }

    protected List<Crawler.ControllerActionId> getUncrawlableActions()
    {
        return Collections.emptyList();
    }

    private void writeActionStatistics(int totalActions, int coveredActions, Double actionCoveragePercent)
    {
        // TODO: Create static class for managing teamcity-info.xml file.
        File xmlFile = new File(TestFileUtils.getLabKeyRoot(), "teamcity-info.xml");
        try (Writer writer = new UTF8PrintWriter(xmlFile))
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

        try(Writer writer = new UTF8PrintWriter(tsvFile))
        {
            writer.write(contents);
            return tsvFile;
        }
        catch (IOException e)
        {
            e.printStackTrace(System.out);
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

            final String fullURL = getBaseURL() + relativeURL;

            Function<Void, Void> func = new Function<Void, Void>()
            {
                @Override
                public Void apply(Void o)
                {
                    getDriver().navigate().to(fullURL);
                    return null;
                }
            };
            long elapsedTime = applyAndWaitForPageToLoad(func, millis);
            logMessage += " [" + elapsedTime + " ms]";

            resumeJsErrorChecker();

            return elapsedTime;
        }
        finally
        {
            log(logMessage); // log after navigation to
        }
    }

    public long goToURL(final URL url, int milliseconds)
    {
        String logMessage = "Navigating to " + url.toString();
        try
        {
            pauseJsErrorChecker();
            Function<Void, Void> func = new Function<Void, Void>()
            {
                @Override
                public Void apply(Void o)
                {
                    getDriver().navigate().to(url);
                    return null;
                }
            };

            long elapsedTime = applyAndWaitForPageToLoad(func, milliseconds);
            logMessage += " [" + elapsedTime + " ms]";

            resumeJsErrorChecker();
            return elapsedTime;
        }
        finally
        {
            log(logMessage);
        }
    }

    public void navigateToQuery(String schemaName, String queryName)
    {
        RelativeUrl queryURL = new RelativeUrl("query", "executequery");
        queryURL.setContainerPath(getCurrentContainerPath());
        queryURL.addParameter("schemaName", schemaName);
        queryURL.addParameter("query.queryName", queryName);

        queryURL.navigate(this);
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

    public String getCurrentContainerPath()
    {
        return (String)executeScript("return LABKEY.container.path;");
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

    public int dismissAllAlerts()
    {
        int alertCount = 0;
        while (isAlertPresent()){
            Alert alert = getDriver().switchTo().alert();
            log("Found unexpected alert: " + alert.getText());
            alert.dismiss();
            alertCount++;
        }
        return alertCount;
    }

    public int acceptAllAlerts()
    {
        int alertCount = 0;
        while (isAlertPresent()){
            Alert alert = getDriver().switchTo().alert();
            log("Found unexpected alert: " + alert.getText());
            alert.accept();
            alertCount++;
        }
        return alertCount;
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

    public enum SeleniumEvent
    {blur,change,mousedown,mouseup,click,reset,select,submit,abort,error,load,mouseout,mouseover,unload,keyup,focus}

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

    public void hoverProjectBar()
    {
        Locator.IdLocator projectBar = Locator.id("projectBar");
        waitForElement(projectBar);
        waitForHoverNavigationReady();
        click(projectBar);
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
        {
            acceptTermsOfUse(null, true);
            waitForElement(Locator.id("folderBar").withText(project));
        }
    }

    public void hoverFolderBar()
    {
        Locator.XPathLocator folderBar = Locator.id("folderBar").withText();
        waitForElement(folderBar);
        waitForFolderNavigationReady();
        click(folderBar);
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
        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                return (Boolean) executeScript("if (HoverNavigation._folder.webPartName == 'foldernav') return true; else return false;");
            }
        }, "HoverNavigation._folder not ready", WAIT_FOR_JAVASCRIPT);
    }

    public void waitForHoverNavigationReady()
    {
        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                return (Boolean) executeScript("if (window.HoverNavigation) return true; else return false;");
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
     * @deprecated Use {@link org.labkey.test.util.AbstractContainerHelper#deleteProject(String, boolean)}
     */
    @Deprecated
    public void deleteProject(String project, boolean failIfFail) throws TestTimeoutException
    {
        _containerHelper.deleteProject(project, failIfFail);
    }

    @LogMethod (quiet = true)
    public void disableMiniProfiler()
    {
        Connection cn = createDefaultConnection(true);
        Command command = new Command("mini-profiler", "setEnabled");
        command.setParameters(new HashMap<String, Object>(Maps.of("enabled", "false")));
        try
        {
            command.execute(cn, null);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to disable mini-profiler", e);
        }
    }

    @LogMethod (quiet = true)
    public void enableEmailRecorder()
    {
        try
        {
            assertEquals("Failed to enable email recording", 200, getHttpGetResponse(WebTestHelper.getBaseURL() + "/dumbster/setRecordEmail.view?record=true", PasswordUtil.getUsername(), PasswordUtil.getPassword()));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to enable email recorder", e);
        }
    }

    @LogMethod(quiet = true)
    public void enableSecondaryAuthentication()
    {
        Connection cn = createDefaultConnection(true);
        Command command = new Command("login", "enable");
        command.setParameters(new HashMap<String, Object>(Maps.of("provider", "Test Secondary Authentication")));
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
        command.setParameters(new HashMap<String, Object>(Maps.of("provider", "Test Secondary Authentication")));
        try
        {
            command.execute(cn, null);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to disable Secondary Authentication", e);
        }
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
        assertTrue("Page title: '" + title + "' doesn't contain '" + match + "'", title.contains(match));
    }

    public void assertNoLabKeyErrors()
    {
        assertElementNotPresent(Locator.xpath("//div[@class='labkey-error']"));
        assertElementNotPresent(Locator.xpath("//font[@class='labkey-error']"));
    }

    public void assertLabKeyErrorPresent()
    {
        assertTrue("No errors found", isElementPresent(Locator.xpath("//div[@class='labkey-error']")) ||
                isElementPresent(Locator.xpath("//font[@class='labkey-error']")));
    }

    public static String encodeText(String unencodedText)
    {
        return unencodedText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public boolean isTextPresent(String... texts)
    {
        final MutableBoolean present = new MutableBoolean(true);

        TextSearcher.TextHandler handler = new TextSearcher.TextHandler()
        {
            @Override
            public boolean handle(String htmlSource, String text)
            {
                // Not found... stop enumerating and return false
                if (!htmlSource.contains(text))
                    present.setFalse();

                return present.getValue();
            }
        };
        TextSearcher searcher = new TextSearcher(this);
        searcher.searchForTexts(handler, texts);

        return present.getValue();
    }

    public List<String> getMissingTexts(TextSearcher searcher, String... texts)
    {
        final List<String> missingTexts = new ArrayList<>();

        TextSearcher.TextHandler handler = new TextSearcher.TextHandler(){
            @Override
            public boolean handle(String htmlSource, String text)
            {
                if (!htmlSource.contains(text))
                    missingTexts.add(text);

                return true;
            }
        };

        searcher.searchForTexts(handler, texts);

        return missingTexts;
    }

    public String getText(Locator elementLocator)
    {
        WebElement el = elementLocator.findElement(getDriver());
        return el.getText();
    }

    public List<String> getTexts(List<WebElement> elements)
    {
        List<String> texts = new ArrayList<>();

        for (WebElement el : elements)
        {
            texts.add(el.getText());
        }

        return texts;
    }

    public WebElement getElement(Locator locator)
    {
        return locator.findElement(getDriver());
    }

    /**
     * Verifies that all the strings are present in the page html source
     */
    public void assertTextPresent(String... texts)
    {
        assertTextPresent(new TextSearcher(this), texts);
    }

    public void assertTextPresent(TextSearcher searcher, String... texts)
    {
        List<String> missingTexts = getMissingTexts(searcher, texts);

        if (!missingTexts.isEmpty())
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
        TextSearcher searcher = new TextSearcher(this);

        searcher.setSearchTransformer(new Function<String, String> ()
        {
            @Override
            public String apply(String text)
            {
                return encodeText(text).toLowerCase();
            }
        });

        searcher.setSourceTransformer(new Function<String, String>()
        {
            @Override
            public String apply(String text)
            {
                return text.toLowerCase();
            }
        });

        assertTextPresent(searcher, texts);
    }

    /**
     * Verifies that at least one of the strings is present in the page html source
     */
    public void assertOneOfTheseTextsPresent(String... texts)
    {
        final MutableBoolean found = new MutableBoolean(false);

        TextSearcher.TextHandler handler = new TextSearcher.TextHandler(){
            @Override
            public boolean handle(String htmlSource, String text)
            {
                if (htmlSource.contains(text))
                    found.setTrue();

                return !found.getValue();
            }
        };
        TextSearcher searcher = new TextSearcher(this);
        searcher.searchForTexts(handler, texts);

        if (!found.getValue())
            fail("Did not find any of the following values on current page " + Arrays.toString(texts));
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
        assertEquals("Text '" + text + "' was not present the correct number of times", amount, countText(text));
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

    public void assertTextNotPresent(TextSearcher searcher, String... texts)
    {
        // Number of characters on either side of found text to include in error message
        final int RANGE = 20;

        TextSearcher.TextHandler handler = new TextSearcher.TextHandler()
        {
            @Override
            public boolean handle(String htmlSource, String text)
            {
                int position = htmlSource.indexOf(text);

                if (position > -1)
                {
                    int prefixStart = Math.max(0, position - RANGE);
                    int suffixEnd = Math.min(htmlSource.length() - 1, position + text.length() + RANGE);
                    String prefix = htmlSource.substring(prefixStart, position);
                    String suffix = htmlSource.substring(position + text.length(), suffixEnd);

                    fail("Text '" + text + "' was present: " + prefix + "[" + text + "]" + suffix);
                }

                return true;
            }
        };

        searcher.searchForTexts(handler, texts);
    }

    public void assertTextNotPresent(String... texts)
    {
        TextSearcher searcher = new TextSearcher(this);
        searcher.setSearchTransformer(new Function<String, String>()
        {
            @Override
            public String apply(String text)
            {
                return encodeText(text).replace("&nbsp;", " ");
            }
        });

        assertTextNotPresent(searcher, texts);
    }

    public String getTextInTable(String dataRegion, int row, int column)
    {
        String id = Locator.xq(dataRegion);
        return getDriver().findElement(By.xpath("//table[@id=" + id + "]/tbody/tr[" + row + "]/td[" + column + "]")).getText();
    }

    public void assertTextAtPlaceInTable(String textToCheck, String dataRegion, int row, int column)
    {
        assertEquals(textToCheck + " is not at that place in the table", textToCheck, getTextInTable(dataRegion, row, column));
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
                return s + " occured out of order; came before " + previousString;
            previousIndex = index;
            previousString = s;
        }
        return null;
    }

    // Searches only the displayed text in the body of the page, not the HTML source.
    public void assertTextPresentInThisOrder(String... text)
    {
        String success = isPresentInThisOrder(text);
        assertTrue(success, success == null);
    }

    public void assertTextBefore(String text1, String text2)
    {
        assertTextPresentInThisOrder(text1, text2);
    }

    private Boolean _preppedForPageLoad = false;

    /**
     * @deprecated Use {@link #applyAndWaitForPageToLoad(com.google.common.base.Function)}
     * To be made private
     */
    @Deprecated
    public void prepForPageLoad()
    {
        executeScript("window.preppedForPageLoadMarker = true;");
        _preppedForPageLoad = true;
    }

    /**
     * @deprecated Use {@link #applyAndWaitForPageToLoad(com.google.common.base.Function, int)}
     * To be made private
     */
    @Deprecated
    public void waitForPageToLoad(int millis)
    {
        if (!_preppedForPageLoad) throw new IllegalStateException("Please call prepForPageLoad() before performing the action that would trigger the expected page load.");
        _testTimeout = true;
        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                // Wait for marker to disappear
                return (Boolean) executeScript("try {if(window.preppedForPageLoadMarker) return false; else return true;}" +
                        "catch(e) {return false;}");
            }
        }, "Page failed to load", millis);
        _testTimeout = false;
//        waitForExtOnReady(); // TODO: Removing temporarily. This might be causing more timeouts than its worth.
        _preppedForPageLoad = false;
    }

    public void waitForExtOnReady()
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

    /**
     * @deprecated Use {@link #applyAndWaitForPageToLoad(com.google.common.base.Function)}
     * To be made private
     */
    @Deprecated
    public void waitForPageToLoad()
    {
        waitForPageToLoad(defaultWaitForPage);
    }

    public long applyAndWaitForPageToLoad(Function<Void, Void> func)
    {
        return applyAndWaitForPageToLoad(func, defaultWaitForPage);
    }

    public long applyAndWaitForPageToLoad(Function<Void, Void> func, final int msWait)
    {
        long startTime = System.currentTimeMillis();

        if (msWait > 0)
        {
            getDriver().manage().timeouts().pageLoadTimeout(msWait, TimeUnit.MILLISECONDS);
            prepForPageLoad();
        }

        func.apply(null);

        if (msWait > 0)
        {
            waitForPageToLoad(msWait);
            getDriver().manage().timeouts().pageLoadTimeout(defaultWaitForPage, TimeUnit.MILLISECONDS);
        }

        return System.currentTimeMillis() - startTime;
    }

    public void waitForExtReady()
    {
        waitForElement(Locator.id("seleniumExtReady"), defaultWaitForPage);
    }

    /**
     * Wait for Checker.check() to return true
     * @param wait milliseconds
     * @return false if Checker.check() doesn't return true within 'wait' ms
     */
    public boolean waitFor(Checker checker, int wait)
    {
        Long startTime = System.currentTimeMillis();
        do
        {
            if( checker.check() )
                return true;
            sleep(100);
        } while ((System.currentTimeMillis() - startTime) < wait);

        return false;
    }

    public void waitFor(Checker checker, String failMessage, int wait)
    {
        if (!waitFor(checker, wait))
        {
            _testTimeout = true;
            fail(failMessage + " ["+wait+"ms]");
        }
    }

    public void waitForAlert(String alertText, int wait)
    {
        waitFor(new Checker(){public boolean check(){return isAlertPresent();}}, "No alert appeared.", wait);
        assertAlert(alertText);
    }

    protected void setSelectedFields(String containerPath, String schema, String query, String viewName, String[] fields)
    {
        pushLocation();
        beginAt("/query" + containerPath + "/internalNewView.view");
        setFormElement(Locator.name("ff_schemaName"), schema);
        setFormElement(Locator.name("ff_queryName"), query);
        if (viewName != null)
            setFormElement(Locator.name("ff_viewName"), viewName);
        clickButton("Create");
        StringBuilder strFields = new StringBuilder(fields[0]);
        for (int i = 1; i < fields.length; i ++)
        {
            strFields.append("&");
            strFields.append(fields[i]);
        }
        setFormElement(Locator.name("ff_columnList"), strFields.toString());
        clickButton("Save");
        popLocation();
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionExportHelper}
     */
    @Deprecated
    protected void clickExportToText()
    {
        clickButton("Export", 0);
        shortWait().until(LabKeyExpectedConditions.dataRegionPanelIsExpanded(DataRegionTable.findDataRegion(this)));
        _extHelper.clickSideTab("Text");
        clickButton("Export to Text");
    }

    protected void selectSecurityGroupsExport()
    {
        checkCheckbox(Locator.checkboxByNameAndValue("types", "Project-level groups and members"));
    }

    protected void selectRoleAssignmentsExport()
    {
        checkCheckbox(Locator.checkboxByNameAndValue("types", "Role assignments for users and groups"));
    }

    @LogMethod
    protected void prepareForFolderExport(@Nullable String folderName, boolean exportSecurityGroups, boolean exportRoleAssignments, boolean includeSubfolders, int locationIndex)
    {

        if (folderName != null)
            clickFolder(folderName);
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Export"));
        if (exportSecurityGroups)
            selectSecurityGroupsExport();
        if (exportRoleAssignments)
            selectRoleAssignmentsExport();
        if (includeSubfolders)
            click(Locator.name("includeSubfolders"));
        checkRadioButton(Locator.name("location").index(locationIndex)); // first locator with this name is "Pipeline root export directory, as individual files
    }

    @LogMethod
    protected void exportFolderAsIndividualFiles(String folderName, boolean exportSecurityGroups, boolean exportRoleAssignments, boolean includeSubfolders)
    {
        // first locator with this name is "Pipeline root export directory, as individual files
        prepareForFolderExport(folderName, exportSecurityGroups, exportRoleAssignments, includeSubfolders, 0);
        clickButton("Export");
    }


    protected void exportFolderAsZip(boolean exportSecurityGroups, boolean exportRoleAssignments)
    {
        prepareForFolderExport(null, exportSecurityGroups, exportRoleAssignments, false, 1);
        clickButton("Export");
    }

    protected File exportFolderToBrowserAsZip()
    {
        prepareForFolderExport(null, false, false, false, 2);
        return clickAndWaitForDownload(Locator.extButton("Export"));
    }

    public File clickAndWaitForDownload(Locator elementToClick)
    {
        return clickAndWaitForDownload(elementToClick, 1)[0];
    }

    public File[] clickAndWaitForDownload(final Locator elementToClick, final int expectedFileCount)
    {
        Function<Void, Void> clicker = new Function<Void, Void>()
        {
            @Override
            public Void apply(Void o)
            {
                click(elementToClick);
                return null;
            }
        };

        return applyAndWaitForDownload(clicker, expectedFileCount);
    }

    public File applyAndWaitForDownload(Function<Void, Void> func)
    {
        return applyAndWaitForDownload(func, 1)[0];
    }

    public File[] applyAndWaitForDownload(Function<Void, Void> func, final int expectedFileCount)
    {
        final File downloadDir = getDownloadDir();
        File[] existingFilesArray = downloadDir.listFiles();
        final List<File> existingFiles;

        if (existingFilesArray != null)
            existingFiles = Arrays.asList(existingFilesArray);
        else
            existingFiles = new ArrayList<>();

        func.apply(null);

        final FileFilter tempFilesFilter = new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                return file.getName().contains(".part") ||
                        file.getName().contains(".tmp") ||
                        file.getName().contains(".crdownload");
            }
        };
        final FileFilter newFileFilter = new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                return !existingFiles.contains(file) && !tempFilesFilter.accept(file) && file.length() > 0;
            }
        };

        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                final File[] files = downloadDir.listFiles(newFileFilter);
                return files != null && files.length >= expectedFileCount;
            }
        }, "File(s) did not appear in download dir", WAIT_FOR_PAGE);

        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                final File[] files = downloadDir.listFiles(tempFilesFilter);
                return files != null && files.length == 0;
            }
        }, "Temp files remain after download", WAIT_FOR_JAVASCRIPT);

        File[] newFiles = downloadDir.listFiles(newFileFilter);
        assertEquals("Wrong number of files downloaded to " + downloadDir.toString(), expectedFileCount, newFiles.length);

        for (File newFile : newFiles)
        {
            log("File downloaded: " + newFile.getName());
        }

        if (BROWSER_TYPE == BrowserType.FIREFOX)
            Locator.css("body").findElement(getDriver()).sendKeys(Keys.ESCAPE); // Dismiss download dialog

        return newFiles;
    }

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
            Ext4FieldRef ref = _ext4Helper.queryOne(query, Ext4FieldRef.class);
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

    public static abstract class Checker
    {
        public abstract boolean check();
    }

    public WebElement waitForElement(final Locator locator)
    {
        return waitForElement(locator, WAIT_FOR_JAVASCRIPT);
    }

    public WebElement waitForElement(final Locator locator, int wait)
    {
        return locator.waitForElement(getDriver(), wait);
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

    public void waitForAnyElement(final Locator... locators)
    {
        if (locators.length > 0)
        {
            waitFor(new Checker()
            {
                @Override
                public boolean check()
                {
                    for (Locator loc : locators)
                    {
                        if (isElementPresent(loc))
                            return true;
                    }
                    return false;
                }
            }, "No Element Appeared", WAIT_FOR_JAVASCRIPT);
        }
    }

    public void waitForAllElements(final Locator... locators)
    {
        if (locators.length > 0)
        {
            waitFor(new Checker()
            {
                @Override
                public boolean check()
                {
                    for (Locator loc : locators)
                    {
                        if (!isElementPresent(loc))
                            return false;
                    }
                    return true;
                }
            }, "No Element Appeared", WAIT_FOR_JAVASCRIPT);
        }
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

    public void waitForElementText(final Locator locator, final String text)
    {
        waitForElementText(locator, text, WAIT_FOR_JAVASCRIPT);
    }

    public void waitForElementText(final Locator locator, final String text, int wait)
    {
        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                String elemText = getText(locator);
                return elemText.equals(text);
            }
        }, "Expected '" + text + "' in element '" + locator + "'", wait);
    }

    public void waitForElementToDisappear(final Locator locator)
    {
        waitForElementToDisappear(locator, WAIT_FOR_JAVASCRIPT);
    }

    public void waitForElementToDisappear(final Locator locator, int wait)
    {
        locator.waitForElementToDisappear(getDriver(), wait);
    }

    public void waitForElementToDisappearWithRefresh(Locator loc, int wait)
    {
        long startTime = System.currentTimeMillis();

        do
        {
            if(!isElementPresent(loc))
                return;
            refresh();
        }while(System.currentTimeMillis() - startTime < wait);

        waitForElementToDisappear(loc, 1000);
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

    public void waitForTextWithRefresh(int wait, String... text)
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
        assertTextPresent(text);
    }

    public void waitForText(final String... text)
    {
        waitForText(WAIT_FOR_JAVASCRIPT, text);
    }

    public void waitForText(int wait, final String... text)
    {
        waitFor(new Checker()
        {
            public boolean check()
            {
                return isTextPresent(text);
            }
        }, wait);
        assertTextPresent(text);
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
    @Deprecated public void submit(final WebElement form)
    {
        Function<Void, Void> func = new Function<Void, Void>()
        {
            @Override
            public Void apply(Void o)
            {
                executeScript("arguments[0].submit()", form);
                return null;
            }
        };

        applyAndWaitForPageToLoad(func);
    }

    public boolean isElementPresent(Locator loc)
    {
        return loc.findElements(getDriver()).size() > 0;
    }

    public void assertElementPresent(Locator loc)
    {
        assertTrue("Element is not present: " + loc.getLoggableDescription(), isElementPresent(loc));
    }

    public void assertElementPresent(Locator loc, int amount)
    {
        assertElementPresent("Element '" + loc + "' is not present " + amount + " times", loc, amount);
    }

    public void assertElementPresent(String message, Locator loc, int amount)
    {
        assertEquals(message, amount, getElementCount(loc));
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
        List<String> values = new ArrayList<>();
        for (WebElement selectedOption : selectedOptions)
            values.add(selectedOption.getAttribute("value"));
        return values;
    }

    public List<String> getSelectOptions(Locator loc)
    {
        Select select = new Select(loc.findElement(getDriver()));
        List<WebElement> selectOptions = select.getOptions();
        return getTexts(selectOptions);
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

    public void assertAtUserUserLacksPermissionPage()
    {
        assertTextPresent(PERMISSION_ERROR);
        assertTitleEquals("403: Error Page -- User does not have permission to perform this operation");
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

    public void clickAt(Locator l, int xCoord, int yCoord)
    {
        clickAt(l, xCoord, yCoord, WAIT_FOR_PAGE);
    }

    public void clickAt(Locator l, int xCoord, int yCoord, int pageTimeout)
    {
        WebElement el = l.waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        clickAt(el, xCoord, yCoord, pageTimeout);
    }

    public void clickAt(final WebElement el, final int xCoord, final int yCoord, int pageTimeout)
    {
        Function<Void, Void> func = new Function<Void, Void>()
        {
            @Override
            public Void apply(Void o)
            {
                Actions builder = new Actions(getDriver());
                builder.moveToElement(el, xCoord, yCoord)
                        .click()
                        .build()
                        .perform();
                return null;
            }
        };

        applyAndWaitForPageToLoad(func, pageTimeout);
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
        try
        {
            el = l.findElement(getDriver());
            clickAndWait(el, pageTimeoutMs);
        }
        catch (StaleElementReferenceException e)
        {
            // Locator.findElement likely didn't return the element we wanted due to timing problem. Wait and try again.
            // Ideally we'd decorate WebElement to know its locator and encapsulate the retry.
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException ignored) {}
            el = l.findElement(getDriver());
            clickAndWait(el, pageTimeoutMs);
        }
    }

    public void clickAndWait(final WebElement el, int pageTimeoutMs)
    {
        Function<Void, Void> func = new Function<Void, Void>()
        {
            @Override
            public Void apply(Void o)
            {
                try
                {
                    try
                    {
                        el.click();
                    }
                    catch (ElementNotVisibleException tryAgain)
                    {
                        scrollIntoView(el);
                        shortWait().until(ExpectedConditions.elementToBeClickable(el));
                        el.click();
                    }

                }
                catch (WebDriverException tryAgain)
                {
                    if (tryAgain.getMessage() != null && tryAgain.getMessage().contains("Other element would receive the click"))
                    {
                        try
                        {
                            Thread.sleep(2500);
                        }
                        catch (InterruptedException ignored) {}
                        el.click();
                    }
                    else
                    {
                        throw tryAgain;
                    }
                }
                return null;
            }
        };

        applyAndWaitForPageToLoad(func, pageTimeoutMs);

        if(pageTimeoutMs==WAIT_FOR_EXT_MASK_TO_APPEAR)
            _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);
        else if(pageTimeoutMs==WAIT_FOR_EXT_MASK_TO_DISSAPEAR)
            _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
    }

    public void doubleClick(Locator l)
    {
        doubleClickAndWait(l, 0);
    }

    public void doubleClickAndWait(final Locator l, int millis)
    {
        Function<Void, Void> func = new Function<Void, Void>()
        {
            @Override
            public Void apply(Void o)
            {
                Actions action = new Actions(getDriver());
                action.doubleClick(l.findElement(getDriver())).perform();
                return null;
            }
        };

        applyAndWaitForPageToLoad(func, millis);
    }

    public void selectFolderTreeItem(String folderName)
    {
        click(Locator.permissionsTreeNode(folderName));
    }

    public void mouseOver(Locator l)
    {
        WebElement el = l.findElement(getDriver());

        Actions builder = new Actions(getDriver());
        builder.moveToElement(el).build().perform();
    }

    public int getElementIndex(Locator.XPathLocator l)
    {
        return getElementCount(l.child("preceding-sibling::*"));
    }

    public int getElementIndex(WebElement el)
    {
        return Locator.xpath("preceding-sibling::*").findElements(el).size();
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
        assertTrue("Tab not selected: " + caption, isElementPresent(Locator.xpath("//li[contains(@class, labkey-tab-active)]/a[text() = '" + caption + "']")));
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
        return getTableCellText(Locator.xpath("//table[@id=" + Locator.xq(tableId) + "]"), row, column);
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

        getArtifactCollector().addArtifactLocation(new File(rootPath));

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

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#getColumnDataAsText(int)}
     */
    @Deprecated
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

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#getFullColumnValues(String...)}
     */
    @Deprecated
    public List<List<String>> getColumnValues(String tableName, String... columnNames)
    {
        DataRegionTable table = new DataRegionTable(tableName, this);
        return table.getFullColumnValues(columnNames);
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
        Locator.XPathLocator locator = Locator.button(text).index(index);
        if (isElementPresent(locator))
            return locator;

        // check for normal labkey submit button:
        locator = Locator.lkButton(text).index(index);
        if (isElementPresent(locator))
            return locator;

        // check for Ext button:
        locator = Locator.extButton(text).index(index);
        if (isElementPresent(locator))
            return locator;

        // check for Ext 4 button:
        locator = Ext4Helper.Locators.ext4Button(text).index(index);
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
        locator = Locator.lkButton(text);
        if (isElementPresent(locator))
            return locator;

        // check for Ext button:
        locator = Locator.extButton(text);
        if (isElementPresent(locator))
            return locator;

        // check for Ext 4 button:
        locator = Ext4Helper.Locators.ext4Button(text);
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
        locator = Locator.lkButtonContainingText(text);
        if (isElementPresent(locator))
            return locator;

        // check for Ext button:
        locator = Locator.extButtonContainingText(text);
        if (isElementPresent(locator))
            return locator;

        // check for Ext 4 button:
        locator = Ext4Helper.Locators.ext4ButtonContainingText(text);
        if (isElementPresent(locator))
            return locator;

        return null;
    }


    /**
     * Wait for a button to appear, click it, then waits for the page to load.
     * Use clickButton(text, 0) to click a button and continue immediately.
     */
    public void clickButton(String text)
    {
        clickButton(text, defaultWaitForPage);
    }

    /**
     * Wait for a button to appear, click it, then waits for the text to appear.
     */
    public void clickButton(String text, String waitForText)
    {
        clickButton(text, 0);
        waitForText(waitForText);
    }

    /**
     * Wait for a button to appear, click it, then wait for <code>waitMillis</code> for the page to load.
     */
    public void clickButton(final String text, int waitMillis)
    {
        // Wait for button to appear
        String failMessage = "Button with text '" + text + "' did not appear";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return null != getButtonLocator(text);
            }
        }, failMessage, WAIT_FOR_JAVASCRIPT);

        // Click and wait for page to load
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
        waitForText(defaultWaitForPage, textShouldAppearAfterLoading);
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
        try
        {
            clickAndWait(el, waitForPageToLoad);
        }
        catch (StaleElementReferenceException e)
        {
            // Locator.findElement likely didn't return the element we wanted due to timing problem. Wait and try again.
            // Ideally we'd decorate WebElement to know its locator and encapsulate the retry.
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException ignored) {}
            el = l.findElement(getDriver());
            clickAndWait(el, waitForPageToLoad);
        }
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
        return (WebTestHelper.getContextPath() + "/" + controller + folderPath + "/" + localAddress);
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
     * @deprecated Use explicit Locator: {@link #setFormElement(Locator, String)}
     */
    @Deprecated public void setFormElement(String name, String text)
    {
        if(getDriver().findElements(By.id(name)).size() > 0)
        {
            log("DEPRECATED: Form element has id: \"" + name + "\". Use Locator.id");
            setFormElement(Locator.id(name), text);
        }
        else
        {
            log("DEPRECATED: Form element is named: \"" + name + "\". Use Locator.name");
            setFormElement(Locator.name(name), text);
        }
    }

    public void setFormElement(Locator l, String text)
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

        fireEvent(el, SeleniumEvent.focus);
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

    public void setFormElement(Locator loc, File file)
    {
        WebElement el = loc.findElement(getDriver());
        setFormElement(el, file);
    }

    public void setFormElement(WebElement el, File file)
    {
        assertTrue("File not found: " + file.toString(), file.exists());
        String cssString = "";
        if (!el.isDisplayed())
        {
            cssString = el.getAttribute("class");
            log("Remove class so that WebDriver can interact with concealed form element");
            executeScript("arguments[0].setAttribute('class', '');", el);
        }

        el.sendKeys(FileUtil.getAbsoluteCaseSensitiveFile(file).getAbsolutePath());

        if (!cssString.isEmpty())
        {
            executeScript("arguments[0].setAttribute('class', arguments[1]);", el, cssString);
        }
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

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#setSort(String, SortDirection)}
     */
    @Deprecated
    public void setSort(String regionName, String columnName, SortDirection direction)
    {
        setSort(regionName, columnName, direction, defaultWaitForPage);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#clearSort(String)}
     */
    @Deprecated
    public void clearSort(String regionName, String columnName)
    {
        clearSort(regionName, columnName, defaultWaitForPage);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#clearSort(String)}
     */
    @Deprecated
    public void clearSort(String regionName, String columnName, int wait)
    {
        log("Clearing sort in " + regionName + " for " + columnName);
        final Locator menuLoc = DataRegionTable.Locators.columnHeader(regionName, columnName);
        waitForElement(menuLoc, WAIT_FOR_JAVASCRIPT);
        Function<Void, Void> func = new Function<Void, Void>()
        {
            @Override
            public Void apply(Void o)
            {
                _ext4Helper.clickExt4MenuButton(false, menuLoc, false, "Clear Sort");
                return null;
            }
        };

        applyAndWaitForPageToLoad(func, wait);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#setSort(String, SortDirection)}
     */
    @Deprecated
    public void setSort(String regionName, String columnName, final SortDirection direction, int wait)
    {
        log("Setting sort in " + regionName + " for " + columnName + " to " + direction.toString());
        final Locator menuLoc = DataRegionTable.Locators.columnHeader(regionName, columnName);
        waitForElement(menuLoc, WAIT_FOR_JAVASCRIPT);
        Function<Void, Void> func = new Function<Void, Void>()
        {
            @Override
            public Void apply(Void o)
            {
                _ext4Helper.clickExt4MenuButton(false, menuLoc, false, "Sort " + (direction.equals(SortDirection.ASC) ? "Ascending" : "Descending"));
                return null;
            }
        };

        applyAndWaitForPageToLoad(func, wait);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#setFilter(String, String, String)}
     */
    @Deprecated
    public void setFilter(String regionName, String columnName, String filterType)
    {
        setUpFilter(regionName, columnName, filterType, null);
        clickButton("OK");
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#setFilter(String, String, String)}
     */
    @Deprecated
    public void setFilter(String regionName, String columnName, String filterType, String filter)
    {
        setFilter(regionName, columnName, filterType, filter, WAIT_FOR_PAGE);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#setFilter(String, String, String, int)}
     */
    @Deprecated
    public void setFilter(String regionName, String columnName, String filterType, String filter, int waitMillis)
    {
        setUpFilter(regionName, columnName, filterType, filter);
        clickButton("OK", waitMillis);
    }

    public void setUpFilter(String regionName, String columnName, String filterType, @Nullable String filter)
    {
        setUpFilter(regionName, columnName, filterType, filter, null, null);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#setFilter(String, String, String, int)}
     */
    @Deprecated
    public void setFilterAndWait(String regionName, String columnName, String filterType, String filter, int milliSeconds)
    {
        setUpFilter(regionName, columnName, filterType, filter);
        clickButton("OK", milliSeconds);
    }

    /**
     * TODO: Move to {@link org.labkey.test.util.DataRegionTable}
     */
    public void setUpFilter(String regionName, String columnName, String filter1Type, @Nullable String filter1, @Nullable String filter2Type, @Nullable String filter2)
    {
        String log = "Setting filter in " + regionName + " for " + columnName + " to " + filter1Type.toLowerCase() + (filter1 != null ? " " + filter1 : "");
        if (filter2Type != null)
        {
            log += " and " + filter2Type.toLowerCase() + (filter2 != null ? " " + filter2 : "");
        }
        log(log);

        openFilter(regionName, columnName);

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

    /**
     * TODO: Move to {@link org.labkey.test.util.DataRegionTable}
     */
    public void setFilter(String regionName, String columnName, String filter1Type, String filter1, String filter2Type, String filter2)
    {
        setUpFilter(regionName, columnName, filter1Type, filter1, filter2Type, filter2);
        clickButton("OK");
    }

    /**
     * TODO: Move to {@link org.labkey.test.util.DataRegionTable}
     */
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

        openFilter(regionName, columnName);
        String columnLabel = getText(DataRegionTable.Locators.columnHeader(regionName, columnName));

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

    /**
     * TODO: Move to {@link org.labkey.test.util.DataRegionTable}
     */
    public void setFacetedFilter(String regionName, String columnName, String... values)
    {
        setUpFacetedFilter(regionName, columnName, values);
        clickButton("OK");
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#openFilterDialog(String)}
     */
    @Deprecated
    public void openFilter(String regionName, String columnName)
    {
        (new DataRegionTable(regionName, this)).openFilterDialog(columnName);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#clearFilter(String)}
     */
    @Deprecated
    public void clearFilter(String regionName, String columnName)
    {
        clearFilter(regionName, columnName, WAIT_FOR_PAGE);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#clearFilter(String, int)}
     */
    @Deprecated
    public void clearFilter(final String regionName, final String columnName, int waitForPageLoad)
    {
        log("Clearing filter in " + regionName + " for " + columnName);

        Function<Void, Void> func = new Function<Void, Void>()
        {
            @Override
            public Void apply(Void o)
            {
                _ext4Helper.clickExt4MenuButton(false, DataRegionTable.Locators.columnHeader(regionName, columnName), false, "Clear Filter");
                return null;
            }
        };

        applyAndWaitForPageToLoad(func, waitForPageLoad);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#clearAllFilters(String)}
     */
    @Deprecated
    public void clearAllFilters(String regionName, String columnName)
    {
        log("Clearing filter in " + regionName + " for " + columnName);
        openFilter(regionName, columnName);
        clickButton("CLEAR ALL FILTERS");
    }

    public String getPropertyXPath(String propertyHeading)
    {
        return "//td[text() = '" + propertyHeading + "']/../..";
    }

    public String getPropertyXPathContains(String propertyHeading)
    {
        return "//td[contains(text(), '" + propertyHeading + "')]/../..";
    }

    public int getElementCount(Locator locator)
    {
        return locator.findElements(getDriver()).size();
    }

    /**
     *
     * @param feature  the enable link will have an id of the form "labkey-experimental-feature-[feature]
     */
    public void enableExperimentalFeature(String feature)
    {
        log("Attempting to enable feature: " + feature);
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("experimental features"));

        String xpath = "//div[div[text()='" + feature + "']]/a";
        if (!isElementPresent(Locator.xpath(xpath)))
            fail("No such feature found: " + feature);
        else
        {
            Locator link = Locator.xpath(xpath + "[text()='Enable']");
            if(isElementPresent(link))
            {
                click(link);
                log("Enable link found, enabling " + feature);
            }
            else
            {
                log("Link not found, presumed enabled: " + feature);
            }
        }
    }

    /**
     * From the assay design page, add a field with the given name, label, and type
     */
    public void addRunField(String name, String label, ListHelper.ListColumnType type)
    {
        String xpath = ("//input[starts-with(@name, 'ff_name");
        int newFieldIndex = getElementCount(Locator.xpath(xpath + "')]"));
        clickButtonByIndex("Add Field", 1, 0);
        _listHelper.setColumnName(newFieldIndex, name);
        _listHelper.setColumnLabel(newFieldIndex, label);
        _listHelper.setColumnType(newFieldIndex, type);
    }

    public void assertButtonPresent(String buttonText)
    {
        assertTrue("Button '" + buttonText + "' was not present", isButtonPresent(buttonText));
    }

    public void assertButtonNotPresent(String buttonText)
    {
        assertFalse("Button '" + buttonText + "' was present", isButtonPresent(buttonText));
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#pageFirst()}
     * @param dataRegionName
     */
    @Deprecated
    public void dataRegionPageFirst(String dataRegionName)
    {
        log("Clicking page first on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "First Page");
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#pageLast()}
     */
    @Deprecated
    public void dataRegionPageLast(String dataRegionName)
    {
        log("Clicking page last on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "Last Page");
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#pageNext()}
     */
    @Deprecated
    public void dataRegionPageNext(String dataRegionName)
    {
        log("Clicking page next on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "Next Page");
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#pagePrev()}
     */
    @Deprecated
    public void dataRegionPagePrev(String dataRegionName)
    {
        log("Clicking page previous on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "Previous Page");
    }

    /**
     * @deprecated Move to {@link org.labkey.test.util.DataRegionTable}
     */
    @Deprecated
    private void clickDataRegionPageLink(String dataRegionName, String title)
    {
        String id = Locator.xq("dataregion_header_" + dataRegionName);
        clickAndWait(Locator.xpath("//table[@id=" + id + "]//div/a[@title='" + title + "']"));
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#getDataRowCount()}
     */
    @Deprecated
    public int getDataRegionRowCount(String dataRegionName)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        return Locator.xpath("//table[@id=" + id + "]/tbody/tr[contains(@class, 'labkey-row') or contains(@class, 'labkey-alternate-row')]").findElements(getDriver()).size();
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#checkAll()}
     */
    @Deprecated
    public void checkAllOnPage(String dataRegionName)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        WebElement toggle = Locator.xpath("//table[@id=" + id + "]//input[@name='.toggle']").findElement(getDriver());
        checkCheckbox(toggle);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#uncheckAll()}
     */
    @Deprecated
    public void uncheckAllOnPage(String dataRegionName)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        WebElement toggle = Locator.xpath("//table[@id=" + id + "]//input[@name='.toggle']").findElement(getDriver());
        checkCheckbox(toggle);
        uncheckCheckbox(toggle);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#checkCheckbox(String)}
     */
    @Deprecated
    public void checkDataRegionCheckbox(String dataRegionName, String value)
    {
        String id = Locator.xq(dataRegionName);
        checkCheckbox(Locator.xpath("//form[@id=" + id + "]//input[@name='.select' and @value='" + value + "']"));
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#checkCheckbox(int)}
     */
    @Deprecated
    public void checkDataRegionCheckbox(String dataRegionName, int index)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        List<WebElement> selects = Locator.xpath("//table[@id=" + id + "]//input[@name='.select']").findElements(getDriver());
        checkCheckbox(selects.get(index));
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.DataRegionTable#uncheckCheckbox(int)}
     */
    @Deprecated
    public void uncheckDataRegionCheckbox(String dataRegionName, int index)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        List<WebElement> selects = Locator.xpath("//table[@id=" + id + "]//input[@name='.select']").findElements(getDriver());
        uncheckCheckbox(selects.get(index));
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
        WebElement checkbox = checkBoxLocator.findElement(getDriver());
        checkCheckbox(checkbox);
    }

    public void checkCheckbox(WebElement el)
    {
        if (!el.isSelected())
        {
            el.click();
        }
    }

    public void uncheckCheckbox(WebElement el)
    {
        if (el.isSelected())
        {
            el.click();
        }
    }

    public boolean isCheckboxChecked(WebElement el)
    {
        return el.isSelected();
    }

    public void assertRadioButtonSelected(Locator radioButtonLocator)
    {
        assertTrue("Radio Button is not selected at " + radioButtonLocator.toString(), isChecked(radioButtonLocator));
    }

    public void uncheckCheckbox(Locator checkBoxLocator)
    {
        WebElement checkbox = checkBoxLocator.findElement(getDriver());
        uncheckCheckbox(checkbox);
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

    public void selectOptionByTextContaining(WebElement selectElement, String value)
    {
        Select select = new Select(selectElement);
        List<WebElement> options = select.getOptions();
        List<String> matches = new ArrayList<>();

        for (WebElement option : options)
        {
            String optionText = option.getText();
            if (optionText.contains(value))
                matches.add(optionText);
        }

        if (matches.size() == 1)
            select.selectByVisibleText(matches.get(0));
        else if (matches.isEmpty())
            select.selectByVisibleText(value);
        else
            Assert.fail("Too many matches for '" + value + "': ['" + StringUtils.join(matches, "', '") + "']");

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
            {
                if (currentURL.indexOf("?") == currentURL.length() - 1)
                    beginAt(currentURL.concat(parameter + suffix));
                else
                    beginAt(currentURL.concat("&" + parameter + suffix));
            }
            else
                beginAt(currentURL.concat("?" + parameter + suffix));
    }

    public void addUserToGroupFromGroupScreen(String userName)
    {
        waitForElement(Locator.name("names"));
        setFormElement(Locator.name("names"), userName );
        uncheckCheckbox(Locator.name("sendEmail"));
        clickButton("Update Group Membership");

    }

    public void impersonateGroup(String group, boolean isSiteGroup)
    {
        _ext4Helper.clickExt4MenuButton(false, Locators.USER_MENU, false, "Impersonate", "Group");
        waitForElement(Ext4Helper.Locators.window("Impersonate Group"));
        _ext4Helper.selectComboBoxItem("Group:", Ext4Helper.TextMatchTechnique.STARTS_WITH, (isSiteGroup ? "Site: " : "") + group);
        clickAndWait(Ext4Helper.Locators.windowButton("Impersonate Group", "Impersonate"));
    }

    public void impersonateRole(String role)
    {
        impersonateRoles(role);
    }

    public void impersonateRoles(String oneRole, String... roles)
    {
        _ext4Helper.clickExt4MenuButton(false, Locators.USER_MENU, false, "Impersonate", "Roles");
        waitForElement(Ext4Helper.Locators.window("Impersonate Roles"));

        waitAndClick(Ext4GridRef.locateExt4GridCell(oneRole));
        for (String role : roles)
            waitAndClick(Ext4GridRef.locateExt4GridCell(role));

        clickAndWait(Ext4Helper.Locators.windowButton("Impersonate Roles", "Impersonate"));
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
        waitForElement(Ext4Helper.Locators.window("Impersonate User"));
        _ext4Helper.selectComboBoxItem("User:", Ext4Helper.TextMatchTechnique.STARTS_WITH, fakeUser + " (");
        clickAndWait(Ext4Helper.Locators.windowButton("Impersonate User", "Impersonate"));
        _impersonationStack.push(fakeUser);

        if (isElementPresent(Locator.lkButton("Home")))
        {
            clickAndWait(Locator.lkButton("Home"));
        }
    }


    public void stopImpersonating()
    {
        if (_impersonationStack.isEmpty())
        {
            throw new IllegalStateException("No impersonations are thought to be in progress, based on those that have been started within the test harness");
        }
        String fakeUser = _impersonationStack.pop();
        assertEquals(displayNameFromEmail(fakeUser), getDisplayName());
        clickUserMenuItem("Stop Impersonating");
        assertSignOutAndMyAccountPresent();
        goToHome();
        assertFalse(displayNameFromEmail(fakeUser).equals(getDisplayName()));
    }

    private final HashMap<String, String> usersAndDisplayNames = new HashMap<>();

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
        _permissionsHelper.setUserPermissions(userName, permissions);
    }

    public CreateUserResponse createUser(String userName, @Nullable String cloneUserName)
    {
        return createUser(userName, cloneUserName, true);
    }

    public CreateUserResponse createUser(String userName, @Nullable String cloneUserName, boolean verifySuccess)
    {
        if(cloneUserName == null)
        {
            return _userHelper.createUser(userName, verifySuccess);
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

    public void deleteUsersIfPresent(String... userEmails)
    {
        deleteUsers(false, userEmails);
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
            else if (!isPresent)
                log("Unable to delete non-existent user: " + userEmail);

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

    public String getUrlParam(String paramName)
    {
        return getUrlParam(paramName, false);
    }

    public String getUrlParam(String paramName, boolean decode)
    {
        Map<String, String> params = getUrlParameters();
        String paramValue = params.get(paramName);

        if (paramValue != null && decode)
        {
            paramValue = paramValue.replace("+", "%20");
            try
            {
                paramValue = URLDecoder.decode(paramValue, "UTF-8");
            } catch(UnsupportedEncodingException ignore) {}
        }

        return paramValue;
    }

    public Map<String, String> getUrlParameters()
    {
        Map<String, String> params = new HashMap<>();
        String urlQuery = getURL().getQuery();
        if (urlQuery != null)
        {
            String[] urlParams = urlQuery.split("&");
            for (String param : urlParams)
            {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2)
                    params.put(keyValue[0].trim(), keyValue[1].trim());
                else if (keyValue.length == 1)
                    params.put(keyValue[0], "");
                else
                    log("Unable to parse url parameter: " + param);
            }
        }
        return params;
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
        clickAndWait(cohortTable.link(row, 0));

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
        startImportStudyFromZip(studyFile, ignoreQueryValidation, false);
    }

    protected void startImportStudyFromZip(File studyFile, boolean ignoreQueryValidation, boolean createSharedDatasets)
    {
        clickButton("Import Study");
        setFormElement(Locator.name("folderZip"), studyFile);
        if (ignoreQueryValidation)
        {
            click(Locator.checkboxByName("validateQueries"));
        }
        Locator createSharedDatasetsCheckbox = Locator.name("createSharedDatasets");
        List<WebElement> webElements = createSharedDatasetsCheckbox.findElements(getDriver());
        if (!webElements.isEmpty())
        {
            if (createSharedDatasets)
                checkCheckbox(createSharedDatasetsCheckbox);
            else
                uncheckCheckbox(createSharedDatasetsCheckbox);
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

    protected void importFolderFromZip(File folderFile)
    {
        importFolderFromZip(folderFile, true, 1);
    }

    protected void importStudyFromZip(File studyFile, boolean ignoreQueryValidation)
    {
        startImportStudyFromZip(studyFile, ignoreQueryValidation);
        waitForPipelineJobsToComplete(1, "Study import", false);
    }

    public void importStudyFromZip(File studyFile, boolean ignoreQueryValidation, boolean createSharedDataset)
    {
        startImportStudyFromZip(studyFile, ignoreQueryValidation, createSharedDataset);
        waitForPipelineJobsToComplete(1, "Study import", false);
    }

    protected void importFolderFromZip(File folderFile, boolean validateQueries, int completedJobs)
    {
        importFolderFromZip(folderFile, validateQueries, completedJobs, false);
    }

    protected void importFolderFromZip(File folderFile, boolean validateQueries, int completedJobs, boolean expectErrors)
    {
        importFolderFromZip(folderFile, validateQueries, completedJobs, expectErrors, MAX_WAIT_SECONDS * 1000);
    }

    protected void importFolderFromZip(File folderFile, boolean validateQueries, int completedJobs, boolean expectErrors, int wait)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Import"));
        waitForElement(Locator.name("folderZip"));
        setFormElement(Locator.name("folderZip"), folderFile);
        if (!validateQueries)
            uncheckCheckbox(Locator.name("validateQueries"));
        clickButtonContainingText("Import Folder From Local Zip Archive");
        waitForText("Data Pipeline");
        waitForPipelineJobsToComplete(completedJobs, "Folder import", expectErrors, wait);
    }

    protected void importFolderFromPipeline(String folderFile)
    {
        importFolderFromPipeline(folderFile, 1, true);
    }

    protected void importFolderFromPipeline(String folderFile, int completedJobsExpected)
    {
        importFolderFromPipeline(folderFile, completedJobsExpected, true);
    }

    protected void importFolderFromPipeline(String folderFile, int completedJobsExpected, boolean validateQueries)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Import"));
        clickButtonContainingText("Import Folder Using Pipeline");
        _fileBrowserHelper.importFile(folderFile, "Import Folder");
        waitForText("Import Folder from Pipeline");
        if (!validateQueries)
            uncheckCheckbox(Locator.name("validateQueries"));
        clickButton("Start Import");
        waitForPipelineJobsToComplete(completedJobsExpected, "Folder import", false);
    }



    @LogMethod
    public void signOut(@Nullable String termsText)
    {
        log("Signing out");
        beginAt("/login/logout.view");

        acceptTermsOfUse(termsText, true);

        if (!isElementPresent(Locators.signInButtonOrLink)) // Sign-out action stopped impersonation
            beginAt("/login/logout.view");
        waitForElement(Locators.signInButtonOrLink);
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

        SimpleHttpResponse response = logOutRequest.getResponse();
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());
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
            shortWait().until(ExpectedConditions.elementToBeClickable(loc.toBy()));
            waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]"));
            // select/expand tree node
            try{
                scrollIntoView(loc);
            }
            catch (StaleElementReferenceException ignore) {}
            click(loc);
            waitForElement(Locator.xpath("//tr").withClass("x4-grid-row-selected").append("/td/div/span").withText(schemaPart), 60000);
        }
    }

    public void selectQuery(String schemaName, String queryName)
    {
        log("Selecting query " + schemaName + "." + queryName + " in the schema browser...");
        selectSchema(schemaName);
        // wait for tool tip to disappear, in case it is covering the element we want to click on
        waitForElement(Locator.xpath("//div[contains(@class, 'x4-tip') and contains(@style, 'display: none')]//div[contains(@class, 'x4-tip-body')]"));
        Locator loc = Locator.queryTreeNode(queryName);
        shortWait().until(ExpectedConditions.elementToBeClickable(loc.toBy()));

        // NOTE: consider abstracting this.
        waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]"));
        // select/expand tree node
        try{
            scrollIntoView(loc);
        }
        catch (StaleElementReferenceException ignore) {}
        clickAt(loc, 1, 1, 0);

        waitForElement(Locator.xpath("//div[contains(./@class,'lk-qd-name')]/a[contains(text(), '"+ schemaName + "." + queryName + "')]/.."), 30000);
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
        createNewQuery(schemaName, null);
    }

    // Careful: If baseQueryName isn't provided, the first table in the schema will be used as the base query.
    public void createNewQuery(@NotNull String schemaName, @Nullable String baseQueryName)
    {
        if (baseQueryName != null)
            selectQuery(schemaName, baseQueryName);
        else
            selectSchema(schemaName);
        click(Locator.xpath("//a[contains(@class, 'x4-btn')]//span[text()='Create New Query']"));
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
            clickButton("Save");
        }
    }

    public void validateQueries(boolean validateSubfolders)
    {
        click(Locator.xpath("//a[contains(@class, 'x4-btn')]//span[text()='Validate Queries']"));
        Locator locFinishMsg = Locator.xpath("//div[contains(@class, 'lk-vq-status-all-ok') or contains(@class, 'lk-vq-status-error')]");
        waitForElement(Locator.tagWithClass("div", "qbrowser-validate"), WAIT_FOR_JAVASCRIPT);
        if (validateSubfolders)
        {
            shortWait().until(ExpectedConditions.elementToBeClickable(By.className("lk-vq-subfolders")));
            checkCheckbox(Locator.tagWithClass("table", "lk-vq-subfolders"));
        }
        checkCheckbox(Locator.tagWithClass("table", "lk-vq-systemqueries"));
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
            clickButton("Process and Import Data", defaultWaitForPage);

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
                _fileBrowserHelper.checkFileBrowserFileCheckbox(copiedArchive.getName());
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


    public void waitForPipelineJobsToComplete(@LoggedParam final int completeJobsExpected, @LoggedParam final String description, final boolean expectError)
    {
        waitForPipelineJobsToComplete(completeJobsExpected, description, expectError, MAX_WAIT_SECONDS * 1000);
    }

    // Wait until the pipeline UI shows the requested number of complete jobs.  Fail if any job status becomes "ERROR".
    @LogMethod
    public void waitForPipelineJobsToComplete(@LoggedParam final int completeJobsExpected, @LoggedParam final String description, final boolean expectError, int wait)
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
        }, "Pipeline jobs did not complete.", wait);

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
            statusValues.removeAll(Arrays.asList("COMPLETE", "ERROR"));
        }

        assertTrue("Running pipeline jobs were found.  Timeout:" + wait, statusValues.size() == 0);
    }

    @LogMethod
    protected void deletePipelineJob(@LoggedParam String jobDescription, @LoggedParam boolean deleteRuns)
    {
        goToModule("Pipeline");

        PipelineStatusTable table = new PipelineStatusTable(this, true, false);
        int tableJobRow = table.getJobRow(jobDescription);
        assertNotEquals("Failed to find job rowid", -1, tableJobRow);
        table.checkCheckbox(tableJobRow);

        clickButton("Delete");
        assertElementPresent(Locator.linkContainingText(jobDescription));
        if (deleteRuns)
            checkCheckbox(Locator.id("deleteRuns"));
        clickButton("Confirm Delete");
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
        reloadStudyFromZip(studyFile, true, 2);
    }

    protected void reloadStudyFromZip(File studyFile, boolean validateQueries, int pipelineJobs)
    {
        goToManageStudy();
        clickButton("Reload Study");
        setFormElement(Locator.name("folderZip"), studyFile);
        if(! validateQueries) {uncheckCheckbox(Locator.checkboxByName("validateQueries"));}
        clickButton("Reload Study From Local Zip Archive");
        waitForPipelineJobsToComplete(pipelineJobs, "Study Reload", false);
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
            waitFor(new BaseWebDriverTest.Checker()
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

            File svgDir = new File(getArtifactCollector().ensureDumpDir(), "svgs");
            String baseName;
            File svgFile;

            int i = 0;
            do
            {
                i++;
                baseName = String.format("%2d-svg[%d]", i, svgIndex);
                svgFile = new File(svgDir, baseName + ".txt");
            }while (svgFile.exists());

            getArtifactCollector().dumpScreen(svgDir, baseName);

            try(Writer writer = new UTF8PrintWriter(svgFile))
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

    public void waitForElements(final Locator loc, final int count)
    {
        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                return count == loc.findElements(getDriver()).size();
            }
        }, WAIT_FOR_JAVASCRIPT);

        assertEquals("Element not present expected number of times", count, loc.findElements(getDriver()).size());
    }

    public String waitForWikiDivPopulation(String testDivName, int waitSeconds)
    {
        while (waitSeconds-- > 0)
        {
            log("Waiting for " + testDivName + " div to render...");
            if (isElementPresent(Locator.id(testDivName)))
            {
                String divHtml = (String)executeScript("return document.getElementById('" + testDivName + "').innerHTML;");
                if (divHtml.length() > 0)
                    return divHtml;
            }
            sleep(1000);
        }
        fail("Div failed to render.");
        return null;
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

    public void goToSvgAxisTab(String axisLabel)
    {
        // Workaround: (Selenium 2.33) Unable to click axis labels reliably for some reason. Use javascript
        fireEvent(Locator.css("svg text").containing(axisLabel).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT), SeleniumEvent.click);
        waitForElement(Ext4Helper.Locators.ext4Button("Cancel")); // Axis label windows always have a cancel button. It should be the only one on the page
    }

    public void createSurveyDesign(String label, @Nullable String description, String schemaName, String queryName, @Nullable File metadataFile)
    {
        clickButton("Create Survey Design");
        waitForElement(Locator.name("label"));
        setFormElement(Locator.name("label"), label);
        if (description != null) setFormElement(Locator.name("description"), description);
        _ext4Helper.selectComboBoxItem("Schema", schemaName);
        // the schema selection enables the query combo, so wait for it to enable
        waitForElementToDisappear(Locator.xpath("//table[contains(@class,'item-disabled')]//label[text() = 'Query']"), WAIT_FOR_JAVASCRIPT);
        sleep(1000); // give it a second to get the queries for the selected schema
        _ext4Helper.selectComboBoxItem("Query", queryName);
        clickButton("Generate Survey Questions", 0);
        sleep(1000); // give it a second to generate the metadata
        String metadataValue = _extHelper.getCodeMirrorValue("metadata");
        assertNotNull("No generate survey question metadata available", metadataValue);
        if (metadataFile != null)
        {
            assertTrue(metadataFile.exists());
            String json = TestFileUtils.getFileContents(metadataFile);
            _extHelper.setCodeMirrorValue("metadata", json);
        }

        clickButton("Save Survey");
        waitForElement(Locator.tagWithText("td", label));
    }
}
