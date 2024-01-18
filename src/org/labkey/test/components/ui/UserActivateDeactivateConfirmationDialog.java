package org.labkey.test.components.ui;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.pages.LabKeyPage;

import java.util.function.Supplier;

public class UserActivateDeactivateConfirmationDialog<SourcePage extends WebDriverWrapper, ConfirmPage extends LabKeyPage> extends ModalDialog
{
    private final SourcePage _sourcePage;
    private final Supplier<ConfirmPage> _confirmPageSupplier;

    public UserActivateDeactivateConfirmationDialog(SourcePage sourcePage)
    {
        this(sourcePage, () -> null);
    }

    public UserActivateDeactivateConfirmationDialog(SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this("user", sourcePage, confirmPageSupplier);
    }

    protected UserActivateDeactivateConfirmationDialog(String partialTitle, SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this(new ModalDialog.ModalDialogFinder(sourcePage.getDriver()).withTitleIgnoreCase(partialTitle), sourcePage, confirmPageSupplier);
    }

    protected UserActivateDeactivateConfirmationDialog(ModalDialogFinder finder, SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        super(finder);
        _sourcePage = sourcePage;
        _confirmPageSupplier = confirmPageSupplier;
    }

    public ConfirmPage confirmDeactivate()
    {
        this.dismiss("Yes, Deactivate");
        return _confirmPageSupplier.get();
    }

    public ConfirmPage confirmReactivate()
    {
        this.dismiss("Yes, Reactivate");
        return _confirmPageSupplier.get();
    }

    public SourcePage cancelDelete()
    {
        this.dismiss("Cancel");
        return _sourcePage;
    }

}
