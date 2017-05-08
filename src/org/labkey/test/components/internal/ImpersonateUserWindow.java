package org.labkey.test.components.internal;

import org.labkey.test.components.ext4.ComboBox;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.util.Ext4Helper.TextMatchTechnique.STARTS_WITH;

public class ImpersonateUserWindow extends ImpersonateWindow
{
    private final ComboBox userCombo = ComboBox.ComboBox(getDriver()).withLabel("User:").findWhenNeeded(this);

    public ImpersonateUserWindow(WebDriver driver)
    {
        super("Impersonate User", driver);
        userCombo.setMatcher(STARTS_WITH);
    }

    public ImpersonateUserWindow selectUser(String userName)
    {
        userCombo.selectComboBoxItem(userName + " (");
        return this;
    }
}
