package me.lycrus;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@JsonSerialize(using = Dependency.DependencySerializer.class)
//@JsonDeserialize(using = Dependency.DependencyDeserializer.class)
public class Dependency {

    //the file must be canonical+absolute and OS independent
    @JsonProperty
    Map<Sol, Node> dependency = new HashMap<>();

    private Dependency() {
    }

    public static Dependency create() {
        return new Dependency();
    }

    public static Dependency load(File parent, PackageManager packageManager) {
        ObjectMapper om = new ObjectMapper();

        SimpleModule sm = new SimpleModule();
        sm.addDeserializer(Dependency.class, new DependencyDeserializer(packageManager));
        om.registerModule(sm);

        Dependency dependency = null;
        try {
            if (new File(parent, "dependency.json").exists()) {
                dependency = om.readValue(new File(parent, "dependency.json"), Dependency.class);
            } else {
                dependency = Dependency.create();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dependency;
    }

    public void save(File parent) {
        ObjectMapper om = new ObjectMapper();
        om.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            File out = new File(parent, "dependency.json");
            FileUtils.forceMkdirParent(out);
            om.writeValue(out, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Set<Sol> findAllPredecessors(Set<Sol> files) {
        //breadth-first to get all predecessors
        Set<Node> startNodes = filesToNodes(files);//searched, especially for starting
        Set<Node> needSearch = startNodes;//searching
        Set<Node> predecessorNodes = new LinkedHashSet<>();//searched
        while (!needSearch.isEmpty()) {
            Set<Node> nextTurn = new LinkedHashSet<>();
            for (Node node : needSearch) {
                nextTurn.addAll(node.predecessors);
            }
            //we eliminate cycle by subtract original fileNodes(which is the start) and successorNodes(which has been searched)
            nextTurn.removeAll(startNodes);
            nextTurn.removeAll(predecessorNodes);
            needSearch = nextTurn;

            predecessorNodes.addAll(nextTurn);
        }
        return nodesToFiles(predecessorNodes);
    }

    public Set<Sol> findAllSuccessors(Set<Sol> files) {
        //breadth-first to get all successors
        Set<Node> startNodes = filesToNodes(files);
        Set<Node> needSearch = startNodes;
        Set<Node> successorNodes = new LinkedHashSet<>();
        while (!needSearch.isEmpty()) {
            Set<Node> nextTurn = new LinkedHashSet<>();
            for (Node node : needSearch) {
                nextTurn.addAll(node.successors);
            }
            //we eliminate cycle by subtract original fileNodes(which is the start) and successorNodes(which has been searched)
            nextTurn.removeAll(startNodes);
            nextTurn.removeAll(successorNodes);
            needSearch = nextTurn;

            successorNodes.addAll(nextTurn);
        }
        return nodesToFiles(successorNodes);
    }

    //dependsOn == predecessor
    //set a new node state, adjusted to both modification or creation
    public Dependency setNode(Sol file, Set<Sol> dependsOns) {
        Node fileNode = getNodeNotNull(file);

        //first, delete old state
        for (Node predecessorNode : fileNode.predecessors) {//if this is a new node, predecessor would be empty
            predecessorNode.successors.remove(fileNode);
        }
        fileNode.predecessors.clear();

        //second, set new state
        for (Sol dependsOn : dependsOns) {
            Node predecessorNode = getNodeNotNull(dependsOn);
            predecessorNode.successors.add(fileNode);
            fileNode.predecessors.add(predecessorNode);
        }

        return this;
    }

    //{must} call this {after} applying modification
    public Dependency removeNode(Set<Sol> files) {
        Iterator<Sol> it = files.iterator();
        while (it.hasNext()) {
            Sol file = it.next();

            Node fileNode = getNodeNotNull(file);

            //first, delete old state
            for (Node predecessorNode : fileNode.predecessors) {
                predecessorNode.successors.remove(fileNode);
            }
            fileNode.predecessors.clear();

            //seconde, delete the node itself
            if (!fileNode.successors.isEmpty()) {

                for (Node successorNode : fileNode.successors) {
                    if (!files.contains(successorNode.file)) {
                        System.out.println(file.fileLocation + " has be imported by " + successorNode.file.fileLocation);
                    }
                }
            }
            dependency.remove(file);
        }

        return this;
    }

    private Set<Node> filesToNodes(Set<Sol> files) {
        /*Set<Node> nodes = new HashSet<>();
        for (Sol file: files) {
            Node node = getNodeNotNull(file);
            nodes.add(node);
        }
        return nodes;*/
        Set<Node> ret = files.stream().map(file -> getNodeNotNull(file)).collect(Collectors.toSet());
        return ret;
    }

    private Set<Sol> nodesToFiles(Set<Node> nodes) {
        /*Set<Sol> file = new HashSet<>();
        for (Node node: nodes) {
            file.add(node.file);
        }
        return file;*/

        Set<Sol> ret = nodes.stream().map(node -> node.file).collect(Collectors.toSet());
        return ret;

    }


    private Node getNodeNotNull(Sol file) {
        Node node = dependency.get(file);
        if (node == null) {
            node = new Node();
            node.file = file;
            dependency.put(file, node);
        }
        return node;
    }


    @Setter
    @Getter
    public class Node {

        /*
        A       B: imports A        C: imports B
        A   ->  B   ->  C

        B = node;  B = successor of A;  B = predecessor of C
        A = predecessor of B
        C = successor of B
        */

        public Set<Node> predecessors = new LinkedHashSet<>();// for the sol files which this node depends on
        public Set<Node> successors = new LinkedHashSet<>();//for the sol files which depends on this node
        //public File file;//for the sol file
        public Sol file;//for the sol file, self ref

        @Override
        public String toString() {
            return "Node{" +
                    "file=" + file +
                    '}';
        }
    }

    @NoArgsConstructor
    public static class DependencySerializer extends JsonSerializer<Dependency> {
        @Override
        public void serialize(Dependency value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            Map<Sol, Node> dependency = value.dependency;
            gen.writeStartObject();
            for (Node node : dependency.values()) {
                gen.writeArrayFieldStart(node.file.fileLocation.toString());
                for (Node predecessorrNodes : node.predecessors) {
                    Path predecessorFile = predecessorrNodes.file.fileLocation;
                    gen.writeString(predecessorFile.toString());
                }
                gen.writeEndArray();
            }
            gen.writeEndObject();
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class DependencyDeserializer extends JsonDeserializer<Dependency> {
        public PackageManager packageManager;

        @Override
        public Dependency deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            ObjectNode root = (ObjectNode) node;
            Dependency dependency = Dependency.create();

            Iterator<Map.Entry<String, JsonNode>> it = root.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> inner = it.next();

                Sol file = Sol.getSol(Paths.get(inner.getKey()));
                Set<Sol> dependsOn = new HashSet<>();

                ArrayNode dependsOnArray = (ArrayNode) inner.getValue();
                Iterator<JsonNode> dependsIt = dependsOnArray.elements();
                while (dependsIt.hasNext()) {
                    Sol dependsOnFile = Sol.getSol(Paths.get(dependsIt.next().asText()));
                    dependsOn.add(dependsOnFile);
                }

                dependency.setNode(file, dependsOn);
            }
            return dependency;
        }
    }
}

