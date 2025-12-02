def call(Map config) {
    def projectType = config.projectType
    
    echo "🔨 Building ${projectType} project..."
    
    // Allow custom build command
    if (config.buildCommand) {
        echo "Using custom build command: ${config.buildCommand}"
        sh config.buildCommand
    } else {
        // Default build commands
        switch(projectType) {
            case 'maven':
                sh 'mvn clean install -DskipTests'
                break
                
            case 'gradle':
                // Check for gradlew first
                if (fileExists('./gradlew')) {
                    sh './gradlew build -x test --no-daemon'
                } else {
                    sh 'gradle build -x test --no-daemon'
                }
                break
                
            case 'npm':
                sh 'npm install'
                def hasBuildScript = sh(
                    script: 'npm run | grep -q "\\sbuild"',
                    returnStatus: true
                ) == 0
                if (hasBuildScript) {
                    sh 'npm run build'
                }
                break
                
            case 'python':
                sh '''
                    if [ -f "requirements.txt" ]; then
                        if python3 -m venv venv 2>/dev/null; then
                            echo "✓ Using virtual environment"
                            . venv/bin/activate
                            pip install -r requirements.txt
                        else
                            echo "⚠ venv not available, using --break-system-packages"
                            pip3 install -r requirements.txt --break-system-packages || \
                            pip install -r requirements.txt --break-system-packages
                        fi
                    fi
                '''
                break
                
            case 'dotnet':
                sh 'dotnet restore'
                sh 'dotnet build --configuration Release --no-restore'
                break
                
            default:
                error "Unsupported project type: ${projectType}"
        }
    }
    
    echo "✓ Build completed successfully"
}
