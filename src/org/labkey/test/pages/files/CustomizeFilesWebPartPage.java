package org.labkey.test.pages.files;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Created by susanh on 9/20/17.
 */
public class CustomizeFilesWebPartPage extends LabKeyPage<CustomizeFilesWebPartPage.ElementCache>
{
    public CustomizeFilesWebPartPage(WebDriver driver)
    {
        super(driver);
    }

    public String getTitle()
    {
        return getFormElement(elementCache().title);
    }

    public CustomizeFilesWebPartPage setTitle(String title)
    {
        setFormElement(elementCache().title, title);
        return this;
    }

    public String getFileRoot()
    {
        Locator.XPathLocator selectedNodeLoc = Locator.xpath("//tr").withClass("x4-grid-row-selected").append("/td/div/span");
        waitForElement(selectedNodeLoc);
        return getText(selectedNodeLoc);
    }

    public CustomizeFilesWebPartPage setFileRoot(String menuOption)
    {
        selectOptionByText(elementCache().fileRootSelect, menuOption);
        return this;
    }

    protected ElementCache elementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected WebElement title = new LazyWebElement(Locator.tagWithName("input", "title"), this);
        protected WebElement fileRootSelect = new LazyWebElement(Locator.tagWithName("select", "fileSet"), this);

    }
}
