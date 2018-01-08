//=======================================================================
// RISCV ALU Reg-Reg Reg-Imm Merged data-path
// Watson Huang
// Dec 5, 2017
// 
// Control Path, add op2_sel to select ALU op2 data 
//=======================================================================
package rvcore_dpm

import chisel3._
import chisel3.util._
import rvcommon._

class c2d_io extends Bundle {
    //getWidth.W from "using def" section in ref.
    //ref: https://github.com/ucb-bar/chisel-tutorial/wiki/scripting-hardware-generation
    val alu_func = Output(UInt(rvalu.X.getWidth.W))
    val op2_sel = Output(UInt(rvdp.op2_sel.X.getWidth.W))
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
        List(rvalu.X, rvdp.op2_sel.X),  //default: List(rvalu.X)
        Array(
            //For Reg-Reg instructions, RV32I Base Instruction Set List up to down order
            rvinst.ADD ->       List(rvalu.ADD, rvdp.op2_sel.R),
            rvinst.SUB ->       List(rvalu.SUB, rvdp.op2_sel.R),
            rvinst.SLL ->       List(rvalu.SLL, rvdp.op2_sel.R), 
            rvinst.SLT ->       List(rvalu.SLT, rvdp.op2_sel.R), 
            rvinst.SLTU ->      List(rvalu.SLTU, rvdp.op2_sel.R), 
            rvinst.XOR ->       List(rvalu.XOR, rvdp.op2_sel.R), 
            rvinst.SRL ->       List(rvalu.SRL, rvdp.op2_sel.R), 
            rvinst.SRA ->       List(rvalu.SRA, rvdp.op2_sel.R), 
            rvinst.OR ->        List(rvalu.OR, rvdp.op2_sel.R), 
            rvinst.AND ->       List(rvalu.AND, rvdp.op2_sel.R),
            //For Reg-Imm instructions, RV32I Base Instruction Set List up to down order
            rvinst.ADDI ->       List(rvalu.ADD, rvdp.op2_sel.I),
            rvinst.SLTI ->       List(rvalu.SLT, rvdp.op2_sel.I), 
            rvinst.SLTIU ->      List(rvalu.SLTU, rvdp.op2_sel.I), 
            rvinst.XORI ->       List(rvalu.XOR, rvdp.op2_sel.I),
            rvinst.ORI ->        List(rvalu.OR, rvdp.op2_sel.I), 
            rvinst.ANDI ->       List(rvalu.AND, rvdp.op2_sel.I),
            rvinst.SLLI ->       List(rvalu.SLL, rvdp.op2_sel.I), 
            rvinst.SRLI ->       List(rvalu.SRL, rvdp.op2_sel.I), 
            rvinst.SRAI ->       List(rvalu.SRA, rvdp.op2_sel.I)
        )
    )

    //Debug, verify instruction in cpath
    //printf("cpath inst: 0x%x\n", io.d2c.inst)

    val alu_func :: op2_sel :: Nil = ctlsig

    io.c2d.alu_func := alu_func
    io.c2d.op2_sel := op2_sel

    /* //Encounter errors after add mram instance
    io.imem.req.valid := true.B
    io.imem.req.addr := io.d2c.pc
    io.imem.req.mfunc := mram_op.MF_RD
    io.imem.req.mtype := mram_op.MT_W
    */

}
