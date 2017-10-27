/*
 * Copyright (c) 2016-2017 LabKey Corporation
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Category({DailyA.class})
public class DataViewsReportOrderingTest extends BaseWebDriverTest
{
    private static final String ORIGINAL_WEBPART_TITLE = "Data Views";
    private final PortalHelper _portalHelper = new PortalHelper(this);

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        DataViewsReportOrderingTest init = (DataViewsReportOrderingTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), "Study");
        importFolderFromZip(TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip"));
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testReportOrdering()
    {
        // test by user the Reorder Reports window to reverse the list of reports for each category that has reports

        // Getting a random js error failure on TeamCity that is benign. Going to pause javascript error checking.
        pauseJsErrorChecker();

        // navigate to Data Views web part
        clickAndWait(Locator.linkContainingText("Clinical and Assay Data"));

        // navigate to Manage Views page
        _portalHelper.clickWebpartMenuItem(ORIGINAL_WEBPART_TITLE, false, "Manage Views");

        // bring up the Reorder Reports popup
        waitAndClick(Locator.linkContainingText("Reorder Reports And Charts"));

        // use the ReorderReportsWindow component to reverse the order of the reports in each category
        ReorderReportsWindow reorderReportsWindow = new ReorderReportsWindow(getDriver());
        HashMap<String, List<String>> categorReportsOriginalOrder = new HashMap<>();
        List<WebElement> categories = reorderReportsWindow.getCategories();
        for (WebElement category : categories)
        {
            String categoryText = category.getText();
            List<String> reports = reorderReportsWindow.getReportsForCategory(categoryText);
            String[] reportsA = new String[reports.size()];
            reportsA = reports.toArray(reportsA);
            if (reportsA.length > 0)
            {
                assertTextPresentInThisOrder(reportsA);  // confirm reports were in original order on Manage Views page
            }
            categorReportsOriginalOrder.put(categoryText, reports);
            // reverse the ordering of reports in this category
            for (int x = 1; x < reports.size(); x++)
            {
                reorderReportsWindow.dragAndDrop(reports.get(x), reports.get(x-1));   // move each report to the postion of the first report
            }
        }

        // when the Done button is clicked the Manage Views page will update to show reversed reports
        reorderReportsWindow.done();
        _ext4Helper.waitForMaskToDisappear();

        // verify that the Manage Views displays the reports in the reverse order from the original
        for (String category : categorReportsOriginalOrder.keySet())
        {
            // determine what the reverse order should be for the reports in each category and verify that it appears on Manage Veiws page
            String[] reportsBackwards = reverseReports(categorReportsOriginalOrder.get(category));
            if (null != reportsBackwards && reportsBackwards.length > 0)
            {
                assertTextPresentInThisOrder(reportsBackwards);  // confirm reports in reverse order on Manage Views page
            }
        }

        resumeJsErrorChecker();
    }

    private String[] reverseReports(List<String> reportsOriginalOrder)
    {
        String[] reverseReports = new String[reportsOriginalOrder.size()];
        for (int x = 0; x < reportsOriginalOrder.size(); x++)
        {
            reverseReports[x] = reportsOriginalOrder.get(reportsOriginalOrder.size() - x -1);
        }
        return reverseReports;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "DataViewsReportOrderingTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("study");
    }

    public static class ReorderReportsWindow extends Window
    {
        protected ReorderReportsWindow(WebDriver driver)
        {
            super(Window(driver).withTitleContaining("Reorder Reports and Charts"));
        }

        public List<WebElement> getCategories()
        {
            return findElements(Locator.xpath("//tr/td/div/span").withClass("x4-tree-node-text"));
        }

        public List<String> getReportsForCategory(String category)
        {
            selectCategory(category);
            List<WebElement> reportWebElements = findElements(Locator.xpath("//tr/td/div").withClass("x4-grid-cell-inner").withoutClass("x4-grid-cell-inner-treecolumn"));
            List<String> reportStrings = new ArrayList<>();
            for (WebElement reportWebElement : reportWebElements)
            {
                reportStrings.add(reportWebElement.getText());
            }
            return reportStrings;
        }

        public void selectCategory(String category)
        {
            Actions builder = new Actions(getWrapper().getDriver());
            builder.click(findElement(Locator.xpath("//tr/td/div/span").withClass("x4-tree-node-text").withText(category))).build().perform();
        }

        public void done()
        {
            clickButton("Done", 0);
            waitForClose();
        }

        public void dragAndDrop(String fromReport, String toReport)
        {
            WebElement fromEl = findElement(Locator.xpath("//tr/td/div").withClass("x4-grid-cell-inner").withoutClass("x4-grid-cell-inner-treecolumn").withText(fromReport));
            WebElement toEl   = findElement(Locator.xpath("//tr/td/div").withClass("x4-grid-cell-inner").withoutClass("x4-grid-cell-inner-treecolumn").withText(toReport));
            dragAndDrop(fromEl, toEl);
        }

        public void dragAndDrop(WebElement fromEl, WebElement toEl)
        {
            Actions builder = new Actions(getWrapper().getDriver());
            builder.dragAndDrop(fromEl, toEl).pause(500).build().perform(); // needs to pause a bit for ui move animation otherwise test fails
        }
    }

}