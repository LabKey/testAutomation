package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;

@Category({DailyB.class})
public class ProjectTermsOfUseTest extends BaseTermsOfUseTest
{
    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsersIfPresent(USER);
        log("Deleting test projects");

        deleteProject(PUBLIC_TERMS_PROJECT_NAME, false);
        deleteProject(NON_PUBLIC_TERMS_PROJECT_NAME, false);
        deleteProject(NON_PUBLIC_TERMS_PROJECT2_NAME, false);

        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        ProjectTermsOfUseTest init = (ProjectTermsOfUseTest) getCurrentTest();

        init.doSetup();
    }

    @Override
    protected void doSetup()
    {
        super.doSetup();
    }

    @Test
    public void projectTermsOfUseTest()
    {
        createUser(USER);

        createProjectWithTermsOfUse(PUBLIC_TERMS_PROJECT_NAME, "The first rule of fight club is do not talk about fight club.", true);
        createProjectWithTermsOfUse(NON_PUBLIC_TERMS_PROJECT_NAME, "The second rule of fight club is do not talk about fight club.", false);
        createProjectWithTermsOfUse(NON_PUBLIC_TERMS_PROJECT2_NAME, "The third rule of fight club is do not talk about fight club.", false);

        createWikiTabForProject(NON_PUBLIC_TERMS_PROJECT_NAME);
        pushLocation();

        goToHome();
        _containerHelper.createSubfolder(NON_PUBLIC_TERMS_PROJECT_NAME, "subfolder", (String[]) null);
        pushLocation(); // For attempting to bypass Terms of Use

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

        signIn();
        log("Attempt to bypass terms with saved URLs");
        popLocation();
        assertTextPresent(PROJECT_TERMS_SNIPPET); // PROJECT_NAME
        popLocation();
        assertTextPresent(PROJECT_TERMS_SNIPPET); // PUBLIC_TERMS_PROJECT_NAME
        popLocation();
        assertTextPresent(PROJECT_TERMS_SNIPPET); // NON_PUBLIC_TERMS_PROJECT_NAME
        popLocation();
        assertTextPresent(PROJECT_TERMS_SNIPPET); // NON_PUBLIC_TERMS_PROJECT_NAME/subfolder

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

