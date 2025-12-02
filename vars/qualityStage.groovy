def call(Map config) {
    def projectType = config.projectType
    
    echo " Running code quality checks for ${projectType}..."
    
    // Allow custom quality command
    if (config.qualityCommand) {
        echo "Using custom quality command: ${config.qualityCommand}"
        sh config.qualityCommand
    } else {
        // Default quality commands
        switch(projectType) {
            case 'maven':
                sh 'mvn checkstyle:check || true'
                sh 'mvn pmd:check || true'
                echo "✓ Maven quality checks completed"
                break
                
            case 'gradle':
                if (fileExists('./gradlew')) {
                    sh './gradlew check || true'
                } else {
                    sh 'gradle check || true'
                }
                echo "✓ Gradle quality checks completed"
                break
                
            case 'npm':
                def hasLintScript = sh(
                    script: 'npm run | grep -q "\\slint"',
                    returnStatus: true
                ) == 0
                if (hasLintScript) {
                    sh 'npm run lint || true'
                } else {
                    echo "No lint script found, skipping"
                }
                echo "✓ npm quality checks completed"
                break
                
            case 'python':
                sh '''
                    if [ -d "venv" ] && [ -f "venv/bin/activate" ]; then
                        . venv/bin/activate
                    fi
                    
                    if [ -d "venv" ]; then
                        pip install flake8 2>/dev/null || true
                    else
                        pip3 install flake8 --break-system-packages 2>/dev/null || true
                    fi
                    flake8 . --count --select=E9,F63,F7,F82 --show-source --statistics || true
                '''
                echo "✓ Python quality checks completed"
                break
                
            case 'dotnet':
                echo " .NET quality checks included in build"
                break
                
            default:
                echo "No quality checks configured for ${projectType}"
        }
    }
    
    echo "✓ Quality stage completed"
}
