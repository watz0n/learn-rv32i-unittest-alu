//=======================================================================
// RISCV ALU Reg-Reg Reg-Imm Merged data-path
// Watson Huang
// Dec 5, 2017
// 
// Data Path, add op2_sel to select ALU op2 data 
//=======================================================================
package rvcore_dpm

import chisel3._
import chisel3.util._
import rvcommon._

class d2c_io extends Bundle {
    //val pc = Output(UInt(rvspec.xlen.W))
    val inst = Output(UInt(rvspec.xlen.W))
}

class dpath_io extends Bundle {
    val imem = new mram_io(rvspec.xlen)
    val dmem = new mram_io(rvspec.xlen)
    val d2c = new d2c_io()
    val c2d = Flipped(new c2d_io())
    //RegFile debug/test path, temporary used for no LUI/ADDI design
    val rfdbg = Flipped(new mram_io(rvspec.xlen))
}

class rvdpath extends Module {
    
    val io = IO(new dpath_io)
    io := DontCare  //Solve NotInitialize Problem from Chisel3 3.0.1+ version
                    //Ref: https://github.com/freechipsproject/chisel3/wiki/Unconnected-Wires

    //Conventional RISCV data-flow path

    //#Part1. PC to Instruction Memory
    val pc = Reg(UInt(rvspec.xlen.W))
    val pc_next = Wire(UInt(rvspec.xlen.W))

    val pc_add4 = pc + UInt(4, rvspec.xlen)
    pc_next := pc_add4
    //pc := pc_next

    //#Part2. Fetch Instruction Memory

    //io.d2c.pc := pc //Encounter errors after add mram instance

    io.imem.req.addr := pc
    io.imem.req.data := 0.U
    io.imem.req.mfunc := mram_op.MF_RD
    io.imem.req.mtype := mram_op.MT_W
    io.imem.req.valid := false.B
    
    when(io.imem.req.ready) {
        io.imem.req.valid := true.B
        pc := pc_next
    }

    //Ref: https://github.com/freechipsproject/chisel3/wiki/Muxes-and-Input-Selection
    val inst = Mux(io.imem.resp.valid, io.imem.resp.data, UInt(0x00, rvspec.xlen))
    io.d2c.inst := inst
    
    //Debug, Check again if instruction send to cpath correctly
    //printf("dpath inst: [0x%x]<=(%b)?0x%x:0x00\n", inst, io.imem.resp.valid, io.imem.resp.data)

    //#Part3. Instruction to RegFile
    //Ref: https://github.com/freechipsproject/chisel3/wiki/Cookbook#how-do-i-create-a-vector-of-registers
    val regfile = Reg(Vec(rvspec.xrsz, UInt(rvspec.xlen.W)))

    //Reg-Reg
    val rs1_addr = inst(rvinst.rs1bh, rvinst.rs1bl)
    val rs2_addr = inst(rvinst.rs2bh, rvinst.rs2bl)
    val rs1_data = Mux((rs1_addr!=0.U), regfile(rs1_addr), UInt(0, rvspec.xlen))
    val rs2_data = Mux((rs2_addr!=0.U), regfile(rs2_addr), UInt(0, rvspec.xlen))
    
    //Reg-Imm
    val imm_sext = Cat(Fill(20,inst(31)), inst(31, 20)).toUInt

    //Data-path Mux for Reg-Reg and Reg-Imm
    val alu_opd1 = rs1_data
    val alu_opd2 = MuxLookup(
        io.c2d.op2_sel, //switch(io.c2d.op2_sel)
        rvdp.op2_sel.X,  //default: rvdp.op2_sel.X
        Array(
            rvdp.op2_sel.R -> rs2_data,
            rvdp.op2_sel.I -> imm_sext
        )
    )

    //#Part4. RegFile to ALU

    val alu_func = io.c2d.alu_func
    //Ref: https://github.com/freechipsproject/chisel3/wiki/Muxes-and-Input-Selection
    val alu_out = MuxLookup(
        alu_func,             //switch(alu_func) 
        UInt(0, rvspec.xlen), //default: UInt(0, xlen=32)
        Array(
            //Use ordering from up to down in SPEC Vol.I Base Instruction Set, Page 54
            rvalu.ADD   -> (alu_opd1 + alu_opd2).asUInt,
            rvalu.SUB   -> (alu_opd1 - alu_opd2).asUInt,
            //Shift left makes internal process length = w(32) + y(max:32) = 64
            //Ref: https://github.com/freechipsproject/chisel3/wiki/Builtin-Operators
            //Statement: z = x << n	w(z) = w(x) + maxNum(n)
            rvalu.SLL   -> (alu_opd1 << alu_opd2(rvinst.shamtsz-1,0)).asUInt,
            rvalu.SLT   -> (alu_opd1.asSInt < alu_opd2.asSInt).asUInt,
            rvalu.SLTU  -> (alu_opd1 < alu_opd2).asUInt,
            rvalu.XOR   -> (alu_opd1 ^ alu_opd2).asUInt,
            rvalu.SRL   -> (alu_opd1 >> alu_opd2(rvinst.shamtsz-1,0)).asUInt,
            rvalu.SRA   -> (alu_opd1.asSInt >> alu_opd2(rvinst.shamtsz-1,0)).asUInt,
            rvalu.OR    -> (alu_opd1 | alu_opd2).asUInt,
            rvalu.AND   -> (alu_opd1 & alu_opd2).asUInt
        )
    )(rvspec.xlen-1,0) //Force ALU output fits to datapath width

    //#Part5. ALU to Write Back
    val rd_addr = inst(rvinst.rdbh, rvinst.rdbl)
    regfile(rd_addr) := alu_out

    //#Extra. Intuitive RegFile direct access path for Scala/Chisel debug
    io.rfdbg.req.ready := true.B
    io.rfdbg.resp.valid := false.B
    when(io.rfdbg.req.valid) {
        switch(io.rfdbg.req.mfunc) {
            is(mram_op.MF_RD) {
                io.rfdbg.resp.valid := true.B
                io.rfdbg.resp.data := regfile(io.rfdbg.req.addr(5,0)) 
            }
            is(mram_op.MF_WR) {
                regfile(io.rfdbg.req.addr(5,0)) := io.rfdbg.req.data
            }
        }
    }

    when(reset.toBool()) {
        pc := UInt(0, rvspec.xlen)

        var i = 0
        for( i <- 0 until (rvspec.xrsz-1)) {
            regfile(i) := UInt(i, rvspec.xlen)
        }

        io.imem.req.valid := false.B
        io.dmem.req.valid := false.B

        io.rfdbg.req.ready := false.B
    }
}