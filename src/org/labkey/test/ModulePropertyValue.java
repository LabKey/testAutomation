package org.labkey.test;

/**
 * User: jeckels
 * Date: 2/8/13
 */
public class ModulePropertyValue
{
    private String _moduleName;
    private String _containerPath;
    private String _propertyName;
    private String _value;

    public ModulePropertyValue(String moduleName, String containerPath, String propertyName, String value)
    {
        _moduleName = moduleName;
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

    public String getValue()
    {
        return _value;
    }
}
