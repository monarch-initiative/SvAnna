package org.jax.svanna.ingest.hpomap;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;

public class HpoMapping {

  private final TermId otherOntologyTermId;
  private final String otherOntologyLabel;
  private final TermId hpoTermId;
  private final String hpoLabel;

  public static HpoMapping of(TermId otherTermId, String otherLabel, TermId hpoId, String hpoLabel) {
    return new HpoMapping(otherTermId, otherLabel, hpoId, hpoLabel);
  }

  private HpoMapping(TermId tid, String label, TermId hpoId, String hpoLabel){
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HpoMapping that = (HpoMapping) o;
    return Objects.equals(otherOntologyTermId, that.otherOntologyTermId) && Objects.equals(otherOntologyLabel, that.otherOntologyLabel) && Objects.equals(hpoTermId, that.hpoTermId) && Objects.equals(hpoLabel, that.hpoLabel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(otherOntologyTermId, otherOntologyLabel, hpoTermId, hpoLabel);
  }

  @Override
  public String toString() {
    return "HpoMapping{" +
            "otherOntologyTermId=" + otherOntologyTermId +
            ", otherOntologyLabel='" + otherOntologyLabel + '\'' +
            ", hpoTermId=" + hpoTermId +
            ", hpoLabel='" + hpoLabel + '\'' +
            '}';
  }
}
