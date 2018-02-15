/*
 * Copyright (c) 2017 LabKey Corporation
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
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SetFolderPermissionsPage extends LabKeyPage
{
    public SetFolderPermissionsPage(WebDriver test)
    {
        super(test);
    }

    @Override
    protected void waitForPage()
    {
        waitFor(()-> Locator.css(".labkey-nav-page-header").withText("Users / Permissions")
                .findElementOrNull(getDriver()) != null,
                WAIT_FOR_JAVASCRIPT);
    }

    public SetInitialFolderSettingsPage clickNext()
    {
        doAndWaitForPageToLoad(() -> newElementCache().nextButton.click());
        return new SetInitialFolderSettingsPage(getDriver());
    }

    public void clickFinish()
    {
        doAndWaitForPageToLoad(() -> newElementCache().finishButton.click());
    }


    public SetFolderPermissionsPage setInheritFromParentFolder()
    {
        RadioButton radio = new RadioButton(Locator.xpath("//td[./label[text()='Inherit From Parent Folder']]/input")
                .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT));
        radio.set(true);
        return this;
    }

    /* this option occurs if the folder is also a project container/ we are creating a project */
    public SetFolderPermissionsPage setCopyFromExistingProject(String projectToCopy)
    {
        RadioButton radio = new RadioButton(Locator.xpath("//td[./label[text()='Copy From Existing Project']]/input")
                .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT));
        radio.set(true);

        waitFor(() ->
                { // Workaround: erratic combo-box behavior
                    try{_ext4Helper.selectComboBoxItem(Locator.xpath("//table[@id='targetProject']"), projectToCopy);}
                    catch (NoSuchElementException recheck) {return false;}

                    if (!getFormElement(Locator.css("#targetProject input")).equals(projectToCopy))
                    {
                        click(Locator.xpath("//table[@id='targetProject']"));
                        return false;
                    }
                    else
                    {
                        return true;
                    }
                },
                "Failed to select project", WAIT_FOR_JAVASCRIPT);

        return this;
    }

    public SetFolderPermissionsPage setMyUserOnly()
    {
        RadioButton radio = new RadioButton(
            Locator.xpath("//td[./label[text()='My User Only']]/input")
                    .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT));
        radio.set(true);
        return this;
    }

    public PermissionsPage clickFinishAndConfigurePermissions()
    {
        clickButton("Finish And Configure Permissions");
        return new PermissionsPage(getDriver());
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    private class Elements extends LabKeyPage.ElementCache
    {
        final WebElement finishButton = Locator.lkButton("Finish").findWhenNeeded(this).withTimeout(4000);
        final WebElement nextButton = Locator.lkButton("Next").findWhenNeeded(this).withTimeout(4000);


        // See AbstractContainerHelper.createSubfolder for what it supports and replace it
    }
}
