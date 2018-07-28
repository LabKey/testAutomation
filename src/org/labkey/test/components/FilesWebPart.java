package org.labkey.test.components;

import org.labkey.test.util.FileBrowserHelper;
import org.openqa.selenium.WebDriver;

public class FilesWebPart extends BodyWebPart<FilesWebPart.ElementCache>
{

    public FilesWebPart(WebDriver driver)
    {
        super(driver, "Files");
    }

    static public FilesWebPart getWebPart(WebDriver driver)
    {
        return new FilesWebPart(driver);
    }

    public FileBrowserHelper fileBrowser()
    {
        return new FileBrowserHelper(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BodyWebPart.ElementCache
    {
    }

}
