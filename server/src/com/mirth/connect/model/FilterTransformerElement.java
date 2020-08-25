/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * 
 * http://www.mirthcorp.com
 * 
 * The software in this package is published under the terms of the MPL license a copy of which has
 * been included with this distribution in the LICENSE.txt file.
 */

package com.mirth.connect.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.mirth.connect.donkey.util.purge.Purgable;
import com.mirth.connect.util.ScriptBuilderException;

public abstract class FilterTransformerElement implements Serializable, Purgable {

    private String name;
    private String sequenceNumber;

    public FilterTransformerElement() {}

    public FilterTransformerElement(FilterTransformerElement props) {
        name = props.getName();
        sequenceNumber = props.getSequenceNumber();
    }

    public abstract String getScript(boolean loadFiles) throws ScriptBuilderException;

    public abstract String getType();

    @Override
    public abstract FilterTransformerElement clone();

    public Collection<String> getResponseVariables() {
        return null;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSequenceNumber() {
        return this.sequenceNumber;
    }

    public void setSequenceNumber(String sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, CalendarToStringStyle.instance());
    }

    @Override
    public Map<String, Object> getPurgedProperties() {
        Map<String, Object> purgedProperties = new HashMap<String, Object>();
        purgedProperties.put("sequenceNumber", sequenceNumber);
        return purgedProperties;
    }
}