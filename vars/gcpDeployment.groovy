def call(Map config) {
    def gcpConfig = config.gcp ?: [:]
    def project = gcpConfig.project ?: error("GCP project ID required")
    def region = gcpConfig.region ?: 'us-central1'
    def service = gcpConfig.service ?: config.projectName

    echo "🟢 Deploying to GCP Cloud Run"

    withCredentials([
        file(credentialsId: 'gcp-service-account-key', variable: 'GCP_KEY')
    ]) {
        sh """
            # Authenticate
            gcloud auth activate-service-account --key-file=\$GCP_KEY
            gcloud config set project ${project}

            # Tag for GCR
            docker tag ${config.projectName}:latest gcr.io/${project}/${config.projectName}:latest

            # Push to GCR
            docker push gcr.io/${project}/${config.projectName}:latest

            # Deploy to Cloud Run
            gcloud run deploy ${service} \
              --image gcr.io/${project}/${config.projectName}:latest \
              --region ${region} \
              --platform managed \
              --allow-unauthenticated \
              --port ${gcpConfig.port ?: 8080}

            # Get URL
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