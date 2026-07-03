pipeline {
    agent any

    stages {
        stage('Check Workspace') {
            steps {
                bat 'cd'
                bat 'dir'
            }
        }

        stage('Build user-api') {
            steps {
                dir('user-api') {
                    bat 'mvn clean package'
                }
            }
        }

        stage('Build book-api') {
            steps {
                dir('book-api') {
                    bat 'mvn clean package'
                }
            }
        }

        stage('Build loan-api') {
            steps {
                dir('loan-api') {
                    bat 'mvn clean package'
                }
            }
        }

        stage('Build gateway-api') {
            steps {
                dir('gateway-api') {
                    bat 'mvn clean package'
                }
            }
        }

        stage('Run Tests') {
            steps {
                dir('user-api') {
                    bat 'mvn test'
                }
                dir('book-api') {
                    bat 'mvn test'
                }
                dir('loan-api') {
                    bat 'mvn test'
                }
                dir('gateway-api'){
                    bat 'mvn test'
                }
            }
        }

        stage('Start System') {
            steps {
                bat 'docker compose up -d --build'
            }
        }

        stage('Check Running Containers') {
            steps {
                bat 'docker compose ps'
            }
        }
    }

    post {
        always {
            bat 'docker compose down'
        }
    }
}