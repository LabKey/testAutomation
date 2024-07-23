package org.labkey.test.stress;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PasswordUtil;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Category({})
public class SimpleParallelApiTest extends BaseBackgroundLoadTest
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
    public static void setupProject() throws Exception
    {
        SimpleParallelApiTest init = (SimpleParallelApiTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup() throws Exception
    {
//        _containerHelper.createProject(getProjectName(), null);
        _userHelper.createUser(USER);
        _userHelper.createUser(USER2);
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addUserAsAppAdmin(USER);
        apiPermissionsHelper.addUserAsAppAdmin(USER2);
        _userHelper.setInitialPassword(USER);
        _userHelper.setInitialPassword(USER2);
    }

    @Override
    protected List<Simulation.Definition> getSimulationDefinitions()
    {
        File sampleData = TestFileUtils.getSampleData("stress/lksm/dashboard-load.xml");

        Simulation.Definition definition = new Simulation.Definition(WebTestHelper.getBaseURL(), USER, PasswordUtil.getPassword())
                .setActivityFiles(sampleData);
        return Collections.nCopies(100, definition);
    }

    @Test
    public void testSomething() throws Exception
    {
        goToHome();
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
