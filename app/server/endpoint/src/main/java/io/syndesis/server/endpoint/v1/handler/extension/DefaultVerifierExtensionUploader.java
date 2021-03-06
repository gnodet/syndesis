/*
 * Copyright (C) 2016 Red Hat, Inc.
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
package io.syndesis.server.endpoint.v1.handler.extension;

import java.io.InputStream;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.syndesis.common.model.extension.Extension;
import io.syndesis.server.dao.file.FileDataManager;
import io.syndesis.server.verifier.MetadataConfigurationProperties;

@Component
@ConditionalOnProperty(value = "openshift.enabled", havingValue = "true", matchIfMissing=true)
public class DefaultVerifierExtensionUploader implements VerifierExtensionUploader {

    private final MetadataConfigurationProperties verificationConfig;

    private final FileDataManager fileDataManager;

    private final NamespacedOpenShiftClient openShiftClient;

    public DefaultVerifierExtensionUploader(
            MetadataConfigurationProperties verificationConfig,
            FileDataManager fileDataManager,
            NamespacedOpenShiftClient openShiftClient) {
        this.verificationConfig = verificationConfig;
        this.fileDataManager = fileDataManager;
        this.openShiftClient = openShiftClient;
    }

    @Override
    public void uploadToVerifier(Extension extension) {

        final WebTarget target = ClientBuilder.newClient()
                .target(String.format("http://%s/api/v1/drivers",
                        verificationConfig.getService()));

        MultipartFormDataOutput multipart = new MultipartFormDataOutput();
        String fileName = ExtensionActivator.getConnectorIdForExtension(extension);
        multipart.addFormData("fileName", fileName, MediaType.TEXT_PLAIN_TYPE);
        InputStream is = fileDataManager.getExtensionBinaryFile(extension.getExtensionId());
        multipart.addFormData("file", is, MediaType.APPLICATION_OCTET_STREAM_TYPE);

        GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(multipart) {};
        Boolean isDeployed = target.request().post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE),Boolean.class);
        if (isDeployed) {
            openShiftClient.deploymentConfigs().withName("syndesis-meta").deployLatest();
        }
    }

    @Override
    public void deleteFromVerifier(Extension extension) {
        final WebTarget target = ClientBuilder.newClient()
                .target(String.format("http://%s/api/v1/drivers/%s",
                        verificationConfig.getService(),ExtensionActivator.getConnectorIdForExtension(extension)));

        Boolean isDeleted = target.request().delete(Boolean.class);
        if (isDeleted) {
            openShiftClient.deploymentConfigs().withName("syndesis-meta").deployLatest();
        }

    }
}
