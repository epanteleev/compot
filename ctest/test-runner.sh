#!/bin/bash

RED=`tput setaf 1`
GREEN=`tput setaf 2`
RESET=`tput sgr0`

JAVA=java

export JAVA_OPTS="-ea"
OUTPUT_DIR="outdir"
IR_COMPILER="./build/shlang-1.0-SNAPSHOT/bin/shlang --dump-ir $OUTPUT_DIR"

rm -rf ../build
mkdir ../build
unzip -o ../shlang/build/distributions/shlang-1.0-SNAPSHOT.zip -d build

function compile_test() {
	${IR_COMPILER} -c "$1.c" -o "$OUTPUT_DIR/$1"
        gcc "$OUTPUT_DIR/$1.o" ../tests/runtime.c -o "$OUTPUT_DIR/$1.base"
	${IR_COMPILER} -c "$1.c" -O 1 -o "$OUTPUT_DIR/$1"
        gcc "$OUTPUT_DIR/$1.o" ../tests/runtime.c -o "$OUTPUT_DIR/$1.opt"
}

function run_test() {
	echo "${GREEN}[Run base: $1]${RESET}"
  ./$OUTPUT_DIR/$1.base
	BASE_RESULT=$(echo $?)
	check $1 "$2" "$BASE_RESULT"

	echo "[Run opt: $1]"
	./$OUTPUT_DIR/$1.opt
	OPT_RESULT=$(echo $?)
	check $1 "$2" "$OPT_RESULT"
}

function check() {
	result=$3
	expected=$2
	if [ "$result" == "$expected" ];
	then
  		echo -e "\t${GREEN}[SUCCESS] '$result'${RESET}"
	else
  		echo -e "\t${RED}[FAIL]: '$1'${RESET}"
  		echo -e "\t${GREEN}[Expected]: '$expected'${RESET}"
  		echo -e "\t${RED}[Actual]:  ${RESET} '$result'"
	fi
}

function compile_and_run() {
	local test_name=$1
	local expected_result=$2
	compile_test "$test_name"
	run_test "$test_name" "$expected_result"
}

compile_and_run printInt 20
compile_and_run cast1 23
compile_and_run 1 57
compile_and_run 2 0
compile_and_run 3 20
compile_and_run 4 0
compile_and_run 5 20
compile_and_run 6 10
compile_and_run doWhile 20
compile_and_run forLoop 30
compile_and_run forLoop1 30
compile_and_run forLoop2 30
compile_and_run functionCall1 8
compile_and_run fibbonach1 55
compile_and_run math1 4
compile_and_run goto 20
compile_and_run deref 67
