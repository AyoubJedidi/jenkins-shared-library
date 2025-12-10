def call(Map config) {
    def imageName = config.imageName ?: config.projectName ?: 'my-app'
    def tag = config.tag ?: 'latest'
    def fullImageName = "${imageName}:${tag}"
    
    echo " Building Docker image: ${fullImageName}"
    
    // Build Docker command with optional arguments
    def dockerBuildCmd = "docker build"
    
    // Add build args if provided
    if (config.buildArgs) {
        config.buildArgs.each { key, value ->
            dockerBuildCmd += " --build-arg ${key}=${value}"
        }
    }
    
    // Add --no-cache if requested
    if (config.noCache) {
        dockerBuildCmd += " --no-cache"
    }
    
    // Add custom Dockerfile path
    def dockerfilePath = config.dockerfile ?: 'Dockerfile'
    dockerBuildCmd += " -f ${dockerfilePath}"
    
    dockerBuildCmd += " -t ${fullImageName} ."
    
    sh dockerBuildCmd
    
    echo "✓ Docker image built: ${fullImageName}"
    
    // Optional: Push to registry
    if (config.pushToRegistry) {
        echo " Pushing to registry..."
        
        if (config.dockerRegistry) {
            sh "docker tag ${fullImageName} ${config.dockerRegistry}/${fullImageName}"
            sh "docker push ${config.dockerRegistry}/${fullImageName}"
        } else {
            sh "docker push ${fullImageName}"
        }
        
        echo "✓ Image pushed to registry"
    }
}
