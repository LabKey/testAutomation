/*
 * Copyright (c) 2009-2010 LabKey Corporation
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
package org.labkey.test.daily;

import org.labkey.test.bvt.SimpleApiTest;

import java.io.File;

/*
* User: dave
* Date: Sep 30, 2009
* Time: 10:57:41 AM
*/
public class SecurityApiTest extends SimpleApiTest
{
    protected static final String PROJECT_NAME = "Security API Test Project";
    private static final String USER_1 = "testuser1@securityapi.test";
    private static final String USER_2 = "testuser2@securityapi.test";
    private static final String GROUP_1 = "testgroup1";
    private static final String GROUP_2 = "testgroup2";

    protected File[] getTestFiles()
    {
        return new File[]{new File(getLabKeyRoot() + "/server/test/data/api/security-api.xml")};
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void runUITests() throws Exception
    {
        //setup the project, users and groups
        createUser(USER_1, null);
        createUser(USER_2, null);

        createProject(PROJECT_NAME);
        createPermissionsGroup(GROUP_1, USER_1);
        createPermissionsGroup(GROUP_2, USER_1, USER_2);
        setPermissions(GROUP_1, "Editor");
        setPermissions(GROUP_2, "Reader");
        exitPermissionsUI();
    }

    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME);} catch(Exception ignore) {}
        try {deleteUser(USER_1);} catch(Exception ignore) {}
        try {deleteUser(USER_2);} catch(Exception ignore) {}
        try {deleteUser("api-created-user@securityapi.test");} catch(Exception ignore) {}
    }
}
