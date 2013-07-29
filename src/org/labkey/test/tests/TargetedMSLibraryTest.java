/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.test.tests;

import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.UIContainerHelper;

import java.util.Arrays;

@Category({DailyB.class, MS2.class})
public class TargetedMSLibraryTest extends TargetedMSTest
{
    public TargetedMSLibraryTest()
    {
        // We want to use the UI when creating the project/folder so that we can verify that we get the wizard
        // that has the extra steps
        setContainerHelper(new UIContainerHelper(this));
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setupAndImportData(FolderType.Library);
        verifyImportedData();
        verifyModificationSearch();
    }

    @Override
    protected void selectFolderType(FolderType folderType)
    {
        // Make sure that we're still in the wizard UI
        assertTextPresent("Create Project", "Users / Permissions");
        super.selectFolderType(folderType);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyImportedData()
    {
        clickAndWait(Locator.linkContainingText("Targeted MS Dashboard"));

        log("Verifying expected protein/peptide counts");
        clickAndWait(Locator.linkContainingText("Targeted MS Dashboard"));
        verifyProteinsWebPart();
        verifyPeptidesWebPart();
        verifyChromatogramLibraryDownloadWebPart();
    }

    private void verifyChromatogramLibraryDownloadWebPart()
    {
        clickAndWait(Locator.linkContainingText("Targeted MS Dashboard"));
        assertElementPresent(Locator.xpath("//img[contains(@src, 'graphLibraryStatistics.view')]"));
        assertTextPresent("44 peptides");
        assertTextPresent("296 ranked transitions");
        assertElementPresent(Locator.linkWithText("Download"));
    }

    private void verifyPeptidesWebPart()
    {
        verifyPeptide();
    }

    private void verifyProteinsWebPart()
    {
        assertTextPresent("YAL038W");
        assertTextPresent("YML028W");
        assertTextPresent("YKL035W");
    }

}
