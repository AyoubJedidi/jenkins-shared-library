def deploy(Map config) {
    def azureConfig = config.azure ?: [:]
    def resourceGroup = azureConfig.resourceGroup ?: 'cicd-framework-rg'
    def containerName = azureConfig.containerName ?: config.projectName
    def dnsLabel = azureConfig.dnsLabel ?: "${config.projectName}-demo"
    def location = azureConfig.location ?: 'eastus'

    echo "🔵 Deploying to Azure Container Instances"
    echo "Resource Group: ${resourceGroup}"
    echo "Container: ${containerName}"

    withCredentials([
        usernamePassword(
            credentialsId: 'azure-acr',
            usernameVariable: 'ACR_USER',
            passwordVariable: 'ACR_PASS'
        )
    ]) {
        sh """
            # Delete old container
            az container delete \
              --resource-group ${resourceGroup} \
              --name ${containerName} \
              --yes || true

            sleep 10

            # Create container
            az container create \
              --resource-group ${resourceGroup} \
              --name ${containerName} \
              --image ${config.dockerRegistry}/${config.projectName}:latest \
              --registry-login-server ${config.dockerRegistry} \
              --registry-username \$ACR_USER \
              --registry-password \$ACR_PASS \
              --dns-name-label ${dnsLabel} \
              --ports ${azureConfig.port ?: 8080} \
              --os-type Linux \
              --cpu ${azureConfig.cpu ?: 2} \
              --memory ${azureConfig.memory ?: 2} \
              --restart-policy Always \
              --location ${location}

            # Get URL
            FQDN=\$(az container show \
              --resource-group ${resourceGroup} \
              --name ${containerName} \
              --query ipAddress.fqdn \
              --output tsv)

            echo ""
            echo "======================================"
            echo "✅ AZURE DEPLOYMENT COMPLETE"
            echo "======================================"
            echo "URL: http://\${FQDN}:${azureConfig.port ?: 8080}"
            echo "======================================"
        """
    }
}