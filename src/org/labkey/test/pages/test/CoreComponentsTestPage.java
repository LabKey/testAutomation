package org.labkey.test.pages.test;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.glassLibrary.components.ReactSelect;
import org.labkey.test.components.glassLibrary.grids.QueryGrid;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

public class CoreComponentsTestPage extends LabKeyPage<CoreComponentsTestPage.ElementCache>
{
    public CoreComponentsTestPage(WebDriver driver)
    {
        super(driver);
    }

    public static CoreComponentsTestPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static CoreComponentsTestPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("core", containerPath, "components"));
        return new CoreComponentsTestPage(webDriverWrapper.getDriver());
    }

    public ReactSelect getComponentSelect()
    {
        return ReactSelect.finder(getDriver()).waitFor();
    }

    /**
     * Configures the current page to show a GridPanel containing the contents of the specified query
     * @param schema
     * @param query
     * @return  a QueryGrid to wrap the GridPanel, once it is found
     */
    public QueryGrid getQueryGrid(String schema, String query)
    {
        getComponentSelect().select("GridPanel");       // note: we use GridPanel now, not QueryGridPanel
        Input.Input(Locator.input("schemaName"), getDriver()).waitFor().set(schema);
        Input.Input(Locator.input("queryName"), getDriver()).waitFor().set(query);
        Locator.button("Apply").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT).click();

        return new QueryGrid.QueryGridFinder(getDriver()).inPanelWithHeaderText("GridPanel").waitFor();
    }

    /**
     * This is here to support comparative testing of QueryGrid:QueryGridPanel components, until the latter is
     * no longer in use.-
     * @param schema
     * @param query
     * @return
     */
    @Deprecated
    public QueryGrid getQueryGridPanel(String schema, String query)
    {
        getComponentSelect().select("QueryGridPanel");       // note: we use GridPanel now, not QueryGridPanel- this is here for backward compat testing only
        Input.Input(Locator.input("schemaNameField"), getDriver()).waitFor().set(schema);
        Input.Input(Locator.input("queryNameField"), getDriver()).waitFor().set(query);
        Locator.button("Apply").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT).click();

        return new QueryGrid.QueryGridFinder(getDriver()).inPanelWithHeaderText("QueryGridPanel").waitFor();
    }

    /**
     * clears the selected component, if there is one, so that the current page can be re-used
     * to test a different component
     * @return
     */
    public CoreComponentsTestPage clearSelectedComponent()
    {
        getComponentSelect().clearSelection();
        return new CoreComponentsTestPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {


    }
}
