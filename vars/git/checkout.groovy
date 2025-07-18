def call(Map config = [:]) {
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
    
    sh '''
        echo "Git Status:"
        git status --porcelain
        echo "Current branch: $(git rev-parse --abbrev-ref HEAD)"
        echo "Current commit: $(git rev-parse --short=8 HEAD)"
        echo "Commit message: $(git log -1 --pretty=%B)"
    '''
}