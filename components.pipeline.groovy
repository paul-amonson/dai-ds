// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
pipeline {
    options { disableConcurrentBuilds() }
    agent none
    parameters {
        booleanParam(name: 'QUICK_BUILD', defaultValue: false,
                description: 'Performs a partial clean to speed up the build.')
        choice(name: 'AGENT', choices: [
                'NRE-COMPONENT'
        ], description: 'Agent label')
    }
    stages {
        stage('Restart Component Test Servers') {
            environment {
                PGPASSWORD = 'postgres'
            }
            agent { label "DOCKER-TEST-SERVER-HOST" }
            steps {
                script { utilities.fixFilesPermission() }
                script { utilities.invokeGradleNoRetries(":procedures:jar") }
                timeout(time: 2, unit: 'MINUTES') {
                    dir('inventory/src/integration/resources/scripts') {
                        sh './restart_sql_servers.sh'
                    }
                }
                sh 'createdb --host=css-centos-8-00.ra.intel.com --username=postgres dai'
                sh 'psql --host=css-centos-8-00.ra.intel.com --username=postgres --dbname=dai < data/db/DAI-Tier2-Schema-Psql.sql'
                sh 'psql --host=css-centos-8-00.ra.intel.com --username=postgres --dbname=dai < inventory/src/integration/resources/scripts/postgres_inventory_data.sql'
                sh 'sqlcmd < inventory/src/integration/resources/scripts/load_procedures.sql'
                sh 'sqlcmd < data/db/Combined_VoltDB.sql'
            }
        }
        stage('Sequential Stages') { // all the sub-stages needs to be run on the same machine
            agent { label "${AGENT}" }
            environment {
                PATH = "${PATH}:/home/${USER}/voltdb9.1/bin"
                scriptDir = 'inventory/src/integration/resources/scripts'
                dataDir = 'inventory/src/integration/resources/data'
                etcDir = '/opt/ucs/etc'
                tmpDir = 'build/tmp'
            }
            stages {    // another stages is required to force operations on the same machine
                stage('Preparation') {
                    steps {
                        script { utilities.updateBuildName() }

                        echo "Building on ${AGENT}"
                        sh 'hostname'

                        lastChanges since: 'PREVIOUS_REVISION', format: 'SIDE', matching: 'LINE'

                        script {
                            utilities.fixFilesPermission()
                            utilities.cleanUpMachine('.')
                        }
                    }
                }
                stage('Quick Component Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'true' } }
                    steps {
                        script { utilities.invokeGradleNoRetries("integrationTest") }
                    }
                }
                stage('Component Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        script {
                            utilities.cleanWithGit()
                            utilities.invokeGradleNoRetries("clean")
                        }
                        script { utilities.invokeGradleNoRetries("integrationTest") }
                    }
                }
                stage('Reports') {
                    steps {
                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        junit allowEmptyResults: true, keepLongStdio: true, skipPublishingChecks: true,
                                testResults: '**/test-results/integrationTest/*.xml'
                    }
                }
                stage('Archive') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        sh 'rm -f *.zip'
                        zip archive: true, dir: '', glob: '**/build/jacoco/integrationTest.exec', zipFile: 'component-test-coverage.zip'
                        zip archive: true, dir: '', glob: '**/test-results/test/*.xml', zipFile: 'component-test-results.zip'
                    }
                }
            }
        }
    }
}
