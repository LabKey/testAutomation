/*
 * Copyright (c) 2005-2017 LabKey Corporation
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
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.labkey.api.reader.Readers;
import org.labkey.api.writer.PrintWriters;
import org.labkey.junit.runner.WebTestProperties;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.aspects.TestPerfAspect;
import org.labkey.test.categories.Continue;
import org.labkey.test.testpicker.TestHelper;
import org.labkey.test.tests.BasicTest;
import org.labkey.test.tests.DatabaseDiagnosticsTest;
import org.labkey.test.tests.JUnitTest;
import org.labkey.test.util.Crawler;
import org.labkey.test.util.DevModeOnlyTest;
import org.labkey.test.util.NonWindowsTest;
import org.labkey.test.util.PostgresOnlyTest;
import org.labkey.test.util.SqlserverOnlyTest;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.WindowsOnlyTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.labkey.test.WebTestHelper.logToServer;

public class Runner extends TestSuite
{
    private static final int DEFAULT_MAX_TEST_FAILURES = 10;
    private static final String DEFAULT_SUITE = org.labkey.test.categories.DRT.class.getSimpleName();
    private static SuiteBuilder _suites = SuiteBuilder.getInstance();
    private static Map<Test, Long> _testStats = new LinkedHashMap<>();
    private static int _testCount;
    private static List<Class> _remainingTests;
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
        Class testClass = getTestClass(test);
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
        for (Class clazz : _remainingTests)
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

    private static List<Class> readClasses(File file)
    {
        List<Class> testClasses = new ArrayList<>();

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
                System.out.println("Error reading " + file.getAbsolutePath());
            }
            catch (ClassNotFoundException e)
            {
                System.out.println("Can't find class '" + line + "'");
            }
        }

        return testClasses;
    }

    private static Class[] readClasses(File recentlyFailedTestsFile, List<Class> tests)
    {
        List<Class> recentlyFailedTests = readClasses(recentlyFailedTestsFile);
        ArrayList<Class> filteredRecentlyFailedTests = new ArrayList<>();

        for (Class item: recentlyFailedTests)
        {
            if (tests.contains(item))
            {
                filteredRecentlyFailedTests.add(item);
            }
        }
        
        return filteredRecentlyFailedTests.toArray(new Class[filteredRecentlyFailedTests.size()]);
    }

    private static File getRemainingTestsFile()
    {
        String labkeyRoot = TestFileUtils.getLabKeyRoot();
        return new File(labkeyRoot, "server/test/remainingTests.txt");
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
                        Cleanable cleanable = (Cleanable) adapter.getTestClass().newInstance();
                        cleanable.cleanup();
                    }
                }
            }
            catch (Throwable t)
            {
                System.out.println("WARNING: failure cleaning test: " + t.getMessage());
                System.out.println("Failures may be expected if the test was already cleaned.");
                t.printStackTrace(System.out);
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
                final Class currentTestClass = getTestClass(test);
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

                try
                {
                    logToServer("=== Starting " + currentTestName + getProgress() + " ===");
                }
                catch (CommandException | IOException ignore)
                {
                }
                TestLogger.log("\n\n=============== Starting " + currentTestName + getProgress() + " =================");

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
                TestLogger.log("=============== " + result + currentTestName + getProgress() + " =================");
                try
                {
                    logToServer("=== " + result + currentTestName + getProgress() + " ===");
                }
                catch (CommandException | IOException ignore)
                {
                }

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
                getRemainingTestsFile().delete();
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
                System.out.println();
                System.out.println(failure.failedTest());
                System.out.println(BaseTestRunner.getFilteredTrace(failure.trace()));
            }
        }
    }

    private static Map<Class, List<String>> specifiedTestMethods = new HashMap<>();
    // Set up only the requested tests
    private static List<Class> getTestClasses(List<String> testNames)
    {
        Map<String, Class> nameMap = new HashMap<>();
        TestSet allTests = _suites.getAllTests();
        for (Class testClass : allTests.getTestList())
        {
            String simpleName = testClass.getSimpleName().toLowerCase();
            nameMap.put(simpleName, testClass);
            if (simpleName.endsWith("test"))
            {
                simpleName = simpleName.substring(0, simpleName.length() - 4);
                nameMap.put(simpleName, testClass);
            }
            // Allow "lists", "samplesets", etc.
            if (!simpleName.endsWith("s"))
            {
                nameMap.put(simpleName + "s", testClass);
            }
        }

        List<Class> testClasses = new ArrayList<>(testNames.size());

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

            Class testClass = nameMap.get(testClassName.toLowerCase());
            if (testClass == null)
            {
                System.err.println("Couldn't find test '" + testClassName + "'.  Valid tests are:");

                List<String> sortedTests = allTests.getTestNames();
                Collections.sort(sortedTests);

                for (String c : sortedTests)
                    System.err.println("    " + c);
                throw new IllegalArgumentException("Couldn't find test '" + testClassName + "'. Check log for details.");
            }
            testClasses.add(testClass);
            if (testMethods != null)
                specifiedTestMethods.put(testClass, testMethods);
        }

        return testClasses;
    }

    private static TestSuite getSuite(List<Class> testClasses, boolean cleanOnly) throws Exception
    {
        // Remove duplicate tests (e.g., don't run "basic" test twice if bvt & drt are selected via ant test) but keep the order
        Set<Class> testClassesCopy = new LinkedHashSet<>(testClasses);
        TestSuite suite = new Runner(cleanOnly);

        addTests(suite, testClassesCopy);

        return suite;
    }

    private static void addTests(TestSuite suite, Set<Class> testClasses)
    {
        boolean foundServerSideTest = false;
        for (Class testClass : testClasses)
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
                String databaseType = System.getProperty("databaseType");
                String databaseVersion = System.getProperty("databaseVersion");
                String osName = System.getProperty("os.name", "<unknown>");
                if (!StringUtils.isEmpty(databaseType)) // Prefer to run illegal tests vs missing coverage when databaseType isn't set
                {
                    if (interfaces.contains(PostgresOnlyTest.class) && !("postgres".equals(databaseType) || "pg".equals(databaseType)))
                    {
                        System.out.println("** Skipping " + testClass.getSimpleName() + " test for unsupported database: " + databaseType + " " + databaseVersion);
                        continue;
                    }
                    else if (interfaces.contains(SqlserverOnlyTest.class) && !("sqlserver".equals(databaseType) || "mssql".equals(databaseType)))
                    {
                        System.out.println("** Skipping " + testClass.getSimpleName() + " test for unsupported database: " + databaseType + " " + databaseVersion);
                        continue;
                    }
                }

                if(interfaces.contains(DevModeOnlyTest.class) && !"true".equals(System.getProperty("devMode")))
                {
                    System.out.println("** Skipping " + testClass.getSimpleName() + ": server must be in dev mode");
                    continue;
                }
                else if(interfaces.contains(WindowsOnlyTest.class) && !osName.toLowerCase().contains("windows"))
                {
                    System.out.println("** Skipping " + testClass.getSimpleName() + " test for unsupported operating system: " + osName);
                    continue;
                }
                else if(interfaces.contains(NonWindowsTest.class) && osName.toLowerCase().contains("windows"))
                {
                    System.out.println("** Skipping " + testClass.getSimpleName() + " test for unsupported operating system: " + osName);
                    continue;
                }
                test = new JUnit4TestAdapter(testClass);

                if (specifiedTestMethods.containsKey(testClass))
                {
                    final List<String> testNames = specifiedTestMethods.get(testClass);
                    final Set<String> unfoundTests = new HashSet<>(specifiedTestMethods.get(testClass));
                    final Set<String> foundTests = new HashSet<>();
                    final Set<String> ignoredTests = new HashSet<>();

                    org.junit.runner.manipulation.Filter testNameFilter = new Filter()
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
                        System.err.println("Test(s) do not exist in class " + testClass.getSimpleName());
                        System.err.println("Specified:");
                        for (String unfoundTest : unfoundTests)
                        {
                            System.err.println("    " + unfoundTest);
                        }
                        System.err.println("Found:");
                        for (String foundTest : foundTests)
                        {
                            System.err.println("    " + foundTest);
                        }
                        if (ignoredTests.size() > 0)
                            System.err.println("Disabled:");
                        for (String ignoredTest : ignoredTests)
                        {
                            System.err.println("    " + ignoredTest);
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

        if (!foundServerSideTest)
        {
            // Automatically run server-side tests based on 'suite' parameter
            // if standard JUnitTest isn't already included
            List<String> specifiedSuites = getSpecifiedSuites();
            Set<String> requestedSuites = new HashSet<>();
            Set<String> excludedSuites = new HashSet<>();
            for (String specifiedSuite : specifiedSuites)
            {
                if (specifiedSuite.startsWith("-"))
                    excludedSuites.add(specifiedSuite.substring(1));
                else if (specifiedSuite.startsWith("?"))
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
        System.out.println("======================= Time Report ========================");

        int totalCrawlTime = 0;
        int totalUniquePages = 0;
        int totalUniqueActions = 0;
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

            System.out.println(getFixedWidthString(testName, durationAndPercent, width));

            for (Map.Entry<TestPerfAspect.TestSection, Long> stats : TestPerfAspect.getPerfStats(testName).entrySet())
            {
                System.out.println("\t" + stats.getKey() + "\t" + formatDuration(stats.getValue()));
            }

            if (Crawler.getCrawlStats().containsKey(testName))
            {
                crawl = true;
                Crawler.CrawlStats crawlStats = Crawler.getCrawlStats().get(testName);
                System.out.println(getFixedWidthString("Crawler Statistics: ", "", width));

                String[] statTitles = {"MaxDepth", "CrawlTime", "NewPages", "NewActions"};
                int crawlTestLengthSeconds = ((crawlStats.getCrawlTestLength() / 1000) % 60);
                String[] stats = { (Integer.toString(crawlStats.getMaxDepth())),
                        ((crawlStats.getCrawlTestLength() / 60000) + ":" + (crawlTestLengthSeconds >= 10 ? crawlTestLengthSeconds : ("0" + crawlTestLengthSeconds))),
                        Integer.toString(crawlStats.getNewPages()),
                        Integer.toString((crawlStats.getUniqueActions() - totalUniqueActions)) };

                int columnWidth = 10;
                System.out.println(getRowString(statTitles, columnWidth));
                System.out.println(getRowString(stats, columnWidth));

                totalCrawlTime += crawlStats.getCrawlTestLength();
                totalUniquePages += crawlStats.getNewPages();
                totalUniqueActions = crawlStats.getUniqueActions();
            }
        }
        if (crawl)
        {
            System.out.println(getFixedWidthString("Total Crawler Statistics: ", "", width));

            String[] statTitles = {"TotCrawlTime", "TotPages", "TotActions"};
            int totalCrawlTimeSeconds = ((totalCrawlTime / 1000) % 60);
            String[] stats = { ((totalCrawlTime / 60000) + ":" + (totalCrawlTimeSeconds >= 10 ? totalCrawlTimeSeconds : ("0" + totalCrawlTimeSeconds))),
                    Integer.toString(totalUniquePages),
                    Integer.toString(totalUniqueActions) };
            int columnWidth = 13;
            System.out.println(getRowString(statTitles, columnWidth));
            System.out.println(getRowString(stats, columnWidth));
        }
        System.out.println("------------------------------------------------------------");
        System.out.println(getFixedWidthString("Total duration:", formatDuration(total), width) + "\n");
        System.out.println("Completed " + FastDateFormat.getInstance("yyyy-MM-dd HH:mm").format(new Date()));
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
        StringBuilder result = new StringBuilder();
        result.append(prefix);
        for (int i = 0; i < padding; i++)
            result.append(" ");
        result.append(suffix);
        return result.toString();
    }

    private static TestSet getCompositeTestSet(List<String> suitesColl)
    {
        if (suitesColl.isEmpty())
            _suites.getTestSet(DEFAULT_SUITE);
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
        try
        {
            return _suites.getTestSet(suiteName);
        }
        catch (Exception e)
        {
            List<String> sortedSuites = new ArrayList<>(_suites.getSuites());
            Collections.sort(sortedSuites);

            System.err.println("Couldn't find suite '" + suiteName + "'.  Valid suites are:");
            for (String suite : sortedSuites)
                System.err.println("   " + suite);
            throw new IllegalArgumentException("Couldn't find suite '" + suiteName + "'. Check log for details.");
        }
    }

    protected static List<String> getSpecifiedSuites()
    {
        String suites = StringUtils.trimToEmpty(System.getProperty("suite"));
        return new ArrayList<>(Arrays.asList(suites.split("\\s*,\\s*")));
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
            final List<String> specifiedSuites = getSpecifiedSuites();
            if (testNames.isEmpty())
            {
                set = getCompositeTestSet(specifiedSuites);
            }
            else
            {
                System.out.println("Custom test list specified. Ignoring specified suite(s): " + specifiedSuites);
                set = getCompositeTestSet(Collections.singletonList(allTestsSuite));
            }

            if (set.getSuite().equalsIgnoreCase(allTestsSuite) && testNames.isEmpty())
            {
                TestHelper.ResultPair pair = TestHelper.run();
                if (pair != null)
                {
                    set = pair.set;
                    testNames = pair.testNames;
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

    public static TestSuite suite(List<String> testNames, TestSet set) throws Exception
    {
        boolean skipLeakCheck = "false".equals(System.getProperty("memCheck"));
        boolean disableAssertions = "true".equals(System.getProperty("disableAssertions"));
        boolean cleanOnly = "true".equals(System.getProperty("cleanOnly"));
        boolean skipClean = "false".equals(System.getProperty("clean"));
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

        if (cleanOnly && skipClean)
        {
            throw new IllegalArgumentException("Invalid parameters: cannot specify both 'cleanOnly=true' and 'clean=false'.");
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
                Class[] recentlyFailedTests = readClasses(new File(recentlyFailedTestsFile), set.getTestList());
                for (Class test: recentlyFailedTests)
                {
                    set.prioritizeTest(test, 0);
                }
            }
        }

        set.prioritizeTest(BasicTest.class, 0); // Always start with BasicTest (if present)

        set.prioritizeTest(DatabaseDiagnosticsTest.class, set.getTestList().size() - 1); // Always end with DatabaseDiagnosticsTest (if present)

        List<Class> testClasses = testNames.isEmpty() ? set.getTestList() : getTestClasses(testNames);

        TestSuite suite = getSuite(testClasses, cleanOnly);

        if (suite.testCount() == 0)
        {
            System.out.println("No tests to run.");
        }
        else
        {
            _remainingTests = new ArrayList<>(suite.testCount());

            System.out.println("Running the following tests:");
            for (Enumeration<Test> e = suite.tests(); e.hasMoreElements(); )
            {
                Test test = e.nextElement();
                Class testClass = getTestClass(test);
                _remainingTests.add(testClass);
                System.out.println("  " + testClass.getSimpleName());
                for (String testMethod : specifiedTestMethods.getOrDefault(testClass, Collections.emptyList()))
                {
                    System.out.println("    ." + testMethod);
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
            System.out.println("Prioritizing tests for modified modules:");
            for (String module : modifiedModules)
            {
                System.out.println("\t" + module);
            }

            int movedTests = 0;
            for (String moduleDir : modifiedModules)
            {
                Collection<Class> associatedTests = WebTestProperties.getAssociatedTests(moduleDir);

                if (null != associatedTests)
                {
                    for (Class test : associatedTests)
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
                        System.err.println("Unexpected changelist format: " + line);
                        continue;
                    }

                    String relativeFilePath = splitLine[0];
                    String changeType = splitLine[1];

                    if ("NOT_CHANGED".equals(changeType))
                    {
                        System.out.println("File in changelist NOT_CHANGED: " + relativeFilePath);
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

        System.out.println("Unable to determine module for: " + path);
        return null;
    }

    private static Class getTestClass(Test test)
    {
        if (test instanceof JUnit4TestAdapter)
            return ((JUnit4TestAdapter) test).getTestClass();
        else
            return test.getClass();
    }
}
