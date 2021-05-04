package org.labkey.test.components.ui.ontology;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

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

    private boolean isLeaf()
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

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement caretContainer = Locator.tagWithAttributeContaining("div", "style", "transform: rotateZ")
                .withDescendant(Locator.tag("polygon")).findWhenNeeded(this);
        final WebElement caret = Locator.tag("polygon").findElement(caretContainer);
        final WebElement checkboxContainer = Locator.tagWithClass("span", "filetree-checkbox-container")
                .refindWhenNeeded(this).withTimeout(1500);
        final WebElement resourceRow = Locator.tagWithClass("div", "filetree-resource-row")
                .refindWhenNeeded(checkboxContainer).withTimeout(1500);

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
        // this locator finds only children of the UL, which hopefully filters
        private final Locator.XPathLocator _baseLocator = Locator.tag("ul").child(Locator.tag("li")
                .withChild(Locator.tagWithClass("span", "filetree-checkbox-container")));
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

        @Override
        protected TreeNode construct(WebElement el, WebDriver driver)
        {
            return new TreeNode(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withChild(Locator.tag("div")
                                .withChild(Locator.tag("div")
                                .withChild(Locator.tagWithClass("div", "filetree-resource-row")
                                        .withAttribute("title", _title))));
            else
                return _baseLocator;
        }
    }
}
