/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
package org.labkey.remoteapi.issues;

import org.json.JSONObject;
import org.labkey.api.collections.CaseInsensitiveHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IssueComment
{
    private final String CREATED_BY_NAME = "createdByName";
    private final String COMMENT = "comment";
    private final String TITLE = "title";
    private final String ATTACHMENTS = "attachments";

    private final Map<String, Object> _properties = new CaseInsensitiveHashMap<>();

    public IssueComment(JSONObject json)
    {
        _properties.putAll(json.toMap());
    }

    public String getCreatedBy()
    {
        return (String)_properties.get(CREATED_BY_NAME);
    }

    public String getComment()
    {
        return (String) _properties.get(COMMENT);
    }

    public String getTitle()
    {
        return (String) _properties.get(TITLE);
    }

    public List<String> getAttachments()
    {
        List<String> attachments= new ArrayList<>();
        if (_properties.get("attachments") != null)
        {
            List<String> attachmentsList = (List<String>) _properties.get(ATTACHMENTS);
            attachments.addAll(attachmentsList);
        }
        return attachments;
    }

    public Map<String, Object> getProperties()
    {
        return _properties;
    }
}
