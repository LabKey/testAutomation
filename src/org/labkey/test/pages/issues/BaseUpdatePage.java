/*
 * Copyright (c) 2016-2017 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.components.html.FormItem;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.components.issues.FilePicker;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

public abstract class BaseUpdatePage<EC extends BaseUpdatePage.ElementCache> extends BaseIssuePage<EC>
{
    public BaseUpdatePage(WebDriver driver)
    {
        super(driver);
    }

    public Input title()
    {
        return elementCache().titleInput;
    }

    public Input comment()
    {
        return elementCache().commentInput;
    }

    /**
     * Find any type of field with the given name. Sufficient for simple get/set
     */
    public FormItem fieldWithName(String fieldName)
    {
        return elementCache().formItemNamed(fieldName);
    }

    /**
     * Find a named field you know to be a <select>
     */
    public OptionSelect selectWithName(String fieldName)
    {
        return elementCache().getSelect(fieldName);
    }

    @Override
    public OptionSelect related()
    {
        return (OptionSelect) super.related();
    }

    @Override
    public OptionSelect priority()
    {
        return (OptionSelect) super.priority();
    }

    public void addAttachment(File file)
    {
        elementCache().filePicker.addAttachment(file);
    }

    public abstract LabKeyPage save();

    public UpdatePage saveFail()
    {
        clickAndWait(elementCache().saveButton);
        return new UpdatePage(getDriver());
    }

    public DetailsPage cancel()
    {
        clickAndWait(elementCache().cancelButton);
        return new DetailsPage(getDriver());
    }

    public DetailsPage cancelDirty()
    {
        doAndWaitForPageToLoad(() ->
        {
            elementCache().cancelButton.click();
            assertAlertContains("Confirm Navigation");
        });
        return new DetailsPage(getDriver());
    }

    protected EC newElementCache()
    {
        return (EC) new ElementCache();
    }

    protected class ElementCache extends BaseIssuePage.ElementCache
    {
        protected ElementCache()
        {
            assignedTo = getSelect("assignedTo");
            priority = getSelect("priority");
            related = getInput("related");
            notifyList = getInput("notifyList");
        }

        protected Input titleInput = getInput("title");
        protected Input commentInput = getInput("comment");

        protected FilePicker filePicker = new FilePicker(getDriver());

        protected WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        protected WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
    }
}
