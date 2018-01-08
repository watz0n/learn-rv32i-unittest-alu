//=======================================================================
// RISCV ALU Reg-Imm data-path
// Watson Huang
// Dec 4, 2017
// 
// Control path, nothing change from Reg-Reg cpath
//=======================================================================
package rvcore_ri

import chisel3._
import chisel3.util._
import rvcommon._

Combine control path (cpath) and data path (dpath) into a coreclass c2d_io extends Bundle {
    val alu_func = Output(UInt(rvalu.X.getWidth.W))
}

class cpath_io extends Bundle {
    val imem = new mram_io(rvspec.xlen)
    val dmem = new mram_io(rvspec.xlen)
    val d2c = Flipped(new d2c_io())
    val c2d = new c2d_io()
}

class rvcpath extends Module {
    val io = IO(new cpath_io)
    io := DontCare  //Solve NotInitialize Problem from Chisel3 3.0.1+ version
                    //Ref: https://github.com/freechipsproject/chisel3/wiki/Unconnected-Wires

    //Ref: https://github.com/ucb-bar/riscv-sodor/blob/master/src/rv32_1stage/cpath.scala, Fail
    //Ref: https://chisel.eecs.berkeley.edu/2.2.0/manual.html , Sect. 19 Extra Stuff, ListLookup
    //https://stackoverflow.com/questions/36612741/listlookup-in-chisel , casez
    val ctlsig = ListLookup(    
        io.d2c.inst,    //switch(io.d2c.inst) or casez(io.d2c.inst) 
        List(rvalu.X),  //default: List(rvalu.X)
        Array(
            rvinst.ADDI ->       List(rvalu.ADD),
            rvinst.SLTI ->       List(rvalu.SLT), 
            rvinst.SLTIU ->      List(rvalu.SLTU), 
            rvinst.XORI ->       List(rvalu.XOR),
            rvinst.ORI ->        List(rvalu.OR), 
            rvinst.ANDI ->       List(rvalu.AND),
            rvinst.SLLI ->       List(rvalu.SLL), 
            rvinst.SRLI ->       List(rvalu.SRL), 
            rvinst.SRAI ->       List(rvalu.SRA)
        )
    )

    //Debug, verify instruction in cpath
    //printf("cpath inst: 0x%x\n", io.d2c.inst)

    val alu_func :: Nil = ctlsig

    io.c2d.alu_func := alu_func

    /* //Encounter errors after add mram instance
    io.imem.req.valid := true.B
    io.imem.req.addr := io.d2c.pc
    io.imem.req.mfunc := mram_op.MF_RD
    io.imem.req.mtype := mram_op.MT_W
    */

}
