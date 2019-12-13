import io.circe.generic.auto._
import io.github.mkotsur.aws.handler.Lambda._
import com.amazonaws.services.lambda.runtime.Context
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}


class LambdaHandler extends Proxy[String, String] {

  override def handle(request: ProxyRequest[String], context: Context) =
    Right(ProxyResponse(201))

}

object Main extends App {
}
