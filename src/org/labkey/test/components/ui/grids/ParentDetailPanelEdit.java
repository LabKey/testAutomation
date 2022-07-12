package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/*
    implements internal/components/ParentEntityEditPanel, in its edit mode.

 */
public class ParentDetailPanelEdit extends WebDriverComponent<ParentDetailPanelEdit.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected ParentDetailPanelEdit(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    // title and summary info
    public String title()
    {
        return elementCache().detailEditHeading.getText();
    }

    public String parentsFor()
    {
        return elementCache().parentsForElement.getText();
    }

    // add parent
    public ParentDetailPanelEdit clickAddParent()
    {
        elementCache().addParentButton().click();
        return this;
    }

    public ParentDetailPanelEdit clickRemoveParent(String buttonText)
    {
        var btn = elementCache().removeParentButton(buttonText);
        btn.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(btn));
        return this;
    }

    public ParentDetailPanelEdit setSelectValue(String selectLabel, String selectValue)
    {
        ReactSelect reactSelect =  ReactSelect.finder(_driver).followingLabelWithSpan(selectLabel).find();
        reactSelect.select(selectValue);
        return this;
    }

    public ParentDetailPanelEdit clearSelectionValue(String selectLabel)
    {
        ReactSelect reactSelect =  ReactSelect.finder(_driver).followingLabelWithSpan(selectLabel).find();
        reactSelect.clearSelection();
        return this;
    }

    // save and cancel
    public ParentDetailPanel clickCancel()
    {
        String title = title().replace("Editing ", "");
        elementCache().cancelBtn.click();
        return new ParentDetailPanel.ParentDetailPanelFinder(getDriver()).withTitle(title).waitFor();
    }

    public ParentDetailPanel clickSave()
    {
        String title = title().replace("Editing ", "");
        elementCache().saveBtn.click();
        return new ParentDetailPanel.ParentDetailPanelFinder(getDriver()).withTitle(title).waitFor();
    }

    public boolean isSaveEnabled()
    {
        return elementCache().saveBtn.isEnabled();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        WebElement detailEditHeading = Locator.tagWithClass("div", "detail__edit--heading").findWhenNeeded(this);
        public WebElement panelBody = Locator.tagWithClass("div", "panel-body").findWhenNeeded(this);
        WebElement parentsForElement = Locator.tagWithClass("div", "bottom-spacing").child("b")
                .findWhenNeeded(panelBody);

        WebElement addParentButton()
        {
            return Locator.tagWithClass("span", "container--action-button")
                    .withChild(Locator.tagWithClass("i", "container--addition-icon")).findElement(this);
        }
        WebElement removeParentButton(String text)
        {
            return Locator.tagWithClass("span", "container--action-button")
                    .withChild(Locator.tagWithClass("i", "container--removal-icon")).withText(text)
                    .findElement(this);
        }

        WebElement buttonPanel = Locator.tagWithClass("div", "full-width").findElement(this);
        WebElement cancelBtn = Locator.button("Cancel").findWhenNeeded(buttonPanel);
        WebElement saveBtn = Locator.button("Save").findWhenNeeded(buttonPanel);
    }

    public static class ParentDetailPanelEditFinder extends WebDriverComponentFinder<ParentDetailPanelEdit, ParentDetailPanelEditFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "panel-info")
                .withChild(Locator.tagWithClass("div", "panel-heading")
                        .withChild(Locator.tagWithClass("div", "detail__edit--heading"))).parent()
                .withChild(Locator.tagWithClass("div", "full-width"));
        private String _title = null;

        public ParentDetailPanelEditFinder(WebDriver driver)
        {
            super(driver);
        }

        public ParentDetailPanelEditFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected ParentDetailPanelEdit construct(WebElement el, WebDriver driver)
        {
            return new ParentDetailPanelEdit(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withDescendant(Locator.tagWithClass("div", "detail__edit--heading")
                        .withText(_title));
            else
                return _baseLocator;
        }
    }
}
