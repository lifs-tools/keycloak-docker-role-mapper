# Keycloak Role to Docker Registry Pull and Push Scope Mapper

This keycloak mapper adds role mapping capability for the docker-v2 authentication flow in Keycloak. 
It currently requires two roles ('docker-pull' and 'docker-push') to be present in the list of a user's roles. 
These roles are checked and depending on their presence or absence, access to the registry or any repository is permitted or denied.
If none of the roles are present, access is denied (e.g. docker login will fail, but so will pull and push). 
For docker login to succeed, at least one of the groups must be present for a user. Through Keycloak's composite roles concept, 
the 'docker-push' role could be defined to automatically include 'docker-pull', too. This is up to the administrator to configure, though.

Please note the the role names are currently hard-coded in the mapper. However, pull requests are always welcome to change this to a more 
Keycloak-like dynamic mapping. Also, defining different access scopes for individual repositories is currently not possible.

## Namespace

The module / mapper has the namespace `org.lifstools.keycloak.keycloak-docker-role-mapper` (see below for instructions on registration).

## Installation in Keycloak

1. Make sure to start Keycloak with the `-Dkeycloak.profile.feature.docker=enabled` profile enabled.
2. Documentation specifically for Keycloak authentication for a Docker registry is here: https://www.keycloak.org/docs/latest/server_admin/index.html#_docker
3. Create a new module

```
/opt/jboss/keycloak/bin/jboss-cli.sh --command="module add --name=org.lifstools.keycloak.keycloak-docker-role-mapper --resources=/keycloak-docker-role-mapper-0.0.1.jar --dependencies=org.jboss.logging,org.keycloak.keycloak-core,org.keycloak.keycloak-services,org.keycloak.keycloak-server-spi-private,org.keycloak.keycloak-server-spi"
```

4. Activate the module with a JBoss cli script (or run this within an interactive cli session with `/opt/jboss/keycloak/bin/jboss-cli.sh`):

```
embed-server --server-config=standalone-ha.xml
/subsystem=keycloak-server:list-add(name=providers, value=module:org.lifstools.keycloak.keycloak-docker-role-mapper)
stop-embedded-server
```

5. Log-in to Keycloak as an administrator
6. Navigate to the realm, where you want to add the docker client
7. Go to 'Clients' and click on 'Create'
8. Enter a client ID, e.g. 'docker-registry', select 'docker-v2' as the client protocol and click on 'Save'
9. Go to the 'Mappers' tab of the 'docker-registry' client (from 'Clients')
10. Remove the default 'Allow All' mapper
11. Click on 'Create' and select mapper type 'User role to scope mapping', set a name for the mapper, click on 'Save'
12. Go to the 'Roles' tab of the 'docker-registry' client and click on 'Add Role'
13. Enter 'docker-pull' as the 'Role name' and click on 'Save'
14. Go to the 'Roles' tab of the 'docker-registry' client and click on 'Add Role'
15. Enter 'docker-push' as the 'Role name' and click on 'Save'
16. Go to the 'Role Mappings' tab of a user of your choice (from 'Users' - select a user - go to 'Role Mappings' tab)
17. Select the 'docker-registry' client from 'Client Roles'
18. Select 'docker-pull' and/or 'docker-push' and click on 'Add selected'
19. Try to run `docker login -u <USERNAME>` with the modified user

**NOTE** For docker login to succeed, at least one of 'docker-pull' or 'docker-push' must be assigned.

## License

The source code is licensed under the terms of the Apache license (see LICENSE file) v2.0.
Contributions are welcome!

## Attributions

Thanks to Ivan Eggel for providing a mapper implementation with a slightly different objective at https://github.com/ieggel/DockerRegistryKeycloakUserNamespaceMapper
