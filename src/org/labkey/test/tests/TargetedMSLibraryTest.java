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
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.util.Arrays;

public class TargetedMSLibraryTest extends TargetedMSTest
{

    @Override
    protected void doTestSteps() throws Exception
    {
        setupAndImportData(FolderType.Library);
        verifyImportedData();
        verifyModificationSearch();
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
        assertElementPresent(Locator.xpath("//img[contains(@src, 'Chromatogram')]"));
        assertTextPresent("0 proteins");
        assertTextPresent("88 precursors");
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
