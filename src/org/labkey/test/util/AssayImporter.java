/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

public class AssayImporter
{
    private BaseWebDriverTest test;
    private AssayImportOptions options;

    public AssayImporter(BaseWebDriverTest test, AssayImportOptions options)
    {
        this.test = test;
        this.options = options;
    }

    public void doImport()
    {
        Locator linkLocator = Locator.linkContainingText("Import Data");
        if (test.isElementPresent(linkLocator))
            test.clickAndWait(linkLocator);
        else
            test.clickButton("Import Data");

        if (!options.isUseDefaultResolver())
        {
            if (options.getVisitResolver() == AssayImportOptions.VisitResolverType.SpecimenIDParticipantVisit)
            {
                test.checkRadioButton(Locator.radioButtonByNameAndValue("participantVisitResolver", AssayImportOptions.VisitResolverType.SpecimenID.name()));
                Locator checkBox = Locator.checkboxByName("includeParticipantAndVisit");
                test.waitForElement(checkBox);
                test.checkCheckbox(checkBox);
            }
            else
                test.checkRadioButton(Locator.radioButtonByNameAndValue("participantVisitResolver", options.getVisitResolver().name()));
        }
        else
        {
            switch (options.getVisitResolver())
            {
                case LookupList:
                    test.assertChecked(Locator.radioButtonByNameAndValue("ThawListType", "List"));
                    test.waitForFormElementToNotEqual(Locator.name("ThawListList-QueryName"), "");
                    break;
                case LookupText:
                    test.assertChecked(Locator.radioButtonByNameAndValue("ThawListType", "Text"));
                    break;
                case SpecimenIDParticipantVisit:
                    test.assertChecked(Locator.radioButtonByNameAndValue("participantVisitResolver", AssayImportOptions.VisitResolverType.SpecimenID.name()));
                    break;
                default:
                    test.assertChecked(Locator.radioButtonByNameAndValue("participantVisitResolver", options.getVisitResolver().name()));
            }
        }

        if (options.isResetDefaults())
        {
            test.clickButton("Reset Default Values");
            test._ext4Helper.waitForMaskToDisappear(); // Wait for sample list combo boxes to load
        }

        test.clickButton("Next");

        if (options.getAssayId() != null)
            test.setFormElement(Locator.name("name"), options.getAssayId());

        test.setFormElement(Locator.name("cutoff1"), options.getCutoff1());
        if (options.getCutoff2() != null)
            test.setFormElement(Locator.name("cutoff2"), options.getCutoff2());
        if (options.getCutoff3() != null)
            test.setFormElement(Locator.name("cutoff3"), options.getCutoff3());

        if (options.getVirusName() != null)
            test.setFormElement(Locator.name("virusName"), options.getVirusName());
        if (options.getVirusId() != null)
            test.setFormElement(Locator.name("virusID"), options.getVirusId());

        test.selectOptionByText(Locator.name("curveFitMethod"), options.getCurveFitMethod());

        if (options.getMetadataFile() == null)
        {
            // populate the sample well group information
            for (int i = 0; i < options.getPtids().length; i++)
            {
                test.setFormElement(Locator.name("specimen" + (i + 1) + "_ParticipantID"), options.getPtids()[i]);
            }

            for (int i = 0; i < options.getVisits().length; i++)
            {
                test.setFormElement(Locator.name("specimen" + (i + 1) + "_VisitID"), options.getVisits()[i]);
            }

            for (int i = 0; i < options.getInitialDilutions().length; i++)
            {
                test.setFormElement(Locator.name("specimen" + (i + 1) + "_InitialDilution"), options.getInitialDilutions()[i]);
            }

            for (int i = 0; i < options.getDilutionFactors().length; i++)
            {
                test.setFormElement(Locator.name("specimen" + (i + 1) + "_Factor"), options.getDilutionFactors()[i]);
            }

            for (int i = 0; i < options.getMethods().length; i++)
            {
                test.selectOptionByText(Locator.name("specimen" + (i + 1) + "_Method"), options.getMethods()[i]);
            }

            for (int i = 0; i < options.getDates().length; i++)
            {
                test.setFormElement(Locator.name("specimen" + (i + 1) + "_Date"), options.getDates()[i]);
            }

            for (int i = 0; i < options.getSampleIds().length; i++)
            {
                test.setFormElement(Locator.name("specimen" + (i + 1) + "_SpecimenID"), options.getSampleIds()[i]);
            }
        }
        else
        {
            test.setFormElement(Locator.name("__sampleMetadataFile__"), options.getMetadataFile());
        }

        test.setFormElement(Locator.name("__primaryFile__"), options.getRunFile());
        test.clickButton("Save and Finish", 60000);
    }
}
