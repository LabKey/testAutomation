/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.test.util;

import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.params.FieldDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestDataValidator
{

    private FieldDefinition.LookupInfo _lookupInfo;

    private Map<String, FieldDefinition> _columns = new CaseInsensitiveHashMap<>();
    private List<Map<String, Object>> _rows = new ArrayList<>();

    public TestDataValidator(FieldDefinition.LookupInfo lookupInfo,
                             Map<String, FieldDefinition> columns, List<Map<String, Object>> rows)
    {
        _lookupInfo = lookupInfo;
        _columns = columns;
        _rows = rows;
    }

    public List<Map<String, Object>> getRows()
    {
        return _rows;
    }

    public String enumerateMissingRows(List<Map<String, String>> rowMaps)
    {
        return enumerateMissingRows(rowMaps, null);
    }

    public String enumerateMissingRows(List<Map<String, String>> rowMaps, List<String> ignoreColumns)
    {
        StringBuilder error = new StringBuilder();

        for (Map row : rowMaps) // iterate over the passed in rows
        {
            // now find at least 1 row with matching value in this column
            List<Map<String, Object>> matchingRows = findRowsWithMatchingColumnValues(row, ignoreColumns);

            if (matchingRows.size()==0)
            {
                for (Object key : row.keySet())
                {
                    error.append("Did not find row matching expected parameters:[");
                    error.append("column:["+key+"], value["+ row.get(key) +"]");
                    error.append("]\n");
                }
            }
        }
        return error.toString();
    }

    private String getCaseInvariantKey(String matchingKey, Map<String, Object> row)
    {
        return row.keySet().stream().filter(a-> a.toLowerCase().equals(matchingKey.toLowerCase()))
                .findFirst().get();
    }

    /**
        @param toValidate : a map of column/value pairs to match
        @return : a list of rows from this object's generated data with matching column/values
    * */
    public List<Map<String, Object>> findRowsWithMatchingColumnValues(Map<String, Object> toValidate, List<String> columnsToIgnore)
    {
        List<Map<String, Object>> filteringList = new ArrayList<>();
        filteringList.addAll(getRows());
        List<String> columnNames = getColumnKeys(filteringList.get(0)); // these are the case-sensitive keys to the passed-in row

        for (String columnName : columnNames)
        {
            if (columnsToIgnore!=null && columnsToIgnore.contains(columnName))      // todo; do this case-insensitively
                continue;

            String key = getCaseInvariantKey(columnName, toValidate);     // gets the corresponding case-sensitive key
            String expectedValue = toValidate.get(key).toString();

            filteringList =  filteringList.stream().filter(a -> a.get(columnName).toString().equals(expectedValue))    // filter smaller and smaller sets
                    .collect(Collectors.toList());
        }
        return filteringList;
    }

    public List<String> getColumnKeys(Map<String, Object> row)
    {
        List<String> colNames = row.keySet().stream().collect(Collectors.toList());
        return colNames;
    }
}
