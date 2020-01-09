/**
 * this class represent the terms in the dictionary
 */
package Model;

import javafx.util.Pair;

import java.io.Serializable;
import java.util.*;

public class Term implements Serializable, Comparable {

    //region Class Fields
    private String term;
    private List<Pair<String, Integer>> docsOccurrence;
    private boolean isEntity;
    private int total_tf =0;
    //endregion

    Term(String term, boolean isEntity) {
        this.term = term;
        this.isEntity = isEntity;
        docsOccurrence = new ArrayList<>();
    }

    /**
     *
     * @return the term
     */
    String getTerm() {
        return term;
    }

    /**
     *
     * @return true if the term id entity
     */
    boolean isEntity() {
        return isEntity;
    }

    /**
     * set the term to entity
     * @param entity
     */
    void setEntity(boolean entity) {
        isEntity = entity;
    }

    /**
     * this function checks and decide if then term should be only in capital letters or small letters
     * @param termToMerge
     */
    void mergeTerms(Term termToMerge) {
        this.docsOccurrence.addAll(termToMerge.docsOccurrence);
        char firstC = termToMerge.term.charAt(0);
        if(Character.isLetter(firstC) && Character.isLowerCase(firstC)){
            this.setTermLower();
        }
        this.total_tf +=termToMerge.total_tf;
    }

    /**
     * this function update the tf for the specific term
     * @param docID
     * @param tf
     */
    void addOccurrenceToTerm(String docID, int tf) {
        docsOccurrence.add(new Pair<>(docID, tf));
        total_tf +=tf;
    }

    /**
     *
     * @return the term frequency
     */
    int getTotal_tf() {
        return total_tf;
    }

    /**
     * this function change the term to lower letters
     */
    void setTermLower() {
        term = term.toLowerCase();
    }

    /**
     *
     * @return the hashcode of the term
     */
    @Override
    public int hashCode() {
        return term.toLowerCase().hashCode();
    }

    /**
     *
     * @param obj - term
     * @return if the term are equals
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Term) {
            return this.term.equals(((Term) obj).term);
        }
        return false;
    }

    /**
     *
     * @param o true
     * @return 0 if the terms are the same
     */
    @Override
    public int compareTo(Object o) {

        return String.CASE_INSENSITIVE_ORDER.compare(this.term, ((Term) o).term);

    }

    /**
     *
     * @return string of the specific term
     */
    @Override
    public String toString(){
        StringBuilder st =  new StringBuilder();
        st.append(term).append(":").append(docsOccurrence.size()).append("\n");
        for (Pair pair: docsOccurrence) {
            st.append(pair.getKey()).append(":").append(pair.getValue()).append("\n");
        }
        st.deleteCharAt(st.length()-1);
        return st.toString();
    }

    /**
     * this clss represent comparator for string case insensitive
     */
    static class Comparators {
        static final Comparator<Term> Term = (Term t1, Term t2) -> t1.compareTo(t2);
    }
}



