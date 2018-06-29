package org.labkey.test.etl.pages;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.etl.ETLHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * User: tgaluhn
 * Date: 6/21/2018
 */
public class DefinitionsQueryView extends LabKeyPage<DefinitionsQueryView.ElementCache>
{
    private final String _regionName;

    public DefinitionsQueryView(WebDriver driver, String regionName)
    {
        super(driver);
        _regionName = regionName;
    }

    public static DefinitionsQueryView beginAtFolderMgmt(WebDriverWrapper driver)
    {
        driver.beginAt(driver.getCurrentContainerPath());
        driver.goToFolderManagement().selectTab("etls");
        return new DefinitionsQueryView(driver.getDriver(), "transforms");
    }

    public static DefinitionsQueryView beginAtQuery(WebDriverWrapper driver)
    {
        driver.beginAt(driver.getCurrentContainerPath());
        driver.navigateToQuery(ETLHelper.DATAINTEGRATION_SCHEMA, ETLHelper.DATAINTEGRATION_ETLDEF);
        return new DefinitionsQueryView(driver.getDriver(), "query");
    }

    public LabKeyPage createNew(String definitionXml)
    {
        return createNew(definitionXml, null);
    }

    public LabKeyPage createNew(String definitionXml, @Nullable String expectedError)
    {
        elementCache()._dataRegionTable.clickInsertNewRow();
        return new DefinitionPage(getDriver()).setDefinitionXml(definitionXml).save(expectedError);
    }

    public DefinitionPage edit(String name)
    {
        elementCache()._dataRegionTable.clickEditRow(getRowIndex(name));
        return new DefinitionPage(getDriver());
    }
    public LabKeyPage editAndSave(String name, String definitionXml, @Nullable String expectedError)
    {
        return edit(name).setDefinitionXml(definitionXml).save(expectedError);
    }

    public DefinitionPage details(String name)
    {
        elementCache()._dataRegionTable.clickRowDetails(getRowIndex(name));
        return new DefinitionPage(getDriver());
    }

    public ConfirmDeletePage delete(String... names)
    {
        Arrays.stream(names).forEach(name -> elementCache()._dataRegionTable.checkCheckbox(getRowIndex(name)));
        elementCache()._dataRegionTable.clickHeaderButton("Delete");
        ConfirmDeletePage deletePage = new ConfirmDeletePage(getDriver());
        deletePage.assertConfirmation(names);
        return deletePage;
    }

    public ConfirmDeletePage deleteWithEnabledCheck(String name, boolean enabled)
    {
        elementCache()._dataRegionTable.checkCheckbox(getRowIndex(name));
        elementCache()._dataRegionTable.clickHeaderButton("Delete");
        ConfirmDeletePage deletePage = new ConfirmDeletePage(getDriver());
        deletePage.assertConfirmationRespectEnabled(name, enabled);
        return deletePage;
    }

    public void assertEtlPresent(String name)
    {
        assertTrue("Etl with name '" + name + "' not present in grid", getRowIndex(name) > -1);
    }

    public void assertEtlNotPresent(String name)
    {
        assertEquals("Etl with name '" + name + "' present in grid ", getRowIndex(name), -1);
    }

    public String getRowPk(String name)
    {
        beginAtQuery(this);
        if (elementCache()._dataRegionTable.getColumnIndex("EtlDefId") < 0)
        {
            CustomizeView helper = elementCache()._dataRegionTable.getCustomizeView();
            helper.openCustomizeViewPanel();
            helper.showHiddenItems();
            helper.addColumn("EtlDefId");
            helper.saveCustomView();
        }
        return elementCache()._dataRegionTable.getRowDataAsMap(getRowIndex(name)).get("EtlDefId");
    }

    private int getRowIndex(String name)
    {
        return elementCache()._dataRegionTable.getRowIndex("Name", name);
    }

    protected DefinitionsQueryView.ElementCache newElementCache()
    {
        return new DefinitionsQueryView.ElementCache(_regionName);
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private final String _regionName;
        DataRegionTable _dataRegionTable;
        WebElement deleteButton = Locator.lkButton("Delete").findWhenNeeded(this);

        public ElementCache(String regionName)
        {
            super();
            _regionName = regionName;
            _dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName(_regionName).findWhenNeeded(this);
        }
    }

}
