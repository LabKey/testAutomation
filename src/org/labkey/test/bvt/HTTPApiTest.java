/*
 * Copyright (c) 2008 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ListHelper;

/**
 * Created by IntelliJ IDEA.
 * User: Erik
 * Date: Jul 2, 2008
 * Time: 2:03:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class HTTPApiTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "HTTPApiVerifyProject";
    private static final String LIST_NAME = "Test List";
    private static final String LIST_NAME_URL = "Test%20List";

    private static final ListHelper.ListColumn COL1 = new ListHelper.ListColumn("Like", "Like", ListHelper.ListColumnType.String, "What the color is like");
    private static final ListHelper.ListColumn COL2 = new ListHelper.ListColumn("Month", "Month to Wear", ListHelper.ListColumnType.DateTime, "When to wear the color", "M");
    private static final ListHelper.ListColumn COL3 = new ListHelper.ListColumn("Good", "Quality", ListHelper.ListColumnType.Integer, "How nice the color is");
    private final static String[][] TEST_DATA = { { "Blue", "Green", "Red", "Yellow" },
            { "Zany", "Robust", "Mellow", "Light"},
            { "1", "4", "3", "2" },
            { "10", "9", "8", "7"} };
    private final static String[] CONVERTED_MONTHS = { "2000-01-01", "2000-04-04", "2000-03-03", "2000-02-02" };
    private final static String LIST_ROW1 = TEST_DATA[0][0] + "\t" + TEST_DATA[1][0] + "\t" + CONVERTED_MONTHS[0] + "\t" + TEST_DATA[3][0];
    private final static String LIST_ROW2 = TEST_DATA[0][1] + "\t" + TEST_DATA[1][1] + "\t" + CONVERTED_MONTHS[1] + "\t" + TEST_DATA[3][1];
    private final static String LIST_ROW3 = TEST_DATA[0][2] + "\t" + TEST_DATA[1][2] + "\t" + CONVERTED_MONTHS[2] + "\t" + TEST_DATA[3][2];
    private final static String LIST_ROW4 = TEST_DATA[0][3] + "\t" + TEST_DATA[1][3] + "\t" + CONVERTED_MONTHS[3] + "\t" + TEST_DATA[3][3];
    private final String LIST_DATA = "Color\t" + COL1.getName() +
            "\t" + COL2.getName() + "\t" + COL3.getName() + "\n" + LIST_ROW1 + "\n" + LIST_ROW2 + "\n" + LIST_ROW3 + "\n" + LIST_ROW4;

    public String getAssociatedModuleDirectory()
    {
        return "query";
    }

    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        log("Create Project");
        createProject(PROJECT_NAME);
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Lists");

        log("Create List");
        ListHelper.createList(this, PROJECT_NAME, LIST_NAME, ListHelper.ListColumnType.String, "Color", COL1, COL2, COL3);

        log("Upload data");
        clickLinkWithText("import data");
        setFormElement("ff_data", LIST_DATA);
        submit();
        
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Query");
        setFormElement("title", "Query");
        selectOptionByValue("schemaName", "lists");
        submit();

        beginAt("/query/" + PROJECT_NAME + "/apiTest.view?");

        // Begin query tests
        checkPostResponsePresent(getQueryURL("insertRows"),
                "{ \"schemaName\": \"lists\",\n" +
                        " \"queryName\": \"Test List\",\n" +
                        " \"command\": \"insert\",\n" +
                        " \"rowsAffected\": 1,\n" +
                        " \"rows\": [ {\n" +
                        "    \"Color\": \"Purple\",\n" +
                        "    \"Like\": \"Magenta\",\n" +
                        "    \"Month\": \"2000-01-01\",\n" +
                        "    \"Good\": \"4\"}\n" +
                        " ]\n" +
                        "}", 
                "Magenta", "Purple", "4");

        checkGetResponsePresent(getQueryURL("selectRows"),
                "Purple", "Magenta");

        checkPostResponsePresent(getQueryURL("insertRows"),
                "{ \"schemaName\": \"lists\",\n" +
                        " \"queryName\": \"Test List\",\n" +
                        " \"command\": \"insert\",\n" +
                        " \"rowsAffected\": 3,\n" +
                        " \"rows\": [ {\n" +
                        "    \"Color\": \"Pink\",\n" +
                        "    \"Like\": \"Rose\",\n" +
                        "    \"Month\": \"2000-02-01\",\n" +
                        "    \"Good\": \"7\"},\n{\n" +
                        "    \"Color\": \"Fuscia\",\n" +
                        "    \"Like\": \"Red\",\n" +
                        "    \"Month\": \"2000-03-01\",\n" +
                        "    \"Good\": \"1\"},\n{\n" +
                        "    \"Color\": \"Gray\",\n" +
                        "    \"Like\": \"Black\",\n" +
                        "    \"Month\": \"2000-04-01\",\n" +
                        "    \"Good\": \"2\"}\n" +
                        " ]\n" +
                        "}",
                "Pink", "Fuscia", "Gray");
        
        checkGetResponsePresent(getQueryURL("selectRows"), 
                "Pink", "Fuscia", "Gray");

        checkPostResponsePresent(getQueryURL("updateRows"),
                "{ \"schemaName\": \"lists\",\n" +
                        " \"queryName\": \"Test List\",\n" +
                        " \"command\": \"insert\",\n" +
                        " \"rowsAffected\": 2,\n" +
                        " \"rows\": [ {\n" +
                        "    \"Color\": \"Green\",\n" +
                        "    \"Like\": \"Jungle Green\",\n" +
                        "    \"Month\": \"2000-01-11\",\n" +
                        "    \"Good\": \"2\"},\n{\n" +
                        "    \"Color\": \"Fuscia\",\n" +
                        "    \"Like\": \"Crimson\"}]\n" +
                        "}",
                "Jungle Green", "Crimson");

        checkGetResponsePresent(getQueryURL("selectRows"), 
                "Jungle Green", "Crimson");
        
        checkPostResponsePresent(getQueryURL("deleteRows"),
                "{ \"schemaName\": \"lists\",\n" +
                        " \"queryName\": \"Test List\",\n" +
                        " \"command\": \"insert\",\n" +
                        " \"rowsAffected\": 3,\n" +
                        " \"rows\": [ " +
                        "{\n \"Color\": \"Green\"},\n" +
                        "{\n \"Color\": \"Blue\"},\n" +
                        "{\n \"Color\": \"Pink\"}]\n" +
                        "}",
                "Green", "Blue", "Pink");

        checkGetResponseNotPresent(getQueryURL("selectRows"),
                "Green", "Blue", "Pink");

    }

    /**
     * If executed at the apiTest.view page, this will check the response section for the Strings provided after
     * executing a GET on the url.
     * @param url
     * @param expectedResponse
     */
    private void checkGetResponsePresent(String url, String... expectedResponse)
    {
        checkGetResponse(url, true, expectedResponse);
    }

    /**
     * If executed at the apiTest.view page, this will check that the response section does not contain the Strings provided after
     * executing a GET on the url.
     * @param url
     * @param unexpectedResponse
     */
    private void checkGetResponseNotPresent(String url, String... unexpectedResponse)
    {
        checkGetResponse(url, false, unexpectedResponse);
    }

    /**
     * If executed at the apiTest.view page, this will check that the response section contains the Strings provided after
     * executing a POST on the url with the given POST body.
     * @param url
     * @param postBody
     * @param expectedResponse
     */
    private void checkPostResponsePresent(String url, String postBody, String... expectedResponse)
    {
        checkPostResponse(url, postBody, true, expectedResponse);
    }

    /**
     * If executed at the apiTest.view page, this will check that the response section does not contain the Strings provided after
     * executing a POST on the url with the given POST body.
     * @param url
     * @param postBody
     * @param unexpectedResponse
     */
    private void checkPostResponseNotPresent(String url, String postBody, String... unexpectedResponse)
    {
        checkPostResponse(url, postBody, false, unexpectedResponse);
    }

    private void checkGetResponse(String url, boolean responsesPresent, String[] expectedResponses)
    {
        setFormElement("txtUrlGet", url);
        click(Locator.raw("//input[@id='btnGet']"));
        checkSuccess(responsesPresent, expectedResponses);
        // clear get url
        setFormElement("txtUrlGet", "");
    }

    private void checkPostResponse(String url, String postBody, boolean responsesPresent, String[] expectedResponses)
    {
        setFormElement("txtUrlPost", url);
        setFormElement("txtPost", postBody);
        click(Locator.raw("//input[@id='btnPost']"));
        checkSuccess(responsesPresent, expectedResponses);
        // clear get url
        setFormElement("txtUrlPost", "");
        setFormElement("txtPost", "");
    }

    private void checkSuccess(boolean responsesPresent, String[] expectedResponses)
    {
        assertElementNotPresent(Locator.raw("//div[@id='lblStatus' and contains(text(), 'ERROR')]"));
        waitForText("Request Complete", defaultWaitForPage);
        // Once response has loaded, check it, also check 'Request Complete'
        if (expectedResponses.length > 0)
        {
            if (responsesPresent)
            {
                waitForElement(Locator.raw("//pre[@id='lblResponse' and contains(text(), '" + expectedResponses[0] + "')]"), defaultWaitForPage);
            }
            checkResponses(responsesPresent, expectedResponses);
            assertTextPresent("Request Complete.");
        }
    }

    private void checkResponses(boolean responsesPresent, String[] responses)
    {
        for (int i = 0; i < responses.length; i++)
        {
            if (responsesPresent)
            {
                assertElementPresent(Locator.raw("//pre[@id='lblResponse' and contains(text(), '" + responses[i] + "')]"));
            }
            else
            {
                assertElementNotPresent(Locator.raw("//pre[@id='lblResponse' and contains(text(), '" + responses[i] + "')]"));
            }
        }
    }

    private String getQueryURL(String query)
    {
        return getBaseURL() + "/query/" + PROJECT_NAME + "/" + query + ".api?schemaName=lists&query.queryName=" + LIST_NAME_URL;
    }
}
