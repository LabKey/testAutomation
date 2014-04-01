/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.test;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static org.labkey.test.TestProperties.isTestCleanupSkipped;
import static org.labkey.test.WebTestHelper.logToServer;

/**
 * This "test" performs the post-test checks and logging for all of
 * It exists so that BaseWebDriverTest can instantiate it in order to
 */
public class WebDriverTestPostamble extends BaseWebDriverTest
{
    public void postamble() throws Exception
    {
        if (!_anyTestCaseFailed && currentTest != null)
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
        else
        {
            log("Skipping post-test checks because a test case failed.");
        }

        if (!_anyTestCaseFailed && getDownloadDir().exists())
        {
            try{
                FileUtils.deleteDirectory(getDownloadDir());
            }
            catch (IOException ignore) { }
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
    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}
