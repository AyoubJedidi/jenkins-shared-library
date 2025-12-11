def call(Map config) {
    def azureConfig = config.azure ?: [:]
    def resourceGroup = azureConfig.resourceGroup ?: error("Resource group required")
    def appName = azureConfig.appName ?: config.projectName
    def runtime = azureConfig.runtime ?: 'DOCKER'
    def location = azureConfig.location ?: 'eastus'
    def sku = azureConfig.sku ?: 'B1'
    def enableBlueGreen = azureConfig.blueGreen ?: false
    def slotName = azureConfig.slotName ?: 'staging'
    def dockerRegistry = azureConfig.dockerRegistry ?: config.dockerRegistry
    
    echo "🔵 Deploying to Azure App Service"
    echo "App: ${appName}"
    echo "Blue/Green: ${enableBlueGreen}"
    
    withCredentials([
        usernamePassword(
            credentialsId: 'azure-acr',
            usernameVariable: 'REGISTRY_USER',
            passwordVariable: 'REGISTRY_PASS'
        )
    ]) {
        sh """
            echo "🔐 Logging into Azure..."
            # Assumes Azure CLI is already authenticated or uses service principal
            
            echo "📦 Creating App Service Plan (if not exists)..."
            az appservice plan create \
              --name ${appName}-plan \
              --resource-group ${resourceGroup} \
              --location ${location} \
              --sku ${sku} \
              --is-linux || echo "Plan exists"
            
            echo "🌐 Creating Web App (if not exists)..."
            az webapp create \
              --name ${appName} \
              --resource-group ${resourceGroup} \
              --plan ${appName}-plan \
              --deployment-container-image-name ${dockerRegistry}/${config.projectName}:latest || echo "App exists"
            
            echo "🔧 Configuring container registry..."
            az webapp config container set \
              --name ${appName} \
              --resource-group ${resourceGroup} \
              --docker-custom-image-name ${dockerRegistry}/${config.projectName}:latest \
              --docker-registry-server-url https://${dockerRegistry} \
              --docker-registry-server-user \$REGISTRY_USER \
              --docker-registry-server-password \$REGISTRY_PASS
            
            if [ "${enableBlueGreen}" = "true" ]; then
                echo "🟢 Blue/Green Deployment Enabled"
                
                echo "📝 Creating staging slot (if not exists)..."
                az webapp deployment slot create \
                  --name ${appName} \
                  --resource-group ${resourceGroup} \
                  --slot ${slotName} || echo "Slot exists"
                
                echo "🚀 Deploying to STAGING slot (Green)..."
                az webapp config container set \
                  --name ${appName} \
                  --resource-group ${resourceGroup} \
                  --slot ${slotName} \
                  --docker-custom-image-name ${dockerRegistry}/${config.projectName}:latest \
                  --docker-registry-server-url https://${dockerRegistry} \
                  --docker-registry-server-user \$REGISTRY_USER \
                  --docker-registry-server-password \$REGISTRY_PASS
                
                echo "⏳ Waiting for staging deployment..."
                sleep 30
                
                STAGING_URL="https://${appName}-${slotName}.azurewebsites.net"
                echo "📋 Staging URL: \$STAGING_URL"
                echo "✅ Test the staging environment before swapping!"
                echo ""
                echo "To complete Blue/Green swap, run:"
                echo "  az webapp deployment slot swap \\"
                echo "    --name ${appName} \\"
                echo "    --resource-group ${resourceGroup} \\"
                echo "    --slot ${slotName}"
                
            else
                echo "🚀 Direct deployment to production..."
                az webapp restart \
                  --name ${appName} \
                  --resource-group ${resourceGroup}
            fi
            
            PROD_URL="https://${appName}.azurewebsites.net"
            
            echo ""
            echo "======================================"
            echo "✅ AZURE APP SERVICE DEPLOYMENT"
            echo "======================================"
            echo "App Name: ${appName}"
            echo "Production URL: \$PROD_URL"
            if [ "${enableBlueGreen}" = "true" ]; then
                echo "Staging URL: \$STAGING_URL"
                echo "Status: Deployed to STAGING (Green)"
                echo "Action Required: Test staging, then swap"
            else
                echo "Status: Deployed to PRODUCTION"
            fi
            echo "======================================"
        """
    }
}
