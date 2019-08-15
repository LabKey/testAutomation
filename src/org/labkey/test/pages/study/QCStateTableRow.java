package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class QCStateTableRow extends WebDriverComponent<QCStateTableRow.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public QCStateTableRow(WebElement element, WebDriver driver)
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

    public boolean isEditable()
    {
        return elementCache().stateNameElement().getTagName().equals("input");
    }

    public String getState()
    {
        if (isEditable())
            return elementCache().stateNameElement().getAttribute("value");
        else
            return elementCache().stateNameElement().getText();
    }

    public QCStateTableRow setState(String state)
    {
        if (!isEditable())
            throw new IllegalStateException("the current qc state row is not editable");

        new Input(elementCache().stateNameElement(), getDriver()).setValue(state);
        return this;
    }

    public String getDescription()
    {
        if (isEditable())
            return elementCache().descElement().getAttribute("value");
        else
            return elementCache().descElement().getText();
    }

    public QCStateTableRow setDescription(String description)
    {
        if (!isEditable())
            throw new IllegalStateException("the current qc state row is not editable");

        new Input(elementCache().descElement(), getDriver()).setValue(description);
        return this;
    }

    public boolean getPublicData()
    {
        return elementCache().publicDataCheckbox.get();
    }

    public QCStateTableRow setPublicData(boolean isPublicData)
    {
        elementCache().publicDataCheckbox.set(isPublicData);
        return this;
    }

    public boolean isInUse()
    {
        return Locator.tag("td").child(Locator.tagWithClass("span", "fa-check-circle"))
                .existsIn(this);
    }

    public void remove()
    {
        if (!isEditable())
            throw new IllegalStateException("the current qc state row is not editable");

        Locator.tagWithClass("span", "fa-times").findElement(this).click();
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        final WebElement stateNameElement()
        {
            WebElement stateInput = Locator.findAnyElementOrNull(this, Locators.editableStateInput,
                    Locators.stateInput);

            if (null != stateInput)
                return stateInput;
            else
                return Locator.tag("td").index(0).findElement(this);
        }

        final WebElement descElement()
        {
            WebElement descInput =  Locator.findAnyElementOrNull(this, Locators.editableDescriptionInput,
                    Locators.descInput);

            if (null != descInput)
                return descInput;
            else
                return Locator.tag("td").index(1).findElement(this);
        }

        final WebElement publicDataChecboxElement = Locator.tagWithAttribute("input", "type", "checkbox")
                .findWhenNeeded(getComponentElement()).withTimeout(2000);
        final Checkbox publicDataCheckbox = new Checkbox(publicDataChecboxElement);

        final WebElement inUseLink = Locator.tag("td").withChild(Locator.tagWithClass("span", "fa-*-circle"))
                .child("a").findWhenNeeded(this).withTimeout(2000);
    }

    static public class Locators
    {
        static public Locator.XPathLocator tableLoc = Locator.id("qcStatesTable").withClass("lk-fields-table");
        static public Locator.XPathLocator rowLoc = tableLoc.child("tbody").child("tr")                 // row
                .withDescendant(Locator.tagWithClassContaining("span", "circle"));
        static public Locator lastRow = Locator.xpath("(//table[@id='qcStatesTable' ]/tbody/tr)[last()]");

        static public Locator stateInput = Locator.input("labels");
        static public Locator editableStateInput = Locator.input("newLabels");
        static public Locator descInput = Locator.input("descriptions");
        static public Locator editableDescriptionInput = Locator.input("newDescriptions");
    }

    public static class QCStateTableRowFinder extends WebDriverComponentFinder<QCStateTableRow, QCStateTableRowFinder>
    {
        private Locator.XPathLocator _locator = Locators.rowLoc;

        public QCStateTableRowFinder(WebDriver driver)
        {
            super(driver);
        }

        public QCStateTableRowFinder withName(String name)
        {
            _locator = Locators.rowLoc.withDescendant(Locator.input("labels").withAttribute("value", name));
            return this;
        }

        @Override
        protected QCStateTableRow construct(WebElement el, WebDriver driver)
        {
            return new QCStateTableRow(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
