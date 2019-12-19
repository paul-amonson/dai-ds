package com.intel.dai.hwinventory.api

import org.apache.commons.io.FileUtils
import spock.lang.*

class HWInvTranslatorSpec extends Specification {
    HWInvTranslatorCLI ts

    static def dataDir = "src/test/resources/data/"
    static def tmpDir = "build/tmp/"

    def setupSpec() {
        "rm noSuchFile".execute().text
    }
    def setup() {
        ts = new HWInvTranslatorCLI()
    }

    def "Test extractParentId"() {
        def ts = new HWInvTranslator("doesNotMatter",
                "doesNotMatter", null)

        expect: ts.extractParentId(id) == parentId

        where:
        id     || parentId
        "x0n0" || "x0"
        "x0"   || ""
        ""     || ""
        "123"  || ""
    }

    def "Test run args - negative"() {
        String[] myArgs = args
        expect: ts.run(myArgs) == expectedValue

        where:
        args                                                          || expectedValue
        []                                                            || 1
        ["-i", "noSuchFile", "-o", "doesNotMatter"]                   || 1
        ["-i", "noSuchFile", "-o", "doesNotMatter"]                   || 1
        ["-i", dataDir+"HWInvTreeOneNode.sql", "-o", "doesNotMatter"] || 1
        ["-i", "noSuchFile", "-o", "doesNotMatter"]                   || 1
    }

    def "Translate HWbyLoc"() {
        String[] myArgs = ["-i", inputFileName, "-o", outputFileName]
        when:
        def retVal = ts.run(myArgs)
        then:
        retVal == 0
        def inputFile = new File(expectedResultFileName)
        def outputFile = new File(outputFileName)

        expect: FileUtils.contentEquals(inputFile, outputFile)

        where:
        inputFileName                            | outputFileName              || expectedResultFileName
        dataDir+"foreignHwByLoc/flatNode.json"   | tmpDir+"flatNode.tr.json"   || dataDir+"foreignHwByLoc/translated/flatNode.json"
        dataDir+"foreignHwByLoc/nestedNode.json" | tmpDir+"nestedNode.tr.json" || dataDir+"foreignHwByLoc/translated/nestedNode.json"
    }
    def "Translate HWbyLoc Array"() {
        String[] myArgs = ["-i", inputFileName, "-o", outputFileName]
        when:
        def retVal = ts.run(myArgs)
        then:
        retVal == 0
        def inputFile = new File(expectedResultFileName)
        def outputFile = new File(outputFileName)

        expect: FileUtils.contentEquals(inputFile, outputFile)

        where:
        inputFileName                                         | outputFileName                       || expectedResultFileName
        dataDir+"foreignHwByLocList/preview4HWInventory.json" | tmpDir+"preview4HWInventory.tr.json" || dataDir+"foreignHwByLocList/translated/preview4HWInventory.json"
    }
    def "Translate HWInventory"() {
        String[] myArgs = ["-i", inputFileName, "-o", outputFileName]
        when:
        def retVal = ts.run(myArgs)
        then:
        retVal == 0
        def inputFile = new File(expectedResultFileName)
        def outputFile = new File(outputFileName)

        expect: FileUtils.contentEquals(inputFile, outputFile)

        where:
        inputFileName                                               | outputFileName                             || expectedResultFileName
        dataDir+"foreignHwInventory/nestedNodeOnlyHWInventory.json" | tmpDir+"nestedNodeOnlyHWInventory.tr.json" || dataDir+"foreignHwInventory/translated/nestedNodeOnlyHWInventory.json"
        dataDir+"foreignHwInventory/missingFromDoc.json"            | tmpDir+"missingFromDoc.tr.json"            || dataDir+"foreignHwInventory/translated/missingFromDoc.json"
    }
    def "toCanonical from ForeignHWInvByLoc - negative" () {
        def ts = new HWInvTranslator("doesNotMatter", "doesNotMatter", null)
        def arg = new ForeignHWInvByLoc()
        arg.ID = ID
        arg.Type = Type
        arg.Ordinal = Ordinal
        arg.Status = Status
        arg.PopulatedFRU = PopulatedFRU

        expect: ts.toCanonical(arg) == null

        where:
        ID      | Type      | Ordinal   | Status            | PopulatedFRU
        null    | "Type"    | 0         | "Empty"           | null
        "ID"    | null      | 0         | "Empty"           | null
        "ID"    | "Type"    | -1        | "Empty"           | null
        "ID"    | "Type"    | 0         | null              | null
        "ID"    | "Type"    | 0         | "Empty"           | new ForeignFRU()
        "ID"    | "Type"    | 0         | "Populated"       | null
        "ID"    | "Type"    | 0         | "noSuchStatus"    | null
    }
    def "foreignToCanonical - negative" () {
        def ts = new HWInvTranslator(inputFile, "doesNotMatter", null)
        expect: ts.foreignToCanonical() == 1

        where:
        inputFile                                               | dummy
        "doesNotExists"                                         | null
        dataDir+"foreignHwByLoc/translated/flatNode.json"       | null   // already translated
        dataDir+"foreignHwByLoc/flatProcessor.json"             | null   // only nodes are supported
        dataDir+"foreignHwInventory/missingFormat.json"         | null
        dataDir+"foreignHwInventory/missingXname.json"          | null
        dataDir+"foreignHwInventory/missingXnameAndFormat.json" | null
    }
    def "extractParentId" () {
        def ts = new HWInvTranslator("doesNotMatter", "doesNotMatter", null)
        expect: ts.extractParentId(candidate) == result

        where:
        candidate       | result
        "x0"            | ""
        "x0c0s0b0n0"    | "x0c0s0b0"
        "x0c0s0b0n"     | ""
        "^#^@!"         | ""
        null            | null
    }
}
