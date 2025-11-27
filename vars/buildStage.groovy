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
            // Build is optional for npm
            def hasBuildScript = sh(
                script: 'npm run | grep -q "\\sbuild"',
                returnStatus: true
            ) == 0
            if (hasBuildScript) {
                sh 'npm run build'
            }
            break
            
        case 'python':
            sh 'pip install -r requirements.txt'
            break
            
        case 'dotnet':
            sh 'dotnet restore'
            sh 'dotnet build --configuration Release --no-restore'
            break
            
        default:
            error "Unsupported project type: ${projectType}"
    }
    
    echo "✓ Build completed successfully"
}
