import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import java.net._
import java.nio.channels.DatagramChannel

import akka.io.Inet.{DatagramChannelCreator, SocketOptionV2}

import scala.io.StdIn


class UDPTest extends Actor {

  override def preStart(): Unit = {
    println("Warming up...")
    //val simpleSenderProps = SimpleSender.props(new InetSocketAddress("239.1.2.3", 1234))
    //val simpleSender: ActorRef = context.actorOf(simpleSenderProps)
    val listenerProps = Listener.props(self)
    val listener: ActorRef = context.actorOf(listenerProps)
    listener ! "Hello"
  }

  override def receive: Receive = {
    case SimpleSender.Done =>
      println("Stoping engines...")
      context.stop(self)
    case msg: String => println(s"Recived from chat: $msg")
  }

}

object Main {

  def main(args: Array[String]): Unit = {
    akka.Main.main(Array(classOf[UDPTest].getName))
  }

}

object SimpleSender {
  case object Done
  def props(remote: InetSocketAddress) = Props(classOf[SimpleSender], remote)
}

class SimpleSender(remote: InetSocketAddress) extends Actor {

  import context.system

  IO(Udp) ! Udp.SimpleSender

  def receive = {
    case Udp.SimpleSenderReady =>
      println("Ready to send!")
      context.become(ready(sender()))
  }

  def ready(send: ActorRef): Receive = {
    case msg: String =>
      println(s"Sending: $msg")
      send ! Udp.Send(ByteString(msg), remote)
  }

}

class Listener(nextActor: ActorRef) extends Actor {

  final case class InetProtocolFamily() extends DatagramChannelCreator {
    override def create() =
      DatagramChannel.open(StandardProtocolFamily.INET)
  }

  final case class MulticastGroup(address: String, interface: String) extends SocketOptionV2 {
    override def afterBind(s: DatagramSocket) {
      val group = InetAddress.getByName(address)
      val networkInterface = NetworkInterface.getByName(interface)
      println(s"Joining group ${group.getHostAddress}")
      s.getChannel.join(group, networkInterface)
    }
  }

  import context.system

  val opts = List(InetProtocolFamily(), MulticastGroup("239.1.2.3", "lo"))

  IO(Udp) ! Udp.Bind(self, new InetSocketAddress(1234), opts)

  def receive = {
    case Udp.Bound(local) =>
      println("Ready to play")
      context.become(ready(sender()))
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      println(s"Recieved data from ${remote.getAddress}")
      val processed = data.toString()
      socket ! Udp.Send(data, remote) // example server echoes back
      nextActor ! processed
    case Udp.Unbind  => socket ! Udp.Unbind
    case Udp.Unbound => context.stop(self)
  }

}

object Listener {
  def props(next: ActorRef) = Props(classOf[Listener], next)
}

object AddressApp extends App {
  val ifaces = NetworkInterface.getNetworkInterfaces
  if (ifaces != null) {
    while (ifaces.hasMoreElements) {
      println(ifaces.nextElement().getName)
    }
  }
}
