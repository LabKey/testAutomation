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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.External;
import org.labkey.test.categories.LabModule;
import org.labkey.test.categories.ONPRC;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4HelperWD;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.ext4cmp.Ext4FieldRefWD;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * User: bimber
 * Date: 12/9/12
 * Time: 11:15 PM
 */
@Category({External.class, ONPRC.class, LabModule.class})
public class GenotypeAssaysTest extends AbstractLabModuleAssayTest
{
    private static final String ASSAY_NAME = "Genotype Test";
    private static final String SSP_ASSAY_NAME = "SSP Assay Test";
    private static final String REF_NT_NAME = "GenotypeAssaysTest";
    private static final String[][] SSP_TEST_DATA1 = new String[][]{
        {"Subject Id", "Sample Type", "Freezer Id", "Lane Number", "Method", "Sample Date", "Primer Pair", "Result", "Comment", "Sequence"},
        {"Subj1", "gDNA", "3", "1", "method", "1/1/2011", "TestPrimer1", "POS", "", "ATGT"},
        {"Subj1", "RNA", "2", "2", "method", "1/2/2011", "TestPrimer1", "NEG"},
        {"Subj1", "gDNA", "1", "3", "method", "3/3/2012", "TestPrimer2", "IND"},
        {"Subj1", "gDNA", "1", "3", "method", "3/3/2012", "TestPrimer2", "Positive"},
        {"Subj1", "gDNA", "1", "3", "method", "3/3/2012", "TestPrimer2", "Negative"},
        {"Subj1", "gDNA", "1", "3", "method", "3/3/2012", "TestPrimer3", "+"},
        {"Subj1", "gDNA", "1", "3", "method", "3/3/2012", "TestPrimer4", "-", "Comment"},
        {"Subj1", "gDNA", "1", "3", "method", "3/3/2012", "TestPrimer4", "pos"},
        {"Subj1", "gDNA", "1", "3", "method", "3/3/2012", "TestPrimer3", "neg"}
    };

    private String[][] SSP_PIVOT_DATA = new String[][]{
        {"Subject Id","Freezer Id","Lane Number","Method","Sample Date","TestPrimer4","Sequence","TestPrimer1","TestPrimer2","TestPrimer5","Comment"},
        {"Subj1","1","1","method","1/2/2011","","atg","POS","","","comment"},
        {"Subj1","1","2","method","2/5/2012","+","","pos","Y","IND","comment"},
        {"Subj1","1","3","method","3/16/2012","","","Positive","","positive","comment"},
        {"Subj1","1","4","method","4/25/2012","-","","Neg","N","","comment"},
        {"Subj2","1","5","method","6/4/2012","","","Fail","","","comment"}
    };
            
