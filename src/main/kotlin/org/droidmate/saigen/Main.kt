package org.droidmate.saigen

import kotlinx.coroutines.runBlocking
import org.droidmate.api.ExplorationAPI
import org.droidmate.configuration.ConfigurationBuilder
import org.droidmate.explorationModel.factory.DefaultModelProvider
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.UUID

/**
 * Example run config:
 *
 * VM Options: -Dkotlinx.coroutines.debug -Dlogback.configurationFile=default-logback.xml
 * Args: --Selectors-randomSeed=0 --Selectors-actionLimit=1000 --DeviceCommunication-deviceOperationDelay=0 --UiAutomatorServer-waitForIdleTimeout=1000 --UiAutomatorServer-waitForInteractableTimeout=1000 --StatementCoverage-onlyCoverAppPackageName=true --StatementCoverage-enableCoverage=true --Deploy-replaceResources=true --Deploy-installAux=true --Deploy-installMonitor=false
 */
class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runBlocking {
                val cfg = ConfigurationBuilder().build(args)

                /** *
                 * To get the coverage, app should first be instrumented
                 * To instrument uncomment the next lines
                 */
                /*CoverageCommand(cfg,
                    Instrumenter(cfg.resourceDir, cfg[StatementCoverageMF.Companion.StatementCoverage.onlyCoverAppPackageName], true)
                ).execute()

                System.exit(0)*/

                val commandBuilder = ExplorationAPI.buildFromConfig(cfg)
                // Add custom strategies
                commandBuilder.withStrategy(SaigenRandom(commandBuilder.getNextSelectorPriority()))
                    .withStrategy(SaigenCAM(commandBuilder.getNextSelectorPriority(), emptyList()))

                ExplorationAPI.explore(
                    args,
                    commandBuilder,
                    modelProvider = DefaultModelProvider()
                )

                writeStatisticsToFile()
            }
        }

        // This method must be executed after SaigenMF.context was initialized. Kinda hacky but a good way to get baseDir.
        private fun writeStatisticsToFile() {
            if (!SaigenMF.isContextInitialized()) {
                return
            }

            println("Writing stats.txt")

            val baseDir = SaigenMF.context.model.config.baseDir
            // val statisticsDir = Paths.get(cfg[ConfigProperties.Output.outputDir].path).toAbsolutePath().resolve("statistics").toAbsolutePath()
            val statisticsDir = baseDir.toAbsolutePath().resolve("statistics").toAbsolutePath()
            if (!Files.exists(statisticsDir))
                Files.createDirectories(statisticsDir)
            val statisticsFile = statisticsDir.resolve("stats.txt")
            Files.deleteIfExists(statisticsFile)
            Files.createFile(statisticsFile)

            val uniqueWidgets = mutableMapOf<UUID, Int>()
            SaigenMF.concreteIDMap.forEach { (key, value) ->
                if (!uniqueWidgets.containsKey(key.uid) || value != 0)
                    uniqueWidgets[key.uid] = value
            }

            Files.write(
                statisticsFile,
                ("#total input fields found: " + SaigenMF.concreteIDMap.size + "\n").toByteArray(),
                StandardOpenOption.APPEND
            )
            Files.write(
                statisticsFile,
                ("#total input fields filled automatically (DBPedia, DictionaryProvider): " + SaigenMF.concreteIDMap.filterValues { it == 1 }.size + "\n").toByteArray(),
                StandardOpenOption.APPEND
            )

            Files.write(
                statisticsFile,
                ("#unique input fields found: " + uniqueWidgets.size + "\n").toByteArray(),
                StandardOpenOption.APPEND
            )
            Files.write(
                statisticsFile,
                ("#unique input fields filled automatically (DBPedia, DictionaryProvider): " + uniqueWidgets.filterValues { it == 1 }.size + "\n").toByteArray(),
                StandardOpenOption.APPEND
            )

            println("Writing querydebug.txt")
            val queryDebugFile = statisticsDir.resolve("querydebug.txt")
            Files.deleteIfExists(queryDebugFile)
            Files.createFile(queryDebugFile)

            Files.write(queryDebugFile, "Query debug information:\n".toByteArray(), StandardOpenOption.APPEND)
            SaigenMF.queryMap.forEach { q ->
                Files.write(
                    queryDebugFile,
                    (q.key.second + " = {" + q.value.joinToString(",") + "}\n").toByteArray(),
                    StandardOpenOption.APPEND
                )

                if (q.key.second in SaigenMF.allQueriedLabels) {
                    SaigenMF.allQueriedLabels.remove(q.key.second)
                }
            }

            Files.write(
                queryDebugFile,
                ("Labels for which we could not get any results: " + SaigenMF.allQueriedLabels + "\n").toByteArray(),
                StandardOpenOption.APPEND
            )
        }
    }
}