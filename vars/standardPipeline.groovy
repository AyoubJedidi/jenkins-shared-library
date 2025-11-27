def call(Map config) {
    // Validate required config
    if (!config.projectType) {
        error "❌ projectType is required! (maven, gradle, npm, python, dotnet)"
    }
    
    pipeline {
        agent any
        
        stages {
            stage('Checkout') {
                when {
                    expression { config.gitUrl != null }
                }
                steps {
                    script {
                        def branch = config.gitBranch ?: 'main'
                        echo "📥 Checking out ${config.gitUrl} (${branch})"
                        git branch: branch, url: config.gitUrl
                    }
                }
            }
            
            stage('Build') {
                steps {
                    script {
                        if (config.projectDir) {
                            dir(config.projectDir) {
                                buildStage(config)
                            }
                        } else {
                            buildStage(config)
                        }
                    }
                }
            }
            
            stage('Test') {
                when {
                    expression { config.runTests != false }
                }
                steps {
                    script {
                        if (config.projectDir) {
                            dir(config.projectDir) {
                                testStage(config)
                            }
                        } else {
                            testStage(config)
                        }
                    }
                }
            }
            
            stage('Quality') {
                when {
                    expression { config.runQuality == true }
                }
                steps {
                    script {
                        if (config.projectDir) {
                            dir(config.projectDir) {
                                qualityStage(config)
                            }
                        } else {
                            qualityStage(config)
                        }
                    }
                }
            }
            
            stage('Docker') {
                when {
                    expression { config.buildDocker == true }
                }
                steps {
                    script {
                        if (config.projectDir) {
                            dir(config.projectDir) {
                                dockerStage(config)
                            }
                        } else {
                            dockerStage(config)
                        }
                    }
                }
            }
        }
        
        post {
            success {
                script {
                    echo "✅ Pipeline completed successfully!"
                    if (config.slackChannel) {
                        notifySlack.buildSuccess(config)
                    }
                }
            }
            failure {
                script {
                    echo "❌ Pipeline failed!"
                    if (config.slackChannel) {
                        notifySlack.buildFailure(config)
                    }
                }
            }
            always {
                script {
                    echo "🏁 Pipeline finished"
                }
            }
        }
    }
}
