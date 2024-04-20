import os
import subprocess


class bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'


def coloring(color, text):
    return color + text + bcolors.ENDC


def run_command(command):
    process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = process.communicate()
    return stdout.decode().strip()


def compile_test(test_name):
    run_command(f"{IR_COMPILER} -c {test_name}.ir")
    run_command(f"gcc {test_name}.o runtime.c -o base")
    run_command(f"{IR_COMPILER} -c {test_name}.ir -O 1")
    run_command(f"gcc {test_name}.o runtime.c -o opt")


def run_test(test_name, expected_result):
    print(coloring(bcolors.WARNING, f"[Run base: {test_name}]"))
    base_result = run_command("./base")
    check(test_name, expected_result, base_result)

    print(coloring(bcolors.WARNING, f"[Run opt: {test_name}]"))
    opt_result = run_command("./opt")
    check(test_name, expected_result, opt_result)


def check(test_name, expected_result, result):
    if result == expected_result:
        print(coloring(bcolors.OKGREEN, f"\t[SUCCESS] '{result}'"))
    else:
        print(coloring(bcolors.FAIL, f"\t[FAIL]: '{test_name}'"))
        print(coloring(bcolors.FAIL, f"\t[Expected]: '{expected_result}'"))
        print(coloring(bcolors.FAIL, f"\t[Actual]: '{result}'"))


def compile_and_run(test_name, expected_result):
    compile_test(test_name)
    run_test(test_name, expected_result)


# Set environment variables
os.environ["JAVA_OPTS"] = "-Xint -ea"
IR_COMPILER = "./build/ssa-1.0/bin/ssa --dump-ir"

# Clean and setup build directory
if os.path.exists("../build"):
    os.system("rm -rf ../build")

os.mkdir("../build")
run_command("unzip -o ../ssa/build/distributions/ssa-1.0.zip -d build")

# Run tests
compile_and_run("getAddress1", "90")
compile_and_run("getAddress", "90")
compile_and_run("doWhile", "20")
compile_and_run("bubble_sort_i8", "0 2 4 4 9 23 45 55 89 90")
compile_and_run("bubble_sort_fp", "0.000000 2.000000 4.000000 4.000000 9.000000 23.000000 45.000000 55.000000 89.000000 90.000000")
compile_and_run("collatz", "1")
compile_and_run("swapStructElements", "67 5")
compile_and_run("swapElements", "4 2 0 9 90 45 55 89 23 4")
compile_and_run("swap1", "4 2 0 9 90 45 55 89 23 4")
compile_and_run("swap", "7\n5")
compile_and_run("removeElement", "4 2 0 9 45 55 89 4 23")
compile_and_run("stringReverse", "dlrow olleH")
compile_and_run("sumLoop2", "45")
compile_and_run("bubble_sort", "0 2 4 4 9 23 45 55 89 90")
compile_and_run("hello_world1", "Hello world")
compile_and_run("select", "0\n1")
compile_and_run("struct_access", "14")
compile_and_run("struct_access1", "16")
compile_and_run("manyArguments", "36")
compile_and_run("manyArguments1", "36.000000")
compile_and_run("sum", "16")
compile_and_run("sum1", "16.000000")
compile_and_run("fib", "21")
compile_and_run("fib_u32", "21")
compile_and_run("fib_opt", "21")
compile_and_run("fib_recursive", "21")
compile_and_run("discriminant", "-192")
compile_and_run("discriminant1", "-192.000000")
compile_and_run("factorial", "40320")
compile_and_run("manyBranches", "7\n0")
compile_and_run("clamp", "9\n10\n8")
compile_and_run("clamp1", "9.000000\n10.000000\n8.000000")
compile_and_run("fill_in_array0", "01234")
compile_and_run("fill_in_array1", "01234")
compile_and_run("fill_in_array2", "01234")
compile_and_run("fill_in_array3", "0123456789")
compile_and_run("fill_in_array4", "01234")
compile_and_run("fill_in_array5", "01234")
compile_and_run("fill_in_fp_array1", "0.000000 1.000000 2.000000 3.000000 4.000000")
compile_and_run("fill_in_fp_array2", "0.000000 1.000000 2.000000 3.000000 4.000000")
compile_and_run("i32_to_i64", "-1")
compile_and_run("u32_to_u64", "1")
compile_and_run("i64_to_i32", "-1")
compile_and_run("hello_world", "Hello world")
compile_and_run("load_global_var", "120")
compile_and_run("load_global_var1", "abc 120")
compile_and_run("load_global_var2", "-8\n-16\n-32\n-64\n8\n16\n32\n64")
compile_and_run("load_global_var3", "120.000000\n140.000000")
compile_and_run("load_store_global_var", "1000")
compile_and_run("neg", "1")
compile_and_run("neg1", "1.000000")
compile_and_run("neg2", "1.000000")
compile_and_run("float_compare", "5.000000")
compile_and_run("float_compare1", "4.000000")
compile_and_run("f64_to_f32", "-1.000000")
compile_and_run("f32_to_f64", "-1.000000")