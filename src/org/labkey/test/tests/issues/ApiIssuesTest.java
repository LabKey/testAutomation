package org.labkey.test.tests.issues;

import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Issues;
import org.labkey.test.util.IssuesApiHelper;

@Category({Issues.class, Daily.class, Data.class})
@BaseWebDriverTest.ClassTimeout(minutes = 20)
/**
 * A version of the issues test that uses the API-based version of the helper.
 */
public class ApiIssuesTest extends IssuesTest
{
    public ApiIssuesTest()
    {
        _issuesHelper = new IssuesApiHelper(this);
    }
}
