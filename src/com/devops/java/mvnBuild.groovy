package com.devops.java

import com.devops.Utils

class MvnBuild {
    
    def script
    def mavenHome
    def javaHome
    
    MvnBuild(script, String mavenHome = null, String javaHome = null) {
        this.script = script
        this.mavenHome = mavenHome ?: (isKubernetes() ? '/usr/share/maven' : '/usr/share/maven')
        this.javaHome = javaHome ?: (isKubernetes() ? '/opt/java/openjdk' : '/usr/lib/jvm/java-11-openjdk')
    }
    
    /**
     * Check if running in Kubernetes environment
     */
    private boolean isKubernetes() {
        return script.env.KUBERNETES_SERVICE_HOST != null
    }
    
    /**
     * Execute Maven clean
     */
    def clean() {
        script.echo "Cleaning Maven project..."
        executeMavenCommand('clean')
    }
    
    /**
     * Execute Maven compile
     */
    def compile() {
        script.echo "Compiling Maven project..."
        executeMavenCommand('compile')
    }
    
    /**
     * Execute Maven test
     * @param skipTests - Skip test execution
     * @param testFailureIgnore - Ignore test failures
     */
    def test(boolean skipTests = false, boolean testFailureIgnore = false) {
        if (skipTests) {
            script.echo "Skipping tests..."
            return
        }
        
        script.echo "Running Maven tests..."
        def params = []
        if (testFailureIgnore) {
            params.add('-Dmaven.test.failure.ignore=true')
        }
        executeMavenCommand('test', params)
    }
    
    /**
     * Execute Maven package
     * @param skipTests - Skip test execution during packaging
     */
    def package(boolean skipTests = false) {
        script.echo "Packaging Maven project..."
        def params = []
        if (skipTests) {
            params.add('-DskipTests')
        }
        executeMavenCommand('package', params)
    }
    
    /**
     * Execute Maven install
     * @param skipTests - Skip test execution during install
     */
    def install(boolean skipTests = false) {
        script.echo "Installing Maven project..."
        def params = []
        if (skipTests) {
            params.add('-DskipTests')
        }
        executeMavenCommand('install', params)
    }
    
    /**
     * Execute Maven deploy
     * @param skipTests - Skip test execution during deploy
     */
    def deploy(boolean skipTests = false) {
        script.echo "Deploying Maven project..."
        def params = []
        if (skipTests) {
            params.add('-DskipTests')
        }
        executeMavenCommand('deploy', params)
    }
    
    /**
     * Execute Maven verify with quality checks
     */
    def verify() {
        script.echo "Running Maven verify..."
        executeMavenCommand('verify')
    }
    
    /**
     * Run SonarQube analysis
     * @param sonarUrl - SonarQube server URL
     * @param sonarToken - SonarQube authentication token
     * @param projectKey - SonarQube project key
     */
    def sonarAnalysis(String sonarUrl, String sonarToken, String projectKey) {
        script.echo "Running SonarQube analysis..."
        def params = [
            "-Dsonar.host.url=${sonarUrl}",
            "-Dsonar.login=${sonarToken}",
            "-Dsonar.projectKey=${projectKey}"
        ]
        executeMavenCommand('sonar:sonar', params)
    }
    
    /**
     * Generate site documentation
     */
    def site() {
        script.echo "Generating Maven site..."
        executeMavenCommand('site')
    }
    
    /**
     * Execute dependency check
     */
    def dependencyCheck() {
        script.echo "Running dependency check..."
        executeMavenCommand('dependency:analyze')
    }
    
    /**
     * Execute security vulnerability scan
     */
    def securityScan() {
        script.echo "Running security vulnerability scan..."
        executeMavenCommand('org.owasp:dependency-check-maven:check')
    }
    
    /**
     * Full build pipeline
     * @param options - Build options map
     */
    def fullBuild(Map options = [:]) {
        def skipTests = options.skipTests ?: false
        def runSonar = options.runSonar ?: false
        def runSecurity = options.runSecurity ?: false
        
        script.echo "Starting full Maven build pipeline..."
        
        clean()
        compile()
        
        if (!skipTests) {
            test()
        }
        
        package(skipTests)
        
        if (runSecurity) {
            securityScan()
        }
        
        if (runSonar && options.sonarUrl && options.sonarToken && options.projectKey) {
            sonarAnalysis(options.sonarUrl, options.sonarToken, options.projectKey)
        }
        
        verify()
        
        script.echo "Maven build pipeline completed successfully!"
    }
    
