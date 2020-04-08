package org.labkey.test.components.labkey.ui.samples;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.components.domain.BaseDomainDesigner;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.domain.DomainPanel;
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
public class SampleTypeDesigner extends BaseDomainDesigner<SampleTypeDesigner.ElementCache>
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

    public DomainFormPanel getDomainEditor()
    {
        return elementCache().fieldEditorPanel.expand();
    }

    public SampleTypeDesigner removeField(boolean confirmDialogExpected, String fieldName)
    {
        getDomainEditor().removeField(fieldName, confirmDialogExpected);
        sleep(250); // wait for collapse animation
        return this;
    }

    public SampleTypeDesigner addFields(FieldDefinition... fields)
    {
        DomainFormPanel domainEditor = getDomainEditor();
        for (FieldDefinition field : fields)
        {
            domainEditor.addField(field);
        }
        return this;
    }

    public SampleTypeDesigner setName(String name)
    {
        elementCache().propertiesPanel.expand();
        elementCache().nameInput.set(name);
        return this;
    }

    public String getName()
    {
        elementCache().propertiesPanel.expand();
        return elementCache().nameInput.get();
    }

    public SampleTypeDesigner setNameExpression(String nameExpression)
    {
        elementCache().propertiesPanel.expand();
        elementCache().nameExpressionInput.set(nameExpression);
        return this;
    }

    public String getNameExpression()
    {
        elementCache().propertiesPanel.expand();
        return elementCache().nameExpressionInput.get();
    }

    public SampleTypeDesigner setDescription(String description)
    {
        elementCache().propertiesPanel.expand();
        elementCache().descriptionInput.set(description);
        return this;
    }

    public String getDescription()
    {
        elementCache().propertiesPanel.expand();
        return elementCache().descriptionInput.get();
    }

    public SampleTypeDesigner addParentAlias(String alias, @Nullable String optionDisplayText)
    {
        elementCache().propertiesPanel.expand();
        int initialCount = elementCache().parentAliases().size();
        elementCache().addAliasButton.click();
        if (optionDisplayText == null)
        {
            optionDisplayText = CURRENT_SAMPLE_TYPE;
        }
        setParentAlias(initialCount, alias, optionDisplayText);
        return this;
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

    public SampleTypeDesigner removeParentAlias(String parentAlias)
    {
        elementCache().propertiesPanel.expand();
        int aliasIndex = getParentAliasIndex(parentAlias);
        return removeParentAlias(aliasIndex);
    }

    public SampleTypeDesigner removeParentAlias(int index)
    {
        elementCache().propertiesPanel.expand();
        elementCache().removeParentAliasIcon(index).click();
        return this;
    }

    public SampleTypeDesigner setParentAlias(int index, @Nullable String alias, @Nullable String optionDisplayText)
    {
        elementCache().propertiesPanel.expand();
        elementCache().parentAlias(index).setValue(alias);
        if (optionDisplayText != null)
        {
            elementCache().parentAliasSelect(index).select(optionDisplayText);
        }
        return this;
    }

    public SampleTypeDesigner setParentAlias(String alias, String optionDisplayText)
    {
        elementCache().propertiesPanel.expand();
        int index = getParentAliasIndex(alias);
        elementCache().parentAliasSelect(index).select(optionDisplayText);
        return this;
    }

    public String getParentAlias(int index)
    {
        elementCache().propertiesPanel.expand();
        return elementCache().parentAlias(index).get();
    }

    public String getParentAliasSelectText(int index)
    {
        elementCache().propertiesPanel.expand();
        return elementCache().parentAliasSelect(index).getSelections().get(0);
    }

    protected class ElementCache extends BaseDomainDesigner.ElementCache
    {
        protected final DomainPanel propertiesPanel = new DomainPanel.DomainPanelFinder(getDriver()).index(0).timeout(1000).findWhenNeeded(this);
        protected final Input nameInput = Input.Input(Locator.id("entity-name"), getDriver()).findWhenNeeded(this);
        protected final Input nameExpressionInput = Input.Input(Locator.id("entity-nameExpression"), getDriver()).waitFor(this);
        protected final Input descriptionInput = Input.Input(Locator.id("entity-description"), getDriver()).findWhenNeeded(this);
        protected final WebElement addAliasButton = Locator.tagWithClass("i","container--addition-icon").findWhenNeeded(this);
        protected final WebElement propertiesPanelHeader = Locator.id("sample-type-properties-hdr").findWhenNeeded(this);

        protected List<Input> parentAliases()
        {
            return Input.Input(Locator.name("alias"), getDriver()).findAll(this);
        }

        protected Input parentAlias(int index)
        {
            return parentAliases().get(index);
        }

        protected ReactSelect parentAliasSelect(int index)
        {
            return ReactSelect.finder(getDriver()).locatedBy(Locator.byClass("sampleset-insert--parent-select"))
                    .index(index).find(this);
        }

        protected WebElement removeParentAliasIcon(int index)
        {
            return Locator.tagWithClass("i","container--removal-icon").findElements(this).get(index);
        }

        protected final DomainFormPanel fieldEditorPanel = new DomainFormPanel(new DomainPanel.DomainPanelFinder(getDriver()).index(1).timeout(1000).findWhenNeeded(this));
    }
}
