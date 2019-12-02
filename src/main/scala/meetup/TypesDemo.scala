package meetup

import cats.effect._
import cats.implicits._
import io.estatico.newtype.macros._
import io.estatico.newtype.ops._
import eu.timepit.refined.api._
import eu.timepit.refined.auto._
import eu.timepit.refined.collection.Contains
import eu.timepit.refined.types.string.NonEmptyString
import scala.util.control.NoStackTrace
import shapeless._

object TypesDemo extends IOApp {
  def putStrLn[A](a: A): IO[Unit] = IO(println(a))

  def showName(username: String, name: String, email: String): String =
    s"""
      Hi $name! Your username is $username
      and your email is $email.
     """

  val p0: IO[Unit] =
    putStrLn(showName("gvolpe@github.com", "12345", "foo"))

  // ----------------- Value classes -------------------
  import types._

  def showNameV(username: UserNameV, name: NameV, email: EmailV): String =
    s"""
      Hi ${name.value}! Your username is ${username.value}
      and your email is ${email.value}.
     """

  val badUserName = UserNameV("gvolpe@github.com")

  val p1: IO[Unit] =
    putStrLn(
      showNameV(
        badUserName.copy(value = ""),
        NameV("12345"),
        EmailV("foo")
      )
    )

  // TODO: Add abstract private class trick

  // ----------------- Newtypes -------------------

  def showNameT(username: UserNameT, name: NameT, email: EmailT): String =
    s"""
      Hi ${name.value}! Your username is ${username.value}
      and your email is ${email.value}.
     """

  val p2: IO[Unit] =
    putStrLn(
      showNameT(
        "gvolpe@github.com".coerce[UserNameT],
        "12345".coerce[NameT],
        "".coerce[EmailT]
      )
    )

  // ----------------- Smart Constructors -------------------

  def mkUsername(value: String): Option[UserNameT] =
    if (value.nonEmpty) value.coerce[UserNameT].some else None

  def mkName(value: String): Option[NameT] =
    if (value.nonEmpty) value.coerce[NameT].some else None

  def mkEmail(value: String): Option[EmailT] =
    if (value.contains("@")) value.coerce[EmailT].some else None

  case object EmptyError extends NoStackTrace
  case object InvalidEmail extends NoStackTrace

  val p3: IO[Unit] =
    for {
      u <- mkUsername("gvolpe").liftTo[IO](EmptyError)
      n <- mkName("George").liftTo[IO](EmptyError)
      e <- mkEmail("123").liftTo[IO](InvalidEmail)
      _ <- putStrLn(showNameT(u, n, e))
    } yield ()

  // ----------------- Refinement Types -------------------

  def showNameR(username: UserNameR, name: NameR, email: EmailR): String =
    s"""
      Hi ${name.value}! Your username is ${username.value}
      and your email is ${email.value}.
     """

  val p4: IO[Unit] =
    putStrLn(
      showNameR("jr", "Joe", "jr@gmail.com")
    )

  // ----------------- Newtypes + Refined -------------------

  def showNameTR(username: UserName, name: Name, email: Email): String =
    s"""
      Hi ${name.value.value}! Your username is ${username.value.value}
      and your email is ${email.value.value}.
     """

  val p5: IO[Unit] =
    putStrLn(
      showNameTR(
        UserName("jr"),
        Name("John"),
        Email("foo@bar.com")
      )
    )

  def run(args: List[String]): IO[ExitCode] =
    p4.as(ExitCode.Success)
}

object types {
  case class UserNameV(value: String) extends AnyVal
  case class NameV(value: String) extends AnyVal
  case class EmailV(value: String) extends AnyVal

  @newtype case class UserNameT(value: String)
  @newtype case class NameT(value: String)
  @newtype case class EmailT(value: String)

  type UserNameR = NonEmptyString
  type NameR     = NonEmptyString
  type EmailR    = String Refined Contains['@']

  @newtype case class UserName(value: NonEmptyString)
  @newtype case class Name(value: NonEmptyString)
  @newtype case class Email(value: String Refined Contains['@'])
}
