package com.aston.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import ognl.Ognl;
import ognl.OgnlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"rawtypes", "unchecked"})
public class FlowScript {

    private final static Logger LOGGER = LoggerFactory.getLogger(FlowScript.class);

    private static final ConcurrentHashMap<String, Object> expressions = new ConcurrentHashMap<>();
    private Map<String, Object> root = new HashMap<>();
    private Map ctx = null;

    public static FlowScript create(){
        return new FlowScript();
    }

    public static FlowScript create(Map<String, Object> map){
        FlowScript script = new FlowScript();
        if(map!=null) script.root = map;
        return script;
    }

    public FlowScript add(String path, Object val) {
        if(ctx!=null) throw new RuntimeException("ctx is closed");
        root.put(path, val);
        return this;
    }

    public boolean execWhere(String expr) throws OgnlException {
        if(expr==null) return false;
        Object r = execExpr(expr);
        if (r instanceof Boolean b) return b;
        if (r instanceof Number n) return n.intValue() != 0;
        if (r instanceof String s) return !s.isEmpty();
        return r!=null;
    }

    public Map<String, Object> execMap(Map mapExpr) throws OgnlException {
        if(mapExpr==null || mapExpr.isEmpty()) return null;
        Map<String, Object> m2 = new HashMap<>();
        for(Map.Entry e : ((Map<Object,Object>)mapExpr).entrySet()){
            String key = e.getKey().toString();
            if(key.startsWith("$$")) {
                if (e.getValue() instanceof String expr) {
                    try{
                        m2.put(key.substring(2), execExpr(expr));
                    }catch (OgnlException ee){
                        LOGGER.debug("ignore property {} expression {} error {}", key, expr, ee.getMessage());
                    }
                    continue;
                }
            }
            if(key.startsWith("$")) {
                if (e.getValue() instanceof String expr) {
                    m2.put(key.substring(1), execExpr(expr));
                    continue;
                }
            }
            if(e.getValue() instanceof Map map2) {
                m2.put(key, execMap(map2));
                continue;
            }
            if(e.getValue()!=null) {
                m2.put(key, e.getValue());
            }
        }

        return m2;
    }

    public Map<String, String> execMapS(Map<String, String> mapExpr) throws OgnlException {
        if(mapExpr==null || mapExpr.isEmpty()) return null;
        Map<String, String> m2 = new HashMap<>();
        for(Map.Entry<String, String> e: mapExpr.entrySet()){
            if(e.getKey().startsWith("$") && e.getValue()!=null){
                Object val = execExpr(e.getValue());
                if(val!=null){
                    m2.put(e.getKey().substring(1), val.toString());
                }
            } else {
                m2.put(e.getKey(), e.getValue());
            }
        }
        return m2;
    }

    public Object execExpr(String expr) throws OgnlException {
        if(expr==null) return null;
        Object expr2 = expressions.computeIfAbsent(expr, this::parseExpr);
        if(expr2==null) return null;
        if(ctx==null) ctx = Ognl.createDefaultContext(root);
        try{
            return Ognl.getValue(expr2, ctx, root);
        }catch (OgnlException e){
            if(e.getReason() instanceof RuntimeException re) throw re;
            throw e;
        }
    }

    private Object parseExpr(String expr) {
        try{
            return Ognl.parseExpression(expr);
        }catch (Exception e){
            throw new RuntimeException("parse expression "+expr, e);
        }
    }

    public static class LazyMap extends HashMap<String, Object> {
        @Override
        public Object get(Object key) {
            Object val = super.get(key);
            if(val instanceof RuntimeException ex) throw ex;
            return val;
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return super.entrySet()
                        .stream()
                        .filter(e->!(e.getValue() instanceof RuntimeException))
                        .collect(Collectors.toSet());
        }
    }
}