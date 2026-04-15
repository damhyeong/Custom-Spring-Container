#!/bin/bash

echo "컴파일 & 실행 구문 시작"

maven_path="./target"

javac -d ${maven_path}/classes -parameters $(find src/com -name "*.java")

javac -d ${maven_path}/test-classes -parameters -cp ${maven_path}/classes $(find src/test -name "*.java")

# 테스팅 분기 추가 - Todo List
# java -cp ${maven_path}/classes:${maven_path}/test-classes com.damsoon.MainTestRunner

# 일반 실행
java -cp ${maven_path}/classes: com.damsoon.Main

echo "컴파일 & 실행 구문 종료"

