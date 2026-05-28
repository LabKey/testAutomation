/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.test.components.query;

import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.pages.query.QueryMetadataEditorPage;


public class AliasFieldDialog extends ModalDialog
{
    private final QueryMetadataEditorPage _page;

    public AliasFieldDialog(QueryMetadataEditorPage page)
    {
        super(new ModalDialogFinder(page.getDriver()).withTitle("Choose a field to wrap"));
        _page = page;
    }

    public AliasFieldDialog selectAliasField(String fieldName)
    {
        ReactSelect.finder(getDriver()).waitFor(this).select(fieldName);
        return this;
    }

    public QueryMetadataEditorPage clickApply()
    {
        dismiss("OK");
        return _page;
    }
}
