def call(Map config) {
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
                        git branch: branch, url: config.gitUrl
                        
                        // If project is in subdirectory
                        if (config.projectDir) {
                            dir(config.projectDir) {
                                echo "Working in directory: ${config.projectDir}"
                            }
                        }
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
                echo "✅ Pipeline completed successfully!"
            }
            failure {
                echo "❌ Pipeline failed!"
            }
        }
    }
}
