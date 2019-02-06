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
                sh 'mvn -B -DskipTests clean package'
		
		// Archive the artifact to be accessible from the Artifacts tab into the Blue Ocean interface, just to have it handy
		archiveArtifacts 'target/*.jar'
		    
		// Test	
                sh 'mvn test'
            }
            // Save the reports always
            post {
                always {
                    // Record the jUnit test
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('Staging image creation') {
            agent any
            options {
                skipDefaultCheckout true
            }
            steps {
                // The Dockerfile.artifact copies the code into the image and run the jar generation.
                echo 'Creating the image...'

                // This will search for a Dockerfile.artifact in the working directory and build the image to the local repository
                sh "docker build -t \"ditas/vdc-blueprint-repository-engine:staging\" -f Dockerfile.artifact ."
                echo "Done"
		    
                // Get the password from a file. This reads the file from the host, not the container. Slaves already have the password in there.
                echo 'Retrieving Docker Hub password from /opt/ditas-docker-hub.passwd...'
                script {
                    password = readFile '/opt/ditas-docker-hub.passwd'
                }
                echo "Done"

                echo 'Login to Docker Hub as ditasgeneric...'
                sh "docker login -u ditasgeneric -p ${password}"
                echo "Done"

                echo "Pushing the image ditas/vdc-blueprint-repository-engine:staging..."
                sh "docker push ditas/vdc-blueprint-repository-engine:staging"
                echo "Done "
            }
        }
        stage('Deployment in Staging') {
            agent any
            options {
                // Don't need to checkout Git again
                skipDefaultCheckout true
            }
            steps {
		        // Deploy to Staging environment calling the deployment script
                sh './jenkins/deploy/deploy-staging.sh'
            }
        }
	    stage('Dredd API validation') {
	        agent any
	        steps {
	            sh 'sleep 10'    
	            sh 'dredd VDC_Blueprint_Repository_Engine_Swagger_v3.yaml http://31.171.247.162:50009 --hookfiles=./hooks.js --user publicUser:Blueprint'
	        }
        }	
	    stage('Production image creation') {
            steps {
                // Change the tag from staging to production 
                sh "docker tag ditas/vdc-blueprint-repository-engine:staging ditas/vdc-blueprint-repository-engine:production"
                sh "docker push ditas/vdc-blueprint-repository-engine:production"
            }
        }	
		
        stage('Deployment in Production') {
            agent any
            steps {
                // Production environment: 178.22.69.83
                // Private key for ssh: /opt/keypairs/ditas-testbed-keypair.pem
                // Call the deployment script
                sh './jenkins/deploy/deploy-production.sh'
            }
        }
    }
}
