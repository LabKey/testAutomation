/*
 * Copyright (c) 2026 LabKey Corporation
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

import org.labkey.test.Locator;

/**
 * Base class for reauth tests that authenticate via the standard LabKey username/password form,
 * as opposed to SSO providers (SAML, CAS) that redirect to an external identity provider.
 */
public abstract class AbstractLoginFormReauthTest extends AbstractReauthTest
{
    protected AbstractLoginFormReauthTest(User user1, User user2)
    {
        super(user1, user2);
    }

    @Override
    protected void clickSignIn()
    {
        clickAndWait(Locator.tagWithClass("a", "header-link").withText("Sign In"));
    }

    @Override
    protected void authenticate(String email, String password)
    {
        doAndWaitForPageToLoad(() -> fillSignInFormAndSubmit(null, email, password));
    }

    @Override
    protected void authenticateExpectingError(String email, String password)
    {
        fillSignInFormAndSubmit(null, email, password);
    }
}
