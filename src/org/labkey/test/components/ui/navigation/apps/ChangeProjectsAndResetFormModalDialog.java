package org.labkey.test.components.ui.navigation.apps;

import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;



public class ChangeProjectsAndResetFormModalDialog extends ModalDialog
{
    private final UpdatingComponent _updatingComponent;
    public ChangeProjectsAndResetFormModalDialog(WebDriver driver, UpdatingComponent updatingComponent)
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
        _updatingComponent.doAndWaitForUpdate(()->
                dismiss("Change Projects"));

    }
}
