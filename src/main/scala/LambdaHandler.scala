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

  private val tables = List("flavius-test2")

  private val _1: String = Option(System.getenv("AWS_ACCESS_KEY_ID")).getOrElse(throw new  Throwable("No AWS_ACCESS_KEY_ID defined!"))
  private val _2: String = Option(System.getenv("AWS_SECRET_KEY")).getOrElse(throw new  Throwable("No AWS_SECRET_KEY defined!"))

  val client: AmazonDynamoDB =
    AmazonDynamoDBClient
      .builder()
      .build()

  override def handle(request: ProxyRequest[String], context: Context): Either[Throwable, ProxyResponse[Response]] = {
    val tableDescriptions = getTableDescriptions
    deleteTables(context)
    createTables(tableDescriptions, context)
    Right(ProxyResponse(statusCode = 201, headers = Some(Map("Content-Type" -> "application/json")), body = Some(OK)))
  }

  private def getTableDescriptions: Seq[TableDescription] = {
    try {
      tables.map(client.describeTable).map(_.getTable)
    } catch {
      case _: Throwable =>
        List()
    }
  }

  private def deleteTables(context: Context): Unit = {
    try {
      context.getLogger.log(s"Initiate the process of deleting tables $tables"+ lineSeparator())
      tables.map(client.deleteTable)

      while(getTableDescriptions.nonEmpty) {
        context.getLogger.log("Still waiting to delete some tables..."+lineSeparator())
        Thread.sleep(200)
      }
      context.getLogger.log(s"All tables were deleted with success"+lineSeparator())
    } catch {
      case t: Throwable =>
        context.getLogger.log(s"Some tables could not be deleted $t"+lineSeparator())
    }
  }

  case class TableDesc(tableName: String, readCapacity: Long, writeCapacity: Long)

  private def createTables(tableDescriptions: Seq[TableDescription], context: Context) = {
    try {
      context.getLogger.log(lineSeparator())
      context.getLogger.log(s"Creating tables $tableDescriptions"+lineSeparator()+lineSeparator())

      tableDescriptions
        .map { td =>
          context.getLogger.log(s"$td"+lineSeparator())
          new CreateTableRequest(td.getTableName, td.getKeySchema)
            .withAttributeDefinitions(td.getAttributeDefinitions)
            .withBillingMode("PAY_PER_REQUEST")
            .withLocalSecondaryIndexes(td.getLocalSecondaryIndexes.asInstanceOf[util.Collection[LocalSecondaryIndex]])
            .withGlobalSecondaryIndexes(td.getGlobalSecondaryIndexes.asInstanceOf[util.Collection[GlobalSecondaryIndex]])
        }.map(client.createTable)

      while(getTableDescriptions.size < tables.size) {
        context.getLogger.log("Still waiting to create some tables..."+lineSeparator())
        Thread.sleep(200)
      }
      context.getLogger.log("All tables were created with success"+lineSeparator())
    } catch {
      case t: Throwable =>
        context.getLogger.log(s"The following exception was thrown while creating tables $tables: ${t.getMessage}"+lineSeparator())
    }
  }

}

object Main extends App {
}
