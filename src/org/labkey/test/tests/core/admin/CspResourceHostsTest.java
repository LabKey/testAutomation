package org.labkey.test.tests.core.admin;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.core.admin.CspConfigHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

@Category({Daily.class})
public class CspResourceHostsTest extends BaseWebDriverTest
{
    private static final String USER = "csp_app_admin@cspresourcehoststest.test";

    private final CspConfigHelper _cspConfigHelper = new CspConfigHelper(this);

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _userHelper.deleteUsers(afterTest, USER);
    }

    @BeforeClass
    public static void setupProject()
    {
        CspResourceHostsTest init = getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _userHelper.createUser(USER);
        new ApiPermissionsHelper(this).addUserAsAppAdmin(USER);
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
