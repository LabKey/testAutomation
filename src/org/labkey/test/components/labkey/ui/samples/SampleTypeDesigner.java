package org.labkey.test.components.labkey.ui.samples;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.components.glassLibrary.components.ReactSelect;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Automates the LabKey ui component defined in: packages/components/src/components/domainproperties/samples/SampleTypeDesigner.tsx
 * This is a full-page component and should be wrapped by a context-specific page class
 */
public class SampleTypeDesigner extends EntityTypeDesigner
{
    public final static String CURRENT_SAMPLE_TYPE = "(Current Sample Type)";

    public SampleTypeDesigner(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected SampleTypeDesigner getThis()
    {
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return  (ElementCache) super.elementCache();
    }

    public SampleTypeDesigner addParentAlias(String alias, @Nullable String optionDisplayText)
    {
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

    public SampleTypeDesigner removeParentAlias(String parentAlias)
    {
        int aliasIndex = getParentAliasIndex(parentAlias);
        return removeParentAlias(aliasIndex);
    }

    public SampleTypeDesigner removeParentAlias(int index)
    {
        elementCache().removeParentAliasIcon(index).click();
        return getThis();
    }

    public SampleTypeDesigner setParentAlias(int index, @Nullable String alias, @Nullable String optionDisplayText)
    {
        elementCache().parentAlias(index).setValue(alias);
        if (optionDisplayText != null)
        {
            elementCache().parentAliasSelect(index).select(optionDisplayText);
        }
        return getThis();
    }

    public SampleTypeDesigner setParentAlias(String alias, String optionDisplayText)
    {
        int index = getParentAliasIndex(alias);
        elementCache().parentAliasSelect(index).select(optionDisplayText);
        return getThis();
    }

    public String getParentAlias(int index)
    {
        return elementCache().parentAlias(index).get();
    }

    public String getParentAliasSelectText(int index)
    {
        return elementCache().parentAliasSelect(index).getSelections().get(0);
    }

    public void expandPropertiesPanel()
    {
        elementCache().propertiesPanelHeader.click();
    }

    protected class ElementCache extends EntityTypeDesigner.ElementCache
    {
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
    }
}
