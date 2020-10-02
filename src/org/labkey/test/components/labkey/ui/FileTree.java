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

    public DirectorySubTree selectFolder(String folderPath)
    {
        String[] pathParts = folderPath.split("/");
        DirectorySubTree dir = elementCache().rootDir;
        for (String pathPart : pathParts)
        {
            dir = dir.getSubfolder(pathPart);
        }
        return dir.select();
    }

    public void selectFile(String filePath)
    {
        int fileNameIndex = filePath.lastIndexOf('/');
        DirectorySubTree parentFolder = selectFolder(filePath.substring(0, fileNameIndex));
        parentFolder.getFile(filePath.substring(fileNameIndex + 1));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        DirectorySubTree rootDir = new DirectorySubTree(FileTree.directoryChildLoc(null, true).findWhenNeeded(this));

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

    private static final Pattern rotationPattern = Pattern.compile("transform: rotateZ\\((.+)deg\\);");

    public class DirectorySubTree extends Component<Component<?>.ElementCache>
    {
        private final WebElement _el; // <li> that wraps subtree
        private final WebElement _toggleArrow = Locator.xpath("./div[1]/div[1]").findWhenNeeded(this);
        private final WebElement _checkboxContainer = Locator.xpath("./div/span").withClass("filetree-checkbox-container").findWhenNeeded(this);
        private final WebElement _directoryName = Locator.byClass("filetree-directory-name").findWhenNeeded(this);
        private final WebElement _children = Locator.xpath("./div[2]").findWhenNeeded(this);
        private final FluentWait<Object> toggleWait = new FluentWait<>(new Object());

        DirectorySubTree(WebElement el)
        {
            _el = el;
        }

        DirectorySubTree(DirectorySubTree parent, String dirName)
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

        public DirectorySubTree getSubfolder(String dirName)
        {
            expand();
            return new DirectorySubTree(this, dirName);
        }

        public WebElement getFile(String fileName)
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

        public List<String> listDirs()
        {
            expand();
            return FileTree.directoryChildLoc(null, true).findElements(_children).stream()
                .map(WebElement::getText).collect(Collectors.toList());
        }

        public DirectorySubTree select()
        {
            if (!isActive())
            {
                _directoryName.click();
                waitForToggle();
            }
            return this;
        }

        private DirectorySubTree expand()
        {
            if (waitForToggle() != DirExpansionState.OPEN)
            {
                _toggleArrow.click();
                toggleWait.until(o -> getState() == DirExpansionState.OPEN);
            }
            return this;
        }

        private DirectorySubTree collapse()
        {
            if (waitForToggle() != DirExpansionState.CLOSED)
            {
                _toggleArrow.click();
                toggleWait.until(o -> getState() == DirExpansionState.CLOSED);
            }
            return this;
        }

        private boolean isActive()
        {
            return _checkboxContainer.getAttribute("class").contains("active");
        }

        private DirExpansionState waitForToggle()
        {
            return toggleWait.until(o -> {
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
            String style = StringUtils.trimToEmpty(_toggleArrow.getAttribute("style"));
            Matcher matcher = rotationPattern.matcher(style);
            if (matcher.matches())
            {
                String rotation = matcher.group(1);
                switch (rotation)
                {
                    case "90":
                        return DirExpansionState.OPEN;
                    case "0":
                        return DirExpansionState.CLOSED;
                    default:
                        return DirExpansionState.ANIMATING;
                }
            }
            else
            {
                throw new IllegalStateException("Unable to determine row expansion state. Possibly not a directory?: " + this.toString());
            }
        }
    }

    private enum DirExpansionState
    {
        OPEN, // transform: rotateZ(90deg)
        CLOSED, // transform: rotateZ(0deg)
        ANIMATING
    }
}
