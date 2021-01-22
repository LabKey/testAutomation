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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.query.Filter;
import org.labkey.test.components.html.OptionSelect;

import java.util.List;

public class FieldDefinition extends PropertyDescriptor
{
    // for UI helpers
    private ColumnType _type;
    private LookupInfo _lookup;

    // Field properties not supported by PropertyDescriptor
    private Integer _scale;
    private Boolean _shownInDetailsView;
    private Boolean _shownInInsertView;
    private Boolean _shownInUpdateView;
    private Boolean _isPrimaryKey;
    private Boolean _lookupValidatorEnabled;
    private String _url;
    private List<FieldValidator<?>> _validators;
    private String _importAliases;

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
        if (getURL() != null)
            json.put("url", getURL());
        if (getImportAliases() != null)
            json.put("importAliases", getImportAliases());
        if (getValidators() != null)
        {
            JSONArray propertyValidators = new JSONArray();
            getValidators().stream().map(FieldValidator::toJSONObject).forEachOrdered(propertyValidators::add);
            json.put("propertyValidators", propertyValidators);
        }

        return json;
    }

    @Override
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

    public List<FieldValidator<?>> getValidators()
    {
        return _validators;
    }

    public FieldDefinition setValidators(List<FieldValidator<?>> validators)
    {
        _validators = validators;
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

    public String getImportAliases()
    {
        return _importAliases;
    }

    public FieldDefinition setImportAliases(String importAliases)
    {
        _importAliases = importAliases;
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
        MultiLine("Multi-Line Text", "multiLine"),
        Integer("Integer", "int"),
        LongInteger("Long Integer", "long"),
        String("Text", "string"),
        Subject("Subject/Participant", "string", "http://cpas.labkey.com/Study#ParticipantId", null),
        DateAndTime("Date Time", "dateTime"),
        Boolean("Boolean", "boolean"),
        Float("Float", "float"),
        Double("Decimal (floating point)", "double"),
        @Deprecated(since = "21.2")
        Decimal("Decimal (floating point)", "double"),
        // Decimal is incorrectly implemented but too widely used for a quick fix.
        // Existing usages should be switched to use 'ColumnType.Double'
        // Then the following 'Decimal' implementation should replace the existing one.
        // Decimal("Decimal (fixed point)", "decimal"),
        File("File", "fileLink"),
        Flag("Flag", "string", "http://www.labkey.org/exp/xml#flag", null),
        Attachment("Attachment", "attachment"),
        User("User", "int", null, new LookupInfo(null, "core", "users")),
        Lookup("Lookup", null),
        Sample("Sample", "int", "http://www.labkey.org/exp/xml#sample", new LookupInfo(null, "exp", "Materials"));

        private final String _label; // the display value in the UI for this kind of field
        private final String _rangeURI;     // the key used inside the API
        private final String _conceptURI;
        private final LookupInfo _lookupInfo;

        ColumnType(String label, String rangeURI, String conceptURI, LookupInfo lookupInfo)
        {
            _label = label;
            _rangeURI = rangeURI;
            _conceptURI = conceptURI;
            _lookupInfo = lookupInfo;
        }

        ColumnType(String label, String rangeURI)
        {
            this(label, rangeURI, null, null);
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

    public enum ScaleType implements OptionSelect.SelectOption
    {
        LINEAR("Linear"),
        LOG("Log");

        String _text;

        ScaleType(String text)
        {
            _text = text;
        }

        @Override
        public String getValue()
        {
            return name();
        }

        @Override
        public String getText()
        {
            return _text;
        }
    }

    public enum DefaultType implements OptionSelect.SelectOption
    {
        FIXED_EDITABLE("Editable default"),
        LAST_ENTERED("Last entered"),
        FIXED_NON_EDITABLE("Fixed value");

        String _text;

        DefaultType(String text)
        {
            _text = text;
        }

        @Override
        public String getValue()
        {
            return name();
        }

        @Override
        public String getText()
        {
            return _text;
        }
    }

    /**
     * Represents possible PHI levels for a field
     *
     * @see org.labkey.api.data.PHI
     */
    public enum PhiSelectType implements OptionSelect.SelectOption
    {
        // Ordered from least to most restrictive
        NotPHI("Not PHI", null),
        Limited("Limited PHI", "Limited PHI Reader"),
        PHI("Full PHI", "Full PHI Reader"),
        Restricted("Restricted PHI", "Restricted PHI Reader");

        private final String _text;
        private final String _roleName;

        PhiSelectType(String text, String roleName)
        {
            _text = text;
            _roleName = roleName;
        }

        @Override
        public String getValue()
        {
            return name();
        }

        @Override
        public String getText()
        {
            return _text;
        }

        public int getRank()
        {
            return ordinal();
        }

        public String getRoleName()
        {
            return _roleName;
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

    public static abstract class FieldValidator<V extends FieldValidator<V>>
    {
        private String _name;
        private String _description;
        private String _message;

        private boolean _failOnMatch = false;

        protected FieldValidator(String name, String description, String message)
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

        public V setFailOnMatch(boolean failOnMatch)
        {
            _failOnMatch = failOnMatch;
            return getThis();
        }

        protected abstract V getThis();

        protected abstract String getExpression();

        protected abstract String getType();

        protected JSONObject getProperties()
        {
            JSONObject json = new JSONObject();
            json.put("failOnMatch", _failOnMatch);
            return json;
        }

        /**
         * JSON for a field validator looks something like this:
         * <pre>
         * {
         *   "description": "description",
         *   "errorMessage": "error message",
         *   "expression": "~gt=34&~lt=99",
         *   "name": "V range 1",
         *   "new": true,
         *   "properties": {
         *     "failOnMatch": false
         *   },
         *   "type": "Range"
         * }
         * </pre>
         * @return
         */
        protected JSONObject toJSONObject()
        {
            JSONObject json = new JSONObject();
            json.put("name", _name);
            if (_description != null)
            {
                json.put("description", _description);
            }
            if (_message != null)
            {
                json.put("errorMessage", _message);
            }
            json.put("new", true);
            json.put("properties", getProperties());
            json.put("type", getType());

            return json;
        }
    }

    public static class RegExValidator extends FieldValidator<RegExValidator>
    {
        private String _expression;

        public RegExValidator(String name, String description, String message, String expression)
        {
            super(name, description, message);
            _expression = expression;
        }

        @Override
        protected RegExValidator getThis()
        {
            return this;
        }

        @Override
        protected String getType()
        {
            return "RegEx";
        }

        @Override
        public String getExpression()
        {
            return _expression;
        }
    }

    public static class RangeValidator extends FieldValidator<RangeValidator>
    {
        private RangeType _firstType;
        private String _firstRange;
        private RangeType _secondType;
        private String _secondRange;

        public RangeValidator(String name, String description, String message, RangeType firstType, String firstRange)
        {
            this(name, description, message, firstType, firstRange, null, null);
        }

        public RangeValidator(String name, String description, String message, RangeType firstType, String firstRange, RangeType secondType, String secondRange)
        {
            super(name, description, message);
            _firstType = firstType;
            _firstRange = firstRange;
            _secondType = secondType;
            _secondRange = secondRange;
        }

        @Override
        protected RangeValidator getThis()
        {
            return this;
        }

        @Override
        protected String getType()
        {
            return "Range";
        }

        @Override
        protected String getExpression()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("~");
            sb.append(_firstType.getOperator().getUrlKey());
            if (_firstRange != null)
            {
                sb.append("=");
                sb.append(_firstRange);
            }
            if (_secondType != null)
            {
                sb.append("&");
                sb.append("~");
                sb.append(_secondType.getOperator().getUrlKey());
                if (_secondRange != null)
                {
                    sb.append("=");
                    sb.append(_secondRange);
                }
            }
            return sb.toString();
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
