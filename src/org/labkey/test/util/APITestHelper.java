/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.xmlbeans.XmlException;
import org.jetbrains.annotations.NotNull;
import org.labkey.query.xml.ApiTestsDocument;
import org.labkey.query.xml.TestCaseType;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.fail;

public class APITestHelper
{
    BaseWebDriverTest test;
    JSONHelper jsonHelper = null;
    File[] testFiles;

    public APITestHelper(BaseWebDriverTest test)
    {
        this.test = test;
    }

    public void setTestFiles(File... testFiles)
    {
        this.testFiles = testFiles;
    }

    public void setIgnoredElements(Pattern[] ignoredElements)
    {
        jsonHelper = new JSONHelper(ignoredElements);
    }

    public void runApiTests() throws Exception
    {
        runApiTests(PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }

    public void runApiTests(String username, String password) throws Exception
    {
        if (testFiles != null && testFiles.length > 0)
        {
            if (jsonHelper == null)
                jsonHelper = new JSONHelper(new Pattern[0]);

            int tests = 0;
            for (File testFile : testFiles)
            {
                if (testFile.exists())
                {
                    for (ApiTestCase testCase : parseTests(testFile))
                    {
                        tests++;
                        test.log("Starting new test case: \"" + StringUtils.trimToEmpty(testCase.getName()) + "\" in file " + testFile.getPath());
                        sendRequestDirect(testFile.getName(), testCase.getUrl(), testCase.getType(), testCase.getFormData(), testCase.getResponse(), testCase.isFailOnMatch(), username, password, false);
                        test.log("test case completed");
                    }
                }
            }
            test.log("Finished running recorded tests, a total of " + tests + " were completed");
        }
    }

    public static List<ApiTestCase> parseTests(File testFile)
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
        catch (IOException | XmlException e)
        {
            throw new RuntimeException("An unexpected error occurred", e);
        }
    }

    private static ApiTestCase parseTestCase(TestCaseType element)
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
            testCase.setResponse(StringUtils.trim(response));

        String formData = element.getFormData();
        if (formData != null)
            testCase.setFormData(StringUtils.trim(formData));

        return testCase;
    }

    private void sendRequestDirect(String name, String url, ActionType type, String formData, String expectedResponse, boolean failOnMatch, @NotNull String username, @NotNull String password, boolean acceptErrors) throws UnsupportedEncodingException
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
                ((HttpPost)method).setEntity(new StringEntity(formData, ContentType.create("application/json", "UTF-8")));
                break;
        }

        injectCookies(username, method);

        try (CloseableHttpClient client = (CloseableHttpClient) WebTestHelper.getHttpClient(username, password))
        {
            response = client.execute(method, context);
            int status = response.getStatusLine().getStatusCode();
            String responseBody = WebTestHelper.getHttpResponseBody(response);
            if (status == HttpStatus.SC_OK || acceptErrors)
            {
                jsonHelper.assertEquals("FAILED: test " + name, expectedResponse, responseBody);
            }
            else
                fail(String.format("FAILED: test %s failed with status code: %s%s", name, status, "\n" + responseBody));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Test failed requesting the URL,", e);
        }
        finally
        {
            if (response != null)
                EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    public static void injectCookies(HttpUriRequest method)
    {
        injectCookies(PasswordUtil.getUsername(), method);
    }

    public static void injectCookies(@NotNull String username, HttpUriRequest method)
    {
        org.openqa.selenium.Cookie csrf = WebTestHelper.getCookies(username).get("X-LABKEY-CSRF");
        if (csrf != null)
            method.setHeader(csrf.getName(), csrf.getValue());
    }

    private void sendRequest(String url, ActionType type, String formData, String expectedResponse, boolean failOnMatch)
    {
        switch (type)
        {
            case get:
                test.setFormElement(Locator.id("txtUrlGet"), url);
                test.click(Locator.xpath("//input[@id='btnGet']"));
                break;
            case post:
                test.setFormElement(Locator.id("txtUrlPost"), url);
                test.setFormElement(Locator.id("txtPost"), StringUtils.trimToEmpty(formData));
                test.click(Locator.xpath("//input[@id='btnPost']"));
                break;
        }

        if (test.isElementPresent(Locator.xpath("//div[@id='lblStatus' and contains(text(), 'ERROR')]")))
            fail("The request has failed: " + url);

        test.waitForText(test.getDefaultWaitForPage(),"Request Complete");

        // Once response has loaded, check it, also check 'Request Complete'
        if (!StringUtils.isEmpty(expectedResponse))
        {
            if (failOnMatch)
                test.assertElementNotPresent(Locator.xpath("//pre[@id='lblResponse' and contains(text(), '" + expectedResponse + "')]"));
            else
                test.assertElementPresent(Locator.xpath("//pre[@id='lblResponse' and contains(text(), '" + expectedResponse + "')]"));

            test.assertTextPresent("Request Complete.");
        }

        // clear all the forms elements
        test.setFormElement(Locator.id("txtUrlGet"), "");
        test.setFormElement(Locator.id("txtUrlPost"), "");
        test.setFormElement(Locator.id("txtPost"), "");
    }

    enum ActionType {
        get,
        post
    }

    public static class ApiTestCase
    {
        private String _name;
        private ActionType _type;
        private String _url;
        private String _response;
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

        public String getResponse()
        {
            return _response;
        }

        public void setResponse(String response)
        {
            _response = response;
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
