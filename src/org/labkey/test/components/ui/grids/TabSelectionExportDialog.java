package org.labkey.test.components.ui.grids;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.Collection;

public class TabSelectionExportDialog extends ModalDialog
{
    private static final String TITLE = "Select the Tabs to Export to Excel";
    public TabSelectionExportDialog(WebDriver driver)
    {
        super(new ModalDialogFinder(driver).withTitle(TITLE).waitFor().getComponentElement(), driver);
    }

    public void clickCancel()
    {
        elementCache().cancelButton.click();
    }

    public File export(@Nullable Collection<String> tabs)
    {
        //If tabs is null use default tab selections
        if (tabs != null)
        {
            for (WebElement checkbox : elementCache().checkboxes)
            {
                String text = checkbox.getText();
                if (!tabs.contains(text))
                    elementCache().getCheckBox(checkbox).check();
                else
                    elementCache().getCheckBox(checkbox).uncheck();
            }
        }
        return getWrapper().doAndWaitForDownload(elementCache().exportButton::click);
    }

    public File exportData()
    {
        return export(null);
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
        WebElement cancelButton = Locator.tagWithClass("button", "pull-left")
                .findWhenNeeded(getComponentElement());
        WebElement exportButton = Locator.tagWithClass("button", "btn-success")
                .findWhenNeeded(getComponentElement());

        Collection<WebElement> checkboxes = Locator.tagWithClass("div", "checkbox").findElements(getComponentElement());

        Checkbox getCheckBox(WebElement checkboxDiv)
        {
            return new Checkbox(Locator.tagWithClass("input", "export-modal-checkbox").findWhenNeeded(checkboxDiv));
        }
    }


}
