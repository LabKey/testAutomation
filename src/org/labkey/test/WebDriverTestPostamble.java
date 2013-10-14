package org.labkey.test;

import org.jetbrains.annotations.Nullable;

import static org.labkey.test.TestProperties.isTestCleanupSkipped;
import static org.labkey.test.WebTestHelper.logToServer;

/**
 * User: tchadick
 * Date: 10/4/13
 * This "test" performs the post-test checks and logging for all of
 * It exists so that BaseWebDriverTest can instantiate it in order to
 */
public class WebDriverTestPostamble extends BaseWebDriverTest
{
    public void postamble() throws Exception
    {
        try
        {
            if (currentTest != null)
            {
                //make sure you're signed in as admin, because this won't work otherwise
                ensureSignedInAsAdmin();

                checkQueries();

                checkViews();

                if(!isPerfTest)
                    checkActionCoverage();

                checkLinks();

                if (!isTestCleanupSkipped() && currentTest != null)
                {
                    goToHome();
                    currentTest.doCleanup(true);
                }
                else
                {
                    log("Skipping test cleanup as requested.");
                }

                if (!"DRT".equals(System.getProperty("suite")) || Runner.isFinalTest())
                {
                    checkLeaksAndErrors();
                }

                checkJsErrors();

                logToServer("=== Completed " + currentTest.getClass().getSimpleName() + Runner.getProgress() + " ===");

                log("=============== Completed " + currentTest.getClass().getSimpleName() + Runner.getProgress() + " =================");
            }
            else
            {
                logToServer("=== Completed Test - Setup failed " + Runner.getProgress() + " ===");

                log("=============== Completed Test - Setup failed " + Runner.getProgress() + " =================");
            }
        }
        finally
        {
            currentTest = null;
        }
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        throw new RuntimeException("This is not a test.");
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}
