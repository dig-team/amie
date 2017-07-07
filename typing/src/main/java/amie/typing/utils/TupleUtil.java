///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package amie.typing.utils;
//
//import java.util.Collection;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.NoSuchElementException;
//
///**
// *
// * @author jlajus
// */
//public class TupleObj {
//    public static class productIterator<TupleObj> implements Iterator<TupleObj> {
//
//        Iterable<TupleObj> iter;
//        Iterator<TupleObj> it1, it2;
//        TupleObj el2;
//        @Override
//        public boolean hasNext() {
//            return it1.hasNext() || it2.hasNext();
//        }
//
//        @Override
//        public TupleObj next() {
//            if (!hasNext()) throw new NoSuchElementException();
//            if(!it1.hasNext()) {
//                el2 = it2.next();
//                it1 = iter.iterator();
//            }
//            return it1.next(), el2;
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }
//        
//    } 
//    
//    public static Collection<Object[]> product(Iterable<Object>... s) {
//        Object[] entry = new Object[s.length];
//        Collection<Object[]> res = new LinkedList<Object[]>();
//        for (int i = 0; i < s.length; i++) {
//            if(s[i].iterator().hasNext()) { entry[i] = s[i].iterator().next(); }
//        }
//    }
//}
