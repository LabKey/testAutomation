/*
 * Copyright (c) 2015 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.study.OverviewPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StudyOverviewWebPart extends BodyWebPart
{
    private static final String DEFAULT_TITLE = "Study Overview";

    public StudyOverviewWebPart(BaseWebDriverTest test)
    {
        this(test, DEFAULT_TITLE);
    }

    public StudyOverviewWebPart(BaseWebDriverTest test, String title)
    {
        super(test, title);
    }

    public int getParticipantCount()
    {
        String studyProperties = elements().studyProperties.getText();
        Pattern participantCountPattern = Pattern.compile("Data is present for (\\d+)");
        Matcher matcher = participantCountPattern.matcher(studyProperties);
        if (matcher.find())
            return Integer.parseInt(matcher.group(1));
        else
            throw new IllegalStateException("Unable to get participant count from Study Overview webpart");
    }

    public OverviewPage clickStudyNavigator()
    {
        _test.clickAndWait(elements().linkStudyNavigator);
        return new OverviewPage(getDriver());
    }

    public LabKeyPage clickManageStudy()
    {
        _test.clickAndWait(elements().linkManageStudy);
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage clickManageFiles()
    {
        _test.clickAndWait(elements().linkManageFiles);
        return new LabKeyPage(getDriver());
    }

    @Override
    protected Elements elements()
    {
        return new Elements();
    }

    private class Elements extends BodyWebPart.Elements
    {
        WebElement studyProperties = new LazyWebElement(Locator.css("td.study-properties"), this);
        WebElement linkStudyNavigator = new LazyWebElement(Locator.xpath("a").withText("Study Navigator"), this);
        WebElement linkManageStudy = new LazyWebElement(Locator.xpath("a").withText("Manage Study"), this);
        WebElement linkManageFiles = new LazyWebElement(Locator.xpath("a").withText("Manage Files"), this);
    }
}
