package org.jax.svanna.ingest.parse.enhancer.fantom;

/**
 * This class coordinates the calculations by ingesting the sample map and the count matrix
 * @author Peter N Robinson
 */
@Deprecated(forRemoval = true) // in favor of FantomEnhancerParser
public class Fantom5Parser {
//    private final List<FantomEnhancer> enhancers;
//    /** Key -- a FANTOM sample id such as ; value -- {@link FantomSample} object
//     * with UBERON/CL and corresponding HPO Term information.
//     */
//    private final Map<String, AnnotatedTissue> fantomIdToAnnotatedTissueMap;
//    /** Key -- an UBERON/CL id; value - {@link HpoMapping} object with corresponding HPO info. */
//    private final Map<TermId, HpoMapping> hpoMap;
//
//    private final double cpmAtThreshold;
//    private final double tauAtThreshold;
//
//    /**
//     *
//     * @param countsPath path to F5.hg38.enhancers.expression.matrix.gz file
//     * @param samplesPath path to Human.sample_name2library_id.txt file
//     */
//    public Fantom5Parser(Path countsPath, Path samplesPath, int percentileThreshold) {
//        this.hpoMap = HpoTissueMapParser.loadEnhancerMap();
//
//        FantomSampleParser sampleParser = new FantomSampleParser(samplesPath, this.hpoMap);
//        this.fantomIdToAnnotatedTissueMap = sampleParser.getIdToFantomSampleMap();
//        FantomCountMatrixParser cparser = new FantomCountMatrixParser(null, countsPath, fantomIdToAnnotatedTissueMap);
//        this.enhancers = cparser.getEnhancers();
//        this.cpmAtThreshold = cparser.getCpmsAtPercentile(percentileThreshold);
//        this.tauAtThreshold = cparser.getTauAtPercentile(percentileThreshold);
//    }
//
//    /**
//     * Output a list of all FANTOM5 enhancers to file
//     * @param path Path of output file to create and write to.
//     */
//    public void outputEnhancers(String path) {
//        Map<String, Integer> hpoCounter = new HashMap<>();
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
//            String[] header = {"chrom", "start", "end", "tau", "ontology.id", "ontology.label","HPO.id", "HPO.label"};
//            writer.write(String.join("\t", header) + "\n");
//            for (FantomEnhancer fe : enhancers) {
//                TermId tid = fe.getTop().getHpoId();
//                HpoMapping hmap = this.hpoMap.get(tid);
//                if (hmap == null) {
//                    throw new SvAnnRuntimeException("Could not find mapping for " + tid.getValue());
//                }
//                TermId hpoId = hmap.getHpoTermId();
//                String hpoLabel = hmap.getHpoLabel();
//                String otherLabel = hmap.getOtherOntologyLabel();
//                hpoCounter.putIfAbsent(hpoLabel, 0);
//                hpoCounter.merge(hpoLabel, 1, Integer::sum);
//                String line = String.format("%s\t%d\t%d\t%f\t%s\t%s\t%s\t%s\n",
//                        fe.getChromosome(),
//                        fe.getBegin(),
//                        fe.getEnd(),
//                        fe.getTau(),
//                        tid.getValue(),
//                        otherLabel,
//                        hpoId.getValue(),
//                        hpoLabel);
//                writer.write(line);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//       for (Map.Entry<String, Integer> entry : hpoCounter.entrySet()) {
//            System.out.printf("%s: n=%d\n",  entry.getKey(), entry.getValue());
//       }
//    }
//
//
//    public double getCpmThreshold() {
//        return this.cpmAtThreshold;
//    }
//
//    /**
//     * Calculate the value of totalCpm at the given percentile. The intention is to use this to define
//     * a threshold below which to filter out low-expressed Enhancers.
//     * @return
//     */
//    public double getTauThreshold() {
//        return this.tauAtThreshold;
//    }
//
//
//    public List<IngestedEnhancer> getAboveThresholdEnhancers(double cpm, double tau) {
//        return this.enhancers
//                .stream()
//                .filter(e -> e.getTau() > tau)
//                .filter(e -> e.getTotalCpm() > cpm)
//                .collect(Collectors.toList());
//    }




}
