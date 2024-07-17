package org.labkey.test.stress;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PasswordUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({})
public class SimpleParallelApiTest extends BaseWebDriverTest
{
    private static final String USER = "template_user@simpleparallelapitest.test";
    private static final String USER2 = "template_user2@simpleparallelapitest.test";

    @Override
    protected void doCleanup(boolean afterTest)
    {
//        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(afterTest, USER, USER2);
    }

    @BeforeClass
    public static void setupProject()
    {
        SimpleParallelApiTest init = (SimpleParallelApiTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
//        _containerHelper.createProject(getProjectName(), null);
        _userHelper.createUser(USER);
        _userHelper.createUser(USER2);
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addUserAsAppAdmin(USER);
        apiPermissionsHelper.addUserAsAppAdmin(USER2);
        setInitialPassword(USER);
        setInitialPassword(USER2);
    }

    @Test
    public void testSomething() throws Exception
    {
        File sampleData = TestFileUtils.getSampleData("stress/lksm/dashboard-load.xml");

        goToHome();
        ConcurrentApiTestHelper apiHelper = new ConcurrentApiTestHelper(WebTestHelper.getBaseURL(), USER, PasswordUtil.getPassword());
        apiHelper.startSimulation(sampleData);
    }

    @Override
    protected String getProjectName()
    {
        return "SM_Pro_Sample_Update_Test";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
