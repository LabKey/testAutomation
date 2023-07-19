package org.labkey.test.components.ui.domainproperties.samples;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.ui.domainproperties.EntityTypeDesigner;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Automates the LabKey ui component defined in: packages/components/src/components/domainproperties/samples/SampleTypeDesigner.tsx
 * This is a full-page component and should be wrapped by a context-specific page class
 */
public abstract class SampleTypeDesigner<T extends SampleTypeDesigner<T>> extends EntityTypeDesigner<T>
{
    public static final String CURRENT_SAMPLE_TYPE = "(Current Sample Type)";

    public SampleTypeDesigner(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected abstract T getThis();

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

    public T addParentAlias(String alias)
    {
        return addParentAlias(alias, null);
    }

    public T addParentAlias(String alias, @Nullable String optionDisplayText)
    {
        expandPropertiesPanel();

        WebDriverWrapper.waitFor(elementCache().addAliasButton::isDisplayed,
                "'Add Parent Alias' button is not visible.", 2_500);

        elementCache().addAliasButton.click();
        int initialCount = findEmptyAlias();
        if (optionDisplayText == null)
        {
            optionDisplayText = CURRENT_SAMPLE_TYPE;
        }
        setParentAlias(initialCount, alias, optionDisplayText);
        return getThis();
    }

    public int getParentAliasIndex(String parentAlias)
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

    public List<String> getParentAliasOptions(int index)
    {
        expandPropertiesPanel();
        return elementCache().parentAliasSelect(index).getOptions();
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

    protected T setParentAlias(int index, @Nullable String alias, @Nullable String optionDisplayText)
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

    protected int findEmptyAlias()
    {
        List<Input> aliases = elementCache().parentAliases();
        int index = -1;
        for(int i = 0; i < aliases.size(); i++)
        {
            if(aliases.get(i).getValue().isEmpty())
            {
                index = i;
                break;
            }
        }

        return index;

    }

    public boolean hasUniqueIdAlert()
    {
        return elementCache().uniqueIdAlert.isDisplayed();
    }

    public T clickUniqueIdAlertAddButton()
    {
        if (hasUniqueIdAlert())
            elementCache().uniqueIdAlertAddButton.click();
        else
            throw new NotFoundException("Unique Id alert is not displayed.");

        return getThis();
    }

    public boolean hasUniqueIdMsg()
    {
        expandPropertiesPanel();
        return elementCache().uniqueIdMsg.isDisplayed();
    }

    public boolean hasUniqueIdCheckIcon()
    {
        expandPropertiesPanel();
        return hasUniqueIdMsg() && elementCache().uniqueIdMsgCheckIcon.isDisplayed();
    }

    public String getUniqueIdMsg()
    {
        expandPropertiesPanel();
        return elementCache().uniqueIdMsg.getText();
    }

    protected class ElementCache extends EntityTypeDesigner<T>.ElementCache
    {
        protected final WebElement uniqueIdAlert = Locator.tagWithClassContaining("div","uniqueid-alert").refindWhenNeeded(this);
        protected final WebElement uniqueIdAlertAddButton = Locator.tagWithClassContaining("div","uniqueid-alert")
                .append(Locator.tag("button")).refindWhenNeeded(this);
        protected final WebElement uniqueIdMsg = Locator.tagWithClass("div","uniqueid-msg").refindWhenNeeded(this);
        protected final WebElement uniqueIdMsgCheckIcon = Locator.tagWithClass("div","uniqueid-msg")
                .append(Locator.tagWithClassContaining("i", "domain-panel-status-icon-green")).refindWhenNeeded(this);

        protected final WebElement addAliasButton = Locator.tagWithClass("i","container--addition-icon").findWhenNeeded(this);

        public List<Input> parentAliases()
        {
            return Input.Input(Locator.name("alias"), getDriver()).findAll(propertiesPanel);
        }

        Input parentAlias(int index)
        {
            return parentAliases().get(index);
        }

        ReactSelect parentAliasSelect(int index)
        {
            return ReactSelect.finder(getDriver())
                    .withInputClass("sampleset-insert--parent-select")
                    .index(index).find(propertiesPanel);
        }

        WebElement removeParentAliasIcon(int index)
        {
            return Locator.tagWithClass("i","container--removal-icon").findElements(propertiesPanel).get(index);
        }
    }
}
