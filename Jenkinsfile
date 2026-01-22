pipeline {
    agent any
    options {
        skipDefaultCheckout()
    }
    tools {
        maven "mvn"
    }


    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', credentialsId: 'Git token', url: 'https://github.com/artemelyashevich/cvbuild.git'
            }
        }
        stage('Build') {
            parallel {
                stage('Java') {
                    steps {
                        dir('expense-tracker-service') {
                            sh 'mvn clean install'
                        }
                    }
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    sh 'cd expense-tracker-service && mvn test'
                }
            }
        }
    }
    post {
        success {
            echo 'Build was successful!'
        }
        failure {
            echo 'Build failed. Check logs.'
        }
    }
}