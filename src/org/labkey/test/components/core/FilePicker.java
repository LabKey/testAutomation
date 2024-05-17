/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.FileInput;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().pickerLink));

        int initialInputCount = elementCache().findAttachmentInputs().size();
        elementCache().pickerLink.click();

        List<FileInput> attachmentInputs = new ArrayList<>();
        WebDriverWrapper.waitFor(() -> {
            attachmentInputs.clear();
            attachmentInputs.addAll(elementCache().findAttachmentInputs());
            return attachmentInputs.size() == initialInputCount + 1;
        }, "Additional file input did not appear", 1000);

        attachmentInputs.get(initialInputCount).set(file);
        return this;
    }

    public FilePicker removeAllAttachments()
    {
        for (WebElement row : elementCache().findAttachmentRows())
        {
            clickRemove(row);
        }
        return this;
    }

    public FilePicker removeAttachment(int index)
    {
        clickRemove(elementCache().findAttachmentRows().get(index));
        return this;
    }

    /**
     * Click provided remove link and handle confirmation
     */
    private void clickRemove(WebElement attachmentRow)
    {
        boolean savedAttachment = !Locator.tag("input").existsIn(attachmentRow);

        WebElement removeLink = Locator.linkWithText("remove").findElement(attachmentRow);
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

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        protected List<FileInput> findAttachmentInputs()
        {
            List<WebElement> inputElements = Locators.attachmentInput.findElements(this);
            return inputElements.stream().map(el -> new FileInput(el, getDriver())).collect(Collectors.toList());
        }

        protected List<WebElement> findAttachmentRows()
        {
            return Locator.xpath("./tbody/tr").findElements(this);
        }

        protected List<WebElement> findRemoveLinks()
        {
            return Locator.linkWithText("remove").findElements(this);
        }

        // Note: pickerLink is outside of pickerTable (usually sibling)
        protected WebElement pickerLink = Locator.id(getLinkId()).findWhenNeeded(getDriver());
    }

    public static class Locators
    {
        static public Locator attachmentInput = Locator.tag("tbody").childTag("tr").append(Locator.tag("input").withAttribute("type", "file"));
    }
}
