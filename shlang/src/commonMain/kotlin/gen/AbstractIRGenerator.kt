package gen

import types.TypeHolder
import ir.module.builder.impl.ModuleBuilder


abstract class AbstractIRGenerator(protected val mb: ModuleBuilder,
                                   protected val typeHolder: TypeHolder,
                                   protected val varStack: VarStack)