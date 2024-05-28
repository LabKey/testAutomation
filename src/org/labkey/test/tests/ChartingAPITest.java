/*
 * Copyright (c) 2013-2019 LabKey Corporation
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

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.Daily;
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

@Category({Daily.class, Charting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 8)
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

    public void initProject() throws Exception
    {
        _containerHelper.createProject(getProjectName(), null);

        ClientAPITest.createPeopleList(ClientAPITest.LIST_NAME, createDefaultConnection(), getProjectName());
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

    @Test
    public void chartAPITest() throws Exception
    {
        goToChartingTestPage("chartTest");
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

        try (CloseableHttpClient httpClient = WebTestHelper.getHttpClient())
        {
            method = new HttpPost(url);
            APITestHelper.injectCookies(method);
            List<NameValuePair> args = new ArrayList<>();
            args.add(new BasicNameValuePair("svg", svgText));
            method.setEntity(new UrlEncodedFormEntity(args));
            try (CloseableHttpResponse response = httpClient.execute(method, context))
            {
                int status = response.getCode();
                assertEquals("SVG Downloaded", HttpStatus.SC_OK, status);
                assertTrue(response.getHeaders("Content-Disposition")[0].getValue().startsWith("attachment;"));
                assertTrue(response.getHeaders("Content-Type")[0].getValue().startsWith("application/pdf"));

                EntityUtils.consumeQuietly(response.getEntity()); // Prevent server-side TranscoderException
            }
        }
    }

    protected static final String SCATTER_ONE = "Scatter Plot One";
    protected static final String SCATTER_TWO = "Scatter Plot Two (Custom)";
    protected static final String BOX_ONE = "Box Plot One";
    protected static final String BOX_TWO = "Box Plot Two (Custom)";
    protected static final String BOX_THREE = "Box Plot Three (Custom, Broken)";

    protected static final String SCATTER_ONE_TEXT = "0200400600800100012001400800100012001400160018002000Scatter Plot OneCD4+ (cells/mm3)Lymphs (cells/mm3)Lymphs (cells/mm3): 1222, CD4+ (cells/mm3): -1Lymphs (cells/mm3): 1235, CD4+ (cells/mm3): 1224Lymphs (cells/mm3): 1123, CD4+ (cells/mm3): 772Lymphs (cells/mm3): 1271, CD4+ (cells/mm3): 390Lymphs (cells/mm3): 1169, CD4+ (cells/mm3): 317Lymphs (cells/mm3): 1039, CD4+ (cells/mm3): 271Lymphs (cells/mm3): 1234, CD4+ (cells/mm3): 327Lymphs (cells/mm3): 1080, CD4+ (cells/mm3): 271Lymphs (cells/mm3): 1081, CD4+ (cells/mm3): 567Lymphs (cells/mm3): 882, CD4+ (cells/mm3): 1020Lymphs (cells/mm3): 736, CD4+ (cells/mm3): 1022Lymphs (cells/mm3): 1095, CD4+ (cells/mm3): 1008Lymphs (cells/mm3): 1039, CD4+ (cells/mm3): 1024Lymphs (cells/mm3): 1173, CD4+ (cells/mm3): 801Lymphs (cells/mm3): 1236, CD4+ (cells/mm3): 1005Lymphs (cells/mm3): 1341, CD4+ (cells/mm3): 1192Lymphs (cells/mm3): 1396, CD4+ (cells/mm3): 937Lymphs (cells/mm3): 1431, CD4+ (cells/mm3): 1034Lymphs (cells/mm3): 1372, CD4+ (cells/mm3): 1005Lymphs (cells/mm3): 1481, CD4+ (cells/mm3): 688Lymphs (cells/mm3): 1672, CD4+ (cells/mm3): 764Lymphs (cells/mm3): 1851, CD4+ (cells/mm3): 405Lymphs (cells/mm3): 1722, CD4+ (cells/mm3): 223Lymphs (cells/mm3): 1879, CD4+ (cells/mm3): 843Lymphs (cells/mm3): 2061, CD4+ (cells/mm3): 462Lymphs (cells/mm3): 1974, CD4+ (cells/mm3): 701Lymphs (cells/mm3): 978, CD4+ (cells/mm3): 1390Lymphs (cells/mm3): 974, CD4+ (cells/mm3): 1457Lymphs (cells/mm3): 1160, CD4+ (cells/mm3): 821Lymphs (cells/mm3): 1310, CD4+ (cells/mm3): 517Lymphs (cells/mm3): 1392, CD4+ (cells/mm3): 295Lymphs (cells/mm3): 1523, CD4+ (cells/mm3): 1076Lymphs (cells/mm3): 1464, CD4+ (cells/mm3): 1342Lymphs (cells/mm3): 1615, CD4+ (cells/mm3): 877Lymphs (cells/mm3): 1579, CD4+ (cells/mm3): 475Lymphs (cells/mm3): 1660, CD4+ (cells/mm3): 736Lymphs (cells/mm3): 1768, CD4+ (cells/mm3): 434Lymphs (cells/mm3): 1954, CD4+ (cells/mm3): 272Lymphs (cells/mm3): 1803, CD4+ (cells/mm3): 391Lymphs (cells/mm3): 1848, CD4+ (cells/mm3): 725Lymphs (cells/mm3): 1798, CD4+ (cells/mm3): 459Lymphs (cells/mm3): 1743, CD4+ (cells/mm3): 1182Lymphs (cells/mm3): 1615, CD4+ (cells/mm3): 1493Lymphs (cells/mm3): 1626, CD4+ (cells/mm3): 764Lymphs (cells/mm3): 1747, CD4+ (cells/mm3): 609Lymphs (cells/mm3): 1635, CD4+ (cells/mm3): 625Lymphs (cells/mm3): 1806, CD4+ (cells/mm3): 400Lymphs (cells/mm3): 1952, CD4+ (cells/mm3): 950Lymphs (cells/mm3): 2105, CD4+ (cells/mm3): 916Lymphs (cells/mm3): 1927, CD4+ (cells/mm3): 542Lymphs (cells/mm3): 2049, CD4+ (cells/mm3): 1090Lymphs (cells/mm3): 2108, CD4+ (cells/mm3): 1283Lymphs (cells/mm3): 1802, CD4+ (cells/mm3): 239Lymphs (cells/mm3): 1787, CD4+ (cells/mm3): 266Lymphs (cells/mm3): 1843, CD4+ (cells/mm3): 224Lymphs (cells/mm3): 1780, CD4+ (cells/mm3): 163Lymphs (cells/mm3): 1859, CD4+ (cells/mm3): 432Lymphs (cells/mm3): 2017, CD4+ (cells/mm3): 954Lymphs (cells/mm3): 2185, CD4+ (cells/mm3): 1044Lymphs (cells/mm3): 2054, CD4+ (cells/mm3): 1094Lymphs (cells/mm3): 2085, CD4+ (cells/mm3): 1028Lymphs (cells/mm3): 2099, CD4+ (cells/mm3): 962Lymphs (cells/mm3): 2121, CD4+ (cells/mm3): 652Lymphs (cells/mm3): 1975, CD4+ (cells/mm3): 386Lymphs (cells/mm3): 1870, CD4+ (cells/mm3): 653Lymphs (cells/mm3): 1953, CD4+ (cells/mm3): 530Lymphs (cells/mm3): 1777, CD4+ (cells/mm3): 366Lymphs (cells/mm3): 1830, CD4+ (cells/mm3): 426Lymphs (cells/mm3): 1668, CD4+ (cells/mm3): 360Lymphs (cells/mm3): 1620, CD4+ (cells/mm3): 467Lymphs (cells/mm3): 1752, CD4+ (cells/mm3): 525Lymphs (cells/mm3): 1551, CD4+ (cells/mm3): 351Lymphs (cells/mm3): 1406, CD4+ (cells/mm3): 379Lymphs (cells/mm3): 1397, CD4+ (cells/mm3): 594Lymphs (cells/mm3): 1587, CD4+ (cells/mm3): 513Lymphs (cells/mm3): 1759, CD4+ (cells/mm3): 546Lymphs (cells/mm3): 1686, CD4+ (cells/mm3): 537Lymphs (cells/mm3): 1609, CD4+ (cells/mm3): 279Lymphs (cells/mm3): 1707, CD4+ (cells/mm3): 226Lymphs (cells/mm3): 1141, CD4+ (cells/mm3): 1190Lymphs (cells/mm3): 999, CD4+ (cells/mm3): 757Lymphs (cells/mm3): 937, CD4+ (cells/mm3): 428Lymphs (cells/mm3): 825, CD4+ (cells/mm3): 291Lymphs (cells/mm3): 882, CD4+ (cells/mm3): 154Lymphs (cells/mm3): 949, CD4+ (cells/mm3): 157Lymphs (cells/mm3): 789, CD4+ (cells/mm3): 121Lymphs (cells/mm3): 692, CD4+ (cells/mm3): 108Lymphs (cells/mm3): 791, CD4+ (cells/mm3): 108Lymphs (cells/mm3): 966, CD4+ (cells/mm3): 148Lymphs (cells/mm3): 922, CD4+ (cells/mm3): 101Lymphs (cells/mm3): 965, CD4+ (cells/mm3): 523Lymphs (cells/mm3): 877, CD4+ (cells/mm3): 690Lymphs (cells/mm3): 770, CD4+ (cells/mm3): 681Lymphs (cells/mm3): 784, CD4+ (cells/mm3): 727Lymphs (cells/mm3): 896, CD4+ (cells/mm3): 741Lymphs (cells/mm3): 780, CD4+ (cells/mm3): 451Lymphs (cells/mm3): 970, CD4+ (cells/mm3): 524Lymphs (cells/mm3): 802, CD4+ (cells/mm3): 749Lymphs (cells/mm3): 870, CD4+ (cells/mm3): 539Lymphs (cells/mm3): 821, CD4+ (cells/mm3): 340";
    protected static final String SCATTER_TWO_TEXT = "0200400600800100012001400800100012001400160018002000Scatter Plot Two (Custom)CD4LymphsLymphs (cells/mm3): 1222, CD4+ (cells/mm3): -1, Gender: MalesLymphs (cells/mm3): 1235, CD4+ (cells/mm3): 1224, Gender: MalesLymphs (cells/mm3): 1123, CD4+ (cells/mm3): 772, Gender: MalesLymphs (cells/mm3): 1271, CD4+ (cells/mm3): 390, Gender: MalesLymphs (cells/mm3): 1169, CD4+ (cells/mm3): 317, Gender: MalesLymphs (cells/mm3): 1039, CD4+ (cells/mm3): 271, Gender: MalesLymphs (cells/mm3): 1234, CD4+ (cells/mm3): 327, Gender: MalesLymphs (cells/mm3): 1080, CD4+ (cells/mm3): 271, Gender: MalesLymphs (cells/mm3): 1081, CD4+ (cells/mm3): 567, Gender: MalesLymphs (cells/mm3): 882, CD4+ (cells/mm3): 1020, Gender: MalesLymphs (cells/mm3): 736, CD4+ (cells/mm3): 1022, Gender: MalesLymphs (cells/mm3): 1095, CD4+ (cells/mm3): 1008, Gender: MalesLymphs (cells/mm3): 1039, CD4+ (cells/mm3): 1024, Gender: MalesLymphs (cells/mm3): 1173, CD4+ (cells/mm3): 801, Gender: MalesLymphs (cells/mm3): 1236, CD4+ (cells/mm3): 1005, Gender: MalesLymphs (cells/mm3): 1341, CD4+ (cells/mm3): 1192, Gender: MalesLymphs (cells/mm3): 1396, CD4+ (cells/mm3): 937, Gender: MalesLymphs (cells/mm3): 1431, CD4+ (cells/mm3): 1034, Gender: MalesLymphs (cells/mm3): 1372, CD4+ (cells/mm3): 1005, Gender: MalesLymphs (cells/mm3): 1481, CD4+ (cells/mm3): 688, Gender: MalesLymphs (cells/mm3): 1672, CD4+ (cells/mm3): 764, Gender: MalesLymphs (cells/mm3): 1851, CD4+ (cells/mm3): 405, Gender: MalesLymphs (cells/mm3): 1722, CD4+ (cells/mm3): 223, Gender: MalesLymphs (cells/mm3): 1879, CD4+ (cells/mm3): 843, Gender: MalesLymphs (cells/mm3): 2061, CD4+ (cells/mm3): 462, Gender: MalesLymphs (cells/mm3): 1974, CD4+ (cells/mm3): 701, Gender: MalesLymphs (cells/mm3): 978, CD4+ (cells/mm3): 1390, Gender: MalesLymphs (cells/mm3): 974, CD4+ (cells/mm3): 1457, Gender: MalesLymphs (cells/mm3): 1160, CD4+ (cells/mm3): 821, Gender: MalesLymphs (cells/mm3): 1310, CD4+ (cells/mm3): 517, Gender: MalesLymphs (cells/mm3): 1392, CD4+ (cells/mm3): 295, Gender: MalesLymphs (cells/mm3): 1523, CD4+ (cells/mm3): 1076, Gender: MalesLymphs (cells/mm3): 1464, CD4+ (cells/mm3): 1342, Gender: MalesLymphs (cells/mm3): 1615, CD4+ (cells/mm3): 877, Gender: MalesLymphs (cells/mm3): 1579, CD4+ (cells/mm3): 475, Gender: MalesLymphs (cells/mm3): 1660, CD4+ (cells/mm3): 736, Gender: MalesLymphs (cells/mm3): 1768, CD4+ (cells/mm3): 434, Gender: MalesLymphs (cells/mm3): 1954, CD4+ (cells/mm3): 272, Gender: MalesLymphs (cells/mm3): 1803, CD4+ (cells/mm3): 391, Gender: MalesLymphs (cells/mm3): 1848, CD4+ (cells/mm3): 725, Gender: MalesLymphs (cells/mm3): 1798, CD4+ (cells/mm3): 459, Gender: MalesLymphs (cells/mm3): 1743, CD4+ (cells/mm3): 1182, Gender: MalesLymphs (cells/mm3): 1615, CD4+ (cells/mm3): 1493, Gender: MalesLymphs (cells/mm3): 1626, CD4+ (cells/mm3): 764, Gender: MalesLymphs (cells/mm3): 1747, CD4+ (cells/mm3): 609, Gender: MalesLymphs (cells/mm3): 1635, CD4+ (cells/mm3): 625, Gender: MalesLymphs (cells/mm3): 1806, CD4+ (cells/mm3): 400, Gender: MalesLymphs (cells/mm3): 1952, CD4+ (cells/mm3): 950, Gender: MalesLymphs (cells/mm3): 2105, CD4+ (cells/mm3): 916, Gender: MalesLymphs (cells/mm3): 1927, CD4+ (cells/mm3): 542, Gender: MalesLymphs (cells/mm3): 2049, CD4+ (cells/mm3): 1090, Gender: MalesLymphs (cells/mm3): 2108, CD4+ (cells/mm3): 1283, Gender: MalesLymphs (cells/mm3): 1802, CD4+ (cells/mm3): 239, Gender: FemalesLymphs (cells/mm3): 1787, CD4+ (cells/mm3): 266, Gender: FemalesLymphs (cells/mm3): 1843, CD4+ (cells/mm3): 224, Gender: FemalesLymphs (cells/mm3): 1780, CD4+ (cells/mm3): 163, Gender: FemalesLymphs (cells/mm3): 1859, CD4+ (cells/mm3): 432, Gender: FemalesLymphs (cells/mm3): 2017, CD4+ (cells/mm3): 954, Gender: FemalesLymphs (cells/mm3): 2185, CD4+ (cells/mm3): 1044, Gender: FemalesLymphs (cells/mm3): 2054, CD4+ (cells/mm3): 1094, Gender: FemalesLymphs (cells/mm3): 2085, CD4+ (cells/mm3): 1028, Gender: FemalesLymphs (cells/mm3): 2099, CD4+ (cells/mm3): 962, Gender: FemalesLymphs (cells/mm3): 2121, CD4+ (cells/mm3): 652, Gender: FemalesLymphs (cells/mm3): 1975, CD4+ (cells/mm3): 386, Gender: FemalesLymphs (cells/mm3): 1870, CD4+ (cells/mm3): 653, Gender: FemalesLymphs (cells/mm3): 1953, CD4+ (cells/mm3): 530, Gender: FemalesLymphs (cells/mm3): 1777, CD4+ (cells/mm3): 366, Gender: FemalesLymphs (cells/mm3): 1830, CD4+ (cells/mm3): 426, Gender: FemalesLymphs (cells/mm3): 1668, CD4+ (cells/mm3): 360, Gender: FemalesLymphs (cells/mm3): 1620, CD4+ (cells/mm3): 467, Gender: FemalesLymphs (cells/mm3): 1752, CD4+ (cells/mm3): 525, Gender: FemalesLymphs (cells/mm3): 1551, CD4+ (cells/mm3): 351, Gender: FemalesLymphs (cells/mm3): 1406, CD4+ (cells/mm3): 379, Gender: FemalesLymphs (cells/mm3): 1397, CD4+ (cells/mm3): 594, Gender: FemalesLymphs (cells/mm3): 1587, CD4+ (cells/mm3): 513, Gender: FemalesLymphs (cells/mm3): 1759, CD4+ (cells/mm3): 546, Gender: FemalesLymphs (cells/mm3): 1686, CD4+ (cells/mm3): 537, Gender: FemalesLymphs (cells/mm3): 1609, CD4+ (cells/mm3): 279, Gender: FemalesLymphs (cells/mm3): 1707, CD4+ (cells/mm3): 226, Gender: FemalesLymphs (cells/mm3): 1141, CD4+ (cells/mm3): 1190, Gender: MalesLymphs (cells/mm3): 999, CD4+ (cells/mm3): 757, Gender: MalesLymphs (cells/mm3): 937, CD4+ (cells/mm3): 428, Gender: MalesLymphs (cells/mm3): 825, CD4+ (cells/mm3): 291, Gender: MalesLymphs (cells/mm3): 882, CD4+ (cells/mm3): 154, Gender: MalesLymphs (cells/mm3): 949, CD4+ (cells/mm3): 157, Gender: MalesLymphs (cells/mm3): 789, CD4+ (cells/mm3): 121, Gender: MalesLymphs (cells/mm3): 692, CD4+ (cells/mm3): 108, Gender: MalesLymphs (cells/mm3): 791, CD4+ (cells/mm3): 108, Gender: MalesLymphs (cells/mm3): 966, CD4+ (cells/mm3): 148, Gender: MalesLymphs (cells/mm3): 922, CD4+ (cells/mm3): 101, Gender: MalesLymphs (cells/mm3): 965, CD4+ (cells/mm3): 523, Gender: MalesLymphs (cells/mm3): 877, CD4+ (cells/mm3): 690, Gender: MalesLymphs (cells/mm3): 770, CD4+ (cells/mm3): 681, Gender: MalesLymphs (cells/mm3): 784, CD4+ (cells/mm3): 727, Gender: MalesLymphs (cells/mm3): 896, CD4+ (cells/mm3): 741, Gender: MalesLymphs (cells/mm3): 780, CD4+ (cells/mm3): 451, Gender: MalesLymphs (cells/mm3): 970, CD4+ (cells/mm3): 524, Gender: MalesLymphs (cells/mm3): 802, CD4+ (cells/mm3): 749, Gender: MalesLymphs (cells/mm3): 870, CD4+ (cells/mm3): 539, Gender: MalesLymphs (cells/mm3): 821, CD4+ (cells/mm3): 340, Gender: MalesFemalesMales";
    protected static final String BOX_ONE_TEXT = "FemalesMales6008001000120014001600180020002200Box Plot OneGenderLymphs (cells/mm3)Males: Min: 692 Max: 2108 Q1: 965 Q2: 1235 Q3: 1672Females: Min: 1397 Max: 2185 Q1: 1677 Q2: 1787 Q3: 1964";
    protected static final String BOX_TWO_TEXT = "FemalesMales02004006008001000120014001600Box Plot Two (Custom)GenderCD4Males: Min: -1 Max: 1493 Q1: 391 Q2: 688 Q3: 950Females: Min: 163 Max: 1094 Q1: 355.5 Q2: 467 Q3: 623CD4+ (cells/mm3): 1044, Gender: Females, undefined: undefinedCD4+ (cells/mm3): 1094, Gender: Females, undefined: undefinedCD4+ (cells/mm3): 1028, Gender: Females, undefined: undefinedFemales";

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
        assertErrorMessage("The measure, ParticipantId/Cohort, was not found. It may have been renamed or removed.", true);
        checkExportedChart(BOX_THREE, null, true, 0);
    }

    protected static final String TIME_CHART_1 = "Luminex";
    protected static final String TIME_CHART_1_TEXT_1 = "051015202530809010020030050100150200250300350Luminex249320127Weeks Since Demographic Start DateFluorescence IntensityObserved Concentration 249320127, Weeks: 0, ObsConc TNF-alpha: 4.56 249320127, Weeks: 7, ObsConc TNF-alpha: 8.33 249320127, Weeks: 22, ObsConc TNF-alpha: 3.1 249320127, Weeks: 27, ObsConc TNF-alpha: 3.06 249320127, Weeks: 32, ObsConc TNF-alpha: 3.23 249320127, Weeks: 0, ObsConc IL-2: 16.69 249320127, Weeks: 7, ObsConc IL-2: 20.93 249320127, Weeks: 22, ObsConc IL-2: 6.07 249320127, Weeks: 27, ObsConc IL-2: 6.59 249320127, Weeks: 32, ObsConc IL-2: 6.38 249320127, Weeks: 0, ObsConc IL-10: 26.55 249320127, Weeks: 7, ObsConc IL-10: 28.28 249320127, Weeks: 22, ObsConc IL-10: 8.44 249320127, Weeks: 27, ObsConc IL-10: 13.61 249320127, Weeks: 32, ObsConc IL-10: 10.73 249320127, Weeks: 0, Fi TNF-alpha: 226.5 249320127, Weeks: 7, Fi TNF-alpha: 381 249320127, Weeks: 22, Fi TNF-alpha: 166.3 249320127, Weeks: 27, Fi TNF-alpha: 164.5 249320127, Weeks: 32, Fi TNF-alpha: 171.5 249320127, Weeks: 0, Fi IL-2: 181.8 249320127, Weeks: 7, Fi IL-2: 225.3 249320127, Weeks: 22, Fi IL-2: 75.5 249320127, Weeks: 27, Fi IL-2: 80.5 249320127, Weeks: 32, Fi IL-2: 78.5 249320127, Weeks: 0, Fi IL-10: 195.3 249320127, Weeks: 7, Fi IL-10: 206.3 249320127, Weeks: 22, Fi IL-10: 81.3 249320127, Weeks: 27, Fi IL-10: 113.5 249320127, Weeks: 32, Fi IL-10: 95.5249320127 Fi IL-10249320127 Fi IL-2249320127 Fi TNF-alpha249320127 ObsConc IL-10249320127 ObsConc IL-2249320127 ObsConcTNF-alpha";
    protected static final String TIME_CHART_1_TEXT_2 = "05101520253070700700050100150200250300350Luminex249320107Weeks Since Demographic Start DateFluorescence IntensityObserved Concentration 249320107, Weeks: 0, ObsConc TNF-alpha: 263.92 249320107, Weeks: 6, ObsConc TNF-alpha: 6.5 249320107, Weeks: 8, ObsConc TNF-alpha: 2.83 249320107, Weeks: 15, ObsConc TNF-alpha: 3.02 249320107, Weeks: 30, ObsConc TNF-alpha: 3.57 249320107, Weeks: 0, ObsConc IL-2: 397.78 249320107, Weeks: 6, ObsConc IL-2: 14.73 249320107, Weeks: 8, ObsConc IL-2: 5.11 249320107, Weeks: 15, ObsConc IL-2: 6.1 249320107, Weeks: 30, ObsConc IL-2: 5.32 249320107, Weeks: 0, ObsConc IL-10: 266.16 249320107, Weeks: 6, ObsConc IL-10: 19.84 249320107, Weeks: 8, ObsConc IL-10: 8.32 249320107, Weeks: 15, ObsConc IL-10: 13.25 249320107, Weeks: 30, ObsConc IL-10: 12.85 249320107, Weeks: 0, Fi TNF-alpha: 7989.3 249320107, Weeks: 6, Fi TNF-alpha: 306.3 249320107, Weeks: 8, Fi TNF-alpha: 155 249320107, Weeks: 15, Fi TNF-alpha: 163 249320107, Weeks: 30, Fi TNF-alpha: 185.8 249320107, Weeks: 0, Fi IL-2: 4023 249320107, Weeks: 6, Fi IL-2: 161.8 249320107, Weeks: 8, Fi IL-2: 66.3 249320107, Weeks: 15, Fi IL-2: 75.8 249320107, Weeks: 30, Fi IL-2: 68.3 249320107, Weeks: 0, Fi IL-10: 1686.5 249320107, Weeks: 6, Fi IL-10: 152.8 249320107, Weeks: 8, Fi IL-10: 80.5 249320107, Weeks: 15, Fi IL-10: 111.3 249320107, Weeks: 30, Fi IL-10: 108.8249320107 Fi IL-10249320107 Fi IL-2249320107 Fi TNF-alpha249320107 ObsConc IL-10249320107 ObsConc IL-2249320107 ObsConcTNF-alpha";
    protected static final String TIME_CHART_1_TEXT_3 = "1015202530357070050100150200250300350Luminex249318596Weeks Since Demographic Start DateFluorescence IntensityObserved Concentration 249318596, Weeks: 6, ObsConc TNF-alpha: 35.87 249318596, Weeks: 11, ObsConc TNF-alpha: 13.68 249318596, Weeks: 15, ObsConc TNF-alpha: 2.82 249318596, Weeks: 27, ObsConc TNF-alpha: 5.12 249318596, Weeks: 35, ObsConc TNF-alpha: 3.09 249318596, Weeks: 6, ObsConc IL-2: 52.74 249318596, Weeks: 11, ObsConc IL-2: 28.35 249318596, Weeks: 15, ObsConc IL-2: 5.19 249318596, Weeks: 27, ObsConc IL-2: 6.69 249318596, Weeks: 35, ObsConc IL-2: 5.76 249318596, Weeks: 6, ObsConc IL-10: 40.07 249318596, Weeks: 11, ObsConc IL-10: 42.38 249318596, Weeks: 15, ObsConc IL-10: 7.99 249318596, Weeks: 27, ObsConc IL-10: 32.33 249318596, Weeks: 35, ObsConc IL-10: 12.49 249318596, Weeks: 6, Fi TNF-alpha: 1454 249318596, Weeks: 11, Fi TNF-alpha: 596.5 249318596, Weeks: 15, Fi TNF-alpha: 154.5 249318596, Weeks: 27, Fi TNF-alpha: 249.8 249318596, Weeks: 35, Fi TNF-alpha: 166 249318596, Weeks: 6, Fi IL-2: 560.5 249318596, Weeks: 11, Fi IL-2: 302.5 249318596, Weeks: 15, Fi IL-2: 67 249318596, Weeks: 27, Fi IL-2: 81.5 249318596, Weeks: 35, Fi IL-2: 72.5 249318596, Weeks: 6, Fi IL-10: 281.3 249318596, Weeks: 11, Fi IL-10: 296 249318596, Weeks: 15, Fi IL-10: 78.5 249318596, Weeks: 27, Fi IL-10: 232 249318596, Weeks: 35, Fi IL-10: 106.5249318596 Fi IL-10249318596 Fi IL-2249318596 Fi TNF-alpha249318596 ObsConc IL-10249318596 ObsConc IL-2249318596 ObsConcTNF-alpha";
    protected static final String TIME_CHART_2 = "Luminex Two";
    protected static final String TIME_CHART_2_TEXT_1 = "111122223333444455550.00.20.40.60.81.01.21.41.61.82.0Luminex TwoVisitsFI 249318596, 1, IL-10: 1 249318596, 2, IL-10: 1 249318596, 3, IL-10: 1 249318596, 4, IL-10: 1 249318596, 5, IL-10: 1 249320127, 1, IL-10: 1 249320127, 2, IL-10: 1 249320127, 3, IL-10: 1 249320127, 4, IL-10: 1 249320127, 5, IL-10: 1249318596249320127";
    protected static final String TIME_CHART_3 = "Male";
    protected static final String TIME_CHART_3_TEXT_1 = "11112222333344445555050100150200250300FemaleVisitObsConcFemale,1,ObsConc:162.61,SD:167.97381938861778Female,2,ObsConc:16.435,SD:8.238996904963614Female,3,ObsConc:5.645,SD:2.4427750612776453Female,4,ObsConc:7.605,SD:4.751853322652117Female,5,ObsConc:7.013333333333333,SD:3.932402149662046Female";
    protected static final String TIME_CHART_3_TEXT_2 = "1111222233334444555505101520253035404550MaleVisitObsConcMale,1,ObsConc:42.89333333333334,SD:8.782233960293533Male,2,ObsConc:28.136666666666667,SD:14.351189265469733Male,3,ObsConc:5.333333333333333,SD:2.587978619180101Male,4,ObsConc:14.713333333333331,SD:15.276663030038119Male,5,ObsConc:7.113333333333333,SD:4.843927469867127Male";
    protected static final String TIME_CHART_4 = "Luminex Four";
    protected static final String TIME_CHART_4_TEXT_1 = "200400600800100012001400Luminex FourDays Since Start DateFi249318596";
    protected static final String TIME_CHART_5 = "Luminex Five";
    protected static final String TIME_CHART_5_TEXT_1 = "05010015020001000200030004000500060007000Luminex FiveDays Since Start DateFi 249318596, Days: 44, TNF-alpha: 1454 249318596, Days: 79, TNF-alpha: 596.5 249318596, Days: 108, TNF-alpha: 154.5 249318596, Days: 190, TNF-alpha: 249.8 249318596, Days: 246, TNF-alpha: 166 249320107, Days: 0, TNF-alpha: 7989.3 249320107, Days: 42, TNF-alpha: 306.3 249320107, Days: 56, TNF-alpha: 155 249320107, Days: 105, TNF-alpha: 163 249320107, Days: 216, TNF-alpha: 185.8 249320127, Days: 0, TNF-alpha: 226.5 249320127, Days: 49, TNF-alpha: 381 249320127, Days: 160, TNF-alpha: 166.3 249320127, Days: 193, TNF-alpha: 164.5 249320127, Days: 225, TNF-alpha: 171.5 249320127, Days: 225, TNF-alpha: 171.5 249318596, Days: 44, IL-2: 560.5 249318596, Days: 79, IL-2: 302.5 249318596, Days: 108, IL-2: 67 249318596, Days: 190, IL-2: 81.5 249318596, Days: 246, IL-2: 72.5 249320107, Days: 0, IL-2: 4023 249320107, Days: 42, IL-2: 161.8 249320107, Days: 56, IL-2: 66.3 249320107, Days: 105, IL-2: 75.8 249320107, Days: 216, IL-2: 68.3 249320127, Days: 0, IL-2: 181.8 249320127, Days: 49, IL-2: 225.3 249320127, Days: 160, IL-2: 75.5 249320127, Days: 193, IL-2: 80.5 249320127, Days: 225, IL-2: 78.5 249320127, Days: 225, IL-2: 78.5 249318596, Days: 44, IL-10: 281.3 249318596, Days: 79, IL-10: 296 249318596, Days: 108, IL-10: 78.5 249318596, Days: 190, IL-10: 232 249318596, Days: 246, IL-10: 106.5 249320107, Days: 0, IL-10: 1686.5 249320107, Days: 42, IL-10: 152.8 249320107, Days: 56, IL-10: 80.5 249320107, Days: 105, IL-10: 111.3 249320107, Days: 216, IL-10: 108.8 249320127, Days: 0, IL-10: 195.3 249320127, Days: 49, IL-10: 206.3 249320127, Days: 160, IL-10: 81.3 249320127, Days: 193, IL-10: 113.5 249320127, Days: 225, IL-10: 95.5 249320127, Days: 225, IL-10: 95.5249318596 IL-10249318596 IL-2249318596 IL-6249318596 TNF-alpha249320107 IL-10249320107 IL-2249320107 IL-6249320107 TNF-alpha249320127 IL-10249320127 IL-2249320127 IL-6249320127 TNF-alpha";

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

    @Test
    public void advancedBrushTest()
    {
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
        assertEquals("Brushed area was not the expected height", "377", Locator.css(".brush .extent").findElement(getDriver()).getAttribute("height"));
        // Make sure when making a 1D selection that the opposite axis handle isn't visible.
        assertElementNotVisible(Locator.css(".y-axis-handle .resize.n"));
        assertElementNotVisible(Locator.css(".y-axis-handle .resize.s"));

        // 1D selection on y axis (select top right)
        builder.moveToElement(Locator.css(".y-axis-handle .background").findElement(getDriver())).moveByOffset(0, -188).clickAndHold().moveByOffset(0, 188).release().perform();
        verifyTopRightGroupBrushed();
        verifyBottomLeftGroupNotBrushed();
        assertEquals("Brushed area was not the expected width", "612", Locator.css(".brush .extent").findElement(getDriver()).getAttribute("width"));
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
