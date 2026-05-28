/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.test.pages.study.specimen;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ShowCreateSpecimenRequestPage extends LabKeyPage<ShowCreateSpecimenRequestPage.ElementCache>
{
    public ShowCreateSpecimenRequestPage(WebDriver driver)
    {
        super(driver);
    }

    public static ShowCreateSpecimenRequestPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static ShowCreateSpecimenRequestPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("specimen", containerPath, "showCreateSpecimenRequest"));
        return new ShowCreateSpecimenRequestPage(webDriverWrapper.getDriver());
    }

    public ShowCreateSpecimenRequestPage setDetails(String... values)
    {
        for (int i = 0; i < values.length; i++)
        {
            String value = values[i];
            if (value != null)
            {
                setFormElement(Locator.id("input" + i), value);
            }
        }
        return this;
    }

    public LabKeyPage clickCreateAndReturnToSpecimens()
    {
        clickAndWait(elementCache().createAndReturnButton);

        return new LabKeyPage(getDriver());
    }

    public ManageRequestPage clickCreateAndViewDetails()
    {
        clickAndWait(elementCache().createAndViewButton);

        return new ManageRequestPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement createAndReturnButton = Locator.lkButton("Create and Return To Specimens").findWhenNeeded(this);
        WebElement createAndViewButton = Locator.lkButton("Create and View Details").findWhenNeeded(this);
        WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
    }
}
