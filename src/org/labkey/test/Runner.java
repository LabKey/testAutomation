package org.labkey.test;

import junit.framework.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.labkey.test.bvt.RemoteAPITest;
import org.labkey.test.testpicker.TestHelper;
import org.labkey.test.util.Crawler;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * User: brittp
 * Date: Nov 15, 2005
 * Time: 5:18:46 PM
 */
public class Runner extends TestSuite
{
    private static final TestSet DEFAULT_TEST_SET = TestSet.DRT;
    private Set<TestFailure> _failures = new HashSet<TestFailure>();
    private boolean _cleanOnly;
    private File _failureDumpDir;
    private static WebTest _currentWebTest;
    private static Map<Test, Long> _testStats = new LinkedHashMap<Test, Long>();
    private static int _testCount;
    private static List<Class> _testClasses;

    private Runner(File failureDumpDir, boolean cleanOnly)
    {
        _cleanOnly = cleanOnly;
        _failureDumpDir = failureDumpDir;
    }

    public static void setCurrentWebTest(WebTest currentWebTest)
    {
        _currentWebTest = currentWebTest;
    }

    private void updateRemainingTests(Test test)
    {
        Class testClass = getTestClass(test);
        _testClasses.remove(testClass);
    }

    private static void writeClasses(List<Class> testClasses)
    {
        PrintWriter pw = null;
        File file = getRemainingTestsFile();

        try
        {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));

