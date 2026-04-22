pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
    timeout(time: 30, unit: 'MINUTES')
  }

  environment {
    HARBOR_REGISTRY = 'harbor.281018.xyz'
    HARBOR_PROJECT = 'chert'
    IMAGE_REPOSITORY = 'chert-standalone'
    IMAGE_NAME = "${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${IMAGE_REPOSITORY}"
    HARBOR_CREDENTIALS_ID = 'harbor-credentials'
    K8S_NAMESPACE = 'chert'
    K8S_DEPLOYMENT_NAME = 'chert'
    KUBECONFIG_CREDENTIALS_ID = 'kubeconfig-k3s'
  }

  stages {
    stage('Checkout & Prepare') {
      steps {
        checkout scm
        script {
          env.IMAGE_TAG = sh(
            script: 'git rev-parse --short=12 HEAD',
            returnStdout: true
          ).trim()
        }
      }
    }

    stage('Build Backend Jar') {
      agent {
        docker {
          image 'eclipse-temurin:25-jdk'
          reuseNode true
        }
      }
      steps {
        sh '''
          export HOME="${WORKSPACE}"
          export GRADLE_USER_HOME="${WORKSPACE}/.gradle"
          mkdir -p "${GRADLE_USER_HOME}"
          chmod +x ./gradlew
          ./gradlew clean :chert-server:bootJar -x test --no-daemon
        '''
      }
    }

    stage('Build Console Dist') {
      agent {
        docker {
          image 'node:24-alpine'
          reuseNode true
        }
      }
      steps {
        dir('chert-console') {
          sh '''
            export HOME="${WORKSPACE}"
            export npm_config_cache="${WORKSPACE}/.npm"
            mkdir -p "${npm_config_cache}"
            npm ci
            npm run build
          '''
        }
      }
    }

    stage('Build & Push Image') {
      steps {
        script {
          docker.withRegistry("https://${HARBOR_REGISTRY}", "${HARBOR_CREDENTIALS_ID}") {
            def standaloneImage = docker.build("${IMAGE_NAME}:${env.IMAGE_TAG}", '.')
            standaloneImage.push()
            standaloneImage.push('latest')
          }
        }
      }
    }

    stage('Deploy to K8s') {
      agent {
        docker {
          image 'bitnami/kubectl:latest'
          args '--entrypoint=""'
        }
      }
      steps {
        withCredentials([file(credentialsId: "${KUBECONFIG_CREDENTIALS_ID}", variable: 'KUBECONFIG_FILE')]) {
          sh '''
            set -e
            export KUBECONFIG="${KUBECONFIG_FILE}"

            sed -i "s|newTag: .*|newTag: ${IMAGE_TAG}|g" k8s/kustomization.yaml
            kubectl apply -k k8s/
            kubectl -n "${K8S_NAMESPACE}" rollout status deployment/${K8S_DEPLOYMENT_NAME} --timeout=180s
          '''
        }
      }
    }
  }

  post {
    success {
      echo "Successfully built ${IMAGE_NAME}:${env.IMAGE_TAG}"
    }
    failure {
      echo 'Pipeline failed. Please inspect the Jenkins stage logs.'
    }
    cleanup {
      sh '''
        docker rmi "${IMAGE_NAME}:${IMAGE_TAG}" || true
        docker rmi "${IMAGE_NAME}:latest" || true
      '''
    }
  }
}
