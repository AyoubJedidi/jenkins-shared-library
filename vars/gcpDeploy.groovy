def call(Map config) {
    def gcpConfig = config.gcp ?: [:]
    def project = gcpConfig.project ?: error("GCP project ID required")
    def region = gcpConfig.region ?: 'us-central1'
    def service = gcpConfig.service ?: config.projectName
    def port = gcpConfig.port ?: 8080
    
    echo "🟢 Deploying to GCP Cloud Run"
    echo "Project: ${project}"
    echo "Region: ${region}"
    echo "Service: ${service}"
    
    withCredentials([
        file(credentialsId: 'gcp-service-account-key', variable: 'GCP_KEY_FILE')
    ]) {
        sh """
            echo "🔐 Authenticating with GCP..."
            gcloud auth activate-service-account --key-file=\$GCP_KEY_FILE
            gcloud config set project ${project}
            
            # Build image name for GCR
            IMAGE_NAME="gcr.io/${project}/${config.projectName}:latest"
            echo "Image: \$IMAGE_NAME"
            
            echo "🏷️  Tagging image for GCR..."
            docker tag ${config.projectName}:latest \$IMAGE_NAME
            
            echo "🔐 Configuring Docker for GCR..."
            gcloud auth configure-docker --quiet
            
            echo "📤 Pushing to Google Container Registry..."
            docker push \$IMAGE_NAME
            
            echo "🚀 Deploying to Cloud Run..."
            gcloud run deploy ${service} \
              --image \$IMAGE_NAME \
              --region ${region} \
              --platform managed \
              --allow-unauthenticated \
              --port ${port} \
              --memory ${gcpConfig.memory ?: '512Mi'} \
              --cpu ${gcpConfig.cpu ?: 1} \
              --timeout ${gcpConfig.timeout ?: '300'} \
              --max-instances ${gcpConfig.maxInstances ?: 10}
            
            echo "📋 Getting service URL..."
            SERVICE_URL=\$(gcloud run services describe ${service} \
              --region ${region} \
              --format='value(status.url)')
            
            echo ""
            echo "======================================"
            echo "✅ GCP DEPLOYMENT COMPLETE"
            echo "======================================"
            echo "Service: ${service}"
            echo "Region: ${region}"
            echo "URL: \$SERVICE_URL"
            echo "======================================"
        """
    }
}
