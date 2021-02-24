package org.labkey.test.components.experiment;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.pages.LabKeyPage;

import java.util.function.Supplier;

public abstract class ConfirmationDialog<SourcePage extends WebDriverWrapper, ConfirmPage extends LabKeyPage<?>> extends ModalDialog
{
    private final SourcePage _sourcePage;
    private final Supplier<ConfirmPage> _confirmPageSupplier;

    protected ConfirmationDialog(String partialTitle, SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this(new ModalDialog.ModalDialogFinder(sourcePage.getDriver()).withTitle(partialTitle), sourcePage, confirmPageSupplier);
    }

    protected ConfirmationDialog(ModalDialogFinder finder, SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        super(finder);
        _sourcePage = sourcePage;
        _confirmPageSupplier = confirmPageSupplier;
    }

    public ConfirmPage confirm()
    {
        this.dismiss(getConfirmButtonText());
        return _confirmPageSupplier.get();
    }

    public SourcePage cancel()
    {
        this.dismiss(getCancelText());
        return _sourcePage;
    }

    protected SourcePage getSourcePage()
    {
        return _sourcePage;
    }

    protected ConfirmPage getConfirmPage()
    {
        return _confirmPageSupplier.get();
    }

    protected abstract String getConfirmButtonText();

    protected String getCancelText()
    {
        return "Cancel";
    }
}
