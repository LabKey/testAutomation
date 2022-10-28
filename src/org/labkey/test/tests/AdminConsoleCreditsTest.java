package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;

import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class AdminConsoleCreditsTest extends BaseWebDriverTest
{
    /*
        Test coverage : Issue 46587: Add test for display of credits page
        https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=46587
     */
    @Test
    public void testAdminConsoleCredits()
    {
        goToAdminConsole().clickCredits();
        log("Verifying the page is properly loaded");
        assertTextPresent("JAR Files Distributed with the API Module");
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
