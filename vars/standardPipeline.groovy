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
                        echo " Checking out ${config.gitUrl} (${branch})"
                        
                        // Before checkout hook
                        config.beforeCheckout?.call()
                        
                        git branch: branch, url: config.gitUrl
                        
                        // After checkout hook
                        config.afterCheckout?.call()
                    }
                }
            }
            
            stage('Build') {
                steps {
                    script {
                        // Before build hook
                        config.beforeBuild?.call()
                        
                        if (config.projectDir) {
                            dir(config.projectDir) {
                                buildStage(config)
                            }
                        } else {
                            buildStage(config)
                        }
                        
                        // After build hook
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
                        // Before test hook
                        config.beforeTest?.call()
                        
                        if (config.projectDir) {
                            dir(config.projectDir) {
                                testStage(config)
                            }
                        } else {
                            testStage(config)
                        }
                        
                        // After test hook
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
                        // Before quality hook
                        config.beforeQuality?.call()
                        
                        if (config.projectDir) {
                            dir(config.projectDir) {
                                qualityStage(config)
                            }
                        } else {
                            qualityStage(config)
                        }
                        
                        // After quality hook
                        config.afterQuality?.call()
                    }
                }
            }
            
            stage('Docker') {
                when {
                    expression { config.buildDocker == true }
                }
                steps {
                    script {
                        // Before docker hook
                        config.beforeDocker?.call()
                        
                        if (config.projectDir) {
                            dir(config.projectDir) {
                                dockerStage(config)
                            }
                        } else {
                            dockerStage(config)
                        }
                        
                        // After docker hook
                        config.afterDocker?.call()
                    }
                }
            }
            
            // User-defined custom stages
            script {
                if (config.customStages) {
                    config.customStages.each { stageName, stageClosure ->
                        stage(stageName) {
                            steps {
                                script {
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
            }
        }
        
        post {
            success {
                script {
                    echo "Pipeline completed successfully!"
                    
                    // Custom notification provider
                    if (config.notificationProvider && config.notificationConfig) {
                        notificationDispatcher.send(
                            config.notificationProvider,
                            'success',
                            config.notificationConfig
                        )
                    }
                    
                    // Legacy Slack support
                    if (config.slackChannel) {
                        notifySlack.buildSuccess(config)
                    }
                    
                    // Success hook
                    config.onSuccess?.call()
                }
            }
            failure {
                script {
                    echo " Pipeline failed!"
                    
                    // Custom notification provider
                    if (config.notificationProvider && config.notificationConfig) {
                        notificationDispatcher.send(
                            config.notificationProvider,
                            'failure',
                            config.notificationConfig
                        )
                    }
                    
                    // Legacy Slack support
                    if (config.slackChannel) {
                        notifySlack.buildFailure(config)
                    }
                    
                    // Failure hook
                    config.onFailure?.call()
                }
            }
            always {
                script {
                    echo " Pipeline finished"
                    
                    // Always hook
                    config.onAlways?.call()
                }
            }
        }
    }
}
