/*
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.detect;

import com.blackducksoftware.integration.hub.bdio.simple.model.Forge
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.ExternalId
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.MavenExternalId
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.NameVersionExternalId
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class ExternalIdTypeAdapter extends TypeAdapter<ExternalId> {
    def forgeMap = [cocoapods: Forge.COCOAPODS, maven: Forge.MAVEN, npm: Forge.NPM, pypi: Forge.PYPI, rubygems: Forge.RUBYGEMS]
    def gson = new Gson()

    @Override
    void write(final JsonWriter jsonWriter, final ExternalId value) throws IOException {
        final TypeAdapter defaultAdapter = gson.getAdapter(value.getClass())
        defaultAdapter.write(jsonWriter, value)
    }

    @Override
    ExternalId read(final JsonReader jsonReader) throws IOException {
        String forgeName = null
        final Map<String, String> otherProperties = [:]
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            final String fieldName = jsonReader.nextName()
            final String fieldValue = jsonReader.nextString()
            if (fieldName.equals("forge")) {
                forgeName = fieldValue
            } else {
                otherProperties.put(fieldName, fieldValue)
            }
        }
        jsonReader.endObject()

        Forge forge = forgeMap[forgeName]
        if (Forge.MAVEN.equals(forge)) {
            return new MavenExternalId(otherProperties.get("group"), otherProperties.get("name"), otherProperties.get("version"))
        } else if (forgeMap.containsKey(forgeName)) {
            return new NameVersionExternalId(forge, otherProperties.get("name"), otherProperties.get("version"))
        }
        return null
    }
}