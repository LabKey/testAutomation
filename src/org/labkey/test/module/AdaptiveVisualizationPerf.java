/*
 * Copyright (c) 2010 LabKey Corporation
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
package org.labkey.test.module;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.util.Random;

import static org.labkey.test.WebTestHelper.DEFAULT_TARGET_SERVER;
import static org.labkey.test.WebTestHelper.getTargetServer;

/**
 * Created by IntelliJ IDEA.
 * User: Treygdor
 * Date: Sep 15, 2010
 * Time: 5:08:22 PM
 */

public class AdaptiveVisualizationPerf extends BaseSeleniumWebTest
{
    public final static int WAIT_FOR_JAVASCRIPT = 120000; // Allow long timeout for perf testing.
    private static final Integer SELENIUM_SPEED = 100;
    private static final long USER_DELAY = 0; // Pause between actions to simulate an actual user.
    private static final String CUSTOMER_FOLDER_NAME = "Robins Data";
    private static final String SAMPLE_1 = "Person_1_memory";
    private static final String SAMPLE_2 = "Person_1_naive";
    private static final String SAMPLE_3 = "Person_2_memory";
    private static final String SAMPLE_4 = "Person_2_naive";
    private static final String SAMPLE_5 = "Person_3_memory";
    private static final String SAMPLE_6 = "Person_3_naive";
    private static final String SAMPLE_7 = "Person_4_memory";
    private static final String SAMPLE_8 = "Person_4_naive";
    private static final String SAMPLE_9 = "Person_5_memory";
    private static final String SAMPLE_10 = "Person_5_naive";
    private static final String SAMPLE_11 = "Person_6_memory";
    private static final String SAMPLE_12 = "Person_6_naive";


    @Override
    public void cleanup() throws Exception
    {
		if (!getTargetServer().equals(DEFAULT_TARGET_SERVER))
        {
            log("Skipping cleanup.  This may be running on a live server.");
            return;
        }
        super.cleanup();
    }

    public void doCleanup()
    {
        log("Nothing to cleanup.  This is a read-only test.");
    }

    public void doTestSteps()
    {
        selenium.setSpeed(SELENIUM_SPEED.toString());

        // 1. User logs in and, from home page, navigates to "view samples"
        beginAt("adaptive/home/CustomerSites/" + CUSTOMER_FOLDER_NAME + "/begin.view");

        // 3. User navigates to sample analysis page via clicking on "view sequences" and selects one sample for tabular view.
        clickAdaptiveMenuButton("Analysis", "Single Sample Analysis");
        setAnalysisSample(SAMPLE_1);
        click(Locator.id("tabular"));
        clickLinkWithText("View Data", false);
        waitForElementToDisappear(Locator.tagWithText("span", "Generating sequence statistics..."), WAIT_FOR_JAVASCRIPT);

        pause();

        // 4. User then selects 3-D graph for that sample.
        click(Locator.id("3dhistogram"));
        viewData(true);

        pause();

        // 5. User then generates 3-D graphs for two additional samples.
        setAnalysisSample(SAMPLE_2);
        click(Locator.id("3dhistogram"));
        viewData(true);

        pause();

        setAnalysisSample(SAMPLE_3);
        click(Locator.id("3dhistogram"));
        viewData(true);

        pause();

        // 6. User then chooses compare 2 samples with log-scale scatterplot, leaving default settings.
        clickAdaptiveMenuButton("Analysis", "Compare Samples");
        setAnalysisSample(SAMPLE_1, 0);
        setAnalysisSample(SAMPLE_2, 1);
        click(Locator.id("compare-scatter"));
        viewData(true);

        // 6x. User saves view. User drills down to two or three data points to view sequences underlying those points.
        String flashVars = getAttribute(Locator.id("amxy"), "flashvars");
        String[] splitVars = flashVars.split("y%3D%22null%22"); // y='0'
        String[] onePoint = splitVars[1].split("'");
        String bucketId = onePoint[1];
        String x = onePoint[3];
        String y = onePoint[5];
        log("Click chart point (" + x + "," + y + ")");
        selenium.getEval("{this.browserbot.getCurrentWindow().amClickedOnPoint('" + bucketId + "','" + x + "','" + y + "');}");
        waitForText("x : " + x, WAIT_FOR_JAVASCRIPT);

        onePoint = splitVars[2].split("'");
        bucketId = onePoint[1];
        x = onePoint[3];
        y = onePoint[5];
        log("Click chart point (" + x + "," + y + ")");
        selenium.getEval("{this.browserbot.getCurrentWindow().amClickedOnPoint('" + bucketId + "','" + x + "','" + y + "');}");
        waitForText("x : " + x, WAIT_FOR_JAVASCRIPT);

        onePoint = splitVars[3].split("'");
        bucketId = onePoint[1];
        x = onePoint[3];
        y = onePoint[5];
        log("Click chart point (" + x + "," + y + ")");
        selenium.getEval("{this.browserbot.getCurrentWindow().amClickedOnPoint('" + bucketId + "','" + x + "','" + y + "');}");
        waitForText("x : " + x, WAIT_FOR_JAVASCRIPT);

        onePoint = splitVars[4].split("'");
        bucketId = onePoint[1];
        x = onePoint[3];
        y = onePoint[5];
        log("Click chart point (" + x + "," + y + ")");
        selenium.getEval("{this.browserbot.getCurrentWindow().amClickedOnPoint('" + bucketId + "','" + x + "','" + y + "');}");
        waitForText("x : " + x, WAIT_FOR_JAVASCRIPT);

        clickButton("Details view", 0);
        waitForElementToDisappear(Locator.tagWithText("div", "Loading, please wait..."), 300000);
        assertTextNotPresent("Load Error");

        // 8. User then selects tabular view to compare the same samples.
        clickNavButton("Close", 0);
        click(Locator.id("tabular"));
        viewData(true);

        // 8b. On sample union page, user selects to view data in tabular form and exports it,
        // then navigates to unique-to-sample-X pages, views the tabular data, and exports it as well.
        clickButton("Show tabular data", 0);
        waitForExtMaskToDisappear(WAIT_FOR_JAVASCRIPT * 3);
        pause();
        click(Locator.tagWithText("span", "Sequences only in " + SAMPLE_1));
        clickButton("Show tabular data", 0);
        waitForExtMaskToDisappear(WAIT_FOR_JAVASCRIPT * 3);
        pause();
        click(Locator.tagWithText("span", "Sequences only in " + SAMPLE_2));
        clickButton("Show tabular data", 0);
        waitForExtMaskToDisappear(WAIT_FOR_JAVASCRIPT * 3);
        pause();
        //TODO: Export once exportAsWebpage functionality is in place.

        // 9. Repeat Steps 4-6 for five additional pairwise comparisons.

        // 10. User takes a 5 minute break.
        // pause(60000 * 5);  // omitting this step. probably unnecessary.

        // 11. User returns to single-sample tabular view and opens six tabs.
        clickAdaptiveMenuButton("Analysis", "Single Sample Analysis");
        setAnalysisSample(SAMPLE_3);
        click(Locator.id("tabular"));
        clickLinkWithText("View Data", false);
        waitForExtMaskToDisappear(WAIT_FOR_JAVASCRIPT);

        setAnalysisSample(SAMPLE_4);
        click(Locator.id("tabular"));
        clickLinkWithText("View Data", false);
        waitForExtMaskToDisappear(WAIT_FOR_JAVASCRIPT);

        setAnalysisSample(SAMPLE_5);
        click(Locator.id("tabular"));
        clickLinkWithText("View Data", false);
        waitForExtMaskToDisappear(WAIT_FOR_JAVASCRIPT);

        setAnalysisSample(SAMPLE_6);
        click(Locator.id("tabular"));
        clickLinkWithText("View Data", false);
        waitForExtMaskToDisappear(WAIT_FOR_JAVASCRIPT);

        setAnalysisSample(SAMPLE_7);
        click(Locator.id("tabular"));
        clickLinkWithText("View Data", false);
        waitForExtMaskToDisappear(WAIT_FOR_JAVASCRIPT);

        setAnalysisSample(SAMPLE_8);
        click(Locator.id("tabular"));
        clickLinkWithText("View Data", false);
        waitForExtMaskToDisappear(WAIT_FOR_JAVASCRIPT);

        // 12. In tab 1, user sorts data by copy number, ascending, 
        // then re-sorts by copy number descending. User then sorts by V gene and then by V family.
        click(Locator.tagWithText("span", "Summary data: " + SAMPLE_3)); // First tab.
        click(Locator.tagWithText("div", "Copy"));
        click(Locator.tagWithText("div", "Copy"));
        click(Locator.tagWithText("div", "VGene"));
        click(Locator.tagWithText("div", "VFamily"));

        // 13. While navigating and filtering, user accidentally hits export button, exporting all data to csv.
        // TODO: Export once exportAsWebpage functionality is in place.

        // 14. User returns to tabular view and generates 2D histogram of Vfamily,
        // total counts for six tabs and prints/saves as pdf for all graphs.

    }

