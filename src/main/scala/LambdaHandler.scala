import java.lang.System.lineSeparator
import java.util

import com.amazonaws.services.dynamodbv2.model.{CreateTableRequest, GlobalSecondaryIndex, LocalSecondaryIndex, TableDescription}
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClient}
import com.amazonaws.services.lambda.runtime.Context
import io.github.mkotsur.aws.handler.Lambda._
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}
import Helpers._

import scala.collection.immutable.Seq

sealed trait Response

case object OK extends Response
case class ErrorMessage(message: String) extends Response


class LambdaHandler extends Proxy[String, Response] {


  override def handle(request: ProxyRequest[String], context: Context): Either[Throwable, ProxyResponse[Response]] = {
    Right(ProxyResponse(statusCode = 201, headers = Some(Map("Content-Type" -> "application/json")), body = Some(OK)))
  }

}

object Main extends App {
}
