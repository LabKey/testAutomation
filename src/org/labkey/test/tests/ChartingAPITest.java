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

import static org.junit.Assert.*;

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

    protected static final String SCATTER_ONE_TEXT = "Created with Rapha\u00ebl 2.1.0\n0\n200\n400\n600\n800\n1000\n1200\n1400\n800\n1000\n1200\n1400\n1600\n1800\n2000\nScatter Plot One\nCD4+ (cells/mm3)\nLymphs (cells/mm3)";
    protected static final String SCATTER_TWO_TEXT = "Created with Rapha\u00ebl 2.1.0\n0\n200\n400\n600\n800\n1000\n1200\n1400\n800\n1000\n1200\n1400\n1600\n1800\n2000\nScatter Plot Two (Custom)\nCD4\nLymphs\nFemales\nMales";
    protected static final String BOX_ONE_TEXT = "Created with Rapha\u00ebl 2.1.0\nMales\nFemales\n600\n800\n1000\n1200\n1400\n1600\n1800\n2000\n2200\nBox Plot One\nGender\nLymphs (cells/mm3)";
    protected static final String BOX_TWO_TEXT = "Created with Rapha\u00ebl 2.1.0\nMales\nFemales\n200\n400\n600\n800\n1000\n1200\n1400\n1600\nBox Plot Two (Custom)\nGender\nCD4\nFemales";

    protected static final String TIME_CHART_1 = "Luminex: 249318596";
    protected static final String TIME_CHART_1_TEXT_1 = "Created with Rapha\u00ebl 2.1.0\n10\n15\n20\n25\n30\n35\n70\n700\n50\n100\n150\n200\n250\n300\n350\nLuminex: 249318596\nWeeks Since Demographic Start Date\nFluorescence Intensity\nObserved Concentration\n249318596 Fi IL-10\n249318596 Fi IL-2\n249318596 Fi TNF-alpha\n249318596 ObsConc IL-10\n249318596 ObsConc IL-2\n249318596 ObsConcTNF-alpha";
    protected static final String TIME_CHART_1_TEXT_2 = "Created with Rapha\u00ebl 2.1.0\n0\n5\n10\n15\n20\n25\n30\n70\n700\n7000\n50\n100\n150\n200\n250\n300\n350\nLuminex: 249320107\nWeeks Since Demographic Start Date\nFluorescence Intensity\nObserved Concentration\n249320107 Fi IL-10\n249320107 Fi IL-2\n249320107 Fi TNF-alpha\n249320107 ObsConc IL-10\n249320107 ObsConc IL-2\n249320107 ObsConcTNF-alpha";
    protected static final String TIME_CHART_1_TEXT_3 = "Created with Rapha\u00ebl 2.1.0\n0\n5\n10\n15\n20\n25\n30\n80\n90\n100\n200\n300\n50\n100\n150\n200\n250\n300\n350\nLuminex: 249320127\nWeeks Since Demographic Start Date\nFluorescence Intensity\nObserved Concentration\n249320127 Fi IL-10\n249320127 Fi IL-2\n249320127 Fi TNF-alpha\n249320127 ObsConc IL-10\n249320127 ObsConc IL-2\n249320127 ObsConcTNF-alpha";
    protected static final String TIME_CHART_2 = "Luminex Two";
    protected static final String TIME_CHART_2_TEXT_1 = "Created with Rapha\u00ebl 2.1.0\n1\n2\n3\n4\n5\n0.2\n0.4\n0.6\n0.8\n1.0\n1.2\n1.4\n1.6\n1.8\n2.0\nLuminex Two\nVisits\nFI\n249318596\n249320127";
    protected static final String TIME_CHART_3 = "Male";
    protected static final String TIME_CHART_3_TEXT_1 = "Created with Rapha\u00ebl 2.1.0\n1\n2\n3\n4\n5\n0\n5\n10\n15\n20\n25\n30\n35\n40\n45\n50\nMale\nVisit\nObs Conc\nMale";
    protected static final String TIME_CHART_3_TEXT_2 = "Created with Rapha\u00ebl 2.1.0\n1\n2\n3\n4\n5\n0\n50\n100\n150\n200\n250\n300\nFemale\nVisit\nObs Conc\nFemale";
    protected static final String TIME_CHART_4 = "Luminex Four";
    protected static final String TIME_CHART_4_TEXT_1 = "Created with Rapha\u00ebl 2.1.0\n200\n400\n600\n800\n1000\n1200\n1400\nLuminex Four\nDays Since Start Date\nFi\n249318596";
    protected static final String TIME_CHART_5 = "Luminex Five";
    protected static final String TIME_CHART_5_TEXT_1 = "Created with Rapha\u00ebl 2.1.0\n0\n50\n100\n150\n200\n1000\n2000\n3000\n4000\n5000\n6000\n7000\nLuminex Five\nDays Since Start Date\nFi\n249318596 IL-10\n249318596 IL-2\n249318596 IL-6\n249318596 TNF-alpha\n249320107 IL-10\n249320107 IL-2\n249320107 IL-6\n249320107 TNF-alpha\n249320127 IL-10\n249320127 IL-2\n249320127 IL-6\n249320127 TNF-alpha";

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
        timeChartHelperTest();
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
            fail("Test div does not contain an image:\n" + chartHtml);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void chartAPITest() throws Exception
    {
        File chartTestFile = new File(getApiFileRoot(), "chartingAPITest.js");
        createAPITestWiki("chartTestWiki2", chartTestFile, true);

        //Some things we know about test 0. After this we loop through some others and just test to see if they convert
        waitForText("Current Config");

        String testCountStr = getFormElement(Locator.id("configCount"));
        int testCount = Integer.parseInt(testCountStr);
        for (int currentTest = 0; currentTest < testCount; currentTest++)
        {
            waitForText(CHARTING_API_TITLES[currentTest]);
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
            assertEquals("SVG Downloaded", HttpStatus.SC_OK, status);
            assertTrue(response.getHeaders("Content-Disposition")[0].getValue().startsWith("attachment;"));
            assertTrue(response.getHeaders("Content-Type")[0].getValue().startsWith("application/pdf"));
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
        createAPITestWiki("exportGenericChartTestWiki", chartTestFile, false);

        waitForText(SCATTER_ONE);
        checkExportedChart(SCATTER_ONE, SCATTER_ONE_TEXT);

        click(Locator.input("next-btn"));
        waitForText(SCATTER_TWO);
        checkExportedChart(SCATTER_TWO, SCATTER_TWO_TEXT);

        click(Locator.input("next-btn"));
        waitForText(BOX_ONE);
        checkExportedChart(BOX_ONE, BOX_ONE_TEXT);

        click(Locator.input("next-btn"));
        waitForText(BOX_TWO);
        checkExportedChart(BOX_TWO, BOX_TWO_TEXT, true);

        click(Locator.input("next-btn"));
        waitForText("The measure Cohort was not found. It may have been renamed or removed.");
        checkExportedChart(BOX_THREE, null, true, 0);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void timeChartHelperTest()
    {
        File chartTestFile = new File(getApiFileRoot(), "timeChartHelperApiTest.html");
        createAPITestWiki("exportTimeChartTestWiki", chartTestFile, false);

        waitForText(TIME_CHART_1);
        checkExportedChart(TIME_CHART_1, TIME_CHART_1_TEXT_1, false, 3, 0);
        checkExportedChart(TIME_CHART_1, TIME_CHART_1_TEXT_2, false, 3, 1);
        checkExportedChart(TIME_CHART_1, TIME_CHART_1_TEXT_3, false, 3, 2);

        click(Locator.input("next-btn"));
        waitForText(TIME_CHART_2);
        checkExportedChart(TIME_CHART_2, TIME_CHART_2_TEXT_1);

        click(Locator.input("next-btn"));
        waitForText(TIME_CHART_3);
        checkExportedChart(TIME_CHART_3, TIME_CHART_3_TEXT_1, false, 2, 0);
        checkExportedChart(TIME_CHART_3, TIME_CHART_3_TEXT_2, false, 2, 1);

        click(Locator.input("next-btn"));
        waitForText(TIME_CHART_4);
        checkExportedChart(TIME_CHART_4, TIME_CHART_4_TEXT_1, true);
        assertTextPresent("No calculated interval values (i.e. Days, Months, etc.) for the selected 'Measure Date' and 'Interval Start Date'.");

        click(Locator.input("next-btn"));
        waitForText(TIME_CHART_5);
        checkExportedChart(TIME_CHART_5, TIME_CHART_5_TEXT_1, true);
        assertTextPresent("The data limit for plotting has been reached. Consider filtering your data.");
        assertTextPresent("No data found for the following measures/dimensions: IL-6");

        click(Locator.input("next-btn"));
        waitForText("No measure selected. Please select at lease one measure.");
        click(Locator.input("next-btn"));
        waitForText("Could not find x-axis in chart measure information.");
        click(Locator.input("next-btn"));
        waitForText("No participant selected. Please select at least one participant.");
        click(Locator.input("next-btn"));
        waitForText("No group selected. Please select at least one group.");
        click(Locator.input("next-btn"));
        waitForText("No series or dimension selected. Please select at least one series/dimension value.");
        click(Locator.input("next-btn"));
        waitForText("Please select either \"Show Individual Lines\" or \"Show Mean\".");
    }


    private void checkExportedChart(String title, String svgText)
    {
        checkExportedChart(title, svgText, false, 1);
    }

    private void checkExportedChart(String title, String svgText, boolean hasError)
    {
        checkExportedChart(title, svgText, hasError, 1);
    }

    private void checkExportedChart(String title, @Nullable String svgText, boolean hasError, int svgCount)
    {
        checkExportedChart(title, svgText, hasError, svgCount, 0);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void checkExportedChart(String title, @Nullable String svgText, boolean hasError, int svgCount, int svgIndex)
    {
        if (hasError)
        {
            assertTrue("Expected one error", getElementCount(Locator.css(".labkey-error")) == 1);
        }
        else
        {
            assertTrue("Expected zero errors.", getElementCount(Locator.css(".labkey-error")) == 0);
        }

        if (svgCount > 0)
        {
            assertTextPresent(title);
            assertTrue("Expected " + svgCount + " SVG element(s).", getElementCount(Locator.css("svg")) == svgCount);
        }
        else
        {
            assertTrue("Expected 0 SVG elements.", getElementCount(Locator.css("svg")) == 0);
        }

        if (svgText != null)
        {
            assertSVG(svgText, svgIndex);
        }
    }
}
