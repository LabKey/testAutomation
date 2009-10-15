/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.test.drt;

import org.labkey.test.WebTestHelper;
import org.labkey.test.Runner;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.simple.JSONValue;
import junit.framework.TestSuite;
import junit.framework.TestResult;
import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.io.IOException;

/**
 * User: brittp
 * Date: Nov 30, 2005
 * Time: 10:53:59 PM
 */
public class JUnitTest extends TestSuite
{
    public JUnitTest() throws Exception
    {
    }

    @Override
    public void run(TestResult testResult)
    {
        log("\n\n=============== Starting " + getClass().getSimpleName() + Runner.getProgress() + " =================");
        try
        {
            super.run(testResult);
        }
        finally
        {
            log("=============== Completed " + getClass().getSimpleName() + Runner.getProgress() + " =================");
        }
    }

    // used when writing JUnitTest class name to the remainingTests.txt log file
    public String toString()
    {
        return getClass().getName();
    }

    public static TestSuite suite() throws Exception
    {
        GetMethod method = null;
        try
        {
            String url = WebTestHelper.getBaseURL() + "/junit/testlist.view?";
            HttpClient client = WebTestHelper.getHttpClient(url);
            method = new GetMethod(url);
            int status = client.executeMethod(method);
            if (status == HttpStatus.SC_OK)
            {
                TestSuite remotesuite = new JUnitTest();

                String response = method.getResponseBodyAsString();

                Map<String, List<String>> obj = (Map<String, List<String>>)JSONValue.parse(response);
                for (Map.Entry<String, List<String>> entry : obj.entrySet())
                {
                    String suiteName = entry.getKey();
                    List<String> arr = entry.getValue();
                    TestSuite testsuite = new TestSuite(suiteName);
                    for (String test : arr)
                    {
                        testsuite.addTest(new RemoteTest(test));
                    }
                    remotesuite.addTest(testsuite);
                }

                return remotesuite;
            }
            else
            {
                throw new AssertionFailedError("Failed to fetch remote junit test list (" + status + "): " + url);
            }
        }
        finally
        {
            if (method != null) method.releaseConnection();
        }
    }

    public static class RemoteTest extends TestCase
    {
        String _remoteClass;

        public RemoteTest(String remoteClass)
        {
            super(remoteClass);
            _remoteClass = remoteClass;
        }

        @Override
        protected void runTest() throws Throwable
        {
            GetMethod method = null;
            try
            {
                String url = WebTestHelper.getBaseURL() + "/junit/go.view?testCase=" + _remoteClass;
                HttpClient client = WebTestHelper.getHttpClient(url);
                method = new GetMethod(url);
                int status = client.executeMethod(method);
                String response = method.getResponseBodyAsString();

                if (status == HttpStatus.SC_OK)
                {
                    log("remote junit successful: " + _remoteClass);
                    log(dump(response));
                }
                else
                {
                    log("remote junit failed: " + _remoteClass);
                    fail("remote junit failed: " + _remoteClass + "\n" + dump(response));
                }
            }
            catch (IOException ioe)
            {
                fail("failed to run remote junit: " + ioe.getMessage());
            }
            finally
            {
                if (method != null) method.releaseConnection();
            }
        }

        static String dump(String response)
        {
            Map<String, Object> json = null;
            try
            {
                json = (Map<String, Object>)JSONValue.parse(response);
            }
            catch (Exception e)
            {
                // ignore
            }

            if (json == null)
                return response;

            StringBuilder sb = new StringBuilder();
            sb.append("ran: ").append(json.get("runCount"));
            sb.append(", errors: ").append(json.get("errorCount"));
            sb.append(", failed: ").append(json.get("failureCount")).append("\n");
            dumpFailures(sb, (List<Map<String, Object>>) json.get("errors"));
            dumpFailures(sb, (List<Map<String, Object>>) json.get("failures"));
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
