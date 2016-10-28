/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.test.pages.issues;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class UpdatePage extends BaseUpdatePage<BaseUpdatePage.ElementCache>
{
    public UpdatePage(WebDriver driver)
    {
        super(driver);
    }

    public static UpdatePage beginAt(WebDriverWrapper driver, String issueId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueId);
    }

    public static UpdatePage beginAt(WebDriverWrapper driver, String containerPath, String issueId)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "update", Maps.of("issueId", issueId)));
        return new UpdatePage(driver.getDriver());
    }

    @Override
    public DetailsPage save()
    {
        clickAndWait(elementCache().saveButton);
        return new DetailsPage(getDriver());
    }

    @Override
    public OptionSelect assignedTo()
    {
        return (OptionSelect) super.assignedTo();
    }
}