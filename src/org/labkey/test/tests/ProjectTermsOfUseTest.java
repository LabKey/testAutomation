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
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;

import static org.junit.Assert.assertEquals;

@Category({DailyB.class})
public class ProjectTermsOfUseTest extends BaseTermsOfUseTest
{
    @Override
    protected void doSetup()
    {
        super.doSetup();

        createWikiTabForProject(NON_PUBLIC_TERMS_PROJECT_NAME);
        _containerHelper.createSubfolder(NON_PUBLIC_TERMS_PROJECT_NAME, "subfolder", (String[]) null);
    }

    @Test
    public void projectTermsOfUseTest()
    {
        log("Terms don't come into play until you log out");
        clickProject(NON_PUBLIC_TERMS_PROJECT2_NAME);
        assertTextNotPresent("fight club");
        signOut();

        log("Access project with guest user");
        clickProject(PUBLIC_TERMS_PROJECT_NAME, false);
        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);

        goToHome();
        clickProject(PUBLIC_TERMS_PROJECT_NAME);
        assertTextNotPresent(PROJECT_TERMS_SNIPPET);

        // simulate a session expiration and make sure you can still log in to a project with terms.
        signOut();
        beginAt(WebTestHelper.buildURL("login", "login", Maps.of("returnUrl", "/labkey/project/" + PUBLIC_TERMS_PROJECT_NAME + "/begin.view?")));
        attemptSignIn(PasswordUtil.getUsername(), PasswordUtil.getPassword());
        waitForElement(Locators.labkeyError.containing("you must log in and approve the terms of use"));
        assertTextPresent(PROJECT_TERMS_SNIPPET);
        checkCheckbox(Locators.termsOfUseCheckbox().findElement(getDriver()));
        clickAndWait(Locator.css(".signin-btn"));
        assertEquals("Wrong project after terms redirect", PUBLIC_TERMS_PROJECT_NAME, getCurrentProject());

        log("Attempt to bypass terms with saved URLs");
        beginAt(WebTestHelper.buildURL("project", PUBLIC_NO_TERMS_PROJECT_NAME, "begin"));
        assertElementNotPresent(Locators.termsOfUseCheckbox());
        assertTextNotPresent(PROJECT_TERMS_SNIPPET);

        beginAt(WebTestHelper.buildURL("query", NON_PUBLIC_TERMS_PROJECT_NAME, "begin"));
        waitForElement(Locators.termsOfUseCheckbox());
        assertElementPresent(Locator.lkButton("Agree"));
        assertTextPresent(PROJECT_TERMS_SNIPPET);

        beginAt(WebTestHelper.buildURL("experiment", NON_PUBLIC_TERMS_PROJECT_NAME + "/subfolder", "begin"));
        waitForElement(Locators.termsOfUseCheckbox());
        assertElementPresent(Locator.lkButton("Agree"));
        assertTextPresent(PROJECT_TERMS_SNIPPET);

        beginAt(WebTestHelper.buildURL("announcements", NON_PUBLIC_TERMS_PROJECT2_NAME, "begin"));
        waitForElement(Locators.termsOfUseCheckbox());
        assertElementPresent(Locator.lkButton("Agree"));
        assertTextPresent(PROJECT_TERMS_SNIPPET);

        beginAt(WebTestHelper.buildURL("project", PUBLIC_TERMS_PROJECT_NAME, "begin"));
        assertElementNotPresent(Locators.termsOfUseCheckbox());
        assertTextNotPresent(PROJECT_TERMS_SNIPPET);

        goToHome();
        clickProject(NON_PUBLIC_TERMS_PROJECT2_NAME, false);
        assertTextPresent(PROJECT_TERMS_SNIPPET);
        log("Submit without agreeing");
        clickButton("Agree");
        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);

        clickProject(NON_PUBLIC_TERMS_PROJECT_NAME, false);
        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);

        log("Check terms with impersonated user");
        clickProject(NON_PUBLIC_TERMS_PROJECT2_NAME, false);
        impersonate(USER);

        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);

        clickProject(PUBLIC_TERMS_PROJECT_NAME, false);
        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);

        clickProject(NON_PUBLIC_TERMS_PROJECT_NAME, false);
        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);

        stopImpersonating();
        clickProject(PUBLIC_TERMS_PROJECT_NAME, false);
        acceptTermsOfUse(PROJECT_TERMS_SNIPPET, true);

        log("Delete terms of use wiki page");
        clickProject(NON_PUBLIC_TERMS_PROJECT_NAME);
        clickTab("Wiki");
        clickAndWait(Locator.linkWithText("Edit"));
        deleteWikiPage();
        assertTextNotPresent(WIKI_TERMS_TITLE);
    }

    protected void deleteWikiPage()
    {
        waitForElementToDisappear(Locator.xpath("//a[contains(@class, 'disabled')]/span[text()='Delete Page']"), WAIT_FOR_JAVASCRIPT);
        clickButton("Delete Page");
        clickButton("Delete");
    }
}

