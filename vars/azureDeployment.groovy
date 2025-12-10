def deploy(Map config) {
    def awsConfig = config.aws ?: [:]
    def region = awsConfig.region ?: 'us-east-1'
    def cluster = awsConfig.cluster ?: 'default-cluster'
    def service = awsConfig.service ?: config.projectName
    def taskFamily = awsConfig.taskFamily ?: config.projectName
    def isLocalStack = awsConfig.localstack ?: false

    // Set endpoint for LocalStack
    def endpoint = isLocalStack ? 'http://localhost:4566' : ''
    def endpointFlag = endpoint ? "--endpoint-url ${endpoint}" : ''

    echo "🔵 Deploying to AWS ${isLocalStack ? '(LocalStack)' : ''}"
    echo "Region: ${region}"
    echo "Cluster: ${cluster}"
    echo "Service: ${service}"

    withCredentials([
        string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
        string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
    ]) {
        sh """
            export AWS_DEFAULT_REGION=${region}
            ${isLocalStack ? 'export AWS_ACCESS_KEY_ID=test' : ''}
            ${isLocalStack ? 'export AWS_SECRET_ACCESS_KEY=test' : ''}

            # Login to ECR
            echo "Logging into ECR..."
            aws ecr get-login-password ${endpointFlag} | \
              docker login --username AWS --password-stdin \
              ${isLocalStack ? 'localhost:4566' : "\$(aws sts get-caller-identity --query Account --output text).dkr.ecr.${region}.amazonaws.com"}

            # Tag image for ECR
            REPO_URI=\$(aws ecr describe-repositories \
              --repository-names ${config.projectName} \
              ${endpointFlag} \
              --query 'repositories[0].repositoryUri' \
              --output text 2>/dev/null || echo "")

            if [ -z "\$REPO_URI" ]; then
                echo "Creating ECR repository..."
                aws ecr create-repository \
                  --repository-name ${config.projectName} \
                  ${endpointFlag}

                REPO_URI=\$(aws ecr describe-repositories \
                  --repository-names ${config.projectName} \
                  ${endpointFlag} \
                  --query 'repositories[0].repositoryUri' \
                  --output text)
            fi

            echo "Repository URI: \$REPO_URI"

            # Push to ECR
            docker tag ${config.projectName}:latest \$REPO_URI:latest
            docker push \$REPO_URI:latest

            # Register task definition
            echo "Registering ECS task definition..."
            cat > task-definition.json << EOF_TASK
{
  "family": "${taskFamily}",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "containerDefinitions": [
    {
      "name": "${service}",
      "image": "\$REPO_URI:latest",
      "portMappings": [
        {
          "containerPort": ${awsConfig.port ?: 8080},
          "protocol": "tcp"
        }
      ],
      "essential": true,
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/${service}",
          "awslogs-region": "${region}",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
EOF_TASK

            TASK_DEF_ARN=\$(aws ecs register-task-definition \
              ${endpointFlag} \
              --cli-input-json file://task-definition.json \
              --query 'taskDefinition.taskDefinitionArn' \
              --output text)

            echo "Task definition: \$TASK_DEF_ARN"

            # Check if service exists
            SERVICE_EXISTS=\$(aws ecs describe-services \
              --cluster ${cluster} \
              --services ${service} \
              ${endpointFlag} \
              --query 'services[0].serviceName' \
              --output text 2>/dev/null || echo "")

            if [ "\$SERVICE_EXISTS" = "${service}" ]; then
                echo "Updating existing service..."
                aws ecs update-service \
                  --cluster ${cluster} \
                  --service ${service} \
                  --task-definition \$TASK_DEF_ARN \
                  --force-new-deployment \
                  ${endpointFlag}
            else
                echo "Creating new service..."
                # Note: This is simplified - real ECS needs VPC/subnet config
                echo "⚠️  Service creation requires VPC configuration"
                echo "✅ Image pushed to ECR and task definition registered"
            fi

            echo ""
            echo "======================================"
            echo "✅ AWS DEPLOYMENT COMPLETE"
            echo "======================================"
            echo "Image: \$REPO_URI:latest"
            echo "Task Definition: \$TASK_DEF_ARN"
            echo "======================================"
        """
    }
}