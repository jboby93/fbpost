#!/bin/bash

# switch current directory to same directory as this script
# DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
OLDPWD=$PWD;
# cd $DIR;
cd "/var/www/fbpost/"; #  !!! change this if you have fbpost installed somewhere else!

java -Dfile.encoding=UTF-8 -cp "./bin:./lib/*" fbpost.App "$@"

cd $OLDPWD;