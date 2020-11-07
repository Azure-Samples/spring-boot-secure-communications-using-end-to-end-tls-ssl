#!/usr/bin/env bash

# ==== Start from Project Root Folder ====
cp .scripts/setup-env-variables-azure-template.sh .scripts/setup-env-variables-azure.sh
# ==== Customize setup-env-variables-azure.sh with your specific info

# ==== Create Resource Group ====
az group create --name ${RESOURCE_GROUP} \
    --location ${REGION}

az group lock create --lock-type CanNotDelete --name DoNotDelete \
    --notes For-Asir \
    --resource-group ${RESOURCE_GROUP}

az configure --defaults \
    group=${RESOURCE_GROUP} \
    location=${REGION} \
    spring-cloud=${SPRING_CLOUD_SERVICE}

# ==== Create Key Vault, self-signed certificate and custom domain certificate ====

az keyvault create --name ${KEY_VAULT} -g ${RESOURCE_GROUP}
export KEY_VAULT_URI=$(az keyvault show --name ${KEY_VAULT} | jq -r '.properties.vaultUri')

az keyvault certificate create --vault-name ${KEY_VAULT} \
    -n ${SERVER_SSL_CERTIFICATE_NAME} \
    -p "$(az keyvault certificate get-default-policy)"

# purchase a certificate from an SSL certificate shop - manual step through portal

# ==== You may have to merge certificates into 1 file ====
# ==== SAMPLE SCRIPT =====================================
openssl pkcs12 -export -out myserver2.pfx -inkey privatekey.key -in mergedcert2.crt

# ==== Add certificate to Key Vault ====

az keyvault certificate import --file myserver2.pfx \
    --name ${CUSTOM_DOMAIN_CERTIFICATE_NAME} \
    --vault-name ${KEY_VAULT} --password 123456

# ==== Deploy external service first ====
source .scripts/deploy-external-service.sh

# ==== Create Azure Spring Cloud ====
az spring-cloud create --name ${SPRING_CLOUD_SERVICE} \
    --resource-group ${RESOURCE_GROUP} \
    --location ${REGION}

# ==== Apply Config ====
az spring-cloud config-server set \
    --config-file application.yml \
    --name ${SPRING_CLOUD_SERVICE}

# ==== Import custom domain certificate ====
# First grant Azure Spring Cloud Domain Manager access to Key Vault
az ad sp show --id 03b39d0f-4213-4864-a245-b1476ec03169 --query objectId

az keyvault set-policy --name ${KEY_VAULT} \
    --object-id 938df8e2-2b9d-40b1-940c-c75c33494239 \
    --certificate-permissions get list \
    --secret-permissions get list

az spring-cloud certificate add --name ${CUSTOM_DOMAIN_CERTIFICATE_NAME} \
    --vault-uri ${KEY_VAULT_URI} \
    --vault-certificate-name ${CUSTOM_DOMAIN_CERTIFICATE_NAME}

# ==== Create the gateway app ====
az spring-cloud app create --name gateway --instance-count 1 --is-public true \
    --memory 2 \
    --jvm-options='-Xms2048m -Xmx2048m -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:+UseG1GC -Djava.awt.headless=true' \
    --env KEY_VAULT_URI=${KEY_VAULT_URI} \
          GREETING_SERVICE=${GREETING_SERVICE} \
          GREETING_EXTERNAL_SERVICE=${GREETING_EXTERNAL_SERVICE}

# ==== Assign System Assigned Managed Identity to the gateway app ====
az spring-cloud app identity assign --name gateway
export GATEWAY_IDENTITY=$(az spring-cloud app show --name gateway | \
    jq -r '.identity.principalId')

# ==== Grant gateway app with access to the Key Vault ====
az keyvault set-policy --name ${KEY_VAULT} \
   --object-id ${GATEWAY_IDENTITY} --certificate-permissions get list \
   --key-permissions get list --secret-permissions get list

export SECURE_GATEWAY_URL=$(az spring-cloud app show --name gateway | jq -r '.properties.url')

# Map custom domain to gateway - manual DNS entry step

# ==== Bind custom domain ====
az spring-cloud app custom-domain bind --app gateway \
    --domain-name ${CUSTOM_DOMAIN} --certificate ${CUSTOM_DOMAIN_CERTIFICATE_NAME}

# ==== Create the greeting-service app ====
az spring-cloud app create --name greeting-service --instance-count 1 \
    --memory 2 \
    --jvm-options='-Xms2048m -Xmx2048m -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:+UseG1GC -Djava.awt.headless=true' \
    --env KEY_VAULT_URI=${KEY_VAULT_URI} \
          SERVER_SSL_CERTIFICATE_NAME=${SERVER_SSL_CERTIFICATE_NAME}

az spring-cloud app identity assign --name greeting-service
export GREETING_SERVICE_IDENTITY=$(az spring-cloud app show --name greeting-service | \
    jq -r '.identity.principalId')

az keyvault set-policy --name ${KEY_VAULT} \
   --object-id ${GREETING_SERVICE_IDENTITY} --certificate-permissions get list \
   --key-permissions get list --secret-permissions get list

# ==== Create the greeting-external-service app ====
az spring-cloud app create --name greeting-external-service --instance-count 1 \
    --memory 2 \
    --jvm-options='-Xms2048m -Xmx2048m -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:+UseG1GC -Djava.awt.headless=true' \
    --env KEY_VAULT_URI=${KEY_VAULT_URI} \
          SERVER_SSL_CERTIFICATE_NAME=${SERVER_SSL_CERTIFICATE_NAME} \
          EXTERNAL_SERVICE_ENDPOINT=${EXTERNAL_SERVICE_ENDPOINT} \
          EXTERNAL_SERVICE_PORT=${EXTERNAL_SERVICE_PORT}

az spring-cloud app identity assign --name greeting-external-service
export GREETING_EXTERNAL_SERVICE_IDENTITY=$(az spring-cloud app show \
    --name greeting-external-service| jq -r '.identity.principalId')

az keyvault set-policy --name ${KEY_VAULT} \
   --object-id ${GREETING_EXTERNAL_SERVICE_IDENTITY} --certificate-permissions get list \
   --key-permissions get list --secret-permissions get list

# ==== Build for cloud ====
mvn clean package -DskipTests -Denv=cloud

# ==== Deploy apps ====
az spring-cloud app deploy --name gateway --jar-path ${GATEWAY_JAR}

az spring-cloud app deploy --name greeting-service --jar-path ${GREETING_SERVICE_JAR}

az spring-cloud app deploy --name greeting-external-service \
    --jar-path ${GREETING_EXTERNAL_SERVICE_JAR}