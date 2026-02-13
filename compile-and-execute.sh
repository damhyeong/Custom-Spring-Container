#!/bin/bash

echo "컴파일 & 실행 구문 시작"

javac -d result/bin -parameters $(find com -name "*.java")

java -cp ./result/bin com.damsoon.Main

echo "컴파일 & 실행 구문 종료"
