package org.labkey.test.components.ui;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;

import java.util.function.Supplier;

public class DeleteConfirmationDialog<SourcePage extends WebDriverWrapper, ConfirmPage extends LabKeyPage> extends ModalDialog
{
    private final SourcePage _sourcePage;
    private final Supplier<ConfirmPage> _confirmPageSupplier;

    public DeleteConfirmationDialog(@NotNull SourcePage sourcePage)
    {
        this(sourcePage, () -> null);
    }

    public DeleteConfirmationDialog(@NotNull SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this("delete", sourcePage, confirmPageSupplier);
    }

    protected DeleteConfirmationDialog(String partialTitle, @NotNull SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this(new ModalDialog.ModalDialogFinder(sourcePage.getDriver()).withTitleIgnoreCase(partialTitle), sourcePage, confirmPageSupplier);
    }

    protected DeleteConfirmationDialog(ModalDialogFinder finder, SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        super(finder);
        _sourcePage = sourcePage;
        _confirmPageSupplier = confirmPageSupplier;
    }

    @Override
    protected void waitForReady()
    {
        WebDriverWrapper.waitFor(()-> elementCache().body.isDisplayed() &&
                        !BootstrapLocators.loadingSpinner.existsIn(this),
                "The delete confirmation dialog did not become ready.", 1_000);
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

    public ConfirmPage confirmPermanentlyDelete()
    {
        this.dismiss("Yes, Permanently Delete");
        return _confirmPageSupplier.get();
    }

    public SourcePage clickDismiss()
    {
        this.dismiss("Dismiss");
        return _sourcePage;
    }

    public DeleteConfirmationDialog setUserComment(String comment)
    {
        var commentInput = Input.Input(Locator.tagWithClass("textarea", "form-control"), getDriver()).timeout(2000)
                .refindWhenNeeded(this);
        commentInput.set(comment);
        return this;
    }
}
