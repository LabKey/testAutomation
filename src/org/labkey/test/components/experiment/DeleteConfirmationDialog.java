package org.labkey.test.components.experiment;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.pages.LabKeyPage;

import java.util.function.Supplier;

public class DeleteConfirmationDialog<SourcePage extends WebDriverWrapper, ConfirmPage extends LabKeyPage> extends ModalDialog
{
    private final SourcePage _sourcePage;
    private final Supplier<ConfirmPage> _confirmPageSupplier;

    public DeleteConfirmationDialog(SourcePage sourcePage)
    {
        this(sourcePage, () -> null);
    }

    public DeleteConfirmationDialog(SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this("delete", sourcePage, confirmPageSupplier);
    }

    protected DeleteConfirmationDialog(String partialTitle, SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this(new ModalDialog.ModalDialogFinder(sourcePage.getDriver()).withTitle(partialTitle), sourcePage, confirmPageSupplier);
    }

    protected DeleteConfirmationDialog(ModalDialogFinder finder, SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        super(finder);
        _sourcePage = sourcePage;
        _confirmPageSupplier = confirmPageSupplier;
    }

    public SourcePage cancelDelete()
    {
        this.dismiss("Cancel");
        return _sourcePage;
    }

    public ConfirmPage confirmDelete()
    {
        this.dismiss("Yes, Delete");
        return _confirmPageSupplier.get();
    }

    public SourcePage clickDismiss()
    {
        this.dismiss("Dismiss");
        return _sourcePage;
    }
}
