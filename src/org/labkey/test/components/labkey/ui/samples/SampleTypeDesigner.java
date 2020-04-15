package org.labkey.test.components.labkey.ui.samples;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.components.domain.DomainDesigner;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.glassLibrary.components.ReactSelect;
import org.labkey.test.components.html.Input;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.labkey.test.WebDriverWrapper.sleep;

/**
 * Automates the LabKey ui component defined in: packages/components/src/components/domainproperties/samples/SampleTypeDesigner.tsx
 * This is a full-page component and should be wrapped by a context-specific page class
 */
public abstract class SampleTypeDesigner<T extends SampleTypeDesigner<T>> extends DomainDesigner<SampleTypeDesigner<T>.ElementCache>
{
    public static final String CURRENT_SAMPLE_TYPE = "(Current Sample Type)";

    public SampleTypeDesigner(WebDriver driver)
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

    public T addFields(List<FieldDefinition> fields)
    {
        DomainFormPanel domainEditor = getFieldsPanel();
        for (FieldDefinition field : fields)
        {
            domainEditor.addField(field);
        }
        return getThis();
    }

    public T setName(String name)
    {
        expandPropertiesPanel();
        elementCache().nameInput.set(name);
        return getThis();
    }

    public String getName()
    {
        expandPropertiesPanel();
        return elementCache().nameInput.get();
    }

    public T setNameExpression(String nameExpression)
    {
        expandPropertiesPanel();
        elementCache().nameExpressionInput.set(nameExpression);
        return getThis();
    }

    public String getNameExpression()
    {
        expandPropertiesPanel();
        return elementCache().nameExpressionInput.get();
    }

    public T setDescription(String description)
    {
        expandPropertiesPanel();
        elementCache().descriptionInput.set(description);
        return getThis();
    }

    public String getDescription()
    {
        expandPropertiesPanel();
        return elementCache().descriptionInput.get();
    }

    public T addParentAlias(String alias)
    {
        return addParentAlias(alias, null);
    }

    public T addParentAlias(String alias, @Nullable String optionDisplayText)
    {
        expandPropertiesPanel();
        int initialCount = elementCache().parentAliases().size();
        elementCache().addAliasButton.click();
        if (optionDisplayText == null)
        {
            optionDisplayText = CURRENT_SAMPLE_TYPE;
        }
        setParentAlias(initialCount, alias, optionDisplayText);
        return getThis();
    }

    private int getParentAliasIndex(String parentAlias)
    {
        List<Input> inputs = elementCache().parentAliases();
        for (int i = 0; i < inputs.size(); i++)
        {
            if (inputs.get(i).get().equals(parentAlias))
            {
                return i;
            }
        }
        throw new NotFoundException("No such parent alias: " + parentAlias);
    }

    public T removeParentAlias(String parentAlias)
    {
        expandPropertiesPanel();
        int aliasIndex = getParentAliasIndex(parentAlias);
        return removeParentAlias(aliasIndex);
    }

    public T removeParentAlias(int index)
    {
        expandPropertiesPanel();
        elementCache().removeParentAliasIcon(index).click();
        return getThis();
    }

    public T setParentAlias(int index, @Nullable String alias, @Nullable String optionDisplayText)
    {
        expandPropertiesPanel();
        elementCache().parentAlias(index).setValue(alias);
        if (optionDisplayText != null)
        {
            elementCache().parentAliasSelect(index).select(optionDisplayText);
        }
        return getThis();
    }

    public T setParentAlias(String alias, String optionDisplayText)
    {
        expandPropertiesPanel();
        int index = getParentAliasIndex(alias);
        elementCache().parentAliasSelect(index).select(optionDisplayText);
        return getThis();
    }

    public String getParentAlias(int index)
    {
        expandPropertiesPanel();
        return elementCache().parentAlias(index).get();
    }

    public String getParentAliasSelectText(int index)
    {
        expandPropertiesPanel();
        return elementCache().parentAliasSelect(index).getSelections().get(0);
    }

    protected class ElementCache extends DomainDesigner.ElementCache
    {
        protected final Input nameInput = Input.Input(Locator.id("entity-name"), getDriver()).findWhenNeeded(propertiesPanel);
        protected final Input nameExpressionInput = Input.Input(Locator.id("entity-nameExpression"), getDriver()).waitFor(propertiesPanel);
        protected final Input descriptionInput = Input.Input(Locator.id("entity-description"), getDriver()).findWhenNeeded(propertiesPanel);
        protected final WebElement addAliasButton = Locator.tagWithClass("i","container--addition-icon").findWhenNeeded(propertiesPanel);

        protected List<Input> parentAliases()
        {
            return Input.Input(Locator.name("alias"), getDriver()).findAll(propertiesPanel);
        }

        protected Input parentAlias(int index)
        {
            return parentAliases().get(index);
        }

        protected ReactSelect parentAliasSelect(int index)
        {
            return ReactSelect.finder(getDriver()).locatedBy(Locator.byClass("sampleset-insert--parent-select"))
                    .index(index).find(propertiesPanel);
        }

        protected WebElement removeParentAliasIcon(int index)
        {
            return Locator.tagWithClass("i","container--removal-icon").findElements(propertiesPanel).get(index);
        }
    }
}
