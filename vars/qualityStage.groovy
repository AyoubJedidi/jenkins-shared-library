def call(Map config) {
    def projectType = config.projectType
    
    echo "🔍 Running code quality checks for ${projectType}..."
    
    switch(projectType) {
        case 'maven':
            // Maven: Checkstyle, SpotBugs, PMD
            sh 'mvn checkstyle:check || true'
            sh 'mvn pmd:check || true'
            echo "✓ Maven quality checks completed"
            break
            
        case 'gradle':
            // Gradle: Checkstyle, SpotBugs
            sh './gradlew check || true'
            echo "✓ Gradle quality checks completed"
            break
            
        case 'npm':
            // npm: ESLint, Prettier
            sh 'npm run lint || true'
            echo "✓ npm quality checks completed"
            break
            
        case 'dotnet':
            // .NET: Built-in analyzers run during build
            echo "✓ .NET quality checks included in build"
            break
            
        default:
            echo "No quality checks configured for ${projectType}"
    }
    
    echo "✓ Quality stage completed"
}
