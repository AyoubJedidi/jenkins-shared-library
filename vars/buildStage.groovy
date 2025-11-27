    def call(Map config) {
        def projectType = config.projectType

        // Ensure the actual project repo is checked out

        echo "🔨 Building ${projectType} project..."

        switch(projectType) {
            case 'maven':
                sh 'mvn clean install -DskipTests'
                break

            case 'gradle':
                sh './gradlew build -x test --no-daemon'
                break

            case 'npm':
                sh 'npm install && npm run build'
                break

            case 'dotnet':
                sh 'dotnet build --configuration Release'
                break

            default:
                error "Unsupported project type: ${projectType}"
        }

        echo "✓ Build completed successfully"
    }
