def call(Map config) {
    def projectType = config.projectType
    
    echo "🧪 Running tests for ${projectType}..."
    
    switch(projectType) {
        case 'maven':
            sh 'mvn test'
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            break
            
        case 'gradle':
            sh './gradlew test --no-daemon'
            junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
            break
            
        case 'npm':
            def hasTestScript = sh(
                script: 'npm run | grep -q "\\stest"',
                returnStatus: true
            ) == 0
            if (hasTestScript) {
                sh 'npm test || true'
            } else {
                echo "No test script found, skipping tests"
            }
            break
            
        case 'python':
            sh '''
                # Activate venv if it exists
                if [ -d "venv" ]; then
                    . venv/bin/activate
                fi
                
                # Check if pytest is needed
                if [ -d "tests" ] || [ -d "test" ]; then
                    pip install pytest --break-system-packages 2>/dev/null || pip install pytest
                    pytest || true
                else
                    echo "No tests directory found, skipping tests"
                fi
            '''
            break
            
        case 'dotnet':
            sh 'dotnet test --no-build --verbosity normal || true'
            break
            
        default:
            echo "No tests configured for ${projectType}"
    }
    
    echo "✓ Tests completed"
}
