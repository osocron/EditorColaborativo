package controller

import java.net.{NetworkInterface, URL}
import java.util
import java.util.ResourceBundle
import javafx.collections.FXCollections
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.{ChoiceDialog, MenuItem, TextArea}
import javafx.stage.{Stage, WindowEvent}

import actors.{Host, Supervisor}
import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import com.jfoenix.controls.JFXListView

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalafx.scene.control.Alert
import scalafx.scene.control.Alert.AlertType


class EditorController extends Initializable {


  @FXML
  var textArea: TextArea = _
  @FXML
  var listView: JFXListView[Host] = _
  @FXML
  var cerrarMenuItem: MenuItem = _

  lazy val system = ActorSystem("EditorSystem")
  lazy val supervisor = chooseInterfaceAndStartSystem()


  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    println("Controller Intizialized")
    handleCloseMenuEvent(supervisor)
  }

  /**
    * El usuario escoge una interfaz de la lista de opciones.
    *
    * @return Regresa la seleccion del usuario o None si no se selecciono nada.
    */
  def chooseInterfaceDialog(): Option[String] = {
    //Obtener las interfaces del sistema operativo y agregarlas a una lista
    val enum = NetworkInterface.getNetworkInterfaces
    val choices = new util.ArrayList[String]()
    while (enum.hasMoreElements) choices.add(enum.nextElement().getName)
    //Crear un dialogo con las interfaces encontradas en el sistema operativo
    val dialog = new ChoiceDialog[String]("Seleccione una opcion", choices)
    dialog.setTitle("Seleccion de Interfaces")
    dialog.setHeaderText("Favor de seleccionar la interfaz conectada a internet.")
    dialog.setContentText("Interfaces: ")
    //Esperar a que el usuario seleccione una opcion
    val result = dialog.showAndWait()
    if (result.isPresent) Some(result.get())
    else None
  }

  /**
    * Se le pide al usuario que elija la interfaz por la cual se escucharan
    * y enviaran paquetes UDP. Si el usuario selecciono una opcion se crea
    * el sistema de Actores, de lo contrario se muestra un error y se vuelve
    * a llamar la funcion.
    */
  def chooseInterfaceAndStartSystem(): ActorRef = {
    val interface = chooseInterfaceDialog()
    interface match {
      case Some(iface) => createSupervisor(iface)
      case None =>
        val alert = new Alert(AlertType.Error)
        alert.setTitle("Error")
        alert.setHeaderText(null)
        alert.setContentText("Es necesario seleccionar una opcion!")
        alert.showAndWait()
        chooseInterfaceAndStartSystem()
    }
  }

  /**
    * Se crea el sistema de Actores y se crea el Actor supervisor.
    *
    * @param iface La interfaz de red usada para escuchar y enviar paquetes UDP
    * @return El Actor supervisor creado.
    */
  def createSupervisor(iface: String): ActorRef = {
    system.actorOf(
      Props(
        new Supervisor(
          iface,
          textArea,
          listView,
          FXCollections.observableArrayList[Host]()
        )
      ).withDispatcher("akka.javafx-dispatcher"),
      name = "Supervisor"
    )
  }

  /**
    * Cuando se seleccione la opcion de cerrar se cierra
    * el sistema de Actores
    *
    * @param supervisor El Actor que cumple la funcion de supervisor
    */
  def handleCloseMenuEvent(supervisor: ActorRef): Unit = {
    cerrarMenuItem.setOnAction((_: ActionEvent) => {
      println("Stopping system...")
      supervisor ! Supervisor.StopConnection
    })
  }

  /**
    * Consumir el evento de cerrar ventana para proporcionar
    * una unica manera de finalizar el sistema de Actores
    *
    * @param primaryStage El primaryStage de la aplicacion
    */
  def registerCloseEvent(primaryStage: Stage) = {
    primaryStage.setOnCloseRequest((event: WindowEvent) => {
      event.consume()
    })
  }

}
