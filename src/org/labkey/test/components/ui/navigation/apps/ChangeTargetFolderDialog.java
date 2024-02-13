package org.labkey.test.components.ui.navigation.apps;

import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;



public class ChangeTargetFolderDialog extends ModalDialog
{
    private final UpdatesTargetFolder _updatingComponent;
    public ChangeTargetFolderDialog(WebDriver driver, UpdatesTargetFolder updatingComponent)
    {
        super(new ModalDialogFinder(driver).withTitle("Change projects and reset form?"));
        this._updatingComponent = updatingComponent;
    }


    public void clickCancel()
    {
        dismiss("Cancel");
    }

    public void clickChangeProjects()
    {
        _updatingComponent.doAndWaitForFolderUpdate(()->
                dismiss("Change Projects"));

    }



    static public interface UpdatesTargetFolder
    {
        void doAndWaitForFolderUpdate(Runnable func);
    }
}
