def call(Map config) {
    def gcpConfig = config.gcp ?: [:]
    def project = gcpConfig.project ?: error("GCP project ID required")
    
    echo "🟢 Deploying to GCP Container Registry"
    echo "Project: ${project}"
    
    sh """
        # Build image name for GCR
        IMAGE_NAME="gcr.io/${project}/${config.projectName}:latest"
        echo "Image: \$IMAGE_NAME"
        
        echo "🏷️  Tagging image for GCR..."
        docker tag ${config.projectName}:latest \$IMAGE_NAME
        
        echo "📤 Pushing to Google Container Registry..."
        docker push \$IMAGE_NAME
        
        echo ""
        echo "======================================"
        echo "✅ GCP GCR PUSH COMPLETE"
        echo "======================================"
        echo "Image: \$IMAGE_NAME"
        echo ""
        echo "To deploy:"
        echo "  kubectl run ${config.projectName} --image=\$IMAGE_NAME"
        echo "  gcloud run deploy --image=\$IMAGE_NAME"
        echo "======================================"
    """
}
