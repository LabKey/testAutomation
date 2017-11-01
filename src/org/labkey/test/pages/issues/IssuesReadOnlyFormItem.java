/*
 * Copyright (c) 2017 LabKey Corporation
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
