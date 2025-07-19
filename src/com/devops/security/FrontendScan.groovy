package com.devops.security

class FrontendScan {
    
    static Map scanNpm(Map config = [:]) {
        def packageJsonPath = config.packageJsonPath ?: 'package.json'
        def outputFormat = config.format ?: 'json'
        def outputFile = config.outputFile ?: 'trivy-npm-scan.json'
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
            '--skip-files', '**/node_modules/**',
            packageJsonPath
        ].join(' ')
        
        return [
            command: trivyCommand,
            outputFile: outputFile,
            format: outputFormat,
            scanType: 'npm'
        ]
    }
    
    static Map scanYarn(Map config = [:]) {
        def yarnLockPath = config.yarnLockPath ?: 'yarn.lock'
        def outputFormat = config.format ?: 'json'
        def outputFile = config.outputFile ?: 'trivy-yarn-scan.json'
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
            '--skip-files', '**/node_modules/**',
            yarnLockPath
        ].join(' ')
        
        return [
            command: trivyCommand,
            outputFile: outputFile,
            format: outputFormat,
            scanType: 'yarn'
        ]
    }
    
    static Map scanPnpm(Map config = [:]) {
        def pnpmLockPath = config.pnpmLockPath ?: 'pnpm-lock.yaml'
        def outputFormat = config.format ?: 'json'
        def outputFile = config.outputFile ?: 'trivy-pnpm-scan.json'
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
            '--skip-files', '**/node_modules/**',
            pnpmLockPath
        ].join(' ')
        
        return [
            command: trivyCommand,
            outputFile: outputFile,
            format: outputFormat,
            scanType: 'pnpm'
        ]
    }
    
    static Map scanFrontendProject(Map config = [:]) {
        def projectPath = config.path ?: '.'
        def packageManager = config.packageManager ?: detectPackageManager(projectPath)
        
        switch(packageManager) {
            case 'npm':
                return scanNpm(config)
            case 'yarn':
                return scanYarn(config)
            case 'pnpm':
                return scanPnpm(config)
            default:
                throw new IllegalArgumentException("Unsupported package manager: ${packageManager}. Supported managers: npm, yarn, pnpm")
        }
    }
    
    static String detectPackageManager(String projectPath) {
        if (fileExists("${projectPath}/yarn.lock")) {
            return 'yarn'
        } else if (fileExists("${projectPath}/pnpm-lock.yaml")) {
            return 'pnpm'
        } else if (fileExists("${projectPath}/package.json")) {
            return 'npm'
        } else {
            throw new IllegalArgumentException("No supported package manager detected in ${projectPath}")
        }
    }
    
    private static boolean fileExists(String filePath) {
        return new File(filePath).exists()
    }
}