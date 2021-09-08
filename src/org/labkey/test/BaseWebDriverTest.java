/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;
import org.labkey.junit.rules.TestWatcher;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.serverapi.reader.TabLoader;
import org.labkey.serverapi.writer.PrintWriters;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.labkey.PortalTab;
import org.labkey.test.components.search.SearchBodyWebPart;
import org.labkey.test.pages.admin.ExportFolderPage;
import org.labkey.test.pages.core.admin.logger.ManagerPage;
import org.labkey.test.pages.query.NewQueryPage;
import org.labkey.test.pages.query.SourceQueryPage;
import org.labkey.test.pages.search.SearchResultsPage;
import org.labkey.test.teamcity.TeamCityUtils;
import org.labkey.test.util.*;
import org.labkey.test.util.core.webdav.WebDavUploadHelper;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
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
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.TestProperties.isInjectionCheckEnabled;
import static org.labkey.test.TestProperties.isLeakCheckSkipped;
import static org.labkey.test.TestProperties.isLinkCheckEnabled;
import static org.labkey.test.TestProperties.isQueryCheckSkipped;
import static org.labkey.test.TestProperties.isRunWebDriverHeadless;
import static org.labkey.test.TestProperties.isSystemMaintenanceDisabled;
import static org.labkey.test.TestProperties.isTestCleanupSkipped;
import static org.labkey.test.TestProperties.isTestRunningOnTeamCity;
import static org.labkey.test.TestProperties.isViewCheckSkipped;
import static org.labkey.test.WebTestHelper.GC_ATTEMPT_LIMIT;
import static org.labkey.test.WebTestHelper.MAX_LEAK_LIMIT;
import static org.labkey.test.WebTestHelper.buildURL;
import static org.labkey.test.WebTestHelper.logToServer;
import static org.labkey.test.components.ext4.Window.Window;
import static org.labkey.test.components.html.RadioButton.RadioButton;
import static org.labkey.test.params.FieldDefinition.PhiSelectType;
import static org.labkey.test.params.FieldDefinition.PhiSelectType.NotPHI;

/**
 * This class should be used as the base for all functional test classes
 * Test cases should be non-destructive and should not depend on a particular execution order
 *
 * Shared setup steps should be in a public static void method annotated with org.junit.BeforeClass
 * The name of the method is not important. The JUnit runner finds the method solely based on the BeforeClass annotation
 *
 * <pre>
 * &amp;BeforeClass
 * public static void setupProject() throws Exception
 * {
 *     MyTestClass initTest = (MyTestClass)getCurrentTest();
 *     initTest.doSetup(); // Perform shared setup steps here
 * }
 *</pre>
 *
 * {@link org.junit.AfterClass} is also supported, but should not be used to perform any destructive cleanup or
 * navigation as it is executed before the base test class can perform its final checks -- link check, leak check, etc.
 * The doCleanup method should be overridden for initial and final project cleanup
 */
@BaseWebDriverTest.ClassTimeout()
public abstract class BaseWebDriverTest extends LabKeySiteWrapper implements Cleanable, WebTest
{
    private static BaseWebDriverTest currentTest;
    private final BrowserType BROWSER_TYPE;

    private String _lastPageTitle = null;
    private URL _lastPageURL = null;
    private String _lastPageText = null;
    protected static boolean _testFailed = false;
    protected static boolean _anyTestFailed = false;
    private final ArtifactCollector _artifactCollector;
    private final DeferredErrorCollector _errorCollector;

    public AbstractContainerHelper _containerHelper = new APIContainerHelper(this);
    public final CustomizeView _customizeViewsHelper;
    public StudyHelper _studyHelper = new StudyHelper(this);
    public final ListHelper _listHelper;
    public AbstractAssayHelper _assayHelper = new APIAssayHelper(this);
    @Deprecated // Redundant class. Use ApiPermissionsHelper or UiPermissionsHelper
    public SecurityHelper _securityHelper = new SecurityHelper(this);
    public FileBrowserHelper _fileBrowserHelper = new FileBrowserHelper(this);
    @Deprecated // Use ApiPermissionsHelper unless UI testing is necessary
    public UIPermissionsHelper _permissionsHelper = new UIPermissionsHelper(this);

