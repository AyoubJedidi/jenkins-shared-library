def buildStarted(Map config) {
    if (!config.slackChannel) return
    
    echo "📢 Would notify Slack: Build started"
    // Uncomment when you have Slack configured:
    // slackSend(
    //     channel: config.slackChannel,
    //     color: '#439FE0',
    //     message: "🔵 Build Started: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    // )
}

def buildSuccess(Map config) {
    if (!config.slackChannel) return
    
    echo "📢 Would notify Slack: Build succeeded"
}

def buildFailure(Map config) {
    if (!config.slackChannel) return
    
    echo "📢 Would notify Slack: Build failed"
}
