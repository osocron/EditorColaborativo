package controller

import java.net._
import java.nio.channels.DatagramChannel
import java.nio.charset.Charset

import akka.actor.{Actor, ActorRef, Props}
import akka.io.Inet.{DatagramChannelCreator, SocketOptionV2}
import akka.io.{IO, Udp}
import controller.MulticastManager.{ListenedData, ReadyToSend}


class MulticastManager(supervisor: ActorRef, iface: String) extends Actor {

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

  val opts = List(InetProtocolFamily(), MulticastGroup("239.1.2.3", iface))

  IO(Udp) ! Udp.Bind(self, new InetSocketAddress(2249), opts)

  def receive = {
    case Udp.Bound(local) =>
      println("Listener ready...")
      context.become(ready(sender()))
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      println(s"Recieved data from ${remote.getAddress}")
      val processed = data.decodeString(Charset.defaultCharset())
      supervisor ! ListenedData(processed, remote)
    case Udp.Unbind  => socket ! Udp.Unbind
    case Udp.Unbound => context.stop(self)
    case ReadyToSend(data) => socket ! data
  }

}

object MulticastManager {
  case class ListenedData(data: String, from: InetSocketAddress)
  case class FromEditor(oldValue: String, newValue: String)
  case class ReadyToSend(data: String)
  def props(next: ActorRef, interface: String): Props =
    Props(classOf[MulticastManager], next, interface).withDispatcher("akka.javafx-dispatcher")
}