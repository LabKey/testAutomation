package org.labkey.test.pages.list;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.FormItem;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;

import static org.labkey.test.components.labkey.FormItemFinder.FormItem;

public class SetDefaultValuesListPage extends LabKeyPage<SetDefaultValuesListPage.ElementCache>
{
    public SetDefaultValuesListPage(WebDriver driver)
    {
        super(driver);
    }

    public static SetDefaultValuesListPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static SetDefaultValuesListPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("list", containerPath, "setDefaultValuesList"));
        return new SetDefaultValuesListPage(driver.getDriver());
    }

    public FormItem<String> defaultValue(String fieldLabel)
    {
        return elementCache().getFormItem(fieldLabel);
    }

    public void save()
    {
        clickAndWait(elementCache().saveButton);
    }

    public void clearDefaults()
    {
        clickAndWait(elementCache().clearButton);
    }

    public void cancel()
    {
        clickAndWait(elementCache().cancelButton);
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private Map<String, FormItem> formItems = new HashMap<>();
        protected WebElement saveButton = Locator.lkButton("Save Defaults").findWhenNeeded(this);
        protected WebElement clearButton = Locator.lkButton("Clear Defaults").findWhenNeeded(this);
        protected WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);

        public FormItem getFormItem(String fieldLabel)
        {
            if (!formItems.containsKey(fieldLabel))
                formItems.put(fieldLabel, FormItem(getDriver()).withLabel(fieldLabel).find(this));
            return formItems.get(fieldLabel);
        }
    }
}