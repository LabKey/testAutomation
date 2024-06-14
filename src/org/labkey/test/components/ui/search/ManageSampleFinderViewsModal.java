package org.labkey.test.components.ui.search;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Override
    protected void waitForReady()
    {
        // This will wait for something to show up in the dialog body and if it is a spinner it will wait for it to go away.
        super.waitForReady();

        // Now, specifically check for:
        // An input box (to save a new image).
        // Or some grey text (get this when you delete all views and the dialog is still up).
        // Or a list of existing views.
        WebDriverWrapper.waitFor(()->
            Locator.tag("input").refindWhenNeeded(this).isDisplayed() ||
                    Locator.tagWithClass("div", "grey-text").refindWhenNeeded(this). isDisplayed() ||
                    Locator.tagWithClass("div", "row").refindWhenNeeded(this). isDisplayed(),
            "Save view dialog did not display in time.",
            2_500);
    }

    public void editViewName(String viewName)
    {
        getViewEditIcon(viewName).click();
    }

    public void deleteView(String viewName)
    {
        getViewDeleteIcon(viewName).click();
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

    private static final String viewNameLocatorXpath = "//div[@class='modal-body']//div[contains(@class,'row')]//div[string-length(text()) > 0]";

    public List<String> getViews()
    {
        Locator viewNameLocator = Locator.xpath(viewNameLocatorXpath);
        // Wait until some view shows up.
        WebDriverWrapper.waitFor(()->getWrapper().isElementPresent(viewNameLocator),
                "No views are present.", 2_500);

        List<String> views = new ArrayList<>();

        // Get all the view names.
        List<WebElement> elements = viewNameLocator.findElements(this);

        getWrapper().log(String.format("Found %d views in the dialog.", elements.size()));

        for(WebElement element : elements)
        {
            views.add(element.getText());
        }

        return views;
    }

    public WebElement getView(String viewName)
    {
        // Wait until some view shows up.
        Locator viewNameLocator = Locator.xpath(viewNameLocatorXpath);
        WebDriverWrapper.waitFor(()->getWrapper().isElementPresent(viewNameLocator),
                "No views are present.", 2_500);

        // Get the row for the given view.
        return Locator.xpath(String.format("//div[text()='%s']/parent::div[contains(@class,'row')]", viewName))
                .findElement(this);
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
        WebDriverWrapper.waitFor(input::isDisplayed, "Name input field not visible.", 2_500);

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
        if(Boolean.TRUE.equals(WebDriverWrapper.waitFor(()->elementCache().errorMsg.isDisplayed(), 1_000)))
            return elementCache().errorMsg.getText();
        else
            return "";
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