JAVA=java
IR_COMPILER="${JAVA} -jar ../ssa-ir/target/ssa-ir-1.0-SNAPSHOT-jar-with-dependencies.jar"

function compile_test() {
	${IR_COMPILER} "$1.ir"
        gcc "$1/base.S" runtime.c -o "$1/base"
        gcc "$1/opt.S" runtime.c -o "$1/opt"
}

function run_test() {
	echo "[Run base: $1"]
	./$1/base
	echo "[Run opt: $1]"
	./$1/opt
}

compile_test manyArguments
run_test manyArguments

compile_test fib
run_test fib

compile_test fib_recursive
run_test fib_recursive

compile_test discriminant
run_test discriminant

compile_test factorial
run_test factorial

compile_test manyBranches
run_test manyBranches

compile_test clamp
run_test clamp

compile_test fill_in_array0
run_test fill_in_array0

