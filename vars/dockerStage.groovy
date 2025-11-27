def call(Map config) {
    def imageName = config.imageName ?: config.projectName ?: 'my-app'
    def tag = config.tag ?: 'latest'
    def fullImageName = "${imageName}:${tag}"
    
    echo "🐳 Building Docker image: ${fullImageName}"

    sh "docker build -t ${fullImageName} ."

    echo "✓ Docker image built: ${fullImageName}"

    // Optional: Push to registry
    if (config.pushToRegistry) {
        echo "📤 Pushing to registry..."

        if (config.dockerRegistry) {
            sh "docker tag ${fullImageName} ${config.dockerRegistry}/${fullImageName}"
            sh "docker push ${config.dockerRegistry}/${fullImageName}"
        } else {
            sh "docker push ${fullImageName}"
        }

        echo "✓ Image pushed to registry"
    }
}