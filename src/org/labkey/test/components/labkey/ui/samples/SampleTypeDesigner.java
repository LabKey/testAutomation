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
import org.openqa.selenium.WrapsDriver;

import java.util.List;

import static org.labkey.test.WebDriverWrapper.sleep;

/**
 * Automates the LabKey ui component defined in: packages/components/src/components/domainproperties/samples/SampleTypeDesigner.tsx
 */
public class SampleTypeDesigner extends WebDriverComponent<SampleTypeDesigner.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    public SampleTypeDesigner(WebDriver driver)
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

    public DomainFormPanel getDomainEditor()
    {
        return elementCache()._domainFormPanel;
    }

    public SampleTypeDesigner removeFields(boolean confirmDialogExpected, String... fieldNames)
    {
        for (String fieldName : fieldNames)
        {
            getDomainEditor().removeField(fieldName, confirmDialogExpected);
            sleep(250); // wait for collapse animation
        }
        return this;
    }

    public SampleTypeDesigner addFields(FieldDefinition... fields)
    {
        for (FieldDefinition field : fields)
        {
            getDomainEditor().addField(field);
        }
        return this;
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

    public void clickSave(boolean isUpdate)
    {
        elementCache().saveButton.click();
        String expectedText = isUpdate ? "Successfully updated sample type details." : "Successfully created a new sample type.";
        BootstrapLocators.successBanner.withText(expectedText).waitForElement(getDriver(), 10000);
    }

    public List<WebElement> clickSaveExpectingError()
    {
        elementCache().saveButton.click();
        return BootstrapLocators.errorBanner.waitForElements(getWrapper().shortWait());
    }

    public SampleTypeDesigner setName(String name)
    {
        elementCache().nameInput.set(name);
        return this;
    }

    public String getName()
    {
        return elementCache().nameInput.get();
    }

    public SampleTypeDesigner setNameExpression(String nameExpression)
    {
        elementCache().nameExpressionInput.set(nameExpression);
        return this;
    }

    public String getNameExpression()
    {
        return elementCache().nameExpressionInput.get();
    }

    public SampleTypeDesigner setDescription(String description)
    {
        elementCache().descriptionInput.set(description);
        return this;
    }

    public String getDescription()
    {
        return elementCache().descriptionInput.get();
    }

    public SampleTypeDesigner addParentAlias()
    {
        elementCache().addAliasButton.click();
        return this;
    }

    public SampleTypeDesigner removeParentAlias(int index)
    {
        elementCache().removeParentAliasIcon(index).click();
        return this;
    }

    public SampleTypeDesigner setParentAlias(int index, String alias, String optionDisplayText)
    {
        elementCache().parentAlias(index).setValue(alias);
        elementCache().parentAliasSelect(index).click();
        elementCache().selectParentOption(index, optionDisplayText).click();

        return this;
    }

    public String getParentAlias(int index)
    {
        return elementCache().parentAlias(index).get();
    }

    public String getParentAliasSelectText(int index)
    {
        return elementCache().parentAliasSelect(index).getText();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        protected final Input nameInput = Input.Input(Locator.id("entity-name"), getDriver()).findWhenNeeded(this);
        protected final Input nameExpressionInput = Input.Input(Locator.id("entity-nameExpression"), getDriver()).waitFor(this);
        protected final Input descriptionInput = Input.Input(Locator.id("entity-description"), getDriver()).findWhenNeeded(this);
        protected final WebElement addAliasButton = Locator.tagWithClass("i","container--addition-icon").findWhenNeeded(this);

        protected final DomainFormPanel _domainFormPanel = new DomainFormPanel.DomainFormPanelFinder(getDriver()).timeout(1000).findWhenNeeded(this);

        protected final WebElement cancelButton = Locator.button("Cancel").findWhenNeeded(this);
        protected final WebElement saveButton = Locator.button("Save").findWhenNeeded(this);

        protected Input parentAlias(int index)
        {
            return Input.Input(Locator.name("alias"), getDriver()).findAll(this).get(index);
        }

        protected WebElement parentAliasSelect(int index)
        {
            return Locator.tagWithClass("div", "Select-control").findElements(getDriver()).get(index);
        }

        protected WebElement selectParentOption(int index, String optionDisplayText)
        {
            return Locator.tagWithText("div", optionDisplayText).findElements(getDriver()).get(index);
        }

        protected WebElement removeParentAliasIcon(int index)
        {
            return Locator.tagWithClass("i","container--removal-icon").findElements(getDriver()).get(index);
        }
    }

    public interface SavesSampleType<W, S> extends WrapsDriver
    {
        Locator getBodyLocator();
        S getSaveDestination();
    }
}
