#!/usr/bin/env bash
cd droidmate-saigen

OUTPUT_DIR=/test/experiment/output
# INPUT_DIR=/test/experiment/input
APKS_DIR=/test/experiment/apks
# NR_SEEDS=11
# ACTION_LIMIT=1000

# mkdir ${INPUT_DIR}
# chmod 777 ${INPUT_DIR}

echo "Cleaning output folder ${OUTPUT_DIR}"
rm -rf ${OUTPUT_DIR}
mkdir ${OUTPUT_DIR}
chmod 777 ${OUTPUT_DIR}

# echo "Cleaning input folder ${INPUT_DIR}/apks/"
# rm -rf ${INPUT_DIR}/apks/droidMate

# echo "Removing previous grammar and input values from ${INPUT_DIR}/apks/"
# rm -rf ${INPUT_DIR}/apks/*.txt

# mkdir ${INPUT_DIR}/apks

# for s in {0..1}
# do
	echo "Stopping emulator"
	cd ..
	./stopEmu.sh

	echo "Starting the emulator"
	./startEmu.sh
	cd droidmate-saigen #?

	echo "Running saigen"
	./gradlew run --args="--Strategies-explore=false --Exploration-apksDir=${APKS_DIR} --Output-outputDir=${OUTPUT_DIR} --Exploration-launchActivityDelay=3000 --Exploration-widgetActionDelay=800 --Selectors-randomSeed=1 --Deploy-installApk=true --Deploy-uninstallApk=true --Selectors-pressBackProbability=0.1" || true
# done

echo "Summary"
#cat ${OUTPUT_DIR}/summary.txt
find ${OUTPUT_DIR}
echo "Done"
