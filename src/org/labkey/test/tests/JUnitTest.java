/*
 * Copyright (c) 2007-2017 LabKey Corporation
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
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONValue;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Runner;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.External;
import org.labkey.test.categories.UnitTests;
import org.labkey.test.util.JUnitFooter;
import org.labkey.test.util.JUnitHeader;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@Category({BVT.class, UnitTests.class, External.class})
public class JUnitTest extends TestSuite
{
    private static final DecimalFormat commaf0 = new DecimalFormat("#,##0");

    public JUnitTest()
    {
    }

    public static TestSuite suite() throws Exception
    {
        return JUnitTest._suite((p) -> true, 0, false);
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
        public void unfail()
        {
            _testFailed = false;
        }

        protected String getProjectName() {return null;}
        protected void doCleanup(boolean afterTest) throws TestTimeoutException
        { }
        public List<String> getAssociatedModules() { return null; }

        @Override public BrowserType bestBrowser() {return BrowserType.CHROME;}
    }

    // Use WebDriver to ensure we're upgraded
    private static void upgradeHelper()
    {
        // TODO: remove upgrade helper from JUnitTest and run before suite starts.
        JUnitSeleniumHelper helper = new JUnitSeleniumHelper();
        helper.setUp();
        // sign in performs upgrade if necessary
        helper.signIn();
        helper.unfail();
    }

    public static TestSuite dynamicSuite(Collection<String> categories, Collection<String> excludedCategories)
    {
        try
        {
            return _suite(testProps -> {
                Set<String> testCategories = getCategories(testProps);
                for (String excludedCategory : excludedCategories)
                {
                    if (testCategories.contains(excludedCategory))
                        return false;
                }
                for (String suite : categories)
                {
                    if (testCategories.contains(suite))
                        return true;
                }
                return false;
            });
        }
        catch (Throwable t)
        {
            log("Unable to fetch Remote JUnit tests");
            t.printStackTrace();
            TestSuite testSuite = new TestSuite();
            testSuite.addTest(new Runner.ErrorTest(JUnitTest.class.getSimpleName(), t));
            return testSuite;
        }
    }

    public static TestSuite _suite(Predicate<Map<String,Object>> accept) throws Exception
    {
        return _suite(accept, 0, false);
    }

    private static TestSuite _suite(Predicate<Map<String,Object>> accept, int attempt, boolean performedUpgrade) throws Exception
    {
        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpResponse response = null;
        try (CloseableHttpClient client = (CloseableHttpClient)WebTestHelper.getHttpClient())
        {
            String url = WebTestHelper.getBaseURL() + "/junit/testlist.view?";
            HttpGet method = new HttpGet(url);
            response = client.execute(method, context);
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK)
            {
                TestSuite remotesuite = new JUnitTest();

                String responseBody = WebTestHelper.getHttpResponseBody(response);
                if (responseBody.isEmpty())
                    throw new AssertionFailedError("Failed to fetch remote junit test list: empty response");

                Object json = JSONValue.parse(responseBody);
                if (json == null)
                {
                    if (responseBody.contains("<title>Start Modules</title>"))
                    {
                        // Server still starting up.  We don't need to use the upgradeHelper to sign in.
                        log("Remote JUnitTest: Server modules starting up (attempt " + attempt + ") ...");
                        Thread.sleep(1000);
                        return _suite(accept, attempt + 1, false);
                    }
                    else if (responseBody.contains("<title>Upgrade Status</title>") ||
                        responseBody.contains("<title>Install Modules</title>") ||
                        responseBody.contains("<title>Upgrade Modules</title>") ||
                        responseBody.contains("<title>Account Setup</title>") ||
                        responseBody.contains("This server is being upgraded to a new version of LabKey Server."))
                    {
                        log("Remote JUnitTest: Server needs install or upgrade ...");
                        if (performedUpgrade)
                            throw new AssertionFailedError("Failed to update or bootstrap on second attempt: " + responseBody);

                        // perform upgrade then try to fetch the list again
                        upgradeHelper();
                        return _suite(accept, attempt + 1, true);
                    }
                }

                if (json == null || !(json instanceof Map))
                    throw new AssertionFailedError("Can't parse or cast json response: " + responseBody);

                Map<String, List<Map<String, Object>>> obj = (Map<String, List<Map<String, Object>>>)json;
                boolean addedHeader = false;
                for (Map.Entry<String, List<Map<String, Object>>> entry : obj.entrySet())
                {
                    String suiteName = entry.getKey();
                    TestSuite testsuite = new TestSuite(suiteName);
                    // Individual tests include both the class name and the requested timeout
                    for (Map<String, Object> testClass : entry.getValue())
                    {
                        String className = (String)testClass.get("className");
                        // Timeout is represented in seconds
                        int timeout = ((Number)testClass.get("timeout")).intValue();
                        if (accept.test(testClass))
                            testsuite.addTest(new RemoteTest(className, timeout));
                    }
                    if (!addedHeader && testsuite.countTestCases() > 0)
                    {
                        remotesuite.addTest(new JUnit4TestAdapter(JUnitHeader.class));
                        addedHeader = true;
                    }
                    remotesuite.addTest(testsuite);
                }
                if (addedHeader)
                {
                    remotesuite.addTest(new JUnit4TestAdapter(JUnitFooter.class));
                    // Exclude header and footer from count
                    log("Remote JUnitTest: found " + (remotesuite.countTestCases() - 2) + " tests.");
                }

                return remotesuite;
            }
            else
            {
                System.err.println("Getting unit test list from server failed with error code " + status + ". Error page content is:");
                response.getEntity().writeTo(System.err);
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
        private static final HttpContext context = WebTestHelper.getBasicHttpContext();

        public RemoteTest(String remoteClass, int timeout)
        {
            super(remoteClass);
            _remoteClass = remoteClass;
            _timeout = timeout;

            /* Configure to use cookies so that we remember our session ID */
            context.setAttribute(HttpClientContext.COOKIE_STORE, new BasicCookieStore());
        }

        @Override
        protected void runTest() throws Throwable
        {
            // Need to clear request config to change socket timeout
            context.setAttribute("http.request-config", null);

            long startTime = System.currentTimeMillis();
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(_timeout * 1000 * 2)
                    .build();

            HttpResponse response = null;
            try (CloseableHttpClient client = WebTestHelper.getHttpClientBuilder()
                    .setDefaultRequestConfig(requestConfig).build())
            {
                URIBuilder builder = new URIBuilder(WebTestHelper.getBaseURL() + "/junit/go.view");
                builder.addParameter("testCase", _remoteClass);
                HttpGet method = new HttpGet(builder.build());
                response = client.execute(method, context);
                int status = response.getStatusLine().getStatusCode();
                String responseBody = WebTestHelper.getHttpResponseBody(response);

                WebTestHelper.logToServer(getLogTestString("successful", startTime) + ", " + dump(responseBody, false), client, context);
                if (status == HttpStatus.SC_OK)
                {
                    log(getLogTestString("successful", startTime));
                    log(dump(responseBody, true));
                }
                else
                {
                    log(getLogTestString("failed", startTime));
                    fail("remote junit failed (HTTP status code " + status + "): " + _remoteClass + "\n" + dump(responseBody, true));
                }
            }
            catch (IOException ioe)
            {
                throw new RuntimeException(getLogTestString("failed", startTime), ioe);
            }
            finally
            {
                if (response != null)
                    EntityUtils.consumeQuietly(response.getEntity());
            }
        }

        private String getLogTestString(String message, long startTime)
        {
            return "remote junit " + message + ": " + _remoteClass + " [" + commaf0.format(System.currentTimeMillis() - startTime) + " ms]";
        }

        static String dump(String response, boolean dumpFailures)
        {
            Map<String, Object> json = (Map<String, Object>)JSONValue.parse(response);

            if (json == null)
                return response;

            StringBuilder sb = new StringBuilder();
            sb.append("ran: ").append(json.get("runCount"));
//            sb.append(", errors: ").append(json.get("errorCount"));
            sb.append(", failed: ").append(json.get("failureCount")).append("\n");
//            dumpFailures(sb, (List<Map<String, Object>>) json.get("errors"));
            if(dumpFailures) dumpFailures(sb, (List<Map<String, Object>>) json.get("failures"));
            return sb.toString();
        }

        static void dumpFailures(StringBuilder sb, List<Map<String, Object>> failures)
        {
            for (Map<String, Object> failure : failures)
            {
                if (failure.get("failedTest") != null)
                    sb.append(failure.get("failedTest")).append("\n");
                if (failure.get("exceptionMesage") != null)
                    sb.append("  ").append(failure.get("exceptionMessage")).append("\n");
                if (failure.get("trace") != null)
                    sb.append("  ").append(failure.get("trace")).append("\n");
                sb.append("\n");
            }
        }

    }

    static void log(String str)
    {
        if (str == null || str.length() == 0)
            return;
        String d = new SimpleDateFormat("HH:mm:ss,SSS").format(new Date());      // Include time with log entry.  Use format that matches labkey log.
        System.out.println(d + " " + str);
    }
}
