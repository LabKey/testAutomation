package org.labkey.test.components.ui;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebElement;

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
        WebElement commentInput = Locator.tag("textarea").waitForElement(this, 1000);
        commentInput.click();
        commentInput.sendKeys(comment);
        return this;
    }
}
