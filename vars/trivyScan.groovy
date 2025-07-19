@Library('shared-lib') _
import com.devops.security.JavaScan
import com.devops.security.FrontendScan

def call(Map config = [:]) {
    def scanType = config.type ?: 'auto'
    def projectPath = config.path ?: '.'
    def archiveResults = config.archive ?: true
    
    echo "Starting Trivy security scan..."
    
    try {
        def scanResult
        
        switch(scanType) {
            case 'java':
            case 'maven':
                scanResult = JavaScan.scanMaven(config)
                break
            case 'gradle':
                scanResult = JavaScan.scanGradle(config)
                break
            case 'frontend':
            case 'npm':
                scanResult = FrontendScan.scanNpm(config)
                break
            case 'yarn':
                scanResult = FrontendScan.scanYarn(config)
                break
            case 'pnpm':
                scanResult = FrontendScan.scanPnpm(config)
                break
            case 'auto':
                scanResult = autoDetectAndScan(config)
                break
            default:
                error("Unsupported scan type: ${scanType}")
        }
        
        echo "Executing Trivy scan: ${scanResult.command}"
        
        def exitCode = sh(
            script: scanResult.command,
            returnStatus: true
        )
        
        if (archiveResults && fileExists(scanResult.outputFile)) {
            archiveArtifacts artifacts: scanResult.outputFile, allowEmptyArchive: true
            echo "Scan results archived: ${scanResult.outputFile}"
        }
        
        if (exitCode != 0) {
            echo "Trivy scan found vulnerabilities (exit code: ${exitCode})"
            if (config.failOnVulnerabilities != false) {
                error("Security vulnerabilities detected by Trivy scan")
            }
        } else {
            echo "Trivy scan completed successfully - no vulnerabilities found"
        }
        
        return [
            exitCode: exitCode,
            outputFile: scanResult.outputFile,
            scanType: scanResult.scanType
        ]
        
    } catch (Exception e) {
        echo "Trivy scan failed: ${e.getMessage()}"
        if (config.failOnError != false) {
            error("Trivy scan execution failed: ${e.getMessage()}")
        }
        return [exitCode: -1, error: e.getMessage()]
    }
}

def autoDetectAndScan(Map config) {
    def projectPath = config.path ?: '.'
    
    if (fileExists("${projectPath}/pom.xml")) {
        echo "Detected Maven project, running Maven scan..."
        return JavaScan.scanMaven(config)
    } else if (fileExists("${projectPath}/build.gradle") || fileExists("${projectPath}/build.gradle.kts")) {
        echo "Detected Gradle project, running Gradle scan..."
        return JavaScan.scanGradle(config)
    } else if (fileExists("${projectPath}/yarn.lock")) {
        echo "Detected Yarn project, running Yarn scan..."
        return FrontendScan.scanYarn(config)
    } else if (fileExists("${projectPath}/pnpm-lock.yaml")) {
        echo "Detected pnpm project, running pnpm scan..."
        return FrontendScan.scanPnpm(config)
    } else if (fileExists("${projectPath}/package.json")) {
        echo "Detected npm project, running npm scan..."
        return FrontendScan.scanNpm(config)
    } else {
        error("Could not auto-detect project type. Please specify the scan type explicitly.")
    }
}