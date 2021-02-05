package org.jax.svanna.ingest.enhancer.fantom;

import org.jax.svanna.core.exception.SvAnnRuntimeException;
import org.jax.svanna.core.reference.Enhancer;
import org.jax.svanna.core.reference.EnhancerTissueSpecificity;
import org.jax.svanna.ingest.enhancer.AnnotatedTissue;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Parse the FANTOM5 F5.hg38.enhancers.expression.matrix file
 * We parse the file multiple times for ease of understanding the code, even though the parsing could be done
 * in a more efficient manner. It is more important to be clear than efficient here, and the run time is still fine.
 * @author Peter Robinson
 */
public class FantomCountMatrixParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(FantomCountMatrixParser.class);

    private static final Pattern ENHANCER_NAME = Pattern.compile("(?<contig>[\\w\\d]+):(?<start>\\d+)-(?<end>\\d+)");

    private static final CoordinateSystem FANTOM_COORDINATE_SYSTEM = CoordinateSystem.oneBased();
    private static final Strand FANTOM_STRAND = Strand.POSITIVE;
    // we do not consider Fantom5 enhancers as developmental enhancers
    private static final boolean FANTOM_ENHANCERS_ARE_DEVELOPMENTAL = false;

    private static final double DOUBLE_EQUALITY_COMPARISON_TOLERANCE = 5E-16;

    /** Path to the F5.hg38.enhancers.expression.matrix file. */
    private final Path fantomCountsPath;
    /** key - FANTOM5 sample id (String); value -- corresponding {@link AnnotatedTissue} object with labels and HPO.*/
    private final Map<String, AnnotatedTissue> idToFantomSampleMap;
    /** key - an UBERON or Cell Ontology id; value -- corresponding {@link AnnotatedTissue} object with labels and HPO.
     * The difference between this and {@link #idToFantomSampleMap} is that this is a map between an UBERON
     * term and the corresponding HPO term and the other map is between the raw sample id and the AnnotatedTissue. In
     * general, multiple FANTOM5 sample ids will be mapped to the same UBERON/Cell Ontology term. We need
     * uberonToAnnotatedTissueMap after we have created the smaller matrix of counts where samples are combined to UBERON terms. */
    private final Map<TermId, AnnotatedTissue> uberonToAnnotatedTissueMap;


    private final static double LOG_2 = Math.log(2);
    /** Number of FANTOM enhancers. */
    private int n_enhancers;
    /** Number of FANTOM samples being analyzed. */
    private int n_samples;
    /** Counter for number of enhancers with a total of less than one overall count in the libraries included. */
    private int n_enhancers_with_less_than_one_count = 0;
    private final int n_selected_samples;

    private final List<FEnhancer> enhancers;

    private final GenomicAssembly assembly;

    /**
     *
     * @param assembly
     * @param hg38CountsPath F5.hg38.enhancers.expression.matrix.gz file
     * @param fantomIdToAnnotatedTissueMap path to Human.sample_name2library_id.txt file
     */
    public FantomCountMatrixParser(GenomicAssembly assembly, Path hg38CountsPath, Map<String, AnnotatedTissue> fantomIdToAnnotatedTissueMap) {
        this.fantomCountsPath = hg38CountsPath;
        if (!hg38CountsPath.toFile().getName().endsWith("gz")) {
            throw new SvAnnRuntimeException("We were expecting a gz file but got " + hg38CountsPath);
        }
        this.assembly = assembly;
        determine_dimensions();
        //enhancers = new ArrayList<>();
        this.idToFantomSampleMap = fantomIdToAnnotatedTissueMap;
        // now get a map for the (much smaller set of) unique UBERON and Cell Ontology terms.
        this.uberonToAnnotatedTissueMap = new HashMap<>();
        for (AnnotatedTissue annotatedTissue : idToFantomSampleMap.values()) {
            this.uberonToAnnotatedTissueMap.putIfAbsent(annotatedTissue.getTissueId(), annotatedTissue);
        }
        n_selected_samples = fantomIdToAnnotatedTissueMap.size();
        String[] enhancerIds;
        List<TermId> tissueList;
        double[][] cpmCounts;
        this.enhancers = new ArrayList<>();
        try  {
            // get the names of the enhancers (e.g., chr1:2345-5412)
            enhancerIds = getEnhancerNames();
            // get the raw counts -- rows, enhancers, columns -- original libraries
            double[][] rawCountsMatrix = getRawcountsMatrix();
            //List of header fields of the F5.hg38.enhancers.expression.matrix file.
            String[] headerFields = getHeaderFields();
            tissueList = getUniqueTissueTermList(headerFields);
            int n_tissues = tissueList.size();
            double[][] byTissueCounts = getByTissueCountsMatrix(rawCountsMatrix, headerFields, tissueList);
            cpmCounts = getCpmCounts(byTissueCounts);
            for (int i=0;i<n_enhancers;i++) {
                List<Double> cpm = new ArrayList<>();
                for (int j=0;j<n_tissues;j++) {
                    cpm.add(cpmCounts[i][j]);
                }
                enhancerFromData(enhancerIds[i], cpm, tissueList).ifPresent(enhancers::add);
            }
        } catch (IOException e) {
            throw new SvAnnRuntimeException(e.getMessage());
        }
        LOGGER.info("We got {} enhancers", enhancers.size());
    }


    List<TermId> getUniqueTissueTermList(String [] headerFields) {
        // first count the number of tissues represented by the data
        // iterate over the header fields and count the number of distinct tissues
        Set<TermId> tissueTypeSet = new HashSet<>();
        for (String tissue : headerFields) {
            if ( this.idToFantomSampleMap.containsKey(tissue)) {
                // the following gets us an UBERON or CL id
                TermId tissueId = this.idToFantomSampleMap.get(tissue).getTissueId();
                tissueTypeSet.add(tissueId);
            }
        }
        return new ArrayList<>(tissueTypeSet);
    }

    double [][] getByTissueCountsMatrix(double [][] rawCountsMatrix,  String [] headerFields, List<TermId> tissueList) {
        // store the indices in a Map for efficiency
        Map<TermId, Integer> tissueToIndexMap = new HashMap<>();
        int i = 0;
        for (TermId tid : tissueList) {
            tissueToIndexMap.put(tid, i);
            i++;
        }
        int n_tissues = tissueList.size();
        double [][]countsMatrix = new double[n_enhancers][n_tissues];

        Map<TermId, Double> typeToCountsMap = new HashMap<>();
        for (int sampleIndex=0; sampleIndex<n_samples;sampleIndex++) {
            // The following limits us to those columns that represent a library that can be mapped to
            // an UBERON/Cell Ontology term, see above.
            String id = headerFields[sampleIndex];
                if (this.idToFantomSampleMap.containsKey(id)) {
                   // this is a column with a mappable tissue.
                    TermId tissueId = this.idToFantomSampleMap.get(id).getTissueId();
                    if (tissueId == null) {
                        // should never happen
                        throw new SvAnnRuntimeException("Bad sample id " + id);
                    }
                    int tissueIndex = tissueToIndexMap.get(tissueId);
                    // add the counts from an individual sample to the corresponding tissue
                    for (int k = 0; k < n_enhancers; k++) {
                        countsMatrix[k][tissueIndex] += rawCountsMatrix[k][sampleIndex];
                }
            }
        }
        return countsMatrix;
    }


    double [][] getCpmCounts(double [][] unnormalizedMatrix) {
        int n_rows = unnormalizedMatrix.length;
        int n_columns = unnormalizedMatrix[0].length;
        System.out.printf("n_rows=%d n_columns=%d\n", n_rows, n_columns);
        double [][] cpm = new double[n_rows][n_columns];
        double []column_totals = new double[n_columns];
        for (int j=0;j<n_columns;j++) {
            for (int i = 0; i < n_rows; i++) {
                column_totals[j] += unnormalizedMatrix[i][j];
            }
        }
        double []cpm_factors = new double[n_columns];
        for (int i=0;i<n_columns;i++) {
            cpm_factors[i] = column_totals[i]/1_000_000;
        }
        for (int j=0;j<n_columns;j++) {
            for (int i = 0; i < n_rows; i++) {
                cpm[i][j] = cpm_factors[j] * unnormalizedMatrix[i][j];
            }
        }
        return cpm;
    }




    /**
     * Extract the names of the enhancers (which are the positions of the enhancers in hg38)
     * @return array of enhancer names
     * @throws IOException if we cannot read the counts file
     */
    String [] getEnhancerNames() throws IOException {
        String [] enhancerIds = new String[n_enhancers];
        InputStream fileStream = Files.newInputStream(fantomCountsPath);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(decoder);

        String line = br.readLine(); // discard header, enhancers names begin on next line
        int i = 0;
        while ((line = br.readLine()) != null) {
            String [] fields = line.split("\t");
            enhancerIds[i] = fields[0];
            i++;
        }
        br.close();
        return enhancerIds;
    }


    /**
     *
     * @return #enhancer x #samples count matrix
     * @throws IOException  if we cannot read the counts file
     */
    double [][] getRawcountsMatrix () throws IOException {
        double[][] countsMatrix = new double[n_enhancers][n_samples];
        InputStream fileStream = Files.newInputStream(fantomCountsPath);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(decoder);
        int enhancer_idx = 0;
        String line = br.readLine(); // discard header, enhancers names begin on next line
        while((line = br.readLine()) != null) {
            String [] fields = line.split("\t");
            // fields[0]; contains the name and is not needed here
            for (int i=1;i< fields.length;i++) {
                double counts = Double.parseDouble(fields[i]);
                countsMatrix[enhancer_idx][i-1] = counts;
            }
            enhancer_idx++;
        }
        br.close();
        return countsMatrix;
    }


    private FantomEnhancer fromData(String posString, List<Double> values, List<TermId> tissueList) {
        String [] F = posString.split(":");
        String chrom = F[0];
        String [] G = F[1].split("-");
        int start = Integer.parseInt(G[0]);
        int end = Integer.parseInt(G[1]);
        // Normalize to counts per million for this library
        double totalCounts = values
                .stream()
                .mapToDouble(Double::doubleValue).sum();
        //specificity(X) = 1 – (entropy(X) / log2(N) )
        double N = values.size();
        double entropy = 0.0;
        for (double v : values) {
            if (v == 0.0) {
                continue; // 0*log(0) is defined to be zero
            }
            v /= totalCounts; // normalize so v 'resembles' a probability
            entropy += v * Math.log(v);
        }
        entropy /= LOG_2;
        entropy *= -1;
        double specificity = 1.0 - entropy/(Math.log(N)/LOG_2);
        int maxIndex = -1;
        double max = -1;
        for (int i=0;i<values.size();i++) {
            double val = values.get(i);
            if (val>max) {
                maxIndex = i;
                max = val;
            }
        }
        TermId topTerm = tissueList.get(maxIndex);
        if (! this.uberonToAnnotatedTissueMap.containsKey(topTerm)) {
            // should never happen
            throw new SvAnnRuntimeException("Could not find " + topTerm.getValue() +" in uberonToAnnotatedTissueMap");
        }
        AnnotatedTissue atissue = this.uberonToAnnotatedTissueMap.get(topTerm);
        return new FantomEnhancer(chrom, start, end, specificity, atissue, totalCounts);
    }


    private Optional<FEnhancer> enhancerFromData(String positionString, List<Double> countsPerMillion, List<TermId> tissueList) {
        Matcher matcher = ENHANCER_NAME.matcher(positionString);
        if (!matcher.matches()) {
            if (LOGGER.isWarnEnabled()) LOGGER.warn("Unparsable enhancer position `{}`", positionString);
            return Optional.empty();
        }

        Contig contig = assembly.contigByName(matcher.group("contig"));
        if (contig == Contig.unknown()) {
            if (LOGGER.isWarnEnabled()) LOGGER.warn("Unknown contig `{}` for enhancer position `{}`", matcher.group("contig"), positionString);
            return Optional.empty();
        }

        Position start = Position.of(Integer.parseInt(matcher.group("start")));
        Position end = Position.of(Integer.parseInt(matcher.group("end")));

        // Normalize to counts per million for this library
        double totalCpmCount = countsPerMillion.stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        if (totalCpmCount - 0. < DOUBLE_EQUALITY_COMPARISON_TOLERANCE) {
            // No read counts per million for this enhancer.
            // No reason to process the enhancer
            return Optional.empty();
        }
        int n = countsPerMillion.size();
        double[] normalizedReadCounts = new double[n];
        for (int i = 0; i < countsPerMillion.size(); i++) {
            // normalize so v 'resembles' a probability
            normalizedReadCounts[i] = countsPerMillion.get(i) / totalCpmCount;
        }
        //specificity(X) = 1 – (entropy(X) / log2(N) )
        double entropy = 0.0;
        for (double readCount : normalizedReadCounts) {
            if (readCount == 0.0) {
                continue; // 0*log(0) is defined to be zero
            }
            entropy += readCount * Math.log(readCount);
        }
        entropy /= LOG_2;
        entropy *= -1;
        double tau = 1.0 - entropy/(Math.log(n)/LOG_2);
        int maxIndex = -1;
        double max = -1;
        for (int i=0; i<normalizedReadCounts.length; i++) {
            double val = normalizedReadCounts[i];
            if (val>max) {
                maxIndex = i;
                max = val;
            }
        }
        TermId topTerm = tissueList.get(maxIndex);

        if (!uberonToAnnotatedTissueMap.containsKey(topTerm)) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Could not find " + topTerm.getValue() +" in uberonToAnnotatedTissueMap");
            return Optional.empty();
        }
        Set<EnhancerTissueSpecificity> specificities = prepareTissueSpecificities(tissueList, normalizedReadCounts, uberonToAnnotatedTissueMap);
        return Optional.of(FEnhancer.of(contig, FANTOM_STRAND, FANTOM_COORDINATE_SYSTEM, start, end,
                "F5:" + positionString, FANTOM_ENHANCERS_ARE_DEVELOPMENTAL, tau, specificities, totalCpmCount));
    }

    private Set<EnhancerTissueSpecificity> prepareTissueSpecificities(List<TermId> tissueList, double[] readCounts, Map<TermId, AnnotatedTissue> uberonToAnnotatedTissueMap) {
        if (tissueList.size() != readCounts.length){ // shouldn't happen
            //
            if (LOGGER.isWarnEnabled()) LOGGER.warn("n tissues ({}) not equal to normalized counts ({})", tissueList.size(), readCounts.length);
            return Set.of();
        }
        Set<EnhancerTissueSpecificity> specificities = new HashSet<>(tissueList.size());
        for (int i = 0; i < tissueList.size(); i++) {
            AnnotatedTissue annotatedTissue = uberonToAnnotatedTissueMap.get(tissueList.get(i));
            if (annotatedTissue == null) {
                if (LOGGER.isWarnEnabled()) LOGGER.warn("Missing `{}` in tissue map", tissueList.get(i));
                continue;
            }
            specificities.add(
                    EnhancerTissueSpecificity.of(
                            Term.of(annotatedTissue.getTissueId(), annotatedTissue.getTissueLabel()),
                            Term.of(annotatedTissue.getHpoId(), annotatedTissue.getHpoLabel()),
                            readCounts[i]));
        }
        return specificities;
    }


    private double getEnhancerThreshold(List<Double> values) {
        return 0;
    }



    public List<FEnhancer> getEnhancers() {
        return this.enhancers;
    }

    /**
     * @return the Key of the map that is associated with the highest value
     */
    private static <K, V extends Comparable<V>> K getMax(Map<K, V> map) {
        Optional<Map.Entry<K, V>> maxEntry = map.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue());
        return maxEntry.get()
                .getKey();
    }




    /**
     * Here, we ingest the header. Note that the zero-th column is not meaningful, and
     * so we remove the first element before returning the array. This means that the
     * index of the header field exactly matches the position of the sample in the
     *
     * @return an array of String representing the fields of the head
     * @throws IOException
     */
    private String [] getHeaderFields() throws IOException {
        InputStream fileStream = Files.newInputStream(this.fantomCountsPath);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(decoder);
        String header = br.readLine();
        br.close();
        String [] fields = header.split("\t");
        // we want to remove the first column
        return Arrays.copyOfRange(fields, 1, fields.length);
    }



    /**
     * Calculate the dimensions of samples and enhancers in the
     * F5.hg38.enhancers.expression.matrix file. This is done before the
     * final parse of the file in order to prepare the analysis.
     */
    private void determine_dimensions() {
        // first let's use this map to ensure that every line has the same number of fields.
        Map<Integer, Integer> fieldNumberMap = new HashMap<>();
        int linecount = 0;
        int N = 0;
        try {
            InputStream fileStream = Files.newInputStream(fantomCountsPath);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(decoder);
            String line;
            while((line = br.readLine()) != null) {
                String [] tmp = line.split("\t");
                N = tmp.length;
                fieldNumberMap.putIfAbsent(N, 0);
                fieldNumberMap.merge(N, 1, Integer::sum); // i.e., += 1
                linecount++;
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fieldNumberMap.size() != 1) {
            System.out.println("[ERROR] Lines with different numbers of fields");
            for (Map.Entry<Integer, Integer> entry : fieldNumberMap.entrySet()) {
                System.out.printf("[ERROR] %d fields: %d times\n", entry.getKey(), entry.getValue());
            }
            throw new SvAnnRuntimeException("Bad number of fields");
        }
        this.n_enhancers = linecount - 1; // subtract one for the header
        this.n_samples = N - 1; // subtract one for the first column
    }

    /**
     * Calculate the value of totalCpm at the given percentile. The intention is to use this to define
     * a threshold below which to filter out low-expressed Enhancers.
     * @param percentile
     * @return
     */
    double getCpmsAtPercentile(int percentile) {
        List<Double> cpms = this.enhancers
                .stream()
                .map(e -> e.tissueSpecificity().stream().mapToDouble(s -> s.specificityValue() * e.totalReadCounts()).sum())
                .collect(Collectors.toList());
        DescriptiveStats dstats = new DescriptiveStats(cpms);
        return dstats.getValueAtPercentile(percentile);
    }

    /**
     * Calculate the value of totalCpm at the given percentile. The intention is to use this to define
     * a threshold below which to filter out low-expressed Enhancers.
     * @param percentile
     * @return
     */
    double getTauAtPercentile(int percentile) {
        List<Double> tauList = this.enhancers
                .stream()
                .map(Enhancer::tau)
                .collect(Collectors.toList());
        DescriptiveStats dstats = new DescriptiveStats(tauList);
        return dstats.getValueAtPercentile(percentile);
    }


}
