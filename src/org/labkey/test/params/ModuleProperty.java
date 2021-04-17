package org.labkey.test.params;

public class ModuleProperty
{
    private final String _moduleName;
    private final String _containerPath;
    private final String _propertyName;
    private final Object _value;

    public ModuleProperty(String moduleName, String containerPath, String propertyName, Object value)
    {
        _moduleName = moduleName;
        if (!containerPath.startsWith("/"))
            _containerPath = "/" + containerPath;
        else
            _containerPath = containerPath;
        _propertyName = propertyName;
        _value = value;
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

    public Object getValue()
    {
        return _value;
    }
}
