/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import com.google.common.base.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;
import org.labkey.api.writer.PrintWriters;
import org.labkey.junit.rules.TestWatcher;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.labkey.PortalTab;
import org.labkey.test.components.search.SearchBodyWebPart;
import org.labkey.test.pages.core.admin.CustomizeSitePage;
import org.labkey.test.pages.search.SearchResultsPage;
import org.labkey.test.util.*;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.remote.service.DriverService;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.TestProperties.isInjectionCheckEnabled;
import static org.labkey.test.TestProperties.isLeakCheckSkipped;
import static org.labkey.test.TestProperties.isLinkCheckEnabled;
import static org.labkey.test.TestProperties.isQueryCheckSkipped;
import static org.labkey.test.TestProperties.isSystemMaintenanceDisabled;
import static org.labkey.test.TestProperties.isTestCleanupSkipped;
import static org.labkey.test.TestProperties.isTestRunningOnTeamCity;
import static org.labkey.test.TestProperties.isViewCheckSkipped;
import static org.labkey.test.WebTestHelper.GC_ATTEMPT_LIMIT;
import static org.labkey.test.WebTestHelper.MAX_LEAK_LIMIT;
import static org.labkey.test.WebTestHelper.buildURL;
import static org.labkey.test.WebTestHelper.isLocalServer;
import static org.labkey.test.WebTestHelper.logToServer;
import static org.labkey.test.components.PropertiesEditor.PhiSelectType;
import static org.labkey.test.components.PropertiesEditor.PhiSelectType.NotPHI;
import static org.labkey.test.components.ext4.Window.Window;
import static org.labkey.test.components.html.RadioButton.RadioButton;

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
@BaseWebDriverTest.ClassTimeout(minutes = BaseWebDriverTest.DEFAULT_CLASS_TIMEOUT)
public abstract class BaseWebDriverTest extends LabKeySiteWrapper implements Cleanable, WebTest
{
    private static BaseWebDriverTest currentTest;
    private static final SingletonWebDriver _driver = SingletonWebDriver.getInstance();
    private final BrowserType BROWSER_TYPE;

    private String _lastPageTitle = null;
    private URL _lastPageURL = null;
    private String _lastPageText = null;
    protected static boolean _testFailed = false;
    protected static boolean _anyTestFailed = false;
    public final static int WAIT_FOR_PAGE = 30000;
    public final static int WAIT_FOR_JAVASCRIPT = 10000;
    private final ArtifactCollector _artifactCollector;

    public AbstractContainerHelper _containerHelper = new APIContainerHelper(this);
    public final CustomizeView _customizeViewsHelper;
    public StudyHelper _studyHelper = new StudyHelper(this);
    public final ListHelper _listHelper;
    public AbstractAssayHelper _assayHelper = new APIAssayHelper(this);
    public SecurityHelper _securityHelper = new SecurityHelper(this);
    public FileBrowserHelper _fileBrowserHelper = new FileBrowserHelper(this);
    @Deprecated // Use ApiPermissionsHelper unless UI testing is necessary
    public UIPermissionsHelper _permissionsHelper = new UIPermissionsHelper(this);

    public static final int MAX_WAIT_SECONDS = 10 * 60;
    public static final int DEFAULT_CLASS_TIMEOUT = 25;

    public static final double DELTA = 10E-10;

    public static final String TRICKY_CHARACTERS = "><&/%\\' \"1\u00E4\u00F6\u00FC\u00C5";
    public static final String TRICKY_CHARACTERS_NO_QUOTES = "></% 1\u00E4\u00F6\u00FC\u00C5";
    public static final String TRICKY_CHARACTERS_FOR_PROJECT_NAMES = "\u2603~!@$&()_+{}-=[],.#\u00E4\u00F6\u00FC\u00C5";
    public static final String INJECT_CHARS_1 = "\"'>--><script>alert('8(');</script>";
    public static final String INJECT_CHARS_2 = "\"'>--><img src=\"xss\" onerror=\"alert('8(')\">";

    /** Have we already done a memory leak and error check in this test harness VM instance? */
    protected static boolean _checkedLeaksAndErrors = false;
    private static final String ACTION_SUMMARY_TABLE_NAME = "actions";

    protected static final String PERMISSION_ERROR = "User does not have permission to perform this operation";

    protected boolean isPerfTest = false;

    static
    {
        TestProperties.load();
    }

