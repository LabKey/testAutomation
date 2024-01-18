/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.pages.assay;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

import java.io.File;

/**
 * Import page for test assay upload page (miniassay module)
 */
public class DataImportPage extends LabKeyPage
{
    public DataImportPage(WebDriver driver)
    {
        super(driver);
    }

    public void uploadFile(File dataFile)
    {
        setFormElement(Locator.id("upload-run-field-file"), dataFile);
        Locator.tagWithId("a","btnSaveBatch")
                .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT).click();
        waitForElement(Locator.linkWithText(dataFile.getName()));   // the link will be in a dataRegion below
    }

    public DataImportPage setField(String fieldId, String value)
    {
        setFormElement(Locator.id(fieldId), value);
        return this;
    }
}
