package org.labkey.test.components.ui.grids;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ui.entities.ParentEntityEditPanel;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
    Wraps the read-only state of ParentEntityEditPanel.
    If no parent is selected, the detailsTable is singluar and contains information saying that no parent/source type has been selected.

    If one or more parent type/id are selected, the detailsTables for each parentType are accompanied by a responsiveGrid
    that list the parent or source records of that type
 */
public class ParentDetailPanel extends WebDriverComponent<ParentDetailPanel.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected ParentDetailPanel(WebElement element, WebDriver driver)
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

    public String title()
    {
        return elementCache().panelHeading.getText();
    }

    public String parentsString()
    {
        return elementCache().parentsForElement.getText();
    }

    public boolean isEditable()
    {
        return elementCache().optionalEditBtn().isPresent();
    }

    public Map<String, ResponsiveGrid> getParentDetailsPanelGrids()
    {
        Map detailsGridMap = new HashMap();
        for (String parentType : getParentTypes())
            detailsGridMap.put(parentType, getParentsGridFor(parentType));
        return detailsGridMap;
    }

    public boolean hasParentTypes()
    {
        return getParentTypes().size() > 0;
    }

    public List<String> getParentTypes()
    {
        return getWrapper().getTexts(elementCache().gridParentTypesLinks());
    }

    /*
        gets the details table for the given parent/source type
     */
    public DetailTable getDetailTableForType(String type)
    {
        return elementCache().detailTableFor(type);
    }

    /*
        gets the grid containing parents/sources for the given parent/source type
     */
    public ResponsiveGrid getParentsGridFor(String type)
    {
        return elementCache().responsiveGridFor(type);
    }

    public ParentEntityEditPanel clickEdit()
    {
        String title = title();
        if (!isEditable())
            throw new IllegalStateException("The current pane (with title ["+title+"]) is not editable");

        var editBtn = elementCache().editBtn;
        editBtn.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(editBtn));
        return new ParentEntityEditPanel.ParentEntityEditPanelFinder(getDriver()).waitFor();
    }


    @Override
    protected void waitForReady()
    {
        WebDriverWrapper.waitFor(()-> {
                    if(BootstrapLocators.loadingSpinner.findWhenNeeded(this).isDisplayed())
                    {
                        return false;   // any spinner being present means the control is not ready
                    }
                    else
                    {
                        List<DetailTable> detailTables = new DetailTable.DetailTableFinder(getDriver()).findAll(this);
                        if (detailTables.size() == 0)
                            return false;

                        // at least one detailtable exists, each with some value in it
                        for (DetailTable table : detailTables)
                        {
                            table.waitForReady();

                            // each table must show an indicator of having no source or parent types, or have a linked parent/source type
                            if (table.getTableData().values().isEmpty())
                                return false;
                        }
                        return true;
                    }
                },
                "the component did not become ready in time", 4000);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public WebElement panelHeading = Locator.tagWithClass("div", "panel-heading")
                .findWhenNeeded(this).withTimeout(2000);

        public Locator editBtnLoc = Locator.tagWithClass("div", "detail__edit-button");
        public Optional<WebElement> optionalEditBtn()
        {
            return editBtnLoc.findOptionalElement(panelHeading);
        }
        public WebElement editBtn = editBtnLoc.findWhenNeeded(panelHeading);

        public WebElement panelBody = Locator.tagWithClass("div", "panel-body").findWhenNeeded(this);
        public WebElement parentsForElement = Locator.tagWithClass("div", "bottom-spacing").child("b")
                .findWhenNeeded(panelBody);

        // finds a webElement that will contain the detailTable and responsiveGrid for the specified source or parent type
        public WebElement detailGroupContainer(String containingType)
        {
            return Locator.tagWithClass("div", "top-spacing")
                    .withChild(Locator.tagWithClass("table", "detail-component--table__fixed")
                            .withDescendant(Locator.tag("td").withChild(Locator.linkWithText(containingType))))
                    .waitForElement(panelBody, 2000);
        }
        // finds the first link in each detail table in each detailGroupContainer; used to get a list of parent types
        public List<WebElement> gridParentTypesLinks()
        {
            Locator detailGroupContainers = Locator.tagWithClass("div", "top-spacing")
                    .child(Locator.tagWithClass("table", "detail-component--table__fixed")
                            .descendant(Locator.tag("td").child(Locator.tag("a"))));
            return detailGroupContainers.findElements(elementCache().panelBody);
        }

        public DetailTable detailTableFor(String type)
        {
            return new DetailTable.DetailTableFinder(getDriver()).waitFor(detailGroupContainer(type));
        }

        public ResponsiveGrid responsiveGridFor(String type)
        {
            return new ResponsiveGrid.ResponsiveGridFinder(getDriver()).withGridId("model").waitFor(detailGroupContainer(type));
        }
    }


    public static class ParentDetailPanelFinder extends WebDriverComponentFinder<ParentDetailPanel, ParentDetailPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "panel-default")
                .withChild(Locator.tagWithClass("div", "panel-heading")
                        .withChild(Locator.tagWithClass("div", "detail__edit--heading")));
        private String _title = null;

        public ParentDetailPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public ParentDetailPanelFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected ParentDetailPanel construct(WebElement el, WebDriver driver)
        {
            return new ParentDetailPanel(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return  _baseLocator.withDescendant(Locator.tagWithClass("div", "detail__edit--heading")
                        .withText(_title));
            else
                return _baseLocator;
        }
    }
}
