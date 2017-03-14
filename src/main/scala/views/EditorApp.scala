package views

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.stage.Stage

import controller.EditorController

import scalafx.Includes._
import scalafx.scene.Scene

/**
  * Inicializacion de la interfaz grafica de usuario
  */
class EditorApp extends Application {
  override def start(primaryStage: Stage): Unit = {
    primaryStage.setTitle("Editor Colaborativo")
    val loader = new FXMLLoader(getClass.getResource("Editor.fxml"))
    val root: Parent = loader.load()
    val controller = loader.getController[EditorController]
    controller.registerCloseEvent(primaryStage)
    controller.handleHelpOption(getHostServices)
    primaryStage.setScene(new Scene(root, 700, 500))
    primaryStage.show()
  }
}

object App {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[EditorApp], args: _*)
  }
}