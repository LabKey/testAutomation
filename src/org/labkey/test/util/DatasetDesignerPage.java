/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.pages.BaseDesignerPage;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;

public class DatasetDesignerPage extends BaseDesignerPage
{
    private Elements _elements;

    public DatasetDesignerPage(WebDriver driver)
    {
        super(driver);
        _elements = new Elements();
    }

    public void waitForReady()
    {
        super.waitForReady();
        waitForElement(Locator.name("description"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public LabKeyPage saveAndClose()
    {
        save();
        return null;
    }

    @Override
    public LabKeyPage save()
    {
        clickButton("Save");
        return null;
    }

    public void checkDemographicData()
    {
        checkCheckbox(elements().demographicCheckbox);
    }

    public void uncheckDemographicData()
    {
        uncheckCheckbox(elements().demographicCheckbox);
    }

    public void shareDemographics(ShareDemographicsBy by)
    {
        selectOptionByValue(elements().sharedBy, by.toString());
    }

    public void inferFieldsFromFile(File file)
    {
        clickButton("Infer Fields from File", 0);
        WebElement dialog =
                Locator.tagWithClass("div", "gwt-DialogBox")
                .withDescendant(Locator.tagWithClass("div", "Caption").withText("Infer Fields from File"))
                .waitForElement(shortWait());
        WebElement radio = Locator.radioButtonByNameAndValue("source", "file").findElement(dialog);
        radio.click();
        WebElement fileField = Locator.tagWithName("input", "uploadFormElement").findElement(dialog);
        setFormElement(fileField, file);
        WebElement submitButton = Locator.lkButton("Submit").findElement(dialog);
        submitButton.click();
        shortWait().until(ExpectedConditions.stalenessOf(dialog));
    }


    public enum ShareDemographicsBy
    {NONE, PTID}

    public Elements elements()
    {
        return _elements;
    }

    private class Elements extends ComponentElements
    {
        @Override
        protected SearchContext getContext()
        {
            return getDriver();
        }

        private WebElement demographicCheckbox = new LazyWebElement(Locator.name("demographicData"), this);
        private WebElement sharedBy = new LazyWebElement(Locator.name("demographicsSharedBy"), this);
    }
}
