def call(Map config) {
    pipeline {
        agent any
        
        stages {
            stage('Build') {
                steps {
                    script {
                        echo "🚀 Starting standardPipeline for ${config.projectType}"
                        buildStage(config)
                    }
                }
            }
            
            stage('Test') {
                when {
                    expression { config.runTests != false }
                }
                steps {
                    script {
                        testStage(config)
                    }
                }
            }
            
            stage('Docker') {
                when {
                    expression { config.buildDocker == true }
                }
                steps {
                    script {
                        dockerStage(config)
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
