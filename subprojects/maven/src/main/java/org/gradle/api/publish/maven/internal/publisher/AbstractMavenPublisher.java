/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.publish.maven.internal.publisher;

import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.artifacts.mvnsettings.LocalMavenRepositoryLocator;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.publication.maven.internal.action.MavenPublishAction;
import org.gradle.api.publish.PublicationArtifact;
import org.gradle.api.publish.maven.MavenArtifact;
import org.gradle.internal.Factory;
import org.gradle.internal.logging.LoggingManagerInternal;
import org.gradle.util.GUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class AbstractMavenPublisher implements MavenPublisher {
    private final Factory<LoggingManagerInternal> loggingManagerFactory;

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractMavenPublisher.class);
    private final LocalMavenRepositoryLocator mavenRepositoryLocator;

    public AbstractMavenPublisher(Factory<LoggingManagerInternal> loggingManagerFactory, LocalMavenRepositoryLocator mavenRepositoryLocator) {
        this.loggingManagerFactory = loggingManagerFactory;
        this.mavenRepositoryLocator = mavenRepositoryLocator;
    }

    public void publish(MavenNormalizedPublication publication, MavenArtifactRepository artifactRepository) {
        if (artifactRepository == null) {
            LOGGER.info("Publishing to maven local repository");
        } else {
            LOGGER.info("Publishing to repository '{}' ({})", artifactRepository.getName(), artifactRepository.getUrl());
        }

        MavenPublishAction deployTask = createDeployTask(publication.getPomFile(), publication.getMetadataFile(), mavenRepositoryLocator, artifactRepository);
        addPomAndArtifacts(deployTask, publication);
        execute(deployTask);
    }

    abstract protected MavenPublishAction createDeployTask(File pomFile, File metadataFile, LocalMavenRepositoryLocator mavenRepositoryLocator, MavenArtifactRepository artifactRepository);

    private void addPomAndArtifacts(MavenPublishAction publishAction, MavenNormalizedPublication publication) {
        MavenArtifact mainArtifact = publication.getMainArtifact();
        if (mainArtifact != null) {
            publishAction.setMainArtifact(mainArtifact.getFile());
        }

        for (PublicationArtifact artifact : publication.getMavenArtifacts()) {
            if (artifact == mainArtifact) {
                continue;
            }
            addAdditionalArtifact(publishAction, artifact);
        }
        for (PublicationArtifact artifact : publication.getAdditionalArtifacts()) {
            addAdditionalArtifact(publishAction, artifact);
        }
    }

    private void addAdditionalArtifact(MavenPublishAction publishAction, PublicationArtifact artifact) {
        publishAction.addAdditionalArtifact(artifact.getFile(), GUtil.elvis(artifact.getExtension(), ""), GUtil.elvis(artifact.getClassifier(), ""));
    }

    private void execute(MavenPublishAction publishAction) {
        LoggingManagerInternal loggingManager = loggingManagerFactory.create();
        loggingManager.captureStandardOutput(LogLevel.INFO).start();
        try {
            publishAction.publish();
        } finally {
            loggingManager.stop();
        }
    }

}
