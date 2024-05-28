package org.labkey.test.components.ui.files;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Wraps 'packages/components/src/components/files/FileTree.tsx' from labkey-ui-components
 * TODO: Merge with {@link FileSelectTree}
 * 'FileSelectTree' assumes that rows have checkboxes, this component doesn't support checkboxes at all
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

    /**
     * Select a folder at the given relative path under the tree's root node.
     * This will expand parent folders as needed to reveal the specified folder.
     * @param folderPath relative to the tree's root. <code>null</code> or blank to select the root node.
     * @return The subtree component with the specified folder as the tree's root.
     */
    public SubTree selectFolder(String folderPath)
    {
        SubTree dir = elementCache().rootDir;
        if (!StringUtils.isBlank(folderPath))
        {
            String[] pathParts = folderPath.split("/");
            for (String pathPart : pathParts)
            {
                dir = dir.findFolder(pathPart);
            }
        }
        return dir.select();
    }
    /**
     * Select a file at the given relative path under the tree's root node.
     * This will expand parent folders as needed to reveal the specified file.
     * @param filePath relative to the tree's root.
     * @return The row element for the selected file.
     */
    public WebElement selectFile(String filePath)
    {
        int fileNameIndex = filePath.lastIndexOf('/');
        SubTree parentFolder = selectFolder(fileNameIndex > 0 ? filePath.substring(0, fileNameIndex) : "");
        WebElement fileRow = parentFolder.findFile(filePath.substring(fileNameIndex + 1));
        fileRow.click();
        Locator.css("span.filetree-leaf-node.active").waitForElement(fileRow, 1000);
        return fileRow;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        SubTree rootDir = new SubTree(FileTree.directoryChildLoc(null, true).findWhenNeeded(this));

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

    private static Locator directoryChildLoc(String name, boolean dir)
    {
        String nameDivClass = dir ? "filetree-directory-name" : "filetree-file-name";
        Locator.XPathLocator nameDivLoc = Locator.xpath("./div[1]//div").withClass(nameDivClass);
        if (name != null)
        {
            nameDivLoc = nameDivLoc.withText(name);
        }
        return Locator.xpath("./ul/li").withDescendant(nameDivLoc);
    }

    private static final Pattern folderIconPattern = Pattern.compile(".*(fa-folder.*?)(?: |$).*");

    /**
     * Represents a particular subtree within the <code>FileTree</code> component. May represent the root node.
     * The DOM looks something like this:
     * <pre>
     * &lt;li>
     *     &lt;div>
     *         Folder's name, icon, selection state, etc.
     *     &lt;/div>
     *     &lt;div>
     *         &lt;ul>
     *             Children: subtrees and files
     *         &lt;/ul>
     *     &lt;/div>
     * &lt;/li>
     * </pre>
     */
    public class SubTree extends Component<Component<?>.ElementCache>
    {
        // <li> that wraps subtree
        private final WebElement _el;

        // Elements in the first <div> under the <li>.
        private final WebElement _toggleArrow = Locator.xpath("./div[1]/div[1]").findWhenNeeded(this);
        private final WebElement _checkboxContainer = Locator.xpath("./div/span").withClass("filetree-checkbox-container").findWhenNeeded(this);
        private final WebElement _icon = Locator.css("span.filetree-folder-icon").findWhenNeeded(_checkboxContainer);
        private final WebElement _directoryName = Locator.byClass("filetree-directory-name").findWhenNeeded(this);

        // Second <div> under the <li>
        private final WebElement _children = Locator.xpath("./div[2]").findWhenNeeded(this);

        private final FluentWait<Object> toggleWait = new FluentWait<>(new Object());

        SubTree(WebElement el)
        {
            _el = el;
        }

        SubTree(SubTree parent, String dirName)
        {
            this(FileTree.directoryChildLoc(dirName, true).findElement(parent._children));
        }

        @Override
        public WebElement getComponentElement()
        {
            return _el;
        }

        /**
         * Get the name of this folder
         * @return folder name
         */
        public String getName()
        {
            return _directoryName.getText();
        }

        /**
         * Expand self and find a child directory with the given name
         * @param dirName directory name
         * @return subtree for the specified directory
         */
        public SubTree findFolder(String dirName)
        {
            expand();
            return new SubTree(this, dirName);
        }

        /**
         * Expand self and find a child file with the given name
         * @param fileName file name
         * @return row element for the specified file
         */
        public WebElement findFile(String fileName)
        {
            expand();
            return FileTree.directoryChildLoc(fileName, false).findElement(_children);
        }

        /**
         * Check whether the specified file is a direct child of this folder
         * @param name file name
         * @return <code>true</code> if the file exists
         */
        public boolean containsFile(String name)
        {
            expand();
            return FileTree.directoryChildLoc(name, false).existsIn(_children);
        }

        /**
         * Check whether the specified folder is a direct child of this folder
         * @param name file name
         * @return <code>true</code> if the file exists
         */
        public boolean containsFolder(String name)
        {
            expand();
            return FileTree.directoryChildLoc(name, true).existsIn(_children);
        }

        /**
         * Get names of all child Files
         * @return List of file names
         */
        public List<String> listFiles()
        {
            expand();
            return FileTree.directoryChildLoc(null, false).findElements(_children).stream()
                .map(WebElement::getText).collect(Collectors.toList());
        }

        /**
         * Get names of all child Folders
         * @return List of folder names
         */
        public List<String> listFolders()
        {
            expand();
            return FileTree.directoryChildLoc(null, true).findElements(_children).stream()
                .map(WebElement::getText).collect(Collectors.toList());
        }

        /**
         * Select this folder.
         * If not already selected, this will toggle the expand/collapse state as well.
         * @return <code>this</code>
         */
        public SubTree select()
        {
            if (!isActive())
            {
                _directoryName.click();
                waitForToggle();
            }
            return this;
        }

        private void expand()
        {
            if (waitForToggle() != DirExpansionState.OPEN)
            {
                _toggleArrow.click();
                toggleWait.withMessage(() -> String.format("Waiting for '%s' to expand.", getName()))
                    .until(o -> getState() == DirExpansionState.OPEN);
            }
        }

        private void collapse()
        {
            if (waitForToggle() != DirExpansionState.CLOSED)
            {
                _toggleArrow.click();
                toggleWait.withMessage(() -> String.format("Waiting for '%s' to collapse.", getName()))
                    .until(o -> getState() == DirExpansionState.CLOSED);
            }
        }

        private boolean isActive()
        {
            return _checkboxContainer.getAttribute("class").contains("active");
        }

        private DirExpansionState waitForToggle()
        {
            return toggleWait.withMessage(() -> String.format("Waiting for '%s' to animate.", getName()))
                .until(o -> {
                    DirExpansionState state = getState();
                    if (state == DirExpansionState.ANIMATING)
                    {
                        return null; // keep waiting
                    }
                    else
                    {
                        return state;
                    }
                });
        }

        private DirExpansionState getState()
        {
            boolean animating = StringUtils.trimToEmpty(_toggleArrow.getAttribute("class")).contains("animating");
            if (animating)
            {
                return DirExpansionState.ANIMATING;
            }

            String iconClasses = _icon.getAttribute("class");
            Matcher matcher = folderIconPattern.matcher(iconClasses);
            if (matcher.matches())
            {
                String folderIcon = matcher.group(1);
                switch (folderIcon)
                {
                    case "fa-folder-open":
                        return DirExpansionState.OPEN;
                    case "fa-folder":
                        return DirExpansionState.CLOSED;
                    default:
                        break; // Fall through to IllegalStateException
                }
            }

            throw new IllegalStateException(String.format(
                "Unable to determine row expansion state for '%s'. Possibly not a directory?: %s", getName(), _icon.toString()));
        }
    }

    private enum DirExpansionState
    {
        OPEN, // fa-folder
        CLOSED, // fa-folder-open
        ANIMATING
    }
}
