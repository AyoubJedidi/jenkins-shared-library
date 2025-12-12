def call(Map config) {
    def azureConfig = config.azure ?: [:]
    def resourceGroup = azureConfig.resourceGroup ?: error("Resource group required")
    def appName = azureConfig.appName ?: config.projectName
    def location = azureConfig.location ?: 'eastus'
    def sku = azureConfig.sku ?: 'S3'
    def slotName = azureConfig.slotName ?: 'staging'
    def autoSwap = azureConfig.autoSwap ?: false
    def trafficPercent = azureConfig.trafficPercent ?: 0
    
    echo "🔵 Deploying to Azure App Service"
    echo "App: ${appName}"
    echo "Auto-Swap: ${autoSwap}"
    echo "Traffic to Staging: ${trafficPercent}%"
    
    withCredentials([
        usernamePassword(
            credentialsId: 'azure-acr-france',
            usernameVariable: 'ACR_USER',
            passwordVariable: 'ACR_PASS'
        )
    ]) {
        sh """
            # Ensure plan exists
            az appservice plan create \
              --name ${appName}-plan \
              --resource-group ${resourceGroup} \
              --location ${location} \
              --sku ${sku} \
              --is-linux 2>/dev/null || \
            az appservice plan update \
              --name ${appName}-plan \
              --resource-group ${resourceGroup} \
              --sku ${sku}
            
            # Ensure web app exists
            az webapp create \
              --name ${appName} \
              --resource-group ${resourceGroup} \
              --plan ${appName}-plan \
              --deployment-container-image-name ${config.dockerRegistry}/${config.projectName}:latest 2>/dev/null || echo "App exists"
            
            # Configure production container
            az webapp config container set \
              --name ${appName} \
              --resource-group ${resourceGroup} \
              --docker-custom-image-name ${config.dockerRegistry}/${config.projectName}:latest \
              --docker-registry-server-url https://${config.dockerRegistry} \
              --docker-registry-server-user \$ACR_USER \
              --docker-registry-server-password "\$ACR_PASS"
            
            # Create staging slot
            az webapp deployment slot create \
              --name ${appName} \
              --resource-group ${resourceGroup} \
              --slot ${slotName} 2>/dev/null || echo "Slot exists"
            
            # Enable auto-swap
            AUTO_SWAP="${autoSwap}"
            if [ "\$AUTO_SWAP" = "true" ]; then
                echo "Enabling auto-swap..."
                az webapp deployment slot auto-swap \
                  --name ${appName} \
                  --resource-group ${resourceGroup} \
                  --slot ${slotName} \
                  --auto-swap-slot production
            fi
            
            # Deploy to staging
            az webapp config container set \
              --name ${appName} \
              --resource-group ${resourceGroup} \
              --slot ${slotName} \
              --docker-custom-image-name ${config.dockerRegistry}/${config.projectName}:latest \
              --docker-registry-server-url https://${config.dockerRegistry} \
              --docker-registry-server-user \$ACR_USER \
              --docker-registry-server-password "\$ACR_PASS"
            
            # Configure traffic if specified
            TRAFFIC="${trafficPercent}"
            if [ "\$TRAFFIC" -gt 0 ]; then
                echo "Routing \$TRAFFIC% traffic to staging..."
                az webapp traffic-routing set \
                  --name ${appName} \
                  --resource-group ${resourceGroup} \
                  --distribution ${slotName}=\$TRAFFIC
            fi
            
            PROD_URL="https://${appName}.azurewebsites.net"
            STAGING_URL="https://${appName}-${slotName}.azurewebsites.net"
            
            echo ""
            echo "======================================"
            echo "✅ AZURE APP SERVICE DEPLOYED"
            echo "======================================"
            echo "Production: \$PROD_URL"
            echo "Staging: \$STAGING_URL"
            echo "Auto-Swap: ${autoSwap}"
            echo "Traffic Split: ${trafficPercent}% to staging"
            echo "======================================"
        """
    }
}
