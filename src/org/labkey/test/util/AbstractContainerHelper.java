package org.labkey.test.util;

import org.labkey.test.BaseSeleniumWebTest;

import java.util.ArrayList;
import java.util.List;

/**
 * User: jeckels
 * Date: Jul 20, 2012
 */
public abstract class AbstractContainerHelper extends AbstractHelper
{
    private List<String> _createdProjects = new ArrayList<String>();

    public AbstractContainerHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    /** @param folderType the name of the type of container to create.
     * May be null, in which case you get the server's default folder type */
    public final void createProject(String projectName, String folderType)
    {
        doCreateProject(projectName, folderType);
        _createdProjects.add(projectName);
    }

    protected abstract void doCreateProject(String projectName, String folderType);

    public List<String> getCreatedProjects()
    {
        return _createdProjects;
    }
}
