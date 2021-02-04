package org.labkey.test.pages.query;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.Map;

/**
 * Page wrapper for 'QueryController.NewQueryAction'
 */
public class NewQueryPage extends LabKeyPage<NewQueryPage.ElementCache>
{
    public NewQueryPage(WebDriver driver)
    {
        super(driver);
    }

    public static NewQueryPage beginAt(WebDriverWrapper webDriverWrapper, String schemaName)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath(), schemaName);
    }

    public static NewQueryPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath, String schemaName)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("query", containerPath, "newQuery", Map.of("schemaName", schemaName)));
        return new NewQueryPage(webDriverWrapper.getDriver());
    }

    public NewQueryPage setName(String name)
    {
        elementCache().queryNameInput.set(name);

        return this;
    }

    public NewQueryPage setBaseTable(String baseTableName)
    {
        elementCache().baseTableInput.selectByVisibleText(baseTableName);

        return this;
    }

    public SourceQueryPage clickCreate()
    {
        clickAndWait(elementCache().createButton);

        return new SourceQueryPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        final Input queryNameInput = Input.Input(Locator.name("ff_newQueryName"), getDriver()).findWhenNeeded(this);
        final Select baseTableInput = SelectWrapper.Select(Locator.name("ff_baseTableName")).findWhenNeeded(this);

        final WebElement createButton = Locator.lkButton("Create and Edit Source").findWhenNeeded(this);
    }
}
