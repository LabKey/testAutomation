/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
