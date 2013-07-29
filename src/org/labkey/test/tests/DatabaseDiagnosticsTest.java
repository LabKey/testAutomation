/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PipelineHelper;

/**
 * User: tchadick
 * Date: 1/15/13
 * Time: 12:32 PM
 */
@Category({DailyA.class, DailyB.class})
public class DatabaseDiagnosticsTest extends BaseWebDriverTest
{
    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        validateDomainsTest();
        databaseCheckTest();
    }

    @LogMethod
    private void validateDomainsTest()
    {
        goToAdmin();

        clickAndWait(Locator.linkWithText("Check Database"));

        click(Locator.linkWithText("Validate"));

        waitForElement(Locator.id("StatusFiles"));

        clickAndWait(PipelineHelper.Locators.pipelineStatusLink(0));

        waitForTextWithRefresh("Check complete", WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Check complete, 0 errors found");
    }

    @LogMethod
    private void databaseCheckTest()
    {
        goToAdmin();
        clickAndWait(Locator.linkWithText("Check Database"));
        clickAndWait(Locator.linkWithText("Do Database Check"));
        waitForText("Database Consistency checker complete", 60000);
        assertTextNotPresent("ERROR");
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
