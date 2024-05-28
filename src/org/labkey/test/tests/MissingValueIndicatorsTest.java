/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.labkey.test.util.DataRegionTable.DataRegion;

public abstract class MissingValueIndicatorsTest extends BaseWebDriverTest
{
    private static final String MV_LOCATOR_CLASS = "labkey-mv-indicator";

    @LogMethod
    protected void setupMVIndicators()
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Missing Values"));
        uncheckCheckbox(Locator.checkboxById("inherit"));

        // Delete all site-level settings
        for (WebElement deleteButton : Locator.tagWithAttribute("img", "alt", "delete").findElements(getDriver()))
        {
            deleteButton.click();
            shortWait().until(ExpectedConditions.stalenessOf(deleteButton));
        }

        String[] mvIndicators = new String[] {"Q", "N", "Z"};
        for (int i = 0; i < mvIndicators.length; i++)
        {
            clickButton("Add", 0);
            WebElement mvInput = Locator.css("#mvIndicatorsDiv input[name=mvIndicators]").index(i).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
            setFormElement(mvInput, mvIndicators[i]);
        }
        clickButton("Save");
    }

    protected void checkDataregionData(DataRegionTable dataRegion, Map<String, List<String>> expectedValues)
    {
        for(Map.Entry<String, List<String>> entry : expectedValues.entrySet())
        {
            List<String> actualValues = dataRegion.getColumnDataAsText(entry.getKey());
            checker().verifyEquals(String.format("Values in column '%s' not as expected.", entry.getKey()),
                    entry.getValue(), actualValues);
        }
        checker().screenShotIfNewError(getProjectName()+ "_DataRegion_Data_Error");
    }

    protected void checkMvIndicatorPresent(DataRegionTable dataRegion, Map<String, List<Integer>> expectedMVCells)
    {

        int numOfRows = dataRegion.getDataRowCount();

        for(Map.Entry<String, List<Integer>> entry : expectedMVCells.entrySet())
        {
            List<Integer> actualMVCells = new ArrayList<>();
            for(int row = 0; row < numOfRows; row++)
            {
                WebElement cell = dataRegion.findCell(row, entry.getKey());
                if(cell.getAttribute("class").equals(MV_LOCATOR_CLASS))
                {
                    actualMVCells.add(row);
                }
            }

            checker().verifyEquals(String.format("Rows with MV indicator in column '%s' not as expected.", entry.getKey()),
                    entry.getValue(), actualMVCells);
        }

        checker().screenShotIfNewError(getProjectName()+ "_MV_Indicator_Error");

    }

    protected void checkOriginalValuePopup(DataRegionTable dataRegion, String column, int row, String expectedValue)
    {
        WebElement origValueMsg = Locator.tagWithId("span", "helpDivBody").findWhenNeeded(getDriver());

        WebElement cell = dataRegion.findCell(row, column);

        WebElement popupLink = Locator.tagWithClass("a", "_helpPopup").findWhenNeeded(cell);

        popupLink.click();

        if(checker().verifyTrue("'Original Value' popup did not show.",
                waitFor(origValueMsg::isDisplayed, 1_000)))
        {
            checker().verifyEquals("'Original Value' message not as expected.",
                    String.format("The value as originally entered was: '%s'.", expectedValue), origValueMsg.getText());
        }

        checker().screenShotIfNewError(getProjectName()+ "_MV_Popup_Error");

    }

    @LogMethod
    protected void testMvFiltering(List<String> expectedMvColumns)
    {
        log("Testing if missing value filtering works as expected");
        String mviFilter = "Has a missing value indicator";
        String noMviFilter = "Does not have a missing value indicator";
        DataRegionTable dataRegionTable = DataRegion(getDriver()).find();
        List<String> columns = dataRegionTable.getColumnNames();
        assertThat("Didn't find expected MV enabled columns.", columns, CoreMatchers.hasItems(expectedMvColumns.toArray(new String[]{})));
        for (String colName: columns)
        {
            if (expectedMvColumns.contains(colName))
            {
                int totalRows = dataRegionTable.getDataRowCount();
                dataRegionTable.setFilter(colName, mviFilter);
                int mvRows = dataRegionTable.getDataRowCount();
                dataRegionTable.setFilter(colName, noMviFilter);
                int nonMvRows = dataRegionTable.getDataRowCount();
                assertEquals("Sum of MV and non-MV rows didn't equal total row count for column: " + colName,
                        totalRows, mvRows + nonMvRows);
                dataRegionTable.clearFilter(colName);
            }
            else
            {
                WebElement filterDialog = dataRegionTable.openFilterDialog(colName);
                try
                {
                    _extHelper.clickExtTab("Choose Filters");
                }
                catch (WebDriverException ignore) { }
                WebElement comboArrow = Locator.css(".x-form-arrow-trigger")
                        .findElement( Locator.tagWithClass("div", "x-form-item").withPredicate(Locator.xpath("./label").withText("Filter Type:")).findElement(filterDialog));
                comboArrow.click();
                List<WebElement> elements = getDriver().findElements(By.xpath("//div[@class='x-combo-list-item']"));

                List<String> options = new ArrayList<>();

                for (WebElement el : elements)
                {
                    String filterType = el.getText();
                    if (filterType.equals(mviFilter) || filterType.equals(noMviFilter))
                    {
                        Assert.fail("Column without missing values enabled has MV filtering options: " + colName);
                    }
                }
                Locator.byClass("x-tool-close").findElement(filterDialog).click();
            }
        }
    }

    @LogMethod
    private void defineList()
    {
        final String TEST_DATA_AGE_LIST =
                        "Age\n" +
                        "10\n" +
                        "17\n" +
                        "25\n" +
                        "50";

        goToModule("List");
        _listHelper.createList(getProjectName(), "Ages", new FieldDefinition("Age", ColumnType.Integer));
        _listHelper.goToList("Ages");
        _listHelper.uploadData(TEST_DATA_AGE_LIST);
    }

    @LogMethod
    protected void defineAssay(String assayName)
    {
        defineList();

        goToManageAssays();

        ReactAssayDesignerPage assayDesignerPage = _assayHelper.createAssayDesign("General", assayName);
        DomainFormPanel resultsPanel = assayDesignerPage.goToResultsFields();

        resultsPanel.addField("age")
            .setLabel("Age")
            .setType(ColumnType.Lookup)
            .setFromSchema("lists")
            .setFromTargetTable("Ages (Integer)")
            .setMissingValuesEnabled(true);

        resultsPanel.addField("sex")
            .setLabel("Sex")
            .setType(ColumnType.String)
            .setMissingValuesEnabled(true);

        assayDesignerPage.clickFinish();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
