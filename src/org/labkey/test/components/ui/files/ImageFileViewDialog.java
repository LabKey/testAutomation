package org.labkey.test.components.ui.files;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ImageFileViewDialog extends ModalDialog
{
    public ImageFileViewDialog(WebDriver driver, String filename)
    {
        super(new ModalDialogFinder(driver).withTitle(filename).waitFor().getComponentElement(), driver);
    }

    public boolean isImageRendered()
    {
        return elementCache().img.isDisplayed();
    }

    @Override
    public ElementCache elementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends ModalDialog.ElementCache
    {
        final WebElement img = Locator.tagWithClass("img", "attachment-card__img_modal").findWhenNeeded(this);
    }
}
