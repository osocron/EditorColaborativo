package controller

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString

/**
  * Created by osocron on 11/03/17.
  */
class Sender(remote: InetSocketAddress) extends Actor {
  import context.system
  IO(Udp) ! Udp.SimpleSender

  def receive = {
    case Udp.SimpleSenderReady =>
      println("Sender ready...")
      context.become(ready(sender()))
  }

  def ready(send: ActorRef): Receive = {
    case msg: String =>
      println(s"Sending through the wire $msg at ${remote.getHostName}")
      send ! Udp.Send(ByteString(msg), remote)
  }
}

object Sender {
  def props(remote: InetSocketAddress): Props = Props(classOf[Sender], remote)
}