/*
 * Copyright (c) 2021-2026 LabKey Corporation
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
