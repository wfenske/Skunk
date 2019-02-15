package de.ovgu.skunk.detection.data;

import com.thoughtworks.xstream.XStream;
import de.ovgu.skunk.detection.input.ParsedFunctionSignature;
import de.ovgu.skunk.util.LinkedGroupingListMap;

import java.io.Reader;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;

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
    private Map<String, LinkedGroupingListMap<String, Method>> methodsPerFile;

    /**
     * Instantiates a new method collection.
     */
    public MethodCollection() {
        methodsPerFile = new LinkedHashMap<>();
    }

    /**
     * Adds the method to file.
     *
     * @param fp     the name of the srcML source file (usually something like <code>&quot;alloc.c.xml&quot;</code>)
     * @param method the method
     */
    public void AddFunctionToFile(FilePath fp, Method method) {
        LinkedGroupingListMap<String, Method> methodsBySignature = findMethodsForFile(fp);

        if (methodsBySignature == null) {
            methodsBySignature = new LinkedGroupingListMap<String, Method>() {
                @Override
                protected List newCollection() {
                    return new ArrayList(1);
                }
            };
            methodsPerFile.put(fp.pathKey, methodsBySignature);
        }

        methodsBySignature.put(method.originalFunctionSignature, method);
    }

    /**
     * Gets the method of a file based on the function signature
     *
     * @param fp                a string denoting the source file. This can either be the name of the SrcML file or the
     *                          key generated from this file name, pointing to the actual C file from which the SrcML
     *                          was generated.
     * @param functionSignature the function signature
     * @return the method, if found, <code>null</code> otherwise
     */
    public Method FindFunction(FilePath fp, ParsedFunctionSignature functionSignature) {
        // get the method based on the method signature
        LinkedGroupingListMap<String, Method> functionsForFile = findMethodsForFile(fp);
        if (functionsForFile == null) {
            return null;
        }

        List<Method> functionsWithSameSignature = functionsForFile.get(functionSignature.signature);
        if (functionsWithSameSignature == null) {
            return null;
        }

        final int signatureStart = functionSignature.cStartLoc;
        for (Method f : functionsWithSameSignature) {
            if (f.start1 == signatureStart) return f;
        }

        return null;
    }

    private LinkedGroupingListMap<String, Method> findMethodsForFile(FilePath fp) {
        return methodsPerFile.get(fp.pathKey);
    }

    /**
     * Calculate metrics for all metrics after finishing the collection
     */
    public void PostAction() {
        // Maybe adjust function end positions that src2srcml got wrong.
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
    public Consumer<Writer> SerializeMethods() {
        for (Method meth : AllMethods()) {
            meth.loac.clear();
        }
        XStream stream = new XStream();
        Map<String, List<Method>> methodsForSerialization = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedGroupingListMap<String, Method>> e : methodsPerFile.entrySet()) {
            String filename = e.getKey();
            LinkedGroupingListMap<String, Method> methodBySig = e.getValue();
            List<Method> methodList = new ArrayList<>();
            for (List<Method> methods : methodBySig.getMap().values()) {
                methodList.addAll(methods);
            }
            methodsForSerialization.put(filename, methodList);
        }

        return (writer -> stream.toXML(methodsForSerialization, writer));
    }

    public Iterable<Method> AllMethods() {
        final Iterator<LinkedGroupingListMap<String, Method>> methodsBySigIt = methodsPerFile.values().iterator();
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
                                LinkedGroupingListMap<String, Method> methodsInNextFile = methodsBySigIt.next();
                                List<Method> values = new ArrayList<>();
                                for (List<Method> functions : methodsInNextFile.getMap().values()) {
                                    values.addAll(functions);
                                }
                                methodsIt = values.iterator();
                                break;
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
     * Deserializes the XML provided by the reader into the collection.
     *
     * @param xmlFileReader Reader providing the serialized XML representation
     */
    public void deserializeMethods(Reader xmlFileReader) {
        XStream stream = new XStream();
        Map<String, List<Method>> deserializedMethods = (Map<String, List<Method>>) stream.fromXML(xmlFileReader);
        for (Map.Entry<String, List<Method>> e : deserializedMethods.entrySet()) {
            final LinkedGroupingListMap<String, Method> methodsBySignature = new LinkedGroupingListMap<>();
            methodsPerFile.put(e.getKey(), methodsBySignature);
            for (Method f : e.getValue()) {
                methodsBySignature.put(f.originalFunctionSignature, f);
            }
        }
    }
}
