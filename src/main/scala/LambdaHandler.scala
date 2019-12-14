import java.lang.System.lineSeparator
import java.util
import java.util.concurrent.{CompletableFuture, Future}

import com.amazonaws.services.dynamodbv2.model.{CreateTableRequest, DeleteTableResult, GlobalSecondaryIndex, LocalSecondaryIndex, ProvisionedThroughput, TableDescription}
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBAsync, AmazonDynamoDBAsyncClient, AmazonDynamoDBClient}
import com.amazonaws.services.lambda.runtime.Context
import com.spikhalskiy.futurity.Futurity
import io.github.mkotsur.aws.handler.Lambda._
import io.github.mkotsur.aws.proxy.{ProxyRequest, ProxyResponse}

import scala.collection.immutable.Seq
import scala.compat.java8.FutureConverters
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class LambdaHandler extends Proxy[String, String] {

  private val tables = List("flavius-test")

  private val _1: String = Option(System.getenv("AWS_ACCESS_KEY_ID")).getOrElse(throw new  Throwable("No AWS_ACCESS_KEY_ID defined!"))
  private val _2: String = Option(System.getenv("AWS_SECRET_KEY")).getOrElse(throw new  Throwable("No AWS_SECRET_KEY defined!"))

  val client: AmazonDynamoDBAsync =
    AmazonDynamoDBAsyncClient
      .asyncBuilder()
      .build()

  override def handle(request: ProxyRequest[String], context: Context) = {
    val tableDescriptions = getTableDescriptions
    deleteTables(context)
    //Thread.sleep(2000)
    createTables(tableDescriptions, context)
    Right(ProxyResponse(201))
  }

  private def getTableDescriptions: Seq[TableDescription] = {
    try {
      tables.map(client.describeTable).map(_.getTable)
    } catch {
      case _: Throwable =>
        List()
    }
  }



  private def deleteTables(context: Context) = {
    try {
      context.getLogger.log(s"Deleting tables $tables"+ lineSeparator())
      val scalaFutures = tables.map(client.deleteTableAsync)
        .map(f => FutureConverters.toScala(Futurity.shift(f)))
      waitAll(scalaFutures)
      context.getLogger.log(s"All tables were deleted with success"+lineSeparator())
    } catch {
      case t: Throwable =>
        context.getLogger.log(s"Some tables could not be deleted $t"+lineSeparator())
    }
  }

  private def waitAll[T](futures: Seq[concurrent.Future[T]]) =
    concurrent.Future.sequence(lift(futures))

  private def lift[T](futures: Seq[concurrent.Future[T]]) =
    futures.map(_.map { Success(_) }.recover { case t => Failure(t) })

  //case class TableDesc(tableName: String, readCapacity: Long, writeCapacity: Long)

  private def createTables(tableDescriptions: Seq[TableDescription], context: Context) = {
    try {
      context.getLogger.log(lineSeparator())
      context.getLogger.log(s"Creating tables $tableDescriptions"+lineSeparator()+lineSeparator())
     val res = tableDescriptions
        .map { td =>
          context.getLogger.log(s"$td"+lineSeparator())

          new CreateTableRequest()
            .withTableName(td.getTableName)
            .withProvisionedThroughput(new ProvisionedThroughput(4L, 4L))
            .withAttributeDefinitions(td.getAttributeDefinitions)
            .withKeySchema(td.getKeySchema)
            .withLocalSecondaryIndexes(td.getLocalSecondaryIndexes.asInstanceOf[util.Collection[LocalSecondaryIndex]])
            .withGlobalSecondaryIndexes(td.getGlobalSecondaryIndexes.asInstanceOf[util.Collection[GlobalSecondaryIndex]])
        }.map(client.createTableAsync)
        .map(f => FutureConverters.toScala(Futurity.shift(f)))

      waitAll(res)
      context.getLogger.log("All tables were created with success"+lineSeparator())
    } catch {
      case t: Throwable =>
        context.getLogger.log(s"The following exception was thrown while creating tables $tables: ${t.getMessage}"+lineSeparator())
    }
  }

}

object Main extends App {
}
