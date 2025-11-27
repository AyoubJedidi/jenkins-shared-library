def call(Map config) {
    def projectType = config.projectType
    
    echo "🔨 Building ${projectType} project..."
    
    switch(projectType) {
        case 'maven':
            sh 'mvn clean install -DskipTests'
            break
            
        case 'gradle':
            sh './gradlew build -x test --no-daemon'
            break
            
        case 'npm':
            sh 'npm install'
            // Only run build if script exists
            def hasBuildScript = sh(
                script: 'npm run | grep -q "build"',
                returnStatus: true
            ) == 0
            if (hasBuildScript) {
                sh 'npm run build'
            } else {
                echo "No build script found, skipping npm run build"
            }
            break
            
        case 'dotnet':
            sh 'dotnet build --configuration Release'
            break
            
        default:
            error "Unsupported project type: ${projectType}"
    }
    
    echo "✓ Build completed successfully"
}
