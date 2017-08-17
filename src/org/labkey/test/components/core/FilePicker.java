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
package org.labkey.test.components.core;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.FileInput;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.List;

import static org.labkey.test.components.ext4.Window.Window;

/**
 * Wraps FilePicker managed by 'internal/webapp/util.js'
 * See methods addFilePicker and removeFilePicker
 */
public class FilePicker extends WebDriverComponent<FilePicker.ElementCache>
{
    private final WebDriver _driver;
    private final WebElement _el;

    public FilePicker(WebDriver driver)
    {
        _driver = driver;
        _el = Locator.id(getTableId()).findWhenNeeded(_driver);
    }

    protected String getTableId()
    {
        return "filePickerTable";
    }

    protected String getLinkId()
    {
        return "filePickerLink";
    }

    public FilePicker addAttachment(File file)
    {
        elementCache().pickerLink.click();
        elementCache().findLastAttachmentInput().set(file);
        return this;
    }

    public FilePicker removeAllAttachments()
    {
        for (WebElement link : elementCache().findRemoveLinks())
        {
            clickRemove(link);
        }
        return this;
    }

    public FilePicker removeAttachment(int index)
    {
        clickRemove(elementCache().findRemoveLinks().get(index));
        return this;
    }

    /**
     * Click provided remove link and handle confirmation
     */
    private void clickRemove(WebElement removeLink)
    {
        boolean savedAttachment = removeLink.getAttribute("onclick") != null;
        removeLink.click();

        if (savedAttachment)
        {
            Window(getDriver()).withTitle("Remove Attachment").waitFor().clickButton("OK", true);
        }

        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(removeLink));
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        protected FileInput findLastAttachmentInput()
        {
            return new FileInput(Locator.tag("tbody").childTag("tr").last().append(Locator.tag("input")).findElement(this), getDriver());
        }

        protected List<WebElement> findRemoveLinks()
        {
            return Locator.linkWithText("remove").findElements(this);
        }

        // Note: pickerLink is outside of pickerTable (usually sibling)
        protected WebElement pickerLink = Locator.id(getLinkId()).findWhenNeeded(getDriver());
    }
}
