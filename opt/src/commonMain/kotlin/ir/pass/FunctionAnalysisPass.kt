package ir.pass


abstract class AnalysisResult

abstract class FunctionAnalysisPass<T: AnalysisResult> {
    abstract fun name(): String
    abstract fun run(): T
}