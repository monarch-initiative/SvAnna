package org.jax.svanna.hpomap;

import org.monarchinitiative.phenol.ontology.data.TermId;

public class HpoMapping {

  private final TermId otherOntologyTermId;
  private final String otherOntologyLabel;
  private final TermId hpoTermId;
  private final String hpoLabel;

  public HpoMapping(TermId tid, String label, TermId hpoId, String hpoLabel){
    this.otherOntologyTermId = tid;
    this.otherOntologyLabel = label;
    this.hpoTermId = hpoId;
    this.hpoLabel = hpoLabel;
  }

  public TermId getOtherOntologyTermId() {
    return otherOntologyTermId;
  }

  public String getOtherOntologyLabel() {
    return otherOntologyLabel;
  }

  public TermId getHpoTermId() {
    return hpoTermId;
  }

  public String getHpoLabel() {
    return hpoLabel;
  }
}
