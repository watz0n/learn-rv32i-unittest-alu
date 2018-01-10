Build RISCV-32I ALU Module by Chisel3
===

This project is a toy-project to build RISCV-32I by Chisel3 form scratch, start from ALU module design.

Current implementation has these features:

* [RISCV Spec. Volumn I: User-Level ISA RV32I v2.0](https://content.riscv.org/wp-content/uploads/2017/05/riscv-spec-v2.2.pdf), ALU related instructions
* Unit-Test for simple instructions sanity check
* Simuate Magic RAM read instruction from Unit-Test

This project start from the lecture [Berkeley CS152 FA16, L3: From CISC to RISC](http://www-inst.eecs.berkeley.edu/~cs152/fa16/lectures/L03-CISCRISC.pdf), Reg-Reg structure at page 10, until merged structure at page 12. But this diagram I thought it's from the lecture in [Berkeley CS61C SP16](http://inst.eecs.berkeley.edu/~cs61c/sp16/), thus the wire from `inst<16:0>` to `ALU Control` is not suitable for current RV Core module. Therefore, I draw [some diagram](https://github.com/watz0n/learn-rv32i-unittest-alu/tree/master/doc) to demystify the data-flow in fixed RV Core module.

There are no Memory module in project, so I use unit-test function to simulate memory read-back operation for instruction path. And this function work normally to help me continue developing progress.

If you wish to have more fundamental learning material, please reference the previous Chisel3 walkthrough, [chisel3-gcd](https://github.com/watz0n/chisel3-gcd).

Adhere, we are talking about how to use Unit-Test for ALU module functionality in this repo. If you are interesting how I implement this project from scratch and handle the Chisel3 error in detail, please reference [my development notes](https://watz0n.github.io/blog/en-post/2018/01/10/learn-rv32i-series-en.html).But it would be under-construction before the documents for chisel3-gcd is ready.

Setup Chisel3 Build Environment
===

We need two software to use Chisel3: `sbt` and `Verilator 3.906`.
The official project has comprehensive installation guide, or you can reference my progress. By the way, My build system is Ubuntu 16.04 under Windows10 via Bash on Windows, if you like setup same environment, please reference [my old win10 setup-up process](https://github.com/wats0n/install-chisel-win10).

Follow the official Chisel3 installation guide
---
Following the Linux Installation Guide from [Chisel3 project page](https://github.com/freechipsproject/chisel3), 
and it's better to update Verilator to v3.906 from [Sodor project page](https://github.com/librecores/riscv-sodor#building-the-processor-emulators), my environment chosen installation from .tgz package.

My Chisel3 installation progress
---
Here is a simplified progress to setup `sbt` environment:
```bash
#install sbt form chisel3 projcet
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
sudo apt-get update
#Install necessary packages for verilator and chisel3
sudo apt-get install git make autoconf g++ flex bison default-jdk sbt
```
Next step is Verilator installation progress. I usually install Verilator under `~/work/verilator` directory:
```bash
cd ~/
mkdir work
cd work
# reference form riscv-sodor
wget https://www.veripool.org/ftp/verilator-3.906.tgz
tar -xzf verilator-3.906.tgz
mv verilator-3.906 verilator
cd verilator
unset VERILATOR_ROOT
./configure
make
export VERILATOR_ROOT=$PWD
export PATH=$PATH:$VERILATOR_ROOT/bin
```

Get the repo.
===
```bash
git clone https://github.com/watz0n/learn-rv32i-unittest-alu.git
cd learn-rv32i-unittest-alu
```
Directory structue in repo.
===
* .\doc\ : System Overview and Data-Path Diagram
* .\project\ : Project settings and compiled class directory
* .\src\main\scala\ : Chisel3 circuit codes
    * .\src\main\scala\common : Pre-defined constants, Memory interface
    * .\src\main\scala\rv32_dpm : RV32I Reg-Reg and Reg-Imm Merged data-path
    * .\src\main\scala\rv32_ri : RV32I Reg-Imm data-path
    * .\src\main\scala\rv32_rr : RV32I Reg-Reg data-path
* .\src\test\scala\ : Chisel3 test-bench codes
    * .\src\main\test\rv32 : Reg-Reg, Reg-Imm, and Merged Test-Bench

Use Chisel3 Unit-Test Function
===

If you need simple unit-test for your new instruction, you can reference the Chisel code under `./src/test/scala/rv32/` directory, and build your custom test-bench.

Use `sbt` to perform Unit-Test
---
The `sbt` command has convi [testOnly function](https://stackoverflow.com/questions/11159953/scalatest-in-sbt-is-there-a-way-to-run-a-single-test-without-tags). 
Not `test-only`, which I can't make it work.
```bash
# Unit-Test for RV32I Reg-Reg Data-Path
sbt "testOnly *rvsim_rr.RVCoreRRPeekPokeSpec"
# Unit-Test for RV32I Reg-Imm Data-Path
sbt "testOnly *rvsim_ri.RVCoreRIPeekPokeSpec"
# Unit-Test for RV32I Reg-Reg and Reg-Imm Data-Path
sbt "testOnly *rvsim_dpm.RVCoreDPMPeekPokeSpec"
```
The `*` in `sbt "testOnly *rvsim_rr.RVCoreRRPeekPokeSpec"` means we want to test the `RVCoreRRPeekPokeSpec` class in `rvsim_rr` package form ANY directory.

Use pre-defined script to perform Unit-Test
---
```bash
# Unit-Test for RV32I Reg-Reg Data-Path
bash unit-test-alu-rr.sh
# Unit-Test for RV32I Reg-Imm Data-Path
bash unit-test-alu-ri.sh
# Unit-Test for RV32I Reg-Reg and Reg-Imm Data-Path
bash unit-test-alu-dp-merged.sh
```

Clean up function
---
Because we use Chisel3 test function would generate large meta-data like `./test_run_dir`, I've write a script to clean it:
```bash
bash unit-clear.sh
```
There is a more strong cleaner, not only clean meta-data, but also clean compiled Chisel3 class data:
```bash
bash clear-deep.sh
```

Debug by Value Change Dump (VCD) File
===
Under `./test_run_dir` directory, there are tester directories.
For example:
```bash
# Execute
sbt "testOnly *rvsim_rr.RVCoreRRPeekPokeSpec"
#...
# Verilog code would be
./test_run_dir/rvsim_rr.RVCoreRRPeekPokeSpec626129560/rvcore.v
# Verilog VCD File would be
./test_run_dir/rvsim_rr.RVCoreRRPeekPokeSpec626129560/rvcore.vcd
```
* rvsim_rr : unit-test package name
* RVCoreRRPeekPokeSpec : unit-test class name
* 626129560 : random seed for Verilator
* rvcore : the Module class name in unit-test

All test use same directory, result would be overwrite by next test. But it would stop on error for our debug.

Learning Material
===

University Courses (Online Data)
---
* [Berkeley CS61C SP16](http://inst.eecs.berkeley.edu/~cs61c/sp16/) : Prerequisite course of CS152, worth to study the basic ideas from old RISC architecture. The [CS61C FA17](http://inst.eecs.berkeley.edu/~cs61c/fa17/) use new RISCV textbook.
* [Berkeley CS150 FA13](http://www-inst.eecs.berkeley.edu/~cs150/fa13/) : 
This laboratory course has essential concept for ValidIO/Decoupled mechanism.
* [Berkeley CS152 FA16](http://www-inst.eecs.berkeley.edu/~cs152/fa16/): RISCV in real class, suggest reading all lectures to build your database in mind before implementation.

Books
---
* [Programming in Scala, First Edition](https://www.artima.com/pins1ed/) : Comprehensive Scala introduction book, strong recommendation to read before implementation.

Wikis
---
* [Chisel3 Official Wiki](https://github.com/freechipsproject/chisel3/wiki) : 
Lots of Chisel3 use cases and examples.
* [Chisel Learning Journey](https://github.com/librecores/riscv-sodor/wiki/Chisel-Learning-Journey) : Great content about Sodor implementation since 2017/12 update.

Projects
---
* [GitHub chisel3-gcd](https://github.com/watz0n/learn-chisel3-gcd) : My Chisel3 learning experience, focus on how to link HDL (Verilog/VHDL) experience with Chisel3 design pattern, and test Chisel3 test-bench fesibility.

FAQs
===
*Hey! You have some typo or something wrong! Where are you?*
* Directly use issue or pull request
* E-Mail: watz0n.tw@gmail.com
* Website: https://blog.watz0n.tech/
* Backup: https://watz0n.github.io/