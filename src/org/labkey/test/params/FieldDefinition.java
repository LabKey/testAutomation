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

public class FieldDefinition
{
    private String _name;
    private String _label;
    private ColumnType _type;
    private String _description;
    private String _format;
    private boolean _mvEnabled;
    private boolean _required;
    private LookupInfo _lookup;
    private FieldValidator _validator;
    private String _url;
    private Integer _scale;

    public FieldDefinition(String name)
    {
        _name = name;
    }

    public String getName()
    {
        return _name;
    }

    public FieldDefinition setName(String name)
    {
        _name = name;
        return this;
    }

    public String getLabel()
    {
        return _label;
    }

    public FieldDefinition setLabel(String label)
    {
        _label = label;
        return this;
    }

    public ColumnType getType()
    {
        return _type;
    }

    public FieldDefinition setType(ColumnType type)
    {
        _type = type;
        return this;
    }

    public String getDescription()
    {
        return _description;
    }

    public FieldDefinition setDescription(String description)
    {
        _description = description;
        return this;
    }

    public String getFormat()
    {
        return _format;
    }

    public FieldDefinition setFormat(String format)
    {
        _format = format;
        return this;
    }

    public boolean isMvEnabled()
    {
        return _mvEnabled;
    }

    public FieldDefinition setMvEnabled(boolean mvEnabled)
    {
        _mvEnabled = mvEnabled;
        return this;
    }

    public boolean isRequired()
    {
        return _required;
    }

    public FieldDefinition setRequired(boolean required)
    {
        _required = required;
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
        return this;
    }

    public Integer getScale()
    {
        return _scale;
    }

    public FieldDefinition setScale(Integer scale)
    {
        _scale = scale;
        return this;
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
        MultiLine("Multi-Line Text"), Integer("Integer"), String("Text (String)"), Subject("Subject/Participant (String)"), DateTime("DateTime"), Boolean("Boolean"),
        Double("Number (Double)"), File("File"), AutoInteger("Auto-Increment Integer"), Flag("Flag (String)"), Attachment("Attachment"), User("User"), Lookup("Lookup");

        private final String _description;

        ColumnType(String description)
        {
            _description = description;
        }

        public String toString()
        {
            return _description;
        }
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
