package org.labkey.test.tests;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 4/5/12
 * Time: 3:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class StudyReloadTest extends StudyBaseTest
{
    @Override
    protected void doCreateSteps()
    {
        initializeFolder();
        importStudyFromZip(getSampledataPath() + "\\" + "studyreload\\original.zip" );
        reloadStudyFromZip("C:\\Users\\elvan\\Downloads\\add_column.zip");
//        reloadStudyFromZip(getSampledataPath() + "\\" + "studyreload\\edited.zip");
    }

    @Override
    protected void doVerifySteps()
    {
        clickLinkWithText(getFolderName());
        clickLinkWithText("1 datasets");
        clickLinkWithText("update_test");
        assertTextPresent("id006", "additional_column");
        //text that was present in original but removed in the update
        assertTextNotPresent("id005", "original_column_numeric");
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
