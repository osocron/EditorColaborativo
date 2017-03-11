package controller

import java.net.{InetAddress, InetSocketAddress}
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

  override def preStart(): Unit = {
    println("Warming up Akka...")
    val listenerProps = Listener.props(self, interface)
    val listener: ActorRef = context.actorOf(listenerProps)
    val senderProps = Sender.props(new InetSocketAddress(InetAddress.getByName("239.1.2.3"), 2250))
    val sender: ActorRef = context.actorOf(senderProps)
    self ! (listener, sender)
  }

  override def receive: Receive = {
    case (listener: ActorRef, sender: ActorRef) =>
      println("Supervisor ready...")
      context.become(ready(listener, sender))
  }

  def ready(listener: ActorRef, sender: ActorRef): Receive = {
    case msg: String =>
      println(s"Sender about to send ${msg}")
      sender ! msg
    case Listener.ListenedData(data, from) =>
      textArea.setText(data)
      hosts.add(Host(from.getHostName))
      listView.setItems(hosts)
  }

}
