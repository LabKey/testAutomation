package org.labkey.test.components.core;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.FileInput;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

/**
 * Wraps FilePicker managed by 'internal/webapp/util.js'
 * See methods addFilePicker and removeFilePicker
 */
public class FilePicker extends WebDriverComponent<FilePicker.ElementCache>
{
    private static final String DEFAULT_TABLE_ID = "filePickerTable";
    private static final String DEFAULT_LINK_ID = "filePickerLink";

    private final WebDriver _driver;
    private final WebElement _el;
    private final String linkId;

    // Make public if necessary. All current implementations use the same IDs
    protected FilePicker(WebDriver driver, String tableId, String linkId)
    {
        _driver = driver;
        _el = Locator.id(tableId).findWhenNeeded(_driver);
        this.linkId = linkId;
    }

    public FilePicker(WebDriver driver)
    {
        this(driver, DEFAULT_TABLE_ID, DEFAULT_LINK_ID);
    }

    public FilePicker addAttachment(File file)
    {
        elementCache().pickerLink.click();
        elementCache().findLastAttachmentInput().set(file);
        return this;
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

        // Note: pickerLink is outside of pickerTable (usually sibling)
        protected WebElement pickerLink = Locator.id(linkId).findWhenNeeded(getDriver());
    }
}
