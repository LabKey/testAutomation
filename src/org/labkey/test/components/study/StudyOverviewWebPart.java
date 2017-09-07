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
package org.labkey.test.components.study;

import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.study.OverviewPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StudyOverviewWebPart extends BodyWebPart<StudyOverviewWebPart.Elements>
{
    private static final String DEFAULT_TITLE = "Study Overview";

    public StudyOverviewWebPart(WebDriver driver)
    {
        this(driver, DEFAULT_TITLE);
    }

    public StudyOverviewWebPart(WebDriver driver, String title)
    {
        super(driver, title);
    }

    public int getParticipantCount()
    {
        String studyProperties = elementCache().studyProperties.getText();
        Pattern participantCountPattern = Pattern.compile("Data is present for (\\d+)");
        Matcher matcher = participantCountPattern.matcher(studyProperties);
        if (matcher.find())
            return Integer.parseInt(matcher.group(1));
        else
            throw new IllegalStateException("Unable to get participant count from Study Overview webpart");
    }

    public OverviewPage clickStudyNavigator()
    {
        getWrapper().clickAndWait(elementCache().linkStudyNavigator);
        return new OverviewPage(getDriver());
    }

    public LabKeyPage clickManageStudy()
    {
        getWrapper().clickAndWait(elementCache().linkManageStudy);
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage clickManageFiles()
    {
        getWrapper().clickAndWait(elementCache().linkManageFiles);
        return new LabKeyPage(getDriver());
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public int getDatasetCount()
    {
        String studyProperties = elementCache().studyProperties.getText();
        Pattern datasetCountPattern = Pattern.compile("tracks data in (\\d+)");
        Matcher matcher = datasetCountPattern.matcher(studyProperties);
        if (matcher.find())
            return Integer.parseInt(matcher.group(1));
        else
            throw new IllegalStateException("Unable to get dataset count from Study Overview webpart");
    }

    protected class Elements extends BodyWebPart.ElementCache
    {
        WebElement studyProperties = new LazyWebElement(Locator.css("td.study-properties"), this);
        WebElement linkStudyNavigator = new LazyWebElement(Locator.tag("a").withText("Study Navigator"), this);
        WebElement linkManageStudy = new LazyWebElement(Locator.tag("a").withText("Manage Study"), this);
        WebElement linkManageFiles = new LazyWebElement(Locator.tag("a").withText("Manage Files"), this);
    }
}
