package org.labkey.test.tests.nab;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.AbstractAssayHelper;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * GitHub Issue #1569: every domain of an assay design shares the assay name as its typeURI objectId, so before the fix
 * they all resolved to one default-value object and saving one domain's defaults destroyed another's.
 */
@Category({Daily.class, Assays.class})
public class NabAssayDefaultValuesTest extends BaseWebDriverTest
{
    private static final String NAB_ASSAY_TYPE = "TZM-bl Neutralization (NAb)";
    private static final String ASSAY_NAME = "DefaultValuesNab";
    private static final String CUTOFF1 = "Cutoff1";
    private static final String CUTOFF1_VALUE = "77";
    private static final String INITIAL_DILUTION = "InitialDilution";
    private static final String INITIAL_DILUTION_VALUE = "20.0";

    @BeforeClass
    public static void setupProject()
    {
        NabAssayDefaultValuesTest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), "Assay");
        goToProjectHome();

        ReactAssayDesignerPage designer = _assayHelper.createAssayDesign(NAB_ASSAY_TYPE, ASSAY_NAME);

        // need to use FIXED_EDITABLE for folder level defaults that test the regression case
        designer.goToRunFields().getField(CUTOFF1).clickAdvancedSettings()
                .setDefaultValueType(FieldDefinition.DefaultType.FIXED_EDITABLE).apply();
        designer.expandFieldsPanel("Sample").getField(INITIAL_DILUTION).clickAdvancedSettings()
                .setDefaultValueType(FieldDefinition.DefaultType.FIXED_EDITABLE).apply();

        designer.clickFinish();
    }

    @Test
    public void testDefaultsOnOneDomainIsNotClearedBySavingAnother()
    {
        goToProjectHome();

        setDefault(AbstractAssayHelper.AssayDefaultAreas.RUN_FIELDS, CUTOFF1, CUTOFF1_VALUE);
        setDefault(AbstractAssayHelper.AssayDefaultAreas.SAMPLE_FIELDS, INITIAL_DILUTION, INITIAL_DILUTION_VALUE);

        assertEquals("Run Fields default was cleared by saving Sample Fields defaults",
                CUTOFF1_VALUE, readDefault(AbstractAssayHelper.AssayDefaultAreas.RUN_FIELDS, CUTOFF1));

        // Re-save Run Fields
        setDefault(AbstractAssayHelper.AssayDefaultAreas.RUN_FIELDS, CUTOFF1, CUTOFF1_VALUE);

        assertEquals("Sample Fields default was cleared by saving Run Fields defaults",
                INITIAL_DILUTION_VALUE, readDefault(AbstractAssayHelper.AssayDefaultAreas.SAMPLE_FIELDS, INITIAL_DILUTION));
    }

    private void setDefault(AbstractAssayHelper.AssayDefaultAreas area, String fieldName, String value)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        _assayHelper.setDefaultValues(ASSAY_NAME, area);
        setFormElement(Locator.name(fieldName), value);
        clickButton("Save Defaults");
    }

    private String readDefault(AbstractAssayHelper.AssayDefaultAreas area, String fieldName)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        _assayHelper.setDefaultValues(ASSAY_NAME, area);
        return getFormElement(Locator.name(fieldName));
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    protected String getProjectName()
    {
        return "NabAssayDefaultValuesTest";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return List.of("nab");
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
