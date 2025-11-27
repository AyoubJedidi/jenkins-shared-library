def call(Map config) {
    def imageName = config.imageName ?: config.projectName ?: 'my-app'
    def tag = config.tag ?: 'latest'
    def fullImageName = "${imageName}:${tag}"
    
    echo "Docker stage called!"
    echo "Building image: ${fullImageName}"
    
    sh "docker build -t ${fullImageName} ."
    
    echo "Docker image built: ${fullImageName}"
}
