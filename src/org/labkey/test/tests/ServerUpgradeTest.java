/*
 * Copyright (c) 2025-2026 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.List;

// Don't run as a git test, explicitly add this test to a suite file.
@Category({})
public class ServerUpgradeTest extends BaseWebDriverTest
{

    @Test
    public void testErrorLog()
    {
        String serverErrors = getServerErrors().trim();

        checker().verifyTrue("There should be no server errors after upgrade. Found: " + serverErrors,
                serverErrors.isEmpty());
    }

    @Test
    public void testMissingModules()
    {
        beginAt(WebTestHelper.buildURL("admin", "modules"));
        List<WebElement> panels = Locator.tagWithClass("div", "panel-portal").findElements(getDriver());

        String panelText = panels.get(1).getText();
        checker().verifyTrue(String.format("It looks like there are unknown modules after upgrade: %s", panelText),
                panelText.contains("This server has no unknown modules."));

    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return "Server Upgrade Test";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("Platform");
    }

}
