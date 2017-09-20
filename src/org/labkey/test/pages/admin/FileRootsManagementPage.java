package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.WebDriver;

/**
 * Created by susanh on 9/20/17.
 */
public class FileRootsManagementPage extends FolderManagementPage
{
    public FileRootsManagementPage(WebDriver driver)
    {
        super(driver);
    }


    public FileRootsManagementPage setCloudStoreEnabled(String name, Boolean enabled)
    {
        Checkbox box = new Checkbox(Locators.cloudStoreCheckBox(name).findElement(getDriver()));
        if (enabled)
            box.check();
        else
            box.uncheck();
        return this;
    }

    public FileRootsManagementPage clickSave()
    {
        elementCache().saveButton.click();
        return this;
    }


    public static class Locators
    {
        public static Locator cloudStoreCheckBox(String name)
        {
            return Locator.tagWithAttribute("input", "value", name);
        }
    }


}
