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
import org.labkey.test.Locator;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.UIContainerHelper;
import org.labkey.test.util.WikiHelper;

import java.io.File;

public class TargetedMSExperimentTest extends TargetedMSTest
{
    String CLIENT_API_CORE1 = "{"+
"   source: {"+
"		type: 'query',"+
"		schemaName: 'targetedms',"+
"		queryName: 'precursor'"+
"	},"+
"	transforms: ["+
"		{"+
"			type: 'aggregate',"+
"			filters: [ { fieldKey: ['ModifiedSequence'], type: 'startswith', value: 'L' } ],"+
"		},"+
"		{"+
"			type: 'aggregate',"+
"			groupBy: [ [\"PeptideId\", \"Sequence\"] ]"+
"			,aggregates: [ { fieldKey: ['PeptideId'], type: 'MAX', label: 'MyPeptideId' },"+
"                                       { fieldKey: ['Charge'], type: 'MAX', label: 'MyCharge' } ]"+
"			,pivot: { columns: [ [\"MyPeptideId\"] ], by: [\"Charge\"] }"+
"		}],"+
"    success: tableSuccess,"+
"    failure: function(responseData){"+
"        console.log(responseData);"+
"    }"+
"}";

    public TargetedMSExperimentTest()
    {
        // We want to use the UI when creating the project/folder so that we can verify that we get the wizard
        // that has the extra steps
        setContainerHelper(new UIContainerHelper(this));
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setupAndImportData(FolderType.Experiment);
        verifyImportedData();
        verifyModificationSearch();
        verifyProteinSearch();
        clientApiTest();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyImportedData()
    {
        clickAndWait(Locator.linkContainingText("Targeted MS Dashboard"));
        clickAndWait(Locator.linkContainingText(SKY_FILE));
        verifyRunSummaryCounts();
        verifyPeptide();
    }

    /*
     * Test for the getData client API. The test was built on the test
     * data for the TargetedMS module and therefore was placed here.
     * The test uses a script saved in the file 'getDataTest.js' which is
     * the source input of a wiki page that is run to generate a brief
     * set of output.
     */
    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void clientApiTest()
    {
        runClientAPITestCore(CLIENT_API_CORE1);
        assertElementContains(Locator.id("jsonWrapperTest"),"Peptide");
        assertElementContains(Locator.id("jsonWrapperTest"),"My Charge");

        assertElementContains(Locator.id("jsonWrapperTest"),"LLPYWQDVIAK");
        assertElementContains(Locator.id("jsonWrapperTest"),"LLSEEALPAIR");
        assertElementContains(Locator.id("jsonWrapperTest"),"LLTTIADAAK");
        assertElementContains(Locator.id("jsonWrapperTest"),"LSVQDLDLK");
        assertElementContains(Locator.id("jsonWrapperTest"),"LTSLNVVAGSDLR");
        assertElementContains(Locator.id("jsonWrapperTest"),"LVEAFQWTDK");
        assertElementContains(Locator.id("jsonWrapperTest"),"LVEDPQVIAPFLGK");
        assertElementContains(Locator.id("jsonWrapperTest"),"LWDVATGETYQR");
        assertElementContains(Locator.id("jsonWrapperTest"), "2");
    }

    private void runClientAPITestCore(String request_core)
    {
        clickAndWait(Locator.linkContainingText("Targeted MS Dashboard"));
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Wiki");

        String scriptText = getFileContents("server/test/data/api/" + "getDataTest.js");
        scriptText = scriptText.replace("REPLACEMENT_STRING", request_core);

        File getDataTestFile = new File(getApiScriptFolder(), "getDataTest.js");
        WikiHelper wikiHelper = new WikiHelper(this);
        wikiHelper.createWikiPage("getDataTest", null, "getData API Test", scriptText, true, null, false);
    }


    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyProteinSearch()
    {
        clickAndWait(Locator.linkContainingText("Targeted MS Dashboard"));
        assertTextPresent("Mass Spec Search");
        assertTextPresent("Protein Search");
        _ext4Helper.clickExt4Tab("Protein Search");
        waitForElement(Locator.name("identifier"));

        // Test fix for issue 18217
        // MRMer.zip contains the protein YAL038W.  MRMer_renamed_protein.zip contains the same protein with the
        // name YAL038W_renamed.  MRMer.zip is imported first and  a new entry is created in prot.sequences with YAL038W
        // as the bestname. MRMer_renamed_protein is imported second, and an entry is created in prot.identifiers
        // for YAL038W_renamed. A search for YAL038W_renamed should return one protein result.
        setFormElement(Locator.name("identifier"), "YAL038W_renamed");
        waitAndClickAndWait(Locator.linkWithSpan("Search"));
        waitForText("Protein Search Results");
        //waitForText("1 - 7 of 7");
        assertTextPresentInThisOrder("Protein Search", "Matching Proteins (1)", "Targeted MS Peptides");
        Assert.assertEquals(1, getElementCount(Locator.xpath("id('dataregion_PotentialProteins')/tbody/tr/td/a[contains(text(),'YAL038W')]")));
        Assert.assertEquals(7, getElementCount( Locator.xpath("//td/span/a[contains(text(), 'YAL038W')]")));
        Assert.assertEquals(1, getElementCount( Locator.xpath("//td/span/a[contains(text(), 'YAL038W_renamed')]")));
    }
}
