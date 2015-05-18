package org.labkey.test.components.study;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;

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
        String studyProperties = _test.getText(elements().studyProperties);
        Pattern participantCountPattern = Pattern.compile("Data is present for (\\d+)");
        Matcher matcher = participantCountPattern.matcher(studyProperties);
        if (matcher.find())
            return Integer.parseInt(matcher.group(1));
        else
            throw new IllegalStateException("Unable to get participant count from Study Overview webpart");
    }

    public void clickStudyNavigator()
    {
        _test.clickAndWait(elements().linkStudyNavigator);
    }

    public void clickManageStudy()
    {
        _test.clickAndWait(elements().linkManageStudy);
    }

    public void clickManageFiles()
    {
        _test.clickAndWait(elements().linkManageFiles);
    }

    @Override
    protected Elements elements()
    {
        return new Elements();
    }

    private class Elements extends BodyWebPart.Elements
    {
        Locator.XPathLocator studyProperties = webPart.append(Locator.tagWithClass("td", "study-properties"));
        Locator.XPathLocator linkStudyNavigator = webPart.append(Locator.tagWithText("a", "Study Navigator"));
        Locator.XPathLocator linkManageStudy = webPart.append(Locator.tagWithText("a", "Manage Study"));
        Locator.XPathLocator linkManageFiles = webPart.append(Locator.tagWithText("a", "Manage Files"));
    }
}
