def call(Map config) {
    def projectType = config.projectType
    
    echo "🧪 Running tests for ${projectType}..."
    
    switch(projectType) {
        case 'maven':
            sh 'mvn test'
            // Only publish results if tests exist
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            break
            
        case 'gradle':
            sh './gradlew test --no-daemon'
            junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
            break
            
        case 'npm':
            sh 'npm test'
            break
            
        case 'dotnet':
            sh 'dotnet test --no-build'
            break
            
        default:
            echo "No tests configured for ${projectType}"
    }
    
    echo "✓ Tests completed"
}
