def call(Map config) {
    def projectType = config.projectType
    
    echo "🔍 Running code quality checks for ${projectType}..."
    
    switch(projectType) {
        case 'maven':
            sh 'mvn checkstyle:check || true'
            sh 'mvn pmd:check || true'
            echo "✓ Maven quality checks completed"
            break
            
        case 'gradle':
            sh './gradlew check || true'
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
                # Activate venv if exists
                if [ -d "venv" ]; then
                    . venv/bin/activate
                fi
                
                # Install and run flake8
                pip install flake8 --break-system-packages 2>/dev/null || pip install flake8 || true
                flake8 . --count --select=E9,F63,F7,F82 --show-source --statistics || true
            '''
            echo "✓ Python quality checks completed"
            break
            
        case 'dotnet':
            echo "✓ .NET quality checks included in build"
            break
            
        default:
            echo "No quality checks configured for ${projectType}"
    }
    
    echo "✓ Quality stage completed"
}
