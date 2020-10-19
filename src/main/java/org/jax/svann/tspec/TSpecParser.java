package org.jax.svann.tspec;

import org.jax.svann.except.SvAnnRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TSpecParser {

    Map<TermId, String> id2labelMap;
    Map<TermId, List<Enhancer>> id2enhancerMap;

    public TSpecParser(String path) {
        id2labelMap = new HashMap();
        id2enhancerMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String [] fields = line.split("\t");
                if (fields.length != 6) {
                    throw new SvAnnRuntimeException("Bad tspec line: " + line);
                }
                String chr = fields[0];
                int start  = Integer.parseInt(fields[1]);
                int end  = Integer.parseInt(fields[2]);
                double tau = Double.parseDouble(fields[3]);
                TermId tid = TermId.of(fields[4]);
                String label = fields[5];
                id2labelMap.putIfAbsent(tid, label);
                Enhancer enhancer = new Enhancer(chr, start, end, tau, tid);
                id2enhancerMap.putIfAbsent(tid, new ArrayList<>());
                id2enhancerMap.get(tid).add(enhancer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<TermId, String> getId2labelMap() {
        return id2labelMap;
    }

    public Map<TermId, List<Enhancer>> getId2enhancerMap() {
        return id2enhancerMap;
    }
}
