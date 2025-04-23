package asm.x64

enum class CondFlagType {
    EQ {  // if equal (ZF=1).
        override fun invert(): CondFlagType = NE
    },
    NE { // if not equal (ZF=0).
        override fun invert(): CondFlagType = EQ
    },
    G {  // if greater (ZF=0 and SF=OF).
        override fun invert(): CondFlagType = L
    },
    GE { // if greater or equal (SF=OF).
        override fun invert(): CondFlagType = L
    },
    L { // if less (SF≠ OF).
        override fun invert(): CondFlagType = GE
    },
    LE { // if less or equal (ZF=1 or SF≠ OF).
        override fun invert(): CondFlagType = G
    },
    A {// if above (CF=0 and ZF=0).
        override fun invert(): CondFlagType = BE
    },
    AE { // if above or equal (CF=0).
        override fun invert(): CondFlagType = B
    },
    B { // if below (CF=1).
        override fun invert(): CondFlagType = AE
    },
    BE { // if below or equal (CF=1 or ZF=1).
        override fun invert(): CondFlagType = A
    },
    NA { // if not above (CF=1 or ZF=1).
        override fun invert(): CondFlagType = A
    },
    NAE { // if not above or equal (CF=1).
        override fun invert(): CondFlagType = AE
    },
    JNB { // if not below (CF=0).
        override fun invert(): CondFlagType = B
    },
    P { // Jump if parity (PF=0).
        override fun invert(): CondFlagType = NP
    },
    S { // Jump if sign (SF=0).
        override fun invert(): CondFlagType = NS
    },
    NS { // Jump if not sign (SF=1).
        override fun invert(): CondFlagType = S
    },
    Z { // Jump if zero (ZF=0).
        override fun invert(): CondFlagType = NZ
    },
    NZ { // Jump if not zero (ZF=1).
        override fun invert(): CondFlagType = Z
    },
    NP { // Jump if not parity (PF=1).
        override fun invert(): CondFlagType = P
    };

    abstract fun invert(): CondFlagType
}