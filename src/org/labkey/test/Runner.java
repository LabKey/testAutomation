/*
 * Copyright (c) 2008-2019 LabKey Corporation
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

import junit.framework.AssertionFailedError;
import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.runner.BaseTestRunner;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.labkey.junit.runner.WebTestProperties;
import org.labkey.serverapi.reader.Readers;
import org.labkey.serverapi.writer.PrintWriters;
import org.labkey.test.aspects.TestPerfAspect;
import org.labkey.test.categories.Continue;
import org.labkey.test.categories.Empty;
import org.labkey.test.teamcity.TeamCityUtils;
import org.labkey.test.testpicker.TestHelper;
import org.labkey.test.tests.JUnitTest;
import org.labkey.test.util.Crawler;
import org.labkey.test.util.DevModeOnlyTest;
import org.labkey.test.util.ExportDiagnosticsPseudoTest;
import org.labkey.test.util.NonWindowsTest;
import org.labkey.test.util.PostgresOnlyTest;
import org.labkey.test.util.SqlserverOnlyTest;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.WindowsOnlyTest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.labkey.test.WebTestHelper.logToServer;

public class Runner extends TestSuite
{
    private static final Logger LOG = LogManager.getLogger(Runner.class);

    private static final int DEFAULT_MAX_TEST_FAILURES = 10;
    private static SuiteFactory _suites = SuiteFactory.getInstance();
    private static Map<Test, Long> _testStats = new LinkedHashMap<>();
    private static int _testCount;
    private static List<Class<?>> _remainingTests;
    private static List<String> _passedTests = new ArrayList<>();
    private static List<String> _failedTests = new ArrayList<>();
    private static List<String> _erroredTests = new ArrayList<>();

    private Set<TestFailure> _failures = new HashSet<>();
    private boolean _cleanOnly;

    private Runner(boolean cleanOnly)
    {
        _cleanOnly = cleanOnly;
    }

    private void updateRemainingTests(Test test, boolean failed, boolean errored)
    {
        Class<?> testClass = getTestClass(test);
        _remainingTests.remove(testClass);
        if (failed)
            _failedTests.add(test.toString());
        else if (errored)
            _erroredTests.add(test.toString());
        else
            _passedTests.add(test.toString());
    }

    private static void writeRemainingTests()
    {
        ArrayList<String> failedAndRemaining = new ArrayList<>();
        failedAndRemaining.addAll(_failedTests);
        failedAndRemaining.addAll(_erroredTests);
        for (Class<?> clazz : _remainingTests)
            failedAndRemaining.add(clazz.getName());
        writeClasses(failedAndRemaining, getRemainingTestsFile());
    }

    private static void writeClasses(List<String> tests, File file)
    {
        try(PrintWriter pw = PrintWriters.getPrintWriter(file))
        {
            for (String test : tests)
                pw.println(test);
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
   }

    private static List<Class<?>> readClasses(File file)
    {
        List<Class<?>> testClasses = new ArrayList<>();

        if (file.exists())
        {
            String line = null;

            try (BufferedReader reader = Readers.getReader(file))
            {
                while ((line = reader.readLine()) != null)
                    if (null != StringUtils.trimToNull(line))
                    {
                        if (line.contains("$")) // Prevent exception for Junit tests.
                            testClasses.add(Class.forName(line.substring(0, line.indexOf('$'))));
                        else
                            testClasses.add(Class.forName(line));
                    }
            }
            catch (IOException e)
            {
                LOG.warn("Error reading " + file.getAbsolutePath(), e);
            }
            catch (ClassNotFoundException e)
            {
                LOG.warn("Can't find class '" + line + "'", e);
            }
        }

        return testClasses;
    }

    private static Class<?>[] readClasses(File recentlyFailedTestsFile, List<Class<?>> tests)
    {
        List<Class<?>> recentlyFailedTests = readClasses(recentlyFailedTestsFile);
        ArrayList<Class<?>> filteredRecentlyFailedTests = new ArrayList<>();

        for (Class<?> item: recentlyFailedTests)
        {
            if (tests.contains(item))
            {
                filteredRecentlyFailedTests.add(item);
            }
        }
        
        return filteredRecentlyFailedTests.toArray(new Class[0]);
    }

    private static File getRemainingTestsFile()
    {
        return new File(TestFileUtils.getTestRoot(), "remainingTests.txt");
    }

    private String getProgress()
    {
        int completed = _testCount -_remainingTests.size() + 1;
        return " (" + completed + " of " + _testCount + ")";
    }

    public static boolean isFinalTest()
    {
        return 0 == (_remainingTests.size() - 1);
    }

    private static void saveTestDuration(Test currentWebTest, long durationMs)
    {
        _testStats.put(currentWebTest, durationMs);
    }

    @Override
    public synchronized void runTest(final Test test, final TestResult testResult)
    {
        long startTimeMs = System.currentTimeMillis();
        if (_cleanOnly)
        {
            try
            {
                if (test instanceof TestSuite)
                {
                    Enumeration<Test> en = ((TestSuite) test).tests();
                    while (en.hasMoreElements())
                    {
                        Test singleTest = en.nextElement();
                        if (singleTest instanceof Cleanable)
                            ((Cleanable) singleTest).cleanup();
                    }
                }
                else if (test instanceof Cleanable)
                    ((Cleanable) test).cleanup();
                else if (test instanceof JUnit4TestAdapter)
                {
                    JUnit4TestAdapter adapter = (JUnit4TestAdapter) test;
                    if (Cleanable.class.isAssignableFrom(adapter.getTestClass()))
                    {
                        Cleanable cleanable = (Cleanable) adapter.getTestClass().getDeclaredConstructor().newInstance();
                        cleanable.cleanup();
                    }
                }
            }
            catch (Throwable t)
            {
                LOG.warn("WARNING: failure cleaning test: " + t.getMessage(), t);
                // fall through
            }
        }
        else
        {
            final MutableBoolean failed = new MutableBoolean(false);
            final MutableBoolean errored = new MutableBoolean(false);

            int _maxTestFailures;
            _maxTestFailures = Integer.getInteger("maxTestFailures", DEFAULT_MAX_TEST_FAILURES); // 0 is unlimited

            if (_failedTests.size() + _erroredTests.size() < _maxTestFailures || _maxTestFailures <= 0)
            {
                final Class<?> currentTestClass = getTestClass(test);
                final String currentTestName = currentTestClass.getSimpleName();

                TestListener classFailListener = new TestListener()
                {
                    @Override
                    public void startTest(Test _test) { }

                    @Override
                    public void addError(Test _test, Throwable e)
                    {
                        errored.setTrue();
                    }

                    @Override
                    public void addFailure(Test _test, AssertionFailedError e)
                    {
                        failed.setTrue();
                    }

                    @Override
                    public void endTest(Test _test) { }
                };
                testResult.addListener(classFailListener);

                logToServer("=== Starting " + currentTestName + getProgress() + " ===");
                LOG.info("=============== Starting " + currentTestName + getProgress() + " =================");

                // This stub matches the failure generated by JUnit when it fails during static setup/teardown (e.g. @BeforeClass)
                // Without this TeamCity has no way of knowing when a setup/teardown failure has been resolved
                final Test loggingStub = test instanceof JUnit4TestAdapter ?
                        new TestSuite(currentTestClass) :
                        null;

                if (loggingStub != null)
                    testResult.startTest(loggingStub);

                test.run(testResult);

                if (loggingStub != null)
                    testResult.endTest(loggingStub);

                testResult.removeListener(classFailListener);

                String result = failed.booleanValue() || errored.booleanValue() ? "Failed " : "Completed ";
                TestLogger.resetLogger();
                LOG.info("=============== " + result + currentTestName + getProgress() + " =================");
                logToServer("=== " + result + currentTestName + getProgress() + " ===");

            }
            else
            {
                testResult.addError(test, new Throwable(test.toString() + " not run: reached " + _maxTestFailures + " failures."));
                errored.setTrue();
            }

            if (failed.booleanValue())
                dumpFailures(testResult.failures());
            if (errored.booleanValue())
                dumpFailures(testResult.errors());
            updateRemainingTests(test, failed.booleanValue(), errored.booleanValue());
            writeRemainingTests();
        }

        long testTimeMs = System.currentTimeMillis() - startTimeMs;
        saveTestDuration(test, testTimeMs);
        TestPerfAspect.savePerfStats(test);

        if (_remainingTests.isEmpty())
        {
            writeTimeReport();
            if (_failedTests.isEmpty() && _erroredTests.isEmpty())
            {
                getRemainingTestsFile().deleteOnExit();
            }
        }
    }

    private void dumpFailures(Enumeration<TestFailure> failures)
    {
        while (failures.hasMoreElements())
        {
            TestFailure failure = failures.nextElement();
            if (!_failures.contains(failure))
            {
                _failures.add(failure);
                LOG.info("");
                LOG.info(failure.failedTest());
                LOG.info(BaseTestRunner.getFilteredTrace(failure.trace()));
            }
        }
    }

    private static Map<Class<?>, List<String>> specifiedTestMethods = new HashMap<>();
    // Set up only the requested tests
    private static List<Class<?>> getTestClasses(List<String> testNames)
    {
        List<Class<?>> testClasses = new ArrayList<>(testNames.size());

        for (String testName : testNames)
        {
            String testClassName;
            List<String> testMethods = null;

            if (testName.contains("."))
            {
                String[] splitTestName = testName.split("\\.");
                testClassName = splitTestName[0];
                testMethods = Arrays.asList(Arrays.copyOfRange(splitTestName, 1, splitTestName.length));
            }
            else
                testClassName = testName;

            Class<?> testClass = _suites.getTestByName(testClassName);
            if (testClass == null)
            {
                LOG.error("Couldn't find test '" + testClassName + "'.  Valid tests are:");

                List<String> sortedTests = _suites.getAllTests().getTestNames();
                Collections.sort(sortedTests);

                for (String c : sortedTests)
                    LOG.error("    " + c);
                throw new IllegalArgumentException("Couldn't find test '" + testClassName + "'. Check log for details.");
            }
            testClasses.add(testClass);
            if (testMethods != null)
                specifiedTestMethods.put(testClass, testMethods);
        }

        return testClasses;
    }

    private static TestSuite getSuite(List<Class<?>> testClasses, boolean cleanOnly) throws Exception
    {
        // Remove duplicate tests (e.g., don't run "basic" test twice if bvt & drt are selected via ant test) but keep the order
        Set<Class<?>> testClassesCopy = new LinkedHashSet<>(testClasses);
        TestSuite suite = new Runner(cleanOnly);

        addTests(suite, testClassesCopy);

        return suite;
    }

    private static void addTests(TestSuite suite, Set<Class<?>> testClasses)
    {
        boolean foundServerSideTest = false;
        for (Class<?> testClass : testClasses)
        {
            Test test = null;
            try
            {
                Method suiteMethod = testClass.getMethod("suite");
                test = (TestSuite)suiteMethod.invoke(null);
            }
            catch (NoSuchMethodException e)
            {
                // ok
            }
            catch (InvocationTargetException | IllegalAccessException e)
            {
                test = new ErrorTest(testClass.getName(), e.getCause());
            }

            if (test == null)
            {
                List<Class<?>> interfaces = ClassUtils.getAllInterfaces(testClass);
                WebTestHelper.DatabaseType databaseType = WebTestHelper.getDatabaseType();
                if (interfaces.contains(PostgresOnlyTest.class) && databaseType != WebTestHelper.DatabaseType.PostgreSQL)
                {
                    LOG.warn("** Skipping " + testClass.getSimpleName() + " test for unsupported database: " + databaseType);
                    continue;
                }
                else if (interfaces.contains(SqlserverOnlyTest.class) && databaseType != WebTestHelper.DatabaseType.MicrosoftSQLServer)
                {
                    LOG.warn("** Skipping " + testClass.getSimpleName() + " test for unsupported database: " + databaseType);
                    continue;
                }

                if(interfaces.contains(DevModeOnlyTest.class) && !TestProperties.isDevModeEnabled())
                {
                    LOG.warn("** Skipping " + testClass.getSimpleName() + ": server must be in dev mode");
                    continue;
                }
                else if(interfaces.contains(WindowsOnlyTest.class) && !SystemUtils.IS_OS_WINDOWS)
                {
                    LOG.warn("** Skipping " + testClass.getSimpleName() + " test for unsupported operating system: " + SystemUtils.OS_NAME);
                    continue;
                }
                else if(interfaces.contains(NonWindowsTest.class) && SystemUtils.IS_OS_WINDOWS)
                {
                    LOG.warn("** Skipping " + testClass.getSimpleName() + " test for unsupported operating system: " + SystemUtils.OS_NAME);
                    continue;
                }
                test = new JUnit4TestAdapter(testClass);

                if (specifiedTestMethods.containsKey(testClass))
                {
                    final List<String> testNames = specifiedTestMethods.get(testClass);
                    final Set<String> unfoundTests = new HashSet<>(specifiedTestMethods.get(testClass));
                    final Set<String> foundTests = new HashSet<>();
                    final Set<String> ignoredTests = new HashSet<>();

                    Filter testNameFilter = new Filter()
                    {
                        @Override
                        public boolean shouldRun(Description description)
                        {
                            String methodName = description.getMethodName();

                            if (description.getAnnotation(Ignore.class) != null)
                            {
                                ignoredTests.add(methodName);
                                return false;
                            }

                            foundTests.add(methodName);

                            if (testNames.contains(methodName))
                            {
                                unfoundTests.remove(methodName);
                                return true;
                            }
                            else
                                return false;
                        }

                        @Override
                        public String describe()
                        {
                            return "Tests specified on command line";
                        }
                    };

                    try
                    {
                        ((Filterable)test).filter(testNameFilter);
                    }
                    catch (NoTestsRemainException ignore) {}

                    if (unfoundTests.size() > 0)
                    {
                        LOG.error("Test(s) do not exist in class " + testClass.getSimpleName());
                        LOG.error("Specified:");
                        for (String unfoundTest : unfoundTests)
                        {
                            LOG.error("    " + unfoundTest);
                        }
                        LOG.error("Found:");
                        for (String foundTest : foundTests)
                        {
                            LOG.error("    " + foundTest);
                        }
                        if (ignoredTests.size() > 0)
                            LOG.error("Disabled:");
                        for (String ignoredTest : ignoredTests)
                        {
                            LOG.error("    " + ignoredTest);
                        }
                        throw new IllegalArgumentException("Couldn't find test(s) [" + String.join(", ", unfoundTests) + "] in class '" + testClass.getSimpleName() + "'. Check log for details.");
                    }
                }

                suite.addTest(test);
            }
            else if (test.countTestCases() > 0)
            {
                suite.addTest(test);
                foundServerSideTest = true;
            }
        }

        if (!foundServerSideTest && BatchInfo.get().isLastBatch())
        {
            // Automatically run server-side tests based on 'suite' parameter
            // if standard JUnitTest isn't already included
            List<String> specifiedSuites = getSpecifiedSuites();
            Set<String> requestedSuites = new HashSet<>();
            Set<String> excludedSuites = new HashSet<>();
            for (String specifiedSuite : specifiedSuites)
            {
                if (specifiedSuite.startsWith("-") && specifiedSuite.length() > 1)
                    excludedSuites.add(specifiedSuite.substring(1));
                else if (specifiedSuite.startsWith("?") && specifiedSuite.length() > 1)
                    requestedSuites.add(specifiedSuite.substring(1));
                else
                    requestedSuites.add(specifiedSuite);
            }
            TestSuite dynamicSuite = JUnitTest.dynamicSuite(requestedSuites, excludedSuites);
            if (dynamicSuite.countTestCases() > 0)
                suite.addTest(dynamicSuite);
        }
    }

    // for error reporting
    public static class ErrorTest extends TestCase
    {
        Throwable t;

        public ErrorTest(String name, Throwable t)
        {
            super(name);
            this.t = t;
        }

        @Override
        public String toString()
        {
            return getName();
        }

        @Override
        public void run(TestResult testResult)
        {
            testResult.startTest(this);
            testResult.addError(this, t);
            testResult.endTest(this);
        }
    }

    private static void writeTimeReport()
    {
        int width = 60;
        long total = 0;
        LOG.info("======================= Time Report ========================");

        Duration totalCrawlTime = Duration.ZERO;
        int totalUniquePages = 0;
        int totalUniqueActions = 0;
        Set<String> crawlWarnings = new HashSet<>();
        boolean crawl = false;

        for (Map.Entry<Test, Long> entry : _testStats.entrySet())
        {
            total += entry.getValue();
        }
        for (Map.Entry<Test, Long> entry : _testStats.entrySet())
        {
            String testName = entry.getKey().toString();
            long duration = entry.getValue();

            long percent = Math.round(100.0 * (duration / (double) total));
            String percentStr = (percent < 10 ? " " : "") + percent + "%";
            String durationAndPercent =
                (_passedTests.contains(testName) ? "passed" :
                    (_failedTests.contains(testName) ? "FAILED" :
                        (_erroredTests.contains(testName) ? "ERROR" : "not run"))) +
                " - " +
                formatDuration(duration) + " " + percentStr;
            testName = testName.substring(testName.lastIndexOf('.') + 1);

            LOG.info(getFixedWidthString(testName, durationAndPercent, width));

            // TODO: TestPerfAspect isn't working correctly now that we don't use @AfterClass
//            for (Map.Entry<TestPerfAspect.TestSection, Long> stats : TestPerfAspect.getPerfStats(testName).entrySet())
//            {
//                System.out.println("\t" + stats.getKey() + "\t" + formatDuration(stats.getValue()));
//            }

            if (Crawler.getCrawlStats().containsKey(testName))
            {
                crawl = true;
                Crawler.CrawlStats crawlStats = Crawler.getCrawlStats().get(testName);
                LOG.info(getFixedWidthString("Crawler Statistics: ", "", width));

                String[] statTitles = {"MaxDepth", "CrawlTime", "NewPages", "NewActions"};
                String[] stats = { (Integer.toString(crawlStats.getMaxDepth())),
                        crawlStats.getCrawlTestLength().toString().replace("PT", "").replaceAll("\\.\\d+", ""),
                        Integer.toString(crawlStats.getNewPages()),
                        Integer.toString((crawlStats.getUniqueActions() - totalUniqueActions)) };

                int columnWidth = 10;
                LOG.info(getRowString(statTitles, columnWidth));
                LOG.info(getRowString(stats, columnWidth));

                totalCrawlTime = totalCrawlTime.plus(crawlStats.getCrawlTestLength());
                totalUniquePages += crawlStats.getNewPages();
                totalUniqueActions = Math.max(crawlStats.getUniqueActions(), totalUniqueActions);
                crawlWarnings.addAll(crawlStats.getWarnings());
            }
        }
        if (crawl)
        {
            LOG.info("");
            LOG.info(getFixedWidthString("Total Crawler Statistics: ", "", width));

            String[] statTitles = {"TotCrawlTime", "TotPages", "TotActions"};
            String[] stats = { totalCrawlTime.toString().replace("PT", "").replaceAll("\\.\\d+", ""),
                    Integer.toString(totalUniquePages),
                    Integer.toString(totalUniqueActions) };
            int columnWidth = 13;
            LOG.info(getRowString(statTitles, columnWidth));
            LOG.info(getRowString(stats, columnWidth));
            if (!crawlWarnings.isEmpty())
            {
                LOG.info("");
                LOG.info(getFixedWidthString("Crawler Warnings: ", "", width));
                for (String warning : crawlWarnings)
                {
                    LOG.info("  " + warning);
                }
            }
        }
        if (!TeamCityUtils.getBuildStatistics().isEmpty())
        {
            LOG.info("--------------------- Build Statistics ---------------------");
            for (String stat : TeamCityUtils.getBuildStatistics().keySet())
            {
                List<Number> values = TeamCityUtils.getBuildStatistics().get(stat);
                String valueStr;
                if (values.size() == 1)
                    valueStr = String.valueOf(values.get(0));
                else
                    valueStr = values.toString();

                LOG.info(" " + stat + " = " + valueStr);
            }
        }
        Map<String, Collection<String>> actionWarnings = WebDriverWrapper.getActionWarnings();
        if (!actionWarnings.isEmpty())
        {
            LOG.info("---------------------- Test Warnings -----------------------");
            for (String warning : actionWarnings.keySet())
            {
                LOG.info("  " + warning + ":");
                List<String> actions = new ArrayList<>(actionWarnings.get(warning));
                Collections.sort(actions);
                for (String action : actions)
                {
                    LOG.info("    " + action);
                }
            }
        }
        LOG.info("------------------------------------------------------------");
        LOG.info(getFixedWidthString("Total duration:", formatDuration(total), width) + "\n");
        LOG.info("Completed " + FastDateFormat.getInstance("yyyy-MM-dd HH:mm").format(new Date()));
    }

    private static String formatDuration(long ms)
    {
        long min = ms / DateUtils.MILLIS_PER_MINUTE;
        ms = ms % DateUtils.MILLIS_PER_MINUTE;
        long sec = ms / DateUtils.MILLIS_PER_SECOND;
        return min + ":" + (sec < 10 ? "0" : "") + sec;
    }

    private static String getRowString(String[] list, int columnWidth)
    {
        String rowString = "";
        for (String item : list)
        {
            if (item.length() > columnWidth)
                item = item.substring(0, columnWidth);
            while (item.length() < columnWidth)
                item = item.concat(" ");
            rowString = rowString.concat(item);
        }
        return rowString;
    }

    private static String getFixedWidthString(String prefix, String suffix, int length)
    {
        int contentLength = prefix.length() + suffix.length();
        int padding = Math.max(0, length - contentLength);
        return prefix + " ".repeat(padding) + suffix;
    }

    private static TestSet getCompositeTestSet(List<String> suitesColl)
    {
        if (suitesColl.isEmpty())
            return _suites.getEmptyTestSet();
        if (suitesColl.size() == 1)
            return getSuite(suitesColl.get(0));

        TestSet tests = new TestSet();
        List<String> includeSuites = new ArrayList<>();
        List<String> excludeSuites = new ArrayList<>();

        suitesColl.forEach(s ->
        {
            if (s.startsWith("-"))
                excludeSuites.add(s.substring(1));
            else
                includeSuites.add(s);
        });

        for (String includedSuite : includeSuites)
        {
            tests.addTests(getSuite(includedSuite));
        }

        for (String excludedSuite : excludeSuites)
        {
            tests.removeTests(getSuite(excludedSuite));
        }

        return tests;
    }

    private static TestSet getSuite(String suiteName)
    {
        TestSet testSet = _suites.getTestSet(suiteName);
        if (testSet == null)
        {
            List<String> sortedSuites = new ArrayList<>(_suites.getSuites());
            Collections.sort(sortedSuites);

            LOG.error("Couldn't find suite '" + suiteName + "'.  Valid suites are:");
            for (String suite : sortedSuites)
                LOG.error("   " + suite);
            throw new IllegalArgumentException("Couldn't find suite '" + suiteName + "'. Check log for details.");
        }
        return testSet;
    }

    protected static List<String> getSpecifiedSuites()
    {
        String suites = StringUtils.trimToEmpty(System.getProperty("suite"));
        Set<String> suiteNames = Collections.newSetFromMap(new CaseInsensitiveMap<>());
        suiteNames.addAll(Arrays.asList(suites.split("\\s*,\\s*")));
        suiteNames.removeAll(Arrays.asList("Test", ""));
        return new ArrayList<>(suiteNames);
    }

    protected static List<String> getTestNames(String namesProperty)
    {
        String testNames = StringUtils.trimToNull(namesProperty);
        return testNames == null
                ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(testNames.split("\\s*,\\s*")));
    }

    /** Entry point for Ant JUnit runner. */
    public static Test suite() throws Throwable
    {
        try
        {
            final String allTestsSuite = org.labkey.test.categories.Test.class.getSimpleName();
            List<String> testNames = getTestNames(System.getProperty("test"));
            TestSet set;
            if (testNames.isEmpty())
            {
                final List<String> specifiedSuites = getSpecifiedSuites();
                set = BatchInfo.get().getBatch(getCompositeTestSet(specifiedSuites));
            }
            else
            {
                LOG.info("Custom test list specified. Ignoring specified suite(s).");
                set = getCompositeTestSet(Collections.singletonList(allTestsSuite));
            }

            if ((set.getSuite().equalsIgnoreCase(allTestsSuite) || set.getSuite().equalsIgnoreCase(Empty.class.getSimpleName())) && testNames.isEmpty())
            {
                String fileName = System.getProperty("dumpTsv", "").trim();

                if (!fileName.isEmpty())
                {
                    dumpTsv(fileName);
                    return new TestSuite();
                }
                else if (!TestProperties.isTestRunningOnTeamCity())
                {
                    TestHelper.ResultPair pair = TestHelper.run();
                    if (pair != null)
                    {
                        set = pair.set;
                        testNames = pair.testNames;
                    }
                }
            }

            return suite(testNames, set);
        }
        catch (InvocationTargetException e)
        {
            System.err.print(BaseTestRunner.getFilteredTrace(e.getTargetException()));
            throw e.getTargetException();
        }
        catch (Throwable e)
        {
            System.err.print(BaseTestRunner.getFilteredTrace(e));
            throw e;
        }
    }

    private static void dumpTsv(String fileName)
    {
        Map<String, Set<String>> testsSuites = new TreeMap<>();
        Map<String, Class<?>> testsClasses = new HashMap<>();

        List<String> nonSuites = Arrays.asList("daily", "weekly", "test", "continue", "empty");
        List<String> suites = new ArrayList<>(_suites.getSuites()).stream()
                .filter(s -> !nonSuites.contains(s.toLowerCase())) // Ignore non-suites and suites that don't get run regularly
                .sorted(Comparator.comparingInt((String s) -> _suites.getTestSet(s).getTestList().size()).reversed())
                .collect(Collectors.toList());

        for (String suite : suites)
        {
            TestSet testSet = _suites.getTestSet(suite);

            for (Class<?> test : testSet.getTestList())
            {
                String testName = test.getSimpleName();
                if (!testsSuites.containsKey(testName))
                {
                    testsSuites.put(testName, Collections.newSetFromMap(new CaseInsensitiveMap<>()));
                    testsClasses.put(testName, test);
                }
                testsSuites.get(testName).add(suite);
            }
        }

        Set<String> nightlySuites = Collections.newSetFromMap(new CaseInsensitiveMap<>());
        nightlySuites.addAll(Arrays.asList("BVT", "Daily", "Git", "CustomModules", "EHR"));
        File dumpFile = new File(fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dumpFile)))
        {
            List<Class<?>> checkedInterfaces = Arrays.asList(PostgresOnlyTest.class, SqlserverOnlyTest.class, WindowsOnlyTest.class, NonWindowsTest.class, DevModeOnlyTest.class);
            writer.write(String.format("Test\tNightly Suites\tSuites\tTimeout\tpackage\t%s\t%s\n",
                    checkedInterfaces.stream().map(Class::getSimpleName).collect(Collectors.joining("\t")),
                    String.join("\t", suites)));
            for (String testName : testsSuites.keySet())
            {
                Class<?> testClass = testsClasses.get(testName);
                String line = testName + "\t" + // Test
                        testsSuites.get(testName).stream().filter(nightlySuites::contains).collect(Collectors.joining(", ")) + "\t" + // Nightly Suites
                        String.join(", ", testsSuites.get(testName)) + "\t" + // Suites
                        getTestTimeout(testClass) + "\t" + // Timeout
                        testClass.getPackage().getName() + "\t" + // Package
                        checkedInterfaces.stream().map(i -> i.isAssignableFrom(testClass) ? i.getSimpleName() : "").collect(Collectors.joining("\t")) + "\t" + // Interfaces
                        suites.stream().map(s -> testsSuites.get(testName).contains(s) ? s : "").collect(Collectors.joining("\t")) + "\t" + // Suites
                        "\n";
                writer.write(line);
            }
            writer.flush();
        }
        catch (IOException ioe)
        {
            throw new RuntimeException("Unable to dump test list", ioe);
        }

        LOG.info("Test list dumped to: " + dumpFile.getAbsolutePath());
    }

    private static int getTestTimeout(Class<?> testClass)
    {
        BaseWebDriverTest.ClassTimeout timeout = testClass.getAnnotation(BaseWebDriverTest.ClassTimeout.class);
        if (timeout != null)
            return timeout.minutes();
        return -1;
    }

    public static TestSuite suite(List<String> testNames, TestSet set) throws Exception
    {
        boolean skipLeakCheck = "false".equals(System.getProperty("memCheck"));
        boolean disableAssertions = "true".equals(System.getProperty("disableAssertions"));
        boolean cleanOnly = "true".equals(System.getProperty("cleanOnly"));
        boolean shuffleTests = "true".equals(System.getProperty("shuffleTests"));
        boolean testRecentlyFailed = "true".equals(System.getProperty("testRecentlyFailed"));
        boolean testNewAndModified = "true".equals(System.getProperty("testNewAndModified"));
        String recentlyFailedTestsFile = System.getProperty("teamcity.tests.recentlyFailedTests.file");
        List<String> additionalTestNames = getTestNames(System.getProperty("addToSuite"));
        String removeFromSuite = System.getProperty("removeFromSuite");
        List<String> excludedTestNames = getTestNames(removeFromSuite);

        if (StringUtils.trimToEmpty(removeFromSuite).contains("."))
        {
            throw new IllegalArgumentException("It looks like you are trying to prevent an individual test method from executing. That is not currently supported. removeFromSuite=" + removeFromSuite);
        }

        if (!skipLeakCheck && disableAssertions)
        {
            throw new IllegalArgumentException("Invalid parameters: 'memCheck = true' and 'disableAssertions = true'.  Unable to do leak check with assertions disabled.");
        }

        if (Continue.class.getSimpleName().equalsIgnoreCase(set.getSuite()))
        {
            set.setTests(readClasses(getRemainingTestsFile()));
            if (shuffleTests)
            {
                set.randomizeTests();
            }
        }
        else if (org.labkey.test.categories.Test.class.getSimpleName().equalsIgnoreCase(set.getSuite()) && testNames.isEmpty())
        {
            set.setTests(new ArrayList<>());
        }
        else if (testNames.isEmpty())
        {
            if (!additionalTestNames.isEmpty())
            {
                set.addTests(getTestClasses(additionalTestNames));
            }
            if (!excludedTestNames.isEmpty())
            {
                set.removeTests(getTestClasses(excludedTestNames));
            }
            if (shuffleTests)
            {
                set.randomizeTests();
            }
            if (testNewAndModified)
            {
                frontLoadTestsOfModifiedModules(set);
            }
            if (testRecentlyFailed && 0<recentlyFailedTestsFile.length())
            {
                //put previously failed tests at the front of the test queue (determined by TeamCity).
                Class<?>[] recentlyFailedTests = readClasses(new File(recentlyFailedTestsFile), set.getTestList());
                for (Class<?> test: recentlyFailedTests)
                {
                    set.prioritizeTest(test, 0);
                }
            }
        }

        List<Class<?>> testClasses = testNames.isEmpty() ? set.getSortedTestList() : getTestClasses(testNames);

        if (TestProperties.isServerRemote() && TestProperties.isTestRunningOnTeamCity() || TestProperties.isDiagnosticsExportEnabled())
        {
            testClasses.add(ExportDiagnosticsPseudoTest.class);
        }

        TestSuite suite = getSuite(testClasses, cleanOnly);

        if (suite.testCount() == 0)
        {
            LOG.info("No tests to run.");
        }
        else
        {
            _remainingTests = new ArrayList<>(suite.testCount());

            LOG.info("Running the following tests:");
            for (Enumeration<Test> e = suite.tests(); e.hasMoreElements(); )
            {
                Test test = e.nextElement();
                Class<?> testClass = getTestClass(test);
                _remainingTests.add(testClass);
                LOG.info("  " + testClass.getSimpleName());
                for (String testMethod : specifiedTestMethods.getOrDefault(testClass, Collections.emptyList()))
                {
                    LOG.info("    ." + testMethod);
                }
            }
            _testCount = _remainingTests.size();
            writeRemainingTests();
        }

        return suite;
    }

    private static void frontLoadTestsOfModifiedModules(TestSet set)
    {
        Collection<String> modifiedModules = getModifiedModules();

        // If changedFilesFile exists where TeamCity indicates then order the tests starting from most recently modified
        if (modifiedModules.size() > 0)
        {
            LOG.info("Prioritizing tests for modified modules:");
            for (String module : modifiedModules)
            {
                LOG.info("\t" + module);
            }

            int movedTests = 0;
            for (String moduleDir : modifiedModules)
            {
                Collection<Class<?>> associatedTests = WebTestProperties.getAssociatedTests(moduleDir);

                if (null != associatedTests)
                {
                    for (Class<?> test : associatedTests)
                    {
                        // Bubble up associated Test, if present.
                        if (set.prioritizeTest(test, movedTests))
                        {
                            movedTests++;
                        }
                    }
                }
            }
        }
    }

    // http://confluence.jetbrains.com/display/TCD8/Risk+Tests+Reordering+in+Custom+Test+Runner
    private static Collection<String> getModifiedModules()
    {
        File changelistFile = new File(System.getProperty("teamcity.build.changedFiles.file"));
        File checkoutDir = new File(System.getProperty("system.teamcity.build.checkoutDir"));
        Collection<String> modifiedModules = new HashSet<>();

        if (changelistFile.exists() && checkoutDir.exists())
        {
            try (BufferedReader reader = Readers.getReader(changelistFile))
            {
                String line;

                while ((line = reader.readLine()) != null)
                {
                    String [] splitLine = line.split(":");
                    if (splitLine.length != 3)
                    {
                        LOG.warn("Unexpected changelist format: " + line);
                        continue;
                    }

                    String relativeFilePath = splitLine[0];
                    String changeType = splitLine[1];

                    if ("NOT_CHANGED".equals(changeType))
                    {
                        LOG.info("File in changelist NOT_CHANGED: " + relativeFilePath);
                        continue;
                    }

                    String moduleName = getModuleNameFromPath(new File(checkoutDir, line));

                    if (moduleName != null)
                        modifiedModules.add(moduleName);
                }
            }
            catch(IOException e)
            {
                System.err.print(e.getMessage());
            }
        }

        return modifiedModules;
    }

    private static String getModuleNameFromPath(File path)
    {
        File parent = path;
        do
        {
            if (new File(parent, "module.properties").exists())
                return parent.getName();
        } while((parent = parent.getParentFile()) != null);

        LOG.warn("Unable to determine module for: " + path);
        return null;
    }

    private static Class<?> getTestClass(Test test)
    {
        if (test instanceof JUnit4TestAdapter)
            return ((JUnit4TestAdapter) test).getTestClass();
        else
            return test.getClass();
    }
}

