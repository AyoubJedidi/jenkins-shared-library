def call(Map config) {
    def projectType = config.projectType

    echo " Running security scans for ${projectType}..."

    // Allow custom security command
    if (config.securityCommand) {
        echo "Using custom security command: ${config.securityCommand}"
        sh config.securityCommand
    } else {
        // Run security scans in parallel for speed
        parallel(
            // SonarQube (optional - only if configured)
            'SonarQube': {
                if (config.runSonarQube == true || fileExists('sonar-project.properties')) {
                    stage('SonarQube Analysis') {
                        echo "📊 Running SonarQube analysis..."

                        try {
                            // Check if sonar-scanner is available
                            sh 'which sonar-scanner'

                            // Run SonarQube scan
                            sh 'sonar-scanner'

                            echo " SonarQube scan completed"
                        } catch (Exception e) {
                            echo "SonarQube scan failed (non-blocking): ${e.message}"
                            echo "   Ensure sonar-scanner is installed and sonar-project.properties exists"
                        }
                    }
                } else {
                    echo " SonarQube disabled (set runSonarQube: true or create sonar-project.properties)"
                }
            },

            // Trivy - Container & Dependency Scanning
            'Trivy': {
                if (config.runTrivy != false) {
                    stage('Trivy Scan') {
                        echo " Running Trivy security scan..."

                        try {
                            // Install Trivy if not present
                            sh '''
                                if ! command -v trivy &> /dev/null; then
                                    echo "Installing Trivy..."
                                    wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | apt-key add - 2>/dev/null || true
                                    echo "deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | tee -a /etc/apt/sources.list.d/trivy.list
                                    apt-get update && apt-get install -y trivy || echo " Could not install Trivy"
                                fi
                            '''

                            // Scan filesystem for vulnerabilities
                            def trivySeverity = config.trivySeverity ?: 'HIGH,CRITICAL'
                            sh """
                                trivy fs --severity ${trivySeverity} . || echo "Trivy scan found issues (non-blocking)"
                            """

                            echo " Trivy scan completed"
                        } catch (Exception e) {
                            echo " Trivy scan failed (non-blocking): ${e.message}"
                        }
                    }
                } else {
                    echo " Trivy disabled (set runTrivy: true to enable)"
                }
            },

            // Language-specific security tools
            'Language Security': {
                stage("${projectType} Security Tools") {
                    switch(projectType) {
                        case 'python':
                            pythonSecurity(config)
                            break

                        case 'npm':
                            npmSecurity(config)
                            break

                        case 'maven':
                            mavenSecurity(config)
                            break

                        case 'gradle':
                            gradleSecurity(config)
                            break

                        case 'dotnet':
                            dotnetSecurity(config)
                            break

                        default:
                            echo " No language-specific security tools for ${projectType}"
                    }
                }
            }
        )
    }

    echo " Security stage completed"
}

// Python-specific security tools
def pythonSecurity(Map config) {
    echo " Running Python security checks..."

    sh '''
        # Activate venv if it exists
        if [ -d "venv" ] && [ -f "venv/bin/activate" ]; then
            . venv/bin/activate
        fi

        # Bandit - SAST for Python
        echo "Running Bandit (Python SAST)..."
        if [ -d "venv" ]; then
            pip install bandit 2>/dev/null || true
        else
            pip3 install bandit --break-system-packages 2>/dev/null || true
        fi

        bandit -r . -f json -o bandit-report.json || echo "Bandit found issues (non-blocking)"

        # Safety - Dependency vulnerability scanner
        echo " Running Safety (Dependency check)..."
        if [ -d "venv" ]; then
            pip install safety 2>/dev/null || true
        else
            pip3 install safety --break-system-packages 2>/dev/null || true
        fi

        safety check --json --output safety-report.json || echo "  Safety found vulnerable dependencies (non-blocking)"
    '''

    echo "Python security checks completed"
}

// Node.js/npm security tools
def npmSecurity(Map config) {
    echo " Running npm security checks..."

    sh '''
        # npm audit - built-in vulnerability scanner
        echo "🔍 Running npm audit..."
        npm audit --json > npm-audit.json || echo " npm audit found vulnerabilities (non-blocking)"

        # Show summary
        npm audit || true
    '''

    echo "npm security checks completed"
}

// Maven security tools
def mavenSecurity(Map config) {
    echo "☕ Running Maven security checks..."

    sh '''
        # OWASP Dependency Check
        echo "🔍 Running OWASP Dependency Check..."
        mvn org.owasp:dependency-check-maven:check || echo "Dependency check found issues (non-blocking)"
    '''

    echo "Maven security checks completed"
}

// Gradle security tools
def gradleSecurity(Map config) {
    echo " Running Gradle security checks..."

    sh '''
        # Gradle dependency check
        if ./gradlew tasks --all | grep -q "dependencyCheckAnalyze"; then
            echo "🔍 Running OWASP Dependency Check..."
            ./gradlew dependencyCheckAnalyze || echo " Dependency check found issues (non-blocking)"
        else
            echo "  OWASP Dependency Check plugin not configured"
        fi
    '''

    echo " Gradle security checks completed"
}

// .NET security tools
def dotnetSecurity(Map config) {
    echo " Running .NET security checks..."

    sh '''
        # .NET list package vulnerabilities
        echo "🔍 Checking for vulnerable packages..."
        dotnet list package --vulnerable || echo " Found vulnerable packages (non-blocking)"

        # Security-scan (if available)
        if command -v security-scan &> /dev/null; then
            security-scan . || true
        fi
    '''

    echo ".NET security checks completed"
}