package org.labkey.test.util;

/**
 * User: cnathe
 * Date: 10/24/13
 */
public class FileBrowserExtendedProperty
{
    private String _name;
    private String _value;
    private boolean _isCombobox;

    public FileBrowserExtendedProperty(String name, String value, boolean isCombobox)
    {
        _name = name;
        _value = value;
        _isCombobox = isCombobox;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getValue()
    {
        return _value;
    }

    public void setValue(String value)
    {
        _value = value;
    }

    public boolean isCombobox()
    {
        return _isCombobox;
    }

    public void setCombobox(boolean combobox)
    {
        _isCombobox = combobox;
    }
}
