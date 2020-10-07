import sbt.Keys.{ testOptions, _ }
import sbt.{ Developer, ScmInfo, Test, TestFrameworks, Tests, url }

object Commons {
  def commonSettings(project: String) = Seq(
    // Info for Maven Publishing
    // ----------------------------------
    version := "0.9.2",
    organization := "de.upb.cs.uc4",
    organizationName := "uc4",
    homepage := Some(url("https://uc4.cs.upb.de/")),
    scmInfo := Some(ScmInfo(url("https://github.com/upb-uc4/hlf-api"), "scm:git@github.com:upb-uc4/hlf-api.git")),
    developers := List(Developer("UC4", "UC4", "UC4_official@web.de", url("https://github.com/upb-uc4"))),
    licenses := List("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    publishMavenStyle := true,
    crossPaths := false,
    pomIncludeRepository := { _ => false },
    publishArtifact in Test := false,
    // ----------------------------------
    // scala version
    scalaVersion := "2.13.0",
    // append -deprecation to the options passed to the Scala compiler
    scalacOptions += "-deprecation",
    // testOption for test-reports
    testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test_reports/" + project)
  )
}
