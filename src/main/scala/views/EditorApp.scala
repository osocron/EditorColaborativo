package views

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.stage.Stage

import scalafx.Includes._
import scalafx.scene.Scene

class EditorApp extends Application {
  override def start(primaryStage: Stage): Unit = {
    primaryStage.setTitle("Editor Colaborativo")
    val root: Parent = FXMLLoader.load(getClass.getResource("Editor.fxml"))
    primaryStage.setScene(new Scene(root, 700, 500))
    primaryStage.show()
  }
}

object App {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[EditorApp], args: _*)
  }
}