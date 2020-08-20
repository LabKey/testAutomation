package org.labkey.test.components.react.files;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class FileSelectTree extends WebDriverComponent<FileSelectTree.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public FileSelectTree(WebElement element, WebDriver driver)
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

    // expand node
    private WebElement expandNode(SearchContext parent, String nodeName)
    {
        // if the parent is null, we start at the root.  Otherwise, we search from the given scope
        WebElement nodeContainer = ( parent == null ) ?
                elementCache().expansionListItem(elementCache().rootListElement, "root") :
                elementCache().expansionListItem(parent, nodeName);

        WebElement nodeRow = elementCache().expansionNode(nodeContainer, nodeName);    // nodeRow has the expand/collapse caret in it
        if (elementCache().collapsedCaret.existsIn(nodeRow))
        {
            elementCache().collapsedCaret.findElement(nodeRow).click();    // expand it
            WebDriverWrapper.waitFor(()-> elementCache().expandedCaret.existsIn(nodeRow),
                "the node did not expand", 2000);
            this.waitForLoadingFileTree();
        }
        return Locator.tag("ul").waitForElement(nodeContainer, 2000);   // contains the new child list, which is a //div/ul with list items
    }

    public FileSelectTree waitForLoadingFileTree()
    {
        WebDriverWrapper.waitFor(()-> (this.findElements(By.xpath("//span[contains(text(), 'Loading...')]")).size() < 1),
                "File tree loading too long", 4000);

        return this;
    }

    private FileSelectTree collapseNode(SearchContext parent, String nodeName)
    {
        WebElement nodeRow = elementCache().expansionNode(parent, nodeName);
        if (!elementCache().expandedCaret.existsIn(nodeRow))
            return this;
        else
        {
            nodeRow.click();
            WebDriverWrapper.waitFor(()-> !elementCache().expandedCaret.existsIn(nodeRow),
                    "the node did not collapse", 2000);
        }
        return this;
    }

    public FileSelectTree selectAllInNode(List<String> path, String nodeName, boolean checked)
    {
        getNodeSelectCheckbox(path, nodeName).set(checked);
        return this;
    }

    public Boolean isNodeSelected(List<String> path, String nodeName)
    {
        return getNodeSelectCheckbox(path, nodeName).get();
    }

    public FileSelectTree selectFile(List<String> path, String fileName, boolean checked)
    {
        getFileSelectCheckbox(path, fileName).set(checked);
        return this;
    }

    public boolean isFileSelected(List<String> path, String fileName)
    {
        return getFileSelectCheckbox(path, fileName).get();
    }

    private Checkbox getFileSelectCheckbox(List<String> path, String fileName)
    {
        WebElement nodeRowContainer = expandPathTo(path);
        return new Checkbox(elementCache().leafNodeCheckBox(fileName).waitForElement(nodeRowContainer, 2000));
    }

    private Checkbox getNodeSelectCheckbox(List<String> path, String nodeName)
    {
        WebElement nodeRowContainer = expandPathTo(path);
        WebElement nodeRow = elementCache().expansionNode(nodeRowContainer, nodeName);
        return new Checkbox(elementCache().expansionNodeCheckBox(nodeName)
                .findElement(nodeRow));
    }


    private WebElement expandPathTo(List<String> path)
    {
        WebElement nodeRowContainer = null;
        for (String node : path)
        {
            nodeRowContainer = expandNode(nodeRowContainer, node);
        }
        return nodeRowContainer;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        WebElement rootListElement = Locator.tag("ul").findWhenNeeded(this).withTimeout(4000);
        Locator.XPathLocator toggleCaret = Locator.tagWithAttributeContaining("div", "style", "transform:");
        Locator expandedCaret = toggleCaret.withAttributeContaining("style", "rotateZ(90deg)");
        Locator collapsedCaret = toggleCaret.withAttributeContaining("style", "rotateZ(0deg)");

        Locator.XPathLocator expansionRow = Locator.tagWithAttributeContaining("div", "style", "cursor")
                .withChild(toggleCaret);    // style tells us open or closed

        // expansionListItem contains the expansionNode, and its children once it is expanded
        WebElement expansionListItem(SearchContext parent, String nodeText)
        {
            return Locator.tag("li").withChild(expansionRow.withChild(checkboxContainer.containing(nodeText)))
                    .waitForElement(parent, 2000);
        }

        // expansionNode contains a caret, its toggle, a checkbox to select all in its scope
        WebElement expansionNode(SearchContext parent, String nodeText)
        {
            return expansionRow.withChild(checkboxContainer.containing(nodeText)).findElement(parent);
        }

        // leaf nodes are for file selection; expansionNodes are to expand/collapse a tree
        Locator.XPathLocator checkboxContainer = Locator.tagWithClass("span", "filetree-checkbox-container");
        Locator.XPathLocator leafNodeCheckBox(String fileName)
        {
            return checkboxContainer.withClass("filetree-leaf-node").containing(fileName)
                    .descendant(Locator.tagWithAttribute("input", "type", "checkbox"));
        }
        Locator.XPathLocator expansionNodeCheckBox(String folderName)
        {
            return expansionRow.withChild(checkboxContainer.containing(folderName))
                    .descendant(Locator.tagWithAttribute("input", "type", "checkbox"));
        }
    }

    public static class FileSelectTreeFinder extends WebDriverComponentFinder<FileSelectTree, FileSelectTreeFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "filetree-container");
        private String _title = null;

        public FileSelectTreeFinder(WebDriver driver)
        {
            super(driver);
        }

        public FileSelectTreeFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected FileSelectTree construct(WebElement el, WebDriver driver)
        {
            return new FileSelectTree(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withAttribute("title", _title);
            else
                return _baseLocator;
        }
    }
}
