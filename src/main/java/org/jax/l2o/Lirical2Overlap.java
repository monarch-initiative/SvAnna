package org.jax.l2o;

import org.jax.l2o.lirical.LiricalHit;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Lirical2Overlap {

    final static double THRESHOLD = 1;
    private final List<LiricalHit> hitlist;

    public Lirical2Overlap(String liricalPath, String Vcf, String outfile) {
        init();
        hitlist = new ArrayList<>();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(liricalPath))) {
            //rank    diseaseName     diseaseCurie    pretestprob     posttestprob    compositeLR     entrezGeneId    variants
            while ((line=br.readLine()) != null) {
                if (line.startsWith("rank")) break;
            }
            while ((line=br.readLine()) != null) {
                String []fields = line.split("\t");
                if (fields.length < 8) {
                    System.err.println("[ERROR] Bad line, less than 8 fields: " + line);
                    continue;
                }
                String dname = fields[1];
                String dcurie = fields[2];
                String posttestprob = fields[4].replace("%","");
                try {
                    double prob = Double.parseDouble(posttestprob);
                    double lr = Double.parseDouble(fields[5].replaceAll(",",""));
                    if (prob > THRESHOLD) {
                        LiricalHit hit = new LiricalHit(dname, dcurie, prob, lr);
                        hitlist.add(hit);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("[ERROR] " + e.getLocalizedMessage());
                    System.err.println("[ERROR] " + line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        System.out.printf("[INFO] We got %d above threshold candidates.\n", hitlist.size());
        System.out.println(Vcf);
    }

    private void init() {

    }
}
