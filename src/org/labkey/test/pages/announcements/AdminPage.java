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
package org.labkey.test.pages.announcements;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.html.Checkbox.Checkbox;

/**
 * User: tgaluhn
 * Date: 4/30/2018
 *
 * Page object for the messsage board admin (customization) page
 */
public class AdminPage extends LabKeyPage<AdminPage.ElementCache>
{
    public AdminPage(WebDriver driver)
    {
        super(driver);
    }

    public static AdminPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static AdminPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("announcements", containerPath, "customize"));
        return new AdminPage(driver.getDriver());
    }

    public LabKeyPage save()
    {
        clickAndWait(elementCache().saveButton);
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage cancel()
    {
        clickAndWait(elementCache().cancelButton);
        return new LabKeyPage(getDriver());
    }

    public AdminPage setModeratorReviewAll()
    {
        confirmOptionSelected(elementCache().moderatorReviewAll);
        return new AdminPage(getDriver());
    }

    public AdminPage setModeratorReviewNewThread()
    {
        confirmOptionSelected(elementCache().moderatorReviewNewThread);
        return new AdminPage(getDriver());
    }

    public AdminPage setModeratorReviewInitial()
    {
        confirmOptionSelected(elementCache().moderatorReviewInitial);
        return new AdminPage(getDriver());
    }

    public AdminPage setModeratorReviewNone()
    {
        confirmOptionSelected(elementCache().moderatorReviewNone);
        return new AdminPage(getDriver());
    }

    private void confirmOptionSelected(WebElement moderatorReviewOption)
    {
        int tries = 1;
        while(!moderatorReviewOption.isSelected() && tries <= 5)
        {
            moderatorReviewOption.click();
            sleep(500);
            tries++;
        }

        if(!moderatorReviewOption.isSelected())
            Assert.fail("Was not able to select the Moderator Review option");

    }

    public AdminPage setBoardName(String name)
    {
        setFormElement(elementCache().boardName, name);
        return this;
    }

    public AdminPage setConversationName(String name)
    {
        setFormElement(elementCache().conversationName, name);
        return this;
    }

    public AdminPage canEditTitle(boolean checked)
    {
        elementCache().canEditTitle.set(checked);
        return this;
    }

    public AdminPage includeMemberList(boolean checked)
    {
        elementCache().includeMemberList.set(checked);
        return this;
    }

    public AdminPage includeStatus(boolean checked)
    {
        elementCache().includeStatus.set(checked);
        return this;
    }

    public AdminPage includeExpires(boolean checked)
    {
        elementCache().includeExpires.set(checked);
        return this;
    }

    public AdminPage includeGroups(boolean checked)
    {
        elementCache().includeGroups.set(checked);
        return this;
    }

    public AdminPage includeAssignedTo(boolean checked)
    {
        elementCache().includeAssignedTo.set(checked);
        return this;
    }

    public AdminPage includeFormatPicker(boolean checked)
    {
        elementCache().includeFormatPicker.set(checked);
        return this;
    }

    public AdminPage selectDefaultAssignedTo(String userName)
    {
        elementCache().defaultAssignedToSelect.set(userName);
        return this;
    }

    public AdminPage setSecurity(boolean on)
    {
        if (on)
        {
            elementCache().securityOn.click();
        }
        else
        {
            elementCache().securityOff.click();
        }
        return this;
    }

    @Override
    protected AdminPage.ElementCache newElementCache()
    {
        return new AdminPage.ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected WebElement boardName = Locator.input("boardName").findWhenNeeded(this);
        protected WebElement conversationName = Locator.input("conversationName").findWhenNeeded(this);

        protected WebElement sortingInitial = Locator.radioButtonByName("sortOrderIndex").withAttribute("value", "0").findWhenNeeded(this);
        protected WebElement sortingRecent = Locator.radioButtonByName("sortOrderIndex").withAttribute("value", "1").findWhenNeeded(this);

        protected WebElement securityOff = Locator.radioButtonByName("secure").withAttribute("value", "0").findWhenNeeded(this);
        protected WebElement securityOn = Locator.radioButtonByName("secure").withAttribute("value", "1").findWhenNeeded(this);

        private Locator.XPathLocator moderatorReview = Locator.radioButtonByName("moderatorReview");
        protected WebElement moderatorReviewAll = moderatorReview.withAttribute("value", "All").findWhenNeeded(this);
        protected WebElement moderatorReviewInitial = moderatorReview.withAttribute("value", "InitialPost").findWhenNeeded(this);
        protected WebElement moderatorReviewNewThread = moderatorReview.withAttribute("value", "NewThread").findWhenNeeded(this);
        protected WebElement moderatorReviewNone = moderatorReview.withAttribute("value", "None").findWhenNeeded(this);

        Checkbox canEditTitle = Checkbox(Locator.checkboxByName("titleEditable")).findWhenNeeded(this);
        Checkbox includeMemberList = Checkbox(Locator.checkboxByName("memberList")).findWhenNeeded(this);
        Checkbox includeStatus = Checkbox(Locator.checkboxByName("status")).findWhenNeeded(this);
        Checkbox includeExpires = Checkbox(Locator.checkboxByName("expires")).findWhenNeeded(this);
        Checkbox includeAssignedTo = Checkbox(Locator.checkboxByName("assignedTo")).findWhenNeeded(this);
        OptionSelect<OptionSelect.SelectOption> defaultAssignedToSelect = OptionSelect.OptionSelect(Locator.tagWithName("select", "defaultAssignedTo")).findWhenNeeded(this);
        Checkbox includeGroups = Checkbox(Locator.checkboxByName("includeGroups")).findWhenNeeded(this);
        Checkbox includeFormatPicker = Checkbox(Locator.checkboxByName("formatPicker")).findWhenNeeded(this);

        protected WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        protected WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);

    }

}
