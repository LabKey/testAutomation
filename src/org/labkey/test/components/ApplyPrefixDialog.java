/*
 * Copyright (c) 2021-2026 LabKey Corporation
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
package org.labkey.test.components;


import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;

/**
 * Wraps the component dialog from labkey-ui-components ../internal/components/settings/NameIdSettings.tsx
 */
public class ApplyPrefixDialog extends ModalDialog
{
    public ApplyPrefixDialog(WebDriver driver)
    {
        this("Apply Prefix?", driver);
    }

    protected ApplyPrefixDialog(String title, WebDriver driver)
    {
        super(new ModalDialog.ModalDialogFinder(driver).withTitle(title));
    }

    public void clickApplyPrefix()
    {
        dismiss("Yes, Save and Apply Prefix");
    }
}
