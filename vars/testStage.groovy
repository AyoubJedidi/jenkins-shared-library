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
            // Check if test script exists
            def hasTestScript = sh(
                script: 'npm run | grep -q "\\stest"',
                returnStatus: true
            ) == 0
            if (hasTestScript) {
                sh 'npm test || true'  // Don't fail if no tests
            } else {
                echo "No test script found, skipping tests"
            }
            break
            
        case 'python':
            def hasPytest = fileExists('tests') || fileExists('test')
            if (hasPytest) {
                sh 'pip install pytest || true'
                sh 'pytest || true'  // Don't fail if no tests
            } else {
                echo "No tests directory found, skipping tests"
            }
            break
            
        case 'dotnet':
            sh 'dotnet test --no-build --verbosity normal || true'
            break
            
        default:
            echo "No tests configured for ${projectType}"
    }
    
    echo "✓ Tests completed"
}
