package org.labkey.test.components.labkey.ui;

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
 * TODO: Merge with {@link org.labkey.test.components.glassLibrary.files.FileSelectTree}
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

    public SubTree selectFolder(String folderPath)
    {
        SubTree dir = elementCache().rootDir;
        if (!folderPath.isEmpty())
        {
            String[] pathParts = folderPath.split("/");
            for (String pathPart : pathParts)
            {
                dir = dir.findSubfolder(pathPart);
            }
        }
        return dir.select();
    }

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

    public class SubTree extends Component<Component<?>.ElementCache>
    {
        private final WebElement _el; // <li> that wraps subtree
        private final WebElement _toggleArrow = Locator.xpath("./div[1]/div[1]").findWhenNeeded(this);
        private final WebElement _checkboxContainer = Locator.xpath("./div/span").withClass("filetree-checkbox-container").findWhenNeeded(this);
        private final WebElement _icon = Locator.css("svg.filetree-folder-icon").findWhenNeeded(_checkboxContainer);
        private final WebElement _directoryName = Locator.byClass("filetree-directory-name").findWhenNeeded(this);
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

        public String getName()
        {
            return _directoryName.getText();
        }

        public SubTree findSubfolder(String dirName)
        {
            expand();
            return new SubTree(this, dirName);
        }

        public WebElement findFile(String fileName)
        {
            expand();
            return FileTree.directoryChildLoc(fileName, false).findElement(_children);
        }

        public boolean containsFile(String name)
        {
            expand();
            return FileTree.directoryChildLoc(name, false).existsIn(_children);
        }

        public boolean containsDir(String name)
        {
            expand();
            return FileTree.directoryChildLoc(name, true).existsIn(_children);
        }

        public List<String> listFiles()
        {
            expand();
            return FileTree.directoryChildLoc(null, false).findElements(_children).stream()
                .map(WebElement::getText).collect(Collectors.toList());
        }

        public List<String> listFolders()
        {
            expand();
            return FileTree.directoryChildLoc(null, true).findElements(_children).stream()
                .map(WebElement::getText).collect(Collectors.toList());
        }

        public SubTree select()
        {
            if (!isActive())
            {
                _directoryName.click();
                waitForToggle();
            }
            return this;
        }

        private SubTree expand()
        {
            if (waitForToggle() != DirExpansionState.OPEN)
            {
                _toggleArrow.click();
                toggleWait.withMessage(() -> String.format("Waiting for '%s' to expand.", getName()))
                    .until(o -> getState() == DirExpansionState.OPEN);
            }
            return this;
        }

        private SubTree collapse()
        {
            if (waitForToggle() != DirExpansionState.CLOSED)
            {
                _toggleArrow.click();
                toggleWait.withMessage(() -> String.format("Waiting for '%s' to collapse.", getName()))
                    .until(o -> getState() == DirExpansionState.CLOSED);
            }
            return this;
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
