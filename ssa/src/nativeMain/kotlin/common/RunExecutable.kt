package common

import kotlinx.cinterop.*
import platform.posix.*


object RunExecutable {
    @OptIn(ExperimentalForeignApi::class)
    fun runCommand(command: String, args: List<String>, workingDir: String? = null): ExecutionResult {
        val arena = Arena()
        val fd = arena.allocArray<IntVar>(2)
        pipe(fd)

        when (val pid = fork()) {
            0 -> {
                dup2(fd[0], STDOUT_FILENO)
                close(STDOUT_FILENO)

                execv(command, args.toCStringArray(arena))
                exit(1)
            }
            -1 -> {
                perror("fork")
                exit(1)
            }
            else -> {
                waitForChildren(pid)

                val buffer = arena.allocArray<ByteVar>(1024)
                read(fd[1], buffer, 1024U)
                return ExecutionResult(buffer.toString())
            }
        }

        return ExecutionResult("")
    }

    @OptIn(ExperimentalForeignApi::class)
    fun waitForChildren(pid: pid_t): Int {
        val arena = Arena()
        val st = arena.alloc<IntVar>()
        waitpid(pid, st.ptr, WEXITED)
        return 1
    }

    @OptIn(ExperimentalForeignApi::class)
    fun env(name: String): String? {
        val env = getenv(name) ?: return null
        return env.toString()
    }
}

class ExecutionResult(val output: String) {
}