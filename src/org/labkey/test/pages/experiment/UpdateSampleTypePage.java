/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.test.pages.experiment;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class UpdateSampleTypePage extends CreateSampleTypePage
{
    public UpdateSampleTypePage(WebDriver driver)
    {
        super(driver);
        // The parent page CreateSampleTypePage has a wait in the constructor.
    }

    public static UpdateSampleTypePage beginAt(WebDriverWrapper driver, Integer sampleTypeId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), sampleTypeId);
    }

    public static UpdateSampleTypePage beginAt(WebDriverWrapper driver, String containerPath, Integer sampleTypeId)
    {
        driver.beginAt(WebTestHelper.buildURL("experiment", containerPath, "editSampleType", Maps.of("RowId", String.valueOf(sampleTypeId))));
        return new UpdateSampleTypePage(driver.getDriver());
    }

    public ModalDialog clickSaveExpectingAlert()
    {
        elementCache().saveButton.click();
        return new ModalDialog.ModalDialogFinder(getDriver()).timeout(1000).waitFor();
    }
}
