package org.labkey.test;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PipelineToolsHelper;
import org.openqa.selenium.WebDriver;

import java.util.List;

public class TestScrubber extends BaseWebDriverTest
{
    private WebDriver extraDriver;

    public TestScrubber(WebDriver extraDriver)
    {
        super();
        this.extraDriver = extraDriver;
    }

    @Override
    public WebDriver getDriver()
    {
        return extraDriver;
    }

    @LogMethod
    public void cleanSiteSettings()
    {
        try
        {
            simpleSignIn();

            try
            {
                // Get DB back in a good state after failed pipeline tools test.
                PipelineToolsHelper pipelineToolsHelper = new PipelineToolsHelper(this);
                pipelineToolsHelper.resetPipelineToolsDirectory();
            }
            catch (Exception e)
            {
                // Assure that this failure is noticed
                // Regression check: https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=10732
                log("**************************ERROR*******************************");
                log("** SERIOUS ERROR: Failed to reset pipeline tools directory. **");
                log("** Server may be in a bad state.                            **");
                log("** Set tools directory manually or bootstrap to fix.        **");
                log("**************************ERROR*******************************");
            }

            try
            {
                deleteSiteWideTermsOfUsePage();
            }
            catch (Exception e)
            {
                log("Failed to remove site-wide terms of use. This will likely cause other tests to fail.");
            }

            try
            {
                resetDbLoginConfig(); // Make sure to return DB config to its pre-test state.
            }
            catch (Exception e)
            {
                log("Failed to reset DB login config after test failure");
            }

            try
            {
                disableSecondaryAuthentication();
            }
            catch (Exception e)
            {
                log("Failed to reset Secondary Authentication after test failure");
            }
        }
        finally
        {
            extraDriver.quit();
        }
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
