/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: tchadick
 * Date: 3/11/13
 * Time: 1:55 PM
 */
public class ChartingAPITest extends ClientAPITest
{
    @Override
    protected String getProjectName()
    {
        return "ChartingAPITest Project";
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }


    @Override
    protected void doTestSteps() throws Exception
    {
        initProject();

        chartTest();
        chartAPITest();
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void initProject()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME, null);
        createPeopleList();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void chartTest()
    {
        File chartTestFile = new File(getApiFileRoot(), "chartTest.js");
        String chartHtml = createAPITestWiki("chartTestWiki", chartTestFile, true);

        if (!chartHtml.contains("<img") && !chartHtml.contains("<IMG"))
            Assert.fail("Test div does not contain an image:\n" + chartHtml);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void chartAPITest() throws Exception
    {
        File chartTestFile = new File(getApiFileRoot(), "chartingAPITest.js");
        createAPITestWiki("chartTestWiki2", chartTestFile, true);

        //Some things we know about test 0. After this we loop through some others and just test to see if they convert
        waitForText("Current Config", WAIT_FOR_JAVASCRIPT);

        String testCountStr = getFormElement(Locator.id("configCount"));
        int testCount = Integer.parseInt(testCountStr);
        for (int currentTest = 0; currentTest < testCount; currentTest++)
        {
            waitForText(CHARTING_API_TITLES[currentTest], WAIT_FOR_JAVASCRIPT);
            checkSVGConversion();
            click(Locator.buttonContainingText("Next"));
        }
    }

    private void checkSVGConversion() throws Exception
    {
        //The server side svg converter is fairly strict and will fail with bad inputs
        clickButton("Get SVG", 0);
        String svgText = getFormElement(Locator.id("svgtext"));

        String url = WebTestHelper.getBaseURL() + "/visualization/" + EscapeUtil.encode(getProjectName())+ "/exportPDF.view";
        HttpClient httpClient = WebTestHelper.getHttpClient();
        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpPost method;
        HttpResponse response = null;

        try
        {
            method = new HttpPost(url);
            List<NameValuePair> args = new ArrayList<NameValuePair>();
            args.add(new BasicNameValuePair("svg", svgText));
            method.setEntity(new UrlEncodedFormEntity(args));
            response = httpClient.execute(method, context);
            int status = response.getStatusLine().getStatusCode();
            Assert.assertEquals("SVG Downloaded", HttpStatus.SC_OK, status);
            Assert.assertTrue(response.getHeaders("Content-Disposition")[0].getValue().startsWith("attachment;"));
            Assert.assertTrue(response.getHeaders("Content-Type")[0].getValue().startsWith("application/pdf"));
        }
        finally
        {
            if (null != response)
                EntityUtils.consume(response.getEntity());
            if (httpClient != null)
                httpClient.getConnectionManager().shutdown();
        }
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}
