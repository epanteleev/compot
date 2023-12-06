package ir.instruction

import ir.Value

abstract class Return(usages: Array<Value>): TerminateInstruction(usages, arrayOf())