package io.kanaka.e2e.plugin

import io.kanaka.e2e.plugin.checker._
import sbt.Keys._
import sbt._
/**
  * @author Valentin Kasas
  */
object E2EPlugin extends AutoPlugin {


  object autoImport {

    lazy val e2eDirectory: SettingKey[File] = SettingKey[File]("e2eDirectory")

    lazy val checkI18N = TaskKey[ConsistencyChecker.ConsistencyCheck]("checkI18N", "Verifies that keys used in the code are defined in the translation files")

    lazy val fixI18N = TaskKey[Unit]("fixI18N", "Interactively fix missing/misused transations")
  }

  import autoImport._

  override def trigger: PluginTrigger = allRequirements


  override def projectSettings: Seq[Setting[_]] = Seq(
    e2eDirectory := resourceManaged.value / ".e2e",
    scalacOptions += s"-Xmacro-settings:${e2eDirectory.value.absolutePath}",
    libraryDependencies += "io.kanaka" %% "e2e-core" % "0.1-SNAPSHOT",
    checkI18N := ConsistencyChecker.verifyAllTranslationFiles(e2eDirectory.value / "key_usages", (resources in Compile).value),
    checkI18N <<= checkI18N dependsOn (compile in Compile),
    fixI18N := I18NFixer.run(checkI18N.value)
  )




}

