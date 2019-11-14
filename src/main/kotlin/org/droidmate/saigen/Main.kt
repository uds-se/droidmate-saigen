package org.droidmate.saigen

import kotlinx.coroutines.runBlocking
import org.droidmate.api.ExplorationAPI
import org.droidmate.configuration.ConfigurationBuilder
import org.droidmate.explorationModel.factory.DefaultModelProvider
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.UUID
import org.droidmate.saigen.Lib.Companion.cachedLabel // = return LabelMatcher.cachedLabel(widget)

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

            println("Writing list of widget uid and their associated results in widgets.txt")
            val widgetsUIDLabelMap = mutableMapOf<UUID, Pair<String, List<String>>>()
            SaigenMF.concreteIDMap.forEach { (key, value) ->
                if (!widgetsUIDLabelMap.containsKey(key.uid)) {
                    SaigenMF.queryMap.forEach { (key2, value2) ->
                        if (key2.second == cachedLabel(key.uid)) {
                            widgetsUIDLabelMap[key.uid] = Pair(key2.second, value2)
                        }
                    }
                }
            }

            val widgetsDebugFile = statisticsDir.resolve("widgets.txt")
            Files.deleteIfExists(widgetsDebugFile)
            Files.createFile(widgetsDebugFile)

            Files.write(widgetsDebugFile, "Widgets debug information, listing widget uuids and potential inputs:\n".toByteArray(), StandardOpenOption.APPEND)
            widgetsUIDLabelMap.forEach { w ->
                Files.write(
                    widgetsDebugFile,
                    (w.key.toString() + " (" + w.value.first + ") = {" + w.value.second.joinToString(",") + "}\n").toByteArray(),
                    StandardOpenOption.APPEND
                )
            }
        }
    }
}
