/*
 * Copyright (c) 2013-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test;

import org.labkey.test.util.ext4cmp.Ext4FieldRef;

public class ModulePropertyValue
{
    private String _moduleName;
    private String _containerPath;
    private String _propertyName;
    private String _value;
    private InputType _inputType;

    public enum InputType
    {
        display("displayfield"),
        text("textfield"),
        select("combo")
                {
                    @Override
                    public boolean isValid(Ext4FieldRef field)
                    {
                        return !((boolean) field.getEval("editable"));
                    }
                },
        combo("combo")
                {
                    @Override
                    public boolean isValid(Ext4FieldRef field)
                    {
                        return ((boolean) field.getEval("editable"));
                    }
                },
        checkbox("checkbox")
                {
                    @Override
                    public String valueToString(Object value)
                    {
                        return value.toString();
                    }
                };

        String _xtype;

        InputType(String xtype)
        {
            _xtype = xtype;
        }

        public String getXtype()
        {
            return _xtype;
        }

        public boolean isValid(Ext4FieldRef field)
        {
            return true;
        }

        public String valueToString(Object value)
        {
            return (String)value;
        }
    }

    public ModulePropertyValue(String moduleName, String containerPath, String propertyName, String value, InputType inputType)
    {
        _moduleName = moduleName;
        _containerPath = containerPath;
        if (!_containerPath.startsWith("/"))
            _containerPath = "/" + _containerPath;
        _propertyName = propertyName;
        _value = value;
        _inputType = inputType;
    }

    public ModulePropertyValue(String moduleName, String containerPath, String propertyName, String value)
    {
        this(moduleName, containerPath, propertyName, value, InputType.text);
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

    public InputType getInputType()
    {
        return _inputType;
    }
}
