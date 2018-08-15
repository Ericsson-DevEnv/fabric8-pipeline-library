#!/usr/bin/groovy
import io.fabric8.Fabric8Commands

def call(Map parameters = [:], body) {
    def flow = new Fabric8Commands()
    def defaultLabel = buildId('halyard')
    def label = parameters.get('label', defaultLabel)

    def halyardImage = parameters.get('halyardImage', 'gcr.io/spinnaker-marketplace/halyard:stable')
    def clientsImage = parameters.get('clientsImage', 'fabric8/builder-clients:0.9')
    def inheritFrom = parameters.get('inheritFrom', 'base')
    def jnlpImage = (flow.isOpenShift()) ? 'fabric8/jenkins-slave-base-centos7:0.0.1' : 'jenkinsci/jnlp-slave:2.62'

    def utils = new io.fabric8.Utils()
    // 0.13 introduces a breaking change when defining pod env vars so check version before creating build pod
    if (utils.isKubernetesPluginVersion013()) {
        podTemplate(label: label, serviceAccount: 'jenkins', inheritFrom: "${inheritFrom}",
                containers: [
                        containerTemplate(
                                name: 'halyard',
                                image: "${halyardImage}",
                                command: '/opt/halyard/bin/halyard',
                                ttyEnabled: true,
                                workingDir: '/home/jenkins/',
                                envVars: [
                                        envVar(key: 'DOCKER_CONFIG', value: '/home/jenkins/.docker/')
                                ]),
                        containerTemplate(
                                name: 'clients',
                                image: "${clientsImage}",
                                command: 'cat',
                                ttyEnabled: true)
                ],
                volumes:
                        [secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken'),
                         secretVolume(secretName: 'jenkins-ssh-config', mountPath: '/root/.ssh'),
                         secretVolume(secretName: 'jenkins-git-ssh', mountPath: '/root/.ssh-git')
                        ]) {
            body()

        }
    } else {
        podTemplate(label: label, serviceAccount: 'jenkins', inheritFrom: "${inheritFrom}",
                containers: [
                        //[name: 'jnlp', image: "${jnlpImage}", args: '${computer.jnlpmac} ${computer.name}',  workingDir: '/home/jenkins/'],
                        [name: 'halyard', image: "${halyardImage}", command: '/opt/halyard/bin/halyard', ttyEnabled: true,  workingDir: '/home/jenkins/',
                         envVars: [
                                 [key: 'DOCKER_CONFIG', value: '/home/jenkins/.docker/']
                         ]],
                        [name: 'clients', image: "${clientsImage}", command: 'cat', ttyEnabled: true]],

                volumes:
                        [secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken'),
                         secretVolume(secretName: 'jenkins-ssh-config', mountPath: '/root/.ssh'),
                         secretVolume(secretName: 'jenkins-git-ssh', mountPath: '/root/.ssh-git')
                        ]) {

            body()

        }
    }
}
