package org.labkey.test.components.ui.domainproperties;

import org.labkey.test.Locator;
import org.labkey.test.components.domain.DomainDesigner;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

import static org.labkey.test.WebDriverWrapper.sleep;

public abstract class EntityTypeDesigner<T extends EntityTypeDesigner<T>> extends DomainDesigner<EntityTypeDesigner<T>.ElementCache>
{
    public EntityTypeDesigner(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected abstract T getThis();

    public T removeField(boolean confirmDialogExpected, String fieldName)
    {
        getFieldsPanel().removeField(fieldName, confirmDialogExpected);
        sleep(250); // wait for collapse animation
        return getThis();
    }

    public T addField(FieldDefinition field)
    {
        return addFields(List.of(field));
    }

    public T addFields(List<FieldDefinition> fields)
    {
        DomainFormPanel fieldsPanel = getFieldsPanel();
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

    public String getName()
    {
        expandPropertiesPanel();
        return elementCache().nameInput.get();
    }

    public T setName(String name)
    {
        expandPropertiesPanel();
        elementCache().nameInput.set(name);
        return getThis();
    }

    public String getNameExpression()
    {
        expandPropertiesPanel();
        return elementCache().nameExpressionInput.get();
    }

    public T setNameExpression(String nameExpression)
    {
        expandPropertiesPanel();
        elementCache().nameExpressionInput.set(nameExpression);
        return getThis();
    }

    public String getAutoLinkDataToStudy()
    {
        expandPropertiesPanel();
        return elementCache().autoLinkDataToStudy.toString();
    }

    public T setAutoLinkDataToStudy(String value)
    {
        expandPropertiesPanel();
        elementCache().autoLinkDataToStudy.selectByVisibleText(value);
        return getThis();
    }

    public String getLinkedDatasetCategory()
    {
        expandPropertiesPanel();
        return elementCache().linkedDatasetCategory.get();
    }

    public T setLinkedDatasetCategory(String value)
    {
        expandPropertiesPanel();
        elementCache().linkedDatasetCategory.set(value);
        return getThis();
    }

    public String getDescription()
    {
        expandPropertiesPanel();
        return elementCache().descriptionInput.get();
    }

    public T setDescription(String description)
    {
        expandPropertiesPanel();
        elementCache().descriptionInput.set(description);
        return getThis();
    }

    protected class ElementCache extends DomainDesigner<?>.ElementCache
    {
        protected final Input nameInput = Input.Input(Locator.id("entity-name"), getDriver()).findWhenNeeded(this);
        protected final Input nameExpressionInput = Input.Input(Locator.id("entity-nameExpression"), getDriver()).waitFor(this);
        protected final Input descriptionInput = Input.Input(Locator.id("entity-description"), getDriver()).findWhenNeeded(this);

        protected final Select autoLinkDataToStudy = SelectWrapper.Select(Locator.id("entity-autoLinkTargetContainerId"))
                .findWhenNeeded(this);
        protected final Input linkedDatasetCategory = Input.Input(Locator.id("entity-autoLinkCategory"), getDriver()).findWhenNeeded(this);
    }
}
