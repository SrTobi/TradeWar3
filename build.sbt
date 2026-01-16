lazy val libgdxVersion = "1.14.0"

scalaVersion := "3.8.0"

lazy val commonSettings = Seq(
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "3.8.0",
)


lazy val core = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(
    name := "tradewar3-core",
    libraryDependencies ++= Seq(
      "com.badlogicgames.gdx" % "gdx" % libgdxVersion,
      "com.badlogicgames.gdx" % "gdx-freetype" % libgdxVersion
    ),
  )

lazy val desktop = (project in file("desktop"))
  .settings(commonSettings: _*)
  .settings(
    name := "tradewar3-desktop",
    libraryDependencies ++= Seq(
      "com.badlogicgames.gdx" % "gdx-backend-lwjgl" % libgdxVersion,
      "com.badlogicgames.gdx" % "gdx-platform" % libgdxVersion classifier "natives-desktop",
      "com.badlogicgames.gdx" % "gdx-freetype-platform" % libgdxVersion classifier "natives-desktop"
    ),
  ).dependsOn(core)

lazy val all = (project in file("."))
  .aggregate(core, desktop)
  .settings(
    name := "tradewar3-all",
  )