    public GenotypeAssaysTest()
    {
        PROJECT_NAME = "GenotypeAssaysVerifyProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    private void createSSPPrimers() throws Exception
    {
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        int i = 1;
        while (i <= 5)
        {
            String name = "TestPrimer" + i;
            SelectRowsCommand cmd = new SelectRowsCommand("genotypeassays", "primer_pairs");
            cmd.addFilter(new Filter("primerName", name));
            SelectRowsResponse resp = cmd.execute(cn, getProjectName());
            if (resp.getRowCount().intValue() == 0)
            {
                log("creating primer: " + name);
                InsertRowsCommand insert = new InsertRowsCommand("genotypeassays", "primer_pairs");
                Map<String, Object> row = new HashMap<>();
                row.put("primerName", name);
                row.put("ref_nt_name", REF_NT_NAME);
                insert.addRow(row);
                insert.execute(cn, getProjectName());
            }

            i++;
        }
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setUpTest();
        sspImportTest();
        sspPivotedImportTest();
        UCDavisTest();
    }

    @Override
    protected void setUpTest() throws Exception
    {
        super.setUpTest();
        createSSPPrimers();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        if (afterTest)
        {
            try
            {
                Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
                SelectRowsCommand cmd = new SelectRowsCommand("genotypeassays", "primer_pairs");
                cmd.addFilter(new Filter("ref_nt_name", REF_NT_NAME));
                SelectRowsResponse resp = cmd.execute(cn, getProjectName());
                if (resp.getRowCount().intValue() > 0)
                {
                    log("deleting created primers");
                    DeleteRowsCommand delete = new DeleteRowsCommand("genotypeassays", "primer_pairs");
                    for (Map<String, Object> row : resp.getRows())
                    {
                        delete.addRow(row);
                    }
                    delete.execute(cn, getProjectName());
                }
            }
            catch (CommandException e)
            {
                //ignore, since this will fail when this runs prior to the project being created
                throw new RuntimeException(e);
            }
            catch (SocketTimeoutException e)
            {
                throw new TestTimeoutException(e);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        super.doCleanup(afterTest);
    }

    private void sspImportTest()
    {
        log("Verifying Basic SSP Import");
        _helper.goToAssayResultImport(SSP_ASSAY_NAME, false);

        //a proxy for page loading
        _helper.waitForField("Sample Type");

        Ext4FieldRefWD.getForLabel(this, "Run Description").setValue("Description");

        assertEquals("Incorrect value for field", "gDNA", Ext4FieldRefWD.getForLabel(this, "Sample Type").getValue());

        Ext4FieldRefWD textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRefWD.class);
        StringBuilder sb = new StringBuilder();
        for (String[] row : SSP_TEST_DATA1)
        {
            sb.append(StringUtils.join(row, "\t")).append(System.getProperty("line.separator"));
        }
        String text = sb.toString();

        log("Trying to save invalid data");
        String errorText = text.replaceAll("Positive", "NotRealResult");
        textarea.setValue(errorText);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4HelperWD.ext4Window("Upload Failed"));
        waitAndClick(Locator.ext4Button("OK"));
        assertTextPresent("There were errors in the upload");
        waitForText("Unknown value for result: NotRealResult for primer: TestPrimer2");

        log("Saving valid data");
        textarea.setValue(text);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4HelperWD.ext4Window("Success"));
        waitAndClick(Locator.ext4Button("OK"));
        waitForText("Import Samples");

        log("Verifying results");
        _helper.clickNavPanelItemAndWait(SSP_ASSAY_NAME + " Runs:", 1);
        waitAndClickAndWait(Locator.linkContainingText("view results"));

        List<String[]> expected = new ArrayList<>();
        expected.add(new String[]{"Subj1", "<3>", "2011-01-01", "POS", "TestPrimer1"});
        expected.add(new String[]{"Subj1", "<2>", "2011-01-02", "NEG", "TestPrimer1"});
        expected.add(new String[]{"Subj1", "<1>", "2012-03-03", "IND", "TestPrimer2"});
        expected.add(new String[]{"Subj1", "<1>", "2012-03-03", "POS", "TestPrimer2"});
        expected.add(new String[]{"Subj1", "<1>", "2012-03-03", "NEG", "TestPrimer2"});
        expected.add(new String[]{"Subj1", "<1>", "2012-03-03", "POS", "TestPrimer3"});
        expected.add(new String[]{"Subj1", "<1>", "2012-03-03", "NEG", "TestPrimer4"});
        expected.add(new String[]{"Subj1", "<1>", "2012-03-03", "POS", "TestPrimer4"});
        expected.add(new String[]{"Subj1", "<1>", "2012-03-03", "NEG", "TestPrimer3"});

        verifySSPResults(expected);
    }

    private void verifySSPResults(List<String[]> expected)
    {
        DataRegionTable results = new DataRegionTable("Data", this);
        assertEquals("Incorrect row count", expected.size(), results.getDataRowCount());
        waitForText("TestPrimer2"); //proxy for DR load
        log("DataRegion column count was: " + results.getColumnCount());

        //recreate the DR to see if this removes intermittent test failures
        results = new DataRegionTable(results.getTableName(), this);

        int i = 0;
        while (i < expected.size())
        {
            String[] expectedVals = expected.get(i);
            String subjectId = results.getDataAsText(i, "Subject Id");
            String date = results.getDataAsText(i, "Sample Date");
            String pair = results.getDataAsText(i, "Primer Pair");
            String result = results.getDataAsText(i, "Result");
            String freezer = results.getDataAsText(i, "Freezer Id");

            assertEquals("Incorrect subjectId on row: " + i, expectedVals[0], subjectId);
            assertEquals("Incorrect freezerId on row: " + i, expectedVals[1], freezer);
            assertEquals("Incorrect date on row: " + i, expectedVals[2], date);
            assertEquals("Incorrect result on row: " + i, expectedVals[3], result);
            assertEquals("Incorrect primer pair on row: " + i, expectedVals[4], pair);

            i++;
        }
    }

