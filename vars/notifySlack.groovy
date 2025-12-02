def buildStarted(Map config) {
    if (!config.slackChannel) {
        echo "📢 Slack notifications disabled (no slackChannel configured)"
        return
    }
    
    echo "📢 Slack: Build started notification"
    
    // Uncomment when Slack plugin is installed and configured:
    /*
    slackSend(
        channel: config.slackChannel,
        color: '#439FE0',
        message: """
            🔵 *Build Started*
            Job: ${env.JOB_NAME}
            Build: #${env.BUILD_NUMBER}
            Branch: ${env.BRANCH_NAME ?: 'N/A'}
            <${env.BUILD_URL}|View Build>
        """.stripIndent()
    )
    */
}

def buildSuccess(Map config) {
    if (!config.slackChannel) return
    
    def duration = currentBuild.durationString.replace(' and counting', '')
    
    echo "Slack: Build success notification (${duration})"
    
    // Uncomment when Slack plugin is installed:
    /*
    slackSend(
        channel: config.slackChannel,
        color: 'good',
        message: """
             *Build Successful*
            Job: ${env.JOB_NAME}
            Build: #${env.BUILD_NUMBER}
            Duration: ${duration}
            <${env.BUILD_URL}|View Build>
        """.stripIndent()
    )
    */
}

def buildFailure(Map config) {
    if (!config.slackChannel) return
    
    echo " Slack: Build failure notification"
    
    // Uncomment when Slack plugin is installed:
    /*
    slackSend(
        channel: config.slackChannel,
        color: 'danger',
        message: """
             *Build Failed*
            Job: ${env.JOB_NAME}
            Build: #${env.BUILD_NUMBER}
            Branch: ${env.BRANCH_NAME ?: 'N/A'}
            <${env.BUILD_URL}console|View Logs>
        """.stripIndent()
    )
    */
}

def testsFailed(Map config, int failedCount) {
    if (!config.slackChannel) return
    
    echo " Slack: Tests failed notification (${failedCount} failures)"
    
    // Uncomment when Slack plugin is installed:
    /*
    slackSend(
        channel: config.slackChannel,
        color: 'warning',
        message: """
            ️ *Tests Failed*
            Job: ${env.JOB_NAME}
            Failed Tests: ${failedCount}
            <${env.BUILD_URL}testReport|View Test Report>
        """.stripIndent()
    )
    */
}

def custom(String message, Map config) {
    if (!config.slackChannel) return
    
    echo " Slack: ${message}"
    
    // Uncomment when Slack plugin is installed:
    /*
    slackSend(
        channel: config.slackChannel,
        message: message
    )
    */
}
