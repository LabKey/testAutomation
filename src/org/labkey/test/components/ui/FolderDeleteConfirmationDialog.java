/*
 * Copyright (c) 2023-2026 LabKey Corporation
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
package org.labkey.test.components.ui;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.WebElement;

import java.util.Map;
import java.util.function.Supplier;


public class FolderDeleteConfirmationDialog<ConfirmPage extends WebDriverWrapper> extends DeleteConfirmationDialog<ConfirmPage>
{
    public FolderDeleteConfirmationDialog(@NotNull WebDriverWrapper sourcePage, WebElement staleOnConfirmElement, Supplier<ConfirmPage> confirmPageSupplier)
    {
        super(sourcePage, staleOnConfirmElement, confirmPageSupplier);
    }


    public Map<String, String> getConfirmationData()
    {
        Map<String, String> data = new CaseInsensitiveHashMap<>();
        WebElement tableEl = Locator.tagWithClass("table", "delete-folder-modal__table")
                .waitForElement(this, 2000);
        var rows = Locator.tag("tbody").child("tr").findElements(tableEl);
        for (WebElement row : rows)
        {
            var cols = getWrapper().getTexts(Locator.tag("td").findElements(row));
            data.put(cols.get(0), cols.get(1));
        }
        return data;
    }
}