    private void sspPivotedImportTest()
    {
        log("Verifying SSP Import Using Pivoted Input");
        _helper.goToAssayResultImport(SSP_ASSAY_NAME, false);

        //a proxy for page loading
        _helper.waitForField("Sample Type");

        //switch import method
        Ext4FieldRefWD field = Ext4FieldRefWD.getForBoxLabel(this, "Pivoted By Allele Name");
        field.setChecked(true);

        _helper.waitForField("Sample Type"); //form is re-rendered when import method changed
        Ext4FieldRefWD.getForLabel(this, "Run Description").setValue("Description");

        assertEquals("Incorrect value for field", "gDNA", Ext4FieldRefWD.getForLabel(this, "Sample Type").getValue());

        Ext4FieldRefWD textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRefWD.class);
        StringBuilder sb = new StringBuilder();
        for (String[] row : SSP_PIVOT_DATA)
        {
            sb.append(StringUtils.join(row, "\t")).append(System.getProperty("line.separator"));
        }
        String text = sb.toString();

        //TODO: verify columns in template

        log("Trying to save invalid data");
        String errorText = text.replaceAll("Positive", "NotRealResult");
        textarea.setValue(errorText);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4HelperWD.ext4Window("Upload Failed"));
        waitAndClick(Locator.ext4Button("OK"));
        assertTextPresent("There were errors in the upload");
        waitForText("Unknown value for result: NotRealResult for primer: TestPrimer1");

        String originalPrimer = "TestPrimer1";
        String fakePrimer = "FakePrimers";
        errorText = text.replaceAll(originalPrimer, fakePrimer);
        textarea.setValue(errorText);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4HelperWD.ext4Window("Upload Failed"));
        waitAndClick(Locator.ext4Button("OK"));
        assertTextPresent("There were errors in the upload");
        waitForText("Unknown column: " + fakePrimer);

        log("Saving valid data");
        textarea.setValue(text);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4HelperWD.ext4Window("Success"));
        waitAndClick(Locator.ext4Button("OK"));
        waitForText("Import Samples");

        log("Verifying results");
        _helper.clickNavPanelItemAndWait(SSP_ASSAY_NAME + " Runs:", 1);
        waitAndClickAndWait(Locator.linkContainingText("view results"));

        List<String[]> expected = new ArrayList<>();
        expected.add(new String[]{"Subj1", "<1>", "2011-01-02", "POS", "TestPrimer1"});
        expected.add(new String[]{"Subj1", "<1>", "2012-02-05", "POS", "TestPrimer4"});
        expected.add(new String[]{"Subj1", "<1>", "2012-02-05", "POS", "TestPrimer1"});
        expected.add(new String[]{"Subj1", "<1>", "2012-02-05", "POS", "TestPrimer2"});
        expected.add(new String[]{"Subj1", "<1>", "2012-02-05", "IND", "TestPrimer5"});
        expected.add(new String[]{"Subj1", "<1>", "2012-03-16", "POS", "TestPrimer1"});
        expected.add(new String[]{"Subj1", "<1>", "2012-03-16", "POS", "TestPrimer5"});
        expected.add(new String[]{"Subj1", "<1>", "2012-04-25", "NEG", "TestPrimer4"});
        expected.add(new String[]{"Subj1", "<1>", "2012-04-25", "NEG", "TestPrimer1"});
        expected.add(new String[]{"Subj1", "<1>", "2012-04-25", "NEG", "TestPrimer2"});
        expected.add(new String[]{"Subj2", "<1>", "2012-06-04", "FAIL", "TestPrimer1"});

        verifySSPResults(expected);
    }

    @Override
    protected List<Pair<String, String>> getAssaysToCreate()
    {
        List<Pair<String, String>> assays = new ArrayList<>();
        assays.add(Pair.of("SSP Typing", SSP_ASSAY_NAME));
        assays.add(Pair.of("Genotype Assay", ASSAY_NAME));

        return assays;
    }

    private void UCDavisTest()
    {
        log("Verifying UC Davis STR Import");
        _helper.goToAssayResultImport(ASSAY_NAME, false);

        //a proxy for page loading
        _helper.waitForField("Instrument");

        //switch import method
        Ext4FieldRefWD field = Ext4FieldRefWD.getForBoxLabel(this, "UC Davis STR");
        field.setChecked(true);
        Locator btn = Locator.linkContainingText("Download Example Data");
        waitForElement(btn);

        Ext4FieldRefWD.getForLabel(this, "Run Description").setValue("Description");

        //assertEquals("Incorrect value for field", "LC480", Ext4FieldRefWD.getForLabel(this, "Instrument").getValue());
        waitAndClick(btn);

        Ext4FieldRefWD textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRefWD.class);
        String text = _helper.getExampleData();

        log("Trying to save invalid data");
        String errorText = text.replaceAll("13294", ",");
        textarea.setValue(errorText);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4HelperWD.ext4Window("Upload Failed"));
        click(Locator.ext4Button("OK"));
        assertTextPresent("There were errors in the upload");
        assertTextPresent("Missing subject Id");

        log("Saving valid data");
        textarea.setValue(text);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4HelperWD.ext4Window("Success"));
        click(Locator.ext4Button("OK"));
        waitForText("Import Samples");

        log("Verifying results");
        _helper.clickNavPanelItemAndWait(ASSAY_NAME + " Runs:", 1);
        waitAndClickAndWait(Locator.linkContainingText("view results"));

        DataRegionTable results = new DataRegionTable("Data", this);

        int totalRows = 100; //current page size
        List<String[]> expected = new ArrayList<>();
        expected.add(new String[]{"13294", "D10S1412", "157"});
        expected.add(new String[]{"13294", "D10S1412", "157"});
        expected.add(new String[]{"13294", "D11S2002", "260"});
        expected.add(new String[]{"13294", "D11S2002", "264"});
        expected.add(new String[]{"13294", "D11S925", "308"});
        expected.add(new String[]{"13294", "D11S925", "312"});
        expected.add(new String[]{"13294", "D12S364", "282"});
        expected.add(new String[]{"13294", "D12S364", "290"});
        expected.add(new String[]{"13294", "D12S67", "117"});
        expected.add(new String[]{"13294", "D12S67", "204"});
        expected.add(new String[]{"13294", "D13S765", "220"});
        expected.add(new String[]{"13294", "D13S765", "228"});
        expected.add(new String[]{"13294", "D15S823", "329"});
        expected.add(new String[]{"13294", "D15S823", "357"});
        expected.add(new String[]{"13294", "D16S403", "164"});
        expected.add(new String[]{"13294", "D16S403", "174"});
        expected.add(new String[]{"13294", "D17S1300", "228"});
        expected.add(new String[]{"13294", "D17S1300", "276"});
        expected.add(new String[]{"13294", "D18S537", "162"});
        expected.add(new String[]{"13294", "D18S537", "162"});
        expected.add(new String[]{"13294", "D18S72", "308"});
        expected.add(new String[]{"13294", "D18S72", "308"});
        expected.add(new String[]{"13294", "D1S548", "206"});
        expected.add(new String[]{"13294", "D1S548", "206"});
        expected.add(new String[]{"13294", "D2S1333", "277"});
        expected.add(new String[]{"13294", "D2S1333", "301"});
        expected.add(new String[]{"13294", "D3S1768", "205"});
        expected.add(new String[]{"13294", "D3S1768", "225"});
        expected.add(new String[]{"13294", "D4S2365", "283"});
        expected.add(new String[]{"13294", "D4S2365", "291"});
        expected.add(new String[]{"13294", "D4S413", "131"});
        expected.add(new String[]{"13294", "D4S413", "151"});
        expected.add(new String[]{"13294", "D5S1457", "136"});
        expected.add(new String[]{"13294", "D5S1457", "136"});
        expected.add(new String[]{"13294", "D6S1691", "197"});
        expected.add(new String[]{"13294", "D6S1691", "215"});
        expected.add(new String[]{"13294", "D6S276", "233"});
        expected.add(new String[]{"13294", "D6S276", "233"});
        expected.add(new String[]{"13294", "D6S291", "206"});
        expected.add(new String[]{"13294", "D6S291", "208"});
        expected.add(new String[]{"13294", "D6S501", "176"});
        expected.add(new String[]{"13294", "D6S501", "184"});
        expected.add(new String[]{"13294", "D7S513", "209"});
        expected.add(new String[]{"13294", "D7S513", "217"});
        expected.add(new String[]{"13294", "D7S794", "108"});
        expected.add(new String[]{"13294", "D7S794", "108"});
        expected.add(new String[]{"13294", "D8S1106", "148"});
        expected.add(new String[]{"13294", "D8S1106", "152"});
        expected.add(new String[]{"13294", "D9S921", "187"});
        expected.add(new String[]{"13294", "D9S921", "191"});
        expected.add(new String[]{"13294", "DXS2506", "262"});
        expected.add(new String[]{"13294", "DXS2506", "262"});
        expected.add(new String[]{"13294", "MFGT21", "115"});
        expected.add(new String[]{"13294", "MFGT21", "125"});
        expected.add(new String[]{"13294", "MFGT22", "110"});
        expected.add(new String[]{"13294", "MFGT22", "110"});
        expected.add(new String[]{"15227", "D10S1412", "157"});
        expected.add(new String[]{"15227", "D10S1412", "157"});
        expected.add(new String[]{"15227", "D11S2002", "256"});
        expected.add(new String[]{"15227", "D11S2002", "260"});
        expected.add(new String[]{"15227", "D11S925", "308"});
        expected.add(new String[]{"15227", "D11S925", "338"});
        expected.add(new String[]{"15227", "D12S364", "290"});
        expected.add(new String[]{"15227", "D12S364", "294"});
        expected.add(new String[]{"15227", "D12S67", "113"});
        expected.add(new String[]{"15227", "D12S67", "204"});
        expected.add(new String[]{"15227", "D13S765", "256"});
        expected.add(new String[]{"15227", "D13S765", "260"});
        expected.add(new String[]{"15227", "D15S823", "329"});
        expected.add(new String[]{"15227", "D15S823", "349"});
        expected.add(new String[]{"15227", "D16S403", "158"});
        expected.add(new String[]{"15227", "D16S403", "164"});
        expected.add(new String[]{"15227", "D17S1300", "228"});
        expected.add(new String[]{"15227", "D17S1300", "252"});
        expected.add(new String[]{"15227", "D18S537", "162"});
        expected.add(new String[]{"15227", "D18S537", "178"});
        expected.add(new String[]{"15227", "D18S72", "308"});
        expected.add(new String[]{"15227", "D18S72", "308"});
        expected.add(new String[]{"15227", "D1S548", "190"});
        expected.add(new String[]{"15227", "D1S548", "202"});
        expected.add(new String[]{"15227", "D2S1333", "289"});
        expected.add(new String[]{"15227", "D2S1333", "301"});
        expected.add(new String[]{"15227", "D3S1768", "205"});
        expected.add(new String[]{"15227", "D3S1768", "209"});
        expected.add(new String[]{"15227", "D4S2365", "279"});
        expected.add(new String[]{"15227", "D4S2365", "283"});
        expected.add(new String[]{"15227", "D4S413", "135"});
        expected.add(new String[]{"15227", "D4S413", "145"});
        expected.add(new String[]{"15227", "D5S1457", "132"});
        expected.add(new String[]{"15227", "D5S1457", "136"});
        expected.add(new String[]{"15227", "D6S1691", "197"});
        expected.add(new String[]{"15227", "D6S1691", "197"});
        expected.add(new String[]{"15227", "D6S276", "215"});
        expected.add(new String[]{"15227", "D6S276", "225"});
        expected.add(new String[]{"15227", "D6S291", "208"});
        expected.add(new String[]{"15227", "D6S291", "208"});
        expected.add(new String[]{"15227", "D6S501", "180"});
        expected.add(new String[]{"15227", "D6S501", "184"});
        expected.add(new String[]{"15227", "D7S513", "193"});
        expected.add(new String[]{"15227", "D7S513", "210"});

        assertEquals("Incorrect row count", totalRows, results.getDataRowCount());
        log("DataRegion column count was: " + results.getColumnCount());

        //recreate the DR to see if this removes intermittent test failures
        results = new DataRegionTable(results.getTableName(), this);

        int i = 0;
        while (i < totalRows)
        {
            String subjectId = results.getDataAsText(i, "Subject Id");
            String marker = results.getDataAsText(i, "Marker");
            String result = results.getDataAsText(i, "Result");
            String[] expectedVals = expected.get(i);

            assertEquals("Incorrect subjectId on row: " + i, expectedVals[0], subjectId);
            assertEquals("Incorrect marker on row: " + i, expectedVals[1], marker);
            assertEquals("Incorrect result on row: " + i, expectedVals[2], result);

            i++;
        }
    }

    @Override
    protected List<String> getEnabledModules()
    {
        List<String> modules = new ArrayList<>();
        modules.add("GenotypeAssays");
        return modules;
    }
}