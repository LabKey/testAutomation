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
package org.labkey.test.tests.core.admin;

import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.core.admin.AllowedFileExtensionAdminPage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class AllowedFileExtensionBaseTest extends BaseWebDriverTest
{

    protected final File TAR_FILE = TestFileUtils.getSampleData("fileTypes/targz_sample.tar.gz");
    protected final File CSV_FILE = TestFileUtils.getSampleData("fileTypes/csv_sample.csv");
    protected final File TSV_FILE = TestFileUtils.getSampleData("fileTypes/tsv_sample.tsv");
    protected final File TXT_FILE = TestFileUtils.getSampleData("fileTypes/sample.txt");
    protected final File XLS_FILE = TestFileUtils.getSampleData("fileTypes/xls_sample.xls");
    protected final File XLSX_FILE = TestFileUtils.getSampleData("fileTypes/xlsx_sample.xlsx");

    protected final Map<String, File> fileMap = Map.of(
            ".tar.gz", TAR_FILE,
            ".xls", XLS_FILE,
            ".tsv", TSV_FILE,
            ".csv", CSV_FILE,
            ".txt", TXT_FILE,
            ".xlsx", XLSX_FILE
    );

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        try
        {
            AllowedFileExtensionAdminPage.deleteAllAllowedFileExtension(createDefaultConnection());
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected AllowedFileExtensionAdminPage setAllowedExtensions(List<String> allowedTypes, List<String> expectedTypes)
    {
        AllowedFileExtensionAdminPage allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();

        for (String extension : allowedTypes)
        {
            allowedFileExtensionAdminPage.setExtension(extension);
            allowedFileExtensionAdminPage.clickSaveExtension();
        }

        List<Input> extensions = allowedFileExtensionAdminPage.getAllowedExtensions();

        checker().withScreenshot()
                .verifyEqualsSorted("List of 'Allowed extensions' is not as expected.",
                        expectedTypes, extensions.stream().map(Input::getValue).toList());

        return allowedFileExtensionAdminPage;
    }

}
