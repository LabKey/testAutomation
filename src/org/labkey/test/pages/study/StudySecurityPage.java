/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.StudyHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class StudySecurityPage extends LabKeyPage<StudySecurityPage.ElementCache>
{
    public StudySecurityPage(WebDriver driver)
    {
        super(driver);
    }

    public static StudySecurityPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static StudySecurityPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("study-security", containerPath, "begin"));
        return new StudySecurityPage(driver.getDriver());
    }

    public StudySecurityPage setSecurityType(StudyHelper.SecurityMode securityType)
    {
        elementCache().securityType.selectByValue(securityType.toString());
        clickAndWait(elementCache().updateTypeButton);
        clearCache();
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Select securityType = SelectWrapper.Select(Locator.name("securityString")).findWhenNeeded(this);
        WebElement updateTypeButton = Locator.lkButton("Update Type").findWhenNeeded(this);
    }
}