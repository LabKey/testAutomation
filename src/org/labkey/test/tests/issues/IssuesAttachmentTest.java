package org.labkey.test.tests.issues;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyC;
import org.labkey.test.pages.issues.ClosePage;
import org.labkey.test.pages.issues.DetailsListPage;
import org.labkey.test.pages.issues.DetailsPage;
import org.labkey.test.pages.issues.ReopenPage;
import org.labkey.test.pages.issues.ResolvePage;
import org.labkey.test.pages.issues.UpdatePage;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.Maps;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({DailyC.class})
public class IssuesAttachmentTest extends BaseWebDriverTest
{
    public static final File ALERT_FILE = TestFileUtils.getSampleData("filenames/<img src='' onerror='alert(0)'>");
    public static final File ESCAPE_FILE = TestFileUtils.getSampleData("filenames/hello&nbsp;&lt;world");
    public static final String LIST_DEF_NAME = "Issues";
    private IssuesHelper issuesHelper = new IssuesHelper(this);

    @BeforeClass
    public static void setupProject()
    {
        IssuesAttachmentTest init = (IssuesAttachmentTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        issuesHelper.createNewIssuesList(LIST_DEF_NAME, _containerHelper);
    }

    @Test
    public void testAttachmentInjection() throws Exception
    {
        Map<String, String> issue = Maps.of("assignedTo", getDisplayName(), "title", "Attempt Issue Attachment Injection");
        final DetailsPage detailsPage = issuesHelper.addIssue(issue, ALERT_FILE, ESCAPE_FILE);
        final String issueId = detailsPage.getIssueId();

        // details page
        assertElementPresent(Locator.linkWithText(" " + ALERT_FILE.getName()));
        assertElementPresent(Locator.linkWithText(" " + ESCAPE_FILE.getName()));

        detailsPage.clickPrint();
        assertElementPresent(Locator.linkWithText(" " + ALERT_FILE.getName()));
        assertElementPresent(Locator.linkWithText(" " + ESCAPE_FILE.getName()));

        UpdatePage.beginAt(this, issueId);
        assertElementPresent(Locator.linkWithText(" " + ALERT_FILE.getName()));
        assertElementPresent(Locator.linkWithText(" " + ESCAPE_FILE.getName()));

        ResolvePage.beginAt(this, issueId);
        assertElementPresent(Locator.linkWithText(" " + ALERT_FILE.getName()));
        assertElementPresent(Locator.linkWithText(" " + ESCAPE_FILE.getName()));

        ClosePage.beginAt(this, issueId);
        assertElementPresent(Locator.linkWithText(" " + ALERT_FILE.getName()));
        assertElementPresent(Locator.linkWithText(" " + ESCAPE_FILE.getName()));

        ReopenPage.beginAt(this, issueId);
        assertElementPresent(Locator.linkWithText(" " + ALERT_FILE.getName()));
        assertElementPresent(Locator.linkWithText(" " + ESCAPE_FILE.getName()));

        DetailsListPage.beginAt(this, LIST_DEF_NAME);
        assertElementPresent(Locator.linkWithText(" " + ALERT_FILE.getName()));
        assertElementPresent(Locator.linkWithText(" " + ESCAPE_FILE.getName()));
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "IssuesAttachmentTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("issues");
    }
}
