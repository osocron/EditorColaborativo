package controller

import java.net.NetworkInterface

/**
  * Created by osocron on 11/03/17.
  */
object AddressApp extends App {
  val ifaces = NetworkInterface.getNetworkInterfaces
  if (ifaces != null) {
    while (ifaces.hasMoreElements) {
      println(ifaces.nextElement().getName)
    }
  }
}
