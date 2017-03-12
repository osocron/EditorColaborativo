package controller

import java.net.URL
import java.util.ResourceBundle
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.FXCollections
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.TextArea

import akka.actor.{ActorRef, ActorSystem, Props}
import com.jfoenix.controls.JFXListView


class EditorController extends Initializable {

  @FXML
  var textArea: TextArea = _
  @FXML
  var listView: JFXListView[Host] = _

  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    println("Controller Intizialized")
    val supervisor = createActorSystem()
    addTextProperty(supervisor)
  }


  def createActorSystem(): ActorRef = {
    val system = ActorSystem("EditorSystem")
    system.actorOf(Props(
      new Supervisor("wlp9s0",
        textArea,
        listView,
        FXCollections.observableArrayList[Host]())).withDispatcher("akka.javafx-dispatcher"),
      name = "Supervisor")
  }

  def addTextProperty(supervisor: ActorRef) = {
    textArea.textProperty().addListener(new ChangeListener[String]() {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String) = {
        supervisor ! newValue
      }
    })
  }


}
