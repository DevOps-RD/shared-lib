@Library('shared-lib') _
import com.devops.java.MvnBuild
import com.devops.security.JavaScan

def call(Map config = [:]) {
    def constants = constants()
    
    pipeline {
        agent {
            kubernetes {
                // use yaml from resources
                yaml libraryResource('k8s/maven-build-pod.yaml')
                    .replace('__MAVEN_IMAGE__', config.mavenImage ?: constants.DOCKER_MAVEN)
            }
        }
        
        stages {
            stage('Checkout') {
                steps {
                    script {
                        git.checkout(config)
                    }
                }
            }
            
            stage('Build') {
                steps {
                    container('maven') {
                        script {
                            def mvn = new MvnBuild(this)
                            mvn.fullBuild([
                                skipTests: config.skipTests ?: false,
                                runSonar: config.runSonar ?: false,
                                runSecurity: config.runSecurity ?: false,
                                sonarUrl: config.sonarUrl,
                                sonarToken: config.sonarToken,
                                projectKey: config.projectKey
                            ])
                        }
                    }
                }
            }
            
            stage('Security Scan') {
                when {
                    expression { config.runTrivyScan != false }
                }
                steps {
                    container('trivy') {
                        script {
                            try {
                                def scanConfig = [
                                    format: config.trivyFormat ?: 'json',
                                    outputFile: config.trivyOutputFile ?: 'trivy-maven-scan.json',
                                    severity: config.trivySeverity ?: 'HIGH,CRITICAL',
                                    exitCode: config.trivyExitCode ?: 1,
                                    pomPath: config.pomPath ?: '.'
                                ]
                                
                                def scanResult = JavaScan.scanMaven(scanConfig)
                                echo "Running Trivy security scan: ${scanResult.command}"
                                
                                def exitCode = sh(
                                    script: scanResult.command,
                                    returnStatus: true
                                )
                                
                                // Archive scan results
                                if (fileExists(scanResult.outputFile)) {
                                    archiveArtifacts artifacts: scanResult.outputFile, allowEmptyArchive: true
                                    echo "Trivy scan results archived: ${scanResult.outputFile}"
                                }
                                
                                // Handle scan results
                                if (exitCode != 0) {
                                    echo "Trivy scan found vulnerabilities (exit code: ${exitCode})"
                                    if (config.failOnVulnerabilities != false) {
                                        error("Security vulnerabilities detected by Trivy scan")
                                    }
                                } else {
                                    echo "Trivy scan completed successfully - no vulnerabilities found"
                                }
                                
                            } catch (Exception e) {
                                echo "Trivy scan failed: ${e.getMessage()}"
                                if (config.failOnSecurityError != false) {
                                    error("Trivy security scan execution failed: ${e.getMessage()}")
                                }
                            }
                        }
                    }
                }
            }
            
            stage('Docker Build') {
                when {
                    expression { config.buildDocker == true }
                }
                steps {
                    container('docker') {
                        script {
                            def mvn = new MvnBuild(this)
                            def artifactId = mvn.getArtifactId()
                            def tags = mvn.generateDockerTag(config.useLatest ?: true)
                            def registry = config.dockerRegistry
                            
                            // Build metadata for labels
                            def metadata = mvn.getBuildMetadata()
                            
                            // Build Docker image with labels
                            def buildArgs = [
                                "--label", "version=${metadata.version}",
                                "--label", "commit=${metadata.commitHash}",
                                "--label", "branch=${metadata.branch}",
                                "--label", "build-date=${metadata.buildTimestamp}",
                                "--label", "author=${metadata.commitAuthor}"
                            ].join(' ')
                            
                            // Build and tag with all generated tags
                            def primaryTag = "${registry}/${artifactId}:${tags[0]}"
                            sh "docker build ${buildArgs} -t ${primaryTag} ."
                            
                            // Tag with additional tags
                            tags.drop(1).each { tag ->
                                sh "docker tag ${primaryTag} ${registry}/${artifactId}:${tag}"
                            }
                            
                            // Push all tags
                            tags.each { tag ->
                                sh "docker push ${registry}/${artifactId}:${tag}"
                            }
                            
                            // Store image info for later use
                            env.DOCKER_IMAGE = "${registry}/${artifactId}"
                            env.DOCKER_TAGS = tags.join(',')
                            
                            echo "Docker images built and pushed:"
                            tags.each { tag ->
                                echo "  ${registry}/${artifactId}:${tag}"
                            }
                        }
                    }
                }
            }
        }
        
        post {
            always {
                script {
                    if (config.notifications) {
                        def constants = constants()
                        def utils = new com.devops.Utils()
                        utils.notifySlack(
                            "Maven build ${currentBuild.result ?: 'SUCCESS'}", 
                            constants.SLACK_CHANNEL_BUILDS
                        )
                    }
                }
            }
        }
    }
}