/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.labkey.query.xml.ApiTestsDocument;
import org.labkey.query.xml.TestCaseType;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.JSONHelper;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * An abstract class that can be used to test recorded API request/response interactions. A typical usage
 * would be for an extending class to perform whatever project setup was required and call super.testSteps.
 * The class would also implement getTestFiles which returns an array of recorded test files, the schema is
 * apiTest.xsd and a test can be recorded using the API test page: query/apiTest.view
 */
public abstract class SimpleApiTest extends BaseWebDriverTest
{
    JSONHelper _helper = null;

    enum ActionType {
        get,
        post
    }

    /**
     * Returns the list of files to run tests over. Each test file contains metadata representing
     * test cases, the metadata schema can be found in apiTest.xsd
     */
    protected abstract File[] getTestFiles();

    protected Pattern[] getIgnoredElements()
    {
        return new Pattern[0];
    }

    protected void ensureConfigured()
    {

    }

    protected void cleanUp()
    {

    }

    @Test
    public void testSteps() throws Exception
    {
        ensureConfigured();
        runUITests();
        runApiTests();
        cleanUp();
    }

    protected abstract void runUITests() throws Exception;

    public void runApiTests() throws Exception
    {
        runApiTests(getTestFiles(), null, null, false);
    }

    public void runApiTests(File[] testFiles, String username, String password, boolean expectErrors) throws Exception
    {
        int tests = 0;

        if (testFiles != null)
        {
            _helper = new JSONHelper(this, getIgnoredElements());
            for (File testFile : testFiles)
            {
                if (testFile.exists())
                {
                    for (ApiTestCase test : parseTests(testFile))
                    {
                        tests++;
                        log("Starting new test case: \"" + StringUtils.trimToEmpty(test.getName()) + "\" in file " + testFile.getPath());
                        sendRequestDirect(testFile.getName(), test.getUrl(), test.getType(), test.getFormData(), test.getReponse(), test.isFailOnMatch(), username, password, expectErrors);
                        log("test case completed");
                    }
                }
            }
        }
        log("Finished running recorded tests, a total of " + tests + " were completed");
    }

    protected List<ApiTestCase> parseTests(File testFile)
    {
        try
        {
            List<ApiTestCase> tests = new ArrayList<>();
            ApiTestsDocument doc = ApiTestsDocument.Factory.parse(testFile);

            if (doc != null)
            {
                for (TestCaseType testCase : doc.getApiTests().getTestArray())
                {
                    tests.add(parseTestCase(testCase));
                }
            }
            return tests;
        }
        catch (Exception e)
        {
            fail("An unexpected error occurred: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    protected ApiTestCase parseTestCase(TestCaseType element)
    {
        ApiTestCase testCase = new ApiTestCase();

        String type = element.getType();
        if ("get".equalsIgnoreCase(type))
            testCase.setType(ActionType.get);
        else if ("post".equalsIgnoreCase(type))
            testCase.setType(ActionType.post);
        else
            throw new RuntimeException("Invalid test type, only 'GET' or 'POST' types are allowed");

        testCase.setName(element.getName());
        testCase.setFailOnMatch(element.getFailOnMatch());

        String url = element.getUrl();
        if (url != null)
            testCase.setUrl(StringUtils.trim(url));
        else
            fail("Test case did not have the required url element");

        String response = element.getResponse();
        if (response != null)
            testCase.setReponse(StringUtils.trim(response));

        String formData = element.getFormData();
        if (formData != null)
            testCase.setFormData(StringUtils.trim(formData));

        return testCase;
    }

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/query";
    }

    private void sendRequestDirect(String name, String url, ActionType type, String formData, String expectedResponse, boolean failOnMatch, String username, String password, boolean acceptErrors) throws UnsupportedEncodingException
    {
        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpUriRequest method = null;
        HttpResponse response = null;
        String requestUrl = WebTestHelper.getBaseURL() + '/' + url;

        switch (type)
        {
            case get:
                method = new HttpGet(requestUrl);
                break;
            case post:
                method = new HttpPost(requestUrl);
                ((HttpPost)method).setEntity(new StringEntity(formData, "application/json", "UTF-8"));
                break;
        }

        try (CloseableHttpClient client = (CloseableHttpClient)(
                username == null ?
                WebTestHelper.getHttpClient() :
                WebTestHelper.getHttpClient(username, password)))
        {

            response = client.execute(method, context);
            int status = response.getStatusLine().getStatusCode();
            String responseBody = WebTestHelper.getHttpResponseBody(response);
            if (status == HttpStatus.SC_OK || acceptErrors)
            {
                _helper.assertEquals("FAILED: test " + name, expectedResponse, responseBody);
            }
            else
                fail(String.format("FAILED: test %s failed with status code: %s%s", name, status, responseBody != null ? "\n" + responseBody : ""));
        }
        catch (IOException e)
        {
            fail("Test failed requesting the URL: " + e.getMessage());
        }
        finally
        {
            try
            {
                if (response != null)
                    EntityUtils.consume(response.getEntity());
            }
            catch (IOException ex)
            {/*ignore*/}
        }
    }

    private void sendRequest(String url, ActionType type, String formData, String expectedResponse, boolean failOnMatch)
    {
        switch (type)
        {
            case get:
                setFormElement("txtUrlGet", url);
                click(Locator.xpath("//input[@id='btnGet']"));
                break;
            case post:
                setFormElement("txtUrlPost", url);
                setFormElement("txtPost", StringUtils.trimToEmpty(formData));
                click(Locator.xpath("//input[@id='btnPost']"));
                break;
        }

        if (isElementPresent(Locator.xpath("//div[@id='lblStatus' and contains(text(), 'ERROR')]")))
            fail("The request has failed: " + url);

        waitForText("Request Complete", defaultWaitForPage);

        // Once response has loaded, check it, also check 'Request Complete'
        if (!StringUtils.isEmpty(expectedResponse))
        {
            if (failOnMatch)
                assertElementNotPresent(Locator.xpath("//pre[@id='lblResponse' and contains(text(), '" + expectedResponse + "')]"));
            else
                assertElementPresent(Locator.xpath("//pre[@id='lblResponse' and contains(text(), '" + expectedResponse + "')]"));

            assertTextPresent("Request Complete.");
        }

        // clear all the forms elements
        setFormElement("txtUrlGet", "");
        setFormElement("txtUrlPost", "");
        setFormElement("txtPost", "");
    }

    protected static class ApiTestCase
    {
        private String _name;
        private ActionType _type;
        private String _url;
        private String _reponse;
        private String _formData;
        private boolean _failOnMatch;

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public ActionType getType()
        {
            return _type;
        }

        public void setType(ActionType type)
        {
            _type = type;
        }

        public String getUrl()
        {
            return _url;
        }

        public void setUrl(String url)
        {
            _url = url;
        }

        public String getReponse()
        {
            return _reponse;
        }

        public void setReponse(String reponse)
        {
            _reponse = reponse;
        }

        public boolean isFailOnMatch()
        {
            return _failOnMatch;
        }

        public void setFailOnMatch(boolean failOnMatch)
        {
            _failOnMatch = failOnMatch;
        }

        public String getFormData()
        {
            return _formData;
        }

        public void setFormData(String formData)
        {
            _formData = formData;
        }
    }
}
