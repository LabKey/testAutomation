package org.labkey.test.pages;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.Ext4Helper;

/**
 * Created by susanh on 6/15/15.
 */
public class WorkflowTask
{
    protected BaseWebDriverTest _test;

    public WorkflowTask(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void reassignTask(String buttonText, String userName)
    {
        _test.click(Locators.getReassignButton(buttonText));
        _test.waitForElement(Ext4Helper.Locators.window("Reassign Task"));
        _test._ext4Helper.selectComboBoxItem("User:", Ext4Helper.TextMatchTechnique.STARTS_WITH, userName);
        _test.clickAndWait(Ext4Helper.Locators.windowButton("Reassign Task", "Assign"));
    }

    public void submitForm(String submitText)
    {
        _test.clickButton(submitText);
    }

    public static class Locators
    {
        public static Locator getReassignButton(String buttonText) { return Locator.lkButtonContainingText(buttonText); }
    }

}
