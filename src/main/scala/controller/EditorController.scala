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

  /**
    * Se le pide al usuario que elija la interfaz por la cual se escucharan
    * y enviaran paquetes UDP. Si el usuario selecciono una opcion se crea
    * el sistema de Actores, de lo contrario se muestra un error y se vuelve
    * a llamar la funcion.
    */
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
      * Se crea el sistema de Actores y se crea el Actor supervisor.
      *
      * @param iface La interfaz de red usada para escuchar y enviar paquetes UDP
      * @return El Actor supervisor creado.
      */
    def createActorSystem(iface: String): ActorRef = {
      val system = ActorSystem("EditorSystem")
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

  }


}
