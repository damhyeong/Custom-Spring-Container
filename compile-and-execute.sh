#!/bin/bash

echo "컴파일 & 실행 구문 시작"

maven_path="./target"

javac -d ${maven_path}/classes -parameters $(find src/com -name "*.java")

javac -d ${maven_path}/test-classes -parameters -cp ${maven_path}/classes $(find src/test -name "*.java")

java -cp ${maven_path}/classes:${maven_path}/test-classes com.damsoon.MainTestRunner

echo "컴파일 & 실행 구문 종료"
