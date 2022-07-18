package org.labkey.test.components.ui.search;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ManageSampleFinderViewsModal extends ModalDialog
{
    public ManageSampleFinderViewsModal(WebDriver driver)
    {
        this("Manage Saved Searches", driver);
    }

    protected ManageSampleFinderViewsModal(String title, WebDriver driver)
    {
        super(new ModalDialog.ModalDialogFinder(driver).withTitle(title));
    }

    public void editViewName(String viewName)
    {
        WebElement editIcon = getViewEditIcon(viewName);
        if (editIcon != null)
        {
            editIcon.click();
        }
    }

    public void deleteView(String viewName)
    {
        WebElement deleteIcon = getViewDeleteIcon(viewName);
        if (deleteIcon != null)
        {
            deleteIcon.click();
        }
    }

    public void deleteAllViews()
    {
        List<String> views = getViews();
        Collections.reverse(views);
        for (String view : views)
        {
            deleteView(view);
        }
    }

    public List<String> getViews()
    {
        return Locator.tagWithClass("div", "small-margin-bottom")
                .findElements(getComponentElement())
                .stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public WebElement getView(String viewName)
    {
        List<WebElement> webElements = Locator.tagWithClass("div", "small-margin-bottom")
                .findElements(getComponentElement());

        for (WebElement element : webElements)
        {
            if (element.getText().equals(viewName))
                return element;
        }

        return null;
    }

    public WebElement getViewIcon(String reportName, String iconCls)
    {
        WebElement viewRow = getView(reportName);
        if (viewRow == null)
            return null;

        return Locator
                .tagWithClass("i", iconCls)
                .findElementOrNull(viewRow);
    }

    public WebElement getViewEditIcon(String reportName)
    {
        return getViewIcon(reportName, "fa-pencil");
    }

    public WebElement getViewDeleteIcon(String reportName)
    {
        return getViewIcon(reportName, "fa-trash-o");
    }

    public boolean isViewLocked(String reportName)
    {
        return getViewLockIcon(reportName) != null && getViewEditIcon(reportName) == null;
    }

    public WebElement getViewLockIcon(String reportName)
    {
        return getViewIcon(reportName, "fa-lock");
    }

    public String getInputValue()
    {
        return Locator.tag("input").findElement(getComponentElement()).getAttribute("value");
    }

    public ManageSampleFinderViewsModal setName(String name)
    {
        WebElement input = elementCache().nameInput;
        getWrapper().actionClear(input);
        input.sendKeys(name);
        input.sendKeys(Keys.TAB);
        return this;
    }

    public void clickDone()
    {
        dismiss("Done editing");
    }

    public String getErrorMsg()
    {
        return elementCache().errorMsg.getText();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        WebElement errorMsg = Locator.tagWithClassContaining("div", "alert-danger").refindWhenNeeded(getComponentElement());

        WebElement nameInput = Locator.tag("input").refindWhenNeeded(getComponentElement());

    }
}