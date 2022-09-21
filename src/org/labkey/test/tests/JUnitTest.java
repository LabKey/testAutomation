/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

package org.labkey.test.tests;

import junit.framework.AssertionFailedError;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONValue;
import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Runner;
import org.labkey.test.SuiteBuilder;
import org.labkey.test.TestProperties;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.UnitTests;
import org.labkey.test.util.ArtifactCollector;
import org.labkey.test.util.JUnitFooter;
import org.labkey.test.util.JUnitHeader;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.QuickBootstrapPseudoTest;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@Category({BVT.class, UnitTests.class})
public class JUnitTest extends TestSuite
{
    private static final Logger LOG = LogManager.getLogger(JUnitTest.class);

    public JUnitTest()
    {
    }

    public static TestSuite suite() throws Exception
    {
        return JUnitTest._suite((p) -> true, false);
    }

    private static String getWhen(Map<String,Object> test)
    {
        Object when = test.get("when");
        if (!(when instanceof String) || StringUtils.isBlank((String)when))
            return "DRT";
        return ((String)when).toUpperCase();
    }

    public static Set<String> getCategories(Map<String,Object> test)
    {
        Set<String> testCategories = Collections.newSetFromMap(new CaseInsensitiveHashMap<>());
        testCategories.add(getWhen(test));
        if (testCategories.contains("DRT"))
            testCategories.add("Base"); // Replicate behavior of JUnitDRTTest until we have a better solution
        Object module = test.get("module");
        if (module instanceof String && !StringUtils.isBlank((String)module))
            testCategories.add((String)module);

        return testCategories;
    }

    // used when writing JUnitTest class name to the remainingTests.txt log file
    public String toString()
    {
        return getClass().getName();
    }

    public static class JUnitSeleniumHelper extends BaseWebDriverTest
    {
        @Override
        protected String getProjectName() {return null;}
        @Override
        protected void doCleanup(boolean afterTest) throws TestTimeoutException
        { }
        @Override
        public List<String> getAssociatedModules() { return null; }

        @Override public BrowserType bestBrowser() {return BrowserType.CHROME;}
    }

    // Use WebDriver to ensure we're upgraded
    @LogMethod
    private static void upgradeHelper(boolean skipInitialUserChecks)
    {
        // TODO: remove upgrade helper from JUnitTest and run before suite starts.
        BaseWebDriverTest helper;
        if (skipInitialUserChecks)
        {
            helper = new QuickBootstrapPseudoTest();
        }
        else
        {
            helper = new JUnitSeleniumHelper();
        }
        try
        {
            helper.setUp();
            // sign in performs upgrade if necessary
            helper.signIn();
        }
        catch (Throwable t)
        {
            if (helper.getWrappedDriver() != null)
            {
                helper.getArtifactCollector().dumpPageSnapshot("ServerBootstrap", null);
            }
            throw t;
        }
    }

    public static TestSuite dynamicSuite(Collection<String> categories, Collection<String> excludedCategories)
    {
        if (categories.isEmpty())
            return new TestSuite();

        final List<SuiteBuilder.SuiteInfo> suiteInfos = categories.stream().map(SuiteBuilder.SuiteInfo::new).toList();
        try
        {
            return _suite(testProps -> {
                Set<String> testCategories = getCategories(testProps);
                for (String excludedCategory : excludedCategories)
                {
                    if (testCategories.contains(excludedCategory))
                        return false;
                }
                for (SuiteBuilder.SuiteInfo suiteInfo : suiteInfos)
                {
                    if (testCategories.contains(suiteInfo.getName()) &&
                            suiteInfo.getSubset() == suiteInfo.getSubsetCount()) // Only run in last shard for sharded suite
                        return true;
                }
                return testCategories.contains("smoke"); // Always run smoke tests
            }, true);
        }
        catch (Throwable t)
        {
            LOG.error("Unable to fetch Remote JUnit tests");
            t.printStackTrace();
            TestSuite testSuite = new TestSuite();
            testSuite.addTest(new Runner.ErrorTest(JUnitTest.class.getSimpleName(), t));
            return testSuite;
        }
    }

    public static TestSuite _suite(Predicate<Map<String,Object>> accept, boolean skipInitialUserChecks) throws Exception
    {
        return _suite(accept, skipInitialUserChecks, 0, 0);
    }

