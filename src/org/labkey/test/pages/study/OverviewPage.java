/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
package org.labkey.test.pages.study;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.CachingLocator;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OverviewPage extends LabKeyPage
{
    Elements _elements;

    public OverviewPage(WebDriver driver)
    {
        super(driver);
    }

    public static OverviewPage beginAt(WebDriverWrapper driver)
    {
        driver.beginAt(WebTestHelper.buildURL("study", driver.getCurrentContainerPath(), "overview"));
        return new OverviewPage(driver.getDriver());
    }

    public boolean isParticipantCountShown()
    {
        return elements().participantCountCheckbox.isSelected();
    }

    public OverviewPage showParticipantCounts()
    {
        if (!isParticipantCountShown())
        {
            clickAndWait(elements().participantCountCheckbox);
            return new OverviewPage(getDriver());
        }
        return this;
    }

    public OverviewPage hideParticipantCounts()
    {
        if (isParticipantCountShown())
        {
            clickAndWait(elements().participantCountCheckbox);
            return new OverviewPage(getDriver());
        }
        return this;
    }

    public boolean isRowCountShown()
    {
        return elements().rowCountCheckbox.isSelected();
    }

    public OverviewPage showRowCounts()
    {
        if (!isRowCountShown())
        {
            clickAndWait(elements().rowCountCheckbox);
            return new OverviewPage(getDriver());
        }
        return this;
    }

    public OverviewPage hideRowCounts()
    {
        if (isRowCountShown())
        {
            clickAndWait(elements().rowCountCheckbox);
            return new OverviewPage(getDriver());
        }
        return this;
    }

    public Map<String, Integer> getDatasetTotalParticipantCounts()
    {
        if (!isParticipantCountShown())
            throw new IllegalStateException("Participant counts are not currently visible");

        Map<String, List<CountPair>> datasetCounts = getDatasetRowData(0, 0);
        Map<String, Integer> datasetParticipantCounts = new HashMap<>();

        for (Map.Entry<String, List<CountPair>> entry : datasetCounts.entrySet())
        {
            datasetParticipantCounts.put(entry.getKey(), entry.getValue().get(0).getParticipantCount());
        }

        return datasetParticipantCounts;
    }

    public Map<String, Integer> getDatasetTotalRowCounts()
    {
        if (!isRowCountShown())
            throw new IllegalStateException("Row counts are not currently visible");

        Map<String, List<CountPair>> datasetCounts = getDatasetRowData(0, 0);
        Map<String, Integer> datasetParticipantCounts = new HashMap<>();

        for (Map.Entry<String, List<CountPair>> entry : datasetCounts.entrySet())
        {
            datasetParticipantCounts.put(entry.getKey(), entry.getValue().get(0).getRowCount());
        }

        return datasetParticipantCounts;
    }

    public Map<String, List<CountPair>> getDatasetRowData()
    {
        return getDatasetRowData(null, null);
    }

    public Map<String, List<CountPair>> getDatasetRowData(@Nullable Integer leftVisitIndex, @Nullable Integer endVisitIndex)
    {
        Map<String, List<CountPair>> datasetRowData = new HashMap<>();
        List<WebElement> overviewRows = elements().getStudyOverviewRows();
        overviewRows = overviewRows.subList(1, overviewRows.size());

        for (WebElement row : overviewRows)
        {
            List<WebElement> cells = Locator.css("td").findElements(row);
            String dataset = cells.get(0).getText();
            dataset = dataset.substring(0, dataset.length() - 1); // Strip help link '?'
            if (leftVisitIndex == null) leftVisitIndex = 0;
            if (endVisitIndex == null) endVisitIndex = cells.size() - 2;
            List<String> countTexts = getTexts(cells.subList(leftVisitIndex + 1, endVisitIndex + 2));
            List<CountPair> participantRowCounts = new ArrayList<>();

            for (String countStr : countTexts)
            {
                countStr = countStr.trim();
                Integer participantCount = null;
                Integer rowCount = null;

                if (countStr.contains("/"))
                {
                    String[] splitStr = countStr.split("\\s*/\\s*");
                    participantCount = Integer.parseInt(splitStr[0]);
                    rowCount = Integer.parseInt(splitStr[1]);
                }
                else if (isParticipantCountShown() && !countStr.isEmpty())
                {
                    participantCount = Integer.parseInt(countStr);
                }
                else if (!countStr.isEmpty())
                {
                    rowCount = Integer.parseInt(countStr);
                }

                participantRowCounts.add(new CountPair(participantCount, rowCount));
            }

            datasetRowData.put(dataset, participantRowCounts);
        }

        return datasetRowData;
    }

    public List<String> getVisits()
    {
        WebElement headerRow = elements().getStudyOverviewRows().get(0);
        List<WebElement> visitCells = Locator.css("td").findElements(headerRow);
        visitCells = visitCells.subList(1, visitCells.size() - 1);

        return getTexts(visitCells);
    }

    public Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }
    
    private class Elements extends ComponentElements
    {
        @Override
        protected SearchContext getContext()
        {
            return getDriver();
        }

        public final WebElement participantCountCheckbox = new LazyWebElement(Locator.checkboxByNameAndValue("visitStatistic", "ParticipantCount"), this);
        public final WebElement rowCountCheckbox = new LazyWebElement(Locator.checkboxByNameAndValue("visitStatistic", "RowCount"), this);
        public final WebElement studyOverview = new LazyWebElement(Locator.tagWithId("table", "studyOverview"), this);

        private final Locator studyOverviewRow = new CachingLocator(Locator.CssLocator.union(Locator.css(".labkey-row"), Locator.css(".labkey-alternate-row")));

        public List<WebElement> getStudyOverviewRows()
        {
            return studyOverviewRow.findElements(studyOverview);
        }
    }

    public class CountPair
    {
        private Integer participantCount;
        private Integer rowCount;
        
        CountPair(Integer participantCount, Integer rowCount)
        {
            this.participantCount = participantCount;
            this.rowCount = rowCount;
        }
        
        public Integer getParticipantCount()
        {
            return participantCount;
        }
        
        public Integer getRowCount()
        {
            return rowCount;
        }
    }
}
