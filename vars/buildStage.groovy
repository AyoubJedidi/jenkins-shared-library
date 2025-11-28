def call(Map config) {
    def projectType = config.projectType
    
    echo "🔨 Building ${projectType} project..."
    
    switch(projectType) {
        case 'maven':
            sh 'mvn clean install -DskipTests'
            break
            
        case 'gradle':

            // --- FIX: auto-detect gradlew like in TestStage ---
            sh '''
                if [ ! -f "./gradlew" ]; then
                    echo "⚠ gradlew not found in current directory, searching..."
                    GRADLEW_PATH=$(find . -maxdepth 3 -name "gradlew" | head -n 1)

                    if [ -z "$GRADLEW_PATH" ]; then
                        echo "❌ gradlew not found anywhere in workspace"
                        exit 1
                    fi

                    echo "✓ gradlew found at: $GRADLEW_PATH"
                    cd "$(dirname "$GRADLEW_PATH")"
                fi
            '''
            // ---------------------------------------------------

            sh './gradlew build -x test --no-daemon'
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
                    # Try venv first
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
    
    echo "✓ Build completed successfully"
}