    private static TestSuite _suite(Predicate<Map<String,Object>> accept, boolean skipInitialUserChecks, final int startupAttempts, final int upgradeAttempts) throws Exception
    {
        if (TestProperties.isPrimaryUserAppAdmin())
        {
            return new TestSuite(); // server-side tests require site admin
        }

        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpResponse response = null;
        try (CloseableHttpClient client = (CloseableHttpClient)WebTestHelper.getHttpClient())
        {
            final String url = WebTestHelper.getBaseURL() + "/junit-testlist.view?";
            HttpGet method = new HttpGet(url);
            try
            {
                response = client.execute(method, context);
            }
            catch (IOException ex)
            {
                if (startupAttempts < 60 && upgradeAttempts == 0)
                {
                    Thread.sleep(1000);
                    return _suite(accept, skipInitialUserChecks, startupAttempts + 1, upgradeAttempts);
                }
                else
                {
                    TestSuite failsuite = new JUnitTest();
                    failsuite.addTest(new Runner.ErrorTest("FetchTestList", ex));
                    return failsuite;
                }
            }
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK)
            {
                final String responseBody = WebTestHelper.getHttpResponseBody(response);
                if (responseBody.isEmpty())
                    throw new AssertionFailedError("Failed to fetch remote junit test list: empty response");

                Object json = JSONValue.parse(responseBody);
                if (json == null)
                {

                    if (responseBody.contains("<title>Start Modules</title>"))
                    {
                        // Server still starting up.  We don't need to use the upgradeHelper to sign in.
                        LOG.info("Remote JUnitTest: Server modules starting up (attempt " + startupAttempts + ") ...");

                        if (startupAttempts < 60)
                        {
                            Thread.sleep(1000);
                            return _suite(accept, skipInitialUserChecks, startupAttempts + 1, upgradeAttempts);
                        }
                        else
                        {
                            TestSuite failsuite = new JUnitTest();
                            failsuite.addTest(new Runner.ErrorTest("StartModules", new RuntimeException("Module Startup timed out.")));
                            return failsuite;
                        }
                    }
                    else if (responseBody.contains("<title>Upgrade Status</title>") ||
                        responseBody.contains("<title>Install Modules</title>") ||
                        responseBody.contains("<title>Upgrade Modules</title>") ||
                        responseBody.contains("<title>Account Setup</title>") ||
                        responseBody.contains("This server is being upgraded to a new version of LabKey Server."))
                    {
                        LOG.info("Remote JUnitTest: Server needs install or upgrade ...");
                        if (upgradeAttempts > 3)
                            throw new AssertionFailedError("Failed to update or bootstrap on second attempt: " + responseBody);

                        // perform upgrade then try to fetch the list again
                        Throwable upgradeError = null;
                        try
                        {
                            upgradeHelper(skipInitialUserChecks);
                        }
                        catch (Throwable t)
                        {
                            upgradeError = t;
                            t.printStackTrace();
                        }
                        TestSuite testSuite;
                        try
                        {
                            testSuite = _suite(accept, skipInitialUserChecks, startupAttempts + 1, upgradeAttempts + 1);
                        }
                        catch (Exception retryException)
                        {
                            retryException.printStackTrace();
                            testSuite = new TestSuite();
                            testSuite.addTest(new Runner.ErrorTest("", retryException));
                        }

                        if (upgradeError != null)
                        {
                            // Remember and log errors from bootstrap and upgrade but don't fail out immediately
                            testSuite.addTest(new Runner.ErrorTest(responseBody.contains("first time logging in") ? "ServerBootstrap" : "ServerUpgrade", upgradeError));
                        }

                        return testSuite;
                    }
                }

                if (!(json instanceof Map))
                    throw new AssertionFailedError("Can't parse or cast json response: " + responseBody);

                TestSuite remotesuite = new JUnitTest();
                Map<String, List<Map<String, Object>>> obj = (Map<String, List<Map<String, Object>>>)json;

                boolean addedHeader = false;
                for (Map.Entry<String, List<Map<String, Object>>> entry : obj.entrySet())
                {
                    String suiteName = entry.getKey();
                    TestSuite testsuite = new TestSuite(suiteName);
                    // Individual tests include both the class name and the requested timeout
                    for (Map<String, Object> testClass : entry.getValue())
                    {
                        // For the time being do not run performance tests with every junit check-in test suite.
                        if(!((String)testClass.get("when")).equalsIgnoreCase("performance"))
                        {
                            String className = (String) testClass.get("className");
                            // Timeout is represented in seconds
                            int timeout = ((Number) testClass.get("timeout")).intValue();
                            if (accept.test(testClass))
                                testsuite.addTest(new RemoteTest(className, timeout));
                        }

                    }
                    if (!addedHeader && testsuite.countTestCases() > 0)
                    {
                        BaseJUnitTestWrapper.extraSetup = !skipInitialUserChecks;
                        remotesuite.addTest(new JUnit4TestAdapter(JUnitHeader.class));
                        addedHeader = true;
                    }
                    remotesuite.addTest(testsuite);
                }
                if (addedHeader)
                {
                    remotesuite.addTest(new JUnit4TestAdapter(JUnitFooter.class));
                    // Exclude header and footer from count
                    LOG.info("Remote JUnitTest: found " + (remotesuite.countTestCases() - 2) + " tests.");
                }

                return remotesuite;
            }
            else
            {
                LOG.error("Getting unit test list from server failed with error code " + status + ". Error page content is:");
                final OutputStream streamLogger = IoBuilder.forLogger(LOG).setLevel(Level.ERROR).buildOutputStream();
                response.getEntity().writeTo(streamLogger);
                throw new AssertionFailedError("Failed to fetch remote junit test list (" + status + " - " + response.getStatusLine() + "): " + url);
            }
        }
        finally
        {
            if (response != null)
                EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    public static class RemoteTest extends TestCase
    {
        String _remoteClass;
        /** Timeout in seconds to wait for the whole testcase to finish on the server */
        private final int _timeout;

        /** Stash and reuse so that we can keep using the same session instead of re-authenticating with every request */
        private static final Connection connection = WebTestHelper.getRemoteApiConnection();

        public RemoteTest(String remoteClass, int timeout)
        {
            super(remoteClass);
            _remoteClass = remoteClass;
            _timeout = timeout;
        }

        @Override
        protected void runTest()
        {
            long startTime = System.currentTimeMillis();
            try
            {
                Command<CommandResponse> command = new PostCommand<>("junit", "go");
                Map<String, Object> params = new HashMap<>();
                params.put("testCase", _remoteClass);
                command.setParameters(params);
                command.setTimeout(_timeout * 1000);

                CommandResponse response = command.execute(connection, "/");
                Map<String, Object> resultJson = response.getParsedData();

                if (resultJson == null)
                    throw new AssertionError("Unparsable response from test [" + response.getStatusCode() + "]: " + response.getText());
                else if (resultJson.get("wasSuccessful") != Boolean.TRUE)
                    throw new AssertionError("Error response from failed test: " + dump(resultJson, true));

                WebTestHelper.logToServer(getLogTestString("successful", startTime) + ", " + dump(resultJson, false), connection);
                LOG.info(getLogTestString("successful", startTime));
                LOG.info(dump(resultJson, true));
            }
            catch (SocketTimeoutException ste)
            {
                String timed_out = getLogTestString("timed out", startTime);
                LOG.error(timed_out);
                ArtifactCollector.dumpThreads();
                throw new RuntimeException(timed_out, ste);
            }
            catch (IOException ioe)
            {
                String message = getLogTestString("failed: " + ioe.getMessage(), startTime);
                LOG.error(message);
                throw new RuntimeException(message, ioe);
            }
            catch (CommandException ce)
            {
                WebTestHelper.logToServer(getLogTestString("failed", startTime) + ", " + dump(ce.getResponseText(), false), connection);
                LOG.error(getLogTestString("failed", startTime));
                LOG.error(dump(ce.getResponseText(), false));
                Assert.fail(("remote junit failed (HTTP status code " + ce.getStatusCode() + "): " + _remoteClass) + "\n" + dump(ce.getResponseText(), true));
            }
        }

        private String getLogTestString(String message, long startTime)
        {
            return "remote junit " + message + ": " + _remoteClass + TestLogger.formatElapsedTime(System.currentTimeMillis() - startTime);
        }

        static String dump(String response, boolean dumpFailures)
        {
            Map<String, Object> json = (Map<String, Object>)JSONValue.parse(response);

            if (json == null)
                return response;

            return dump(json, dumpFailures);
        }

        @NotNull
        private static String dump(Map<String, Object> json, boolean dumpFailures)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("ran: ").append(json.get("runCount")).append(", ");
            sb.append("failed: ").append(json.get("failureCount"));
            Object ignored = json.get("ignored");
            if (ignored != null)
                sb.append(", ignored: ").append(ignored);
            sb.append("\n");
            if(dumpFailures) dumpFailures(sb, (List<Map<String, Object>>) json.get("failures"));
            return sb.toString();
        }

        static void dumpFailures(StringBuilder sb, List<Map<String, Object>> failures)
        {
            for (Map<String, Object> failure : failures)
            {
                if (failure.get("failedTest") != null)
                    sb.append(failure.get("failedTest")).append("\n");
                if (failure.get("exceptionMessage") != null)
                    sb.append("  ").append(failure.get("exceptionMessage")).append("\n");
                if (failure.get("trace") != null)
                    sb.append("  ").append(failure.get("trace")).append("\n");
                sb.append("\n");
            }
        }
    }

    public static class BaseJUnitTestWrapper extends BaseWebDriverTest
    {
        // Used by 'JUnitFooter' to check for leaks from server-side tests
        protected static Long startTime = null;
        // Don't configure pipeline tools or R for smoke suite
        protected static boolean extraSetup = false;

        @Override
        public List<String> getAssociatedModules()
        {
            return List.of();
        }

        @Override
        protected String getProjectName()
        {
            return null;
        }

        @Override
        protected void checkLinks()
        {
            // skip
        }

        @Override public BrowserType bestBrowser()
        {
            return BrowserType.CHROME;
        }
    }
}
