package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.ui.search.FilterExpressionPanel;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.labkey.test.components.html.Input.Input;


public class HitSelectionDialog extends ModalDialog
{

    public HitSelectionDialog(WebDriver driver)
    {
        super(new ModalDialogFinder(driver));
    }

    public List<String> getAvailableFields()
    {
        return getWrapper().getTexts(elementCache().findFieldOptions());
    }

    public FilterExpressionPanel selectField(String fieldName)
    {
        var fieldItem = elementCache().findFieldOption(fieldName);
        fieldItem.click();
        return elementCache().filterExpressionPanel();
    }

    public void cancel()
    {
        dismiss("Cancel");
    }

    public void clickApply()
    {
        if (!elementCache().submitButton.isEnabled())
        {
            throw new IllegalStateException("Apply button is not enabled.");
        }
        elementCache().submitButton.click();
        waitForClose();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends ModalDialog.ElementCache
    {
        public final Locator listItemLoc = Locator.byClass("list-group-item");

        // fields panel
        WebElement fieldsPanel = Locator.tagWithClass("div", "field-modal__col")
                .withChild(Locator.tagWithClass("div", "field-modal__col-title").withText("Fields"))
                .child(Locator.tagWithClass("div", "field-modal__col-content")).findWhenNeeded(this).withTimeout(5000);
        protected WebElement findFieldOption(String queryName)
        {
            return listItemLoc.withText(queryName).findElement(fieldsPanel);
        }
        protected List<WebElement> findFieldOptions()
        {
            return listItemLoc.findElements(fieldsPanel);
        }

        // filter panel
        WebElement filterCriteriaPanel = Locator.tagWithClass("div", "field-modal__col")
                .withChild(Locator.tagWithClass("div", "field-modal__col-title").withText("Filter Criteria"))
                .child(Locator.tagWithClass("div", "field-modal__col-content")).findWhenNeeded(this).withTimeout(5000);
        protected final FilterExpressionPanel filterExpressionPanel()
        {
            return new FilterExpressionPanel.FilterExpressionPanelFinder(getDriver()).findWhenNeeded(filterCriteriaPanel);
        }

        protected final WebElement submitButton = Locator.css(".modal-footer .btn-success").findWhenNeeded(this);
    }
}
