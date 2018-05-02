package org.labkey.test.util;

import org.labkey.test.WebDriverWrapper;

public interface ConfiguresSite
{
    void setWrapper(WebDriverWrapper driverWrapper);
    void configureSite();
    void configureProject(String project);
    void configureFolder(String containerPath);
}
