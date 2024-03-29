package api.routes.article

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import api.utils.{AuthValidators, exceptionHandlers}
import database.operations.ArticleQ
import org.ektorp.DocumentNotFoundException
import org.json4s.DefaultFormats
import org.json4s.native.Serialization
import scala.util.{Failure, Success}


class Get extends Directives {

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  val dbOperations = new ArticleQ
  val auth = new AuthValidators

  val route: Route = concat(
    path("article") {
      handleExceptions(exceptionHandlers.articleExceptionHandler) {
        authenticateOAuth2(realm = "secure site", auth.myUserPassAuthenticator) { auth =>
          authorize(auth) {
            get {
              parameter("id".as[String]) { id =>

                val documentToReturn = dbOperations.getDocumentByID(id)
                onComplete(documentToReturn) {
                  case Success(article) =>
                    val articleToReturn = Serialization.write(article)
                    if (articleToReturn != "null") {
                      complete(articleToReturn)
                    } else {
                      throw new DocumentNotFoundException("Document not found.")
                    }
                  case Failure(exception) => complete(StatusCodes.InternalServerError, s"Error occurred. Reason: ${exception.getMessage}")
                }
              }
            }
          }
        }
    }
  },
    path("article") {
      handleExceptions(exceptionHandlers.articleExceptionHandler) {
        get {
          parameter("id".as[String]) { id =>

            val documentToReturn = dbOperations.getDocumentByID(id)
            onComplete(documentToReturn) {
              case Success(article) =>
                if (Serialization.write(article) == "null") throw new DocumentNotFoundException("Document not found.")
                else if (article.published) complete(Serialization.write(article))
                else if (!article.published) complete(StatusCodes.Forbidden, "Does not have necessary access.")
                else complete(StatusCodes.InternalServerError, "Document cannot be processed.")

              case Failure(exception) => complete(StatusCodes.InternalServerError, s"Error occurred. Reason: ${exception.getMessage}")
            }
          }
        }
      }
    }
  )
}
