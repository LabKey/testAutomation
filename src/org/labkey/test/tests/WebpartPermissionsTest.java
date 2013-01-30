package org.labkey.test.tests;

import junit.framework.Assert;
import org.apache.commons.lang3.ArrayUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;

/**
 * User: elvan
 * Date: 1/16/13
 * Time: 2:37 PM
 */
public class WebpartPermissionsTest extends BaseWebDriverTest
{

    protected static final String DUMMY_PROJECT_NAME = "Dummy Project";
    protected static final String[] users = {"read@webpartpermissions.test", "edit@webpartpermissions.test", "admin@webpartpermissions.test"};
    @Override
    protected String getProjectName()
    {
        return "Webpart Perms project";
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setUpDummyFolder();

        setUpFocusFolder();

        verifyNoWebpartsVisible();

        setPermissionsOnDummyFolder();

        verifyCorrectWebpartsVisible();

        changeWebpartPermToCurrentFolderAndVerify();

        changeWebpartPermAndVerify();

        folderDelete();
    }

    private void folderDelete()
    {
        try
        {
            _containerHelper.deleteProject(DUMMY_PROJECT_NAME);
        }
        catch (TestTimeoutException e)
        {
            Assert.fail("Unable to delete dummy project");
        }
        verifyCorrectWebpartsVisible();


    }

    private void changeWebpartPermAndVerify()
    {
        goToProjectHome();
        goToProjectHome();
        String changedWebPart = "Flow Analyses";
        _securityHelper.setWebpartPermission(changedWebPart, "Update", DUMMY_PROJECT_NAME);

        impersonate(users[0]);
        goToProjectHome();
        assertTextNotPresent(changedWebPart);
        stopImpersonating();
    }

    private void changeWebpartPermToCurrentFolderAndVerify ()
    {
        goToProjectHome();
        String changedWebPart = "Flow Analyses";
        _securityHelper.setWebpartPermission(changedWebPart, "Read", null);

        //all users are readers, so the changed part will be visible now
        impersonate(users[0]);
        goToProjectHome();
        assertTextPresent(changedWebPart);
        stopImpersonating();

    }

    private void verifyCorrectWebpartsVisible()
    {
        String[] webparts = {"Flow Summary", "Flow Scripts", "Flow Experiment Management", "Flow Analyses"};
//        verifyReadEditUpdate(webparts);
    }

    private void verifyReadEditUpdate(String[] webparts)
    {
        for(int i=0; i<3; i++) //TODO:  up to 3 when fix checked in Issue 16987
        {
            impersonate(users[i]);
            goToProjectHome();
            int arrayIndex= i+2;
            assertTextPresent(ArrayUtils.subarray(webparts, 0, arrayIndex));
            assertTextNotPresent(ArrayUtils.subarray(webparts, arrayIndex, webparts.length));
            stopImpersonating();
        }
    }

    private void setPermissionsOnDummyFolder()
    {
        click(Locator.linkWithText(DUMMY_PROJECT_NAME));
        _securityHelper.setProjectPerm(users[0], DUMMY_PROJECT_NAME, "Reader");
        _securityHelper.setProjectPerm(users[1], "Editor");
        _securityHelper.setProjectPerm(users[2], "Project Administrator");
    }

    private void verifyNoWebpartsVisible()
    {
        //impersonate admin
        impersonate(users[2]);

        //verify no webparts present
        goToProjectHome();
//        assertTextNotPresent("Flow Scripts");  TODO

        stopImpersonating();
    }

    private void setUpFocusFolder()
    {
        /**
         * this folder has three parts, with permissions set (dependent on DUMMY_PROJECT
         * Flow Experiment managagement = Administrate
         * Flow Analyses = Edit
         * Flow Script = Read
         */
        _containerHelper.createProject(getProjectName(), "Collaboration");
        importFolderFromZip(getLabKeyRoot() + "/sampledata/webpartPerm/webPerms.folder.zip");
        //set all users to Reader so they have access to the folder
        _securityHelper.setSiteGroupPermissions("All Site Users", "Reader");
    }

    //This folder contains no data, but will create users and set them with specific permissions
    private void setUpDummyFolder()
    {
        //create users
        for(String user : users)
                createUser(user, null);
        //create dummy project
        _containerHelper.createProject(DUMMY_PROJECT_NAME, "Collaboration");

    }

    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
        deleteProject(DUMMY_PROJECT_NAME, false); // Project should be deleted during test

        deleteUsers(afterTest, users);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/core";
    }
}
