package org.labkey.test.pages;

import org.apache.commons.lang3.StringUtils;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.params.wiki.SaveWikiParams;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.WikiHelper;
import org.labkey.test.util.wiki.ApiWikiHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.Map;
import java.util.Optional;

/**
 * Wraps test wiki defined in 'data/api/executeSql.html'
 */
public class ExecuteSqlTestWiki extends LabKeyPage<ExecuteSqlTestWiki.ElementCache>
{
    private static final String wikiName = "executeSqlTestWiki";
    private static final String qwpTitle = "SQL Results";
    private static final File wikiHtml = TestFileUtils.getSampleData("api/executeSql.html");

    public ExecuteSqlTestWiki(WebDriver driver)
    {
        super(driver);
    }

    public static ExecuteSqlTestWiki beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("wiki", containerPath, "page", Map.of("name", wikiName)));
        return new ExecuteSqlTestWiki(webDriverWrapper.getDriver());
    }

    public static void createTestWiki(String containerPath) throws CommandException
    {
        SaveWikiParams wikiParams = new SaveWikiParams(wikiName, wikiHtml);
        wikiParams.setFormat(WikiHelper.WikiRendererType.HTML);
        new ApiWikiHelper().createWiki(containerPath, wikiParams);
    }

    public DataRegionTable executeSql(String schemaName, String sql)
    {
        if (StringUtils.trimToEmpty(schemaName).isEmpty())
        {
            throw new IllegalArgumentException("Schema name is required for 'executeSql'");
        }

        Optional<WebElement> panel = elementCache().findResultPanel();
        setFormElement(elementCache().schemaInput, schemaName);
        setFormElement(elementCache().sqlInput, sql);
        elementCache().executeButton.click();
        panel.ifPresent(p -> shortWait().until(ExpectedConditions.stalenessOf(p)));
        return new DataRegionTable.DataRegionFinder(getDriver()).waitFor();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        WebElement schemaInput = Locator.id("qwpSchema").findWhenNeeded(this);
        WebElement sqlInput = Locator.id("qwpSql").findWhenNeeded(this);
        WebElement executeButton = Locator.id("button_loadqwp").findWhenNeeded(this);

        Optional<WebElement> findResultPanel()
        {
            return Locator.tagWithName("div", "webpart")
                    .withDescendant(Locator.tagWithClass("span", "labkey-wp-title-text").withText(qwpTitle))
                    .findOptionalElement(this);
        }
    }
}
