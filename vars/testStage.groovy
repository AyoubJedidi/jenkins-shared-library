def call(Map config) {
    def projectType = config.projectType
    
    echo " Running tests for ${projectType}..."
    
    // Allow custom test command
    if (config.testCommand) {
        echo "Using custom test command: ${config.testCommand}"
        sh config.testCommand
        
        // Custom test results path
        if (config.testResults) {
            junit allowEmptyResults: true, testResults: config.testResults
        }
    } else {
        // Default test commands
        switch(projectType) {
            case 'maven':
                sh 'mvn test'
                junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                break
                
            case 'gradle':
                if (fileExists('./gradlew')) {
                    sh './gradlew test --no-daemon'
                } else {
                    sh 'gradle test --no-daemon'
                }
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
                    if [ -d "venv" ] && [ -f "venv/bin/activate" ]; then
                        . venv/bin/activate
                    fi
                    
                    if [ -d "tests" ] || [ -d "test" ] || ls test_*.py 2>/dev/null | grep -q .; then
                        if [ -d "venv" ]; then
                            pip install pytest 2>/dev/null || true
                        else
                            pip3 install pytest --break-system-packages 2>/dev/null || true
                        fi
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
    }
    
    echo "Tests completed"
}
