#!/usr/bin/env bash

# ==== Create Azure Container Registry ====
az acr create --name ${CONTAINER_REGISTRY} \
    --resource-group ${RESOURCE_GROUP} \
    --sku basic --location ${REGION} \
    --admin-enabled true

export RESOURCE_GROUP_ID=$(az group show --name ${RESOURCE_GROUP} --query id --output tsv)

# ==== Move to external service ====
cd external-service

# ==== Compile and run ====
mvn package

# ==== Build and push image to Azure Container Registry ====
az acr build -r ${CONTAINER_REGISTRY} \
    -t "${CONTAINER_REGISTRY}.azurecr.io/${EXTERNAL_SERVICE_NAME}" .

# ==== Deploy to Azure Container Instance ====
az container create --name ${EXTERNAL_SERVICE_NAME} \
  --image "${CONTAINER_REGISTRY}.azurecr.io/${EXTERNAL_SERVICE_NAME}"  \
  --registry-password "$(az acr credential show -n ${CONTAINER_REGISTRY} --query "passwords[0].value" -o tsv)" \
  --registry-username "${CONTAINER_REGISTRY}" \
  --assign-identity --scope ${RESOURCE_GROUP_ID} \
  --environment-variables 'KEY_VAULT_URI'=${KEY_VAULT_URI} \
    'SERVER_SSL_CERTIFICATE_NAME'=${SERVER_SSL_CERTIFICATE_NAME} \
    'EXTERNAL_SERVICE_PORT'=${EXTERNAL_SERVICE_PORT} \
  --ip-address Public \
  --ports ${EXTERNAL_SERVICE_PORT} \
  --query "ipAddress.ip" \
  --command-line "tail -f /dev/null"

# ==== Retrieve System Assigned Managed Identity ====
export EXTERNAL_SERVICE_SYSTEM_ASSIGNED_MANAGED_IDENTITY_SERVICE_PRINCIPAL=$(az container show --name ${EXTERNAL_SERVICE_NAME} \
    | jq -r '.identity.principalId')

# ==== Grant System Assigned Managed Identity with access to Key Vault ====
az keyvault set-policy --name ${KEY_VAULT} \
    --object-id ${EXTERNAL_SERVICE_SYSTEM_ASSIGNED_MANAGED_IDENTITY_SERVICE_PRINCIPAL} \
    --certificate-permissions get list \
    --key-permissions get list --secret-permissions get list

# ==== Stop the container instance =====
az container stop --name ${EXTERNAL_SERVICE_NAME}

# ==== Wait for 5 minutes and redeploy :-) ====
sleep 300

az container create --name ${EXTERNAL_SERVICE_NAME} \
  --image "${CONTAINER_REGISTRY}.azurecr.io/${EXTERNAL_SERVICE_NAME}"  \
  --registry-password "$(az acr credential show -n ${CONTAINER_REGISTRY} --query "passwords[0].value" -o tsv)" \
  --registry-username "${CONTAINER_REGISTRY}" \
  --assign-identity --scope ${RESOURCE_GROUP_ID} \
  --environment-variables 'KEY_VAULT_URI'=${KEY_VAULT_URI} \
    'SERVER_SSL_CERTIFICATE_NAME'=${SERVER_SSL_CERTIFICATE_NAME} \
    'EXTERNAL_SERVICE_PORT'=${EXTERNAL_SERVICE_PORT} \
  --ip-address Public \
  --ports ${EXTERNAL_SERVICE_PORT} \
  --query "ipAddress.ip"

# ==== Start container instance ====
az container start --name ${EXTERNAL_SERVICE_NAME}

export EXTERNAL_SERVICE_ENDPOINT=$(az container show --name ${EXTERNAL_SERVICE_NAME} \
    | jq -r '.ipAddress.ip')

cd ..

# ==== Trouble shooting ... ====

# az container attach --name ${EXTERNAL_SERVICE_NAME}
# az container logs --follow --name ${EXTERNAL_SERVICE_NAME}
