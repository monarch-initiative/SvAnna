package org.jax.svann.tspec;

import org.jax.svann.except.SvAnnRuntimeException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * We are moving to Jannovar and do not really need this any longer.
 */
@Deprecated
public class GencodeParser {

    private final List<TssPosition> transcripts;

    public GencodeParser(String path) {
        transcripts = new ArrayList<>();
        try {
            InputStream fileStream = new FileInputStream(path);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(decoder);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue; // comment
                }
                String [] fields = line.split("\t");
                if (fields.length < 9 ) {
                    String msg = String.format("Bad line with %d instead of 9 fields: %s", fields.length, line);
                    throw new SvAnnRuntimeException(msg);
                }
                try {
                    String chr = fields[0];
                    String feature = fields[2];
                    if (! feature.equals("transcript")) {
                        continue;
                    }
                    int start = Integer.parseInt(fields[3]);
                    int end = Integer.parseInt(fields[4]);
                    String strand = fields[6];
                    String attr = fields[8];
                    if (strand.equals("+")) {
                        fromFields(chr,start, true, attr );
                    } else if (strand.equals("-")) {
                        fromFields(chr,end, false, attr );
                    } else {
                        throw new SvAnnRuntimeException("Did not recognize strand \"" + strand +"\"");
                    }
                } catch (Exception e) {
                    System.out.printf("[ERROR] %s from line %s\n", e.getMessage(), line);
                }

            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        if (transcripts.isEmpty()) {
            throw new SvAnnRuntimeException("Could not parse gencode transcripts");
        }
        System.out.printf("[INFO] We parsed %d transcripts from genecode\n", transcripts.size());

    }

    private Optional<String> getMatch(String [] entryList, String key) {
        final Pattern pattern = Pattern.compile("\"([^\"]+)\"");  // "\"([^\"]*)\"")
        for (String entry : entryList) {
            if (entry.length() < 1 + key.length()) {
                return Optional.empty();
            }
             if (entry.trim().startsWith(key)) {
                 String searchString = entry.substring(key.length() + 1);
                 Matcher m = pattern.matcher(searchString);
                 if (m.find()) {
                     return Optional.of(m.group(0));
                 }
            }
        }
        return Optional.empty();
    }

    private void fromFields(String chr, int position, boolean isPos, String attr) {
        String [] semicolonList = attr.split(";");
        Optional<String> geneopt = getMatch(semicolonList, "gene_name");
        Optional<String> transcriptIdOpt = getMatch(semicolonList, "transcript_id");
        if (geneopt.isPresent() && transcriptIdOpt.isPresent()) {
            TssPosition tp = new TssPosition(geneopt.get(),transcriptIdOpt.get(), chr, position, isPos);
            transcripts.add(tp);
        } else {
            System.out.printf("Could not find %s\n", attr);
        }
    }

    public List<TssPosition> getTranscripts() {
        return transcripts;
    }

    public Map<String, List<TssPosition>> getSymbolToTranscriptListMap() {
        Map<String, List<TssPosition>> symMap = new HashMap<>();
        for (var tss : this.transcripts) {
            symMap.putIfAbsent(tss.getGeneSymbol(), new ArrayList<>());
            symMap.get(tss.getGeneSymbol()).add(tss);
        }
        return symMap;
    }
}
