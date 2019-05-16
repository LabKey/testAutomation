/*
 * Copyright (c) 2016-2017 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;

import java.util.Map;

public class FieldDefinition
{
    private String NAME_KEY = "name";
    private String _name;
    private String LABEL_KEY = "label";
    private String _label;
    private ColumnType _type;
    private String TYPE_KEY = "rangeURI";
    private String DESCRIPTION_KEY="description";
    private String _description;
    private String FORMAT_KEY= "formatString";
    private String _format;
    private String MV_ENABLED_KEY = "mvEnabled";
    private boolean _mvEnabled;
    private String REQUIRED_KEY = "required";
    private boolean _required;
    private LookupInfo _lookup;
    private String LOOKUP_KEY;
    private FieldValidator _validator;
    private String URL_KEY;
    private String _url;
    private String SCALE_KEY = "scale";
    private Integer _scale;

    private CaseInsensitiveHashMap _map = new CaseInsensitiveHashMap();

    public FieldDefinition(String name)
    {
        setName(name);
    }

    public String getName()
    {
        return _name;
    }

    public FieldDefinition setName(String name)
    {
        _name = name;
        replaceInMap(NAME_KEY, name);
        return this;
    }

    public String getLabel()
    {
        return _label;
    }

    public FieldDefinition setLabel(String label)
    {
        _label = label;
        replaceInMap(LABEL_KEY, label);
        return this;
    }

    public ColumnType getType()
    {
        return _type;
    }

    public FieldDefinition setType(ColumnType type)
    {
        _type = type;
        replaceInMap(TYPE_KEY, type.getJsonType());
        return this;
    }

    public String getDescription()
    {
        return _description;
    }

    public FieldDefinition setDescription(String description)
    {
        _description = description;
        replaceInMap(DESCRIPTION_KEY, description);
        return this;
    }

    public String getFormat()
    {
        return _format;
    }

    public FieldDefinition setFormat(String format)
    {
        _format = format;
        replaceInMap(FORMAT_KEY, format);
        return this;
    }

    public boolean isMvEnabled()
    {
        return _mvEnabled;
    }

    public FieldDefinition setMvEnabled(boolean mvEnabled)
    {
        _mvEnabled = mvEnabled;
        replaceInMap(MV_ENABLED_KEY, mvEnabled);
        return this;
    }

    public boolean isRequired()
    {
        return _required;
    }

    public FieldDefinition setRequired(boolean required)
    {
        _required = required;
        _map.put(REQUIRED_KEY, required);
        return this;
    }

    public LookupInfo getLookup()
    {
        return _lookup;
    }

    public FieldDefinition setLookup(LookupInfo lookup)
    {
        _lookup = lookup;
        return this;
    }

    public FieldValidator getValidator()
    {
        return _validator;
    }

    public FieldDefinition setValidator(FieldValidator validator)
    {
        _validator = validator;
        return this;
    }

    public String getURL()
    {
        return _url;
    }

    public FieldDefinition setURL(String url)
    {
        _url = url;
        replaceInMap(URL_KEY, url);
        return this;
    }

    public Integer getScale()
    {
        return _scale;
    }

    public FieldDefinition setScale(Integer scale)
    {
        _scale = scale;
        replaceInMap(SCALE_KEY, scale);
        return this;
    }

    public Map<String, Object> toMap()
    {
        return _map;
    }

    private void replaceInMap(String key, Object value)
    {
        if (_map.get(key) != null)
            _map.remove(key);
        _map.put(key, value);
    }

    public enum RangeType
    {
        Equals("Equals"), NE("Does Not Equal"), GT("Greater than"), GTE("Greater than or Equals"), LT("Less than"), LTE("Less than or Equals");
        private final String _description;

        RangeType(String description)
        {
            _description = description;
        }

        public String toString()
        {
            return _description;
        }
    }

    public enum ColumnType
    {
        MultiLine("Multi-Line Text", "string"), Integer("Integer", "int"),
        String("Text (String)", "string"), Subject("Subject/Participant (String)", "string"),
        DateTime("DateTime", "date"), Boolean("Boolean", "boolean"),
        Double("Number (Double)", "float"), File("File", null),
        AutoInteger("Auto-Increment Integer", "int"),
        Flag("Flag (String)", null), Attachment("Attachment", null),
        User("User", "int"), Lookup("Lookup", null);

        private final String _description;  // the display value in the UI for this kind of field
        private final String _jsonType;     // the key used inside the API

        ColumnType(String description, String jsonType)
        {
            _description = description;
            _jsonType = jsonType;
        }

        public String toString()
        {
            return _description;
        }
        public String getJsonType() { return _jsonType; }
    }

    public static class LookupInfo
    {
        private String _folder;
        private String _schema;
        private String _table;
        private String _tableType;

        public LookupInfo(@Nullable String folder, String schema, String table)
        {
            _folder = ("".equals(folder) ? null : folder);
            //container must exactly match an item in the dropdown
            if(_folder != null && !_folder.startsWith("/"))
                _folder = "/" + _folder;

            _schema = ("".equals(schema) ? null : schema);
            _table = ("".equals(table) ? null : table);
        }

        public String getFolder()
        {
            return _folder;
        }

        public String getSchema()
        {
            return _schema;
        }

        public String getTable()
        {
            return _table;
        }

        public String getTableType()
        {
            return _tableType;
        }

        public void setTableType(String tableType)
        {
            _tableType = tableType;
        }

    }

    public static abstract class FieldValidator
    {
        private String _name;
        private String _description;
        private String _message;

        public FieldValidator()
        {

        }
        public FieldValidator(String name, String description, String message)
        {
            _name = name;
            _description = description;
            _message = message;
        }

        public String getName()
        {
            return _name;
        }

        public String getDescription()
        {
            return _description;
        }

        public String getMessage()
        {
            return _message;
        }
    }

    public static class RegExValidator extends FieldValidator
    {
        private String _expression;

        public RegExValidator(String name, String description, String message, String expression)
        {
            super(name, description, message);
            _expression = expression;
        }

        public String getExpression()
        {
            return _expression;
        }
    }

    public static class LookUpValidator extends FieldValidator
    {
        private ColumnType _colType;

        public LookUpValidator()
        {
            super("Lookup validator", null, null);
            _colType = ColumnType.Lookup;
        }

        public ColumnType getColType()
        {
            return _colType;
        }
    }

    public static class RangeValidator extends FieldValidator
    {
        private RangeType _firstType;
        private String _firstRange;
        private RangeType _secondType;
        private String _secondRange;

        public RangeValidator(String name, String description, String message, RangeType firstType, String firstRange)
        {
            super(name, description, message);
            _firstType = firstType;
            _firstRange = firstRange;
        }

        public RangeValidator(String name, String description, String message, RangeType firstType, String firstRange, RangeType secondType, String secondRange)
        {
            this(name, description, message, firstType, firstRange);
            _secondType = secondType;
            _secondRange = secondRange;
        }

        public RangeType getFirstType()
        {
            return _firstType;
        }

        public String getFirstRange()
        {
            return _firstRange;
        }

        public RangeType getSecondType()
        {
            return _secondType;
        }

        public String getSecondRange()
        {
            return _secondRange;
        }
    }

}
