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
package org.labkey.test.components.studydesigner;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

public class ManageStudyProductsPage extends BaseManageVaccineDesignPage
{
    private String currentSection = "Immunogens";

    public ManageStudyProductsPage(BaseWebDriverTest test, boolean canInsert)
    {
        super(test);
        waitForElements(elements().studyVaccineDesignLoc, 3);
        if (canInsert)
            waitForElements(elements().outerAddRowIconLoc, 3);
    }

    public boolean canAddNewRow()
    {
        return isElementPresent(baseElements().getOutergridAddNewRowLocator(elements().immunogensLoc))
            && isElementPresent(baseElements().getOutergridAddNewRowLocator(elements().adjuvantsLoc))
            && isElementPresent(baseElements().getOutergridAddNewRowLocator(elements().challengesLoc));
    }

    public void addNewImmunogenRow(String label, String immunogenType, int rowIndex)
    {
        clickOuterAddNewRow(elements().immunogensLoc);

        setOuterTextFieldValue(elements().immunogensLoc, "Label", label, rowIndex);
        if (immunogenType != null)
            setOuterComboFieldValue(elements().immunogensLoc, "Type", immunogenType, rowIndex);
    }

    public void addNewImmunogenAntigen(String gene, String subtype, String genBankId, String sequence, int outerRowIndex, int subgridRowIndex)
    {
        currentSection = "Immunogens";
        clickSubgridAddNewRow(elements().immunogensLoc, "Antigens", outerRowIndex);

        if (gene != null)
            setSubgridComboFieldValue(elements().immunogensLoc, "Antigens", "Gene", gene, outerRowIndex, subgridRowIndex);
        if (subtype != null)
            setSubgridComboFieldValue(elements().immunogensLoc, "Antigens", "SubType", subtype, outerRowIndex, subgridRowIndex);
        if (genBankId != null)
            setSubgridTextFieldValue(elements().immunogensLoc, "Antigens", "GenBankId", genBankId, outerRowIndex, subgridRowIndex);
        if (sequence != null)
            setSubgridTextFieldValue(elements().immunogensLoc, "Antigens", "Sequence", sequence, outerRowIndex, subgridRowIndex);
    }

    public void addNewAdjuvantRow(String label, int rowIndex)
    {
        currentSection = "Adjuvants";
        clickOuterAddNewRow(elements().adjuvantsLoc);

        setOuterTextFieldValue(elements().adjuvantsLoc, "Label", label, rowIndex);
    }

    public void addNewChallengeRow(String label, String challengeSubType, int rowIndex)
    {
        currentSection = "Challenges";
        clickOuterAddNewRow(elements().challengesLoc);
        setOuterTextFieldValue(elements().challengesLoc, "Label", label, rowIndex);
        if (challengeSubType != null)
            setOuterComboFieldValue(elements().challengesLoc, "Type", challengeSubType, rowIndex);
    }

    public void addNewImmunogenDoseAndRoute(String dose, String route, int outerRowIndex, int subgridRowIndex)
    {
        currentSection = "Immunogens";
        addNewDoseAndRoute(elements().immunogensLoc, dose, route, outerRowIndex, subgridRowIndex);
    }

    public void addNewAdjuvantDoseAndRoute(String dose, String route, int outerRowIndex, int subgridRowIndex)
    {
        currentSection = "Adjuvants";
        addNewDoseAndRoute(elements().adjuvantsLoc, dose, route, outerRowIndex, subgridRowIndex);
    }

    public void addNewChallengesDoseAndRoute(String dose, String route, int outerRowIndex, int subgridRowIndex)
    {
        currentSection = "Challenges";
        addNewDoseAndRoute(elements().challengesLoc, dose, route, outerRowIndex, subgridRowIndex);
    }

    public void addNewDoseAndRoute(Locator.XPathLocator table, String dose, String route, int outerRowIndex, int subgridRowIndex)
    {
        clickSubgridAddNewRow(table, "DoseAndRoute", outerRowIndex);

        if (dose != null)
            setSubgridTextFieldValue(table, "DoseAndRoute", "Dose", dose, outerRowIndex, subgridRowIndex);
        if (route != null)
            setSubgridComboFieldValue(table, "DoseAndRoute", "Route", route, outerRowIndex, subgridRowIndex);
    }

    @Override
    protected void removeFocusAndWait()
    {
        super.removeFocusAndWait(currentSection);
    }

    public void save()
    {
        doAndWaitForPageToLoad(() -> elements().saveButton.click());
    }

    public void cancel()
    {
        elements().cancelButton.click();
    }

    protected Elements elements()
    {
        return new Elements();
    }

    protected class Elements extends BaseElements
    {
        Locator.XPathLocator immunogensLoc = Locator.tagWithClass("div", "vaccine-design-immunogens");
        Locator.XPathLocator adjuvantsLoc = Locator.tagWithClass("div", "vaccine-design-adjuvants");
        Locator.XPathLocator challengesLoc = Locator.tagWithClass("div", "vaccine-design-challenges");
    }
}

