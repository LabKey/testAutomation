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
package org.labkey.test.pages.experiment;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class InsertDataClassPage extends LabKeyPage<InsertDataClassPage.ElementCache>
{
    public InsertDataClassPage(WebDriver driver)
    {
        super(driver);
    }

    public static InsertDataClassPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static InsertDataClassPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("experiment", containerPath, "insertDataClass"));
        return new InsertDataClassPage(driver.getDriver());
    }

    public InsertDataClassPage setName(String name)
    {
        elementCache().nameInput.set(name);
        return this;
    }

    public InsertDataClassPage setDescription(String desc)
    {
        elementCache().descriptionInput.set(desc);
        return this;
    }

    public InsertDataClassPage setNameExpression(String nameExp)
    {
        elementCache().nameExpressionInput.set(nameExp);
        return this;
    }

    public InsertDataClassPage selectMaterialSourceId(Integer matSrcId)
    {
        elementCache().materialSourceSelect.selectByValue(matSrcId.toString());
        return this;
    }

    public InsertDataClassPage selectMaterialSourceName(String matSrcName)
    {
        elementCache().materialSourceSelect.selectByVisibleText(matSrcName);
        return this;
    }

    public void clickCreate()
    {
        clickAndWait(elementCache().createBtn);
    }
    public void clickCancel()
    {
        clickAndWait(elementCache().cancelBtn);
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Input nameInput = Input.Input(Locator.tagWithName("input","name"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);
        Input descriptionInput = Input.Input(Locator.tagWithName("input","description"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);
        Input nameExpressionInput = Input.Input(Locator.tagWithName("input","nameExpression"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);
        Select materialSourceSelect = SelectWrapper.Select(Locator.tagWithName("select","materialSourceId"))
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);

        WebElement createBtn = Locator.lkButton("Create").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement cancelBtn = Locator.lkButton("Cancel").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
    }
}
