package org.labkey.test.components.ui.search;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SaveSampleFinderViewModal extends ModalDialog
{
    public SaveSampleFinderViewModal(WebDriver driver)
    {
        this("Save Custom Search", driver);
    }

    protected SaveSampleFinderViewModal(String title, WebDriver driver)
    {
        super(new ModalDialog.ModalDialogFinder(driver).withTitle(title));
    }

    public String getName()
    {
        return Locator.tag("input").findElement(getComponentElement()).getAttribute("value");
    }

    public SaveSampleFinderViewModal setName(String name)
    {
        WebElement input = elementCache().nameInput;
        input.clear();
        if (name != null)
            input.sendKeys(name);
        return this;
    }

    public void clickSave()
    {
        dismiss("Save");
    }

    public String clickSaveExpectingError()
    {
        elementCache().saveBtn.click();
        return getErrorMsg();
    }

    public String getErrorMsg()
    {
        return elementCache().errorMsg.getText();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        WebElement errorMsg = Locator.tagWithClassContaining("div", "alert-danger").findWhenNeeded(getComponentElement());

        WebElement nameInput = Locator.tag("input").findWhenNeeded(getComponentElement());

        WebElement saveBtn = Locator.tagWithClassContaining("button", "btn-success")
                .withText("Save")
                .findWhenNeeded(getComponentElement());
        WebElement cancelButton = Locator.tagWithClassContaining("button", "btn-default")
                .withText("Cancel")
                .findWhenNeeded(getComponentElement());
    }

}
