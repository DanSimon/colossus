import akka.actor.ActorSystem
import colossus.IOSystem
import colossus.protocols.http.{Http, HttpRequest}
import colossus.protocols.http.HttpMethod.Get
import colossus.protocols.http.UrlParsing.{Root, on}
import colossus.protocols.http.server.{HttpServer, Initializer, RequestHandler}
import colossus.service.GenRequestHandler.PartialHandler

object HttpClientExample extends App {
  implicit val actorSystem = ActorSystem()
  implicit val ioSystem = IOSystem()

  // #example
  HttpServer.start("example-server", 9000) {
    new Initializer(_) {

      val httpClient = Http.client("google.com", 80)

      override def onConnect = new RequestHandler(_) {
        override def handle: PartialHandler[Http] = {
          case request@Get on Root =>
            val asyncResult = httpClient.send(HttpRequest.get("/#q=mysearch"))
            asyncResult.map { response =>
              val body = response.body.bytes.utf8String
              request.ok(body)
            }
        }
      }
    }
  }
  // #example
}
