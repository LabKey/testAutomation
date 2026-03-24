package org.labkey.test.params;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.TestProperties;
import org.labkey.test.util.AbstractContainerHelper;
import org.labkey.test.util.TestDataGenerator;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

import static org.labkey.test.util.TestDataGenerator.ALL_CHARS_PLACEHOLDER;
import static org.labkey.test.util.TestDataGenerator.WIDE_PLACEHOLDER;
import static org.labkey.test.util.TextUtils.containerPath;

public class ContainerInfo
{
    public static final String TRICKY_CHARACTERS = "\u2603~!@$&()_+{}-=[],.#\u00E4\u00F6\u00FC\u00C5"; // No slash or space. Don't change; hard-coded in some test data
    public static final String RANDOM_CHARSET = TRICKY_CHARACTERS + WIDE_PLACEHOLDER + ALL_CHARS_PLACEHOLDER;

    private final @NotNull String _name;
    private final String _parentContainerPath;
    private final @NotNull String _containerPath;
    private final String _folderType;
    private final @NotNull List<String> _enableModules;

    protected ContainerInfo(String name, ContainerInfo parentContainer, String folderType, List<String> enableModules)
    {
        _parentContainerPath = parentContainer == null ? null : parentContainer.getContainerPath();
        _name = Objects.requireNonNull(containerPath(name));
        _containerPath = containerPath(_parentContainerPath, name);
        _folderType = folderType;
        _enableModules = enableModules == null || enableModules.isEmpty() ? Collections.emptyList() : List.copyOf(enableModules);
    }

    private static @NotNull String getRandomName(String folderName)
    {
        if (TestProperties.isTestRunningOnTeamCity())
        {
            String name = TestDataGenerator.randomName(folderName, TestDataGenerator.randomInt(0, 5), 5, RANDOM_CHARSET, null).name();
            if (name.startsWith("@"))
            {
                // Folder name may not begin with '@'
                String replacement = TestDataGenerator.randomString(1, "@", RANDOM_CHARSET);
                name = name.replaceFirst("@", Matcher.quoteReplacement(replacement));
            }
            return name;
        }
        else // Don't clutter dev machines with random project names
            return folderName + TRICKY_CHARACTERS;
    }

    public static ContainerInfo folder(String folderName, ContainerInfo parentContainer, String folderType, List<String> enableModules)
    {
        return new ContainerInfo(getRandomName(folderName), parentContainer, folderType, enableModules);
    }

    public static ContainerInfo folder(String folderName, ContainerInfo parentContainer, String folderType)
    {
        return folder(folderName, parentContainer, folderType, null);
    }

    public static ContainerInfo folder(String folderName, ContainerInfo parentContainer)
    {
        return folder(folderName, parentContainer, null, null);
    }

    public static ContainerInfo project(String projectName, String folderType, List<String> enableModules)
    {
        return folder(projectName, null, folderType, enableModules);
    }

    public static ContainerInfo project(String projectName, String folderType)
    {
        return project(projectName, folderType, null);
    }

    public static ContainerInfo project(String projectName)
    {
        return project(projectName, null, null);
    }

    public void create(AbstractContainerHelper containerHelper)
    {
        create(containerHelper, _folderType);
    }

    public void create(AbstractContainerHelper containerHelper, String folderType)
    {
        if (isProject())
            containerHelper.createProject(_name, folderType);
        else
            containerHelper.createSubfolder(_parentContainerPath, _name, folderType);
        if (!_enableModules.isEmpty())
        {
            containerHelper.enableModules(_containerPath, _enableModules);
        }
    }

    public @NotNull String getName()
    {
        return _name;
    }

    public @NotNull String getContainerPath()
    {
        return _containerPath;
    }

    public boolean isProject()
    {
        return _parentContainerPath == null;
    }

}
