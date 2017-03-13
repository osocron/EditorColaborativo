package actors

import java.net.{DatagramSocket, InetAddress, NetworkInterface, StandardProtocolFamily}
import java.nio.channels.DatagramChannel

import akka.io.Inet.{DatagramChannelCreator, SocketOptionV2}

/**
  * Created by osocron on 13/03/17.
  */
trait Multicast {

  /**
    * Crear una instancia de un protocolo de red basado en datagramas.
    */
  final case class InetProtocolFamily() extends DatagramChannelCreator {
    override def create() =
      DatagramChannel.open(StandardProtocolFamily.INET)
  }

  /**
    * Clase que permite unirse a un grupo Multicast.
    *
    * @param address    La direccion IP del grupo a unirse.
    * @param interface  La interfaz de red usada para escuchar y enviar paquetes
    *                   UDP.
    */
  final case class MulticastGroup(address: String, interface: String) extends SocketOptionV2 {
    override def afterBind(s: DatagramSocket) {
      val group = InetAddress.getByName(address)
      val networkInterface = NetworkInterface.getByName(interface)
      println(s"Joining group ${group.getHostAddress}")
      s.getChannel.join(group, networkInterface)
    }
  }

}
