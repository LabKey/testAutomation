/*
 * Copyright (c) 2009 LabKey Corporation
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

package org.labkey.test.bvt;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.labkey.query.xml.ApiTestsDocument;
import org.labkey.query.xml.TestCaseType;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.PasswordUtil;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * An abstract class that can be used to test recorded API request/response interactions. A typical usage
 * would be for an extending class to perform whatever project setup was required and call super.doTestSteps.
 * The class would also implement getTestFiles which returns an array of recorded test files, the schema is
 * apiTest.xsd and a test can be recorded using the API test page: query/apiTest.view
 */
public abstract class SimpleApiTest extends BaseSeleniumWebTest
{
    private HttpClient _client;

    // json key elements to ignore during the comparison phase
    static final String[] _ignored = {
            "entityid",
            "containerid",
            "rowid",
            "lsid",
            "_labkeyurl_like",
            "id",
            "userId",
            "groupId",
            "message",
            "displayName"
    };

    enum ActionType {
        get,
        post
    }

    /**
     * Returns the list of files to run tests over. Each test file contains metadata representing
     * test cases, the metadata schema can be found in apiTest.xsd
     */
    protected abstract File[] getTestFiles();

    protected void doTestSteps() throws Exception
    {
        int tests = 0;
        for (File testFile : getTestFiles())
        {
            if (testFile.exists())
            {
                for (ApiTestCase test : parseTests(testFile))
                {
                    tests++;
                    log("Starting new test case: " + StringUtils.trimToEmpty(test.getName()));
                    sendRequestDirect(test.getUrl(), test.getType(), test.getFormData(), test.getReponse(), test.isFailOnMatch());
                    log("test case completed");
                }
            }
        }
        log("Finished running recorded tests, a total of " + tests + " were completed");
    }

    protected List<ApiTestCase> parseTests(File testFile)
    {
        try {
            List<ApiTestCase> tests = new ArrayList<ApiTestCase>();
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
        return "query";
    }

    private void sendRequestDirect(String url, ActionType type, String formData, String expectedResponse, boolean failOnMatch) throws UnsupportedEncodingException
    {
        HttpMethod method = null;
        String requestUrl = WebTestHelper.getBaseURL() + '/' + url;

        switch (type) {
            case get:
                method = new GetMethod(requestUrl);
                break;
            case post:
                method = new PostMethod(requestUrl);
                ((PostMethod)method).setRequestEntity(new StringRequestEntity(formData, "application/json", "UTF-8"));
                break;
        }

        if (method != null)
        {
            try {
                HttpClient client = getHttpClient(requestUrl);

                int status = client.executeMethod(method);
                if (status == HttpStatus.SC_OK)
                {
                    String response = method.getResponseBodyAsString();
                    if (compareResponse(response, expectedResponse))
                        log("response matched");
                    else
                        fail("The response: " + response + "\ndid not match the expected response: " + expectedResponse);
                }
            }
            catch (IOException e)
            {
                fail("Test failed requesting the URL: " + e.getMessage());
            }
            finally
            {
                method.releaseConnection();
            }
        }
    }

    private HttpClient getHttpClient(String url) throws URIException
    {
        if (_client == null)
        {
            _client = new HttpClient(new MultiThreadedHttpConnectionManager());
            _client.getState().setCredentials(
                    new AuthScope(new URI(url, false).getHost(),
                            AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                    new UsernamePasswordCredentials(PasswordUtil.getUsername(), PasswordUtil.getPassword())
            );
            //send basic auth header on first request
            _client.getParams().setAuthenticationPreemptive(true);
        }
        return _client;
    }

    private boolean compareResponse(String responseStr, String expectedResponseStr)
    {
        JSONObject response = (JSONObject)JSONValue.parse(responseStr);
        JSONObject expectedResponse = (JSONObject)JSONValue.parse(expectedResponseStr);

        return compareElement(response, expectedResponse);
    }

    private boolean compareMap(Map map1, Map map2)
    {
        if (map1.size() != map2.size())
        {
            logInfo("Comparison of maps failed: sizes are different: " + map1.size() + " and: " + map2.size());
            return false;
        }

        for (Object key : map1.keySet())
        {
            if (map2.containsKey(key))
            {
                if (!skipElement(String.valueOf(key)) && !compareElement(map1.get(key), map2.get(key)))
                    return false;
            }
            else
            {
                logInfo("Comparison of maps failed: could not find key: " + key);
                return false;
            }
        }
        return true;
    }

    private boolean compareList(List list1, List list2)
    {
        if (list1.size() != list2.size())
        {
            logInfo("Comparison of lists failed: sizes are different");
            return false;
        }

        // lists are not ordered
        for (int i=0; i < list1.size(); i++)
        {
            boolean matched = false;
            for (int j=0; j < list2.size(); j++)
            {
                if (compareElement(list1.get(i), list2.get(j)))
                {
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        return true;
    }

    private boolean compareElement(Object o1, Object o2)
    {
        if (o1 instanceof Map)
            return compareMap((Map)o1, (Map)o2);
        else if (o1 instanceof List)
            return compareList((List)o1, (List)o2);
        else
        {
            if (StringUtils.equals(String.valueOf(o1), String.valueOf(o2)))
                return true;
            else
            {
                logInfo("Comparison of elements: " + o1 + " and: " + o2 + " failed");
                return false;
            }
        }
    }

    private void logInfo(String msg)
    {
        log(msg);
    }

    private boolean skipElement(String element)
    {
        for (String ignore : _ignored)
            if (StringUtils.equalsIgnoreCase(element, ignore)) return true;

        return false;
    }

    private void sendRequest(String url, ActionType type, String formData, String expectedResponse, boolean failOnMatch)
    {
        switch (type)
        {
            case get:
                setFormElement("txtUrlGet", url);
                click(Locator.raw("//input[@id='btnGet']"));
                break;
            case post:
                setFormElement("txtUrlPost", url);
                setFormElement("txtPost", StringUtils.trimToEmpty(formData));
                click(Locator.raw("//input[@id='btnPost']"));
                break;
        }

        if (isElementPresent(Locator.raw("//div[@id='lblStatus' and contains(text(), 'ERROR')]")))
            fail("The request has failed: " + url);

        waitForText("Request Complete", defaultWaitForPage);

        // Once response has loaded, check it, also check 'Request Complete'
        if (!StringUtils.isEmpty(expectedResponse))
        {
            if (failOnMatch)
                assertElementNotPresent(Locator.raw("//pre[@id='lblResponse' and contains(text(), '" + expectedResponse + "')]"));
            else
                assertElementPresent(Locator.raw("//pre[@id='lblResponse' and contains(text(), '" + expectedResponse + "')]"));

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
