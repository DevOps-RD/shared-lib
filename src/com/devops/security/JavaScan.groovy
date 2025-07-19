package com.devops.security

class JavaScan {
    
    static Map scanMaven(Map config = [:]) {
        def pomPath = config.pomPath ?: 'pom.xml'
        def outputFormat = config.format ?: 'json'
        def outputFile = config.outputFile ?: 'trivy-maven-scan.json'
        def severityFilter = config.severity ?: 'HIGH,CRITICAL'
        def exitCode = config.exitCode ?: 1
        
        def trivyCommand = [
            'trivy',
            'fs',
            '--scanners', 'vuln',
            '--format', outputFormat,
            '--output', outputFile,
            '--severity', severityFilter,
            '--exit-code', exitCode.toString(),
            '--skip-files', '**/*.class,**/*.jar',
            pomPath
        ].join(' ')
        
        return [
            command: trivyCommand,
            outputFile: outputFile,
            format: outputFormat,
            scanType: 'maven'
        ]
    }
    
    static Map scanGradle(Map config = [:]) {
        def gradlePath = config.gradlePath ?: 'build.gradle'
        def outputFormat = config.format ?: 'json'
        def outputFile = config.outputFile ?: 'trivy-gradle-scan.json'
        def severityFilter = config.severity ?: 'HIGH,CRITICAL'
        def exitCode = config.exitCode ?: 1
        
        def trivyCommand = [
            'trivy',
            'fs',
            '--scanners', 'vuln',
            '--format', outputFormat,
            '--output', outputFile,
            '--severity', severityFilter,
            '--exit-code', exitCode.toString(),
            '--skip-files', '**/*.class,**/*.jar,**/build/**',
            gradlePath
        ].join(' ')
        
        return [
            command: trivyCommand,
            outputFile: outputFile,
            format: outputFormat,
            scanType: 'gradle'
        ]
    }
    
    static Map scanJavaProject(Map config = [:]) {
        def projectPath = config.path ?: '.'
        def buildTool = config.buildTool ?: detectBuildTool(projectPath)
        
        switch(buildTool) {
            case 'maven':
                return scanMaven(config)
            case 'gradle':
                return scanGradle(config)
            default:
                throw new IllegalArgumentException("Unsupported build tool: ${buildTool}. Supported tools: maven, gradle")
        }
    }
    
    static String detectBuildTool(String projectPath) {
        if (fileExists("${projectPath}/pom.xml")) {
            return 'maven'
        } else if (fileExists("${projectPath}/build.gradle") || fileExists("${projectPath}/build.gradle.kts")) {
            return 'gradle'
        } else {
            throw new IllegalArgumentException("No supported build tool detected in ${projectPath}")
        }
    }
    
    private static boolean fileExists(String filePath) {
        return new File(filePath).exists()
    }
}