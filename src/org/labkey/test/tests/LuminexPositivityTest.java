/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PerlHelper;

import java.io.File;
import java.util.List;

/**
 * User: elvan
 * Date: 2/21/12
 * Time: 1:37 PM
 */
public class LuminexPositivityTest extends LuminexTest
{

    private String assayName = "Positivity";


    protected void ensureConfigured()
    {
        PerlHelper perlHelper = new PerlHelper(this);
        if(!perlHelper.ensurePerlConfig())
            Assert.fail("No Perl engine");

        setUseXarImport(true);
        super.ensureConfigured();
    }


    protected void runUITests()
    {
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + "/resources/transformscripts/description_parsing_example.pl"), 0);
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE1), 1);

        //clickButton("Save & Close");
        //TODO: Just 'Save & Close' to avoid timing issues. Blocked
        saveAssay();
        sleep(5000);

        // Test positivity data upload with 3x Fold Change
        uploadPositivityFile(assayName + " 3x Fold Change", "1", "3", false);
        String[] posWells = new String[] {"A2", "B2", "A6", "B6", "A8", "B8", "A9", "B9"};
        checkPositivityValues("positive", posWells.length, posWells);
        String[] negWells = new String[] {"A3", "B3", "A5", "B5", "A10", "B10", "A13", "B13"};
        checkPositivityValues("negative", negWells.length, negWells);
        checkDescriptionParsingForPositivityXLS();

        // Test positivity data upload with 5x Fold Change
        uploadPositivityFile(assayName + " 5x Fold Change", "1", "5", false);
        posWells = new String[] {"A8", "B8", "A9", "B9"};
        checkPositivityValues("positive", posWells.length, posWells);
        negWells = new String[] {"A2", "B2", "A3", "B3", "A5", "B5", "A6", "B6", "A10", "B10", "A13", "B13"};
        checkPositivityValues("negative", negWells.length, negWells);
        checkDescriptionParsingForPositivityXLS();

        // Test positivity data upload w/out a baseline visit and/or fold change
        uploadPositivityFile(assayName + " No Fold Change", "1", "", false);
        // should result in error for having a base visit without a fold change
        assertTextPresent("An error occurred when running the script (exit code: 1).", "Error: No value provided for 'Positivity Fold Change'.");
        clickButton("Cancel");
        // TODO: set positivity threshold analyte props to 1000
        uploadPositivityFile(assayName + " No Base Visit", "", "", false);
        posWells = new String[] {"A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A6", "B6", "A7", "B7", "A8", "B8", "A9", "B9"};
        checkPositivityValues("positive", posWells.length, posWells);
        negWells = new String[] {"A5", "B5", "A10", "B10", "A13", "B13"};
        checkPositivityValues("negative", negWells.length, negWells);
        checkDescriptionParsingForPositivityXLS();
    }

    /**
     * This function verify three specific descriptions present in positivity.xls are present in the
     * data grid and that they have been correctly processed
     */
    private void checkDescriptionParsingForPositivityXLS()
    {
        //TODO:  bug
//        checkDescriptionParsing("123400001 1 2012-10-01", "", "123400001", "1", "2012-10-01");
        checkDescriptionParsing("123400002,2,1/15/2012", "", "123400002", "2.0", "2012-01-15");
        checkDescriptionParsing("P562, Wk 48, 7-27-2011", "", "P562", "48.0", "2011-07-27");

    }

    private void checkDescriptionParsing(String description, String specimenID, String participantID, String visitID, String date)
    {
        DataRegionTable drt = new DataRegionTable("Data", this);
        drt.ensureColumnsPresent("Description", "Specimen ID", "Participant ID", "Visit ID", "Date");
        int rowID = drt.getIndexWhereDataAppears(description, "Description");
        Assert.assertEquals(specimenID, drt.getDataAsText(rowID, "Specimen ID"));
        Assert.assertEquals(participantID, drt.getDataAsText(rowID, "Participant ID"));
        Assert.assertEquals(visitID, drt.getDataAsText(rowID, "Visit ID"));
        Assert.assertEquals(date, drt.getDataAsText(rowID, "Date"));
    }

    private void checkPositivityValues(String type, int numExpected, String[] positivityWells)
    {
        // verify that we are already on the Data results view
        assertTextPresent(TEST_ASSAY_LUM+ " Results");

        assertTextPresent(type, numExpected);

        DataRegionTable drt = new DataRegionTable("Data", this);
        List<String> posivitiy = drt.getColumnDataAsText("Positivity");
        List<String> wells = drt.getColumnDataAsText("Well");

        for(String well : positivityWells)
        {
            int i = wells.indexOf(well);
            Assert.assertEquals(type, posivitiy.get(i));
        }

        //verify correctly parsed the description file into participant id, visit id, and date
    }
}
