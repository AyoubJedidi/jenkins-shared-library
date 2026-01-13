def call(Map config) {
    def terraformConfig = config.terraform ?: [:]
    def cloudProvider = terraformConfig.cloudProvider ?: 'azure'
    def deploymentType = terraformConfig.deploymentType ?: 'webapp'
    def environment = terraformConfig.environment ?: 'dev'
    
    echo "🏗️ Terraform Deployment"
    echo "Cloud: ${cloudProvider}"
    echo "Type: ${deploymentType}"
    echo "Environment: ${environment}"
    
    withCredentials([
        usernamePassword(
            credentialsId: 'azure-acr-france',
            usernameVariable: 'REGISTRY_USER',
            passwordVariable: 'REGISTRY_PASS'
        )
    ]) {
        sh """
            # Clone Terraform repo (if not exists)
            if [ ! -d "terraform-multicloud" ]; then
                git clone https://github.com/AyoubJedidi/Terraform-Multicloud.git terraform-multicloud
            fi
            
            cd terraform-multicloud/parent
            
            # Initialize Terraform
            terraform init
            
            # Export variables (instead of terraform.tfvars)
            export TF_VAR_project_name="${config.projectName}"
            export TF_VAR_environment="${environment}"
            export TF_VAR_cloud_provider="${cloudProvider}"
            export TF_VAR_deployment_type="${deploymentType}"
            export TF_VAR_azure_resource_group="${terraformConfig.resourceGroup}"
            export TF_VAR_azure_location="${terraformConfig.location ?: 'francecentral'}"
            export TF_VAR_docker_image="${config.dockerRegistry}/${config.projectName}:latest"
            export TF_VAR_registry_server="${config.dockerRegistry}"
            export TF_VAR_registry_username="\$REGISTRY_USER"
            export TF_VAR_registry_password="\$REGISTRY_PASS"
            export TF_VAR_gcp_project="dummy-project"
            
            # Plan
            terraform plan -out=tfplan
            
            # Apply
            terraform apply -auto-approve tfplan
            
            # Get outputs
            echo ""
            echo "======================================"
            echo "✅ TERRAFORM DEPLOYMENT COMPLETE"
            echo "======================================"
            DEPLOYMENT_URL=\$(terraform output -raw deployment_url || echo "N/A")
            echo "Application URL: \$DEPLOYMENT_URL"
            echo "======================================"
            
            # Save outputs for next stages
            terraform output -json > ../../deployment-outputs.json
        """
    }
}
