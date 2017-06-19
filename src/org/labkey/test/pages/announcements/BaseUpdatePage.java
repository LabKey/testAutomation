package org.labkey.test.pages.announcements;

import org.labkey.test.Locator;
import org.labkey.test.components.core.FilePicker;
import org.labkey.test.components.html.EnumSelect;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.WikiHelper.WikiRendererType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

import static org.labkey.test.components.html.EnumSelect.EnumSelect;
import static org.labkey.test.components.html.Input.Input;

abstract class BaseUpdatePage<PAGE> extends LabKeyPage<BaseUpdatePage.ElementCache>
{
    protected BaseUpdatePage(WebDriver driver)
    {
        super(driver);
    }

    public PAGE setTitle(String title)
    {
        elementCache().titleInput.set(title);
        return getThis();
    }

    public PAGE setBody(String body)
    {
        elementCache().bodyInput.set(body);
        return getThis();
    }

    public PAGE setRenderAs(WikiRendererType renderAs)
    {
        elementCache().rendererSelect.set(renderAs);
        return getThis();
    }

    public PAGE addAttachments(File... files)
    {
        for (File file : files)
        {
            elementCache().filePicker.addAttachment(file);
        }
        return getThis();
    }

    public PAGE removeAttachment(int index)
    {
        elementCache().filePicker.removeAttachment(index);
        return getThis();
    }

    public ThreadPage submit()
    {
        clickAndWait(elementCache().submitButton);
        return new ThreadPage(getDriver());
    }

    public LabKeyPage cancel()
    {
        clickAndWait(elementCache().cancelButton);
        return new LabKeyPage(getDriver());
    }

    protected abstract PAGE getThis();

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected Input titleInput = Input(Locator.id("title"), getDriver()).findWhenNeeded(this);
        protected Input bodyInput = Input(Locator.id("body"), getDriver()).findWhenNeeded(this);

        protected EnumSelect<WikiRendererType> rendererSelect = EnumSelect(Locator.id("wiki-input-window-change-format-to"), WikiRendererType.class).findWhenNeeded(this);

        protected FilePicker filePicker = new FilePicker(getDriver());
        protected WebElement submitButton = Locator.lkButton("Submit").findWhenNeeded(this);
        protected WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
    }
}