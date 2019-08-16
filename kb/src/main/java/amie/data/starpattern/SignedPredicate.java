/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data.starpattern;

import javatools.datatypes.ByteString;

/**
 *
 * @author jlajus
 */
public class SignedPredicate implements Comparable<SignedPredicate> {

    public int predicate;
    public boolean subject;

    public SignedPredicate(int p, boolean s) {
        this.predicate = p;
        this.subject = s;
    }

    public SignedPredicate inverse() {
        return new SignedPredicate(this.predicate, !this.subject);
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(!(o instanceof SignedPredicate)) {
            return false;
        }
        SignedPredicate sp = (SignedPredicate) o; //
        return (this.predicate.equals(sp.predicate) && this.subject == sp.subject);
    }

    @Override
    public int compareTo(SignedPredicate o) {
        if (this.subject == o.subject) {
            return this.predicate.compareTo(o.predicate);
        } else if (this.subject) {
            return 1;
        } else {
            return -1;
        }
    }
    
    @Override
    public int hashCode() {
        if (subject) {
            return predicate.hashCode();
        } else {
            return predicate.hashCode()^(-1); // XOR 111...1 32 bits.
        }
    }
    
    @Override
    public String toString() {
        if (!subject) {
            return predicate.toString() + "-1";
        }
        return predicate.toString();
    }
}
