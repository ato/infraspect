#!/bin/bash

mvn package dependency:copy-dependencies

mkdir $1/lib
cp -v target/{dependency/,}*.jar $1/lib

