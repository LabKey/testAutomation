package org.labkey.test.components.ui;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.function.Supplier;

public class FolderArchiveDialog <ConfirmPage extends WebDriverWrapper> extends DeleteConfirmationDialog<ConfirmPage>
{

    public FolderArchiveDialog(String dialogTitle, @NotNull WebDriverWrapper sourcePage, WebElement staleOnConfirmElement, Supplier<ConfirmPage> confirmPageSupplier)
    {
        super(dialogTitle, sourcePage, runnable -> {
                    runnable.run();
                    sourcePage.longWait().until(ExpectedConditions.stalenessOf(staleOnConfirmElement));
                    return confirmPageSupplier.get();
                }
        );

    }

    public ConfirmPage clickYesArchive()
    {
        return clickYesArchive(10);
    }

    public ConfirmPage clickYesArchive(Integer waitSeconds)
    {
        return super.clickConfirmButton(waitSeconds, "Yes, Archive Folder");
    }

    public void clickCancel()
    {
        this.dismiss("Cancel");
    }

}
