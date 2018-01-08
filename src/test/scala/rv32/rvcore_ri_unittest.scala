//=======================================================================
// RISCV ALU Reg-Imm data-path
// Watson Huang
// Dec 4, 2017
// 
// Unit-test simulation for for Reg-Imm instructions
//=======================================================================
package rvsim_ri

import chisel3._
import chisel3.util._
import chisel3.iotesters._
import org.scalatest._              
import org.scalatest.exceptions._

import rvcore_ri._
import rvcommon._

class RVCoreRIPeekPokeTester(dut: rvcore, rs1:(Int,UInt), imm:UInt, rd:(Int,UInt), op:UInt) extends PeekPokeTester(dut)  {

    poke(dut.io.imem.req.ready, 0);
    poke(dut.io.dmem.req.ready, 0);

    step(1)

    val (rs1a:Int, rs1d:UInt) = rs1
    val (rda:Int, rdd:UInt) = rd

    println("#RS1[%d]:%08X, imm:%08X".format(rs1a, rs1d.litValue, imm.litValue)); 

    while(peek(dut.io.rfdbg.req.ready) == BigInt(0)) {
        step(1)
    }

    poke(dut.io.rfdbg.req.mfunc, mram_op.MF_WR)
    poke(dut.io.rfdbg.req.mtype, mram_op.MT_W)
    poke(dut.io.rfdbg.req.addr, UInt(rs1a&0x3F, rvspec.xlen))
    poke(dut.io.rfdbg.req.data, UInt(rs1d, rvspec.xlen))
    poke(dut.io.rfdbg.req.valid, true.B)
    step(1)
    poke(dut.io.rfdbg.req.valid, false.B)
    step(1)  
    
    val opval = op.litValue
    val immval = imm.litValue
    val inst =  (opval&0x08)<<(30-3) |
                (immval&0x3FF)<<(20-0) | 
                (rs1a&0x3F)<<(15-0) | 
                (opval&0x07)<<(12-0) |
                (rda&0x3F)<<(7-0) |
                0x13 //Reg-Imm opcode

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

class RVCoreRIPeekPokeSpec extends ChiselFlatSpec with Matchers {
  
  it should "Test1: RVCore should be elaborate normally" in {
    elaborate { 
      new rvcore 
    }
    info("elaborate rvcore done")
  }

  it should "Test2: RVCore Reg-Imm Tester return the correct result" in {
    val manager = new TesterOptionsManager {
      testerOptions = testerOptions.copy(backendName = "verilator")
    }

    var test_count = 0
    val rvinst_ri_tests = List(
    ((1, UInt(1)), UInt(1), (3, UInt(2)), rvalu.ADD),
    ((1, "hFFFFFFFF".U), UInt(0x00000001), (3, UInt(1)), rvalu.SLT),
    ((1, "hFFFFFFFF".U), UInt(0x00000001), (3, UInt(0)), rvalu.SLTU),
    ((1, UInt(0x55)), UInt(0xAA), (3, UInt(0xFF)), rvalu.XOR),
    ((1, UInt(0xAF)), UInt(0x55), (3, UInt(0xFF)), rvalu.OR),
    ((1, UInt(0xAF)), UInt(0x55), (3, UInt(0x05)), rvalu.AND),
    ((1, UInt(0x55)), UInt(1), (3, UInt(0xAA)), rvalu.SLL),
    ((1, UInt(0xAA)), UInt(1), (3, UInt(0x55)), rvalu.SRL),
    ((1, "hF0000000".U),UInt(1), (3, "hF8000000".U), rvalu.SRA))

    rvinst_ri_tests.foreach { listElement => {
      val (rs1:(Int,UInt), imm:UInt, rd:(Int,UInt), op:UInt) = listElement
      test_count += 1
      try {
        chisel3.iotesters.Driver.execute(() => new rvcore, manager) {
          dut => new RVCoreRIPeekPokeTester(dut, rs1, imm, rd, op)
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
