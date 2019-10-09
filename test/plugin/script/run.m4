#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ARG_OPTIONAL_BOOLEAN([build_agent], [], [no comment], [off])
# ARG_OPTIONAL_BOOLEAN([build_scenario], [], [no comment], [off])
# ARG_OPTIONAL_SINGLE([agent_home], [], [no comment])
# ARG_OPTIONAL_SINGLE([parallel_run_size], [], [The size of running testcase at the same time], 1)
# ARG_POSITIONAL_INF([scenarios], [The scenario that you want to running])
# DEFINE_SCRIPT_DIR([scenarios_home], [SCENARIO HOME])
# ARG_HELP([The general script's help msg])
# ARGBASH_GO
# [

home="$(cd "$(dirname $0)"; pwd)"

mvnw=${home}/../../mvnw
agent_home=${home}"/../../skywalking-agent"
scenarios_home="${home}/scenarios"

workspace="${home}/workspace"
task_state_house="${workspace}/.states"


plugin_autotest_helper="${home}/dist/plugin-autotest-helper.jar"

prepareAndClean() {
  echo "prepare and clear"
  [[ -d ${workspace} ]] && rm -fr ${workspace}

  mkdir -p ${workspace}/{.states,testcases}

  if [[ ${#_arg_scenarios[@]} -lt 1 ]]; then
    _arg_scenarios=`ls ./scenarios/|sed -e "s/\t/\n/g"`
  fi

  # docker prune
  docker container prune -f
  docker network   prune -f
  docker volume    prune -f
#  docker image     prune -f

  # build plugin/test
  ${mvnw} clean package -DskipTests docker:build
  if [[ ! -f ${plugin_autotest_helper} ]]; then
    echo -e "\033[31mplugin/test build failure\033[0m" # ]]
    exit 1;
  fi
}

waitForAvailable() {
  while [[ `ls -l ${task_state_house} |grep -c RUNNING` -ge ${_arg_parallel_run_size} ]]
  do
    sleep 2
  done

  if [[ `ls -l ${task_state_house} |grep -c FAILURE` -gt 0 ]]; then
    exit 1
  fi
}

################################################
start_stamp=`date +%s`

prepareAndClean ## prepare to start

echo "start submit job"
num_of_scenarios=0
for scenario_name in ${_arg_scenarios}
do
  scenario_home=${scenarios_home}/${scenario_name} && cd ${scenario_home}

  supported_version_file=${scenario_home}/support-version.list
  if [[ ! -f $supported_version_file ]]; then
    echo -e "\033[31m[ERROR] cannot found 'support-version.list' in directory ${scenario_name}\033[0m" # to escape ]]
    continue
  fi

  echo "scenario.name=${scenario_name}"
  num_of_scenarios=$((num_of_scenarios+1))

  supported_versions=`grep -v -E "^$|^#" ${supported_version_file}`
  for version in ${supported_versions}
  do
    testcase_name="${scenario_name}-${version}"

    # testcase working directory, there are logs, reports, and packages.
    case_work_base=${workspace}/testcases/${scenario_name}/${testcase_name}
    mkdir -p ${case_work_base}/{data,packages,logs,reports}

    case_work_logs_dir=${case_work_base}/logs

    # copy expectedData.yml
    cp ./config/expectedData.yaml ${case_work_base}/data

#    echo "build ${testcase_name}"
    ${mvnw} clean package -P${testcase_name} > ${case_work_logs_dir}/build.log

    mv ./target/${scenario_name}.war ${case_work_base}/packages

    java -Dconfigure.file=${scenario_home}/configuration.yml \
        -Dscenario.home=${case_work_base} \
        -Dscenario.name=${scenario_name} \
        -Dscenario.version=${version} \
        -Doutput.dir=${case_work_base} \
        -Dagent.dir=${agent_home} \
        -jar ${plugin_autotest_helper} 1>${case_work_logs_dir}/helper.log 2>&2

    [[ $? -ne 0 ]] && echo -e "\033[31m[ERROR] ${testcase_name}, generate script failure! \033[0m" && continue # ]]

    waitForAvailable
    echo "start container of testcase.name=${testcase_name}"
    bash ${case_work_base}/scenario.sh ${task_state_house} 1>${case_work_logs_dir}/${testcase_name}.log 2>&2 &
  done

  echo -e "\033[33m${scenario_name} has already sumbitted\033[0m" # to escape ]]
done

# wait to finish
while [[ `ls -l ${task_state_house} |grep -c RUNNING` -gt 0 ]]; do
  sleep 1
done

if [[ `ls -l ${task_state_house} |grep -c FAILURE` -gt 0 ]]; then
  exit 1
fi

elapsed=$(( `date +%s` - $start_stamp ))
num_of_testcases="`ls -l ${task_state_house} |grep -c FINISH`"

printf "Scenarios: %d, Testcases: %d, parallel_run_size: %d, Elapsed: %02d:%02d:%02d \n" \
  ${num_of_scenarios} "${num_of_testcases}" "${_arg_parallel_run_size}" \
  $(( ${elapsed}/3600 )) $(( ${elapsed}%3600/60 )) $(( ${elapsed}%60 ))

# ]
