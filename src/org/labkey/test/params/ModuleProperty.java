package org.labkey.test.params;

public class ModuleProperty
{
    private final String _moduleName;
    private final String _containerPath;
    private final String _propertyName;
    private final Object _value;
    private final String _propertLabel;

    public ModuleProperty(String moduleName, String containerPath, String propertyName, Object value)
    {
        this(moduleName, containerPath, propertyName, null, value);
    }

    public ModuleProperty(String moduleName, String containerPath, String propertyName, String propertyLabel, Object value)
    {
        _moduleName = moduleName;
        if (!containerPath.startsWith("/"))
            _containerPath = "/" + containerPath;
        else
            _containerPath = containerPath;
        _propertyName = propertyName;
        _value = value;

        // If no label is provided use the name.
        _propertLabel = (null == propertyLabel) || (propertyLabel.isEmpty()) ? propertyName : propertyLabel;
    }

    public String getModuleName()
    {
        return _moduleName;
    }

    public String getContainerPath()
    {
        return _containerPath;
    }

    public String getPropertyName()
    {
        return _propertyName;
    }

    public String getPropertyLabel()
    {
        return _propertLabel;
    }

    public Object getValue()
    {
        return _value;
    }
}
