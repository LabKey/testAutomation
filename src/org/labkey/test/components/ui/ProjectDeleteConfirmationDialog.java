package org.labkey.test.components.ui;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebElement;

import java.util.Map;
import java.util.function.Supplier;


public class ProjectDeleteConfirmationDialog<SourcePage extends LabKeyPage, ConfirmPage extends LabKeyPage> extends DeleteConfirmationDialog<SourcePage, ConfirmPage>
{


    public ProjectDeleteConfirmationDialog(@NotNull SourcePage sourcePage)
    {
       super(sourcePage, null);
    }

    public ProjectDeleteConfirmationDialog(@NotNull SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        super(sourcePage, confirmPageSupplier);
    }


    public Map<String, String> getConfirmationData()
    {
        Map<String, String> data = new CaseInsensitiveHashMap<>();
        WebElement tableEl = Locator.tagWithClass("table", "delete-project-modal__table")
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
