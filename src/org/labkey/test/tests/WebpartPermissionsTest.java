/*
 * Copyright (c) 2013-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.tests;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.fail;

@Category({DailyB.class})
public class WebpartPermissionsTest extends BaseWebDriverTest
{
    protected static final String DUMMY_PROJECT_NAME = "Dummy Project";
    protected static final String[] users = {"read_webpart@webpartpermissions.test", "edit_webpart@webpartpermissions.test", "admin_webpart@webpartpermissions.test"};
    @Override
    protected String getProjectName()
    {
        return "Webpart Perms project";
    }

    @Test
    public void testSteps()
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
            fail("Unable to delete dummy project");
        }
        assertTextNotPresent("Flow Script");


    }

    private void changeWebpartPermAndVerify()
    {
        goToProjectHome();
        goToProjectHome();
        String changedWebPart = "Flow Analyses";
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.setWebpartPermission(changedWebPart, "Update", DUMMY_PROJECT_NAME);

        impersonate(users[0]);
        goToProjectHome();
        assertTextNotPresent(changedWebPart);
        stopImpersonating();
    }

    private void changeWebpartPermToCurrentFolderAndVerify ()
    {
        goToProjectHome();
        String changedWebPart = "Flow Analyses";
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.setWebpartPermission(changedWebPart, "Read", null);

        //all users are readers, so the changed part will be visible now
        impersonate(users[0]);
        goToProjectHome();
        assertTextPresent(changedWebPart);
        stopImpersonating();

    }

    private void verifyCorrectWebpartsVisible()
    {
        String[] webparts = {"Flow Summary", "Flow Scripts", "Flow Analyses", "Flow Experiment Management"};
        verifyReadEditUpdate(webparts);
    }

    private void verifyReadEditUpdate(String[] webparts)
    {
        for(int i=0; i<3; i++)
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
        clickProject(DUMMY_PROJECT_NAME);
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
        importFolderFromZip(new File(TestFileUtils.getLabKeyRoot(), "/sampledata/webpartPerm/webPerms.folder.zip"));
        //set all users to Reader so they have access to the folder
        _securityHelper.setSiteGroupPermissions("All Site Users", "Reader");
    }

    //This folder contains no data, but will create users and set them with specific permissions
    private void setUpDummyFolder()
    {
        //create users
        for(String user : users)
            _userHelper.createUser(user);
        //create dummy project
        _containerHelper.createProject(DUMMY_PROJECT_NAME, "Collaboration");

    }

    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _containerHelper.deleteProject(DUMMY_PROJECT_NAME, false);

        deleteUsersIfPresent(users);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("core");
    }
}
