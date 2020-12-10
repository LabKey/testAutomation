package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyC;
import org.labkey.test.pages.LabkeyErrorPage;

import java.util.List;

@Category({DailyC.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public class LabkeyErrorPageTest extends BaseWebDriverTest
{
    private static final String EDITOR_USER = "editor_user@user.test";
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
        createUserWithPermissions(EDITOR_USER, getProjectName(), "Editor");
        createUserWithPermissions(READER_USER, getProjectName(), "Reader");
    }

    @Test
    public void testGeneralErrors()
    {
        String imageTitle = "notFound_error.svg";
        log("Verifying error message with mothership controller and action");
        beginAt(WebTestHelper.buildURL("mothership", "ThrowNotFoundException"));
        LabkeyErrorPage errorPage = new LabkeyErrorPage(getDriver());

        checker().verifyEquals("Incorrect error heading message","Oops! The requested page cannot be found.",
                errorPage.getErrorHeading());
        checker().verifyEquals("Incorrect error sub-heading message","This is a test message for not found exception.",
                errorPage.getSubErrorHeading());
        checker().verifyTrue("Incorrect error image",errorPage.getErrorImage().contains(imageTitle));

        goToProjectHome();
        beginAt(WebTestHelper.buildRelativeUrl("project",getCurrentContainerPath(),"beginning"));
        errorPage = new LabkeyErrorPage(getDriver());
        checker().verifyEquals("Incorrect error heading message","Oops! The requested page cannot be found.",
                errorPage.getErrorHeading());
        checker().verifyEquals("Incorrect error sub-heading message","Unable to find action 'beginning' to handle request in controller 'project'.",
                errorPage.getSubErrorHeading());

    }

    @Test
    public void testPermissionErrors()
    {
        String imageTitle = "permission_error.svg";
        log("Verifying permission error message with mothership action");
        beginAt(WebTestHelper.buildURL("mothership", "ThrowPermissionException"));
        LabkeyErrorPage errorPage = new LabkeyErrorPage(getDriver());

        checker().verifyEquals("Incorrect error heading message","Oops! An error has occurred.",
                errorPage.getErrorHeading());
        checker().verifyEquals("Incorrect error sub-heading message","You do not have the permissions required to access this page.",
                errorPage.getSubErrorHeading());
        checker().verifyTrue("Incorrect error image",errorPage.getErrorImage().contains(imageTitle));

    }

    @Test
    public void testServerConfigurationErrors()
    {
        String imageTitle = "configuration_error.svg";
        log("Verifying configuration error message with mothership action");
        beginAt(WebTestHelper.buildURL("mothership", "ThrowConfigurationException"));
        LabkeyErrorPage errorPage = new LabkeyErrorPage(getDriver());

        checker().verifyEquals("Incorrect error heading message","Oops! A server configuration error has occurred.",
                errorPage.getErrorHeading());
        checker().verifyEquals("Incorrect error sub-heading message","The requested page cannot be found. This is a test message for configuration exception.",
                errorPage.getSubErrorHeading());
        checker().verifyTrue("Incorrect error image",errorPage.getErrorImage().contains(imageTitle));

        checkExpectedErrors(1);
    }

    @Test
    public void testExecutionErrors()
    {
        String imageTitle = "code_error.svg";
        log("Verifying execution error message with mothership action");
        beginAt(WebTestHelper.buildURL("mothership", "ThrowExecutionException"));
        LabkeyErrorPage errorPage = new LabkeyErrorPage(getDriver());

        checker().verifyEquals("Incorrect error heading message","Oops! An error has occurred.",
                errorPage.getErrorHeading());
        checker().verifyEquals("Incorrect error sub-heading message","This is a test message for execution exception",
                errorPage.getSubErrorHeading());
        checker().verifyTrue("Incorrect error image",errorPage.getErrorImage().contains(imageTitle));

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
