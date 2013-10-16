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
        if (currentTest != null)
        {
            //make sure you're signed in as admin, because this won't work otherwise
            ensureSignedInAsAdmin();

            checkQueries();

            checkViews();

            if(!isPerfTest)
                checkActionCoverage();

            checkLinks();

            if (!isTestCleanupSkipped())
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
        }

        logToServer("=== Completed " + Runner.getCurrentTestName() + Runner.getProgress() + " ===");

        log("=============== Completed " + Runner.getCurrentTestName() + Runner.getProgress() + " =================");
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
