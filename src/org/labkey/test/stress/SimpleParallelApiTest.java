package org.labkey.test.stress;

import org.junit.BeforeClass;
import org.junit.Before;
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

import static org.junit.Assert.*;

@Category({})
public class SimpleParallelApiTest extends BaseWebDriverTest
{
    private static final String USER = "template_user@simpleparallelapitest.test";

    @Override
    protected void doCleanup(boolean afterTest)
    {
//        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(afterTest, USER);
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
        new ApiPermissionsHelper(this).addUserAsAppAdmin(USER);
        setInitialPassword(USER);
    }

    @Test
    public void testSomething() throws Exception
    {
        File sampleData = TestFileUtils.getSampleData("api/http-api.xml");

        goToHome();
        ConcurrentApiTestHelper apiHelper = new ConcurrentApiTestHelper(WebTestHelper.getBaseURL(), USER, PasswordUtil.getPassword());
        apiHelper.start();
        List<Integer> results = apiHelper.runTests(sampleData);
        log("Results: " + results);
    }

    @Override
    protected String getProjectName()
    {
        return "HTTPApiVerifyProject";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
