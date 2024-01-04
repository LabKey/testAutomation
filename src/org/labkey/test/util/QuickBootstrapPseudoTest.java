package org.labkey.test.util;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;

import java.util.Arrays;
import java.util.List;

/**
 * Bootstrap a server without the initial user validation done by {@link LabKeySiteWrapper#signIn()}
 * Not actually a test. Just piggy-backing on the test harness to make it easier to run.
 * TODO: Make this class extend {@link LabKeySiteWrapper} so that we don't open a browser.
 * Requires that we are able to create the initial user via API.
 */
@Category({})
public class QuickBootstrapPseudoTest extends BaseWebDriverTest
{
    final ApiBootstrapHelper _bootstrapHelper = new ApiBootstrapHelper(getDriver());

    @Override
    public void signIn()
    {
        _bootstrapHelper.signIn();
    }

    @Test
    public void testNothing()
    {
        TestLogger.log(whoAmI().getParsedData().toString());
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
