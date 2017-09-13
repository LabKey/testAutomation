package org.labkey.test.pages.issues;

import org.labkey.test.components.labkey.FormItemFinder;
import org.labkey.test.components.labkey.ReadOnlyFormItem;
import org.openqa.selenium.WebElement;

public class IssuesReadOnlyFormItem extends ReadOnlyFormItem
{
    protected IssuesReadOnlyFormItem(WebElement el)
    {
        super(el);
    }

    public static FormItemFinder<IssuesReadOnlyFormItem> IssueReadOnlyFormItem()
    {
        return new IssuesFormItemFinder<IssuesReadOnlyFormItem>()
        {
            @Override
            protected IssuesReadOnlyFormItem construct(WebElement el)
            {
                return new IssuesReadOnlyFormItem(el);
            }

            @Override
            protected String itemTag()
            {
                return ".";
            }
        };
    }
}
