#!/usr/bin/env bash

# Create Azure Container Registry
az acr create --name ${CONTAINER_REGISTRY} \
    --resource-group ${RESOURCE_GROUP} \
    --sku basic --location ${REGION} \
    --admin-enabled true

# Create Service Principal
az ad sp create-for-rbac --name ${SERVICE_PRINCIPAL_NAME} --sdk-auth > .scripts/my.azureauth

export SERVICE_PRINCIPAL_OBJECT_ID=$(az ad sp show --id ${SERVICE_PRINCIPAL_NAME} \
    --query objectId --output tsv)

az keyvault set-policy --name ${KEY_VAULT} \
    --object-id ${SERVICE_PRINCIPAL_OBJECT_ID} \
    --certificate-permissions get list \
    --key-permissions get list --secret-permissions get list

# Move to external service
cd external-service

# Deploy to Azure Container Instance

az acr build -r ${CONTAINER_REGISTRY} \
    -t "${CONTAINER_REGISTRY}.azurecr.io/${EXTERNAL_SERVICE_NAME}" .

az container create --name ${EXTERNAL_SERVICE_NAME} \
  --image "${CONTAINER_REGISTRY}.azurecr.io/${EXTERNAL_SERVICE_NAME}"  \
  --registry-password "$(az acr credential show -n ${CONTAINER_REGISTRY} --query "passwords[0].value" -o tsv)" \
  --registry-username "${CONTAINER_REGISTRY}" \
  --environment-variables 'KEY_VAULT_URI'=${KEY_VAULT_URI} \
    'SERVER_SSL_CERTIFICATE_NAME'=${SERVER_SSL_CERTIFICATE_NAME} \
    'EXTERNAL_SERVICE_PORT'=${EXTERNAL_SERVICE_PORT} \
    'SERVICE_PRINCIPAL_TENANT_ID'=${SERVICE_PRINCIPAL_TENANT_ID} \
    'SERVICE_PRINCIPAL_CLIENT_ID'=${SERVICE_PRINCIPAL_CLIENT_ID} \
    'SERVICE_PRINCIPAL_CLIENT_SECRET'=${SERVICE_PRINCIPAL_CLIENT_SECRET} \
  --ip-address Public \
  --ports ${EXTERNAL_SERVICE_PORT} \
  --query "ipAddress.ip" \
  --command-line "tail -f /dev/null"

export EXTERNAL_SERVICE_ENDPOINT=$(az container show --name ${EXTERNAL_SERVICE_NAME} \
    | jq -r '.ipAddress.ip')

# Trouble shooting ...

az container attach --name ${EXTERNAL_SERVICE_NAME}
az container logs --follow --name ${EXTERNAL_SERVICE_NAME}
