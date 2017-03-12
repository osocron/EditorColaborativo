package controller

import javafx.collections.ObservableList
import javafx.scene.control.TextArea

import akka.actor.{Actor, ActorRef}
import com.jfoenix.controls.JFXListView

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
      textArea.setText(data)
      hosts.add(Host(from.getHostName))
      listView.setItems(hosts)
  }

}
