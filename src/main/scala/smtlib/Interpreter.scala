package smtlib

import Commands.{Script, Command}
import CommandResponses.CommandResponse

/*
 * An interpreter is a stateful object that can eval Commands and returns
 * CommandResponse.
 *
 * Note that despite returning the command response, the interpreter should handle the printing
 * of those responses itself. That is because it needs to handle the verbosity and *-output-channel
 * options commands, and will do the correct printing depending on the internal state.
 * The responses are returned as a way to progamatically communicate with a solver.
 */
trait Interpreter {

  def eval(cmd: Command): CommandResponse

}

object Interpreter {

  import java.io.Reader
  import java.io.FileReader
  import java.io.BufferedReader
  import java.io.File

  def execute(script: Script)(implicit interpreter: Interpreter): Unit = {
    for(cmd <- script.commands)
      interpreter.eval(cmd)
  }

  def execute(scriptReader: Reader)(implicit interpreter: Interpreter): Unit = {
    val parser = new Parser(scriptReader)
    execute(Script(parser))
  }

  def execute(file: File)(implicit interpreter: Interpreter): Unit = {
    val parser = new Parser(new BufferedReader(new FileReader(file)))
    execute(Script(parser))
  }

}
