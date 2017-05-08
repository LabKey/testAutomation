package org.labkey.test.components.internal;

import org.labkey.test.components.ext4.ComboBox;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.util.Ext4Helper.TextMatchTechnique.STARTS_WITH;

public class ImpersonateGroupWindow extends ImpersonateWindow
{
    private final ComboBox groupCombo = ComboBox.ComboBox(getDriver()).withLabel("Group:").findWhenNeeded(this);

    public ImpersonateGroupWindow(WebDriver driver)
    {
        super("Impersonate Group", driver);
        groupCombo.setMatcher(STARTS_WITH);
    }

    public ImpersonateGroupWindow selectGroup(String groupName)
    {
        groupCombo.selectComboBoxItem(groupName);
        return this;
    }

    public ImpersonateGroupWindow selectSiteGroup(String groupName)
    {
        groupCombo.selectComboBoxItem("Site: " + groupName);
        return this;
    }
}
