/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ShortUrlAdminPage extends LabKeyPage<ShortUrlAdminPage.ElementCache>
{
    public static final String SHORT_URL_QUERY = "ShortURL";
    public static final String SHORT_URL_COL = "ShortURL";
    public static final String TARGET_URL_COL = "FullURL";

    public ShortUrlAdminPage(WebDriver driver)
    {
        super(driver);
    }

    public static ShortUrlAdminPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("admin", "shortURLAdmin"));
        return new ShortUrlAdminPage(webDriverWrapper.getDriver());
    }

    public static ShortUrlAdminPage beginAtFiltered(WebDriverWrapper webDriverWrapper, String contains)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("admin", "shortURLAdmin",
                Map.of("%s.%s~contains".formatted(SHORT_URL_QUERY, SHORT_URL_COL), contains)));
        return new ShortUrlAdminPage(webDriverWrapper.getDriver());
    }

    @Override
    protected void waitForPage()
    {
        shortWait().until(ExpectedConditions.visibilityOf(elementCache().shortUrlsTable.getComponentElement()));
    }

    public void createNewShortUrl(String shortUrl, String targetUrl)
    {
        int initialCount = getShortUrlGrid().getDataRowCount();
        boolean alreadyExists = getShortUrlGrid().getColumnDataAsText(SHORT_URL_COL).contains(shortUrl);

        submitShortUrl(shortUrl, targetUrl);
        assertEquals("shortUrl count", initialCount + (alreadyExists ? 0 : 1), getShortUrlGrid().getDataRowCount());
    }

    public void submitShortUrl(String shortUrl, String targetUrl)
    {
        elementCache().shortUrlInput.set(shortUrl);
        elementCache().targetUrlInput.set(targetUrl);

        clickSubmit();
    }

    private void clickSubmit()
    {
        clickAndWait(elementCache().submitButton);

        clearCache();
    }

    public String clickCopyToClipboard(String shortUrl) throws IOException, UnsupportedFlavorException
    {
        int rowIndex = getRowIndex(shortUrl);
        WebElement copyToClipboardCell = getShortUrlGrid().findCell(rowIndex, "CopyToClipboard");
        Locator.tagWithClass("a", "fa-clipboard").findElement(copyToClipboardCell).click();

        return getClipboardContent();
    }

    public Map<String, String> getUrlsFromGrid()
    {
        List<List<String>> rows = elementCache().shortUrlsTable.getRows(SHORT_URL_COL, TARGET_URL_COL);
        Map<String, String> urls = new LinkedHashMap<>(rows.size());
        rows.forEach(row -> urls.put(row.getFirst(), row.get(1)));
        return urls;
    }

    public UpdateShortUrlPage editShortUrl(String shortUrl)
    {
        int rowIndex = getRowIndex(shortUrl);
        getShortUrlGrid().clickEditRow(rowIndex);

        return new UpdateShortUrlPage(getDriver());
    }

    public DataRegionTable getShortUrlGrid()
    {
        return elementCache().shortUrlsTable;
    }

    private int getRowIndex(String shortUrl)
    {
        return getShortUrlGrid().getRowIndexStrict(SHORT_URL_COL, shortUrl);
    }

    public void deleteAll()
    {
        DataRegionTable shortUrlGrid = getShortUrlGrid();
        if (shortUrlGrid.getDataRowCount() > 0)
        {
            shortUrlGrid.checkAllOnPage();
            shortUrlGrid.deleteSelectedRows();
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<ElementCache>.ElementCache
    {
        final WebElement createNewShortUrlPanel = PortalHelper.Locators.webPart("Create New Short URL").findWhenNeeded(this);
        final Input shortUrlInput = Input.Input(Locator.id("shortURLTextField"), getDriver()).findWhenNeeded(createNewShortUrlPanel);
        final Input targetUrlInput = Input.Input(Locator.id("targetURLTextField"), getDriver()).findWhenNeeded(createNewShortUrlPanel);
        final WebElement submitButton = Locator.lkButton("Submit").findWhenNeeded(createNewShortUrlPanel);

        final DataRegionTable shortUrlsTable = DataRegionTable.DataRegion(getDriver()).withName("ShortURL").findWhenNeeded(this);
    }
}
