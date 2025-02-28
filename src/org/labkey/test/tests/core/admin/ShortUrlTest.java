package org.labkey.test.tests.core.admin;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

@Category({})
public class ShortUrlTest extends BaseWebDriverTest
{
    private static final String URL_PREFIX = "surl_test_";
    private static final String URL_SUFFIX = ".url";
    private static final String USER = "template_user@shorturltest.test";

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _userHelper.deleteUsers(afterTest, USER);
    }

    @BeforeClass
    public static void setupProject()
    {
        ShortUrlTest init = getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _userHelper.createUser(USER);
    }

    @Test
    public void testSomething()
    {
        assertTrue("Failing stub test", false);
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
