/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.InDevelopment;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({InDevelopment.class})
public class SecurityLDAPTest extends BaseWebDriverTest
{
    protected static final String LDAP_USER = "tester1@test.labkey.local";
    protected static final String LDAP_USER_PASSWORD = "yeahforDocker1";

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("core");
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    /*
     * preconditions:  LDAP Service is running and reachable. Labkey has been configured to use LDAP authentication. Test user credentials have been created on LDAP server.
     * post conditions:
     */
    @Test
    public void loginLDAPAutoCreateEnabledTest()
    {
        // prep: ensure that user does not currently exist in labkey and ldap user auto-create is enabled
        deleteUsersIfPresent(LDAP_USER);
        try
        {
            int getResponse = WebTestHelper.getHttpResponse(WebTestHelper.getBaseURL() + "/login/setAuthenticationParameter.view?parameter=AutoCreateAccounts&enabled=true").getResponseCode();
            assertEquals("failed to set authentication param to enable ldap user auto-create via http get", 200, getResponse);
        }
        catch (Exception e){
            throw new RuntimeException("failed to enable ldap user auto-create", e);
        }
        signOut();

        // test: attempt login via ldap and confirm new user landing page
        signIn(LDAP_USER, LDAP_USER_PASSWORD);
        assertTextPresent("Please enter your contact information.");
        assertElementPresent(Locator.tagWithId("form", "Users"));
        signOut();

        // cleanup: delete test user
        signIn();
        deleteUsersIfPresent(LDAP_USER);
    }

    /*
     * preconditions:  LDAP Service is running and reachable. Labkey has been configured to use LDAP authentication. Test user credentials have been created on LDAP server.
     * post conditions:
     */
    @Test
    public void loginLDAPAutoCreateDisabledTest()
    {
        // prep: ensure that user does not currently exist in labkey and  auto-create is disabled
        deleteUsersIfPresent(LDAP_USER);
        try
        {
            int getResponse = WebTestHelper.getHttpResponse(WebTestHelper.getBaseURL() + "/login/setAuthenticationParameter.view?parameter=AutoCreateAccounts&enabled=false").getResponseCode();
            assertEquals("failed to set authentication param to disable ldap user auto-create via http get", 200, getResponse);
        }
        catch (Exception e){
            throw new RuntimeException("failed to disable ldap user auto-create", e);
        }
        signOut();

        // test: attempt login via ladap and confirm message displayed on login screen
        attemptSignIn(LDAP_USER, LDAP_USER_PASSWORD);
        assertTitleEquals("Sign In");
        assertTextPresent("to have your account created.");
        assertElementPresent(Locator.tagWithName("form", "login"));

        // cleanup: sign admin back in
        signIn();
    }
}
