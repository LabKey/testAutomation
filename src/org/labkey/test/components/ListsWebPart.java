package org.labkey.test.components;

import org.apache.commons.lang3.NotImplementedException;
import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ListsWebPart extends BodyWebPart
{
    private Elements _elements;

    public ListsWebPart(WebDriver driver)
    {
        super(driver, "Lists");
    }

    public LabKeyPage createNewList()
    {
        clickMenuItem("Create New List");
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage manageLists()
    {
        _test.clickAndWait(elements().manageLink);
        return new LabKeyPage(getDriver());
    }

    public List<String> getLists()
    {
        return _test.getTexts(elements().listLinks());
    }

    public LabKeyPage viewData(String list)
    {
        throw new NotImplementedException("Update ListsWebPart.java to add this functionality");
    }

    public LabKeyPage viewDesign(String list)
    {
        throw new NotImplementedException("Update ListsWebPart.java to add this functionality");
    }

    public LabKeyPage viewHistory(String list)
    {
        throw new NotImplementedException("Update ListsWebPart.java to add this functionality");
    }

    public LabKeyPage deleteList(String list)
    {
        throw new NotImplementedException("Update ListsWebPart.java to add this functionality");
    }

    protected Elements elements()
    {
        if (null == _elements)
            _elements = new Elements();
        return _elements;
    }

    protected class Elements extends WebPart.Elements
    {
        private List<WebElement> listLinks;

        WebElement manageLink = new LazyWebElement(Locator.linkWithText("Manage Lists"), this);
        List<WebElement> listLinks()
        {
            if (listLinks == null)
                listLinks = Locator.tag("a").withAttributeContaining("href", "grid.view").findElements(this);
            return listLinks;
        }
    }
}
