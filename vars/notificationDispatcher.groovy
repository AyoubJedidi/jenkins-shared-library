// Registry for notification providers
class NotificationProviders {
    static providers = [:]
    
    static void register(String name, Closure handler) {
        providers[name] = handler
    }
    
    static Closure get(String name) {
        return providers[name]
    }
}

def send(String providerName, String status, Map config) {
    def provider = NotificationProviders.get(providerName)
    
    if (provider) {
        echo "📢 Sending ${status} notification via ${providerName}"
        provider(status, config)
    } else {
        echo "⚠️  Notification provider '${providerName}' not registered"
    }
}

// Built-in Slack provider
def registerSlack() {
    NotificationProviders.register('slack') { status, config ->
        def color = status == 'success' ? 'good' : 'danger'
        def emoji = status == 'success' ? '✅' : '❌'
        
        echo "📢 Slack: ${emoji} Build ${status}"
        
        // Uncomment when Slack plugin configured:
        /*
        slackSend(
            channel: config.channel,
            color: color,
            message: "${emoji} Build ${status}: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
        )
        */
    }
}

// Built-in Teams provider
def registerTeams() {
    NotificationProviders.register('teams') { status, config ->
        def emoji = status == 'success' ? '✅' : '❌'
        
        echo "📢 Teams: ${emoji} Build ${status}"
        
        // Uncomment when httpRequest plugin available:
        /*
        httpRequest(
            url: config.webhook,
            httpMode: 'POST',
            contentType: 'APPLICATION_JSON',
            requestBody: """
            {
                "text": "${emoji} Build ${status}",
                "title": "${env.JOB_NAME} #${env.BUILD_NUMBER}"
            }
            """
        )
        */
    }
}

// Built-in Email provider
def registerEmail() {
    NotificationProviders.register('email') { status, config ->
        def subject = status == 'success' ? '✅ Build Success' : '❌ Build Failed'
        
        echo "📢 Email: ${subject}"
        
        // Uncomment when email configured:
        /*
        emailext(
            to: config.recipients,
            subject: "${subject}: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            body: "Build ${status}: ${env.BUILD_URL}"
        )
        */
    }
}

// Auto-register built-in providers
registerSlack()
registerTeams()
registerEmail()
