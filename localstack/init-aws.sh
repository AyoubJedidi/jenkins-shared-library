#!/bin/bash

echo "Initializing LocalStack AWS services..."

# Create ECR repository
awslocal ecr create-repository --repository-name test-repo || true

# Create ECS cluster
awslocal ecs create-cluster --cluster-name test-cluster || true

echo "LocalStack initialization complete!"