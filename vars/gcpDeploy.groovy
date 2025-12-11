def call(Map config) {
    def gcpConfig = config.gcp ?: [:]
    def project = gcpConfig.project ?: error("GCP project ID required")
    def zone = gcpConfig.zone ?: 'us-central1-a'
    def cluster = gcpConfig.cluster ?: 'default-cluster'
    def deployment = gcpConfig.deployment ?: config.projectName
    def namespace = gcpConfig.namespace ?: 'default'
    def replicas = gcpConfig.replicas ?: 2
    def port = gcpConfig.port ?: 8080
    
    echo "🟢 Deploying to GCP GKE"
    echo "Project: ${project}"
    echo "Cluster: ${cluster}"
    echo "Deployment: ${deployment}"
    
    withCredentials([
        file(credentialsId: 'gcp-service-account-key', variable: 'GCP_KEY_FILE')
    ]) {
        sh """
            echo "🔐 Authenticating with GCP..."
            gcloud auth activate-service-account --key-file=\$GCP_KEY_FILE || echo "Using default auth"
            gcloud config set project ${project}
            
            # Build image name for GCR
            IMAGE_NAME="gcr.io/${project}/${config.projectName}:latest"
            echo "Image: \$IMAGE_NAME"
            
            echo "🏷️  Tagging image for GCR..."
            docker tag ${config.projectName}:latest \$IMAGE_NAME
            
            echo "📤 Pushing to Google Container Registry..."
            docker push \$IMAGE_NAME
            
            echo "☸️  Getting GKE credentials..."
            gcloud container clusters get-credentials ${cluster} --zone ${zone} || echo "Using existing kubeconfig"
            
            echo "📝 Creating Kubernetes deployment..."
            kubectl create deployment ${deployment} \
              --image=\$IMAGE_NAME \
              --replicas=${replicas} \
              --dry-run=client -o yaml | kubectl apply -f -
            
            echo "🌐 Exposing deployment as LoadBalancer..."
            kubectl expose deployment ${deployment} \
              --type=LoadBalancer \
              --port=80 \
              --target-port=${port} \
              --name=${deployment}-service || echo "Service exists"
            
            echo "⏳ Waiting for external IP..."
            for i in {1..30}; do
                EXTERNAL_IP=\$(kubectl get service ${deployment}-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
                if [ ! -z "\$EXTERNAL_IP" ]; then
                    break
                fi
                echo "Waiting for IP... (\$i/30)"
                sleep 10
            done
            
            echo ""
            echo "======================================"
            echo "✅ GCP GKE DEPLOYMENT COMPLETE"
            echo "======================================"
            echo "Deployment: ${deployment}"
            echo "Namespace: ${namespace}"
            echo "Replicas: ${replicas}"
            if [ ! -z "\$EXTERNAL_IP" ]; then
                echo "URL: http://\$EXTERNAL_IP"
            else
                echo "Getting IP: kubectl get service ${deployment}-service"
            fi
            echo "======================================"
        """
    }
}
