package org.labkey.test.tests;

/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 12/12/12
 * Time: 12:17 PM
 */
abstract public class AbstractLabModuleAssayTest extends LabModulesTest
{
    @Override
    public boolean enableLinkCheck()
    {
        if ( super.enableLinkCheck() )
            log("LabModulesTest uses essentially the same UI and will check links, so link checking is skipped");
        return false;
    }
}
