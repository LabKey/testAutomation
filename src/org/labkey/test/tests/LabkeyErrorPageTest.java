package org.labkey.test.tests;

import org.hamcrest.CoreMatchers;
import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.LabkeyErrorPage;

import java.util.List;

import static org.labkey.test.util.PermissionsHelper.READER_ROLE;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public class LabkeyErrorPageTest extends BaseWebDriverTest
{
    private static final String READER_USER = "reader_user@user.test";

    @BeforeClass
    public static void setupProject()
    {
        LabkeyErrorPageTest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        createUserWithPermissions(READER_USER, getProjectName(), READER_ROLE);
    }

    @Test
    public void testGeneralErrors()
    {
        String imageTitle = "notFound_error.svg";
        log("Verifying error message with mothership controller and action");
        beginAt(WebTestHelper.buildURL("test", "NotFound"));
        LabkeyErrorPage errorPage = new LabkeyErrorPage(getDriver());

        checker().verifyEquals("Incorrect error heading message", "404: page not found",
                errorPage.getErrorHeading());
        checker().verifyThat("Incorrect error image", errorPage.getErrorImage(), CoreMatchers.containsString(imageTitle));
        checker().verifyTrue("'Show Details' button should appear on not found error page",
                errorPage.isShowDetailsPresent());

        beginAt(WebTestHelper.buildRelativeUrl("project", getCurrentContainerPath(), "beginning"));
        errorPage = new LabkeyErrorPage(getDriver());
        checker().verifyEquals("Incorrect error heading message", "Unable to find action 'beginning' to handle request in controller 'project'",
                errorPage.getErrorHeading());

        beginAt(WebTestHelper.buildRelativeUrl("projects", getCurrentContainerPath(), "begin"));
        errorPage = new LabkeyErrorPage(getDriver());
        checker().verifyEquals("Incorrect error heading message", "No LabKey Server module registered to handle request for controller: projects",
                errorPage.getErrorHeading());

    }

    @Test
    public void testPermissionErrors()
    {
        goToProjectHome();
        impersonate(READER_USER);
        beginAt(WebTestHelper.buildURL("test", "PermUpdate"));

        LabkeyErrorPage errorPage = new LabkeyErrorPage(getDriver());
        errorPage.assertUnauthorized(checker());

        errorPage.clickViewDetails();
        scrollIntoView(Locator.button("Stop Impersonating"));
        checker().verifyEquals("Incorrect view details content", "You are currently impersonating: reader user\nStop Impersonating",
                errorPage.getViewDetailsSubDetails());
        stopImpersonating();
    }

    @Test
    public void testServerConfigurationErrors()
    {
        String imageTitle = "configuration_error.svg";
        log("Verifying configuration error message with mothership action");
        beginAt(WebTestHelper.buildURL("test", "ConfigurationException"));
        LabkeyErrorPage errorPage = new LabkeyErrorPage(getDriver());

        checker().verifyEquals("Incorrect error heading message", "Oops! A server configuration error has occurred.",
                errorPage.getErrorHeading());
        checker().verifyEquals("Incorrect error sub-heading message", "The requested page cannot be found. You have a configuration problem.",
                errorPage.getSubErrorHeading());
        checker().verifyThat("Incorrect error image", errorPage.getErrorImage(), CoreMatchers.containsString(imageTitle));
        checker().verifyTrue("'Show Details' button should appear on configuration error page",
                errorPage.isShowDetailsPresent());

        checkExpectedErrors(1);
    }

    @Test
    public void testExecutionErrors()
    {
        String imageTitle = "code_error.svg";
        log("Verifying execution error message with mothership action");
        beginAt(WebTestHelper.buildURL("test", "npe"));
        LabkeyErrorPage errorPage = new LabkeyErrorPage(getDriver());

        checker().verifyEquals("Incorrect error heading message", "Oops! An error has occurred.",
                errorPage.getErrorHeading());
        checker().verifyEquals("Incorrect error instructions", "You can find help resources here and may " +
                "find troubleshooting hints by reading the full stack trace in the server logs.",
                errorPage.getErrorInstruction());
        checker().verifyThat("Incorrect error image", errorPage.getErrorImage(), CoreMatchers.containsString(imageTitle));
        checker().verifyFalse("'Show Details' button should not appear on execution error page",
                errorPage.isShowDetailsPresent());

        checkExpectedErrors(2);
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return  "LabkeyErrPageTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