    public BaseWebDriverTest()
    {
        _artifactCollector = new ArtifactCollector(this);
        _listHelper = new ListHelper(this);
        _customizeViewsHelper = new CustomizeView(this);

        String seleniumBrowser = System.getProperty("selenium.browser");
        if (seleniumBrowser == null || seleniumBrowser.length() == 0)
        {
            if (isTestRunningOnTeamCity())
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

    public static BaseWebDriverTest getCurrentTest()
    {
        return currentTest;
    }

    public static Class getCurrentTestClass()
    {
        return getCurrentTest() != null ? getCurrentTest().getClass() : null;
    }

    public WebDriver getWrappedDriver()
    {
        return _driver.getWebDriver();
    }

    protected void setIsPerfTest(boolean isPerfTest)
    {
        this.isPerfTest = isPerfTest;
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

        _driver.setUp(this);

        getDriver().manage().timeouts().setScriptTimeout(WAIT_FOR_PAGE, TimeUnit.MILLISECONDS);
        getDriver().manage().timeouts().pageLoadTimeout(defaultWaitForPage, TimeUnit.MILLISECONDS);
        getDriver().manage().window().setSize(new Dimension(1280, 1024));
        closeExtraWindows();
    }

    public ArtifactCollector getArtifactCollector()
    {
        return _artifactCollector;
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

    private static void doTearDown()
    {
        boolean closeWindow = !_testFailed || Boolean.parseBoolean(System.getProperty("close.on.fail", "true"));
        _driver.tearDown(closeWindow || isTestRunningOnTeamCity());
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

    private static final String BEFORE_CLASS = "BeforeClass";
    private static final String AFTER_CLASS = "AfterClass";
    private static boolean beforeClassSucceeded = false;
    private static boolean reenableMiniProfiler = false;
    private static long testClassStartTime;
    private static int testCount;
    private static int currentTestNumber;

    @ClassRule
    public static RuleChain testClassWatcher()
    {
        TestWatcher innerClassWatcher = new TestWatcher()
        {
            @Override
            public void starting(Description description)
            {
                testClassStartTime = System.currentTimeMillis();
                _driver.clear();
                testCount = description.getChildren().size();
                currentTestNumber = 0;
                beforeClassSucceeded = false;
                _anyTestFailed = false;

                ArtifactCollector.init();

                try
                {
                    currentTest = (BaseWebDriverTest) description.getTestClass().newInstance();
                }
                catch (InstantiationException | IllegalAccessException e)
                {
                    currentTest = null; // Make sure previous instance is cleared
                    throw new RuntimeException(e);
                }

                currentTest.setUp();

                if (getDownloadDir().exists())
                {
                    try{
                        FileUtils.deleteDirectory(getDownloadDir());
                    }
                    catch (IOException ignore) { }
                }

                currentTest.getContainerHelper().clearCreatedProjects();
                currentTest.doPreamble();
            }

            @Override
            protected void succeeded(Description description)
            {
                if (!_anyTestFailed)
                    getCurrentTest().doPostamble();
                else
                    TestLogger.log("Skipping post-test checks because a test case failed.");
            }
        };

        TestWatcher classFailWatcher = new TestWatcher()
        {
            @Override
            protected void failed(Throwable e, Description description)
            {
                String pseudoTestName = description.getTestClass().getSimpleName() + (beforeClassSucceeded ? AFTER_CLASS : BEFORE_CLASS);

                if (getCurrentTest() != null && description.getTestClass().equals(getCurrentTestClass()))
                {
                    getCurrentTest().handleFailure(e, pseudoTestName);
                }
            }

            @Override
            protected void finished(Description description)
            {
                // Skip teardown if another test has already started
                if (description.getTestClass().equals(getCurrentTestClass()))
                {
                    doTearDown();
                    if (!isTestCleanupSkipped())
                    {
                        try (TestScrubber scrubber = new TestScrubber(BrowserType.FIREFOX, getDownloadDir()))
                        {
                            scrubber.cleanSiteSettings();
                        }
                    }
                }
            }
        };

        /*
         * Using Timeout at the class level isn't actually supported by JUnit.
         * We do some extra magic to make sure subsequent test methods don't keep running
         * when the class times out
         */
        TestRule classTimeout = new TestWatcher()
        {
            Statement createFailOnTimeoutStatement(Statement statement, Class<?> testClass) throws Exception
            {
                long minutes;
                ClassTimeout timeout = testClass.getAnnotation(ClassTimeout.class);
                if (timeout != null)
                    minutes = timeout.minutes();
                else
                    minutes = DEFAULT_CLASS_TIMEOUT;

                return FailOnTimeout.builder()
                        .withTimeout(minutes, TimeUnit.MINUTES)
                        .build(statement);
            }

            @Override
            public Statement apply(Statement base, Description description)
            {
                try
                {
                    return createFailOnTimeoutStatement(base, description.getTestClass());
                }
                catch (final Exception e)
                {
                    return new Statement()
                    {
                        @Override public void evaluate() throws Throwable
                        {
                            throw new RuntimeException("Invalid parameters for Timeout", e);
                        }
                    };
                }
            }

            @Override
            protected void failed(Throwable e, Description description)
            {
                if (e instanceof TestTimedOutException || e instanceof InterruptedException)
                {
                    _driver.clear();
                    currentTest = null;
                }
            }
        };

        TestWatcher loggingClassWatcher = new TestWatcher()
        {
            @Override
            public void starting(Description description)
            {
                TestLogger.resetLogger();
                TestLogger.log("// BeforeClass - " + description.getTestClass().getSimpleName() + " \\\\");
                TestLogger.increaseIndent();
            }

            @Override
            protected void finished(Description description)
            {
                TestLogger.resetLogger();
                TestLogger.log("\\\\ AfterClass Complete - " + description.getTestClass().getSimpleName() + " //");
            }
        };

        TestWatcher lock = new TestWatcher()
        {
            @Override
            public Statement apply(Statement base, Description description)
            {
                final Statement statement = super.apply(base, description);
                return new Statement()
                {
                    @Override
                    public void evaluate() throws Throwable
                    {
                        synchronized (BaseWebDriverTest.class)
                        {
                            statement.evaluate();
                        }
                        synchronized (description.getTestClass()) { /* Make sure test methods have finished */ }
                    }
                };
            }
        };

        return RuleChain.outerRule(lock).around(loggingClassWatcher).around(classTimeout).around(classFailWatcher).around(innerClassWatcher);
//        return RuleChain.outerRule(loggingClassWatcher).around(classTimeout).around(classFailWatcher).around(innerClassWatcher);
    }

    @Deprecated
    protected boolean setIsBootstrapWhitelisted(boolean addMeToWhitelist)
    {
        return true;
    }

    private static boolean securityConfigured = false;
    private void configureSecurity()
    {
        if (!securityConfigured && TestProperties.isConfigureSecurity())
        {
            goToAdminConsole()
                    .clickSiteSettings()
                    .setCSRFCheck(CustomizeSitePage.CSRFCheck.POST)
                    .save();
            securityConfigured = true;
        }
    }

    private void doPreamble()
    {
        signIn();

        // Start logging JS errors.
        resumeJsErrorChecker();
        configureSecurity();

        resetErrors();
        assertModulesAvailable(getAssociatedModules());
        deleteSiteWideTermsOfUsePage();
        enableEmailRecorder();
        reenableMiniProfiler = disableMiniProfiler();

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

    private void assertModulesAvailable(List<String> modules)
    {
        if (modules != null && !modules.isEmpty())
        {
            Set<String> allModules = _containerHelper.getAllModules();
            Set<String> missing = Collections.newSetFromMap(new CaseInsensitiveHashMap<>());
            missing.addAll(modules);
            missing.removeAll(allModules);
            if (!missing.isEmpty()) // TODO: Make this a fail state so that tests fail quickly if required modules are missing
                log(String.format("WARNING: Missing associated module%s [%s]. Ensure that you have these modules and that they are actually module, not controllers.", missing.size() > 1 ? "s" : "", String.join(", ", missing)));
        }
    }

    public Timeout testTimeout()
    {
        return new Timeout(30, TimeUnit.MINUTES);
    }

    @Rule
    public final RuleChain testRules()
    {
        TestWatcher _watcher = new TestWatcher()
        {
            @Override
            public Statement apply(Statement base, Description description)
            {
                final Statement statement = super.apply(base, description);
                return new Statement()
                {
                    @Override
                    public void evaluate() throws Throwable
                    {
                        Assume.assumeTrue("Class timed out, skipping remaining tests", description.getTestClass().equals(getCurrentTestClass()));
                        statement.evaluate();
                    }
                };
            }

            @Override
            protected void starting(Description description)
            {
                // We know that @BeforeClass methods are done now that we are in a non-static context
                beforeClassSucceeded = true;

                if (TestProperties.isNewWebDriverForEachTest())
                    doTearDown();

                setUp(); // Instantiate new WebDriver if needed
                ensureSignedInAsPrimaryTestUser();
                if (_testFailed)
                    resetErrors(); // Clear errors from a previously failed test
                _testFailed = false;
            }

            @Override
            protected void succeeded(Description description)
            {
                closeExtraWindows();
                checkErrors();
            }
        };

        // Separate TestWatcher to handle failures that happen in the nested succeeded method
        TestWatcher _failWatcher = new TestWatcher()
        {
            @Override
            protected void failed(Throwable e, Description description)
            {
                handleFailure(e, description.getMethodName());
            }

            @Override
            protected void finished(Description description)
            {
                if (description.getTestClass().equals(getCurrentTestClass()))
                {
                    Ext4Helper.resetCssPrefix();
                }
            }
        };

        TestWatcher _logger = new TestWatcher()
        {
            private long testStartTimeStamp;

            @Override
            protected void starting(Description description)
            {
                if (currentTestNumber == 0)
                {
                    TestLogger.resetLogger();
                    TestLogger.log("\\\\ BeforeClass - " + description.getTestClass().getSimpleName() + " Complete //");
                }

                currentTestNumber++;
                testStartTimeStamp = System.currentTimeMillis();

                TestLogger.resetLogger();
                TestLogger.log("// Begin Test Case - " + description.getMethodName() + " \\\\");
                try{logToServer("=== Begin Test Case - " + description.getTestClass().getSimpleName() + "." + description.getMethodName());} catch (CommandException | IOException ignore){}
                TestLogger.increaseIndent();
            }

            @Override
            protected void skipped(AssumptionViolatedException e, Description description)
            {
                TestLogger.log(e.getMessage());
                TestLogger.resetLogger();
                TestLogger.log("\\\\ Test Case Skipped - " + description.getMethodName() + " //");
            }

            @Override
            protected void succeeded(Description description)
            {
                Long elapsed = System.currentTimeMillis() - testStartTimeStamp;

                TestLogger.resetLogger();
                TestLogger.log("\\\\ Test Case Complete - " + description.getMethodName() + " [" + getElapsedString(elapsed) + "] //");
            }

            @Override
            protected void failed(Throwable e, Description description)
            {
                Long elapsed = System.currentTimeMillis() - testStartTimeStamp;

                TestLogger.resetLogger();
                TestLogger.log("\\\\ Failed Test Case - " + description.getMethodName() + " [" + getElapsedString(elapsed) + "] //");
            }

            @Override
            protected void finished(Description description)
            {
                if (currentTestNumber == testCount)
                {
                    TestLogger.resetLogger();
                    TestLogger.log("// AfterClass - " + description.getTestClass().getSimpleName() + " \\\\");
                    TestLogger.increaseIndent();
                }

            }

            private String getElapsedString(long elapsed)
            {
                return String.format("%dm %d.%ds",
                        TimeUnit.MILLISECONDS.toMinutes(elapsed),
                        TimeUnit.MILLISECONDS.toSeconds(elapsed) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed)),
                        elapsed - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(elapsed)));
            }
        };

        TestWatcher _lock = new TestWatcher()
        {
            @Override
            public Statement apply(Statement base, Description description)
            {
                final Statement statement = super.apply(base, description);
                return new Statement()
                {
                    @Override
                    public void evaluate() throws Throwable
                    {
                        synchronized (description.getTestClass())
                        {
                            statement.evaluate();
                        }
                    }
                };
            }
        };

         return RuleChain.outerRule(_lock).around(_logger).around(testTimeout()).around(_failWatcher).around(_watcher);
//        return RuleChain.outerRule(_logger).around(testTimeout()).around(_failWatcher).around(_watcher);
    }

    /**
     * Collect additional information about test failures and publish build artifacts for TeamCity
     */
    @LogMethod
    private void handleFailure(Throwable error, @LoggedParam String testName)
    {
        _testFailed = true;
        _anyTestFailed = true;

        error.printStackTrace();

        if (error instanceof TestTimedOutException ||
                error instanceof InterruptedException ||
                error.getCause() != null && error.getCause() instanceof InterruptedException)
        {
            _testTimeout = true;
            return;
        }

        System.err.println("ERROR: " + error.getMessage());

        try
        {
            try
            {
                if (isTestRunningOnTeamCity())
                {
                    getArtifactCollector().addArtifactLocation(new File(TestFileUtils.getLabKeyRoot(), "sampledata"));
                    getArtifactCollector().addArtifactLocation(new File(TestFileUtils.getLabKeyRoot(), "build/deploy/files"));
                    getArtifactCollector().dumpPipelineFiles();
                }
                if (_testTimeout)
                    getArtifactCollector().dumpThreads();
            }
            catch (RuntimeException | Error e)
            {
                log("Unable to dump pipeline files");
                System.err.println(e.getMessage());
            }

            if (error instanceof UnreachableBrowserException || getWrappedDriver() == null)
            {
                log("Browser is unavailable. Skipping browser-dependant failure handling.");
                return;
            }
            if (error instanceof TestTimeoutException || error instanceof TimeoutException)
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

                if (_lastPageTitle != null && !_lastPageTitle.startsWith("404") && _lastPageURL != null && _lastPageURL.toString().contains(WebTestHelper.getBaseURL()))
                {
                    // On failure, re-invoke the last action with _debug paramter set, which lets the action log additional debugging information
                    try
                    {
                        URL baseUrl = new URL(_lastPageURL.getProtocol(), _lastPageURL.getHost(), _lastPageURL.getPort(), _lastPageURL.getPath());
                        String query = _lastPageURL.getQuery();
                        String ref = _lastPageURL.getRef();
                        StringBuilder debugUrl = new StringBuilder(baseUrl.toString());
                        debugUrl.append("?");
                        if (StringUtils.trimToNull(query) != null)
                        {
                            debugUrl.append(query);
                            debugUrl.append("&");
                        }
                        debugUrl.append("_debug=1");
                        if (StringUtils.trimToNull(ref) != null)
                        {
                            debugUrl.append("#");
                            debugUrl.append(ref);
                        }
                        log("Re-invoking last action with _debug parameter set");
                        WebTestHelper.getHttpResponse(debugUrl.toString()).getResponseCode();
                    }
                    catch (MalformedURLException e)
                    {
                        log("Unable to construct debug URL");
                        e.printStackTrace();
                    }
                }
            }
            catch (RuntimeException | Error e)
            {
                log("Unable to determine information about the last page");
                System.err.println(e.getMessage());
            }

            // Render any client-side errors to page before taking a screenshot (just biologics for now)
            try
            {
                if (_lastPageURL != null && _lastPageURL.toString().contains("biologics"))
                {
                    log("Rendering client-side errors to page");
                    executeScript("if (LABKEY.Mothership) { LABKEY.Mothership.renderLastErrors(); }");
                }
            }
            catch (RuntimeException | Error e)
            {
                log("Unable to render client-side errors to page");
                System.err.println(e.getMessage());
            }

            try
            {
                getArtifactCollector().dumpPageSnapshot(testName, null); // Snapshot of current window
                Set<String> windowHandles = getDriver().getWindowHandles();
                windowHandles.remove(getDriver().getWindowHandle()); // All except current window
                for (String windowHandle : windowHandles)
                {
                    getDriver().switchTo().window(windowHandle);
                    getArtifactCollector().dumpPageSnapshot(testName + "-" + windowHandle, "otherWindows");
                }
            }
            catch (RuntimeException | Error e)
            {
                log("Unable to dump screenshots");
                System.err.println(e.getMessage());
            }
        }
        finally
        {
            doTearDown();
        }
    }

    @LogMethod
    private void doPostamble()
    {
        executeScript("window.onbeforeunload = null;");

        ensureSignedInAsPrimaryTestUser();

        checkQueries();

        checkViews();

        if (!isPerfTest || isTestRunningOnTeamCity())
            checkActionCoverage();

        checkLinks();

        if (!isTestCleanupSkipped())
        {
            goToHome();
            cleanup(true);

            if (getDownloadDir().exists())
            {
                try
                {
                    FileUtils.deleteDirectory(getDownloadDir());
                }
                catch (IOException ignore)
                {
                }
            }
        }
        else
        {
            log("Skipping test cleanup as requested.");
        }

        if (!"DRT".equals(System.getProperty("suite")) || Runner.isFinalTest())
        {
            checkLeaksAndErrors();
        }

        if (reenableMiniProfiler)
            setMiniProfilerEnabled(true);
    }

    private void waitForPendingRequests(int msWait)
    {
        Connection connection = createDefaultConnection(true);
        MutableLong pendingRequestCount = new MutableLong(-1);
        waitFor(() -> {
            pendingRequestCount.setValue(getPendingRequestCount(connection));
            return pendingRequestCount.getValue() == 0;
        }, msWait);
        if (pendingRequestCount.getValue() > 0)
            TestLogger.log(pendingRequestCount.getValue() + " requests still pending after " + msWait + "ms");
        if (pendingRequestCount.getValue() < 0)
            TestLogger.log("Unable to fetch pending request count" + msWait + "ms");
    }

    private long getPendingRequestCount(Connection connection)
    {
        Command getPendingRequestCount = new Command("admin", "getPendingRequestCount");
        try
        {
            CommandResponse response = getPendingRequestCount.execute(connection, null);
            return (Long)response.getProperty("pendingRequestCount");
        }
        catch (IOException | CommandException e)
        {
            return -1;
        }
    }

    private void cleanup(boolean afterTest)
    {
        if (!ClassUtils.getAllInterfaces(getClass()).contains(ReadOnlyTest.class) || ((ReadOnlyTest) this).needsSetup())
        {
            if (afterTest)
                waitForPendingRequests(WAIT_FOR_PAGE);
            doCleanup(afterTest);
        }
    }

    // Standard cleanup: delete created projects
    protected void doCleanup(boolean afterTest)
    {
        String projectName = getProjectName();

        if (null != projectName)
            _containerHelper.deleteProject(projectName, afterTest);

        for (String project : _containerHelper.getCreatedProjects())
        {
            _containerHelper.deleteProject(project, false);
        }
    }

    public void cleanup() throws Exception
    {
        try
        {
            log("========= Cleaning up " + getClass().getSimpleName() + " =========");

            setUp();
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
        return _driver.getDownloadDir();
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
        long msSinceTestStart = Long.MAX_VALUE;

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
            msSinceTestStart = System.currentTimeMillis() - testClassStartTime;
            beginAt("/admin/memTracker.view?gc=1&clearCaches=1", 120000);
            if (!isTextPresent("In-Use Objects"))
                throw new IllegalStateException("Asserts must be enabled to track memory leaks; add -ea to your server VM params and restart or add -DmemCheck=false to your test VM params.");
            leakCount = getImageWithAltTextCount("expand/collapse");
        }

        if (leakCount > MAX_LEAK_LIMIT)
        {
            String newLeak = null;
            List<WebElement> errorRows = Locator.css("#leaks tr:not(:first-child)").findElements(getDriver());
            for (WebElement errorRow : errorRows)
            {
                String ageStr = errorRow.findElement(By.cssSelector(".age")).getText();
                Duration leakAge = Duration.parse("PT" + ageStr);
                if (msSinceTestStart > leakAge.toMillis())
                {
                    newLeak = errorRow.findElement(By.cssSelector(".allocationStack")).getText();
                    break;
                }
            }

            if (newLeak != null)
            {
                getArtifactCollector().dumpHeap();
                getArtifactCollector().dumpThreads();
                fail(String.format("Found memory leak: %s [1 of %d, MAX:%d]\nSee test artifacts for more information.", newLeak, leakCount, MAX_LEAK_LIMIT));
            }

            log("Found " + leakCount + " in-use objects.  They appear to be from a previous test.");
        }
        else
            log("Found " + leakCount + " in-use objects.  This is within the expected number of " + MAX_LEAK_LIMIT + ".");
    }

    @LogMethod
    public void checkExpectedErrors(@LoggedParam int expectedErrors)
    {
        String text = getServerErrors();
        Pattern errorPattern = Pattern.compile("^ERROR", Pattern.MULTILINE);
        Matcher errorMatcher = errorPattern.matcher(text);
        int count = 0;
        while (errorMatcher.find())
        {
            count++;
        }

        if (expectedErrors != count)
        {
            beginAt(buildURL("admin", "showErrorsSinceMark"));
            resetErrors();
            assertEquals("Expected error count does not match actual count for this run.", expectedErrors, count);
        }

        // Clear expected errors to prevent the test from failing.
        resetErrors();
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
            goToProjectHome(projectName);
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
                navigateToFolder(project, folder);

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
        catch (WebDriverException ignore)
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

                //String reportType = getAttribute(expandedReport.append("//div").withClass("x-grid3-col-1").append("/img"), "alt");
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

    protected SelectRowsResponse executeSelectRowCommand(String schemaName, String queryName)
    {
        return executeSelectRowCommand(schemaName, queryName, null);
    }

    protected SelectRowsResponse executeSelectRowCommand(String schemaName, String queryName,  @Nullable List<Filter> filters)
    {
        return executeSelectRowCommand(schemaName, queryName, ContainerFilter.CurrentAndSubfolders, "/" + getProjectName(), filters);
    }

    @LogMethod
    protected void checkActionCoverage()
    {
        if ( isGuestModeTest() )
            return;

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
        File download = doAndWaitForDownload(() -> getDriver().get(WebTestHelper.buildURL("admin", "exportActions")));
        File actionCoverageFile = new File(TestProperties.getDumpDir(), "ActionCoverage.tsv");
        actionCoverageFile.delete();
        download.renameTo(actionCoverageFile);
        getArtifactCollector().publishArtifact(actionCoverageFile);
    }

    @LogMethod
    protected void checkLinks()
    {
        if (isLinkCheckEnabled())
        {
            pauseJsErrorChecker();
            String crawlerTimeoutProp = System.getProperty("crawlerTimeout");
            int crawlerTimeout = 90000;
            if (crawlerTimeoutProp != null)
                crawlerTimeout = Integer.parseInt(crawlerTimeoutProp);

            Crawler crawler = new Crawler(this, crawlerTimeout);
            crawler.addExcludedActions(getUncrawlableActions());
            crawler.setInjectionCheckEnabled(isInjectionCheckEnabled());
            crawler.addProject(getProjectName());
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
        try (Writer writer = PrintWriters.getPrintWriter(xmlFile))
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

    /**
     * @deprecated Inline me: {@link WebTestHelper#getBaseURL()}
     */
    @Deprecated
    public String getBaseURL()
    {
        return WebTestHelper.getBaseURL();
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

    @LogMethod
    protected void prepareForFolderExport(@Nullable String folderName, boolean exportSecurityGroups, boolean exportRoleAssignments, boolean includeSubfolders, int locationIndex)
    {
        if (folderName != null)
            clickFolder(folderName);
        goToFolderManagement().goToExportTab();
        waitForElement(Locator.tagWithClass("table", "export-location"));

        if (exportSecurityGroups)
            new Checkbox(Locator.tagWithText("label", "Project-level groups and members").precedingSibling("input").findElement(getDriver())).check();

        if (exportRoleAssignments)
            new Checkbox(Locator.tagWithText("label", "Role assignments for users and groups").precedingSibling("input").findElement(getDriver())).check();
        if (includeSubfolders)
            new Checkbox(Locator.tagContainingText("label", "Include Subfolders").precedingSibling("input").findElement(getDriver())).check();
        checkRadioButton(Locator.tagWithClass("table", "export-location").index(locationIndex)); // first locator with this name is "Pipeline root export directory, as individual files
    }

    @LogMethod
    protected void exportFolderAsIndividualFiles(String folderName, boolean exportSecurityGroups, boolean exportRoleAssignments, boolean includeSubfolders)
    {
        // first locator with this name is "Pipeline root export directory, as individual files
        prepareForFolderExport(folderName, exportSecurityGroups, exportRoleAssignments, includeSubfolders, 0);
        clickButton("Export");
        _fileBrowserHelper.waitForFileGridReady();
    }

    protected void exportFolderAsZip(boolean exportSecurityGroups, boolean exportRoleAssignments)
    {
        prepareForFolderExport(null, exportSecurityGroups, exportRoleAssignments, false, 1);
        clickButton("Export");
    }

    protected File exportFolderToBrowserAsZip()
    {
        prepareForFolderExport(null, false, false, false, 2);
        return clickAndWaitForDownload(findButton("Export"));
    }

    protected void goToModuleProperties()
    {
        goToFolderManagement().goToModulePropertiesTab();
    }

    protected Ext4FieldRef getModulePropertyFieldRef(ModulePropertyValue property)  //TODO: refactor this into FolderManagementPage.modulePropertyPane
    {
        Map<String, String> map = new HashMap<>();
        map.put("moduleName", property.getModuleName());
        map.put("containerPath", property.getContainerPath());
        map.put("propName", property.getPropertyName());
        waitForText(property.getPropertyName()); //wait for the property name to appear
        String query = ComponentQuery.fromAttributes("field", map);
        return _ext4Helper.queryOne(query, Ext4FieldRef.class);
    }

    public String getModulePropertyValue(ModulePropertyValue property)
    {
        goToModuleProperties();
        Ext4FieldRef ref = getModulePropertyFieldRef(property);
        return (String)ref.getValue();
    }

    public void setModuleProperties(List<ModulePropertyValue> values)
    {
        log("setting module properties");
        goToModuleProperties();

        boolean changed = false;
        for (ModulePropertyValue value : values)
        {
            String desc = value.getPropertyName() + " for container: " + value.getContainerPath();
            Ext4FieldRef ref = getModulePropertyFieldRef(value);
            if (ref == null)
                fail("Module property not found: " + desc);
            else
                log("setting property: " + desc + " to value: " + value.getValue());
            String val = value.getInputType().valueToString(ref.getValue());
            if((StringUtils.isEmpty(val) != StringUtils.isEmpty(value.getValue())) || !val.equals(value.getValue()))
            {
                changed = true;
                ref.setValue(value.getValue());
            }
        }
        if (changed)
        {
            clickButton("Save Changes", 0);
            Window window = Window(getDriver()).withTitle("Success").waitFor();
            window.clickButton("OK", true);
            _ext4Helper.waitForMaskToDisappear();
        }
        else
        {
            log("properties were already set, no changed needed");
        }
    }

    public void assertAtUserUserLacksPermissionPage()
    {
        assertTextPresent(PERMISSION_ERROR);
        assertTitleEquals("403: Error Page -- User does not have permission to perform this operation");
    }

    public void assertNavTrail(String... links)
    {
        String expectedNavTrail = String.join("", links);
        String navTrail = Locator.tagWithClass("ol", "breadcrumb").findElement(getDriver()).getText();
        assertEquals("Wrong nav trail", expectedNavTrail, navTrail);
    }

    public void clickTab(String tabname)
    {
        log("Selecting tab " + tabname);
        clickAndWait(Locator.folderTab(tabname).waitForElement(shortWait()));
    }

    public void verifyTabSelected(String caption)
    {
        assertTrue("Tab not selected: " + caption, PortalTab.find(caption, getDriver()).isActive());
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
        final RadioButton pipeOptionProjectSpecified = RadioButton(Locator.radioButtonById("pipeOptionProjectSpecified")).find(getDriver());
        try
        {
            pipeOptionProjectSpecified.check();
        }
        catch (WebDriverException retry) // Workaround for "Other element would receive the click" error
        {
            pipeOptionProjectSpecified.check();
        }
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
        {
            if ("COMPLETE".equals(statusValue))
                complete++;
        }

        return complete;
    }

    // Returns count of "COMPLETE" and "ERROR"
    public int getFinishedCount(List<String> statusValues)
    {
        List<String> finishedStates = Arrays.asList("COMPLETE", "ERROR", "CANCELLED");
        return statusValues.stream().filter(finishedStates::contains).collect(Collectors.toList()).size();
    }

    /**
     * Create a user with the specified permissions for the specified project
     */
    public void createUserWithPermissions(String userName, String projectName, String permissions)
    {
        _userHelper.createUser(userName, true);
        if(projectName==null)
            goToProjectHome();
        else
            goToProjectHome(projectName);
        _permissionsHelper.setUserPermissions(userName, permissions);
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

    @Deprecated
    public void deleteUsersIfPresent(String... userEmails)
    {
        _userHelper.deleteUsers(false, userEmails);
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
        return new DataRegionTable("Cohort", getDriver());
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Verifies the enrolled status of a cohort
     */
    public void verifyCohortStatus(DataRegionTable table, String cohort, boolean enrolled)
    {
        int row = getCohortRow(table, cohort);
        assertEquals("Enrollment state for cohort " + cohort, String.valueOf(enrolled).toLowerCase(), table.getDataAsText(row, "Enrolled").toLowerCase());
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
        // if the row does not exist then most likely the cohort passed in is incorrect
        int rowIndex = getCohortRow(cohortTable, cohort);
        clickAndWait(cohortTable.updateLink(rowIndex));

        if (!enroll)
            uncheckCheckbox(Locator.name("quf_enrolled"));
        else
            checkCheckbox(Locator.name("quf_enrolled"));
        clickButton("Submit");
    }

    public void setExportPhi(PhiSelectType exportPhiLevel)
    {
        if(NotPHI != exportPhiLevel)
        {
            new Checkbox(Locator.tagContainingText("label", "Include PHI Columns:").precedingSibling("input").findElement(getDriver())).check();
            switch(exportPhiLevel)
            {
                case Limited:
                    selectPhiCombo("Limited PHI");
                    break;
                case PHI:
                    selectPhiCombo("Full and Limited PHI");
                    break;
                case Restricted:
                    selectPhiCombo("Restricted, Full and Limited PHI");
                    break;
            }
        }
        else
        {
            new Checkbox(Locator.tagContainingText("label", "Include PHI Columns:").precedingSibling("input").findElement(getDriver())).uncheck();
        }
    }

    private void selectPhiCombo(String label)
    {
        _ext4Helper.selectComboBoxItem(Ext4Helper.Locators.formItemWithInputNamed("exportPhiLevel"), Ext4Helper.TextMatchTechnique.EXACT, label);
    }

    public void goToProjectHome()
    {
        goToProjectHome(getProjectName());
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

        clickButton("Import Study");
        List<WebElement> errors = Locators.labkeyError.withText().findElements(getDriver());
        if (!errors.isEmpty())
        {
            String errorText = String.join("\n", getTexts(errors)).trim();
            assertTrue("Error(s) present: " + errorText, errorText.isEmpty());
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
        goToFolderManagement().goToImportTab();
        waitForElement(Locator.name("folderZip"));
        setFormElement(Locator.name("folderZip"), folderFile);
        if (!validateQueries)
            uncheckCheckbox(Locator.name("validateQueries"));
        clickButtonContainingText("Import Folder");
        waitForText("Data Pipeline");
        waitForPipelineJobsToComplete(completedJobs, "Folder import", expectErrors, wait);
    }

    public void importFolderFromPipeline(String folderFile)
    {
        importFolderFromPipeline(folderFile, 1, true);
    }

    public void importFolderFromPipeline(String folderFile, int completedJobsExpected)
    {
        importFolderFromPipeline(folderFile, completedJobsExpected, true);
    }

    public void importFolderFromPipeline(String folderFile, int completedJobsExpected, boolean validateQueries)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Import"));
        clickButtonContainingText("Use Pipeline");
        _fileBrowserHelper.importFile(folderFile, "Import Folder");

        waitForText("Import Folder from Pipeline");
        Locator validateQuriesCheckbox = Locator.name("validateQueries");
        waitForElement(validateQuriesCheckbox);
        if (!validateQueries)
            uncheckCheckbox(validateQuriesCheckbox);
        clickButton("Start Import");

        waitForPipelineJobsToComplete(completedJobsExpected, "Folder import", false);
    }

    /*
     * This assumes that you have added the "search" webpart to your project
     */
    public void searchFor(final String projectName, String searchFor, final Integer expectedResults, @Nullable final String titleName)
    {
        log("Searching Project : " + projectName + " for \"" + searchFor + "\".  Expecting to find : " + expectedResults + " results");
        clickProject(projectName);
        final SearchResultsPage searchResults = new SearchBodyWebPart(getDriver()).searchForm().searchFor(searchFor);

        try
        {
            longWait().until((Predicate<WebDriver>) webDriver ->
            {
                if (titleName == null && searchResults.getResultCount().equals(expectedResults) ||
                        titleName != null && isElementPresent(Locator.linkContainingText(titleName)))
                    return true;
                refresh();
                return false;
            });
        }
        catch (TimeoutException ignore) {}

        assertEquals(String.format("Found wrong number of search results for '%s'", searchFor), expectedResults, searchResults.getResultCount());
        if (titleName != null)
        {
            clickAndWait(Locator.linkContainingText(titleName));
            if (searchFor.startsWith("\""))
                searchFor = searchFor.substring(1, searchFor.length() - 1);
            assertTextPresent(searchFor);
        }
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

            Locator.XPathLocator loc = Locator.schemaTreeNode(schemaPart);

            //first load of schemas might a few seconds
            shortWait().until(ExpectedConditions.elementToBeClickable(loc));
            Locator.XPathLocator selectedSchema = Locator.xpath("//tr").withClass("x4-grid-row-selected").append("/td/div/span").withText(schemaPart);

            if (getDriver().getCurrentUrl().endsWith("schemaName=" + schemaPart))
                waitForElement(selectedSchema);
            if (isElementPresent(selectedSchema))
                continue; // already selected
            log("Selecting schema " + schemaWithParents + " in the schema browser...");
            waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]"));
            // select/expand tree node
            try
            {
                scrollIntoView(loc);
            }
            catch (StaleElementReferenceException ignore)
            {
                log(ignore.getMessage());
            }
            doAndWaitForPageSignal(() -> click(loc), "queryTreeSelectionChange");
            waitForElement(selectedSchema, 60000);
            mouseOut(); // Dismiss tooltip
            waitForElementToDisappear(Locator.tagWithClass("div", "x4-tip").notHidden());
        }
    }

    public void selectQuery(String schemaName, String queryName)
    {
        log("Selecting query " + schemaName + "." + queryName + " in the schema browser...");
        selectSchema(schemaName);
        Locator loc = Locator.tagWithClass("span", "labkey-link").withText(queryName).notHidden();
        waitAndClick(loc);
        // NOTE: consider abstracting this.
        waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]"));
        waitForElement(Locator.xpath("//div[contains(./@class,'lk-qd-name')]//a[contains(text(), '" + schemaName + "." + queryName + "')]/.."), 30000);
    }

    public void clickFkExpando(String schemaName, String queryName, String columnName)
    {
        String queryLabel = schemaName + "." + queryName;
        click(Locator.tagWithClass("img", "lk-qd-expando").withAttribute("lkqdfieldkey", columnName)); // do we really need queryLabel?
    }

    public DataRegionTable viewQueryData(String schemaName, String queryName)
    {
        return viewQueryData(schemaName, queryName, null);
    }

    public DataRegionTable viewQueryData(String schemaName, String queryName, @Nullable String moduleName)
    {
        selectQuery(schemaName, queryName);
        Locator loc = Locator.xpath("//div[contains(@class,'lk-qd-name')]//a[contains(text(),'" + schemaName + "." + queryName + "')]");
        waitForElement(loc, WAIT_FOR_JAVASCRIPT);
        String href = getAttribute(loc, "href");
        if (moduleName != null) // 12474
            assertTextPresent("Defined in " + moduleName + " module");
        log("Navigating to " + href);
        beginAt(href);
        return new DataRegionTable("query", this);
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
        clickAndWait(Locator.xpath("//a[contains(@class, 'x4-btn')]//span[contains(text(), 'Create New Query')]"));
    }


    protected void createQuery(String container, String name, String schemaName, String sql, String xml, boolean inheritable)
    {
        String queryURL = "query/" + container + "/begin.view?schemaName=" + schemaName;
        beginAt(queryURL);
        createNewQuery(schemaName);
        waitForElement(Locator.name("ff_newQueryName"));
        setFormElement(Locator.name("ff_newQueryName"), name);
        clickButton("Create and Edit Source", 0);
        waitForElement(Locators.bodyTitle("Edit " + name));
        setCodeEditorValue("queryText", sql);
        if (xml != null)
        {
            _ext4Helper.clickExt4Tab("XML Metadata");
            setCodeEditorValue("metadataText", xml);
        }
        clickButton("Save", 0);
        waitForElement(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);
        if (inheritable)
        {
            beginAt(queryURL);
            editQueryProperties(schemaName, name);
            selectOptionByValue(Locator.name("inheritable"), "true");
            clickButton("Save");
        }
    }

    public void validateQueries(boolean validateSubfolders, int waitTime)
    {
        click(Locator.xpath("//a[contains(@class, 'x4-btn')]//span[contains(text(), 'Validate Queries')]"));
        Locator locFinishMsg = Locator.xpath("//div[contains(@class, 'lk-vq-status-all-ok') or contains(@class, 'lk-vq-status-error')]");
        waitForElement(Locator.tagWithClass("div", "qbrowser-validate"), WAIT_FOR_JAVASCRIPT);
        if (validateSubfolders)
        {
            shortWait().until(ExpectedConditions.elementToBeClickable(By.className("lk-vq-subfolders")));
            new Checkbox(Locator.css("table.lk-vq-subfolders input").findElement(getDriver())).check();
        }
        new Checkbox(Locator.css("table.lk-vq-systemqueries input").findElement(getDriver())).check();
        clickButton("Start Validation", 0);
        waitForElement(locFinishMsg, waitTime);
        //test for success
        if (!isElementPresent(Locator.xpath("//div[contains(@class, 'lk-vq-status-all-ok')]")))
        {
            fail("Some queries did not pass validation. See error log for more details.");
        }
    }

    public void validateQueries(boolean validateSubfolders)
    {
        validateQueries(validateSubfolders, 120000);
    }

    public void deleteAllRows(String projectName, String schema, String table) throws IOException, CommandException
    {

        Connection cn = new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        SelectRowsCommand cmd = new SelectRowsCommand(schema, table);
        SelectRowsResponse resp = cmd.execute(cn, projectName);
        if (resp.getRowCount().intValue() > 0)
        {
            log("Deleting rows from " + schema + "." + table);
            DeleteRowsCommand delete = new DeleteRowsCommand(schema, table);
            for (Map<String, Object> row : resp.getRows())
            {
                delete.addRow(row);
            }
            delete.execute(cn, projectName);
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

    public void waitForPipelineJobsToComplete(final int finishedJobsExpected, final String description, final boolean expectError)
    {
        waitForPipelineJobsToComplete(finishedJobsExpected, description, expectError, MAX_WAIT_SECONDS * 1000);
    }

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
        return waitForPipelineJobsToFinish(jobsExpected, Duration.ofSeconds(MAX_WAIT_SECONDS));
    }

    // wait until pipeline UI shows that all jobs have finished (either COMPLETE or ERROR status)
    @LogMethod
    public List<String> waitForPipelineJobsToFinish(@LoggedParam int jobsExpected, @LoggedParam Duration timeout)
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
        assertTrue("Running pipeline jobs were found.  Timeout:" + timeoutMilliseconds + "sec", statusValues.size() == getFinishedCount(statusValues));

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
        clickButton("Reload Study");
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
            waitFor(() -> {
                if (isElementPresent(svgLoc))
                {
                    String svgText = prepareSvgText(getText(svgLoc));
                    return expectedText.equals(svgText);
                }
                else
                    return false;
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

            try(Writer writer = PrintWriters.getPrintWriter(svgFile))
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

    public String getSVGText()
    {
        return getSVGText(0);
    }

    public String getSVGText(final int svgIndex)
    {
        final Locator svgLoc = Locator.css("div:not(.thumbnail) > svg").index(svgIndex);

        waitFor(() -> { return isElementPresent(svgLoc);}, WAIT_FOR_JAVASCRIPT);

        String svgText = getText(svgLoc);
        final String ignoredRaphaelText = "Created with Rapha\u00ebl 2.1.0";
        svgText = svgText.replace(ignoredRaphaelText, "");
        svgText = svgText.trim();
        svgText = svgText.replaceAll("[\n]", "");
        return svgText;

    }

    public String waitForWikiDivPopulation(String testDivId, int waitSeconds)
    {
        while (waitSeconds-- > 0)
        {
            log("Waiting for " + testDivId + " div to render...");
            if (isElementPresent(Locator.id(testDivId)))
            {
                String divHtml = (String)executeScript("return document.getElementById('" + testDivId + "').innerHTML;");
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
        // Remove raphael credits to make this function work with Raphael and d3 renderers.
        final String ignoredRaphaelText = "Created with Rapha\u00ebl 2.1.0";
        svgText = svgText.replace(ignoredRaphaelText, "");
        svgText = svgText.trim();

        // Strip out all the whitespace to deal with different return of getText from svgs
        return svgText.replaceAll("[\n ]", "");
    }

    private boolean isDumpSvgs()
    {
        return "true".equals(System.getProperty("dump.svgs"));
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

    Locator.XPathLocator EXPORT_ICON = Locator.tagWithClass("div", "export-icon");
    Locator.XPathLocator EXPORT_SCRIPT_ICON = Locator.tagWithClass("i", "fa-file-code-o");
    Locator.XPathLocator EXPORT_PNG_ICON = Locator.tagWithClass("i", "fa-file-image-o");
    Locator.XPathLocator EXPORT_PDF_ICON = Locator.tagWithClass("i", "fa-file-pdf-o");

    public int getExportScriptIconCount(String chartParentCls)
    {
        Locator exportIconLoc = Locator.tagWithClass("div", chartParentCls).append(EXPORT_ICON).append(EXPORT_SCRIPT_ICON);
        return exportIconLoc.findElements(getDriver()).size();
    }

    public void clickExportScriptIcon(String chartParentCls, int chartIndex)
    {
        Locator.XPathLocator chartLoc = Locator.tagWithClass("div", chartParentCls).index(chartIndex);
        Locator exportIconLoc = chartLoc.append(EXPORT_ICON).append(EXPORT_SCRIPT_ICON);
        WebElement exportIcon = exportIconLoc.findElement(getDriver());

        String exportDialogTitle = "Export as script";
        Locator exportDialog = Locator.tagWithClass("div", "chart-wizard-dialog").notHidden()
                .withDescendant(Locator.tagWithClass("div", "title-panel"))
                .withDescendant(Locator.tagWithText("div", exportDialogTitle));

        mouseOver(chartLoc); // mouse over to make sure icon is visible
        exportIcon.click();
        waitFor(() -> isElementPresent(exportDialog),
                "Ext4 Dialog with title '" + exportDialogTitle + "' did not appear after " + WAIT_FOR_JAVASCRIPT + "ms",
                WAIT_FOR_JAVASCRIPT
        );
    }

    public int getExportPNGIconCount(String chartParentCls)
    {
        Locator exportIconLoc = Locator.tagWithClass("div", chartParentCls).append(EXPORT_ICON).append(EXPORT_PNG_ICON);
        return exportIconLoc.findElements(getDriver()).size();
    }

    public File clickExportPNGIcon(String chartParentCls, int chartIndex)
    {
        return clickExportImageIcon(chartParentCls, chartIndex, EXPORT_PNG_ICON);
    }

    public int getExportPDFIconCount(String chartParentCls)
    {
        Locator exportIconLoc = Locator.tagWithClass("div", chartParentCls).append(EXPORT_ICON).append(EXPORT_PDF_ICON);
        return exportIconLoc.findElements(getDriver()).size();
    }

    public File clickExportPDFIcon(String chartParentCls, int chartIndex)
    {
        return clickExportImageIcon(chartParentCls, chartIndex, EXPORT_PDF_ICON);
    }

    private File clickExportImageIcon(String chartParentCls, int chartIndex, Locator.XPathLocator imageLoc)
    {
        Locator.XPathLocator chartLoc = Locator.tagWithClass("div", chartParentCls).index(chartIndex);
        Locator iconLoc = chartLoc.append(EXPORT_ICON).append(imageLoc);
        WebElement exportIcon = iconLoc.findElement(getDriver());
        mouseOver(chartLoc); // mouse over to make sure icon is visible
        return doAndWaitForDownload(exportIcon::click);
    }

    protected void flash(WebElement element)
    {
        DebugUtils.flash(getDriver(), element, 3);
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface ClassTimeout
    {
        int minutes();
    }

    private static final class SingletonWebDriver
    {
        private static final SingletonWebDriver INSTANCE = new SingletonWebDriver();

        @NotNull
        private Pair<WebDriver, DriverService> _driverAndService = new ImmutablePair<>(null, null);
        private File _downloadDir;

        private SingletonWebDriver()
        {
            // Just for me
            if (INSTANCE != null)
                throw new IllegalStateException("Don't create multiple instances");
        }

        private static SingletonWebDriver getInstance()
        {
            return INSTANCE;
        }

        private WebDriver getWebDriver()
        {
            return _driverAndService.getLeft();
        }

        private DriverService getDriverService()
        {
            return _driverAndService.getRight();
        }

        private File getDownloadDir()
        {
            return _downloadDir;
        }

        private void setUp(BaseWebDriverTest test)
        {
            WebDriver oldWebDriver = getWebDriver();
            File newDownloadDir = new File(test.getArtifactCollector().ensureDumpDir(test.getClass().getSimpleName()), "downloads");
            _driverAndService = test.createNewWebDriver(_driverAndService, test.BROWSER_TYPE, newDownloadDir);
            if (getWebDriver() != oldWebDriver) // downloadDir only changes when a new WebDriver is started.
                _downloadDir = newDownloadDir;
        }

        private void tearDown(boolean closeOldBrowser)
        {
            try
            {
                if (closeOldBrowser && getWebDriver() != null)
                {
                    getWebDriver().quit();
                }
            }
            catch (UnreachableBrowserException ignore)
            {
                ignore.printStackTrace(System.out);
            }
            finally
            {
                clear();
            }
        }

        private void clear()
        {
            if (getDriverService() != null && getDriverService().isRunning())
                getDriverService().stop();
            // Don't clear _downloadDir. Cleanup steps might still need it after tearDown
            _driverAndService = new ImmutablePair<>(null, null);
        }
    }
}
