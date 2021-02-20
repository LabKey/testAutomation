package org.labkey.test.components.ui;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.pages.LabKeyPage;

import java.util.function.Supplier;

/**
 * Wraps several confirmation dialogs
 */
public class DeleteConfirmationDialog<SourcePage extends WebDriverWrapper, ConfirmPage extends LabKeyPage<?>> extends ConfirmationDialog<SourcePage, ConfirmPage>
{
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
        super(new ModalDialog.ModalDialogFinder(sourcePage.getDriver()).withTitle(partialTitle), sourcePage, confirmPageSupplier);
    }

    public SourcePage cancelDelete()
    {
        return cancel();
    }

    public ConfirmPage confirmDelete()
    {
        return confirm();
    }

    public SourcePage clickDismiss()
    {
        this.dismiss("Dismiss");
        return getSourcePage();
    }

    @Override
    protected String getConfirmButtonText()
    {
        return "Yes, Delete";
    }
}