            for (Class clazz : testClasses)
                pw.println(clazz.getName());
        }
        catch(IOException e)
        {
            System.out.println("Error writing " + file.getAbsolutePath());
            System.exit(1);
        }
        finally
        {
            if (null != pw)
                pw.close();
        }
   }

    private static Class[] readClasses()
    {
        List<Class> testClasses = new ArrayList<Class>(20);
        File file = getRemainingTestsFile();

        if (file.exists())
        {
            BufferedReader reader = null;
            String line = null;

            try
            {
                reader = new BufferedReader(new FileReader(file));

                while ((line = reader.readLine()) != null)
                    if (null != StringUtils.trimToNull(line))
                        testClasses.add(Class.forName(line));
            }
            catch(IOException e)
            {
                System.out.println("Error reading " + file.getAbsolutePath());
                System.exit(1);
            }
            catch(ClassNotFoundException e)
            {
                System.out.println("Can't find class " + line);
                System.exit(1);
            }
            finally
            {
                try
                {
                    if (null != reader)
                        reader.close();
                }
                catch(IOException e)
                {
                    //
                }
            }
        }

        return testClasses.toArray(new Class[testClasses.size()]);
    }

    private static File getRemainingTestsFile()
    {
        String labkeyRoot = WebTestHelper.getLabKeyRoot();
        return new File(labkeyRoot, "server/test/remainingTests.txt");
    }

    public static String getProgress()
    {
        int completed = _testCount -_testClasses.size() + 1;
        return " (" + completed + " of " + _testCount + ")";
    }

    private static void saveTestDuration(Test currentWebTest, long durationMs)
    {
        _testStats.put(currentWebTest, durationMs);
    }

    public void runTest(Test test, TestResult testResult)
    {
        long startTimeMs = System.currentTimeMillis();
        _currentWebTest = null;
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
                    JUnit4TestAdapter adapter = (JUnit4TestAdapter)test;
                    if (Cleanable.class.isAssignableFrom(adapter.getTestClass()))
                    {
                        Cleanable cleanable = (Cleanable)adapter.getTestClass().newInstance();
                        cleanable.cleanup();
                    }
                }
            }
            catch (Throwable t)
            {
                System.out.println("WARNING: failure cleaning test: " + t.getMessage());
                System.out.println("Failures may be expected if the test was already cleaned.");
                // fall through
            }
        }
        else if (_failures.isEmpty())
        {
            writeClasses(_testClasses);
            super.runTest(test, testResult);
            if (testResult.failureCount() > 0 || testResult.errorCount() > 0)
            {
                dumpFailures(testResult.errors());
                dumpFailures(testResult.failures());
            }
            else
            {
                updateRemainingTests(test);
            }
        }
        long testTimeMs = System.currentTimeMillis() - startTimeMs;
        saveTestDuration(test, testTimeMs);
    }

    private void dumpFailures(Enumeration failures)
    {
        while (failures.hasMoreElements())
        {
            TestFailure failure = (TestFailure) failures.nextElement();
            if (!_failures.contains(failure))
            {
                _failures.add(failure);
                if (_currentWebTest != null)
                {
                    File f = _currentWebTest.dumpHtml(_failureDumpDir);
                    String message = "Test failed.";
                    if (f != null)
                        message += "  Page html dumped to:\n    " + f.getPath();
                    System.out.println(message);
                }
            }
        }
    }


    // Set up only the requested tests
    private static List<Class> getTestClasses(TestSet testSet, List<String> testNames)
    {
        Map<String, Class> nameMap = new HashMap<String, Class>();
        for (Class testClass : testSet.tests)
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

        List<Class> testClasses = new ArrayList<Class>(testNames.size());

        for (String testName : testNames)
        {
            Class testClass = nameMap.get(testName.toLowerCase());
            if (testClass == null)
            {
                System.out.println("Couldn't find test '" + testName + "' in suite '" + testSet.name() + "'.  Valid tests are:");
                for (Class c : testSet.tests)
                    System.out.println("    " + c.getSimpleName());
            }
            else
                testClasses.add(testClass);
        }

        return testClasses;
    }


    private static TestSuite getSuite(List<Class> testClasses, File dumpDir, boolean cleanOnly, boolean modifiedOnly)
    {
        // Remove duplicate tests (e.g., don't run "basic" test twice if bvt & drt are selected via ant test) but keep the order
        Set<Class> testClassesCopy = new LinkedHashSet<Class>(testClasses);
        TestSuite suite = new Runner(dumpDir, cleanOnly);

        List<String> moduleDirs = getModifiedModuleDirectories();

        // If svnModified.txt exists then order the tests starting from most recently modified
        if (null != moduleDirs)
        {
            TestMap tm = new TestMap();

            for (Class testClass : testClassesCopy)
            {
                try
                {
                    // TODO: Remove special handling (add interface or make RemoteAPITest extend WebTest)
                    if (testClass.equals(RemoteAPITest.class))
                    {
                        tm.put("experiment", testClass);
                    }
                    else
                    {
                        Constructor<WebTest> c = testClass.getConstructor();
                        WebTest test = c.newInstance();
                        String directory = test.getAssociatedModuleDirectory();

                        if (!"none".equals(directory))
                        {
                            File testDir = new File(WebTestHelper.getLabKeyRoot(), "/server/modules/" + directory);

                            if (!testDir.exists())
                            {
                                System.out.println("Module directory \"" + directory + "\" specified in " + testClass + " does not exist!");
                                System.exit(1);
                            }
                        }

                        tm.put(directory, testClass);
                    }
                }
                catch(Exception e)
                {
                    System.out.println("Error: " + e);
                }
            }

            for (String moduleDir : moduleDirs)
            {
                Collection<Class> associatedTests = tm.get(moduleDir);

                if (null != associatedTests)
                {
                    for (Class testClass : associatedTests)
                    {
                        suite.addTest(new JUnit4TestAdapter(testClass));
                        testClassesCopy.remove(testClass);
                    }
                }
            }
        }
        else if (modifiedOnly)
        {
            System.out.println("Invalid combination: quick=true but svnModified.txt was not found");
            System.exit(1);
        }

        if (!modifiedOnly)
        {
            for (Class testClass : testClassesCopy)
                suite.addTest(new JUnit4TestAdapter(testClass));
        }

        return suite;
    }


    // A simple MultiMap
    private static class TestMap extends HashMap<String, Collection<Class>>
    {
        public Collection<Class> put(String key, Class clazz)
        {
            Collection<Class> collection = get(key);

            if (null == collection)
            {
                collection = new ArrayList<Class>();
                put(key, collection);
            }

            collection.add(clazz);
            return collection;
        }
    }


    private static void writeTimeReport()
    {
        long total = 0;
        System.out.println("============= Time Report ==============");

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
            long percent = Math.round(100.0 * (entry.getValue() / (double) total));
            String percentStr = (percent < 10 ? " " : "") + percent + "%";
            String durationAndPercent = formatDuration(entry.getValue()) + " " + percentStr;
            String testName = entry.getKey().toString();
            testName = testName.substring(testName.lastIndexOf('.') + 1);
            System.out.println(getFixedWidthString(testName, durationAndPercent, 40));
            if (Crawler.getCrawlStats().containsKey(testName))
            {
                crawl = true;
                Crawler.CrawlStats crawlStats = Crawler.getCrawlStats().get(testName);
                System.out.println(getFixedWidthString("Crawler Statistics: ", "", 40));

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
            System.out.println(getFixedWidthString("Total Crawler Statistics: ", "", 40));

            String[] statTitles = {"TotCrawlTime", "TotPages", "TotActions"};
            int totalCrawlTimeSeconds = ((totalCrawlTime / 1000) % 60);
            String[] stats = { ((totalCrawlTime / 60000) + ":" + (totalCrawlTimeSeconds >= 10 ? totalCrawlTimeSeconds : ("0" + totalCrawlTimeSeconds))),
                    Integer.toString(totalUniquePages),
                    Integer.toString(totalUniqueActions) };
            int columnWidth = 13;
            System.out.println(getRowString(statTitles, columnWidth));
            System.out.println(getRowString(stats, columnWidth));
        }
        System.out.println("----------------------------------------");
        System.out.println(getFixedWidthString("Total duration:", formatDuration(total), 40) + "\n");
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

    // Return a list of modified module directories, ordered starting with most recently modified.
    // If svnModified.txt can't be accessed or parsed then return an empty list.
    private static List<String> getModifiedModuleDirectories()
    {
        String labkeyRoot = WebTestHelper.getLabKeyRoot();
        File svnModifiedFilelist = new File(labkeyRoot, "server/test/build/svnModified.txt");
        String sep = File.separator;
        String modulePrefix = "server" + sep + "modules" + sep;
        Map<String, Long> moduleDirs = new HashMap<String, Long>(10);

        if (svnModifiedFilelist.exists())
        {
            BufferedReader reader = null;
            try
            {
                reader = new BufferedReader(new FileReader(svnModifiedFilelist));
                String line;

                while ((line = reader.readLine()) != null)
                {
                    if (line.charAt(0) != '?')
                    {
                        String path = line.substring(7, line.length());

                        // If path starts with "server/modules" then find the end index of module name.  If path doesn't
                        //  start with module prefix or the next separator is missing, set index to -1
                        int i = (path.startsWith(modulePrefix) ? path.indexOf(sep, modulePrefix.length()) : -1);

                        // Anything outside "server/modules" is labeled "none"
                        String moduleDir = (-1 == i ? "none" : path.substring(modulePrefix.length(), i));

                        // Note: We don't have a modification date for deleted or renamed files.  They end up with lastModified == 0 and are treated as the oldest modifications.
                        long lastModified = new File(labkeyRoot, path).lastModified();
                        Long mostRecent = moduleDirs.get(moduleDir);

                        if (null == mostRecent || lastModified > mostRecent)
                            moduleDirs.put(moduleDir, lastModified);
                    }
                }

                // Now sort modules by most recent file modification date
                Map<Long, String> orderedModuleDirs = new TreeMap<Long, String>();

                for (String moduleDir : moduleDirs.keySet())
                    orderedModuleDirs.put(-moduleDirs.get(moduleDir), moduleDir);  // Start with most recent change

                return new ArrayList<String>(orderedModuleDirs.values());
            }
            catch(IOException e)
            {
                System.err.print(e.getMessage());
            }
            finally
            {
                if (reader != null) { try { reader.close(); } catch (IOException e) {} }
                svnModifiedFilelist.delete();
            }
        }

        return Collections.emptyList();
    }

    public static Class[] getAllTests()
    {
        List<Class> tests = new ArrayList<Class>();
        for (TestSet testSet : TestSet.values())
        {
            for (Class testClass : testSet.tests)
            {
                if (!tests.contains(testClass))
                    tests.add(testClass);
            }
        }
        return tests.toArray(new Class[tests.size()]);
    }

    public static void main(String args[])
    {
        TestSet set = null;
        String suiteName = System.getProperty("suite");

        if (suiteName != null)
        {
            try
            {
                set = TestSet.valueOf(suiteName);
            }
            catch (Exception e)
            {
                System.out.println("Couldn't find suite '" + suiteName + "'.  Valid suites are:");
                for (TestSet s : TestSet.values())
                    System.out.println("   " + s.name());
            }
        }
        else
            set = DEFAULT_TEST_SET;

        if (set != null)
        {
            boolean cleanOnly = "true".equals(System.getProperty("cleanonly"));
            boolean skipClean = "false".equals(System.getProperty("clean"));

            if (cleanOnly && skipClean)
            {
                System.err.print("Invalid parameters: cannot specify both 'cleanonly=true' and 'clean=false'.");
                System.exit(1);
            }

            String testNames = System.getProperty("test");
            if ((TestSet.TEST == set) && ((testNames == null) || testNames.length() == 0))
            {
                new TestHelper();
            }
            else
            {
                List<String> tests = new ArrayList<String>();
                if (testNames != null && testNames.length() > 0)
                {
                    String[] testNameArray = testNames.split(",");
                    tests.addAll(Arrays.asList(testNameArray));
                }
                runTests(tests, set);
            }
        }
    }

    public static void runTests(List<String> testNames, TestSet set)
    {

        if (TestSet.CONTINUE == set)
        {
            set.setTests(readClasses());
        }
        else if (TestSet.TEST == set)
        {
            set.setTests(getAllTests());
        }

        boolean cleanOnly = "true".equals(System.getProperty("cleanonly"));

        File dumpDir = null;
        String outputDir = System.getProperty("failure.output.dir");
        if (outputDir != null)
            dumpDir = new File(outputDir);
        if (dumpDir == null || !dumpDir.exists())
            dumpDir = new File(System.getProperty("java.io.tmpdir"));
        if (!dumpDir.exists())
        {
            System.err.println("Couldn't determine directory for placement of output files. " +
                    "Tried system properties failure.output.dir and java.io.tmpdir");
            System.exit(1);
        }

        boolean loop = "true".equals(System.getProperty("loop"));
        boolean modifiedOnly = "true".equals(System.getProperty("quick"));

        int count = 0;
        while (count++ == 0 || loop)
        {
            if (loop)
                System.out.println("Starting test iteration " + count);

            List<Class> testClasses = testNames.isEmpty() ? set.getTestList() : getTestClasses(set, testNames);
            TestSuite suite = getSuite(testClasses, dumpDir, cleanOnly, modifiedOnly);

            _testClasses = new ArrayList<Class>(suite.testCount());

            System.out.println("Running the following tests:");
            for (Enumeration<Test> e = suite.tests(); e.hasMoreElements(); )
            {
                Test test = e.nextElement();
                Class testClass = getTestClass(test);
                _testClasses.add(testClass);
                System.out.println("  " + testClass.getSimpleName());
            }
            _testCount = _testClasses.size();
            TestResult result = junit.textui.TestRunner.run(suite);

            writeTimeReport();

            if (result.errorCount() > 0 || result.failureCount() > 0)
                System.exit(result.errorCount() + result.failureCount());
            else
                getRemainingTestsFile().delete();
        }
    }

    private static Class getTestClass(Test test)
    {
        if (test instanceof JUnit4TestAdapter)
            return ((JUnit4TestAdapter) test).getTestClass();
        else
            return test.getClass();
    }
}
