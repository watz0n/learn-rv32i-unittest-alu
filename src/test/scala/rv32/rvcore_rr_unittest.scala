//=======================================================================
// RISCV ALU Reg-Imm data-path
// Watson Huang
// Dec 3, 2017
// 
// Unit-test simulation for for Reg-Reg instructions,
// and check mram_io functionality
//=======================================================================
package rvsim_rr

import chisel3._
import chisel3.util._
import chisel3.iotesters._
import org.scalatest._              
import org.scalatest.exceptions._

import rvcore_rr._
import rvcommon._

class RVCoreRRPeekPokeTester(dut: rvcore, rs1:(Int,UInt), rs2:(Int,UInt), rd:(Int,UInt), op:UInt) extends PeekPokeTester(dut)  {

    poke(dut.io.imem.req.ready, 0);
    poke(dut.io.dmem.req.ready, 0);

    step(1)

    val (rs1a:Int, rs1d:UInt) = rs1
    val (rs2a:Int, rs2d:UInt) = rs2
    val (rda:Int, rdd:UInt) = rd

    //printf("#RS1[%d]:%08X, RS2[%d]:%08X\n", UInt(rs1a), UInt(rs1d), UInt(rs2a), UInt(rs2d)); 
    println("#RS1[%d]:%08X, RS2[%d]:%08X".format(rs1a, rs1d.litValue, rs2a, rs2d.litValue)); 

    while(peek(dut.io.rfdbg.req.ready) == BigInt(0)) {
        step(1)
    }

/*
    //Failed on directly assign value, maybe used in HWSteppedIO Tester
    dut.io.rfdbg.req.mfunc := mram_op.MF_WR
    dut.io.rfdbg.req.mtype := mram_op.MT_W
    dut.io.rfdbg.req.addr := UInt(rs1a&0x3F, rvspec.xlen)
    dut.io.rfdbg.req.data := UInt(SInt(rs1d, rvspec.xlen))
    dut.io.rfdbg.req.valid := true.B
    step(1)
*/
    poke(dut.io.rfdbg.req.mfunc, mram_op.MF_WR)
    poke(dut.io.rfdbg.req.mtype, mram_op.MT_W)
    poke(dut.io.rfdbg.req.addr, UInt(rs1a&0x3F, rvspec.xlen))
    poke(dut.io.rfdbg.req.data, UInt(rs1d, rvspec.xlen))
    poke(dut.io.rfdbg.req.valid, true.B)
    step(1)
    poke(dut.io.rfdbg.req.valid, false.B)
    step(1)

    poke(dut.io.rfdbg.req.mfunc, mram_op.MF_WR)
    poke(dut.io.rfdbg.req.mtype, mram_op.MT_W)
    poke(dut.io.rfdbg.req.addr, UInt(rs2a&0x3F, rvspec.xlen))
    poke(dut.io.rfdbg.req.data, UInt(rs2d, rvspec.xlen))
    poke(dut.io.rfdbg.req.valid, true.B)
    step(1)
    poke(dut.io.rfdbg.req.valid, false.B)
    step(1)     

    //Chisel Concatenate lead to verilator fail, treat as BlackBox fail
    //val inst = Cat(UInt(0,1), op(3), UInt(0,5), UInt(rs2a,6), UInt(rs1a,6), op(2,0), UInt(rda,6), UInt(0x33,7))

    //"b0000000_00010_00001_000_00011_0110011" ADD r1,r2,r3
    //"b0000_0000_0010_0000_1000_0001_1011_0011"
    //val inst = 0x002081B3 //Quick Hack for ADD r1, r2, r3
    
    val opval = op.litValue
    val inst =  (opval&0x08)<<(30-3) | 
                (rs2a&0x3F)<<(20-0) | 
                (rs1a&0x3F)<<(15-0) | 
                (opval&0x07)<<(12-0) |
                (rda&0x3F)<<(7-0) |
                0x33 //Reg-Reg opcode
    
    //printf("#Test Inst: %08X\n", Bits(inst)) //Failed, treat as BlackBox fail
    println("#Test Inst: 0x%08X".format(inst))
    
