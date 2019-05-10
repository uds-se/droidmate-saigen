package org.droidmate.saigen

import kotlinx.coroutines.runBlocking
import org.droidmate.api.ExplorationAPI
import org.droidmate.command.ExploreCommandBuilder
import org.droidmate.configuration.ConfigurationBuilder
import org.droidmate.exploration.SelectorFunction
import org.droidmate.exploration.StrategySelector
import org.droidmate.saigen.storage.DictionaryProvider
import org.droidmate.saigen.storage.LinkProvider
import org.droidmate.saigen.storage.Storage

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
                // debug()
                // System.exit(0)
                val cfg = ConfigurationBuilder().build(args)
                val builder = ExploreCommandBuilder.fromConfig(cfg)

                ExplorationAPI.explore(cfg, builder)

                /** *
                 * To get the coverage, app should first be instrumented
                 * To instrument uncomment the next lines
                 */
                /*CoverageCommand(cfg,
                    Instrumenter(cfg.resourceDir, cfg[StatementCoverageMF.Companion.StatementCoverage.onlyCoverAppPackageName], true)
                ).execute()

                System.exit(0)*/

                val saigenSelector: SelectorFunction = { eContext, pool, _ ->
                    val saigen by lazy { eContext.getOrCreateWatcher<SaigenMF>() }
                    saigen.join()

                    pool.getFirstInstanceOf(SaigenRandom::class.java)
                }

                val commandBuilder = ExploreCommandBuilder.fromConfig(cfg)
                    // Add custom strategies
                    .withStrategy(SaigenRandom(cfg.randomSeed))
                    .withStrategy(SaigenCAM(emptyList(), 0))
                    // Remove random and cannotExplore
                    .remove(StrategySelector.randomWidget)
                    // Add custom selector
                    .append("CAM", camSelector, arrayOf(getCAMs()))
                    .append("SAIGEN", saigenSelector)

                ExplorationAPI.explore(
                    args,
                    commandBuilder = commandBuilder,
                    watcher = emptyList()
                )
            }
        }

        private fun getCAMs(): List<CAM> {
            // TODO Pending...
            // val loginCam = CAM(listOf("username", "password"), listOf("log in"))
            return emptyList()
        }

        private fun debug() {
            val link = Storage(sortedSetOf(LinkProvider(), DictionaryProvider(mapOf("name" to listOf("first name", "second name")))))

            val r = link.query(listOf("address", "name", "city", "email", "phone", "car"))

            r.forEach { result ->
                println("GroupId: ${result.queryId}\tLabel: ${result.label}\tValues: ${result.values.joinToString(" | ")}")
            }
        }

        /* Wordnik dict

        System.setProperty("WORDNIK_API_KEY", "72d81865c31c2912fd866055fc20c53890e8ff1b85a019f3d")

            val status = AccountApi.apiTokenStatus()
            if (status.isValid) {
                println("API key is valid.")
            } else {
                println("API key is invalid!")
                System.exit(1)
            }

            // get a list of definitions for a word
            val def = WordApi.definitions("siren")
            println("Found " + def.size + " definitions.")

            var i = 1
            for (d in def) {
                println(i++.toString() + ") " + d.partOfSpeech + ": " + d.text)
            }

            val syn = WordApi.related("siren", true, setOf(Knicker.RelationshipType.synonym), 50)
            println("Found " + syn.size + " synonyms.")
            i = 1
            for (d in syn.first().words) {
                println(i++.toString() + ") " + d)
            }
         */
    }
}