/*
 * Copyright (c) 2015-2016 LabKey Corporation
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

import com.google.common.base.Function;
import net.jsourcerer.webdriver.jserrorcollector.JavaScriptError;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Pair;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.selenium.EphemeralWebElement;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.RelativeUrl;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.TextSearcher;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
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
import org.openqa.selenium.internal.WrapsDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.TestProperties.isScriptCheckEnabled;
import static org.labkey.test.WebTestHelper.stripContextPath;

public abstract class WebDriverWrapper implements WrapsDriver
{
    public final static int WAIT_FOR_JAVASCRIPT = 10000;
    public final static int WAIT_FOR_PAGE = 30000;

    private static JSErrorChecker _jsErrorChecker = null;

    protected boolean _testTimeout = false;

    public int defaultWaitForPage = WAIT_FOR_PAGE;
    public int longWaitForPage = defaultWaitForPage * 5;

    public ExtHelper _extHelper;
    public Ext4Helper _ext4Helper;

    private Stack<String> _locationStack = new Stack<>();
    private String _savedLocation = null;

    public WebDriverWrapper()
    {
        _extHelper = new ExtHelper(this);
        _ext4Helper = new Ext4Helper(this);
    }

    @NotNull
    public final WebDriver getDriver()
    {
        if (Thread.interrupted())
            throw new RuntimeException("Test thread terminated", new InterruptedException());
        if (getWrappedDriver() == null)
            throw new NullPointerException("WebDriver has not been initialized yet");
        return getWrappedDriver();
    }

    protected WebDriver createNewWebDriver(WebDriver oldWebDriver, BrowserType browserType, File downloadDir)
    {
        WebDriver newWebDriver = null;

        switch (browserType)
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
                    prefs.put("download.default_directory", downloadDir.getAbsolutePath());
                    prefs.put("profile.content_settings.pattern_pairs.*.multiple-automatic-downloads", 1); // Turns off multiple download warning
                    prefs.put("security.warn_submit_insecure", "false");
                    options.setExperimentalOption("prefs", prefs);
                    options.addArguments("test-type"); // Suppress '--ignore-certificate-errors' warning
                    options.addArguments("disable-xss-auditor");
                    options.addArguments("ignore-certificate-errors");

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
                    profile.setPreference("browser.download.downloadDir", downloadDir.getAbsolutePath());
                    profile.setPreference("browser.download.dir", downloadDir.getAbsolutePath());
                    profile.setPreference("browser.download.manager.showAlertOnComplete", false);
                    profile.setPreference("browser.download.manager.showWhenStarting",false);
                    profile.setPreference("browser.helperApps.alwaysAsk.force", false);
                    profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
                            "application/vnd.ms-excel," + // .xls
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet," + // .xlsx
                                    "application/octet-stream," +
                                    "application/pdf," +
                                    "application/zip," +
                                    "application/x-gzip," +
                                    "application/x-zip-compressed," +
                                    "application/xml," +
                                    "text/plain," +
                                    "text/xml," +
                                    "text/x-script.perl," +
                                    "text/tab-separated-values," +
                                    "text/csv");
                    profile.setPreference("pdfjs.disabled", true); // disable Firefox's built-in PDF viewer

                    profile.setPreference("browser.ssl_override_behavior", 0);

                    if (isScriptCheckEnabled())
                    {
                        try
                        {
                            // This doesn't collect timestamps
                            JavaScriptError.addExtension(profile);}
                        catch(IOException e)
                        {throw new RuntimeException("Failed to load JS error checker", e);}
                    }
                    profile.setAcceptUntrustedCertificates(true);
                    profile.setAssumeUntrustedCertificateIssuer(false);
                    profile.setEnableNativeEvents(useNativeEvents());

                    DesiredCapabilities capabilities = DesiredCapabilities.firefox();
                    capabilities.setCapability(FirefoxDriver.PROFILE, profile);
                    if (isScriptCheckEnabled())
                    {
                        // This doesn't collect JS source information (file & line number)
                        LoggingPreferences loggingPreferences = new LoggingPreferences();
                        loggingPreferences.enable(LogType.BROWSER, Level.SEVERE);
                        capabilities.setCapability("loggingPrefs", loggingPreferences);
                    }

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
                throw new IllegalArgumentException("Browser not yet implemented: " + browserType);
        }

        if (newWebDriver != null)
        {
            Capabilities caps = ((HasCapabilities) newWebDriver).getCapabilities();
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

    protected boolean useNativeEvents()
    {
        return false;
    }

    public Object executeScript(String script, Object... arguments)
    {
        return ((JavascriptExecutor) getDriver()).executeScript(script, arguments);
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

    protected static JSErrorChecker getJsErrorChecker()
    {
        return _jsErrorChecker;
    }

    protected abstract class JSErrorChecker
    {
        public abstract void pause();
        public abstract void resume();
        @NotNull
        public abstract List<LogEntry> getErrors();
        @NotNull
        protected abstract List<String> ignored();

        public boolean isErrorIgnored(LogEntry error)
        {
            for (String ignoredText : ignored())
            {
                if(error.toString().contains(ignoredText))
                    return true;
            }

            return false;
        }
    }

    protected class LogEntryWithSourceInfo extends LogEntry
    {
        private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

        private String sourceName;
        private int lineNumber;

        LogEntryWithSourceInfo(LogEntry entry, @NotNull String sourceName, int lineNumber)
        {
            super(entry.getLevel(), entry.getTimestamp(), entry.getMessage());
            this.sourceName = sourceName;
            this.lineNumber = lineNumber;
        }

        public String getSourceName()
        {
            return sourceName;
        }

        public int getLineNumber()
        {
            return lineNumber;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s", DATE_FORMAT.format(new Date(getTimestamp())), toStringNoTimestamp());
        }

        private String toStringNoTimestamp()
        {
            return String.format("%s {%s%s}",getMessage(), sourceName, sourceName.contains(".view?") ? "" : String.format(":%d", lineNumber));
        }

        @Override
        public int hashCode()
        {
            return toStringNoTimestamp().hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            LogEntryWithSourceInfo that = (LogEntryWithSourceInfo) o;

            return toStringNoTimestamp().equals(that.toStringNoTimestamp());
        }
    }

    private class FirefoxJSErrorChecker extends JSErrorChecker
    {
        private boolean jsCheckerPaused = false;
        private int _jsErrorPauseCount = 0; // To keep track of nested pauses
        private List<Pair<Long, Long>> _pauseWindows = new ArrayList<>();
        private long _pauseStart = Long.MAX_VALUE;

        @Override
        public void pause()
        {
            _jsErrorPauseCount++;
            if (!jsCheckerPaused)
            {
                jsCheckerPaused = true;
                _pauseStart = System.currentTimeMillis();
            }
        }

        @Override
        public void resume()
        {
            if (--_jsErrorPauseCount < 1 && jsCheckerPaused)
            {
                jsCheckerPaused = false;
                _pauseWindows.add(new Pair<>(_pauseStart, System.currentTimeMillis()));
                _pauseStart = Long.MAX_VALUE;
            }
        }

        @Override @NotNull
        public List<String> ignored()
        {
            return Arrays.asList(
                    "[:0]", "{:0}", // Truncated JSON: "Ext.Error: You're trying to decode an invalid JSON String:"
                    "__webdriver_evaluate",
                    "setting a property that has only a getter",
                    "records[0].get is not a function",
                    "{file: \"chrome://",
                    "ext-base-debug.js",
                    "ext-all-sandbox-debug.js",
                    "ext-all-sandbox.js",
                    "ext-all-sandbox-dev.js",
                    "schemaMap.schemas", // EHR error
                    "com.google.gwt", // Ignore GWT errors
                    "d3-3.3.9.js", // Ignore internal D3 errors
                    "XULElement.selectedIndex", // Ignore known Firefox Issue
                    "Failed to decode base64 string!", // Firefox issue
                    "xulrunner-1.9.0.14/components/FeedProcessor.js", // Firefox problem
                    "Image corrupt or truncated:",
                    "mutating the [[Prototype]] of an object will cause your code to run very slowly", //d3 issue: https://github.com/mbostock/d3/issues/1805
                    "Using //@ to indicate", // jQuery
                    "CodeMirror is not defined", // There will be more severe errors than this if CodeMirror is actually broken
                    "NS_ERROR_FAILURE" // NS_ERROR_FAILURE:  [http://localhost:8111/labkey/vis/lib/d3pie.min.js:8]
            );
        }

        @NotNull
        @Override
        public List<LogEntry> getErrors()
        {
            List<LogEntry> _jsErrors = new ArrayList<>();

            List<LogEntry> errorsFromWebDriver = getDriver().manage().logs().get(LogType.BROWSER).filter(Level.SEVERE);
            List<JavaScriptError> errorsFromPlugin = JavaScriptError.readErrors(getDriver());

            for (int i = 0, j = 0; i < errorsFromWebDriver.size() && j < errorsFromPlugin.size(); i++)
            {
                LogEntry logEntry = errorsFromWebDriver.get(i);

                if (!isErrorWhilePaused(logEntry))
                {
                    if (errorsFromPlugin.get(j).getErrorMessage().equals(logEntry.getMessage()))
                    {
                        _jsErrors.add(new LogEntryWithSourceInfo(logEntry,
                                errorsFromPlugin.get(i).getSourceName(), errorsFromPlugin.get(i).getLineNumber()));
                        j++;
                    }
                }
            }
            return _jsErrors;
        }

        private boolean isErrorWhilePaused(LogEntry entry)
        {
            for (Pair<Long, Long> pauseWindow : _pauseWindows)
            {
                if (entry.getTimestamp() < pauseWindow.getKey())
                    return false;
                else if (entry.getTimestamp() < pauseWindow.getValue())
                    return true;
            }
            return !(entry.getTimestamp() < _pauseStart);
        }
    }

    private class ChromeJSErrorChecker extends JSErrorChecker
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

        @Override @NotNull
        public List<LogEntry> getErrors()
        {
            return Collections.emptyList();
        }
    }

    public enum BrowserType
    {
        FIREFOX,
        IE,
        CHROME,
        HTML
    }

    public void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e);
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
        doAndWaitForPageToLoad(() -> getDriver().navigate().refresh(), millis);
    }

    public void goBack(int millis)
    {
        doAndWaitForPageToLoad(() -> getDriver().navigate().back(), millis);
    }

    public void goBack()
    {
        goBack(defaultWaitForPage);
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
        shortWait().until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.lk-sb-instructions")));
        waitForElement(Locators.pageSignal("queryTreeRendered"));
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


    /**
     * Switch to the initial test window
     */
    public void switchToMainWindow()
    {
        switchToWindow(0);
    }

    public void switchToWindow(int index)
    {
        waitFor(() -> getDriver().getWindowHandles().size() > index, WAIT_FOR_JAVASCRIPT);
        List<String> windows = new ArrayList<>(getDriver().getWindowHandles());
        getDriver().switchTo().window(windows.get(index));
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
            catch (TimeoutException |NoSuchElementException tex)
            {
                return getDriver().getPageSource(); // probably viewing a tsv or text file
            }
        }
    }

    public WebDriverWait shortWait()
    {
        return new WebDriverWait(getDriver(), 10);
    }

    public WebDriverWait longWait()
    {
        return new WebDriverWait(getDriver(), 30);
    }

    /**
     * @param reuseSession true to have the Java API connection "hijack" the session from the Selenium browser window
     */
    public Connection createDefaultConnection(boolean reuseSession)
    {
        Connection connection = new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
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

    protected SelectRowsResponse executeSelectRowCommand(String schemaName, String queryName, ContainerFilter containerFilter, String path, @Nullable List<Filter> filters, boolean reuseSession)
    {
        Connection cn = createDefaultConnection(reuseSession);
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

            final String fullURL = WebTestHelper.getBaseURL() + relativeURL;

            long elapsedTime = doAndWaitForPageToLoad(() -> getDriver().navigate().to(fullURL), millis);
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

            long elapsedTime = doAndWaitForPageToLoad(() -> getDriver().navigate().to(url), milliseconds);
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
        navigateToQuery(schemaName, queryName, null);
    }

    public void navigateToQuery(String schemaName, String queryName, Integer msTimeout)
    {
        RelativeUrl queryURL = new RelativeUrl("query", "executequery");
        queryURL.setContainerPath(getCurrentContainerPath());
        queryURL.addParameter("schemaName", schemaName);
        queryURL.addParameter("query.queryName", queryName);
        queryURL.setTimeout(msTimeout);

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

    public String getCurrentProject()
    {
        String[] splitPath = getCurrentContainerPath().split("/");
        for (String pathElement : splitPath)
        {
            if (!pathElement.isEmpty())
                return pathElement;
        }
        return "/"; //root
    }

    public String getCurrentContainerPath()
    {
        return (String)executeScript("return LABKEY.container.path;");
    }

    public String getCurrentUser()
    {
        return (String)executeScript("return LABKEY.user.email;");
    }

    public String getCurrentUserName()
    {
        return (String)executeScript("return LABKEY.user.displayName");
    }

    public boolean onLabKeyPage()
    {
        return (Boolean)executeScript("return window.LABKEY != undefined;");
    }

    public boolean isSignedIn()
    {
        return (Boolean)executeScript("return LABKEY.user.isSignedIn;");
    }

    public boolean isSignedInAsAdmin()
    {
        return (Boolean)executeScript("return LABKEY.user.isSystemAdmin;");
    }

    public boolean isImpersonating()
    {
        return (Boolean)executeScript("return LABKEY.impersonatingUser != undefined;");
    }

    public void assertAlert(String msg)
    {
        Alert alert = waitForAlert();
        assertEquals(msg, alert.getText());
        alert.accept();
    }

    public void assertAlertContains(String partialMessage)
    {
        Alert alert = waitForAlert();
        assertTrue(alert.getText().contains(partialMessage));
        alert.accept();
    }

    public int dismissAllAlerts()
    {
        int alertCount = 0;
        while (isAlertPresent()){
            Alert alert = getDriver().switchTo().alert();
            log("Found unexpected alert: " + getAlertText(alert));
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
            log("Found unexpected alert: " + getAlertText(alert));
            alert.accept();
            alertCount++;
        }
        return alertCount;
    }

    private String getAlertText(Alert alert)
    {
        try
        {
            return alert.getText();
        }
        catch (RuntimeException e)
        {
            return "Failed to get alert text: " + e.getMessage();
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

    public String acceptAlert()
    {
        Alert alert = waitForAlert();
        String text = alert.getText();
        alert.accept();
        return text;
    }

    public String cancelAlert()
    {
        Alert alert = waitForAlert();
        String text = alert.getText();
        alert.dismiss();
        return text;
    }

    private Alert waitForAlert()
    {
        waitFor(this::isAlertPresent, WAIT_FOR_JAVASCRIPT);
        return getDriver().switchTo().alert();
    }

    public void waitForAlert(String alertText, int wait)
    {
        waitFor(this::isAlertPresent, "No alert appeared.", wait);
        assertAlert(alertText);
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
        executeScript("" +
                "var element = arguments[0];" +
                "var eventType = arguments[1];" +
                "var myEvent = document.createEvent('UIEvent');" +
                "myEvent.initEvent(" +
                "   eventType, /* event type */" +
                "   true,      /* can bubble? */" +
                "   true       /* cancelable? */" +
                ");" +
                "element.dispatchEvent(myEvent);", el, event.toString());
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
        List<WebElement> errors = Locators.labkeyError.findElements(getDriver());

        for (WebElement error : errors)
        {
            String errorText = error.getText();
            if (!errorText.isEmpty())
                fail("Unexpected error found: " + errorText);
        }
    }

    public void assertLabKeyErrorPresent()
    {
        List<WebElement> errors = Locators.labkeyError.findElements(getDriver());

        for (WebElement error : errors)
        {
            if (!error.getText().isEmpty())
                return;
        }
        fail("No errors found");
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

        TextSearcher.TextHandler handler = (htmlSource, text) -> {
            if (!htmlSource.contains(text))
                missingTexts.add(text);
            return true;
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

    @Deprecated
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
            String failMsg = (missingTexts.size() == 1 ? "Text '" : "Texts ['") + String.join("', '", missingTexts) + "'";
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

        searcher.setSearchTransformer((text) -> encodeText(text).toLowerCase());

        searcher.setSourceTransformer(String::toLowerCase);

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
        else if(type.contains("Is Blank"))
        {
            desc = column + " is blank";
        }
        else if(type.contains("Is Not Blank"))
        {
            desc = column + " is not blank";
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
        searcher.setSearchTransformer((text) -> encodeText(text).replace("&nbsp;", " "));

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

    private boolean _preppedForPageLoad = false;

    /**
     * @deprecated Use {@link #doAndWaitForPageToLoad(Runnable)}
     * To be made private
     */
    @Deprecated
    public void prepForPageLoad()
    {
        executeScript("window.preppedForPageLoadMarker = true;");
        _preppedForPageLoad = true;
    }

    /**
     * @deprecated Use {@link #doAndWaitForPageToLoad(Runnable, int)}
     * To be made private
     */
    @Deprecated
    public void waitForPageToLoad(int millis)
    {
        if (!_preppedForPageLoad) throw new IllegalStateException("Please call prepForPageLoad() before performing the action that would trigger the expected page load.");
        _testTimeout = true;
        waitFor(() -> (boolean) executeScript(
                        "try {if(window.preppedForPageLoadMarker) return false; else return true;}" +
                                "catch(e) {return false;}"),
                "Page failed to load", millis);
        _testTimeout = false;
        _preppedForPageLoad = false;
    }

    /**
     * @deprecated Use {@link #doAndWaitForPageToLoad(Runnable)}
     * To be made private
     */
    @Deprecated
    public void waitForPageToLoad()
    {
        waitForPageToLoad(defaultWaitForPage);
    }

    public long doAndWaitForPageToLoad(Runnable func)
    {
        return doAndWaitForPageToLoad(func, defaultWaitForPage);
    }

    public long doAndWaitForPageToLoad(Runnable func, final int msWait)
    {
        long startTime = System.currentTimeMillis();

        if (msWait > 0)
        {
            getDriver().manage().timeouts().pageLoadTimeout(msWait, TimeUnit.MILLISECONDS);
            prepForPageLoad();
        }

        func.run();

        if (msWait > 0)
        {
            waitForPageToLoad(msWait);
            getDriver().manage().timeouts().pageLoadTimeout(defaultWaitForPage, TimeUnit.MILLISECONDS);
        }

        return System.currentTimeMillis() - startTime;
    }

    /**
     * Wait for signaling element created by LABKEY.Utils.signalWebDriverTest
     * If signal element is already present, this will wait for it to become stale and be recreated
     * @param func Function that will trigger the desired signal to appear on the page
     * @param signalName Should match the signal name defined in LABKEY.Utils.signalWebDriverTest
     * @return The 'value' of the signal, if any
     */
    public String doAndWaitForPageSignal(Runnable func, String signalName)
    {
        return doAndWaitForPageSignal(func, signalName, shortWait());
    }

    /**
     * Wait for signaling element created by LABKEY.Utils.signalWebDriverTest
     * If signal element is already present, this will wait for it to become stale and be recreated
     * @param func Function that will trigger the desired signal to appear on the page
     * @param signalName Should match the signal name defined in LABKEY.Utils.signalWebDriverTest
     * @return The 'value' of the signal, if any
     */
    public String doAndWaitForPageSignal(Runnable func, String signalName, WebDriverWait wait)
    {
        return doAndWaitForElementToRefresh(func, Locators.pageSignal(signalName), wait).getAttribute("value");
    }

    /**
     * Do something that should make an element disappear and reappear
     */
    public WebElement doAndWaitForElementToRefresh(Runnable func, Locator loc, WebDriverWait wait)
    {
        List<WebElement> previousElement = loc.findElements(getDriver());

        func.run();

        if (!previousElement.isEmpty())
            wait.until(ExpectedConditions.stalenessOf(previousElement.get(0)));

        return loc.waitForElement(wait);
    }

    /**
     * Wait for Supplier to return true
     * @param wait milliseconds
     * @return false if Supplier.get() doesn't return true within 'wait' ms
     */
    public boolean waitFor(Supplier<Boolean> checker, int wait)
    {
        long startTime = System.currentTimeMillis();
        do
        {
            if( checker.get() )
                return true;
            sleep(100);
        } while ((System.currentTimeMillis() - startTime) < wait);

        return false;
    }

    public void waitForEquals(String message, Supplier expected, Supplier actual, int wait)
    {
        waitFor(() -> Objects.equals(expected.get(), actual.get()), wait);

        assertEquals(message, expected.get(), actual.get());
    }

    public void waitForNotEquals(String message, Supplier expected, Supplier actual, int wait)
    {
        waitFor(() -> !Objects.equals(expected.get(), actual.get()), wait);

        assertNotEquals(message, expected.get(), actual.get());
    }

    public void waitFor(Supplier<Boolean> checker, String failMessage, int wait)
    {
        if (!waitFor(checker, wait))
        {
            _testTimeout = true;
            fail(failMessage + " [" + wait + "ms]");
        }
    }

    public File clickAndWaitForDownload(Locator elementToClick)
    {
        return clickAndWaitForDownload(elementToClick.findElement(getDriver()));
    }

    public File[] clickAndWaitForDownload(final Locator elementToClick, final int expectedFileCount)
    {
        return clickAndWaitForDownload(elementToClick.findElement(getDriver()), expectedFileCount);
    }

    public File clickAndWaitForDownload(final WebElement elementToClick)
    {
        return clickAndWaitForDownload(elementToClick, 1)[0];
    }

    public File[] clickAndWaitForDownload(final WebElement elementToClick, final int expectedFileCount)
    {
        return doAndWaitForDownload(elementToClick::click, expectedFileCount);
    }

    public File doAndWaitForDownload(Runnable func)
    {
        return doAndWaitForDownload(func, 1)[0];
    }

    private static final Pattern TEMP_FILE_PATTERN = Pattern.compile("[a-zA-Z0-9]{4,}\\.tmp");

    public File[] doAndWaitForDownload(Runnable func, final int expectedFileCount)
    {
        final File downloadDir = BaseWebDriverTest.getDownloadDir();
        File[] existingFilesArray = downloadDir.listFiles();
        final List<File> existingFiles;

        if (existingFilesArray != null)
            existingFiles = Arrays.asList(existingFilesArray);
        else
            existingFiles = new ArrayList<>();

        func.run();

        final FileFilter tempFilesFilter = new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                return file.getName().contains(".part") ||
                        file.getName().contains(".crdownload") || TEMP_FILE_PATTERN.matcher(file.getName()).matches();
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

        waitFor(() ->{
                    final File[] files = downloadDir.listFiles(newFileFilter);
                    return files != null && files.length >= expectedFileCount;
                },
                "File(s) did not appear in download dir", WAIT_FOR_PAGE);

        waitFor(() -> {
                    final File[] files = downloadDir.listFiles(tempFilesFilter);
                    return files != null && files.length == 0;
                },
                "Temp files remain after download", WAIT_FOR_JAVASCRIPT);

        File[] newFiles = downloadDir.listFiles(newFileFilter);
        assertEquals("Wrong number of files downloaded to " + downloadDir.toString(), expectedFileCount, newFiles.length);

        log("File(s) downloaded to " + downloadDir);
        for (File newFile : newFiles)
        {
            log("File downloaded: " + newFile.getName());
        }

        if (getDriver() instanceof FirefoxDriver)
            Locator.css("body").findElement(getDriver()).sendKeys(Keys.ESCAPE); // Dismiss download dialog

        return newFiles;
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
            catch(NoSuchElementException e)
            {
                return false;
            }
        }
        return true;
    }

    public WebElement waitForAnyElement(final Locator... locators)
    {
        if (locators.length == 0)
            throw new IllegalArgumentException("Specify at least one Locator");

        try
        {
            return shortWait().until(new ExpectedCondition<WebElement>()
            {
                @Override
                public WebElement apply(@Nullable WebDriver webDriver)
                {
                    for (Locator loc : locators)
                    {
                        try
                        {
                            return loc.findElement(webDriver);
                        }
                        catch (NoSuchElementException ignore) {}
                    }
                    return null;
                }
            });
        }
        catch (TimeoutException notFound)
        {
            throw new NoSuchElementException("None of the specified elements appeared");
        }
    }

    public List<WebElement> waitForElements(final Locator... locators)
    {
        if (locators.length > 0)
        {
            return shortWait().until(new Function<SearchContext, List<WebElement>>()
            {
                @Override
                public List<WebElement> apply(@Nullable SearchContext context)
                {
                    List<WebElement> allElements = new ArrayList<>();
                    for (Locator loc : locators)
                    {
                        List<WebElement> elements = loc.findElements(context);
                        if (elements.isEmpty())
                            return null;
                        allElements.addAll(elements);
                    }
                    return allElements;
                }

                @Override
                public String toString()
                {
                    return "elements to appear";
                }
            });
        }
        return null;
    }

    public WebElement waitForElementWithRefresh(Locator loc, int wait)
    {
        long startTime = System.currentTimeMillis();

        do
        {
            try
            {
                return waitForElement(loc, 1000);
            }
            catch (NoSuchElementException retry)
            {
                refresh();
            }
        }while(System.currentTimeMillis() - startTime < wait);

        return waitForElement(loc, 1000);
    }

    public void waitForElementText(final Locator locator, final String text)
    {
        waitForElementText(locator, text, WAIT_FOR_JAVASCRIPT);
    }

    public void waitForElementText(final Locator locator, final String text, int wait)
    {
        waitFor(() -> getText(locator).equals(text),
                "Expected '" + text + "' in element '" + locator + "'", wait);
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
        waitFor(() -> !isTextPresent(text), failMessage, wait);
    }

    public void waitForTextWithRefresh(int wait, String... text)
    {
        long startTime = System.currentTimeMillis();

        do
        {
            if(isTextPresent(text))
                return;
            else
                sleep(1000);
            refresh();
        }while(System.currentTimeMillis() - startTime < wait);
        assertTextPresent(text);
    }

    public void waitForText(final String... text)
    {
        waitForText(WAIT_FOR_JAVASCRIPT, text);
    }

    public void waitForText(int wait, final String... text)
    {
        waitFor(() -> isTextPresent(text), wait);
        assertTextPresent(text);
    }

    public void waitForText(final String text, final int count, int wait)
    {
        final String failMessage = "'" + text + "' was not present " + count + " times.";
        waitFor(() -> countText(text) == count, failMessage, wait);
    }

    public boolean isElementPresent(Locator loc)
    {
        return loc.findElements(getDriver()).size() > 0;
    }

    public boolean isElementVisible(Locator loc)
    {
        return loc.findElement(getDriver()).isDisplayed();
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

    public void waitForFormElementToEqual(final WebElement el, final String value)
    {
        String failMessage = "Field with name " + el.getAttribute("name") + " did not have expected value";
        waitForEquals(failMessage, () -> value, () -> getFormElement(el), WAIT_FOR_JAVASCRIPT);
    }

    public void waitForFormElementToEqual(final Locator locator, final String value)
    {
        waitForFormElementToEqual(new EphemeralWebElement(locator, getDriver()), value);
    }

    public void waitForFormElementToNotEqual(final WebElement el, final String value)
    {
        String failMessage = "Field with name " + el.getAttribute("name") + " did change value";
        waitForNotEquals(failMessage, () -> value, () -> getFormElement(el), WAIT_FOR_JAVASCRIPT);
    }

    public void waitForFormElementToNotEqual(final Locator locator, final String value)
    {
        waitForFormElementToNotEqual(new EphemeralWebElement(locator, getDriver()), value);
    }

    public String getFormElement(Locator loc)
    {
        return getFormElement(loc.findElement(getDriver()));
    }

    public String getFormElement(WebElement el)
    {
        return (String) executeScript("return arguments[0].value;", el);
    }

    public void assertFormElementEquals(Locator loc, String value)
    {
        assertEquals(value, getFormElement(loc));
    }

    public void assertFormElementNotEquals(Locator loc, String value)
    {
        assertNotEquals(value, getFormElement(loc));
    }

    public void assertOptionEquals(Locator loc, String value)
    {
        assertEquals(value, getSelectedOptionText(loc));
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

    public WebElement scrollIntoView(Locator loc)
    {
        return scrollIntoView(loc.findElement(getDriver()), true);
    }

    public WebElement scrollIntoView(Locator loc, Boolean alignToTop)
    {
        return scrollIntoView(loc.findElement(getDriver()), alignToTop);
    }

    public WebElement scrollIntoView(WebElement el)
    {
        return scrollIntoView(el, true);
    }

    public WebElement scrollIntoView(WebElement el, Boolean alignToTop)
    {
        if(alignToTop)
        {
            executeScript("arguments[0].scrollIntoView(true);", el);
        }
        else
        {
            executeScript("arguments[0].scrollIntoView(false);", el);
        }
        return el;
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
        doAndWaitForPageToLoad(() ->
        {
            Actions builder = new Actions(getDriver());
            builder.moveToElement(el, xCoord, yCoord)
                    .click()
                    .build()
                    .perform();
        }, pageTimeout);
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
            sleep(500);
            el = l.findElement(getDriver());
            clickAndWait(el, pageTimeoutMs);
        }
    }

    public void clickAndWait(WebElement el)
    {
        clickAndWait(el, getDefaultWaitForPage());
    }

    public void clickAndWait(final WebElement el, int pageTimeoutMs)
    {
        doAndWaitForPageToLoad(() ->
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
                    sleep(2500);
                    el.click();
                }
                else
                {
                    throw tryAgain;
                }
            }
        }, pageTimeoutMs);

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
        doAndWaitForPageToLoad(() ->
        {
            Actions action = new Actions(getDriver());
            action.doubleClick(l.findElement(getDriver())).perform();
        }, millis);
    }

    public void selectFolderTreeItem(String folderName)
    {
        click(Locator.permissionsTreeNode(folderName));
    }

    public void mouseOver(Locator l)
    {
        WebElement el = l.findElement(getDriver());
        mouseOver(el);
    }

    public void mouseOver(WebElement el)
    {
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
    public void dragAndDrop(WebElement from, WebElement to)
    {
        dragAndDrop(from, to, Position.top);
    }

    public enum Position
    {top, bottom, middle}

    public void dragAndDrop(Locator from, Locator to, Position pos)
    {
        WebElement fromEl = from.findElement(getDriver());
        WebElement toEl = to.findElement(getDriver());
        dragAndDrop(fromEl, toEl, pos);
    }

    public void dragAndDrop(WebElement fromEl, WebElement toEl, Position pos)
    {
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
        builder.clickAndHold(fromEl).moveToElement(toEl, toEl.getSize().getWidth() / 2, y).release().build().perform();
    }

    public void dragAndDrop(Locator el, int xOffset, int yOffset)
    {
        WebElement fromEl = el.findElement(getDriver());
        dragAndDrop(fromEl, xOffset, yOffset);
    }

    public void dragAndDrop(WebElement fromEl, int xOffset, int yOffset)
    {
        Actions builder = new Actions(getDriver());
        builder.clickAndHold(fromEl).moveByOffset(xOffset + 1, yOffset + 1).release().build().perform();
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
            catch(NoSuchElementException ignore) {}
        }

        return values;
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

        doAndWaitForPageToLoad(() ->
                _ext4Helper.clickExt4MenuButton(false, menuLoc, false, "Clear Sort"), wait);
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

        doAndWaitForPageToLoad(() ->
                _ext4Helper.clickExt4MenuButton(false, menuLoc, false, "Sort " + (direction.equals(SortDirection.ASC) ? "Ascending" : "Descending")), wait);
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

        doAndWaitForPageToLoad(() ->
                _ext4Helper.clickExt4MenuButton(false, DataRegionTable.Locators.columnHeader(regionName, columnName), false, "Clear Filter"), waitForPageLoad);
    }

    public boolean isButtonPresent(String text)
    {
        try
        {
            findButton(text);
            return true;
        }
        catch (NoSuchElementException notPresent)
        {
            return false;
        }
    }

    public void clickButtonByIndex(String text, int index)
    {
        clickButtonByIndex(text, index, defaultWaitForPage);
    }

    public void clickButtonByIndex(String text, int index, int wait)
    {
        clickAndWait(findButton(text, index), wait);
    }

    public WebElement findButton(String text, int index)
    {
        Locator.XPathLocator[] locators = {
                // check for normal labkey button:
                Locator.lkButton(text).index(index),
                // check for Ext 4 button:
                Ext4Helper.Locators.ext4Button(text).index(index),
                // check for Ext button:
                Locator.extButton(text).index(index),
                // check for normal html button:
                Locator.button(text).index(index)
        };

        try
        {
            return waitForAnyElement(locators);
        }
        catch (NoSuchElementException notFound)
        {
            throw new NoSuchElementException("No button found with text \"" + text + "\" at index " + index, notFound);
        }
    }

    public WebElement findButton(String text)
    {
        Locator.XPathLocator[] locators = {
                // normal labkey nav button:
                Locator.lkButton(text),
                // Ext 4 button:
                Ext4Helper.Locators.ext4Button(text),
                // Ext 3 button:
                Locator.extButton(text),
                // normal HTML button:
                Locator.button(text),
                // GWT button:
                Locator.gwtButton(text)
        };

        try
        {
            return waitForAnyElement(locators);
        }
        catch (NoSuchElementException tryCaps)
        {
            if (!text.equals(text.toUpperCase()))
            {
                log("WARNING: Update test. Possible wrong casing for button: " + text);
                try
                {
                    return findButton(text.toUpperCase());
                }
                catch (NoSuchElementException capsFailed) {}
            }
            throw new NoSuchElementException("No button found with test " + text, tryCaps);
        }
    }

    protected WebElement findButtonContainingText(String text)
    {
        Locator.XPathLocator[] locators = {
                // normal labkey button:
                Locator.lkButtonContainingText(text),
                // Ext 4 button:
                Ext4Helper.Locators.ext4ButtonContainingText(text),
                // Ext 3 button:
                Locator.extButtonContainingText(text),
                // normal HTML button:
                Locator.buttonContainingText(text)
        };

        try
        {
            return waitForAnyElement(locators);
        }
        catch (NoSuchElementException notFound)
        {
            throw new NoSuchElementException("No button found containing test " + text, notFound);
        }
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
        clickButton(text, waitMillis, 0);
    }

    /**
     * Wait for a button to appear, click it, then wait for <code>waitMillis</code> for the page to load.
     *     -- which is which button of this name (first, second, etc.)
     */
    public void clickButton(final String text, int waitMillis, int which)
    {
        clickAndWait(findButton(text, which), waitMillis);
    }

    public void clickButtonContainingText(String text)
    {
        clickButtonContainingText(text, defaultWaitForPage);
    }

    public void clickButtonContainingText(String text, int waitMills)
    {
        clickAndWait(findButtonContainingText(text), waitMills);
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
            sleep(500);
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
            try
            {
                executeScript("arguments[0].setAttribute('class', arguments[1]);", el, cssString);
            }
            catch (StaleElementReferenceException ignore) {}
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

    public int getElementCount(Locator locator)
    {
        return locator.findElements(getDriver()).size();
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
        setCheckbox(el, true);
    }

    public void uncheckCheckbox(WebElement el)
    {
        setCheckbox(el, false);
    }

    public void setCheckbox(WebElement el, boolean check)
    {
        if (check && !el.isSelected())
            el.click();
        else if (!check && el.isSelected())
            el.click();
    }

    public void setCheckbox(Locator checkBoxLocator, boolean check)
    {
        WebElement checkbox = checkBoxLocator.findElement(getDriver());
        setCheckbox(checkbox, check);
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
            fail("Too many matches for '" + value + "': ['" + StringUtils.join(matches, "', '") + "']");

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

    public void assertAttributeEquals(Locator locator, String attributeName, String value)
    {
        String actual = getAttribute(locator, attributeName);
        assertEquals(value, actual);
    }

    public void assertAttributeEquals(WebElement element, String attributeName, String value)
    {
        String actual = element.getAttribute(attributeName);
        assertEquals(value, actual);
    }

    public void assertAttributeContains(Locator locator, String attributeName, String value)
    {
        String actual = getAttribute(locator, attributeName);
        assertTrue("Expected attribute '" + locator + "@" + attributeName + "' value to contain '" + value + "', but was '" + actual + "' instead.", actual != null && actual.contains(value));
    }

    public void assertAttributeContains(WebElement element, String attributeName, String value)
    {
        String actual = element.getAttribute(attributeName);
        assertTrue("Expected attribute '" + element + "@" + attributeName + "' value to contain '" + value + "', but was '" + actual + "' instead.", actual != null && actual.contains(value));
    }

    public void assertAttributeNotContains(Locator locator, String attributeName, String value)
    {
        String actual = getAttribute(locator, attributeName);
        assertTrue("Expected attribute '" + locator + "@" + attributeName + "' value to not contain '" + value + "', but was '" + actual + "' instead.", actual != null && !actual.contains(value));
    }

    public void assertAttributeNotContains(WebElement element, String attributeName, String value)
    {
        String actual = element.getAttribute(attributeName);
        assertTrue("Expected attribute '" + element + "@" + attributeName + "' value to not contain '" + value + "', but was '" + actual + "' instead.", actual != null && !actual.contains(value));
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

    public void pressUpArrow(Locator l)
    {
        WebElement el = l.findElement(getDriver());
        el.sendKeys(Keys.UP);
    }

    public void setCodeEditorValue(String id, String value)
    {
        _extHelper.setCodeMirrorValue(id, value);
    }

    public void waitForElements(final Locator loc, final int count)
    {
        waitForElements(loc, count, WAIT_FOR_JAVASCRIPT);
    }

    public void waitForElements(final Locator loc, final int count, int wait)
    {
        waitFor(() -> count == loc.findElements(getDriver()).size(), wait);

        assertEquals("Element not present expected number of times", count, loc.findElements(getDriver()).size());
    }

}
