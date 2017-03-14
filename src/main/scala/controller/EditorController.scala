package controller

import java.io.{BufferedWriter, FileWriter}
import java.net.{NetworkInterface, URL}
import java.util
import java.util.ResourceBundle
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.Alert.AlertType
import javafx.scene.control._
import javafx.scene.layout.FlowPane
import javafx.stage.{Stage, WindowEvent}

import actors.{Host, Supervisor}
import akka.actor.{ActorRef, ActorSystem, Props}
import com.jfoenix.controls.JFXListView

import scalafx.application.HostServices
import scalafx.stage.FileChooser


class EditorController extends Initializable {


  @FXML
  var textArea: TextArea = _
  @FXML
  var listView: JFXListView[Host] = _
  @FXML
  var cerrarMenuItem: MenuItem = _
  @FXML
  var menuItemSave: MenuItem = _
  @FXML
  var menuItemHelp: MenuItem = _

  lazy val system = ActorSystem("EditorSystem")
  lazy val supervisor = chooseInterfaceAndStartSystem()


  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    println("Controller Intizialized")
    handleCloseMenuEvent(supervisor)
    handleSaveFile()
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
        val alert = new Alert(AlertType.ERROR)
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

  /**
    * Guarda el contenido del TextArea en un archivo
    */
  def handleSaveFile(): Unit = {
    menuItemSave.setOnAction((_: ActionEvent) => {
      val fileChooser = new FileChooser {
        title = "Guardar el archivo"
      }
      val file = fileChooser.showSaveDialog(textArea.getScene.getWindow)
      if (file != null) {
        try {
          val writer = new BufferedWriter(new FileWriter(file))
          writer.write(textArea.getText)
          writer.close()
        } catch  {
          case e: Exception =>
            val alert = new Alert(AlertType.ERROR)
            alert.setTitle("Error")
            alert.setHeaderText("Error al guardar el archivo")
            alert.setContentText(e.getMessage)
            alert.showAndWait()
        }
      }
    })
  }

  /**
    * Muestra una liga al codigo fuente del proyecto
    *
    * @param hostServices Se nececita para abrir el navegador
    */
  def handleHelpOption(hostServices: HostServices): Unit = {
    menuItemHelp.setOnAction((_: ActionEvent) => {
      val alert = new Alert(AlertType.INFORMATION)
      alert.setTitle("Editor Colaborativo")
      alert.setHeaderText("Editor Colaborativo v0.1")
      val fp = new FlowPane()
      val lbl = new Label("Codigo fuente disponible en ")
      val link = new Hyperlink("github.")
      fp.getChildren.addAll(lbl, link)
      link.setOnAction((_:ActionEvent) => {
        alert.close()
        hostServices.showDocument("https://github.com/osocron/EditorColaborativo")
      })
      alert.getDialogPane.contentProperty().set(fp)
      alert.showAndWait()
    })
  }

}
