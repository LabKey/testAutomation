/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.LogMethod;

import java.io.File;

import static org.junit.Assert.*;

@Category({DailyA.class})
public class StudyReloadTest extends StudyBaseTest
{
    @Override
    //disabled for 14569
    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        initializeFolder();
        importStudyFromZip(new File(getSampledataPath(), "studyreload/original.zip"));
    }

    @Override
    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void doVerifySteps()
    {
        reloadStudyFromZip(new File(getSampledataPath(), "/studyreload/edited.zip"));
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("1 dataset"));
        clickAndWait(Locator.linkWithText("update_test"));
        assertTextPresent("id006", "additional_column", "1234566");
        //text that was present in original but removed in the update
        assertTextNotPresent("original_column_numeric");

        verifyProtectedColumn();
    }

    private void verifyProtectedColumn()
    {
        clickButtonContainingText("Manage Dataset");
        clickButtonContainingText("Edit Definition");
        Locator.NameLocator ff_name1 = Locator.name("ff_name1");
        waitForElement(ff_name1);
        click(ff_name1);
        click(Locator.linkContainingText("Advanced"));
        assertEquals("on", getAttribute(Locator.name("protected"), "value"));
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
