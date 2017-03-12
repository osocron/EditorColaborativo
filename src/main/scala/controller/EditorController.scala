package controller

import java.net.{NetworkInterface, URL}
import java.util
import java.util.ResourceBundle
import javafx.collections.FXCollections
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.{ChoiceDialog, TextArea}

import akka.actor.{ActorRef, ActorSystem, Props}
import com.jfoenix.controls.JFXListView

import scalafx.scene.control.Alert
import scalafx.scene.control.Alert.AlertType



class EditorController extends Initializable {

  @FXML
  var textArea: TextArea = _
  @FXML
  var listView: JFXListView[Host] = _

  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    println("Controller Intizialized")
    chooseInterfaceAndStartSystem()
  }


  def createActorSystem(iface: String): ActorRef = {
    val system = ActorSystem("EditorSystem")
    system.actorOf(Props(
      new Supervisor(iface,
        textArea,
        listView,
        FXCollections.observableArrayList[Host]())).withDispatcher("akka.javafx-dispatcher"),
      name = "Supervisor")
  }

  def chooseInterfaceDialog(): Option[String] = {
    val enum = NetworkInterface.getNetworkInterfaces
    val choices = new util.ArrayList[String]()
    while (enum.hasMoreElements) choices.add(enum.nextElement().getName)
    val dialog = new ChoiceDialog[String]("Seleccione una opcion", choices)
    dialog.setTitle("Seleccion de Interfaces")
    dialog.setHeaderText("Favor de seleccionar la interfaz conectada a internet.")
    dialog.setContentText("Interfaces: ")
    val result = dialog.showAndWait()
    if (result.isPresent) Some(result.get())
    else None
  }

  def chooseInterfaceAndStartSystem(): Unit = {
    val interface = chooseInterfaceDialog()
    interface match {
      case Some(iface) => createActorSystem(iface)
      case None =>
        val alert = new Alert(AlertType.Error)
        alert.setTitle("Error")
        alert.setHeaderText(null)
        alert.setContentText("Es necesario seleccionar una opcion!")
        alert.showAndWait()
        chooseInterfaceAndStartSystem()
    }
  }


}
