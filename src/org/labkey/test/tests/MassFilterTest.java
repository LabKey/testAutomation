package org.labkey.test.tests;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 3/13/13
 * Time: 12:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class MassFilterTest extends  FilterTest
{
    @Override
    protected String getProjectName()
    {
        return "Mass Filter Test";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doTestSteps()
    {
        doSetUp();
        doVerify();
    }

    private void doVerify()
    {
        windowMaximize();
        Locator advancedFilteringLoc = Locator.name("value_1");
        Locator.XPathLocator factedFilterLoc = Locator.linkContainingText("[All]");

        sleep(4000);

        startFilter("Small");
        assertElementNotVisible(advancedFilteringLoc);
        assertElementVisible(factedFilterLoc);
        click(Locator.button("CANCEL"));

        refresh(); //stupid selenium issue, it doesn't register the second filter dialogue as visible


        startFilter("ID");
        assertElementVisible(advancedFilteringLoc);
        assertElementNotVisible(factedFilterLoc);
        click(Locator.button("CANCEL"));



    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void doSetUp()
    {
        _containerHelper.createProject(getProjectName(), null);
        new PortalHelper(this).addWebPart("Lists");
        _listHelper.importListArchive(getProjectName(), new File(getLabKeyRoot(), "/sampledata/MassFilter/massFilter.lists.zip"));
        click(Locator.linkContainingText("101 agents"));

    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
