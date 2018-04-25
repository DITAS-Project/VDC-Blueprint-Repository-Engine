// Jenkins CI manual can be found here (CI_manual_v2): https://repository.atosresearch.eu/index.php/apps/files/?dir=%2FDITAS%2FWP5%20case%20studies%2FIntegration
pipeline {
    agent none
    stages {
        stage('Build - test') {
            agent {
                dockerfile {
                    filename 'Dockerfile.build'
                }
            }
            steps {
		// Build 
                // sh 'mvn whatever'
				
		// Test	
                // sh 'mvn whatever'
            }
            // Save the reports always
            post {
                always {
                    // Record the jUnit test
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('Image creation') {
            agent any
            options {
                skipDefaultCheckout true
            }
            steps {
                // The Dockerfile.artifact copies the code into the image and run the jar generation.
                echo 'Creating the image...'

                // This will search for a Dockerfile.artifact in the working directory and build the image to the local repository
                sh "docker build -t \"ditas/vdc-blueprint-repository-engine\" -f Dockerfile.artifact ."
                echo "Done"

                echo 'Retrieving Docker Hub password from /opt/ditas-docker-hub.passwd...'
                // Get the password from a file. This reads the file from the host, not the container. Slaves already have the password in there.
                script {
                    password = readFile '/opt/ditas-docker-hub.passwd'
                }
                echo "Done"

                echo 'Login to Docker Hub as ditasgeneric...'
                sh "docker login -u ditasgeneric -p ${password}"
                echo "Done"

                echo "Pushing the image ditas/vdc-blueprint-repository-engine:latest..."
                sh "docker push ditas/vdc-blueprint-repository-engine:latest"
                echo "Done "
            }
        }
        stage('Image deploy') {
            agent any
            options {
                // Don't need to checkout Git again
                skipDefaultCheckout true
            }
            steps {
                // TODO: Uncomment this when the previous stages run correctly
		// TODO: Remember to edit 'deploy-staging.sh' and configure the ports
		// Deploy to Staging environment calling the deployment script
                // sh './jenkins/deploy-staging.sh'
            }
        }
    }
}
