import io.circe.{Encoder, Json}

object Helpers {

  implicit val encodeFoo: Encoder[Response] = new Encoder[Response] {
    final def apply(a: Response): Json = a match {
      case ErrorMessage(m) => Json.obj(("message", Json.fromString(m)))
      case OK => Json.Null
    }
  }
}
