import ReleaseTransformations._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

organization in ThisBuild := "io.estatico"

lazy val root = project.in(file("."))
  .aggregate(newtypeJS, newtypeJVM, catsTestsJVM)
  .settings(noPublishSettings)

lazy val newtype = crossProject(JSPlatform, JVMPlatform).in(file("."))
  .settings(defaultSettings)
  .settings(releasePublishSettings)
  .settings(name := "newtype")

lazy val newtypeJVM = newtype.jvm
lazy val newtypeJS = newtype.js

lazy val catsTests = crossProject(JVMPlatform).in(file("cats-tests"))
  .dependsOn(newtype)
  .settings(defaultSettings)
  .settings(noPublishSettings)
  .settings(
    name := "newtype-cats-tests",
    description := "Test suite for newtype + cats interop",
    libraryDependencies +=
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v >= 12 =>
          "org.typelevel" %%% "cats-core" % "2.1.1"
        case _ =>
          "org.typelevel" %%% "cats-core" % "2.0.0"
      })
  )

lazy val catsTestsJVM = catsTests.jvm

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val releasePublishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  ),
  homepage := Some(url("https://github.com/estatico/scala-newtype")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo in ThisBuild := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/estatico/scala-newtype"),
      "scm:git:git@github.com:estatico/scala-newtype.git"
    )
  ),
  developers := List(
    Developer("caryrobbins", "Cary Robbins", "carymrobbins@gmail.com", url("http://caryrobbins.com"))
  ),

  credentials ++= (
    for {
      username <- Option(System.getenv().get("SONATYPE_USERNAME"))
      password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
    } yield Credentials(
      "Sonatype Nexus Repository Manager",
      "oss.sonatype.org",
      username,
      password
    )
  ).toSeq
)

lazy val defaultSettings = Seq(
  defaultScalacOptions,
  scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
    case Some((2, n)) if n >= 13 =>
      Seq(
        "-Ymacro-annotations"
      )
  }.toList.flatten,
  defaultLibraryDependencies,
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq(
          compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
        )
      case _ =>
        // if scala 2.13.0-M4 or later, macro annotations merged into scala-reflect
        // https://github.com/scala/scala/pull/6606
        Nil
    }
  }
)

lazy val defaultScalacOptions = scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-Yno-predef", // needed to ensure users can use -Yno-predef
  "-unchecked",
  "-feature",
  "-deprecation",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros"
) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 11)) =>
    Seq("-Xlint")
  case _ =>
    // on scala 2.12+ some spurious unused warnings get triggered
    Seq("-Xlint:-unused,_")
})

lazy val defaultLibraryDependencies = libraryDependencies ++= Seq(
  scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided,
  scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided,
  "org.scalacheck" %%% "scalacheck" % "1.14.3" % Test,
  "org.scalatest" %%% "scalatest" % "3.1.1" % Test,
  "org.scalatestplus" %%% "scalacheck-1-14" % "3.1.1.0" % Test,
)
