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
                // Handle global.json SDK version mismatch
                if (fileExists('global.json')) {
                    echo "⚠️ Found global.json - checking SDK compatibility..."
                    
                    def sdkCompatible = sh(
                        script: '''
                            # Extract required SDK version from global.json
                            REQUIRED=$(cat global.json | grep -o '"version"[[:space:]]*:[[:space:]]*"[^"]*"' | cut -d'"' -f4 | head -1)
                            
                            if [ -z "$REQUIRED" ]; then
                                # No version specified, compatible
                                echo "compatible"
                                exit 0
                            fi
                            
                            # Check if required SDK is installed
                            if dotnet --list-sdks | grep -q "^${REQUIRED}"; then
                                echo "compatible"
                            else
                                echo "incompatible"
                            fi
                        ''',
                        returnStdout: true
                    ).trim()
                    
                    if (sdkCompatible == 'incompatible') {
                        echo "⚠️ Required SDK not installed - removing global.json to use latest SDK"
                        sh 'mv global.json global.json.bak'
                        echo "✓ Backed up global.json to global.json.bak"
                    } else {
                        echo "✓ SDK version compatible"
                    }
                }
                
                // Find and build solution or project
                def slnFiles = sh(
                    script: 'ls *.sln 2>/dev/null || echo ""',
                    returnStdout: true
                ).trim()
                
                def csprojFiles = sh(
                    script: 'ls *.csproj 2>/dev/null || echo ""',
                    returnStdout: true
                ).trim()
                
                if (slnFiles) {
                    // Multiple solutions - use first one or let restore/build auto-detect
                    def slnCount = slnFiles.split('\n').size()
                    if (slnCount == 1) {
                        echo "Found solution file: ${slnFiles}"
                        sh "dotnet restore '${slnFiles}'"
                        sh "dotnet build '${slnFiles}' --configuration Release --no-restore"
                    } else {
                        echo "⚠️ Found ${slnCount} solution files - using first one: ${slnFiles.split('\n')[0]}"
                        def firstSln = slnFiles.split('\n')[0]
                        sh "dotnet restore '${firstSln}'"
                        sh "dotnet build '${firstSln}' --configuration Release --no-restore"
                    }
                } else if (csprojFiles) {
                    def csprojCount = csprojFiles.split('\n').size()
                    if (csprojCount == 1) {
                        echo "Found project file: ${csprojFiles}"
                        sh "dotnet restore '${csprojFiles}'"
                        sh "dotnet build '${csprojFiles}' --configuration Release --no-restore"
                    } else {
                        echo "Found ${csprojCount} project files - building all"
                        sh 'dotnet restore'
                        sh 'dotnet build --configuration Release --no-restore'
                    }
                } else {
                    // No specific files found, let dotnet auto-detect
                    sh 'dotnet restore'
                    sh 'dotnet build --configuration Release --no-restore'
                }
                break
                
            default:
                error "Unsupported project type: ${projectType}"
        }
    }
    
    echo "✓ Build completed successfully"
}