    private void pause()
    {
        pause(USER_DELAY);
    }

    private static final int PAUSE_VARIANCE = 25;
    private void pause(Long approximateWait)
    {
        // Pause for a moment.  USER_DELAY +/- 25%
        Random rand = new Random(System.currentTimeMillis());
        Double actualWait = approximateWait.doubleValue() * (1 + ((double)rand.nextInt(PAUSE_VARIANCE * 2) - PAUSE_VARIANCE)/100);
        log("Simulated user pause: wait for " + actualWait.intValue() + " ms.");
        sleep(actualWait.intValue());
    }

    public void clickAdaptiveMenuButton(String menu, String menuItem)
    {
        mouseOver(Locator.linkWithText(menu));
        waitForElement(Locator.linkWithText(menuItem), WAIT_FOR_JAVASCRIPT);
        clickLinkWithText(menuItem);
    }

    private void viewData(boolean wait)
    {
        clickLinkWithText("View Data", false);
        if (wait)
        {
            waitForElement(Locator.xpath("//div[contains(@style, 'visibility: visible')]//span[text() = 'Please Wait...']"), WAIT_FOR_JAVASCRIPT);
            waitForElementToDisappear(Locator.xpath("//div[contains(@style, 'visibility: visible')]//span[text() = 'Please Wait...']"), WAIT_FOR_JAVASCRIPT * 3);
            waitForElementToDisappear(Locator.xpath("//div[contains(@style, 'visibility: visible')]//span[text() = 'Loading Views...']"), WAIT_FOR_JAVASCRIPT);
        }

        assertTextNotPresent("Load Error");
    }
    
    private void setAnalysisSample(String value)
    {
        setAnalysisSample(value, 0);
    }

    private void setAnalysisSample(String value, int index)
    {
        click(Locator.xpath("//div[contains(@class, 'x-list-body-inner')]/dl/dt/em[contains(@unselectable, 'on') and text()='" + value + "']").index(index));
    }

    @Override
    public boolean isGuestModeTest()
    {
        return true;
    }
    
    @Override
    public String getAssociatedModuleDirectory()
    {
        return "none";
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }
}
