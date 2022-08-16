package mult

import chisel3._
import chisel3.util._
import chisel3.util.random.LFSR
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class MultTB(width : Int, seed1 : BigInt, seed2 : BigInt) extends Module {
  val io = IO(new Bundle {
    val cycles = Flipped(new ValidIO(UInt(16.W)))
    val error = Output(Bool())
    val busy = Output(Bool())
  })
  val cycle_count_a = RegInit(init=0.U(16.W))
  val cycle_count_b = RegInit(init=0.U(16.W))
  val mult = Module(new Multiplier(width))
  val error = RegInit(init=0.B)
  val a_in = LFSR(width, increment=mult.io.a.fire, seed=Option(seed1))
  val b_in = LFSR(width, increment=mult.io.b.fire, seed=Option(seed2))
  val aq = Module(new Queue(UInt(width.W), 2))
  val bq = Module(new Queue(UInt(width.W), 2))
  val correct_result = aq.io.deq.bits * bq.io.deq.bits

  // set multiplier inputs
  mult.io.a.valid := cycle_count_a =/= 0.U
  mult.io.a.bits := a_in
  mult.io.b.bits := b_in
  mult.io.b.valid := cycle_count_b =/= 0.U
  mult.io.z.ready := 1.B

  // collect a/b values for later checking
  aq.io.deq.ready := 0.B
  bq.io.deq.ready := 0.B
  aq.io.enq.valid := mult.io.a.fire
  bq.io.enq.valid := mult.io.b.fire
  aq.io.enq.bits := a_in
  bq.io.enq.bits := b_in

  // start testing when valid asserted
  when (io.cycles.valid) {
    cycle_count_a := io.cycles.bits
    cycle_count_b := io.cycles.bits
  }.otherwise {
    when (cycle_count_a > 0.U && mult.io.a.ready) {
      cycle_count_a := cycle_count_a - 1.U
    }
    when (cycle_count_b > 0.U && mult.io.b.ready) {
      cycle_count_b := cycle_count_b - 1.U
    }
  }

  when (mult.io.z.valid) {
    aq.io.deq.ready := 1.B
    bq.io.deq.ready := 1.B
    when (!(aq.io.deq.valid & aq.io.deq.valid & (mult.io.z.bits === correct_result))) {
      error := 1.B
    }
  }

  io.error := error
  io.busy := cycle_count_b =/= 0.U || cycle_count_a =/= 0.U
}

class TestSynthTB extends AnyFreeSpec with ChiselScalatestTester{
  "use synthesizable testbench" in {
    test(new MultTB(8, 10, 20)).withAnnotations(Seq(WriteVcdAnnotation)) {
      c => {
        c.io.cycles.valid.poke(1.B)
        c.io.cycles.bits.poke(5.U)
        c.clock.step(1)
        c.io.cycles.valid.poke(0.B)
        while (c.io.busy.peekBoolean()) {
          c.clock.step(1)
        }
        c.io.error.expect(0.B)
      }
    }
  }
}
