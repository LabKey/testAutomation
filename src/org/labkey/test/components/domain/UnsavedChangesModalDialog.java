package org.labkey.test.components.domain;

import org.labkey.test.components.bootstrap.ModalDialog;

public class UnsavedChangesModalDialog extends ModalDialog
{
    public UnsavedChangesModalDialog(ModalDialogFinder finder)
    {
        super(finder);
    }

    public void discardChanges()
    {
        getWrapper().doAndWaitForPageToLoad(() -> dismiss("No, Discard Changes"));
    }

    public void saveChanges()   // if successful, should navigate away with same effect as clicking 'save' on domain designer page
    {
        getWrapper().doAndWaitForPageToLoad(() -> dismiss("Yes, Save Changes"));
    }
}
