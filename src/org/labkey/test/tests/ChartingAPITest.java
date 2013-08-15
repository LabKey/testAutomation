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
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
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
@Category(BVT.class)
public class ChartingAPITest extends ClientAPITest
{
    protected static final String[] CHARTING_API_TITLES = {
            "Line Plot - no y-scale defined",
            "Line Plot - y-scale defined, no legend, no shape aes",
            "Line Plot - No Layer AES, Changed Opacity",
            "Two Axis Scatter, plot null points",
            "Discrete X Scale Scatter No Geom Config",
            "Discrete X Scale Scatter, Log Y",
            "Boxplot no Geom Config",
            "Boxplot No Outliers",
            "Boxplot No Outliers, All Points"
    };

    protected static final String SCATTER_ONE = "Scatter Plot One";
    protected static final String SCATTER_TWO = "Scatter Plot Two (Custom)";
    protected static final String BOX_ONE = "Box Plot One";
    protected static final String BOX_TWO = "Box Plot Two (Custom)";
    protected static final String BOX_THREE = "Box Plot Three (Custom, Broken)";

    protected static final String SCATTER_ONE_TEXT = "Created with Rapha\u00ebl 2.1.0\nScatter Plot One\n200\n400\n600\n800\n1000\n1200\n1400\nCD4+ (cells/mm3)\nLymphs (cells/mm3)\n800\n1000\n1200\n1400\n1600\n1800\n2000";
    protected static final String SCATTER_TWO_TEXT = "Created with Rapha\u00ebl 2.1.0\nScatter Plot Two (Custom)\n200\n400\n600\n800\n1000\n1200\n1400\nCD4\nLymphs\n800\n1000\n1200\n1400\n1600\n1800\n2000\nMales\nFemales";
    protected static final String BOX_ONE_TEXT = "Created with Rapha\u00ebl 2.1.0\nBox Plot One\nMales\nFemales\nGender\nLymphs (cells/mm3)\n600\n800\n1000\n1200\n1400\n1600\n1800\n2000\n2200";
    protected static final String BOX_TWO_TEXT = "Created with Rapha\u00ebl 2.1.0\nBox Plot Two (Custom)\nMales\nFemales\nGender\nCD4\n200\n400\n600\n800\n1000\n1200\n1400\n1600\nFemales";

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
        genericChartHelperTest();
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
            click(Locator.ext4Button("Next"));
        }
    }

    private void checkSVGConversion() throws Exception
    {
        //The server side svg converter is fairly strict and will fail with bad inputs
        clickButton("Get SVG", 0);
        waitForElement(Locator.id("svgtext"));
        String svgText = getFormElement(Locator.id("svgtext"));

        String url = WebTestHelper.getBaseURL() + "/visualization/" + EscapeUtil.encode(getProjectName())+ "/exportPDF.view";
        HttpClient httpClient = WebTestHelper.getHttpClient();
        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpPost method;
        HttpResponse response = null;

        try
        {
            method = new HttpPost(url);
            List<NameValuePair> args = new ArrayList<>();
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

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void genericChartHelperTest()
    {
        File chartTestFile = new File(getApiFileRoot(), "genericChartHelperApiTest.html");
        createAPITestWiki("exportChartTestWiki", chartTestFile, false);

        waitForText(SCATTER_ONE, WAIT_FOR_JAVASCRIPT);
        checkExportedChart(SCATTER_ONE, SCATTER_ONE_TEXT);

        click(Locator.input("next-btn"));
        waitForText(SCATTER_TWO, WAIT_FOR_JAVASCRIPT);
        checkExportedChart(SCATTER_TWO, SCATTER_TWO_TEXT);

        click(Locator.input("next-btn"));
        waitForText(BOX_ONE, WAIT_FOR_JAVASCRIPT);
        checkExportedChart(BOX_ONE, BOX_ONE_TEXT);

        click(Locator.input("next-btn"));
        waitForText(BOX_TWO, WAIT_FOR_JAVASCRIPT);
        checkExportedChart(BOX_TWO, BOX_TWO_TEXT, true);

        click(Locator.input("next-btn"));
        waitForText("The measure Cohort was not found. It may have been renamed or removed.", WAIT_FOR_JAVASCRIPT);
        checkExportedChart(BOX_THREE, null, true, false);
    }


    private void checkExportedChart(String title, String svgText)
    {
        checkExportedChart(title, svgText, false, true);
    }

    private void checkExportedChart(String title, String svgText, boolean hasError)
    {
        checkExportedChart(title, svgText, hasError, true);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void checkExportedChart(String title, @Nullable String svgText, boolean hasError, boolean hasSVG)
    {

        if (hasError)
        {
            Assert.assertTrue("Expected one error", getElementCount(Locator.css(".labkey-error")) == 1);
        }
        else
        {
            Assert.assertTrue("Expected zero errors.", getElementCount(Locator.css(".labkey-error")) == 0);
        }

        if (hasSVG)
        {
            assertTextPresent(title);
            Assert.assertTrue("Expected 1 SVG element.", getElementCount(Locator.css("svg")) == 1);
        }
        else
        {
            Assert.assertTrue("Expected 0 SVG elements.", getElementCount(Locator.css("svg")) == 0);
        }

        if (svgText != null)
        {
            assertSVG(svgText);
        }
    }
}
