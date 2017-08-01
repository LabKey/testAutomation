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
package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CreateSubFolderPage extends LabKeyPage
{
    public CreateSubFolderPage(WebDriver test)
    {
        super(test);
    }


    @Override
    public void waitForPage()
    {
        waitFor(()-> Locator.input("name").findElementOrNull(getDriver()) != null, WAIT_FOR_JAVASCRIPT);
    }

    public SetFolderPermissionsPage clickNext()
    {
        doAndWaitForPageToLoad(() -> newElementCache().nextButton.click());
        return new SetFolderPermissionsPage(getDriver());
    }

    public CreateSubFolderPage setFolderName(String name)
    {
        setFormElement(newElementCache().nameInput, name);
        return this;
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    private class Elements extends LabKeyPage.ElementCache
    {
        final WebElement nameInput = Locator.input("name").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        final WebElement nextButton = Locator.button("Next").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        // TODO: Add support for "Use name as title" setting
        // TODO: Add support for configuring the "Folder Type" settings

        // See AbstractContainerHelper.createSubfolder for what it supports and replace it
    }
}