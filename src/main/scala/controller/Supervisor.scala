package controller

import javafx.collections.ObservableList
import javafx.scene.control.TextArea

import akka.actor.{Actor, ActorRef}
import com.jfoenix.controls.JFXListView

import scala.collection.mutable

/**
  * Created by osocron on 11/03/17.
  */
class Supervisor(interface: String, textArea: TextArea,
                 listView: JFXListView[Host],
                 hosts: ObservableList[Host]) extends Actor {

  case class ManagerReady(a: ActorRef)

  override def preStart(): Unit = {
    println("Warming up Akka...")
    val managerProps = MulticastManager.props(self, interface)
    val manager: ActorRef = context.actorOf(managerProps)
    self ! ManagerReady(manager)
  }

  override def receive: Receive = {
    case ManagerReady(manager) =>
      println("Supervisor ready...")
      context.become(ready(manager))
  }

  def ready(listener: ActorRef): Receive = {
    case MulticastManager.FromEditor(_, newValue) =>
      listener ! MulticastManager.ReadyToSend(newValue)
    case MulticastManager.ListenedData(data, from) =>
      handleIncomingText(data)
      handleIncomingHost(Host(from.getAddress.getHostAddress))
  }

  def handleIncomingText(text: String): Unit = {
    textArea.setText(text)
    textArea.positionCaret(text.length)
  }

  def handleIncomingHost(h: Host): Unit = {
    val hostNameArray = mutable.MutableList[String]()
    val iterator = hosts.iterator()
    while (iterator.hasNext) hostNameArray += iterator.next().address
    if (!hostNameArray.contains(h.address)) hosts.add(h)
    listView.setItems(hosts)
  }

}