class BatchInfo
{
    private static BatchInfo _instance;

    private final int _currentBatch;
    private final int _totalBatches;

    private BatchInfo(int currentBatch, int totalBatches)
    {
        this._currentBatch = currentBatch;
        this._totalBatches = totalBatches;
    }

    static BatchInfo get()
    {
        if (_instance == null)
        {
            String currentBatch = StringUtils.trimToNull(System.getProperty("webtest.parallelTests.currentBatch"));
            String totalBatches = StringUtils.trimToNull(System.getProperty("webtest.parallelTests.totalBatches"));
            try
            {
                _instance = new BatchInfo(Integer.parseInt(currentBatch), Integer.parseInt(totalBatches));
            }
            catch (NumberFormatException ex)
            {
                _instance = new BatchInfo(1, 1);
            }
        }
        return _instance;
    }

    int getCurrentBatch()
    {
        return _currentBatch;
    }

    int getTotalBatches()
    {
        return _totalBatches;
    }

    boolean isLastBatch()
    {
        return _currentBatch == _totalBatches;
    }

    @NotNull
    public TestSet getBatch(TestSet testSet)
    {
        return new TestSet(SuiteFactory.extractBatch(new HashSet<>(testSet.getTestList()), getCurrentBatch(), getTotalBatches()), testSet.getSuite());
    }
}
