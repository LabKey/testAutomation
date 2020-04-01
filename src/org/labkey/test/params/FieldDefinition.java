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
import org.json.simple.JSONObject;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.query.Filter;

public class FieldDefinition extends PropertyDescriptor
{
    // for UI helpers
    private ColumnType _type;
    private LookupInfo _lookup;

    // UI Only field properties
    private FieldValidator _validator;
    private String _url;

    // Field properties not supported by PropertyDescriptor
    private Integer _scale;
    private Boolean _shownInDetailsView;
    private Boolean _shownInInsertView;
    private Boolean _shownInUpdateView;
    private Boolean _isPrimaryKey;
    private Boolean _lookupValidatorEnabled;

    public FieldDefinition(String name, ColumnType type)
    {
        super(name, type.getRangeURI());
        setType(type);
    }

    public FieldDefinition(String name)
    {
        this(name, ColumnType.String);
    }

    public FieldDefinition(String name, LookupInfo lookup)
    {
        setName(name);
        setLookup(lookup);
    }

    @Override
    public JSONObject toJSONObject()
    {
        if (getType() != null && getType().getRangeURI() == null)
        {
            throw new IllegalArgumentException("`FieldDefinition` cannot be used to create column over API: " + getType().name());
        }

        JSONObject json = super.toJSONObject();
        if (getScale() != null)
            json.put("scale", getScale());
        if (isPrimaryKey() != null)
            json.put("isPrimaryKey", isPrimaryKey());
        if (getShownInDetailsView() != null)
            json.put("shownInDetailsView", getShownInDetailsView());
        if (getShownInInsertView() != null)
            json.put("shownInInsertView", getShownInInsertView());
        if (getShownInUpdateView() != null)
            json.put("shownInUpdateView", getShownInUpdateView());
        if (getLookupValidatorEnabled() != null)
            json.put("lookupValidatorEnabled", getLookupValidatorEnabled());
        if (getType().getConceptURI() != null)
            json.put("conceptURI", getType().getConceptURI());

        return json;
    }

    public FieldDefinition setLabel(String label)
    {
        super.setLabel(label);
        return this;
    }

    public ColumnType getType()
    {
        return _type;
    }

    public FieldDefinition setType(ColumnType type)
    {
        _type = type;
        if (type.getLookupInfo() != null)
        {
            // Special 'User' and 'Sample' lookups
            super.setLookup(type.getLookupInfo().getSchema(),
                    type.getLookupInfo().getTable(),
                    type.getLookupInfo().getFolder());
        }
        super.setRangeURI(type.getRangeURI());
        return this;
    }

    @Override
    public FieldDefinition setDescription(String description)
    {
        super.setDescription(description);
        return this;
    }

    @Override
    public FieldDefinition setFormat(String format)
    {
        super.setFormat(format);
        return this;
    }

    @Override
    public FieldDefinition setMvEnabled(Boolean mvEnabled)
    {
        super.setMvEnabled(mvEnabled);
        return this;
    }

    @Override
    public FieldDefinition setRequired(Boolean required)
    {
        super.setRequired(required);
        return this;
    }

    public LookupInfo getLookup()
    {
        return _lookup;
    }

    public FieldDefinition setLookup(LookupInfo lookup)
    {
        if (lookup == null)
        {
            super.setLookup(null, null, null);
        }
        else
        {
            super.setLookup(lookup.getSchema(), lookup.getTable(), lookup.getFolder());
            setRangeURI(lookup.getTableType().getRangeURI());
        }
        _lookup = lookup;
        return this;
    }

    @Override
    public PropertyDescriptor setLookup(String schema, String query, String container)
    {
        return setLookup(new LookupInfo(container, schema, query));
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

    @Override
    public FieldDefinition setHidden(Boolean hidden)
    {
        super.setHidden(hidden);
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

    public Boolean getLookupValidatorEnabled()
    {
        return _lookupValidatorEnabled;
    }

    public FieldDefinition setLookupValidatorEnabled(Boolean lookupValidatorEnabled)
    {
        _lookupValidatorEnabled = lookupValidatorEnabled;
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
        String("Text", "Text (String)", "string"),
        Subject("Subject/Participant", "Subject/Participant (String)",
                "string", "http://cpas.labkey.com/Study#ParticipantId", null),
        DateTime("DateTime", "DateTime", "date"), // TODO remove this after GWT designer removed
        DateAndTime("Date Time", "Date Time", "date"),
        Boolean("Boolean", "Boolean", "boolean"),
        Double("Number (Double)", "Number (Double)", "float"), // TODO remove this after GWT designer removed
        Decimal("Decimal", "Decimal", "float"),
        File("File", "File", "fileLink"),
        AutoInteger("Auto-Increment Integer", "Auto-Increment Integer", "int"),
        Flag("Flag", "Flag (String)", "string",
                "http://www.labkey.org/exp/xml#flag", null),
        Attachment("Attachment", "Attachment", "attachment"),
        User("User", "User", "int", null,
                new LookupInfo(null, "core", "users")),
        Lookup("Lookup", "Lookup", null),
        Sample("Sample", "Sample", "int",
                "http://www.labkey.org/exp/xml#sample",
                new LookupInfo(null, "exp", "Materials"));

        private final String _label; // the display value in the UI for this kind of field
        private final String _description; // TODO remove this after GWT designer removed
        private final String _rangeURI;     // the key used inside the API
        private final String _conceptURI;
        private final LookupInfo _lookupInfo;

        ColumnType(String label, String description, String rangeURI, String conceptURI, LookupInfo lookupInfo)
        {
            _label = label;
            _description = description;
            _rangeURI = rangeURI;
            _conceptURI = conceptURI;
            _lookupInfo = lookupInfo;
        }

        ColumnType(String label, String description, String rangeURI)
        {
            this(label, description, rangeURI, null, null);
        }

        public String toString()
        {
            return _description;
        }

        public String getLabel()
        {
            return _label;
        }

        public String getRangeURI() { return _rangeURI; }

        protected String getConceptURI()
        {
            return _conceptURI;
        }

        protected LookupInfo getLookupInfo()
        {
            return _lookupInfo;
        }
    }

    public static class LookupInfo
    {
        private String _folder;
        private String _schema;
        private String _table;
        private ColumnType _tableType;

        public LookupInfo(@Nullable String folder, String schema, String table)
        {
            _folder = ("".equals(folder) ? null : folder);
            //container must exactly match an item in the dropdown
            if(_folder != null && !_folder.startsWith("/"))
                _folder = "/" + _folder;

            _schema = ("".equals(schema) ? null : schema);
            _table = ("".equals(table) ? null : table);
            setTableType(ColumnType.String);
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

        public ColumnType getTableType()
        {
            return _tableType;
        }

        @Deprecated (forRemoval = true)
        public LookupInfo setTableType(String tableType)
        {
            _tableType = "int".equals(tableType) ? ColumnType.Integer : ColumnType.String;
            return this;
        }
        public LookupInfo setTableType(ColumnType tableType)
        {
            _tableType = tableType;
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
