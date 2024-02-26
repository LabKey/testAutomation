package org.labkey.test.pages.query;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegion;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Map;

/**
 * Page wrapper for 'QueryController.SourceQueryAction'
 */
public class SourceQueryPage extends LabKeyPage<SourceQueryPage.ElementCache>
{
    public SourceQueryPage(WebDriver driver)
    {
        super(driver);
    }

    public static SourceQueryPage beginAt(WebDriverWrapper webDriverWrapper, String schemaName, String queryName)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath(), schemaName, queryName);
    }

    public static SourceQueryPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath, String schemaName, String queryName)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("query", containerPath, "sourceQuery", Map.of("schemaName", schemaName, "query.queryName", queryName)));
        return new SourceQueryPage(webDriverWrapper.getDriver());
    }

    public SourceQueryPage viewSource()
    {
        _ext4Helper.clickExt4Tab("Source");
        return this;
    }

    public SourceQueryPage setSource(String sql)
    {
        viewSource();
        setCodeEditorValue("queryText", sql);
        return this;
    }

    /**
     * Select "Data" tab. Assumes that data region will refresh.
     * @return this
     */
    public DataRegionTable viewData()
    {
        return viewData(Duration.ofSeconds(10));
    }

    public DataRegionTable viewData(Duration timeout)
    {
        doAndWaitForPageSignal(this::clickDataTab, DataRegion.UPDATE_SIGNAL, new WebDriverWait(getDriver(), timeout));
        return DataRegionTable.DataRegion(getDriver()).waitFor();
    }

    private void clickDataTab()
    {
        _ext4Helper.clickExt4Tab("Data");
    }

    public SourceQueryPage viewMetadata()
    {
        _ext4Helper.clickExt4Tab("XML Metadata");
        return this;
    }

    public SourceQueryPage setMetadataXml(String xml)
    {
        viewMetadata();
        setCodeEditorValue("metadataText", xml);
        return this;
    }

    public String getMetadataXml()
    {
        viewMetadata();
        return getCodeEditorValue("metadataText");
    }

    public SourceQueryPage clickSave()
    {
        Ext4Helper.Locators.ext4Button("Save").findElement(getDriver()).click();
        waitForElement(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);

        return this;
    }

    public ExecuteQueryPage clickSaveAndFinish()
    {
        clickAndWait(Ext4Helper.Locators.ext4Button("Save & Finish").findElement(getDriver()));
        return new ExecuteQueryPage(getDriver());
    }

    public String clickSaveExpectingError()
    {
        Ext4Helper.Locators.ext4Button("Save").findElement(getDriver()).click();
        return waitForElement(Locator.tagWithId("div","status")).getText();
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
