package org.labkey.test.components.labkey.ui;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wraps 'packages/components/src/components/files/FileTree.tsx' from labkey-ui-components
 */
public class FileTree extends WebDriverComponent<FileTree.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    private FileTree(WebElement element, WebDriver driver)
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

    private void expandToFolder(DirectorySubTree parent, String[] pathParts)
    {
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        DirectorySubTree rootDir = new DirectorySubTree(FileTree.directorySubTreeLoc(null).findWhenNeeded(this));

    }

    public static class FileTreeFinder extends WebDriverComponentFinder<FileTree, FileTreeFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "filetree-container");

        public FileTreeFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected FileTree construct(WebElement el, WebDriver driver)
        {
            return new FileTree(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }

    private static Locator directorySubTreeLoc(String directory)
    {
        Locator.XPathLocator dirNameLoc = Locator.xpath("./div[1]//div").withClass("filetree-directory-name");
        if (directory != null)
        {
            dirNameLoc = dirNameLoc.withText(directory);
        }
        return Locator.xpath("./div/ul/li").withDescendant(dirNameLoc);
    }

    private static final Pattern rotationPattern = Pattern.compile("transform: rotateZ\\((.+)deg\\);");

    private class DirectorySubTree extends Component
    {
        private final WebElement _el; // <li> that wraps subtree
        private final WebElement _toggleArrow = Locator.xpath("./div[1]/div[1]").findWhenNeeded(this);
        private final WebElement _directoryName = Locator.byClass("filetree-directory-name").findWhenNeeded(this);

        DirectorySubTree(WebElement el)
        {
            _el = el;
        }

        DirectorySubTree(DirectorySubTree parent, String dirName)
        {
            this(FileTree.directorySubTreeLoc(dirName).findElement(parent._el));
        }

        @Override
        public WebElement getComponentElement()
        {
            return _el;
        }

        private String getRotation()
        {
            String style = StringUtils.trimToEmpty(_toggleArrow.getAttribute("style"));
            Matcher matcher = rotationPattern.matcher(style);
            if (matcher.matches())
            {
                String rotation = matcher.group(1);
                return rotation;
            }
            else
            {
                throw new IllegalStateException("Unable to determine row expansion state. Possibly not a directory?: " + this.toString());
            }
        }
    }

    private enum DirExpansionState
    {

    }
}
