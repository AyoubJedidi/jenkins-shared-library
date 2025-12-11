def call(Map config) {
    def awsConfig = config.aws ?: [:]
    def region = awsConfig.region ?: 'us-east-1'
    def isLocalStack = awsConfig.localstack ?: false
    
    def endpoint = isLocalStack ? 'http://localhost:4566' : ''
    def endpointFlag = endpoint ? "--endpoint-url ${endpoint}" : ''
    
    echo "🔵 Deploying to AWS ${isLocalStack ? '(LocalStack)' : ''}"
    echo "Region: ${region}"
    
    withCredentials([
        string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
        string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
    ]) {
        sh """
            export AWS_DEFAULT_REGION=${region}
            export AWS_ACCESS_KEY_ID=test
            export AWS_SECRET_ACCESS_KEY=test
            
            echo "📦 Creating ECR repository..."
            aws ecr create-repository \
              --repository-name ${config.projectName} \
              --region ${region} \
              ${endpointFlag} || echo "Repository exists"
            
            echo "📋 Getting repository URI..."
            REPO_URI=\$(aws ecr describe-repositories \
              --repository-names ${config.projectName} \
              --region ${region} \
              ${endpointFlag} \
              --query 'repositories[0].repositoryUri' \
              --output text)
            
            echo "Repository: \$REPO_URI"
            
            # Extract registry domain from URI
            REGISTRY_DOMAIN=\${REPO_URI%%/*}
            echo "Registry Domain: \$REGISTRY_DOMAIN"
            
            echo "🔐 Logging into ECR..."
            aws ecr get-login-password \
              --region ${region} \
              ${endpointFlag} | \
              docker login --username AWS --password-stdin \$REGISTRY_DOMAIN
            
            echo "🏷️  Tagging image..."
            docker tag ${config.projectName}:latest \$REPO_URI:latest
            
            echo "📤 Pushing to ECR..."
            docker push \$REPO_URI:latest
            
            echo "📝 Registering ECS task definition..."
            cat > task-def.json << 'EOFTASK'
{
  "family": "${config.projectName}",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "containerDefinitions": [{
    "name": "${config.projectName}",
    "image": "\$REPO_URI:latest",
    "portMappings": [{
      "containerPort": ${awsConfig.port ?: 8080},
      "protocol": "tcp"
    }],
    "essential": true
  }]
}
EOFTASK
            
            TASK_ARN=\$(aws ecs register-task-definition \
              ${endpointFlag} \
              --region ${region} \
              --cli-input-json file://task-def.json \
              --query 'taskDefinition.taskDefinitionArn' \
              --output text)
            
            echo "Task Definition: \$TASK_ARN"
            
            echo ""
            echo "======================================"
            echo "✅ AWS DEPLOYMENT COMPLETE"
            echo "======================================"
            echo "Image: \$REPO_URI:latest"
            echo "Task: \$TASK_ARN"
            echo "======================================"
        """
    }
}
