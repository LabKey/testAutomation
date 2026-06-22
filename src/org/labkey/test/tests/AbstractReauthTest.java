/*
 * Copyright (c) 2018-2026 LabKey Corporation
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

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.pages.test.TestReauthPage;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public abstract class AbstractReauthTest extends BaseWebDriverTest
{
    public record User(String email, String password) { }

    private final User user1;
    private final User user2;

    protected AbstractReauthTest(User user1, User user2)
    {
        this.user1 = user1;
        this.user2 = user2;
    }

    protected abstract void clickSignIn();
    protected abstract void authenticate(String email, String password);
    protected abstract void authenticateExpectingError(String email, String password);
    protected abstract String getAuthDescription();

    @Test
    public void testReauth()
    {
        signInAs(user1);

        TestReauthPage testReauthPage = TestReauthPage.beginAt(this);
        assertEquals("Authentication method", getAuthDescription(), testReauthPage.getDescription());
        testReauthPage.clickReauth();
        authenticate(user1.email, user1.password);
        String reauthToken1 = testReauthPage.getReauthToken();
        assertEquals("Authentication method", getAuthDescription(), testReauthPage.getDescription());
        testReauthPage.validateToken();

        // Reauth again as the same user to ensure a new token is generated
        testReauthPage = TestReauthPage.beginAt(this);
        testReauthPage.clickReauth();
        authenticate(user1.email, user1.password);
        String reauthToken2 = testReauthPage.getReauthToken();
        assertNotEquals("Reauth should generate a new token each time", reauthToken1, reauthToken2);
        testReauthPage.validateToken();
    }

    @Test
    public void testReuseReauthToken()
    {
        signInAs(user1);

        TestReauthPage testReauthPage = TestReauthPage.beginAt(this);
        testReauthPage.clickReauth();
        authenticate(user1.email, user1.password);
        String reauthToken = testReauthPage.getReauthToken();
        testReauthPage.validateToken();

        testReauthPage = TestReauthPage.beginAt(this, reauthToken);
        testReauthPage.validateTokenExpectingError();
        assertElementPresent(Locator.byClass("labkey-error-heading").withText("Reauthentication validation failed!"));
    }

    @Test
    public void testReauthAsWrongUser()
    {
        signInAs(user1);

        TestReauthPage testReauthPage = TestReauthPage.beginAt(this);
        testReauthPage.clickReauth();
        authenticate(user2.email, user2.password);
        Assertions.assertThat(testReauthPage.getReauthError()).as("Reauth error").contains("wrong user reauthenticated");

        testReauthPage.clickReauth(); // Try again
        authenticate(user1.email, user1.password);
        testReauthPage.validateToken();
    }

    /**
     * Test that reauth works when fixing the password after logging in with the wrong password
     */
    @Test
    public void testReauthWithBadPassword()
    {
        signInAs(user1);

        TestReauthPage testReauthPage = TestReauthPage.beginAt(this);
        testReauthPage.clickReauth();
        authenticateExpectingError(user1.email, user1.password + "wrong");

        authenticate(user1.email, user1.password);
        testReauthPage.validateToken();
    }

    private void signInAs(User user)
    {
        signOut();
        clickSignIn();
        authenticate(user.email, user.password);
        assertSignedInAs(user);
    }

    private void assertSignedInAs(User user)
    {
        if (!waitFor(() -> getCurrentUser().equals(user.email), 1_000))
            assertEquals("Signed in as", user.email, getCurrentUser());
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
