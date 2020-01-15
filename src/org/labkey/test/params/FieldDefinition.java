/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
import org.labkey.remoteapi.query.Filter;

import java.util.Map;

public class FieldDefinition
{
    private final String _name;
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
    private Boolean _hidden;
    private Boolean _shownInDetailsView;
    private Boolean _shownInInsertView;
    private Boolean _shownInUpdateView;
    private Boolean _isPrimaryKey;

    public FieldDefinition(String name, ColumnType type)
    {
        _name = name;
        _type = type;
    }

    public FieldDefinition(String name)
    {
        this(name, ColumnType.String);
    }

    public FieldDefinition(String name, LookupInfo lookup)
    {
        _name = name;
        _lookup = lookup;
    }

    public String getName()
    {
        return _name;
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

    public Boolean isHidden()
    {
        return _hidden;
    }

    public FieldDefinition setHidden(Boolean hidden)
    {
        _hidden = hidden;
        return this;
    }

    public Boolean isPrimaryKey()
    {
        return  _isPrimaryKey;
    }

    public FieldDefinition setPrimaryKey(Boolean isPrimaryKey)
    {
        _isPrimaryKey = isPrimaryKey;
        return this;
    }

    public Boolean getShownInDetailsView()
    {
        return _shownInDetailsView;
    }

    public FieldDefinition setShownInDetailsView(Boolean shownInDetailsView)
    {
        _shownInDetailsView = shownInDetailsView;
        return this;
    }

    public Boolean getShownInInsertView()
    {
        return _shownInInsertView;
    }

    public FieldDefinition setShownInInsertView(Boolean shownInInsertView)
    {
        _shownInInsertView = shownInInsertView;
        return this;
    }

    public Boolean getShownInUpdateView()
    {
        return _shownInUpdateView;
    }

    public FieldDefinition setShownInUpdateView(Boolean shownInUpdateView)
    {
        _shownInUpdateView = shownInUpdateView;
        return this;
    }

    public FieldDefinition setScale(Integer scale)
    {
        _scale = scale;
        return this;
    }

    public Map<String, Object> toMap()
    {
        CaseInsensitiveHashMap<Object> map = new CaseInsensitiveHashMap<>();

        map.put("name", getName());
        if (getLabel() != null)
            map.put("label", getLabel());

        if (getLookup() != null)
        {
            map.put("lookupSchema", getLookup().getSchema());
            map.put("lookupQuery", getLookup().getTable());
            map.put("lookupContainer", getLookup().getFolder());
            map.put("rangeURI", getLookup().getTableType());
        }
        else if (getType() != null)
        {
            if (getType().getJsonType() == null)
                throw new IllegalArgumentException("`FieldDefinition.toMap()` does not currently support column type: " + getType().name());
            map.put("rangeURI", getType().getJsonType());
        }
        if (getDescription() != null)
            map.put("description", getDescription());
        if (getFormat() != null)
            map.put("format", getFormat());
        map.put("mvEnabled", isMvEnabled());
        map.put("required", isRequired());
        if (getScale() != null)
            map.put("scale", getScale());
        if (isHidden() != null)
            map.put("hidden", isHidden());
        if (isPrimaryKey() != null)
            map.put("isPrimaryKey", isPrimaryKey());
        if (getShownInDetailsView() != null)
            map.put("shownInDetailsView", getShownInDetailsView());
        if (getShownInInsertView() != null)
            map.put("shownInInsertView", getShownInInsertView());
        if (getShownInUpdateView() != null)
            map.put("shownInUpdateView", getShownInUpdateView());

        return map;
    }

    public enum RangeType
    {
        Equals("Equals", Filter.Operator.EQUAL),
        NE("Does Not Equal", Filter.Operator.NEQ),
        GT("Greater than", Filter.Operator.GT),
        GTE("Greater than or Equals", Filter.Operator.GTE),
        LT("Less than", Filter.Operator.LT),
        LTE("Less than or Equals", Filter.Operator.LTE);
        private final String _description;
        private final Filter.Operator _operator;

        RangeType(String description, Filter.Operator operator)
        {
            _description = description;
            _operator = operator;
        }

        public String toString()
        {
            return _description;
        }

        public Filter.Operator getOperator()
        {
            return _operator;
        }
    }

    public enum ColumnType
    {
        MultiLine("Multi-Line Text", "Multi-Line Text", "string"),
        Integer("Integer", "Integer", "int"),
        string("Text", "Text (String)", "string"),
        String("Text", "Text (String)", "String"), // need upper case String for lookup tableType text
        Subject("Subject/Participant", "Subject/Participant (String)", "string"),
        DateTime("DateTime", "DateTime", "date"), // TODO remove this after GWT designer removed
        DateAndTime("Date Time", "Date Time", "date"),
        Boolean("Boolean", "Boolean", "boolean"),
        Double("Number (Double)", "Number (Double)", "float"), // TODO remove this after GWT designer removed
        Decimal("Decimal", "Decimal", "float"),
        File("File", "File", null),
        AutoInteger("Auto-Increment Integer", "Auto-Increment Integer", "int"),
        Flag("Flag", "Flag (String)", null),
        Attachment("Attachment", "Attachment", "attachment"),
        User("User", "User", "int"),
        Lookup("Lookup", "Lookup", null),
        Sample("Sample", "Sample", null);

        private final String _label; // the display value in the UI for this kind of field
        private final String _description; // TODO remove this after GWT designer removed
        private final String _jsonType;     // the key used inside the API

        ColumnType(String label, String description, String jsonType)
        {
            _label = label;
            _description = description;
            _jsonType = jsonType;
        }

        public String toString()
        {
            return _description;
        }

        public String getLabel()
        {
            return _label;
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

        @Deprecated
        public LookupInfo setTableType(String tableType)
        {
            _tableType = tableType;
            return this;
        }
        public LookupInfo setTableType(ColumnType tableType)
        {
            _tableType = tableType._jsonType;
            return this;
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
