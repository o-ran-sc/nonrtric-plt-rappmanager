def reportFile = new File("${project.basedir}/target/site/jacoco-aggregate/index.html")

//println "${reportFile.getAbsolutePath()}"

if (!reportFile.exists() || !reportFile.canRead()) {
    println "[JacocoPrinter] Skipped due to missing report file."
    return
}

reportFile.withReader('UTF-8') { reader ->
    def html = getParser().parseText(reader.readLine())
    def totalRow = html.body.table.tfoot.tr
    def instructionRatio = totalRow.td[2]
    def branchRatio = totalRow.td[4]
    def complexityMissed = getInt(totalRow.td[5])
    def complexity = getInt(totalRow.td[6])
    def complexityRatio = ((complexity - complexityMissed) / complexity) * 100;
    def linesMissed = getInt(totalRow.td[7])
    def lines = getInt(totalRow.td[8])
    def linesRatio = ((lines - linesMissed) / lines) * 100;
    def methodsMissed = getInt(totalRow.td[9])
    def methods = getInt(totalRow.td[10])
    def methodsRatio = ((methods - methodsMissed) / methods) * 100;
    def classesMissed = getInt(totalRow.td[11])
    def classes = getInt(totalRow.td[12])
    def classesRatio = ((classes - classesMissed) / classes) * 100;

    println "Overall coverage: class: ${classesRatio}, method: ${methodsRatio}, line: ${linesRatio}, branch: ${branchRatio}, instruction: ${instructionRatio}, complexity: ${complexityRatio}"
}

Integer getInt(Object object) {
    def valueString = object.toString()
    return Integer.parseInt(valueString.replaceAll(",", ""))
}

XmlSlurper getParser() {
    parser = new XmlSlurper()
    parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
    parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    return parser
}