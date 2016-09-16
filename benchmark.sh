#!/bin/bash

cd benchmarks
echo 'Running maven build'
mvn clean install
rc=$?
if [[ ${rc} -ne 0 ]] ; then
    echo 'Could not build benchmarks'
    exit ${rc}
fi

echo 'Running benchmarks'
java -jar target/benchmarks.jar



