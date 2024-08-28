package org.labkey.test.components.ui;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.bootstrap.ModalDialog;

public class UserActivateDeactivateConfirmationDialog extends ModalDialog
{
    private final UpdatingComponent _grid;

    public UserActivateDeactivateConfirmationDialog(WebDriverWrapper wdw, UpdatingComponent grid)
    {
        super(new ModalDialog.ModalDialogFinder(wdw.getDriver()).withTitleIgnoreCase("user"));
        _grid = grid;
    }

    public void confirmDeactivate()
    {
        _grid.doAndWaitForUpdate(() -> this.dismiss("Yes, Deactivate"));
    }

    public void confirmReactivate()
    {
        _grid.doAndWaitForUpdate(() -> this.dismiss("Yes, Reactivate"));
    }

    public void cancel()
    {
        this.dismiss("Cancel");
    }
}