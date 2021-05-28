package org.labkey.test.components.ui.ontology;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.Optional;

/**
 * wraps nodes in the ontologyTreePanel
 */
public class TreeNode extends WebDriverComponent<TreeNode.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected TreeNode(WebElement element, WebDriver driver)
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


    public String getTitle()
    {
        return elementCache().resourceRow.getAttribute("title");
    }

    public TreeNode expand()
    {
        if (!isExpanded())
        {
            elementCache().caret.click();
            getWrapper().waitFor(()-> isExpanded(), "The treenode did not expand in time",1000);
        }
        return this;
    }

    public TreeNode collapse()
    {
        if (isExpanded())
        {
            elementCache().caret.click();
            getWrapper().waitFor(()-> !isExpanded(), "the treenode did not collapse in time", 1000);
        }
        return this;
    }

    /**
     * a Tree node that is selected shows up in blue bold text.  The root always remains selected, while only
     * the lowest-traversed node (which can be a branch or a leaf node) will be selected.  The selected node's
     * information will appear in the detail tabs when they are shown
     * @return
     */
    public TreeNode select()
    {
        if (!isSelected())
            elementCache().checkboxContainer.click();
        getWrapper().waitFor(()-> isSelected(), "the node did not become selected in time", 1000);
        return this;
    }

    private boolean isExpanded()
    {
        return elementCache().caretContainer.getAttribute("style").contains("90deg");
    }

    private boolean isSelected()
    {
        return elementCache().checkboxContainer.getAttribute("class").contains("active");
    }

    public boolean isLeaf()
    {
        return elementCache().checkboxContainer.getAttribute("class").contains("filetree-leaf-node");
    }

    public List<TreeNode> getChildren()
    {
        expand();
        return elementCache().children();
    }

    public TreeNode getChild(String title)
    {
        expand();
        return new TreeNode.TreeNodeFinder(getDriver()).withTitle(title).waitFor(elementCache().childrenContainer);
    }

    public Optional<WebElement> getFilterElement()
    {
        return elementCache().filterElement();
    }

    public boolean isFilterSelected()
    {
        var filterElement = getFilterElement();
        if (filterElement.isPresent())
        {
            String elementClass = filterElement.get().getAttribute("class");
            return elementClass.contains("selected");
        }
        else
            return false;
    }

    public TreeNode setFilterSelect(boolean select)
    {
        assert(getFilterElement().isPresent());
        var filterElement = getFilterElement().get();
        if (select)
        {
            if (!isFilterSelected())
            {

                getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(filterElement));
                filterElement.click();
                getWrapper().waitFor(() -> isFilterSelected(), "the filter did not become selected", 2000);
            }
        } else
        {
            if (isFilterSelected())
            {
                getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(filterElement));
                filterElement.click();
                getWrapper().waitFor(() -> !isFilterSelected(), "the filter did not become deselected", 2000);
            }
        }
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement caretContainer = Locator.tagWithAttributeContaining("div", "style", "transform: rotateZ")
                .findWhenNeeded(this);
        final WebElement caret = Locator.tag("polygon").findWhenNeeded(caretContainer);
        final WebElement checkboxContainer = Locator.tagWithClass("span", "filetree-checkbox-container")
                .refindWhenNeeded(this).withTimeout(1500);
        final WebElement resourceRow = Locator.tagWithClass("div", "filetree-resource-row")
                .refindWhenNeeded(checkboxContainer).withTimeout(1500);
        Optional<WebElement> filterElement()
        {
            return Locator.tagWithClass("i", "fa-filter").findOptionalElement(resourceRow);
        }

        // note: when expanded/collapsed, the UL is destroyed/recreated
        // because we want to search just for children (not descendants) use the div as searchcontext, so
        // children of the UL can be found/further descendants can be filtered out
        final WebElement childrenContainer = Locator.tag("div").withChild(Locator.tag("ul")).findWhenNeeded(this);

        List<TreeNode> children()
        {
            return new TreeNode.TreeNodeFinder(getDriver()).findAll(childrenContainer);
        }
    }


    public static class TreeNodeFinder extends WebDriverComponentFinder<TreeNode, TreeNodeFinder>
    {
        // this locator finds only children of the UL, which hopefully filters out descendants
        private final Locator.XPathLocator _baseLocator = Locator.tag("ul").child(Locator.tag("li")
                .withChild(Locator.tag("div")
                        .withChild(Locator.tagWithClass("span", "filetree-checkbox-container"))));
        private Boolean _active = null;
        private Boolean _withFilterSelected = null;
        private String _title = null;

        public TreeNodeFinder(WebDriver driver)
        {
            super(driver);
        }

        public TreeNodeFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        /**
         * use this finder to find treeNodes that aren't necessarily a direct child of the current search context, but
         * which are 'active'.
         * For example, search for children of a treeNode will return only direct descendants; use this one to get the
         * active/focused node(s) in the tree
         * @return
         */
        public TreeNodeFinder activeOnly()
        {
            _active = true;
            return this;
        }

        /**
         * use this to search for nodes that have a filter icon
         * @return
         */
        public TreeNodeFinder withSelectedFilter()
        {
            _withFilterSelected = true;
            return this;
        }

        @Override
        protected TreeNode construct(WebElement el, WebDriver driver)
        {
            return new TreeNode(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withDescendant(Locator.tagWithClass("div", "filetree-resource-row")
                                        .withAttribute("title", _title));
            else if (_active != null)
                // this mode of search doesn't limit to finding only as a child of the containing element; use
                // this to find whatever node is 'active' (or selected) within the tree
                return Locator.tag("li").withChild(Locator.tag("div")
                        .withChild(Locator.tagWithClass("span", "filetree-checkbox-container")
                            .withClass("active")));
            else if (_withFilterSelected != null)
                return Locator.tag("li").withChild(Locator.tag("div")
                        .withChild(Locator.tagWithClass("span", "filetree-checkbox-container")
                                .withDescendant(Locator.tagWithClass("i", "fa-filter").withClass("selected"))));
            else
                return _baseLocator;
        }
    }
}
