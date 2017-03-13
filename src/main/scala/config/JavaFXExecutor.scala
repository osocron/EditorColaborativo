package config

import akka.dispatch.{DispatcherPrerequisites, ExecutorServiceFactory, ExecutorServiceConfigurator}
import com.typesafe.config.Config
import java.util.concurrent.{ExecutorService, AbstractExecutorService, ThreadFactory, TimeUnit}
import java.util.Collections
import javafx.application.Platform

/**
  * Configuracion necesaria para ejecutar codigo concurrente
  * en los hilos GUI de JavaFX
  */
object JavaFXExecutorService extends AbstractExecutorService {
  override def shutdown(): Unit = ()
  override def isTerminated: Boolean = false
  override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true
  override def shutdownNow(): java.util.List[Runnable] = Collections.emptyList[Runnable]
  override def isShutdown: Boolean = false
  override def execute(command: Runnable): Unit = Platform.runLater(command)
}

class JavaFXEventThreadExecutorServiceConfigurator(config: Config,
                                                   prerequisites: DispatcherPrerequisites)
  extends ExecutorServiceConfigurator(config, prerequisites) {
  private val f = new ExecutorServiceFactory {
    def createExecutorService: ExecutorService = JavaFXExecutorService
  }
  def createExecutorServiceFactory(id: String, threadFactory: ThreadFactory): ExecutorServiceFactory = f
}



