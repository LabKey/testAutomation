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
