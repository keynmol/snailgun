package sailgun

import sailgun.logging.Logger
import sailgun.logging.SailgunLogger
import sailgun.protocol.Defaults
import sailgun.protocol.Protocol
import sailgun.protocol.Streams

import java.net.Socket
import java.nio.file.Paths
import java.nio.file.Path
import java.io.PrintStream
import java.io.InputStream
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import java.net.SocketException

class TcpClient(addr: InetAddress, port: Int) extends Client {
  def run(
      cmd: String,
      args: Array[String],
      cwd: Path,
      env: Map[String, String],
      streams: Streams,
      logger: Logger,
      stop: AtomicBoolean
  ): Int = {
    val socket = new Socket(addr, port)
    try {
      val in = socket.getInputStream()
      val out = socket.getOutputStream()
      val protocol = new Protocol(streams, cwd, env, logger, stop)
      protocol.sendCommand(cmd, args, out, in)
    } finally {
      try {
        if (socket.isClosed()) ()
        else {
          try socket.shutdownInput()
          finally {
            try socket.shutdownOutput()
            finally socket.close()
          }
        }
      } catch {
        case _: SocketException => ()
      }
    }
  }
}

object TcpClient {
  def apply(host: String, port: Int): TcpClient = {
    new TcpClient(InetAddress.getByName(host), port)
  }

  def main(args: Array[String]): Unit = {
    val client = TcpClient(Defaults.Host, Defaults.Port)
    val streams = Streams(System.in, System.out, System.err)
    val logger = new SailgunLogger("tcp-logger", System.out, isVerbose = false)

    val code = client.run(
      "about",
      new Array(0),
      Defaults.cwd,
      Defaults.env,
      streams,
      logger,
      new AtomicBoolean(false)
    )

    logger.debug(s"Return code is $code")
    System.exit(code)
  }
}
