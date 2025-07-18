@Library('shared-lib') _
import com.devops.java.MvnBuild

def call(Map config = [:]) {
    def constants = constants()
    
    pipeline {
        agent {
            kubernetes {
                // use yaml from resources
                yaml libraryResource('k8s/maven-build-pod.yaml')
                    .replace('maven:3.8-openjdk-11', config.mavenImage ?: constants.DOCKER_MAVEN)
            }
        }
        
        stages {
            stage('Checkout') {
                steps {
                    script {
                        // Ensure we have full git history and proper branch info
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: env.BRANCH_NAME ?: '*/main']],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [
                                [$class: 'CloneOption', 
                                 depth: 0, 
                                 noTags: false, 
                                 reference: '', 
                                 shallow: false],
                                [$class: 'CheckoutOption', timeout: 20],
                                [$class: 'LocalBranch', localBranch: env.BRANCH_NAME ?: 'main']
                            ],
                            submoduleCfg: [],
                            userRemoteConfigs: [[
                                url: env.GIT_URL ?: scm.userRemoteConfigs[0].url,
                                credentialsId: config.gitCredentials
                            ]]
                        ])
                        
                        // Verify git information is available
                        sh '''
                            echo "Git Status:"
                            git status --porcelain
                            echo "Current branch: $(git rev-parse --abbrev-ref HEAD)"
                            echo "Current commit: $(git rev-parse --short=8 HEAD)"
                            echo "Commit message: $(git log -1 --pretty=%B)"
                        '''
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