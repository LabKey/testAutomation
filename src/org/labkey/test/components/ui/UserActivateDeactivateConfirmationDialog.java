/*
 * Copyright (c) 2020-2026 LabKey Corporation
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

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.bootstrap.ModalDialog;

public class UserActivateDeactivateConfirmationDialog extends ModalDialog
{
    private final UpdatingComponent _grid;

    public UserActivateDeactivateConfirmationDialog(WebDriverWrapper wdw, UpdatingComponent grid)
    {
        super(new ModalDialog.ModalDialogFinder(wdw.getDriver()).withTitleIgnoreCase("user"));
        _grid = grid;
    }

    public void confirmDeactivate()
    {
        _grid.doAndWaitForUpdate(() -> this.dismiss("Yes, Deactivate"));
    }

    public void confirmReactivate()
    {
        _grid.doAndWaitForUpdate(() -> this.dismiss("Yes, Reactivate"));
    }

    public void cancel()
    {
        this.dismiss("Cancel");
    }
}