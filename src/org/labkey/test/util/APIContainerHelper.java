package org.labkey.test.util;

import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.CreateContainerCommand;
import org.labkey.test.BaseSeleniumWebTest;

/**
 * User: jeckels
 * Date: Jul 20, 2012
 */
public class APIContainerHelper extends AbstractContainerHelper
{
    public APIContainerHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    @Override
    public void doCreateProject(String projectName, String folderType)
    {
        _test.log("Creating project with name via API " + projectName);
        Connection connection = new Connection(_test.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        CreateContainerCommand command = new CreateContainerCommand(projectName);
        command.setFolderType(folderType);
        try
        {
            command.execute(connection, "/");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        _test.goToHome();
        _test.clickLinkWithText(projectName);
    }
}
