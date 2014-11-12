package colossus.examples

import akka.actor._
import akka.util.ByteString
import colossus.IOSystem
import colossus.core.ServerRef
import colossus.service._
import colossus.protocols.redis._
import scala.concurrent.{Promise, Future}
import UnifiedProtocol._

class KeyValDB extends Actor {

  import KeyValDB._

  val db = collection.mutable.Map[ByteString, ByteString]()

  def receive = {
    case Get(key, promise) => promise.success(db.get(key))
    case Set(key, value, promise) => {
      db(key) = value
      promise.success(())
    }
  }
}

object KeyValDB {
  case class Get(key: ByteString, promise: Promise[Option[ByteString]] = Promise())
  case class Set(key: ByteString, value: ByteString, promise: Promise[Unit] = Promise())
}

object KeyValExample {
  
  def start(port: Int)(implicit io: IOSystem): ServerRef = {
    import io.actorSystem.dispatcher

    val db = io.actorSystem.actorOf(Props[KeyValDB])

    Service.become[Redis]("key-value-example", port){
      case Command("GET", args) => {
        val dbCmd = KeyValDB.Get(args(0))
        db ! dbCmd
        dbCmd.promise.future.map{
          case Some(value) => BulkReply(value)
          case None => NilReply
        }
      }
      case Command("SET", args) => {
        val dbCmd = KeyValDB.Set(args(0), args(1))
        db ! dbCmd
        dbCmd.promise.future.map{_ =>
          StatusReply("OK")
        }
      }
    }
  }
}
        
