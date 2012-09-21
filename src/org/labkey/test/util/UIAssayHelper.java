package org.labkey.test.util;

import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseSeleniumWebTest;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 9/18/12
 * Time: 1:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class UIAssayHelper extends AbstractAssayHelper
{
    public UIAssayHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    public void importAssay(int assayID, String file, String projectPath) throws CommandException, IOException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void importAssay(String assayName, String file, String projectPath) throws CommandException, IOException
    {
        String[] folders = projectPath.split("/");
        for(String folder : folders)
            _test.clickLinkWithText(folder);
        _test.clickLinkContainingText(assayName);
        _test.clickButton("Import Data");
        _test.clickButton("Next");

        _test.checkRadioButton("dataCollectorName", "File upload");
        _test.setFormElement("__primaryFile__", new File(file));
        _test.clickButton("Save and Finish");


        //To change body of implemented methods use File | Settings | File Templates.
    }
}
