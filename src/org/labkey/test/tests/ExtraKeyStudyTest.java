package org.labkey.test.tests;

import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 4/29/12
 * Time: 1:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtraKeyStudyTest extends StudyBaseTest
{

    static String studyDataPath = "/sampledata/study/ExtraKeyStudy/study";

    @Override
    protected void doCreateSteps()
    {
        importStudy(getLabKeyRoot() + studyDataPath);
    }

    @Override
    protected void doVerifySteps()
    {
        log("TODO");
        clickLinkContainingText(getFolderName());
        clickLinkContainingText("datasets");
        waitForTextWithRefresh("PVDouble_Two", defaultWaitForPage);
        verifyDemoDataset();
        goBack();
        verifyDemoVisitDataset();
        goBack();
        verifyDemoVisitKeyDataset();


    }

    private void verifyDemoVisitKeyDataset()
    {
        clickLinkContainingText("PVInt_One");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        verifyElements(true, true, true);
    }

    private void verifyDemoVisitDataset()
    {
        clickLinkWithText("PV_Two");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        verifyElements(true, true, false);


    }

    private void verifyDemoDataset()
    {

        clickLinkWithText("P_One");
        CustomizeViewsHelper.openCustomizeViewPanel(this);

        verifyElements(true, false, false);

    }

    private void verifyElements(boolean demoVisibile, boolean visitVisible, boolean extraKeyVisibile)
    {
        assertTextNotPresent("Panda Visit");
        assertTextPresent("DataSets");
        assertElementNotPresent(Locator.xpath("//div[a/span[text()='Panda Id']]/img[contains(@class, 'plus')]"));
        assertEquals("Visibility of id only data sets what was expected", demoVisibile, CustomizeViewsHelper.isColumnPresent(this, "DataSets/P_Two"));
        assertEquals("Visibility of id + visit data sets not what was expected", visitVisible, CustomizeViewsHelper.isColumnPresent(this, "DataSets/PV_Two"));
        assertEquals("Visibility of id, visit, extra key data sets not what was expected", extraKeyVisibile, CustomizeViewsHelper.isColumnPresent(this, "DataSets/PVInt_Two"));
        assertFalse("Visibility of discordant key data sets not what was expected", CustomizeViewsHelper.isColumnPresent(this, "DataSets/PVSInt_Two"));

    }
}
