#!/bin/bash

RED=`tput setaf 1`
GREEN=`tput setaf 2`
RESET=`tput sgr0`

JAVA=java

export JAVA_OPTS="-Xint -ea"
OUTPUT_DIR="outdir"
IR_COMPILER="./build/ssa-1.0/bin/ssa --dump-ir $OUTPUT_DIR"

rm -rf ../build
mkdir ../build
unzip -o ../ssa/build/distributions/ssa-1.0.zip -d build

function compile_test() {
	${IR_COMPILER} -c "$1.ir" -o "$OUTPUT_DIR/$1"
        gcc "$OUTPUT_DIR/$1.o" runtime.c -o "$OUTPUT_DIR/$1.base"
	${IR_COMPILER} -c "$1.ir" -O 1 -o "$OUTPUT_DIR/$1" 
        gcc "$OUTPUT_DIR/$1.o" runtime.c -o "$OUTPUT_DIR/$1.opt"
}

function run_test() {
	echo "${GREEN}[Run base: $1]${RESET}"
	BASE_RESULT=$(./$OUTPUT_DIR/$1.base)
	check $1 "$2" "$BASE_RESULT"

	echo "[Run opt: $1]"
	OPT_RESULT=$(./$OUTPUT_DIR/$1.opt)
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

compile_and_run getAddress1 90
compile_and_run getAddress 90
compile_and_run doWhile 20
compile_and_run bubble_sort_i8 "0 2 4 4 9 23 45 55 89 90 " 
compile_and_run bubble_sort_fp "0.000000 2.000000 4.000000 4.000000 9.000000 23.000000 45.000000 55.000000 89.000000 90.000000 "
compile_and_run collatz "1"
compile_and_run swapStructElements "67 5 "
compile_and_run swapElements "4 2 0 9 90 45 55 89 23 4 "
compile_and_run swap1 "4 2 0 9 90 45 55 89 23 4 "
compile_and_run swap "7
5"
compile_and_run removeElement "4 2 0 9 45 55 89 4 23 "
compile_and_run stringReverse "dlrow olleH"
compile_and_run sumLoop2 "45"
compile_and_run bubble_sort "0 2 4 4 9 23 45 55 89 90 "
compile_and_run hello_world1 "Hello world"
compile_and_run select "0
1"
compile_and_run struct_access 14
compile_and_run struct_access1 16
compile_and_run manyArguments 36
compile_and_run manyArguments1 36.000000
compile_and_run sum 16
compile_and_run sum1 16.000000
compile_and_run fib 21
compile_and_run fib_u32 21
compile_and_run fib_opt 21
compile_and_run fib_recursive 21
compile_and_run discriminant -192
compile_and_run discriminant1 -192.000000
compile_and_run factorial 40320
compile_and_run manyBranches "7
0"
compile_and_run clamp "9
10
8"
compile_and_run clamp1 "9.000000
10.000000
8.000000"
compile_and_run fill_in_array0 "01234"
compile_and_run fill_in_array1 "01234"
compile_and_run fill_in_array2 "01234"
compile_and_run fill_in_array3 "0123456789"
compile_and_run fill_in_array4 "01234"
compile_and_run fill_in_array5 "01234"
compile_and_run fill_in_fp_array1 "0.000000 1.000000 2.000000 3.000000 4.000000 "
compile_and_run fill_in_fp_array2 "0.000000 1.000000 2.000000 3.000000 4.000000 "
compile_and_run i32_to_i64 "-1"
compile_and_run u32_to_u64 "1"
compile_and_run i64_to_i32 "-1"
compile_and_run hello_world "Hello world"
compile_and_run load_global_var 120
compile_and_run load_global_var1 "abc 120"
compile_and_run load_global_var2 "-8
-16
-32
-64
8
16
32
64"
compile_and_run load_global_var3 "120.000000
140.000000"
compile_and_run load_store_global_var "1000"
compile_and_run neg 1
compile_and_run neg1 1.000000
compile_and_run neg2 1.000000
compile_and_run float_compare 5.000000
compile_and_run float_compare1 4.000000
compile_and_run f64_to_f32 -1.000000
compile_and_run f32_to_f64 -1.000000
