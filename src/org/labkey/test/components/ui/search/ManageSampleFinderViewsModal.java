package org.labkey.test.components.ui.search;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
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

    @Override
    protected void waitForReady()
    {
        super.waitForReady();

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

    public List<String> getViews()
    {
        return Locator.tagWithClass("div", "row")
                .findElements(this)
                .stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public WebElement getView(String viewName)
    {
        return Locator.tagWithClass("div", "row")
                .withText(viewName)
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
        return Locator.tag("input").findElement(this).getAttribute("value");
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
        if(WebDriverWrapper.waitFor(()->elementCache().errorMsg.isDisplayed(), 1_000))
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
        WebElement errorMsg = Locator.tagWithClassContaining("div", "alert-danger").refindWhenNeeded(this);

        WebElement nameInput = Locator.tag("input").refindWhenNeeded(this);

    }
}