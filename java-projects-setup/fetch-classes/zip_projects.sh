#!/bin/bash
directory="/Desktop/projects"

for folder in "$directory"/*; do
    if [[ -d "$folder" ]]; then
        find $folder -type f ! -name "mvn-test.log" ! -name "mvn-dependencies.log" ! -name "APP_SOURCE.cp" ! -name "TEST_SOURCE.cp" ! -name "mvn-compile.log" -exec rm {} +
        find $folder -mindepth 1 -type d -exec rm -rf {} +
        # shopt -s extglob
        # rm -r $folder/!(mvn-test.log|mvn-compile.log|mvn-dependencies.log|APP_SOURCE.cp|TEST_SOURCE.cp)
        echo "Removed everything from: $folder"
    fi
done