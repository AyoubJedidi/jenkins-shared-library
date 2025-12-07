def call(Map config) {
    // Validate required config
    if (!config.projectType) {
        error " projectType is required! (maven, gradle, npm, python, dotnet)"
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
                        echo "Checking out ${config.gitUrl} (${branch})"
                        
                        config.beforeCheckout?.call()
                        git branch: branch, url: config.gitUrl
                        config.afterCheckout?.call()
                    }
                }
            }
            
            stage('Build') {
                steps {
                    script {
                        config.beforeBuild?.call()
                        
                        if (config.projectDir) {
                            dir(config.projectDir) {
                                buildStage(config)
                            }
                        } else {
                            buildStage(config)
                        }
                        
                        config.afterBuild?.call()
                    }
                }
            }
            
            stage('Test') {
                when {
                    expression { config.runTests != false }
                }
                steps {
                    script {
                        config.beforeTest?.call()
                        
                        if (config.projectDir) {
                            dir(config.projectDir) {
                                testStage(config)
                            }
                        } else {
                            testStage(config)
                        }
                        
                        config.afterTest?.call()
                    }
                }
            }
            
                  stage('Quality') {
                when {
                    expression { config.runQuality == true }
                }
                steps {
                    script {
                        config.beforeQuality?.call()

                        if (config.projectDir) {
                            dir(config.projectDir) {
                                qualityStage(config)
                            }
                        } else {
                            qualityStage(config)
                        }

                        config.afterQuality?.call()
                    }
                }
            }

            stage('Security') {
                when {
                    expression { config.runSecurity != false }
                }
                steps {
                    script {
                        config.beforeSecurity?.call()

                        if (config.projectDir) {
                            dir(config.projectDir) {
                                securityStage(config)
                            }
                        } else {
                            securityStage(config)
                        }

                        config.afterSecurity?.call()
                    }
                }
            }
            
            stage('Docker') {
                when {
                    expression { config.buildDocker == true }
                }
                steps {
                    script {
                        config.beforeDocker?.call()
                        
                        if (config.projectDir) {
                            dir(config.projectDir) {
                                dockerStage(config)
                            }
                        } else {
                            dockerStage(config)
                        }
                        
                        config.afterDocker?.call()
                    }
                }
            }
            
            // Custom stages wrapped in a stage block
            stage('Custom Stages') {
                when {
                    expression { config.customStages != null && config.customStages.size() > 0 }
                }
                steps {
                    script {
                        config.customStages.each { stageName, stageClosure ->
                            echo "Running custom stage: ${stageName}"
                            
                            if (config.projectDir) {
                                dir(config.projectDir) {
                                    stageClosure()
                                }
                            } else {
                                stageClosure()
                            }
                        }
                    }
                }
            }
        }
        
        post {
            success {
                script {
                    echo " Pipeline completed successfully!"
                    
                    if (config.notificationProvider && config.notificationConfig) {
                        notificationDispatcher.send(
                            config.notificationProvider,
                            'success',
                            config.notificationConfig
                        )
                    }
                    
                    if (config.slackChannel) {
                        notifySlack.buildSuccess(config)
                    }
                    
                    config.onSuccess?.call()
                }
            }
            failure {
                script {
                    echo " Pipeline failed!"
                    
                    if (config.notificationProvider && config.notificationConfig) {
                        notificationDispatcher.send(
                            config.notificationProvider,
                            'failure',
                            config.notificationConfig
                        )
                    }
                    
                    if (config.slackChannel) {
                        notifySlack.buildFailure(config)
                    }
                    
                    config.onFailure?.call()
                }
            }
            always {
                script {
                    echo " Pipeline finished"
                    config.onAlways?.call()
                }
            }
        }
    }
}
