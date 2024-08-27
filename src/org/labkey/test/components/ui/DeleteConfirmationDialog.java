package org.labkey.test.components.ui;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Input;

import java.util.function.Function;
import java.util.function.Supplier;

public class DeleteConfirmationDialog<ConfirmPage extends WebDriverWrapper> extends ModalDialog
{
    private final Function<Runnable, ConfirmPage> _confirmPageSupplier;

    /**
     * @deprecated Use a constructor that provides some synchronization
     */
    @Deprecated (since = "24.8")
    public DeleteConfirmationDialog(@NotNull WebDriverWrapper sourcePage)
    {
        this(sourcePage, () -> null);
    }

    public DeleteConfirmationDialog(@NotNull WebDriverWrapper sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this(sourcePage, UpdatingComponent.NO_OP, confirmPageSupplier);
    }

    public DeleteConfirmationDialog(@NotNull ConfirmPage sourcePage, UpdatingComponent updatingComponent)
    {
        this(sourcePage, updatingComponent, () -> sourcePage);
    }

    public DeleteConfirmationDialog(@NotNull WebDriverWrapper sourcePage, UpdatingComponent updatingComponent, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this("delete", sourcePage, runnable -> {
            updatingComponent.doAndWaitForUpdate(runnable);
            return confirmPageSupplier.get();
        });
    }

    protected DeleteConfirmationDialog(String partialTitle, @NotNull WebDriverWrapper sourcePage, Function<Runnable, ConfirmPage> confirmPageSupplier)
    {
        this(new ModalDialog.ModalDialogFinder(sourcePage.getDriver()).withTitleIgnoreCase(partialTitle), confirmPageSupplier);
    }

    protected DeleteConfirmationDialog(ModalDialogFinder finder, Function<Runnable, ConfirmPage> confirmPageSupplier)
    {
        super(finder);
        _confirmPageSupplier = confirmPageSupplier;
    }

    @Override
    protected void waitForReady()
    {
        WebDriverWrapper.waitFor(()-> elementCache().body.isDisplayed() &&
                !elementCache().title.getText().isEmpty() &&
                !BootstrapLocators.loadingSpinner.existsIn(this),
                "The delete confirmation dialog did not become ready.", 1_000);
    }

    public void cancelDelete()
    {
        this.dismiss("Cancel");
    }

    public ConfirmPage confirmDelete()
    {
        return confirmDelete(10);
    }

    public ConfirmPage confirmDelete(Integer waitSeconds)
    {
        return _confirmPageSupplier.apply(() -> this.dismiss("Yes, Delete", waitSeconds));
    }

    public Boolean isDeleteEnabled()
    {
        return isDismissEnabled("Yes, Delete");
    }

    public ConfirmPage confirmPermanentlyDelete()
    {
        return confirmPermanentlyDelete(10);
    }

    public ConfirmPage confirmPermanentlyDelete(Integer waitSeconds)
    {
        return _confirmPageSupplier.apply(() -> this.dismiss("Yes, Permanently Delete", waitSeconds));
    }

    public void clickDismiss()
    {
        this.dismiss("Dismiss");
    }

    public DeleteConfirmationDialog setUserComment(String comment)
    {

        WebDriverWrapper.waitFor(()-> elementCache().commentInput.getComponentElement().isDisplayed(),
                "The 'Comment' field is not visible.", 2_500);

        elementCache().commentInput.set(comment);
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {

        Input commentInput = Input.Input(Locator.tagWithClass("textarea", "form-control"), getDriver()).timeout(2000)
                .refindWhenNeeded(this);

    }


}
