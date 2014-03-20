package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

/**
 * Created by klum on 3/3/14.
 */
public class DilutionAssayHelper
{
    private BaseWebDriverTest _test;

    public DilutionAssayHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void verifyDataIdentifiers(AssayImportOptions.VisitResolverType type, String ptidSuffix)
    {
        _test.log("Verifying data identifiers");

        // verify menu items
        switch (type)
        {
            case ParticipantDate:
                openDataIdentifierMenu();

                _test.waitForElement(Locator.extMenuItemDisabled("Specimen ID"));
                _test.waitForElement(Locator.extMenuItemDisabled("Participant ID / Visit"));
                _test.waitForElement(Locator.extMenuItemEnabled("Participant ID / Date"));
                _test.waitForElement(Locator.extMenuItemDisabled("Specimen ID / Participant ID / Visit"));
                _test.click(Locator.extMenuItemEnabled("Participant ID / Date"));
                verifyDataIdentifierText(type, ptidSuffix);
                break;
            case ParticipantVisit:
                openDataIdentifierMenu();

                _test.waitForElement(Locator.extMenuItemDisabled("Specimen ID"));
                _test.waitForElement(Locator.extMenuItemEnabled("Participant ID / Visit"));
                _test.waitForElement(Locator.extMenuItemDisabled("Participant ID / Date"));
                _test.waitForElement(Locator.extMenuItemDisabled("Specimen ID / Participant ID / Visit"));
                _test.click(Locator.extMenuItemEnabled("Participant ID / Visit"));
                verifyDataIdentifierText(type, ptidSuffix);
                break;
            case ParticipantVisitDate:
                openDataIdentifierMenu();

                _test.waitForElement(Locator.extMenuItemDisabled("Specimen ID"));
                _test.waitForElement(Locator.extMenuItemEnabled("Participant ID / Visit"));
                _test.waitForElement(Locator.extMenuItemEnabled("Participant ID / Date"));
                _test.waitForElement(Locator.extMenuItemDisabled("Specimen ID / Participant ID / Visit"));

                // click and verify the identifiers on the page
                _test.click(Locator.extMenuItemEnabled("Participant ID / Date"));
                verifyDataIdentifierText(AssayImportOptions.VisitResolverType.ParticipantDate, ptidSuffix);
                break;
            case SpecimenIDParticipantVisit:
                openDataIdentifierMenu();

                _test.waitForElement(Locator.extMenuItemEnabled("Specimen ID"));
                _test.waitForElement(Locator.extMenuItemEnabled("Participant ID / Visit"));
                _test.waitForElement(Locator.extMenuItemDisabled("Participant ID / Date"));
                _test.waitForElement(Locator.extMenuItemEnabled("Specimen ID / Participant ID / Visit"));

                // click and verify the identifiers on the page
                _test.click(Locator.extMenuItemEnabled("Specimen ID / Participant ID / Visit"));
                verifyDataIdentifierText(AssayImportOptions.VisitResolverType.SpecimenIDParticipantVisit, ptidSuffix);
                break;
        }

    }

    /**
     * Renders the data identifier menu, so we can validate the menu items
     */
    private void openDataIdentifierMenu()
    {
        Locator menu = Locator.linkContainingText("Change Graph Options");
        _test.mouseOver(menu);
        _test.click(menu);

        Locator parentLocator = Locator.menuItem("Data Identifiers");
        _test.waitForElement(parentLocator, _test.WAIT_FOR_JAVASCRIPT);
        _test.mouseOver(parentLocator);
    }

    private void verifyDataIdentifierText(AssayImportOptions.VisitResolverType type, @Nullable String ptidSuffix)
    {
        String format = "";

        switch (type)
        {
            case ParticipantDate:
                format = (ptidSuffix == null) ? "ptid %1$d, 2014-02-28" : "ptid %1$d %2$s, 2014-02-28";
                break;
            case ParticipantVisit:
                format = (ptidSuffix == null) ? "ptid %1$d, Vst %3$.1f" : "ptid %1$d %2$s, Vst %3$.1f";
                break;
            case ParticipantVisitDate:
                format = (ptidSuffix == null) ? "ptid %1$d, 2014-02-28" : "ptid %1$d %2$s, 2014-02-28";
                break;
            case SpecimenIDParticipantVisit:
                format = (ptidSuffix == null) ? "SPECIMEN-%1$d, ptid %1$d, Vst %3$.1f" : "SPECIMEN-%1$d, ptid %1$d %2$s, Vst %3$.1f";
                break;
        }

        for (int i=1; i < 6; i++)
        {
            String text = String.format(format, i, ptidSuffix, (double)i);

            // cutoff
            _test.waitForElement(Locator.xpath("//table").withClass("cutoff-table").append("//td").withClass("sample-heading").withText(text));

            // dilution
            _test.assertElementPresent(Locator.xpath("//table").withClass("labkey-data-region").append("//td").withClass("labkey-data-region-header-container").withText(text));
        }
    }
}
