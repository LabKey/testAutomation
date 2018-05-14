package org.labkey.test.pages.list;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class EditListDefinitionPage extends LabKeyPage<EditListDefinitionPage.ElementCache>
{
    public EditListDefinitionPage(WebDriver driver)
    {
        super(driver);
    }

    public static EditListDefinitionPage beginAt(WebDriverWrapper driver, int listId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), listId);
    }

    public static EditListDefinitionPage beginAt(WebDriverWrapper driver, String containerPath, int listId)
    {
        driver.beginAt(WebTestHelper.buildURL("list", containerPath, "editListDefinition", Maps.of("listId", String.valueOf(listId))));
        return new EditListDefinitionPage(driver.getDriver());
    }

    // TODO: List Properties

    public PropertiesEditor listFields()
    {
        return elementCache()._propertiesEditor;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private final PropertiesEditor _propertiesEditor = new PropertiesEditor.PropertiesEditorFinder(getDriver()).withTitle("List Fields").findWhenNeeded();
    }
}
