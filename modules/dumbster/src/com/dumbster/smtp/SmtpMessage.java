/*
 * Dumbster - a dummy SMTP server
 * Copyright 2004 Jason Paul Kitchen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dumbster.smtp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container for a complete SMTP message - headers and message body.
 */
public class SmtpMessage {
  /** Headers: Map of List of String hashed on header name. */
  private final Map<String, List<String>> headers;
  private String headerLast;
  /** Message body. */
  private final StringBuffer body;
  private boolean inBody;
  private final Date createdTimestamp = new Date();


  /**
   * Constructor. Initializes headers Map and body buffer.
   */
  public SmtpMessage() {
    headers = new HashMap<>(10);
    body = new StringBuffer();
  }

  /**
   * Update the headers or body depending on the SmtpResponse object and line of input.
   * @param response SmtpResponse object
   * @param params remainder of input line after SMTP command has been removed
   */
  public void store(SmtpResponse response, String params) {
    if (params != null) {
      if (SmtpState.DATA_HDR.equals(response.getNextState())) {
        int headerNameEnd = params.indexOf(':');
        if (headerNameEnd >= 0) {
          String name = params.substring(0, headerNameEnd).trim();
          String value = params.substring(headerNameEnd+1).trim();
          addHeader(name, value);
          headerLast = name;
        } else if (headerLast != null) {
          appendHeader(headerLast, params);
        }
      } else if (SmtpState.DATA_BODY == response.getNextState()) {
        if (inBody)
          body.append("\n");
        inBody = true;
        body.append(params);
      }
    }
  }

  /**
   * Get an Iterator over the header names.
   * @return an Iterator over the set of header names (String)
   */
  public Iterator<String> getHeaderNames() {
    Set<String> nameSet = headers.keySet();
    return nameSet.iterator();
  }

  /**
   * Get the value(s) associated with the given header name.
   * @param name header name
   * @return value(s) associated with the header name
   */
  public String[] getHeaderValues(String name) {
    List<String> values = headers.get(name);
    if (values == null) {
      return new String[0];
    } else {
      return values.toArray(new String[0]);
    }
  }

  /**
   * Get the first values associated with a given header name.
   * @param name header name
   * @return first value associated with the header name
   */
  public String getHeaderValue(String name) {
    List<String> values = headers.get(name);
    if (values == null) {
      return null;
    } else {
      Iterator<String> iterator = values.iterator();
      return iterator.next();
    }
  }

  /**
   * Get the message body.
   * @return message body
   */
  public String getBody() {
    return body.toString();
  }

  /**
   * Adds a header to the Map.
   * @param name header name
   * @param value header value
   */
  private void addHeader(String name, String value) {
    List<String> valueList = headers.get(name);
    if (valueList == null) {
      valueList = new ArrayList<>(1);
      headers.put(name, valueList);
    }
    valueList.add(value);
  }

  /**
   * Appends a to the existing value for a header to the Map.
   * @param name header name
   * @param value header value
   */
  private void appendHeader(String name, String value) {
    List<String> valueList = headers.get(name);
    if (valueList == null) {
      valueList = new ArrayList<>(1);
      headers.put(name, valueList);
    }
    valueList.set(0, valueList.get(0) + value);
  }

  /**
   * String representation of the SmtpMessage.
   * @return a String
   */
  public String toString() {
    StringBuilder msg = new StringBuilder();
    for(Iterator<String> i = headers.keySet().iterator(); i.hasNext();) {
      String name = i.next();
      List<String> values = headers.get(name);
      for(Iterator<String> j = values.iterator(); j.hasNext();) {
        String value = j.next();
        msg.append(name);
        msg.append(": ");
        msg.append(value);
        msg.append('\n');
      }
    }
    msg.append('\n');
    msg.append(body);
    msg.append('\n');
    return msg.toString();
  }

    public Date getCreatedTimestamp()
    {
        return createdTimestamp;
    }
}
