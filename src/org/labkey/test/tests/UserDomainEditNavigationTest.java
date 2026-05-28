/*
 * Copyright (c) 2023-2026 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


    /*
        adds regression coverage for Issue 46738
        explicitly navigate between admin console, site users and user domain editor
    */
@Category({Daily.class})
public class UserDomainEditNavigationTest extends BaseWebDriverTest
{

    // override doCleanup, prevent base class from trying to clean up a project this test never creates
    @Override
    protected void doCleanup(boolean afterTest){}

    @Test
    public void testNavigateFromAdminConsole()
    {
        var console = goToAdminConsole();
        var userEditPage = console.clickChangeUserProperties();
        var adminConsoleUrl = getURL();
        checker().wrapAssertion(()-> assertThat(userEditPage.fieldsPanel().fieldNames())
                .as("expect any standard user fields to be present")
                .contains("FirstName", "LastName", "Description"));
        userEditPage.clickCancel();
        checker().verifyEquals("expect redirect back to admin console",
                adminConsoleUrl, getURL());
    }

    @Test
    public void testNavigateFromSiteUsers()
    {
        var siteUsers = goToSiteUsers();
        var siteUsersURL = getURL();
        var usersPropertyPage = siteUsers.clickChangeUserProperties();
        checker().wrapAssertion(()-> assertThat(usersPropertyPage.fieldsPanel().fieldNames())
                .as("expect any standard user fields to be present")
                .contains("FirstName", "LastName", "Description"));
        usersPropertyPage.clickCancel();
        checker().verifyEquals("expect redirect back to site users",
                siteUsersURL, getURL());
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
