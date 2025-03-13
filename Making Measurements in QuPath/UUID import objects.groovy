//0.4.x
// from https://forum.image.sc/t/importing-cell-types-back-into-qupath-by-object-id/76718/8?u=research_associate
// Path to CSV file
def path = "/path/to/cellmeta.csv"

// Color separator
def delim = ","

// Get a map from cell ID -> cell
def cells = getCellObjects()
def cellsById = cells.groupBy(c -> c.getID().toString())

// Read lines
def lines = new File(path).readLines()
def header = lines.pop().split(delim)

// Handle each line
for (def line in lines) {
    def map = lineToMap(header, line.split(delim))
    def id = map['Object ID']
    def cell = cellsById[id]
    if (cell == null) {
        println "WARN: No cell found for $id"
        continue
    }
    // Can set a list of classifications like this (will be auto-converted to PathClass in v0.4)
    cell.classifications = [map['PhenoGraph_clusters']]
}

// Helper function to create a map from column headings -> values
Map lineToMap(String[] header, String[] content) {
    def map = [:]
    if (header.size() != content.size()) {
        throw new IllegalArgumentException("Header length doesn't match the content length!")
    }
    for (int i = 0; i < header.size(); i++)
        map[header[i]] = content[i]
    return map
}