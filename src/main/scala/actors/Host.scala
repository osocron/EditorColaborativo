package actors


/**
  * Esta clase representa los usuarios del Editor Colaborativo
  *
  * @param address La direccion IP de los usuarios del editor.
  */
case class Host(address: String) {
  override def toString: String = address
}
