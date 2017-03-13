package actors

import java.net._
import java.nio.charset.Charset

import actors.MulticastManager._
import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString


/**
  * Actor encargado de escuchar y enviar paquetes UDP
  *
  * @param supervisor El actor que supervisa este actor
  * @param iface      La interfaz de red usada para escuchar y enviar paquetes
  */
class MulticastManager(supervisor: ActorRef,
                       iface: String) extends Actor with Multicast {

  import context.system

  //Lista de configuraciones necesarias para unirse a un grupo Multicast
  val opts = List(InetProtocolFamily(), MulticastGroup("239.1.2.3", iface))
  //Socket Multicast usado para enviar paquetes
  val multicastSocket = new InetSocketAddress("239.1.2.3", 2249)
  //Se manda un mensaje al sistema operativo para abrir los canales de comunicacion
  IO(Udp) ! Udp.Bind(self, new InetSocketAddress(2249), opts)

  def receive = {
    //Si el sistema operativo responde con el mensaje Bound se cambia el
    //comportamiento del actor a ready() y como parametro se manda una referencia
    //al actor que representa el Socket de red.
    case Udp.Bound(local) =>
      println(s"Listener ready at ${local.getHostName}:${local.getPort}")
      context.become(ready(sender()))
  }

  def ready(socket: ActorRef): Receive = {
    //Si el sistema operativo recibe datos por el Socket se mandan al actor
    //supervisor
    case Udp.Received(data, remote) =>
      println(s"Recieved data from ${remote.getAddress}")
      val processed = data.decodeString(Charset.defaultCharset())
      supervisor ! ListenedData(processed, remote)
    // Si se requiere que se detenga el enlace al socket
    case Udp.Unbind  => socket ! Udp.Unbind
    case Udp.Unbound => context.stop(self)
    //Si se recive un mensaje con datos del editor se envian por la red
    //por medio del socket multicast.
    case ReadyToSend(data) =>
      println(s"About to send through the wire $data")
    socket ! Udp.Send(ByteString.fromString(data), multicastSocket)
  }

}

object MulticastManager {
  //Mensajes usados entre los actores del sistema
  case class ListenedData(data: String, from: InetSocketAddress)
  case class FromEditor(oldValue: String, newValue: String)
  case class ReadyToSend(data: String)
  //Factory method para el Actor MulticastManager. Se especifica que se
  //usara el dispatcher de JavaFX para ejecutar codigo en el hilo de la
  //interfaz de usuario.
  def props(next: ActorRef, interface: String): Props =
    Props(classOf[MulticastManager], next, interface).withDispatcher("akka.javafx-dispatcher")
}