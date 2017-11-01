/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

//TODO: Move domain helpers that aren't list-specific from ListHelper
public class MetadataEditorHelper
{
    private BaseWebDriverTest _test;

    public MetadataEditorHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void save()
    {
        _test.clickButton("Save", 0);
        _test.waitForText("Save successful.");
    }

    public int getFieldIndexForName(String name)
    {
        Locator.XPathLocator fieldRow = Locator.tagWithClass("table", "labkey-wp").withPredicate(Locator.tagWithClass("tr", "labkey-wp-header").withText("Metadata Properties"))
                .append(Locator.tagWithClass("table", "labkey-pad-cells"))
                .append(Locator.tag("tr"));

        List<WebElement> allRows = fieldRow.waitForElements(_test.getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        WebElement headerRow = allRows.get(0);
        List<WebElement> fieldRows = allRows.subList(1, allRows.size());

        int nameColumnIndex = getCellIndexFromRow(headerRow, "Name");

        WebElement desiredRow = fieldRow.withPredicate(Locator.xpath("td").index(nameColumnIndex).withText(name)).findElement(_test.getDriver());

        return fieldRows.indexOf(desiredRow);
    }

    private int getCellIndexFromRow(WebElement tr, String cellText)
    {
        List<WebElement> headerCells = tr.findElements(By.xpath("td"));

        WebElement desiredCell = tr.findElement(Locator.xpath("td").withText(cellText));

        return headerCells.indexOf(desiredCell);
    }
}