    /**
     * Execute custom Maven command
     * @param goals - Maven goals to execute
     * @param params - Additional parameters
     */
    def executeMavenCommand(String goals, List<String> params = []) {
        def timestamp = Utils.getCurrentTimestamp()
        script.echo "Executing Maven command at ${timestamp}: ${goals}"
        
        def command = "mvn ${goals}"
        if (params) {
            command += " ${params.join(' ')}"
        }
        
        if (isKubernetes()) {
            // In Kubernetes, Maven containers typically have mvn in PATH
            script.sh command
        } else {
            // Traditional agent setup
            script.withEnv([
                "MAVEN_HOME=${mavenHome}",
                "JAVA_HOME=${javaHome}",
                "PATH+MAVEN=${mavenHome}/bin",
                "PATH+JAVA=${javaHome}/bin"
            ]) {
                script.sh command
            }
        }
    }
    
    /**
     * Set Maven settings file
     * @param settingsFile - Path to Maven settings.xml
     */
    def setSettingsFile(String settingsFile) {
        script.echo "Using Maven settings file: ${settingsFile}"
        this.settingsFile = settingsFile
    }
    
    /**
     * Get project version from pom.xml
     */
    def getProjectVersion() {
        def version = script.sh(
            script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
            returnStdout: true
        ).trim()
        script.echo "Project version: ${version}"
        return version
    }
    
    /**
     * Get project artifact ID from pom.xml
     */
    def getArtifactId() {
        def artifactId = script.sh(
            script: "mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout",
            returnStdout: true
        ).trim()
        script.echo "Artifact ID: ${artifactId}"
        return artifactId
    }
    
    /**
     * Get Git commit hash (8 characters)
     */
    def getCommitHash() {
        def commitHash = script.sh(
            script: "git rev-parse --short=8 HEAD",
            returnStdout: true
        ).trim()
        script.echo "Commit hash: ${commitHash}"
        return commitHash
    }
    
    /**
     * Get Git branch name
     */
    def getBranch() {
        def branch = script.sh(
            script: "git rev-parse --abbrev-ref HEAD",
            returnStdout: true
        ).trim()
        script.echo "Branch: ${branch}"
        return branch
    }
    
    /**
     * Get Git commit message
     */
    def getCommitMessage() {
        def message = script.sh(
            script: "git log -1 --pretty=%B",
            returnStdout: true
        ).trim()
        script.echo "Commit message: ${message}"
        return message
    }
    
    /**
     * Get Git commit author
     */
    def getCommitAuthor() {
        def author = script.sh(
            script: "git log -1 --pretty=%an",
            returnStdout: true
        ).trim()
        script.echo "Commit author: ${author}"
        return author
    }
    
    /**
     * Get Git commit timestamp
     */
    def getCommitTimestamp() {
        def timestamp = script.sh(
            script: "git log -1 --pretty=%ct",
            returnStdout: true
        ).trim()
        script.echo "Commit timestamp: ${timestamp}"
        return timestamp
    }
    
    /**
     * Generate Docker tag based on Git info
     * @param useLatest - Add 'latest' tag for main/master branch
     */
    def generateDockerTag(boolean useLatest = true) {
        def version = getProjectVersion()
        def commitHash = getCommitHash()
        def branch = getBranch()
        
        def tags = []
        
        // Version-commit tag
        tags.add("${version}-${commitHash}")
        
        // Branch-commit tag
        if (branch != 'main' && branch != 'master') {
            def safeBranch = branch.replaceAll(/[^a-zA-Z0-9._-]/, '-')
            tags.add("${safeBranch}-${commitHash}")
        }
        
        // Latest tag for main/master
        if (useLatest && (branch == 'main' || branch == 'master')) {
            tags.add('latest')
        }
        
        script.echo "Generated Docker tags: ${tags.join(', ')}"
        return tags
    }
    
    /**
     * Get comprehensive build metadata
     */
    def getBuildMetadata() {
        def metadata = [
            version: getProjectVersion(),
            artifactId: getArtifactId(),
            commitHash: getCommitHash(),
            branch: getBranch(),
            commitMessage: getCommitMessage(),
            commitAuthor: getCommitAuthor(),
            commitTimestamp: getCommitTimestamp(),
            buildTimestamp: Utils.getCurrentTimestamp()
        ]
        
        script.echo "Build metadata: ${metadata}"
        return metadata
    }
}