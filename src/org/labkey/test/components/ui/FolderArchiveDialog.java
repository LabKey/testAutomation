package org.labkey.test.components.ui;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.function.Function;
import java.util.function.Supplier;

public class FolderArchiveDialog <ConfirmPage extends WebDriverWrapper> extends ModalDialog
{

    private final Function<Runnable, ConfirmPage> _confirmationSynchronizationFunction;

    public FolderArchiveDialog(@NotNull WebDriverWrapper sourcePage, WebElement staleOnConfirmElement, Supplier<ConfirmPage> confirmPageSupplier)
    {

        // Dialog finder stumbles with 'tricky characters' so limiting the search to just the word 'Archive'.
        super(new ModalDialog.ModalDialogFinder(sourcePage.getDriver()).withTitleIgnoreCase("Archive"));

        UpdatingComponent updatingComponent = runnable -> {
            runnable.run();
            sourcePage.longWait().until(ExpectedConditions.stalenessOf(staleOnConfirmElement));
        };

        _confirmationSynchronizationFunction = runnable -> {
            updatingComponent.doAndWaitForUpdate(runnable);
            return confirmPageSupplier.get();
        };

    }

    public ConfirmPage clickYesArchive()
    {
        return clickYesArchive(10);
    }

    public ConfirmPage clickYesArchive(Integer waitSeconds)
    {
        return  _confirmationSynchronizationFunction.apply(() -> this.dismiss( "Yes, Archive Folder", waitSeconds));
    }

    public void clickCancel()
    {
        this.dismiss("Cancel");
    }

}