    public static final int MAX_WAIT_SECONDS = 10 * 60;

    public static final double DELTA = 10E-10;

    public static final String TRICKY_CHARACTERS = "><&/%\\' \"1\u00E4\u00F6\u00FC\u00C5";
    public static final String TRICKY_CHARACTERS_NO_QUOTES = "></% 1\u00E4\u00F6\u00FC\u00C5";
    public static final String TRICKY_CHARACTERS_FOR_PROJECT_NAMES = "\u2603~!@$&()_+{}-=[],.#\u00E4\u00F6\u00FC\u00C5"; // No slash or space
    // TODO using </script> breaks CustomizeViewTest because of the '/'
    public static final String INJECT_CHARS_1 = Crawler.injectScriptBlock;
    public static final String INJECT_CHARS_2 = Crawler.injectAttributeScript;

    /** Have we already done a memory leak and error check in this test harness VM instance? */
    protected static boolean _checkedLeaksAndErrors = false;
    private static final String ACTION_SUMMARY_TABLE_NAME = "actions";

    static final Set<String> urlsSeen = new HashSet<>();

    static
    {
        TestProperties.load();
    }

    public BaseWebDriverTest()
    {
        _artifactCollector = new ArtifactCollector(this);
        _errorCollector = new DeferredErrorCollector(_artifactCollector);
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

    public Set<String> getUrlsSeen()
    {
        return urlsSeen;
    }

    public static BaseWebDriverTest getCurrentTest()
    {
        return currentTest;
    }

    public static Class getCurrentTestClass()
    {
        return getCurrentTest() != null ? getCurrentTest().getClass() : null;
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return SingletonWebDriver.getInstance().getWebDriver();
    }

    protected abstract @Nullable String getProjectName();

    public final @Nullable String getPrimaryTestProject()
    {
        return getProjectName();
    }

    @LogMethod
    public void setUp()
    {
        if (_testFailed)
        {
            // In case the previous test failed so catastrophically that it couldn't clean up after itself
            doTearDown();
        }

        SingletonWebDriver.getInstance().setUp(this);

        getDriver().manage().timeouts().setScriptTimeout(WAIT_FOR_PAGE, TimeUnit.MILLISECONDS);
        getDriver().manage().timeouts().pageLoadTimeout(defaultWaitForPage, TimeUnit.MILLISECONDS);
        try
        {
            getDriver().manage().window().setSize(new Dimension(1280, 1024));
        }
        catch (WebDriverException ex)
        {
            // Ignore occasional error from attempting to resize maximized window
            if (!ex.getMessage().contains("current state is maximized"))
                throw ex;
        }
        closeExtraWindows();
    }

    public ArtifactCollector getArtifactCollector()
    {
        return _artifactCollector;
    }

    public final DeferredErrorCollector checker()
    {
        return _errorCollector;
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
        boolean closeWindow = !_testFailed || isRunWebDriverHeadless() || Boolean.parseBoolean(System.getProperty("close.on.fail", "true"));
        SingletonWebDriver.getInstance().tearDown(closeWindow || isTestRunningOnTeamCity());
    }

    private void clearLastPageInfo()
    {
        _lastPageTitle = null;
        _lastPageURL = null;
        _lastPageText = null;
    }

    private void populateLastPageInfo()
    {
        clearLastPageInfo();
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
        return _lastPageText != null ? _lastPageText : getHtmlSource();
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
    private static long testCount;
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
                SingletonWebDriver.getInstance().clear();
                testCount = description.getChildren().stream().filter(child -> child.getAnnotation(Ignore.class) == null).count();
                currentTestNumber = 0;
                beforeClassSucceeded = false;
                _anyTestFailed = false;

                ArtifactCollector.init();

                try
                {
                    currentTest = (BaseWebDriverTest) description.getTestClass().getConstructor().newInstance();
                }
                catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e)
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
                getCurrentTest().checker().reportResults();
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
                        try (TestScrubber scrubber = new TestScrubber(TestProperties.isTestRunningOnTeamCity() ? BrowserType.FIREFOX : getCurrentTest().getBrowserType(), getDownloadDir()))
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
            Statement createFailOnTimeoutStatement(Statement statement, Class<?> testClass)
            {
                double timeoutMultiplier = TestProperties.getTimeoutMultiplier();

                // No class timeout when running through IntelliJ or when multiplier is zero
                if ("true".equals(System.getProperty("intellij.debug.agent")) || timeoutMultiplier == 0)
                    return statement;

                long minutes;
                ClassTimeout timeout = testClass.getAnnotation(ClassTimeout.class);
                if (timeout != null)
                    minutes = timeout.minutes();
                else
                    minutes = ClassTimeout.DEFAULT;

                minutes *= timeoutMultiplier;

                if (minutes == 0)
                    minutes = 1;

                if (isLinkCheckEnabled())
                {
                    // Increase timeout to account for crawler
                    minutes += TestProperties.getCrawlerTimeout().toMinutes();
                    minutes++;
                }

                if (!canConnectWithPrimaryUser())
                {
                    // Increase timeout to allow initial user creation and testing
                    minutes += 3;
                }

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
                        @Override public void evaluate()
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
                    SingletonWebDriver.getInstance().clear();
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

    private static boolean canConnectWithPrimaryUser()
    {
        try
        {
            String startPage = buildURL("project", "home", "start");
            SimpleHttpResponse httpResponse = WebTestHelper.getHttpResponse(startPage);
            return httpResponse.getResponseCode() < 400;
        }
        catch (RuntimeException re)
        {
            return false; // Probably a connection timeout
        }
    }

    private void doPreamble()
    {
        signIn();

        // Only do this as part of test startup if we haven't already checked. Since we do this as the last
        // step in the test, there's no reason to bother doing it again at the beginning of the next test
        if (!_checkedLeaksAndErrors && !"DRT".equals(System.getProperty("suite")))
        {
            checker().addRecordableErrorType(WebDriverException.class);
            checker().withScreenshot("startupErrors").wrapAssertion(this::checkErrors);
            checker().withScreenshot("startupLeaks").wrapAssertion(() -> checkLeaks(null));
            checker().resetErrorTypes();
            _checkedLeaksAndErrors = true;
        }

        if (TestProperties.isTroubleshootingStacktracesEnabled())
        {
            enableTroubleshootingStacktraces();
        }
        setServerDebugLogging();
        setExperimentalFlags();

        // Start logging JS errors.
        resumeJsErrorChecker();

        assertModulesAvailable(getAssociatedModules());
        deleteSiteWideTermsOfUsePage();
        try
        {
            enableEmailRecorder();
        }
        catch (AssumptionViolatedException | AssertionError ignore) { } // Tests should, generally, enable dumbster if they need it
        reenableMiniProfiler = disableMiniProfiler();

        if (isSystemMaintenanceDisabled())
        {
            // Disable scheduled system maintenance to prevent timeouts during nightly tests.
            disableMaintenance();
        }

        cleanup(false);
    }

    private void enableTroubleshootingStacktraces()
    {
        if (TestProperties.isPrimaryUserAppAdmin())
        {
            return; // app admin can't enable stack traces
        }
        Connection cn = createDefaultConnection();
        PostCommand command = new PostCommand("mini-profiler", "enableTroubleshootingStacktraces");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("enabled", true);
        command.setJsonObject(jsonObject);
        try
        {
            CommandResponse r = command.execute(cn, null);
            Map<String, Object> response = r.getParsedData();
            log("Troubleshooting stacktraces state updated: " + response.get("data"));
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to enable troubleshooting stacktraces", e);
        }
    }

    private void setServerDebugLogging()
    {
        Log4jUtils.resetAllLogLevels();
        for (String pkg : TestProperties.getDebugLoggingPackages())
        {
            Log4jUtils.setLogLevel(pkg, ManagerPage.LoggingLevel.DEBUG);
        }
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
                checker().wrapAssertion(() -> checkErrors());
                checker().reportResults();
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
                TestLogger.log("// Begin Test Case [" + currentTestNumber + "/" + testCount + "] - " + description.getMethodName() + " \\\\");
                logToServer("=== Begin Test Case - " + description.getTestClass().getSimpleName() + "[" + currentTestNumber + "/" + testCount + "]." + description.getMethodName());
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
                TestLogger.log("\\\\ Test Case Complete - " + description.getMethodName() + TestLogger.formatElapsedTime(elapsed) + " //");
            }

            @Override
            protected void failed(Throwable e, Description description)
            {
                Long elapsed = System.currentTimeMillis() - testStartTimeStamp;

                TestLogger.resetLogger();
                TestLogger.log("\\\\ Failed Test Case - " + description.getMethodName() + TestLogger.formatElapsedTime(elapsed) + " //");
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

        Timeout timeoutRule = "true".equals(System.getProperty("intellij.debug.agent")) ? Timeout.millis(0) : testTimeout();
        return RuleChain.outerRule(_lock).around(_logger).around(timeoutRule).around(_failWatcher).around(_watcher);
//        return RuleChain.outerRule(_logger).around(timeoutRule).around(_failWatcher).around(_watcher);
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

        if (Thread.interrupted() || wasCausedBy(error, Arrays.asList(TestTimedOutException.class, InterruptedException.class)))
        {
            log("Test interrupted. Skipping failure handling");
            return;
        }

        try
        {
            try
            {
                if (isTestRunningOnTeamCity())
                {
                    getArtifactCollector().addArtifactLocation(new File(TestFileUtils.getLabKeyRoot(), "build/deploy/files"));
                    getArtifactCollector().dumpPipelineFiles();
                }
            }
            catch (RuntimeException | Error e)
            {
                log("Unable to dump pipeline files");
                System.err.println(e.getMessage());
            }
            try
            {
                if (wasCausedBy(error, Arrays.asList(TestTimeoutException.class, SocketTimeoutException.class)))
                    getArtifactCollector().dumpThreads();
            }
            catch (RuntimeException | Error e)
            {
                log("Unable to dump threads");
                System.err.println(e.getMessage());
            }

            if (error instanceof UnreachableBrowserException || getWrappedDriver() == null)
            {
                log("Browser is unavailable. Skipping browser-dependant failure handling.");
                return;
            }

            if (error instanceof UnhandledAlertException)
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
                        WebTestHelper.getHttpResponse(debugUrl.toString());
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
                boolean inIFrame = Locator.css(":root").findElement(getDriver()).getSize().getHeight() > 0;
                if (inIFrame)
                {
                    // TODO: Need a better way to detect when we are in an iFrame. Above method is not reliable
                    //if (isFirefox()) // As of 2.45, Chromedriver screenshots are not restricted to currently focused iFrame
                    //{
                    //    getArtifactCollector().dumpPageSnapshot(testName + "-iframe", null, false); // Snapshot of iFrame
                    //}
                    try
                    {
                        // Switch to default content just in case we're in an iFrame
                        getDriver().switchTo().defaultContent();
                    }
                    catch (UnhandledAlertException alert)
                    {
                        TestLogger.warn("Alert was triggered by iframe: " + alert.getAlertText());
                    }
                }
                getArtifactCollector().dumpPageSnapshot(testName, null); // Snapshot of current window
                String failureWindow = getDriver().getWindowHandle();
                Set<String> otherWindowHandles = getDriver().getWindowHandles().stream()
                        .filter(handle -> !handle.equals(failureWindow)).collect(Collectors.toSet());
                for (String windowHandle : otherWindowHandles)
                {
                    getDriver().switchTo().window(windowHandle);
                    getArtifactCollector().dumpPageSnapshot(testName + "-" + windowHandle, "otherWindows");
                }
                if (!otherWindowHandles.isEmpty())
                {
                    // Leave browser in fail state for local investigation
                    getDriver().switchTo().window(failureWindow);
                }
            }
            catch (RuntimeException e)
            {
                log("Unable to dump screenshots");
                System.err.println(e.getMessage());
            }
            if (isTestRunningOnTeamCity()) // Don't risk modifying browser state when running locally
            {
                clearLastPageInfo(); // Make sure server error screenshot doesn't reuse cached page text
                // Reset errors before next test and make it easier to view server-side errors that may have happened during the test.
                checker().withScreenshot(testName + "_serverErrors").wrapAssertion(this::checkErrors);
            }
        }
        finally
        {
            getArtifactCollector().publishDumpedArtifacts();
            doTearDown();
        }
    }

    private boolean wasCausedBy(Throwable throwable, Collection<Class<? extends Throwable>> causes)
    {
        while (throwable != null)
        {
            for (Class<? extends Throwable> check : causes)
            {
                if (check.isAssignableFrom(throwable.getClass()))
                {
                    return true;
                }
            }
            throwable = throwable.getCause();
        }
        return false;
    }

    protected void disablePageUnloadEvents()
    {
        executeScript(
                "window.addEventListener(\"beforeunload\", function (event) {\n" +
                        "    event.stopPropagation();\n" +
                        "}, true);");
        executeScript("beforeunload = null;");
        executeScript("window.onbeforeunload = null;");
    }

    @LogMethod
    private void doPostamble()
    {
        disablePageUnloadEvents();

        ensureSignedInAsPrimaryTestUser();

        checkQueries();

        checkViews();

        if (isTestRunningOnTeamCity())
            checkActionCoverage();

        checkLinks();

        if (!isTestCleanupSkipped())
        {
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
            checkErrors();
            checkLeaks(null);
        }

        if (reenableMiniProfiler && !TestProperties.isTestRunningOnTeamCity())
            setMiniProfilerEnabled(true);

        resetExperimentalFlags();
    }

    private void waitForPendingRequests(int msWait)
    {
        Connection connection = createDefaultConnection();
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
        ensureSignedInAsPrimaryTestUser();

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

    @Override
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
        return SingletonWebDriver.getInstance().getDownloadDir();
    }

    protected void checkLeaks(Long leakCutoffTime)
    {
        if (isLeakCheckSkipped())
            return;
        if (isGuestModeTest())
            return;

        if (leakCutoffTime == null)
        {
            leakCutoffTime = testClassStartTime;
        }

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
            msSinceTestStart = System.currentTimeMillis() - leakCutoffTime;
            beginAt("/admin/memTracker.view?gc=1&clearCaches=1", 120000);
            if (!isTextPresent("In-Use Objects"))
                throw new IllegalStateException("Asserts must be enabled to track memory leaks; add -ea to your server VM params and restart or add -DmemCheck=false to your test VM params.");
            leakCount = getImageWithAltTextCount("expand/collapse");
        }

        if (leakCount > MAX_LEAK_LIMIT)
        {
            String newLeak = null;
            List<WebElement> errorRows = Locator.css("#leaks tr:not(:first-child)").findElements(getDriver());
            assertEquals("Didn't find memTracker rows. Test Locator may need to be updated.", leakCount, errorRows.size());
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
        int viewCount = view.findElements(getDriver()).size();
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
        double actionCoveragePercent;
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
        File downloadActions = downloadFromUrl("admin-exportActions.view");
        File actionCoverageFile = new File(TestProperties.getDumpDir(), "ActionCoverage.tsv");
        replaceArtifact(downloadActions, actionCoverageFile, "exported action coverage");

        if (BROWSER_TYPE == BrowserType.CHROME)
            refresh(); // Chrome blocks sequential downloads from javascript
    }

    private void replaceArtifact(File downloadedFile, File artifactFile, String description)
    {
        try
        {
            Files.move(downloadedFile.toPath(), artifactFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            getArtifactCollector().publishArtifact(artifactFile);
        }
        catch (IOException e)
        {
            TestLogger.error("Failed to move " + description + " file.");
            e.printStackTrace();
        }
    }

    @LogMethod
    protected void checkLinks()
    {
        if (isLinkCheckEnabled())
        {
            checkErrors(); // Check for errors that happened before crawler
            pauseJsErrorChecker();

            Crawler crawler = new Crawler(this, TestProperties.getCrawlerTimeout(), isInjectionCheckEnabled());
            crawler.addExcludedActions(getUncrawlableActions());
            crawler.addProject(getProjectName());
            crawler.crawlAllLinks();
            resumeJsErrorChecker();
            try
            {
                checkErrors();
            }
            catch (AssertionError ae)
            {
                throw new AssertionError("Crawler triggered some server-side errors.");
            }
        }
    }

    protected List<Crawler.ControllerActionId> getUncrawlableActions()
    {
        return Collections.emptyList();
    }

    private void writeActionStatistics(int totalActions, int coveredActions, double actionCoveragePercent)
    {
        TeamCityUtils.reportBuildStatisticValue("totalActions", totalActions);
        TeamCityUtils.reportBuildStatisticValue("coveredActions", coveredActions);
        TeamCityUtils.reportBuildStatisticValue("actionCoveragePercent", actionCoveragePercent);
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
    protected ExportFolderPage prepareForFolderExport(@Nullable String folderName, boolean exportSecurityGroups, boolean exportRoleAssignments, boolean includeSubfolders, boolean includeFiles,boolean exportETLDefination, int locationIndex)
    {
        if (folderName != null)
            clickFolder(folderName);
        goToFolderManagement().goToExportTab();
        waitForElement(Locator.tagWithClass("table", "export-location"));

        ExportFolderPage exportFolderPage = new ExportFolderPage(getDriver());

        if (exportETLDefination)
            exportFolderPage.includeETLDefintions(exportETLDefination);

        if (exportSecurityGroups)
            exportFolderPage.includeSecurityGroups(exportSecurityGroups);

        if (exportRoleAssignments)
            exportFolderPage.includeRoleAssignments(exportRoleAssignments);

        if (includeSubfolders)
            exportFolderPage.includeSubfolders(includeSubfolders);

        if (includeFiles)
            exportFolderPage.includeFiles(includeFiles);

        checkRadioButton(Locator.tagWithClass("table", "export-location").index(locationIndex)); // first locator with this name is "Pipeline root export directory, as individual files
        return exportFolderPage;
    }

    @LogMethod
    protected void exportFolderAsIndividualFiles(String folderName, boolean exportSecurityGroups, boolean exportRoleAssignments, boolean includeSubfolders)
    {
        // first locator with this name is "Pipeline root export directory, as individual files
        prepareForFolderExport(folderName, exportSecurityGroups, exportRoleAssignments, includeSubfolders, false,false, 0);
        clickButton("Export");
        _fileBrowserHelper.waitForFileGridReady();
    }

    protected void exportFolderAsZip(boolean exportSecurityGroups, boolean exportRoleAssignments)
    {
        prepareForFolderExport(null, exportSecurityGroups, exportRoleAssignments, false, false,false,1);
        clickButton("Export");
    }

    protected File exportFolderAsZip(String folderName, boolean exportSecurityGroups, boolean exportRoleAssignments, boolean includeSubfolders, boolean includeFiles)
    {
        prepareForFolderExport(folderName, exportSecurityGroups, exportRoleAssignments, includeSubfolders, includeFiles,false,2);
        return clickAndWaitForDownload(findButton("Export"));
    }

    protected File exportFolderAsZipWithETLAndSubfolder(String folderName,boolean includeSubfolder,boolean exportETLDefination)
    {
        prepareForFolderExport(folderName,false,false,includeSubfolder,false,exportETLDefination,2);
        return clickAndWaitForDownload(findButton("Export"));
    }

    protected File exportFolderToBrowserAsZip()
    {
        prepareForFolderExport(null, false, false, false, false,false,2);
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
            if((StringUtils.isEmpty(val) != StringUtils.isEmpty(String.valueOf(value.getValue()))) || !val.equals(value.getValue()))
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

    public void assertNavTrail(String... links)
    {
        verifyNavTrail(true, links);
    }

    public boolean verifyNavTrail(boolean throwOnNoMatch, String... links)
    {
        Locator navTrailLocator = Locator.tagWithClass("ol", "breadcrumb");
        boolean exists = navTrailLocator.existsIn(getDriver());

        if (!exists)
        {
            if (throwOnNoMatch)
                fail("NavTrail does not exist");
            else
                return false;
        }

        String navTrailText = navTrailLocator.findElement(getDriver()).getText();
        String expectedNavTrail = String.join("", links);

        if (throwOnNoMatch)
            assertEquals("Nav trail does not match", expectedNavTrail, navTrailText);

        return expectedNavTrail.equals(navTrailText);
    }

    public void clickTab(String tabname)
    {
        clickTab(tabname, true);
    }

    public void clickTab(String tabname, boolean waitForPageLoad)
    {
        tabname = tabname.trim();
        if (tabname.equals("+") || tabname.equals("add"))
            throw new IllegalArgumentException("Use PortalHelper.addTab");

        log("Selecting tab " + tabname);
        WebElement tab = Locator.folderTab(tabname).waitForElement(shortWait());
        mouseOver(tab);
        if (waitForPageLoad)
            clickAndWait(tab);
        else
            tab.click();
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

    /**
     * @deprecated Only use for testing pipeline functionality.
     * Setting a custom pipeline root is unnecessarily restrictive for most tests.
     * Consider using {@link WebDavUploadHelper#uploadDirectoryContents(java.io.File)} for a similar result
     */
    @Deprecated
    public void setPipelineRoot(String rootPath)
    {
        setPipelineRoot(rootPath, false);
    }

    public void setPipelineRoot(String rootPath, boolean inherit)
    {
        if (TestProperties.isServerRemote())
        {
            WebDavUploadHelper uploadHelper = new WebDavUploadHelper(getProjectName());
            uploadHelper.uploadDirectoryContents(new File(rootPath));
        }
        else
        {
            _setPipelineRoot(rootPath, inherit);

            waitForElement(Locators.labkeyMessage.withText("The pipeline root was set to '" + Paths.get(rootPath).normalize().toString() + "'"));

            getArtifactCollector().addArtifactLocation(new File(rootPath));

            log("Finished setting pipeline to: " + rootPath);
        }
    }

    public String setPipelineRootExpectingError(String rootPath)
    {
        _setPipelineRoot(rootPath, false);
        return Locators.labkeyError.waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT).getText();
    }

    private void _setPipelineRoot(String rootPath, boolean inherit)
    {
        log("Set pipeline to: " + rootPath);
        goToDataPipeline()
                .clickSetup();

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
    }

    public void setPipelineRootToDefault()
    {
        log("Set pipeline to default based on the site-level root");
        goToDataPipeline()
                .clickSetup();
        checkRadioButton(Locator.radioButtonById("pipeOptionSiteDefault"));
        clickButton("Save");
        log("Finished setting pipeline to default based on the site-level root");
    }

    /**
     * Create a user with the specified permissions for the specified project
     */
    public void createUserWithPermissions(String userName, String projectName, String permissions)
    {
        if (projectName == null)
        {
            projectName = getProjectName();
        }
        _userHelper.createUser(userName, true);
        new ApiPermissionsHelper(this)
                .addMemberToRole(userName, permissions, PermissionsHelper.MemberType.user, projectName);
    }

    public ApiPermissionsHelper createSiteDeveloper(String userEmail)
    {
        _userHelper.createUser(userEmail);
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        if (TestProperties.isPrimaryUserAppAdmin())
        {
            apiPermissionsHelper
                .addMemberToRole(userEmail, "Trusted Analyst", PermissionsHelper.MemberType.user, "/");
        }
        else
        {
            apiPermissionsHelper.addUserToSiteGroup(userEmail, "Developers");
        }

        return apiPermissionsHelper;
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
        int row = table.getRowIndex("Label", cohort);
        assertEquals("Enrollment state for cohort " + cohort, String.valueOf(enrolled).toLowerCase(), table.getDataAsText(row, "Enrolled").toLowerCase());
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Changes the enrolled status of the passed in cohort name
     */
    public void changeCohortStatus(DataRegionTable cohortTable, String cohort, boolean enroll)
    {
        // if the row does not exist then most likely the cohort passed in is incorrect
        int rowIndex = cohortTable.getRowIndex("Label", cohort);
        cohortTable.updateRow(rowIndex, Map.of("enrolled", enroll), true);
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
            longWait().until(webDriver ->
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

        StringBuilder schemaWithParents = new StringBuilder();
        String separator = "";
        for (String schemaPart : schemaParts)
        {
            schemaWithParents.append(separator).append(schemaPart);
            separator = ".";

            Locator.XPathLocator loc = Locator.tag("tr").withClass("x4-grid-row").append("/td/div/span").withText(schemaPart).precedingSibling("img").withClass("x4-tree-icon");

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
            catch (StaleElementReferenceException log)
            {
                log(log.getMessage());
            }
            doAndWaitForPageSignal(() -> {
                WebElement folderIcon = loc.findElement(getDriver());
                // Moving to desired tree node should dismiss tooltip from previously clicked folder
                new Actions(getDriver()).moveToElement(folderIcon).perform();
                folderIcon.click();
            }, "queryTreeSelectionChange");
            waitForElement(selectedSchema, 60000);
        }
    }

    public void selectQuery(String schemaName, String queryName)
    {
        log("Selecting query " + schemaName + "." + queryName + " in the schema browser...");
        selectSchema(schemaName);
        mouseOver(Locator.byClass(".x4-tab-button")); // Move away from schema tree to dismiss tooltip
        waitAndClick(Ext4Helper.Locators.tab(schemaName)); // Click schema tab to make sure query list is visible
        WebElement queryLink = Locator.tagWithClass("table", "lk-qd-coltable").append(Locator.tagWithClass("span", "labkey-link")).withText(queryName).notHidden().waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        queryLink.click();
        waitForElement(Locator.tagWithClass("div", "lk-qd-name").startsWith(schemaName + "." + queryName), 30000);
    }

    public void clickFkExpando(String schemaName, String queryName, String columnName)
    {
        click(Locator.tagWithClass("img", "lk-qd-expando").withAttribute("lkqdfieldkey", columnName));
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
            assertTextPresent("LabKey SQL query defined in " + moduleName + " module");
        if (!href.contains("executeQuery.view"))
            log("DEBUG: viewQueryData(" + schemaName + "." + queryName + ") doesn't use executeQuery");
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
    public NewQueryPage createNewQuery(@NotNull String schemaName, @Nullable String baseQueryName)
    {
        if (baseQueryName != null)
            selectQuery(schemaName, baseQueryName);
        else
            selectSchema(schemaName);
        clickAndWait(Locator.xpath("//a[contains(@class, 'x4-btn')]//span[contains(text(), 'Create New Query')]"));

        return new NewQueryPage(getDriver());
    }


    protected void createQuery(String container, String name, String schemaName, String sql, String xml, boolean inheritable)
    {
        SourceQueryPage sourcePage = createQuery(container, name, schemaName);
        sourcePage.setSource(sql);
        setCodeEditorValue("queryText", sql);
        if (xml != null)
        {
            sourcePage.setMetadataXml(xml);
        }
        sourcePage.clickSave();
        if (inheritable)
        {
            String queryURL = "query/" + container + "/begin.view?schemaName=" + schemaName;
            beginAt(queryURL);
            editQueryProperties(schemaName, name);
            selectOptionByValue(Locator.name("inheritable"), "true");
            clickButton("Save");
        }
    }

    @NotNull
    protected SourceQueryPage createQuery(String container, String name, String schemaName)
    {
        SourceQueryPage sourceQueryPage = NewQueryPage.beginAt(this, container, schemaName)
            .setName(name)
            .clickCreate();
        waitForElement(Locators.bodyTitle("Edit " + name));
        return sourceQueryPage;
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
        Connection cn = WebTestHelper.getRemoteApiConnection();
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
            StringBuilder path = new StringBuilder("/");
            for (String dir : dirNames)
                path.append(dir).append("/");

            _fileBrowserHelper.selectFileBrowserItem(path.toString());

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
            goToDataPipeline();

            waitForPipelineJobsToComplete(_completeJobsExpected, "specimen import", _expectError);

            for (File copiedArchive : _copiedArchives)
                if (!copiedArchive.delete())
                    throw new RuntimeException("Couldn't delete copied specimen archive: " + copiedArchive.getAbsolutePath());
        }
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

        waitFor(() -> isElementPresent(svgLoc), WAIT_FOR_JAVASCRIPT);

        String svgText = getText(svgLoc);
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
        try
        {
            return doAndWaitForDownload(exportIcon::click);
        }
        catch (TimeoutException retry) // Download sometimes fails on first attempt
        {
            mouseOver(chartLoc);
            return doAndWaitForDownload(exportIcon::click);
        }
    }

    public List<Map<String, Object>> loadTsv(File tsv)
    {
        TabLoader loader = new TabLoader(tsv, true);
        return loader.load();
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
        int DEFAULT = 25;
        int minutes() default DEFAULT;
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
            if (Thread.interrupted())
                return null; // Not for you
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

            if (TestProperties.isInjectionCheckEnabled())
            {
                test.addPageLoadListener(new PageLoadListener(){
                    @Override
                    public void afterPageLoad()
                    {
                        urlsSeen.add(test.getCurrentRelativeURL());
                    }
                });
            }

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
            catch (UnreachableBrowserException log)
            {
                log.printStackTrace(System.out);
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
