/*
 * Copyright (c) 2014-2015 LabKey Corporation
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
import org.labkey.test.pages.DomainEditor;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;

public class DatasetDomainEditor extends DomainEditor
{
    private Elements _elements;

    public DatasetDomainEditor(BaseWebDriverTest test)
    {
        super(test);
        _elements = new Elements();
    }

    public void waitForReady()
    {
        super.waitForReady();
        _test.waitForElement(Locator.name("description"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public void saveAndClose()
    {
        save();
    }

    @Override
    public void save()
    {
        _test.clickButton("Save");
    }

    public void checkDemographicData()
    {
        _test.checkCheckbox(elements().demographicCheckbox);
    }

    public void uncheckDemographicData()
    {
        _test.uncheckCheckbox(elements().demographicCheckbox);
    }

    public void shareDemographics(ShareDemographicsBy by)
    {
        _test.selectOptionByValue(elements().sharedBy, by.toString());
    }

    public void inferFieldsFromFile(File file)
    {
        _test.clickButton("Infer Fields from File", 0);
        WebElement dialog =
                Locator.tagWithClass("div", "gwt-DialogBox")
                .withDescendant(Locator.tagWithClass("div", "Caption").withText("Infer Fields from File"))
                .waitForElement(_test.shortWait());
        WebElement radio = Locator.radioButtonByNameAndValue("source", "file").findElement(dialog);
        radio.click();
        WebElement fileField = Locator.tagWithName("input", "uploadFormElement").findElement(dialog);
        _test.setFormElement(fileField, file);
        WebElement submitButton = Locator.lkButton("Submit").findElement(dialog);
        submitButton.click();
        _test.shortWait().until(ExpectedConditions.stalenessOf(dialog));
    }


    public enum ShareDemographicsBy
    {NONE, PTID}

    public Elements elements()
    {
        return _elements;
    }

    private class Elements extends ComponentElements
    {
        private WebElement demographicCheckbox = new LazyWebElement(Locator.name("demographicData"), context);
        private WebElement sharedBy = new LazyWebElement(Locator.name("demographicsSharedBy"), context);

        Elements()
        {
            super(_test.getDriver());
        }
    }
}
