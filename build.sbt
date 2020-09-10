scalaVersion  := "2.13.1"
organization  := "ua.levkuts"
name          := "akka-graph-db"

lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion    = "2.6.8"

libraryDependencies += "com.typesafe.akka"     %% "akka-http"                    % akkaHttpVersion
libraryDependencies += "com.typesafe.akka"     %% "akka-http-spray-json"         % akkaHttpVersion
libraryDependencies += "com.typesafe.akka"     %% "akka-actor-typed"             % akkaVersion
libraryDependencies += "com.typesafe.akka"     %% "akka-stream"                  % akkaVersion
libraryDependencies += "com.typesafe.akka"     %% "akka-serialization-jackson"   % akkaVersion
libraryDependencies += "com.thesamet.scalapb"  %% "scalapb-runtime"              % scalapb.compiler.Version.scalapbVersion % "protobuf"
libraryDependencies += "com.typesafe.akka"     %% "akka-persistence-typed"       % akkaVersion
libraryDependencies += "com.typesafe.akka"     %% "akka-persistence-query"       % akkaVersion
libraryDependencies += "com.typesafe.akka"     %% "akka-cluster-tools"           % akkaVersion
libraryDependencies += "com.typesafe.akka"     %% "akka-cluster-sharding-typed"  % akkaVersion
libraryDependencies += "com.typesafe.akka"     %% "akka-persistence-cassandra"   % "1.0.0"
libraryDependencies += "com.lightbend.akka"    %% "akka-stream-alpakka-slick"    % "2.0.0"
libraryDependencies += "org.postgresql"        % "postgresql"                    % "42.2.12"
libraryDependencies += "org.typelevel"         %% "cats-core"                    % "2.1.1"
libraryDependencies += "ch.qos.logback"        % "logback-classic"               % "1.2.3"

libraryDependencies += "com.typesafe.akka"     %% "akka-http-testkit"            % akkaHttpVersion % Test
libraryDependencies += "com.typesafe.akka"     %% "akka-actor-testkit-typed"     % akkaVersion     % Test
libraryDependencies += "com.typesafe.akka"     %% "akka-persistence-testkit"     % akkaVersion     % Test
libraryDependencies += "org.scalatest"         %% "scalatest"                    % "3.2.2"         % Test

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

version in Docker := "latest"
dockerExposedPorts in Docker := Seq(1600, 8080)
dockerBaseImage := "java"
enablePlugins(JavaAppPackaging)