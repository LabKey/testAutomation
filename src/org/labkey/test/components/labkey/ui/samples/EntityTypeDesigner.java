package org.labkey.test.components.labkey.ui.samples;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.html.Input;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.labkey.test.WebDriverWrapper.sleep;

public abstract class EntityTypeDesigner<T extends EntityTypeDesigner<T>> extends WebDriverComponent<EntityTypeDesigner<T>.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    public EntityTypeDesigner(WebDriver driver)
    {
        _driver = driver;
        _el = Locator.id("app").findElement(_driver); // Full page component
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

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected abstract T getThis();

    public DomainFormPanel getDomainEditor()
    {
        return elementCache()._fieldEditorPanel.expand();
    }

    public T removeField(boolean confirmDialogExpected, String fieldName)
    {
        getDomainEditor().removeField(fieldName, confirmDialogExpected);
        sleep(250); // wait for collapse animation
        return getThis();
    }

    public T addField(FieldDefinition field)
    {
        return addFields(List.of(field));
    }

    public T addFields(List<FieldDefinition> fields)
    {
        DomainFormPanel fieldsPanel = getDomainEditor();
        boolean firstField = true;

        for (FieldDefinition field : fields)
        {
            if (firstField && fieldsPanel.isManuallyDefineFieldsPresent())
            {
                fieldsPanel.manuallyDefineFields(field.getName());
                fieldsPanel.setField(field);
            }
            else
            {
                fieldsPanel.addField(field);
            }

            firstField = false;
        }
        return getThis();
    }

    public boolean isCancelButtonEnabled()
    {
        return elementCache().cancelButton.isEnabled();
    }

    public void clickCancel()
    {
        elementCache().cancelButton.click();
    }

    public boolean isSaveButtonEnabled()
    {
        return elementCache().saveButton.isEnabled();
    }

    public void clickSave()
    {
        elementCache().saveButton.click();
    }

    public List<WebElement> clickSaveExpectingError()
    {
        elementCache().saveButton.click();
        return BootstrapLocators.errorBanner.waitForElements(getWrapper().shortWait());
    }

    public T setName(String name)
    {
        elementCache().nameInput.set(name);
        return getThis();
    }

    public String getName()
    {
        return elementCache().nameInput.get();
    }

    public T setNameExpression(String nameExpression)
    {
        elementCache().nameExpressionInput.set(nameExpression);
        return getThis();
    }

    public String getNameExpression()
    {
        return elementCache().nameExpressionInput.get();
    }

    public T setDescription(String description)
    {
        elementCache().descriptionInput.set(description);
        return getThis();
    }

    public String getDescription()
    {
        return elementCache().descriptionInput.get();
    }

    public DomainFormPanel expandPropertiesPanel()
    {
        return elementCache()._propertiesPanel.expand();
    }

    protected class ElementCache extends Component<ElementCache>.ElementCache
    {
        protected final Input nameInput = Input.Input(Locator.id("entity-name"), getDriver()).findWhenNeeded(this);
        protected final Input nameExpressionInput = Input.Input(Locator.id("entity-nameExpression"), getDriver()).waitFor(this);
        protected final Input descriptionInput = Input.Input(Locator.id("entity-description"), getDriver()).findWhenNeeded(this);
        protected final DomainFormPanel _propertiesPanel = new DomainFormPanel.DomainFormPanelFinder(getDriver()).index(0).timeout(1000).findWhenNeeded(this);
        protected final DomainFormPanel _fieldEditorPanel = new DomainFormPanel.DomainFormPanelFinder(getDriver()).index(1).timeout(1000).findWhenNeeded(this);

        protected final WebElement cancelButton = Locator.button("Cancel").findWhenNeeded(this);

        // the SM app uses alternate text for the sample type designer save buttons
        protected final WebElement saveButton = Locator.XPathLocator.union(Locator.button("Save"), Locator.buttonContainingText("Finish")).findWhenNeeded(this);
    }
}
