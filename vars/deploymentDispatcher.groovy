def deploy(Map config) {
    if (!config.cloudProvider) {
        error " cloudProvider is required! (azure, aws, gcp)"
    }

    echo "🚀 Deploying to ${config.cloudProvider}"

    switch(config.cloudProvider.toLowerCase()) {
        case 'azure':
            azureDeployment.deploy(config)
            break

        case 'aws':
            awsDeployment.deploy(config)
            break

        case 'gcp':
            gcpDeployment.deploy(config)
            break

        default:
            error " Unsupported cloud provider: ${config.cloudProvider}"
    }
}