/*
 * Copyright (c) 2017-2019 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;

public class DataRegionElecSignHelper extends AbstractDataRegionExportOrSignHelper
{
    private final String _userName;
    private final String _userPassword;

    public DataRegionElecSignHelper(DataRegionTable drt)
    {
        this(PasswordUtil.getUsername(), drt);
    }

    public DataRegionElecSignHelper(String userName, DataRegionTable drt)
    {
        super(drt);
        _userName = userName;
        _userPassword = PasswordUtil.getPassword();
    }

    public void signExcel(ColumnHeaderType headerType, ExcelFileType type, @Nullable Boolean selected, String reason)
    {
        startExcelExport(headerType, type, selected);
        signDocument(reason);
        // Ideally these should be returning an instance of the SignedSnapshotDetailsPage.
        // Because the detail page is defined in a git branch this code in the svn branch can't see it until the branch is merged with develop.
//        return new SignedSnapshotDetailsPage(BaseWebDriverTest.getCurrentTest());
    }

    public void signText(ColumnHeaderType headerType, TextSeparator delim, TextQuote quote, @Nullable Boolean exportSelected, String reason)
    {
        startTextExport(headerType, delim, quote, exportSelected);
        signDocument(reason);
        // Ideally these should be returning an instance of the SignedSnapshotDetailsPage.
        // Because the detail page is defined in a git branch this code in the svn branch can't see it until the branch is merged with develop.
//        return new SignedSnapshotDetailsPage(BaseWebDriverTest.getCurrentTest());
    }

    public void signDocument(String reason){
        Locator submit = Locator.linkWithText("Submit");
        getWrapper().waitForElement(submit);
        getWrapper().setFormElement(Locator.input("reason"), reason);
        getWrapper().setFormElement(Locator.input("email"), _userName);
        getWrapper().setFormElement(Locator.input("password"), _userPassword);
        getWrapper().clickAndWait(submit);
        getWrapper().waitForText("Details");        // We should be on Details page for the newly signed snapshot

    }

    @Override
    protected String getExcelActionButtonText()
    {
        return "Sign Data";
    }

    @Override
    protected String getTextActionButtonText()
    {
        return "Sign Data";
    }
}
