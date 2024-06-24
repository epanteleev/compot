package common

import kotlinx.cinterop.*
import platform.posix.*


@OptIn(ExperimentalForeignApi::class)
actual fun runCommand(command: String, args: List<String>, workingDir: String?): ExecutionResult {
    val arena = Arena()

    val pid = fork()
    if (pid == 0) {
        val argv = arena.allocArray<CPointerVar<ByteVar>>(args.size + 1)
        for (i in args.indices) {
            argv[i] = args[i].cstr.getPointer(arena)
        }
        argv[args.size] = null
        execvp(command, argv)
        perror("execve")
        exit(1)
    }

    sleep(2U) // TODO #$%$!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    //TODO waitpid is frozen
    //while (waitpid(-1, null, 0) == -1) {
    //    println("RRR")
    //    if (errno != EINTR) {
    //        perror("waitpid")
    //        exit(1)
    //    }
    //}

    return ExecutionResult("", "", 0)
}

@OptIn(ExperimentalForeignApi::class)
actual fun env(name: String): String? {
    val env = getenv(name) ?: return null
    return env.toString()
}

@OptIn(ExperimentalForeignApi::class)
actual fun pwd(): String {
    val buffer = ByteArray(4096)
    getcwd(buffer.refTo(0), buffer.size.convert())
    return buffer.toKString()
}