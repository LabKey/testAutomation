/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

@Category({MS2.class, DailyA.class})
public class MS2ExportTest extends AbstractMS2ImportTest
{
    @Override
    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void setupMS2()
    {
        super.setupMS2();

        importMS2Run("DRT2", 2);
    }

    @Override
    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyMS2()
    {
        validateBulkExport();
    }

    private void validateBulkExport()
    {
        log("Test export 2 runs together");
        DataRegionTable searchRunsTable = new DataRegionTable("MS2SearchRuns", this);
        searchRunsTable.checkAllOnPage();
        clickButton("MS2 Export");

        assertTextPresent("BiblioSpec");

        Runnable tsvPeptideValidator = new Runnable()
        {
            @Override
            public void run()
            {
                assertTextPresent("Scan", "Protein", "gi|5002198|AF143203_1_interle", "1386.6970", "gi|6049221|AF144467_1_nonstru");
                assertTextBefore("K.QLDSIHVTILHK.E", "R.GRRNGPRPVHPTSHNR.Q");
                assertTextBefore("R.EADKVLVQMPSGK.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");
                assertTextPresent("\n", 86, true);
            }
        };
        validateExport("TSV", LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME, tsvPeptideValidator);
        validateExport("TSV", QUERY_PEPTIDES_VIEW_NAME, tsvPeptideValidator);

        Runnable amtPeptideValidator = new Runnable()
        {
            @Override
            public void run()
            {
                assertTextPresent("Run", "Peptide", "-.MELFSNELLYK.T", "1386.6970");
                assertTextBefore("K.QLDSIHVTILHK.E", "R.GRRNGPRPVHPTSHNR.Q");
                assertTextBefore("R.EADKVLVQMPSGK.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");
                assertTextPresent("\n", 89, true);
            }
        };
        validateExport("AMT", LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME, amtPeptideValidator);
        validateExport("AMT", QUERY_PEPTIDES_VIEW_NAME, amtPeptideValidator);

        Runnable pklPeptideValidator = new Runnable()
        {
            @Override
            public void run()
            {
                assertTextPresent("515.9 1684.0");
                assertTextNotPresent("717.4 4043.0");
                assertTextPresent("\n", 4271, true);
            }
        };
        validateExport("PKL", LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME, pklPeptideValidator);
        validateExport("PKL", QUERY_PEPTIDES_VIEW_NAME, pklPeptideValidator);

        Runnable tsvProteinProphetValidator = new Runnable()
        {
            @Override
            public void run()
            {
                assertTextPresent("gi|16078254|similar_to_riboso", "20925.0", "gi|13470573|30S_ribosomal_pro, gi|16125519|ribosomal_protein");
                assertTextPresent("\n", 7, true);
            }
        };
        validateExport("TSV", QUERY_PROTEINPROPHET_VIEW_NAME, tsvProteinProphetValidator);
        validateExport("TSV", LEGACY_PROTEIN_PROPHET_VIEW_NAME, tsvProteinProphetValidator);

        Runnable pklProteinProphetValidator = new Runnable()
        {
            @Override
            public void run()
            {
                assertTextPresent("426.9465 1 3", "174.8 2400.0");
                assertTextPresent("\n", 245, true);
            }
        };
        validateExport("PKL", QUERY_PROTEINPROPHET_VIEW_NAME, pklProteinProphetValidator);
        validateExport("PKL", LEGACY_PROTEIN_PROPHET_VIEW_NAME, pklProteinProphetValidator);
    }

    private void validateExport(String exportType, String viewName, Runnable validator)
    {
        checkRadioButton(Locator.radioButtonByNameAndValue("exportFormat", exportType));
        selectOptionByText(Locator.name("viewParams"), viewName);
        clickButton("Export");
        validator.run();
        goBack();
    }
}
