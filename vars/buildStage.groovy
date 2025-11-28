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
            // Use virtual environment or --break-system-packages
            sh '''
                if [ -f "requirements.txt" ]; then
                    # Try virtual environment first
                    if command -v python3 &> /dev/null; then
                        python3 -m venv venv || true
                        if [ -d "venv" ]; then
                            . venv/bin/activate
                            pip install -r requirements.txt
                        else
                            # Fallback to --break-system-packages
                            pip install -r requirements.txt --break-system-packages
                        fi
                    else
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
    
    echo "✓ Build completed successfully"
}
