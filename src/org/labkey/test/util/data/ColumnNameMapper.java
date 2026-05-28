/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
package org.labkey.test.util.data;

import org.labkey.test.params.FieldDefinition;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ColumnNameMapper implements Function<String, String>
{
    private final Function<String, String> _nameMapper;
    private final Map<String, String> _mappingOverrides = new HashMap<>();

    public ColumnNameMapper(Function<String, String> nameMapper)
    {
        _nameMapper = nameMapper;
    }

    public ColumnNameMapper(Map<String, String> mappings)
    {
        this(Function.identity());
        _mappingOverrides.putAll(mappings);
    }

    public static ColumnNameMapper labelToName(Collection<FieldDefinition> fields)
    {
        Map<String, String> labelToNameMap = fields.stream().collect(Collectors.toMap(FieldDefinition::getEffectiveLabel, FieldDefinition::getName));
        return new ColumnNameMapper(label -> labelToNameMap.getOrDefault(label, label));
    }

    public static ColumnNameMapper nameToLabel(Collection<FieldDefinition> fields)
    {
        Map<String, String> map = fields.stream().collect(Collectors.toMap(FieldDefinition::getName, FieldDefinition::getEffectiveLabel));
        return new ColumnNameMapper(name -> Objects.requireNonNullElseGet(map.get(name), () -> FieldDefinition.labelFromName(name)));
    }

    public ColumnNameMapper addMapping(String from, String to)
    {
        _mappingOverrides.put(from, to);
        return this;
    }

    @Override
    public String apply(String s)
    {
        List<Supplier<String>> options = List.of(
            () -> _mappingOverrides.get(s),
            () -> _nameMapper.apply(s));
        return options.stream()
            .map(Supplier::get)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No column mapping found for " + s));
    }
}
