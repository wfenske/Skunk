package de.ovgu.skunk.detection.data;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.util.*;

/**
 * The Class MethodCollection.
 */
public class MethodCollection {
    /**
     * <p>
     * Nested map of methods, which has the form
     * <code>&lt;Filename, Map&lt;MethodSignature, Method&gt;&gt;</code>, where
     * both <code>Filename</code> and <code>MethodSignature</code> are of type
     * <code>String</code>.
     * </p>
     * <p>
     * <p>
     * Both, files and methods per file are returned in the order they were inserted.
     * </p>
     */
    private Map<String, Map<String, Method>> methodsPerFile;

    /**
     * Instantiates a new method collection.
     */
    public MethodCollection() {
        methodsPerFile = new LinkedHashMap<>();
    }

    /**
     * Adds the method to file.
     *
     * @param fileName the name of the srcML source file (usually something like <code>&quot;alloc.c.xml&quot;</code>)
     * @param method   the method
     */
    public void AddFunctionToFile(String fileName, Method method) {
        Map<String, Method> methods = findMethodsForFile(fileName);
        if (methods == null) {
            methods = new LinkedHashMap<>();
            String fileKey = FileCollection.KeyFromFilePath(fileName);
            methodsPerFile.put(fileKey, methods);
        }
        methods.put(method.functionSignatureXml, method);
    }

    /**
     * Gets the methods of file.
     *
     * @param fileName the file name
     * @return A possibly empty collection of the methods within the file. This collection should not be modified.
     */
    public Collection<Method> GetMethodsOfFile(String fileName) {
        Map<String, Method> methods = findMethodsForFile(fileName);
        if (methods != null)
            return methods.values();
        else return Collections.emptyList();
    }

    /**
     * Gets the method of a file based on the function signature
     *
     * @param fileDesignator    a string denoting the source file. This can either be the name of the SrcML file or the
     *                          key generated from this file name, pointing to the actual C file from which the SrcML
     *                          was generated.
     * @param functionSignature the function signature
     * @return the method, if found, <code>null</code> otherwise
     */
    public Method FindFunction(String fileDesignator, String functionSignature) {
        // get the method based on the method signature
        Map<String, Method> methods = findMethodsForFile(fileDesignator);
        if (methods != null) {
            return methods.get(functionSignature);
        }
        return null;
    }

    private Map<String, Method> findMethodsForFile(String fileDesignator) {
        String key = FileCollection.KeyFromFilePath(fileDesignator);
        return methodsPerFile.get(key);
    }

    /**
     * Calculate metrics for all metrics after finishing the collection
     */
    public void PostAction() {
        // Maybe adjust function end positions that src2srcml got wrong.
        adjustImprobableFunctionEndPositions();
        for (Method meth : AllMethods()) {
            meth.InitializeNetLocMetric();
            meth.SetNegationCount();
            meth.SetNumberOfFeatureConstantsNonDup();
            meth.SetNumberOfFeatureLocations();
            meth.SetNestingSum();
        }
    }

    /**
     * Serialize the features into a xml representation
     *
     * @return A xml representation of this object.
     */
    public String SerializeMethods() {
        for (Method meth : AllMethods()) {
            meth.loac.clear();
        }
        XStream stream = new XStream();
        Map<String, Collection<Method>> methodsForSerialization = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Method>> e : methodsPerFile.entrySet()) {
            String filename = e.getKey();
            Map<String, Method> methodBySig = e.getValue();
            List<Method> methodList = new ArrayList<>(methodBySig.values());
            methodsForSerialization.put(filename, methodList);
        }
        String xmlFeatures = stream.toXML(methodsForSerialization);
        return xmlFeatures;
    }

    public void adjustImprobableFunctionEndPositions() {
        for (Map<String, Method> methodsInFile : methodsPerFile.values()) {
            List<Method> functionsByStartPos = new ArrayList<>();
            functionsByStartPos.addAll(methodsInFile.values());
            Collections.sort(functionsByStartPos, Method.COMP_BY_OCCURRENCE);

            Method previousFunc = null;
            for (Method nextFunc : functionsByStartPos) {
                if (previousFunc != null) {
                    previousFunc.maybeAdjustMethodEndBasedOnNextFunction(nextFunc);
                }
                previousFunc = nextFunc;
            }
        }
    }

    public Iterable<Method> AllMethods() {
        final Iterator<Map<String, Method>> methodsBySigIt = methodsPerFile.values().iterator();
        return new Iterable<Method>() {
            @Override
            public Iterator<Method> iterator() {
                return new Iterator<Method>() {
                    Iterator<Method> methodsIt = Collections.emptyIterator();

                    @Override
                    public boolean hasNext() {
                        return ensureMethodsIt().hasNext();
                    }

                    @Override
                    public Method next() {
                        return ensureMethodsIt().next();
                    }

                    private Iterator<Method> ensureMethodsIt() {
                        if (!methodsIt.hasNext()) {
                            while (methodsBySigIt.hasNext()) {
                                Map<String, Method> methodsInNextFile = methodsBySigIt.next();
                                if (!methodsInNextFile.isEmpty()) {
                                    methodsIt = methodsInNextFile.values().iterator();
                                    break;
                                }
                            }
                        }
                        return methodsIt;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * Deserializes an XML string into the collection.
     *
     * @param xmlFile File holding the serialized xml representation
     */
    public void deserializeMethods(File xmlFile) {
        XStream stream = new XStream();
        Map<String, List<Method>> deserializedMethods = (Map<String, List<Method>>) stream.fromXML(xmlFile);
        for (Map.Entry<String, List<Method>> e : deserializedMethods.entrySet()) {
            final Map<String, Method> methods = new LinkedHashMap<>();
            methodsPerFile.put(e.getKey(), methods);
            for (Method method : e.getValue()) {
                methods.put(method.functionSignatureXml, method);
            }
        }
    }
}
