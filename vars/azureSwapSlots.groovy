def call(Map config) {
    def azureConfig = config.azure ?: [:]
    def resourceGroup = azureConfig.resourceGroup ?: error("Resource group required")
    def appName = azureConfig.appName ?: config.projectName
    def slotName = azureConfig.slotName ?: 'staging'
    
    echo "🔄 Swapping deployment slots (Blue/Green switch)"
    echo "App: ${appName}"
    echo "Swapping: ${slotName} → production"
    
    sh """
        echo "🔄 Executing slot swap..."
        az webapp deployment slot swap \
          --name ${appName} \
          --resource-group ${resourceGroup} \
          --slot ${slotName}
        
        echo "⏳ Waiting for swap to complete..."
        sleep 10
        
        PROD_URL="https://${appName}.azurewebsites.net"
        STAGING_URL="https://${appName}-${slotName}.azurewebsites.net"
        
        echo ""
        echo "======================================"
        echo "✅ BLUE/GREEN SWAP COMPLETE"
        echo "======================================"
        echo "Production URL: \$PROD_URL"
        echo "Old version now at: \$STAGING_URL"
        echo "Rollback: Swap again to revert"
        echo "======================================"
    """
}
