package ir.pass


abstract class AnalysisResult

abstract class FunctionAnalysisPass {
    abstract fun name(): String
    abstract fun run(): AnalysisResult
}