    poke(dut.io.imem.req.ready, true.B)
    while(peek(dut.io.rfdbg.req.ready) == BigInt(0)) {
        step(1)
    }
    step(1)

    poke(dut.io.imem.resp.valid, true.B)
    poke(dut.io.imem.resp.data, inst)
    step(1)
    poke(dut.io.imem.resp.valid, false.B)
    step(1)

    poke(dut.io.rfdbg.req.mfunc, mram_op.MF_RD)
    poke(dut.io.rfdbg.req.mtype, mram_op.MT_W)
    poke(dut.io.rfdbg.req.addr, UInt(rda&0x3F, rvspec.xlen))
    poke(dut.io.rfdbg.req.data, UInt(0x00, rvspec.xlen))
    poke(dut.io.rfdbg.req.valid, true.B)
    var resp_count = 0;
    while((peek(dut.io.rfdbg.resp.valid) == BigInt(0)) && (resp_count<10)) {
        step(1)
        resp_count += 1
    }
    expect(dut.io.rfdbg.resp.valid, 1)
    expect(dut.io.rfdbg.resp.data, UInt(rdd, rvspec.xlen))

    //printf("#RD[%d], expect: %08X, sim:%08X\n", UInt(rda), UInt(rdd), UInt(peek(dut.io.rfdbg.resp.data)));
    println("#RD[%d], expect: %08X, sim:%08X".format(rda, rdd.litValue, peek(dut.io.rfdbg.resp.data))); 

    step(1)
    poke(dut.io.rfdbg.req.valid, false.B)
    step(1)

}

class RVCoreRRPeekPokeSpec extends ChiselFlatSpec with Matchers {
  
  it should "Test1: RVCore should be elaborate normally" in {
    elaborate { 
      new rvcore 
    }
    info("elaborate rvcore done")
  }

  it should "Test2: RVCore Reg-Reg Tester return the correct result" in {
    val manager = new TesterOptionsManager {
      testerOptions = testerOptions.copy(backendName = "verilator")
    }

    var test_count = 0
    val rvinst_rr_tests = List(
    ((1, UInt(1)), (2, UInt(1)), (3, UInt(2)), rvalu.ADD),
    ((1, UInt(21)), (2, UInt(11)), (3, UInt(10)), rvalu.SUB),
    ((1, UInt(0x55)), (2, UInt(1)), (3, UInt(0xAA)), rvalu.SLL),
    //Using string hexdecimal representation, ref: https://github.com/freechipsproject/chisel3/wiki/Datatypes-in-Chisel
    //Default style 0xFFFFFFFF would be interpret as Int(0xFFFFFFFF), 
    //thus for Chisel datatype UInt(Int(0xFFFFFFFF)) => UInt(-1), produce fail operation
    ((1, "hFFFFFFFF".U), (2, UInt(0x00000001)), (3, UInt(1)), rvalu.SLT),
    ((1, "hFFFFFFFF".U), (2, UInt(0x00000001)), (3, UInt(0)), rvalu.SLTU),
    ((1, UInt(0x55)), (2, UInt(0xAA)), (3, UInt(0xFF)), rvalu.XOR),
    ((1, UInt(0xAA)), (2, UInt(1)), (3, UInt(0x55)), rvalu.SRL),
    ((1, "hF0000000".U), (2, UInt(1)), (3, "hF8000000".U), rvalu.SRA),
    ((1, UInt(0xAF)), (2, UInt(0x55)), (3, UInt(0xFF)), rvalu.OR),
    ((1, UInt(0xAF)), (2, UInt(0x55)), (3, UInt(0x05)), rvalu.AND))

    rvinst_rr_tests.foreach { listElement => {
      val (rs1:(Int,UInt), rs2:(Int,UInt), rd:(Int,UInt), op:UInt) = listElement
      test_count += 1
      try {
        chisel3.iotesters.Driver.execute(() => new rvcore, manager) {
          dut => new RVCoreRRPeekPokeTester(dut, rs1, rs2, rd, op)
        } should be (true)
      } catch {
        case tfe: TestFailedException => {
          info("Failed on No.%d tests".format(test_count))
          throw tfe
        }
      }
    }}

    info("Passed %d tests".format(test_count))
    
  }
}
