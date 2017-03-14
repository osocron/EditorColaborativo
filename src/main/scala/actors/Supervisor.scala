package actors

import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.ObservableList
import javafx.scene.control.TextArea

import actors.Supervisor.{StopConnection, TerminateSystem}
import akka.actor.{Actor, ActorRef, PoisonPill}
import akka.io.Udp
import com.jfoenix.controls.JFXListView

import scala.collection.mutable

/**
  * Actor encargado de procesar los datos obtenidos de la red y de supervisar
  * al actor encargado de obtener y enviar datos UDP.
  *
  * @param interface El nombre de la interfaz de red (eth0, en1, lo, etc...)
  * @param textArea  Referencia al TextArea del editor.
  * @param listView  Referencia el ListView donde se mostraran los usuarios
  * @param hosts     Lista de usuarios
  */
class Supervisor(interface: String,
                 textArea: TextArea,
                 listView: JFXListView[Host],
                 hosts: ObservableList[Host]) extends Actor {

  import MulticastManager._

  case class ManagerReady(a: ActorRef)

  override def preStart(): Unit = {
    println("Warming up Akka System...")
    //Creacion del actor MulticastManager
    val managerProps = MulticastManager.props(self, interface)
    val manager: ActorRef = context.actorOf(managerProps)
    //Registro del listener al textArea
    textArea.textProperty().addListener(textListener)
    //Se manda un mensaje indicando que se ha completado la primera fase
    self ! ManagerReady(manager)
  }

  override def receive: Receive = {
    case ManagerReady(manager) =>
      println("Supervisor ready...")
      //Pasar al estado ready
      context.become(ready(manager))
  }

  def ready(listener: ActorRef): Receive = {
    //Cuando se reciven cambios de parte del editor se envian al MulticastManager
    //para enviar los datos.
    case FromEditor(_, newValue) =>
      listener ! ReadyToSend(newValue)
    //Cuando se reciven datos de la red se actualiza el texto y se agrega
    //al usuario a la lista si es necesario
    case ListenedData(data, from) =>
      handleIncomingText(data)
      handleIncomingHost(Host(from.getAddress.getHostAddress))
    //Cerrar el sistema de actores
    case StopConnection => listener ! Udp.Unbind
    case TerminateSystem => context.system.terminate()
  }

  /**
    * Cuando se obtienen datos de la red se agregan al editor conservando
    * la posicion del Caret.
    *
    * @param text Los datos recibidos de la red
    */
  def handleIncomingText(text: String): Unit = {
    //Solo actualizar el editor si los cambios son externos
    if (text != textArea.getText) {
      //Remover el textListener para evitar disparar eventos no necesarios
      textArea.textProperty().removeListener(textListener)
      val pos = textArea.getCaretPosition
      textArea.setText(text)
      textArea.positionCaret(pos)
      //Volver a registrar el textListener
      textArea.textProperty().addListener(textListener)
    }
  }

  /**
    * Cuando llega informacion de la red se agrega la direccion IP a la
    * lista de Hosts solo si no existe en la lista para evitar
    * duplicados
    *
    * @param h El host a ser agregado a la lista
    */
  def handleIncomingHost(h: Host): Unit = {
    val hostNameArray = mutable.MutableList[String]()
    val iterator = hosts.iterator()
    while (iterator.hasNext) hostNameArray += iterator.next().address
    if (!hostNameArray.contains(h.address)) hosts.add(h)
    listView.setItems(hosts)
  }

  //El listener que se registra al TextArea
  val textListener = new ChangeListener[String]() {
    override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String) = {
      self ! MulticastManager.FromEditor(oldValue, newValue)
    }
  }

}

object Supervisor {
  case object StopConnection
  case object TerminateSystem
}
