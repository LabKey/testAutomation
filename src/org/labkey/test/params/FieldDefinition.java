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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldDefinition extends PropertyDescriptor
{
    // for UI helpers
    private ColumnType _type;
    private LookupInfo _lookup;
    private String _principalConceptSearchSourceOntology;
    private String _principalConceptSearchExpression;

    // Stash validator collection to avoid having to convert back from JSON Maps
    private List<FieldValidator<?>> _validators;

    // Collection of JSON properties not explicitly known by 'PropertyDescriptor'
    private final Map<String, Object> _extraFieldProperties = new HashMap<>();

    public FieldDefinition(String name, ColumnType type)
    {
        setName(name);
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
    public Map<String, Object> getAllProperties()
    {
        return _extraFieldProperties;
    }

    public ColumnType getType()
    {
        return _type;
    }

    public FieldDefinition setType(ColumnType type)
    {
        if (type == ColumnType.Lookup)
        {
            throw new IllegalArgumentException("Use 'setLookup' or construct with 'FieldDefinition(String, LookupInfo)' to create lookup fields");
        }

        _type = type;
        if (type.getLookupInfo() != null)
        {
            // Special 'User' and 'Sample' lookups
            super.setLookup(type.getLookupInfo().getSchema(),
                    type.getLookupInfo().getTable(),
                    type.getLookupInfo().getFolder());
        }
        super.setRangeURI(type.getRangeURI());
        setFieldProperty("conceptURI", type.getConceptURI());
        return this;
    }

    // Override return type of PropertyDescriptor setters

    @Override
    public FieldDefinition setLabel(String label)
    {
        super.setLabel(label);
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

    @Override
    public FieldDefinition setHidden(Boolean hidden)
    {
        super.setHidden(hidden);
        return this;
    }

    public LookupInfo getLookup()
    {
        return _lookup;
    }

    public FieldDefinition setLookup(LookupInfo lookup)
    {
        super.setLookup(lookup.getSchema(), lookup.getTable(), lookup.getFolder());
        setRangeURI(lookup.getTableType().getRangeURI());
        _lookup = lookup;
        return this;
    }

    @Override
    public FieldDefinition setLookup(String schema, String query, String container)
    {
        setLookup(new LookupInfo(container, schema, query));
        return this;
    }

    // Additional field properties, not currently supported by 'PropertyDescriptor'

    private Object getFieldProperty(String propertyName)
    {
        return _extraFieldProperties.get(propertyName);
    }

    private void setFieldProperty(String propertyName, Object value)
    {
        _extraFieldProperties.put(propertyName, value);
    }

    public List<FieldValidator<?>> getValidators()
    {
        return _validators;
    }

    public FieldDefinition setValidators(List<FieldValidator<?>> validators)
    {
        JSONArray propertyValidators = null;
        if (validators != null)
        {
            propertyValidators = new JSONArray();
            validators.stream().map(FieldValidator::toJSONObject).forEachOrdered(propertyValidators::add);
        }
        setFieldProperty("propertyValidators", propertyValidators);
        _validators = validators;
        return this;
    }

    public String getURL()
    {
        return (String) getFieldProperty("URL");
    }

    public FieldDefinition setURL(String url)
    {
        setFieldProperty("URL", url);
        return this;
    }

    public String getImportAliases()
    {
        return (String) getFieldProperty("importAliases");
    }

    public FieldDefinition setImportAliases(String importAliases)
    {
        setFieldProperty("importAliases", importAliases);
        return this;
    }

    public Integer getScale()
    {
        return (Integer) getFieldProperty("scale");
    }

    public FieldDefinition setScale(Integer scale)
    {
        setFieldProperty("scale", scale);
        return this;
    }

    public Boolean isPrimaryKey()
    {
        return (Boolean) getFieldProperty("isPrimaryKey");
    }

    public FieldDefinition setPrimaryKey(Boolean isPrimaryKey)
    {
        setFieldProperty("isPrimaryKey", isPrimaryKey);
        return this;
    }

    public Boolean getLookupValidatorEnabled()
    {
        return (Boolean) getFieldProperty("lookupValidatorEnabled");
    }

    public FieldDefinition setLookupValidatorEnabled(Boolean lookupValidatorEnabled)
    {
        setFieldProperty("lookupValidatorEnabled", lookupValidatorEnabled);
        return this;
    }

    public Boolean getShownInDetailsView()
    {
        return (Boolean) getFieldProperty("shownInDetailsView");
    }

    public FieldDefinition setShownInDetailsView(Boolean shownInDetailsView)
    {
        setFieldProperty("shownInDetailsView", shownInDetailsView);
        return this;
    }

    public Boolean getShownInInsertView()
    {
        return (Boolean) getFieldProperty("shownInInsertView");
    }

    public FieldDefinition setShownInInsertView(Boolean shownInInsertView)
    {
        setFieldProperty("shownInInsertView", shownInInsertView);
        return this;
    }

    public Boolean getShownInUpdateView()
    {
        return (Boolean) getFieldProperty("shownInUpdateView");
    }

    public FieldDefinition setShownInUpdateView(Boolean shownInUpdateView)
    {
        setFieldProperty("shownInUpdateView", shownInUpdateView);
        return this;
    }

    public String getSourceOntology()
    {
        return (String) getFieldProperty("sourceOntology");
    }

    public FieldDefinition setSourceOntology(String sourceOntology)
    {
        setFieldProperty("sourceOntology", sourceOntology);
        return this;
    }

    public String getConceptLabelColumn()
    {
        return (String) getFieldProperty("conceptLabelColumn");
    }

    public FieldDefinition setConceptLabelColumn(String conceptLabelColumn)
    {
        setFieldProperty("conceptLabelColumn", conceptLabelColumn);
        return this;
    }

    public String getConceptImportColumn()
    {
        return (String) getFieldProperty("conceptImportColumn");
    }

    public FieldDefinition setConceptImportColumn(String conceptImportColumn)
    {
        setFieldProperty("conceptImportColumn", conceptImportColumn);
        return this;
    }

    public String getPrincipalConceptCode()
    {
        return (String) getFieldProperty("principalConceptCode");
    }

    public FieldDefinition setPrincipalConceptCode(String principalConceptCode)
    {
        setFieldProperty("principalConceptCode", principalConceptCode);
        return this;
    }

    public String getPrincipalConceptSearchSourceOntology()
    {
        return _principalConceptSearchSourceOntology;
    }

    public String getPrincipalConceptSearchExpression()
    {
        return _principalConceptSearchExpression;
    }

    public FieldDefinition setPrincipalConceptSearchExpression(String ontologyName, String searchExpression)
    {
        _principalConceptSearchSourceOntology = ontologyName;
        _principalConceptSearchExpression = searchExpression;
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
        String("Text", "string"),
        Subject("Subject/Participant", "string", "http://cpas.labkey.com/Study#ParticipantId", null),
        DateAndTime("Date Time", "dateTime"),
        Boolean("Boolean", "boolean"),
        Double("Number (Double)", "float"),
        Decimal("Decimal (floating point)", "double"),
        File("File", "fileLink"),
        Flag("Flag", "string", "http://www.labkey.org/exp/xml#flag", null),
        Attachment("Attachment", "attachment"),
        User("User", "int", null, new LookupInfo(null, "core", "users")),
        Lookup("Lookup", null),
        OntologyLookup("Ontology Lookup", "string", "http://www.labkey.org/types#conceptCode", null),
        VisitId("Visit ID","double","http://cpas.labkey.com/Study#VisitId",null),
        VisitDate("Visit Date","dateTime","http://cpas.labkey.com/Study#VisitId",null),
        Sample("Sample", "int", "http://www.labkey.org/exp/xml#sample", new LookupInfo(null, "exp", "Materials")),
        Barcode("Barcode", "string", "http://www.labkey.org/types#storageUniqueId", null);

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
        private final String _folder;
        private final String _schema;
        private final String _table;
        private ColumnType _tableType;

        public LookupInfo(@Nullable String folder, String schema, String table)
        {
            if (folder == null || folder.isEmpty())
            {
                _folder = null;
            }
            else if (!folder.startsWith("/"))
            {
                //container must exactly match an item in the dropdown
                _folder = "/" + folder;
            }
            else
            {
                _folder = folder;
            }

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

        public LookupInfo setTableType(ColumnType tableType)
        {
            _tableType = tableType;
            return this;
        }
    }

    public static abstract class FieldValidator<V extends FieldValidator<V>>
    {
        private final String _name;
        private final String _description;
        private final String _message;

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

        // Even with the <pre> tag the & needs to be escaped for a javadoc compile.
        // "expression": "~gt=34&~lt=99", & -> &#38;
        /**
         * JSON for a field validator looks something like this:
         * <pre>
         * {
         *   "description": "description",
         *   "errorMessage": "error message",
         *   "expression": "~gt=34&#38;~lt=99",
         *   "name": "V range 1",
         *   "new": true,
         *   "properties": {
         *     "failOnMatch": false
         *   },
         *   "type": "Range"
         * }
         * </pre>
         * @return Serializable representation of field validator
         */
        protected JSONObject toJSONObject()
        {
            JSONObject json = new JSONObject();
            json.put("name", _name);
            json.put("expression", getExpression());
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
        private final String _expression;

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
        private final RangeType _firstType;
        private final String _firstRange;
        private final RangeType _secondType;
        private final String _secondRange;

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
