package generate

import chisel3.stage.ChiselStage
import mult.Multiplier
import net.sourceforge.argparse4j._

object Generate extends App {
  val parser = ArgumentParsers.newFor("MultiplierGenerator").build.description("Generate multiplier code")
  parser.addArgument("--width").dest("width").metavar("W").help("Input bit width").required(true)
  parser.addArgument("--target-dir").dest("targetDir").metavar("DIR").help("Target output directory").setDefault("genrtl")

  try {
    val res = parser.parseArgs(args)
    val width = res.get("width").toString.toInt
    val targetDir = res.get("targetDir").toString
    println(s"Generating multiplier width=$width, target dir=$targetDir")
    val chiselArgs = Array("-X", "verilog", "-e", "verilog", "--target-dir", targetDir)

    (new ChiselStage).emitVerilog(new Multiplier(width), chiselArgs)
  } catch {
    case e : net.sourceforge.argparse4j.helper.HelpScreenException => ;
    case e : net.sourceforge.argparse4j.inf.ArgumentParserException => println(e);
  }
}
