RED=`tput setaf 1`
GREEN=`tput setaf 2`
RESET=`tput sgr0`

JAVA=java
IR_COMPILER="${JAVA} -ea -jar ../ssa-ir/target/ssa-ir-1.0-SNAPSHOT-jar-with-dependencies.jar"

function compile_test() {
	${IR_COMPILER} "$1.ir"
        gcc "$1/base.S" runtime.c -o "$1/base"
        gcc "$1/opt.S" runtime.c -o "$1/opt"
}

function run_test() {
	echo "${GREEN}[Run base: $1]${RESET}"
	BASE_RESULT=$(./$1/base)
	check $1 "$2" "$BASE_RESULT"

	echo "[Run opt: $1]"
	OPT_RESULT=$(./$1/opt)
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
  		echo -e "\t${RED}[Actual]:${RESET} '$result'"
	fi
}


compile_test manyArguments
run_test manyArguments 36

compile_test fib
run_test fib 21

compile_test fib_opt
run_test fib_opt 21

compile_test fib_recursive
run_test fib_recursive 21

compile_test discriminant
run_test discriminant -192

compile_test factorial
run_test factorial 40320

compile_test manyBranches
run_test manyBranches "7
0"

compile_test clamp
run_test clamp "9
10
8"

compile_test fill_in_array0
run_test fill_in_array0 "12345"

