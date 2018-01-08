//=======================================================================
// RISCV Magic RAM Interface
// Watson Huang
// Dec 3, 2017
// 
// A magic ram interface for unittest to simulate read/write
//=======================================================================
package rvcommon

import chisel3._
import chisel3.util._

//Ref: https://github.com/freechipsproject/chisel3/wiki/Memories
//Ref: https://github.com/ucb-bar/riscv-sodor/blob/master/src/common/memory.scala

trait mram_op {

    val MF_X = UInt(0,1)
    val MF_RD = UInt(0,1)
    val MF_WR = UInt(1,1)

    val MT_X = UInt(0,2)
    val MT_B = UInt(1,2)
    val MT_H = UInt(2,2)
    val MT_W = UInt(3,2)
}

object mram_op extends mram_op

class mram_io(data_width: Int) extends Bundle {
    val req = new mram_req(data_width)
    val resp = Flipped(new mram_resp(data_width))
    
    //Solve cloneType Error!?
    //Ref: https://github.com/ucb-bar/riscv-sodor/blob/master/src/common/memory.scala
    override def cloneType = { new mram_io(data_width).asInstanceOf[this.type] }
}

class mram_req(data_width: Int) extends Bundle {
    val addr = Output(UInt(rvspec.xlen.W))
    val data = Output(UInt(data_width.W))
    //Use pre-defined data width
    //Ref: https://github.com/ucb-bar/riscv-sodor/blob/master/src/common/memory.scala
    val mfunc = Output(UInt(mram_op.MF_X.getWidth.W)) 
    val mtype = Output(UInt(mram_op.MT_X.getWidth.W))
    val valid = Output(Bool())
    val ready = Input(Bool())

    //Solve cloneType Error!?
    //Ref: https://github.com/ucb-bar/riscv-sodor/blob/master/src/common/memory.scala
    override def cloneType = { new mram_req(data_width).asInstanceOf[this.type] }
}

class mram_resp(data_width: Int) extends Bundle {
    val data = Output(UInt(data_width.W))
    val valid = Output(Bool())
    
    //Solve cloneType Error!?
    //Ref: https://github.com/ucb-bar/riscv-sodor/blob/master/src/common/memory.scala
    override def cloneType = { new mram_resp(data_width).asInstanceOf[this.type] }
}
