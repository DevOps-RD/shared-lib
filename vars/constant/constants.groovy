class Constants {
    // Docker Images
    static final String DOCKER_NODE_16 = 'node:16-alpine'
    static final String DOCKER_NODE_18 = 'node:18-alpine'
    static final String DOCKER_NODE_20 = 'node:20-alpine'
    static final String DOCKER_PYTHON_39 = 'python:3.9-slim'
    static final String DOCKER_PYTHON_311 = 'python:3.11-slim'
    static final String DOCKER_JAVA_11 = 'openjdk:11-jdk-slim'
    static final String DOCKER_JAVA_17 = 'openjdk:17-jdk-slim'
    static final String DOCKER_MAVEN = 'maven:3.8-openjdk-11'
    static final String DOCKER_GRADLE = 'gradle:7.6-jdk11'
    static final String DOCKER_NGINX = 'nginx:alpine'
    static final String DOCKER_POSTGRES = 'postgres:14-alpine'
    static final String DOCKER_REDIS = 'redis:7-alpine'
    static final String DOCKER_ALPINE = 'alpine:latest'
    static final String DOCKER_UBUNTU = 'ubuntu:20.04'
    
    // Build Tools
    static final String DOCKER_SONARQUBE = 'sonarqube:community'
    static final String DOCKER_TRIVY = 'aquasec/trivy:latest'
    static final String DOCKER_HADOLINT = 'hadolint/hadolint:latest'
    
    // CI/CD Tools
    static final String DOCKER_HELM = 'alpine/helm:latest'
    static final String DOCKER_KUBECTL = 'bitnami/kubectl:latest'
    static final String DOCKER_TERRAFORM = 'hashicorp/terraform:latest'
    static final String DOCKER_ANSIBLE = 'ansible/ansible:latest'
    
    // Common Timeouts (in minutes)
    static final int TIMEOUT_BUILD = 30
    static final int TIMEOUT_TEST = 15
    static final int TIMEOUT_DEPLOY = 45
    static final int TIMEOUT_SCAN = 20
    
    // Environment Names
    static final String ENV_DEV = 'development'
    static final String ENV_STAGING = 'staging'
    static final String ENV_PROD = 'production'
    
    // Notification Channels
    static final String SLACK_CHANNEL_BUILDS = '#builds'
    static final String SLACK_CHANNEL_DEPLOYMENTS = '#deployments'
    static final String SLACK_CHANNEL_ALERTS = '#alerts'
    
    // Common Ports
    static final int PORT_HTTP = 80
    static final int PORT_HTTPS = 443
    static final int PORT_SSH = 22
    static final int PORT_POSTGRES = 5432
    static final int PORT_REDIS = 6379
    static final int PORT_MYSQL = 3306
}

def call() {
    return new Constants()
}