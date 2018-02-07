/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.Charting;
import org.labkey.test.util.APITestHelper;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({BVT.class, Charting.class})
public class ChartingAPITest extends BaseWebDriverTest
{
    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
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
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        ChartingAPITest initTest = (ChartingAPITest)getCurrentTest();
        initTest.initProject();
    }

    public void initProject()
    {
        _containerHelper.createProject(getProjectName(), null);
        createPeopleList();
    }

    @Before
    public void preTest()
    {
        clickProject(getProjectName());
    }

    private String goToChartingTestPage(String linkText)
    {
        goToModule("chartingapi");
        waitForElement(Locator.linkWithText(linkText));
        clickAndWait(Locator.linkWithText(linkText));
        return waitForWikiDivPopulation("testDiv", 30);
    }

    private void createPeopleList()
    {
        String data = ClientAPITest.getListData(ClientAPITest.LIST_KEY_NAME, ClientAPITest.LIST_COLUMNS, ClientAPITest.TEST_DATA);

        _listHelper.createList(getProjectName(), ClientAPITest.LIST_NAME, ClientAPITest.LIST_KEY_TYPE, ClientAPITest.LIST_KEY_NAME, ClientAPITest.LIST_COLUMNS);
        _listHelper.clickImportData();
        setFormElement(Locator.name("text"), data);
        _listHelper.submitImportTsv_success();
    }

    @Test
    public void chartTest()
    {
        String chartHtml = goToChartingTestPage("chartTest");

        if (!chartHtml.contains("<img") && !chartHtml.contains("<IMG"))
            fail("Test div does not contain an image:\n" + chartHtml);
    }

    @Test
    public void chartAPITest() throws Exception
    {
        goToChartingTestPage("chartTest2");
        verifyChartAPIPlots(new String[]{
            "Line Plot - no y-scale defined",
            "Line Plot - y-scale defined, no legend, no shape aes",
            "Line Plot - No Layer AES, Changed Opacity",
            "Two Axis Scatter, plot null points",
            "Discrete X Scale Scatter No Geom Config",
            "Discrete X Scale Scatter, Log Y",
            "Binned plot, hex shape",
            "Binned plot, square shape",
            "Boxplot no Geom Config",
            "Boxplot No Outliers",
            "Boxplot No Outliers, All Points"
        });
    }

    @Test
    public void customChartPlotWrapperTest() throws Exception
    {
        goToChartingTestPage("customPlotWrappers");
        verifyChartAPIPlots(new String[]{
            "Barplot With Cumulative Totals",
            "Levey-Jennings Plot",
            "Survival Curve Plot",
            "Base Pie Chart"
        });
    }

    private void verifyChartAPIPlots(String[] chartTitles) throws Exception
    {
        waitForElement(Locator.tagWithText("label", "Current Config:"));

        String testCountStr = getFormElement(Locator.id("configCount"));
        int testCount = Integer.parseInt(testCountStr);
        for (int currentTest = 0; currentTest < testCount; currentTest++)
        {
            waitForSvgWithTitle(chartTitles[currentTest]);
            checkSVGConversion();
            click(Ext4Helper.Locators.ext4Button("Next"));
        }
    }

    private void waitForSvgWithTitle(String title)
    {
        waitForSvgWithTitle(title, true);
    }

    private void waitForSvgWithTitle(String title, boolean wait)
    {
        waitForSvgWithTitleAndSubtitle(title, null, wait);
    }

    private void waitForSvgWithTitleAndSubtitle(String title, String subtitle, boolean wait)
    {
        Locator l = Locator.css("svg text").withText(title);
        assertOrWait(l, wait);

        if (subtitle != null)
            assertElementPresent(Locator.css("svg text").withText(subtitle));
    }

    private void assertErrorMessage(String msg, boolean wait)
    {
        Locator l = Locator.tagWithClass("div", "labkey-error").containing(msg);
        assertOrWait(l, wait);
    }

    private void assertOrWait(Locator l, boolean wait)
    {
        if (wait)
            waitForElement(l);
        else
            assertElementPresent(l);
    }

    private void checkSVGConversion() throws Exception
    {
        //The server side svg converter is fairly strict and will fail with bad inputs
        String svgText = (String)executeScript("return LABKEY.vis.SVGConverter.svgToStr(Ext4.query('svg')[0]);");

        // TODO can we add a PNG export check here as well?
        String url = WebTestHelper.getBaseURL() + "/visualization/" + EscapeUtil.encode(getProjectName())+ "/exportPDF.view";
        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpPost method;
        HttpResponse response = null;

        try (CloseableHttpClient httpClient = (CloseableHttpClient)WebTestHelper.getHttpClient())
        {
            method = new HttpPost(url);
            APITestHelper.injectCookies(method);
            List<NameValuePair> args = new ArrayList<>();
            args.add(new BasicNameValuePair("svg", svgText));
            method.setEntity(new UrlEncodedFormEntity(args));
            response = httpClient.execute(method, context);
            int status = response.getStatusLine().getStatusCode();
            assertEquals("SVG Downloaded", HttpStatus.SC_OK, status);
            assertTrue(response.getHeaders("Content-Disposition")[0].getValue().startsWith("attachment;"));
            assertTrue(response.getHeaders("Content-Type")[0].getValue().startsWith("application/pdf"));

            EntityUtils.consumeQuietly(response.getEntity()); // Prevent server-side TranscoderException
        }
        finally
        {
            if (null != response)
                EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    protected static final String SCATTER_ONE = "Scatter Plot One";
    protected static final String SCATTER_TWO = "Scatter Plot Two (Custom)";
    protected static final String BOX_ONE = "Box Plot One";
    protected static final String BOX_TWO = "Box Plot Two (Custom)";
    protected static final String BOX_THREE = "Box Plot Three (Custom, Broken)";

    protected static final String SCATTER_ONE_TEXT = "0\n200\n400\n600\n800\n1000\n1200\n1400\n800\n1000\n1200\n1400\n1600\n1800\n2000\nScatter Plot One\nCD4+ (cells/mm3)\nLymphs (cells/mm3)";
    protected static final String SCATTER_TWO_TEXT = "0\n200\n400\n600\n800\n1000\n1200\n1400\n800\n1000\n1200\n1400\n1600\n1800\n2000\nScatter Plot Two (Custom)\nCD4\nLymphs\nFemales\nMales";
    protected static final String BOX_ONE_TEXT = "Females\nMales\n600\n800\n1000\n1200\n1400\n1600\n1800\n2000\n2200\nBox Plot One\nGender\nLymphs (cells/mm3)";
    protected static final String BOX_TWO_TEXT = "Females\nMales\n0\n200\n400\n600\n800\n1000\n1200\n1400\n1600\nBox Plot Two (Custom)\nGender\nCD4\nFemales";

    @Test
    public void genericChartHelperTest()
    {
        goToChartingTestPage("exportGenericChartTest");

        waitForSvgWithTitle(SCATTER_ONE);
        checkExportedChart(SCATTER_ONE, SCATTER_ONE_TEXT);

        click(Locator.input("next-btn"));
        waitForSvgWithTitle(SCATTER_TWO);
        checkExportedChart(SCATTER_TWO, SCATTER_TWO_TEXT);

        click(Locator.input("next-btn"));
        waitForSvgWithTitle(BOX_ONE);
        checkExportedChart(BOX_ONE, BOX_ONE_TEXT);

        click(Locator.input("next-btn"));
        waitForSvgWithTitle(BOX_TWO);
        checkExportedChart(BOX_TWO, BOX_TWO_TEXT, true);

        click(Locator.input("next-btn"));
        assertErrorMessage("The measure Cohort was not found. It may have been renamed or removed.", true);
        checkExportedChart(BOX_THREE, null, true, 0);
    }

    protected static final String TIME_CHART_1 = "Luminex";
    protected static final String TIME_CHART_1_TEXT_1 = "10\n15\n20\n25\n30\n35\n70\n700\n50\n100\n150\n200\n250\n300\n350\nLuminex\n249318596\nWeeks Since Demographic Start Date\nFluorescence Intensity\nObserved Concentration\n249318596 Fi IL-10\n249318596 Fi IL-2\n249318596 Fi TNF-alpha\n249318596 ObsConc IL-10\n249318596 ObsConc IL-2\n249318596 ObsConc TNF-alpha";
    protected static final String TIME_CHART_1_TEXT_2 = "0\n5\n10\n15\n20\n25\n30\n70\n700\n7000\n50\n100\n150\n200\n250\n300\n350\nLuminex\n249320107\nWeeks Since Demographic Start Date\nFluorescence Intensity\nObserved Concentration\n249320107 Fi IL-10\n249320107 Fi IL-2\n249320107 Fi TNF-alpha\n249320107 ObsConc IL-10\n249320107 ObsConc IL-2\n249320107 ObsConc TNF-alpha";
    protected static final String TIME_CHART_1_TEXT_3 = "0\n5\n10\n15\n20\n25\n30\n80\n90\n100\n200\n300\n50\n100\n150\n200\n250\n300\n350\nLuminex\n249320127\nWeeks Since Demographic Start Date\nFluorescence Intensity\nObserved Concentration\n249320127 Fi IL-10\n249320127 Fi IL-2\n249320127 Fi TNF-alpha\n249320127 ObsConc IL-10\n249320127 ObsConc IL-2\n249320127 ObsConc TNF-alpha";
    protected static final String TIME_CHART_2 = "Luminex Two";
    protected static final String TIME_CHART_2_TEXT_1 = "1\n2\n3\n4\n5\n0.0\n0.2\n0.4\n0.6\n0.8\n1.0\n1.2\n1.4\n1.6\n1.8\n2.0\nLuminex Two\nVisits\nFI\n249318596\n249320127";
    protected static final String TIME_CHART_3 = "Male";
    protected static final String TIME_CHART_3_TEXT_1 = "1\n2\n3\n4\n5\n0\n5\n10\n15\n20\n25\n30\n35\n40\n45\n50\nMale\nVisit\nObs Conc\nMale";
    protected static final String TIME_CHART_3_TEXT_2 = "1\n2\n3\n4\n5\n0\n50\n100\n150\n200\n250\n300\nFemale\nVisit\nObs Conc\nFemale";
    protected static final String TIME_CHART_4 = "Luminex Four";
    protected static final String TIME_CHART_4_TEXT_1 = "200\n400\n600\n800\n1000\n1200\n1400\nLuminex Four\nDays Since Start Date\nFi\n249318596";
    protected static final String TIME_CHART_5 = "Luminex Five";
    protected static final String TIME_CHART_5_TEXT_1 = "0\n50\n100\n150\n200\n0\n1000\n2000\n3000\n4000\n5000\n6000\n7000\nLuminex Five\nDays Since Start Date\nFi\n249318596 IL-10\n249318596 IL-2\n249318596 IL-6\n249318596 TNF-alpha\n249320107 IL-10\n249320107 IL-2\n249320107 IL-6\n249320107 TNF-alpha\n249320127 IL-10\n249320127 IL-2\n249320127 IL-6\n249320127 TNF-alpha";

    @Test
    public void timeChartHelperTest()
    {
        goToChartingTestPage("exportTimeChartTest");

        waitForSvgWithTitleAndSubtitle(TIME_CHART_1, "249318596", true);
        checkExportedChart(TIME_CHART_1, TIME_CHART_1_TEXT_1, false, 3, 0);
        checkExportedChart(TIME_CHART_1, TIME_CHART_1_TEXT_2, false, 3, 1);
        checkExportedChart(TIME_CHART_1, TIME_CHART_1_TEXT_3, false, 3, 2);

        click(Locator.input("next-btn"));
        waitForSvgWithTitle(TIME_CHART_2);
        checkExportedChart(TIME_CHART_2, TIME_CHART_2_TEXT_1);

        click(Locator.input("next-btn"));
        waitForSvgWithTitle(TIME_CHART_3);
        checkExportedChart(TIME_CHART_3, TIME_CHART_3_TEXT_1, false, 2, 0);
        checkExportedChart(TIME_CHART_3, TIME_CHART_3_TEXT_2, false, 2, 1);

        click(Locator.input("next-btn"));
        waitForSvgWithTitle(TIME_CHART_4);
        checkExportedChart(TIME_CHART_4, TIME_CHART_4_TEXT_1, true);
        assertErrorMessage("No calculated interval values (i.e. Days, Months, etc.) for the selected 'Measure Date' and 'Interval Start Date'.", false);

        click(Locator.input("next-btn"));
        waitForSvgWithTitle(TIME_CHART_5);
        checkExportedChart(TIME_CHART_5, TIME_CHART_5_TEXT_1, true);
        assertErrorMessage("The data limit for plotting has been reached. Consider filtering your data.", false);
        assertErrorMessage("No data found for the following measures/dimensions: IL-6", false);

        click(Locator.input("next-btn"));
        assertErrorMessage("No measure selected. Please select at lease one measure.", true);
        click(Locator.input("next-btn"));
        assertErrorMessage("Could not find x-axis in chart measure information.", true);
        click(Locator.input("next-btn"));
        assertErrorMessage("No participant selected. Please select at least one participant.", true);
        click(Locator.input("next-btn"));
        assertErrorMessage("No group selected. Please select at least one group.", true);
        click(Locator.input("next-btn"));
        assertErrorMessage("No series or dimension selected. Please select at least one series/dimension value.", true);
        click(Locator.input("next-btn"));
        assertErrorMessage("Please select either \"Show Individual Lines\" or \"Show Mean\".", true);
    }
    protected static final String BOX_PLOT_COLOR_SHAPE = "Box Plot - Change outlier color/shape";
    protected static final String SCATTER_PLOT_SHAPE_COLOR_X = "Scatter - add shape/color change x";
    protected static final String SCATTER_PLOT_SHAPE_COLOR_X_SVG_BEFORE = "800\n1000\n1200\n1400\n1600\n1800\n2000\n0\n200\n400\n600\n800\n1000\n1200\n1400\nScatter - add shape/color change x";
    protected static final String SCATTER_PLOT_SHAPE_COLOR_X_SVG_AFTER = "0\n200\n400\n600\n800\n1000\n1200\n1400\n0\n200\n400\n600\n800\n1000\n1200\n1400\nScatter - add shape/color change x\n103866\n110349\n119180\n125478\nMales\nFemales";
    protected static final String SCATTER_HOVER_CLICK = "Scatter with hover and click";
    protected static final String LINE_ERROR_COLOR_Y = "Line/Error - Add color, change Y";
    protected static final String LINE_ERROR_COLOR_Y_SVG = "0\n5\n10\n15\n20\n0\n10\n20\n30\n40\n50\n60\n70\n80\n90\n100\nLine/Error - Add color, change Y\nAlan\nTrey\nNick";
    protected static final String SCATTER_REMOVE_LEGEND = "Scatter remove legend";
    protected static final String SCATTER_REMOVE_LEGEND_SVG_BEFORE = "0\n200\n400\n600\n800\n1000\n1200\n1400\n800\n1000\n1200\n1400\n1600\n1800\n2000\nScatter remove legend\n103866\n110349\n119180\n125478";
    protected static final String SCATTER_REMOVE_LEGEND_SVG_AFTER = "0\n200\n400\n600\n800\n1000\n1200\n1400\n800\n1000\n1200\n1400\n1600\n1800\n2000\nScatter remove legend";
    protected static final String BRUSHED_SCATTER_W_CUSTOM_SCALES = "Scatter With Brushing and Custom Scales";
    protected static final String BRUSHED_SCATTER_W_CUSTOM_SCALES_SVG = "0\n100\n200\n300\n400\n500\n600\n0\n100\n200\n300\n400\n500\n600\n700\n800\n900\nScatter With Brushing and Custom Scales\n0\n1\n10\n11\n12\n13\n14\n15\n16\n17\n18\n19\n2\n3\n4\n5\n6\n7\n8\n9";
    protected static final String CIRCLE_COLOR = "#010101";

    @Test
    public void setAesTest()
    {
        Locator nextBtn = Locator.input("next-btn");
        Locator setAesBtn = Locator.input("set-aes-btn");
        goToChartingTestPage("setAesTest");

        waitForSvgWithTitle(BOX_PLOT_COLOR_SHAPE);
        click(setAesBtn);
        waitForSvgWithTitle("119180");
        waitForSvgWithTitle("Females");

        click(nextBtn);
        waitForSvgWithTitle(SCATTER_PLOT_SHAPE_COLOR_X);
        assertSVG(SCATTER_PLOT_SHAPE_COLOR_X_SVG_BEFORE);
        click(setAesBtn);
        waitForSvgWithTitle("103866");
        assertSVG(SCATTER_PLOT_SHAPE_COLOR_X_SVG_AFTER);

        click(nextBtn);
        waitForSvgWithTitle(SCATTER_HOVER_CLICK);
        click(Locator.css("svg g a path"));
        assertExt4MsgBox("Look a click handler!", "OK");
        click(setAesBtn);
        click(Locator.css("svg g a path"));
        assertExt4MsgBox("The click handler has changed!", "OK");

        click(nextBtn);
        waitForSvgWithTitle(LINE_ERROR_COLOR_Y);
        click(setAesBtn);
        waitForSvgWithTitle("Alan");
        assertSVG(LINE_ERROR_COLOR_Y_SVG);

        click(nextBtn);
        waitForSvgWithTitle(SCATTER_REMOVE_LEGEND);
        assertSVG(SCATTER_REMOVE_LEGEND_SVG_BEFORE);
        click(setAesBtn);
        assertSVG(SCATTER_REMOVE_LEGEND_SVG_AFTER);

        click(nextBtn);
        waitForSvgWithTitle(BRUSHED_SCATTER_W_CUSTOM_SCALES);
        assertSVG(BRUSHED_SCATTER_W_CUSTOM_SCALES_SVG);

        List<WebElement> points;
        // Test removal of mouseover/mouseout aesthetics (Issue 19455).
        click(setAesBtn);
        points = Locator.css("svg g a path").findElements(getDriver());
        fireEvent(points.get(0), SeleniumEvent.mouseover);
        assertEquals("Related point had an unexpected fill color.", CIRCLE_COLOR, points.get(1).getAttribute("fill"));
        assertEquals("Related point had an unexpected stroke color.", CIRCLE_COLOR, points.get(1).getAttribute("stroke"));
    }

    protected static final String ARROW_COLOR = "#FF33E5";
    protected static final String CIRCLE_PATH = "M0-2.6c-1.5,0-2.6,1.1-2.6,2.6S-1.4,2.6,0,2.6 c1.5,0,2.6-1.2,2.6-2.6C2.6-1.5,1.5-2.6,0-2.6z M0,1.9c-1.1,0-1.9-0.8-1.9-1.9S-1-1.9,0-1.9C1.1-1.9,1.9-1,1.9,0 C1.9,1.1,1.1,1.9,0,1.9z";
    protected static final String ARROW_PATH = "M3,0.6 L3,0.6 L0,-3.4 L-3,0.6 L-2,1.3 L-0.6,-0.5 L-0.6,3.4 L0.6,3.4 L0.6,-0.5 L2,1.3z";
    protected static final String MOUSEOVER_FILL = "#01BFC2";
    protected static final String MOUSEOVER_STROKE = "#00EAFF";
    protected static final String BRUSH_FILL = "#14C9CC";
    protected static final String BRUSH_STROKE = "#00393A";

    @Test
    public void mouseEventsTest()
    {
        Actions builder = new Actions(getDriver());
        List<WebElement> points;
        goToChartingTestPage("interactivityTest");
        waitForSvgWithTitle("Interactive Plot");

        /*
        The points on the scatter plot are split into two groups. Each 10 points wide and 20 points tall, for a
        total of 400 points. Index wise, they go in order from bottom left to top right. Point 0 is the very bottom left
        point on the left group. Point 19, is the bottom right point on the right group. Point 380 is the top left, point
        399 is top right.
         */

        points = Locator.css("svg g a path").findElements(getDriver());
        assertEquals("Bottom left point was an unexpected color.", CIRCLE_COLOR, points.get(0).getAttribute("fill"));
        assertEquals("Bottom left point was not a circle.", CIRCLE_PATH, points.get(0).getAttribute("d"));
        assertEquals("Top right point was an unexpected color.", ARROW_COLOR, points.get(399).getAttribute("fill"));
        assertEquals("Top right point was not an upward arrow.", ARROW_PATH, points.get(399).getAttribute("d"));

        // Test mouseover/mouseout aesthetics.
        builder.moveToElement(points.get(380)).perform();
        assertEquals("Related point had an unexpected fill color.", MOUSEOVER_FILL, points.get(381).getAttribute("fill"));
        assertEquals("Related point had an unexpected stroke color.", MOUSEOVER_STROKE, points.get(381).getAttribute("stroke"));

        builder.moveToElement(points.get(380)).moveByOffset(-20, -20).perform();
        assertEquals("Related point had an unexpected fill color.", ARROW_COLOR, points.get(381).getAttribute("fill"));
        assertEquals("Related point had an unexpected stroke color.", ARROW_COLOR, points.get(381).getAttribute("stroke"));
    }

    @Test
    public void basicBrushTest()
    {
        Actions builder = new Actions(getDriver());
        List<WebElement> points;
        goToChartingTestPage("interactivityTest");
        waitForSvgWithTitle("Interactive Plot");

        // Brush from the top left point of the left group, to the bottom right point of the left group.
        points = Locator.css("svg g a path").findElements(getDriver());
        builder.moveToElement(points.get(380)).moveByOffset(-10, -10).clickAndHold().moveByOffset(150, 190).release().perform();
        verifyBottomLeftGroupBrushed();
        verifyTopRightGroupNotBrushed();

        // Move the brushed area to the top right and verify
        builder.moveToElement(points.get(380)).moveByOffset(5, 5).clickAndHold().moveByOffset(480, -200).release().perform();
        verifyTopRightGroupBrushed();
        verifyBottomLeftGroupNotBrushed();

        // NOTE: have to use clickAndHold().release() here because Firefox does not like click().
        builder.moveToElement(points.get(380)).moveByOffset(-20, 0).clickAndHold().release().perform();
        verifyNoPointsBrushed();

        // Brush from the bottom left point of the right group, to the top right point of the right group.
        builder.moveToElement(points.get(10)).moveByOffset(-10, 10).clickAndHold().moveByOffset(150, -190).release().perform();
        verifyEdgePointsBrushed();
        verifyTopRightGroupBrushed();
        verifyBottomLeftGroupNotBrushed();
    }

    @Test @Ignore
    public void advancedBrushTest()
    {
        // Only run this in Chrome. There is a bug in web driver for Firefox that prevents it from properly releasing
        // the mouse. You can view the (very old) issue here:
        // https://code.google.com/p/selenium/issues/detail?id=3356

        Actions builder = new Actions(getDriver());
        goToChartingTestPage("interactivityTest");
        waitForSvgWithTitle("Interactive Plot");

        WebElement xRightHandle = Locator.css(".x-axis-handle .resize.e").findElement(getDriver());
        WebElement yTopHandle = Locator.css(".y-axis-handle .resize.n").findElement(getDriver());
        WebElement xExtent = Locator.css(".x-axis-handle .extent").findElement(getDriver());
        WebElement yExtent = Locator.css(".y-axis-handle .extent").findElement(getDriver());
        List<WebElement> points = Locator.css("svg g a path").findElements(getDriver());

        // Brush from the bottom left point of the right group, to the top right point of the right group.
        builder.moveToElement(points.get(10)).moveByOffset(-10, 10).clickAndHold().moveByOffset(150, -190).release().perform();

        // Move the brushed area to the bottom left via brush handles and re-verify selected points.
        builder.moveToElement(xExtent).clickAndHold().moveByOffset(-420, 0).release().perform();
        builder.moveToElement(yExtent).clickAndHold().moveByOffset(0, 190).release().perform();
        verifyBottomLeftGroupBrushed();
        verifyTopRightGroupNotBrushed();

        // Stretch handles to select all points.
        builder.moveToElement(xRightHandle).clickAndHold().moveByOffset(420, 0).release().perform();
        builder.moveToElement(yTopHandle).clickAndHold().moveByOffset(0,-190).release().perform();
        verifyAllPointsBrushed();

        // Clear the brushed area
        builder.moveToElement(Locator.css(".brush .resize.w").findElement(getDriver())).moveByOffset(-5, 0).click().perform();
        verifyNoPointsBrushed();

        // 1D selection on x axis (select bottom left)
        builder.moveToElement(Locator.css(".x-axis-handle .background").findElement(getDriver())).moveByOffset(-280, 0).clickAndHold().moveByOffset(180, 0).release().perform();
        verifyBottomLeftGroupBrushed();
        verifyTopRightGroupNotBrushed();
        assertEquals("Brushed area was not the expected height", "385", Locator.css(".brush .extent").findElement(getDriver()).getAttribute("height"));
        // Make sure when making a 1D selection that the opposite axis handle isn't visible.
        assertElementNotVisible(Locator.css(".y-axis-handle .resize.n"));
        assertElementNotVisible(Locator.css(".y-axis-handle .resize.s"));

        // 1D selection on y axis (select top right)
        builder.moveToElement(Locator.css(".y-axis-handle .background").findElement(getDriver())).moveByOffset(0, -190).clickAndHold().moveByOffset(0, 190).release().perform();
        verifyTopRightGroupBrushed();
        verifyBottomLeftGroupNotBrushed();
        assertEquals("Brushed area was not the expected width", "619.9999999999999", Locator.css(".brush .extent").findElement(getDriver()).getAttribute("width"));
        // Make sure when making a 1D selection that the opposite axis handle isn't visible.
        assertElementNotVisible(Locator.css(".x-axis-handle .resize.e"));
        assertElementNotVisible(Locator.css(".x-axis-handle .resize.w"));

        // Move 1D selection, make sure the opposite axis handle doesn't show up
        builder.moveToElement(Locator.css(".brush .extent").findElement(getDriver())).clickAndHold().moveByOffset(0, 50).release().perform();
        assertElementNotVisible(Locator.css(".x-axis-handle .resize.e"));
        assertElementNotVisible(Locator.css(".x-axis-handle .resize.w"));

        // Resize 1D selection via main brush, make sure opposite axis handle doesn't show up.
        builder.moveToElement(Locator.css(".brush .resize.s").findElement(getDriver())).clickAndHold().moveByOffset(0, 50).release().perform();
        assertElementNotVisible(Locator.css(".x-axis-handle .resize.e"));
        assertElementNotVisible(Locator.css(".x-axis-handle .resize.w"));

        // Resize 1D selection via main brush, make sure new axis handle does show up (because we're shrinking the x-axis part).
        builder.moveToElement(Locator.css(".brush .resize.w").findElement(getDriver())).clickAndHold().moveByOffset(50, 0).release().perform();
        assertElementVisible(Locator.css(".x-axis-handle .resize.e"));
        assertElementVisible(Locator.css(".x-axis-handle .resize.w"));

        // Verify that clicking in the margins clears the brush.
        builder.moveToElement(Locator.css(".x-axis-handle .resize.w").findElement(getDriver())).moveByOffset(-5, 0).click().perform();
        verifyNoPointsBrushed();
    }

    private void verifyAllPointsBrushed()
    {
        verifyBottomLeftGroupBrushed();
        verifyTopRightGroupBrushed();
    }

    private void verifyNoPointsBrushed()
    {
        verifyBottomLeftGroupNotBrushed();
        verifyTopRightGroupNotBrushed();
    }

    private void verifyBottomLeftGroupBrushed()
    {
        // Check the first, middle, and end point for each "row" in the selected area.
        List<WebElement> points = Locator.css("svg g a path").findElements(getDriver());
        for (int i = 0; i < 20; i++)
        {
            int baseIndex = i * 20;
            verifyBrushedPoint(points.get(baseIndex));
            verifyBrushedPoint(points.get(baseIndex + 4));
            verifyBrushedPoint(points.get(baseIndex + 9));
        }
    }

    private void verifyBottomLeftGroupNotBrushed()
    {
        // Check the first, middle, and end point for each "row" in the selected area.
        List<WebElement> points = Locator.css("svg g a path").findElements(getDriver());
        for (int i = 0; i < 20; i++)
        {
            int baseIndex = i * 20;
            verifyNonBrushedPoint(points.get(baseIndex));
            verifyNonBrushedPoint(points.get(baseIndex + 4));
            verifyNonBrushedPoint(points.get(baseIndex + 9));
        }
    }

    private void verifyTopRightGroupBrushed()
    {
        // Check the first, middle, and end point for each "row" in the set of points in the top right of the plot.
        List<WebElement> points = Locator.css("svg g a path").findElements(getDriver());
        for (int i = 0; i < 20; i++)
        {
            int baseIndex = (i * 20) + 10;
            verifyBrushedPoint(points.get(baseIndex));
            verifyBrushedPoint(points.get(baseIndex + 4));
            verifyBrushedPoint(points.get(baseIndex + 9));
        }
    }

    private void verifyTopRightGroupNotBrushed()
    {
        // Check the first, middle, and end point for each "row" in the set of points in the top right of the plot.
        List<WebElement> points = Locator.css("svg g a path").findElements(getDriver());
        for (int i = 0; i < 20; i++)
        {
            int baseIndex = (i * 20) + 10;
            verifyNonBrushedPoint(points.get(baseIndex));
            verifyNonBrushedPoint(points.get(baseIndex + 4));
            verifyNonBrushedPoint(points.get(baseIndex + 9));
        }
    }

    private void verifyEdgePointsBrushed()
    {
        // Issue 19445: Can't select points on outer edge of plot with chart brushing area
        // Check the points on the far right and top edges.
        List<WebElement> points = Locator.css("svg g a path").findElements(getDriver());
        // Right edge.
        for (int i = 0; i < 19; i++)
        {
            int index = (i * 20) + 19;
            verifyBrushedPoint(points.get(index));
        }
        // Top edge.
        for (int index = 390; index < 400; index++)
        {
            verifyBrushedPoint(points.get(index));
        }
    }

    private void verifyBrushedPoint(WebElement el)
    {
        assertEquals("Brushed point had an unexpected fill color.", BRUSH_FILL, el.getAttribute("fill"));
        assertEquals("Brushed point had an unexpected stroke color.", BRUSH_STROKE, el.getAttribute("stroke"));
    }

    private void verifyNonBrushedPoint(WebElement el)
    {
        assertNotEquals("Non-brushed point had fill color of a brushed point.", BRUSH_FILL, el.getAttribute("fill"));
        assertNotEquals("Non-brushed point had stroke color of a brushed point.", BRUSH_STROKE, el.getAttribute("stroke"));
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

    @LogMethod(quiet = true)
    private void checkExportedChart(@LoggedParam String title, @Nullable String svgText, boolean hasError, int svgCount, int svgIndex)
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
            waitForSvgWithTitle(title, false);
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
