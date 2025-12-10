def call(Map config) {
    def gcpConfig = config.gcp ?: [:]
    def project = gcpConfig.project ?: error("GCP project ID required")
    def region = gcpConfig.region ?: 'us-central1'
    def service = gcpConfig.service ?: config.projectName
    
    echo "🟢 Deploying to GCP Cloud Run"
    echo "Project: ${project}"
    echo "Region: ${region}"
    echo "Service: ${service}"
    
    withCredentials([
        file(credentialsId: 'gcp-service-account-key', variable: 'GCP_KEY')
    ]) {
        sh """
            gcloud auth activate-service-account --key-file=\$GCP_KEY
            gcloud config set project ${project}
            
            docker tag ${config.projectName}:latest gcr.io/${project}/${config.projectName}:latest
            docker push gcr.io/${project}/${config.projectName}:latest
            
            gcloud run deploy ${service} \
              --image gcr.io/${project}/${config.projectName}:latest \
              --region ${region} \
              --platform managed \
              --allow-unauthenticated \
              --port ${gcpConfig.port ?: 8080}
            
            SERVICE_URL=\$(gcloud run services describe ${service} \
              --region ${region} \
              --format 'value(status.url)')
            
            echo ""
            echo "======================================"
            echo "✅ GCP DEPLOYMENT COMPLETE"
            echo "======================================"
            echo "URL: \$SERVICE_URL"
            echo "======================================"
        """
    }
}
