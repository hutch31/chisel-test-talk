package mult

import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3._

import scala.util.Random

class TestMultiplier extends AnyFreeSpec with ChiselScalatestTester{
  /*
   * This example shows a cycle-by-cycle test where the test needs to have
   * variable waits built-in
   */
  "multiply two numbers" in {
    test(new Multiplier(8)).withAnnotations(Seq(WriteVcdAnnotation)) {
      c => {
        def mul (a : Int, b : Int) = {
          // A input asserted through whole cycle
          c.io.a.valid.poke(1.B)
          c.io.a.bits.poke(a.U)
          c.io.a.ready.expect(0.B)
          c.io.b.valid.poke(1.B)

          // B should only be asserted for first cycle, so step for one cycle
          // and then check
          c.io.b.bits.poke(b.U)
          c.io.b.ready.expect(1.B)
          c.clock.step(1)
          c.io.b.valid.poke(0.B)

          // Wait until valid signal is asserted
          while (!c.io.z.valid.peekBoolean())
            c.clock.step(1)

          // Check for correct answer
          c.io.z.bits.expect((a*b).U)

          // Check that Z.valid is deasserted one cycle later
          c.io.z.ready.poke(1.B)
          c.clock.step(1)
          c.io.z.valid.expect(0.B)
        }

        mul(1, 1)
        mul(2, 2)
        mul(7, 3)
        mul(254, 55)
      }
    }
  }

  "multiply lots of numbers" in {
    test(new Multiplier(8)).withAnnotations(Seq(WriteVcdAnnotation)) {
      c => {
        c.io.a.initSource().setSourceClock(c.clock)
        c.io.b.initSource().setSourceClock(c.clock)
        c.io.z.initSink().setSinkClock(c.clock)
        val rand = new Random(1)

        val total_count = 250
        var a_count: Int = 0
        var b_count: Int = 0
        var rx_count: Int = 0
        val a_in = for (i <- 0 until total_count) yield rand.nextInt(254)
        val b_in = for (i <- 0 until total_count) yield rand.nextInt(254)
        val z_out = for (i <- 0 until total_count) yield a_in(i) * b_in(i)

        fork {
          c.io.a.enqueueSeq(for (a <- a_in) yield a.U)
        }.fork {
          c.io.b.enqueueSeq(for (b <- b_in) yield b.U)
        }.fork {
          c.io.z.expectDequeueSeq(for (z <- z_out) yield z.U)
        }.join()
      }
    }
  }

  /*
   * This test shows how to use the enqueue/dequeue methods to send data to
   * interfaces with ready/valid, and increase test coverage using random
   * delays
   */
  "muliply lots of numbers with flow control" in {
    test(new Multiplier(8)).withAnnotations(Seq(WriteVcdAnnotation)) {
      c => {
        c.io.a.initSource().setSourceClock(c.clock)
        c.io.b.initSource().setSourceClock(c.clock)
        c.io.z.initSink().setSinkClock(c.clock)
        val rand = new Random(1)

        val total_count = 250
        var a_count: Int = 0
        var b_count: Int = 0
        var rx_count: Int = 0
        val a_in = for (i <- 0 until total_count) yield rand.nextInt(254)
        val b_in = for (i <- 0 until total_count) yield rand.nextInt(254)
        val z_out = for (i <- 0 until total_count) yield a_in(i) * b_in(i)

        fork {
          while (a_count < total_count) {
            if (rand.nextFloat() > 0.35) {
              c.clock.step(1)
            }
            c.io.a.enqueue(a_in(a_count).U)
            a_count += 1
          }
        }.fork {
          while (b_count < total_count) {
            if (rand.nextFloat() > 0.35) {
              c.clock.step(1)
            }
            c.io.b.enqueue(b_in(b_count).U)
            b_count += 1
          }
        }.fork {
          while (rx_count < total_count) {
            if (rand.nextFloat() > 0.65) {
              c.clock.step(1)
            }
            c.io.z.expectDequeue(z_out(rx_count).U)
            rx_count += 1
          }
        }.join()
      }
    }
  }
}
