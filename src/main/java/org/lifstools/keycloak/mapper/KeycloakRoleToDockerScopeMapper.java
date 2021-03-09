/*
 * Copyright 2021 Nils Hoffmann.
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
package org.lifstools.keycloak.mapper;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.docker.DockerAuthV2Protocol;
import org.keycloak.protocol.docker.mapper.DockerAuthV2AttributeMapper;
import org.keycloak.protocol.docker.mapper.DockerAuthV2ProtocolMapper;
import org.keycloak.representations.docker.DockerAccess;
import org.keycloak.representations.docker.DockerResponseToken;

/**
 * Maps Keycloak user roles docker-pull and docker-push to the respective pull
 * and push scopes for Docker registry authentication.
 *
 * @author nilshoffmann
 */
public class KeycloakRoleToDockerScopeMapper extends DockerAuthV2ProtocolMapper implements DockerAuthV2AttributeMapper {

    private static final Logger log = Logger.getLogger(KeycloakRoleToDockerScopeMapper.class);

    public static final String MAPPER_ID = "docker-v2-group-to-scope-mapper";

    private static final String DOCKER_PULL_ROLE = "docker-pull";

    private static final String DOCKER_PUSH_ROLE = "docker-push";

    private static final String REGISTRY_RESOURCE = "registry";

    private static final String REPOSITORY_RESOURCE = "repository";

    public KeycloakRoleToDockerScopeMapper() {
    }

    @Override
    public String getDisplayType() {
        return "User role to scope mapping";
    }

    @Override
    public String getId() {
        return MAPPER_ID;
    }

    @Override
    public String getHelpText() {
        return "Allows to map between client roles docker-push and docker-pull and docker scopes pull and push on the complete repository.";
    }

    @Override
    public boolean appliesTo(DockerResponseToken drt) {
        return true;
    }

    @Override
    public DockerResponseToken transformDockerResponseToken(DockerResponseToken drt, ProtocolMapperModel pmm, KeycloakSession ks, UserSessionModel usm, AuthenticatedClientSessionModel acsm) {
        // clear any pre-existing permissions
        drt.getAccessItems().clear();
        final String requestedScope = acsm.getNote(DockerAuthV2Protocol.SCOPE_PARAM);
        log.debugf("Received requested docker scope: %s", requestedScope);
        // if no scope is requested (e.g. login), return empty list of access items.
        if (requestedScope == null) {
            return drt;
        }

        Set<String> userRoleNames = usm.getUser().getRoleMappingsStream()
                .map(role -> role.getName()).collect(Collectors.toSet());
        log.debugf("Assigned user roles: %s", userRoleNames);
        // check if user's roles contain at least one of docker-pull or docker-push, deny access otherwise (empty resources)
        if (!userRoleNames.contains(DOCKER_PULL_ROLE) && !userRoleNames.contains(DOCKER_PUSH_ROLE)) {
            log.warn("userRoleNames contained neither " + DOCKER_PULL_ROLE + " nor " + DOCKER_PUSH_ROLE);
            return drt;
        }

        // grant access based on user's assigned roles
        final DockerAccess requestedAccess = new DockerAccess(requestedScope);
        if (REGISTRY_RESOURCE.equals(requestedAccess.getName()) || REPOSITORY_RESOURCE.equals(requestedAccess.getName())) {
            log.debugf("Processing resource: %s", requestedAccess.getName());
            List<String> allowedActions = new LinkedList<>();
            if (userRoleNames.contains(DOCKER_PULL_ROLE)) {
                log.debug("Granting pull access");
                allowedActions.add("pull");
            }
            if (userRoleNames.contains(DOCKER_PUSH_ROLE)) {
                log.debug("Granting push access");
                allowedActions.add("push");
            }
            requestedAccess.setActions(allowedActions);
        }
        drt.getAccessItems().add(requestedAccess);
        return drt;
    }

}
