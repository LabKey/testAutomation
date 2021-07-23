package org.labkey.test.tests;

import org.hamcrest.CoreMatchers;
import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyC;
import org.labkey.test.pages.LabkeyErrorPage;

import java.util.List;

@Category({DailyC.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public class LabkeyErrorPageTest extends BaseWebDriverTest
{
    private static final String READER_USER = "reader_user@user.test";
    private static final String PROJECT_NAME = "Labkey Error Page Test";

    @BeforeClass
    public static void setupProject()
    {
        LabkeyErrorPageTest init = (LabkeyErrorPageTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        createUserWithPermissions(READER_USER, getProjectName(), "Reader");
    }

    @Test
    public void testGeneralErrors()
    {
        String imageTitle = "notFound_error.svg";
        log("Verifying error message with mothership controller and action");
        beginAt(WebTestHelper.buildURL("test", "NotFound"));
        LabkeyErrorPage errorPage = new LabkeyErrorPage(getDriver());

        checker().verifyEquals("Incorrect error heading message", "Oops! The requested page cannot be found.",
                errorPage.getErrorHeading());
        checker().verifyEquals("Incorrect error sub-heading message", "404: page not found.",
                errorPage.getSubErrorHeading());
        checker().verifyThat("Incorrect error image", errorPage.getErrorImage(), CoreMatchers.containsString(imageTitle));

        beginAt(WebTestHelper.buildRelativeUrl("project", getCurrentContainerPath(), "beginning"));
        errorPage = new LabkeyErrorPage(getDriver());
        checker().verifyEquals("Incorrect error heading message", "Oops! The requested page cannot be found.",
                errorPage.getErrorHeading());
        checker().verifyEquals("Incorrect error sub-heading message", "Unable to find action 'beginning' to handle request in controller 'project'.",
                errorPage.getSubErrorHeading());

        beginAt(WebTestHelper.buildRelativeUrl("projects", getCurrentContainerPath(), "begin"));
        errorPage = new LabkeyErrorPage(getDriver());
        checker().verifyEquals("Incorrect error heading message", "Oops! The requested page cannot be found.",
                errorPage.getErrorHeading());
        checker().verifyEquals("Incorrect error sub-heading message", "No LabKey Server module registered to handle request for controller: projects.",
                errorPage.getSubErrorHeading());

    }

    @Test
    public void testPermissionErrors()
    {
        String imageTitle = "permission_error.svg";

        goToProjectHome();
        impersonate(READER_USER);
        beginAt(WebTestHelper.buildURL("test", "PermUpdate"));
        LabkeyErrorPage errorPage = new LabkeyErrorPage(getDriver());

        checker().verifyEquals("Incorrect error heading message", "Oops! An error has occurred.",
                errorPage.getErrorHeading());
        checker().verifyEquals("Incorrect error sub-heading message", "User does not have permission to perform this operation.",
                errorPage.getSubErrorHeading());
        checker().verifyThat("Incorrect error image", errorPage.getErrorImage(), CoreMatchers.containsString(imageTitle));

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
        checker().verifyEquals("Incorrect error instructions", "Please report this bug to LabKey Support by copying " +
                        "and pasting both your unique reference code and the full stack trace in the View Details section below.",
                errorPage.getErrorInstruction());
        checker().verifyThat("Incorrect error image", errorPage.getErrorImage(), CoreMatchers.containsString(imageTitle));

        checkExpectedErrors(3);
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
